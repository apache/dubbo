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
package org.apache.dubbo.config.event;

import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.event.EventListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link DubboShutdownHookUnregisteredEvent} Test
 *
 * @since 2.7.4
 */
public class DubboShutdownHookUnregisteredEventTest implements EventListener<DubboShutdownHookUnregisteredEvent> {

    private DubboShutdownHookUnregisteredEvent event;

    private EventDispatcher eventDispatcher = EventDispatcher.getDefaultExtension();

    private DubboShutdownHook dubboShutdownHook;

    @BeforeEach
    public void init() {
        eventDispatcher.removeAllEventListeners();
        eventDispatcher.addEventListener(this);
        dubboShutdownHook = DubboShutdownHook.getDubboShutdownHook();
    }

    @Test
    public void testOnEvent() {
        dubboShutdownHook.register();
        dubboShutdownHook.unregister();
        assertEquals(dubboShutdownHook, event.getSource());
        assertEquals(dubboShutdownHook, event.getDubboShutdownHook());
    }

    @Override
    public void onEvent(DubboShutdownHookUnregisteredEvent event) {
        this.event = event;
    }
}
