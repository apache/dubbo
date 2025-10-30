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
package org.apache.dubbo.mcp.tool;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.mcp.core.McpServiceFilter;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import java.util.ArrayList;
import java.util.List;

import io.modelcontextprotocol.server.McpAsyncServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DubboServiceToolRegistryTest {

    @Mock
    private McpAsyncServer mcpServer;

    @Mock
    private DubboOpenApiToolConverter toolConverter;

    @Mock
    private DubboMcpGenericCaller genericCaller;

    @Mock
    private McpServiceFilter mcpServiceFilter;

    @Mock
    private ProviderModel providerModel;

    @Mock
    private ServiceDescriptor serviceDescriptor;

    private DubboServiceToolRegistry registry;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        registry = new DubboServiceToolRegistry(mcpServer, toolConverter, genericCaller, mcpServiceFilter);
    }

    @Test
    void testRegistryConstruction() {
        assertNotNull(registry);
    }

    @Test
    void testRegisterService_WithNullUrls() {
        when(providerModel.getServiceModel()).thenReturn(serviceDescriptor);
        when(providerModel.getServiceUrls()).thenReturn(null);

        int result = registry.registerService(providerModel);

        assertEquals(0, result);
    }

    @Test
    void testRegisterService_WithEmptyUrls() {
        when(providerModel.getServiceModel()).thenReturn(serviceDescriptor);
        when(providerModel.getServiceUrls()).thenReturn(new ArrayList<>());

        int result = registry.registerService(providerModel);

        assertEquals(0, result);
    }

    @Test
    void testRegisterService_WithNullServiceInterface() {
        List<URL> urls = createMockUrls();
        when(providerModel.getServiceModel()).thenReturn(serviceDescriptor);
        when(providerModel.getServiceUrls()).thenReturn(urls);
        when(serviceDescriptor.getServiceInterfaceClass()).thenReturn(null);

        int result = registry.registerService(providerModel);

        assertEquals(0, result);
    }

    @Test
    void testRegisterService_WithValidService() {
        setupValidProviderModel();
        when(mcpServiceFilter.shouldExposeMethodAsMcpTool(any(), any())).thenReturn(true);
        when(mcpServiceFilter.getMcpToolConfig(any(), any())).thenReturn(createMockToolConfig());
        when(mcpServer.addTool(any())).thenReturn(Mono.empty());

        int result = registry.registerService(providerModel);

        assertTrue(result > 0);
        verify(mcpServer, atLeastOnce()).addTool(any());
    }

    @Test
    void testRegisterService_WithServiceLevelTools() {
        setupValidProviderModel();
        when(mcpServiceFilter.shouldExposeMethodAsMcpTool(any(), any())).thenReturn(false);
        when(mcpServiceFilter.shouldExposeAsMcpTool(any())).thenReturn(true);
        when(toolConverter.convertToTools(any(), any(), any())).thenReturn(createMockTools());
        when(mcpServer.addTool(any())).thenReturn(Mono.empty());

        int result = registry.registerService(providerModel);

        assertTrue(result >= 0);
    }

    @Test
    void testRegisterService_WithException() {
        when(providerModel.getServiceModel()).thenThrow(new RuntimeException("Test exception"));

        assertThrows(RuntimeException.class, () -> {
            registry.registerService(providerModel);
        });
    }

    @Test
    void testUnregisterService_WithExistingService() {
        setupValidProviderModel();
        when(mcpServiceFilter.shouldExposeMethodAsMcpTool(any(), any())).thenReturn(true);
        when(mcpServiceFilter.getMcpToolConfig(any(), any())).thenReturn(createMockToolConfig());
        when(mcpServer.addTool(any())).thenReturn(Mono.empty());
        when(mcpServer.removeTool(anyString())).thenReturn(Mono.empty());

        registry.registerService(providerModel);

        assertDoesNotThrow(() -> registry.unregisterService(providerModel));
    }

    @Test
    void testUnregisterService_WithNonExistentService() {
        when(providerModel.getServiceKey()).thenReturn("nonExistentService");

        assertDoesNotThrow(() -> registry.unregisterService(providerModel));
    }

    @Test
    void testUnregisterService_WithException() {
        setupValidProviderModel();
        when(mcpServiceFilter.shouldExposeMethodAsMcpTool(any(), any())).thenReturn(true);
        when(mcpServiceFilter.getMcpToolConfig(any(), any())).thenReturn(createMockToolConfig());
        when(mcpServer.addTool(any())).thenReturn(Mono.empty());
        when(mcpServer.removeTool(anyString())).thenThrow(new RuntimeException("Test exception"));

        registry.registerService(providerModel);

        assertDoesNotThrow(() -> registry.unregisterService(providerModel));
    }

    @Test
    void testClearRegistry() {
        assertDoesNotThrow(() -> registry.clearRegistry());
    }

    private void setupValidProviderModel() {
        List<URL> urls = createMockUrls();
        Class<?> mockInterface = TestInterface.class;

        when(providerModel.getServiceModel()).thenReturn(serviceDescriptor);
        when(providerModel.getServiceUrls()).thenReturn(urls);
        when(providerModel.getServiceKey()).thenReturn("testService");
        when(serviceDescriptor.getServiceInterfaceClass()).thenReturn((Class) mockInterface);
        when(serviceDescriptor.getInterfaceName()).thenReturn("TestInterface");
    }

    private List<URL> createMockUrls() {
        List<URL> urls = new ArrayList<>();
        URL url = URL.valueOf("dubbo://localhost:20880/TestInterface");
        urls.add(url);
        return urls;
    }

    private McpServiceFilter.McpToolConfig createMockToolConfig() {
        McpServiceFilter.McpToolConfig config = new McpServiceFilter.McpToolConfig();
        config.setToolName("testTool");
        config.setDescription("Test tool description");
        return config;
    }

    private java.util.Map<String, io.modelcontextprotocol.spec.McpSchema.Tool> createMockTools() {
        java.util.Map<String, io.modelcontextprotocol.spec.McpSchema.Tool> tools = new java.util.HashMap<>();
        io.modelcontextprotocol.spec.McpSchema.Tool tool =
                new io.modelcontextprotocol.spec.McpSchema.Tool("testTool", "Test description", "{}");
        tools.put("testTool", tool);
        return tools;
    }

    public interface TestInterface {
        String testMethod(String param);

        void anotherMethod(int value);
    }
}
