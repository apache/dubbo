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
