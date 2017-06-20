package com.alibaba.dubbo.trace.filter.support;

import io.opentracing.NoopTracerFactory;
import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * @author qinliujie
 * @date 2017/06/19
 */
public interface TracerFactory {
    /**
     * 通过 TracerFactory 拿到 具体的 tracer
     * @return Tracer
     */
    Tracer getTracer();

    /**
     * openTracing 的 span 是没有 traceId 的，但是我们
     * 经常需要使用 traceId，比如在日志下面打印出，方便
     * 查找出相应的链路
     * @return traceId 链路的 traceId
     */
    String getTraceId(Span span);

    TracerFactory DEFAULT = new DefaultTracerFactory();


    class DefaultTracerFactory implements TracerFactory {
        private static Tracer tracer =  NoopTracerFactory.create();

        public Tracer getTracer() {
            return tracer;
        }

        public String getTraceId(Span span) {
            return "";
        }

    }
}
