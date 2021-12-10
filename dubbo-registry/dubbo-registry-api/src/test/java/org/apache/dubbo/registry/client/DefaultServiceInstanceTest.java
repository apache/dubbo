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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_STORAGE_TYPE_PROPERTY_NAME;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getEndpoint;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.setEndpoints;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * {@link DefaultServiceInstance} Test
 *
 * @since 2.7.5
 */
public class DefaultServiceInstanceTest {

    public DefaultServiceInstance instance;

    public static DefaultServiceInstance createInstance() {
        DefaultServiceInstance instance = new DefaultServiceInstance("A", "127.0.0.1", 20880, ApplicationModel.defaultModel());
        Map<String, String> metadata = instance.getMetadata();
        metadata.put(METADATA_STORAGE_TYPE_PROPERTY_NAME, "remote");
        metadata.put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, "111");
        metadata.put("site", "dubbo");

        Map<String, Integer> protocolPorts = new HashMap<>();
        protocolPorts.put("rest", 8080);
        protocolPorts.put("dubbo", 20880);
        setEndpoints(instance, protocolPorts);
        return instance;
    }

    @BeforeEach
    public void init() {
        instance = createInstance();
    }

    @Test
    public void testSetAndGetValues() {
        instance.setEnabled(false);
        instance.setHealthy(false);

        assertEquals("A", instance.getServiceName());
        assertEquals("127.0.0.1", instance.getHost());
        assertEquals(20880, instance.getPort());
        assertFalse(instance.isEnabled());
        assertFalse(instance.isHealthy());
        assertFalse(instance.getMetadata().isEmpty());
    }

    @Test
    public void testInstanceOperations() {
        // test multiple protocols
        assertEquals(2, instance.getEndpoints().size());
        DefaultServiceInstance.Endpoint endpoint = getEndpoint(instance, "rest");
        DefaultServiceInstance copyInstance = instance.copyFrom(endpoint);
        assertEquals(8080, endpoint.getPort());
        assertEquals("rest", endpoint.getProtocol());
        assertEquals(endpoint.getPort(), copyInstance.getPort());

        // test all params
        Map<String, String> allParams = instance.getAllParams();
        assertEquals(instance.getMetadata().size(), allParams.size());
        assertEquals("dubbo", allParams.get("site"));
        instance.putExtendParam("key", "value");
        Map<String, String> allParams2 = instance.getAllParams();
        assertNotSame(allParams, allParams2);
        assertEquals(instance.getMetadata().size() + instance.getExtendParams().size(), allParams2.size());
        assertEquals("value", allParams2.get("key"));

        // test equals
        DefaultServiceInstance instance2 = new DefaultServiceInstance("A", "127.0.0.1", 20880, ApplicationModel.defaultModel());
        instance2.setMetadata(new HashMap<>(instance.getMetadata()));
        instance2.getMetadata().put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, "222");
        // assert instances with different revision and extend params are equal
        assertEquals(instance, instance2);
    }
}
