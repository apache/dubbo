package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;

import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;
import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responsePlainTextError;

public class TripleHttp2ServerHandler extends ChannelDuplexHandler {
    private static final InvokerResolver invokerResolver = ExtensionLoader.getExtensionLoader(InvokerResolver.class).getDefaultExtension();
    private final GrpcDataDecoder decoder = new GrpcDataDecoder();
    private UnaryInvoker invoker;

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

    public void onDataRead(ChannelHandlerContext ctx, Http2DataFrame msg) throws Exception {
        List<Object> out = new ArrayList<>();
        decoder.decode(ctx, msg.content(), out);
        // todo support stream
        if (out.isEmpty()) {
            return;
        } else {
            for (Object o : out) {
                invoker.receiveData((ByteBuf) o);
            }
        }
        if (msg.isEndStream()) {
            try {
                this.invoker.halfClose();
            }catch (Throwable t){
                responseErr(ctx,GrpcStatus.UNKNOWN,t.getMessage());
            }
        }
    }

    private Invoker<?> getInvoker(Http2Headers headers,String serviceName) {
        final String version = headers.contains(TripleConstant.VERSION_KEY) ? headers.get(TripleConstant.VERSION_KEY).toString() : null;
        final String group = headers.contains(TripleConstant.GROUP_KEY) ? headers.get(TripleConstant.GROUP_KEY).toString() : null;
        final String key = URL.buildKey(serviceName, group, version);
        Invoker<?> invoker = invokerResolver.resolve(key);
        if (invoker == null) {
            invoker = invokerResolver.resolve(serviceName);
        }
        return invoker;
    }

    public void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame msg) {
        final Http2Headers headers = msg.headers();

        if (!HttpMethod.POST.asciiName().contentEquals(headers.method())) {
            responsePlainTextError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED.code(), GrpcStatus.INTERNAL.code,
                    String.format("Method '%s' is not supported", headers.method()));
            return;
        }

        if (headers.path() == null) {
            responsePlainTextError(ctx, HttpResponseStatus.NOT_FOUND.code(), GrpcStatus.UNIMPLEMENTED.code, "Expected path but is missing");
            return;
        }

        final String path = headers.path().toString();
        if (path.charAt(0) != '/') {
            responsePlainTextError(ctx, HttpResponseStatus.NOT_FOUND.code(), GrpcStatus.UNIMPLEMENTED.code,
                    String.format("Expected path to start with /: %s", path));
            return;
        }

        final CharSequence contentType = HttpUtil.getMimeType(headers.get(HttpHeaderNames.CONTENT_TYPE));
        if (contentType == null) {
            responsePlainTextError(ctx, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.code(), GrpcStatus.INTERNAL.code,
                    "Content-Type is missing from the request");
            return;
        }

        final String contentString = contentType.toString();
        if (!TripleUtil.supportContentType(contentString)) {
            responsePlainTextError(ctx, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.code(), GrpcStatus.INTERNAL.code,
                    String.format("Content-Type '%s' is not supported", contentString));
            return;
        }

        String[] parts = path.split("/");
        String serviceName = parts[1];
        String originalMethodName = parts[2];

        String methodName = Character.toLowerCase(originalMethodName.charAt(0)) + originalMethodName.substring(1);

        final Invoker<?> delegateInvoker = getInvoker(headers,serviceName);
        if (delegateInvoker == null) {
            responseErr(ctx, GrpcStatus.UNIMPLEMENTED, "Service not found:" + serviceName);
            return;
        }

        ServiceRepository repo = ApplicationModel.getServiceRepository();
        MethodDescriptor methodDescriptor = repo.lookupMethod(serviceName, methodName);
        if (methodDescriptor == null) {
            responseErr(ctx, GrpcStatus.UNIMPLEMENTED, "Method not found:" + methodName + " of service:" + serviceName);
            return;
        }
        this.invoker = new UnaryInvoker(delegateInvoker, methodDescriptor, ctx, headers, serviceName, methodName);
        if (msg.isEndStream()) {
            this.invoker.halfClose();
        }
    }
}
