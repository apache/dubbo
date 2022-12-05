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

package org.apache.dubbo.metrics.filter;

import org.apache.dubbo.common.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.common.metrics.model.MetricsKey;
import org.apache.dubbo.common.metrics.model.sample.MetricSample;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_GROUP_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_INTERFACE_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_METHOD_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_VERSION_KEY;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class MetricsFilterTest {

    private ApplicationModel applicationModel;
    private MetricsFilter filter;
    private DefaultMetricsCollector collector;
    private RpcInvocation invocation;
    private final Invoker<?> invoker = mock(Invoker.class);

    private static final String INTERFACE_NAME = "org.apache.dubbo.MockInterface";
    private static final String METHOD_NAME = "mockMethod";
    private static final String GROUP = "mockGroup";
    private static final String VERSION = "1.0.0";

    @BeforeEach
    public void setup() {
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");

        applicationModel = ApplicationModel.defaultModel();
        applicationModel.getApplicationConfigManager().setApplication(config);

        invocation = new RpcInvocation();
        filter = new MetricsFilter();

        collector = applicationModel.getBeanFactory().getOrRegisterBean(DefaultMetricsCollector.class);
        filter.setApplicationModel(applicationModel);
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    void testCollectDisabled() {
        given(invoker.invoke(invocation)).willReturn(new AppResponse("success"));

        filter.invoke(invoker, invocation);
        Map<String, MetricSample> metricsMap = getMetricsMap();
        Assertions.assertTrue(metricsMap.isEmpty());
    }

    @Test
    void testFailedRequests() {
        collector.setCollectEnabled(true);
        given(invoker.invoke(invocation)).willThrow(new RpcException("failed"));
        initParam();

        try {
            filter.invoke(invoker, invocation);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof RpcException);
            filter.onError(e, invoker, invocation);
        }

        Map<String, MetricSample> metricsMap = getMetricsMap();
        Assertions.assertTrue(metricsMap.containsKey("requests.failed"));
        Assertions.assertFalse(metricsMap.containsKey("requests.succeed"));

        MetricSample sample = metricsMap.get("requests.failed");
        Map<String, String> tags = sample.getTags();

        Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), INTERFACE_NAME);
        Assertions.assertEquals(tags.get(TAG_METHOD_KEY), METHOD_NAME);
        Assertions.assertEquals(tags.get(TAG_GROUP_KEY), GROUP);
        Assertions.assertEquals(tags.get(TAG_VERSION_KEY), VERSION);
    }


    @Test
    void testBusinessFailedRequests() {
        collector.setCollectEnabled(true);

        given(invoker.invoke(invocation)).willThrow(new RpcException(RpcException.BIZ_EXCEPTION));
        initParam();

        try {
            filter.invoke(invoker, invocation);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof RpcException);
            filter.onError(e, invoker, invocation);
        }

        Map<String, MetricSample> metricsMap = getMetricsMap();
        Assertions.assertTrue(metricsMap.containsKey(MetricsKey.METRIC_REQUEST_BUSINESS_FAILED.getName()));
        Assertions.assertFalse(metricsMap.containsKey("requests.succeed"));

        MetricSample sample = metricsMap.get(MetricsKey.METRIC_REQUEST_BUSINESS_FAILED.getName());

        Map<String, String> tags = sample.getTags();

        Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), INTERFACE_NAME);
        Assertions.assertEquals(tags.get(TAG_METHOD_KEY), METHOD_NAME);
        Assertions.assertEquals(tags.get(TAG_GROUP_KEY), GROUP);
        Assertions.assertEquals(tags.get(TAG_VERSION_KEY), VERSION);
    }

    @Test
    void testSucceedRequests() {
        collector.setCollectEnabled(true);
        given(invoker.invoke(invocation)).willReturn(new AppResponse("success"));
        initParam();

        Result result = filter.invoke(invoker, invocation);

        filter.onResponse(result, invoker, invocation);

        Map<String, MetricSample> metricsMap = getMetricsMap();
        Assertions.assertFalse(metricsMap.containsKey("requests.failed"));
        Assertions.assertTrue(metricsMap.containsKey("requests.succeed"));

        MetricSample sample = metricsMap.get("requests.succeed");
        Map<String, String> tags = sample.getTags();

        Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), INTERFACE_NAME);
        Assertions.assertEquals(tags.get(TAG_METHOD_KEY), METHOD_NAME);
        Assertions.assertEquals(tags.get(TAG_GROUP_KEY), GROUP);
        Assertions.assertEquals(tags.get(TAG_VERSION_KEY), VERSION);
    }

    @Test
    void testMissingGroup() {
        collector.setCollectEnabled(true);
        given(invoker.invoke(invocation)).willReturn(new AppResponse("success"));
        invocation.setTargetServiceUniqueName(INTERFACE_NAME + ":" + VERSION);
        invocation.setMethodName(METHOD_NAME);
        invocation.setParameterTypes(new Class[]{String.class});

        Result result = filter.invoke(invoker, invocation);

        filter.onResponse(result, invoker, invocation);

        Map<String, MetricSample> metricsMap = getMetricsMap();

        MetricSample sample = metricsMap.get("requests.succeed");
        Map<String, String> tags = sample.getTags();

        Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), INTERFACE_NAME);
        Assertions.assertEquals(tags.get(TAG_METHOD_KEY), METHOD_NAME);
        Assertions.assertNull(tags.get(TAG_GROUP_KEY));
        Assertions.assertEquals(tags.get(TAG_VERSION_KEY), VERSION);
    }

    @Test
    void testMissingVersion() {
        collector.setCollectEnabled(true);
        given(invoker.invoke(invocation)).willReturn(new AppResponse("success"));
        invocation.setTargetServiceUniqueName(GROUP + "/" + INTERFACE_NAME);
        invocation.setMethodName(METHOD_NAME);
        invocation.setParameterTypes(new Class[]{String.class});

        Result result = filter.invoke(invoker, invocation);

        filter.onResponse(result, invoker, invocation);

        Map<String, MetricSample> metricsMap = getMetricsMap();

        MetricSample sample = metricsMap.get("requests.succeed");
        Map<String, String> tags = sample.getTags();

        Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), INTERFACE_NAME);
        Assertions.assertEquals(tags.get(TAG_METHOD_KEY), METHOD_NAME);
        Assertions.assertEquals(tags.get(TAG_GROUP_KEY), GROUP);
        Assertions.assertNull(tags.get(TAG_VERSION_KEY));
    }

    @Test
    void testMissingGroupAndVersion() {
        collector.setCollectEnabled(true);
        given(invoker.invoke(invocation)).willReturn(new AppResponse("success"));
        invocation.setTargetServiceUniqueName(INTERFACE_NAME);
        invocation.setMethodName(METHOD_NAME);
        invocation.setParameterTypes(new Class[]{String.class});

        Result result = filter.invoke(invoker, invocation);

        filter.onResponse(result, invoker, invocation);

        Map<String, MetricSample> metricsMap = getMetricsMap();

        MetricSample sample = metricsMap.get("requests.succeed");
        Map<String, String> tags = sample.getTags();

        Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), INTERFACE_NAME);
        Assertions.assertEquals(tags.get(TAG_METHOD_KEY), METHOD_NAME);
        Assertions.assertNull(tags.get(TAG_GROUP_KEY));
        Assertions.assertNull(tags.get(TAG_VERSION_KEY));
    }

    @Test
    void testGenericCall() {
        collector.setCollectEnabled(true);
        given(invoker.invoke(invocation)).willReturn(new AppResponse("success"));
        invocation.setTargetServiceUniqueName(INTERFACE_NAME);
        invocation.setMethodName(METHOD_NAME);
        invocation.setParameterTypes(new Class[]{String.class});

        Result result = filter.invoke(invoker, invocation);

        invocation.setMethodName($INVOKE);
        invocation.setParameterTypesDesc(GENERIC_PARAMETER_DESC);
        invocation.setArguments(new Object[]{METHOD_NAME, new String[]{"java.lang.String"}, new Object[]{"mock"}});

        filter.onResponse(result, invoker, invocation);

        Map<String, MetricSample> metricsMap = getMetricsMap();

        MetricSample sample = metricsMap.get("requests.processing");
        Map<String, String> tags = sample.getTags();

        Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), INTERFACE_NAME);
        Assertions.assertEquals(tags.get(TAG_METHOD_KEY), METHOD_NAME);
    }

    private void initParam() {
        invocation.setTargetServiceUniqueName(GROUP + "/" + INTERFACE_NAME + ":" + VERSION);
        invocation.setMethodName(METHOD_NAME);
        invocation.setParameterTypes(new Class[]{String.class});
    }

    private Map<String, MetricSample> getMetricsMap() {
        List<MetricSample> samples = collector.collect();
        return samples.stream().collect(Collectors.toMap(MetricSample::getName, Function.identity()));
    }
}
