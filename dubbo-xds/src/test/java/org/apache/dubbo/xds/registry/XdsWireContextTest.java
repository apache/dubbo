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
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.stream.ClientCallStreamObserver;
import org.apache.dubbo.common.stream.ClientResponseObserver;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.ClassUtils;
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
import org.apache.dubbo.rpc.ServerService;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.stub.StubSuppliers;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.fixture.DubboWireEchoTriple;
import org.apache.dubbo.xds.fixture.WireEcho;
import org.apache.dubbo.xds.fixture.WireMessage;
import org.apache.dubbo.xds.registry.WireHeaderRecorder.Capture;
import org.apache.dubbo.xds.registry.XdsServiceDiscoveryTest.Ads;
import org.apache.dubbo.xds.security.NativeTlsAdapter;
import org.apache.dubbo.xds.traffic.XdsFaultPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import com.google.protobuf.Any;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Listener;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.HeaderMatcher;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.fault.v3.HTTPFault;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XdsWireContextTest {
    private static final String BLUE = "outbound|7070|blue|echo.test.svc.cluster.local";
    private static final String GREEN = "outbound|7070|green|echo.test.svc.cluster.local";
    private static final String AUTHORITY = "echo.test.svc.cluster.local:7070";
    private static final String POJO_PATH = "custom.pojo.Wire";

    public interface Pojo {
        String hello(String value);

        CompletableFuture<String> asyncHello(String value);
    }

    @AfterEach
    void clearRequestContext() {
        // Completing a provider Future on this thread restores its filter callback context.
        RpcContext.removeContext();
    }

    @Test
    void generatedClientStreamQueuesBeforeDelayAndRejectsOkAbortWithoutAResponse() throws Exception {
        try (Fixture f = new Fixture()) {
            WireEcho remote = f.reference(WireEcho.class, "blue", "1.2.0", false);
            f.fault(XdsFaultExecutionTest.delay(300));
            WireRecording delayed = new WireRecording();
            StreamObserver<WireMessage> writer = remote.collect(delayed);
            assertSame(delayed.writer, writer);
            assertFalse(delayed.writer.isReady());
            writer.onNext(WireMessage.newBuilder().setValue("one").build());
            writer.onNext(WireMessage.newBuilder().setValue("two").build());
            writer.onCompleted();
            await(() -> XdsFaultPolicy.activeFaults() == 1);
            assertEquals(0, f.calls.get());
            assertTrue(delayed.done.await(2, TimeUnit.SECONDS));
            assertNull(delayed.error);
            assertEquals(Collections.singletonList("blue/blue@1.2.0:one,two"), delayed.messages);
            assertEquals(1, delayed.before.get());
            assertEquals(1, delayed.terminals.get());
            assertEquals(1, f.calls.get());
            for (int code : new int[] {0, 14}) {
                f.fault(XdsFaultExecutionTest.abort(code));
                WireRecording aborted = new WireRecording();
                remote.collect(aborted);
                assertTrue(aborted.done.await(1, TimeUnit.SECONDS));
                assertEquals(
                        code == 0 ? TriRpcStatus.Code.INTERNAL : TriRpcStatus.Code.UNAVAILABLE,
                        TriRpcStatus.getStatus(aborted.error).code);
                assertTrue(aborted.messages.isEmpty());
                assertEquals(1, aborted.before.get());
                assertEquals(1, aborted.terminals.get());
                assertEquals(1, f.calls.get());
            }
            await(() -> XdsFaultPolicy.activeFaults() == 0);
        }
    }

    @Test
    void deniedClientStreamDoesNotCancelAnAlreadyDispatchedAsyncUnary() throws Exception {
        try (Fixture f = new Fixture()) {
            WireEcho remote = f.reference(WireEcho.class, "blue", "1.2.0", false);
            CompletableFuture<WireMessage> held;
            try {
                held = remote.sayHelloAsync(
                        WireMessage.newBuilder().setValue("held-unary").build());
            } finally {
                RpcContext.removeContext();
            }
            assertTrue(f.heldStarted.await(3, TimeUnit.SECONDS));
            WireRecording denied = new WireRecording();
            try (Capture capture = tagged("denied-input-stream")) {
                capture.reject = true;
                remote.collect(denied)
                        .onNext(WireMessage.newBuilder()
                                .setValue("denied-input")
                                .build());
                assertEquals(
                        "/" + WireEcho.SERVICE_NAME + "/Collect",
                        capture.received().get(":path"));
            } finally {
                RpcContext.removeContext();
            }
            assertTrue(denied.done.await(3, TimeUnit.SECONDS));
            assertEquals(TriRpcStatus.Code.PERMISSION_DENIED, TriRpcStatus.getStatus(denied.error).code);
            assertEquals(1, denied.terminals.get());
            assertFalse(held.isDone(), "A rejected input stream terminated the earlier unary");
            assertFalse(f.heldContext.isCancelled(), "A rejected input stream cancelled the earlier provider");
            f.heldResponse.complete(
                    WireMessage.newBuilder().setValue("released-unary").build());
            assertEquals("released-unary", held.get(3, TimeUnit.SECONDS).getValue());
        }
    }

    @Test
    void deniedClientStreamDoesNotCancelAnAlreadyDispatchedPojoAsyncUnary() throws Exception {
        try (Fixture f = new Fixture()) {
            Pojo remote = f.reference(Pojo.class, "blue", "1.2.0", false);
            WireEcho input = f.reference(WireEcho.class, "blue", "1.2.0", false);
            CompletableFuture<String> held;
            try {
                held = remote.asyncHello("held-unary");
            } finally {
                RpcContext.removeContext();
            }
            assertTrue(f.heldPojoStarted.await(3, TimeUnit.SECONDS));
            WireRecording denied = new WireRecording();
            try (Capture capture = tagged("denied-input-pojo")) {
                capture.reject = true;
                input.collect(denied)
                        .onNext(WireMessage.newBuilder()
                                .setValue("denied-input")
                                .build());
                assertEquals(
                        "/" + WireEcho.SERVICE_NAME + "/Collect",
                        capture.received().get(":path"));
            } finally {
                RpcContext.removeContext();
            }
            assertTrue(denied.done.await(3, TimeUnit.SECONDS));
            assertEquals(TriRpcStatus.Code.PERMISSION_DENIED, TriRpcStatus.getStatus(denied.error).code);
            assertEquals(1, denied.terminals.get());
            assertFalse(held.isDone(), "A rejected input stream terminated the earlier POJO unary");
            assertFalse(f.heldPojoContext.isCancelled(), "A rejected input stream cancelled the earlier POJO provider");
            f.heldPojoResponse.complete("released-unary");
            assertEquals("released-unary", held.get(3, TimeUnit.SECONDS));
        }
    }

    private static final class WireRecording implements ClientResponseObserver<WireMessage, WireMessage> {
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicInteger before = new AtomicInteger();
        final AtomicInteger terminals = new AtomicInteger();
        final List<String> messages = new CopyOnWriteArrayList<>();
        volatile ClientCallStreamObserver<WireMessage> writer;
        volatile Throwable error;

        public void beforeStart(ClientCallStreamObserver<WireMessage> writer) {
            this.writer = writer;
            before.incrementAndGet();
            writer.disableAutoRequestWithInitial(1);
        }

        public void onNext(WireMessage value) {
            messages.add(value.getValue());
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

    @Test
    void generatedIdlUsesProtoServiceAndMethodNamesForSyncAndAsyncCalls() throws Exception {
        assertNotEquals(WireEcho.JAVA_SERVICE_NAME, WireEcho.SERVICE_NAME);
        try (Fixture fixture = new Fixture()) {
            WireEcho blue = fixture.reference(WireEcho.class, "blue", "1.2.0", false);
            WireEcho green = fixture.reference(WireEcho.class, "green", "2.0.0", false);
            WireMessage request = WireMessage.newBuilder().setValue("sync").build();
            try (Capture capture = tagged("idl-sync")) {
                assertEquals("blue/blue@1.2.0:sync", blue.sayHello(request).getValue());
                check(capture.received(), "/" + WireEcho.SERVICE_NAME + "/SayHello", "blue", "1.2.0");
            } finally {
                RpcContext.removeClientAttachment();
            }
            try (Capture capture = tagged("idl-async")) {
                assertEquals(
                        "green/green@2.0.0:async",
                        green.sayHelloAsync(WireMessage.newBuilder()
                                        .setValue("async")
                                        .build())
                                .get(5, TimeUnit.SECONDS)
                                .getValue());
                check(capture.received(), "/" + WireEcho.SERVICE_NAME + "/SayHello", "green", "2.0.0");
            } finally {
                RpcContext.removeClientAttachment();
            }
        }
    }

    @Test
    void pojoUsesExportedWirePathAndKeepsGroupAndVersionSeparate() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Pojo blue = fixture.reference(Pojo.class, "blue", "1.2.0", false);
            Pojo green = fixture.reference(Pojo.class, "green", "2.0.0", false);
            try (Capture capture = tagged("pojo-blue")) {
                assertEquals("blue/blue@1.2.0:one", blue.hello("one"));
                check(capture.received(), "/" + POJO_PATH + "/hello", "blue", "1.2.0");
            } finally {
                RpcContext.removeClientAttachment();
            }
            try (Capture capture = tagged("pojo-green")) {
                assertEquals("green/green@2.0.0:two", green.hello("two"));
                check(capture.received(), "/" + POJO_PATH + "/hello", "green", "2.0.0");
            } finally {
                RpcContext.removeClientAttachment();
            }
        }
    }

    @Test
    void unpublishedGroupOrVersionCannotUseAnotherReferencesInvokers() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Pojo available = fixture.reference(Pojo.class, "blue", "1.2.0", false);
            assertEquals("blue/blue@1.2.0:ready", available.hello("ready"));
            Pojo wrongGroup = fixture.reference(Pojo.class, "missing", "1.2.0", true);
            Pojo wrongVersion = fixture.reference(Pojo.class, "blue", "9.0.0", true);
            WireEcho wrongIdlVersion = fixture.reference(WireEcho.class, "green", "9.0.0", true);
            int before = fixture.calls.get();
            assertThrows(RpcException.class, () -> wrongGroup.hello("wrong-group"));
            assertThrows(RpcException.class, () -> wrongVersion.hello("wrong-version"));
            assertThrows(RpcException.class, () -> wrongIdlVersion.sayHello(WireMessage.getDefaultInstance()));
            assertEquals(before, fixture.calls.get());
            assertEquals("blue/blue@1.2.0:still-ready", available.hello("still-ready"));
        }
    }

    private static Capture tagged(String name) {
        Capture capture = WireHeaderRecorder.capture(name);
        RpcContext.getClientAttachment().setAttachment(WireHeaderRecorder.HEADER, name);
        return capture;
    }

    private static void check(Map<String, String> actual, String path, String group, String version) {
        assertEquals(path, actual.get(":path"));
        assertEquals(AUTHORITY, actual.get(":authority"));
        assertEquals("POST", actual.get(":method"));
        assertEquals("http", actual.get(":scheme"));
        assertEquals("application/grpc+proto", actual.get("content-type"));
        assertEquals(group, actual.get("tri-service-group"));
        assertEquals(version, actual.get("tri-service-version"));
    }

    private static RouteConfiguration routes() {
        VirtualHost.Builder host = VirtualHost.newBuilder().setName("wire").addDomains(AUTHORITY);
        for (String path : Arrays.asList(
                "/" + WireEcho.SERVICE_NAME + "/SayHello",
                "/" + POJO_PATH + "/hello",
                "/" + POJO_PATH + "/asyncHello")) {
            host.addRoutes(route(path, "blue", "1.2.0", BLUE));
            host.addRoutes(route(path, "green", "2.0.0", GREEN));
        }
        host.addRoutes(route("/" + WireEcho.SERVICE_NAME + "/Collect", "blue", "1.2.0", BLUE));
        host.addRoutes(route("/" + WireEcho.SERVICE_NAME + "/Collect", "green", "2.0.0", GREEN));
        return RouteConfiguration.newBuilder()
                .setName("routes")
                .addVirtualHosts(host)
                .build();
    }

    private static Route route(String path, String group, String version, String cluster) {
        return Route.newBuilder()
                .setMatch(RouteMatch.newBuilder()
                        .setPath(path)
                        .addHeaders(HeaderMatcher.newBuilder()
                                .setName("tri-service-group")
                                .setExactMatch(group))
                        .addHeaders(HeaderMatcher.newBuilder()
                                .setName("tri-service-version")
                                .setExactMatch(version)))
                .setRoute(RouteAction.newBuilder().setCluster(cluster))
                .build();
    }

    private static final class Fixture implements AutoCloseable {
        final FrameworkModel framework = new FrameworkModel();
        final ApplicationModel application = application(framework, "wire-consumer");
        final Protocol protocol = application.getExtensionLoader(Protocol.class).getExtension("tri");
        final ProxyFactory proxy =
                application.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        final Ads ads = new Ads();
        final List<Exporter<?>> exports = new ArrayList<>();
        final List<AutoCloseable> providers = new ArrayList<>();
        final List<Invoker<?>> consumers = new ArrayList<>();
        final List<ServiceDiscoveryRegistryDirectory<?>> directories = new ArrayList<>();
        final AtomicInteger calls = new AtomicInteger();
        final CompletableFuture<WireMessage> heldResponse = new CompletableFuture<>();
        final CountDownLatch heldStarted = new CountDownLatch(1);
        volatile CancellationContext heldContext;
        final CompletableFuture<String> heldPojoResponse = new CompletableFuture<>();
        final CountDownLatch heldPojoStarted = new CountDownLatch(1);
        volatile CancellationContext heldPojoContext;
        final XdsRegistry registry;

        Fixture() throws Exception {
            int blue = NetUtils.getAvailablePort();
            int green = NetUtils.getAvailablePort();
            while (green == blue) green = NetUtils.getAvailablePort();
            ads.clusters = Arrays.asList(BLUE, GREEN);
            ads.clusterPorts.put(BLUE, blue);
            ads.clusterPorts.put(GREEN, green);
            ads.routeOverride = routes();
            publish(blue, "blue");
            publish(green, "green");
            registry = new XdsRegistry(ads.registry(application), application);
            ads.endpoints("127.0.0.1");
        }

        <T> T reference(Class<T> type, String group, String version, boolean absent) throws Exception {
            boolean stub = type == WireEcho.class;
            String service = stub ? WireEcho.SERVICE_NAME : type.getName();
            ServiceDescriptor descriptor = stub
                    ? StubSuppliers.getServiceDescriptor(WireEcho.JAVA_SERVICE_NAME)
                    : application.getInternalModule().getServiceRepository().registerService(type);
            application.getInternalModule().getServiceRepository().registerService(descriptor);
            URL reference = URL.valueOf("tri://localhost/" + service)
                    .addParameter("interface", type.getName())
                    .addParameter("provided-by", "echo")
                    .addParameter("provider-port", "7070")
                    .addParameter("provider-namespace", "test")
                    .addParameter("protocol", "tri")
                    .addParameter("xds.metadata.application", "provider-app")
                    .addParameter("group", group)
                    .addParameter("version", version)
                    .addParameter("proxy", stub ? CommonConstants.NATIVE_STUB : "javassist")
                    .setScopeModel(application.getInternalModule());
            reference = reference.setServiceModel(
                    application.getInternalModule().registerInternalConsumer(type, reference, descriptor));
            URL directoryUrl = ads.registry(application)
                    .setPath(service)
                    .setScopeModel(application.getInternalModule())
                    .setServiceModel(reference.getServiceModel())
                    .addParameterAndEncoded("refer", reference.toParameterString());
            ServiceDiscoveryRegistryDirectory<T> directory =
                    new ServiceDiscoveryRegistryDirectory<>(type, directoryUrl);
            directory.setRegistry(registry);
            directory.setProtocol(protocol);
            directory.buildRouterChain(reference);
            directory.subscribe(reference);
            directories.add(directory);
            await(() -> registry.getServiceDiscovery()
                            .getInstances("tri://" + AUTHORITY)
                            .size()
                    == 2);
            if (!absent) await(() -> directory.getAllInvokers().size() == 2);
            else assertTrue(directory.getAllInvokers().isEmpty());
            Invoker<T> consumer = application
                    .getExtensionLoader(Cluster.class)
                    .getAdaptiveExtension()
                    .join(directory, true);
            consumers.add(consumer);
            return proxy.getProxy(consumer);
        }

        void fault(HTTPFault config) throws Exception {
            XdsEndpoint endpoint = NativeTlsAdapter.endpoint(
                    directories.get(0).getAllInvokers().get(0).getUrl());
            Listener previous = endpoint.getSnapshot().getListener();
            HttpConnectionManager hcm =
                    previous.getApiListener().getApiListener().unpack(HttpConnectionManager.class);
            HttpFilter router = hcm.getHttpFilters(hcm.getHttpFiltersCount() - 1);
            HttpConnectionManager next = hcm.toBuilder()
                    .clearHttpFilters()
                    .addHttpFilters(HttpFilter.newBuilder().setName("fault").setTypedConfig(Any.pack(config)))
                    .addHttpFilters(router)
                    .build();
            Listener listener = previous.toBuilder()
                    .setApiListener(previous.getApiListener().toBuilder().setApiListener(Any.pack(next)))
                    .build();
            ads.listenerOverrides.put(previous.getName(), listener);
            ads.send(XdsResourceType.LDS.getTypeUrl());
            await(() -> endpoint.getSnapshot().getListener().equals(listener));
        }

        void publish(int port, String endpoint) {
            FrameworkModel providerFramework = new FrameworkModel();
            ApplicationModel provider = application(providerFramework, "provider-app");
            Protocol providerProtocol =
                    provider.getExtensionLoader(Protocol.class).getExtension("tri");
            ProxyFactory providerProxy =
                    provider.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
            providers.add(() -> {
                providerProtocol.destroy();
                providerFramework.destroy();
            });
            MetadataInfo metadata = new MetadataInfo("provider-app");
            for (String group : Arrays.asList("blue", "green")) {
                String version = group.equals("blue") ? "1.2.0" : "2.0.0";
                String reply = endpoint + "/" + group + "@" + version + ":";
                DubboWireEchoTriple.WireEchoImplBase idl = new DubboWireEchoTriple.WireEchoImplBase() {
                    public WireMessage sayHello(WireMessage request) {
                        calls.incrementAndGet();
                        return WireMessage.newBuilder()
                                .setValue(reply + request.getValue())
                                .build();
                    }

                    @Override
                    public CompletableFuture<WireMessage> sayHelloAsync(WireMessage request) {
                        if (!"held-unary".equals(request.getValue()))
                            return CompletableFuture.completedFuture(sayHello(request));
                        heldContext = RpcContext.getCancellationContext();
                        heldStarted.countDown();
                        return heldResponse;
                    }

                    public StreamObserver<WireMessage> collect(StreamObserver<WireMessage> response) {
                        calls.incrementAndGet();
                        return new StreamObserver<WireMessage>() {
                            final List<String> values = new ArrayList<>();

                            public void onNext(WireMessage message) {
                                values.add(message.getValue());
                            }

                            public void onError(Throwable error) {
                                response.onError(error);
                            }

                            public void onCompleted() {
                                response.onNext(WireMessage.newBuilder()
                                        .setValue(reply + String.join(",", values))
                                        .build());
                                response.onCompleted();
                            }
                        };
                    }
                };
                URL idlUrl = providerUrl(port, WireEcho.SERVICE_NAME, group, version)
                        .addParameter("interface", WireEcho.JAVA_SERVICE_NAME)
                        .addParameter("proxy", CommonConstants.NATIVE_STUB)
                        .setScopeModel(provider.getInternalModule());
                exportStub(provider, providerProtocol, idl, idlUrl);
                metadata.addService(idlUrl);
                Pojo pojo = new Pojo() {
                    public String hello(String value) {
                        calls.incrementAndGet();
                        return reply + value;
                    }

                    public CompletableFuture<String> asyncHello(String value) {
                        if (!"held-unary".equals(value)) return CompletableFuture.completedFuture(hello(value));
                        heldPojoContext = RpcContext.getCancellationContext();
                        heldPojoStarted.countDown();
                        return heldPojoResponse;
                    }
                };
                URL pojoUrl = providerUrl(port, POJO_PATH, group, version)
                        .addParameter("interface", Pojo.class.getName())
                        .setScopeModel(provider.getInternalModule());
                exports.add(
                        XdsRoutingTest.export(provider, providerProtocol, providerProxy, Pojo.class, pojo, pojoUrl));
                metadata.addService(pojoUrl);
            }
            metadata.init();
            MetadataService service = mock(MetadataService.class);
            when(service.getMetadataInfos()).thenReturn(Collections.singletonList(metadata));
            exports.add(XdsRoutingTest.export(
                    provider,
                    providerProtocol,
                    providerProxy,
                    MetadataService.class,
                    service,
                    providerUrl(port, MetadataService.class.getName(), "provider-app", "1.0.0")
                            .addParameter("filter", "-default")));
        }

        void exportStub(
                ApplicationModel provider, Protocol providerProtocol, ServerService<WireEcho> service, URL url) {
            ServiceDescriptor descriptor = service.getServiceDescriptor();
            provider.getInternalModule().getServiceRepository().registerService(descriptor);
            ProviderModel model = new ProviderModel(
                    url.getServiceKey(),
                    service,
                    descriptor,
                    new ServiceMetadata(),
                    ClassUtils.getClassLoader(WireEcho.class));
            provider.getInternalModule().getServiceRepository().registerProvider(model);
            url = url.setScopeModel(provider.getInternalModule()).setServiceModel(model);
            exports.add(providerProtocol.export(service.getInvoker(url)));
        }

        public void close() throws Exception {
            RpcContext.removeClientAttachment();
            consumers.forEach(Invoker::destroy);
            registry.destroy();
            exports.forEach(Exporter::unexport);
            for (AutoCloseable provider : providers) provider.close();
            protocol.destroy();
            framework.destroy();
            ads.close();
        }
    }

    private static ApplicationModel application(FrameworkModel framework, String name) {
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig(name));
        return application;
    }

    private static URL providerUrl(int port, String path, String group, String version) {
        return URL.valueOf("tri://127.0.0.1:" + port + "/" + path)
                .addParameter("group", group)
                .addParameter("version", version);
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > until) fail("Timed out waiting for native discovery");
            Thread.sleep(10);
        }
    }
}
