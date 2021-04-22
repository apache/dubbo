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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus.Code;
import org.apache.dubbo.rpc.service.EchoService;
import org.apache.dubbo.rpc.service.GenericService;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.util.ReferenceCountUtil;

import java.util.List;

import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;
import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responsePlainTextError;

public class TripleHttp2FrameServerHandler extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripleHttp2FrameServerHandler.class);
    private static final PathResolver PATH_RESOLVER = ExtensionLoader.getExtensionLoader(PathResolver.class)
            .getDefaultExtension();


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
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Exception in processing triple message", cause);
        }
        if (cause instanceof TripleRpcException) {
            TripleUtil.responseErr(ctx, ((TripleRpcException) cause).getStatus());
        } else {
            TripleUtil.responseErr(ctx, GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription("Provider's error:\n" + cause.getMessage()));
        }
    }

    public void onDataRead(ChannelHandlerContext ctx, Http2DataFrame msg) throws Exception {
        super.channelRead(ctx, msg.content());

        if (msg.isEndStream()) {
            final AbstractServerStream serverStream = TripleUtil.getServerStream(ctx);
            if (serverStream != null) {
                serverStream.asTransportObserver().tryOnComplete();
            }
        }
    }

    private Invoker<?> getInvoker(Http2Headers headers, String serviceName) {
        final String version = headers.contains(TripleConstant.SERVICE_VERSION) ? headers.get(
                TripleConstant.SERVICE_VERSION).toString() : null;
        final String group = headers.contains(TripleConstant.SERVICE_GROUP) ? headers.get(TripleConstant.SERVICE_GROUP)
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
            responsePlainTextError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED.code(),
                    GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                            .withDescription(String.format("Method '%s' is not supported", headers.method())));
            return;
        }

        if (headers.path() == null) {
            responsePlainTextError(ctx, HttpResponseStatus.NOT_FOUND.code(),
                    GrpcStatus.fromCode(Code.UNIMPLEMENTED.code).withDescription("Expected path but is missing"));
            return;
        }

        final String path = headers.path().toString();
        if (path.charAt(0) != '/') {
            responsePlainTextError(ctx, HttpResponseStatus.NOT_FOUND.code(),
                    GrpcStatus.fromCode(Code.UNIMPLEMENTED.code)
                            .withDescription(String.format("Expected path to start with /: %s", path)));
            return;
        }

        final CharSequence contentType = HttpUtil.getMimeType(headers.get(HttpHeaderNames.CONTENT_TYPE));
        if (contentType == null) {
            responsePlainTextError(ctx, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.code(),
                    GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL.code)
                            .withDescription("Content-Type is missing from the request"));
            return;
        }

        final String contentString = contentType.toString();
        if (!TripleUtil.supportContentType(contentString)) {
            responsePlainTextError(ctx, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.code(),
                    GrpcStatus.fromCode(Code.INTERNAL.code)
                            .withDescription(String.format("Content-Type '%s' is not supported", contentString)));
            return;
        }

        String[] parts = path.split("/");
        if (parts.length != 3) {
            responseErr(ctx, GrpcStatus.fromCode(Code.UNIMPLEMENTED).withDescription("Bad path format:" + path));
            return;
        }
        String serviceName = parts[1];
        String originalMethodName = parts[2];
        String methodName = Character.toLowerCase(originalMethodName.charAt(0)) + originalMethodName.substring(1);

        final Invoker<?> invoker = getInvoker(headers, serviceName);
        if (invoker == null) {
            responseErr(ctx,
                    GrpcStatus.fromCode(Code.UNIMPLEMENTED).withDescription("Service not found:" + serviceName));
            return;
        }
        ServiceRepository repo = ApplicationModel.getServiceRepository();
        final ServiceDescriptor serviceDescriptor = repo.lookupService(invoker.getUrl().getServiceKey());
        if (serviceDescriptor == null) {
            responseErr(ctx,
                    GrpcStatus.fromCode(Code.UNIMPLEMENTED).withDescription("Service not found:" + serviceName));
            return;
        }

        MethodDescriptor methodDescriptor = null;
        List<MethodDescriptor> methodDescriptors = null;

        if (CommonConstants.$INVOKE.equals(methodName) || CommonConstants.$INVOKE_ASYNC.equals(methodName)) {
            methodDescriptor = repo.lookupMethod(GenericService.class.getName(), methodName);
        } else if (CommonConstants.$ECHO.equals(methodName)) {
            methodDescriptor = repo.lookupMethod(EchoService.class.getName(), methodName);
        } else {
            methodDescriptors = serviceDescriptor.getMethods(methodName);
            if (methodDescriptors == null || methodDescriptors.isEmpty()) {
                responseErr(ctx, GrpcStatus.fromCode(Code.UNIMPLEMENTED)
                        .withDescription("Method :" + methodName + " not found of service:" + serviceName));
                return;
            }
            if (methodDescriptors.size() == 1) {
                methodDescriptor = methodDescriptors.get(0);
            }
        }
        final AbstractServerStream stream;
        if (methodDescriptor != null && methodDescriptor.isStream()) {
            stream = AbstractServerStream.stream(invoker.getUrl());
        } else {
            stream = AbstractServerStream.unary(invoker.getUrl());
        }
        stream.service(serviceDescriptor)
                .invoker(invoker)
                .methodName(methodName)
                .subscribe(new ServerTransportObserver(ctx));
        if (methodDescriptor != null) {
            stream.method(methodDescriptor);
        } else {
            stream.methods(methodDescriptors);
        }
        final TransportObserver observer = stream.asTransportObserver();
        observer.tryOnMetadata(new Http2HeaderMeta(headers), false);
        if (msg.isEndStream()) {
            observer.tryOnComplete();
        }

        ctx.channel().attr(TripleUtil.SERVER_STREAM_KEY).set(stream);
    }
}
