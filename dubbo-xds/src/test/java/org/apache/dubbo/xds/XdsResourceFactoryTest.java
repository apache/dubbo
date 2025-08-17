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
import org.apache.dubbo.xds.registry.EdsListener;
import org.apache.dubbo.xds.resource.update.CdsUpdate;
import org.apache.dubbo.xds.resource.update.EdsUpdate;
import org.apache.dubbo.xds.resource.update.LdsUpdate;
import org.apache.dubbo.xds.resource.update.RdsUpdate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for XdsResourceFactory
 */
class XdsResourceFactoryTest {

    @Mock
    private EdsListener edsListener;

    @Mock
    private Bootstrapper mockBootstrapper;

    @Mock
    private BootstrapInfo mockBootstrapInfo;

    @Mock
    private org.apache.dubbo.xds.bootstrap.Node mockBootstrapNode;

    @Mock
    private XdsServer mockXdsServer;

    private XdsResourceFactory xdsResourceFactory;

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

            // Create XdsResourceFactory with mocked bootstrap
            xdsResourceFactory = XdsResourceFactory.getInstance();
        }
    }

    @Test
    void testSubscribeApp() {
        // Arrange
        String appName = "test-service:8080";

        // Act & Assert
        assertDoesNotThrow(() -> xdsResourceFactory.subscribeApp(appName, edsListener));
    }

    @Test
    void testSubscribeAppWithInvalidName() {
        // Arrange
        String appName = "test-service"; // Missing port

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> xdsResourceFactory.subscribeApp(appName, edsListener));
    }

    @Test
    void testCreateLdsWatcher() {
        // Arrange
        String resourceName = "test-listener:8080";

        // Act
        XdsResourceFactory.LdsUpdateWatcher watcher = xdsResourceFactory.new LdsUpdateWatcher(resourceName);

        // Assert
        assertNotNull(watcher);
    }

    @Test
    void testCreateRdsWatcher() {
        // Arrange
        String rdsName = "test-route-config";
        long httpMaxStreamDurationNano = 30000000000L; // 30 seconds in nanoseconds

        XdsResourceFactory.LdsUpdateWatcher ldsWatcher = xdsResourceFactory.new LdsUpdateWatcher("test-listener:8080");

        // Act
        XdsResourceFactory.LdsUpdateWatcher.RdsUpdateWatcher watcher =
                ldsWatcher.new RdsUpdateWatcher(rdsName, httpMaxStreamDurationNano, null);

        // Assert
        assertNotNull(watcher);
    }

    @Test
    void testCreateCdsWatcher() {
        // Arrange
        XdsResourceFactory.LdsUpdateWatcher ldsWatcher = xdsResourceFactory.new LdsUpdateWatcher("test-listener:8080");

        // Act
        XdsResourceFactory.LdsUpdateWatcher.CdsUpdateNodeDirectory watcher = ldsWatcher.new CdsUpdateNodeDirectory();

        // Assert
        assertNotNull(watcher);
    }

    @Test
    void testCreateEdsWatcher() {
        // Arrange
        String clusterName = "test-cluster";
        XdsResourceFactory.LdsUpdateWatcher ldsWatcher = xdsResourceFactory.new LdsUpdateWatcher("test-listener:8080");

        // Act
        XdsResourceFactory.LdsUpdateWatcher.EdsUpdateLeafDirectory watcher =
                ldsWatcher.new EdsUpdateLeafDirectory(clusterName);

        // Assert
        assertNotNull(watcher);
    }

    @Test
    void testLdsUpdateWatcherOnResourceUpdate() {
        // Arrange
        String resourceName = "test-listener:8080";
        XdsResourceFactory.LdsUpdateWatcher watcher = xdsResourceFactory.new LdsUpdateWatcher(resourceName);

        LdsUpdate ldsUpdate = mock(LdsUpdate.class);
        org.apache.dubbo.xds.resource.listener.HttpConnectionManager httpConnectionManager =
                mock(org.apache.dubbo.xds.resource.listener.HttpConnectionManager.class);

        when(ldsUpdate.getHttpConnectionManager()).thenReturn(httpConnectionManager);
        when(httpConnectionManager.getVirtualHosts()).thenReturn(Collections.emptyList());

        // Act & Assert
        assertDoesNotThrow(() -> watcher.onResourceUpdate(ldsUpdate));
    }

    @Test
    void testLdsUpdateWatcherOnResourceUpdateWithNull() {
        // Arrange
        String resourceName = "test-listener:8080";
        XdsResourceFactory.LdsUpdateWatcher watcher = xdsResourceFactory.new LdsUpdateWatcher(resourceName);

        // Act & Assert
        assertDoesNotThrow(() -> watcher.onResourceUpdate(null));
    }

    @Test
    void testGettersReturnNonNullMaps() {
        // Act & Assert
        assertNotNull(xdsResourceFactory.getEdsListeners());
        assertNotNull(xdsResourceFactory.getXdsVirtualHostMap());
        assertNotNull(xdsResourceFactory.getXdsClusterMap());
        assertNotNull(xdsResourceFactory.getXdsEdsMap());
        assertNotNull(xdsResourceFactory.getLdsWatchers());
        assertNotNull(xdsResourceFactory.getRdsWatchers());
        assertNotNull(xdsResourceFactory.getCdsWatchers());
        assertNotNull(xdsResourceFactory.getEdsWatchers());
    }

    @Test
    void testRdsUpdateWatcherOnResourceUpdate() {
        // Arrange
        XdsResourceFactory.LdsUpdateWatcher ldsWatcher = xdsResourceFactory.new LdsUpdateWatcher("test-listener:8080");
        XdsResourceFactory.LdsUpdateWatcher.RdsUpdateWatcher rdsWatcher =
                ldsWatcher.new RdsUpdateWatcher("test-route-config", 30000000000L, null);

        RdsUpdate rdsUpdate = mock(RdsUpdate.class);
        when(rdsUpdate.getVirtualHosts()).thenReturn(Collections.emptyList());

        // Act & Assert
        assertDoesNotThrow(() -> rdsWatcher.onResourceUpdate(rdsUpdate));
    }

    @Test
    void testCdsUpdateWatcherOnResourceUpdate() {
        // Arrange
        XdsResourceFactory.LdsUpdateWatcher ldsWatcher = xdsResourceFactory.new LdsUpdateWatcher("test-listener:8080");
        XdsResourceFactory.LdsUpdateWatcher.CdsUpdateNodeDirectory cdsWatcher = ldsWatcher.new CdsUpdateNodeDirectory();

        CdsUpdate cdsUpdate = mock(CdsUpdate.class);
        when(cdsUpdate.getClusterName()).thenReturn("test-cluster");
        when(cdsUpdate.getClusterType()).thenReturn(CdsUpdate.ClusterType.EDS);

        // Act & Assert
        assertDoesNotThrow(() -> cdsWatcher.onResourceUpdate(cdsUpdate));
    }

    @Test
    void testCdsUpdateWatcherOnResourceUpdateWithNull() {
        // Arrange
        XdsResourceFactory.LdsUpdateWatcher ldsWatcher = xdsResourceFactory.new LdsUpdateWatcher("test-listener:8080");
        XdsResourceFactory.LdsUpdateWatcher.CdsUpdateNodeDirectory cdsWatcher = ldsWatcher.new CdsUpdateNodeDirectory();

        // Act & Assert
        assertDoesNotThrow(() -> cdsWatcher.onResourceUpdate(null));
    }

    @Test
    void testEdsUpdateWatcherOnResourceUpdate() {
        // Arrange
        // Subscribe with the same name as the LDS resource name to match the lookup logic
        String ldsResourceName = "test-listener:8080";
        String clusterName = "test-cluster";
        xdsResourceFactory.subscribeApp(ldsResourceName, edsListener);

        XdsResourceFactory.LdsUpdateWatcher ldsWatcher = xdsResourceFactory.new LdsUpdateWatcher(ldsResourceName);
        XdsResourceFactory.LdsUpdateWatcher.EdsUpdateLeafDirectory edsWatcher =
                ldsWatcher.new EdsUpdateLeafDirectory(clusterName);

        EdsUpdate edsUpdate = mock(EdsUpdate.class);
        when(edsUpdate.getClusterName()).thenReturn(clusterName);
        when(edsUpdate.getLocalityLbEndpointsMap()).thenReturn(Collections.emptyMap());

        // Act & Assert
        // Now the EdsUpdateLeafDirectory should find the listeners by ldsResourceName
        assertDoesNotThrow(() -> edsWatcher.onResourceUpdate(edsUpdate));
    }

    @Test
    void testMultipleWatchersCreation() {
        // Act
        XdsResourceFactory.LdsUpdateWatcher watcher1 = xdsResourceFactory.new LdsUpdateWatcher("listener-1:8080");
        XdsResourceFactory.LdsUpdateWatcher watcher2 = xdsResourceFactory.new LdsUpdateWatcher("listener-2:8080");

        // Assert
        assertNotNull(watcher1);
        assertNotNull(watcher2);
        assertNotEquals(watcher1, watcher2);
    }

    @Test
    void testMultipleSubscriptions() {
        // Arrange
        String appName1 = "service-1:8080";
        String appName2 = "service-2:8080";

        // Act & Assert
        assertDoesNotThrow(() -> {
            xdsResourceFactory.subscribeApp(appName1, edsListener);
            xdsResourceFactory.subscribeApp(appName2, edsListener);
        });

        // Verify that listeners are registered
        assertTrue(xdsResourceFactory.getEdsListeners().containsKey(appName1));
        assertTrue(xdsResourceFactory.getEdsListeners().containsKey(appName2));
    }

    @Test
    void testSubscribeAppMultipleListeners() {
        // Arrange
        String appName = "test-service:8080";
        EdsListener listener2 = mock(EdsListener.class);

        // Act
        xdsResourceFactory.subscribeApp(appName, edsListener);
        xdsResourceFactory.subscribeApp(appName, listener2);

        // Assert
        List<EdsListener> listeners = xdsResourceFactory.getEdsListeners().get(appName);
        assertNotNull(listeners);
        assertEquals(2, listeners.size());
        assertTrue(listeners.contains(edsListener));
        assertTrue(listeners.contains(listener2));
    }

    @Test
    void testSubscribeAppSameListenerTwice() {
        // Arrange
        String appName = "unique-service:8080"; // Use unique name to avoid conflicts

        // Act
        xdsResourceFactory.subscribeApp(appName, edsListener);
        xdsResourceFactory.subscribeApp(appName, edsListener); // Same listener again

        // Assert
        List<EdsListener> listeners = xdsResourceFactory.getEdsListeners().get(appName);
        assertNotNull(listeners);
        assertEquals(1, listeners.size()); // Should not duplicate
        assertTrue(listeners.contains(edsListener));
    }

    @Test
    void testWatcherWithEmptyResourceName() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            XdsResourceFactory.LdsUpdateWatcher watcher = xdsResourceFactory.new LdsUpdateWatcher("");
            assertNotNull(watcher);
        });
    }
}
