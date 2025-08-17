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
import org.apache.dubbo.xds.resource.update.CdsUpdate;
import org.apache.dubbo.xds.resource.update.EdsUpdate;
import org.apache.dubbo.xds.resource.update.LdsUpdate;
import org.apache.dubbo.xds.resource.update.ResourceUpdate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PilotExchanger
 */
class PilotExchangerTest {

    @Mock
    private XdsResourceType<LdsUpdate> ldsResourceType;

    @Mock
    private XdsResourceType<CdsUpdate> cdsResourceType;

    @Mock
    private XdsResourceType<EdsUpdate> edsResourceType;

    @Mock
    private XdsResourceListener<LdsUpdate> ldsResourceListener;

    @Mock
    private XdsResourceListener<CdsUpdate> cdsResourceListener;

    @Mock
    private XdsResourceListener<EdsUpdate> edsResourceListener;

    @Mock
    private AdsObserver mockAdsObserver;

    @Mock
    private Bootstrapper mockBootstrapper;

    @Mock
    private BootstrapInfo mockBootstrapInfo;

    @Mock
    private org.apache.dubbo.xds.bootstrap.Node mockBootstrapNode;

    @Mock
    private XdsServer mockXdsServer;

    private PilotExchanger pilotExchanger;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Reset the singleton instance before each test
        resetSingletonInstance();

        // Mock the bootstrap chain to avoid initialization issues
        when(mockBootstrapNode.getId()).thenReturn("test-node-id");
        when(mockBootstrapNode.getCluster()).thenReturn("test-cluster");
        when(mockBootstrapNode.getMetadata()).thenReturn(new HashMap<>());
        when(mockBootstrapInfo.getNode()).thenReturn(mockBootstrapNode);

        // Mock XDS server info to avoid IndexOutOfBoundsException in XdsChannel
        when(mockXdsServer.getServerURI()).thenReturn("istiod.istio-system.svc:15010");
        when(mockBootstrapInfo.getXdsServers()).thenReturn(Arrays.asList(mockXdsServer));

        // Mock resource types
        when(ldsResourceType.typeUrl()).thenReturn("type.googleapis.com/envoy.config.listener.v3.Listener");
        when(cdsResourceType.typeUrl()).thenReturn("type.googleapis.com/envoy.config.cluster.v3.Cluster");
        when(edsResourceType.typeUrl()).thenReturn("type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment");

        try (MockedStatic<Bootstrapper> bootstrapperMock = mockStatic(Bootstrapper.class)) {
            bootstrapperMock.when(Bootstrapper::getInstance).thenReturn(mockBootstrapper);
            when(mockBootstrapper.bootstrap()).thenReturn(mockBootstrapInfo);

            // Create PilotExchanger with mocked bootstrap
            pilotExchanger = PilotExchanger.getInstance();
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up singleton instance after each test
        resetSingletonInstance();
    }

