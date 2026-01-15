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
package org.apache.dubbo.mcp.annotations;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class McpToolTest {

    static class TestService {
        @McpTool
        public void defaultMethod() {}

        @McpTool(
                enabled = false,
                name = "customTool",
                description = "Test description",
                tags = {"tag1", "tag2"},
                priority = 10)
        public void customMethod() {}

        @McpTool(enabled = true)
        public void enabledMethod() {}

        public void normalMethod(
                @McpToolParam String param1,
                @McpToolParam(name = "customParam", description = "Custom param", required = true) String param2) {}
    }

    @Test
    void testMcpToolDefaultValues() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("defaultMethod");
        McpTool annotation = method.getAnnotation(McpTool.class);

        assertNotNull(annotation);
        assertTrue(annotation.enabled());
        assertEquals("", annotation.name());
        assertEquals("", annotation.description());
        assertEquals(0, annotation.tags().length);
        assertEquals(0, annotation.priority());
    }

    @Test
    void testMcpToolCustomValues() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("customMethod");
        McpTool annotation = method.getAnnotation(McpTool.class);

        assertNotNull(annotation);
        assertFalse(annotation.enabled());
        assertEquals("customTool", annotation.name());
        assertEquals("Test description", annotation.description());
        assertEquals(2, annotation.tags().length);
        assertEquals("tag1", annotation.tags()[0]);
        assertEquals("tag2", annotation.tags()[1]);
        assertEquals(10, annotation.priority());
    }

    @Test
    void testMcpToolEnabledTrue() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("enabledMethod");
        McpTool annotation = method.getAnnotation(McpTool.class);

        assertNotNull(annotation);
        assertTrue(annotation.enabled());
    }

    @Test
    void testMcpToolParamDefaultValues() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("normalMethod", String.class, String.class);
        McpToolParam[] paramAnnotations = method.getParameters()[0].getAnnotationsByType(McpToolParam.class);

        assertEquals(1, paramAnnotations.length);
        McpToolParam annotation = paramAnnotations[0];

        assertEquals("", annotation.name());
        assertEquals("", annotation.description());
        assertFalse(annotation.required());
    }

    @Test
    void testMcpToolParamCustomValues() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("normalMethod", String.class, String.class);
        McpToolParam[] paramAnnotations = method.getParameters()[1].getAnnotationsByType(McpToolParam.class);

        assertEquals(1, paramAnnotations.length);
        McpToolParam annotation = paramAnnotations[0];

        assertEquals("customParam", annotation.name());
        assertEquals("Custom param", annotation.description());
        assertTrue(annotation.required());
    }

    @Test
    void testMethodWithoutAnnotation() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("normalMethod", String.class, String.class);
        McpTool annotation = method.getAnnotation(McpTool.class);

        assertNull(annotation);
    }

    @Test
    void testAnnotationPresence() {
        assertTrue(McpTool.class.isAnnotationPresent(java.lang.annotation.Documented.class));
        assertTrue(McpTool.class.isAnnotationPresent(java.lang.annotation.Retention.class));
        assertTrue(McpTool.class.isAnnotationPresent(java.lang.annotation.Target.class));

        assertTrue(McpToolParam.class.isAnnotationPresent(java.lang.annotation.Documented.class));
        assertTrue(McpToolParam.class.isAnnotationPresent(java.lang.annotation.Retention.class));
        assertTrue(McpToolParam.class.isAnnotationPresent(java.lang.annotation.Target.class));
    }

    @Test
    void testAnnotationRetentionAndTarget() {
        java.lang.annotation.Retention retention = McpTool.class.getAnnotation(java.lang.annotation.Retention.class);
        assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME, retention.value());

        java.lang.annotation.Target target = McpTool.class.getAnnotation(java.lang.annotation.Target.class);
        assertEquals(1, target.value().length);
        assertEquals(java.lang.annotation.ElementType.METHOD, target.value()[0]);

        java.lang.annotation.Retention paramRetention =
                McpToolParam.class.getAnnotation(java.lang.annotation.Retention.class);
        assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME, paramRetention.value());

        java.lang.annotation.Target paramTarget = McpToolParam.class.getAnnotation(java.lang.annotation.Target.class);
        assertEquals(2, paramTarget.value().length);
        assertTrue(java.util.Arrays.asList(paramTarget.value()).contains(java.lang.annotation.ElementType.PARAMETER));
        assertTrue(java.util.Arrays.asList(paramTarget.value()).contains(java.lang.annotation.ElementType.FIELD));
    }
}
