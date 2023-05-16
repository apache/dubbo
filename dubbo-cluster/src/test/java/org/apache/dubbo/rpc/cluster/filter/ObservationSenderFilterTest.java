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

package org.apache.dubbo.rpc.cluster.filter;

import io.micrometer.common.KeyValues;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.tracing.test.SampleTestRunner;
import io.micrometer.tracing.test.simple.SpansAssert;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.cluster.filter.support.ObservationSenderFilter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.assertj.core.api.BDDAssertions;

class ObservationSenderFilterTest extends AbstractObservationFilterTest {

    @Override
    public SampleTestRunner.SampleTestRunnerConsumer yourCode() {
        return (buildingBlocks, meterRegistry) -> {
            setupConfig();
            setupAttachments();

            ObservationSenderFilter senderFilter = (ObservationSenderFilter) filter;
            senderFilter.invoke(invoker, invocation);
            senderFilter.onResponse(null, invoker, invocation);

            BDDAssertions.then(invocation.getObjectAttachment("X-B3-TraceId")).isNotNull();
            MeterRegistryAssert.then(meterRegistry)
                .hasMeterWithNameAndTags("rpc.client.duration", KeyValues.of("net.peer.name", "foo.bar.com", "net.peer.port", "8080", "rpc.method", "mockMethod", "rpc.service", "DemoService", "rpc.system", "apache_dubbo"));
            SpansAssert.then(buildingBlocks.getFinishedSpans())
                .hasASpanWithNameIgnoreCase("DemoService/mockMethod", spanAssert ->
                    spanAssert
                        .hasTag("net.peer.name", "foo.bar.com")
                        .hasTag("net.peer.port", "8080")
                        .hasTag("rpc.method", "mockMethod")
                        .hasTag("rpc.service", "DemoService")
                        .hasTag("rpc.system", "apache_dubbo"));
        };
    }

    void setupAttachments() {
        RpcContext.getClientAttachment().setUrl(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&side=consumer"));
        RpcContext.getClientAttachment().setRemoteAddress("foo.bar.com", 8080);
    }

    @Override
    ClusterFilter createFilter(ApplicationModel applicationModel) {
        return new ObservationSenderFilter(applicationModel);
    }
}
