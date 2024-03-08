/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.tracing.handler;

import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.tracing.context.DubboServerContext;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.transport.ReceiverContext;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;

public class DubboServerTracingObservationHandler<T extends DubboServerContext> implements ObservationHandler<T> {

    private static final String DEFAULT_TRACE_ID_KEY = "traceId";

    private final Tracer tracer;

    public DubboServerTracingObservationHandler(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void onScopeOpened(T context) {
        TraceContext traceContext = tracer.currentTraceContext().context();
        if (traceContext == null) {
            return;
        }
        RpcContext.getServerContext().setAttachment(DEFAULT_TRACE_ID_KEY, traceContext.traceId());
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ReceiverContext;
    }
}
