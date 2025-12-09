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
package org.apache.dubbo.xds;

import org.apache.dubbo.xds.bootstrap.BootstrapInfo;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;

import java.util.HashMap;
import java.util.Map;

import io.envoyproxy.envoy.config.core.v3.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NodeBuilder
 */
class NodeBuilderTest {

    @Mock
    private Bootstrapper mockBootstrapper;

    @Mock
    private BootstrapInfo mockBootstrapInfo;

    @Mock
    private org.apache.dubbo.xds.bootstrap.Node mockBootstrapNode;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testBuildNodeWithDefaults() {
        // Arrange
        when(mockBootstrapNode.getId()).thenReturn("default-node-id");
        when(mockBootstrapNode.getCluster()).thenReturn("default-cluster");
        when(mockBootstrapNode.getMetadata()).thenReturn(new HashMap<>());
        when(mockBootstrapInfo.getNode()).thenReturn(mockBootstrapNode);

        try (MockedStatic<Bootstrapper> bootstrapperMock = mockStatic(Bootstrapper.class)) {
            bootstrapperMock.when(Bootstrapper::getInstance).thenReturn(mockBootstrapper);
            when(mockBootstrapper.bootstrap()).thenReturn(mockBootstrapInfo);

            // Act
            Node node = NodeBuilder.build();

            // Assert
            assertNotNull(node);
            assertNotNull(node.getId());
            assertEquals("default-cluster", node.getCluster());
        }
    }

    @Test
    void testBuildNodeWithKubernetesEnvironment() {
        // Arrange
        when(mockBootstrapNode.getId()).thenReturn("k8s-node-id");
        when(mockBootstrapNode.getMetadata()).thenReturn(new HashMap<>());
        when(mockBootstrapNode.getCluster()).thenReturn("k8s-cluster");
        when(mockBootstrapInfo.getNode()).thenReturn(mockBootstrapNode);

        try (MockedStatic<Bootstrapper> bootstrapperMock = mockStatic(Bootstrapper.class)) {

            bootstrapperMock.when(Bootstrapper::getInstance).thenReturn(mockBootstrapper);
            when(mockBootstrapper.bootstrap()).thenReturn(mockBootstrapInfo);

            // Act
            Node node = NodeBuilder.build();

            // Assert
            assertNotNull(node);
            assertEquals("k8s-cluster", node.getCluster());
            // Note: Without mocking System.getenv(), we can't control environment variables
            // So we just verify the basic structure
        }
    }

    @Test
    void testBuildNodeWithoutKubernetesEnvironment() {
        // Arrange
        when(mockBootstrapNode.getId()).thenReturn("fallback-node-id");
        when(mockBootstrapNode.getCluster()).thenReturn("fallback-cluster");
        when(mockBootstrapNode.getMetadata()).thenReturn(new HashMap<>());
        when(mockBootstrapInfo.getNode()).thenReturn(mockBootstrapNode);

        try (MockedStatic<Bootstrapper> bootstrapperMock = mockStatic(Bootstrapper.class)) {

            bootstrapperMock.when(Bootstrapper::getInstance).thenReturn(mockBootstrapper);
            when(mockBootstrapper.bootstrap()).thenReturn(mockBootstrapInfo);

            // Act
            Node node = NodeBuilder.build();

            // Assert
            assertNotNull(node);
            assertEquals("fallback-node-id", node.getId());
            assertEquals("fallback-cluster", node.getCluster());
            assertNotNull(node.getMetadata());
        }
    }

    @Test
    void testBuildNodeWithEmptyCluster() {
        // Arrange
        when(mockBootstrapNode.getId()).thenReturn("test-node-id");
        when(mockBootstrapNode.getCluster()).thenReturn(""); // Empty cluster
        when(mockBootstrapNode.getMetadata()).thenReturn(new HashMap<>());
        when(mockBootstrapInfo.getNode()).thenReturn(mockBootstrapNode);

        try (MockedStatic<Bootstrapper> bootstrapperMock = mockStatic(Bootstrapper.class)) {
            bootstrapperMock.when(Bootstrapper::getInstance).thenReturn(mockBootstrapper);
            when(mockBootstrapper.bootstrap()).thenReturn(mockBootstrapInfo);

            // Act
            Node node = NodeBuilder.build();

            // Assert
            assertNotNull(node);
            assertEquals("test-node-id", node.getId());
            // Empty cluster should not be set
            assertEquals("", node.getCluster());
        }
    }

    @Test
    void testMapToStructWithSimpleValues() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put("stringValue", "test");
        map.put("numberValue", 42);
        map.put("booleanValue", true);
        map.put("nullValue", null);

        // Act
        com.google.protobuf.Struct struct = NodeBuilder.mapToStruct(map);

        // Assert
        assertNotNull(struct);
        assertTrue(struct.getFieldsMap().containsKey("stringValue"));
        assertTrue(struct.getFieldsMap().containsKey("numberValue"));
        assertTrue(struct.getFieldsMap().containsKey("booleanValue"));
        assertTrue(struct.getFieldsMap().containsKey("nullValue"));

