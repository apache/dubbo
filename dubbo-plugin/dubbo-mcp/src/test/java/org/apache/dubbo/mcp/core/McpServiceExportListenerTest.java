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

import org.apache.dubbo.config.ServiceConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class McpServiceExportListenerTest {

    @Mock
    private ServiceConfig serviceConfig;

    private McpServiceExportListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new McpServiceExportListener();
    }

    @Test
    void testExported_WithNullRef() {
        when(serviceConfig.getRef()).thenReturn(null);

        listener.exported(serviceConfig);

        verify(serviceConfig).getRef();
        verify(serviceConfig, never()).getUniqueServiceName();
    }

    @Test
    void testUnexported_WithNullRef() {
        when(serviceConfig.getRef()).thenReturn(null);

        listener.unexported(serviceConfig);

        verify(serviceConfig).getRef();
        verify(serviceConfig, never()).getUniqueServiceName();
    }

    @Test
    void testExported_WithValidRef() {
        Object serviceRef = new TestService();
        when(serviceConfig.getRef()).thenReturn(serviceRef);
        when(serviceConfig.getUniqueServiceName()).thenReturn("test.service");

        assertDoesNotThrow(() -> listener.exported(serviceConfig));

        verify(serviceConfig).getRef();
        verify(serviceConfig).getUniqueServiceName();
    }

    @Test
    void testUnexported_WithValidRef() {
        Object serviceRef = new TestService();
        when(serviceConfig.getRef()).thenReturn(serviceRef);
        when(serviceConfig.getUniqueServiceName()).thenReturn("test.service");

        assertDoesNotThrow(() -> listener.unexported(serviceConfig));

        verify(serviceConfig).getRef();
        verify(serviceConfig).getUniqueServiceName();
    }

    @Test
    void testExported_WithException() {
        when(serviceConfig.getRef()).thenReturn(new TestService());
        when(serviceConfig.getUniqueServiceName()).thenThrow(new RuntimeException("Test exception"));

        assertDoesNotThrow(() -> listener.exported(serviceConfig));

        verify(serviceConfig).getRef();
        verify(serviceConfig).getUniqueServiceName();
    }

    @Test
    void testUnexported_WithException() {
        when(serviceConfig.getRef()).thenReturn(new TestService());
        when(serviceConfig.getUniqueServiceName()).thenThrow(new RuntimeException("Test exception"));

        assertDoesNotThrow(() -> listener.unexported(serviceConfig));

        verify(serviceConfig).getRef();
        verify(serviceConfig).getUniqueServiceName();
    }

    @Test
    void testListenerCreation() {

        McpServiceExportListener newListener = new McpServiceExportListener();
        assertNotNull(newListener);
    }

    public static class TestService {
        public void testMethod() {
            // Test method
        }
    }
}
