package org.apache.dubbo.rpc.protocol.tri;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2GoAwayFrame;
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2PingFrame;
import io.netty.util.concurrent.Future;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.Serialization2;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;

import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus.Code;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;

public class ServerStream extends AbstractStream implements Stream {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerStream.class);
    private static final String TOO_MANY_REQ = "Too many requests";
    private static final String MISSING_REQ = "Missing request";
    private final Invoker<?> invoker;
    private final MethodDescriptor methodDescriptor;
    private final ChannelHandlerContext ctx;
    private final String serviceName;
    private final String methodName;
    private final Serialization2 serialization2;


    public ServerStream(Invoker<?> invoker, MethodDescriptor methodDescriptor, ChannelHandlerContext ctx, String serviceName, String methodName) {
        super(ctx);
        this.invoker = invoker;
        this.methodDescriptor = methodDescriptor;
        this.ctx = ctx;
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.serialization2 = ExtensionLoader.getExtensionLoader(Serialization2.class).getExtension("protobuf");
    }


    @Override
    public void onError(GrpcStatus status) {
    }

    public void halfClose() {
        if (getData() == null) {
            responseErr(ctx, GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription(MISSING_REQ));
            return;
        }

        Invocation invocation = buildInvocation(serviceName, methodName, methodDescriptor);

        final Result result = this.invoker.invoke(invocation);
        CompletionStage<Object> future = result.thenApply(Function.identity());

        future.whenComplete((appResult, t) -> {
            try {
                if (t != null) {
                    if (t instanceof TimeoutException) {
                        responseErr(ctx, GrpcStatus.fromCode(Code.DEADLINE_EXCEEDED).withCause(t));
                    }
                    responseErr(ctx, GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN).withCause(t));
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
                            .setInt(TripleConstant.STATUS_KEY, GrpcStatus.Code.OK.code);
                    ctx.write(new DefaultHttp2HeadersFrame(trailers, true));
                } else {
                    final Throwable exception = response.getException();
                    if (exception instanceof TripleRpcException) {
                        responseErr(ctx, ((TripleRpcException) exception).getStatus());
                    } else {
                        responseErr(ctx, GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN)
                                .withCause(exception));
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Exception processing triple message", e);
                if (e instanceof TripleRpcException) {
                    responseErr(ctx, ((TripleRpcException) e).getStatus());
                } else {
                    responseErr(ctx, GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN).withCause(e));
                }
            }
        });

    }

    private Invocation buildInvocation(String serviceName, String methodName, MethodDescriptor methodDescriptor) {
        RpcInvocation inv = new RpcInvocation();
        try {
            final Object req = serialization2.deserialize(getData(), methodDescriptor.getParameterClasses()[0]);
            getData().close();
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
