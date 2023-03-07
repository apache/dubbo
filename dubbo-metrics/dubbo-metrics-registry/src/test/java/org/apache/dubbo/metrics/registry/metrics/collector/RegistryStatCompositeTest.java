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

import org.apache.dubbo.metrics.model.container.LongContainer;
import org.apache.dubbo.metrics.registry.collector.stat.RegistryStatComposite;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.metrics.registry.collector.stat.RegistryStatComposite.OP_TYPE_NOTIFY;

public class RegistryStatCompositeTest {

    private final String applicationName = "app1";

    @Test
    void testInit() {
        RegistryStatComposite statComposite = new RegistryStatComposite();
        Assertions.assertEquals(statComposite.numStats.size(), RegistryEvent.Type.values().length);
        //(rt)5 * (register,subscribe,notify)3
        Assertions.assertEquals(5 * 3, statComposite.rtStats.size());
        statComposite.numStats.values().forEach((v ->
            Assertions.assertEquals(v, new ConcurrentHashMap<>())));
        statComposite.rtStats.forEach(rtContainer ->
        {
            for (Map.Entry<String, ? extends Number> entry : rtContainer.entrySet()) {
                Assertions.assertEquals(0L, rtContainer.getValueSupplier().apply(entry.getKey()));
            }
        });
    }

    @Test
    void testIncrement() {
        RegistryStatComposite statComposite = new RegistryStatComposite();
        statComposite.increment(RegistryEvent.Type.R_TOTAL, applicationName);
        Assertions.assertEquals(1L, statComposite.numStats.get(RegistryEvent.Type.R_TOTAL).get(applicationName).get());
    }

    @Test
    void testCalcRt() {
        RegistryStatComposite statComposite = new RegistryStatComposite();
        statComposite.calcRt(applicationName, OP_TYPE_NOTIFY, 10L);
        Assertions.assertTrue(statComposite.rtStats.stream().anyMatch(longContainer -> longContainer.specifyType(OP_TYPE_NOTIFY)));
        Optional<LongContainer<? extends Number>> subContainer = statComposite.rtStats.stream().filter(longContainer -> longContainer.specifyType(OP_TYPE_NOTIFY)).findFirst();
        subContainer.ifPresent(v -> Assertions.assertEquals(10L, v.get(applicationName).longValue()));
    }
}
