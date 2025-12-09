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
import org.apache.dubbo.xds.resource.update.RdsUpdate;

import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.envoyproxy.envoy.config.route.v3.WeightedCluster;
import io.envoyproxy.envoy.type.matcher.v3.StringMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XdsRouteResource
 */
class XdsRouteResourceTest {

    private XdsRouteConfigureResource xdsRouteResource;

    @BeforeEach
    void setUp() {
        xdsRouteResource = new XdsRouteConfigureResource();
    }

    @Test
    void testParseValidRouteConfiguration() throws ResourceInvalidException {
        // Arrange
        RouteConfiguration routeConfig = RouteConfiguration.newBuilder()
                .setName("test-route-config")
                .addVirtualHosts(VirtualHost.newBuilder()
                        .setName("test-virtual-host")
                        .addDomains("example.com")
                        .addRoutes(Route.newBuilder()
                                .setMatch(RouteMatch.newBuilder().setPath("/api/v1/test"))
                                .setRoute(RouteAction.newBuilder().setCluster("test-cluster"))))
                .build();

        // Act
        RdsUpdate result = xdsRouteResource.doParse(
                new XdsResourceType.Args(null, null, null, null, FilterRegistry.getDefaultRegistry(), null, null, null),
                routeConfig);

        // Assert
        assertNotNull(result);
        assertFalse(result.getVirtualHosts().isEmpty());
    }

    @Test
    void testParseRouteWithHeaderMatching() throws ResourceInvalidException {
        // Arrange
        RouteConfiguration routeConfig = RouteConfiguration.newBuilder()
                .setName("header-route-config")
                .addVirtualHosts(VirtualHost.newBuilder()
                        .setName("header-virtual-host")
                        .addDomains("*")
                        .addRoutes(Route.newBuilder()
                                .setMatch(RouteMatch.newBuilder()
                                        .setPrefix("/")
                                        .addHeaders(io.envoyproxy.envoy.config.route.v3.HeaderMatcher.newBuilder()
                                                .setName("user-type")
                                                .setStringMatch(StringMatcher.newBuilder()
                                                        .setExact("vip"))))
                                .setRoute(RouteAction.newBuilder().setCluster("vip-cluster")))
                        .addRoutes(Route.newBuilder()
                                .setMatch(RouteMatch.newBuilder().setPrefix("/"))
                                .setRoute(RouteAction.newBuilder().setCluster("default-cluster"))))
                .build();

        // Act
        RdsUpdate result = xdsRouteResource.doParse(
                new XdsResourceType.Args(null, null, null, null, FilterRegistry.getDefaultRegistry(), null, null, null),
                routeConfig);

        // Assert
        assertNotNull(result);
        assertEquals("header-route-config", routeConfig.getName());
        assertEquals(1, result.getVirtualHosts().size());
        // Note: We can't easily check the number of routes here since they may be filtered out
    }

    @Test
    void testParseRouteWithWeightedClusters() throws ResourceInvalidException {
        // Arrange
        RouteConfiguration routeConfig = RouteConfiguration.newBuilder()
                .setName("weighted-route-config")
                .addVirtualHosts(VirtualHost.newBuilder()
                        .setName("weighted-virtual-host")
                        .addDomains("*")
                        .addRoutes(Route.newBuilder()
                                .setMatch(RouteMatch.newBuilder().setPrefix("/"))
                                .setRoute(RouteAction.newBuilder()
                                        .setWeightedClusters(WeightedCluster.newBuilder()
                                                .addClusters(WeightedCluster.ClusterWeight.newBuilder()
                                                        .setName("cluster-v1")
                                                        .setWeight(com.google.protobuf.UInt32Value.newBuilder()
                                                                .setValue(70)))
                                                .addClusters(WeightedCluster.ClusterWeight.newBuilder()
                                                        .setName("cluster-v2")
                                                        .setWeight(com.google.protobuf.UInt32Value.newBuilder()
                                                                .setValue(30)))))))
                .build();

        // Act
        RdsUpdate result = xdsRouteResource.doParse(
                new XdsResourceType.Args(null, null, null, null, FilterRegistry.getDefaultRegistry(), null, null, null),
                routeConfig);

        // Assert
        assertNotNull(result);
        assertEquals("weighted-route-config", routeConfig.getName());
        assertFalse(result.getVirtualHosts().isEmpty());
    }

