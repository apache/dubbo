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

import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.registry.XdsRetryTest.Fixture;
import org.apache.dubbo.xds.registry.XdsRetryTest.Implementation;
import org.apache.dubbo.xds.registry.XdsRetryTest.Request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster.RingHashLbConfig;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction.HashPolicy;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsRingHashRoutingTest {
    @Test
    void nativeWireHeadersChooseStickyEndpointsAndDoNotChangeRouteClusterSelection() throws Exception {
        try (Fixture f = new Fixture()) {
            install(f);
            Map<String, String> sticky = new HashMap<>();
            Set<String> providers = new HashSet<>();
            for (int user = 0; user < 32; user++) {
                String key = "user-" + user;
                String first = call(f, key, "first").get(2, TimeUnit.SECONDS).split(":")[0];
                sticky.put(key, first);
                providers.add(first);
                assertEquals(first + ":repeat", call(f, key, "repeat").get(2, TimeUnit.SECONDS));
            }
            assertEquals(2, providers.size());
            assertEquals(0, f.green.calls.get(), "Hash is evaluated after weighted route-cluster selection");
            XdsLeastRequestTest.endpoints(f, f.ads.workloadPort, f.replacementPort);
            for (Map.Entry<String, String> entry : sticky.entrySet()) {
                assertEquals(
                        entry.getValue() + ":after-refresh",
                        call(f, entry.getKey(), "after-refresh").get(2, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    void unaryRetriesRetainTheRequestHashAndRandomFallbackIsNotResampledByAttempts() throws Exception {
        try (Fixture f = new Fixture()) {
            install(f);
            String chosen =
                    call(f, "retry-user", "identify").get(2, TimeUnit.SECONDS).split(":")[0];
            f.blue.reset();
            f.replacement.reset();
            f.blue.failures = 2;
            f.replacement.failures = 2;
            assertEquals(chosen + ":retried", call(f, "retry-user", "retried").get(2, TimeUnit.SECONDS));
            Implementation selected = "blue".equals(chosen) ? f.blue : f.replacement;
            Implementation other = selected == f.blue ? f.replacement : f.blue;
            assertEquals(3, selected.calls.get());
            assertEquals(0, other.calls.get());
            f.blue.reset();
            f.replacement.reset();
            f.blue.failures = 2;
            f.replacement.failures = 2;
            String random = call(f, null, "random").get(2, TimeUnit.SECONDS).split(":")[0];
            assertEquals(3, "blue".equals(random) ? f.blue.calls.get() : f.replacement.calls.get());
            assertEquals(0, "blue".equals(random) ? f.replacement.calls.get() : f.blue.calls.get());
            assertEquals(0, f.green.calls.get());
        }
    }

    @Test
    void invalidCdsAndRingEndpointUpdatesNackAndKeepTheLastWorkingRing() throws Exception {
        try (Fixture f = new Fixture()) {
            install(f);
            XdsEndpoint observed = XdsLeastRequestTest.blue(f);
            Cluster good = observed.getCluster();
            String expected =
                    call(f, "last-good", "baseline").get(2, TimeUnit.SECONDS).split(":")[0];
            for (Cluster bad : new Cluster[] {
                good.toBuilder().setLbPolicy(Cluster.LbPolicy.MAGLEV).build(),
                good.toBuilder()
                        .setRingHashLbConfig(RingHashLbConfig.newBuilder().setMinimumRingSize(UInt64Value.of(0)))
                        .build(),
                good.toBuilder()
                        .setRingHashLbConfig(RingHashLbConfig.newBuilder().setMaximumRingSize(UInt64Value.of(8388609)))
                        .build()
            }) {
                int rejected = f.ads.rejected.get();
                f.ads.clusterOverrides.put(good.getName(), bad);
                f.ads.send(XdsResourceType.CDS.getTypeUrl());
                await(() -> f.ads.rejected.get() > rejected);
                assertEquals(good, observed.getSnapshot().getClusters().get(good.getName()));
                assertEquals(
                        expected + ":last-good",
                        call(f, "last-good", "last-good").get(2, TimeUnit.SECONDS));
            }
            f.ads.clusterOverrides.put(good.getName(), good);
            ClusterLoadAssignment original = f.ads.clusterAssignments.get(good.getName());
            LocalityLbEndpoints first = original.getEndpoints(0);
            int rejected = f.ads.rejected.get();
            ClusterLoadAssignment oversized = original.toBuilder()
                    .setEndpoints(0, first.toBuilder().setLoadBalancingWeight(UInt32Value.of(-1)))
                    .build();
            f.ads.clusterAssignments.put(good.getName(), oversized);
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> f.ads.rejected.get() > rejected);
            assertEquals(original, observed.getSnapshot().getEndpoints().get(good.getName()));
            assertEquals(
                    expected + ":eds-retained",
                    call(f, "last-good", "eds-retained").get(2, TimeUnit.SECONDS));
        }
    }

    private static void install(Fixture f) throws Exception {
        XdsEndpoint blue = XdsLeastRequestTest.blue(f);
        XdsLeastRequestTest.policy(
                f,
                blue.getCluster().toBuilder()
                        .setLbPolicy(Cluster.LbPolicy.RING_HASH)
                        .build());
        XdsLeastRequestTest.endpoints(f, blue.getPort(), f.replacementPort);
        RouteConfiguration base = f.config(100, 6000);
        RouteConfiguration routes = base.toBuilder()
                .setVirtualHosts(
                        0,
                        base.getVirtualHosts(0).toBuilder()
                                .setRoutes(
                                        0,
                                        base.getVirtualHosts(0).getRoutes(0).toBuilder()
                                                .setRoute(base.getVirtualHosts(0).getRoutes(0).getRoute().toBuilder()
                                                        .addHashPolicy(HashPolicy.newBuilder()
                                                                .setHeader(HashPolicy.Header.newBuilder()
                                                                        .setHeaderName("x-user"))))))
                .build();
        f.ads.routeOverride = routes;
        f.ads.send(XdsResourceType.LDS.getTypeUrl());
        await(() -> routes.equals(blue.getSnapshot().getRoutes().get("routes")));
    }

    private static CompletableFuture<String> call(Fixture f, String key, String value) {
        if (key != null) RpcContext.getClientAttachment().setAttachment("x-user", key);
        RpcContext.getClientAttachment().setAttachment("timeout", 6000);
        try {
            return f.remote.call(new Request(value));
        } finally {
            RpcContext.removeClientAttachment();
        }
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < until) Thread.sleep(10);
        assertTrue(condition.getAsBoolean());
    }
}
