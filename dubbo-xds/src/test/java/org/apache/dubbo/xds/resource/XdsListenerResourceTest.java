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

import org.apache.dubbo.xds.resource.update.LdsUpdate;

import io.envoyproxy.envoy.config.core.v3.Address;
import io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.envoyproxy.envoy.config.listener.v3.Filter;
import io.envoyproxy.envoy.config.listener.v3.FilterChain;
import io.envoyproxy.envoy.config.listener.v3.Listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XdsListenerResource
 */
class XdsListenerResourceTest {

    private XdsListenerResource xdsListenerResource;
    private XdsResourceType.Args args;

    @BeforeEach
    void setUp() {
        xdsListenerResource = XdsListenerResource.getInstance();
        args = XdsResourceType.xdsResourceTypeArgs;
    }

    @Test
    void testParseValidListener() {
        // Arrange
        Listener listener = Listener.newBuilder()
            .setName("test-listener")
            .setAddress(
                Address.newBuilder()
                    .setSocketAddress(
                        SocketAddress.newBuilder()
                            .setAddress("0.0.0.0")
                            .setPortValue(8080)
                    )
            )
            .addFilterChains(
                FilterChain.newBuilder()
                    .addFilters(
                        Filter.newBuilder()
                            .setName("envoy.filters.network.http_connection_manager")
                            .setTypedConfig(
                                com.google.protobuf.Any.pack(
                                    io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager.newBuilder()
                                        .addHttpFilters(
                                            io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter.newBuilder()
                                                .setName("envoy.filters.http.router")
                                                .setTypedConfig(
                                                    com.google.protobuf.Any.pack(
                                                        io.envoyproxy.envoy.extensions.filters.http.router.v3.Router.newBuilder().build()
                                                    )
                                                )
                                        )
                                        .setRouteConfig(
                                            io.envoyproxy.envoy.config.route.v3.RouteConfiguration.newBuilder()
                                                .setName("test-route")
                                                .addVirtualHosts(
                                                    io.envoyproxy.envoy.config.route.v3.VirtualHost.newBuilder()
                                                        .setName("test-host")
                                                        .addDomains("*")
                                                        .addRoutes(
                                                            io.envoyproxy.envoy.config.route.v3.Route.newBuilder()
                                                                .setMatch(
                                                                    io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
                                                                        .setPrefix("/")
                                                                )
                                                                .setRoute(
                                                                    io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
                                                                        .setCluster("test-cluster")
                                                                )
                                                        )
                                                )
                                        )
                                        .build()
                                )
                            )
                    )
            )
            .build();

        // Act & Assert
        assertDoesNotThrow(() -> {
            LdsUpdate result = xdsListenerResource.doParse(args, listener);
            assertNotNull(result);
            assertEquals(8080, result.getPort());
        });
    }

