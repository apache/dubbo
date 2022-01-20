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
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.tri.AbstractTransportObserver;
import org.apache.dubbo.rpc.protocol.tri.ClassLoadUtil;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.H2TransportObserver;
import org.apache.dubbo.rpc.protocol.tri.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.TripleHttp2ClientResponseHandler;
import org.apache.dubbo.rpc.protocol.tri.WriteQueue;
import org.apache.dubbo.rpc.protocol.tri.command.CancelQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.frame.TriDecoder;

import com.google.protobuf.Any;
import com.google.rpc.DebugInfo;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;


public class ClientStream extends AbstractStream implements Stream {
    private static final Logger logger = LoggerFactory.getLogger(ClientStream.class);

    public final ClientStreamListener listener;
    public final H2TransportObserver remoteObserver = new ClientTransportObserver();
    private final WriteQueue writeQueue;

    private boolean remoteClosed;

    public ClientStream(URL url,
                        Executor executor,
                        Channel parent,
                        ClientStreamListener listener) {
        super(url, executor);
        this.writeQueue = createWriteQueue(parent);
        this.listener = listener;
    }


    WriteQueue createWriteQueue(Channel parent) {
        final Http2StreamChannelBootstrap streamChannelBootstrap = new Http2StreamChannelBootstrap(parent);
        final Future<Http2StreamChannel> future = streamChannelBootstrap.open().syncUninterruptibly();
        if (!future.isSuccess()) {
            listener.complete(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                .withDescription("Create remote stream failed"), null);
            return null;
        }
        final Http2StreamChannel channel = future.getNow();
        channel.pipeline()
            .addLast(new TripleCommandOutBoundHandler())
            .addLast(new TripleHttp2ClientResponseHandler(this));
        return new WriteQueue(channel);
    }

    public void startCall(Http2Headers headers) {
        if (this.writeQueue == null) {
            // already processed at createStream()
            return;
        }
        final HeaderQueueCommand headerCmd = HeaderQueueCommand.createHeaders(headers);
        this.writeQueue.enqueue(headerCmd, true);
    }

    public void cancelByLocal(Throwable t) {
        RpcContext.getCancellationContext().cancel(t);
    }

    @Override
    public URL url() {
        return null;
    }

    @Override
    public void writeMessage(byte[] message) {
        try {
            final DataQueueCommand cmd = DataQueueCommand.createGrpcCommand(message, false);
            this.writeQueue.enqueue(cmd, true);
        } catch (Throwable t) {
            cancelByLocal(t);
        }

    }

    public void complete() {
        final DataQueueCommand cmd = DataQueueCommand.createGrpcCommand(new byte[0], true);
        this.writeQueue.enqueue(cmd, true);
    }

    class ClientTransportObserver extends AbstractTransportObserver implements H2TransportObserver {
        private GrpcStatus transportError;
        private DeCompressor decompressor;
        private TriDecoder decoder;
        private boolean headerReceived;
        private boolean streamClosed;

        void handleH2TransportError(GrpcStatus status, Http2Headers trailers) {
            writeQueue.enqueue(CancelQueueCommand.createCommand(status), true);
            finishProcess(status, trailers);
        }

        void finishProcess(GrpcStatus status, Http2Headers trailers) {
            if (streamClosed) {
                return;
            }
            streamClosed = true;

            final Map<String, Object> attachments = headersToMap(trailers);
            if (!status.isOk()) {
                final Throwable throwableFromTrailers = getThrowableFromTrailers(trailers);
                if (throwableFromTrailers != null) {
                    listener.complete(status.withCause(throwableFromTrailers), attachments);
                } else {
                    listener.complete(status, attachments);
                }
            } else {
                listener.complete(status, attachments);
            }
            if (decoder != null) {
                decoder.close();
            }
        }

        Throwable getThrowableFromTrailers(Http2Headers metadata) {
            if (null == metadata) {
                return null;
            }
            // second get status detail
            if (!metadata.contains(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader())) {
                return null;
            }
            final CharSequence raw = metadata.get(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader());
            byte[] statusDetailBin = H2TransportObserver.decodeASCIIByte(raw);
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            try {
                final Status statusDetail = (Status)Status.parseFrom(statusDetailBin);
                List<Any> detailList = statusDetail.getDetailsList();
                Map<Class<?>, Object> classObjectMap = tranFromStatusDetails(detailList);

                // get common exception from DebugInfo
                DebugInfo debugInfo = (DebugInfo) classObjectMap.get(DebugInfo.class);
                if (debugInfo == null) {
                    return new RpcException(statusDetail.getCode(),
                        GrpcStatus.decodeMessage(statusDetail.getMessage()));
                }
                String msg = ExceptionUtils.getStackFrameString(debugInfo.getStackEntriesList());
                return new RpcException(statusDetail.getCode(), msg);
            } catch (IOException ioException) {
                return null;
            } finally {
                ClassLoadUtil.switchContextLoader(tccl);
            }
        }

