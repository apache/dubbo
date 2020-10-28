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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;

import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.apache.dubbo.common.utils.NetUtils.getAvailablePort;
import static org.apache.dubbo.registry.zookeeper.util.CuratorFrameworkUtils.generateId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ZookeeperServiceDiscovery} Test
 *
 * @since 2.7.5
 */
public class ZookeeperServiceDiscoveryTest {

    private static final String SERVICE_NAME = "A";

    private static final String LOCALHOST = "127.0.0.1";

    private TestingServer zkServer;
    private int zkServerPort;
    private URL registryUrl;

    private ZookeeperServiceDiscovery discovery;

    @BeforeEach
    public void init() throws Exception {
        EventDispatcher.getDefaultExtension().removeAllEventListeners();
        zkServerPort = getAvailablePort();
        zkServer = new TestingServer(zkServerPort, true);
        zkServer.start();

        this.registryUrl = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort);
        this.discovery = new ZookeeperServiceDiscovery();
        this.discovery.initialize(registryUrl);
    }

    @AfterEach
    public void close() throws Exception {
        discovery.destroy();
        zkServer.stop();
    }

    @Test
    public void testRegistration() {

        DefaultServiceInstance serviceInstance = createServiceInstance(SERVICE_NAME, LOCALHOST, NetUtils.getAvailablePort());

        discovery.register(serviceInstance);

        List<ServiceInstance> serviceInstances = discovery.getInstances(SERVICE_NAME);

        assertTrue(serviceInstances.contains(serviceInstance));
        assertEquals(asList(serviceInstance), serviceInstances);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("message", "Hello,World");
        serviceInstance.setMetadata(metadata);

        discovery.update(serviceInstance);

        serviceInstances = discovery.getInstances(SERVICE_NAME);

        assertEquals(serviceInstance, serviceInstances.get(0));

        discovery.unregister(serviceInstance);

        serviceInstances = discovery.getInstances(SERVICE_NAME);

        assertTrue(serviceInstances.isEmpty());
    }

    private DefaultServiceInstance createServiceInstance(String serviceName, String host, int port) {
        return new DefaultServiceInstance(generateId(host, port), serviceName, host, port);
    }

//    @Test
//    public void testGetInstances() throws InterruptedException {
//
//        List<ServiceInstance> instances = asList(
//                createServiceInstance(SERVICE_NAME, LOCALHOST, 8080),
//                createServiceInstance(SERVICE_NAME, LOCALHOST, 8081),
//                createServiceInstance(SERVICE_NAME, LOCALHOST, 8082)
//        );
//
//        instances.forEach(discovery::register);
//
//        List<ServiceInstance> serviceInstances = new LinkedList<>();
//
//        CountDownLatch latch = new CountDownLatch(1);
//
//        // Add Listener
//        discovery.addServiceInstancesChangedListener(new ServiceInstancesChangedListener(SERVICE_NAME) {
//            @Override
//            public void onEvent(ServiceInstancesChangedEvent event) {
//                serviceInstances.addAll(event.getServiceInstances());
//                latch.countDown();
//            }
//        });
//
//        discovery.register(createServiceInstance(SERVICE_NAME, LOCALHOST, 8082));
//        discovery.update(createServiceInstance(SERVICE_NAME, LOCALHOST, 8082));
//
//        latch.await();
//
//        assertFalse(serviceInstances.isEmpty());
//
//        // offset starts 0
//        int offset = 0;
//        // requestSize > total elements
//        int requestSize = 5;
//
//        Page<ServiceInstance> page = discovery.getInstances(SERVICE_NAME, offset, requestSize);
//        assertEquals(0, page.getOffset());
//        assertEquals(5, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(3, page.getData().size());
//        assertTrue(page.hasData());
//
//        for (ServiceInstance instance : page.getData()) {
//            assertTrue(instances.contains(instance));
//        }
//
//        // requestSize < total elements
//        requestSize = 2;
//
//        page = discovery.getInstances(SERVICE_NAME, offset, requestSize);
//        assertEquals(0, page.getOffset());
//        assertEquals(2, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(2, page.getData().size());
//        assertTrue(page.hasData());
//
//        for (ServiceInstance instance : page.getData()) {
//            assertTrue(instances.contains(instance));
//        }
//
//        offset = 1;
//        page = discovery.getInstances(SERVICE_NAME, offset, requestSize);
//        assertEquals(1, page.getOffset());
//        assertEquals(2, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(2, page.getData().size());
//        assertTrue(page.hasData());
//
//        for (ServiceInstance instance : page.getData()) {
//            assertTrue(instances.contains(instance));
//        }
//
//        offset = 2;
//        page = discovery.getInstances(SERVICE_NAME, offset, requestSize);
//        assertEquals(2, page.getOffset());
//        assertEquals(2, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(1, page.getData().size());
//        assertTrue(page.hasData());
//
//        offset = 3;
//        page = discovery.getInstances(SERVICE_NAME, offset, requestSize);
//        assertEquals(3, page.getOffset());
//        assertEquals(2, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(0, page.getData().size());
//        assertFalse(page.hasData());
//
//        offset = 5;
//        page = discovery.getInstances(SERVICE_NAME, offset, requestSize);
//        assertEquals(5, page.getOffset());
//        assertEquals(2, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(0, page.getData().size());
//        assertFalse(page.hasData());
//
//    }
}
