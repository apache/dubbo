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
import org.apache.dubbo.rpc.service.GenericService;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;

import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;
import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responsePlainTextError;

public class TripleHttp2FrameServerHandler extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripleHttp2FrameServerHandler.class);
    private static final PathResolver pathResolver = ExtensionLoader.getExtensionLoader(PathResolver.class).getDefaultExtension();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            onHeadersRead(ctx, (Http2HeadersFrame) msg);
        } else if (msg instanceof Http2DataFrame) {
            onDataRead(ctx, (Http2DataFrame) msg);
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
            final ServerStream serverStream = TripleUtil.getServerStream(ctx);
            // stream already closed;
            if (serverStream != null) {
                serverStream.halfClose();
            }
        }
    }

    private Invoker<?> getInvoker(Http2Headers headers, String serviceName) {
        final String version = headers.contains(TripleConstant.VERSION_KEY) ? headers.get(TripleConstant.VERSION_KEY).toString() : null;
        final String group = headers.contains(TripleConstant.GROUP_KEY) ? headers.get(TripleConstant.GROUP_KEY).toString() : null;
        final String key = URL.buildKey(serviceName, group, version);
        Invoker<?> invoker = pathResolver.resolve(key);
        if (invoker == null) {
            invoker = pathResolver.resolve(serviceName);
        }
        return invoker;
    }

    public void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame msg) throws Exception {
        final Http2Headers headers = msg.headers();

        if (!HttpMethod.POST.asciiName().contentEquals(headers.method())) {
            responsePlainTextError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED.code(), GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription(String.format("Method '%s' is not supported", headers.method())));
            return;
        }

        if (headers.path() == null) {
            responsePlainTextError(ctx, HttpResponseStatus.NOT_FOUND.code(), GrpcStatus.fromCode(Code.UNIMPLEMENTED.code).withDescription("Expected path but is missing"));
            return;
        }

        final String path = headers.path().toString();
        if (path.charAt(0) != '/') {
            responsePlainTextError(ctx, HttpResponseStatus.NOT_FOUND.code(), GrpcStatus.fromCode(Code.UNIMPLEMENTED.code)
                    .withDescription(String.format("Expected path to start with /: %s", path)));
            return;
        }

        final CharSequence contentType = HttpUtil.getMimeType(headers.get(HttpHeaderNames.CONTENT_TYPE));
        if (contentType == null) {
            responsePlainTextError(ctx, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.code(), GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL.code)
                    .withDescription("Content-Type is missing from the request"));
            return;
        }

        final String contentString = contentType.toString();
        if (!TripleUtil.supportContentType(contentString)) {
            responsePlainTextError(ctx, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.code(), GrpcStatus.fromCode(Code.INTERNAL.code)
                    .withDescription(String.format("Content-Type '%s' is not supported", contentString)));
            return;
        }

        String[] parts = path.split("/");
        String serviceName = parts[1];
        String originalMethodName = parts[2];
        String methodName = Character.toLowerCase(originalMethodName.charAt(0)) + originalMethodName.substring(1);

        final Invoker<?> delegateInvoker = getInvoker(headers, serviceName);
        if (delegateInvoker == null) {
            responseErr(ctx, GrpcStatus.fromCode(Code.UNIMPLEMENTED).withDescription("Service not found:" + serviceName));
            return;
        }
        ServiceRepository repo = ApplicationModel.getServiceRepository();
        final ServiceDescriptor descriptor = repo.lookupService(delegateInvoker.getUrl().getServiceKey());
        if (descriptor == null) {
            responseErr(ctx, GrpcStatus.fromCode(Code.UNIMPLEMENTED).withDescription("Service not found:" + serviceName));
            return;
        }

        MethodDescriptor methodDescriptor;
        if (CommonConstants.$INVOKE.equals(methodName) || CommonConstants.$INVOKE_ASYNC.equals(methodName)) {
            methodDescriptor = repo.lookupMethod(GenericService.class.getName(), methodName);
        } else {
            methodDescriptor = repo.lookupMethod(serviceName, methodName);
        }
        if (methodDescriptor == null) {
            responseErr(ctx, GrpcStatus.fromCode(Code.UNIMPLEMENTED).withDescription("Method not found:" + methodName + " of service:" + serviceName));
            return;
        }
        final ServerStream serverStream = new ServerStream(delegateInvoker, descriptor, methodDescriptor, ctx);
        serverStream.onHeaders(headers);
        ctx.channel().attr(TripleUtil.SERVER_STREAM_KEY).set(serverStream);
        if (msg.isEndStream()) {
            serverStream.halfClose();
        }
    }
}
