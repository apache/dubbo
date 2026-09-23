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

import org.apache.dubbo.tracing.context.DubboClientContext;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class DubboClientTracingObservationHandlerTest {

    private Tracer mockTracer;
    private CurrentTraceContext mockCurrentTraceContext;
    private TraceContext mockTraceContext;
    private DubboClientContext mockContext;
    private DubboClientTracingObservationHandler<DubboClientContext> handler;

    @BeforeEach
    void setUp() {
        // Clear MDC before each test to ensure a clean state
        MDC.clear();

        mockTracer = Mockito.mock(Tracer.class);
        mockCurrentTraceContext = Mockito.mock(CurrentTraceContext.class);
        mockTraceContext = Mockito.mock(TraceContext.class);
        mockContext = Mockito.mock(DubboClientContext.class);

        when(mockTracer.currentTraceContext()).thenReturn(mockCurrentTraceContext);
        when(mockCurrentTraceContext.context()).thenReturn(mockTraceContext);
        when(mockTraceContext.traceId()).thenReturn("mock-trace-id-123");
        when(mockTraceContext.spanId()).thenReturn("mock-span-id-456");

        handler = new DubboClientTracingObservationHandler<>(mockTracer);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void proveWorking_WhenScopeOpens_MdcShouldHaveIds() {
        // 1. Verify MDC is currently empty
        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("spanId"));

        handler.onScopeOpened(mockContext);

        assertEquals("mock-trace-id-123", MDC.get("traceId"), "Trace ID should be mapped to MDC!");
        assertEquals("mock-span-id-456", MDC.get("spanId"), "Span ID should be mapped to MDC!");
    }

    @Test
    void proveFailurePrevention_WhenScopeCloses_MdcShouldBeCleaned() {
        handler.onScopeOpened(mockContext);
        assertEquals("mock-trace-id-123", MDC.get("traceId"));

        handler.onScopeClosed(mockContext);

        // PROOF THE LEAK IS PREVENTED: MDC must be completely empty now.
        assertNull(MDC.get("traceId"), "Trace ID leaked! It was not removed when scope closed.");
        assertNull(MDC.get("spanId"), "Span ID leaked! It was not removed when scope closed.");
    }
}
