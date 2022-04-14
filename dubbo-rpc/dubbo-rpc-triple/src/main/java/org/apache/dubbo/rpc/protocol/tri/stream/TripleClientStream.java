/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.rpc.protocol.tri.stream;

import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.command.CancelQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.EndStreamQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.frame.Deframer;
import org.apache.dubbo.rpc.protocol.tri.frame.TriDecoder;
import org.apache.dubbo.rpc.protocol.tri.transport.AbstractH2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.H2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleHttp2ClientResponseHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.WriteQueue;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executor;


/**
 * ClientStream is an abstraction for bi-directional messaging. It maintains a {@link WriteQueue} to
 * write Http2Frame to remote. A {@link H2TransportListener} receives Http2Frame from remote.
 * Instead of maintaining state, this class depends on upper layer or transport layer's states.
 */
public class TripleClientStream extends AbstractStream implements ClientStream {

    public final ClientStream.Listener listener;
    private final WriteQueue writeQueue;
    private Deframer deframer;
    private final Channel parent;

    // for test
    TripleClientStream(FrameworkModel frameworkModel,
        Executor executor,
        WriteQueue writeQueue,
        ClientStream.Listener listener) {
        super(executor, frameworkModel);
        this.parent = null;
        this.listener = listener;
        this.writeQueue = writeQueue;
    }

    public TripleClientStream(FrameworkModel frameworkModel,
        Executor executor,
        Channel parent,
        ClientStream.Listener listener) {
        super(executor, frameworkModel);
        this.parent = parent;
        this.listener = listener;
        this.writeQueue = createWriteQueue(parent);
    }

    private WriteQueue createWriteQueue(Channel parent) {
        final Http2StreamChannelBootstrap bootstrap = new Http2StreamChannelBootstrap(parent);
        final Future<Http2StreamChannel> future = bootstrap.open().syncUninterruptibly();
        if (!future.isSuccess()) {
            throw new IllegalStateException("Create remote stream failed. channel:" + parent);
        }
        final Http2StreamChannel channel = future.getNow();
        channel.pipeline()
            .addLast(new TripleCommandOutBoundHandler())
            .addLast(new TripleHttp2ClientResponseHandler(createTransportListener()));
        channel.closeFuture()
            .addListener(f -> transportException(f.cause()));
        return new WriteQueue(channel);
    }

    public void close() {
        writeQueue.close();
    }

    public ChannelFuture sendHeader(Http2Headers headers) {
        if (this.writeQueue == null) {
            // already processed at createStream()
            return parent.newFailedFuture(new IllegalStateException("Stream already closed"));
        }
        final HeaderQueueCommand headerCmd = HeaderQueueCommand.createHeaders(headers);
        return writeQueue.enqueue(headerCmd).addListener(future -> {
            if (!future.isSuccess()) {
                transportException(future.cause());
            }
        });
    }

    private void transportException(Throwable cause) {
        final TriRpcStatus status = TriRpcStatus.INTERNAL.withDescription("Http2 exception")
            .withCause(cause);
        listener.onComplete(status, null);
    }

    public ChannelFuture cancelByLocal(TriRpcStatus status) {
        final CancelQueueCommand cmd = CancelQueueCommand.createCommand(Http2Error.CANCEL);
        return this.writeQueue.enqueue(cmd, true);
    }

    @Override
    public SocketAddress remoteAddress() {
        return parent.remoteAddress();
    }


    @Override
    public ChannelFuture sendMessage(byte[] message, int compressFlag, boolean eos) {
        final DataQueueCommand cmd = DataQueueCommand.createGrpcCommand(message, false,
            compressFlag);
        return this.writeQueue.enqueue(cmd)
            .addListener(future -> {
                    if (!future.isSuccess()) {
                        cancelByLocal(
                            TriRpcStatus.INTERNAL.withDescription("Client write message failed")
                                .withCause(future.cause())
                        );
                        transportException(future.cause());
                    }
                }
            );
    }

    @Override
    public void request(int n) {
        deframer.request(n);
    }

    @Override
    public ChannelFuture halfClose() {
        final EndStreamQueueCommand cmd = EndStreamQueueCommand.create();
        return this.writeQueue.enqueue(cmd);
    }

    /**
     * @return transport listener
     */
    H2TransportListener createTransportListener() {
        return new ClientTransportListener();
    }

