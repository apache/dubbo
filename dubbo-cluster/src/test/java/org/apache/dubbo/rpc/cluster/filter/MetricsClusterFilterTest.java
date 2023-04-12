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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.filter.MetricsFilter;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.CounterMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.filter.support.MetricsClusterFilter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class MetricsClusterFilterTest {

    private       ApplicationModel        applicationModel;
    private       MetricsFilter           filter;
    private       MetricsClusterFilter    metricsClusterFilter;
    private       DefaultMetricsCollector collector;
    private       RpcInvocation           invocation;
    private final Invoker<?>              invoker = mock(Invoker.class);

    private static final String INTERFACE_NAME = "org.apache.dubbo.MockInterface";
    private static final String METHOD_NAME = "mockMethod";
    private static final String GROUP = "mockGroup";
    private static final String VERSION = "1.0.0";
    private String side;

    private AtomicBoolean initApplication = new AtomicBoolean(false);


    @BeforeEach
    public void setup() {
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");
        //RpcContext.getContext().setAttachment("MockMetrics","MockMetrics");

        applicationModel = ApplicationModel.defaultModel();
        applicationModel.getApplicationConfigManager().setApplication(config);

        invocation = new RpcInvocation();
        filter = new MetricsFilter();

        collector = applicationModel.getBeanFactory().getOrRegisterBean(DefaultMetricsCollector.class);
        if(!initApplication.get()) {
            collector.collectApplication(applicationModel);
            initApplication.set(true);
        }
        filter.setApplicationModel(applicationModel);
        side = CommonConstants.CONSUMER;
        invocation.setInvoker(new TestMetricsInvoker(side));
        RpcContext.getServiceContext().setUrl(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&side=" + side));

        metricsClusterFilter = new MetricsClusterFilter();
        metricsClusterFilter.setApplicationModel(applicationModel);
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    public void testNoProvider(){
        testClusterFilterError(RpcException.FORBIDDEN_EXCEPTION,
            MetricsKey.METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED.getNameByType(CommonConstants.CONSUMER));
    }

    private void testClusterFilterError(int errorCode,String name){
        collector.setCollectEnabled(true);
        given(invoker.invoke(invocation)).willThrow(new RpcException(errorCode));
        initParam();

        Long count = 1L;

        for (int i = 0; i < count; i++) {
            try {
                metricsClusterFilter.invoke(invoker, invocation);
            } catch (Exception e) {
                Assertions.assertTrue(e instanceof RpcException);
                metricsClusterFilter.onError(e, invoker, invocation);
            }
        }
        Map<String, MetricSample> metricsMap = getMetricsMap();
        Assertions.assertTrue(metricsMap.containsKey(name));

        MetricSample sample = metricsMap.get(name);

        Assertions.assertSame(((CounterMetricSample) sample).getValue().longValue(), count);
        teardown();
    }



    private void initParam() {
        invocation.setTargetServiceUniqueName(GROUP + "/" + INTERFACE_NAME + ":" + VERSION);
        invocation.setMethodName(METHOD_NAME);
        invocation.setParameterTypes(new Class[]{String.class});
    }

    private Map<String, MetricSample> getMetricsMap() {
        List<MetricSample> samples = collector.collect();
        List<MetricSample> samples1 = new ArrayList<>();
        for (MetricSample sample : samples) {
            if (sample.getName().contains("dubbo.thread.pool")) {
                continue;
            }
            samples1.add(sample);
        }
        return samples1.stream().collect(Collectors.toMap(MetricSample::getName, Function.identity()));
    }

    public class TestMetricsInvoker implements Invoker {

        private String side;

        public TestMetricsInvoker(String side) {
            this.side = side;
        }

        @Override
        public Class getInterface() {
            return null;
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            return null;
        }

        @Override
        public URL getUrl() {
            return URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&side="+side);
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void destroy() {

        }
    }
}
