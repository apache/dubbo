/*
 * Copyright 1999-2012 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