    class ClientTransportListener extends AbstractH2TransportListener implements
        H2TransportListener {

        private TriRpcStatus transportError;
        private DeCompressor decompressor;
        private boolean halfClosed;
        private boolean headerReceived;
        private Http2Headers trailers;

        void handleH2TransportError(TriRpcStatus status) {
            writeQueue.enqueue(CancelQueueCommand.createCommand(Http2Error.NO_ERROR), true);
            finishProcess(status, null);
        }

        void finishProcess(TriRpcStatus status, Http2Headers trailers) {
            if (halfClosed) {
                return;
            }
            halfClosed = true;

            final Map<String, String> reserved = filterReservedHeaders(trailers);
            final Map<String, Object> attachments = headersToMap(trailers);
            listener.onComplete(status, attachments, reserved);
        }

        private TriRpcStatus validateHeaderStatus(Http2Headers headers) {
            Integer httpStatus =
                headers.status() == null ? null : Integer.parseInt(headers.status().toString());
            if (httpStatus == null) {
                return TriRpcStatus.INTERNAL.withDescription("Missing HTTP status code");
            }
            final CharSequence contentType = headers.get(
                TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader());
            if (contentType == null || !contentType.toString()
                .startsWith(TripleHeaderEnum.APPLICATION_GRPC.getHeader())) {
                return TriRpcStatus.fromCode(TriRpcStatus.httpStatusToGrpcCode(httpStatus))
                    .withDescription("invalid content-type: " + contentType);
            }
            return null;
        }

        void onHeaderReceived(Http2Headers headers) {
            if (transportError != null) {
                transportError.appendDescription("headers:" + headers);
                return;
            }
            if (headerReceived) {
                transportError = TriRpcStatus.INTERNAL.withDescription("Received headers twice");
                return;
            }
            Integer httpStatus =
                headers.status() == null ? null : Integer.parseInt(headers.status().toString());

            if (httpStatus != null && Integer.parseInt(httpStatus.toString()) > 100
                && httpStatus < 200) {
                // ignored
                return;
            }
            headerReceived = true;
            transportError = validateHeaderStatus(headers);

            // todo support full payload compressor
            CharSequence messageEncoding = headers.get(TripleHeaderEnum.GRPC_ENCODING.getHeader());
            if (null != messageEncoding) {
                String compressorStr = messageEncoding.toString();
                if (!Identity.IDENTITY.getMessageEncoding().equals(compressorStr)) {
                    DeCompressor compressor = DeCompressor.getCompressor(frameworkModel,
                        compressorStr);
                    if (null == compressor) {
                        throw TriRpcStatus.UNIMPLEMENTED.withDescription(String.format(
                            "Grpc-encoding '%s' is not supported",
                            compressorStr)).asException();
                    } else {
                        decompressor = compressor;
                    }
                }
            }
            TriDecoder.Listener listener = new TriDecoder.Listener() {
                @Override
                public void onRawMessage(byte[] data) {
                    TripleClientStream.this.listener.onMessage(data);
                }

                public void close() {
                    finishProcess(statusFromTrailers(trailers), trailers);
                }
            };
            deframer = new TriDecoder(decompressor, listener);
            TripleClientStream.this.listener.onStart();
        }

        void onTrailersReceived(Http2Headers trailers) {
            if (transportError == null && !headerReceived) {
                transportError = validateHeaderStatus(trailers);
            }
            if (transportError != null) {
                transportError = transportError.appendDescription("trailers: " + trailers);
            } else {
                this.trailers = trailers;
                TriRpcStatus status = statusFromTrailers(trailers);
                if (deframer == null) {
                    finishProcess(status, trailers);
                }
                if (deframer != null) {
                    deframer.close();
                }
            }
        }

        /**
         * Extract the response status from trailers.
         */
        private TriRpcStatus statusFromTrailers(Http2Headers trailers) {
            final Integer intStatus = trailers.getInt(TripleHeaderEnum.STATUS_KEY.getHeader());
            TriRpcStatus status = intStatus == null ? null : TriRpcStatus.fromCode(intStatus);
            if (status != null) {
                final CharSequence message = trailers.get(TripleHeaderEnum.MESSAGE_KEY.getHeader());
                if (message != null) {
                    final String description = TriRpcStatus.decodeMessage(message.toString());
                    status = status.withDescription(description);
                }
                return status;
            }
            // No status; something is broken. Try to provide a rational error.
            if (headerReceived) {
                return TriRpcStatus.UNKNOWN.withDescription("missing GRPC status in response");
            }
            Integer httpStatus =
                trailers.status() == null ? null : Integer.parseInt(trailers.status().toString());
            if (httpStatus != null) {
                status = TriRpcStatus.fromCode(TriRpcStatus.httpStatusToGrpcCode(httpStatus));
            } else {
                status = TriRpcStatus.INTERNAL.withDescription("missing HTTP status code");
            }
            return status.appendDescription(
                "missing GRPC status, inferred error from HTTP status code");
        }

        @Override
        public void onHeader(Http2Headers headers, boolean endStream) {
            executor.execute(() -> {
                if (endStream) {
                    if (!halfClosed) {
                        writeQueue.enqueue(CancelQueueCommand.createCommand(Http2Error.CANCEL),
                            true);
                    }
                    onTrailersReceived(headers);
                } else {
                    onHeaderReceived(headers);
                }
            });

        }

        @Override
        public void onData(ByteBuf data, boolean endStream) {
            executor.execute(() -> {
                if (transportError != null) {
                    transportError.appendDescription(
                        "Data:" + data.toString(StandardCharsets.UTF_8));
                    ReferenceCountUtil.release(data);
                    if (transportError.description.length() > 512 || endStream) {
                        handleH2TransportError(transportError);

                    }
                    return;
                }
                if (!headerReceived) {
                    handleH2TransportError(TriRpcStatus.INTERNAL.withDescription(
                        "headers not received before payload"));
                    return;
                }
                deframer.deframe(data);
            });
        }

        @Override
        public void cancelByRemote(long errorCode) {
            executor.execute(() -> {
                transportError = TriRpcStatus.CANCELLED
                    .withDescription("Canceled by remote peer, errorCode=" + errorCode);
                finishProcess(transportError, null);
            });
        }
    }
}
