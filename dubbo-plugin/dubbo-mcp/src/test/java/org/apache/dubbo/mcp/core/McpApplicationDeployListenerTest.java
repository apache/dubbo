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
package org.apache.dubbo.mcp.core;

import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

class McpApplicationDeployListenerTest {

    @Mock
    private ApplicationModel applicationModel;

    private McpApplicationDeployListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new McpApplicationDeployListener();
    }

    @Test
    void testOnInitialize() {
        assertDoesNotThrow(() -> listener.onInitialize(applicationModel));
    }

    @Test
    void testOnStarting() {
        assertDoesNotThrow(() -> listener.onStarting(applicationModel));
    }

    @Test
    void testOnStarted_WithNullModel() {

        assertDoesNotThrow(() -> listener.onStarted(null));
    }

    @Test
    void testOnStarted_WithMockModel() {
        assertThrows(NullPointerException.class, () -> listener.onStarted(applicationModel));
    }

    @Test
    void testOnStopping() {

        assertDoesNotThrow(() -> listener.onStopping(applicationModel));
    }

    @Test
    void testOnStopped() {

        assertDoesNotThrow(() -> listener.onStopped(applicationModel));
    }

    @Test
    void testOnFailure() {
        Throwable cause = new RuntimeException("Test failure");

        assertDoesNotThrow(() -> listener.onFailure(applicationModel, cause));
    }

    @Test
    void testGetDubboMcpSseTransportProvider() {

        assertDoesNotThrow(() -> McpApplicationDeployListener.getDubboMcpSseTransportProvider());
    }

    @Test
    void testListenerCreation() {

        McpApplicationDeployListener newListener = new McpApplicationDeployListener();
        assertNotNull(newListener);
    }

    @Test
    void testBasicLifecycleMethods() {

        assertDoesNotThrow(() -> {
            listener.onInitialize(applicationModel);
            listener.onStarting(applicationModel);
            listener.onStopping(applicationModel);
            listener.onStopped(applicationModel);
            listener.onFailure(applicationModel, new Exception("test"));
        });
    }
}
