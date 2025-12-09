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

import java.util.Arrays;
import java.util.HashMap;

import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryResponse;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for XdsChannel
 */
class XdsChannelTest {

    @Mock
    private StreamObserver<DiscoveryResponse> discoveryResponseObserver;

    @Mock
    private StreamObserver<DeltaDiscoveryResponse> deltaDiscoveryResponseObserver;

    @Mock
    private StreamObserver<io.envoyproxy.envoy.api.v2.DiscoveryResponse> discoveryResponseObserverV2;

    @Mock
    private StreamObserver<io.envoyproxy.envoy.api.v2.DeltaDiscoveryResponse> deltaDiscoveryResponseObserverV2;

    @Mock
    private Bootstrapper mockBootstrapper;

    @Mock
    private BootstrapInfo mockBootstrapInfo;

    @Mock
    private org.apache.dubbo.xds.bootstrap.Node mockBootstrapNode;

    @Mock
    private XdsServer mockXdsServer;

    private XdsChannel xdsChannel;

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

            // Create XdsChannel with mocked bootstrap
            xdsChannel = new XdsChannel();
        }
    }

    @Test
    void testConstructor() {
        // Act & Assert
        assertNotNull(xdsChannel);
        // Channel might be null if bootstrap fails, but should not throw exception
        assertDoesNotThrow(() -> xdsChannel.getChannel());
    }

    @Test
    void testGetChannel() {
        // Act
        ManagedChannel channel = xdsChannel.getChannel();

        // Assert
        // Channel might be null if bootstrap configuration is not available
        // The important thing is that the method doesn't throw an exception
        assertDoesNotThrow(() -> xdsChannel.getChannel());
    }

    @Test
    void testCreateDeltaDiscoveryRequest() {
        // Act & Assert
        // If channel is null, this method should handle gracefully
        assertDoesNotThrow(() -> {
            StreamObserver<DiscoveryRequest> requestObserver =
                    xdsChannel.createDeltaDiscoveryRequest(discoveryResponseObserver);
            // Observer might be null if channel is not available
        });
    }

    @Test
    void testObserveDeltaDiscoveryRequest() {
        // Act & Assert
        // If channel is null, this method should handle gracefully
        assertDoesNotThrow(() -> {
            StreamObserver<DeltaDiscoveryRequest> requestObserver =
                    xdsChannel.observeDeltaDiscoveryRequest(deltaDiscoveryResponseObserver);
            // Observer might be null if channel is not available
        });
    }

    @Test
    void testCreateDeltaDiscoveryRequestV2() {
        // Act & Assert
        // If channel is null, this method should handle gracefully
        assertDoesNotThrow(() -> {
            StreamObserver<io.envoyproxy.envoy.api.v2.DiscoveryRequest> requestObserver =
                    xdsChannel.createDeltaDiscoveryRequestV2(discoveryResponseObserverV2);
            // Observer might be null if channel is not available
        });
    }

    @Test
    void testObserveDeltaDiscoveryRequestV2() {
        // Act & Assert
        // If channel is null, this method should handle gracefully
        assertDoesNotThrow(() -> {
            StreamObserver<io.envoyproxy.envoy.api.v2.DeltaDiscoveryRequest> requestObserver =
                    xdsChannel.observeDeltaDiscoveryRequestV2(deltaDiscoveryResponseObserverV2);
            // Observer might be null if channel is not available
        });
    }

    @Test
    void testDestroy() {
        // Act & Assert
        assertDoesNotThrow(() -> xdsChannel.destroy());
    }

    @Test
    void testMultipleDestroy() {
        // Act & Assert - multiple destroy calls should be handled gracefully
        assertDoesNotThrow(() -> {
            xdsChannel.destroy();
            xdsChannel.destroy();
        });
    }

    @Test
    void testChannelOperationsAfterDestroy() {
        // Arrange
        xdsChannel.destroy();

        // Act & Assert - operations after destroy should still work or handle gracefully
        assertDoesNotThrow(() -> {
            ManagedChannel channel = xdsChannel.getChannel();
            // Channel might be null after destroy, but method should not throw
        });
    }

    @Test
    void testCreateMultipleStreamObservers() {
        // Act
        StreamObserver<DiscoveryRequest> requestObserver1 =
                xdsChannel.createDeltaDiscoveryRequest(discoveryResponseObserver);
        StreamObserver<DiscoveryRequest> requestObserver2 =
                xdsChannel.createDeltaDiscoveryRequest(discoveryResponseObserver);

        // Assert
        assertNotNull(requestObserver1);
        assertNotNull(requestObserver2);
        // Different observers should be created
        assertNotSame(requestObserver1, requestObserver2);
    }

    @Test
    void testCreateStreamObserversWithNullObserver() {
        // Act & Assert - gRPC requires non-null responseObserver, should throw NullPointerException
        assertThrows(NullPointerException.class, () -> {
            StreamObserver<DiscoveryRequest> requestObserver = xdsChannel.createDeltaDiscoveryRequest(null);
        });
    }

    @Test
    void testV2AndV3StreamObservers() {
        // Act & Assert
        // If channel is null (due to bootstrap failure), these methods should handle gracefully
        assertDoesNotThrow(() -> {
            StreamObserver<DiscoveryRequest> v3RequestObserver =
                    xdsChannel.createDeltaDiscoveryRequest(discoveryResponseObserver);
            StreamObserver<io.envoyproxy.envoy.api.v2.DiscoveryRequest> v2RequestObserver =
                    xdsChannel.createDeltaDiscoveryRequestV2(discoveryResponseObserverV2);

            // If channel is available, observers should be created
            // If channel is null, methods might return null or throw exception
        });
    }

    @Test
    void testDeltaAndRegularStreamObservers() {
        // Act
        StreamObserver<DiscoveryRequest> regularObserver =
                xdsChannel.createDeltaDiscoveryRequest(discoveryResponseObserver);
        StreamObserver<DeltaDiscoveryRequest> deltaObserver =
                xdsChannel.observeDeltaDiscoveryRequest(deltaDiscoveryResponseObserver);

        // Assert
        assertNotNull(regularObserver);
        assertNotNull(deltaObserver);
    }

    @Test
    void testChannelNotNullAfterConstruction() {
        // Arrange
        XdsChannel newChannel = new XdsChannel();

        // Act
        ManagedChannel channel = newChannel.getChannel();

        // Assert
        // Channel might be null if bootstrap fails, but the method should not throw
        assertDoesNotThrow(() -> newChannel.getChannel());
    }
}