    private void resetSingletonInstance() throws Exception {
        Field instanceField = PilotExchanger.class.getDeclaredField("GLOBAL_PILOT_EXCHANGER");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void testGetInstanceReturnsSameInstance() {
        // Act - use the already created pilotExchanger from setUp
        PilotExchanger instance1 = pilotExchanger;
        PilotExchanger instance2 = PilotExchanger.getInstance();

        // Assert
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertSame(instance1, instance2);
    }

    @Test
    void testGetInstanceThreadSafety() throws InterruptedException {
        // Arrange - since we already have a singleton instance from setUp,
        // we can test that multiple calls return the same instance
        final PilotExchanger[] instances = new PilotExchanger[2];
        Thread thread1 = new Thread(() -> instances[0] = pilotExchanger);
        Thread thread2 = new Thread(() -> instances[1] = PilotExchanger.getInstance());

        // Act
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Assert
        assertNotNull(instances[0]);
        assertNotNull(instances[1]);
        assertSame(instances[0], instances[1]);
    }

    @Test
    void testSubscribeXdsResourceNewSubscription() {
        // Arrange
        String resourceName = "test-listener";

        // Act
        pilotExchanger.subscribeXdsResource(resourceName, ldsResourceType, ldsResourceListener);

        // Assert - verify that the subscription was processed
        assertDoesNotThrow(() -> pilotExchanger.subscribeXdsResource(resourceName, ldsResourceType, ldsResourceListener));
    }

    @Test
    void testSubscribeXdsResourceWithNullResourceName() {
        // Act & Assert
        // ConcurrentHashMap doesn't allow null keys, so this should throw NullPointerException
        assertThrows(NullPointerException.class, () ->
            pilotExchanger.subscribeXdsResource(null, ldsResourceType, ldsResourceListener));
    }

    @Test
    void testSubscribeXdsResourceWithNullResourceType() {
        // Act & Assert
        assertThrows(NullPointerException.class, () ->
            pilotExchanger.subscribeXdsResource("test-resource", null, ldsResourceListener));
    }

    @Test
    void testSubscribeXdsResourceWithNullResourceListener() {
        // Act & Assert
        // ConcurrentHashMap doesn't allow null values, so this should throw NullPointerException
        assertThrows(NullPointerException.class, () ->
            pilotExchanger.subscribeXdsResource("test-resource", ldsResourceType, null));
    }

    @Test
    void testSubscribeXdsResourceMultipleResourceTypes() {
        // Arrange
        String ldsResourceName = "test-listener";
        String cdsResourceName = "test-cluster";
        String edsResourceName = "test-endpoint";

        // Act
        pilotExchanger.subscribeXdsResource(ldsResourceName, ldsResourceType, ldsResourceListener);
        pilotExchanger.subscribeXdsResource(cdsResourceName, cdsResourceType, cdsResourceListener);
        pilotExchanger.subscribeXdsResource(edsResourceName, edsResourceType, edsResourceListener);

        // Assert - all subscriptions should be processed without exceptions
        assertDoesNotThrow(() -> {
            pilotExchanger.subscribeXdsResource(ldsResourceName, ldsResourceType, ldsResourceListener);
            pilotExchanger.subscribeXdsResource(cdsResourceName, cdsResourceType, cdsResourceListener);
            pilotExchanger.subscribeXdsResource(edsResourceName, edsResourceType, edsResourceListener);
        });
    }

    @Test
    void testSubscribeXdsResourceSameResourceMultipleTimes() {
        // Arrange
        String resourceName = "test-listener";

        // Act
        pilotExchanger.subscribeXdsResource(resourceName, ldsResourceType, ldsResourceListener);
        pilotExchanger.subscribeXdsResource(resourceName, ldsResourceType, ldsResourceListener);

        // Assert - multiple subscriptions to the same resource should be handled gracefully
        assertDoesNotThrow(() ->
            pilotExchanger.subscribeXdsResource(resourceName, ldsResourceType, ldsResourceListener));
    }

    @Test
    void testSubscribeXdsResourceDifferentListenersSameResource() {
        // Arrange
        String resourceName = "test-listener";
        XdsResourceListener<LdsUpdate> anotherListener = mock(XdsResourceListener.class);

        // Act
        pilotExchanger.subscribeXdsResource(resourceName, ldsResourceType, ldsResourceListener);
        pilotExchanger.subscribeXdsResource(resourceName, ldsResourceType, anotherListener);

        // Assert - different listeners for the same resource should be handled
        assertDoesNotThrow(() -> {
            pilotExchanger.subscribeXdsResource(resourceName, ldsResourceType, ldsResourceListener);
            pilotExchanger.subscribeXdsResource(resourceName, ldsResourceType, anotherListener);
        });
    }

    @Test
    void testDestroy() {
        // Act & Assert
        assertDoesNotThrow(() -> pilotExchanger.destroy());
    }

    @Test
    void testDestroyMultipleTimes() {
        // Act & Assert - multiple destroy calls should be handled gracefully
        assertDoesNotThrow(() -> {
            pilotExchanger.destroy();
            pilotExchanger.destroy();
        });
    }

    @Test
    void testOperationsAfterDestroy() {
        // Arrange
        pilotExchanger.destroy();

        // Act & Assert - operations after destroy should still work or handle gracefully
        assertDoesNotThrow(() ->
            pilotExchanger.subscribeXdsResource("test-resource", ldsResourceType, ldsResourceListener));
    }

    @Test
    void testSubscribeXdsResourceWithEmptyResourceName() {
        // Act & Assert
        assertDoesNotThrow(() ->
            pilotExchanger.subscribeXdsResource("", ldsResourceType, ldsResourceListener));
    }

    @Test
    void testSubscribeXdsResourceWithSpecialCharacters() {
        // Arrange
        String specialResourceName = "test-resource-with-special-chars-!@#$%^&*()";

        // Act & Assert
        assertDoesNotThrow(() ->
            pilotExchanger.subscribeXdsResource(specialResourceName, ldsResourceType, ldsResourceListener));
    }

    @Test
    void testAdsObserverNotNull() throws Exception {
        // Arrange - access the protected adsObserver field
        Field adsObserverField = PilotExchanger.class.getDeclaredField("adsObserver");
        adsObserverField.setAccessible(true);
        AdsObserver adsObserver = (AdsObserver) adsObserverField.get(pilotExchanger);

        // Assert
        assertNotNull(adsObserver);
    }

    @Test
    void testConstructorCreatesAdsObserver() throws Exception {
        // Arrange - use the already created pilotExchanger from setUp which has bootstrap mocks
        Field adsObserverField = PilotExchanger.class.getDeclaredField("adsObserver");
        adsObserverField.setAccessible(true);
        AdsObserver adsObserver = (AdsObserver) adsObserverField.get(pilotExchanger);

        // Assert
        assertNotNull(adsObserver);
    }

    @Test
    void testSubscribeXdsResourceInternalLogic() throws Exception {
        // Arrange
        String resourceName = "test-resource";

        // Replace the adsObserver with a mock to verify interactions
        Field adsObserverField = PilotExchanger.class.getDeclaredField("adsObserver");
        adsObserverField.setAccessible(true);
        adsObserverField.set(pilotExchanger, mockAdsObserver);

        // Mock the hasSubscribed method to return false first, then true
        when(mockAdsObserver.hasSubscribed(ldsResourceType)).thenReturn(false);

        // Act
        pilotExchanger.subscribeXdsResource(resourceName, ldsResourceType, ldsResourceListener);

        // Assert
        verify(mockAdsObserver).hasSubscribed(ldsResourceType);
        verify(mockAdsObserver).saveSubscribedType(ldsResourceType);
        verify(mockAdsObserver).addListener(resourceName, ldsResourceType, ldsResourceListener);
    }

    @Test
    void testSubscribeXdsResourceAlreadySubscribed() throws Exception {
        // Arrange
        String resourceName = "test-resource";

        // Replace the adsObserver with a mock
        Field adsObserverField = PilotExchanger.class.getDeclaredField("adsObserver");
        adsObserverField.setAccessible(true);
        adsObserverField.set(pilotExchanger, mockAdsObserver);

        // Mock the hasSubscribed method to return true (already subscribed)
        when(mockAdsObserver.hasSubscribed(ldsResourceType)).thenReturn(true);

        // Act
        pilotExchanger.subscribeXdsResource(resourceName, ldsResourceType, ldsResourceListener);

        // Assert
        verify(mockAdsObserver).hasSubscribed(ldsResourceType);
        verify(mockAdsObserver, never()).saveSubscribedType(ldsResourceType); // Should not save again
        verify(mockAdsObserver).addListener(resourceName, ldsResourceType, ldsResourceListener);
    }

    @Test
    void testSubscribeXdsResourceMultipleResourceTypesInternalLogic() throws Exception {
        // Arrange
        String ldsResourceName = "test-listener";
        String cdsResourceName = "test-cluster";

        // Replace the adsObserver with a mock
        Field adsObserverField = PilotExchanger.class.getDeclaredField("adsObserver");
        adsObserverField.setAccessible(true);
        adsObserverField.set(pilotExchanger, mockAdsObserver);

        // Mock hasSubscribed to return false for both resource types
        when(mockAdsObserver.hasSubscribed(ldsResourceType)).thenReturn(false);
        when(mockAdsObserver.hasSubscribed(cdsResourceType)).thenReturn(false);

        // Act
        pilotExchanger.subscribeXdsResource(ldsResourceName, ldsResourceType, ldsResourceListener);
        pilotExchanger.subscribeXdsResource(cdsResourceName, cdsResourceType, cdsResourceListener);

        // Assert
        verify(mockAdsObserver).hasSubscribed(ldsResourceType);
        verify(mockAdsObserver).hasSubscribed(cdsResourceType);
        verify(mockAdsObserver).saveSubscribedType(ldsResourceType);
        verify(mockAdsObserver).saveSubscribedType(cdsResourceType);
        verify(mockAdsObserver).addListener(ldsResourceName, ldsResourceType, ldsResourceListener);
        verify(mockAdsObserver).addListener(cdsResourceName, cdsResourceType, cdsResourceListener);
    }

    @Test
    void testDestroyInternalLogic() throws Exception {
        // Arrange
        Field adsObserverField = PilotExchanger.class.getDeclaredField("adsObserver");
        adsObserverField.setAccessible(true);
        adsObserverField.set(pilotExchanger, mockAdsObserver);

        // Act
        pilotExchanger.destroy();

        // Assert
        verify(mockAdsObserver).destroy();
    }

    @Test
    void testSubscribeXdsResourceWithGenericResourceUpdate() {
        // Arrange
        XdsResourceType<ResourceUpdate> genericResourceType = mock(XdsResourceType.class);
        XdsResourceListener<ResourceUpdate> genericResourceListener = mock(XdsResourceListener.class);
        when(genericResourceType.typeUrl()).thenReturn("type.googleapis.com/test.Generic");

        // Act & Assert
        assertDoesNotThrow(() ->
            pilotExchanger.subscribeXdsResource("generic-resource", genericResourceType, genericResourceListener));
    }

    @Test
    void testConcurrentSubscriptions() throws InterruptedException {
        // Arrange
        final Exception[] exceptions = new Exception[2];
        Thread thread1 = new Thread(() -> {
            try {
                pilotExchanger.subscribeXdsResource("resource1", ldsResourceType, ldsResourceListener);
            } catch (Exception e) {
                exceptions[0] = e;
            }
        });
        Thread thread2 = new Thread(() -> {
            try {
                pilotExchanger.subscribeXdsResource("resource2", cdsResourceType, cdsResourceListener);
            } catch (Exception e) {
                exceptions[1] = e;
            }
        });

        // Act
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Assert
        assertNull(exceptions[0]);
        assertNull(exceptions[1]);
    }
}
