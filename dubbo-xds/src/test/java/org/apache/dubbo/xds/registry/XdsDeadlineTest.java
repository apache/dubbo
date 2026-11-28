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
import org.apache.dubbo.rpc.TimeoutCountDown;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.registry.XdsServiceDiscoveryTest.Ads;
import org.apache.dubbo.xds.security.NativeTlsAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import com.google.protobuf.Duration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIME_COUNTDOWN_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XdsDeadlineTest {
    private static final String CLUSTER = "outbound|7070||echo.test.svc.cluster.local";

    public interface Business {
        String slow(long millis);

        long observedTimeout();

        CompletableFuture<String> slowAsync(long millis);

        void watch(String value, StreamObserver<String> observer);

        StreamObserver<String> chat(StreamObserver<String> observer);
    }

    @Test
    void realUnaryAsyncAndStreamingCallsRespectTheSameRouteDeadline() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.routes(limit(150));
            assertEquals("ok", fixture.remote.slow(0));
            long wire = fixture.remote.observedTimeout();
            assertTrue(wire > 0 && wire <= 150);
            long started = System.nanoTime();
            RpcException sync = assertThrows(RpcException.class, () -> fixture.remote.slow(450));
            assertEquals(TriRpcStatus.Code.DEADLINE_EXCEEDED, TriRpcStatus.getStatus(sync).code);
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 380);
            CompletableFuture<String> async = fixture.remote.slowAsync(450);
            ExecutionException failed = assertThrows(ExecutionException.class, () -> async.get(1, TimeUnit.SECONDS));
            assertEquals(TriRpcStatus.Code.DEADLINE_EXCEEDED, TriRpcStatus.getStatus(failed.getCause()).code);
            RecordingObserver watching = new RecordingObserver();
            fixture.remote.watch("first", watching);
            assertEquals("first", watching.events.poll(1, TimeUnit.SECONDS));
            assertTrue(fixture.implementation.streamTimeout > 0 && fixture.implementation.streamTimeout <= 150);
            Object expired = watching.events.poll(1, TimeUnit.SECONDS);
            assertInstanceOf(Throwable.class, expired);
            assertEquals(TriRpcStatus.Code.DEADLINE_EXCEEDED, TriRpcStatus.getStatus((Throwable) expired).code);
            RecordingObserver chat = new RecordingObserver();
            StreamObserver<String> requests = fixture.remote.chat(chat);
            Object noHeaders = chat.events.poll(1, TimeUnit.SECONDS);
            assertInstanceOf(Throwable.class, noHeaders);
            assertEquals(TriRpcStatus.Code.DEADLINE_EXCEEDED, TriRpcStatus.getStatus((Throwable) noHeaders).code);
            assertThrows(IllegalStateException.class, () -> requests.onNext("late"));
            RouteAction.Builder immediatelyExpired = RouteAction.newBuilder()
                    .setCluster(CLUSTER)
                    .setMaxStreamDuration(RouteAction.MaxStreamDuration.newBuilder()
                            .setMaxStreamDuration(Duration.newBuilder().setNanos(1)));
            fixture.routes(immediatelyExpired);
            RecordingObserver immediate = new RecordingObserver();
            fixture.remote.watch("never-sent", immediate);
            Object beforeDispatch = immediate.events.poll(1, TimeUnit.SECONDS);
            assertInstanceOf(Throwable.class, beforeDispatch);
            assertEquals(TriRpcStatus.Code.DEADLINE_EXCEEDED, TriRpcStatus.getStatus((Throwable) beforeDispatch).code);
        }
    }

    @Test
    void explicitBudgetsZeroOverridesAndHcmDefaultsAreAppliedOnLiveInvokers() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.routes(limit(1000));
            assertEquals("ok", fixture.remote.slow(0));
            RpcContext.getClientAttachment().setObjectAttachment(TIMEOUT_KEY, 80);
            assertThrows(RpcException.class, () -> fixture.remote.slow(450));
            RpcContext.getClientAttachment()
                    .setObjectAttachment(TIME_COUNTDOWN_KEY, TimeoutCountDown.newCountDown(80, TimeUnit.MILLISECONDS));
            assertThrows(RpcException.class, () -> fixture.remote.slow(450));
            fixture.ads.listenerTimeout = duration(80);
            RouteAction.Builder zero = limit(80);
            zero.getMaxStreamDurationBuilder().setGrpcTimeoutHeaderMax(Duration.getDefaultInstance());
            fixture.routes(zero);
            assertEquals("ok", fixture.remote.slow(180));
            fixture.routes(RouteAction.newBuilder().setCluster(CLUSTER));
            assertThrows(RpcException.class, () -> fixture.remote.slow(450));
            // An invalid update is NACKed; the already accepted 80 ms default remains active.
            int rejected = fixture.ads.rejected.get();
            fixture.ads.listenerTimeout = Duration.newBuilder().setSeconds(-1).build();
            fixture.ads.send(XdsResourceType.LDS.getTypeUrl());
            await(() -> fixture.ads.rejected.get() > rejected);
            assertEquals(
                    TimeUnit.MILLISECONDS.toNanos(80),
                    NativeTlsAdapter.endpoint(
                                    fixture.directory.getAllInvokers().get(0).getUrl())
                            .getSnapshot()
                            .getDefaultTimeoutNanos());
            assertThrows(RpcException.class, () -> fixture.remote.slow(450));
            assertEquals(1, fixture.directory.getAllInvokers().size());
        }
    }

    @Test
    void cancellingAnAsyncFutureResetsTheRealProviderCallBeforeItsDeadline() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.routes(limit(1500));
            assertEquals("ok", fixture.remote.slow(0));
            CompletableFuture<String> future = fixture.remote.slowAsync(900);
            assertTrue(fixture.implementation.asyncStarted.await(1, TimeUnit.SECONDS));
            assertTrue(future.cancel(true));
            assertTrue(
                    fixture.implementation.cancelled.await(400, TimeUnit.MILLISECONDS),
                    "Future cancellation did not reset the provider stream");
        }
    }

    private static RouteAction.Builder limit(long millis) {
        return RouteAction.newBuilder()
                .setCluster(CLUSTER)
                .setMaxStreamDuration(
                        RouteAction.MaxStreamDuration.newBuilder().setMaxStreamDuration(duration(millis)));
    }

    private static Duration duration(long millis) {
        return Duration.newBuilder()
                .setSeconds(millis / 1000)
                .setNanos((int) (millis % 1000) * 1000000)
                .build();
    }

    private static final class Fixture implements AutoCloseable {
        final FrameworkModel framework = new FrameworkModel();
        final FrameworkModel providerFramework = new FrameworkModel();
        final ApplicationModel application = framework.newApplication();
        final ApplicationModel provider = providerFramework.newApplication();
        final List<Exporter<?>> exporters = new ArrayList<>();
        final Ads ads = new Ads();
        final Protocol protocol;
        final Protocol providerProtocol;
        final XdsRegistry registry;
        final ServiceDiscoveryRegistryDirectory<Business> directory;
        final Invoker<Business> consumer;
        final Business remote;
        final Implementation implementation = new Implementation();

        Fixture() throws Exception {
            application.getApplicationConfigManager().setApplication(new ApplicationConfig("deadline-test"));
            provider.getApplicationConfigManager().setApplication(new ApplicationConfig("provider-app"));
            protocol = application.getExtensionLoader(Protocol.class).getExtension("tri");
            providerProtocol = provider.getExtensionLoader(Protocol.class).getExtension("tri");
            ProxyFactory providerProxy =
                    provider.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
            int port = NetUtils.getAvailablePort();
            ads.workloadPort = port;
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
            when(metadataService.getMetadataInfos()).thenReturn(Collections.singletonList(metadata));
            exporters.add(XdsRoutingTest.export(
                    provider,
                    providerProtocol,
                    providerProxy,
                    MetadataService.class,
                    metadataService,
                    URL.valueOf("tri://127.0.0.1:" + port + "/" + MetadataService.class.getName()
                            + "?filter=-default&group=provider-app&version=1.0.0")));
            exporters.add(XdsRoutingTest.export(
                    provider,
                    providerProtocol,
                    providerProxy,
                    Business.class,
                    implementation,
                    URL.valueOf("tri://127.0.0.1:" + port + "/" + Business.class.getName())));
            registry = new XdsRegistry(ads.registry(application), application);
            URL reference = URL.valueOf(
                            "tri://localhost/" + Business.class.getName()
                                    + "?provided-by=echo&provider-port=7070&provider-namespace=test&protocol=tri&xds.metadata.application=provider-app&timeout=2000")
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
            await(() -> directory.getAllInvokers().size() == 1);
            consumer = application
                    .getExtensionLoader(Cluster.class)
                    .getAdaptiveExtension()
                    .join(directory, true);
            remote = application
                    .getExtensionLoader(ProxyFactory.class)
                    .getAdaptiveExtension()
                    .getProxy(consumer);
        }

        void routes(RouteAction.Builder action) throws Exception {
            RouteConfiguration route = RouteConfiguration.newBuilder()
                    .setName("routes")
                    .addVirtualHosts(VirtualHost.newBuilder()
                            .addDomains("echo.test.svc.cluster.local:7070")
                            .addRoutes(Route.newBuilder()
                                    .setMatch(RouteMatch.newBuilder().setPrefix("/"))
                                    .setRoute(action)))
                    .build();
            ads.routeOverride = route;
            ads.send(XdsResourceType.LDS.getTypeUrl());
            await(() -> NativeTlsAdapter.endpoint(
                            directory.getAllInvokers().get(0).getUrl())
                    .getSnapshot()
                    .getRoutes()
                    .get("routes")
                    .equals(route));
        }

        @Override
        public void close() throws Exception {
            consumer.destroy();
            registry.destroy();
            exporters.forEach(Exporter::unexport);
            providerProtocol.destroy();
            protocol.destroy();
            providerFramework.destroy();
            framework.destroy();
            ads.close();
            RpcContext.removeClientAttachment();
        }
    }

    private static final class Implementation implements Business {
        volatile long streamTimeout;
        final CountDownLatch asyncStarted = new CountDownLatch(1);
        final CountDownLatch cancelled = new CountDownLatch(1);

        public long observedTimeout() {
            return (Long) RpcContext.getServiceContext().getInvocation().get(TIMEOUT_KEY);
        }

        public String slow(long millis) {
            try {
                Thread.sleep(millis);
                return "ok";
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(error);
            }
        }

        public CompletableFuture<String> slowAsync(long millis) {
            CancellationContext cancellation = RpcContext.getCancellationContext();
            cancellation.addListener(ignored -> {
                if (cancellation.getCancellationCause() != null) {
                    cancelled.countDown();
                }
            });
            asyncStarted.countDown();
            return CompletableFuture.supplyAsync(() -> slow(millis));
        }

        public void watch(String value, StreamObserver<String> observer) {
            streamTimeout = observedTimeout();
            observer.onNext(value);
        }

        public StreamObserver<String> chat(StreamObserver<String> observer) {
            return new StreamObserver<String>() {
                public void onNext(String value) {
                    observer.onNext(value);
                }

                public void onError(Throwable error) {}

                public void onCompleted() {
                    observer.onCompleted();
                }
            };
        }
    }

    private static final class RecordingObserver implements StreamObserver<String> {
        final BlockingQueue<Object> events = new LinkedBlockingQueue<>();

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

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean(), "Timed out waiting for deadline policy");
    }
}
