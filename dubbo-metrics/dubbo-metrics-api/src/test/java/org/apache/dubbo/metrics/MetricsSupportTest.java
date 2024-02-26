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
package org.apache.dubbo.metrics;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metrics.model.MetricsSupport;
import org.apache.dubbo.metrics.model.ServiceKeyMetric;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS;

public class MetricsSupportTest {

    @Test
    void testFillZero() {
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");
        applicationModel.getApplicationConfigManager().setApplication(config);

        Map<MetricsKeyWrapper, Map<ServiceKeyMetric, AtomicLong>> data = new HashMap<>();
        MetricsKeyWrapper key1 = new MetricsKeyWrapper(
                METRIC_REQUESTS, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.METHOD));
        MetricsKeyWrapper key2 = new MetricsKeyWrapper(
                METRIC_REQUESTS, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.METHOD));
        ServiceKeyMetric sm1 = new ServiceKeyMetric(applicationModel, "a.b.c");
        ServiceKeyMetric sm2 = new ServiceKeyMetric(applicationModel, "a.b.d");
        data.computeIfAbsent(key1, k -> new HashMap<>()).put(sm1, new AtomicLong(1));
        data.computeIfAbsent(key1, k -> new HashMap<>()).put(sm2, new AtomicLong(1));
        data.put(key2, new HashMap<>());
        Assertions.assertEquals(
                2, data.values().stream().mapToLong(map -> map.values().size()).sum());
        MetricsSupport.fillZero(data);
        Assertions.assertEquals(
                4, data.values().stream().mapToLong(map -> map.values().size()).sum());
    }
}
