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

package metrics.metrics.collector;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metrics.event.GlobalMetricsEventMulticaster;
import org.apache.dubbo.metrics.metadata.collector.MetadataMetricsCollector;
import org.apache.dubbo.metrics.metadata.event.MetadataEvent;
import org.apache.dubbo.metrics.model.TimePair;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class MetadataMetricsCollectorTest {

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
        MetadataMetricsCollector collector = applicationModel.getBeanFactory().getOrRegisterBean(MetadataMetricsCollector.class);
        eventMulticaster.addListener(collector);
        collector.setCollectEnabled(true);

        eventMulticaster.publishEvent(new MetadataEvent.PushEvent(applicationModel, timePair));

        List<MetricSample> metricSamples = collector.collect();

        // push success +1
        Assertions.assertEquals(metricSamples.size(), 1);
        Assertions.assertTrue(metricSamples.get(0) instanceof GaugeMetricSample);
        Assertions.assertEquals(metricSamples.get(0).getName(), "dubbo.metadata.push.num.total");

        eventMulticaster.publishFinishEvent(new MetadataEvent.PushEvent(applicationModel, timePair));

        // push finish rt +1
        metricSamples = collector.collect();

        //num(total+success) + rt(5) = 7
        Assertions.assertEquals(metricSamples.size(), 7);
        System.out.println(metricSamples);

        timePair = TimePair.start();
        eventMulticaster.publishEvent(new MetadataEvent.PushEvent(applicationModel, timePair));
        Thread.sleep(50);
        // push error rt +1
        eventMulticaster.publishErrorEvent(new MetadataEvent.PushEvent(applicationModel, timePair));
        metricSamples = collector.collect();

        //num(total+success+error) + rt(5)
        Assertions.assertEquals(metricSamples.size(), 8);
    }

//    @Test
//    void testRTMetrics() {
//        DefaultMetricsCollector collector = new DefaultMetricsCollector();
//        collector.setCollectEnabled(true);
//        MethodMetricsSampler methodMetricsCountSampler = collector.getMethodSampler();
//        String applicationName = applicationModel.getApplicationName();
//
//        collector.setApplicationName(applicationName);
//
//        methodMetricsCountSampler.addRT(invocation, 10L);
//        methodMetricsCountSampler.addRT(invocation, 0L);
//
//        List<MetricSample> samples = collector.collect();
//        for (MetricSample sample : samples) {
//            if (sample.getName().contains(DUBBO_THREAD_METRIC_MARK)) {
//                continue;
//            }
//            Map<String, String> tags = sample.getTags();
//
//            Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), interfaceName);
//            Assertions.assertEquals(tags.get(TAG_METHOD_KEY), methodName);
//            Assertions.assertEquals(tags.get(TAG_GROUP_KEY), group);
//            Assertions.assertEquals(tags.get(TAG_VERSION_KEY), version);
//        }
//        List<MetricSample> samples1 = new ArrayList<>();
//        for (MetricSample sample : samples) {
//            if (sample.getName().contains(DUBBO_THREAD_METRIC_MARK)) {
//                continue;
//            }
//            samples1.add(sample);
//        }
//        Map<String, Long> sampleMap = samples1.stream().collect(Collectors.toMap(MetricSample::getName, k -> {
//            Number number = ((GaugeMetricSample) k).getSupplier().get();
//            return number.longValue();
//        }));
//
//        Assertions.assertEquals(sampleMap.get(MetricsKey.PROVIDER_METRIC_RT_LAST.getName()), 0L);
//        Assertions.assertEquals(sampleMap.get(MetricsKey.PROVIDER_METRIC_RT_MIN.getName()), 0L);
//        Assertions.assertEquals(sampleMap.get(MetricsKey.PROVIDER_METRIC_RT_MAX.getName()), 10L);
//        Assertions.assertEquals(sampleMap.get(MetricsKey.PROVIDER_METRIC_RT_AVG.getName()), 5L);
//        Assertions.assertEquals(sampleMap.get(MetricsKey.PROVIDER_METRIC_RT_SUM.getName()), 10L);
//    }



}
