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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.protocol.tri.DefaultFuture2;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.RpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.TripleHttp2ClientResponseHandler;
import org.apache.dubbo.rpc.protocol.tri.WriteQueue;
import org.apache.dubbo.rpc.protocol.tri.command.CancelQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.EndStreamQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.frame.Deframer;
import org.apache.dubbo.rpc.protocol.tri.frame.TriDecoder;
import org.apache.dubbo.rpc.protocol.tri.observer.AbstractTransportObserver;
import org.apache.dubbo.rpc.protocol.tri.observer.H2TransportObserver;

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


public class ClientStream extends AbstractStream implements Stream {
    private static final Logger logger = LoggerFactory.getLogger(ClientStream.class);
    public final ClientStreamListener listener;
    public final H2TransportObserver remoteObserver = new ClientTransportObserver();
    private final WriteQueue writeQueue;
    private final long requestId;
    private final URL url;
    private EventLoop eventLoop;
    private boolean canceled;
    private boolean headerReceived;
    private Deframer deframer;
    private Http2Headers trailers;

    public ClientStream(URL url,
                        long requestId,
                        Channel parent,
                        ClientStreamListener listener) {
        this.url = url;
        this.requestId = requestId;
        this.writeQueue = createWriteQueue(parent);
        this.listener = listener;
    }

    WriteQueue createWriteQueue(Channel parent) {
        final Http2StreamChannelBootstrap bootstrap = new Http2StreamChannelBootstrap(parent);
        final Future<Http2StreamChannel> future = bootstrap.open().syncUninterruptibly();
        if (!future.isSuccess()) {
            listener.complete(RpcStatus.INTERNAL
                .withDescription("Create remote stream failed"), null);
            return null;
        }
        final Http2StreamChannel channel = future.getNow();
        eventLoop = channel.eventLoop();
        channel.pipeline()
            .addLast(new TripleCommandOutBoundHandler())
            .addLast(new TripleHttp2ClientResponseHandler(this));
        DefaultFuture2.addTimeoutListener(requestId, channel::close);
        return new WriteQueue(channel);
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
        final RpcStatus status = RpcStatus.INTERNAL
            .withDescription("Http2 exception")
            .withCause(cause);
        listener.complete(status);
    }

    public void cancelByLocal(RpcStatus status) {
        if (canceled) {
            return;
        }
        canceled = true;
        final CancelQueueCommand cmd = CancelQueueCommand.createCommand();
        this.writeQueue.enqueue(cmd);
    }


    @Override
    public void writeMessage(byte[] message, int compressed) {
        try {
            final DataQueueCommand cmd = DataQueueCommand.createGrpcCommand(message, false, compressed);
            this.writeQueue.enqueue(cmd);
        } catch (Throwable t) {
            cancelByLocal(RpcStatus.INTERNAL
                .withDescription("Client write message failed")
                .withCause(t));
        }
    }

    @Override
    public void requestN(int n) {
        runOnEventLoop(() -> deframer.request(n));
    }

    public void halfClose() {
        final EndStreamQueueCommand cmd = EndStreamQueueCommand.create();
        this.writeQueue.enqueue(cmd);
    }

    @Override
    EventLoop getEventLoop() {
        return eventLoop;
    }

    class ClientTransportObserver extends AbstractTransportObserver implements H2TransportObserver {
        private RpcStatus transportError;
        private DeCompressor decompressor;
        private boolean remoteClosed;

        void handleH2TransportError(RpcStatus status) {
            writeQueue.enqueue(CancelQueueCommand.createCommand());
            finishProcess(status, null);
        }

        void finishProcess(RpcStatus status, Http2Headers trailers) {
            if (remoteClosed) {
                return;
            }
            remoteClosed = true;

            final Map<String, String> reserved = filterReservedHeaders(trailers);
            final Map<String, Object> attachments = headersToMap(trailers);
            listener.complete(status, attachments, reserved);
        }

        private RpcStatus validateHeaderStatus(Http2Headers headers) {
            Integer httpStatus = headers.status() == null ? null : Integer.parseInt(headers.status().toString());
            if (httpStatus == null) {
                return RpcStatus.INTERNAL.withDescription("Missing HTTP status code");
            }
            final CharSequence contentType = headers.get(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader());
            if (contentType == null || !contentType.toString().startsWith(TripleHeaderEnum.APPLICATION_GRPC.getHeader())) {
                return RpcStatus.fromCode(RpcStatus.httpStatusToGrpcCode(httpStatus))
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
                transportError = RpcStatus.INTERNAL
                    .withDescription("Received headers twice");
                return;
            }
            Integer httpStatus = headers.status() == null ? null : Integer.parseInt(headers.status().toString());

            if (httpStatus != null && Integer.parseInt(httpStatus.toString()) > 100 && httpStatus < 200) {
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
                    DeCompressor compressor = DeCompressor.getCompressor(url.getOrDefaultFrameworkModel(), compressorStr);
                    if (null == compressor) {
                        throw RpcStatus.UNIMPLEMENTED
                            .withDescription(String.format("Grpc-encoding '%s' is not supported", compressorStr))
                            .asException();
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
                ClientStream.this.trailers = trailers;
                RpcStatus status = statusFromTrailers(trailers);
                if (deframer == null) {
                    finishProcess(status, trailers);
                }
                if (deframer != null) {
                    deframer.close();
//                    deframer = null;
                }
            }
        }

        /**
         * Extract the response status from trailers.
         */
        private RpcStatus statusFromTrailers(Http2Headers trailers) {
            final Integer intStatus = trailers.getInt(TripleHeaderEnum.STATUS_KEY.getHeader());
            RpcStatus status = intStatus == null ? null : RpcStatus.fromCode(intStatus);
            if (status != null) {
                final CharSequence message = trailers.get(TripleHeaderEnum.MESSAGE_KEY.getHeader());
                if (message != null) {
                    final String description = RpcStatus.decodeMessage(message.toString());
                    status = status.withDescription(description);
                }
                return status;
            }
            // No status; something is broken. Try to provide a rational error.
            if (headerReceived) {
                return RpcStatus.UNKNOWN.withDescription("missing GRPC status in response");
            }
            Integer httpStatus = trailers.status() == null ? null : Integer.parseInt(trailers.status().toString());
            if (httpStatus != null) {
                status = RpcStatus.fromCode(RpcStatus.httpStatusToGrpcCode(httpStatus));
            } else {
                status = RpcStatus.INTERNAL.withDescription("missing HTTP status code");
            }
            return status.appendDescription("missing GRPC status, inferred error from HTTP status code");
        }

        @Override
        public void onHeader(Http2Headers headers, boolean endStream) {
            if (endStream) {
                if (!remoteClosed) {
                    writeQueue.enqueue(CancelQueueCommand.createCommand());
                }
                onTrailersReceived(headers);
            } else {
                onHeaderReceived(headers);
            }
        }

        @Override
        public void onData(ByteBuf data, boolean endStream) {
            if (transportError != null) {
                transportError.appendDescription("Data:" + data.toString(StandardCharsets.UTF_8));
                ReferenceCountUtil.release(data);
                if (transportError.description.length() > 512 || endStream) {
                    handleH2TransportError(transportError);

                }
                return;
            }
            if (!headerReceived) {
                handleH2TransportError(RpcStatus.INTERNAL
                    .withDescription("headers not received before payload"));
                return;
            }
            deframer.deframe(data);
        }

        @Override
        public void cancelByRemote(RpcStatus status) {
            transportError = status;
            finishProcess(status, null);
        }
    }
}
