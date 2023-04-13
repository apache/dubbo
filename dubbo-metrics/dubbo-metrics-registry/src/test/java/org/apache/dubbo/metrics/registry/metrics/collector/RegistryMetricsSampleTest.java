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

package org.apache.dubbo.metrics.registry.metrics.collector;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.metrics.registry.event.type.ApplicationType;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER;

class RegistryMetricsSampleTest {

    private ApplicationModel applicationModel;

    @BeforeEach
    public void setup() {
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        applicationModel = frameworkModel.newApplication();
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");
        applicationModel.getApplicationConfigManager().setApplication(config);
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    void testRegisterMetrics() {
    }

    @Test
    void testRTMetrics() {
        RegistryMetricsCollector collector = new RegistryMetricsCollector(applicationModel);
        collector.setCollectEnabled(true);
        String applicationName = applicationModel.getApplicationName();
        collector.addApplicationRT(applicationName, OP_TYPE_REGISTER.getType(), 10L);
        collector.addApplicationRT(applicationName, OP_TYPE_REGISTER.getType(), 0L);

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            Map<String, String> tags = sample.getTags();
            Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationName);
        }

        @SuppressWarnings("rawtypes")
        Map<String, Long> sampleMap = samples.stream().collect(Collectors.toMap(MetricSample::getName, k -> ((GaugeMetricSample) k).applyAsLong()));

        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_LAST, OP_TYPE_REGISTER).targetKey()), 0L);
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MIN, OP_TYPE_REGISTER).targetKey()), 0L);
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MAX, OP_TYPE_REGISTER).targetKey()), 10L);
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_AVG, OP_TYPE_REGISTER).targetKey()), 5L);
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_SUM, OP_TYPE_REGISTER).targetKey()), 10L);
    }

    @Test
    void testListener() {
        RegistryMetricsCollector collector = new RegistryMetricsCollector(applicationModel);
        collector.setCollectEnabled(true);
        String applicationName = applicationModel.getApplicationName();
        collector.increment(applicationName, ApplicationType.R_TOTAL);
    }

}
