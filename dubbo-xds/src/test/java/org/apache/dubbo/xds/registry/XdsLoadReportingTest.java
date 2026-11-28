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

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.registry.XdsRetryTest.Fixture;
import org.apache.dubbo.xds.registry.XdsRetryTest.Request;
import org.apache.dubbo.xds.security.NativeTlsAdapter;
import org.apache.dubbo.xds.traffic.XdsCircuitBreaker;

import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import com.google.protobuf.Duration;
import com.google.protobuf.UInt32Value;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.CircuitBreakers;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.ConfigSource;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.SelfConfigSource;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterStats;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.load_stats.v3.LoadReportingServiceGrpc;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.load_stats.v3.LoadStatsRequest;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.load_stats.v3.LoadStatsResponse;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.v3.FractionalPercent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsLoadReportingTest {
    @Test
    void unsupportedLrsServiceDoesNotInterruptDiscoveryOrTripleCalls() throws Exception {
        XdsServiceDiscovery discovery;
        try (Fixture f = new Fixture()) {
            XdsEndpoint endpoint = f.directory.getAllInvokers().stream()
                    .map(invoker -> NativeTlsAdapter.endpoint(invoker.getUrl()))
                    .filter(value -> value.getCluster().getName().equals(f.ads.clusters.get(0)))
                    .findFirst()
                    .get();
            String name = endpoint.getCluster().getName();
            Cluster cluster = endpoint.getCluster().toBuilder()
                    .setLrsServer(ConfigSource.newBuilder().setSelf(SelfConfigSource.getDefaultInstance()))
                    .build();
            f.ads.clusterOverrides.put(name, cluster);
            f.ads.send(XdsResourceType.CDS.getTypeUrl());
            discovery = (XdsServiceDiscovery) f.registry.getServiceDiscovery();
            await(() -> discovery.inspectLoadReporting().containsValue("BACKOFF:UNIMPLEMENTED"));
            await(() -> cluster.equals(endpoint.getSnapshot().getClusters().get(name)));
            assertEquals("blue:lrs-unimplemented", f.remote.sync("lrs-unimplemented"));
            assertEquals(1, f.blue.calls.get());
            Cluster disabled = cluster.toBuilder().clearLrsServer().build();
            f.ads.clusterOverrides.put(name, disabled);
            f.ads.send(XdsResourceType.CDS.getTypeUrl());
            await(() -> disabled.equals(endpoint.getSnapshot().getClusters().get(name)));
            assertEquals("blue:lrs-disabled", f.remote.sync("lrs-disabled"));
            assertEquals(2, f.blue.calls.get());
            ClusterStats pending = endpoint.getSecurity()
                    .getLoadStats()
                    .getReports()
                    .prepare(endpoint.getSecurity().getBootstrap().getServers().get(0), Collections.emptySet(), true)
                    .getStats()
                    .get(0);
            assertEquals(1, pending.getUpstreamLocalityStats(0).getTotalIssuedRequests());
            assertEquals(1, pending.getUpstreamLocalityStats(0).getTotalSuccessfulRequests());
            assertFalse(discovery.inspectLoadReporting().isEmpty(), "Unsent retired counts remain pending");
        }
        await(() -> discovery.inspectLoadReporting().isEmpty());
    }

    @Test
    void actualTripleRetryStreamAndMetadataLifecycleAppearInWireReports() throws Exception {
        Sink sink = new Sink();
        try (Fixture f = new Fixture(Collections.emptyMap(), sink)) {
            XdsEndpoint endpoint = f.directory.getAllInvokers().stream()
                    .map(invoker -> NativeTlsAdapter.endpoint(invoker.getUrl()))
                    .filter(value -> value.getCluster().getName().equals(f.ads.clusters.get(0)))
                    .findFirst()
                    .get();
            String clusterName = endpoint.getCluster().getName();
            Cluster cluster = endpoint.getCluster().toBuilder()
                    .setLrsServer(ConfigSource.newBuilder().setSelf(SelfConfigSource.getDefaultInstance()))
                    .setCircuitBreakers(CircuitBreakers.newBuilder()
                            .addThresholds(
                                    CircuitBreakers.Thresholds.newBuilder().setMaxRequests(UInt32Value.of(1))))
                    .build();
            f.ads.clusterOverrides.put(clusterName, cluster);
            f.ads.send(XdsResourceType.CDS.getTypeUrl());
            assertNotNull(sink.initial.poll(3, TimeUnit.SECONDS));
            XdsServiceDiscovery discovery = (XdsServiceDiscovery) f.registry.getServiceDiscovery();
            await(() -> discovery.inspectLoadReporting().containsValue("PAUSED"));
            await(() -> cluster.equals(endpoint.getSnapshot().getClusters().get(clusterName)));
            assertEquals("blue:success", f.remote.sync("success"));
            f.blue.failures = f.blue.calls.get() + 2;
            assertEquals("blue:retry", f.remote.call(new Request("retry")).get(2, TimeUnit.SECONDS));
            f.blue.keepStream = true;
            BlockingQueue<Object> events = new LinkedBlockingQueue<>();
            f.remote.watch("held", new StreamObserver<String>() {
                public void onNext(String value) {
                    events.add(value);
                }

                public void onError(Throwable error) {
                    events.add(error);
                }

                public void onCompleted() {
                    events.add("done");
                }
            });
            assertEquals("held", events.poll(2, TimeUnit.SECONDS));
            Throwable overflow = assertThrows(
                            ExecutionException.class,
                            () -> f.remote.call(new Request("overflow")).get(1, TimeUnit.SECONDS))
                    .getCause();
            assertInstanceOf(XdsCircuitBreaker.DropException.class, overflow);
            MetadataEntryOptions options =
                    new MetadataEntryOptions(f.directory.getConsumerUrl(), f.directory.getUrl(), endpoint.getTarget());
            PointToPointMetadataFetcher metadata = new PointToPointMetadataFetcher(f.application);
            assertThrows(XdsCircuitBreaker.DropException.class, () -> metadata.fetch(endpoint, endpoint, options));
            assertEquals(5, f.blue.calls.get());
            sink.interval(30_000_000);
            ClusterStats first = sink.report(stats -> stats.getTotalDroppedRequests() == 2);
            assertEquals(clusterName, first.getClusterName());
            assertEquals(0, first.getDroppedRequestsCount(), "Circuit drops are uncategorized");
            assertEquals(1, first.getUpstreamLocalityStatsCount());
            assertEquals(
                    endpoint.getLocality().getLocality(),
                    first.getUpstreamLocalityStats(0).getLocality());
            assertEquals(5, first.getUpstreamLocalityStats(0).getTotalIssuedRequests());
            assertEquals(2, first.getUpstreamLocalityStats(0).getTotalSuccessfulRequests());
            assertEquals(2, first.getUpstreamLocalityStats(0).getTotalErrorRequests());
            assertEquals(1, first.getUpstreamLocalityStats(0).getTotalRequestsInProgress());
            f.blue.streamObserver.onError(
                    TriRpcStatus.fromCode(TriRpcStatus.Code.PERMISSION_DENIED).asException());
            Object terminal = events.poll(2, TimeUnit.SECONDS);
            assertInstanceOf(Throwable.class, terminal);
            assertEquals(TriRpcStatus.Code.PERMISSION_DENIED, TriRpcStatus.getStatus((Throwable) terminal).code);
            ClusterStats ended = sink.report(stats -> stats.getUpstreamLocalityStatsCount() > 0
                    && stats.getUpstreamLocalityStats(0).getTotalErrorRequests() == 1);
            assertEquals(0, ended.getUpstreamLocalityStats(0).getTotalIssuedRequests());
            assertEquals(0, ended.getUpstreamLocalityStats(0).getTotalRequestsInProgress());
            assertEquals(0, ended.getTotalDroppedRequests());
            assertNotNull(metadata.fetch(endpoint, endpoint, options));
            ClusterStats read = sink.report(stats -> stats.getUpstreamLocalityStatsCount() > 0
                    && stats.getUpstreamLocalityStats(0).getTotalSuccessfulRequests() == 1);
            assertEquals(1, read.getUpstreamLocalityStats(0).getTotalIssuedRequests());
            assertEquals(0, read.getUpstreamLocalityStats(0).getTotalErrorRequests());
            assertEquals(0, read.getUpstreamLocalityStats(0).getTotalRequestsInProgress());
            assertEquals(5, f.blue.calls.get(), "Metadata is a separate service, not a business invocation");
            ClusterLoadAssignment.Policy policy = ClusterLoadAssignment.Policy.newBuilder()
                    .addDropOverloads(ClusterLoadAssignment.Policy.DropOverload.newBuilder()
                            .setCategory("server-throttle")
                            .setDropPercentage(FractionalPercent.newBuilder().setNumerator(100)))
                    .build();
            f.ads.clusterAssignments.put(
                    clusterName, f.ads.endpoints.toBuilder().setPolicy(policy).build());
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> policy.equals(
                    endpoint.getSnapshot().getEndpoints().get(clusterName).getPolicy()));
            Throwable edsDrop = assertThrows(
                            ExecutionException.class,
                            () -> f.remote.call(new Request("eds-drop")).get(1, TimeUnit.SECONDS))
                    .getCause();
            assertInstanceOf(XdsCircuitBreaker.DropException.class, edsDrop);
            ClusterStats dropReport = sink.report(stats -> stats.getDroppedRequestsCount() > 0);
            assertEquals(1, dropReport.getTotalDroppedRequests());
            assertEquals("server-throttle", dropReport.getDroppedRequests(0).getCategory());
            assertEquals(1, dropReport.getDroppedRequests(0).getDroppedCount());
            assertEquals(0, dropReport.getUpstreamLocalityStatsCount());
            assertEquals(5, f.blue.calls.get(), "EDS drop does not create another upstream attempt");
        }
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        while (!condition.getAsBoolean() && System.nanoTime() < end) Thread.sleep(5);
        assertTrue(condition.getAsBoolean());
    }

    private static final class Sink extends LoadReportingServiceGrpc.LoadReportingServiceImplBase {
        final BlockingQueue<LoadStatsRequest> initial = new LinkedBlockingQueue<>();
        final BlockingQueue<LoadStatsRequest> reports = new LinkedBlockingQueue<>();
        volatile io.grpc.stub.StreamObserver<LoadStatsResponse> response;

        @Override
        public io.grpc.stub.StreamObserver<LoadStatsRequest> streamLoadStats(
                io.grpc.stub.StreamObserver<LoadStatsResponse> observer) {
            response = observer;
            return new io.grpc.stub.StreamObserver<LoadStatsRequest>() {
                public void onNext(LoadStatsRequest value) {
                    if (value.getClusterStatsCount() == 0) {
                        initial.add(value);
                        interval(0);
                    } else reports.add(value);
                }

                public void onError(Throwable error) {}

                public void onCompleted() {
                    observer.onCompleted();
                }
            };
        }

        void interval(int nanos) {
            response.onNext(LoadStatsResponse.newBuilder()
                    .setSendAllClusters(true)
                    .setLoadReportingInterval(Duration.newBuilder().setNanos(nanos))
                    .build());
        }

        ClusterStats report(Predicate<ClusterStats> predicate) throws Exception {
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
            while (System.nanoTime() < end) {
                LoadStatsRequest value = reports.poll(100, TimeUnit.MILLISECONDS);
                if (value != null)
                    for (ClusterStats stats : value.getClusterStatsList()) if (predicate.test(stats)) return stats;
            }
            throw new AssertionError("Missing actual Triple load report");
        }
    }
}