    @Test
    void testParseListenerWithMultipleFilterChains() {
        // Arrange
        Listener listener = Listener.newBuilder()
            .setName("multi-chain-listener")
            .setAddress(
                Address.newBuilder()
                    .setSocketAddress(
                        SocketAddress.newBuilder()
                            .setAddress("127.0.0.1")
                            .setPortValue(9090)
                    )
            )
            .addFilterChains(
                FilterChain.newBuilder()
                    .addFilters(
                        Filter.newBuilder()
                            .setName("envoy.filters.network.http_connection_manager")
                            .setTypedConfig(
                                com.google.protobuf.Any.pack(
                                    io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager.newBuilder()
                                        .addHttpFilters(
                                            io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter.newBuilder()
                                                .setName("envoy.filters.http.router")
                                                .setTypedConfig(
                                                    com.google.protobuf.Any.pack(
                                                        io.envoyproxy.envoy.extensions.filters.http.router.v3.Router.newBuilder().build()
                                                    )
                                                )
                                        )
                                        .setRouteConfig(
                                            io.envoyproxy.envoy.config.route.v3.RouteConfiguration.newBuilder()
                                                .setName("test-route-2")
                                                .addVirtualHosts(
                                                    io.envoyproxy.envoy.config.route.v3.VirtualHost.newBuilder()
                                                        .setName("test-host-2")
                                                        .addDomains("*")
                                                        .addRoutes(
                                                            io.envoyproxy.envoy.config.route.v3.Route.newBuilder()
                                                                .setMatch(
                                                                    io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
                                                                        .setPrefix("/")
                                                                )
                                                                .setRoute(
                                                                    io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
                                                                        .setCluster("test-cluster-2")
                                                                )
                                                        )
                                                )
                                        )
                                        .build()
                                )
                            )
                    )
            )
            .addFilterChains(
                FilterChain.newBuilder()
                    .addFilters(
                        Filter.newBuilder()
                            .setName("envoy.filters.network.tcp_proxy")
                    )
            )
            .build();

        // Act & Assert
        assertDoesNotThrow(() -> {
            LdsUpdate result = xdsListenerResource.doParse(args, listener);
            assertNotNull(result);
            assertEquals(9090, result.getPort());
        });
    }

    @Test
    void testParseListenerWithoutAddress() {
        // Arrange
        Listener listener = Listener.newBuilder()
            .setName("no-address-listener")
            .addFilterChains(
                FilterChain.newBuilder()
                    .addFilters(
                        Filter.newBuilder()
                            .setName("envoy.filters.network.http_connection_manager")
                            .setTypedConfig(
                                com.google.protobuf.Any.pack(
                                    io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager.newBuilder()
                                        .addHttpFilters(
                                            io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter.newBuilder()
                                                .setName("envoy.filters.http.router")
                                                .setTypedConfig(
                                                    com.google.protobuf.Any.pack(
                                                        io.envoyproxy.envoy.extensions.filters.http.router.v3.Router.newBuilder().build()
                                                    )
                                                )
                                        )
                                        .setRouteConfig(
                                            io.envoyproxy.envoy.config.route.v3.RouteConfiguration.newBuilder()
                                                .setName("no-address-route")
                                                .addVirtualHosts(
                                                    io.envoyproxy.envoy.config.route.v3.VirtualHost.newBuilder()
                                                        .setName("no-address-host")
                                                        .addDomains("*")
                                                        .addRoutes(
                                                            io.envoyproxy.envoy.config.route.v3.Route.newBuilder()
                                                                .setMatch(
                                                                    io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
                                                                        .setPrefix("/")
                                                                )
                                                                .setRoute(
                                                                    io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
                                                                        .setCluster("no-address-cluster")
                                                                )
                                                        )
                                                )
                                        )
                                        .build()
                                )
                            )
                    )
            )
            .build();

        // Act & Assert
        assertDoesNotThrow(() -> {
            LdsUpdate result = xdsListenerResource.doParse(args, listener);
            assertNotNull(result);
            assertEquals(-1, result.getPort()); // No address means port is -1
        });
    }

    @Test
    void testParseListenerWithoutFilterChains() {
        // Arrange
        Listener listener = Listener.newBuilder()
            .setName("no-filters-listener")
            .setAddress(
                Address.newBuilder()
                    .setSocketAddress(
                        SocketAddress.newBuilder()
                            .setAddress("0.0.0.0")
                            .setPortValue(8080)
                    )
            )
            .build();

        // Act & Assert
        // Listener without filter chains should return null (no valid HttpConnectionManager found)
        assertDoesNotThrow(() -> {
            LdsUpdate result = xdsListenerResource.doParse(args, listener);
            assertNull(result); // Should return null when no valid filter chains found
        });
    }

