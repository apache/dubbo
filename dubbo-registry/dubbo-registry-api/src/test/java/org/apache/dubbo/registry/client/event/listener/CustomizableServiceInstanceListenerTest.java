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

import org.apache.dubbo.registry.client.DefaultServiceInstanceTest;
import org.apache.dubbo.registry.client.event.ServiceInstancePreRegisteredEvent;

import org.junit.jupiter.api.Test;

/**
 * {@link CustomizableServiceInstanceListener} Test
 *
 * @since 2.7.5
 */
public class CustomizableServiceInstanceListenerTest {

    private CustomizableServiceInstanceListener listener = new CustomizableServiceInstanceListener();

    @Test
    public void testOnEvent() {
        ServiceInstancePreRegisteredEvent event = new ServiceInstancePreRegisteredEvent(this, DefaultServiceInstanceTest.createInstance());
        // breaking test
        listener.onEvent(event);
    }
}
