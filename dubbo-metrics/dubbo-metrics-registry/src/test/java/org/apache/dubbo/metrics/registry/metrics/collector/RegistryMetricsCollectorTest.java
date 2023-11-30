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
import org.apache.dubbo.metrics.model.TimePair;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.registry.RegistryMetricsConstants;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.APP_LEVEL_KEYS;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER_SERVICE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_SUBSCRIBE_SERVICE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.REGISTER_LEVEL_KEYS;

class RegistryMetricsCollectorTest {

    private ApplicationModel applicationModel;
    private RegistryMetricsCollector collector;

    @BeforeEach
    public void setup() {
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        applicationModel = frameworkModel.newApplication();
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");

        applicationModel.getApplicationConfigManager().setApplication(config);
        applicationModel.getBeanFactory().getOrRegisterBean(MetricsDispatcher.class);
        collector = applicationModel.getBeanFactory().getOrRegisterBean(RegistryMetricsCollector.class);
        collector.setCollectEnabled(true);
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    void testRegisterMetrics() {

        RegistryEvent registryEvent = RegistryEvent.toRegisterEvent(applicationModel, Lists.newArrayList("reg1"));
        MetricsEventBus.post(registryEvent, () -> {
            List<MetricSample> metricSamples = collector.collect();
            // push success +1 -> other default 0 = APP_LEVEL_KEYS.size()
            Assertions.assertEquals(APP_LEVEL_KEYS.size() + REGISTER_LEVEL_KEYS.size(), metricSamples.size());
            Assertions.assertTrue(
                    metricSamples.stream().allMatch(metricSample -> metricSample instanceof GaugeMetricSample));
            Assertions.assertTrue(metricSamples.stream()
                    .anyMatch(metricSample -> ((GaugeMetricSample) metricSample).applyAsDouble() == 1));
            return null;
        });

        // push finish rt +1
        List<MetricSample> metricSamples = collector.collect();
        // APP_LEVEL_KEYS.size() + rt(5) = 12
        Assertions.assertEquals(APP_LEVEL_KEYS.size() + REGISTER_LEVEL_KEYS.size() + 5, metricSamples.size());
        long c1 = registryEvent.getTimePair().calc();

        registryEvent = RegistryEvent.toRegisterEvent(applicationModel, Lists.newArrayList("reg1"));
        TimePair lastTimePair = registryEvent.getTimePair();
        MetricsEventBus.post(
                registryEvent,
                () -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return null;
                },
                Objects::nonNull);
        // push error rt +1
        long c2 = lastTimePair.calc();

        metricSamples = collector.collect();

        // num(total+success+error) + rt(5)
        Assertions.assertEquals(APP_LEVEL_KEYS.size() + REGISTER_LEVEL_KEYS.size() + 5, metricSamples.size());

        // calc rt
        for (MetricSample sample : metricSamples) {
            Map<String, String> tags = sample.getTags();
            Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationModel.getApplicationName());
        }

