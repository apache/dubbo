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

package org.apache.dubbo.metrics.observation;

import io.micrometer.common.KeyValues;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SpansAssert;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.assertj.core.api.BDDAssertions;

class ObservationReceiverFilterTest extends AbstractObservationFilterTest {

    @Override
    public SampleTestRunnerConsumer yourCode() {
        return (buildingBlocks, meterRegistry) -> {
            setupConfig();
            setupAttachments(buildingBlocks.getTracer());
            invoker = new AssertingInvoker(buildingBlocks.getTracer());

            ObservationReceiverFilter senderFilter = (ObservationReceiverFilter) filter;
            senderFilter.invoke(invoker, invocation);
            senderFilter.onResponse(null, invoker, invocation);

            MeterRegistryAssert.then(meterRegistry)
                .hasMeterWithNameAndTags("rpc.server.duration", KeyValues.of("rpc.method", "mockMethod", "rpc.service", "DemoService", "rpc.system", "apache_dubbo"));
            SpansAssert.then(buildingBlocks.getFinishedSpans())
                .hasASpanWithNameIgnoreCase("DemoService/mockMethod", spanAssert ->
                    spanAssert
                        .hasTag("rpc.method", "mockMethod")
                        .hasTag("rpc.service", "DemoService")
                        .hasTag("rpc.system", "apache_dubbo"));
        };
    }

    void setupAttachments(Tracer tracer) {
        RpcContext.getServerAttachment().setUrl(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&side=consumer"));
        RpcContext.getServerAttachment().setMethodName("foo");
        RpcContext.getServerAttachment().setRemoteAddress("foo.bar.com", 8080);
        RpcContext.getServerAttachment().setAttachment("X-B3-TraceId", tracer.currentSpan().context().traceId());
        RpcContext.getServerAttachment().setAttachment("X-B3-SpanId", tracer.currentSpan().context().spanId());
        RpcContext.getServerAttachment().setAttachment("X-B3-Sampled", "1");
    }

    @Override
    Filter createFilter(ApplicationModel applicationModel) {
        return new ObservationReceiverFilter(applicationModel);
    }

    static class AssertingInvoker implements Invoker {

        private final String expectedTraceId;

        private final String parentSpanId;

        private final Tracer tracer;

        AssertingInvoker(Tracer tracer) {
            this.tracer = tracer;
            this.expectedTraceId = tracer.currentSpan().context().traceId();
            this.parentSpanId = tracer.currentSpan().context().spanId();
        }

        @Override
        public URL getUrl() {
            return null;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void destroy() {

        }

        @Override
        public Class getInterface() {
            return AssertingInvoker.class;
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            Span span = this.tracer.currentSpan();
            BDDAssertions.then(span.context().traceId())
                .as("Should propagate the trace id from the attributes")
                .isEqualTo(this.expectedTraceId);
            BDDAssertions.then(span.context().spanId())
                .as("A child span must be created")
                .isNotEqualTo(this.parentSpanId);
            return new AppResponse("OK");
        }
    }
}