        private Map<Class<?>, Object> tranFromStatusDetails(List<Any> detailList) {
            Map<Class<?>, Object> map = new HashMap<>();
            try {
                for (Any any : detailList) {
                    if (any.is(ErrorInfo.class)) {
                        ErrorInfo errorInfo = any.unpack(ErrorInfo.class);
                        map.putIfAbsent(ErrorInfo.class, errorInfo);
                    } else if (any.is(DebugInfo.class)) {
                        DebugInfo debugInfo = any.unpack(DebugInfo.class);
                        map.putIfAbsent(DebugInfo.class, debugInfo);
                    }
                    // support others type but now only support this
                }
            } catch (Throwable t) {
                logger.error("tran from grpc-status-details error", t);
            }
            return map;
        }


        private GrpcStatus validateHeaderStatus(Http2Headers headers) {
            Integer httpStatus = headers.status() == null ? null : Integer.parseInt(headers.status().toString());
            if (httpStatus == null) {
                return GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL).withDescription("Missing HTTP status code");
            }
            final CharSequence contentType = headers.get(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader());
            if (contentType == null || !contentType.toString().startsWith(TripleHeaderEnum.APPLICATION_GRPC.getHeader())) {
                return GrpcStatus.fromCode(GrpcStatus.httpStatusToGrpcCode(httpStatus))
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
                transportError = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
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
                    DeCompressor compressor = DeCompressor.getCompressor(url().getOrDefaultFrameworkModel(), compressorStr);
                    if (null == compressor) {
                        throw GrpcStatus.fromCode(GrpcStatus.Code.UNIMPLEMENTED)
                            .withDescription(String.format("Grpc-encoding '%s' is not supported", compressorStr))
                            .asException();
                    } else {
                        decompressor = compressor;
                    }
                }
            }
            TriDecoder.Listener listener = ClientStream.this.listener::onMessage;
            decoder = new TriDecoder(decompressor, listener);
            decoder.request(Integer.MAX_VALUE);
        }

        void onTrailersReceived(Http2Headers trailers) {
            if (transportError == null && !headerReceived) {
                transportError = validateHeaderStatus(trailers);
            }
            if (transportError != null) {
                transportError = transportError.appendDescription("trailers: " + trailers);
            } else {
                GrpcStatus status = statusFromTrailers(trailers);
                finishProcess(status, trailers);
            }
        }

        /**
         * Extract the response status from trailers.
         */
        private GrpcStatus statusFromTrailers(Http2Headers trailers) {
            final Integer intStatus = trailers.getInt(TripleHeaderEnum.STATUS_KEY.getHeader());
            GrpcStatus status = intStatus == null ? null : GrpcStatus.fromCode(intStatus);
            if (status != null) {
                final CharSequence message = trailers.get(TripleHeaderEnum.MESSAGE_KEY.getHeader());
                if (message != null) {
                    status.withDescription(message.toString());
                }
                return status;
            }
            // No status; something is broken. Try to provide a resonanable error.
            if (headerReceived) {
                return GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN).withDescription("missing GRPC status in response");
            }
            Integer httpStatus = trailers.status() == null ? null : Integer.parseInt(trailers.status().toString());
            if (httpStatus != null) {
                status = GrpcStatus.fromCode(GrpcStatus.httpStatusToGrpcCode(httpStatus));
            } else {
                status = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL).withDescription("missing HTTP status code");
            }
            return status.appendDescription("missing GRPC status, inferred error from HTTP status code");
        }

        @Override
        public void onHeader(Http2Headers headers, boolean endStream) {
            if (endStream) {
                if (!remoteClosed) {
                    writeQueue.enqueue(CancelQueueCommand.createCommand(GrpcStatus.fromCode(GrpcStatus.Code.CANCELLED)), true);
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
                    handleH2TransportError(transportError, null);

                }
                return;
            }
            if (!headerReceived) {
                handleH2TransportError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription("headers not received before payload"), null);
                return;
            }
            decoder.deframe(data);
        }

        @Override
        public void cancelByRemote(GrpcStatus status) {
            transportError = status;
            if (status.code == GrpcStatus.Code.CANCELLED) {
                listener.complete(status, null);
            } else {
                finishProcess(status, null);
            }
        }
    }
}