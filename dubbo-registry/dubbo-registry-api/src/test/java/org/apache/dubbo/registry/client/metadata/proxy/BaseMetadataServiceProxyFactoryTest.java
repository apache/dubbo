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
package org.apache.dubbo.registry.client.metadata.proxy;

import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.valueOf;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link BaseMetadataServiceProxyFactory} Test-Cases
 *
 * @since 2.7.8
 */
public class BaseMetadataServiceProxyFactoryTest {

    private MyMetadataServiceProxyFactory factory;

    private DefaultServiceInstance instance;

    @BeforeEach
    public void init() {
        factory = new MyMetadataServiceProxyFactory();
        instance = createServiceInstance();
    }

    private DefaultServiceInstance createServiceInstance() {
        DefaultServiceInstance serviceInstance = new DefaultServiceInstance(valueOf(System.nanoTime()), "A", "127.0.0.1", 8080);
        Map<String, String> metadata = new HashMap<>();
        metadata.put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, "X");
        serviceInstance.setMetadata(metadata);
        return serviceInstance;
    }

    @Test
    public void testCreateProxyCacheKey() {
        assertEquals("A#X", factory.createProxyCacheKey(instance));
    }

    @Test
    public void testCreateProxy() {
        MetadataService metadataService = factory.createProxy(instance);
        MetadataService metadataService2 = factory.createProxy(instance);
        assertNotSame(metadataService, metadataService2);
    }

    @Test
    public void testGetProxy() {
        MetadataService metadataService = factory.getProxy(instance);
        MetadataService metadataService2 = factory.getProxy(instance);
        assertSame(metadataService, metadataService2);
    }

}
