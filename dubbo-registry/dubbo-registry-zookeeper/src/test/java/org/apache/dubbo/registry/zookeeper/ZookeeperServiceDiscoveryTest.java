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
package org.apache.dubbo.registry.zookeeper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.mockito.internal.util.collections.Sets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ZookeeperServiceDiscovery} Test
 *
 * @since 2.7.5
 */
@DisabledForJreRange(min = JRE.JAVA_16)
public class ZookeeperServiceDiscoveryTest {

    private static final String SERVICE_NAME = "A";

    private static final String LOCALHOST = "127.0.0.1";

    private URL registryUrl;

    private ZookeeperServiceDiscovery discovery;
    private static String zookeeperConnectionAddress1;

    @BeforeAll
    public static void beforeAll() {
        zookeeperConnectionAddress1 = System.getProperty("zookeeper.connection.address.1");
    }

    @BeforeEach
    public void init() throws Exception {
        this.registryUrl = URL.valueOf(zookeeperConnectionAddress1);
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig(SERVICE_NAME));
        registryUrl.setScopeModel(applicationModel);
        this.discovery = new ZookeeperServiceDiscovery(applicationModel, registryUrl);
    }

    @AfterEach
    public void close() throws Exception {
        discovery.destroy();
    }

    @Test
    public void testRegistration() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);

        // Add Listener
        discovery.addServiceInstancesChangedListener(
                new ServiceInstancesChangedListener(Sets.newSet(SERVICE_NAME), discovery) {
            @Override
            public void onEvent(ServiceInstancesChangedEvent event) {
                latch.countDown();
            }
        });

        discovery.register();
        latch.await();
        List<ServiceInstance> serviceInstances = discovery.getInstances(SERVICE_NAME);
        assertEquals(0, serviceInstances.size());

        discovery.register(URL.valueOf("dubbo://1.1.2.3:20880/DemoService"));
        discovery.register();
        serviceInstances = discovery.getInstances(SERVICE_NAME);

        DefaultServiceInstance serviceInstance = (DefaultServiceInstance)discovery.getLocalInstance();
        assertTrue(serviceInstances.contains(serviceInstance));
        assertEquals(asList(serviceInstance), serviceInstances);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("message", "Hello,World");
        serviceInstance.setMetadata(metadata);

        discovery.register(URL.valueOf("dubbo://1.1.2.3:20880/DemoService1"));
        discovery.update();

        serviceInstances = discovery.getInstances(SERVICE_NAME);

        assertEquals(serviceInstance, serviceInstances.get(0));

        discovery.unregister();

        serviceInstances = discovery.getInstances(SERVICE_NAME);

        assertTrue(serviceInstances.isEmpty());
    }

}
