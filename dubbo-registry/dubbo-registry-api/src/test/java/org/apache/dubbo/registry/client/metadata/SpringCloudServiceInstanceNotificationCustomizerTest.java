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
package org.apache.dubbo.registry.client.metadata;

import org.apache.dubbo.common.ProtocolServiceKey;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SpringCloudServiceInstanceNotificationCustomizer} Test
 */
class SpringCloudServiceInstanceNotificationCustomizerTest {

    private SpringCloudServiceInstanceNotificationCustomizer customizer;
    private ApplicationModel applicationModel;

    @AfterEach
    public void clearUp() {
        applicationModel.destroy();
    }

    @BeforeEach
    public void setUp() {
        customizer = new SpringCloudServiceInstanceNotificationCustomizer();
        applicationModel = ApplicationModel.defaultModel();
    }

    @Test
    void testCustomizeWithEmptyList() {
        List<ServiceInstance> instances = Collections.emptyList();
        // no exception thrown
        Assertions.assertDoesNotThrow(() -> customizer.customize(instances));
    }

    @Test
    void testCustomizeSkipsNonSpringCloudInstances() {
        DefaultServiceInstance instance = createDubboInstance("app1", "192.168.1.1", 20880);
        List<ServiceInstance> instances = Collections.singletonList(instance);

        customizer.customize(instances);

        // Non-Spring Cloud instance should not have metadata set by customizer
        assertNull(instance.getServiceMetadata());
    }

    @Test
    void testCustomizeSetsMetadataForSpringCloudInstances() {
        DefaultServiceInstance instance = createSpringCloudInstance("app1", "192.168.1.1", 8080);
        List<ServiceInstance> instances = Collections.singletonList(instance);

        customizer.customize(instances);

        MetadataInfo metadata = instance.getServiceMetadata();
        assertNotNull(metadata);
        assertEquals("app1", metadata.getApp());
        assertTrue(metadata.getRevision().startsWith("SPRING_CLOUD-"));
        assertNotNull(metadata.getServices().get("*"));
        assertEquals("rest", metadata.getServices().get("*").getProtocol());
    }

    @Test
    void testCustomizeSkipsMixedInstances() {
        DefaultServiceInstance springCloudInstance = createSpringCloudInstance("app1", "192.168.1.1", 8080);
        DefaultServiceInstance dubboInstance = createDubboInstance("app1", "192.168.1.2", 20880);
        List<ServiceInstance> instances = Arrays.asList(springCloudInstance, dubboInstance);

        customizer.customize(instances);

        // Mixed instances: customizer should skip all (allMatch fails)
        assertNull(springCloudInstance.getServiceMetadata());
        assertNull(dubboInstance.getServiceMetadata());
    }

    @Test
    void testGetMatchedServiceInfos_consumerProtocolNull_returnsEmpty() {
        DefaultServiceInstance instance = createSpringCloudInstance("app1", "192.168.1.1", 8080);
        customizer.customize(Collections.singletonList(instance));

        MetadataInfo metadata = instance.getServiceMetadata();
        assertNotNull(metadata);

        // Consumer protocol is null (e.g., @DubboReference without explicit protocol)
        ProtocolServiceKey consumerKey = new ProtocolServiceKey("com.example.DemoService", null, null, null);

        List<MetadataInfo.ServiceInfo> matched = metadata.getMatchedServiceInfos(consumerKey);
        assertTrue(matched.isEmpty(), "Should return empty when consumer protocol is null");
    }

    @Test
    void testGetMatchedServiceInfos_consumerProtocolDubbo_returnsEmpty() {
        DefaultServiceInstance instance = createSpringCloudInstance("app1", "192.168.1.1", 8080);
        customizer.customize(Collections.singletonList(instance));

        MetadataInfo metadata = instance.getServiceMetadata();
        assertNotNull(metadata);

        // Consumer explicitly requests dubbo protocol
        ProtocolServiceKey consumerKey = new ProtocolServiceKey("com.example.DemoService", null, null, "dubbo");

        List<MetadataInfo.ServiceInfo> matched = metadata.getMatchedServiceInfos(consumerKey);
        assertTrue(matched.isEmpty(), "Should return empty when consumer protocol is dubbo");
    }

    @Test
    void testGetMatchedServiceInfos_consumerProtocolTriple_returnsEmpty() {
        DefaultServiceInstance instance = createSpringCloudInstance("app1", "192.168.1.1", 8080);
        customizer.customize(Collections.singletonList(instance));

        MetadataInfo metadata = instance.getServiceMetadata();

        // Consumer explicitly requests triple protocol
        ProtocolServiceKey consumerKey = new ProtocolServiceKey("com.example.DemoService", null, null, "tri");

        List<MetadataInfo.ServiceInfo> matched = metadata.getMatchedServiceInfos(consumerKey);
        assertTrue(matched.isEmpty(), "Should return empty when consumer protocol is tri");
    }

    @Test
    void testGetMatchedServiceInfos_consumerProtocolRest_returnsMatched() {
        DefaultServiceInstance instance = createSpringCloudInstance("app1", "192.168.1.1", 8080);
        customizer.customize(Collections.singletonList(instance));

        MetadataInfo metadata = instance.getServiceMetadata();
        assertNotNull(metadata);

        // Consumer explicitly requests REST protocol — should match
        ProtocolServiceKey consumerKey = new ProtocolServiceKey("com.example.DemoService", null, null, "rest");

        List<MetadataInfo.ServiceInfo> matched = metadata.getMatchedServiceInfos(consumerKey);
        assertEquals(1, matched.size(), "Should return matched service info when consumer protocol is rest");
        assertEquals("rest", matched.get(0).getProtocol());
    }

    @Test
    void testCustomizeMultipleSpringCloudInstances() {
        DefaultServiceInstance instance1 = createSpringCloudInstance("app1", "192.168.1.1", 8080);
        DefaultServiceInstance instance2 = createSpringCloudInstance("app1", "192.168.1.2", 8081);
        List<ServiceInstance> instances = Arrays.asList(instance1, instance2);

        customizer.customize(instances);

        // Both instances should have metadata set
        assertNotNull(instance1.getServiceMetadata());
        assertNotNull(instance2.getServiceMetadata());

        // Each instance should have a unique revision
        String revision1 = instance1.getServiceMetadata().getRevision();
        String revision2 = instance2.getServiceMetadata().getRevision();
        assertNotEquals(revision1, revision2, "Each instance should have a unique revision");
    }

    // --- Helper methods ---

    private DefaultServiceInstance createSpringCloudInstance(String serviceName, String host, int port) {
        DefaultServiceInstance instance = new DefaultServiceInstance();
        instance.setServiceName(serviceName);
        instance.setHost(host);
        instance.setPort(port);
        instance.setEnabled(true);
        instance.setHealthy(true);
        instance.setApplicationModel(applicationModel);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("preserved.register.source", "SPRING_CLOUD");
        instance.setMetadata(metadata);
        return instance;
    }

    private DefaultServiceInstance createDubboInstance(String serviceName, String host, int port) {
        DefaultServiceInstance instance = new DefaultServiceInstance();
        instance.setServiceName(serviceName);
        instance.setHost(host);
        instance.setPort(port);
        instance.setEnabled(true);
        instance.setHealthy(true);
        instance.setApplicationModel(applicationModel);
        instance.setMetadata(new HashMap<>());
        return instance;
    }
}
