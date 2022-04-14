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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.call.ReflectionAbstractServerCall;
import org.apache.dubbo.rpc.protocol.tri.call.StubAbstractServerCall;
import org.apache.dubbo.rpc.protocol.tri.command.CancelQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.TextDataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.frame.Deframer;
import org.apache.dubbo.rpc.protocol.tri.frame.TriDecoder;
import org.apache.dubbo.rpc.protocol.tri.transport.AbstractH2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.H2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.WriteQueue;

import com.google.protobuf.Any;
import com.google.rpc.DebugInfo;
import com.google.rpc.Status;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class TripleServerStream extends AbstractStream implements ServerStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(TripleServerStream.class);
    public final ServerTransportObserver transportObserver = new ServerTransportObserver();
    private final WriteQueue writeQueue;
    private final PathResolver pathResolver;
    private final List<HeaderFilter> filters;
    private final String acceptEncoding;
    private boolean headerSent;
    private boolean trailersSent;
    private volatile boolean reset;
    private ServerStream.Listener listener;
    private final InetSocketAddress remoteAddress;
    private final Channel channel;
    private Deframer deframer;

    public TripleServerStream(Channel channel,
                              FrameworkModel frameworkModel,
                              Executor executor,
                              PathResolver pathResolver,
                              String acceptEncoding,
                              List<HeaderFilter> filters) {
        super(executor, frameworkModel);
        this.channel = channel;
        this.pathResolver = pathResolver;
        this.acceptEncoding = acceptEncoding;
        this.filters = filters;
        this.writeQueue = new WriteQueue(channel);
        this.remoteAddress = (InetSocketAddress) channel.remoteAddress();
    }

    @Override
    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    @Override
    public void request(int n) {
        deframer.request(n);
    }

    public ChannelFuture reset(Http2Error cause) {
        return writeQueue.enqueue(CancelQueueCommand.createCommand(cause), true);
    }

    @Override
    public ChannelFuture sendHeader(Http2Headers headers) {
        if (reset) {
            return writeQueue.failure(
                new IllegalStateException("Stream already reset, no more headers allowed"));
        }
        if (headerSent) {
            return writeQueue.failure(new IllegalStateException("Header already sent"));
        }
        if (trailersSent) {
            return writeQueue.failure(new IllegalStateException("Trailers already sent"));
        }
        headerSent = true;

        return writeQueue.enqueue(HeaderQueueCommand.createHeaders(headers, false))
            .addListener(f -> {
                if (!f.isSuccess()) {
                    reset(Http2Error.INTERNAL_ERROR);
                }
            });
    }

    @Override
    public Future<?> cancelByLocal(TriRpcStatus status) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Cancel stream:%s by local: %s", channel, status));
        }
        return reset(Http2Error.CANCEL);
    }


    @Override
    public ChannelFuture complete(TriRpcStatus status, Map<String, Object> attachments) {
        Http2Headers trailers = getTrailers(status, attachments);
        return sendTrailers(trailers);
    }

    private ChannelFuture sendTrailers(Http2Headers trailers) {
        if (reset) {
            return writeQueue.failure(
                new IllegalStateException("Stream already reset, no more trailers allowed"));
        }
        if (trailersSent) {
            return writeQueue.failure(new IllegalStateException("Trailers already sent"));
        }
        headerSent = true;
        trailersSent = true;
        return writeQueue.enqueue(HeaderQueueCommand.createHeaders(trailers, true))
            .addListener(f -> {
                if (!f.isSuccess()) {
                    reset(Http2Error.INTERNAL_ERROR);
                }
            });
    }

    private Http2Headers getTrailers(TriRpcStatus rpcStatus, Map<String, Object> attachments) {
        DefaultHttp2Headers headers = new DefaultHttp2Headers();
        if (!headerSent) {
            headers.status(HttpResponseStatus.OK.codeAsText());
            headers.set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
        }
        StreamUtils.convertAttachment(headers, attachments);
        headers.set(TripleHeaderEnum.STATUS_KEY.getHeader(), String.valueOf(rpcStatus.code.code));
        if (rpcStatus.isOk()) {
            return headers;
        }
        String grpcMessage = getGrpcMessage(rpcStatus);
        grpcMessage = TriRpcStatus.encodeMessage(TriRpcStatus.limitSizeTo1KB(grpcMessage));
        headers.set(TripleHeaderEnum.MESSAGE_KEY.getHeader(), grpcMessage);
        Status.Builder builder = Status.newBuilder().setCode(rpcStatus.code.code)
            .setMessage(grpcMessage);
        Throwable throwable = rpcStatus.cause;
        if (throwable == null) {
            Status status = builder.build();
            headers.set(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader(),
                StreamUtils.encodeBase64ASCII(status.toByteArray()));
            return headers;
        }
        DebugInfo debugInfo = DebugInfo.newBuilder()
            .addAllStackEntries(ExceptionUtils.getStackFrameList(throwable, 6))
            // can not use now
            // .setDetail(throwable.getMessage())
            .build();
        builder.addDetails(Any.pack(debugInfo));
        Status status = builder.build();
        headers.set(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader(),
            StreamUtils.encodeBase64ASCII(status.toByteArray()));
        return headers;
    }

    private String getGrpcMessage(TriRpcStatus status) {
        if (StringUtils.isNotEmpty(status.description)) {
            return status.description;
        }
        return Optional.ofNullable(status.cause).map(Throwable::getMessage).orElse("unknown");
    }


    @Override
    public ChannelFuture sendMessage(byte[] message, int compressFlag) {
        if (reset) {
            return writeQueue.failure(
                new IllegalStateException("Stream already reset, no more body allowed"));
        }
        if (!headerSent) {
            return writeQueue.failure(
                new IllegalStateException("Headers did not sent before send body"));
        }
        if (trailersSent) {
            return writeQueue.failure(
                new IllegalStateException("Trailers already sent, no more body allowed"));
        }
        return writeQueue.enqueue(DataQueueCommand.createGrpcCommand(message, false, compressFlag));
    }

    /**
     * Error before create server stream, http plain text will be returned
     *
     * @param code   code of error
     * @param status status of error
     */
    private void responsePlainTextError(int code, TriRpcStatus status) {
        Http2Headers headers = new DefaultHttp2Headers(true).status(String.valueOf(code))
            .setInt(TripleHeaderEnum.STATUS_KEY.getHeader(), status.code.code)
            .set(TripleHeaderEnum.MESSAGE_KEY.getHeader(), status.description)
            .set(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader(), TripleConstant.TEXT_PLAIN_UTF8);
        writeQueue.enqueue(HeaderQueueCommand.createHeaders(headers, false));
        writeQueue.enqueue(TextDataQueueCommand.createCommand(status.description, true));
    }

    /**
     * Error in create stream, unsupported config or triple protocol error. There is no return value
     * because stream will be reset if send trailers failed.
     *
     * @param status status of error
     */
    private void responseErr(TriRpcStatus status) {
        Http2Headers trailers = new DefaultHttp2Headers().status(OK.codeAsText())
            .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO)
            .setInt(TripleHeaderEnum.STATUS_KEY.getHeader(), status.code.code)
            .set(TripleHeaderEnum.MESSAGE_KEY.getHeader(), status.toEncodedMessage());
        sendTrailers(trailers);
    }


    private Invoker<?> getInvoker(Http2Headers headers, String serviceName) {
        final String version =
            headers.contains(TripleHeaderEnum.SERVICE_VERSION.getHeader()) ? headers.get(
                TripleHeaderEnum.SERVICE_VERSION.getHeader()).toString() : null;
        final String group =
            headers.contains(TripleHeaderEnum.SERVICE_GROUP.getHeader()) ? headers.get(
                TripleHeaderEnum.SERVICE_GROUP.getHeader()).toString() : null;
        final String key = URL.buildKey(serviceName, group, version);
        Invoker<?> invoker = pathResolver.resolve(key);
        if (invoker == null) {
            invoker = pathResolver.resolve(URL.buildKey(serviceName, group, "1.0.0"));
        }
        if (invoker == null) {
            invoker = pathResolver.resolve(serviceName);
        }
        return invoker;
    }

    public class ServerTransportObserver extends AbstractH2TransportListener implements
        H2TransportListener {

        /**
         * must starts from application/grpc
         */
        private boolean supportContentType(String contentType) {
            if (contentType == null) {
                return false;
            }
            return contentType.startsWith(TripleConstant.APPLICATION_GRPC);
        }

        @Override
        public void onHeader(Http2Headers headers, boolean endStream) {
            executor.execute(() -> processHeader(headers, endStream));
        }

        private void processHeader(Http2Headers headers, boolean endStream) {
            if (!HttpMethod.POST.asciiName().contentEquals(headers.method())) {
                responsePlainTextError(HttpResponseStatus.METHOD_NOT_ALLOWED.code(),
                    TriRpcStatus.INTERNAL.withDescription(
                        String.format("Method '%s' is not supported", headers.method())));
                return;
            }

            if (headers.path() == null) {
                responsePlainTextError(HttpResponseStatus.NOT_FOUND.code(),
                    TriRpcStatus.fromCode(TriRpcStatus.Code.UNIMPLEMENTED.code)
                        .withDescription("Expected path but is missing"));
                return;
            }

            final String path = headers.path().toString();
            if (path.charAt(0) != '/') {
                responsePlainTextError(HttpResponseStatus.NOT_FOUND.code(),
                    TriRpcStatus.fromCode(TriRpcStatus.Code.UNIMPLEMENTED.code)
                        .withDescription(String.format("Expected path to start with /: %s", path)));
                return;
            }

            final CharSequence contentType = HttpUtil.getMimeType(
                headers.get(HttpHeaderNames.CONTENT_TYPE));
            if (contentType == null) {
                responsePlainTextError(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.code(),
                    TriRpcStatus.fromCode(TriRpcStatus.Code.INTERNAL.code)
                        .withDescription("Content-Type is missing from the request"));
                return;
            }

            final String contentString = contentType.toString();
            if (!supportContentType(contentString)) {
                responsePlainTextError(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.code(),
                    TriRpcStatus.fromCode(TriRpcStatus.Code.INTERNAL.code)
                        .withDescription(
                            String.format("Content-Type '%s' is not supported", contentString)));
                return;
            }

            String[] parts = path.split("/");
            if (parts.length != 3) {
                responseErr(TriRpcStatus.UNIMPLEMENTED.withDescription("Bad path format:" + path));
                return;
            }
            String serviceName = parts[1];
            String originalMethodName = parts[2];

            Invoker<?> invoker = getInvoker(headers, serviceName);
            if (invoker == null) {
                responseErr(
                    TriRpcStatus.UNIMPLEMENTED.withDescription("Service not found:" + serviceName));
                return;
            }

            if (endStream) {
                return;
            }

            DeCompressor deCompressor = DeCompressor.NONE;
            CharSequence messageEncoding = headers.get(TripleHeaderEnum.GRPC_ENCODING.getHeader());
            if (null != messageEncoding) {
                String compressorStr = messageEncoding.toString();
                if (!Identity.MESSAGE_ENCODING.equals(compressorStr)) {
                    DeCompressor compressor = DeCompressor.getCompressor(frameworkModel,
                        compressorStr);
                    if (null == compressor) {
                        responseErr(TriRpcStatus.fromCode(TriRpcStatus.Code.UNIMPLEMENTED.code)
                            .withDescription(String.format("Grpc-encoding '%s' is not supported",
                                compressorStr)));
                        return;
                    }
                    deCompressor = compressor;
                }
            }

            try {
                final TriDecoder.Listener listener = new ServerDecoderListener();
                deframer = new TriDecoder(deCompressor, listener);
            } catch (Throwable t) {
                responseErr(TriRpcStatus.INTERNAL.withCause(t));
                return;
            }

            Map<String, Object> requestMetadata = headersToMap(headers);
            boolean hasStub = pathResolver.hasNativeStub(path);
            if (hasStub) {
                listener = new StubAbstractServerCall(invoker, TripleServerStream.this,
                    frameworkModel,
                    acceptEncoding, serviceName, originalMethodName, executor);
            } else {
                listener = new ReflectionAbstractServerCall(invoker, TripleServerStream.this,
                    frameworkModel, acceptEncoding, serviceName, originalMethodName, filters,
                    executor);
            }
            listener.onHeader(requestMetadata);
            if (listener == null) {
                deframer.close();
            }
        }


        @Override
        public void onData(ByteBuf data, boolean endStream) {
            executor.execute(() -> doOnData(data, endStream));
        }

        private void doOnData(ByteBuf data, boolean endStream) {
            if (deframer == null) {
                return;
            }
            deframer.deframe(data);
            if (endStream) {
                deframer.close();
            }
        }

        @Override
        public void cancelByRemote(long errorCode) {
            TripleServerStream.this.reset = true;
            if (!trailersSent) {
                // send rst if stream not closed
                reset(Http2Error.valueOf(errorCode));
            }
            if (listener == null) {
                return;
            }
            executor.execute(() -> {
                listener.onCancelByRemote(TriRpcStatus.CANCELLED
                    .withDescription("Canceled by client ,errorCode=" + errorCode));
            });
        }

        private class ServerDecoderListener implements TriDecoder.Listener {

            @Override
            public void onRawMessage(byte[] data) {
                listener.onMessage(data);
            }

            @Override
            public void close() {
                if (listener != null) {
                    listener.onComplete();
                }
            }
        }
    }

}