    @Test
    void testParseListenerWithTlsContext() {
        // Arrange
        Listener listener = Listener.newBuilder()
            .setName("tls-listener")
            .setAddress(
                Address.newBuilder()
                    .setSocketAddress(
                        SocketAddress.newBuilder()
                            .setAddress("0.0.0.0")
                            .setPortValue(8443)
                    )
            )
            .addFilterChains(
                FilterChain.newBuilder()
                    .setTransportSocket(
                        io.envoyproxy.envoy.config.core.v3.TransportSocket.newBuilder()
                            .setName("envoy.transport_sockets.tls")
                            .setTypedConfig(
                                com.google.protobuf.Any.newBuilder()
                                    .setTypeUrl("type.googleapis.com/envoy.extensions.transport_sockets.tls.v3.DownstreamTlsContext")
                            )
                    )
                    .addFilters(
                        Filter.newBuilder()
                            .setName("envoy.filters.network.http_connection_manager")
                            .setTypedConfig(
                                com.google.protobuf.Any.pack(
                                    io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager.newBuilder()
                                        .addHttpFilters(
                                            io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter.newBuilder()
                                                .setName("envoy.filters.http.router")
                                                .setTypedConfig(
                                                    com.google.protobuf.Any.pack(
                                                        io.envoyproxy.envoy.extensions.filters.http.router.v3.Router.newBuilder().build()
                                                    )
                                                )
                                        )
                                        .setRouteConfig(
                                            io.envoyproxy.envoy.config.route.v3.RouteConfiguration.newBuilder()
                                                .setName("tls-route")
                                                .addVirtualHosts(
                                                    io.envoyproxy.envoy.config.route.v3.VirtualHost.newBuilder()
                                                        .setName("tls-host")
                                                        .addDomains("*")
                                                        .addRoutes(
                                                            io.envoyproxy.envoy.config.route.v3.Route.newBuilder()
                                                                .setMatch(
                                                                    io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
                                                                        .setPrefix("/")
                                                                )
                                                                .setRoute(
                                                                    io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
                                                                        .setCluster("tls-cluster")
                                                                )
                                                        )
                                                )
                                        )
                                        .build()
                                )
                            )
                    )
            )
            .build();

        // Act & Assert
        assertDoesNotThrow(() -> {
            LdsUpdate result = xdsListenerResource.doParse(args, listener);
            assertNotNull(result);
            assertEquals(8443, result.getPort());
        });
    }

    @Test
    void testTypeUrl() {
        // Act
        String typeUrl = xdsListenerResource.typeUrl();

        // Assert
        assertEquals("type.googleapis.com/envoy.config.listener.v3.Listener", typeUrl);
    }

    @Test
    void testUnpackedClassName() {
        // Act
        Class<?> resourceClass = xdsListenerResource.unpackedClassName();

        // Assert
        assertEquals(Listener.class, resourceClass);
    }

    @Test
    void testTypeName() {
        // Act
        String typeName = xdsListenerResource.typeName();

        // Assert
        assertEquals("LDS", typeName);
    }

    @Test
    void testParseListenerWithEmptyName() {
        // Arrange
        Listener listener = Listener.newBuilder()
            .setName("")
            .setAddress(
                Address.newBuilder()
                    .setSocketAddress(
                        SocketAddress.newBuilder()
                            .setAddress("0.0.0.0")
                            .setPortValue(8080)
                    )
            )
            .addFilterChains(
                FilterChain.newBuilder()
                    .addFilters(
                        Filter.newBuilder()
                            .setName("envoy.filters.network.http_connection_manager")
                            .setTypedConfig(
                                com.google.protobuf.Any.pack(
                                    io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager.newBuilder()
                                        .addHttpFilters(
                                            io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter.newBuilder()
                                                .setName("envoy.filters.http.router")
                                                .setTypedConfig(
                                                    com.google.protobuf.Any.pack(
                                                        io.envoyproxy.envoy.extensions.filters.http.router.v3.Router.newBuilder().build()
                                                    )
                                                )
                                        )
                                        .setRouteConfig(
                                            io.envoyproxy.envoy.config.route.v3.RouteConfiguration.newBuilder()
                                                .setName("empty-name-route")
                                                .addVirtualHosts(
                                                    io.envoyproxy.envoy.config.route.v3.VirtualHost.newBuilder()
                                                        .setName("empty-name-host")
                                                        .addDomains("*")
                                                        .addRoutes(
                                                            io.envoyproxy.envoy.config.route.v3.Route.newBuilder()
                                                                .setMatch(
                                                                    io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
                                                                        .setPrefix("/")
                                                                )
                                                                .setRoute(
                                                                    io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
                                                                        .setCluster("empty-name-cluster")
                                                                )
                                                        )
                                                )
                                        )
                                        .build()
                                )
                            )
                    )
            )
            .build();

        // Act & Assert
        assertDoesNotThrow(() -> {
            LdsUpdate result = xdsListenerResource.doParse(args, listener);
            assertNotNull(result);
            assertEquals(8080, result.getPort());
        });
    }

