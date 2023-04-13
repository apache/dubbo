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
import org.apache.dubbo.metrics.event.MetricsDispatcher;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.key.MetricsKey;
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
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER_SERVICE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_SUBSCRIBE_SERVICE;


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
    void testRegisterMetrics() {

        applicationModel.getBeanFactory().getOrRegisterBean(MetricsDispatcher.class);
        RegistryMetricsCollector collector = applicationModel.getBeanFactory().getOrRegisterBean(RegistryMetricsCollector.class);
        collector.setCollectEnabled(true);

        RegistryEvent registryEvent = RegistryEvent.toRegisterEvent(applicationModel);
        MetricsEventBus.post(registryEvent,
            () -> {
                List<MetricSample> metricSamples = collector.collect();
                // push success +1
                Assertions.assertEquals(1, metricSamples.size());
                Assertions.assertTrue(metricSamples.get(0) instanceof GaugeMetricSample);
                return null;
            }
        );

        // push finish rt +1
        List<MetricSample> metricSamples = collector.collect();
        //num(total+success) + rt(5) = 7
        Assertions.assertEquals(7, metricSamples.size());
        long c1 = registryEvent.getTimePair().calc();


        registryEvent = RegistryEvent.toRegisterEvent(applicationModel);
        TimePair lastTimePair = registryEvent.getTimePair();
        MetricsEventBus.post(registryEvent,
            () -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }, Objects::nonNull
        );
        // push error rt +1
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

        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_LAST, OP_TYPE_REGISTER).targetKey()), lastTimePair.calc());
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MIN, OP_TYPE_REGISTER).targetKey()), Math.min(c1, c2));
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MAX, OP_TYPE_REGISTER).targetKey()), Math.max(c1, c2));
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_AVG, OP_TYPE_REGISTER).targetKey()), (c1 + c2) / 2);
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_SUM, OP_TYPE_REGISTER).targetKey()), c1 + c2);
    }


    @Test
    void testServicePushMetrics() {

        applicationModel.getBeanFactory().getOrRegisterBean(MetricsDispatcher.class);
        RegistryMetricsCollector collector = applicationModel.getBeanFactory().getOrRegisterBean(RegistryMetricsCollector.class);
        collector.setCollectEnabled(true);
        String serviceName = "demo.gameService";

        RegistryEvent registryEvent = RegistryEvent.toRsEvent(applicationModel, serviceName, 2);
        MetricsEventBus.post(registryEvent,
            () -> {
                List<MetricSample> metricSamples = collector.collect();

                // push success +1
                Assertions.assertEquals(1, metricSamples.size());
                Assertions.assertTrue(metricSamples.get(0) instanceof GaugeMetricSample);
                Assertions.assertEquals(metricSamples.get(0).getName(), MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS.getName());
                Assertions.assertEquals(metricSamples.get(0).getTags().get("interface"), serviceName);
                return null;
            }
        );

        // push finish rt +1
        List<MetricSample> metricSamples = collector.collect();
        //num(total+success) + rt(5) = 7
        Assertions.assertEquals(7, metricSamples.size());

        long c1 = registryEvent.getTimePair().calc();
        registryEvent = RegistryEvent.toRsEvent(applicationModel, serviceName, 2);
        TimePair lastTimePair = registryEvent.getTimePair();
        MetricsEventBus.post(registryEvent,
            () -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }, Objects::nonNull
        );
        // push error rt +1
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

        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_LAST, OP_TYPE_REGISTER_SERVICE).targetKey()), lastTimePair.calc());
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MIN, OP_TYPE_REGISTER_SERVICE).targetKey()), Math.min(c1, c2));
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MAX, OP_TYPE_REGISTER_SERVICE).targetKey()), Math.max(c1, c2));
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_AVG, OP_TYPE_REGISTER_SERVICE).targetKey()), (c1 + c2) / 2);
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_SUM, OP_TYPE_REGISTER_SERVICE).targetKey()), c1 + c2);
    }


    @Test
    void testServiceSubscribeMetrics() {

        applicationModel.getBeanFactory().getOrRegisterBean(MetricsDispatcher.class);
        RegistryMetricsCollector collector = applicationModel.getBeanFactory().getOrRegisterBean(RegistryMetricsCollector.class);
        collector.setCollectEnabled(true);
        String serviceName = "demo.gameService";

        RegistryEvent subscribeEvent = RegistryEvent.toSsEvent(applicationModel, serviceName);
        MetricsEventBus.post(subscribeEvent,
            () -> {
                List<MetricSample> metricSamples = collector.collect();

                // push success +1
                Assertions.assertEquals(1, metricSamples.size());
                Assertions.assertTrue(metricSamples.get(0) instanceof GaugeMetricSample);
                Assertions.assertEquals(metricSamples.get(0).getName(), MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM.getName());
                Assertions.assertEquals(metricSamples.get(0).getTags().get("interface"), serviceName);
                return null;
            }
        );

        // push finish rt +1
        List<MetricSample> metricSamples = collector.collect();
        //num(total+success) + rt(5) = 7
        Assertions.assertEquals(7, metricSamples.size());

        long c1 = subscribeEvent.getTimePair().calc();
        subscribeEvent = RegistryEvent.toSsEvent(applicationModel, serviceName);
        TimePair lastTimePair = subscribeEvent.getTimePair();
        MetricsEventBus.post(subscribeEvent,
            () -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }, Objects::nonNull
        );
        // push error rt +1
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

        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_LAST, OP_TYPE_SUBSCRIBE_SERVICE).targetKey()), lastTimePair.calc());
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MIN, OP_TYPE_SUBSCRIBE_SERVICE).targetKey()), Math.min(c1, c2));
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MAX, OP_TYPE_SUBSCRIBE_SERVICE).targetKey()), Math.max(c1, c2));
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_AVG, OP_TYPE_SUBSCRIBE_SERVICE).targetKey()), (c1 + c2) / 2);
        Assertions.assertEquals(sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_SUM, OP_TYPE_SUBSCRIBE_SERVICE).targetKey()), c1 + c2);
    }
}
