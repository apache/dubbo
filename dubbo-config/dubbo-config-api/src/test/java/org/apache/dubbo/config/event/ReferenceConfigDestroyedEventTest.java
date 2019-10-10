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

import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.event.EventListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.util.ReflectionUtils.findField;
import static org.springframework.util.ReflectionUtils.setField;

/**
 * {@link ReferenceConfigDestroyedEvent} Test
 *
 * @since 2.7.4
 */
public class ReferenceConfigDestroyedEventTest implements EventListener<ReferenceConfigDestroyedEvent> {

    private ReferenceConfigDestroyedEvent event;

    private EventDispatcher eventDispatcher = EventDispatcher.getDefaultExtension();

    private ReferenceConfig referenceConfig;

    @BeforeEach
    public void init() throws Exception {
        eventDispatcher.removeAllEventListeners();
        eventDispatcher.addEventListener(this);
        referenceConfig = new ReferenceConfig();
        Field field = findField(referenceConfig.getClass(), "ref");
        field.setAccessible(true);
        setField(field, referenceConfig, new Object());
    }

    @Test
    public void testOnEvent() {
        referenceConfig.destroy();
        assertEquals(referenceConfig, event.getSource());
    }

    @Override
    public void onEvent(ReferenceConfigDestroyedEvent event) {
        this.event = event;
    }
}
