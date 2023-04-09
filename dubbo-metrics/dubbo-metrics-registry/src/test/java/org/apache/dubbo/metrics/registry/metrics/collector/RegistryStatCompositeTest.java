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

import org.apache.dubbo.metrics.data.ApplicationStatComposite;
import org.apache.dubbo.metrics.data.BaseStatComposite;
import org.apache.dubbo.metrics.data.RtStatComposite;
import org.apache.dubbo.metrics.data.ServiceStatComposite;
import org.apache.dubbo.metrics.model.container.LongContainer;
import org.apache.dubbo.metrics.registry.RegistryMetricsConstants;
import org.apache.dubbo.metrics.registry.event.type.ApplicationType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_NOTIFY;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER_SERVICE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_SUBSCRIBE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_SUBSCRIBE_SERVICE;

public class RegistryStatCompositeTest {

    private final String applicationName = "app1";
    private final BaseStatComposite statComposite = new BaseStatComposite() {
        @Override
        protected void init(ApplicationStatComposite applicationStatComposite, ServiceStatComposite serviceStatComposite, RtStatComposite rtStatComposite) {
            applicationStatComposite.init(RegistryMetricsConstants.appKeys);
            serviceStatComposite.init(RegistryMetricsConstants.serviceKeys);
            rtStatComposite.init(OP_TYPE_REGISTER, OP_TYPE_SUBSCRIBE, OP_TYPE_NOTIFY, OP_TYPE_REGISTER_SERVICE, OP_TYPE_SUBSCRIBE_SERVICE);
        }
    };

    @Test
    void testInit() {
        Assertions.assertEquals(statComposite.getApplicationStatComposite().getApplicationNumStats().size(), ApplicationType.values().length);
        //(rt)5 * (register,subscribe,notify,register.service,subscribe.service)5
        Assertions.assertEquals(5 * 5, statComposite.getRtStatComposite().getRtStats().size());
        statComposite.getApplicationStatComposite().getApplicationNumStats().values().forEach((v ->
            Assertions.assertEquals(v, new ConcurrentHashMap<>())));
        statComposite.getRtStatComposite().getRtStats().forEach(rtContainer ->
        {
            for (Map.Entry<String, ? extends Number> entry : rtContainer.entrySet()) {
                Assertions.assertEquals(0L, rtContainer.getValueSupplier().apply(entry.getKey()));
            }
        });
    }

    @Test
    void testIncrement() {
        statComposite.incrementApp(ApplicationType.R_TOTAL.getMetricsKey(), applicationName, 1);
        Assertions.assertEquals(1L, statComposite.getApplicationStatComposite().getApplicationNumStats().get(ApplicationType.R_TOTAL.getMetricsKey()).get(applicationName).get());
    }

    @Test
    void testCalcRt() {
        statComposite.calcApplicationRt(applicationName, OP_TYPE_NOTIFY.getType(), 10L);
        Assertions.assertTrue(statComposite.getRtStatComposite().getRtStats().stream().anyMatch(longContainer -> longContainer.specifyType(OP_TYPE_NOTIFY.getType())));
        Optional<LongContainer<? extends Number>> subContainer = statComposite.getRtStatComposite().getRtStats().stream().filter(longContainer -> longContainer.specifyType(OP_TYPE_NOTIFY.getType())).findFirst();
        subContainer.ifPresent(v -> Assertions.assertEquals(10L, v.get(applicationName).longValue()));
    }
}