    @Test
    void testExtractResourceName() {
        // Arrange
        Listener listener = Listener.newBuilder()
            .setName("test-listener")
            .build();

        // Act
        String resourceName = xdsListenerResource.extractResourceName(listener);

        // Assert
        assertEquals("test-listener", resourceName);
    }

    @Test
    void testParseListenerWithComplexConfiguration() {
        // Arrange
        Listener listener = Listener.newBuilder()
            .setName("complex-listener")
            .setAddress(
                Address.newBuilder()
                    .setSocketAddress(
                        SocketAddress.newBuilder()
                            .setAddress("0.0.0.0")
                            .setPortValue(8080)
                            .setProtocol(SocketAddress.Protocol.TCP)
                    )
            )
            .addFilterChains(
                FilterChain.newBuilder()
                    .setFilterChainMatch(
                        io.envoyproxy.envoy.config.listener.v3.FilterChainMatch.newBuilder()
                            .setDestinationPort(
                                com.google.protobuf.UInt32Value.newBuilder()
                                    .setValue(8080)
                            )
                    )
                    .addFilters(
                        Filter.newBuilder()
                            .setName("envoy.filters.network.http_connection_manager")
                            .setTypedConfig(
                                com.google.protobuf.Any.pack(
                                    io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager.newBuilder()
                                        .addHttpFilters(
                                            io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter.newBuilder()
                                                .setName("envoy.filters.http.router")
                                                .setTypedConfig(
                                                    com.google.protobuf.Any.pack(
                                                        io.envoyproxy.envoy.extensions.filters.http.router.v3.Router.newBuilder().build()
                                                    )
                                                )
                                        )
                                        .setRouteConfig(
                                            io.envoyproxy.envoy.config.route.v3.RouteConfiguration.newBuilder()
                                                .setName("complex-route")
                                                .addVirtualHosts(
                                                    io.envoyproxy.envoy.config.route.v3.VirtualHost.newBuilder()
                                                        .setName("complex-host")
                                                        .addDomains("*")
                                                        .addRoutes(
                                                            io.envoyproxy.envoy.config.route.v3.Route.newBuilder()
                                                                .setMatch(
                                                                    io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder()
                                                                        .setPrefix("/")
                                                                )
                                                                .setRoute(
                                                                    io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
                                                                        .setCluster("complex-cluster")
                                                                )
                                                        )
                                                )
                                        )
                                        .build()
                                )
                            )
                    )
            )
            .build();

        // Act & Assert
        assertDoesNotThrow(() -> {
            LdsUpdate result = xdsListenerResource.doParse(args, listener);
            assertNotNull(result);
            assertEquals(8080, result.getPort());
        });
    }

    @Test
    void testIsFullStateOfTheWorld() {
        // Act
        boolean isFullStateOfTheWorld = xdsListenerResource.isFullStateOfTheWorld();

        // Assert
        assertTrue(isFullStateOfTheWorld);
    }
}
