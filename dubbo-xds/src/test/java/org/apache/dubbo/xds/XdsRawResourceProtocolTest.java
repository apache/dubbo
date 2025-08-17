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

import org.apache.dubbo.xds.resource.XdsResourceType;
import org.apache.dubbo.xds.resource.update.LdsUpdate;

import io.envoyproxy.envoy.config.core.v3.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for XdsRawResourceProtocol
 */
class XdsRawResourceProtocolTest {

    @Mock
    private AdsObserver adsObserver;

    @Mock
    private XdsResourceType<LdsUpdate> resourceType;

    @Mock
    private XdsResourceListener<LdsUpdate> resourceListener1;

    @Mock
    private XdsResourceListener<LdsUpdate> resourceListener2;

    @Mock
    private LdsUpdate ldsUpdate1;

    @Mock
    private LdsUpdate ldsUpdate2;

    private Node node;
    private XdsRawResourceProtocol<LdsUpdate> protocol;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create a test node
        node = Node.newBuilder().setId("test-node").setCluster("test-cluster").build();

        // Mock resource type
        when(resourceType.typeUrl()).thenReturn("type.googleapis.com/envoy.config.listener.v3.Listener");

        // Create the protocol instance
        protocol = new XdsRawResourceProtocol<>(adsObserver, node, resourceType);
    }

    @Test
    void testConstructor() {
        // Act & Assert
        assertNotNull(protocol);
        assertEquals("type.googleapis.com/envoy.config.listener.v3.Listener", protocol.getTypeUrl());
    }

    @Test
    void testGetTypeUrl() {
        // Act
        String typeUrl = protocol.getTypeUrl();

        // Assert
        assertEquals("type.googleapis.com/envoy.config.listener.v3.Listener", typeUrl);
        verify(resourceType).typeUrl();
    }

    @Test
    void testOnResourceUpdateWithNull() {
        // Act
        protocol.onResourceUpdate(null);

        // Assert - should not throw exception and no listeners should be called
        verifyNoInteractions(resourceListener1, resourceListener2);
    }

    @Test
    void testOnResourceUpdateWithNoListeners() {
        // Act
        protocol.onResourceUpdate(ldsUpdate1);

        // Assert - should not throw exception
        assertDoesNotThrow(() -> protocol.onResourceUpdate(ldsUpdate1));
    }

    @Test
    void testOnResourceUpdateWithListeners() {
        // Arrange
        protocol.subscribeResource("resource1", resourceType, resourceListener1);
        protocol.subscribeResource("resource2", resourceType, resourceListener2);

        // Act
        protocol.onResourceUpdate(ldsUpdate1);

        // Assert
        verify(resourceListener1).onResourceUpdate(ldsUpdate1);
        verify(resourceListener2).onResourceUpdate(ldsUpdate1);
        verify(adsObserver, times(2)).adjustResourceSubscription(resourceType);
    }

    @Test
    void testOnResourceUpdateWithSameUpdate() {
        // Arrange
        protocol.subscribeResource("resource1", resourceType, resourceListener1);
        protocol.onResourceUpdate(ldsUpdate1);
        reset(resourceListener1); // Reset to clear previous interactions

        // Act - send the same update again
        protocol.onResourceUpdate(ldsUpdate1);

        // Assert - listeners should not be called again for the same update
        verifyNoInteractions(resourceListener1);
    }

    @Test
    void testOnResourceUpdateWithDifferentUpdate() {
        // Arrange
        protocol.subscribeResource("resource1", resourceType, resourceListener1);
        protocol.onResourceUpdate(ldsUpdate1);
        reset(resourceListener1); // Reset to clear previous interactions

        // Act - send a different update
        protocol.onResourceUpdate(ldsUpdate2);

        // Assert - listeners should be called for the new update
        verify(resourceListener1).onResourceUpdate(ldsUpdate2);
    }

    @Test
    void testSubscribeResourceWithNullResourceName() {
        // Act
        protocol.subscribeResource(null, resourceType, resourceListener1);

        // Assert - should not throw exception and no subscription should occur
        verifyNoInteractions(adsObserver);
    }

    @Test
    void testSubscribeResourceNewSubscription() {
        // Act
        protocol.subscribeResource("test-resource", resourceType, resourceListener1);

        // Assert
        verify(adsObserver).adjustResourceSubscription(resourceType);
    }

    @Test
    void testSubscribeResourceExistingSubscription() {
        // Arrange - first subscription
        protocol.subscribeResource("test-resource", resourceType, resourceListener1);
        protocol.onResourceUpdate(ldsUpdate1);
        reset(adsObserver, resourceListener1); // Reset to clear previous interactions

        // Act - second subscription to the same resource
        protocol.subscribeResource("test-resource", resourceType, resourceListener2);

        // Assert - should not call adjustResourceSubscription again, but should send existing update
        verifyNoInteractions(adsObserver);
        verify(resourceListener2).onResourceUpdate(ldsUpdate1);
        verifyNoInteractions(resourceListener1); // First listener should not be called
    }

    @Test
    void testSubscribeResourceExistingSubscriptionWithNoUpdate() {
        // Arrange - first subscription without any update
        protocol.subscribeResource("test-resource", resourceType, resourceListener1);
        reset(adsObserver); // Reset to clear previous interactions

        // Act - second subscription to the same resource
        protocol.subscribeResource("test-resource", resourceType, resourceListener2);

        // Assert - should not call adjustResourceSubscription again
        verifyNoInteractions(adsObserver);
        // Note: If there's an existing update, it will be sent to the new listener
        // So we don't verify no interactions with resourceListener2
    }

    @Test
    void testMultipleResourceSubscriptions() {
        // Act
        protocol.subscribeResource("resource1", resourceType, resourceListener1);
        protocol.subscribeResource("resource2", resourceType, resourceListener2);

        // Assert
        verify(adsObserver, times(2)).adjustResourceSubscription(resourceType);
    }

    @Test
    void testResourceUpdateAfterMultipleSubscriptions() {
        // Arrange
        protocol.subscribeResource("resource1", resourceType, resourceListener1);
        protocol.subscribeResource("resource2", resourceType, resourceListener2);

        // Act
        protocol.onResourceUpdate(ldsUpdate1);

        // Assert - both listeners should receive the update
        verify(resourceListener1).onResourceUpdate(ldsUpdate1);
        verify(resourceListener2).onResourceUpdate(ldsUpdate1);
    }

    @Test
    void testConstructorWithNullParameters() {
        // Act & Assert
        // XdsRawResourceProtocol constructor may handle null parameters gracefully
        assertDoesNotThrow(() -> new XdsRawResourceProtocol<>(null, node, resourceType));

        assertDoesNotThrow(() -> new XdsRawResourceProtocol<>(adsObserver, null, resourceType));

        assertDoesNotThrow(() -> new XdsRawResourceProtocol<>(adsObserver, node, null));
    }

    @Test
    void testSubscribeResourceWithEmptyResourceName() {
        // Act
        protocol.subscribeResource("", resourceType, resourceListener1);

        // Assert - empty string is not null, so subscription should proceed
        verify(adsObserver).adjustResourceSubscription(resourceType);
    }

    @Test
    void testConcurrentResourceUpdates() {
        // Arrange
        protocol.subscribeResource("resource1", resourceType, resourceListener1);
        protocol.subscribeResource("resource2", resourceType, resourceListener2);

        // Act - simulate concurrent updates
        protocol.onResourceUpdate(ldsUpdate1);
        protocol.onResourceUpdate(ldsUpdate2);

        // Assert - all listeners should receive the latest update
        verify(resourceListener1).onResourceUpdate(ldsUpdate1);
        verify(resourceListener1).onResourceUpdate(ldsUpdate2);
        verify(resourceListener2).onResourceUpdate(ldsUpdate1);
        verify(resourceListener2).onResourceUpdate(ldsUpdate2);
    }

    @Test
    void testResourceUpdateWithMockEquality() {
        // Arrange
        LdsUpdate sameUpdate = mock(LdsUpdate.class);
        // Note: Cannot mock equals() method as it's final, so we'll test with the same object
        sameUpdate = ldsUpdate1; // Use the same object to test equality

        protocol.subscribeResource("resource1", resourceType, resourceListener1);
        protocol.onResourceUpdate(ldsUpdate1);
        reset(resourceListener1);

        // Act - send an "equal" update
        protocol.onResourceUpdate(sameUpdate);

        // Assert - listener should not be called for equal updates
        verifyNoInteractions(resourceListener1);
    }

    @Test
    void testGetTypeUrlWithDifferentResourceType() {
        // Arrange
        XdsResourceType<LdsUpdate> differentResourceType = mock(XdsResourceType.class);
        when(differentResourceType.typeUrl()).thenReturn("type.googleapis.com/envoy.config.cluster.v3.Cluster");

        XdsRawResourceProtocol<LdsUpdate> differentProtocol =
                new XdsRawResourceProtocol<>(adsObserver, node, differentResourceType);

        // Act
        String typeUrl = differentProtocol.getTypeUrl();

        // Assert
        assertEquals("type.googleapis.com/envoy.config.cluster.v3.Cluster", typeUrl);
    }

    @Test
    void testResourceListenerMap() {
        // Arrange
        String resourceName1 = "resource1";
        String resourceName2 = "resource2";

        // Act
        protocol.subscribeResource(resourceName1, resourceType, resourceListener1);
        protocol.subscribeResource(resourceName2, resourceType, resourceListener2);

        // Assert - verify that the internal map contains the listeners
        // This is tested indirectly through the behavior
        protocol.onResourceUpdate(ldsUpdate1);
        verify(resourceListener1).onResourceUpdate(ldsUpdate1);
        verify(resourceListener2).onResourceUpdate(ldsUpdate1);
    }

    @Test
    void testSubscribeResourceOverwriteExistingListener() {
        // Arrange
        String resourceName = "test-resource";
        protocol.subscribeResource(resourceName, resourceType, resourceListener1);
        protocol.onResourceUpdate(ldsUpdate1);
        reset(adsObserver, resourceListener1);

        // Act - subscribe with a different listener for the same resource name
        // Note: The current implementation uses putIfAbsent, so this won't overwrite
        protocol.subscribeResource(resourceName, resourceType, resourceListener2);

        // Assert - should not call adjustResourceSubscription again
        verifyNoInteractions(adsObserver);
        // The existing update should be sent to the new listener
        verify(resourceListener2).onResourceUpdate(ldsUpdate1);
        verifyNoInteractions(resourceListener1);
    }
}
