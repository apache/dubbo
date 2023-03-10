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

package org.apache.dubbo.metrics.collector;

import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metrics.model.ConfigCenterMetric;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_GROUP_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_INTERFACE_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_METHOD_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_VERSION_KEY;
import static org.junit.jupiter.api.Assertions.*;

class ConfigCenterMetricsCollectorTest {

    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;

    @BeforeEach
    public void setup() {
        frameworkModel = FrameworkModel.defaultModel();
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
    void increase4Initialized() {
        ConfigCenterMetricsCollector collector = new ConfigCenterMetricsCollector(applicationModel);
        collector.setCollectEnabled(true);
        String applicationName = applicationModel.getApplicationName();
        collector.increase4Initialized("key", "group", "nacos", applicationName, 1);
        collector.increase4Initialized("key", "group", "nacos", applicationName, 1);

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            Assertions.assertTrue(sample instanceof GaugeMetricSample);
            GaugeMetricSample<Long> gaugeSample = (GaugeMetricSample) sample;
            Map<String, String> tags = gaugeSample.getTags();

            Assertions.assertEquals(gaugeSample.applyAsLong(), 2);
            Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationName);
        }
    }

    @Test
    void increaseUpdated() {
        ConfigCenterMetricsCollector collector = new ConfigCenterMetricsCollector(applicationModel);
        collector.setCollectEnabled(true);
        String applicationName = applicationModel.getApplicationName();

        ConfigChangedEvent event = new ConfigChangedEvent("key", "group", null, ConfigChangeType.ADDED);
        
        collector.increaseUpdated("nacos", applicationName, event);
        collector.increaseUpdated("nacos", applicationName, event);

        List<MetricSample> samples = collector.collect();
        for (MetricSample sample : samples) {
            Assertions.assertTrue(sample instanceof GaugeMetricSample);
            GaugeMetricSample<Long> gaugeSample = (GaugeMetricSample) sample;
            Map<String, String> tags = gaugeSample.getTags();

            Assertions.assertEquals(gaugeSample.applyAsLong(), 2);
            Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationName);
        }
    }
}