        @SuppressWarnings("rawtypes")
        Map<String, Long> sampleMap = metricSamples.stream()
                .collect(Collectors.toMap(MetricSample::getName, k -> ((GaugeMetricSample) k).applyAsLong()));

        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_LAST, OP_TYPE_REGISTER).targetKey()),
                lastTimePair.calc());
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MIN, OP_TYPE_REGISTER).targetKey()),
                Math.min(c1, c2));
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MAX, OP_TYPE_REGISTER).targetKey()),
                Math.max(c1, c2));
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_AVG, OP_TYPE_REGISTER).targetKey()),
                (c1 + c2) / 2);
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_SUM, OP_TYPE_REGISTER).targetKey()), c1 + c2);
    }

    @Test
    void testServicePushMetrics() {

        String serviceName = "demo.gameService";
        List<String> rcNames = Lists.newArrayList("demo1");

        RegistryEvent registryEvent = RegistryEvent.toRsEvent(applicationModel, serviceName, 2, rcNames);
        MetricsEventBus.post(registryEvent, () -> {
            List<MetricSample> metricSamples = collector.collect();

            // push success +1
            Assertions.assertEquals(RegistryMetricsConstants.APP_LEVEL_KEYS.size() + 1, metricSamples.size());
            // Service num only 1 and contains tag of interface
            Assertions.assertEquals(
                    1,
                    metricSamples.stream()
                            .filter(metricSample ->
                                    serviceName.equals(metricSample.getTags().get("interface")))
                            .count());
            return null;
        });

        // push finish rt +1
        List<MetricSample> metricSamples = collector.collect();
        // App(7) + rt(5) + service(total/success) = 14
        Assertions.assertEquals(RegistryMetricsConstants.APP_LEVEL_KEYS.size() + 5 + 2, metricSamples.size());

        long c1 = registryEvent.getTimePair().calc();
        registryEvent = RegistryEvent.toRsEvent(applicationModel, serviceName, 2, rcNames);
        TimePair lastTimePair = registryEvent.getTimePair();
        MetricsEventBus.post(
                registryEvent,
                () -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return null;
                },
                Objects::nonNull);
        // push error rt +1
        long c2 = lastTimePair.calc();

        metricSamples = collector.collect();

        // App(7) + rt(5) + service(total/success/failed) = 15
        Assertions.assertEquals(RegistryMetricsConstants.APP_LEVEL_KEYS.size() + 5 + 3, metricSamples.size());

        // calc rt
        for (MetricSample sample : metricSamples) {
            Map<String, String> tags = sample.getTags();
            Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationModel.getApplicationName());
        }

        @SuppressWarnings("rawtypes")
        Map<String, Long> sampleMap = metricSamples.stream()
                .collect(Collectors.toMap(MetricSample::getName, k -> ((GaugeMetricSample) k).applyAsLong()));

        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_LAST, OP_TYPE_REGISTER_SERVICE).targetKey()),
                lastTimePair.calc());
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MIN, OP_TYPE_REGISTER_SERVICE).targetKey()),
                Math.min(c1, c2));
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MAX, OP_TYPE_REGISTER_SERVICE).targetKey()),
                Math.max(c1, c2));
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_AVG, OP_TYPE_REGISTER_SERVICE).targetKey()),
                (c1 + c2) / 2);
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_SUM, OP_TYPE_REGISTER_SERVICE).targetKey()),
                c1 + c2);
    }

    @Test
    void testServiceSubscribeMetrics() {

        String serviceName = "demo.gameService";

        RegistryEvent subscribeEvent =
                RegistryEvent.toSsEvent(applicationModel, serviceName, Collections.singletonList("demo1"));
        MetricsEventBus.post(subscribeEvent, () -> {
            List<MetricSample> metricSamples = collector.collect();
            Assertions.assertTrue(
                    metricSamples.stream().allMatch(metricSample -> metricSample instanceof GaugeMetricSample));
            Assertions.assertTrue(metricSamples.stream()
                    .anyMatch(metricSample -> ((GaugeMetricSample) metricSample).applyAsDouble() == 1));
            // App(default=7) + (service success +1)
            Assertions.assertEquals(RegistryMetricsConstants.APP_LEVEL_KEYS.size() + 1, metricSamples.size());
            // Service num only 1 and contains tag of interface
            Assertions.assertEquals(
                    1,
                    metricSamples.stream()
                            .filter(metricSample ->
                                    serviceName.equals(metricSample.getTags().get("interface")))
                            .count());
            return null;
        });

        // push finish rt +1
        List<MetricSample> metricSamples = collector.collect();
        // App(7) + rt(5) + service(total/success) = 14
        Assertions.assertEquals(RegistryMetricsConstants.APP_LEVEL_KEYS.size() + 5 + 2, metricSamples.size());

        long c1 = subscribeEvent.getTimePair().calc();
        subscribeEvent = RegistryEvent.toSsEvent(applicationModel, serviceName, Collections.singletonList("demo1"));
        TimePair lastTimePair = subscribeEvent.getTimePair();
        MetricsEventBus.post(
                subscribeEvent,
                () -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return null;
                },
                Objects::nonNull);
        // push error rt +1
        long c2 = lastTimePair.calc();

        metricSamples = collector.collect();

        // App(7) + rt(5) + service(total/success/failed) = 15
        Assertions.assertEquals(RegistryMetricsConstants.APP_LEVEL_KEYS.size() + 5 + 3, metricSamples.size());

        // calc rt
        for (MetricSample sample : metricSamples) {
            Map<String, String> tags = sample.getTags();
            Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationModel.getApplicationName());
        }

        @SuppressWarnings("rawtypes")
        Map<String, Long> sampleMap = metricSamples.stream()
                .collect(Collectors.toMap(MetricSample::getName, k -> ((GaugeMetricSample) k).applyAsLong()));

        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_LAST, OP_TYPE_SUBSCRIBE_SERVICE).targetKey()),
                lastTimePair.calc());
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MIN, OP_TYPE_SUBSCRIBE_SERVICE).targetKey()),
                Math.min(c1, c2));
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MAX, OP_TYPE_SUBSCRIBE_SERVICE).targetKey()),
                Math.max(c1, c2));
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_AVG, OP_TYPE_SUBSCRIBE_SERVICE).targetKey()),
                (c1 + c2) / 2);
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_SUM, OP_TYPE_SUBSCRIBE_SERVICE).targetKey()),
                c1 + c2);
    }

    @Test
    public void testNotify() {
        Map<String, Integer> lastNumMap = new HashMap<>();
        MetricsEventBus.post(RegistryEvent.toNotifyEvent(applicationModel), () -> {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 1 different services
            lastNumMap.put("demo.service1", 3);
            lastNumMap.put("demo.service2", 4);
            lastNumMap.put("demo.service3", 5);
            return lastNumMap;
        });
        List<MetricSample> metricSamples = collector.collect();
        // App(7) + num(service*3) + rt(5) = 9
        Assertions.assertEquals((RegistryMetricsConstants.APP_LEVEL_KEYS.size() + 3 + 5), metricSamples.size());
    }
}
