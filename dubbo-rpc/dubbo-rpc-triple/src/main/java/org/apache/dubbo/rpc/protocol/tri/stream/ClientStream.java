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
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.tri.AbstractTransportObserver;
import org.apache.dubbo.rpc.protocol.tri.ClassLoadUtil;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.H2TransportObserver;
import org.apache.dubbo.rpc.protocol.tri.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.TripleHttp2ClientResponseHandler;
import org.apache.dubbo.rpc.protocol.tri.WriteQueue;
import org.apache.dubbo.rpc.protocol.tri.command.CancelQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.frame.TriDecoder;
import org.apache.dubbo.rpc.protocol.tri.pack.Pack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbUnpack;
import org.apache.dubbo.rpc.protocol.tri.pack.Unpack;

import com.google.protobuf.Any;
import com.google.rpc.DebugInfo;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;


public class ClientStream extends AbstractStream implements Stream {
    public final ClientStreamListener responseListener;
    public final H2TransportObserver remoteObserver = new ClientTransportObserver();
    private final DefaultHttp2Headers headers;
    private final long id;
    private final WriteQueue writeQueue;
    private final Pack requestPack;
    private final Unpack responseUnpack;
    private final Compressor compressor;
    private boolean remoteClosed;

    public ClientStream(URL url,
                        Executor executor,
                        long id,
                        Channel parent,
                        AsciiString scheme,
                        String path,
                        String serviceVersion,
                        String serviceGroup,
                        String application,
                        String authority,
                        String encoding,
                        String acceptEncoding,
                        String timeout,
                        Compressor compressor,
                        Map<String, Object> attachments,
                        Pack requestPack,
                        Unpack responseUnpack,
                        ClientStreamListener listener) {
        super(url, executor);
        this.id = id;
        this.compressor = compressor;
        this.writeQueue = createWriteQueue(parent);
        this.requestPack = requestPack;
        this.responseUnpack = responseUnpack;
        this.responseListener = listener;
        this.headers = new DefaultHttp2Headers(false);
        this.headers.scheme(scheme)
            .authority(authority)
            .method(HttpMethod.POST.asciiName())
            .path(path)
            .set(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader(), TripleConstant.CONTENT_PROTO)
            .set(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);
        setIfNotNull(headers, TripleHeaderEnum.TIMEOUT.getHeader(), timeout);
        setIfNotNull(headers, TripleHeaderEnum.SERVICE_VERSION.getHeader(), serviceVersion);
        setIfNotNull(headers, TripleHeaderEnum.SERVICE_GROUP.getHeader(), serviceGroup);
        setIfNotNull(headers, TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader(), application);
        setIfNotNull(headers, TripleHeaderEnum.GRPC_ENCODING.getHeader(), encoding);
        setIfNotNull(headers, TripleHeaderEnum.GRPC_ACCEPT_ENCODING.getHeader(), acceptEncoding);
        if (attachments != null) {
            convertAttachment(headers, attachments);
        }
        this.cancellationContext.addListener(context -> {
            Throwable throwable = cancellationContext.getCancellationCause();
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Triple request to "
                    + path +
                    " was canceled by local exception ", throwable);
            }
        });
    }

    private void setIfNotNull(DefaultHttp2Headers headers, CharSequence key, CharSequence value) {
        if (key == null) {
            return;
        }
        headers.set(key, value);
    }

    WriteQueue createWriteQueue(Channel parent) {
        final Http2StreamChannelBootstrap streamChannelBootstrap = new Http2StreamChannelBootstrap(parent);
        final Future<Http2StreamChannel> future = streamChannelBootstrap.open().syncUninterruptibly();
        if (!future.isSuccess()) {
            DefaultFuture2.getFuture(id).cancel();
            return null;
        }
        final Http2StreamChannel channel = future.getNow();
        channel.pipeline()
            .addLast(new TripleCommandOutBoundHandler())
            .addLast(new TripleHttp2ClientResponseHandler(this));
        DefaultFuture2.addTimeoutListener(id, channel::close);
        return new WriteQueue(channel);
    }

    public void startCall() {
        if (this.writeQueue == null) {
            // already processed at createStream()
            return;
        }
        final HeaderQueueCommand headerCmd = HeaderQueueCommand.createHeaders(headers);
        this.writeQueue.enqueue(headerCmd, false);
    }

    public void sendMessage(Object message){
        try {
            final byte[] data = requestPack.pack(message);

            final byte[] compress = compressor.compress(data);

            final DataQueueCommand dataCmd = DataQueueCommand.createGrpcCommand(compress, false);
            this.writeQueue.enqueue(dataCmd, false);
        }catch (Throwable t){

        }
    }

    public void cancelByLocal(Throwable t) {
        RpcContext.getCancellationContext().cancel(t);
    }

    @Override
    public URL url() {
        return null;
    }

    class ClientTransportObserver extends AbstractTransportObserver implements H2TransportObserver {
        private final PbUnpack STATUS_DETAIL_UNPACK = new PbUnpack(Status.class);
        private GrpcStatus transportError;
        private DeCompressor decompressor;
        private TriDecoder decoder;
        private Object appResponse;
        private boolean headerReceived;

        void handleH2TransportError(GrpcStatus status, Http2Headers trailers) {
            writeQueue.enqueue(CancelQueueCommand.createCommand(status), true);
            finishProcess(status, trailers);
        }

        void finishProcess(GrpcStatus status, Http2Headers trailers) {
            final Map<String, Object> attachments = headersToMap(trailers);
            if(!status.isOk()){
                final Throwable throwableFromTrailers = getThrowableFromTrailers(trailers);
                if(throwableFromTrailers!=null){
                    responseListener.complete( status.withCause(throwableFromTrailers), attachments);
                }else{
                    responseListener.complete(status,attachments);
                }
            }else{
                responseListener.complete(status,attachments);
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
                final Status statusDetail = (Status) STATUS_DETAIL_UNPACK.unpack(statusDetailBin);
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
            } catch (IOException | ClassNotFoundException ioException) {
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
                LOGGER.error("tran from grpc-status-details error", t);
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
            TriDecoder.Listener listener = data -> {
                try {
                    appResponse = responseUnpack.unpack(data);
                    responseListener.onMessage(appResponse);
                } catch (IOException | ClassNotFoundException e) {
                    finishProcess(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                        .withDescription("Decode response failed")
                        .withCause(e), null);
                }
            };
            decoder = new TriDecoder(decompressor, listener);
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
                return status.withDescription(trailers.get(TripleHeaderEnum.MESSAGE_KEY.getHeader()).toString());
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
        public void onError(GrpcStatus status) {
            // handle cancel

        }

        @Override
        public void cancelByRemote() {
            DefaultFuture2.getFuture(id).cancel();
        }
    }

    public void complete(){

    }
}