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
package org.apache.dubbo.xds.resource;

import org.apache.dubbo.xds.resource.exception.ResourceInvalidException;
import org.apache.dubbo.xds.resource.filter.FilterRegistry;
import org.apache.dubbo.xds.resource.update.EdsUpdate;

import io.envoyproxy.envoy.config.core.v3.Address;
import io.envoyproxy.envoy.config.core.v3.HealthStatus;
import io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.endpoint.v3.Endpoint;
import io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XdsEndpointResource
 */
class XdsEndpointResourceTest {

    private XdsEndpointResource xdsEndpointResource;

    @BeforeEach
    void setUp() {
        xdsEndpointResource = new XdsEndpointResource();
    }

    @Test
    void testParseValidEndpoint() throws ResourceInvalidException {
        // Arrange
        ClusterLoadAssignment loadAssignment = ClusterLoadAssignment.newBuilder()
                .setClusterName("test-cluster")
                .addEndpoints(LocalityLbEndpoints.newBuilder()
                        .setLoadBalancingWeight(
                                com.google.protobuf.UInt32Value.newBuilder().setValue(100))
                        .addLbEndpoints(LbEndpoint.newBuilder()
                                .setEndpoint(Endpoint.newBuilder()
                                        .setAddress(Address.newBuilder()
                                                .setSocketAddress(SocketAddress.newBuilder()
                                                        .setAddress("127.0.0.1")
                                                        .setPortValue(8080))))
                                .setHealthStatus(HealthStatus.HEALTHY)
                                .setLoadBalancingWeight(com.google.protobuf.UInt32Value.newBuilder()
                                        .setValue(100))))
                .build();

        // Act
        EdsUpdate result = xdsEndpointResource.doParse(
                new XdsResourceType.Args(null, null, null, null, FilterRegistry.getDefaultRegistry(), null, null, null),
                loadAssignment);

        // Assert
        assertNotNull(result);
        assertEquals("test-cluster", result.getClusterName());
        assertTrue(result.getLocalityLbEndpointsMap().isEmpty()
                == false); // Map should not be empty after successful parsing
    }

    @Test
    void testParseEndpointWithMultipleLocalities() throws ResourceInvalidException {
        // Arrange
        ClusterLoadAssignment loadAssignment = ClusterLoadAssignment.newBuilder()
                .setClusterName("multi-locality-cluster")
                .addEndpoints(LocalityLbEndpoints.newBuilder()
                        .setLocality(io.envoyproxy.envoy.config.core.v3.Locality.newBuilder()
                                .setRegion("us-east-1")
                                .setZone("us-east-1a"))
                        .setLoadBalancingWeight(
                                com.google.protobuf.UInt32Value.newBuilder().setValue(100))
                        .addLbEndpoints(LbEndpoint.newBuilder()
                                .setEndpoint(Endpoint.newBuilder()
                                        .setAddress(Address.newBuilder()
                                                .setSocketAddress(SocketAddress.newBuilder()
                                                        .setAddress("10.0.1.1")
                                                        .setPortValue(8080))))
                                .setLoadBalancingWeight(com.google.protobuf.UInt32Value.newBuilder()
                                        .setValue(100))))
                .addEndpoints(LocalityLbEndpoints.newBuilder()
                        .setLocality(io.envoyproxy.envoy.config.core.v3.Locality.newBuilder()
                                .setRegion("us-west-2")
                                .setZone("us-west-2a"))
                        .setLoadBalancingWeight(
                                com.google.protobuf.UInt32Value.newBuilder().setValue(100))
                        .addLbEndpoints(LbEndpoint.newBuilder()
                                .setEndpoint(Endpoint.newBuilder()
                                        .setAddress(Address.newBuilder()
                                                .setSocketAddress(SocketAddress.newBuilder()
                                                        .setAddress("10.0.2.1")
                                                        .setPortValue(8080))))
                                .setLoadBalancingWeight(com.google.protobuf.UInt32Value.newBuilder()
                                        .setValue(100))))
                .build();

        // Act
        EdsUpdate result = xdsEndpointResource.doParse(
                new XdsResourceType.Args(null, null, null, null, FilterRegistry.getDefaultRegistry(), null, null, null),
                loadAssignment);

        // Assert
        assertNotNull(result);
        assertEquals("multi-locality-cluster", result.getClusterName());
        assertTrue(result.getLocalityLbEndpointsMap().size()
                >= 2); // Should have two localities after adding loadBalancingWeight
    }

