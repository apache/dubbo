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

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.mcp.annotations.McpTool;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

class McpServiceFilterTest {

    @Mock
    private ApplicationModel applicationModel;

    private McpServiceFilter mcpServiceFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        try {
            mcpServiceFilter = new McpServiceFilter(ApplicationModel.defaultModel());
        } catch (Exception e) {
            mcpServiceFilter = null;
        }
    }

    @Test
    void testMcpToolConfig_SettersAndGetters() {
        McpServiceFilter.McpToolConfig config = new McpServiceFilter.McpToolConfig();

        config.setToolName("testTool");
        assertEquals("testTool", config.getToolName());

        config.setDescription("Test description");
        assertEquals("Test description", config.getDescription());

        config.setPriority(10);
        assertEquals(10, config.getPriority());
    }

    @Test
    void testMcpToolConfig_DefaultValues() {
        McpServiceFilter.McpToolConfig config = new McpServiceFilter.McpToolConfig();

        assertNull(config.getToolName());
        assertNull(config.getDescription());
        assertEquals(0, config.getPriority());
    }

    @Test
    void testTestServiceAnnotations() throws NoSuchMethodException {

        Class<?> testServiceClass = TestServiceImpl.class;
        assertTrue(testServiceClass.isAnnotationPresent(DubboService.class));

        Method annotatedMethod = testServiceClass.getMethod("annotatedMethod");
        assertTrue(annotatedMethod.isAnnotationPresent(McpTool.class));

        McpTool mcpTool = annotatedMethod.getAnnotation(McpTool.class);
        assertEquals("customTool", mcpTool.name());
        assertEquals("Custom tool description", mcpTool.description());
        assertEquals(5, mcpTool.priority());
    }

    @Test
    void testDisabledMethodAnnotation() throws NoSuchMethodException {
        Method disabledMethod = TestServiceImpl.class.getMethod("disabledMethod");
        assertTrue(disabledMethod.isAnnotationPresent(McpTool.class));

        McpTool mcpTool = disabledMethod.getAnnotation(McpTool.class);
        assertFalse(mcpTool.enabled());
    }

    @Test
    void testPublicMethodExists() throws NoSuchMethodException {
        Method publicMethod = TestServiceImpl.class.getMethod("publicMethod");
        assertNotNull(publicMethod);
        assertTrue(java.lang.reflect.Modifier.isPublic(publicMethod.getModifiers()));
    }

    @Test
    void testServiceImplementsInterface() {
        TestServiceImpl impl = new TestServiceImpl();
        assertTrue(impl instanceof TestService);
    }

    @DubboService
    public static class TestServiceImpl implements TestService {

        public void publicMethod() {}

        @McpTool(name = "customTool", description = "Custom tool description", priority = 5)
        public void annotatedMethod() {}

        @McpTool(enabled = false)
        public void disabledMethod() {}

        private void privateMethod() {}
    }

    public interface TestService {
        void publicMethod();

        void annotatedMethod();

        void disabledMethod();
    }
}