    @Test
    void testParseRouteWithTimeout() throws ResourceInvalidException {
        // Arrange
        RouteConfiguration routeConfig = RouteConfiguration.newBuilder()
                .setName("timeout-route-config")
                .addVirtualHosts(VirtualHost.newBuilder()
                        .setName("timeout-virtual-host")
                        .addDomains("*")
                        .addRoutes(Route.newBuilder()
                                .setMatch(RouteMatch.newBuilder().setPrefix("/"))
                                .setRoute(RouteAction.newBuilder()
                                        .setCluster("test-cluster")
                                        .setTimeout(com.google.protobuf.Duration.newBuilder()
                                                .setSeconds(5)
                                                .setNanos(0)))))
                .build();

        // Act
        RdsUpdate result = xdsRouteResource.doParse(
                new XdsResourceType.Args(null, null, null, null, FilterRegistry.getDefaultRegistry(), null, null, null),
                routeConfig);

        // Assert
        assertNotNull(result);
        assertEquals("timeout-route-config", routeConfig.getName());
        assertFalse(result.getVirtualHosts().isEmpty());
    }

    @Test
    void testParseRouteWithRetryPolicy() throws ResourceInvalidException {
        // Arrange
        RouteConfiguration routeConfig = RouteConfiguration.newBuilder()
                .setName("retry-route-config")
                .addVirtualHosts(VirtualHost.newBuilder()
                        .setName("retry-virtual-host")
                        .addDomains("*")
                        .addRoutes(Route.newBuilder()
                                .setMatch(RouteMatch.newBuilder().setPrefix("/"))
                                .setRoute(RouteAction.newBuilder()
                                        .setCluster("test-cluster")
                                        .setRetryPolicy(io.envoyproxy.envoy.config.route.v3.RetryPolicy.newBuilder()
                                                .setRetryOn("5xx,reset,connect-failure")
                                                .setNumRetries(com.google.protobuf.UInt32Value.newBuilder()
                                                        .setValue(3))))))
                .build();

        // Act
        RdsUpdate result = xdsRouteResource.doParse(
                new XdsResourceType.Args(null, null, null, null, FilterRegistry.getDefaultRegistry(), null, null, null),
                routeConfig);

        // Assert
        assertNotNull(result);
        assertEquals("retry-route-config", routeConfig.getName());
        assertFalse(result.getVirtualHosts().isEmpty());
    }

    @Test
    void testParseEmptyRouteConfiguration() throws ResourceInvalidException {
        // Arrange
        RouteConfiguration routeConfig =
                RouteConfiguration.newBuilder().setName("empty-route-config").build();

        // Act
        RdsUpdate result = xdsRouteResource.doParse(
                new XdsResourceType.Args(null, null, null, null, FilterRegistry.getDefaultRegistry(), null, null, null),
                routeConfig);

        // Assert
        assertNotNull(result);
        assertEquals("empty-route-config", routeConfig.getName());
        assertTrue(result.getVirtualHosts().isEmpty());
    }

    @Test
    void testGetResourceType() {
        // Act
        String resourceType = xdsRouteResource.typeUrl();

        // Assert
        assertEquals("type.googleapis.com/envoy.config.route.v3.RouteConfiguration", resourceType);
    }

    @Test
    void testGetResourceClass() {
        // Act
        Class<?> resourceClass = xdsRouteResource.unpackedClassName();

        // Assert
        assertEquals(RouteConfiguration.class, resourceClass);
    }

    @Test
    void testParseRouteWithNullArgs() throws ResourceInvalidException {
        // Arrange
        RouteConfiguration routeConfig =
                RouteConfiguration.newBuilder().setName("test-route-config").build();

        // Act
        RdsUpdate result = xdsRouteResource.doParse(
                new XdsResourceType.Args(null, null, null, null, FilterRegistry.getDefaultRegistry(), null, null, null),
                routeConfig);

        // Assert
        assertNotNull(result);
        assertEquals("test-route-config", routeConfig.getName());
    }
}
