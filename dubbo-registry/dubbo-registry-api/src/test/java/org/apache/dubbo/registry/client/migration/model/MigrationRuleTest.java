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
package org.apache.dubbo.registry.client.migration.model;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.ServiceNameMapping;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_CLUSTER_TYPE_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MigrationRuleTest {
    private static final ServiceNameMapping mapping = mock(ServiceNameMapping.class);

    @Test
    void test_parse() {
        when(mapping.getMapping(any(URL.class))).thenReturn(Collections.emptySet());

        String rule = "key: demo-consumer\n" +
            "step: APPLICATION_FIRST\n" +
            "threshold: 1.0\n" +
            "proportion: 60\n" +
            "delay: 60\n" +
            "force: false\n" +
            "interfaces:\n" +
            "  - serviceKey: DemoService:1.0.0\n" +
            "    threshold: 0.5\n" +
            "    proportion: 30\n" +
            "    delay: 30\n" +
            "    force: true\n" +
            "    step: APPLICATION_FIRST\n" +
            "  - serviceKey: GreetingService:1.0.0\n" +
            "    step: FORCE_APPLICATION\n" +
            "applications:\n" +
            "  - serviceKey: TestApplication\n" +
            "    threshold: 0.3\n" +
            "    proportion: 20\n" +
            "    delay: 10\n" +
            "    force: false\n" +
            "    step: FORCE_INTERFACE\n";

        MigrationRule migrationRule = MigrationRule.parse(rule);
        assertEquals("demo-consumer", migrationRule.getKey());
        assertEquals(MigrationStep.APPLICATION_FIRST, migrationRule.getStep());
        assertEquals(1.0f, migrationRule.getThreshold());
        assertEquals(60, migrationRule.getProportion());
        assertEquals(60, migrationRule.getDelay());
        assertEquals(false, migrationRule.getForce());

        URL url = Mockito.mock(URL.class);
        ApplicationModel defaultModel = Mockito.spy(ApplicationModel.defaultModel());
        Mockito.when(defaultModel.getDefaultExtension(ServiceNameMapping.class)).thenReturn(mapping);

        Mockito.when(url.getScopeModel()).thenReturn(defaultModel);
        Mockito.when(url.getDisplayServiceKey()).thenReturn("DemoService:1.0.0");
        Mockito.when(url.getParameter(ArgumentMatchers.eq(REGISTRY_CLUSTER_TYPE_KEY), anyString())).thenReturn("default");
        Mockito.when(url.getParameter(ArgumentMatchers.eq(REGISTRY_CLUSTER_TYPE_KEY), anyString())).thenReturn("default");

        assertEquals(2, migrationRule.getInterfaces().size());

        assertEquals(0.5f, migrationRule.getThreshold(url));
        assertEquals(30, migrationRule.getProportion(url));
        assertEquals(30, migrationRule.getDelay(url));
        assertTrue(migrationRule.getForce(url));
        assertEquals(MigrationStep.APPLICATION_FIRST, migrationRule.getStep(url));

        Mockito.when(url.getDisplayServiceKey()).thenReturn("GreetingService:1.0.0");
        assertEquals(1.0f, migrationRule.getThreshold(url));
        assertEquals(60, migrationRule.getProportion(url));
        assertEquals(60, migrationRule.getDelay(url));
        assertFalse(migrationRule.getForce(url));
        assertEquals(MigrationStep.FORCE_APPLICATION, migrationRule.getStep(url));

        Mockito.when(url.getDisplayServiceKey()).thenReturn("GreetingService:1.0.1");
        Mockito.when(url.getServiceInterface()).thenReturn("GreetingService");
        when(mapping.getRemoteMapping(any(URL.class))).thenReturn(Collections.singleton("TestApplication"));

        Set<String> services = new HashSet<>();
        services.add("TestApplication");
        when(mapping.getMapping(any(URL.class))).thenReturn(services);
        assertEquals(0.3f, migrationRule.getThreshold(url));
        assertEquals(20, migrationRule.getProportion(url));
        assertEquals(10, migrationRule.getDelay(url));
        assertFalse(migrationRule.getForce(url));
        assertEquals(MigrationStep.FORCE_INTERFACE, migrationRule.getStep(url));
        when(mapping.getMapping(any(URL.class))).thenReturn(Collections.emptySet());

        ApplicationModel.defaultModel().destroy();
    }
}
