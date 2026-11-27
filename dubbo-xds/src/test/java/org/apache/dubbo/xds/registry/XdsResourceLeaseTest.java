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

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.client.XdsClient;
import org.apache.dubbo.xds.client.XdsClient.ResourceWatcher;
import org.apache.dubbo.xds.client.XdsResourceGraph;
import org.apache.dubbo.xds.client.XdsResourceGraph.ClusterLease;
import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.client.XdsRuntimePool;
import org.apache.dubbo.xds.registry.XdsServiceDiscoveryTest.Ads;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.ApiListener;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Listener;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class XdsResourceLeaseTest {
    private static final String BLUE = "outbound|7070|blue|echo.test.svc.cluster.local";
    private static final String GREEN = "outbound|7070|green|echo.test.svc.cluster.local";

    @Test
    void callbacksAlreadyEnteringAReleasedWatchCannotReviveOrDeleteItsReplacement() throws Exception {
        Map<String, ResourceWatcher<?>> watchers = new LinkedHashMap<>();
        XdsClient client = mock(XdsClient.class);
        when(client.watch(any(), anyString(), any(), any())).thenAnswer(call -> {
            XdsResourceType<?> type = call.getArgument(0);
            watchers.put(type.getTypeUrl() + "#" + call.getArgument(1), call.getArgument(2));
            return (AutoCloseable) () -> {};
        });
        BootstrapInfo bootstrap = Bootstrapper.parse("{\"xds_servers\":[{\"server_uri\":\"localhost:1234\","
                + "\"channel_creds\":[{\"type\":\"insecure\"}]}],\"node\":{\"id\":\"resource-test\"}}");
        DiscoveryTarget target = new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "tri");
        try (XdsResourceGraph graph =
                new XdsResourceGraph(client, bootstrap, target, ignored -> {}, error -> fail(error))) {
            ResourceWatcher<Listener> lds = watcher(watchers, XdsResourceType.LDS, target.authority());
            lds.onChanged(listener(BLUE));
            ResourceWatcher<Cluster> oldCds = watcher(watchers, XdsResourceType.CDS, BLUE);
            Cluster blue = Cluster.newBuilder()
                    .setName(BLUE)
                    .setType(Cluster.DiscoveryType.EDS)
                    .build();
            oldCds.onChanged(blue);
            ResourceWatcher<ClusterLoadAssignment> oldEds = watcher(watchers, XdsResourceType.EDS, BLUE);
            ClusterLoadAssignment assignment =
                    ClusterLoadAssignment.newBuilder().setClusterName(BLUE).build();
            oldEds.onChanged(assignment);
            ClusterLease call = graph.getSnapshot().retainCluster(current -> BLUE);
            lds.onChanged(listener(GREEN));
            call.close();
            oldCds.onChanged(blue);
            oldEds.onChanged(assignment);
            assertFalse(graph.getSnapshot().getClusters().containsKey(BLUE));
            assertFalse(graph.getSnapshot().getEndpoints().containsKey(BLUE));
            lds.onChanged(listener(BLUE));
            ResourceWatcher<Cluster> replacement = watcher(watchers, XdsResourceType.CDS, BLUE);
            assertNotSame(oldCds, replacement);
            replacement.onChanged(blue);
            watcher(watchers, XdsResourceType.EDS, BLUE).onChanged(assignment);
            oldCds.onAbsent();
            oldEds.onAbsent();
            assertEquals(blue, graph.getSnapshot().getClusters().get(BLUE));
            assertEquals(assignment, graph.getSnapshot().getEndpoints().get(BLUE));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Message> ResourceWatcher<T> watcher(
            Map<String, ResourceWatcher<?>> watchers, XdsResourceType<T> type, String name) {
        return (ResourceWatcher<T>) watchers.get(type.getTypeUrl() + "#" + name);
    }

    private static Listener listener(String cluster) {
        return Listener.newBuilder()
                .setApiListener(ApiListener.newBuilder()
                        .setApiListener(Any.pack(HttpConnectionManager.newBuilder()
                                .setRouteConfig(route(cluster))
                                .build())))
                .build();
    }

    @Test
    void oldSnapshotsSelectCurrentRoutesAndResourcesRemainUntilTheLastOwnerReleases() throws Exception {
        try (Fixture f = new Fixture()) {
            Snapshot old = f.graph.getSnapshot();
            ClusterLease first = old.retainCluster(current -> BLUE);
            ClusterLease second = old.retainCluster(current -> BLUE);
            f.routes(GREEN);
            assertEquals(2, f.graph.getRetainedClusters().get(BLUE));
            assertTrue(f.graph.getSnapshot().getClusters().containsKey(BLUE));
            try (ClusterLease fresh = old.retainCluster(current -> {
                assertNotSame(old, current);
                return current.getRoutes()
                        .get("routes")
                        .getVirtualHosts(0)
                        .getRoutes(0)
                        .getRoute()
                        .getCluster();
            })) {
                assertEquals(GREEN, fresh.getCluster());
                assertEquals(
                        GREEN,
                        fresh.getSelectedSnapshot()
                                .getRoutes()
                                .get("routes")
                                .getVirtualHosts(0)
                                .getRoutes(0)
                                .getRoute()
                                .getCluster());
                first.close();
                first.close();
                assertEquals(1, f.graph.getRetainedClusters().get(BLUE));
                assertTrue(f.subscribed(BLUE, XdsResourceType.CDS.getTypeUrl()));
                second.close();
                await(() -> !f.subscribed(BLUE, XdsResourceType.CDS.getTypeUrl())
                        && !f.subscribed(BLUE, XdsResourceType.EDS.getTypeUrl()));
                assertFalse(f.graph.getSnapshot().getClusters().containsKey(BLUE));
                assertNull(first.getSnapshot());
            }
            assertTrue(f.graph.getRetainedClusters().isEmpty());
            f.graph.close();
            assertThrows(IllegalStateException.class, () -> old.retainCluster(current -> BLUE));
        }
    }

    @Test
    void clusterOwnershipDoesNotPreserveARevokedCdsOrEdsValue() throws Exception {
        try (Fixture f = new Fixture();
                ClusterLease selected = f.graph.getSnapshot().retainCluster(current -> BLUE)) {
            f.routes(GREEN);
            f.ads.clusters = Collections.singletonList(GREEN);
            f.ads.send(XdsResourceType.CDS.getTypeUrl());
            await(() -> !selected.getSnapshot().getClusters().containsKey(BLUE));
            await(() -> !f.subscribed(BLUE, XdsResourceType.EDS.getTypeUrl()));
            assertTrue(f.subscribed(BLUE, XdsResourceType.CDS.getTypeUrl()));
            assertEquals(1, f.graph.getRetainedClusters().get(BLUE));
            assertTrue(selected.getSnapshot().isReady(), "The new route remains ready after old CDS deletion");
            assertFalse(selected.getSnapshot().getEndpoints().containsKey(BLUE));
            f.graph.close();
            assertNull(selected.getSnapshot());
            assertTrue(f.graph.getRetainedClusters().isEmpty());
        }
    }

    private static final class Fixture implements AutoCloseable {
        final FrameworkModel framework = new FrameworkModel();
        final ApplicationModel application = framework.newApplication();
        final Ads ads = new Ads();
        final XdsRuntimePool.Lease runtime;
        final XdsResourceGraph graph;

        Fixture() throws Exception {
            application.getApplicationConfigManager().setApplication(new ApplicationConfig("resource-test"));
            ads.clusters = Arrays.asList(BLUE, GREEN);
            ads.routeOverride = route(BLUE);
            ads.endpoints("127.0.0.1");
            BootstrapInfo bootstrap =
                    Bootstrapper.parse(ads.registry(application).getParameter("xds.bootstrap.config"));
            runtime = XdsRuntimePool.acquire(bootstrap);
            graph = new XdsResourceGraph(
                    runtime.getClient(),
                    bootstrap,
                    new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "tri"),
                    ignored -> {},
                    error -> {
                        throw new AssertionError(error);
                    });
            await(() -> graph.getSnapshot().isReady());
        }

        boolean subscribed(String cluster, String type) {
            return ads.names.getOrDefault(type, Collections.emptyList()).contains(cluster);
        }

        void routes(String cluster) throws Exception {
            RouteConfiguration changed = route(cluster);
            ads.routeOverride = changed;
            ads.send(XdsResourceType.LDS.getTypeUrl());
            await(() -> graph.getSnapshot().getRoutes().get("routes").equals(changed)
                    && graph.getSnapshot().isReady());
        }

        public void close() throws Exception {
            graph.close();
            runtime.close();
            ads.close();
            framework.destroy();
        }
    }

    private static RouteConfiguration route(String cluster) {
        return RouteConfiguration.newBuilder()
                .setName("routes")
                .addVirtualHosts(VirtualHost.newBuilder()
                        .addDomains("*")
                        .addRoutes(Route.newBuilder()
                                .setMatch(RouteMatch.newBuilder().setPrefix("/"))
                                .setRoute(RouteAction.newBuilder().setCluster(cluster))))
                .build();
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > until) fail("Timed out waiting for resource ownership update");
            Thread.sleep(10);
        }
    }
}
