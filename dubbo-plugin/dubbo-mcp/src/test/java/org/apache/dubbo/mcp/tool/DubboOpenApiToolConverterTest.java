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
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.rest.OpenAPIRequest;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.DefaultOpenAPIService;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.PathItem;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema;

import java.util.HashMap;
import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DubboOpenApiToolConverterTest {

    @Mock
    private DefaultOpenAPIService openApiService;

    @Mock
    private ServiceDescriptor serviceDescriptor;

    @Mock
    private URL serviceUrl;

    private DubboOpenApiToolConverter converter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        converter = new DubboOpenApiToolConverter(openApiService);
    }

    @Test
    void testConverterConstruction() {
        assertNotNull(converter);
    }

    @Test
    void testConvertToTools_WithNullOpenAPI() {
        when(serviceDescriptor.getInterfaceName()).thenReturn("TestService");
        when(openApiService.getOpenAPI(any(OpenAPIRequest.class))).thenReturn(null);

        Map<String, McpSchema.Tool> result = converter.convertToTools(serviceDescriptor, serviceUrl, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testConvertToTools_WithEmptyPaths() {
        when(serviceDescriptor.getInterfaceName()).thenReturn("TestService");

        OpenAPI openAPI = new OpenAPI();
        openAPI.setPaths(new HashMap<>());
        when(openApiService.getOpenAPI(any(OpenAPIRequest.class))).thenReturn(openAPI);

        Map<String, McpSchema.Tool> result = converter.convertToTools(serviceDescriptor, serviceUrl, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testConvertToTools_WithValidOperation() {
        when(serviceDescriptor.getInterfaceName()).thenReturn("TestService");

        OpenAPI openAPI = createMockOpenAPI();
        when(openApiService.getOpenAPI(any(OpenAPIRequest.class))).thenReturn(openAPI);

        Map<String, McpSchema.Tool> result = converter.convertToTools(serviceDescriptor, serviceUrl, null);

        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("testOperation"));

        McpSchema.Tool tool = result.get("testOperation");
        assertEquals("testOperation", tool.name());
        assertNotNull(tool.description());
        assertNotNull(tool.inputSchema());
    }

    @Test
    void testConvertToTools_WithCustomToolConfig() {
        when(serviceDescriptor.getInterfaceName()).thenReturn("TestService");

        OpenAPI openAPI = createMockOpenAPI();
        when(openApiService.getOpenAPI(any(OpenAPIRequest.class))).thenReturn(openAPI);

        McpServiceFilter.McpToolConfig toolConfig = new McpServiceFilter.McpToolConfig();
        toolConfig.setToolName("customTool");
        toolConfig.setDescription("Custom description");

        Map<String, McpSchema.Tool> result = converter.convertToTools(serviceDescriptor, serviceUrl, toolConfig);

        assertFalse(result.isEmpty());
        McpSchema.Tool tool = result.values().iterator().next();
        assertEquals("customTool", tool.name());
        assertEquals("Custom description", tool.description());
    }

    @Test
    void testGetOperationByToolName_WithExistingTool() {
        when(serviceDescriptor.getInterfaceName()).thenReturn("TestService");

        OpenAPI openAPI = createMockOpenAPI();
        when(openApiService.getOpenAPI(any(OpenAPIRequest.class))).thenReturn(openAPI);

        converter.convertToTools(serviceDescriptor, serviceUrl, null);

        Operation operation = converter.getOperationByToolName("testOperation");
        assertNotNull(operation);
        assertEquals("testOperation", operation.getOperationId());
    }

    @Test
    void testGetOperationByToolName_WithNonExistentTool() {
        Operation operation = converter.getOperationByToolName("nonExistent");
        assertNull(operation);
    }

    @Test
    void testConvertToTools_WithParameter() {
        when(serviceDescriptor.getInterfaceName()).thenReturn("TestService");

        OpenAPI openAPI = createMockOpenAPIWithParameters();
        when(openApiService.getOpenAPI(any(OpenAPIRequest.class))).thenReturn(openAPI);

        Map<String, McpSchema.Tool> result = converter.convertToTools(serviceDescriptor, serviceUrl, null);

        assertFalse(result.isEmpty());
        McpSchema.Tool tool = result.get("testOperation");
        assertNotNull(tool);
        assertNotNull(tool.inputSchema());
    }

    @Test
    void testConvertToTools_WithException() {
        when(serviceDescriptor.getInterfaceName()).thenReturn("TestService");
        when(openApiService.getOpenAPI(any(OpenAPIRequest.class))).thenThrow(new RuntimeException("Test exception"));

        assertThrows(RuntimeException.class, () -> {
            converter.convertToTools(serviceDescriptor, serviceUrl, null);
        });
    }

    private OpenAPI createMockOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        Map<String, PathItem> paths = new HashMap<>();

        PathItem pathItem = new PathItem();
        Map<HttpMethods, Operation> operations = new HashMap<>();

        Operation operation = new Operation();
        operation.setOperationId("testOperation");
        operation.setSummary("Test operation summary");
        operation.setDescription("Test operation description");

        operations.put(HttpMethods.GET, operation);
        pathItem.setOperations(operations);

        paths.put("/test", pathItem);
        openAPI.setPaths(paths);

        return openAPI;
    }

    private OpenAPI createMockOpenAPIWithParameters() {
        OpenAPI openAPI = createMockOpenAPI();

        PathItem pathItem = openAPI.getPaths().get("/test");
        Operation operation = pathItem.getOperations().get(HttpMethods.GET);

        Parameter parameter = new Parameter("testParam", Parameter.In.QUERY);
        parameter.setSchema(new Schema());

        operation.setParameters(java.util.Arrays.asList(parameter));

        return openAPI;
    }
}
