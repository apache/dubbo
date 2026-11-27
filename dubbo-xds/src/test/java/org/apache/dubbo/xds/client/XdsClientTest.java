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
package org.apache.dubbo.xds.client;

import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.client.XdsClient.ResourceStatus;
import org.apache.dubbo.xds.client.XdsClient.ResourceWatcher;
import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;
import org.apache.dubbo.xds.registry.DiscoveryTarget;
import org.apache.dubbo.xds.traffic.XdsLoadBalance;

import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.AggregatedConfigSource;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.ConfigSource;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.ApiListener;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Listener;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.WeightedCluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.Rds;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XdsClientTest {
    @Test
    void resourceDiagnosticsDistinguishRequestsRejectionsLastGoodAndExplicitDeletion() throws Exception {
        try (FakeAds server = new FakeAds();
                XdsClient client = new XdsClient(server.bootstrap("node"))) {
            Watcher<Cluster> watcher = new Watcher<>();
            Cluster valid = clusterResource("cluster");
            Cluster invalid =
                    valid.toBuilder().setLbPolicy(Cluster.LbPolicy.MAGLEV).build();
            String key = XdsResourceType.CDS.getTypeUrl() + ":cluster";
            try (AutoCloseable watch =
                    client.watch(XdsResourceType.CDS, "cluster", watcher, XdsLoadBalance::validate)) {
                server.await(r -> r.getResourceNamesList().contains("cluster"));
                client.snapshot(XdsResourceType.CDS).get(3, TimeUnit.SECONDS);
                assertEquals(
                        ResourceStatus.REQUESTED,
                        client.getResourceMetadata().get(key).getStatus());
                server.send(XdsResourceType.CDS, "1", "initial-rejection", invalid);
                server.await(r -> "initial-rejection".equals(r.getResponseNonce()) && r.hasErrorDetail());
                client.snapshot(XdsResourceType.CDS).get(3, TimeUnit.SECONDS);
                assertEquals(
                        ResourceStatus.NACKED_WITHOUT_RESOURCE,
                        client.getResourceMetadata().get(key).getStatus());
                assertTrue(client.getResourceMetadata().get(key).getRejection().contains("MAGLEV"));
                server.send(XdsResourceType.CDS, "2", "accepted", valid);
                server.await(r -> "accepted".equals(r.getResponseNonce()) && !r.hasErrorDetail());
                client.snapshot(XdsResourceType.CDS).get(3, TimeUnit.SECONDS);
                assertEquals(
                        ResourceStatus.ACKED,
                        client.getResourceMetadata().get(key).getStatus());
                server.send(XdsResourceType.CDS, "3", "last-good", invalid);
                server.await(r -> "last-good".equals(r.getResponseNonce()) && r.hasErrorDetail());
                assertEquals(
                        valid,
                        client.snapshot(XdsResourceType.CDS)
                                .get(3, TimeUnit.SECONDS)
                                .get("cluster"));
                assertEquals(
                        ResourceStatus.NACKED_WITH_LAST_GOOD,
                        client.getResourceMetadata().get(key).getStatus());
                assertEquals("2", client.getResourceMetadata().get(key).getVersion());
                server.send(XdsResourceType.CDS, "4", "deleted");
                server.await(r -> "deleted".equals(r.getResponseNonce()) && !r.hasErrorDetail());
                assertTrue(client.snapshot(XdsResourceType.CDS)
                        .get(3, TimeUnit.SECONDS)
                        .isEmpty());
                assertEquals(
                        ResourceStatus.ABSENT,
                        client.getResourceMetadata().get(key).getStatus());
            }
            client.snapshot(XdsResourceType.CDS).get(3, TimeUnit.SECONDS);
            assertFalse(client.getResourceMetadata().containsKey(key));
        }
    }

    @Test
    void subscriptionValidationRejectsCachedPoliciesNacksUpdatesAndEndsWithItsOwner() throws Exception {
        try (FakeAds server = new FakeAds();
                XdsClient client = new XdsClient(server.bootstrap("node"))) {
            Watcher<Cluster> discovery = new Watcher<>();
            Watcher<Cluster> execution = new Watcher<>();
            Cluster unsupported = clusterResource("cluster").toBuilder()
                    .setLbPolicy(Cluster.LbPolicy.MAGLEV)
                    .build();
            try (AutoCloseable diagnostic = client.watch(XdsResourceType.CDS, "cluster", discovery)) {
                server.await(r -> r.getResourceNamesList().contains("cluster"));
                server.send(XdsResourceType.CDS, "1", "diagnostic", unsupported);
                assertEquals(unsupported, discovery.updates.poll(3, TimeUnit.SECONDS));
                try (AutoCloseable nativeCall =
                        client.watch(XdsResourceType.CDS, "cluster", execution, XdsLoadBalance::validate)) {
                    assertNotNull(execution.errors.poll(3, TimeUnit.SECONDS));
                    assertTrue(execution.updates.isEmpty(), "A strict consumer cannot use an invalid cached resource");
                    Cluster valid = clusterResource("cluster");
                    server.send(XdsResourceType.CDS, "2", "valid", valid);
                    assertEquals(valid, discovery.updates.poll(3, TimeUnit.SECONDS));
                    assertEquals(valid, execution.updates.poll(3, TimeUnit.SECONDS));
                    server.await(r -> "valid".equals(r.getResponseNonce()) && !r.hasErrorDetail());
                    Cluster bad = valid.toBuilder()
                            .setLbPolicy(Cluster.LbPolicy.LEAST_REQUEST)
                            .setLeastRequestLbConfig(
                                    Cluster.LeastRequestLbConfig.newBuilder().setChoiceCount(UInt32Value.of(0)))
                            .build();
                    server.send(XdsResourceType.CDS, "3", "invalid", bad);
                    DiscoveryRequest rejected =
                            server.await(r -> "invalid".equals(r.getResponseNonce()) && r.hasErrorDetail());
                    assertEquals("2", rejected.getVersionInfo());
                    assertEquals(
                            valid,
                            client.snapshot(XdsResourceType.CDS)
                                    .get(3, TimeUnit.SECONDS)
                                    .get("cluster"));
                    assertTrue(execution.updates.isEmpty());
                    assertTrue(discovery.updates.isEmpty());
                }
                client.snapshot(XdsResourceType.CDS).get(3, TimeUnit.SECONDS);
                server.send(XdsResourceType.CDS, "4", "discovery-only", unsupported);
                assertEquals(unsupported, discovery.updates.poll(3, TimeUnit.SECONDS));
                server.await(r -> "discovery-only".equals(r.getResponseNonce()) && !r.hasErrorDetail());
                assertTrue(execution.updates.isEmpty());
                assertEquals(1, server.streams.get());
            }
        }
    }

    @Test
    void sharedWatchersNackKeepsLastGoodAndExplicitDeletionRemovesIt() throws Exception {
        try (FakeAds server = new FakeAds();
                XdsClient client = new XdsClient(server.bootstrap("node"))) {
            Watcher<Listener> one = new Watcher<>();
            Watcher<Listener> two = new Watcher<>();
            try (AutoCloseable a = client.watch(XdsResourceType.LDS, "service", one);
                    AutoCloseable b = client.watch(XdsResourceType.LDS, "service", two)) {
                server.await(r -> r.getResourceNamesList().contains("service"));
                Listener resource = listenerResource("service");
                server.send(XdsResourceType.LDS, "1", "n1", resource);
                assertEquals(resource, one.updates.poll(5, TimeUnit.SECONDS));
                assertEquals(resource, two.updates.poll(5, TimeUnit.SECONDS));
                server.await(r -> "n1".equals(r.getResponseNonce()) && !r.hasErrorDetail());
                server.send(XdsResourceType.LDS, "2", "bad", StringValue.of("wrong payload"));
                DiscoveryRequest nack = server.await(r -> "bad".equals(r.getResponseNonce()) && r.hasErrorDetail());
                assertEquals("1", nack.getVersionInfo());
                assertEquals(
                        resource,
                        client.snapshot(XdsResourceType.LDS)
                                .get(5, TimeUnit.SECONDS)
                                .get("service"));
                server.send(XdsResourceType.LDS, "3", "removed");
                assertNotNull(one.absent.poll(5, TimeUnit.SECONDS));
                assertTrue(client.snapshot(XdsResourceType.LDS)
                        .get(5, TimeUnit.SECONDS)
                        .isEmpty());
                assertEquals(1, server.streams.get());
            }
        }
    }

    @Test
    void anOlderFullStateResponseDoesNotDeclareANewSubscriptionAbsent() throws Exception {
        try (FakeAds server = new FakeAds();
                XdsClient client = new XdsClient(server.bootstrap("node"))) {
            Watcher<Listener> old = new Watcher<>();
            Watcher<Listener> added = new Watcher<>();
            try (AutoCloseable first = client.watch(XdsResourceType.LDS, "old", old)) {
                server.await(r -> r.getResourceNamesList().contains("old"));
                server.send(XdsResourceType.LDS, "1", "old", listenerResource("old"));
                assertNotNull(old.updates.poll(5, TimeUnit.SECONDS));
                server.await(r -> "old".equals(r.getResponseNonce()));
                try (AutoCloseable second = client.watch(XdsResourceType.LDS, "new", added)) {
                    server.await(r -> r.getResourceNamesList().contains("new"));
                    server.send(XdsResourceType.LDS, "2", "older-response", listenerResource("old"));
                    server.await(r -> "older-response".equals(r.getResponseNonce()));
                    client.snapshot(XdsResourceType.LDS).get(5, TimeUnit.SECONDS);
                    assertFalse(client.getResourceMetadata()
                            .get(XdsResourceType.LDS.getTypeUrl() + ":new")
                            .isAbsent());
                    assertTrue(added.absent.isEmpty());
                }
            }
        }
    }

    @Test
    void poolSharesAcrossOwnersRejectsConflictingIdentityAndReleasesLastOwner() throws Exception {
        try (FakeAds server = new FakeAds()) {
            int baseline = XdsRuntimePool.activeRuntimeCount();
            try (XdsRuntimePool.Lease one = XdsRuntimePool.acquire(server.bootstrap("node"));
                    XdsRuntimePool.Lease two = XdsRuntimePool.acquire(server.bootstrap("node"))) {
                assertSame(one.getClient(), two.getClient());
                assertEquals(baseline + 1, XdsRuntimePool.activeRuntimeCount());
                assertThrows(IllegalStateException.class, () -> XdsRuntimePool.acquire(server.bootstrap("other-node")));
                BootstrapInfo rewritten = Bootstrapper.parse("{\"xds_servers\":[{\"server_uri\":\"localhost:"
                        + server.server.getPort()
                        + "\",\"channel_creds\":[{\"type\":\"insecure\"}],\"server_features\":[\"xds_v3\"]}],"
                        + "\"node\":{\"id\":\"node\",\"metadata\":{\"ANNOTATIONS\":{\"cloud\":\"allocated\"}}}}");
                assertThrows(IllegalStateException.class, () -> XdsRuntimePool.acquire(rewritten));
                one.close();
                assertEquals("IDLE", two.getClient().getConnectionState());
            }
            assertEquals(baseline, XdsRuntimePool.activeRuntimeCount());
        }
    }

    @Test
    void reconnectRestoresSubscriptionsAndVersionWithFreshNonce() throws Exception {
        try (FakeAds server = new FakeAds();
                XdsClient client = new XdsClient(server.bootstrap("node"))) {
            Watcher<Listener> watcher = new Watcher<>();
            try (AutoCloseable ignored = client.watch(XdsResourceType.LDS, "service", watcher)) {
                server.await(r -> r.getResourceNamesList().contains("service"));
                server.send(XdsResourceType.LDS, "v1", "old-nonce", listenerResource("service"));
                assertNotNull(watcher.updates.poll(5, TimeUnit.SECONDS));
                server.await(r -> "old-nonce".equals(r.getResponseNonce()));
                server.current().onCompleted();
                DiscoveryRequest resubscribe =
                        server.await(r -> r.getResponseNonce().isEmpty() && "v1".equals(r.getVersionInfo()));
                assertEquals("node", resubscribe.getNode().getId());
                assertEquals(2, server.streams.get());
            }
        }
    }

    @Test
    void dependencyGraphWaitsForBothWeightedClustersAndReleasesUnusedSubscriptions() throws Exception {
        try (FakeAds server = new FakeAds();
                XdsClient client = new XdsClient(server.bootstrap("node"))) {
            BlockingQueue<Snapshot> snapshots = new LinkedBlockingQueue<>();
            BlockingQueue<Throwable> errors = new LinkedBlockingQueue<>();
            DiscoveryTarget target = new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "tri");
            try (XdsResourceGraph graph =
                    new XdsResourceGraph(client, server.bootstrap("node"), target, snapshots::add, errors::add)) {
                server.await(r -> r.getResourceNamesList().contains(target.authority()));
                RouteConfiguration route = RouteConfiguration.newBuilder()
                        .setName("route")
                        .addVirtualHosts(VirtualHost.newBuilder()
                                .setName("echo")
                                .addDomains("*")
                                .addRoutes(Route.newBuilder()
                                        .setMatch(RouteMatch.newBuilder().setPrefix("/"))
                                        .setRoute(RouteAction.newBuilder()
                                                .setWeightedClusters(WeightedCluster.newBuilder()
                                                        .addClusters(weight("c1", 20))
                                                        .addClusters(weight("c2", 80))))))
                        .build();
                HttpConnectionManager hcm =
                        HttpConnectionManager.newBuilder().setRouteConfig(route).build();
                server.send(
                        XdsResourceType.LDS,
                        "1",
                        "lds",
                        Listener.newBuilder()
                                .setName(target.authority())
                                .setApiListener(ApiListener.newBuilder().setApiListener(Any.pack(hcm)))
                                .build());
                server.await(
                        r -> XdsResourceType.CDS.getTypeUrl().equals(r.getTypeUrl()) && r.getResourceNamesCount() == 2);
                Cluster c1 = clusterResource("c1");
                Cluster c2 = clusterResource("c2");
                server.send(XdsResourceType.CDS, "1", "cds", c1, c2);
                server.await(
                        r -> XdsResourceType.EDS.getTypeUrl().equals(r.getTypeUrl()) && r.getResourceNamesCount() == 2);
                server.send(
                        XdsResourceType.EDS,
                        "1",
                        "eds-one",
                        ClusterLoadAssignment.newBuilder().setClusterName("c1").build());
                server.await(r -> "eds-one".equals(r.getResponseNonce()));
                Snapshot partial = await(
                        snapshots,
                        snapshot -> snapshot.getEndpoints().containsKey("c1")
                                && !snapshot.getEndpoints().containsKey("c2"));
                assertFalse(partial.isReady());
                server.send(
                        XdsResourceType.EDS,
                        "2",
                        "eds-two",
                        ClusterLoadAssignment.newBuilder().setClusterName("c2").build());
                Snapshot ready = await(snapshots, Snapshot::isReady);
                assertEquals(2, ready.getEndpoints().size());
                server.send(XdsResourceType.CDS, "2", "remove-c1", c2);
                server.await(r -> XdsResourceType.EDS.getTypeUrl().equals(r.getTypeUrl())
                        && r.getResourceNamesCount() == 1
                        && r.getResourceNames(0).equals("c2"));
                Snapshot removed = await(
                        snapshots,
                        snapshot -> snapshot.getClusters().keySet().equals(Collections.singleton("c2"))
                                && snapshot.getEndpoints().keySet().equals(Collections.singleton("c2")));
                assertFalse(removed.isReady());
                assertTrue(errors.isEmpty());
            }
        }
    }

    private static Listener listenerResource(String name) {
        HttpConnectionManager hcm = HttpConnectionManager.newBuilder()
                .setRds(Rds.newBuilder().setRouteConfigName("route").setConfigSource(ads()))
                .build();
        return Listener.newBuilder()
                .setName(name)
                .setApiListener(ApiListener.newBuilder().setApiListener(Any.pack(hcm)))
                .build();
    }

    private static Cluster clusterResource(String name) {
        return Cluster.newBuilder()
                .setName(name)
                .setType(Cluster.DiscoveryType.EDS)
                .setEdsClusterConfig(Cluster.EdsClusterConfig.newBuilder().setEdsConfig(ads()))
                .build();
    }

    private static ConfigSource ads() {
        return ConfigSource.newBuilder()
                .setAds(AggregatedConfigSource.getDefaultInstance())
                .build();
    }

    private static WeightedCluster.ClusterWeight weight(String name, int weight) {
        return WeightedCluster.ClusterWeight.newBuilder()
                .setName(name)
                .setWeight(UInt32Value.of(weight))
                .build();
    }

    private static <T> T await(BlockingQueue<T> queue, Predicate<T> predicate) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            T value = queue.poll(100, TimeUnit.MILLISECONDS);
            if (value != null && predicate.test(value)) {
                return value;
            }
        }
        throw new AssertionError("Timed out awaiting protocol state");
    }

    private static final class Watcher<T> implements ResourceWatcher<T> {
        final BlockingQueue<T> updates = new LinkedBlockingQueue<>();
        final BlockingQueue<Boolean> absent = new LinkedBlockingQueue<>();
        final BlockingQueue<Throwable> errors = new LinkedBlockingQueue<>();

        @Override
        public void onChanged(T resource) {
            updates.add(resource);
        }

        @Override
        public void onAbsent() {
            absent.add(true);
        }

        @Override
        public void onError(Throwable error) {
            errors.add(error);
        }
    }

    private static final class FakeAds implements AutoCloseable {
        final BlockingQueue<DiscoveryRequest> requests = new LinkedBlockingQueue<>();
        final CopyOnWriteArrayList<StreamObserver<DiscoveryResponse>> responses = new CopyOnWriteArrayList<>();
        final AtomicInteger streams = new AtomicInteger();
        final Server server;

        FakeAds() throws Exception {
            server = NettyServerBuilder.forPort(0)
                    .addService(new AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceImplBase() {
                        @Override
                        public StreamObserver<DiscoveryRequest> streamAggregatedResources(
                                StreamObserver<DiscoveryResponse> response) {
                            responses.add(response);
                            streams.incrementAndGet();
                            return new StreamObserver<DiscoveryRequest>() {
                                @Override
                                public void onNext(DiscoveryRequest value) {
                                    requests.add(value);
                                }

                                @Override
                                public void onError(Throwable error) {}

                                @Override
                                public void onCompleted() {
                                    response.onCompleted();
                                }
                            };
                        }
                    })
                    .build()
                    .start();
        }

        BootstrapInfo bootstrap(String node) throws Exception {
            return Bootstrapper.parse("{\"xds_servers\":[{\"server_uri\":\"localhost:" + server.getPort()
                    + "\",\"channel_creds\":[{\"type\":\"insecure\"}],\"server_features\":[\"xds_v3\"]}],"
                    + "\"node\":{\"id\":\"" + node + "\"}}");
        }

        DiscoveryRequest await(Predicate<DiscoveryRequest> predicate) throws Exception {
            return XdsClientTest.await(requests, predicate);
        }

        StreamObserver<DiscoveryResponse> current() {
            return responses.get(responses.size() - 1);
        }

        void send(XdsResourceType<?> type, String version, String nonce, Message... resources) {
            DiscoveryResponse.Builder response = DiscoveryResponse.newBuilder()
                    .setTypeUrl(type.getTypeUrl())
                    .setVersionInfo(version)
                    .setNonce(nonce);
            for (Message resource : resources) {
                response.addResources(Any.pack(resource));
            }
            current().onNext(response.build());
        }

        @Override
        public void close() throws Exception {
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
