package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus.Code;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;

public class ServerStream extends AbstractStream implements Stream {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerStream.class);
    private static final String TOO_MANY_REQ = "Too many requests";
    private static final String MISSING_REQ = "Missing request";
    private static final ExecutorRepository EXECUTOR_REPOSITORY =
            ExtensionLoader.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();
    private final Invoker<?> invoker;
    private final MethodDescriptor methodDescriptor;
    private final ChannelHandlerContext ctx;
    private final ServiceDescriptor serviceDescriptor;
    private final ProviderModel providerModel;


    public ServerStream(Invoker<?> invoker, ServiceDescriptor serviceDescriptor, MethodDescriptor methodDescriptor, ChannelHandlerContext ctx) {
        super(ExecutorUtil.setThreadName(invoker.getUrl(), "DubboPUServerHandler"),
                ctx, TripleUtil.needWrapper(methodDescriptor.getParameterClasses()));
        this.invoker = invoker;
        ServiceRepository repo = ApplicationModel.getServiceRepository();
        this.providerModel = repo.lookupExportedService(getUrl().getServiceKey());
        this.methodDescriptor = methodDescriptor;
        this.serviceDescriptor = serviceDescriptor;
        this.ctx = ctx;
    }


    @Override
    public void onError(GrpcStatus status) {
    }

    @Override
    public void write(Object obj, ChannelPromise promise) throws Exception {

    }

    public void halfClose() throws Exception {
        if (getData() == null) {
            responseErr(ctx, GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription(MISSING_REQ));
            return;
        }
        ExecutorService executor = null;
        if (providerModel != null) {
            executor = (ExecutorService) providerModel.getServiceMetadata().getAttribute(CommonConstants.THREADPOOL_KEY);
        }
        if (executor == null) {
            executor = EXECUTOR_REPOSITORY.getExecutor(getUrl());
        }
        if (executor == null) {
            executor = EXECUTOR_REPOSITORY.createExecutorIfAbsent(getUrl());
        }

        try {
            executor.execute(this::unaryInvoke);
        } catch (RejectedExecutionException e) {
            responseErr(ctx, GrpcStatus.fromCode(Code.RESOURCE_EXHAUSTED)
                    .withDescription("Provider's thread pool is full")
                    .withCause(e));
        } catch (Throwable t) {
            responseErr(ctx, GrpcStatus.fromCode(Code.INTERNAL)
                    .withCause(t)
                    .withDescription("Provider's thread pool is full"));
        }
    }

    private void unaryInvoke() {
        Invocation invocation = buildInvocation();

        final Result result = this.invoker.invoke(invocation);
        CompletionStage<Object> future = result.thenApply(Function.identity());

        BiConsumer<Object, Throwable> onComplete = (appResult, t) -> {
            try {
                if (t != null) {
                    if (t instanceof TimeoutException) {
                        responseErr(ctx, GrpcStatus.fromCode(Code.DEADLINE_EXCEEDED).withCause(t));
                    } else {
                        responseErr(ctx, GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN).withCause(t));
                    }
                    return;
                }
                AppResponse response = (AppResponse) appResult;
                if (response.hasException()) {
                    final Throwable exception = response.getException();
                    if (exception instanceof TripleRpcException) {
                        responseErr(ctx, ((TripleRpcException) exception).getStatus());
                    } else {
                        responseErr(ctx, GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN)
                                .withCause(exception));
                    }
                    return;
                }
                Http2Headers http2Headers = new DefaultHttp2Headers()
                        .status(OK.codeAsText())
                        .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
                final Message message;

                ClassLoader tccl = Thread.currentThread().getContextClassLoader();

                final ByteBuf buf;
                try {
                    ClassLoadUtil.switchContextLoader(providerModel.getServiceInterfaceClass().getClassLoader());
                    if (isNeedWrap()) {
                        message = TripleUtil.wrapResp(getUrl(), getSerializeType(), response.getValue(), methodDescriptor, getMultipleSerialization());
                    } else {
                        message = (Message) response.getValue();
                    }
                    buf = TripleUtil.pack(ctx, message);
                } finally {
                    ClassLoadUtil.switchContextLoader(tccl);
                }

                final Http2Headers trailers = new DefaultHttp2Headers()
                        .setInt(TripleConstant.STATUS_KEY, GrpcStatus.Code.OK.code);
                final Map<String, Object> attachments = response.getObjectAttachments();
                if (attachments != null) {
                    convertAttachment(trailers, attachments);
                }
                ctx.write(new DefaultHttp2HeadersFrame(http2Headers));
                ctx.write(new DefaultHttp2DataFrame(buf));
                ctx.writeAndFlush(new DefaultHttp2HeadersFrame(trailers, true));
            } catch (Exception e) {
                LOGGER.warn("Exception processing triple message", e);
                if (e instanceof TripleRpcException) {
                    responseErr(ctx, ((TripleRpcException) e).getStatus());
                } else {
                    responseErr(ctx, GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN).withCause(e));
                }
            }
        };

        future.whenComplete(onComplete);
    }


    private Invocation buildInvocation() {

        RpcInvocation inv = new RpcInvocation();
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            if (providerModel != null) {
                ClassLoadUtil.switchContextLoader(providerModel.getServiceInterfaceClass().getClassLoader());
            }
            if (isNeedWrap()) {
                final TripleWrapper.TripleRequestWrapper req = TripleUtil.unpack(getData(), TripleWrapper.TripleRequestWrapper.class);
                setSerializeType(req.getSerializeType());
                final Object[] arguments = TripleUtil.unwrapReq(getUrl(), req, getMultipleSerialization());
                inv.setArguments(arguments);
            } else {
                final Object req = TripleUtil.unpack(getData(), methodDescriptor.getParameterClasses()[0]);
                inv.setArguments(new Object[]{req});
            }
        } finally {
            ClassLoadUtil.switchContextLoader(tccl);
        }
        inv.setMethodName(methodDescriptor.getMethodName());
        inv.setServiceName(serviceDescriptor.getServiceName());
        inv.setTargetServiceUniqueName(getUrl().getServiceKey());
        inv.setParameterTypes(methodDescriptor.getParameterClasses());
        inv.setReturnTypes(methodDescriptor.getReturnTypes());

        Map<String, Object> attachments = new HashMap<>();
        for (Map.Entry<CharSequence, CharSequence> header : getHeaders()) {
            String key = header.getKey().toString();
            if (ENABLE_ATTACHMENT_WRAP) {
                if (key.endsWith("-tw-bin") && key.length() > 7) {
                    try {
                        attachments.put(key.substring(0, key.length() - 7), TripleUtil.decodeObjFromHeader(getUrl(), header.getValue(), getMultipleSerialization()));
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse response attachment key=" + key, e);
                    }
                }
            }
            if (key.endsWith("-bin") && key.length() > 4) {
                try {
                    attachments.put(key.substring(0, key.length() - 4), TripleUtil.decodeByteFromHeader(header.getValue()));
                } catch (Exception e) {
                    LOGGER.error("Failed to parse response attachment key=" + key, e);
                }
            } else {
                attachments.put(key, header.getValue().toString());
            }
        }
        inv.setObjectAttachments(attachments);
        return inv;
    }
}
