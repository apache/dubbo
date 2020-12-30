package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.Serialization2;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;

import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class TripleHttp2ServerHandler extends ChannelDuplexHandler {
    private static final InvokerResolver invokerResolver = ExtensionLoader.getExtensionLoader(InvokerResolver.class).getDefaultExtension();
    private final GrpcDecoder decoder = new GrpcDecoder();
    private final Serialization2 serialization = ExtensionLoader.getExtensionLoader(Serialization2.class).getExtension("protobuf");
    private Http2Headers headers;

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
        }
        final String path = headers.path().toString();
        String[] parts = path.split("/");
        String serviceName = parts[1];
        String originalMethodName = parts[2];

        String methodName = Character.toLowerCase(originalMethodName.charAt(0)) + originalMethodName.substring(1);

        Invoker<?> invoker = getInvoker(serviceName);
        if (invoker == null) {
            responseErr(ctx, GrpcStatus.UNIMPLEMENTED, "Service not found:" + serviceName);
            return;
        }

        ServiceRepository repo = ApplicationModel.getServiceRepository();
        MethodDescriptor methodDescriptor = repo.lookupMethod(serviceName, methodName);
        if (methodDescriptor == null) {
            responseErr(ctx, GrpcStatus.UNIMPLEMENTED, "Method not found:" + methodName + " of service:" + serviceName);
            return;
        }

        Invocation invocation = buildInvocation(out, serviceName, methodName, methodDescriptor);

        final Result result = invoker.invoke(invocation);
        CompletionStage<Object> future = result.thenApply(Function.identity());

        future.whenComplete((appResult, t) -> {
            try {
                if (t != null) {
                    responseErr(ctx, GrpcStatus.UNKNOWN, t.getMessage());
                    return;
                }
                AppResponse response = (AppResponse) appResult;
                if (!response.hasException()) {
                    Http2Headers http2Headers = new DefaultHttp2Headers()
                            .status(OK.codeAsText())
                            .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
                    ctx.write(new DefaultHttp2HeadersFrame(http2Headers));
                    final ByteBuf buf = ctx.alloc().buffer();
                    final ByteBufOutputStream bos = new ByteBufOutputStream(buf);
                    bos.write(0);
                    bos.writeInt(((MessageLite) (response.getValue())).getSerializedSize());
                    serialization.serialize(response.getValue(), bos);
                    ctx.write(new DefaultHttp2DataFrame(buf));
                    final Http2Headers trailers = new DefaultHttp2Headers()
                            .setInt(TripleConstant.STATUS_KEY, GrpcStatus.OK.code);
                    ctx.write(new DefaultHttp2HeadersFrame(trailers, true));
                }
            } catch (Exception e) {
                // TODO add log here
                responseErr(ctx, GrpcStatus.UNKNOWN, e.getMessage());
            }
        });
    }

    private Invocation buildInvocation(List<Object> out, String serviceName, String methodName, MethodDescriptor methodDescriptor) throws IOException {
        RpcInvocation inv = new RpcInvocation();
        final Object req = serialization.deserialize(new ByteBufInputStream((ByteBuf) out.get(0)), methodDescriptor.getParameterClasses()[0]);
        inv.setMethodName(methodName);
        inv.setServiceName(serviceName);
        inv.setTargetServiceUniqueName(serviceName);
        inv.setParameterTypes(methodDescriptor.getParameterClasses());
        inv.setArguments(new Object[]{req});
        inv.setReturnTypes(methodDescriptor.getReturnTypes());
        return inv;
    }

    private Invoker<?> getInvoker(String serviceName) {
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

        this.headers = headers;
    }

    private void responseErr(ChannelHandlerContext ctx, GrpcStatus status, String message) {
        Http2Headers trailers = new DefaultHttp2Headers()
                .status(OK.codeAsText())
                .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO)
                .setInt(TripleConstant.STATUS_KEY, status.code)
                .set(TripleConstant.MESSAGE_KEY, message);
        ctx.write(new DefaultHttp2HeadersFrame(trailers, true));
    }

    private void responsePlainTextError(ChannelHandlerContext ctx, int code, int statusCode, String
            msg) {
        Http2Headers headers = new DefaultHttp2Headers(true)
                .status("" + code)
                .setInt(TripleConstant.STATUS_KEY, statusCode)
                .set(TripleConstant.MESSAGE_KEY, msg)
                .set(TripleConstant.CONTENT_TYPE_KEY, "text/plain; encoding=utf-8");
        ctx.write(new DefaultHttp2HeadersFrame(headers));
        ByteBuf buf = ByteBufUtil.writeUtf8(ctx.alloc(), msg);
        ctx.write(new DefaultHttp2DataFrame(buf, true));
    }
}
