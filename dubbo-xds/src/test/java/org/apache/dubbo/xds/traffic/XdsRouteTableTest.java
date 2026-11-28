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
package org.apache.dubbo.xds.traffic;

import org.apache.dubbo.xds.traffic.XdsRouteTable.Match;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.UInt32Value;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.RuntimeFractionalPercent;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.HeaderMatcher;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.WeightedCluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.RegexMatcher;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.v3.FractionalPercent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsRouteTableTest {
    @Test
    void exactDomainWinsAndMissingRouteNeverFallsBackToAnotherVirtualHost() {
        RouteConfiguration config = RouteConfiguration.newBuilder()
                .setName("routes")
                .addVirtualHosts(
                        host("*", route("wildcard", RouteMatch.newBuilder().setPrefix("/"))))
                .addVirtualHosts(host(
                        "*.svc:7070", route("suffix", RouteMatch.newBuilder().setPrefix("/"))))
                .addVirtualHosts(
                        host("echo*", route("prefix", RouteMatch.newBuilder().setPrefix("/"))))
                .addVirtualHosts(host(
                        "echo.test.svc:7070",
                        route("exact", RouteMatch.newBuilder().setPath("/svc/hello"))))
                .build();
        XdsRouteTable table = new XdsRouteTable(config);
        assertEquals(
                "exact",
                table.match(headers("echo.test.svc:7070", "/svc/hello")).selectCluster());
        assertNull(table.match(headers("echo.test.svc:7070", "/svc/missing")));
        assertEquals(
                "suffix",
                table.match(headers("other.test.svc:7070", "/svc/hello")).selectCluster());
        assertEquals(
                "wildcard", table.match(headers("unrelated:80", "/svc/hello")).selectCluster());
    }

    @Test
    void routesInOrderByWirePathHeadersAndDefaultFraction() {
        RouteMatch.Builder onlyTenant = RouteMatch.newBuilder()
                .setSafeRegex(RegexMatcher.newBuilder().setRegex("/svc/.*"))
                .addHeaders(HeaderMatcher.newBuilder().setName("host").setExactMatch("echo.test.svc:7070"))
                .addHeaders(HeaderMatcher.newBuilder().setName("x-tenant").setExactMatch("blue"));
        RouteMatch.Builder disabled = RouteMatch.newBuilder()
                .setPrefix("/")
                .setRuntimeFraction(RuntimeFractionalPercent.newBuilder()
                        .setDefaultValue(FractionalPercent.newBuilder().setNumerator(0)));
        XdsRouteTable table = new XdsRouteTable(RouteConfiguration.newBuilder()
                .setName("routes")
                .addVirtualHosts(VirtualHost.newBuilder()
                        .addDomains("*")
                        .addRoutes(route("disabled", disabled))
                        .addRoutes(route("blue", onlyTenant))
                        .addRoutes(route("green", RouteMatch.newBuilder().setPrefix("/"))))
                .build());
        Map<String, String> headers = headers("echo.test.svc:7070", "/svc/hello");
        assertEquals("green", table.match(headers).selectCluster());
        headers.put("x-tenant", "blue");
        assertEquals("blue", table.match(headers).selectCluster());
    }

    @Test
    void weightedClustersAreIndependentOfEndpointCountsAndZeroWeightIsNeverPicked() {
        WeightedCluster.Builder weights = WeightedCluster.newBuilder()
                .addClusters(weight("blue", 20))
                .addClusters(weight("green", 80))
                .addClusters(weight("disabled", 0));
        Route route = Route.newBuilder()
                .setMatch(RouteMatch.newBuilder().setPrefix("/"))
                .setRoute(RouteAction.newBuilder().setWeightedClusters(weights))
                .build();
        Match match = new XdsRouteTable(RouteConfiguration.newBuilder()
                        .setName("routes")
                        .addVirtualHosts(host("*", route))
                        .build())
                .match(headers("echo:7070", "/svc/hello"));
        int blue = 0;
        for (int i = 0; i < 1000; i++) {
            String cluster = match.selectCluster();
            assertNotEquals("disabled", cluster);
            if (cluster.equals("blue")) {
                blue++;
            }
        }
        assertTrue(blue >= 140 && blue <= 260, "Observed blue requests: " + blue);
    }

    @Test
    void invalidWeightsDuplicateDomainsAndUnsupportedConditionsAreRejected() {
        Route invalid = Route.newBuilder()
                .setMatch(RouteMatch.newBuilder().setPrefix("/"))
                .setRoute(RouteAction.newBuilder()
                        .setWeightedClusters(WeightedCluster.newBuilder()
                                .setTotalWeight(UInt32Value.of(100))
                                .addClusters(weight("blue", 20))
                                .addClusters(weight("green", 20))))
                .build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new XdsRouteTable(RouteConfiguration.newBuilder()
                        .addVirtualHosts(host("*", invalid))
                        .build()));
        Route valid = route("blue", RouteMatch.newBuilder().setPrefix("/"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new XdsRouteTable(RouteConfiguration.newBuilder()
                        .addVirtualHosts(host("ECHO", valid))
                        .addVirtualHosts(host("echo", valid))
                        .build()));
        Route unsupported = valid.toBuilder()
                .setMatch(valid.getMatch().toBuilder().setGrpc(RouteMatch.GrpcRouteMatchOptions.getDefaultInstance()))
                .build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new XdsRouteTable(RouteConfiguration.newBuilder()
                        .addVirtualHosts(host("*", unsupported))
                        .build()));
    }

    private static VirtualHost host(String domain, Route route) {
        return VirtualHost.newBuilder().addDomains(domain).addRoutes(route).build();
    }

    private static Route route(String cluster, RouteMatch.Builder match) {
        return Route.newBuilder()
                .setMatch(match)
                .setRoute(RouteAction.newBuilder().setCluster(cluster))
                .build();
    }

    private static WeightedCluster.ClusterWeight weight(String name, int value) {
        return WeightedCluster.ClusterWeight.newBuilder()
                .setName(name)
                .setWeight(UInt32Value.of(value))
                .build();
    }

    private static Map<String, String> headers(String authority, String path) {
        Map<String, String> values = new HashMap<>();
        values.put(":authority", authority);
        values.put(":path", path);
        return values;
    }
}
