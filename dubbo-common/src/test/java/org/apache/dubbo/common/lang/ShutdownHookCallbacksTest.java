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
package org.apache.dubbo.common.lang;

import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ShutdownHookCallbacks}
 *
 * @since 2.7.5
 */
class ShutdownHookCallbacksTest {

    private ShutdownHookCallbacks callbacks;

    @BeforeEach
    public void init() {
        callbacks = new ShutdownHookCallbacks(ApplicationModel.defaultModel());
    }

    @Test
    void testSingleton() {
        assertNotNull(callbacks);
    }

    @Test
    void testCallback() {
        callbacks.callback();
        DefaultShutdownHookCallback callback = (DefaultShutdownHookCallback) callbacks.getCallbacks().iterator().next();
        assertTrue(callback.isExecuted());
    }

    @AfterEach
    public void destroy() {
        callbacks.destroy();
        assertTrue(callbacks.getCallbacks().isEmpty());
    }
}