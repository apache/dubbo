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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.PathResolver;
import org.apache.dubbo.rpc.protocol.tri.RpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.call.ServerCall;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.TextDataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.frame.TriDecoder;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;
import org.apache.dubbo.rpc.protocol.tri.transport.AbstractH2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.H2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.WriteQueue;

import com.google.protobuf.Any;
import com.google.rpc.DebugInfo;
import com.google.rpc.Status;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class ServerStream extends AbstractStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerStream.class);
    public final ServerTransportObserver transportObserver = new ServerTransportObserver();
    private final WriteQueue writeQueue;
    private final PathResolver pathResolver;
    private final List<HeaderFilter> filters;
    private final GenericUnpack genericUnpack;
    private final FrameworkModel frameworkModel;
    private final EventLoop eventLoop;
    private boolean headerSent;
    private boolean trailersSent;
    private ServerStreamListener listener;
    private boolean closed;
    private final String defaultSerialization;
    private TriDecoder decoder;

    public ServerStream(Channel channel,
                        FrameworkModel frameworkModel,
                        Executor executor,
                        String defaultSerialization, PathResolver pathResolver,
                        List<HeaderFilter> filters,
                        GenericUnpack genericUnpack) {
        super(executor);
        this.eventLoop = channel.eventLoop();
        this.pathResolver = pathResolver;
        this.filters = filters;
        this.frameworkModel = frameworkModel;
        this.defaultSerialization = defaultSerialization;
        this.genericUnpack = genericUnpack;
        this.writeQueue = new WriteQueue(channel);
    }


    private String getGrpcMessage(RpcStatus status) {
        if (StringUtils.isNotEmpty(status.description)) {
            return status.description;
        }
        return Optional.ofNullable(status.cause)
                .map(Throwable::getMessage)
                .orElse("unknown");
    }

    public void sendHeader(Http2Headers headers) {
        if (headerSent && trailersSent) {
            // todo handle this state
            return;
        }
        if (headerSent) {
            trailersSent = true;
            writeQueue.enqueue(HeaderQueueCommand.createHeaders(headers, true));
        } else {
            headerSent = true;
            writeQueue.enqueue(HeaderQueueCommand.createHeaders(headers, false));
        }
    }

    public void sendHeaderWithEos(Http2Headers headers) {
        headerSent = true;
        sendHeader(headers);
    }

    public void close(RpcStatus status, Map<String, Object> attachments) {
        if (closed) {
            return;
        }
        closed = true;
        final Http2Headers headers = getTrailers(status, attachments);
        sendHeaderWithEos(headers);
    }

    private Http2Headers getTrailers(RpcStatus rpcStatus, Map<String, Object> attachments) {
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
        grpcMessage = RpcStatus.encodeMessage(grpcMessage);
        headers.set(TripleHeaderEnum.MESSAGE_KEY.getHeader(), grpcMessage);
        Status.Builder builder = Status.newBuilder()
                .setCode(rpcStatus.code.code)
                .setMessage(grpcMessage);
        Throwable throwable = rpcStatus.cause;
        if (throwable == null) {
            Status status = builder.build();
            headers.set(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader(),
                    H2TransportListener.encodeBase64ASCII(status.toByteArray()));
            return headers;
        }
        DebugInfo debugInfo = DebugInfo.newBuilder()
                .addAllStackEntries(ExceptionUtils.getStackFrameList(throwable, 10))
                // can not use now
                // .setDetail(throwable.getMessage())
                .build();
        builder.addDetails(Any.pack(debugInfo));
        Status status = builder.build();
        headers.set(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader(),
                H2TransportListener.encodeBase64ASCII(status.toByteArray()));
        return headers;
    }

    @Override
    public void writeMessage(byte[] message, int compressed) {
        writeQueue.enqueue(DataQueueCommand.createGrpcCommand(message, false, compressed));
    }

    @Override
    public void requestN(int n) {
        decoder.request(n);
    }

    /**
     * Error before create server stream, http plain text will be returned
     *
     * @param code
     * @param status
     */
    private void responsePlainTextError(int code, RpcStatus status) {
        Http2Headers headers = new DefaultHttp2Headers(true)
                .status(String.valueOf(code))
                .setInt(TripleHeaderEnum.STATUS_KEY.getHeader(), status.code.code)
                .set(TripleHeaderEnum.MESSAGE_KEY.getHeader(), status.description)
                .set(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader(), TripleConstant.TEXT_PLAIN_UTF8);
        writeQueue.enqueue(HeaderQueueCommand.createHeaders(headers, false));
        writeQueue.enqueue(TextDataQueueCommand.createCommand(status.description, true));
    }

    /**
     * Error in create stream, unsupported config or triple protocol error.
     *
     * @param status
     */
    private void responseErr(RpcStatus status) {
        Http2Headers trailers = new DefaultHttp2Headers()
                .status(OK.codeAsText())
                .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO)
                .setInt(TripleHeaderEnum.STATUS_KEY.getHeader(), status.code.code)
                .set(TripleHeaderEnum.MESSAGE_KEY.getHeader(), status.toEncodedMessage());
        writeQueue.enqueue(HeaderQueueCommand.createHeaders(trailers, true));
    }

    @Override
    EventLoop getEventLoop() {
        return eventLoop;
    }

    public class ServerTransportObserver extends AbstractH2TransportListener implements H2TransportListener {


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
            executor.execute(() -> {
                if (!HttpMethod.POST.asciiName().contentEquals(headers.method())) {
                    responsePlainTextError(HttpResponseStatus.METHOD_NOT_ALLOWED.code(),
                            RpcStatus.INTERNAL
                                    .withDescription(String.format("Method '%s' is not supported", headers.method())));
                    return;
                }

                if (headers.path() == null) {
                    responsePlainTextError(HttpResponseStatus.NOT_FOUND.code(),
                            RpcStatus.fromCode(RpcStatus.Code.UNIMPLEMENTED.code).withDescription("Expected path but is missing"));
                    return;
                }

                final String path = headers.path().toString();
                if (path.charAt(0) != '/') {
                    responsePlainTextError(HttpResponseStatus.NOT_FOUND.code(),
                            RpcStatus.fromCode(RpcStatus.Code.UNIMPLEMENTED.code)
                                    .withDescription(String.format("Expected path to start with /: %s", path)));
                    return;
                }

                final CharSequence contentType = HttpUtil.getMimeType(headers.get(HttpHeaderNames.CONTENT_TYPE));
                if (contentType == null) {
                    responsePlainTextError(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.code(),
                            RpcStatus.fromCode(RpcStatus.Code.INTERNAL.code)
                                    .withDescription("Content-Type is missing from the request"));
                    return;
                }

                final String contentString = contentType.toString();
                if (!supportContentType(contentString)) {
                    responsePlainTextError(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.code(),
                            RpcStatus.fromCode(RpcStatus.Code.INTERNAL.code)
                                    .withDescription(String.format("Content-Type '%s' is not supported", contentString)));
                    return;
                }

                if (path.charAt(0) != '/') {
                    responseErr(RpcStatus.UNIMPLEMENTED
                            .withDescription("Path must start with '/'. Request path: " + path));
                    return;
                }

                String[] parts = path.split("/");
                if (parts.length != 3) {
                    responseErr(RpcStatus.UNIMPLEMENTED
                            .withDescription("Bad path format:" + path));
                    return;
                }
                String serviceName = parts[1];
                String originalMethodName = parts[2];
                String methodName = Character.toLowerCase(originalMethodName.charAt(0)) + originalMethodName.substring(1);

                DeCompressor deCompressor = DeCompressor.NONE;
                CharSequence messageEncoding = headers.get(TripleHeaderEnum.GRPC_ENCODING.getHeader());
                if (null != messageEncoding) {
                    String compressorStr = messageEncoding.toString();
                    if (!Identity.MESSAGE_ENCODING.equals(compressorStr)) {
                        DeCompressor compressor = DeCompressor.getCompressor(frameworkModel, compressorStr);
                        if (null == compressor) {
                            responseErr(RpcStatus.fromCode(RpcStatus.Code.UNIMPLEMENTED.code)
                                    .withDescription(String.format("Grpc-encoding '%s' is not supported", compressorStr)));
                            return;
                        }
                        deCompressor = compressor;
                    }
                }

                try {
                    final TriDecoder.Listener listener = new ServerDecoderListener();
                    ServerStream.this.decoder = new TriDecoder(deCompressor, listener);
                } catch (Throwable t) {
                    close(RpcStatus.INTERNAL
                            .withCause(t), null);
                }
                ServerCall call = new ServerCall(ServerStream.this, frameworkModel,
                        serviceName,
                        methodName,
                        defaultSerialization,
                        executor,
                        filters,
                        genericUnpack,
                        pathResolver);
                ServerStream.this.listener = call.streamListener;
                listener.onHeaders(headersToMap(headers));
                if (endStream) {
                    decoder.close();
                }
            });
        }


        @Override
        public void onData(ByteBuf data, boolean endStream) {
            executor.execute(() -> {
                decoder.deframe(data);
                if (endStream) {
                    decoder.close();
                }
            });
        }


        @Override
        public void cancelByRemote(RpcStatus status) {
            executor.execute(() -> {
                listener.cancel(status);
            });
        }


        private class ServerDecoderListener implements TriDecoder.Listener {

            @Override
            public void onRawMessage(byte[] data) {
                ServerStream.this.listener.onMessage(data);
            }

            @Override
            public void close() {
                ServerStream.this.listener.complete();
            }
        }
    }
}
