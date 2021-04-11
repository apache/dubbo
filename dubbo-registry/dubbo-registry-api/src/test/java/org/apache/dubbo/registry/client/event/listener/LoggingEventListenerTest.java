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
package org.apache.dubbo.registry.client.event.listener;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.FileSystemServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryDestroyedEvent;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryDestroyingEvent;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryInitializedEvent;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryInitializingEvent;
import org.apache.dubbo.registry.client.event.ServiceInstancePreRegisteredEvent;
import org.apache.dubbo.registry.client.event.ServiceInstancePreUnregisteredEvent;
import org.apache.dubbo.registry.client.event.ServiceInstanceRegisteredEvent;
import org.apache.dubbo.registry.client.event.ServiceInstanceUnregisteredEvent;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.apache.dubbo.registry.client.DefaultServiceInstanceTest.createInstance;

/**
 * {@link LoggingEventListener} Test
 *
 * @since 2.7.5
 */
public class LoggingEventListenerTest {

    private LoggingEventListener listener;

    @BeforeEach
    public void init() {
        listener = new LoggingEventListener();
    }

    @Test
    public void testOnEvent() throws Exception {

        URL connectionURL = URL.valueOf("file:///Users/Home");

        ServiceDiscovery serviceDiscovery = new FileSystemServiceDiscovery();

        serviceDiscovery.initialize(connectionURL);

        // ServiceDiscoveryStartingEvent
        listener.onEvent(new ServiceDiscoveryInitializingEvent(serviceDiscovery, serviceDiscovery));

        // ServiceDiscoveryStartedEvent
        listener.onEvent(new ServiceDiscoveryInitializedEvent(serviceDiscovery, serviceDiscovery));

        // ServiceInstancePreRegisteredEvent
        listener.onEvent(new ServiceInstancePreRegisteredEvent(serviceDiscovery, createInstance()));

        // ServiceInstanceRegisteredEvent
        listener.onEvent(new ServiceInstanceRegisteredEvent(serviceDiscovery, createInstance()));

        // ServiceInstancesChangedEvent
        listener.onEvent(new ServiceInstancesChangedEvent("test", Arrays.asList(createInstance())));

        // ServiceInstancePreUnregisteredEvent
        listener.onEvent(new ServiceInstancePreUnregisteredEvent(serviceDiscovery, createInstance()));

        // ServiceInstanceUnregisteredEvent
        listener.onEvent(new ServiceInstanceUnregisteredEvent(serviceDiscovery, createInstance()));

        // ServiceDiscoveryStoppingEvent
        listener.onEvent(new ServiceDiscoveryDestroyingEvent(serviceDiscovery, serviceDiscovery));

        // ServiceDiscoveryStoppedEvent
        listener.onEvent(new ServiceDiscoveryDestroyedEvent(serviceDiscovery, serviceDiscovery));
    }
}
