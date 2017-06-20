package com.alibaba.dubbo.trace.filter.support;

import com.alibaba.dubbo.rpc.RpcContext;
import io.opentracing.Span;
import io.opentracing.Tracer;

public class OpenTracingContext {
    // replace TracerFactory with any tracer implementation
    public static TracerFactory tracerFactory = TracerFactory.DEFAULT;
    public static final String ACTIVE_SPAN = "ot_active_span";

    public static Tracer getTracer() {
        return tracerFactory.getTracer();
    }

    public static String getTraceId(Span span) {
        return tracerFactory.getTraceId(span);
    }

    public static Span getActiveSpan() {
        Object span = RpcContext.getContext().get(ACTIVE_SPAN);
        if (span != null && span instanceof Span) {
            return (Span) span;
        }
        return null;
    }

    public static void setActiveSpan(Span span) {
        RpcContext.getContext().set(ACTIVE_SPAN, span);
    }

    /**
     * 设置默认的 tracerFactory
     * @param tracerFactory
     */
    public static void setTracerFactory(TracerFactory tracerFactory) {
        OpenTracingContext.tracerFactory = tracerFactory;
    }
}
