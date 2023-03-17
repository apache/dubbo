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

import org.apache.dubbo.metrics.metadata.collector.stat.MetadataStatComposite;
import org.apache.dubbo.metrics.metadata.event.MetadataEvent;
import org.apache.dubbo.metrics.model.container.LongContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.metrics.metadata.collector.stat.MetadataStatComposite.OP_TYPE_SUBSCRIBE;

public class MetadataStatCompositeTest {

    private final String applicationName = "app1";

    @Test
    void testInit() {
        MetadataStatComposite statComposite = new MetadataStatComposite();
        Assertions.assertEquals(statComposite.applicationNumStats.size(), MetadataEvent.ApplicationType.values().length);
        //(rt)5 * (push,subscribe,storeProvider)3
        Assertions.assertEquals(5 * 3, statComposite.appRtStats.size());
        statComposite.applicationNumStats.values().forEach((v ->
            Assertions.assertEquals(v, new ConcurrentHashMap<>())));
        statComposite.appRtStats.forEach(rtContainer ->
        {
            for (Map.Entry<String, ? extends Number> entry : rtContainer.entrySet()) {
                Assertions.assertEquals(0L, rtContainer.getValueSupplier().apply(entry.getKey()));
            }
        });
    }

    @Test
    void testIncrement() {
        MetadataStatComposite statComposite = new MetadataStatComposite();
        statComposite.increment(MetadataEvent.ApplicationType.P_TOTAL, applicationName);
        Assertions.assertEquals(1L, statComposite.applicationNumStats.get(MetadataEvent.ApplicationType.P_TOTAL).get(applicationName).get());
    }

    @Test
    void testCalcRt() {
        MetadataStatComposite statComposite = new MetadataStatComposite();
        statComposite.calcApplicationRt(applicationName, OP_TYPE_SUBSCRIBE, 10L);
        Assertions.assertTrue(statComposite.appRtStats.stream().anyMatch(longContainer -> longContainer.specifyType(OP_TYPE_SUBSCRIBE)));
        Optional<LongContainer<? extends Number>> subContainer = statComposite.appRtStats.stream().filter(longContainer -> longContainer.specifyType(OP_TYPE_SUBSCRIBE)).findFirst();
        subContainer.ifPresent(v -> Assertions.assertEquals(10L, v.get(applicationName).longValue()));
    }
}
