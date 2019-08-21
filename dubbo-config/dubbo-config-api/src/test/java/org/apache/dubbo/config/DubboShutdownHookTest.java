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
package org.apache.dubbo.config;

import org.apache.dubbo.common.lang.ShutdownHookCallback;
import org.apache.dubbo.event.EventDispatcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * {@link DubboShutdownHook} Test
 *
 * @since 2.7.4
 */
public class DubboShutdownHookTest implements ShutdownHookCallback {

    private final DubboShutdownHook dubboShutdownHook = DubboShutdownHook.getDubboShutdownHook();

    private final EventDispatcher eventDispatcher = EventDispatcher.getDefaultExtension();


    @BeforeEach
    public void before() {

    }

    @AfterEach
    public void after() {
        eventDispatcher.removeAllEventListeners();
        dubboShutdownHook.clear();
    }
//
//    @Test
//    public void testCallback() {
//        assertEquals(this.getClass(), dubboShutdownHook.getCallbacks().iterator().next().getClass());
//        dubboShutdownHook.addCallback(this);
//        assertEquals(2, dubboShutdownHook.getCallbacks().size());
//    }

    @Override
    public void callback() throws Throwable {

    }
}