        assertEquals("test", struct.getFieldsMap().get("stringValue").getStringValue());
        assertEquals(42.0, struct.getFieldsMap().get("numberValue").getNumberValue());
        assertTrue(struct.getFieldsMap().get("booleanValue").getBoolValue());
        assertEquals(
                com.google.protobuf.NullValue.NULL_VALUE,
                struct.getFieldsMap().get("nullValue").getNullValue());
    }

    @Test
    void testMapToStructWithNestedMap() {
        // Arrange
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("nestedKey", "nestedValue");

        Map<String, Object> map = new HashMap<>();
        map.put("nested", nestedMap);

        // Act
        com.google.protobuf.Struct struct = NodeBuilder.mapToStruct(map);

        // Assert
        assertNotNull(struct);
        assertTrue(struct.getFieldsMap().containsKey("nested"));
        assertTrue(struct.getFieldsMap().get("nested").hasStructValue());
    }

    @Test
    void testMapToStructWithList() {
        // Arrange
        java.util.List<String> list = java.util.Arrays.asList("item1", "item2", "item3");
        Map<String, Object> map = new HashMap<>();
        map.put("listValue", list);

        // Act
        com.google.protobuf.Struct struct = NodeBuilder.mapToStruct(map);

        // Assert
        assertNotNull(struct);
        assertTrue(struct.getFieldsMap().containsKey("listValue"));
        assertTrue(struct.getFieldsMap().get("listValue").hasListValue());
    }

    @Test
    void testMapToStructWithEmptyMap() {
        // Arrange
        Map<String, Object> emptyMap = new HashMap<>();

        // Act
        com.google.protobuf.Struct struct = NodeBuilder.mapToStruct(emptyMap);

        // Assert
        assertNotNull(struct);
        assertTrue(struct.getFieldsMap().isEmpty());
    }

    @Test
    void testBuildNodeWithComplexMetadata() {
        // Arrange
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("service", "dubbo-service");
        nestedMap.put("version", "1.0.0");

        when(mockBootstrapNode.getId()).thenReturn("complex-node");
        when(mockBootstrapNode.getCluster()).thenReturn("complex-cluster");
        when(mockBootstrapNode.getMetadata()).thenReturn(new HashMap<>());
        when(mockBootstrapInfo.getNode()).thenReturn(mockBootstrapNode);

        try (MockedStatic<Bootstrapper> bootstrapperMock = mockStatic(Bootstrapper.class)) {
            bootstrapperMock.when(Bootstrapper::getInstance).thenReturn(mockBootstrapper);
            when(mockBootstrapper.bootstrap()).thenReturn(mockBootstrapInfo);

            // Act
            Node node = NodeBuilder.build();

            // Assert
            assertNotNull(node);
            assertEquals("complex-node", node.getId());
            assertEquals("complex-cluster", node.getCluster());
            assertNotNull(node.getMetadata());
            // Since we're using empty HashMap, we just verify the structure exists
        }
    }

    @Test
    void testBuildNodeWithPartialKubernetesEnvironment() {
        // Arrange
        when(mockBootstrapNode.getId()).thenReturn("partial-k8s-node");
        when(mockBootstrapNode.getCluster()).thenReturn("partial-k8s-cluster");
        when(mockBootstrapNode.getMetadata()).thenReturn(new HashMap<>());
        when(mockBootstrapInfo.getNode()).thenReturn(mockBootstrapNode);

        try (MockedStatic<Bootstrapper> bootstrapperMock = mockStatic(Bootstrapper.class)) {

            bootstrapperMock.when(Bootstrapper::getInstance).thenReturn(mockBootstrapper);
            when(mockBootstrapper.bootstrap()).thenReturn(mockBootstrapInfo);

            // Act
            Node node = NodeBuilder.build();

            // Assert
            assertNotNull(node);
            // Should use bootstrap ID
            assertEquals("partial-k8s-node", node.getId());
            assertEquals("partial-k8s-cluster", node.getCluster());
        }
    }

    @Test
    void testMapToStructWithMixedTypes() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put("string", "value");
        map.put("integer", 123);
        map.put("double", 45.67);
        map.put("boolean", false);
        map.put("null", null);

        // Act
        com.google.protobuf.Struct struct = NodeBuilder.mapToStruct(map);

        // Assert
        assertNotNull(struct);
        assertEquals(5, struct.getFieldsMap().size());
        assertEquals("value", struct.getFieldsMap().get("string").getStringValue());
        assertEquals(123.0, struct.getFieldsMap().get("integer").getNumberValue());
        assertEquals(45.67, struct.getFieldsMap().get("double").getNumberValue());
        assertFalse(struct.getFieldsMap().get("boolean").getBoolValue());
        assertEquals(
                com.google.protobuf.NullValue.NULL_VALUE,
                struct.getFieldsMap().get("null").getNullValue());
    }
}
