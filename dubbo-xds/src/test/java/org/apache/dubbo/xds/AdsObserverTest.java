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
import org.apache.dubbo.xds.bootstrap.XdsServer;
import org.apache.dubbo.xds.resource.XdsResourceType;
import org.apache.dubbo.xds.resource.update.ResourceUpdate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AdsObserver
 */
class AdsObserverTest {

    @Mock
    private XdsResourceListener<ResourceUpdate> resourceListener;

    @Mock
    private XdsResourceType<ResourceUpdate> resourceType;

    @Mock
    private Bootstrapper mockBootstrapper;

    @Mock
    private BootstrapInfo mockBootstrapInfo;

    @Mock
    private org.apache.dubbo.xds.bootstrap.Node mockBootstrapNode;

    @Mock
    private XdsServer mockXdsServer;

    private AdsObserver adsObserver;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock the bootstrap chain to avoid initialization issues
        when(mockBootstrapNode.getId()).thenReturn("test-node-id");
        when(mockBootstrapNode.getCluster()).thenReturn("test-cluster");
        when(mockBootstrapNode.getMetadata()).thenReturn(new HashMap<>());
        when(mockBootstrapInfo.getNode()).thenReturn(mockBootstrapNode);

        // Mock XDS server info to avoid IndexOutOfBoundsException in XdsChannel
        when(mockXdsServer.getServerURI()).thenReturn("istiod.istio-system.svc:15010");
        when(mockBootstrapInfo.getXdsServers()).thenReturn(Arrays.asList(mockXdsServer));

        try (MockedStatic<Bootstrapper> bootstrapperMock = mockStatic(Bootstrapper.class)) {
            bootstrapperMock.when(Bootstrapper::getInstance).thenReturn(mockBootstrapper);
            when(mockBootstrapper.bootstrap()).thenReturn(mockBootstrapInfo);

            // Create AdsObserver with mocked bootstrap
            adsObserver = new AdsObserver();
        }
    }

    @Test
    void testConstructor() {
        // Act & Assert
        // Use the already created adsObserver from setUp which has bootstrap mocks
        assertNotNull(adsObserver);
    }

    @Test
    void testAddListener() {
        // Arrange
        String resourceName = "test-resource";
        when(resourceType.typeUrl()).thenReturn("type.googleapis.com/envoy.config.cluster.v3.Cluster");

        // Act & Assert
        assertDoesNotThrow(() -> adsObserver.addListener(resourceName, resourceType, resourceListener));
    }

    @Test
    void testAddListenerWithNullResourceName() {
        // Arrange
        String resourceName = null;

        // Act & Assert
        // ConcurrentHashMap doesn't allow null keys, so this should throw NullPointerException
        assertThrows(NullPointerException.class, () ->
            adsObserver.addListener(resourceName, resourceType, resourceListener));
    }

    @Test
    void testHasSubscribed() {
        // Arrange
        String resourceName = "test-resource";
        when(resourceType.typeUrl()).thenReturn("type.googleapis.com/envoy.config.cluster.v3.Cluster");

        // Act
        adsObserver.saveSubscribedType(resourceType); // Need to save the type first
        adsObserver.addListener(resourceName, resourceType, resourceListener);
        boolean hasSubscribed = adsObserver.hasSubscribed(resourceType);

        // Assert
        assertTrue(hasSubscribed);
    }

    @Test
    void testHasNotSubscribed() {
        // Act
        boolean hasSubscribed = adsObserver.hasSubscribed(resourceType);

        // Assert
        assertFalse(hasSubscribed);
    }

    @Test
    void testSaveSubscribedType() {
        // Arrange
        when(resourceType.typeUrl()).thenReturn("type.googleapis.com/envoy.config.cluster.v3.Cluster");

        // Act & Assert
        assertDoesNotThrow(() -> adsObserver.saveSubscribedType(resourceType));
        assertTrue(adsObserver.hasSubscribed(resourceType));
    }

    @Test
    void testBuildDiscoveryRequest() {
        // Arrange
        String resourceName = "test-resource";
        when(resourceType.typeUrl()).thenReturn("type.googleapis.com/envoy.config.cluster.v3.Cluster");

        adsObserver.addListener(resourceName, resourceType, resourceListener);

        // Act & Assert
        assertDoesNotThrow(() -> adsObserver.adjustResourceSubscription(resourceType));
    }

    @Test
    void testGetResourcesToObserve() {
        // Arrange
        String resourceName1 = "test-resource-1";
        String resourceName2 = "test-resource-2";
        when(resourceType.typeUrl()).thenReturn("type.googleapis.com/envoy.config.cluster.v3.Cluster");

        adsObserver.addListener(resourceName1, resourceType, resourceListener);
        adsObserver.addListener(resourceName2, resourceType, resourceListener);

        // Act
        Set<String> resources = adsObserver.getResourcesToObserve(resourceType);

        // Assert
        assertNotNull(resources);
        assertEquals(2, resources.size());
        assertTrue(resources.contains(resourceName1));
        assertTrue(resources.contains(resourceName2));
    }

    @Test
    void testGetResourcesToObserveWithNoSubscriptions() {
        // Act
        Set<String> resources = adsObserver.getResourcesToObserve(resourceType);

        // Assert
        assertNotNull(resources);
        assertTrue(resources.isEmpty());
    }

    @Test
    void testDestroy() {
        // Act & Assert
        assertDoesNotThrow(() -> adsObserver.destroy());
    }

    @Test
    void testMultipleResourceTypes() {
        // Arrange
        XdsResourceType<ResourceUpdate> resourceType2 = mock(XdsResourceType.class);
        when(resourceType.typeUrl()).thenReturn("type.googleapis.com/envoy.config.cluster.v3.Cluster");
        when(resourceType2.typeUrl()).thenReturn("type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment");

        String resourceName1 = "cluster-resource";
        String resourceName2 = "endpoint-resource";

        // Act
        adsObserver.saveSubscribedType(resourceType); // Need to save the types first
        adsObserver.saveSubscribedType(resourceType2);
        adsObserver.addListener(resourceName1, resourceType, resourceListener);
        adsObserver.addListener(resourceName2, resourceType2, resourceListener);

        // Assert
        assertTrue(adsObserver.hasSubscribed(resourceType));
        assertTrue(adsObserver.hasSubscribed(resourceType2));

        Set<String> clusterResources = adsObserver.getResourcesToObserve(resourceType);
        Set<String> endpointResources = adsObserver.getResourcesToObserve(resourceType2);

        assertEquals(1, clusterResources.size());
        assertEquals(1, endpointResources.size());
        assertTrue(clusterResources.contains(resourceName1));
        assertTrue(endpointResources.contains(resourceName2));
    }
}
