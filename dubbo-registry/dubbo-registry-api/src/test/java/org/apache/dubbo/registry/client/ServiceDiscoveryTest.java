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

import org.apache.dubbo.common.utils.Page;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.registry.client.event.ServiceInstanceEvent;
import org.apache.dubbo.registry.client.event.ServiceInstancePreRegisteredEvent;
import org.apache.dubbo.registry.client.event.ServiceInstanceRegisteredEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static org.apache.dubbo.registry.client.DefaultServiceInstanceTest.INSTANCE;
import static org.apache.dubbo.registry.client.ServiceDiscoveryTest.handleEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ServiceDiscovery} Test case
 *
 * @since 2.7.2
 */
public class ServiceDiscoveryTest {

    private static InMemoryServiceDiscovery instance;

    private EventDispatcher dispatcher = EventDispatcher.getDefaultExtension();

    @BeforeEach
    public void init() {
        instance = new InMemoryServiceDiscovery();
        dispatcher.addEventListener(new BeforeEventListener());
        dispatcher.addEventListener(new AfterEventListener());
        dispatcher.removeAllEventListeners();
    }

    @Test
    public void testToString() {
        assertEquals("InMemoryServiceDiscovery", instance.toString());
    }

    @Test
    public void testGetPriority() {
        assertEquals(MAX_VALUE, instance.getPriority());
    }

    @Test
    public void testGetServices() {
        instance.addServiceInstance(new DefaultServiceInstance("A", "127.0.0.1", 8080));
        instance.addServiceInstance(new DefaultServiceInstance("B", "127.0.0.1", 8080));
        instance.addServiceInstance(new DefaultServiceInstance("C", "127.0.0.1", 8080));
        assertEquals(new HashSet<>(asList("A", "B", "C")), instance.getServices());
    }

    @Test
    public void testGetInstances() {

        List<ServiceInstance> instances = asList(
                new DefaultServiceInstance("A", "127.0.0.1", 8080),
                new DefaultServiceInstance("A", "127.0.0.1", 8081),
                new DefaultServiceInstance("A", "127.0.0.1", 8082)
        );

        instances.forEach(instance::addServiceInstance);

        // Duplicated
        instance.addServiceInstance(new DefaultServiceInstance("A", "127.0.0.1", 8080));
        // Duplicated
        instance.addServiceInstance(new DefaultServiceInstance("A", "127.0.0.1", 8081));

        // offset starts 0
        int offset = 0;
        // requestSize > total elements
        int requestSize = 5;

        Page<ServiceInstance> page = instance.getInstances("A", offset, requestSize);
        assertEquals(0, page.getRequestOffset());
        assertEquals(5, page.getRequestSize());
        assertEquals(3, page.getTotalSize());
        assertEquals(3, page.getData().size());
        assertTrue(page.hasData());

        for (ServiceInstance instance : page.getData()) {
            assertTrue(instances.contains(instance));
        }

        // requestSize < total elements
        requestSize = 2;

        page = instance.getInstances("A", offset, requestSize);
        assertEquals(0, page.getRequestOffset());
        assertEquals(2, page.getRequestSize());
        assertEquals(3, page.getTotalSize());
        assertEquals(2, page.getData().size());
        assertTrue(page.hasData());

        for (ServiceInstance instance : page.getData()) {
            assertTrue(instances.contains(instance));
        }

        offset = 1;
        page = instance.getInstances("A", offset, requestSize);
        assertEquals(1, page.getRequestOffset());
        assertEquals(2, page.getRequestSize());
        assertEquals(3, page.getTotalSize());
        assertEquals(2, page.getData().size());
        assertTrue(page.hasData());

        for (ServiceInstance instance : page.getData()) {
            assertTrue(instances.contains(instance));
        }

        offset = 2;
        page = instance.getInstances("A", offset, requestSize);
        assertEquals(2, page.getRequestOffset());
        assertEquals(2, page.getRequestSize());
        assertEquals(3, page.getTotalSize());
        assertEquals(1, page.getData().size());
        assertTrue(page.hasData());

        offset = 3;
        page = instance.getInstances("A", offset, requestSize);
        assertEquals(3, page.getRequestOffset());
        assertEquals(2, page.getRequestSize());
        assertEquals(3, page.getTotalSize());
        assertEquals(0, page.getData().size());
        assertFalse(page.hasData());

        offset = 5;
        page = instance.getInstances("A", offset, requestSize);
        assertEquals(5, page.getRequestOffset());
        assertEquals(2, page.getRequestSize());
        assertEquals(3, page.getTotalSize());
        assertEquals(0, page.getData().size());
        assertFalse(page.hasData());
    }

    @Test
    public void testGetInstancesWithHealthy() {

        List<ServiceInstance> instances = new LinkedList<>(asList(
                new DefaultServiceInstance("A", "127.0.0.1", 8080),
                new DefaultServiceInstance("A", "127.0.0.1", 8081)
        ));


        DefaultServiceInstance serviceInstance = new DefaultServiceInstance("A", "127.0.0.1", 8082);
        serviceInstance.setHealthy(false);
        instances.add(serviceInstance);

        instances.forEach(instance::addServiceInstance);

        // offset starts 0
        int offset = 0;
        // requestSize > total elements
        int requestSize = 5;

        Page<ServiceInstance> page = instance.getInstances("A", offset, requestSize, true);
        assertEquals(0, page.getRequestOffset());
        assertEquals(5, page.getRequestSize());
        assertEquals(3, page.getTotalSize());
        assertEquals(2, page.getData().size());
        assertTrue(page.hasData());

        for (ServiceInstance instance : page.getData()) {
            assertTrue(instances.contains(instance));
        }

        // requestSize < total elements
        requestSize = 2;

        offset = 1;
        page = instance.getInstances("A", offset, requestSize, true);
        assertEquals(1, page.getRequestOffset());
        assertEquals(2, page.getRequestSize());
        assertEquals(3, page.getTotalSize());
        assertEquals(1, page.getData().size());
        assertTrue(page.hasData());

        for (ServiceInstance instance : page.getData()) {
            assertTrue(instances.contains(instance));
        }

        offset = 2;
        page = instance.getInstances("A", offset, requestSize, true);
        assertEquals(2, page.getRequestOffset());
        assertEquals(2, page.getRequestSize());
        assertEquals(3, page.getTotalSize());
        assertEquals(0, page.getData().size());
        assertFalse(page.hasData());

        offset = 3;
        page = instance.getInstances("A", offset, requestSize, true);
        assertEquals(3, page.getRequestOffset());
        assertEquals(2, page.getRequestSize());
        assertEquals(3, page.getTotalSize());
        assertEquals(0, page.getData().size());
        assertFalse(page.hasData());

        offset = 5;
        page = instance.getInstances("A", offset, requestSize, true);
        assertEquals(5, page.getRequestOffset());
        assertEquals(2, page.getRequestSize());
        assertEquals(3, page.getTotalSize());
        assertEquals(0, page.getData().size());
        assertFalse(page.hasData());
    }


    static void handleEvent(ServiceInstanceEvent event) {
        assertEquals(INSTANCE, event.getServiceInstance());
        assertEquals(instance, event.getSource());
    }
}

class BeforeEventListener implements EventListener<ServiceInstancePreRegisteredEvent> {

    @Override
    public void onEvent(ServiceInstancePreRegisteredEvent event) {
        handleEvent(event);
    }
}

class AfterEventListener implements EventListener<ServiceInstanceRegisteredEvent> {

    @Override
    public void onEvent(ServiceInstanceRegisteredEvent event) {
        handleEvent(event);
    }
}