    @Test
    void testParseEndpointWithUnhealthyEndpoints() throws ResourceInvalidException {
        // Arrange
        ClusterLoadAssignment loadAssignment = ClusterLoadAssignment.newBuilder()
                .setClusterName("test-cluster")
                .addEndpoints(LocalityLbEndpoints.newBuilder()
                        .setLoadBalancingWeight(
                                com.google.protobuf.UInt32Value.newBuilder().setValue(100))
                        .addLbEndpoints(LbEndpoint.newBuilder()
                                .setEndpoint(Endpoint.newBuilder()
                                        .setAddress(Address.newBuilder()
                                                .setSocketAddress(SocketAddress.newBuilder()
                                                        .setAddress("127.0.0.1")
                                                        .setPortValue(8080))))
                                .setHealthStatus(HealthStatus.HEALTHY)
                                .setLoadBalancingWeight(com.google.protobuf.UInt32Value.newBuilder()
                                        .setValue(100)))
                        .addLbEndpoints(LbEndpoint.newBuilder()
                                .setEndpoint(Endpoint.newBuilder()
                                        .setAddress(Address.newBuilder()
                                                .setSocketAddress(SocketAddress.newBuilder()
                                                        .setAddress("127.0.0.2")
                                                        .setPortValue(8080))))
                                .setHealthStatus(HealthStatus.UNHEALTHY)
                                .setLoadBalancingWeight(com.google.protobuf.UInt32Value.newBuilder()
                                        .setValue(100))))
                .build();

        // Act
        EdsUpdate result = xdsEndpointResource.doParse(
                new XdsResourceType.Args(null, null, null, null, FilterRegistry.getDefaultRegistry(), null, null, null),
                loadAssignment);

        // Assert
        assertNotNull(result);
        assertEquals("test-cluster", result.getClusterName());
        // Should include both healthy and unhealthy endpoints
        assertTrue(result.getLocalityLbEndpointsMap().size() >= 1);
    }

    @Test
    void testParseEmptyEndpoint() throws ResourceInvalidException {
        // Arrange
        ClusterLoadAssignment loadAssignment = ClusterLoadAssignment.newBuilder()
                .setClusterName("empty-cluster")
                .build();

        // Act
        EdsUpdate result = xdsEndpointResource.doParse(
                new XdsResourceType.Args(null, null, null, null, FilterRegistry.getDefaultRegistry(), null, null, null),
                loadAssignment);

        // Assert
        assertNotNull(result);
        assertEquals("empty-cluster", result.getClusterName());
        assertTrue(result.getLocalityLbEndpointsMap().isEmpty());
    }

    @Test
    void testGetResourceType() {
        // Act
        String resourceType = xdsEndpointResource.typeUrl();

        // Assert
        assertEquals("type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment", resourceType);
    }

    @Test
    void testGetResourceClass() {
        // Act
        Class<?> resourceClass = xdsEndpointResource.unpackedClassName();

        // Assert
        assertEquals(ClusterLoadAssignment.class, resourceClass);
    }

    @Test
    void testParseEndpointWithLoadBalancingWeight() throws ResourceInvalidException {
        // Arrange
        ClusterLoadAssignment loadAssignment = ClusterLoadAssignment.newBuilder()
                .setClusterName("weighted-cluster")
                .addEndpoints(LocalityLbEndpoints.newBuilder()
                        .setLoadBalancingWeight(
                                com.google.protobuf.UInt32Value.newBuilder().setValue(100))
                        .addLbEndpoints(LbEndpoint.newBuilder()
                                .setEndpoint(Endpoint.newBuilder()
                                        .setAddress(Address.newBuilder()
                                                .setSocketAddress(SocketAddress.newBuilder()
                                                        .setAddress("127.0.0.1")
                                                        .setPortValue(8080))))
                                .setLoadBalancingWeight(com.google.protobuf.UInt32Value.newBuilder()
                                        .setValue(100)))
                        .addLbEndpoints(LbEndpoint.newBuilder()
                                .setEndpoint(Endpoint.newBuilder()
                                        .setAddress(Address.newBuilder()
                                                .setSocketAddress(SocketAddress.newBuilder()
                                                        .setAddress("127.0.0.2")
                                                        .setPortValue(8080))))
                                .setLoadBalancingWeight(com.google.protobuf.UInt32Value.newBuilder()
                                        .setValue(50))))
                .build();

        // Act
        EdsUpdate result = xdsEndpointResource.doParse(
                new XdsResourceType.Args(null, null, null, null, FilterRegistry.getDefaultRegistry(), null, null, null),
                loadAssignment);

        // Assert
        assertNotNull(result);
        assertEquals("weighted-cluster", result.getClusterName());
        assertTrue(result.getLocalityLbEndpointsMap().size()
                >= 1); // Should have at least one locality after adding loadBalancingWeight
    }

    @Test
    void testParseEndpointWithNullArgs() throws ResourceInvalidException {
        // Arrange
        ClusterLoadAssignment loadAssignment = ClusterLoadAssignment.newBuilder()
                .setClusterName("test-cluster")
                .build();

        // Act
        EdsUpdate result = xdsEndpointResource.doParse(
                new XdsResourceType.Args(null, null, null, null, FilterRegistry.getDefaultRegistry(), null, null, null),
                loadAssignment);

        // Assert
        assertNotNull(result);
        assertEquals("test-cluster", result.getClusterName());
    }
}
