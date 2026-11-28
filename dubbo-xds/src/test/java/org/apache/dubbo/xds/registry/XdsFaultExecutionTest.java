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
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.registry.XdsRetryTest.Fixture;
import org.apache.dubbo.xds.registry.XdsRetryTest.Request;
import org.apache.dubbo.xds.security.NativeTlsAdapter;
import org.apache.dubbo.xds.traffic.XdsCircuitBreaker;
import org.apache.dubbo.xds.traffic.XdsFaultPolicy;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.UInt32Value;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Listener;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.WeightedCluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.common.fault.v3.FaultDelay;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.fault.v3.FaultAbort;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.fault.v3.HTTPFault;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.v3.FractionalPercent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XdsFaultExecutionTest {
    @Test
    void syncAndAsyncAbortStayLocalAndDoNotEnterRetry() throws Exception {
        try (Fixture f = new Fixture()) {
            install(f, abort(14), f.ads.routeOverride);
            status(TriRpcStatus.Code.UNAVAILABLE, f.remote.call(new Request("async")));
            Throwable error = assertThrows(Exception.class, () -> f.remote.sync("sync"));
            assertEquals(TriRpcStatus.Code.UNAVAILABLE, TriRpcStatus.getStatus(error).code);
            await(() -> XdsFaultPolicy.activeFaults() == 0);
            assertEquals(0, f.blue.calls.get());
            assertEquals(0, f.green.calls.get());
            assertEquals(0, admission(f));
        }
    }

    @Test
    void delayPrecedesAdmissionAndRetryRunsWithoutRepeatingTheFault() throws Exception {
        try (Fixture f = new Fixture()) {
            install(f, delay(600), f.ads.routeOverride);
            f.blue.failures = 1;
            CompletableFuture<String> call = f.remote.call(new Request("delayed"));
            await(() -> XdsFaultPolicy.activeFaults() == 1);
            assertEquals(0, f.blue.calls.get());
            assertEquals(0, admission(f));
            assertEquals("blue:delayed", call.get(2, TimeUnit.SECONDS));
            assertEquals(2, f.blue.calls.get());
            assertEquals(0, XdsFaultPolicy.activeFaults());
            assertEquals(0, admission(f));
            assertTrue(f.blue.timeouts.get(0) < 1700, "Fault delay consumes the existing call budget");
            f.blue.businessError = true;
            Throwable error = assertThrows(
                            ExecutionException.class,
                            () -> f.remote.call(new Request("business")).get(2, TimeUnit.SECONDS))
                    .getCause();
            assertInstanceOf(IllegalArgumentException.class, error);
        }
    }

    @Test
    void futureCancellationAndReferenceDestructionStopPendingDelays() throws Exception {
        for (boolean destroy : new boolean[] {false, true}) {
            try (Fixture f = new Fixture()) {
                install(f, delay(900), f.ads.routeOverride);
                XdsEndpoint selected = endpoint(f);
                CompletableFuture<String> call = f.remote.call(new Request("cancelled"));
                await(() -> XdsFaultPolicy.activeFaults() == 1);
                if (destroy) f.consumer.destroy();
                else assertTrue(call.cancel(false));
                await(() -> XdsFaultPolicy.activeFaults() == 0 && call.isDone());
                assertFalse(f.blue.started.await(1000, TimeUnit.MILLISECONDS));
                assertEquals(0, XdsCircuitBreaker.active(f.workloadFingerprint, selected.getCluster()));
            }
        }
    }

    @Test
    void totalDeadlineExpiresDuringDelayBeforeUnaryOrStreamTraffic() throws Exception {
        try (Fixture f = new Fixture()) {
            install(f, delay(1200), f.config(100, 250));
            status(TriRpcStatus.Code.DEADLINE_EXCEEDED, f.remote.call(new Request("expired")));
            Recording stream = new Recording();
            f.remote.watch("expired", stream);
            assertTrue(stream.done.await(1, TimeUnit.SECONDS));
            assertEquals(TriRpcStatus.Code.DEADLINE_EXCEEDED, TriRpcStatus.getStatus(stream.error).code);
            assertEquals(1, stream.before.get());
            assertEquals(1, stream.terminals.get());
            assertTrue(stream.messages.isEmpty());
            await(() -> XdsFaultPolicy.activeFaults() == 0);
            assertEquals(0, f.blue.calls.get());
            assertEquals(0, admission(f));
        }
    }

    @Test
    void delayedCallPinsItsClusterButUsesCurrentEndpoints() throws Exception {
        try (Fixture f = new Fixture()) {
            HTTPFault fault = delay(800);
            install(f, fault, metadataBypass(f.ads.routeOverride));
            CompletableFuture<String> call = f.remote.call(new Request("pinned"));
            await(() -> XdsFaultPolicy.activeFaults() == 1);
            f.ads.clusterPorts.put(f.ads.clusters.get(0), f.replacementPort);
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            install(f, fault, metadataBypass(f.config(0, 2000)));
            assertEquals("replacement:pinned", call.get(2, TimeUnit.SECONDS));
            assertEquals(0, f.blue.calls.get());
            assertEquals(0, f.green.calls.get());
            assertEquals("green:fresh", f.remote.call(new Request("fresh")).get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void providersWithdrawnDuringFaultDelayFailUnavailableWithoutDispatch() throws Exception {
        try (Fixture f = new Fixture()) {
            install(f, delay(800), metadataBypass(f.ads.routeOverride));
            XdsEndpoint selected = endpoint(f);
            CompletableFuture<String> call = f.remote.call(new Request("withdrawn-during-delay"));
            await(() -> XdsFaultPolicy.activeFaults() == 1);
            f.ads.emptyClusters.addAll(f.ads.clusters);
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> f.directory.getAllInvokers().isEmpty());
            status(TriRpcStatus.Code.UNAVAILABLE, call);
            await(() -> XdsFaultPolicy.activeFaults() == 0);
            assertEquals(0, f.blue.calls.get());
            assertEquals(0, f.green.calls.get());
            assertEquals(0, XdsCircuitBreaker.active(f.workloadFingerprint, selected.getCluster()));
        }
    }

    @Test
    void streamAbortDeliversOneTerminalAndOkAbortRequiresNoResponseForServerStream() throws Exception {
        try (Fixture f = new Fixture()) {
            install(f, abort(14), f.ads.routeOverride);
            Recording failed = new Recording();
            f.remote.watch("abort", failed);
            assertTrue(failed.done.await(1, TimeUnit.SECONDS));
            assertEquals(TriRpcStatus.Code.UNAVAILABLE, TriRpcStatus.getStatus(failed.error).code);
            assertEquals(1, failed.before.get());
            assertEquals(1, failed.terminals.get());
            install(f, abort(0), f.ads.routeOverride);
            Recording ok = new Recording();
            f.remote.watch("empty-ok", ok);
            assertTrue(ok.done.await(1, TimeUnit.SECONDS));
            assertNull(ok.error);
            assertEquals(1, ok.terminals.get());
            status(TriRpcStatus.Code.INTERNAL, f.remote.call(new Request("missing-response")));
            assertEquals(0, f.blue.calls.get());
            assertEquals(0, admission(f));
        }
    }

    @Test
    void bidiWriterQueuesMessagesAndHalfCloseWithoutSendingBeforeDelay() throws Exception {
        try (Fixture f = new Fixture()) {
            install(f, delay(450), f.ads.routeOverride);
            Recording response = new Recording();
            response.manual = true;
            StreamObserver<String> writer = f.remote.chat(response);
            assertEquals(1, response.before.get());
            assertSame(response.writer, writer);
            assertFalse(response.writer.isReady());
            writer.onNext("one");
            writer.onNext("two");
            writer.onCompleted();
            await(() -> XdsFaultPolicy.activeFaults() == 1);
            assertEquals(0, f.blue.calls.get());
            assertEquals(0, admission(f));
            assertTrue(response.done.await(2, TimeUnit.SECONDS));
            assertNull(response.error);
            assertEquals(Arrays.asList("blue:one", "blue:two"), response.messages);
            assertEquals(Arrays.asList("chat", "one", "two"), f.blue.values);
            assertEquals(1, response.before.get());
            assertEquals(1, response.terminals.get());
            await(() -> admission(f) == 0 && XdsFaultPolicy.activeFaults() == 0);
        }
    }

    @Test
    void streamWriterCancellationAndOverflowNeverDispatchPendingMessages() throws Exception {
        for (boolean overflow : new boolean[] {false, true}) {
            try (Fixture f = new Fixture()) {
                install(f, delay(800), f.ads.routeOverride);
                Recording response = new Recording();
                StreamObserver<String> writer = f.remote.chat(response);
                if (overflow) {
                    for (int i = 0; i < 129; i++) writer.onNext("queued-" + i);
                } else response.writer.onError(TriRpcStatus.CANCELLED.asException());
                assertTrue(response.done.await(1, TimeUnit.SECONDS));
                assertEquals(
                        overflow ? TriRpcStatus.Code.RESOURCE_EXHAUSTED : TriRpcStatus.Code.CANCELLED,
                        TriRpcStatus.getStatus(response.error).code);
                assertEquals(1, response.terminals.get());
                await(() -> XdsFaultPolicy.activeFaults() == 0);
                assertFalse(f.blue.started.await(900, TimeUnit.MILLISECONDS));
                assertEquals(0, admission(f));
            }
        }
    }

    @Test
    void activeFaultLimitIncludesBlockedTerminalDelivery() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch entered = new CountDownLatch(1);
        try (Fixture f = new Fixture()) {
            install(
                    f,
                    abort(14).toBuilder().setMaxActiveFaults(UInt32Value.of(1)).build(),
                    f.ads.routeOverride);
            Recording response = new Recording() {
                @Override
                public void onError(Throwable error) {
                    entered.countDown();
                    try {
                        assertTrue(release.await(3, TimeUnit.SECONDS));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(interrupted);
                    }
                    super.onError(error);
                }
            };
            f.remote.watch("blocked-terminal", response);
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            assertEquals(1, XdsFaultPolicy.activeFaults());
            assertEquals(
                    "blue:over-limit", f.remote.call(new Request("over-limit")).get(1, TimeUnit.SECONDS));
            release.countDown();
            assertTrue(response.done.await(1, TimeUnit.SECONDS));
            await(() -> XdsFaultPolicy.activeFaults() == 0);
            assertEquals(1, f.blue.calls.get());
        } finally {
            release.countDown();
        }
    }

    @Test
    void pointToPointMetadataUsesFaultBeforeAdmissionAndRetainsItsInstance() throws Exception {
        try (Fixture f = new Fixture()) {
            install(f, abort(14), f.ads.routeOverride);
            XdsEndpoint endpoint = endpoint(f);
            MetadataEntryOptions options =
                    new MetadataEntryOptions(f.directory.getConsumerUrl(), f.directory.getUrl(), endpoint.getTarget());
            PointToPointMetadataFetcher fetcher = new PointToPointMetadataFetcher(f.application);
            f.metadataServices.forEach(service -> clearInvocations(service));
            Throwable failure = assertThrows(Exception.class, () -> fetcher.fetch(endpoint, endpoint, options));
            assertEquals(TriRpcStatus.Code.UNAVAILABLE, TriRpcStatus.getStatus(failure).code);
            f.metadataServices.forEach(service -> verify(service, never()).getMetadataInfos());
            assertEquals(0, admission(f));
            install(f, delay(40), f.ads.routeOverride);
            assertNotNull(fetcher.fetch(endpoint(f), endpoint(f), options));
            verify(f.metadataServices.get(0), atLeastOnce()).getMetadataInfos();
            verify(f.metadataServices.get(1), never()).getMetadataInfos();
            assertEquals(0, XdsFaultPolicy.activeFaults());
        }
    }

    @Test
    void pointToPointMetadataDeadlineDoesNotDependOnAnActiveFaultFilter() throws Exception {
        try (Fixture f = new Fixture()) {
            install(f, HTTPFault.getDefaultInstance(), f.config(100, 160));
            XdsEndpoint endpoint = endpoint(f);
            MetadataEntryOptions options =
                    new MetadataEntryOptions(f.directory.getConsumerUrl(), f.directory.getUrl(), endpoint.getTarget());
            List<MetadataInfo> response = f.metadataServices.get(0).getMetadataInfos();
            when(f.metadataServices.get(0).getMetadataInfos()).thenAnswer(ignored -> {
                Thread.sleep(500);
                return response;
            });
            PointToPointMetadataFetcher fetcher = new PointToPointMetadataFetcher(f.application);
            Throwable failure = assertThrows(Exception.class, () -> fetcher.fetch(endpoint, endpoint, options));
            assertEquals(TriRpcStatus.Code.DEADLINE_EXCEEDED, TriRpcStatus.getStatus(failure).code);
            await(() -> admission(f) == 0);
            assertEquals(0, XdsFaultPolicy.activeFaults());
            assertEquals(0, f.blue.calls.get());
        }
    }

    @Test
    void routeAndWeightedClusterOverridesExecuteInTheirMostSpecificScope() throws Exception {
        try (Fixture f = new Fixture()) {
            RouteConfiguration baseline = f.ads.routeOverride;
            VirtualHost host = baseline.getVirtualHosts(0);
            Route route = host.getRoutes(0);
            RouteConfiguration routeOverride = baseline.toBuilder()
                    .setVirtualHosts(
                            0,
                            host.toBuilder()
                                    .putTypedPerFilterConfig("fault", Any.pack(abort(7)))
                                    .setRoutes(
                                            0, route.toBuilder().putTypedPerFilterConfig("fault", Any.pack(abort(14)))))
                    .build();
            install(f, abort(8), routeOverride);
            status(TriRpcStatus.Code.UNAVAILABLE, f.remote.call(new Request("route")));
            WeightedCluster selected = route.getRoute().getWeightedClusters().toBuilder()
                    .setClusters(
                            0,
                            route.getRoute().getWeightedClusters().getClusters(0).toBuilder()
                                    .putTypedPerFilterConfig("fault", Any.pack(HTTPFault.getDefaultInstance())))
                    .build();
            Route specific = routeOverride.getVirtualHosts(0).getRoutes(0).toBuilder()
                    .setRoute(route.getRoute().toBuilder().setWeightedClusters(selected))
                    .build();
            install(
                    f,
                    abort(8),
                    routeOverride.toBuilder()
                            .setVirtualHosts(
                                    0,
                                    routeOverride.getVirtualHosts(0).toBuilder().setRoutes(0, specific))
                            .build());
            assertEquals("blue:weighted", f.remote.call(new Request("weighted")).get(1, TimeUnit.SECONDS));
            assertEquals(1, f.blue.calls.get());
            assertEquals(0, f.green.calls.get());
        }
    }

    @Test
    void faultControlHeadersUseTheActualInvocationAndCanOnlyReduceTheConfiguredPercentage() throws Exception {
        try (Fixture f = new Fixture()) {
            HTTPFault header = HTTPFault.newBuilder()
                    .setAbort(FaultAbort.newBuilder()
                            .setHeaderAbort(FaultAbort.HeaderAbort.getDefaultInstance())
                            .setPercentage(percent()))
                    .build();
            install(f, header, f.ads.routeOverride);
            RpcContext.getClientAttachment().setAttachment("x-envoy-fault-abort-grpc-request", "7");
            status(TriRpcStatus.Code.PERMISSION_DENIED, f.remote.call(new Request("header")));
            RpcContext.getClientAttachment().setAttachment("x-envoy-fault-abort-grpc-request", "14");
            RpcContext.getClientAttachment().setAttachment("x-envoy-fault-abort-request-percentage", "0");
            assertEquals("blue:zero", f.remote.call(new Request("zero")).get(1, TimeUnit.SECONDS));
            RpcContext.removeClientAttachment();
            assertEquals("blue:missing", f.remote.call(new Request("missing")).get(1, TimeUnit.SECONDS));
            assertEquals(2, f.blue.calls.get());
        } finally {
            RpcContext.removeClientAttachment();
        }
    }

    @Test
    void acceptedStreamKeepsTheOriginalDeadlineAndCancellationAfterFaultDelay() throws Exception {
        for (boolean destroy : new boolean[] {false, true}) {
            try (Fixture f = new Fixture()) {
                f.blue.keepStream = true;
                install(f, delay(120), f.config(100, 650));
                Recording response = new Recording();
                f.remote.watch("active", response);
                await(() -> response.messages.size() == 1);
                assertEquals(1, f.blue.calls.get());
                assertEquals(0, XdsFaultPolicy.activeFaults());
                if (destroy) f.consumer.destroy();
                assertTrue(response.done.await(1, TimeUnit.SECONDS));
                assertEquals(
                        destroy ? TriRpcStatus.Code.CANCELLED : TriRpcStatus.Code.DEADLINE_EXCEEDED,
                        TriRpcStatus.getStatus(response.error).code);
                assertEquals(1, response.terminals.get());
                assertEquals(1, response.before.get());
                assertTrue(f.blue.cancelled.await(1, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    void blockingFutureCallbackDoesNotHoldTheFaultDeadlineTimer() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (Fixture f = new Fixture()) {
            install(f, delay(900), f.config(100, 140));
            CompletableFuture<String> first = f.remote.call(new Request("blocked-future"));
            first.whenComplete((value, error) -> {
                entered.countDown();
                try {
                    release.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            CompletableFuture<String> second = f.remote.call(new Request("independent-expiry"));
            Throwable failure = assertThrows(ExecutionException.class, () -> second.get(500, TimeUnit.MILLISECONDS))
                    .getCause();
            assertEquals(TriRpcStatus.Code.DEADLINE_EXCEEDED, TriRpcStatus.getStatus(failure).code);
            assertEquals(0, f.blue.calls.get());
        } finally {
            release.countDown();
        }
        await(() -> XdsFaultPolicy.activeFaults() == 0);
    }

    @Test
    void destructionDuringBeforeStartCancelsResourcesButDeliversAfterPreparation() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService caller = Executors.newSingleThreadExecutor();
        try (Fixture f = new Fixture()) {
            install(f, delay(900), f.ads.routeOverride);
            Recording response = new Recording() {
                @Override
                public void beforeStart(ClientCallStreamObserver<String> writer) {
                    entered.countDown();
                    try {
                        assertTrue(release.await(3, TimeUnit.SECONDS));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(interrupted);
                    }
                    super.beforeStart(writer);
                }
            };
            // Provider async replies also use the common pool; isolate the caller being controlled here.
            CompletableFuture<Void> start = CompletableFuture.runAsync(() -> f.remote.chat(response), caller);
            if (!entered.await(3, TimeUnit.SECONDS)) {
                if (start.isDone()) start.get(1, TimeUnit.SECONDS);
                fail("The owned caller did not reach beforeStart");
            }
            f.consumer.destroy();
            assertEquals(1, response.done.getCount());
            assertEquals(0, f.blue.calls.get());
            release.countDown();
            start.get(1, TimeUnit.SECONDS);
            assertTrue(response.done.await(1, TimeUnit.SECONDS));
            assertEquals(1, response.before.get());
            assertEquals(1, response.terminals.get());
            assertEquals(TriRpcStatus.Code.CANCELLED, TriRpcStatus.getStatus(response.error).code);
        } finally {
            release.countDown();
            caller.shutdownNow();
            assertTrue(caller.awaitTermination(3, TimeUnit.SECONDS));
        }
        await(() -> XdsFaultPolicy.activeFaults() == 0);
    }

    @Test
    void multipleFaultInstancesExecuteInHcmOrderAndStopAtTheFirstAbort() throws Exception {
        try (Fixture f = new Fixture()) {
            install(f, Arrays.asList(named("first", delay(80)), named("second", abort(7))), f.ads.routeOverride);
            status(TriRpcStatus.Code.PERMISSION_DENIED, f.remote.call(new Request("delay-then-abort")));
            install(f, Arrays.asList(named("first", abort(14)), named("second", delay(1200))), f.ads.routeOverride);
            Throwable error = assertThrows(
                            ExecutionException.class,
                            () -> f.remote.call(new Request("abort-first")).get(500, TimeUnit.MILLISECONDS))
                    .getCause();
            assertEquals(TriRpcStatus.Code.UNAVAILABLE, TriRpcStatus.getStatus(error).code);
            await(() -> XdsFaultPolicy.activeFaults() == 0);
            assertEquals(0, f.blue.calls.get());
            assertEquals(0, admission(f));
        }
    }

    static void install(Fixture f, HTTPFault fault, RouteConfiguration routes) throws Exception {
        install(f, Arrays.asList(named("fault", fault)), routes);
    }

    private static HttpFilter named(String name, HTTPFault fault) {
        return HttpFilter.newBuilder()
                .setName(name)
                .setTypedConfig(Any.pack(fault))
                .build();
    }

    private static void install(Fixture f, List<HttpFilter> faults, RouteConfiguration routes) throws Exception {
        XdsEndpoint target =
                NativeTlsAdapter.endpoint(f.directory.getAllInvokers().get(0).getUrl());
        Listener previous = target.getSnapshot().getListener();
        HttpConnectionManager hcm = previous.getApiListener().getApiListener().unpack(HttpConnectionManager.class);
        HttpFilter router = hcm.getHttpFilters(hcm.getHttpFiltersCount() - 1);
        HttpConnectionManager next = hcm.toBuilder()
                .setRouteConfig(routes)
                .clearHttpFilters()
                .addAllHttpFilters(faults)
                .addHttpFilters(router)
                .build();
        Listener listener = previous.toBuilder()
                .setApiListener(previous.getApiListener().toBuilder().setApiListener(Any.pack(next)))
                .build();
        f.ads.routeOverride = routes;
        f.ads.listenerOverrides.put(previous.getName(), listener);
        f.ads.send(XdsResourceType.LDS.getTypeUrl());
        await(() -> target.getSnapshot().getListener().equals(listener));
    }

    private static RouteConfiguration metadataBypass(RouteConfiguration routes) {
        VirtualHost host = routes.getVirtualHosts(0);
        Route metadata = host.getRoutes(0).toBuilder()
                .setMatch(RouteMatch.newBuilder().setPrefix("/" + MetadataService.class.getName() + "/"))
                .putTypedPerFilterConfig("fault", Any.pack(HTTPFault.getDefaultInstance()))
                .build();
        // This case isolates business dispatch from metadata warm-up; metadata fault is tested separately.
        return routes.toBuilder()
                .setVirtualHosts(
                        0, host.toBuilder().clearRoutes().addRoutes(metadata).addAllRoutes(host.getRoutesList()))
                .build();
    }

    static HTTPFault abort(int grpcStatus) {
        return HTTPFault.newBuilder()
                .setAbort(FaultAbort.newBuilder().setGrpcStatus(grpcStatus).setPercentage(percent()))
                .build();
    }

    static HTTPFault delay(long millis) {
        return HTTPFault.newBuilder()
                .setDelay(FaultDelay.newBuilder()
                        .setFixedDelay(Duration.newBuilder()
                                .setSeconds(millis / 1000)
                                .setNanos((int) (millis % 1000) * 1000000))
                        .setPercentage(percent()))
                .build();
    }

    private static FractionalPercent percent() {
        return FractionalPercent.newBuilder().setNumerator(100).build();
    }

    static XdsEndpoint endpoint(Fixture f) {
        return f.directory.getAllInvokers().stream()
                .map(invoker -> NativeTlsAdapter.endpoint(invoker.getUrl()))
                .filter(value -> value.getCluster().getName().equals(f.ads.clusters.get(0)))
                .findFirst()
                .orElseThrow(AssertionError::new);
    }

    private static long admission(Fixture f) {
        return XdsCircuitBreaker.active(f.workloadFingerprint, endpoint(f).getCluster());
    }

    static void status(TriRpcStatus.Code status, CompletableFuture<?> call) {
        Throwable failure = assertThrows(ExecutionException.class, () -> call.get(2, TimeUnit.SECONDS))
                .getCause();
        assertEquals(status, TriRpcStatus.getStatus(failure).code, () -> String.valueOf(failure));
    }

    static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(10);
        assertTrue(condition.getAsBoolean());
    }

    static class Recording implements ClientResponseObserver<String, String> {
        final List<String> messages = new CopyOnWriteArrayList<>();
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicInteger before = new AtomicInteger();
        final AtomicInteger terminals = new AtomicInteger();
        volatile ClientCallStreamObserver<String> writer;
        volatile Throwable error;
        boolean manual;

        public void beforeStart(ClientCallStreamObserver<String> writer) {
            this.writer = writer;
            before.incrementAndGet();
            if (manual) writer.disableAutoRequestWithInitial(2);
        }

        public void onNext(String value) {
            messages.add(value);
        }

        public void onError(Throwable error) {
            this.error = error;
            terminals.incrementAndGet();
            done.countDown();
        }

        public void onCompleted() {
            terminals.incrementAndGet();
            done.countDown();
        }
    }
}
