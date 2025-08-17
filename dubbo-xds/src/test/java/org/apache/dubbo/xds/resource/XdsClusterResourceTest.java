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

import org.apache.dubbo.xds.bootstrap.BootstrapInfo;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.XdsServer;
import org.apache.dubbo.xds.resource.exception.ResourceInvalidException;
import org.apache.dubbo.xds.resource.update.CdsUpdate;

import java.util.Arrays;
import java.util.HashMap;

import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.envoyproxy.envoy.config.core.v3.Address;
import io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.endpoint.v3.Endpoint;
import io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for XdsClusterResource
 */
class XdsClusterResourceTest {

    private XdsClusterResource xdsClusterResource;
    private XdsResourceType.Args args;

    @Mock
    private Bootstrapper mockBootstrapper;

    @Mock
    private BootstrapInfo mockBootstrapInfo;

    @Mock
    private org.apache.dubbo.xds.bootstrap.Node mockBootstrapNode;

    @Mock
    private XdsServer mockXdsServer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock the bootstrap chain to avoid initialization issues
        when(mockBootstrapNode.getId()).thenReturn("test-node-id");
        when(mockBootstrapNode.getCluster()).thenReturn("test-cluster");
        when(mockBootstrapNode.getMetadata()).thenReturn(new HashMap<>());
        when(mockBootstrapInfo.getNode()).thenReturn(mockBootstrapNode);

        // Mock XDS server info to avoid NullPointerException in CdsUpdate.forEds
        when(mockXdsServer.getServerURI()).thenReturn("istiod.istio-system.svc:15010");
        when(mockBootstrapInfo.getXdsServers()).thenReturn(Arrays.asList(mockXdsServer));

