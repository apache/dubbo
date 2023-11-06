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

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metrics.data.ApplicationStatComposite;
import org.apache.dubbo.metrics.data.BaseStatComposite;
import org.apache.dubbo.metrics.data.RtStatComposite;
import org.apache.dubbo.metrics.data.ServiceStatComposite;
import org.apache.dubbo.metrics.model.ApplicationMetric;
import org.apache.dubbo.metrics.model.Metric;
import org.apache.dubbo.metrics.model.container.LongContainer;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.metrics.metadata.MetadataMetricsConstants.OP_TYPE_PUSH;
import static org.apache.dubbo.metrics.metadata.MetadataMetricsConstants.OP_TYPE_STORE_PROVIDER_INTERFACE;
import static org.apache.dubbo.metrics.metadata.MetadataMetricsConstants.OP_TYPE_SUBSCRIBE;

public class MetadataStatCompositeTest {
    private ApplicationModel applicationModel;
    private BaseStatComposite statComposite;

    @BeforeEach
    public void setup() {
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        applicationModel = frameworkModel.newApplication();
        ApplicationConfig application = new ApplicationConfig();
        application.setName("App1");
        applicationModel.getApplicationConfigManager().setApplication(application);
        statComposite = new BaseStatComposite(applicationModel) {
            @Override
            protected void init(ApplicationStatComposite applicationStatComposite) {
                super.init(applicationStatComposite);
                applicationStatComposite.init(MetadataMetricsConstants.APP_LEVEL_KEYS);
            }

            @Override
            protected void init(ServiceStatComposite serviceStatComposite) {
                super.init(serviceStatComposite);
                serviceStatComposite.initWrapper(MetadataMetricsConstants.SERVICE_LEVEL_KEYS);
            }

            @Override
            protected void init(RtStatComposite rtStatComposite) {
                super.init(rtStatComposite);
                rtStatComposite.init(OP_TYPE_PUSH, OP_TYPE_SUBSCRIBE, OP_TYPE_STORE_PROVIDER_INTERFACE);
            }
        };
    }

    @Test
    void testInit() {
        Assertions.assertEquals(
                statComposite
                        .getApplicationStatComposite()
                        .getApplicationNumStats()
                        .size(),
                MetadataMetricsConstants.APP_LEVEL_KEYS.size());

        // (rt)5 * (push,subscribe,service)3
        Assertions.assertEquals(
                5 * 3, statComposite.getRtStatComposite().getRtStats().size());
        statComposite
                .getApplicationStatComposite()
                .getApplicationNumStats()
                .values()
                .forEach((v -> Assertions.assertEquals(v.get(), new AtomicLong(0L).get())));
        statComposite.getRtStatComposite().getRtStats().forEach(rtContainer -> {
            for (Map.Entry<Metric, ? extends Number> entry : rtContainer.entrySet()) {
                Assertions.assertEquals(0L, rtContainer.getValueSupplier().apply(entry.getKey()));
            }
        });
    }

    @Test
    void testIncrement() {
        statComposite.incrementApp(MetricsKey.METADATA_PUSH_METRIC_NUM, 1);

        Assertions.assertEquals(
                1L,
                statComposite
                        .getApplicationStatComposite()
                        .getApplicationNumStats()
                        .get(MetricsKey.METADATA_PUSH_METRIC_NUM)
                        .get());
    }

    @Test
    void testCalcRt() {
        statComposite.calcApplicationRt(OP_TYPE_SUBSCRIBE.getType(), 10L);
        Assertions.assertTrue(statComposite.getRtStatComposite().getRtStats().stream()
                .anyMatch(longContainer -> longContainer.specifyType(OP_TYPE_SUBSCRIBE.getType())));
        Optional<LongContainer<? extends Number>> subContainer =
                statComposite.getRtStatComposite().getRtStats().stream()
                        .filter(longContainer -> longContainer.specifyType(OP_TYPE_SUBSCRIBE.getType()))
                        .findFirst();
        subContainer.ifPresent(v -> Assertions.assertEquals(
                10L, v.get(new ApplicationMetric(applicationModel)).longValue()));
    }
}
