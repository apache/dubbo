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

import org.apache.dubbo.common.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.URL.valueOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ServiceDiscoveryFactory} Test
 *
 * @since 2.7.3
 */
public class ServiceDiscoveryFactoryTest {

    private static final URL dubboURL = valueOf("dubbo://localhost:20880");

    private static final URL inMemoryURL = valueOf("in-memory://localhost:12345");

    private ServiceDiscoveryFactory serviceDiscoveryFactory;

    @BeforeEach
    public void init() {
        serviceDiscoveryFactory = ServiceDiscoveryFactory.getDefaultExtension();
    }

    @Test
    public void testClass() {
        assertEquals(EventPublishingServiceDiscoveryFactory.class, serviceDiscoveryFactory.getClass());
    }

    @Test
    public void testCreate() {
        ServiceDiscovery serviceDiscovery = serviceDiscoveryFactory.create(inMemoryURL);
        assertEquals(EventPublishingServiceDiscovery.class, serviceDiscovery.getClass());
    }
}
