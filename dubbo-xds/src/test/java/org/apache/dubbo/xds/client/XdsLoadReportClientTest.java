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
import org.apache.dubbo.xds.traffic.XdsLoadStats;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.AggregatedConfigSource;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.ConfigSource;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Locality;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.SelfConfigSource;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterStats;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.load_stats.v3.LoadReportingServiceGrpc;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.load_stats.v3.LoadStatsRequest;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.load_stats.v3.LoadStatsResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsLoadReportClientTest {
    private static final Locality LOCALITY =
            Locality.newBuilder().setRegion("r").setZone("z").build();

    @Test
    void realProtocolNegotiatesNodeFiltersIntervalsAndStreamingGaugeWithoutResettingDiagnostics() throws Exception {
        try (Controller controller = new Controller();
                XdsClient client = new XdsClient(controller.bootstrap())) {
            client.watch(XdsResourceType.CDS, "blue", watcher());
            client.watch(XdsResourceType.CDS, "green", watcher());
            awaitQueue(controller.adsRequests, request -> request.getResourceNamesList()
                    .containsAll(Arrays.asList("blue", "green")));
            Cluster blue = cluster("blue", false), green = cluster("green", false);
            controller.cds("v1", blue, green);
            controller.ack("v1", false);
            client.snapshot(XdsResourceType.CDS).get(2, TimeUnit.SECONDS);
            assertTrue(client.getLoadReportingStates().isEmpty());
            assertEquals(0, controller.lrsStreams.get());
            blue = cluster("blue", true);
            green = cluster("green", true);
            controller.cds("v2", blue, green);
            controller.ack("v2", false);
            LoadStatsRequest initial =
                    awaitQueue(controller.lrsRequests, request -> request.getClusterStatsCount() == 0);
            assertEquals("native-lrs-test", initial.getNode().getId());
            assertTrue(initial.getNode().getClientFeaturesList().contains("envoy.lrs.supports_send_all_clusters"));
            controller.interval(0, false, "blue");
            await(() -> client.getLoadReportingStates().containsValue("PAUSED"));
            blue = client.snapshot(XdsResourceType.CDS).get(2, TimeUnit.SECONDS).get("blue");
            green = client.snapshot(XdsResourceType.CDS)
                    .get(2, TimeUnit.SECONDS)
                    .get("green");
            XdsLoadStats stats = client.getLoadStats();
            XdsLoadStats.Attempt held = stats.started(blue, LOCALITY);
            stats.started(green, LOCALITY).finished(new IllegalStateException());
            stats.circuitDropped(blue);
            stats.edsDropped(blue, "throttle");
            controller.interval(30_000_000, false, "blue");
            LoadStatsRequest first = awaitQueue(
                    controller.lrsRequests,
                    request -> request.getClusterStatsCount() == 1
                            && request.getClusterStats(0).getTotalDroppedRequests() == 2);
            ClusterStats report = first.getClusterStats(0);
            assertEquals("blue", report.getClusterName());
            assertEquals(1, report.getUpstreamLocalityStats(0).getTotalIssuedRequests());
            assertEquals(1, report.getUpstreamLocalityStats(0).getTotalRequestsInProgress());
            assertEquals(1, report.getDroppedRequestsCount());
            assertEquals("throttle", report.getDroppedRequests(0).getCategory());
            assertTrue(report.getLoadReportInterval().getSeconds() > 0
                    || report.getLoadReportInterval().getNanos() > 0);
            LoadStatsRequest ongoing = awaitQueue(
                    controller.lrsRequests,
                    request -> request.getClusterStatsCount() == 1
                            && request.getClusterStats(0).getUpstreamLocalityStatsCount() > 0
                            && request.getClusterStats(0)
                                            .getUpstreamLocalityStats(0)
                                            .getTotalIssuedRequests()
                                    == 0);
            assertEquals(
                    1, ongoing.getClusterStats(0).getUpstreamLocalityStats(0).getTotalRequestsInProgress());
            held.finished(null);
            controller.interval(30_000_000, true);
            LoadStatsRequest all = awaitQueue(controller.lrsRequests, request -> request.getClusterStatsList().stream()
                    .anyMatch(s -> s.getClusterName().equals("green") && s.getUpstreamLocalityStatsCount() > 0));
            ClusterStats pending = all.getClusterStatsList().stream()
                    .filter(s -> s.getClusterName().equals("green"))
                    .findFirst()
                    .get();
            assertEquals(1, pending.getUpstreamLocalityStats(0).getTotalErrorRequests());
            assertEquals(1, pending.getUpstreamLocalityStats(0).getTotalIssuedRequests());
            assertEquals(
                    1,
                    stats.snapshot().stream()
                            .filter(s -> s.getCluster().equals("blue"))
                            .findFirst()
                            .get()
                            .getSuccessful());
            assertEquals(1, controller.lrsStreams.get());
            assertEquals(1, controller.adsStreams.get());
        }
    }

    @Test
    void lrsReconnectRetainsUnsentCountsAndLeavesAdsAlive() throws Exception {
        try (Controller controller = new Controller();
                XdsClient client = new XdsClient(controller.bootstrap())) {
            client.watch(XdsResourceType.CDS, "blue", watcher());
            awaitQueue(controller.adsRequests, request -> request.getResourceNamesList()
                    .contains("blue"));
            Cluster blue = cluster("blue", true);
            controller.cds("v1", blue);
            controller.ack("v1", false);
            awaitQueue(controller.lrsRequests, request -> request.getClusterStatsCount() == 0);
            controller.interval(0, true);
            await(() -> client.getLoadReportingStates().containsValue("PAUSED"));
            client.getLoadStats()
                    .started(
                            client.snapshot(XdsResourceType.CDS)
                                    .get(2, TimeUnit.SECONDS)
                                    .get("blue"),
                            LOCALITY)
                    .finished(null);
            controller.currentLrs().onError(Status.UNAVAILABLE.asRuntimeException());
            LoadStatsRequest reconnect =
                    awaitQueue(controller.lrsRequests, request -> request.getClusterStatsCount() == 0);
            assertEquals("native-lrs-test", reconnect.getNode().getId());
            assertEquals(2, controller.lrsStreams.get());
            controller.interval(30_000_000, true);
            LoadStatsRequest recovered = awaitQueue(
                    controller.lrsRequests,
                    request -> request.getClusterStatsCount() > 0
                            && request.getClusterStats(0).getUpstreamLocalityStatsCount() > 0);
            assertEquals(
                    1, recovered.getClusterStats(0).getUpstreamLocalityStats(0).getTotalSuccessfulRequests());
            assertEquals(1, controller.adsStreams.get());
            assertEquals("READY", client.getConnectionState());
            assertTrue(client.snapshot(XdsResourceType.CDS)
                    .get(2, TimeUnit.SECONDS)
                    .containsKey("blue"));
            client.close();
            await(() -> client.getConnectionState().equals("CLOSED"));
            assertTrue(client.getLoadReportingStates().isEmpty());
            assertTrue(client.getLoadStats().getReports().servers().isEmpty());
        }
    }

    @Test
    void malformedIntervalAndNonSelfCdsDoNotReplaceValidAdsResourcesAndDisablingClosesLrs() throws Exception {
        try (Controller controller = new Controller();
                XdsClient client = new XdsClient(controller.bootstrap())) {
            client.watch(XdsResourceType.CDS, "blue", watcher());
            awaitQueue(controller.adsRequests, request -> request.getResourceNamesList()
                    .contains("blue"));
            Cluster blue = cluster("blue", true);
            controller.cds("v1", blue);
            controller.ack("v1", false);
            awaitQueue(controller.lrsRequests, request -> request.getClusterStatsCount() == 0);
            controller
                    .currentLrs()
                    .onNext(LoadStatsResponse.newBuilder()
                            .setLoadReportingInterval(Duration.newBuilder().setNanos(1_000_000_000))
                            .build());
            awaitQueue(controller.lrsRequests, request -> request.getClusterStatsCount() == 0);
            controller.interval(0, true);
            await(() -> client.getLoadReportingStates().containsValue("PAUSED"));
            controller.cds(
                    "bad",
                    blue.toBuilder()
                            .setLrsServer(ConfigSource.newBuilder().setAds(AggregatedConfigSource.getDefaultInstance()))
                            .build());
            DiscoveryRequest nack = controller.ack("bad", true);
            assertEquals("v1", nack.getVersionInfo());
            assertEquals(
                    blue,
                    client.snapshot(XdsResourceType.CDS)
                            .get(2, TimeUnit.SECONDS)
                            .get("blue"));
            assertEquals(1, controller.adsStreams.get());
            controller.cds("off", cluster("blue", false));
            controller.ack("off", false);
            await(() -> client.getLoadReportingStates().isEmpty());
            assertTrue(client.getLoadStats().getReports().servers().isEmpty());
        }
    }

    @Test
    void identicalCdsOnFallbackUpdatesWatchersAndLateOldSnapshotCallsReportToTheirOwnOrigin() throws Exception {
        try (Controller first = new Controller();
                Controller second = new Controller();
                XdsClient client = new XdsClient(
                        Bootstrapper.parse(
                                "{\"xds_servers\":[{\"server_uri\":\"localhost:" + first.server.getPort()
                                        + "\",\"channel_creds\":[{\"type\":\"insecure\"}]},{\"server_uri\":\"localhost:"
                                        + second.server.getPort()
                                        + "\",\"channel_creds\":[{\"type\":\"insecure\"}]}],\"node\":{\"id\":\"native-lrs-test\"}}"))) {
            BlockingQueue<Cluster> changes = new LinkedBlockingQueue<>();
            client.watch(XdsResourceType.CDS, "blue", new XdsClient.ResourceWatcher<Cluster>() {
                public void onChanged(Cluster value) {
                    changes.add(value);
                }

                public void onAbsent() {}

                public void onError(Throwable error) {}
            });
            awaitQueue(
                    first.adsRequests, request -> request.getResourceNamesList().contains("blue"));
            Cluster resource = cluster("blue", true);
            first.cds("v1", resource);
            first.ack("v1", false);
            Cluster old = changes.poll(2, TimeUnit.SECONDS);
            assertNotNull(old);
            awaitQueue(first.lrsRequests, request -> request.getClusterStatsCount() == 0);
            first.interval(0, true);
            await(() -> client.getLoadReportingStates().containsValue("PAUSED"));
            first.adsResponses.get(0).onError(Status.UNAVAILABLE.asRuntimeException());
            awaitQueue(second.adsRequests, request -> request.getResourceNamesList()
                    .contains("blue"));
            second.cds("v1", resource);
            second.ack("v1", false);
            Cluster fresh = changes.poll(2, TimeUnit.SECONDS);
            assertNotNull(fresh, "Same protobuf on another server still updates its resource origin");
            assertNotSame(old, fresh);
            assertEquals(old, fresh);
            awaitQueue(second.lrsRequests, request -> request.getClusterStatsCount() == 0);
            second.interval(0, true);
            await(() -> "PAUSED".equals(client.getLoadReportingStates().get("server-1")));
            client.getLoadStats().started(old, LOCALITY).finished(new IllegalStateException());
            awaitQueue(first.lrsRequests, request -> request.getClusterStatsCount() == 0);
            first.interval(0, true);
            client.getLoadStats().started(fresh, LOCALITY).finished(null);
            await(() -> client.getLoadReportingStates().size() == 2
                    && client.getLoadReportingStates().values().stream().allMatch("PAUSED"::equals));
            first.interval(30_000_000, true);
            second.interval(30_000_000, true);
            LoadStatsRequest oldReport = awaitQueue(
                    first.lrsRequests,
                    request -> request.getClusterStatsCount() > 0
                            && request.getClusterStats(0).getUpstreamLocalityStatsCount() > 0);
            LoadStatsRequest currentReport = awaitQueue(
                    second.lrsRequests,
                    request -> request.getClusterStatsCount() > 0
                            && request.getClusterStats(0).getUpstreamLocalityStatsCount() > 0);
            assertEquals(
                    1, oldReport.getClusterStats(0).getUpstreamLocalityStats(0).getTotalErrorRequests());
            assertEquals(
                    0, oldReport.getClusterStats(0).getUpstreamLocalityStats(0).getTotalSuccessfulRequests());
            assertEquals(
                    1,
                    currentReport.getClusterStats(0).getUpstreamLocalityStats(0).getTotalSuccessfulRequests());
            assertEquals(
                    0,
                    currentReport.getClusterStats(0).getUpstreamLocalityStats(0).getTotalErrorRequests());
            assertEquals(
                    1,
                    currentReport.getClusterStats(0).getUpstreamLocalityStats(0).getTotalIssuedRequests());
            assertEquals(1, first.adsStreams.get());
            assertEquals(1, second.adsStreams.get());
            assertEquals(2, client.getLoadStats().snapshot().get(0).getIssued());
        }
    }

    private static Cluster cluster(String name, boolean lrs) {
        Cluster.Builder cluster = Cluster.newBuilder()
                .setName(name)
                .setType(Cluster.DiscoveryType.EDS)
                .setEdsClusterConfig(Cluster.EdsClusterConfig.newBuilder()
                        .setEdsConfig(ConfigSource.newBuilder().setAds(AggregatedConfigSource.getDefaultInstance())));
        if (lrs) cluster.setLrsServer(ConfigSource.newBuilder().setSelf(SelfConfigSource.getDefaultInstance()));
        return cluster.build();
    }

    private static XdsClient.ResourceWatcher<Cluster> watcher() {
        return new XdsClient.ResourceWatcher<Cluster>() {
            public void onChanged(Cluster cluster) {}

            public void onAbsent() {}

            public void onError(Throwable error) {}
        };
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        while (!condition.getAsBoolean() && System.nanoTime() < end) Thread.sleep(5);
        assertTrue(condition.getAsBoolean());
    }

    private static <T> T awaitQueue(BlockingQueue<T> queue, Predicate<T> predicate) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        while (System.nanoTime() < end) {
            T value = queue.poll(100, TimeUnit.MILLISECONDS);
            if (value != null && predicate.test(value)) return value;
        }
        throw new AssertionError("Missing protocol message");
    }

    private static final class Controller implements AutoCloseable {
        final BlockingQueue<DiscoveryRequest> adsRequests = new LinkedBlockingQueue<>();
        final BlockingQueue<LoadStatsRequest> lrsRequests = new LinkedBlockingQueue<>();
        final CopyOnWriteArrayList<StreamObserver<DiscoveryResponse>> adsResponses = new CopyOnWriteArrayList<>();
        final CopyOnWriteArrayList<StreamObserver<LoadStatsResponse>> lrsResponses = new CopyOnWriteArrayList<>();
        final AtomicInteger adsStreams = new AtomicInteger();
        final AtomicInteger lrsStreams = new AtomicInteger();
        final Server server;

        Controller() throws Exception {
            server = NettyServerBuilder.forPort(0)
                    .addService(new AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceImplBase() {
                        @Override
                        public StreamObserver<DiscoveryRequest> streamAggregatedResources(
                                StreamObserver<DiscoveryResponse> response) {
                            adsResponses.add(response);
                            adsStreams.incrementAndGet();
                            return new StreamObserver<DiscoveryRequest>() {
                                public void onNext(DiscoveryRequest request) {
                                    adsRequests.add(request);
                                }

                                public void onError(Throwable error) {}

                                public void onCompleted() {
                                    response.onCompleted();
                                }
                            };
                        }
                    })
                    .addService(new LoadReportingServiceGrpc.LoadReportingServiceImplBase() {
                        @Override
                        public StreamObserver<LoadStatsRequest> streamLoadStats(
                                StreamObserver<LoadStatsResponse> response) {
                            lrsResponses.add(response);
                            lrsStreams.incrementAndGet();
                            return new StreamObserver<LoadStatsRequest>() {
                                public void onNext(LoadStatsRequest request) {
                                    lrsRequests.add(request);
                                }

                                public void onError(Throwable error) {}

                                public void onCompleted() {
                                    response.onCompleted();
                                }
                            };
                        }
                    })
                    .build()
                    .start();
        }

        BootstrapInfo bootstrap() throws Exception {
            return Bootstrapper.parse("{\"xds_servers\":[{\"server_uri\":\"localhost:" + server.getPort()
                    + "\",\"channel_creds\":[{\"type\":\"insecure\"}]}],\"node\":{\"id\":\"native-lrs-test\"}}");
        }

        void cds(String version, Cluster... clusters) {
            DiscoveryResponse.Builder response = DiscoveryResponse.newBuilder()
                    .setTypeUrl(XdsResourceType.CDS.getTypeUrl())
                    .setVersionInfo(version)
                    .setNonce(version);
            for (Cluster cluster : clusters) response.addResources(Any.pack(cluster));
            adsResponses.get(adsResponses.size() - 1).onNext(response.build());
        }

        DiscoveryRequest ack(String nonce, boolean error) throws Exception {
            return awaitQueue(
                    adsRequests,
                    request -> request.getResponseNonce().equals(nonce) && request.hasErrorDetail() == error);
        }

        StreamObserver<LoadStatsResponse> currentLrs() {
            return lrsResponses.get(lrsResponses.size() - 1);
        }

        void interval(int nanos, boolean all, String... clusters) {
            currentLrs()
                    .onNext(LoadStatsResponse.newBuilder()
                            .setSendAllClusters(all)
                            .addAllClusters(Arrays.asList(clusters))
                            .setLoadReportingInterval(Duration.newBuilder().setNanos(nanos))
                            .build());
        }

        @Override
        public void close() throws Exception {
            server.shutdownNow();
            assertTrue(server.awaitTermination(4, TimeUnit.SECONDS));
        }
    }
}
