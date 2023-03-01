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
import org.apache.dubbo.metrics.event.GlobalMetricsEventMulticaster;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.TimePair;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
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
import static org.apache.dubbo.metrics.registry.collector.stat.RegistryStatComposite.OP_TYPE_REGISTER;


class RegistryMetricsCollectorTest {

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
    void testPushMetrics() throws InterruptedException {

        TimePair timePair = TimePair.start();
        GlobalMetricsEventMulticaster eventMulticaster = applicationModel.getBeanFactory().getOrRegisterBean(GlobalMetricsEventMulticaster.class);
        RegistryMetricsCollector collector = applicationModel.getBeanFactory().getOrRegisterBean(RegistryMetricsCollector.class);
        collector.setCollectEnabled(true);

        eventMulticaster.publishEvent(new RegistryEvent.MetricsRegisterEvent(applicationModel, timePair));
        List<MetricSample> metricSamples = collector.collect();

        // push success +1
        Assertions.assertEquals(1, metricSamples.size());
        Assertions.assertTrue(metricSamples.get(0) instanceof GaugeMetricSample);

        eventMulticaster.publishFinishEvent(new RegistryEvent.MetricsRegisterEvent(applicationModel, timePair));
        // push finish rt +1
        metricSamples = collector.collect();
        //num(total+success) + rt(5) = 7
        Assertions.assertEquals(7, metricSamples.size());
        long c1 = timePair.calc();
        TimePair lastTimePair = TimePair.start();
        eventMulticaster.publishEvent(new RegistryEvent.MetricsRegisterEvent(applicationModel, lastTimePair));
        Thread.sleep(50);
        // push error rt +1
        eventMulticaster.publishErrorEvent(new RegistryEvent.MetricsRegisterEvent(applicationModel, lastTimePair));
        long c2 = lastTimePair.calc();
        metricSamples = collector.collect();

        // num(total+success+error) + rt(5)
        Assertions.assertEquals(8, metricSamples.size());

        // calc rt
        for (MetricSample sample : metricSamples) {
            Map<String, String> tags = sample.getTags();
            Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationModel.getApplicationName());
        }

        @SuppressWarnings("rawtypes")
        Map<String, Long> sampleMap = metricSamples.stream().collect(Collectors.toMap(MetricSample::getName, k -> ((GaugeMetricSample) k).applyAsLong()));

        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(OP_TYPE_REGISTER, MetricsKey.METRIC_RT_LAST).targetKey()), lastTimePair.calc());
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(OP_TYPE_REGISTER, MetricsKey.METRIC_RT_MIN).targetKey()), Math.min(c1, c2));
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(OP_TYPE_REGISTER, MetricsKey.METRIC_RT_MAX).targetKey()), Math.max(c1, c2));
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(OP_TYPE_REGISTER, MetricsKey.METRIC_RT_AVG).targetKey()), (c1 + c2) / 2);
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(OP_TYPE_REGISTER, MetricsKey.METRIC_RT_SUM).targetKey()), c1 + c2);
    }


}
