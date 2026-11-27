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
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.client.InstanceAddressURL;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistryDirectory;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.support.RegistryManager;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.xds.client.XdsResourceType;

import javax.net.ssl.SSLHandshakeException;

import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.UInt32Value;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Address;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.AggregatedConfigSource;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.ApiVersion;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.ConfigSource;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.HttpProtocolOptions;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.Endpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.ApiListener;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Listener;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.WeightedCluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.router.v3.Router;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XdsServiceDiscoveryTest {
    @Test
    void normalServiceRegistryDispatcherLoadsTheNativeXdsRegistry() throws Exception {
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig("xds-test"));
        try (Ads ads = new Ads()) {
            URL url = ads.registry(application)
                    .setProtocol("service-discovery-registry")
                    .addParameter("registry", "xds");
            Registry registry = application
                    .getExtensionLoader(RegistryFactory.class)
                    .getExtension("service-discovery-registry", false)
                    .getRegistry(url);
            assertInstanceOf(XdsRegistry.class, registry);
            assertEquals(
                    1,
                    application
                            .getBeanFactory()
                            .getBean(RegistryManager.class)
                            .getServiceDiscoveries()
                            .size());
            registry.destroy();
        } finally {
            framework.destroy();
        }
    }

    @Test
    void metadataAndBusinessCallsUseRealTripleThroughTheNativeRegistryAndDirectory() throws Exception {
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig("xds-test"));
        Protocol protocol = application.getExtensionLoader(Protocol.class).getExtension("tri");
        try (Ads ads = new Ads()) {
            ads.workloadPort = NetUtils.getAvailablePort();
            ads.clusters = Arrays.asList(
                    "outbound|7070|blue|echo.test.svc.cluster.local",
                    "outbound|7070|green|echo.test.svc.cluster.local");
            MetadataInfo metadata = new MetadataInfo("provider-app");
            MetadataInfo.ServiceInfo service = new MetadataInfo.ServiceInfo(
                    Business.class.getName(),
                    null,
                    null,
                    "tri",
                    ads.workloadPort,
                    Business.class.getName(),
                    Collections.emptyMap());
            metadata.getServices().put(service.getMatchKey(), service);
            MetadataService metadataService = mock(MetadataService.class);
            when(metadataService.getMetadataInfos()).thenReturn(Collections.singletonList(metadata));
            URL url = URL.valueOf("tri://127.0.0.1:" + ads.workloadPort + "/" + MetadataService.class.getName()
                            + "?group=provider-app&version=1.0.0&filter=-default")
                    .setScopeModel(application.getInternalModule());
            ServiceDescriptor descriptor =
                    application.getInternalModule().getServiceRepository().registerService(MetadataService.class);
            ProviderModel provider = new ProviderModel(
                    url.getServiceKey(),
                    metadataService,
                    descriptor,
                    new ServiceMetadata(),
                    ClassUtils.getClassLoader(MetadataService.class));
            application.getInternalModule().getServiceRepository().registerProvider(provider);
            url = url.setServiceModel(provider);
            ProxyFactory proxy =
                    application.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
            Exporter<MetadataService> exporter =
                    protocol.export(proxy.getInvoker(metadataService, MetadataService.class, url));
            Business business = value -> "hello " + value;
            URL businessUrl = URL.valueOf("tri://127.0.0.1:" + ads.workloadPort + "/" + Business.class.getName())
                    .setScopeModel(application.getInternalModule());
            ServiceDescriptor businessDescriptor =
                    application.getInternalModule().getServiceRepository().registerService(Business.class);
            ProviderModel businessProvider = new ProviderModel(
                    businessUrl.getServiceKey(),
                    business,
                    businessDescriptor,
                    new ServiceMetadata(),
                    ClassUtils.getClassLoader(Business.class));
            application.getInternalModule().getServiceRepository().registerProvider(businessProvider);
            businessUrl = businessUrl.setServiceModel(businessProvider);
            Exporter<Business> businessExporter =
                    protocol.export(proxy.getInvoker(business, Business.class, businessUrl));
            XdsRegistry registry = new XdsRegistry(ads.registry(application), application);
            URL consumer = reference(application, Business.class.getName())
                    .addParameter("interface", Business.class.getName())
                    .addParameter("retries", "0")
                    .setScopeModel(application.getInternalModule());
            consumer = consumer.setServiceModel(application
                    .getInternalModule()
                    .registerInternalConsumer(Business.class, consumer, businessDescriptor));
            URL directoryUrl = ads.registry(application)
                    .setPath(Business.class.getName())
                    .setScopeModel(application.getInternalModule())
                    .setServiceModel(consumer.getServiceModel())
                    .addParameterAndEncoded("refer", consumer.toParameterString());
            ServiceDiscoveryRegistryDirectory<Business> directory =
                    new ServiceDiscoveryRegistryDirectory<>(Business.class, directoryUrl);
            directory.setRegistry(registry);
            directory.setProtocol(protocol);
            directory.buildRouterChain(consumer);
            try {
                directory.subscribe(consumer);
                ads.endpoints("127.0.0.1");
                await(() -> directory.getAllInvokers().size() == 2);
                assertEquals(
                        directory.getAllInvokers().get(0).getUrl().getAddress(),
                        directory.getAllInvokers().get(1).getUrl().getAddress());
                assertNotEquals(
                        ((InstanceAddressURL) directory.getAllInvokers().get(0).getUrl())
                                .getInstance()
                                .getAddressIdentity(),
                        ((InstanceAddressURL) directory.getAllInvokers().get(1).getUrl())
                                .getInstance()
                                .getAddressIdentity());
                Business remote = proxy.getProxy(application
                        .getExtensionLoader(org.apache.dubbo.rpc.cluster.Cluster.class)
                        .getAdaptiveExtension()
                        .join(directory, false));
                assertEquals("hello xds", remote.hello("xds"));
                assertEquals(
                        ads.workloadPort,
                        directory.getAllInvokers().get(0).getUrl().getPort());
                ads.endpoints();
                await(() -> directory.getAllInvokers().isEmpty());
            } finally {
                directory.destroy();
                registry.destroy();
                businessExporter.unexport();
                exporter.unexport();
            }

        } finally {
            protocol.destroy();
            framework.destroy();
        }
    }

    @Test
    void eachInstanceSuppliesItsOwnMetadataWithoutRevisionsAndEmptyRemovesAddresses() throws Exception {
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig("xds-test"));
        try (Ads ads = new Ads()) {
            AtomicInteger reads = new AtomicInteger();
            XdsServiceDiscovery discovery = new XdsServiceDiscovery(
                    application, ads.registry(application), (endpoint, metadataEndpoint, options) -> {
                        reads.incrementAndGet();
                        assertEquals("provider-app", options.application);
                        MetadataInfo info = metadata(endpoint.getHost().endsWith("1") ? "First" : "Second");
                        info.getServices()
                                .put(
                                        "wrongProtocol",
                                        new MetadataInfo.ServiceInfo(
                                                "Wrong", null, null, "dubbo", 17070, "Wrong", Collections.emptyMap()));
                        info.getServices()
                                .put(
                                        "wrongPort",
                                        new MetadataInfo.ServiceInfo(
                                                "WrongPort",
                                                null,
                                                null,
                                                "tri",
                                                18080,
                                                "WrongPort",
                                                Collections.emptyMap()));
                        return info;
                    });
            try {
                URL reference = reference(application, "First");
                URL binding = discovery.bindReference(reference);
                assertEquals("tri://echo.test.svc.cluster.local:7070", binding.getParameter("provided-by"));
                assertThrows(
                        IllegalArgumentException.class,
                        () -> discovery.bindReference(reference.addParameter("xds.metadata.application", "other")));
                ServiceInstancesChangedListener listener =
                        discovery.createListener(Collections.singleton(binding.getParameter("provided-by")));
                BlockingQueue<List<URL>> notifications = new LinkedBlockingQueue<>();
                listener.addListenerAndNotify(reference, notifyListener(reference, notifications));
                discovery.addServiceInstancesChangedListener(listener);
                ads.endpoints("127.0.0.1", "127.0.0.2");
                await(() -> discovery
                                .getInstances(binding.getParameter("provided-by"))
                                .size()
                        == 2);
                assertEquals(2, reads.get());
                List<URL> addresses = takeMatching(notifications, false);
                assertEquals(1, addresses.size());
                assertEquals("127.0.0.1", addresses.get(0).getHost());
                assertEquals(17070, addresses.get(0).getPort());
                assertEquals("tri", addresses.get(0).getProtocol());
                assertNotNull(addresses.get(0).getAttribute(XdsEndpoint.ATTRIBUTE));
                assertNull(((InstanceAddressURL) addresses.get(0))
                        .getInstance()
                        .getMetadata()
                        .get("dubbo.metadata.revision"));
                discovery.bindReference(reference.setPath("Second"));
                assertEquals(2, reads.get());
                ads.endpoints();
                await(() -> discovery
                        .getInstances(binding.getParameter("provided-by"))
                        .isEmpty());
                assertEquals("empty", takeMatching(notifications, true).get(0).getProtocol());
                listener.destroy();
                await(() -> discovery.getServices().isEmpty());
            } finally {
                discovery.destroy();
            }
        } finally {
            framework.destroy();
        }
    }

    @Test
    void metadataCompletingAfterRemovalCannotResurrectAnEndpoint() throws Exception {
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig("xds-test"));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        try (Ads ads = new Ads()) {
            XdsServiceDiscovery discovery = new XdsServiceDiscovery(
                    application, ads.registry(application), (endpoint, metadataEndpoint, options) -> {
                        started.countDown();
                        while (true) {
                            try {
                                finish.await();
                                break;
                            } catch (InterruptedException ignored) {
                                /* Simulate a transport completing after cancellation. */
                            }
                        }
                        completed.countDown();
                        return metadata("First");
                    });
            try {
                URL bound = discovery.bindReference(reference(application, "First"));
                ads.endpoints("127.0.0.1");
                assertTrue(started.await(5, TimeUnit.SECONDS));
                ads.endpoints();
                assertTrue(ads.emptyAccepted.await(5, TimeUnit.SECONDS));
                await(() -> status(discovery, bound.getParameter("provided-by")).getDiscoveredEndpoints() == 0);
                finish.countDown();
                assertTrue(completed.await(5, TimeUnit.SECONDS));
                await(() -> discovery.getDiscardedMetadataResults() == 1);
                assertTrue(discovery
                        .getInstances(bound.getParameter("provided-by"))
                        .isEmpty());
            } finally {
                finish.countDown();
                discovery.destroy();
            }
        } finally {
            framework.destroy();
        }
    }

    @Test
    void aFailedRefreshKeepsOnlyUnexpiredSameInstanceMetadata() throws Exception {
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig("xds-test"));
        try (Ads ads = new Ads()) {
            AtomicInteger reads = new AtomicInteger();
            URL registry = ads.registry(application)
                    .addParameter("xds.metadata.ttl", "1500")
                    .addParameter("xds.metadata.refresh", "500");
            XdsServiceDiscovery discovery =
                    new XdsServiceDiscovery(application, registry, (endpoint, metadataEndpoint, options) -> {
                        if (reads.incrementAndGet() > 1) {
                            throw new IllegalStateException("metadata unavailable");
                        }
                        return metadata("First");
                    });
            try {
                URL bound = discovery.bindReference(reference(application, "First"));
                String target = bound.getParameter("provided-by");
                ads.endpoints("127.0.0.1");
                await(() -> discovery.getInstances(target).size() == 1);
                await(() -> reads.get() >= 2);
                assertEquals(1, discovery.getInstances(target).size());
                await(() -> discovery.getInstances(target).isEmpty());
                assertEquals(1, status(discovery, target).getDiscoveredEndpoints());
                assertNotNull(status(discovery, target).getLastError());
            } finally {
                discovery.destroy();
            }
        } finally {
            framework.destroy();
        }
    }

    @Test
    void targetIncludesProtocolPortAndCanonicalKubernetesHostname() {
        assertEquals(
                new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "tri"),
                DiscoveryTarget.kubernetes("echo.test", "unused", "cluster.local", 7070, "tri"));
        assertNotEquals(
                new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "tri"),
                new DiscoveryTarget("echo.test.svc.cluster.local", 20880, "dubbo"));
        assertNotEquals(
                new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "tri"),
                new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "dubbo"));
        assertNotEquals(
                new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "tri"),
                new DiscoveryTarget("echo.test.svc.cluster.local", 7071, "tri"));
        assertThrows(
                IllegalArgumentException.class,
                () -> DiscoveryTarget.kubernetes("echo", "", "cluster.local", 7070, "tri"));
    }

    public interface Business {
        String hello(String value);
    }

    @Test
    void oneInstanceSuccessCannotEraseAnotherInstancesMetadataDenial() throws Exception {
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig("xds-diagnostics"));
        AtomicBoolean denied = new AtomicBoolean(true);
        try (Ads ads = new Ads()) {
            XdsServiceDiscovery discovery = new XdsServiceDiscovery(
                    application, ads.registry(application), (endpoint, metadataEndpoint, options) -> {
                        if (endpoint.getHost().endsWith("1") && denied.get())
                            throw TriRpcStatus.fromCode(TriRpcStatus.Code.PERMISSION_DENIED)
                                    .asException();
                        return metadata("First");
                    });
            try {
                URL reference = reference(application, "First");
                discovery.bindReference(reference);
                String target = discovery.getServices().iterator().next();
                ads.endpoints("127.0.0.1", "127.0.0.2");
                await(() -> status(discovery, target).getMetadataFailures() > 0
                        && status(discovery, target).getMetadataSuccesses() > 0);
                XdsServiceDiscovery.TargetStatus failed = status(discovery, target);
                assertEquals(1, failed.getAvailableInstances());
                assertEquals(1, failed.getMetadataFailureStatuses().size());
                assertTrue(failed.getMetadataFailureStatuses().containsValue("PERMISSION_DENIED"));
                assertThrows(UnsupportedOperationException.class, () -> failed.getMetadataFailureStatuses()
                        .clear());
                denied.set(false);
                await(() -> status(discovery, target).getAvailableInstances() == 2);
                assertTrue(
                        status(discovery, target).getMetadataFailureStatuses().isEmpty());
                assertTrue(status(discovery, target).getMetadataFailureCauses().isEmpty());
                assertTrue(status(discovery, target).getMetadataFailures() >= failed.getMetadataFailures());
                assertTrue(status(discovery, target).getMetadataSuccesses() > failed.getMetadataSuccesses());
            } finally {
                discovery.destroy();
            }
        } finally {
            framework.destroy();
        }
    }

    @Test
    void metadataTlsCausesAreSafeIsolatedImmutableAndClearedAfterRecovery() throws Exception {
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig("xds-diagnostics"));
        AtomicBoolean failed = new AtomicBoolean(true);
        SSLHandshakeException tls = new SSLHandshakeException("credential-secret");
        tls.initCause(new KeyStoreException("private-key-secret"));
        RuntimeException wrapped = new RuntimeException("metadata-payload-secret", tls);
        try (Ads ads = new Ads()) {
            XdsServiceDiscovery discovery = new XdsServiceDiscovery(
                    application, ads.registry(application), (endpoint, metadataEndpoint, options) -> {
                        if (endpoint.getHost().endsWith("1") && failed.get()) throw wrapped;
                        return metadata("First");
                    });
            try {
                discovery.bindReference(reference(application, "First"));
                String target = discovery.getServices().iterator().next();
                ads.endpoints("127.0.0.1", "127.0.0.2");
                await(() -> status(discovery, target).getMetadataFailures() > 0
                        && status(discovery, target).getMetadataSuccesses() > 0);
                XdsServiceDiscovery.TargetStatus before = status(discovery, target);
                assertEquals(1, before.getAvailableInstances());
                assertEquals(1, before.getMetadataFailureCauses().size());
                String identity =
                        before.getMetadataFailureStatuses().keySet().iterator().next();
                List<String> causes = before.getMetadataFailureCauses().get(identity);
                assertEquals(
                        Arrays.asList(
                                RuntimeException.class.getName(),
                                SSLHandshakeException.class.getName(),
                                KeyStoreException.class.getName()),
                        causes);
                assertFalse(before.getMetadataFailureCauses().toString().contains("secret"));
                assertThrows(UnsupportedOperationException.class, () -> before.getMetadataFailureCauses()
                        .clear());
                assertThrows(UnsupportedOperationException.class, causes::clear);
                failed.set(false);
                await(() -> status(discovery, target).getAvailableInstances() == 2);
                XdsServiceDiscovery.TargetStatus recovered = status(discovery, target);
                assertTrue(recovered.getMetadataFailureStatuses().isEmpty());
                assertTrue(recovered.getMetadataFailureCauses().isEmpty());
                assertEquals(before.getMetadataFailures(), recovered.getMetadataFailures());
                assertTrue(recovered.getMetadataSuccesses() > before.getMetadataSuccesses());
                assertEquals(3, causes.size());
                ads.endpoints();
                await(() -> status(discovery, target).getDiscoveredEndpoints() == 0);
                failed.set(true);
                ads.endpoints("127.0.0.1");
                await(() ->
                        !status(discovery, target).getMetadataFailureCauses().isEmpty());
                ads.endpoints();
                await(() -> status(discovery, target).getDiscoveredEndpoints() == 0);
                assertTrue(status(discovery, target).getMetadataFailureCauses().isEmpty());
            } finally {
                discovery.destroy();
            }
        } finally {
            framework.destroy();
        }
    }

    @Test
    void metadataCauseDiagnosticsBoundDeepChainsAndStopCycles() {
        Throwable deep = new KeyStoreException("private-key-secret");
        for (int i = 0; i < 20; i++) deep = new RuntimeException("credential-secret", deep);
        assertEquals(8, XdsServiceDiscovery.failureCauses(deep).size());
        RuntimeException first = new RuntimeException("metadata-payload-secret");
        RuntimeException second = new RuntimeException("credential-secret");
        first.initCause(second);
        second.initCause(first);
        assertEquals(
                Arrays.asList(RuntimeException.class.getName(), RuntimeException.class.getName()),
                XdsServiceDiscovery.failureCauses(first));
        assertTrue(XdsServiceDiscovery.failureCauses(null).isEmpty());
    }

    private static MetadataInfo metadata(String name) {
        MetadataInfo info = new MetadataInfo("provider-app");
        MetadataInfo.ServiceInfo service =
                new MetadataInfo.ServiceInfo(name, null, null, "tri", 17070, name, Collections.emptyMap());
        info.getServices().put(service.getMatchKey(), service);
        info.init();
        return info;
    }

    private static URL reference(ApplicationModel app, String name) {
        return URL.valueOf(
                        "consumer://localhost/" + name
                                + "?provided-by=echo&provider-port=7070&provider-namespace=test&protocol=tri&xds.metadata.application=provider-app")
                .setScopeModel(app);
    }

    private static NotifyListener notifyListener(URL consumer, BlockingQueue<List<URL>> queue) {
        return new NotifyListener() {
            public void notify(List<URL> urls) {
                queue.add(urls);
            }

            public URL getConsumerUrl() {
                return consumer;
            }
        };
    }

    private static List<URL> takeMatching(BlockingQueue<List<URL>> queue, boolean empty) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            List<URL> urls = queue.poll(100, TimeUnit.MILLISECONDS);
            if (urls != null
                    && !urls.isEmpty()
                    && empty == "empty".equals(urls.get(0).getProtocol())) {
                return urls;
            }
        }
        throw new AssertionError("No matching address notification");
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean());
    }

    private static XdsServiceDiscovery.TargetStatus status(XdsServiceDiscovery discovery, String target) {
        try {
            return discovery.inspectTargets().get(5, TimeUnit.SECONDS).get(target);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    static class Ads extends AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceImplBase
            implements AutoCloseable {
        final Server server;
        final Map<String, List<String>> names = new ConcurrentHashMap<>();
        final AtomicInteger version = new AtomicInteger();
        final AtomicInteger rejected = new AtomicInteger();
        volatile String lastRejection;
        final CountDownLatch emptyAccepted = new CountDownLatch(1);
        volatile StreamObserver<DiscoveryResponse> response;
        volatile ClusterLoadAssignment endpoints;
        int workloadPort = 17070;
        volatile RouteConfiguration routeOverride;
        volatile Duration listenerTimeout;
        final Map<String, Integer> clusterPorts = new ConcurrentHashMap<>();
        final Map<String, RouteConfiguration> listenerRoutes = new ConcurrentHashMap<>();
        final Map<String, RouteConfiguration> routeResources = new ConcurrentHashMap<>();
        final Map<String, Listener> listenerOverrides = new ConcurrentHashMap<>();
        final Map<String, Cluster> clusterOverrides = new ConcurrentHashMap<>();
        final Map<String, ClusterLoadAssignment> clusterAssignments = new ConcurrentHashMap<>();
        final Set<String> emptyClusters = ConcurrentHashMap.newKeySet();
        List<String> clusters = Collections.singletonList("outbound|7070||echo.test.svc.cluster.local");

        Ads(BindableService... additional) throws Exception {
            NettyServerBuilder builder = NettyServerBuilder.forPort(0).addService(this);
            for (BindableService service : additional) builder.addService(service);
            server = builder.build().start();
        }

        URL registry(ApplicationModel application) {
            String bootstrap = "{\"xds_servers\":[{\"server_uri\":\"localhost:" + server.getPort()
                    + "\",\"channel_creds\":[{\"type\":\"insecure\"}],\"server_features\":[\"xds_v3\"]}],\"node\":{\"id\":\"test\"}}";
            return URL.valueOf("xds://localhost?file-cache=false")
                    .addParameter("xds.bootstrap.config", bootstrap)
                    .setScopeModel(application);
        }

        @Override
        public StreamObserver<DiscoveryRequest> streamAggregatedResources(StreamObserver<DiscoveryResponse> observer) {
            response = observer;
            return new StreamObserver<DiscoveryRequest>() {
                public void onNext(DiscoveryRequest request) {
                    if (request.hasErrorDetail()) {
                        rejected.incrementAndGet();
                        lastRejection = request.getErrorDetail().getMessage();
                    }
                    if ("empty".equals(request.getResponseNonce()) && !request.hasErrorDetail()) {
                        emptyAccepted.countDown();
                    }
                    List<String> old = names.put(request.getTypeUrl(), new ArrayList<>(request.getResourceNamesList()));
                    if (old == null || !old.equals(request.getResourceNamesList())) {
                        send(request.getTypeUrl());
                    }
                }

                public void onError(Throwable error) {
                    released(observer);
                }

                public void onCompleted() {
                    released(observer);
                    observer.onCompleted();
                }
            };
        }

        private synchronized void released(StreamObserver<DiscoveryResponse> observer) {
            if (response == observer) {
                // Stream cancellation releases every subscription even if its final request was not delivered.
                names.replaceAll((type, subscriptions) -> Collections.emptyList());
                response = null;
            }
        }

        synchronized void endpoints(String... hosts) {
            LocalityLbEndpoints.Builder locality = LocalityLbEndpoints.newBuilder();
            for (String host : hosts) {
                locality.addLbEndpoints(LbEndpoint.newBuilder()
                        .setEndpoint(Endpoint.newBuilder()
                                .setAddress(Address.newBuilder()
                                        .setSocketAddress(SocketAddress.newBuilder()
                                                .setAddress(host)
                                                .setPortValue(workloadPort)))));
            }
            endpoints = ClusterLoadAssignment.newBuilder()
                    .setClusterName("outbound|7070||echo.test.svc.cluster.local")
                    .addEndpoints(locality)
                    .build();
            send(XdsResourceType.EDS.getTypeUrl());
        }

        synchronized void send(String type) {
            if (response == null || names.get(type) == null || names.get(type).isEmpty()) {
                return;
            }
            DiscoveryResponse.Builder update = DiscoveryResponse.newBuilder()
                    .setTypeUrl(type)
                    .setVersionInfo(Integer.toString(version.incrementAndGet()))
                    .setNonce(type);
            String clusterName = clusters.get(0);
            RouteAction.Builder action = RouteAction.newBuilder();
            if (clusters.size() == 1) {
                action.setCluster(clusterName);
            } else {
                WeightedCluster.Builder weights = WeightedCluster.newBuilder();
                for (String name : clusters) {
                    weights.addClusters(WeightedCluster.ClusterWeight.newBuilder()
                            .setName(name)
                            .setWeight(UInt32Value.of(1)));
                }
                action.setWeightedClusters(weights);
            }
            if (type.equals(XdsResourceType.LDS.getTypeUrl())) {
                RouteConfiguration route = RouteConfiguration.newBuilder()
                        .setName("routes")
                        .addVirtualHosts(VirtualHost.newBuilder()
                                .setName("service")
                                .addDomains("*")
                                .addRoutes(Route.newBuilder()
                                        .setMatch(RouteMatch.newBuilder().setPrefix("/"))
                                        .setRoute(action)))
                        .build();
                if (routeOverride != null) {
                    route = routeOverride;
                }
                for (String name : names.get(type)) {
                    if (listenerOverrides.containsKey(name)) {
                        update.addResources(Any.pack(listenerOverrides.get(name)));
                        continue;
                    }
                    RouteConfiguration selected = listenerRoutes.get(name);
                    if (selected == null && name.equals("echo.test.svc.cluster.local:7070")) {
                        selected = route;
                    }
                    if (selected == null) {
                        continue;
                    }
                    HttpConnectionManager.Builder hcm = HttpConnectionManager.newBuilder()
                            .setRouteConfig(selected)
                            .addHttpFilters(HttpFilter.newBuilder()
                                    .setName("envoy.filters.http.router")
                                    .setTypedConfig(Any.pack(Router.getDefaultInstance())));
                    if (listenerTimeout != null) {
                        hcm.setCommonHttpProtocolOptions(
                                HttpProtocolOptions.newBuilder().setMaxStreamDuration(listenerTimeout));
                    }
                    update.addResources(Any.pack(Listener.newBuilder()
                            .setName(name)
                            .setApiListener(ApiListener.newBuilder().setApiListener(Any.pack(hcm.build())))
                            .build()));
                }
            } else if (type.equals(XdsResourceType.RDS.getTypeUrl())) {
                for (String name : names.get(type)) {
                    RouteConfiguration configured = routeResources.get(name);
                    if (configured != null) update.addResources(Any.pack(configured));
                }
            } else if (type.equals(XdsResourceType.CDS.getTypeUrl())) {
                for (String name : clusters) {
                    if (clusterOverrides.containsKey(name)) {
                        update.addResources(Any.pack(clusterOverrides.get(name)));
                        continue;
                    }
                    update.addResources(Any.pack(Cluster.newBuilder()
                            .setName(name)
                            .setType(Cluster.DiscoveryType.EDS)
                            .setEdsClusterConfig(Cluster.EdsClusterConfig.newBuilder()
                                    .setEdsConfig(ConfigSource.newBuilder()
                                            .setAds(AggregatedConfigSource.getDefaultInstance())
                                            .setResourceApiVersion(ApiVersion.V3)))
                            .build()));
                }
            } else if (type.equals(XdsResourceType.EDS.getTypeUrl()) && endpoints != null) {
                for (String name : clusters) {
                    ClusterLoadAssignment.Builder assignment =
                            clusterAssignments.getOrDefault(name, endpoints).toBuilder()
                                    .setClusterName(name);
                    if (clusterPorts.containsKey(name)) {
                        LocalityLbEndpoints.Builder locality = assignment.getEndpoints(0).toBuilder();
                        for (int i = 0; i < locality.getLbEndpointsCount(); i++) {
                            LbEndpoint original = locality.getLbEndpoints(i);
                            SocketAddress socket =
                                    original.getEndpoint().getAddress().getSocketAddress();
                            locality.setLbEndpoints(
                                    i,
                                    original.toBuilder()
                                            .setEndpoint(original.getEndpoint().toBuilder()
                                                    .setAddress(Address.newBuilder()
                                                            .setSocketAddress(socket.toBuilder()
                                                                    .setPortValue(clusterPorts.get(name))))));
                        }
                        assignment.setEndpoints(0, locality);
                    }
                    if (emptyClusters.contains(name)) {
                        assignment.clearEndpoints();
                    }
                    update.addResources(Any.pack(assignment.build()));
                }
                if (endpoints.getEndpoints(0).getLbEndpointsCount() == 0) {
                    update.setNonce("empty");
                }
            } else {
                return;
            }
            response.onNext(update.build());
        }

        public void close() throws Exception {
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
