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
package org.apache.dubbo.registry.eureka;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link EurekaServiceDiscovery} Test
 *
 * @since 2.7.5
 */
public class EurekaServiceDiscoveryTest {

    private EurekaServiceDiscovery serviceDiscovery;

    private ServiceInstance serviceInstance;

    private URL registryURL = URL.valueOf("eureka://127.0.0.1:" + NetUtils.getAvailablePort() + "/eureka");

    @BeforeEach
    public void init() throws Exception {
        serviceDiscovery = new EurekaServiceDiscovery();
        serviceDiscovery.initialize(registryURL);
        serviceInstance = new DefaultServiceInstance("test", "127.0.0.1", NetUtils.getAvailablePort());
        serviceDiscovery.register(serviceInstance);
    }

    @AfterEach
    public void destroy() throws Exception {
        serviceDiscovery.destroy();
    }

    @Test
    public void testGetServices() {
        assertNotNull(serviceDiscovery.getServices());
    }

    @Test
    public void testGetInstances() {
        serviceDiscovery.getServices().forEach(serviceName -> {
            assertNotNull(serviceDiscovery.getInstances(serviceName));
        });
    }
}