        try (MockedStatic<Bootstrapper> bootstrapperMock = mockStatic(Bootstrapper.class)) {
            bootstrapperMock.when(Bootstrapper::getInstance).thenReturn(mockBootstrapper);
            when(mockBootstrapper.bootstrap()).thenReturn(mockBootstrapInfo);

            xdsClusterResource = XdsClusterResource.getInstance();
            args = XdsResourceType.xdsResourceTypeArgs;
        }
    }

    @Test
    void testParseValidCluster() throws ResourceInvalidException {
        // Arrange
        Cluster cluster = Cluster.newBuilder()
                .setName("test-cluster")
                .setType(Cluster.DiscoveryType.EDS)
                .setEdsClusterConfig(Cluster.EdsClusterConfig.newBuilder()
                        .setEdsConfig(io.envoyproxy.envoy.config.core.v3.ConfigSource.newBuilder()
                                .setAds(
                                        io.envoyproxy.envoy.config.core.v3.AggregatedConfigSource
                                                .getDefaultInstance())))
                .build();

        // Act & Assert
        try (MockedStatic<Bootstrapper> bootstrapperMock = mockStatic(Bootstrapper.class)) {
            bootstrapperMock.when(Bootstrapper::getInstance).thenReturn(mockBootstrapper);
            when(mockBootstrapper.bootstrap()).thenReturn(mockBootstrapInfo);

            CdsUpdate result = xdsClusterResource.doParse(args, cluster);

            assertNotNull(result);
            assertEquals("test-cluster", result.getClusterName());
            assertEquals(CdsUpdate.ClusterType.EDS, result.getClusterType());
        }
    }

    @Test
    void testParseClusterWithUnsupportedType() {
        // Arrange
        Cluster cluster = Cluster.newBuilder()
                .setName("test-cluster")
                .setType(Cluster.DiscoveryType.STATIC)
                .build();

        // Act & Assert
        assertThrows(ResourceInvalidException.class, () -> xdsClusterResource.doParse(args, cluster));
    }

    @Test
    void testParseClusterWithTransportSocketMatches() {
        // Arrange
        Cluster cluster = Cluster.newBuilder()
                .setName("test-cluster")
                .setType(Cluster.DiscoveryType.EDS)
                .addTransportSocketMatches(
                        Cluster.TransportSocketMatch.newBuilder().setName("tls").build())
                .setEdsClusterConfig(Cluster.EdsClusterConfig.newBuilder()
                        .setEdsConfig(io.envoyproxy.envoy.config.core.v3.ConfigSource.newBuilder()
                                .setAds(
                                        io.envoyproxy.envoy.config.core.v3.AggregatedConfigSource
                                                .getDefaultInstance())))
                .build();

        // Act & Assert
        try (MockedStatic<Bootstrapper> bootstrapperMock = mockStatic(Bootstrapper.class)) {
            bootstrapperMock.when(Bootstrapper::getInstance).thenReturn(mockBootstrapper);
            when(mockBootstrapper.bootstrap()).thenReturn(mockBootstrapInfo);

            // Transport socket matches are not supported but parsing should succeed with warning
            assertDoesNotThrow(() -> {
                CdsUpdate result = xdsClusterResource.doParse(args, cluster);
                assertNotNull(result);
            });
        }
    }

    @Test
    void testParseClusterWithLogicalDns() throws ResourceInvalidException {
        // Arrange
        Cluster cluster = Cluster.newBuilder()
                .setName("test-cluster")
                .setType(Cluster.DiscoveryType.LOGICAL_DNS)
                .setLoadAssignment(ClusterLoadAssignment.newBuilder()
                        .setClusterName("test-cluster")
                        .addEndpoints(LocalityLbEndpoints.newBuilder()
                                .addLbEndpoints(LbEndpoint.newBuilder()
                                        .setEndpoint(Endpoint.newBuilder()
                                                .setAddress(Address.newBuilder()
                                                        .setSocketAddress(SocketAddress.newBuilder()
                                                                .setAddress("127.0.0.1")
                                                                .setPortValue(8080)))))))
                .build();

        // Act
        CdsUpdate result = xdsClusterResource.doParse(args, cluster);

        // Assert
        assertNotNull(result);
        assertEquals("test-cluster", result.getClusterName());
        assertEquals(CdsUpdate.ClusterType.LOGICAL_DNS, result.getClusterType());
        assertEquals("127.0.0.1:8080", result.getDnsHostName()); // DNS hostname includes port
    }

    @Test
    void testTypeUrl() {
        // Act
        String typeUrl = xdsClusterResource.typeUrl();

        // Assert
        assertEquals("type.googleapis.com/envoy.config.cluster.v3.Cluster", typeUrl);
    }

    @Test
    void testUnpackedClassName() {
        // Act
        Class<?> resourceClass = xdsClusterResource.unpackedClassName();

        // Assert
        assertEquals(Cluster.class, resourceClass);
    }

    @Test
    void testTypeName() {
        // Act
        String typeName = xdsClusterResource.typeName();

        // Assert
        assertEquals("CDS", typeName);
    }

    @Test
    void testIsFullStateOfTheWorld() {
        // Act
        boolean isFullStateOfTheWorld = xdsClusterResource.isFullStateOfTheWorld();

        // Assert
        assertTrue(isFullStateOfTheWorld);
    }

    @Test
    void testParseEmptyCluster() throws ResourceInvalidException {
        // Arrange
        Cluster cluster = Cluster.newBuilder()
                .setName("")
                .setType(Cluster.DiscoveryType.EDS)
                .setEdsClusterConfig(Cluster.EdsClusterConfig.newBuilder()
                        .setEdsConfig(io.envoyproxy.envoy.config.core.v3.ConfigSource.newBuilder()
                                .setAds(
                                        io.envoyproxy.envoy.config.core.v3.AggregatedConfigSource
                                                .getDefaultInstance())))
                .build();

        // Act & Assert
        try (MockedStatic<Bootstrapper> bootstrapperMock = mockStatic(Bootstrapper.class)) {
            bootstrapperMock.when(Bootstrapper::getInstance).thenReturn(mockBootstrapper);
            when(mockBootstrapper.bootstrap()).thenReturn(mockBootstrapInfo);

            CdsUpdate result = xdsClusterResource.doParse(args, cluster);

            assertNotNull(result);
            assertEquals("", result.getClusterName());
        }
    }

    @Test
    void testExtractResourceName() {
        // Arrange
        Cluster cluster = Cluster.newBuilder().setName("test-cluster").build();

        // Act
        String resourceName = xdsClusterResource.extractResourceName(cluster);

        // Assert
        assertEquals("test-cluster", resourceName);
    }

    @Test
    void testParseClusterWithOriginalDst() {
        // Arrange
        Cluster cluster = Cluster.newBuilder()
                .setName("PassthroughCluster")
                .setType(Cluster.DiscoveryType.ORIGINAL_DST)
                .build();

        // Act & Assert
        assertThrows(ResourceInvalidException.class, () -> xdsClusterResource.doParse(args, cluster));
    }

    @Test
    void testParseClusterWithComplexConfiguration() throws ResourceInvalidException {
        // Arrange
        Cluster cluster = Cluster.newBuilder()
                .setName("complex-cluster")
                .setType(Cluster.DiscoveryType.EDS)
                .setEdsClusterConfig(Cluster.EdsClusterConfig.newBuilder()
                        .setEdsConfig(io.envoyproxy.envoy.config.core.v3.ConfigSource.newBuilder()
                                .setAds(io.envoyproxy.envoy.config.core.v3.AggregatedConfigSource.getDefaultInstance()))
                        .setServiceName("complex-service"))
                .setConnectTimeout(
                        com.google.protobuf.Duration.newBuilder().setSeconds(5).setNanos(0))
                .build();

        // Act & Assert
        try (MockedStatic<Bootstrapper> bootstrapperMock = mockStatic(Bootstrapper.class)) {
            bootstrapperMock.when(Bootstrapper::getInstance).thenReturn(mockBootstrapper);
            when(mockBootstrapper.bootstrap()).thenReturn(mockBootstrapInfo);

            CdsUpdate result = xdsClusterResource.doParse(args, cluster);

            assertNotNull(result);
            assertEquals("complex-cluster", result.getClusterName());
            assertEquals("complex-service", result.getEdsServiceName());
        }
    }

    @Test
    void testParseInvalidMessageType() {
        // Arrange
        io.envoyproxy.envoy.config.listener.v3.Listener invalidMessage =
                io.envoyproxy.envoy.config.listener.v3.Listener.newBuilder()
                        .setName("invalid")
                        .build();

        // Act & Assert
        assertThrows(ResourceInvalidException.class, () -> xdsClusterResource.doParse(args, invalidMessage));
    }

    @Test
    void testParseClusterWithAggregateType() throws ResourceInvalidException {
        // Arrange
        Cluster cluster = Cluster.newBuilder()
                .setName("aggregate-cluster")
                .setClusterType(Cluster.CustomClusterType.newBuilder()
                        .setName("envoy.clusters.aggregate")
                        .setTypedConfig(com.google.protobuf.Any.newBuilder()
                                .setTypeUrl(
                                        "type.googleapis.com/envoy.extensions.clusters.aggregate.v3.ClusterConfig")))
                .build();

        // Act
        CdsUpdate result = xdsClusterResource.doParse(args, cluster);

        // Assert
        assertNotNull(result);
        assertEquals("aggregate-cluster", result.getClusterName());
        assertEquals(CdsUpdate.ClusterType.AGGREGATE, result.getClusterType());
    }
}
