package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.Serialization2;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;

import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;

public class UnaryInvoker {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnaryInvoker.class);
    private static final String TOO_MANY_REQ = "Too many requests";
    private static final String MISSING_REQ = "Missing request";
    private final Invoker<?> invoker;
    private final MethodDescriptor methodDescriptor;
    private final ChannelHandlerContext ctx;
    private final Http2Headers headers;
    private final String serviceName;
    private final String methodName;
    private final Serialization2 serialization2;
    private ByteBuf pendingData;


    public UnaryInvoker(Invoker<?> invoker, MethodDescriptor methodDescriptor, ChannelHandlerContext ctx, Http2Headers headers, String serviceName, String methodName) {
        this.invoker = invoker;
        this.methodDescriptor = methodDescriptor;
        this.ctx = ctx;
        this.headers = headers;
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.serialization2 = ExtensionLoader.getExtensionLoader(Serialization2.class).getExtension("protobuf");
    }

    public void receiveData(ByteBuf buf) {
        if (pendingData != null) {
            if (buf.isReadable()) {
                responseErr(ctx, GrpcStatus.INTERNAL, TOO_MANY_REQ);
            }
            return;
        }
        this.pendingData = buf;
    }

    public void halfClose() {
        if (pendingData == null) {
            responseErr(ctx, GrpcStatus.INTERNAL, MISSING_REQ);
            return;
        }

        Invocation invocation = buildInvocation(pendingData, serviceName, methodName, methodDescriptor);

        final Result result = this.invoker.invoke(invocation);
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
                    serialization2.serialize(response.getValue(), bos);
                    ctx.write(new DefaultHttp2DataFrame(buf));
                    final Http2Headers trailers = new DefaultHttp2Headers()
                            .setInt(TripleConstant.STATUS_KEY, GrpcStatus.OK.code);
                    ctx.write(new DefaultHttp2HeadersFrame(trailers, true));
                }
            } catch (Exception e) {
                LOGGER.warn("Exception processing triple message", e);
                if (e instanceof TripleRpcException) {
                    responseErr(ctx, ((TripleRpcException) e).getStatus(), e.getMessage());
                } else {
                    responseErr(ctx, GrpcStatus.UNKNOWN, e.getMessage());
                }
            }
        });

    }

    private Invocation buildInvocation(ByteBuf data, String serviceName, String methodName, MethodDescriptor methodDescriptor) {
        RpcInvocation inv = new RpcInvocation();
        try {
            final Object req = serialization2.deserialize(new ByteBufInputStream(data), methodDescriptor.getParameterClasses()[0]);
            inv.setArguments(new Object[]{req});
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize request on server side", e);
        }
        inv.setMethodName(methodName);
        inv.setServiceName(serviceName);
        inv.setTargetServiceUniqueName(serviceName);
        inv.setParameterTypes(methodDescriptor.getParameterClasses());
        inv.setReturnTypes(methodDescriptor.getReturnTypes());
        return inv;
    }
}
