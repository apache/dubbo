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
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryStartedEvent;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryStartingEvent;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryStoppedEvent;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryStoppingEvent;
import org.apache.dubbo.registry.client.event.ServiceInstancePreRegisteredEvent;
import org.apache.dubbo.registry.client.event.ServiceInstanceRegisteredEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link EventPublishingServiceDiscovery} Test
 *
 * @since 2.7.4
 */
public class EventPublishingServiceDiscoveryTest {

    private static final URL url = URL.valueOf("zookeeper://127.0.0.1:2181/");

    private EventDispatcher eventDispatcher = EventDispatcher.getDefaultExtension();

    private InMemoryServiceDiscovery delegate;

    private EventPublishingServiceDiscovery serviceDiscovery;

    private ServiceDiscoveryTest serviceDiscoveryTest;

    @BeforeEach
    public void init() {

        // remove all EventListeners
        eventDispatcher.removeAllEventListeners();

        delegate = new InMemoryServiceDiscovery();

        serviceDiscovery = new EventPublishingServiceDiscovery(delegate);

        serviceDiscoveryTest = new ServiceDiscoveryTest();

        serviceDiscoveryTest.setServiceDiscovery(serviceDiscovery);

        // ServiceDiscoveryStartingEvent
        eventDispatcher.addEventListener(new EventListener<ServiceDiscoveryStartingEvent>() {
            @Override
            public void onEvent(ServiceDiscoveryStartingEvent event) {
                assertEquals(delegate, event.getServiceDiscovery());
            }
        });

        // ServiceDiscoveryStartedEvent
        eventDispatcher.addEventListener(new EventListener<ServiceDiscoveryStartedEvent>() {
            @Override
            public void onEvent(ServiceDiscoveryStartedEvent event) {
                assertEquals(delegate, event.getServiceDiscovery());
            }
        });

        // ServiceInstancePreRegisteredEvent
        eventDispatcher.addEventListener(new EventListener<ServiceInstancePreRegisteredEvent>() {
            @Override
            public void onEvent(ServiceInstancePreRegisteredEvent event) {
                assertNotNull(event.getServiceInstance());
            }
        });

        // ServiceInstanceRegisteredEvent
        eventDispatcher.addEventListener(new EventListener<ServiceInstanceRegisteredEvent>() {
            @Override
            public void onEvent(ServiceInstanceRegisteredEvent event) {
                assertNotNull(event.getServiceInstance());
            }
        });

        assertFalse(serviceDiscovery.isStarted());
        assertFalse(serviceDiscovery.isStopped());

        // test start()
        serviceDiscoveryTest.init();

        assertTrue(serviceDiscovery.isStarted());
        assertFalse(serviceDiscovery.isStopped());
    }

    @AfterEach
    public void destroy() {

        // ServiceDiscoveryStoppingEvent
        eventDispatcher.addEventListener(new EventListener<ServiceDiscoveryStoppingEvent>() {
            @Override
            public void onEvent(ServiceDiscoveryStoppingEvent event) {
                assertEquals(delegate, event.getServiceDiscovery());
            }
        });

        // ServiceDiscoveryStoppedEvent
        eventDispatcher.addEventListener(new EventListener<ServiceDiscoveryStoppedEvent>() {
            @Override
            public void onEvent(ServiceDiscoveryStoppedEvent event) {
                assertEquals(delegate, event.getServiceDiscovery());
            }
        });

        assertTrue(serviceDiscovery.isStarted());
        assertFalse(serviceDiscovery.isStopped());

        // test stop()
        serviceDiscoveryTest.destroy();

        assertTrue(serviceDiscovery.isStarted());
        assertTrue(serviceDiscovery.isStopped());
    }

    @Test
    public void testToString() {
        serviceDiscoveryTest.testToString();
    }

    @Test
    public void testRegisterAndUpdateAndUnregister() {
        serviceDiscoveryTest.testRegisterAndUpdateAndUnregister();
    }

    @Test
    public void testGetServices() {
        serviceDiscoveryTest.testGetServices();
    }

    @Test
    public void testGetInstances() {
        serviceDiscoveryTest.testGetInstances();
    }

    @Test
    public void testGetInstancesWithHealthy() {
        serviceDiscoveryTest.testGetInstancesWithHealthy();
    }
}