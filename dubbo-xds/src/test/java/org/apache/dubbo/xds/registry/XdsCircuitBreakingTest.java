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
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistryDirectory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.registry.XdsRetryTest.Business;
import org.apache.dubbo.xds.registry.XdsRetryTest.Fixture;
import org.apache.dubbo.xds.registry.XdsRetryTest.Request;
import org.apache.dubbo.xds.security.NativeTlsAdapter;
import org.apache.dubbo.xds.traffic.XdsCircuitBreaker;
import org.apache.dubbo.xds.traffic.XdsLoadStats;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import com.google.protobuf.UInt32Value;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.CircuitBreakers;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.v3.FractionalPercent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsCircuitBreakingTest {
    @Test
    void actualUnaryCallsDropWithoutRetryAndOnlineLoweringDoesNotResetOutstandingCalls() throws Exception {
        try (Fixture f = new Fixture()) {
            maximum(f, 2);
            f.blue.delay = 1000;
            CompletableFuture<String> first = f.remote.call(new Request("first"));
            CompletableFuture<String> second = f.remote.call(new Request("second"));
            await(() -> f.blue.calls.get() == 2);
            dropped(f.remote.call(new Request("drop")));
            assertEquals(1, drops(f));
            maximum(f, 1);
            assertEquals(2, active(f));
            dropped(f.remote.call(new Request("still-drop")));
            assertEquals(2, drops(f), "Local circuit drops cannot be retried");
            assertEquals("blue:first", first.get(2, TimeUnit.SECONDS));
            assertEquals("blue:second", second.get(2, TimeUnit.SECONDS));
            await(() -> active(f) == 0);
            f.blue.delay = 0;
            assertEquals("blue:restored", f.remote.call(new Request("restored")).get(1, TimeUnit.SECONDS));
            assertEquals(3, f.blue.calls.get());
        }
    }

    @Test
    void referencesShareTheSameCounterAndDestroyingOneDoesNotDestroyTheOther() throws Exception {
        try (Fixture f = new Fixture()) {
            maximum(f, 1);
            ServiceDiscoveryRegistryDirectory<Business> directory =
                    new ServiceDiscoveryRegistryDirectory<>(Business.class, f.directory.getUrl());
            directory.setRegistry(f.registry);
            directory.setProtocol(f.protocol);
            directory.buildRouterChain(f.directory.getConsumerUrl());
            directory.subscribe(f.directory.getConsumerUrl());
            await(() -> directory.getAllInvokers().size() == 2);
            Invoker<Business> reference = f.consumerCluster().join(directory, true);
            try {
                Business other = f.application
                        .getExtensionLoader(ProxyFactory.class)
                        .getAdaptiveExtension()
                        .getProxy(reference);
                f.blue.delay = 500;
                CompletableFuture<String> first = f.remote.call(new Request("owned"));
                await(() -> f.blue.calls.get() == 1);
                dropped(other.call(new Request("shared-drop")));
                assertEquals(1, drops(f));
                assertEquals("blue:owned", first.get(1, TimeUnit.SECONDS));
                f.blue.delay = 0;
                assertEquals("blue:other", other.call(new Request("other")).get(1, TimeUnit.SECONDS));
            } finally {
                reference.destroy();
            }
            assertEquals("blue:original", f.remote.call(new Request("original")).get(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void streamingInitialResultsDoNotReleaseTheLimitAndCompletionOrDestroyDoes() throws Exception {
        for (boolean destroy : new boolean[] {false, true}) {
            try (Fixture f = new Fixture()) {
                maximum(f, 1);
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
                        events.add("completed");
                    }
                });
                assertEquals("held", events.poll(1, TimeUnit.SECONDS));
                assertEquals(1, active(f));
                dropped(f.remote.call(new Request("stream-drop")));
                if (destroy) {
                    f.consumer.destroy();
                    Object terminal = events.poll(1, TimeUnit.SECONDS);
                    assertInstanceOf(Throwable.class, terminal);
                    assertEquals(TriRpcStatus.Code.CANCELLED, TriRpcStatus.getStatus((Throwable) terminal).code);
                } else {
                    f.blue.streamObserver.onCompleted();
                    assertEquals("completed", events.poll(1, TimeUnit.SECONDS));
                    assertEquals(
                            "blue:after-stream",
                            f.remote.call(new Request("after-stream")).get(1, TimeUnit.SECONDS));
                }
                await(() -> active(f) == 0);
            }
        }
    }

    @Test
    void nativePointToPointMetadataCallsUseTheSameAdmissionAsBusinessCalls() throws Exception {
        try (Fixture f = new Fixture()) {
            maximum(f, 1);
            XdsEndpoint endpoint = endpoint(f);
            MetadataEntryOptions options =
                    new MetadataEntryOptions(f.directory.getConsumerUrl(), f.directory.getUrl(), endpoint.getTarget());
            PointToPointMetadataFetcher fetcher = new PointToPointMetadataFetcher(f.application);
            f.blue.delay = 500;
            CompletableFuture<String> first = f.remote.call(new Request("business"));
            await(() -> f.blue.calls.get() == 1);
            Throwable failure = assertThrows(Exception.class, () -> fetcher.fetch(endpoint, endpoint, options));
            assertEquals(TriRpcStatus.Code.UNAVAILABLE, TriRpcStatus.getStatus(failure).code);
            assertEquals(1, drops(f));
            assertEquals("blue:business", first.get(1, TimeUnit.SECONDS));
            assertNotNull(fetcher.fetch(endpoint, endpoint, options));
            assertEquals(0, active(f));
        }
    }

    @Test
    void zeroAdmissionLimitIsCumulativeAndDoesNotCountAsDispatchedOrRetry() throws Exception {
        try (Fixture f = new Fixture()) {
            maximum(f, 0);
            XdsLoadStats stats = endpoint(f).getSecurity().getLoadStats();
            XdsLoadStats.Snapshot before = accounting(f, stats);
            int calls = f.blue.calls.get();
            dropped(f.remote.call(new Request("zero-limit")));
            dropped(f.remote.call(new Request("zero-limit-again")));
            XdsLoadStats.Snapshot after = accounting(f, stats);
            assertEquals(2, after.getCircuitDrops() - before.getCircuitDrops());
            assertEquals(before.getIssued(), after.getIssued());
            assertEquals(0, after.getInProgress());
            assertEquals(calls, f.blue.calls.get());
            assertEquals(0, active(f));
        }
    }

    @Test
    void realStreamingCountsTerminalFailureAndCompletionRatherThanInitialResult() throws Exception {
        for (boolean error : new boolean[] {false, true}) {
            try (Fixture f = new Fixture()) {
                f.blue.keepStream = true;
                XdsLoadStats stats = endpoint(f).getSecurity().getLoadStats();
                XdsLoadStats.Snapshot before = accounting(f, stats);
                BlockingQueue<Object> events = new LinkedBlockingQueue<>();
                f.remote.watch("stats", new StreamObserver<String>() {
                    public void onNext(String value) {
                        events.add(value);
                    }

                    public void onError(Throwable failure) {
                        events.add(failure);
                    }

                    public void onCompleted() {
                        events.add("done");
                    }
                });
                assertEquals("stats", events.poll(2, TimeUnit.SECONDS));
                XdsLoadStats.Snapshot running = accounting(f, stats);
                assertEquals(1, running.getIssued() - before.getIssued());
                assertEquals(1, running.getInProgress());
                assertEquals(before.getSuccessful(), running.getSuccessful());
                assertEquals(before.getErrors(), running.getErrors());
                if (error)
                    f.blue.streamObserver.onError(TriRpcStatus.fromCode(TriRpcStatus.Code.PERMISSION_DENIED)
                            .asException());
                else f.blue.streamObserver.onCompleted();
                Object terminal = events.poll(2, TimeUnit.SECONDS);
                if (error) assertInstanceOf(Throwable.class, terminal);
                else assertEquals("done", terminal);
                await(() -> accounting(f, stats).getInProgress() == 0);
                XdsLoadStats.Snapshot completed = accounting(f, stats);
                assertEquals(error ? 0 : 1, completed.getSuccessful() - before.getSuccessful());
                assertEquals(error ? 1 : 0, completed.getErrors() - before.getErrors());
            }
        }
    }

    @Test
    void actualRetryAttemptsRecordTwoErrorsAndOneSuccess() throws Exception {
        try (Fixture f = new Fixture()) {
            XdsLoadStats stats = endpoint(f).getSecurity().getLoadStats();
            XdsLoadStats.Snapshot before = accounting(f, stats);
            f.blue.failures = 2;
            assertEquals("blue:retried", f.remote.call(new Request("retried")).get(2, TimeUnit.SECONDS));
            XdsLoadStats.Snapshot after = accounting(f, stats);
            assertEquals(3, after.getIssued() - before.getIssued());
            assertEquals(2, after.getErrors() - before.getErrors());
            assertEquals(1, after.getSuccessful() - before.getSuccessful());
            assertEquals(0, after.getInProgress());
            assertEquals(3, f.blue.calls.get());
        }
    }

    @Test
    void edsDropDoesNotDispatchOrRetryAndInvalidUpdateKeepsLastGoodPolicy() throws Exception {
        try (Fixture f = new Fixture()) {
            String cluster = f.ads.clusters.get(0);
            XdsEndpoint endpoint = endpoint(f);
            XdsLoadStats stats = endpoint.getSecurity().getLoadStats();
            XdsLoadStats.Snapshot before = accounting(f, stats);
            setDrop(f, 100, "overload");
            dropped(f.remote.call(new Request("eds-dropped")));
            assertEquals(0, f.blue.calls.get());
            XdsLoadStats.Snapshot dropped = accounting(f, stats);
            assertEquals(before.getIssued(), dropped.getIssued());
            assertEquals(1, dropped.getEdsDrops() - before.getEdsDrops());
            assertEquals(before.getCircuitDrops(), dropped.getCircuitDrops());
            f.ads.clusterAssignments.put(
                    cluster,
                    f.ads.clusterAssignments.get(cluster).toBuilder()
                            .setPolicy(policy(100, ""))
                            .build());
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> f.ads.lastRejection != null);
            dropped(f.remote.call(new Request("last-good-drop")));
            assertEquals(0, f.blue.calls.get());
            setDrop(f, 0, "overload");
            assertEquals("blue:restored", f.remote.call(new Request("restored")).get(2, TimeUnit.SECONDS));
            assertEquals(1, f.blue.calls.get());
        }
    }

    private static void setDrop(Fixture f, int percentage, String category) throws Exception {
        String cluster = f.ads.clusters.get(0);
        ClusterLoadAssignment.Policy policy = policy(percentage, category);
        f.ads.clusterAssignments.put(
                cluster, f.ads.endpoints.toBuilder().setPolicy(policy).build());
        f.ads.send(XdsResourceType.EDS.getTypeUrl());
        await(() -> endpoint(f)
                .getSnapshot()
                .getEndpoints()
                .get(cluster)
                .getPolicy()
                .equals(policy));
    }

    private static ClusterLoadAssignment.Policy policy(int numerator, String category) {
        return ClusterLoadAssignment.Policy.newBuilder()
                .addDropOverloads(ClusterLoadAssignment.Policy.DropOverload.newBuilder()
                        .setCategory(category)
                        .setDropPercentage(FractionalPercent.newBuilder().setNumerator(numerator)))
                .build();
    }

    private static XdsLoadStats.Snapshot accounting(Fixture f, XdsLoadStats stats) {
        return stats.snapshot().stream()
                .filter(value -> value.getCluster().equals(f.ads.clusters.get(0)))
                .findFirst()
                .orElseThrow(AssertionError::new);
    }

    private static void dropped(CompletableFuture<String> call) {
        Throwable error = assertThrows(ExecutionException.class, () -> call.get(1, TimeUnit.SECONDS))
                .getCause();
        assertInstanceOf(XdsCircuitBreaker.DropException.class, error);
        assertEquals(TriRpcStatus.Code.UNAVAILABLE, TriRpcStatus.getStatus(error).code);
    }

    private static XdsEndpoint endpoint(Fixture f) {
        return f.directory.getAllInvokers().stream()
                .map(invoker -> NativeTlsAdapter.endpoint(invoker.getUrl()))
                .filter(value -> value.getCluster().getName().equals(f.ads.clusters.get(0)))
                .findFirst()
                .orElseThrow(AssertionError::new);
    }

    private static long active(Fixture f) {
        return XdsCircuitBreaker.active(fingerprint(f), policy(f));
    }

    private static long drops(Fixture f) {
        return XdsCircuitBreaker.dropped(fingerprint(f), policy(f));
    }

    private static String fingerprint(Fixture f) {
        return f.workloadFingerprint;
    }

    private static Cluster policy(Fixture f) {
        return f.ads.clusterOverrides.get(f.ads.clusters.get(0));
    }

    private static void maximum(Fixture f, int maximum) throws Exception {
        XdsEndpoint endpoint = endpoint(f);
        f.ads.clusterOverrides.put(
                endpoint.getCluster().getName(),
                endpoint.getCluster().toBuilder()
                        .setCircuitBreakers(CircuitBreakers.newBuilder()
                                .addThresholds(CircuitBreakers.Thresholds.newBuilder()
                                        .setMaxRequests(UInt32Value.of(maximum))))
                        .build());
        f.ads.send(XdsResourceType.CDS.getTypeUrl());
        await(() -> XdsCircuitBreaker.limit(endpoint.getSnapshot()
                        .getClusters()
                        .get(endpoint.getCluster().getName()))
                == maximum);
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > until) fail("Circuit-breaking condition did not become true");
            Thread.sleep(10);
        }
    }
}
