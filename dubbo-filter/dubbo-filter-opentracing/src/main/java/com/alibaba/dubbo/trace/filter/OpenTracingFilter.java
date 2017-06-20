package com.alibaba.dubbo.trace.filter;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.trace.filter.support.OpenTracingContext;
import com.alibaba.dubbo.trace.filter.support.TracerFactory;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.propagation.TextMapExtractAdapter;

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * @author qinliujie
 * @date 2017/06/19
 */
@Activate(
        group = {"provider", "consumer"}
)
public class OpenTracingFilter implements Filter {


    static {
        // 将相应的 tracer 实现放到 META-INF/services/com.alibaba.dubbo.trace.filter.support.TracerFactory
        Iterator<TracerFactory> iterator = ServiceLoader.load(TracerFactory.class).iterator();
        if (iterator.hasNext()) {
            TracerFactory tracerFactory = iterator.next();
            OpenTracingContext.setTracerFactory(tracerFactory);
        } else {
            //just for test
            //OpenTracingContext.setTracerFactory(new BraveTracerFactory());
        }
    }


    private static Logger logger = LoggerFactory.getLogger(OpenTracingFilter.class);

    public OpenTracingFilter() {

    }

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        RpcContext context = RpcContext.getContext();
        boolean isConsumerSide = context.isConsumerSide();

        if (!isConsumerSide) {
            return processProviderTrace(invoker, invocation, context);
        } else {
            return processConsumerTrace(invoker, invocation, context);
        }
    }

    private Result processProviderTrace(Invoker<?> invoker, Invocation invocation, RpcContext context) {
        Tracer tracer = getTracer();
        String operationName = buildProviderOperationName(invoker, invocation);
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName);
        try {
            SpanContext spanContext = tracer.extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(invocation.getAttachments()));
            if (spanContext != null) {
                spanBuilder.asChildOf(spanContext);
            }
        } catch (Exception e) {
            spanBuilder.withTag("Error", "extract from request fail, error msg:" + e.getMessage());
        }

        spanBuilder.withTag("ServerHost", invoker.getUrl().getAddress());
        spanBuilder.withTag("ServerRecieved", System.currentTimeMillis());
        Span traceSpan = spanBuilder.start();
        OpenTracingContext.setActiveSpan(traceSpan);
        Result returnResult;
        try {
            Result result = invoker.invoke(invocation);
            traceSpan.log("Server process success.");
            returnResult = result;
        } catch (RpcException rpcException) {
            traceSpan.log("Server process  fail." + (rpcException == null ? "unknown exception" : rpcException.getMessage()));
            logger.error("traceId:" + getTraceId(traceSpan) + rpcException.getMessage(), rpcException);
            throw rpcException;
        } finally {
            try {
                traceSpan.setTag("ServerSend", System.currentTimeMillis());
                traceSpan.finish();
            } catch (Exception exception) {
                logger.error(exception.getMessage(), exception);
            }

        }
        return returnResult;
    }

    private Result processConsumerTrace(Invoker<?> invoker, final Invocation invocation, final RpcContext context) {
        Tracer tracer = getTracer();
        String operationName = buildConsumerOperationName(invoker, invocation);
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName);
        Span activeSpan = OpenTracingContext.getActiveSpan();
        if (activeSpan != null) {
            spanBuilder.asChildOf(activeSpan);
        }
        spanBuilder.withTag("ClientHost", NetUtils.getLocalHost());
        spanBuilder.withTag("ClientSend", System.currentTimeMillis());
        Span traceSpan = spanBuilder.start();

        System.out.println("traceId:" + getTraceId(traceSpan));

        tracer.inject(traceSpan.context(), Format.Builtin.TEXT_MAP, new TextMap() {

            public void put(String key, String value) {
                context.setAttachment(key, value);
            }

            public Iterator<Map.Entry<String, String>> iterator() {
                throw new UnsupportedOperationException("TextMapInjectAdapter should only be used with Tracer.inject()");
            }
        });


        Result returnResult;
        try {
            Result result = invoker.invoke(invocation);
            traceSpan.log("client request success.");
            returnResult = result;
        } catch (RpcException rpcException) {
            traceSpan.log("client request fail." + (rpcException == null ? "unknown exception" : rpcException.getMessage()));
            logger.error("traceId:" + getTraceId(traceSpan) + rpcException.getMessage(), rpcException);
            throw rpcException;
        } finally {
            try {
                traceSpan.setTag("ClientRecieve", System.currentTimeMillis());
                traceSpan.finish();
            } catch (Exception exception) {
                logger.error(exception.getMessage(), exception);
            }

        }
        return returnResult;
    }

    private Tracer getTracer() {
        return OpenTracingContext.getTracer();
    }

    private String getTraceId(Span span) {
        return OpenTracingContext.getTraceId(span);
    }

    protected String buildConsumerOperationName(Invoker invoker, Invocation invocation) {
        return "Invoke_" + invoker.getInterface().getName() + "_" + invocation.getMethodName();
    }

    protected String buildProviderOperationName(Invoker invoker, Invocation invocation) {
        return "Process_Invoke_" + invoker.getInterface().getName() + "_" + invocation.getMethodName();
    }


}
