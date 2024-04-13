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
package org.apache.dubbo.metrics.metadata;

import org.apache.dubbo.common.event.DubboEvent;
import org.apache.dubbo.common.event.DubboEventBus;
import org.apache.dubbo.common.utils.TimePair;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.event.MetaDataPushEvent;
import org.apache.dubbo.metadata.event.MetaDataServiceSubscribeEvent;
import org.apache.dubbo.metadata.event.MetaDataSubscribeEvent;
import org.apache.dubbo.metrics.metadata.collector.MetadataMetricsCollector;
import org.apache.dubbo.metrics.metadata.event.MetadataSubDispatcher;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.metrics.metadata.MetadataMetricsConstants.OP_TYPE_PUSH;
import static org.apache.dubbo.metrics.metadata.MetadataMetricsConstants.OP_TYPE_STORE_PROVIDER_INTERFACE;
import static org.apache.dubbo.metrics.metadata.MetadataMetricsConstants.OP_TYPE_SUBSCRIBE;

class MetadataMetricsCollectorTest {

    private ApplicationModel applicationModel;

    private MetadataMetricsCollector collector;

    @BeforeEach
    public void setup() {
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        applicationModel = frameworkModel.newApplication();
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");

        applicationModel.getApplicationConfigManager().setApplication(config);

        collector = applicationModel.getBeanFactory().getOrRegisterBean(MetadataMetricsCollector.class);
        collector.setCollectEnabled(true);
    }

