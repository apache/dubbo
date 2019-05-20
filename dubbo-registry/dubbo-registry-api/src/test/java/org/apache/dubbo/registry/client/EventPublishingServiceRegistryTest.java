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

import org.junit.jupiter.api.Test;

import static org.apache.dubbo.registry.client.DefaultServiceInstanceTest.INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link EventPublishingServiceRegistry} Test
 *
 * @since 2.7.2
 */
public class EventPublishingServiceRegistryTest {

    private static ServiceRegistry serviceRegistry = new DefaultServiceRegistry();

    private ServiceInstance serviceInstance = INSTANCE;

    @Test
    public void testRegister() {
        serviceRegistry.register(serviceInstance);
    }

    @Test
    public void testUpdate() {
        serviceRegistry.update(serviceInstance);
    }

    @Test
    public void testUnregister() {
        serviceRegistry.unregister(serviceInstance);
    }
}

class DefaultServiceRegistry extends EventPublishingServiceRegistry {

    @Override
    protected void doRegister(ServiceInstance serviceInstance) throws RuntimeException {
        assertEquals(INSTANCE, serviceInstance);
    }

    @Override
    protected void doUpdate(ServiceInstance serviceInstance) throws RuntimeException {
        assertEquals(INSTANCE, serviceInstance);
    }

    @Override
    protected void doUnregister(ServiceInstance serviceInstance) throws RuntimeException {
        assertEquals(INSTANCE, serviceInstance);
    }

    @Override
    protected void doStart() {

    }

    @Override
    protected void doStop() {

    }
}
