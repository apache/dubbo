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

import org.apache.dubbo.common.stream.ClientCallStreamObserver;
import org.apache.dubbo.common.stream.ClientResponseObserver;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistryDirectory;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.registry.XdsRetryTest.Business;
import org.apache.dubbo.xds.registry.XdsRetryTest.Fixture;
import org.apache.dubbo.xds.registry.XdsRetryTest.Request;
import org.apache.dubbo.xds.security.NativeTlsAdapter;
import org.apache.dubbo.xds.traffic.XdsLoadBalance;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import com.google.protobuf.UInt32Value;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.CircuitBreakers;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Address;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsLeastRequestTest {
    @Test
    void noRetryReferenceDestroyCancelsItsOwnUnaryWithoutClosingAnotherReference() throws Exception {
        for (boolean sync : new boolean[] {false, true}) {
            try (Fixture f = new Fixture()) {
                RouteConfiguration base = f.config(100, 6000);
                RouteConfiguration noRetry = base.toBuilder()
                        .setVirtualHosts(
                                0,
                                base.getVirtualHosts(0).toBuilder()
                                        .setRoutes(
                                                0,
                                                base.getVirtualHosts(0).getRoutes(0).toBuilder()
                                                        .setRoute(
                                                                base
                                                                        .getVirtualHosts(0)
                                                                        .getRoutes(0)
                                                                        .getRoute()
                                                                        .toBuilder()
                                                                        .clearRetryPolicy())))
                        .build();
                XdsEndpoint blue = blue(f);
                f.ads.routeOverride = noRetry;
                f.ads.send(XdsResourceType.LDS.getTypeUrl());
                await(() -> noRetry.equals(blue.getSnapshot().getRoutes().get("routes")));
                policy(f, least(blue.getCluster(), 2));
                ServiceDiscoveryRegistryDirectory<Business> directory =
                        new ServiceDiscoveryRegistryDirectory<>(Business.class, f.directory.getUrl());
                directory.setRegistry(f.registry);
                directory.setProtocol(f.protocol);
                directory.buildRouterChain(f.directory.getConsumerUrl());
                directory.subscribe(f.directory.getConsumerUrl());
                await(() -> directory.getAllInvokers().size() == 2
                        && directory.getAllInvokers().stream().allMatch(Invoker::isAvailable));
                Invoker<Business> reference = f.consumerCluster().join(directory, true);
                ExecutorService caller = Executors.newSingleThreadExecutor();
                CompletableFuture<String> firstResponse = new CompletableFuture<>();
                CompletableFuture<String> otherResponse = new CompletableFuture<>();
                try {
                    Invoker<?> primaryEndpoint = f.directory.getAllInvokers().stream()
                            .filter(invoker -> invoker.getUrl().getPort() == blue.getPort())
                            .findFirst()
                            .get();
                    Invoker<?> secondaryEndpoint = directory.getAllInvokers().stream()
                            .filter(invoker -> invoker.getUrl().getPort() == blue.getPort())
                            .findFirst()
                            .get();
                    assertSame(
                            findField(primaryEndpoint, AbstractConnectionClient.class),
                            findField(secondaryEndpoint, AbstractConnectionClient.class),
                            "The two References must actually share the transport client");
                    Business other = f.application
                            .getExtensionLoader(ProxyFactory.class)
                            .getAdaptiveExtension()
                            .getProxy(reference);
                    f.blue.responses.put("other", otherResponse);
                    RpcContext.getClientAttachment().setAttachment("timeout", 6000);
                    CompletableFuture<String> secondary = other.call(new Request("other"));
                    RpcContext.removeClientAttachment();
                    XdsLoadBalance primaryLb = loadBalance(f.consumer);
                    XdsLoadBalance secondaryLb = loadBalance(reference);
                    await(() -> secondaryLb.active(blue) == 1 && f.blue.calls.get() == 1);
                    f.blue.responses.put("owned", firstResponse);
                    if (sync) f.blue.delay = 1500;
                    Future<String> owned;
                    if (sync)
                        owned = caller.submit(() -> {
                            RpcContext.getClientAttachment().setAttachment("timeout", 6000);
                            try {
                                return f.remote.sync("owned");
                            } finally {
                                RpcContext.removeContext();
                            }
                        });
                    else {
                        RpcContext.getClientAttachment().setAttachment("timeout", 6000);
                        owned = f.remote.call(new Request("owned"));
                        RpcContext.removeClientAttachment();
                    }
                    await(() -> primaryLb.active(blue) == 1 && f.blue.calls.get() == 2);
                    f.consumer.destroy();
                    Exception stopped = assertThrows(Exception.class, () -> owned.get(600, TimeUnit.MILLISECONDS));
                    assertFalse(
                            stopped instanceof TimeoutException,
                            "Reference destruction must cancel its owned call promptly");
                    if (!(stopped instanceof CancellationException)) {
                        Throwable cause = stopped instanceof ExecutionException ? stopped.getCause() : stopped;
                        assertEquals(TriRpcStatus.Code.CANCELLED, TriRpcStatus.getStatus(cause).code);
                    }
                    await(() -> primaryLb.active(blue) == 0);
                    assertFalse(secondary.isDone(), "A different Reference owns the other call on this connection");
                    assertEquals(1, secondaryLb.active(blue));
                    providerAction(() -> otherResponse.complete("blue:other"));
                    assertEquals("blue:other", secondary.get(1, TimeUnit.SECONDS));
                    await(() -> secondaryLb.active(blue) == 0);
                } finally {
                    providerAction(() -> firstResponse.complete("blue:owned"));
                    providerAction(() -> otherResponse.complete("blue:other"));
                    reference.destroy();
                    caller.shutdownNow();
                    assertTrue(caller.awaitTermination(2, TimeUnit.SECONDS));
                }
            }
        }
    }

    @Test
    void livePolicySwitchKeepsAnOutstandingUnaryRequestAndCompletionRestoresEqualSelection() throws Exception {
        try (Fixture f = new Fixture()) {
            f.routes(100, 6000);
            XdsEndpoint blue = blue(f);
            XdsLoadBalance lb = loadBalance(f.consumer);
            CompletableFuture<String> response = new CompletableFuture<>();
            f.blue.responses.put("held", response);
            RpcContext.getClientAttachment().setAttachment("timeout", 6000);
            CompletableFuture<String> pending = f.remote.call(new Request("held"));
            RpcContext.removeClientAttachment();
            assertTrue(f.blue.started.await(1, TimeUnit.SECONDS));
            assertEquals(1, lb.active(blue));
            endpoints(f, blue.getPort(), f.replacementPort);
            policy(f, least(blue.getCluster(), 10));
            policy(f, blue.getCluster());
            assertEquals(1, lb.active(blue), "Policy changes cannot reset outstanding RR calls");
            policy(f, least(blue.getCluster(), 10));
            int replacement = calls(f, 40);
            assertTrue(replacement >= 34, "Busy original endpoint was selected too often: " + replacement);
            assertEquals(1, lb.active(blue));
            providerAction(() -> response.complete("blue:held"));
            assertEquals("blue:held", pending.get(1, TimeUnit.SECONDS));
            await(() -> lb.active(blue) == 0);
            int idleReplacement = calls(f, 40);
            assertTrue(
                    idleReplacement >= 5 && idleReplacement <= 35,
                    "Completion must restore equal idle sampling: " + idleReplacement);
        }
    }

    @Test
    void initialStreamResultsKeepTheirLoadUntilCompletionCancellationOrReferenceDestroy() throws Exception {
        for (String stop : new String[] {"complete", "cancel", "destroy"}) {
            try (Fixture f = new Fixture()) {
                f.routes(100, 6000);
                XdsEndpoint blue = blue(f);
                XdsLoadBalance lb = loadBalance(f.consumer);
                f.blue.keepStream = true;
                Observer observer = new Observer();
                RpcContext.getClientAttachment().setAttachment("timeout", 6000);
                f.remote.watch("held-stream", observer);
                RpcContext.removeClientAttachment();
                assertEquals("held-stream", observer.events.poll(1, TimeUnit.SECONDS));
                assertEquals(1, lb.active(blue), "The initial stream Result cannot release endpoint load");
                endpoints(f, blue.getPort(), f.replacementPort);
                policy(f, least(blue.getCluster(), 10));
                assertTrue(calls(f, 25) >= 20);
                assertEquals(1, lb.active(blue));
                if (stop.equals("complete")) providerAction(() -> f.blue.streamObserver.onCompleted());
                else if (stop.equals("cancel")) observer.writer.onError(TriRpcStatus.CANCELLED.asException());
                else f.consumer.destroy();
                assertNotNull(observer.events.poll(1, TimeUnit.SECONDS));
                await(() -> lb.active(blue) == 0);
                if (!stop.equals("complete")) assertTrue(f.blue.cancelled.await(1, TimeUnit.SECONDS));
                if (!stop.equals("destroy")) {
                    int idleReplacement = calls(f, 40);
                    assertTrue(
                            idleReplacement >= 5 && idleReplacement <= 35,
                            "Terminal stream leaked endpoint load: " + idleReplacement);
                }
            }
        }
    }

    @Test
    void retryBackoffFutureCancellationDeadlineAndAdmissionDropReleaseTheirAttempts() throws Exception {
        try (Fixture f = new Fixture()) {
            XdsEndpoint blue = blue(f);
            XdsLoadBalance lb = loadBalance(f.consumer);
            Cluster least = least(blue.getCluster(), 2);
            policy(f, least);
            f.blue.failures = 1;
            f.blue.pushback = "500";
            CompletableFuture<String> retry = f.remote.call(new Request("retry"));
            assertTrue(f.blue.started.await(1, TimeUnit.SECONDS));
            await(() -> lb.active(blue) == 0);
            assertFalse(retry.isDone(), "Backoff must occur after releasing the attempt");
            assertEquals("blue:retry", retry.get(2, TimeUnit.SECONDS));
            assertEquals(2, f.blue.calls.get());
            assertEquals(0, lb.active(blue));
            for (String stop : new String[] {"cancel", "deadline"}) {
                f.blue.reset();
                f.blue.responses.put(stop, new CompletableFuture<>());
                f.routes(100, stop.equals("deadline") ? 180 : 2000);
                CompletableFuture<String> pending = f.remote.call(new Request(stop));
                assertTrue(f.blue.started.await(1, TimeUnit.SECONDS));
                assertEquals(1, lb.active(blue));
                if (stop.equals("cancel")) assertTrue(pending.cancel(true));
                else status(TriRpcStatus.Code.DEADLINE_EXCEEDED, pending);
                await(() -> lb.active(blue) == 0);
                assertTrue(f.blue.cancelled.await(1, TimeUnit.SECONDS));
                assertEquals(1, f.blue.calls.get());
            }
            f.routes(100, 2000);
            policy(
                    f,
                    least.toBuilder()
                            .setCircuitBreakers(CircuitBreakers.newBuilder()
                                    .addThresholds(CircuitBreakers.Thresholds.newBuilder()
                                            .setMaxRequests(UInt32Value.of(0))))
                            .build());
            int previous = f.blue.calls.get();
            status(TriRpcStatus.Code.UNAVAILABLE, f.remote.call(new Request("local-drop")));
            assertEquals(previous, f.blue.calls.get());
            assertEquals(0, lb.active(blue));
        }
    }

    @Test
    void removingAnEndpointStopsNewSelectionWithoutLosingAnOutstandingCounter() throws Exception {
        try (Fixture f = new Fixture()) {
            f.routes(100, 6000);
            XdsEndpoint blue = blue(f);
            XdsLoadBalance lb = loadBalance(f.consumer);
            policy(f, least(blue.getCluster(), 10));
            CompletableFuture<String> response = new CompletableFuture<>();
            f.blue.responses.put("held", response);
            RpcContext.getClientAttachment().setAttachment("timeout", 6000);
            CompletableFuture<String> pending = f.remote.call(new Request("held"));
            RpcContext.removeClientAttachment();
            assertTrue(f.blue.started.await(1, TimeUnit.SECONDS));
            endpoints(f, f.replacementPort);
            assertEquals(1, lb.active(blue));
            for (int i = 0; i < 10; i++) assertEquals("replacement:removed", f.remote.sync("removed"));
            assertEquals(1, f.blue.calls.get());
            assertNull(
                    RpcContext.getServerAttachment().getObjectAttachment("timeout-countdown"),
                    "Direct client calls cannot leave an inherited server budget on the caller thread");
            providerAction(() -> response.complete("blue:held"));
            assertEquals("blue:held", pending.get(1, TimeUnit.SECONDS));
            await(() -> lb.active(blue) == 0);
            assertNull(RpcContext.getServerAttachment().getObjectAttachment("timeout-countdown"));
        }
    }

    @Test
    void malformedChoiceCountAndUnsupportedPolicyNackRetainTheWorkingLeastRequestPolicy() throws Exception {
        try (Fixture f = new Fixture()) {
            XdsEndpoint blue = blue(f);
            Cluster least = least(blue.getCluster(), 2);
            policy(f, least);
            for (Cluster bad : new Cluster[] {
                least(least, 0),
                least(least, 1),
                least.toBuilder().setLbPolicy(Cluster.LbPolicy.MAGLEV).build()
            }) {
                int rejected = f.ads.rejected.get();
                f.ads.clusterOverrides.put(least.getName(), bad);
                f.ads.send(XdsResourceType.CDS.getTypeUrl());
                await(() -> f.ads.rejected.get() > rejected);
                assertEquals(least, blue.getSnapshot().getClusters().get(least.getName()));
                assertEquals("blue:last-good", f.remote.sync("last-good"));
            }
        }
    }

    private static Cluster least(Cluster cluster, int choices) {
        return cluster.toBuilder()
                .setLbPolicy(Cluster.LbPolicy.LEAST_REQUEST)
                .setLeastRequestLbConfig(
                        Cluster.LeastRequestLbConfig.newBuilder().setChoiceCount(UInt32Value.of(choices)))
                .build();
    }

    static XdsEndpoint blue(Fixture f) {
        return f.directory.getAllInvokers().stream()
                .map(invoker -> NativeTlsAdapter.endpoint(invoker.getUrl()))
                .filter(endpoint -> endpoint.getCluster().getName().equals(f.ads.clusters.get(0)))
                .findFirst()
                .get();
    }

    static void policy(Fixture f, Cluster cluster) throws Exception {
        XdsEndpoint observer =
                NativeTlsAdapter.endpoint(f.directory.getAllInvokers().get(0).getUrl());
        f.ads.clusterOverrides.put(cluster.getName(), cluster);
        f.ads.send(XdsResourceType.CDS.getTypeUrl());
        await(() -> cluster.equals(observer.getSnapshot().getClusters().get(cluster.getName())));
    }

    static void endpoints(Fixture f, int... ports) throws Exception {
        String cluster = f.ads.clusters.get(0);
        LocalityLbEndpoints previous = f.ads.endpoints.getEndpoints(0);
        LocalityLbEndpoints.Builder locality = previous.toBuilder().clearLbEndpoints();
        for (int port : ports) {
            LbEndpoint first = previous.getLbEndpoints(0);
            SocketAddress socket = first.getEndpoint().getAddress().getSocketAddress();
            locality.addLbEndpoints(first.toBuilder()
                    .setEndpoint(first.getEndpoint().toBuilder()
                            .setAddress(Address.newBuilder()
                                    .setSocketAddress(socket.toBuilder().setPortValue(port)))));
        }
        f.ads.clusterPorts.remove(cluster);
        f.ads.clusterAssignments.put(
                cluster,
                ClusterLoadAssignment.newBuilder()
                        .setClusterName(cluster)
                        .addEndpoints(locality)
                        .build());
        f.ads.send(XdsResourceType.EDS.getTypeUrl());
        Set<Integer> expected = Arrays.stream(ports).boxed().collect(Collectors.toSet());
        await(() -> f.directory.getAllInvokers().stream()
                .filter(invoker -> NativeTlsAdapter.endpoint(invoker.getUrl())
                        .getCluster()
                        .getName()
                        .equals(cluster))
                .map(invoker -> invoker.getUrl().getPort())
                .collect(Collectors.toSet())
                .equals(expected));
        await(() -> f.directory.getAllInvokers().stream().allMatch(Invoker::isAvailable));
    }

    private static int calls(Fixture f, int count) throws Exception {
        int replacement = 0;
        for (int i = 0; i < count; i++) {
            if (f.remote.call(new Request("sample")).get(1, TimeUnit.SECONDS).equals("replacement:sample"))
                replacement++;
        }
        return replacement;
    }

    private static void providerAction(Runnable action) throws Exception {
        // Completing a provider Future restores provider RpcContext on its completing thread.
        // Keep it off the client/test thread, just as normal provider execution would.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(() -> {
                        try {
                            action.run();
                        } finally {
                            RpcContext.removeContext();
                        }
                    })
                    .get(2, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    // Inspect the actual LB beneath the normal filter/interceptor pipeline without adding test APIs.
    static XdsLoadBalance loadBalance(Invoker<?> root) throws Exception {
        return findField(root, XdsLoadBalance.class);
    }

    private static <V> V findField(Invoker<?> root, Class<V> expected) throws Exception {
        Queue<Invoker<?>> remaining = new ArrayDeque<>();
        Set<Invoker<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        remaining.add(root);
        while (!remaining.isEmpty()) {
            Invoker<?> current = remaining.remove();
            if (!visited.add(current)) continue;
            for (Class<?> type = current.getClass(); type != Object.class; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    if (expected.isAssignableFrom(field.getType()) || Invoker.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object value = field.get(current);
                        if (expected.isInstance(value)) return expected.cast(value);
                        if (value instanceof Invoker) remaining.add((Invoker<?>) value);
                    }
                }
            }
        }
        throw new AssertionError("Native xDS field not found: " + expected.getSimpleName());
    }

    private static void status(TriRpcStatus.Code code, CompletableFuture<?> result) {
        Throwable error = assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS))
                .getCause();
        assertEquals(code, TriRpcStatus.getStatus(error).code);
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > until) fail("Least-request condition did not become true");
            Thread.sleep(10);
        }
    }

    private static final class Observer implements ClientResponseObserver<String, String> {
        final BlockingQueue<Object> events = new LinkedBlockingQueue<>();
        volatile ClientCallStreamObserver<String> writer;

        public void beforeStart(ClientCallStreamObserver<String> writer) {
            this.writer = writer;
        }

        public void onNext(String value) {
            events.add(value);
        }

        public void onError(Throwable error) {
            events.add(error);
        }

        public void onCompleted() {
            events.add("completed");
        }
    }
}
