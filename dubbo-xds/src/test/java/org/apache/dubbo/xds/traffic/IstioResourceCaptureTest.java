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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Listener;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.fault.v3.HTTPFault;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.UpstreamTlsContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IstioResourceCaptureTest {
    private static final String TARGET = "echo-policy.dubbo-xds-test.svc.cluster.local";

    @Test
    void grpcgenProvidesSeparateClustersForGrpcAndTcpPortsOfTheSameService() throws Exception {
        Cluster triple = read("dual-triple", "cds", Cluster.newBuilder()).build();
        Cluster binary = read("dual-dubbo", "cds", Cluster.newBuilder()).build();
        assertEquals("outbound|7070||echo-dual.dubbo-xds-test.svc.cluster.local", triple.getName());
        assertEquals("outbound|7071||echo-dual.dubbo-xds-test.svc.cluster.local", binary.getName());
        assertTrue(triple.hasTransportSocket());
        assertFalse(binary.hasTransportSocket());
        assertEquals(0, binary.getTransportSocketMatchesCount());
        assertEquals(Cluster.DiscoveryType.EDS, binary.getType());
        for (String directory : new String[] {"dual-triple", "dual-dubbo"}) {
            ClusterLoadAssignment endpoints =
                    read(directory, "eds", ClusterLoadAssignment.newBuilder()).build();
            assertEquals(
                    directory.equals("dual-triple") ? triple.getName() : binary.getName(), endpoints.getClusterName());
            assertEquals(2, endpoints.getEndpoints(0).getLbEndpointsCount());
            endpoints.getEndpointsList().forEach(locality -> locality.getLbEndpointsList()
                    .forEach(endpoint -> assertEquals(
                            directory.equals("dual-triple") ? 17070 : 20880,
                            endpoint.getEndpoint()
                                    .getAddress()
                                    .getSocketAddress()
                                    .getPortValue())));
        }
        RouteConfiguration routes =
                read("dual-triple", "rds", RouteConfiguration.newBuilder()).build();
        Map<String, String> headers = new HashMap<>();
        headers.put(":authority", "echo-dual.dubbo-xds-test.svc.cluster.local:7070");
        headers.put(":path", "/xds.testing.WireEcho/SayHello");
        assertEquals(triple.getName(), new XdsRouteTable(routes).match(headers).selectCluster());
    }

    @Test
    void grpcgenRingHashIncludesHeaderPolicyAndItsCapturedRoutesAndEndpointsAreExecutable() throws Exception {
        Cluster cluster = read("hash-ring", "cds", Cluster.newBuilder()).build();
        assertEquals(Cluster.LbPolicy.RING_HASH, cluster.getLbPolicy());
        assertEquals(1024, cluster.getRingHashLbConfig().getMinimumRingSize().getValue());
        XdsLoadBalance.validate(cluster);
        ClusterLoadAssignment endpoints =
                read("hash-ring", "eds", ClusterLoadAssignment.newBuilder()).build();
        XdsRingHash.validateEndpoints(endpoints);
        RouteConfiguration routes =
                read("hash-ring", "rds", RouteConfiguration.newBuilder()).build();
        Listener listener = read("hash-ring", "lds", Listener.newBuilder()).build();
        assertEquals(
                routes.getName(),
                listener.getApiListener()
                        .getApiListener()
                        .unpack(HttpConnectionManager.class)
                        .getRds()
                        .getRouteConfigName());
        Map<String, String> headers = new HashMap<>();
        headers.put(":authority", "echo-ring.dubbo-xds-test.svc.cluster.local:7070");
        headers.put(":path", "/xds.testing.WireEcho/SayHello");
        headers.put("x-user", "alice");
        Match route = new XdsRouteTable(routes).match(headers);
        assertNotNull(route);
        assertEquals(cluster.getName(), route.selectCluster());
        assertEquals(
                "x-user",
                route.getRoute().getRoute().getHashPolicy(0).getHeader().getHeaderName());
        assertEquals(XdsHash64.string("alice"), route.requestHash(headers, 1));
        assertEquals(XdsHash64.string("alice"), route.requestHash(headers, 2));
        assertEquals(2, endpoints.getEndpoints(0).getLbEndpointsCount());
    }

    @Test
    void grpcgenCanEmitMaglevButNativeDoesNotSilentlyTreatItAsRoundRobin() throws Exception {
        Cluster cluster = read("hash-maglev", "cds", Cluster.newBuilder()).build();
        assertEquals(Cluster.LbPolicy.MAGLEV, cluster.getLbPolicy());
        assertEquals(65537, cluster.getMaglevLbConfig().getTableSize().getValue());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> XdsLoadBalance.validate(cluster))
                .getMessage()
                .contains("MAGLEV"));
        RouteConfiguration routes =
                read("hash-maglev", "rds", RouteConfiguration.newBuilder()).build();
        assertEquals(
                "x-user",
                routes.getVirtualHosts(0)
                        .getRoutes(0)
                        .getRoute()
                        .getHashPolicy(0)
                        .getHeader()
                        .getHeaderName());
        assertDoesNotThrow(() -> new XdsRouteTable(routes));
    }

    @Test
    void grpcgenActuallyEmitsFaultCircuitBreakerLeastRequestAndWorkloadPortsButNoOutlier() throws Exception {
        Listener listener = read("lds", Listener.newBuilder()).build();
        HttpConnectionManager hcm = listener.getApiListener().getApiListener().unpack(HttpConnectionManager.class);
        assertEquals("envoy.filters.http.fault", hcm.getHttpFilters(0).getName());
        assertEquals("envoy.filters.http.router", hcm.getHttpFilters(1).getName());
        assertTrue(new XdsFaultFilters(hcm).getPolicies().values().stream().noneMatch(XdsFaultPolicy::hasActions));
        RouteConfiguration routes = read("rds", RouteConfiguration.newBuilder()).build();
        assertEquals(routes.getName(), hcm.getRds().getRouteConfigName());
        HTTPFault fault = routes.getVirtualHosts(0)
                .getRoutes(0)
                .getTypedPerFilterConfigOrThrow("envoy.filters.http.fault")
                .unpack(HTTPFault.class);
        assertEquals(20_000_000, fault.getDelay().getFixedDelay().getNanos());
        assertEquals(14, fault.getAbort().getGrpcStatus());
        assertEquals(100_000, fault.getDelay().getPercentage().getNumerator());
        assertEquals(100_000, fault.getAbort().getPercentage().getNumerator());
        Cluster cluster = read("cds", Cluster.newBuilder()).build();
        assertEquals(2, XdsCircuitBreaker.limit(cluster));
        assertEquals(Cluster.LbPolicy.LEAST_REQUEST, cluster.getLbPolicy());
        assertDoesNotThrow(() -> XdsLoadBalance.validate(cluster));
        assertEquals(2, XdsLoadBalance.choiceCount(cluster));
        assertFalse(cluster.hasOutlierDetection());
        UpstreamTlsContext tls = cluster.getTransportSocket().getTypedConfig().unpack(UpstreamTlsContext.class);
        assertEquals(
                "spiffe://cluster.local/ns/dubbo-xds-test/sa/xds-echo",
                tls.getCommonTlsContext()
                        .getCombinedValidationContext()
                        .getDefaultValidationContext()
                        .getMatchSubjectAltNames(0)
                        .getExact());
        ClusterLoadAssignment endpoints =
                read("eds", ClusterLoadAssignment.newBuilder()).build();
        assertEquals(cluster.getEdsClusterConfig().getServiceName(), endpoints.getClusterName());
        assertEquals(2, endpoints.getEndpoints(0).getLbEndpointsCount());
        endpoints.getEndpointsList().forEach(locality -> locality.getLbEndpointsList()
                .forEach(endpoint -> assertEquals(
                        17070,
                        endpoint.getEndpoint().getAddress().getSocketAddress().getPortValue())));
    }

    @Test
    void completeIstioRouteAcceptsProvenanceAttemptCountAndFaultConfiguration() throws Exception {
        RouteConfiguration captured =
                read("rds", RouteConfiguration.newBuilder()).build();
        XdsRouteTable capturedTable = new XdsRouteTable(captured);
        VirtualHost host = captured.getVirtualHosts(0);
        assertTrue(host.getIncludeRequestAttemptCount());
        Route route = host.getRoutes(0);
        assertTrue(route.hasMetadata());
        Map<String, String> headers = new HashMap<>();
        headers.put(":authority", TARGET + ":7070");
        headers.put(":path", "/xds.testing.WireEcho/SayHello");
        Match selected = capturedTable.match(headers);
        assertNotNull(selected);
        assertEquals(captured.getName(), selected.selectCluster());
        assertEquals(3, selected.getRetryPolicy().getMaxAttempts());
        assertEquals(0, selected.getTimeoutNanos(1_000));
    }

    private static <T extends Message.Builder> T read(String name, T builder) throws Exception {
        return read("extension-policy", name, builder);
    }

    private static <T extends Message.Builder> T read(String directory, String name, T builder) throws Exception {
        try (InputStream input = IstioResourceCaptureTest.class.getResourceAsStream(
                "/istio-1.31.1/" + directory + "/" + name + ".textproto")) {
            assertNotNull(input);
            TextFormat.merge(new InputStreamReader(input, StandardCharsets.UTF_8), builder);
            return builder;
        }
    }
}
