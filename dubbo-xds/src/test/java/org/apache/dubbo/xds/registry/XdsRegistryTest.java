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
package org.apache.dubbo.xds.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.XdsResourceFactory;
import org.apache.dubbo.xds.bootstrap.BootstrapInfo;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.XdsServer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for XdsRegistry
 */
class XdsRegistryTest {

    @Mock
    private NotifyListener notifyListener;

    @Mock
    private ApplicationModel applicationModel;

    @Mock
    private Bootstrapper mockBootstrapper;

    @Mock
    private BootstrapInfo mockBootstrapInfo;

    @Mock
    private org.apache.dubbo.xds.bootstrap.Node mockBootstrapNode;

    @Mock
    private XdsServer mockXdsServer;

    private XdsRegistry xdsRegistry;
    private URL registryUrl;
    private URL serviceUrl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        registryUrl = URL.valueOf("xds://istiod.istio-system.svc:15010");
        serviceUrl = URL.valueOf("dubbo://127.0.0.1:20880/com.example.Service?provided-by=test-app:8080");

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

            // Create XdsRegistry with mocked bootstrap
            xdsRegistry = new XdsRegistry(registryUrl);
        }
    }

    @Test
    void testIsAvailable() {
        // Act
        boolean available = xdsRegistry.isAvailable();

        // Assert
        // XdsRegistry.isAvailable() always returns true
        assertTrue(available);
    }

    @Test
    void testRegister() {
        // Act & Assert
        // XDS registry doesn't perform traditional registration
        // Verify no exceptions are thrown
        assertDoesNotThrow(() -> xdsRegistry.register(serviceUrl));
    }

    @Test
    void testUnregister() {
        // Act & Assert
        // XDS registry doesn't perform traditional unregistration
        // Verify no exceptions are thrown
        assertDoesNotThrow(() -> xdsRegistry.unregister(serviceUrl));
    }

    @Test
    void testDoRegister() {
        // Act & Assert
        // doRegister is a no-op in XdsRegistry
        assertDoesNotThrow(() -> xdsRegistry.doRegister(serviceUrl));
    }

    @Test
    void testDoUnregister() {
        // Act & Assert
        // doUnregister is a no-op in XdsRegistry
        assertDoesNotThrow(() -> xdsRegistry.doUnregister(serviceUrl));
    }

    @Test
    void testSubscribe() {
        // Act & Assert
        // Verify subscription is handled properly
        assertDoesNotThrow(() -> xdsRegistry.subscribe(serviceUrl, notifyListener));
    }

    @Test
    void testDoSubscribe() {
        // Act & Assert
        // Since XdsRegistry is already created with mocked bootstrap,
        // we can test doSubscribe directly
        assertDoesNotThrow(() -> xdsRegistry.doSubscribe(serviceUrl, notifyListener));
    }

    @Test
    void testUnsubscribe() {
        // Arrange
        xdsRegistry.subscribe(serviceUrl, notifyListener);

        // Act & Assert
        // Verify unsubscription is handled properly
        assertDoesNotThrow(() -> xdsRegistry.unsubscribe(serviceUrl, notifyListener));
    }

    @Test
    void testDoUnsubscribe() {
        // Act & Assert
        // doUnsubscribe is a no-op in XdsRegistry
        assertDoesNotThrow(() -> xdsRegistry.doUnsubscribe(serviceUrl, notifyListener));
    }

    @Test
    void testLookup() {
        // Act
        List<URL> actualUrls = xdsRegistry.lookup(serviceUrl);

        // Assert
        assertNotNull(actualUrls);
        // XDS registry lookup returns empty list by default (inherited from FailbackRegistry)
        assertTrue(actualUrls.isEmpty());
    }

    @Test
    void testDestroy() {
        // Act & Assert
        // Verify destroy is handled properly
        assertDoesNotThrow(() -> xdsRegistry.destroy());
    }

    @Test
    void testGetUrl() {
        // Act
        URL url = xdsRegistry.getUrl();

        // Assert
        assertEquals(registryUrl, url);
    }

    @Test
    void testMultipleSubscriptions() {
        // Arrange
        NotifyListener listener1 = mock(NotifyListener.class);
        NotifyListener listener2 = mock(NotifyListener.class);

        URL service1 = URL.valueOf("dubbo://127.0.0.1:20880/com.example.Service1?provided-by=app1:8080");
        URL service2 = URL.valueOf("dubbo://127.0.0.1:20880/com.example.Service2?provided-by=app2:8080");

        // Act & Assert
        assertDoesNotThrow(() -> {
            xdsRegistry.subscribe(service1, listener1);
            xdsRegistry.subscribe(service2, listener2);
        });
    }

    @Test
    void testSubscribeWithNullListener() {
        // Act & Assert
        // XdsRegistry extends FailbackRegistry which throws IllegalArgumentException for null listener
        assertThrows(IllegalArgumentException.class, () ->
            xdsRegistry.subscribe(serviceUrl, null));
    }

    @Test
    void testSubscribeWithNullUrl() {
        // Act & Assert
        // XdsRegistry extends FailbackRegistry which throws IllegalArgumentException for null URL
        assertThrows(IllegalArgumentException.class, () ->
            xdsRegistry.subscribe(null, notifyListener));
    }

    @Test
    void testSubscribeWithoutProvidedByParameter() {
        // Arrange
        URL urlWithoutProvidedBy = URL.valueOf("dubbo://127.0.0.1:20880/com.example.Service");

        // Act & Assert
        // Should throw exception when URL doesn't have provided-by parameter
        assertThrows(IllegalStateException.class, () ->
            xdsRegistry.subscribe(urlWithoutProvidedBy, notifyListener));
    }

    @Test
    void testGetApplicationModel() {
        // Act
        // The applicationModel is set during construction from the URL
        // We can verify the registry was created successfully
        assertNotNull(xdsRegistry);
        assertEquals(registryUrl, xdsRegistry.getUrl());
    }
}
