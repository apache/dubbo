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
package org.apache.dubbo.registry.consul;

import com.pszymczyk.consul.ConsulProcess;
import com.pszymczyk.consul.ConsulStarterBuilder;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.valueOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConsulServiceDiscoveryTest {

    private static ConsulProcess consul;
    private URL url;
    static ConsulServiceDiscovery consulServiceDiscovery;
    private static final String SERVICE_NAME = "A";
    private static final String LOCALHOST = "127.0.0.1";

    @BeforeEach
    public void init() throws Exception {
        this.consul = ConsulStarterBuilder.consulStarter()
                .build()
                .start();
        url = URL.valueOf("consul://localhost:" + consul.getHttpPort());
        consulServiceDiscovery = new ConsulServiceDiscovery();
        Assertions.assertNull(consulServiceDiscovery.getServices());
        consulServiceDiscovery.initialize(url);
    }

    @AfterEach
    public void close() {
        consulServiceDiscovery.destroy();
        consul.close();
    }

    @Test
    public void testRegistration() throws InterruptedException{
        DefaultServiceInstance serviceInstance = createServiceInstance(SERVICE_NAME, LOCALHOST, 8012);
        consulServiceDiscovery.register(serviceInstance);
        Thread.sleep(5000);

        List<ServiceInstance> serviceInstances = consulServiceDiscovery.getInstances(SERVICE_NAME);
        assertEquals(serviceInstances.size(), 1);
        assertEquals(serviceInstances.get(0).getId(), Integer.toHexString(serviceInstance.hashCode()));
        assertEquals(serviceInstances.get(0).getHost(), serviceInstance.getHost());
        assertEquals(serviceInstances.get(0).getServiceName(), serviceInstance.getServiceName());
        assertEquals(serviceInstances.get(0).getPort(), serviceInstance.getPort());

        consulServiceDiscovery.unregister(serviceInstance);
        Thread.sleep(5000);
        serviceInstances = consulServiceDiscovery.getInstances(SERVICE_NAME);
        System.out.println(serviceInstances.size());
        assertTrue(serviceInstances.isEmpty());
    }

    private DefaultServiceInstance createServiceInstance(String serviceName, String host, int port) {
        return new DefaultServiceInstance(host + ":" + port, serviceName, host, port);
    }

    @Test
    public void testGetInstances() throws Exception {
        String serviceName = "ConsulTest77Service";
        assertTrue(consulServiceDiscovery.getInstances(serviceName).isEmpty());
        consulServiceDiscovery.register(new DefaultServiceInstance(valueOf(System.nanoTime()), serviceName, "127.0.0.1", 8080));
        consulServiceDiscovery.register(new DefaultServiceInstance(valueOf(System.nanoTime()), serviceName, "127.0.0.1", 9809));
        Thread.sleep(5000);
        Assertions.assertFalse(consulServiceDiscovery.getInstances(serviceName).isEmpty());
        List<String> r = convertToIpPort(consulServiceDiscovery.getInstances(serviceName));
        assertTrue(r.contains("127.0.0.1:8080"));
        assertTrue(r.contains("127.0.0.1:9809"));
    }

    private List<String> convertToIpPort(List<ServiceInstance> serviceInstances) {
        List<String> result = new ArrayList<>();
        for (ServiceInstance serviceInstance : serviceInstances) {
            result.add(serviceInstance.getHost() + ":" + serviceInstance.getPort());
        }
        return result;
    }
}