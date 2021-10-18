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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus.Code;
import org.apache.dubbo.rpc.service.ServiceDescriptorInternalCache;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2ResetFrame;
import io.netty.util.ReferenceCountUtil;

import java.util.List;

import static org.apache.dubbo.rpc.protocol.tri.Compressor.DEFAULT_COMPRESSOR;

public class TripleHttp2FrameServerHandler extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripleHttp2FrameServerHandler.class);
    private final PathResolver PATH_RESOLVER;
    private final FrameworkModel frameworkModel;

    public TripleHttp2FrameServerHandler(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        this.PATH_RESOLVER = frameworkModel.getExtensionLoader(PathResolver.class).getDefaultExtension();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            onHeadersRead(ctx, (Http2HeadersFrame) msg);
        } else if (msg instanceof Http2DataFrame) {
            onDataRead(ctx, (Http2DataFrame) msg);
        } else if (msg instanceof Http2Frame) {
            // ignored
            ReferenceCountUtil.release(msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof Http2ResetFrame) {
            onResetRead(ctx, (Http2ResetFrame) evt);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    public void onResetRead(ChannelHandlerContext ctx, Http2ResetFrame frame) {
        Http2Error http2Error = Http2Error.valueOf(frame.errorCode());
        final AbstractServerStream serverStream = ctx.channel().attr(TripleConstant.SERVER_STREAM_KEY).get();
        serverStream.cancelByRemote(http2Error);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Exception in processing triple message", cause);
        }
        if (cause instanceof RpcException) {
            TripleUtil.responseErr(ctx, GrpcStatus.rpcExceptionCodeToGrpc(((RpcException) cause).getCode()));
        } else {
            TripleUtil.responseErr(ctx, GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                .withDescription("Provider's error:\n" + cause.getMessage()));
        }
    }

    public void onDataRead(ChannelHandlerContext ctx, Http2DataFrame msg) throws Exception {
        super.channelRead(ctx, msg.content());

        if (msg.isEndStream()) {
            final AbstractServerStream serverStream = ctx.channel().attr(TripleConstant.SERVER_STREAM_KEY).get();
            if (serverStream != null) {
                serverStream.asTransportObserver().onComplete();
            }
        }
    }

    private Invoker<?> getInvoker(Http2Headers headers, String serviceName) {
        final String version = headers.contains(TripleHeaderEnum.SERVICE_VERSION.getHeader()) ? headers.get(
            TripleHeaderEnum.SERVICE_VERSION.getHeader()).toString() : null;
        final String group = headers.contains(TripleHeaderEnum.SERVICE_GROUP.getHeader()) ? headers.get(TripleHeaderEnum.SERVICE_GROUP.getHeader())
            .toString() : null;
        final String key = URL.buildKey(serviceName, group, version);
        Invoker<?> invoker = PATH_RESOLVER.resolve(key);
        if (invoker == null) {
            invoker = PATH_RESOLVER.resolve(serviceName);
        }
        return invoker;
    }

    public void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame msg) throws Exception {
        final Http2Headers headers = msg.headers();

        if (!HttpMethod.POST.asciiName().contentEquals(headers.method())) {
            TripleUtil.responsePlainTextError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED.code(),
                GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription(String.format("Method '%s' is not supported", headers.method())));
            return;
        }

        if (headers.path() == null) {
            TripleUtil.responsePlainTextError(ctx, HttpResponseStatus.NOT_FOUND.code(),
                GrpcStatus.fromCode(Code.UNIMPLEMENTED.code).withDescription("Expected path but is missing"));
            return;
        }

        final String path = headers.path().toString();
        if (path.charAt(0) != '/') {
            TripleUtil.responsePlainTextError(ctx, HttpResponseStatus.NOT_FOUND.code(),
                GrpcStatus.fromCode(Code.UNIMPLEMENTED.code)
                    .withDescription(String.format("Expected path to start with /: %s", path)));
            return;
        }

        final CharSequence contentType = HttpUtil.getMimeType(headers.get(HttpHeaderNames.CONTENT_TYPE));
        if (contentType == null) {
            TripleUtil.responsePlainTextError(ctx, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.code(),
                GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL.code)
                    .withDescription("Content-Type is missing from the request"));
            return;
        }

        final String contentString = contentType.toString();
        if (!TripleUtil.supportContentType(contentString)) {
            TripleUtil.responsePlainTextError(ctx, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.code(),
                GrpcStatus.fromCode(Code.INTERNAL.code)
                    .withDescription(String.format("Content-Type '%s' is not supported", contentString)));
            return;
        }

        String[] parts = path.split("/");
        if (parts.length != 3) {
            TripleUtil.responseErr(ctx, GrpcStatus.fromCode(Code.UNIMPLEMENTED).withDescription("Bad path format:" + path));
            return;
        }
        String serviceName = parts[1];
        String originalMethodName = parts[2];
        String methodName = Character.toLowerCase(originalMethodName.charAt(0)) + originalMethodName.substring(1);

        final Invoker<?> invoker = getInvoker(headers, serviceName);
        if (invoker == null) {
            TripleUtil.responseErr(ctx,
                GrpcStatus.fromCode(Code.UNIMPLEMENTED).withDescription("Service not found:" + serviceName));
            return;
        }
        FrameworkServiceRepository repo = frameworkModel.getServiceRepository();
        ProviderModel providerModel = repo.lookupExportedService(invoker.getUrl().getServiceKey());
        if (providerModel == null || providerModel.getServiceModel() == null) {
            TripleUtil.responseErr(ctx,
                GrpcStatus.fromCode(Code.UNIMPLEMENTED).withDescription("Service not found:" + serviceName));
            return;
        }

        MethodDescriptor methodDescriptor = null;
        List<MethodDescriptor> methodDescriptors = null;

        if (isGeneric(methodName)) {
            // There should be one and only one
            methodDescriptor = ServiceDescriptorInternalCache.genericService().getMethods(methodName).get(0);
        } else if (isEcho(methodName)) {
            // There should be one and only one
            methodDescriptor = ServiceDescriptorInternalCache.echoService().getMethods(methodName).get(0);
        } else {
            methodDescriptors = providerModel.getServiceModel().getMethods(methodName);
            if (CollectionUtils.isEmpty(methodDescriptors)) {
                TripleUtil.responseErr(ctx, GrpcStatus.fromCode(Code.UNIMPLEMENTED)
                    .withDescription("Method :" + methodName + " not found of service:" + serviceName));
                return;
            }
            // In most cases there is only one method
            if (methodDescriptors.size() == 1) {
                methodDescriptor = methodDescriptors.get(0);
            }
        }
        boolean isUnary = methodDescriptor != null && methodDescriptor.isUnary();
        final AbstractServerStream stream = AbstractServerStream.newServerStream(invoker.getUrl(), isUnary);

        Channel channel = ctx.channel();
        // You can add listeners to ChannelPromise here if you want to listen for the result of sending a frame
        stream.service(providerModel.getServiceModel())
            .invoker(invoker)
            .methodName(methodName)
            .subscribe(new ServerTransportObserver(ctx));
        if (methodDescriptor != null) {
            stream.method(methodDescriptor);
        } else {
            // Then you need to find the corresponding parameter according to the request body
            stream.methods(methodDescriptors);
        }
        CharSequence messageEncoding = headers.get(TripleHeaderEnum.GRPC_ENCODING.getHeader());
        if (null != messageEncoding) {
            String compressorStr = messageEncoding.toString();
            if (!DEFAULT_COMPRESSOR.equals(compressorStr)) {
                Compressor compressor = Compressor.getCompressor(frameworkModel, compressorStr);
                if (null == compressor) {
                    TripleUtil.responsePlainTextError(ctx, HttpResponseStatus.NOT_FOUND.code(),
                        GrpcStatus.fromCode(Code.UNIMPLEMENTED.code)
                            .withDescription(String.format("Grpc-encoding '%s' is not supported", compressorStr)));
                } else {
                    stream.setDeCompressor(compressor);
                }
            }
        }
        final TransportObserver observer = stream.asTransportObserver();
        observer.onMetadata(new Http2HeaderMeta(headers), false);
        if (msg.isEndStream()) {
            observer.onComplete();
        }
        channel.attr(TripleConstant.SERVER_STREAM_KEY).set(stream);
    }


    private boolean isEcho(String methodName) {
        return CommonConstants.$ECHO.equals(methodName);
    }

    private boolean isGeneric(String methodName) {
        return CommonConstants.$INVOKE.equals(methodName) || CommonConstants.$INVOKE_ASYNC.equals(methodName);
    }

}
