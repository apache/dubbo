package com.alibaba.dubbo.trace.filter.support;

import brave.Tracing;
import brave.opentracing.BraveSpanContext;
import brave.opentracing.BraveTracer;
import io.opentracing.Tracer;
import zipkin.Span;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.okhttp3.OkHttpSender;

/**
 * @author qinliujie
 * @date 2017/06/19
 */

/**
 * quick start sample use docker-zipkin https://github.com/openzipkin/docker-zipkin
 */
public class BraveTracerFactory implements TracerFactory {
    private static Tracer tracer = null;

    public Tracer getTracer() {
        return tracer;
    }

    public String getTraceId(io.opentracing.Span span) {
        return ((BraveSpanContext)span.context()).unwrap().traceIdString();
    }

    static {
        OkHttpSender sender = OkHttpSender.create("http://127.0.0.1:9411/api/v1/spans");
        AsyncReporter<Span> reporter = AsyncReporter.builder(sender).build();

        Tracing braveTracing = Tracing.newBuilder()
                .reporter(reporter)
                .build();

        Tracer tracer = BraveTracer.create(braveTracing);

        BraveTracerFactory.tracer = tracer;

    }

}
