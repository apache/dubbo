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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistryDirectory;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.registry.XdsServiceDiscoveryTest.Ads;
import org.apache.dubbo.xds.security.NativeTlsAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import com.google.protobuf.Duration;
import com.google.protobuf.UInt32Value;
import io.grpc.BindableService;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RetryPolicy;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.WeightedCluster;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XdsRetryTest {
    private static final String BLUE = "outbound|7070|blue|echo.test.svc.cluster.local";
    private static final String GREEN = "outbound|7070|green|echo.test.svc.cluster.local";

    public interface Business {
        String sync(String value);

        CompletableFuture<String> call(Request request);

        void watch(String value, StreamObserver<String> observer);

        StreamObserver<String> chat(StreamObserver<String> observer);
    }

    public static class Request implements Serializable {
        private static final long serialVersionUID = 1L;
        public String value;

        public Request() {}

        Request(String value) {
            this.value = value;
        }
    }

    @Test
    void retriesSyncAndAsyncFailuresAndDoesNotStackDubboFailover() throws Exception {
        try (Fixture f = new Fixture()) {
            f.blue.failures = 1;
            assertEquals("blue:sync", f.remote.sync("sync"));
            assertEquals(2, f.blue.calls.get());
            f.blue.reset();
            f.blue.failures = 2;
            assertEquals("blue:async", f.remote.call(new Request("async")).get(2, TimeUnit.SECONDS));
            assertEquals(3, f.blue.calls.get());
            assertEquals(Arrays.asList(null, "1", "2"), f.blue.previousAttempts);
            f.blue.reset();
            f.blue.failures = 20;
            Throwable failure = assertThrows(
                            ExecutionException.class,
                            () -> f.remote.call(new Request("fails")).get(2, TimeUnit.SECONDS))
                    .getCause();
            assertEquals(TriRpcStatus.Code.UNAVAILABLE, TriRpcStatus.getStatus(failure).code);
            assertEquals(3, f.blue.calls.get());
            assertEquals(0, f.green.calls.get());
        }
    }

    @Test
    void nonRetryableErrorsAndStreamingResponsesAreNotReplayed() throws Exception {
        try (Fixture f = new Fixture()) {
            f.blue.failures = 20;
            f.blue.code = TriRpcStatus.Code.PERMISSION_DENIED;
            assertThrows(
                    ExecutionException.class,
                    () -> f.remote.call(new Request("denied")).get(2, TimeUnit.SECONDS));
            assertEquals(1, f.blue.calls.get());
            f.blue.reset();
            f.blue.businessError = true;
            assertThrows(
                    ExecutionException.class,
                    () -> f.remote.call(new Request("business")).get(2, TimeUnit.SECONDS));
            assertEquals(1, f.blue.calls.get());
            f.blue.reset();
            BlockingQueue<Object> events = new LinkedBlockingQueue<>();
            f.remote.watch("first", new StreamObserver<String>() {
                public void onNext(String value) {
                    events.add(value);
                }

                public void onError(Throwable error) {
                    events.add(error);
                }

                public void onCompleted() {
                    events.add("complete");
                }
            });
            assertEquals("first", events.poll(1, TimeUnit.SECONDS));
            assertInstanceOf(Throwable.class, events.poll(1, TimeUnit.SECONDS));
            assertEquals(1, f.blue.calls.get());
        }
    }

    @Test
    void totalDeadlineExpiresDuringPushbackAndNegativePushbackStopsRetries() throws Exception {
        try (Fixture f = new Fixture()) {
            f.routes(100, 180);
            f.blue.failures = 20;
            f.blue.pushback = "800";
            long started = System.nanoTime();
            Throwable failure = assertThrows(
                            ExecutionException.class,
                            () -> f.remote.call(new Request("deadline")).get(1, TimeUnit.SECONDS))
                    .getCause();
            assertEquals(TriRpcStatus.Code.DEADLINE_EXCEEDED, TriRpcStatus.getStatus(failure).code);
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 600);
            assertEquals(1, f.blue.calls.get());
            f.routes(100, 2000);
            f.blue.reset();
            f.blue.failures = 20;
            f.blue.pushback = "-1";
            assertThrows(
                    ExecutionException.class,
                    () -> f.remote.call(new Request("stop")).get(1, TimeUnit.SECONDS));
            assertEquals(1, f.blue.calls.get());
            f.blue.reset();
            f.blue.failures = 20;
            f.blue.pushback = "invalid";
            assertThrows(
                    ExecutionException.class,
                    () -> f.remote.call(new Request("invalid")).get(1, TimeUnit.SECONDS));
            assertEquals(1, f.blue.calls.get());
            f.routes(100, 420);
            f.blue.reset();
            f.blue.failures = 1;
            f.blue.delay = 180;
            f.blue.pushback = "100";
            Throwable acrossAttempts = assertThrows(
                            ExecutionException.class,
                            () -> f.remote.call(new Request("across-attempts")).get(1, TimeUnit.SECONDS))
                    .getCause();
            assertEquals(TriRpcStatus.Code.DEADLINE_EXCEEDED, TriRpcStatus.getStatus(acrossAttempts).code);
            assertEquals(2, f.blue.calls.get());
            assertTrue(f.blue.timeouts.get(1) < 200);
        }
    }

    @Test
    void cancellationAndDestroyStopActiveAttemptsAndBackoff() throws Exception {
        try (Fixture f = new Fixture()) {
            f.blue.delay = 900;
            CompletableFuture<String> active = f.remote.call(new Request("active"));
            assertTrue(f.blue.started.await(1, TimeUnit.SECONDS));
            assertTrue(active.cancel(true));
            assertTrue(f.blue.cancelled.await(400, TimeUnit.MILLISECONDS));
            f.blue.reset();
            f.blue.failures = 20;
            f.blue.pushback = "700";
            CompletableFuture<String> waiting = f.remote.call(new Request("backoff"));
            assertTrue(f.blue.started.await(1, TimeUnit.SECONDS));
            Thread.sleep(60);
            assertTrue(waiting.cancel(true));
            Thread.sleep(760);
            assertEquals(1, f.blue.calls.get());
            f.blue.reset();
            f.blue.failures = 20;
            f.blue.pushback = "700";
            CompletableFuture<String> destroying = f.remote.call(new Request("destroy"));
            assertTrue(f.blue.started.await(1, TimeUnit.SECONDS));
            f.consumer.destroy();
            await(destroying::isDone);
            Thread.sleep(760);
            assertEquals(1, f.blue.calls.get());
        }
    }

    @Test
    void retriesUseCurrentEndpointsButKeepTheOriginalClusterAfterRouteUpdates() throws Exception {
        try (Fixture f = new Fixture()) {
            f.blue.failures = 1;
            f.blue.pushback = "400";
            CompletableFuture<String> retry = f.remote.call(new Request("pinned"));
            assertTrue(f.blue.started.await(1, TimeUnit.SECONDS));
            f.ads.clusterPorts.put(BLUE, f.replacementPort);
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            f.routes(0, 2000);
            await(() -> f.directory.getAllInvokers().stream()
                    .anyMatch(invoker -> invoker.getUrl().getPort() == f.replacementPort));
            assertEquals("replacement:pinned", retry.get(2, TimeUnit.SECONDS));
            assertEquals(1, f.blue.calls.get());
            assertEquals(1, f.replacement.calls.get());
            assertEquals(0, f.green.calls.get());
            assertEquals("green:new", f.remote.call(new Request("new")).get(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void aClusterRemovedFromRoutesStaysSubscribedUntilItsCallFinishes() throws Exception {
        try (Fixture f = new Fixture()) {
            f.blue.failures = 1;
            f.blue.pushback = "800";
            CompletableFuture<String> pending = f.remote.call(new Request("retained"));
            assertTrue(f.blue.started.await(1, TimeUnit.SECONDS));
            f.onlyGreen();
            assertTrue(f.subscribed(BLUE, XdsResourceType.CDS.getTypeUrl()));
            assertEquals(
                    "green:new-route", f.remote.call(new Request("new-route")).get(1, TimeUnit.SECONDS));
            f.ads.clusterPorts.put(BLUE, f.replacementPort);
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> f.directory.getAllInvokers().stream()
                    .anyMatch(invoker -> invoker.getUrl().getPort() == f.replacementPort));
            assertEquals("replacement:retained", pending.get(2, TimeUnit.SECONDS));
            await(() -> !f.subscribed(BLUE, XdsResourceType.CDS.getTypeUrl())
                    && !f.subscribed(BLUE, XdsResourceType.EDS.getTypeUrl()));
            await(() -> f.directory.getAllInvokers().size() == 1);
            assertEquals(1, f.blue.calls.get());
            assertEquals(1, f.replacement.calls.get());
            assertEquals(1, f.green.calls.get());
        }
    }

    @Test
    void retainedClusterDeletionCannotReviveItsAddressesOrRedirectTheCall() throws Exception {
        try (Fixture f = new Fixture()) {
            f.blue.failures = 1;
            f.blue.pushback = "600";
            CompletableFuture<String> pending = f.remote.call(new Request("deleted-cluster"));
            assertTrue(f.blue.started.await(1, TimeUnit.SECONDS));
            f.onlyGreen();
            f.ads.clusters = Collections.singletonList(GREEN);
            f.ads.send(XdsResourceType.CDS.getTypeUrl());
            await(() -> f.directory.getAllInvokers().size() == 1);
            Throwable error = assertThrows(ExecutionException.class, () -> pending.get(2, TimeUnit.SECONDS))
                    .getCause();
            assertEquals(TriRpcStatus.Code.UNAVAILABLE, TriRpcStatus.getStatus(error).code);
            assertEquals(1, f.blue.calls.get());
            assertEquals(0, f.green.calls.get());
            await(() -> !f.subscribed(BLUE, XdsResourceType.CDS.getTypeUrl()));
            assertEquals(
                    "green:still-ready",
                    f.remote.call(new Request("still-ready")).get(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void cancellationExpiryAndDestroyReleaseAClusterRemovedFromRoutes() throws Exception {
        for (String stop : Arrays.asList("cancel", "expiry", "destroy")) {
            try (Fixture f = new Fixture()) {
                if (stop.equals("expiry")) f.routes(100, 550);
                f.blue.failures = 20;
                f.blue.pushback = "900";
                CompletableFuture<String> pending = f.remote.call(new Request(stop));
                assertTrue(f.blue.started.await(1, TimeUnit.SECONDS));
                f.onlyGreen();
                assertTrue(f.subscribed(BLUE, XdsResourceType.CDS.getTypeUrl()));
                if (stop.equals("cancel")) {
                    assertTrue(pending.cancel(true));
                } else if (stop.equals("destroy")) {
                    f.consumer.destroy();
                } else {
                    Throwable error = assertThrows(ExecutionException.class, () -> pending.get(1, TimeUnit.SECONDS))
                            .getCause();
                    assertEquals(TriRpcStatus.Code.DEADLINE_EXCEEDED, TriRpcStatus.getStatus(error).code);
                }
                await(pending::isDone);
                await(() -> !f.subscribed(BLUE, XdsResourceType.CDS.getTypeUrl()));
                assertEquals(1, f.blue.calls.get());
            }
        }
    }

    @Test
    void streamsHoldTheClusterUntilTransportCompletionExpiryOrReferenceDestroy() throws Exception {
        for (String stop : Arrays.asList("complete", "expiry", "destroy")) {
            try (Fixture f = new Fixture()) {
                if (stop.equals("expiry")) f.routes(100, 550);
                f.blue.keepStream = true;
                BlockingQueue<Object> events = new LinkedBlockingQueue<>();
                f.remote.watch(stop, new StreamObserver<String>() {
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
                assertEquals(stop, events.poll(1, TimeUnit.SECONDS));
                f.onlyGreen();
                assertTrue(f.subscribed(BLUE, XdsResourceType.CDS.getTypeUrl()));
                if (stop.equals("complete")) {
                    f.blue.streamObserver.onCompleted();
                    assertEquals("completed", events.poll(1, TimeUnit.SECONDS));
                } else if (stop.equals("destroy")) {
                    f.consumer.destroy();
                    assertTrue(f.blue.cancelled.await(500, TimeUnit.MILLISECONDS));
                    Object terminal = events.poll(1, TimeUnit.SECONDS);
                    assertInstanceOf(Throwable.class, terminal);
                    assertEquals(TriRpcStatus.Code.CANCELLED, TriRpcStatus.getStatus((Throwable) terminal).code);
                } else {
                    Object terminal = events.poll(1, TimeUnit.SECONDS);
                    assertInstanceOf(Throwable.class, terminal);
                    assertEquals(
                            TriRpcStatus.Code.DEADLINE_EXCEEDED, TriRpcStatus.getStatus((Throwable) terminal).code);
                    assertTrue(f.blue.cancelled.await(500, TimeUnit.MILLISECONDS));
                }
                await(() -> !f.subscribed(BLUE, XdsResourceType.CDS.getTypeUrl()));
                assertEquals(1, f.blue.calls.get());
            }
        }
    }

    @Test
    void removedEndpointsAreNotRevivedAndFrozenRequestsAreReplayed() throws Exception {
        try (Fixture f = new Fixture()) {
            f.blue.failures = 1;
            f.blue.delay = 120;
            Request request = new Request("original");
            CompletableFuture<String> replay = f.remote.call(request);
            assertTrue(f.blue.started.await(1, TimeUnit.SECONDS));
            request.value = "mutated";
            assertEquals("blue:original", replay.get(2, TimeUnit.SECONDS));
            assertEquals(Arrays.asList("original", "original"), f.blue.values);
            f.blue.reset();
            f.blue.failures = 20;
            f.blue.pushback = "300";
            CompletableFuture<String> removed = f.remote.call(new Request("removed"));
            assertTrue(f.blue.started.await(1, TimeUnit.SECONDS));
            f.ads.emptyClusters.add(BLUE);
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> f.directory.getAllInvokers().size() == 1);
            assertThrows(ExecutionException.class, () -> removed.get(2, TimeUnit.SECONDS));
            assertEquals(1, f.blue.calls.get());
            assertEquals(0, f.green.calls.get());
        }
    }

    @Test
    void allWithdrawnProvidersReturnUnavailableAndKeepAuthorizationFailuresDistinct() throws Exception {
        try (Fixture f = new Fixture()) {
            f.blue.code = TriRpcStatus.Code.PERMISSION_DENIED;
            f.blue.failures = 1;
            RpcException denied = assertThrows(RpcException.class, () -> f.remote.sync("denied"));
            assertEquals(TriRpcStatus.Code.PERMISSION_DENIED, TriRpcStatus.getStatus(denied).code);
            assertEquals(1, f.blue.calls.get());
            f.ads.emptyClusters.addAll(Arrays.asList(BLUE, GREEN));
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> f.directory.getAllInvokers().isEmpty());
            RpcException unavailable = assertThrows(RpcException.class, () -> f.remote.sync("withdrawn"));
            assertEquals(TriRpcStatus.Code.UNAVAILABLE, TriRpcStatus.getStatus(unavailable).code);
            assertEquals(1, f.blue.calls.get());
            assertEquals(0, f.green.calls.get());
            f.ads.emptyClusters.clear();
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> f.directory.getAllInvokers().size() == 2);
            assertEquals("blue:restored", f.remote.sync("restored"));
        }
    }

    @Test
    void withdrawnEndpointFinishesDispatchedUnaryButCannotReceiveNewCalls() throws Exception {
        try (Fixture f = new Fixture(Collections.singletonMap("timeout", "5000"))) {
            f.routes(100, 5000);
            CompletableFuture<String> provider = new CompletableFuture<>();
            f.blue.responses.put("draining", provider);
            CompletableFuture<String> pending = f.remote.call(new Request("draining"));
            assertTrue(f.blue.started.await(1, TimeUnit.SECONDS));
            Invoker<Business> removed = f.directory.getAllInvokers().stream()
                    .filter(invoker -> NativeTlsAdapter.endpoint(invoker.getUrl())
                            .getCluster()
                            .getName()
                            .equals(BLUE))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
            f.ads.emptyClusters.add(BLUE);
            f.ads.emptyClusters.add(GREEN);
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> f.directory.getAllInvokers().isEmpty() && !removed.isAvailable());
            RpcContext.getClientAttachment().setAttachment("timeout", 120);
            try {
                Exception absent = assertThrows(
                        Exception.class,
                        () -> f.remote.call(new Request("after-withdrawal")).get(1, TimeUnit.SECONDS));
                Throwable cause = absent instanceof ExecutionException ? absent.getCause() : absent;
                assertEquals(TriRpcStatus.Code.UNAVAILABLE, TriRpcStatus.getStatus(cause).code);
            } finally {
                RpcContext.removeClientAttachment();
            }
            assertEquals(1, f.blue.calls.get());
            assertEquals(0, f.green.calls.get());
            provider.complete("blue:draining");
            assertEquals("blue:draining", pending.get(1, TimeUnit.SECONDS));
            f.ads.emptyClusters.remove(BLUE);
            f.ads.emptyClusters.remove(GREEN);
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> f.directory.getAllInvokers().size() == 2);
            assertEquals("blue:restored", f.remote.call(new Request("restored")).get(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void withdrawnEndpointKeepsItsDispatchedStreamUntilCompletion() throws Exception {
        try (Fixture f = new Fixture(Collections.singletonMap("timeout", "5000"))) {
            f.routes(100, 5000);
            f.blue.keepStream = true;
            BlockingQueue<Object> events = new LinkedBlockingQueue<>();
            f.remote.watch("draining-stream", new StreamObserver<String>() {
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
            assertEquals("draining-stream", events.poll(1, TimeUnit.SECONDS));
            Invoker<Business> removed = f.directory.getAllInvokers().stream()
                    .filter(invoker -> NativeTlsAdapter.endpoint(invoker.getUrl())
                            .getCluster()
                            .getName()
                            .equals(BLUE))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
            f.ads.emptyClusters.add(BLUE);
            f.ads.emptyClusters.add(GREEN);
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> f.directory.getAllInvokers().isEmpty() && !removed.isAvailable());
            assertNull(events.poll(100, TimeUnit.MILLISECONDS));
            f.blue.streamObserver.onCompleted();
            assertEquals("completed", events.poll(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void oversizedRequestsCommitWithoutRetryAndInvalidUpdatesRetainTheLastPolicy() throws Exception {
        try (Fixture f = new Fixture()) {
            f.blue.failures = 20;
            char[] chars = new char[1100000];
            Arrays.fill(chars, 'a');
            assertThrows(
                    ExecutionException.class,
                    () -> f.remote.call(new Request(new String(chars))).get(2, TimeUnit.SECONDS));
            assertEquals(1, f.blue.calls.get());
            int rejected = f.ads.rejected.get();
            RouteConfiguration invalid = f.config(100, 2000).toBuilder()
                    .setVirtualHosts(
                            0,
                            f.config(100, 2000).getVirtualHosts(0).toBuilder()
                                    .setRoutes(
                                            0,
                                            f.config(100, 2000).getVirtualHosts(0).getRoutes(0).toBuilder()
                                                    .setRoute(f
                                                            .config(100, 2000)
                                                            .getVirtualHosts(0)
                                                            .getRoutes(0)
                                                            .getRoute()
                                                            .toBuilder()
                                                            .setRetryPolicy(RetryPolicy.newBuilder()
                                                                    .setRetryOn("unavailable")
                                                                    .setNumRetries(UInt32Value.of(0))))))
                    .build();
            f.ads.routeOverride = invalid;
            f.ads.send(XdsResourceType.LDS.getTypeUrl());
            await(() -> f.ads.rejected.get() > rejected);
            f.blue.reset();
            f.blue.failures = 1;
            assertEquals(
                    "blue:last-good", f.remote.call(new Request("last-good")).get(2, TimeUnit.SECONDS));
            assertEquals(2, f.blue.calls.get());
        }
    }

    static final class Implementation implements Business {
        final String name;
        final AtomicInteger calls = new AtomicInteger();
        final List<String> values = new CopyOnWriteArrayList<>();
        final List<String> previousAttempts = new CopyOnWriteArrayList<>();
        final List<Long> timeouts = new CopyOnWriteArrayList<>();
        final Map<String, CompletableFuture<String>> responses = new ConcurrentHashMap<>();
        volatile int failures;
        volatile long delay;
        volatile String pushback;
        volatile TriRpcStatus.Code code = TriRpcStatus.Code.UNAVAILABLE;
        volatile boolean businessError;
        volatile boolean keepStream;
        volatile StreamObserver<String> streamObserver;
        volatile CountDownLatch started = new CountDownLatch(1);
        volatile CountDownLatch cancelled = new CountDownLatch(1);

        Implementation(String name) {
            this.name = name;
        }

        void reset() {
            calls.set(0);
            values.clear();
            previousAttempts.clear();
            timeouts.clear();
            responses.clear();
            failures = 0;
            delay = 0;
            pushback = null;
            code = TriRpcStatus.Code.UNAVAILABLE;
            businessError = false;
            started = new CountDownLatch(1);
            cancelled = new CountDownLatch(1);
        }

        int record(String value) {
            values.add(value);
            timeouts.add((Long) RpcContext.getServiceContext().getInvocation().get(TIMEOUT_KEY));
            previousAttempts.add(
                    RpcContext.getServiceContext().getInvocation().getAttachment("grpc-previous-rpc-attempts"));
            if (pushback != null) {
                RpcContext.getServerResponseContext().setAttachment("grpc-retry-pushback-ms", pushback);
            }
            CancellationContext context = RpcContext.getCancellationContext();
            CountDownLatch cancellation = cancelled;
            context.addListener(ignored -> {
                if (context.getCancellationCause() != null) {
                    cancellation.countDown();
                }
            });
            int number = calls.incrementAndGet();
            started.countDown();
            return number;
        }

        String respond(String value, int number) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(error);
            }
            if (businessError) {
                throw new IllegalArgumentException("ordinary business error");
            }
            if (number <= failures) {
                throw TriRpcStatus.fromCode(code)
                        .withDescription("retry test transport status")
                        .asException();
            }
            return name + ":" + value;
        }

        public String sync(String value) {
            return respond(value, record(value));
        }

        public CompletableFuture<String> call(Request request) {
            int number = record(request.value);
            String value = request.value;
            CompletableFuture<String> held = responses.get(value);
            if (held != null) return held;
            return CompletableFuture.supplyAsync(() -> respond(value, number));
        }

        public void watch(String value, StreamObserver<String> observer) {
            record(value);
            if (keepStream) {
                streamObserver = observer;
            }
            observer.onNext(value);
            if (!keepStream) {
                observer.onError(TriRpcStatus.UNAVAILABLE.asException());
            }
        }

        public StreamObserver<String> chat(StreamObserver<String> observer) {
            record("chat");
            return new StreamObserver<String>() {
                public void onNext(String value) {
                    values.add(value);
                    observer.onNext(name + ":" + value);
                }

                public void onError(Throwable error) {
                    observer.onError(error);
                }

                public void onCompleted() {
                    observer.onCompleted();
                }
            };
        }
    }

    static final class Fixture implements AutoCloseable {
        final FrameworkModel framework = new FrameworkModel();
        final FrameworkModel providerFramework = new FrameworkModel();
        final ApplicationModel application = framework.newApplication();
        final ApplicationModel provider = providerFramework.newApplication();
        final List<Exporter<?>> exporters = new ArrayList<>();
        final List<MetadataService> metadataServices = new ArrayList<>();
        final List<FrameworkModel> extraFrameworks = new ArrayList<>();
        final List<Protocol> extraProtocols = new ArrayList<>();
        final Ads ads;
        final Protocol protocol;
        final Protocol providerProtocol;
        final XdsRegistry registry;
        final ServiceDiscoveryRegistryDirectory<Business> directory;
        final Invoker<Business> consumer;
        final Business remote;
        final Implementation blue = new Implementation("blue");
        final Implementation replacement = new Implementation("replacement");
        final Implementation green = new Implementation("green");
        final int replacementPort;
        final String workloadFingerprint;

        Fixture() throws Exception {
            this(Collections.emptyMap());
        }

        Fixture(Map<String, String> referenceParameters, BindableService... controlServices) throws Exception {
            ads = new Ads(controlServices);
            workloadFingerprint = Bootstrapper.parse(ads.registry(application).getParameter("xds.bootstrap.config"))
                    .getFingerprint();
            application.getApplicationConfigManager().setApplication(new ApplicationConfig("deadline-test"));
            provider.getApplicationConfigManager().setApplication(new ApplicationConfig("provider-app"));
            protocol = application.getExtensionLoader(Protocol.class).getExtension("tri");
            providerProtocol = provider.getExtensionLoader(Protocol.class).getExtension("tri");
            ProxyFactory providerProxy =
                    provider.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
            int bluePort = export(blue, providerProxy);
            replacementPort = export(replacement, providerProxy);
            int greenPort = export(green, providerProxy);
            ads.workloadPort = bluePort;
            ads.clusters = Arrays.asList(BLUE, GREEN);
            ads.clusterPorts.put(BLUE, bluePort);
            ads.clusterPorts.put(GREEN, greenPort);
            ads.routeOverride = config(100, 2000);
            registry = new XdsRegistry(ads.registry(application), application);
            URL reference = URL.valueOf(
                            "tri://localhost/" + Business.class.getName()
                                    + "?provided-by=echo&provider-port=7070&provider-namespace=test&protocol=tri&xds.metadata.application=provider-app&timeout=2000&retries=10")
                    .addParameters(referenceParameters)
                    .setScopeModel(application.getInternalModule())
                    .addParameter("interface", Business.class.getName());
            ServiceDescriptor descriptor =
                    application.getInternalModule().getServiceRepository().registerService(Business.class);
            reference = reference.setServiceModel(
                    application.getInternalModule().registerInternalConsumer(Business.class, reference, descriptor));
            URL directoryUrl = ads.registry(application)
                    .setPath(Business.class.getName())
                    .setScopeModel(application.getInternalModule())
                    .setServiceModel(reference.getServiceModel())
                    .addParameterAndEncoded("refer", reference.toParameterString());
            directory = new ServiceDiscoveryRegistryDirectory<>(Business.class, directoryUrl);
            directory.setRegistry(registry);
            directory.setProtocol(protocol);
            directory.buildRouterChain(reference);
            directory.subscribe(reference);
            ads.endpoints("127.0.0.1");
            try {
                await(() -> directory.getAllInvokers().size() == 2
                        && directory.getAllInvokers().stream().allMatch(Invoker::isAvailable));
            } catch (AssertionError error) {
                System.err.println("Retry fixture ADS rejection: " + ads.lastRejection);
                ((XdsServiceDiscovery) registry.getServiceDiscovery())
                        .inspectTargets()
                        .get(1, TimeUnit.SECONDS)
                        .forEach((key, status) -> System.err.println("Retry fixture " + key + " endpoints="
                                + status.getDiscoveredEndpoints() + " instances=" + status.getAvailableInstances()
                                + " graph=" + status.isGraphReady() + " error=" + status.getLastError()));
                close();
                throw error;
            }
            consumer = application
                    .getExtensionLoader(Cluster.class)
                    .getAdaptiveExtension()
                    .join(directory, true);
            remote = application
                    .getExtensionLoader(ProxyFactory.class)
                    .getAdaptiveExtension()
                    .getProxy(consumer);
        }

        int export(Implementation implementation, ProxyFactory providerProxy) {
            ApplicationModel peer = provider;
            Protocol peerProtocol = providerProtocol;
            if (implementation != blue) {
                FrameworkModel peerFramework = new FrameworkModel();
                extraFrameworks.add(peerFramework);
                peer = peerFramework.newApplication();
                peer.getApplicationConfigManager().setApplication(new ApplicationConfig("provider-app"));
                peerProtocol = peer.getExtensionLoader(Protocol.class).getExtension("tri");
                extraProtocols.add(peerProtocol);
                providerProxy = peer.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
            }
            int port = NetUtils.getAvailablePort();
            MetadataInfo metadata = new MetadataInfo("provider-app");
            MetadataInfo.ServiceInfo info = new MetadataInfo.ServiceInfo(
                    Business.class.getName(),
                    null,
                    null,
                    "tri",
                    port,
                    Business.class.getName(),
                    Collections.emptyMap());
            metadata.getServices().put(info.getMatchKey(), info);
            MetadataService metadataService = mock(MetadataService.class);
            metadataServices.add(metadataService);
            when(metadataService.getMetadataInfos()).thenReturn(Collections.singletonList(metadata));
            exporters.add(XdsRoutingTest.export(
                    peer,
                    peerProtocol,
                    providerProxy,
                    MetadataService.class,
                    metadataService,
                    URL.valueOf("tri://127.0.0.1:" + port + "/" + MetadataService.class.getName()
                            + "?filter=-default&group=provider-app&version=1.0.0")));
            exporters.add(XdsRoutingTest.export(
                    peer,
                    peerProtocol,
                    providerProxy,
                    Business.class,
                    implementation,
                    URL.valueOf("tri://127.0.0.1:" + port + "/" + Business.class.getName())));
            return port;
        }

        Cluster consumerCluster() {
            return application.getExtensionLoader(Cluster.class).getAdaptiveExtension();
        }

        RouteConfiguration config(int blueWeight, long timeout) {
            RouteAction.Builder action = RouteAction.newBuilder()
                    .setWeightedClusters(WeightedCluster.newBuilder()
                            .addClusters(WeightedCluster.ClusterWeight.newBuilder()
                                    .setName(BLUE)
                                    .setWeight(UInt32Value.of(blueWeight)))
                            .addClusters(WeightedCluster.ClusterWeight.newBuilder()
                                    .setName(GREEN)
                                    .setWeight(UInt32Value.of(100 - blueWeight))))
                    .setRetryPolicy(RetryPolicy.newBuilder()
                            .setRetryOn("connect-failure,refused-stream,unavailable,cancelled,retriable-status-codes")
                            .setNumRetries(UInt32Value.of(2))
                            .setHostSelectionRetryMaxAttempts(5))
                    .setMaxStreamDuration(
                            RouteAction.MaxStreamDuration.newBuilder().setMaxStreamDuration(duration(timeout)));
            return RouteConfiguration.newBuilder()
                    .setName("routes")
                    .addVirtualHosts(VirtualHost.newBuilder()
                            .addDomains("echo.test.svc.cluster.local:7070")
                            .addRoutes(Route.newBuilder()
                                    .setMatch(RouteMatch.newBuilder().setPrefix("/"))
                                    .setRoute(action)))
                    .build();
        }

        void routes(int weight, long timeout) throws Exception {
            RouteConfiguration config = config(weight, timeout);
            ads.routeOverride = config;
            ads.send(XdsResourceType.LDS.getTypeUrl());
            await(() -> NativeTlsAdapter.endpoint(
                            directory.getAllInvokers().get(0).getUrl())
                    .getSnapshot()
                    .getRoutes()
                    .get("routes")
                    .equals(config));
        }

        boolean subscribed(String cluster, String type) {
            return ads.names.getOrDefault(type, Collections.emptyList()).contains(cluster);
        }

        void onlyGreen() throws Exception {
            RouteConfiguration previous = config(0, 2000);
            Route only = previous.getVirtualHosts(0).getRoutes(0).toBuilder()
                    .setRoute(previous.getVirtualHosts(0).getRoutes(0).getRoute().toBuilder()
                            .setCluster(GREEN))
                    .build();
            RouteConfiguration changed = previous.toBuilder()
                    .setVirtualHosts(0, previous.getVirtualHosts(0).toBuilder().setRoutes(0, only))
                    .build();
            ads.routeOverride = changed;
            ads.send(XdsResourceType.LDS.getTypeUrl());
            await(() -> NativeTlsAdapter.endpoint(
                            directory.getAllInvokers().get(0).getUrl())
                    .getSnapshot()
                    .getRoutes()
                    .get("routes")
                    .equals(changed));
        }

        @Override
        public void close() throws Exception {
            if (consumer != null) {
                consumer.destroy();
            }
            registry.destroy();
            exporters.forEach(Exporter::unexport);
            extraProtocols.forEach(Protocol::destroy);
            providerProtocol.destroy();
            protocol.destroy();
            extraFrameworks.forEach(FrameworkModel::destroy);
            providerFramework.destroy();
            framework.destroy();
            ads.close();
            RpcContext.removeClientAttachment();
        }
    }

    private static Duration duration(long millis) {
        return Duration.newBuilder()
                .setSeconds(millis / 1000)
                .setNanos((int) (millis % 1000) * 1000000)
                .build();
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("Condition did not become true");
            }
            Thread.sleep(10);
        }
    }
}
