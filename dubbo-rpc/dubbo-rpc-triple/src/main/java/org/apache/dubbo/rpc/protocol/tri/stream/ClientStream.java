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
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
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
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executor;


/**
 * ClientStream is an abstraction for bi-directional messaging. It maintains a {@link WriteQueue} to
 * write Http2Frame to remote. A {@link H2TransportListener} receives Http2Frame from remote.
 * Instead of maintaining state, this class depends on upper layer or transport layer's states.
 */
public class ClientStream extends AbstractStream implements Stream {

    public final ClientStreamListener listener;
    private final WriteQueue writeQueue;
    private EventLoop eventLoop;
    private Deframer deframer;

    // for test
    ClientStream(FrameworkModel frameworkModel,
        Executor executor,
        WriteQueue writeQueue,
        ClientStreamListener listener) {

        super(executor, frameworkModel);
        this.listener = listener;
        this.writeQueue = writeQueue;
    }

    public ClientStream(FrameworkModel frameworkModel,
        Executor executor,
        Channel parent,
        ClientStreamListener listener) {
        super(executor, frameworkModel);
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
        eventLoop = channel.eventLoop();
        channel.pipeline()
            .addLast(new TripleCommandOutBoundHandler())
            .addLast(new TripleHttp2ClientResponseHandler(createTransportListener()));
        return new WriteQueue(channel);
    }

    public void close() {
        writeQueue.close();
    }

    public void startCall(RequestMetadata metadata) {
        if (this.writeQueue == null) {
            // already processed at createStream()
            return;
        }
        DefaultHttp2Headers headers = StreamUtils.metadataToHeaders(metadata);

        final HeaderQueueCommand headerCmd = HeaderQueueCommand.createHeaders(headers);
        this.writeQueue.enqueue(headerCmd).addListener(future -> {
            if (!future.isSuccess()) {
                transportException(future.cause());
            }
        });
    }

    private void transportException(Throwable cause) {
        final TriRpcStatus status = TriRpcStatus.INTERNAL.withDescription("Http2 exception")
            .withCause(cause);
        listener.complete(status);
    }

    public void cancelByLocal(TriRpcStatus status) {
        final CancelQueueCommand cmd = CancelQueueCommand.createCommand();
        this.writeQueue.enqueue(cmd);
    }


    @Override
    public void writeMessage(byte[] message, int compressed) {
        try {
            final DataQueueCommand cmd = DataQueueCommand.createGrpcCommand(message, false,
                compressed);
            this.writeQueue.enqueue(cmd);
        } catch (Throwable t) {
            cancelByLocal(
                TriRpcStatus.INTERNAL.withDescription("Client write message failed").withCause(t));
        }
    }

    @Override
    public void requestN(int n) {
        deframer.request(n);
    }

    public void halfClose() {
        final EndStreamQueueCommand cmd = EndStreamQueueCommand.create();
        this.writeQueue.enqueue(cmd);
    }

    @Override
    EventLoop getEventLoop() {
        return eventLoop;
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
        private boolean remoteClosed;
        private boolean headerReceived;
        private Http2Headers trailers;

        void handleH2TransportError(TriRpcStatus status) {
            writeQueue.enqueue(CancelQueueCommand.createCommand());
            finishProcess(status, null);
        }

        void finishProcess(TriRpcStatus status, Http2Headers trailers) {
            if (remoteClosed) {
                return;
            }
            remoteClosed = true;

            final Map<String, String> reserved = filterReservedHeaders(trailers);
            final Map<String, Object> attachments = headersToMap(trailers);
            listener.complete(status, attachments, reserved);
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
                    ClientStream.this.listener.onMessage(data);
                }

                public void close() {
                    finishProcess(statusFromTrailers(trailers), trailers);
                }
            };
            deframer = new TriDecoder(decompressor, listener);
            ClientStream.this.listener.onStart();
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
                    if (!remoteClosed) {
                        writeQueue.enqueue(CancelQueueCommand.createCommand());
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
        public void cancelByRemote(TriRpcStatus status) {
            executor.execute(() -> {
                transportError = status;
                finishProcess(status, null);
            });
        }
    }
}
