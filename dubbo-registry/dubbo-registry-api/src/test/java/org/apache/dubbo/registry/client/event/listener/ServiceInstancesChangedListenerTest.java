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

import org.apache.dubbo.event.Event;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static org.apache.dubbo.event.EventDispatcher.getDefaultExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ServiceInstancesChangedListener} Test
 *
 * @since 2.7.5
 */
public class ServiceInstancesChangedListenerTest {

    @Test
    public void testOnEvent() {

        EventDispatcher eventDispatcher = getDefaultExtension();

        Event event = new ServiceInstancesChangedEvent("test", emptyList());

        AtomicReference<Event> eventRef = new AtomicReference<>();

        eventDispatcher.addEventListener(new ServiceInstancesChangedListener("test") {
            @Override
            public void onEvent(ServiceInstancesChangedEvent event) {
                eventRef.set(event);
            }
        });

        // Dispatch a ServiceInstancesChangedEvent
        eventDispatcher.dispatch(event);

        assertEquals(eventRef.get(), event);
    }
}
