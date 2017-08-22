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