    @Test
    void testListener() {
        MetadataSubDispatcher dispatcher = new MetadataSubDispatcher(collector);
        MetaDataPushEvent event = new MetaDataPushEvent(applicationModel);
        MetaDataSubscribeEvent subscribeEvent = new MetaDataSubscribeEvent(applicationModel);
        MetaDataServiceSubscribeEvent serviceSubscribeEvent =
                new MetaDataServiceSubscribeEvent(applicationModel, "serviceKey");

        DubboEvent otherEvent = new DubboEvent(applicationModel);
        Assertions.assertTrue(dispatcher.support(event.getClass()));
        Assertions.assertTrue(dispatcher.support(subscribeEvent.getClass()));
        Assertions.assertTrue(dispatcher.support(serviceSubscribeEvent.getClass()));
        Assertions.assertFalse(dispatcher.support(otherEvent.getClass()));
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    void testPushMetrics() {
        MetaDataPushEvent pushEvent = new MetaDataPushEvent(applicationModel);
        DubboEventBus.post(pushEvent, () -> {
            List<MetricSample> metricSamples = collector.collect();

            // push success +1
            Assertions.assertEquals(MetadataMetricsConstants.APP_LEVEL_KEYS.size(), metricSamples.size());
            Assertions.assertTrue(
                    metricSamples.stream().allMatch(metricSample -> metricSample instanceof GaugeMetricSample));
            Assertions.assertTrue(metricSamples.stream()
                    .anyMatch(metricSample -> ((GaugeMetricSample) metricSample).applyAsDouble() == 1));
            return null;
        });

        // push finish rt +1
        List<MetricSample> metricSamples = collector.collect();
        // App(6) + rt(5) = 7
        Assertions.assertEquals(MetadataMetricsConstants.APP_LEVEL_KEYS.size() + 5, metricSamples.size());
        long c1 = pushEvent.getTimePair().calc();

        pushEvent = new MetaDataPushEvent(applicationModel);
        TimePair lastTimePair = pushEvent.getTimePair();
        DubboEventBus.post(
                pushEvent,
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

        // App(6) + rt(5)
        Assertions.assertEquals(MetadataMetricsConstants.APP_LEVEL_KEYS.size() + 5, metricSamples.size());

        // calc rt
        for (MetricSample sample : metricSamples) {
            Map<String, String> tags = sample.getTags();
            Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationModel.getApplicationName());
        }

        @SuppressWarnings("rawtypes")
        Map<String, Long> sampleMap = metricSamples.stream()
                .collect(Collectors.toMap(MetricSample::getName, k -> ((GaugeMetricSample) k).applyAsLong()));

        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_LAST, OP_TYPE_PUSH).targetKey()),
                lastTimePair.calc());
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MIN, OP_TYPE_PUSH).targetKey()),
                Math.min(c1, c2));
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MAX, OP_TYPE_PUSH).targetKey()),
                Math.max(c1, c2));
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_AVG, OP_TYPE_PUSH).targetKey()),
                (c1 + c2) / 2);
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_SUM, OP_TYPE_PUSH).targetKey()), c1 + c2);
    }

    @Test
    void testSubscribeMetrics() {
        //        MetadataMetricsCollector collector = getCollector();

        MetaDataSubscribeEvent subscribeEvent = new MetaDataSubscribeEvent(applicationModel);
        DubboEventBus.post(subscribeEvent, () -> {
            List<MetricSample> metricSamples = collector.collect();

            // push success +1
            Assertions.assertEquals(MetadataMetricsConstants.APP_LEVEL_KEYS.size(), metricSamples.size());
            Assertions.assertTrue(
                    metricSamples.stream().allMatch(metricSample -> metricSample instanceof GaugeMetricSample));
            Assertions.assertTrue(metricSamples.stream()
                    .anyMatch(metricSample -> ((GaugeMetricSample) metricSample).applyAsDouble() == 1));
            return null;
        });
        long c1 = subscribeEvent.getTimePair().calc();

        // push finish rt +1
        List<MetricSample> metricSamples = collector.collect();
        // App(6) + rt(5) = 7
        Assertions.assertEquals(MetadataMetricsConstants.APP_LEVEL_KEYS.size() + 5, metricSamples.size());

        subscribeEvent = new MetaDataSubscribeEvent(applicationModel);
        TimePair lastTimePair = subscribeEvent.getTimePair();
        DubboEventBus.post(
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

        // App(6) + rt(5)
        Assertions.assertEquals(MetadataMetricsConstants.APP_LEVEL_KEYS.size() + 5, metricSamples.size());

        // calc rt
        for (MetricSample sample : metricSamples) {
            Map<String, String> tags = sample.getTags();
            Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationModel.getApplicationName());
        }

        @SuppressWarnings("rawtypes")
        Map<String, Long> sampleMap = metricSamples.stream()
                .collect(Collectors.toMap(MetricSample::getName, k -> ((GaugeMetricSample) k).applyAsLong()));

        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_LAST, OP_TYPE_SUBSCRIBE).targetKey()),
                lastTimePair.calc());
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MIN, OP_TYPE_SUBSCRIBE).targetKey()),
                Math.min(c1, c2));
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MAX, OP_TYPE_SUBSCRIBE).targetKey()),
                Math.max(c1, c2));
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_AVG, OP_TYPE_SUBSCRIBE).targetKey()),
                (c1 + c2) / 2);
        Assertions.assertEquals(
                sampleMap.get(new MetricsKeyWrapper(MetricsKey.METRIC_RT_SUM, OP_TYPE_SUBSCRIBE).targetKey()), c1 + c2);
    }

    @Test
    void testStoreProviderMetadataMetrics() {
        //        MetadataMetricsCollector collector = getCollector();

        String serviceKey = "store.provider.test";
        MetaDataServiceSubscribeEvent metadataEvent = new MetaDataServiceSubscribeEvent(applicationModel, serviceKey);
        DubboEventBus.post(metadataEvent, () -> {
            List<MetricSample> metricSamples = collector.collect();

            // App(6) + service success(1)
            Assertions.assertEquals(MetadataMetricsConstants.APP_LEVEL_KEYS.size() + 1, metricSamples.size());
            Assertions.assertTrue(
                    metricSamples.stream().allMatch(metricSample -> metricSample instanceof GaugeMetricSample));
            Assertions.assertTrue(metricSamples.stream()
                    .anyMatch(metricSample -> ((GaugeMetricSample) metricSample).applyAsDouble() == 1));
            return null;
        });

        // push finish rt +1
        List<MetricSample> metricSamples = collector.collect();
        // App(6) + service total/success(2) + rt(5) = 7
        Assertions.assertEquals(MetadataMetricsConstants.APP_LEVEL_KEYS.size() + 2 + 5, metricSamples.size());

        long c1 = metadataEvent.getTimePair().calc();
        metadataEvent = new MetaDataServiceSubscribeEvent(applicationModel, serviceKey);
        TimePair lastTimePair = metadataEvent.getTimePair();
        DubboEventBus.post(
                metadataEvent,
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

        // App(6) + service total/success/failed(3) + rt(5)
        Assertions.assertEquals(MetadataMetricsConstants.APP_LEVEL_KEYS.size() + 3 + 5, metricSamples.size());

        // calc rt
        for (MetricSample sample : metricSamples) {
            Map<String, String> tags = sample.getTags();
            Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationModel.getApplicationName());
        }

        @SuppressWarnings("rawtypes")
        Map<String, Long> sampleMap = metricSamples.stream()
                .collect(Collectors.toMap(MetricSample::getName, k -> ((GaugeMetricSample) k).applyAsLong()));

        Assertions.assertEquals(
                sampleMap.get(
                        new MetricsKeyWrapper(MetricsKey.METRIC_RT_LAST, OP_TYPE_STORE_PROVIDER_INTERFACE).targetKey()),
                lastTimePair.calc());
        Assertions.assertEquals(
                sampleMap.get(
                        new MetricsKeyWrapper(MetricsKey.METRIC_RT_MIN, OP_TYPE_STORE_PROVIDER_INTERFACE).targetKey()),
                Math.min(c1, c2));
        Assertions.assertEquals(
                sampleMap.get(
                        new MetricsKeyWrapper(MetricsKey.METRIC_RT_MAX, OP_TYPE_STORE_PROVIDER_INTERFACE).targetKey()),
                Math.max(c1, c2));
        Assertions.assertEquals(
                sampleMap.get(
                        new MetricsKeyWrapper(MetricsKey.METRIC_RT_AVG, OP_TYPE_STORE_PROVIDER_INTERFACE).targetKey()),
                (c1 + c2) / 2);
        Assertions.assertEquals(
                sampleMap.get(
                        new MetricsKeyWrapper(MetricsKey.METRIC_RT_SUM, OP_TYPE_STORE_PROVIDER_INTERFACE).targetKey()),
                c1 + c2);
    }

    @Test
    void testMetadataPushNum() {

        for (int i = 0; i < 10; i++) {
            MetaDataPushEvent event = new MetaDataPushEvent(applicationModel);
            if (i % 2 == 0) {
                DubboEventBus.post(event, () -> true, r -> r);
            } else {
                DubboEventBus.post(event, () -> false, r -> r);
            }
        }

        List<MetricSample> samples = collector.collect();

        GaugeMetricSample<?> totalNum = getSample(MetricsKey.METADATA_PUSH_METRIC_NUM.getName(), samples);
        GaugeMetricSample<?> succeedNum = getSample(MetricsKey.METADATA_PUSH_METRIC_NUM_SUCCEED.getName(), samples);
        GaugeMetricSample<?> failedNum = getSample(MetricsKey.METADATA_PUSH_METRIC_NUM_FAILED.getName(), samples);

        Assertions.assertEquals(10, totalNum.applyAsLong());
        Assertions.assertEquals(5, succeedNum.applyAsLong());
        Assertions.assertEquals(5, failedNum.applyAsLong());
    }

    @Test
    void testSubscribeSum() {

        for (int i = 0; i < 10; i++) {
            MetaDataSubscribeEvent event = new MetaDataSubscribeEvent(applicationModel);
            if (i % 2 == 0) {
                DubboEventBus.post(event, () -> true, r -> r);
            } else {
                DubboEventBus.post(event, () -> false, r -> r);
            }
        }

        List<MetricSample> samples = collector.collect();

        GaugeMetricSample<?> totalNum = getSample(MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM.getName(), samples);
        GaugeMetricSample<?> succeedNum =
                getSample(MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM_SUCCEED.getName(), samples);
        GaugeMetricSample<?> failedNum = getSample(MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM_FAILED.getName(), samples);

        Assertions.assertEquals(10, totalNum.applyAsLong());
        Assertions.assertEquals(5, succeedNum.applyAsLong());
        Assertions.assertEquals(5, failedNum.applyAsLong());
    }

    GaugeMetricSample<?> getSample(String name, List<MetricSample> samples) {
        return (GaugeMetricSample<?>) samples.stream()
                .filter(metricSample -> metricSample.getName().equals(name))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
    }
}
