package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;

import io.netty.buffer.ByteBuf;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class TripleHttp2Handler extends ChannelDuplexHandler {
    private static final InvokerResolver invokerResolver = ExtensionLoader.getExtensionLoader(InvokerResolver.class).getDefaultExtension();
    private final GrpcDecoder decoder = new GrpcDecoder();
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
        Invocation invocation = buildInvocation(headers, (ByteBuf) out.get(0));
        final String path = headers.path().toString();
        String[] parts = path.split("/");
        // todo parts illegal service not found
        String serviceName = parts[1];
        // TODO add version/group support
        //TODO add method not found / service not found err
        Result result = invokerResolver.resolve(serviceName).invoke(invocation);
        CompletionStage<Object> future = result.thenApply(Function.identity());

        future.whenComplete((appResult, t) -> {
            try {
                if (t != null) {
                    // TODO add exception response
                    return;
                }
                AppResponse response = (AppResponse) appResult;
                if (!response.hasException()) {
                    Http2Headers http2Headers = new DefaultHttp2Headers()
                            .status(OK.codeAsText())
                            .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
                    ctx.write(new DefaultHttp2HeadersFrame(http2Headers));
                    ByteBuf byteBuf = Marshaller.marshaller.marshaller(ctx.alloc(), response.getValue());
                    ctx.write(new DefaultHttp2DataFrame(byteBuf));
                    final Http2Headers trailers = new DefaultHttp2Headers()
                            .setInt(TripleConstant.STATUS_KEY, GrpcStatus.OK.code);
                    ctx.write(new DefaultHttp2HeadersFrame(trailers, true));
                }
            } catch (Exception e) {
                // TODO
                e.printStackTrace();
            }
        });
    }

    private ByteBuf readMessage(ByteBuf content) {
        ByteBuf data = null;
        // TODO check reserved bit
        content.readByte();
        // TODO check len
        final int len = content.readInt();
        if (len <= content.readableBytes()) {
            data = content.readSlice(len);
        }
        return data;
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


    private Invocation buildInvocation(Http2Headers http2Headers, ByteBuf data) {

        RpcInvocation inv = new RpcInvocation();
        final String path = http2Headers.path().toString();
        String[] parts = path.split("/");
        // todo parts illegal service not found
        String serviceName = parts[1];
        String originalMethodName = parts[2];
        String methodName = originalMethodName.substring(0, 1).toLowerCase().concat(originalMethodName.substring(1));

        ServiceRepository repo = ApplicationModel.getServiceRepository();
        MethodDescriptor methodDescriptor = repo.lookupMethod(serviceName, methodName);
        Object obj = Marshaller.marshaller.unmarshaller(methodDescriptor.getParameterClasses()[0], data);
        inv.setMethodName(methodName);
        inv.setServiceName(serviceName);
        inv.setTargetServiceUniqueName(serviceName);
        inv.setParameterTypes(methodDescriptor.getParameterClasses());
        inv.setArguments(new Object[]{obj});
        inv.setReturnTypes(methodDescriptor.getReturnTypes());

        return inv;
    }

}
