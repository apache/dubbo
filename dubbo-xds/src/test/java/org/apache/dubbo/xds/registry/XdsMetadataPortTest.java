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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistryDirectory;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.registry.XdsServiceDiscoveryTest.Ads;
import org.apache.dubbo.xds.security.NativeSecurityTestSupport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Address;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.Endpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XdsMetadataPortTest {
    @TempDir
    Path certificateDirectory;

    private static final String BUSINESS = "outbound|7070||echo.test.svc.cluster.local";
    private static final String METADATA = "outbound|7071||echo.test.svc.cluster.local";

    public interface Business {
        String hello(String value);
    }

    @Test
    void metadataEntryIdentityIncludesItsServicePortAndProtocol() {
        URL registry = URL.valueOf("xds://localhost");
        URL reference = URL.valueOf("tri://localhost?xds.metadata.application=provider-app");
        DiscoveryTarget business = new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "dubbo");
        assertThrows(
                IllegalArgumentException.class,
                () -> new MetadataEntryOptions(
                        reference.addParameter("xds.metadata.protocol", "tri"), registry, business));
        MetadataEntryOptions options = new MetadataEntryOptions(
                reference
                        .addParameter("xds.metadata.protocol", "tri")
                        .addParameter("xds.metadata.service-port", "7071")
                        .addParameter("xds.metadata.service", "metadata"),
                registry,
                business);
        assertEquals(new DiscoveryTarget("metadata.test.svc.cluster.local", 7071, "tri"), options.target);
        assertNotEquals(options, new MetadataEntryOptions(reference, registry, business));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MetadataEntryOptions(
                        reference.addParameter("xds.metadata.service-port", "0"), registry, business));
    }

    @Test
    void onlyTheSameWorkloadMetadataEndpointIsQueriedAndAmbiguityIsIsolated() throws Exception {
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = application(framework, "metadata-test");
        try (Ads ads = new Ads()) {
            setup(ads, 18081, "10.0.0.2", "10.0.0.9");
            List<String> reads = new CopyOnWriteArrayList<>();
            XdsServiceDiscovery discovery =
                    new XdsServiceDiscovery(application, ads.registry(application), (business, metadata, options) -> {
                        assertEquals(business.getHost(), metadata.getHost());
                        assertEquals(17070, business.getPort());
                        assertEquals(METADATA, metadata.getCluster().getName());
                        reads.add(metadata.getHost() + ":" + metadata.getPort());
                        return metadata(business, metadata.getPort());
                    });
            NotifyListener owner = mock(NotifyListener.class);
            URL reference = reference(application);
            URL target = discovery.bindReference(reference, owner);
            String key = target.getParameter("provided-by");
            try {
                ads.endpoints("10.0.0.1", "10.0.0.2");
                await(() -> discovery.getInstances(key).size() == 1);
                assertEquals("10.0.0.2", discovery.getInstances(key).get(0).getHost());
                assertEquals(Collections.singletonList("10.0.0.2:18081"), reads);
                ClusterLoadAssignment duplicate = assignment(METADATA, 18081, "10.0.0.2").toBuilder()
                        .setEndpoints(
                                0,
                                LocalityLbEndpoints.newBuilder()
                                        .addLbEndpoints(endpoint("10.0.0.2", 18081))
                                        .addLbEndpoints(endpoint("10.0.0.2", 18082)))
                        .build();
                ads.clusterAssignments.put(METADATA, duplicate);
                ads.send(XdsResourceType.EDS.getTypeUrl());
                await(() -> discovery.getInstances(key).isEmpty());
                assertEquals(1, reads.size());
                assertEquals(
                        "MetadataEndpointAmbiguous",
                        discovery
                                .inspectTargets()
                                .get(1, TimeUnit.SECONDS)
                                .get(key)
                                .getLastError());
                ads.clusterAssignments.put(METADATA, assignment(METADATA, 18083, "10.0.0.1"));
                ads.send(XdsResourceType.EDS.getTypeUrl());
                await(() -> discovery.getInstances(key).size() == 1);
                assertEquals("10.0.0.1", discovery.getInstances(key).get(0).getHost());
                assertEquals(Arrays.asList("10.0.0.2:18081", "10.0.0.1:18083"), reads);
                discovery.releaseReference(target, owner);
                await(() -> discovery.getServices().isEmpty());
                await(() -> ads.names.get(XdsResourceType.LDS.getTypeUrl()).isEmpty());
            } finally {
                discovery.destroy();
            }
        } finally {
            framework.destroy();
        }
    }

    @Test
    void lateResultsFromThePreviousMetadataPortCannotReviveItsCache() throws Exception {
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = application(framework, "metadata-test");
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (Ads ads = new Ads()) {
            setup(ads, 18081, "10.0.0.1");
            XdsServiceDiscovery discovery =
                    new XdsServiceDiscovery(application, ads.registry(application), (business, metadata, options) -> {
                        if (metadata.getPort() == 18081) {
                            started.countDown();
                            while (true) {
                                try {
                                    release.await();
                                    break;
                                } catch (InterruptedException ignored) {
                                }
                            }
                        }
                        return metadata(business, metadata.getPort());
                    });
            String key = discovery.bindReference(reference(application)).getParameter("provided-by");
            try {
                ads.endpoints("10.0.0.1");
                assertTrue(started.await(2, TimeUnit.SECONDS));
                ads.clusterAssignments.put(METADATA, assignment(METADATA, 18082, "10.0.0.1"));
                ads.send(XdsResourceType.EDS.getTypeUrl());
                await(() -> discovery.getInstances(key).size() == 1);
                release.countDown();
                await(() -> discovery.getDiscardedMetadataResults() > 0);
                assertEquals(
                        "18082",
                        discovery
                                .getInstances(key)
                                .get(0)
                                .getServiceMetadata()
                                .getServices()
                                .values()
                                .iterator()
                                .next()
                                .getParams()
                                .get("source-port"));
            } finally {
                release.countDown();
                discovery.destroy();
            }
        } finally {
            framework.destroy();
        }
    }

    @Test
    void realMetadataOnASeparatePortBuildsBusinessInvokersWithoutUsingTheMetadataPort() throws Exception {
        realSeparatePorts(false);
    }

    @Test
    void separateMetadataMutualTlsAndRbacDoNotInheritThePlaintextBusinessPolicy() throws Exception {
        NativeSecurityTestSupport.writeIdentity(certificateDirectory);
        realSeparatePorts(true);
    }

    @Test
    void crossServiceMetadataUsesItsOwnApplicationGroupPortAndSecurityOnTheSameSocket() throws Exception {
        NativeSecurityTestSupport.writeIdentity(certificateDirectory);
        realSeparatePorts(true, true);
    }

    private void realSeparatePorts(boolean secured) throws Exception {
        realSeparatePorts(secured, false);
    }

    private void realSeparatePorts(boolean secured, boolean crossService) throws Exception {
        FrameworkModel framework = new FrameworkModel();
        FrameworkModel peerFramework = new FrameworkModel();
        FrameworkModel otherFramework = new FrameworkModel();
        ApplicationModel application = application(framework, "metadata-test");
        ApplicationModel peer = application(peerFramework, "provider-app");
        ApplicationModel other = application(otherFramework, "other-app");
        Protocol protocol = application.getExtensionLoader(Protocol.class).getExtension("tri");
        Protocol peerProtocol = peer.getExtensionLoader(Protocol.class).getExtension("tri");
        ProxyFactory peerProxy = peer.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol otherProtocol = other.getExtensionLoader(Protocol.class).getExtension("tri");
        ProxyFactory otherProxy = other.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        List<Exporter<?>> exporters = new ArrayList<>();
        Invoker<Business> consumer = null;
        XdsRegistry registry = null;
        ServiceDiscoveryRegistryDirectory<Business> directory = null;
        try (Ads ads = new Ads()) {
            int businessPort = NetUtils.getAvailablePort();
            int metadataPort = NetUtils.getAvailablePort();
            int otherBusinessPort = NetUtils.getAvailablePort();
            String otherBusiness = "outbound|8080||other.test.svc.cluster.local";
            String otherMetadata = "outbound|8081||other.test.svc.cluster.local";
            setup(ads, metadataPort, "127.0.0.1");
            ads.workloadPort = businessPort;
            URL registryUrl = ads.registry(application);
            URL metadataUrl = URL.valueOf("tri://127.0.0.1:" + metadataPort + "/" + MetadataService.class.getName()
                    + "?filter=-default&group=provider-app&version=1.0.0");
            String inbound = "xds.istio.io/grpc/lds/inbound/0.0.0.0:" + metadataPort;
            if (secured) {
                String config = NativeSecurityTestSupport.bootstrap(certificateDirectory, ads.server.getPort());
                registryUrl = registryUrl
                        .addParameter("xds.bootstrap.config", config)
                        .addParameter("xds.metadata.ttl", "500")
                        .addParameter("xds.metadata.refresh", "250");
                metadataUrl =
                        metadataUrl.addParameter("xds.enabled", true).addParameter("xds.bootstrap.config", config);
                ads.listenerOverrides.put(inbound, NativeSecurityTestSupport.listener(metadataPort, false));
                ads.clusterOverrides.put(METADATA, NativeSecurityTestSupport.cluster(METADATA, false));
            }
            MetadataService service = mock(MetadataService.class);
            MetadataInfo info = info(businessPort, metadataPort);
            AtomicInteger metadataCalls = new AtomicInteger();
            when(service.getMetadataInfos()).thenAnswer(ignored -> {
                metadataCalls.incrementAndGet();
                return Collections.singletonList(info);
            });
            exporters.add(
                    XdsRoutingTest.export(peer, peerProtocol, peerProxy, MetadataService.class, service, metadataUrl));
            exporters.add(XdsRoutingTest.export(
                    peer,
                    peerProtocol,
                    peerProxy,
                    Business.class,
                    value -> "business:" + value,
                    URL.valueOf("tri://127.0.0.1:" + businessPort + "/" + Business.class.getName())));
            registry = new XdsRegistry(registryUrl, application);
            URL reference = reference(application).addParameter("timeout", "2000");
            MetadataService otherService = mock(MetadataService.class);
            AtomicInteger otherCalls = new AtomicInteger();
            if (crossService) {
                ads.clusters = Arrays.asList(BUSINESS, METADATA, otherBusiness, otherMetadata);
                ads.clusterAssignments.put(otherBusiness, assignment(otherBusiness, otherBusinessPort, "127.0.0.1"));
                // Different Service identities intentionally resolve to the same metadata socket.
                ads.clusterAssignments.put(otherMetadata, assignment(otherMetadata, metadataPort, "127.0.0.1"));
                ads.listenerRoutes.put("other.test.svc.cluster.local:8081", route(otherMetadata));
                ads.clusterOverrides.put(otherMetadata, NativeSecurityTestSupport.cluster(otherMetadata, false));
                MetadataInfo otherInfo = new MetadataInfo("other-app");
                MetadataInfo.ServiceInfo published = new MetadataInfo.ServiceInfo(
                        Business.class.getName(),
                        null,
                        null,
                        "tri",
                        otherBusinessPort,
                        Business.class.getName(),
                        Collections.emptyMap());
                otherInfo.getServices().put(published.getMatchKey(), published);
                when(otherService.getMetadataInfos()).thenAnswer(ignored -> {
                    otherCalls.incrementAndGet();
                    return Collections.singletonList(otherInfo);
                });
                exporters.add(XdsRoutingTest.export(
                        peer,
                        peerProtocol,
                        peerProxy,
                        MetadataService.class,
                        otherService,
                        metadataUrl.addParameter("group", "other-metadata-group")));
                exporters.add(XdsRoutingTest.export(
                        other,
                        otherProtocol,
                        otherProxy,
                        Business.class,
                        value -> "other:" + value,
                        URL.valueOf("tri://127.0.0.1:" + otherBusinessPort + "/" + Business.class.getName())));
                reference = reference.addParameter(
                        MetadataEntryConfig.TARGETS,
                        "{\"tri://other.test.svc.cluster.local:8080\":{\"application\":\"other-app\","
                                + "\"group\":\"other-metadata-group\",\"service-port\":\"8081\"}}");
            }
            ServiceDescriptor descriptor =
                    application.getInternalModule().getServiceRepository().registerService(Business.class);
            reference = reference.setServiceModel(
                    application.getInternalModule().registerInternalConsumer(Business.class, reference, descriptor));
            URL directoryUrl = registryUrl
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
            ServiceDiscoveryRegistryDirectory<Business> current = directory;
            await(() -> current.getAllInvokers().size() == 1
                    && current.getAllInvokers().get(0).isAvailable());
            assertEquals(businessPort, current.getAllInvokers().get(0).getUrl().getPort());
            consumer = application
                    .getExtensionLoader(Cluster.class)
                    .getAdaptiveExtension()
                    .join(directory, true);
            Business remote = application
                    .getExtensionLoader(ProxyFactory.class)
                    .getAdaptiveExtension()
                    .getProxy(consumer);
            assertEquals("business:separate", remote.hello("separate"));
            verify(service, atLeastOnce()).getMetadataInfos();
            if (secured) {
                XdsServiceDiscovery discovery = (XdsServiceDiscovery) registry.getServiceDiscovery();
                ads.listenerOverrides.put(inbound, NativeSecurityTestSupport.listener(metadataPort, true));
                ads.send(XdsResourceType.LDS.getTypeUrl());
                await(() -> current.getAllInvokers().isEmpty() && metadataFailureRecorded(discovery));
                int denied = metadataCalls.get();
                ads.listenerOverrides.put(inbound, NativeSecurityTestSupport.listener(metadataPort, false));
                ads.send(XdsResourceType.LDS.getTypeUrl());
                await(() -> current.getAllInvokers().size() == 1
                        && current.getAllInvokers().get(0).isAvailable());
                assertTrue(metadataCalls.get() > denied);
                assertEquals("business:restored", remote.hello("restored"));
                ads.clusterOverrides.put(METADATA, NativeSecurityTestSupport.cluster(METADATA, true));
                ads.send(XdsResourceType.CDS.getTypeUrl());
                await(() -> current.getAllInvokers().isEmpty() && metadataFailureRecorded(discovery));
                int beforeRestore = metadataCalls.get();
                ads.clusterOverrides.put(METADATA, NativeSecurityTestSupport.cluster(METADATA, false));
                ads.send(XdsResourceType.CDS.getTypeUrl());
                await(() -> current.getAllInvokers().size() == 1
                        && current.getAllInvokers().get(0).isAvailable());
                assertTrue(metadataCalls.get() > beforeRestore);
                assertEquals("business:policy-isolated", remote.hello("policy-isolated"));
            }
            if (crossService) {
                XdsServiceDiscovery discovery = (XdsServiceDiscovery) registry.getServiceDiscovery();
                ads.routeOverride = route(otherBusiness);
                ads.send(XdsResourceType.LDS.getTypeUrl());
                await(() -> current.getAllInvokers().size() == 1
                        && current.getAllInvokers().get(0).getUrl().getPort() == otherBusinessPort
                        && current.getAllInvokers().get(0).isAvailable());
                assertEquals("other:cross", remote.hello("cross"));
                assertTrue(otherCalls.get() > 0);
                assertEquals(
                        "other-app",
                        discovery
                                .getInstances("tri://echo.test.svc.cluster.local:7070")
                                .get(0)
                                .getServiceMetadata()
                                .getApp());
                int accepted = otherCalls.get();
                ads.clusterOverrides.put(otherMetadata, NativeSecurityTestSupport.cluster(otherMetadata, true));
                ads.send(XdsResourceType.CDS.getTypeUrl());
                await(() -> current.getAllInvokers().isEmpty() && metadataFailureRecorded(discovery));
                assertEquals(accepted, otherCalls.get(), "Wrong destination SAN must fail before dispatch");
                // The source metadata policy and connection remain usable on the same socket.
                ads.routeOverride = route(BUSINESS);
                ads.send(XdsResourceType.LDS.getTypeUrl());
                await(() -> current.getAllInvokers().size() == 1
                        && current.getAllInvokers().get(0).getUrl().getPort() == businessPort
                        && current.getAllInvokers().get(0).isAvailable());
                assertEquals("business:source-isolated", remote.hello("source-isolated"));
                ads.clusterOverrides.put(otherMetadata, NativeSecurityTestSupport.cluster(otherMetadata, false));
                ads.routeOverride = route(otherBusiness);
                ads.send(XdsResourceType.CDS.getTypeUrl());
                ads.send(XdsResourceType.LDS.getTypeUrl());
                await(() -> current.getAllInvokers().size() == 1
                        && current.getAllInvokers().get(0).getUrl().getPort() == otherBusinessPort
                        && current.getAllInvokers().get(0).isAvailable());
                assertEquals("other:restored", remote.hello("restored"));
                ads.emptyClusters.add(otherMetadata);
            } else {
                ads.emptyClusters.add(METADATA);
            }
            ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> current.getAllInvokers().isEmpty());
        } finally {
            if (consumer != null) consumer.destroy();
            else if (directory != null) directory.destroy();
            if (registry != null) registry.destroy();
            exporters.forEach(Exporter::unexport);
            otherProtocol.destroy();
            otherFramework.destroy();
            peerProtocol.destroy();
            protocol.destroy();
            peerFramework.destroy();
            framework.destroy();
        }
    }

    private static ApplicationModel application(FrameworkModel framework, String name) {
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig(name));
        return application;
    }

    private static boolean metadataFailureRecorded(XdsServiceDiscovery discovery) {
        String error = discovery
                .inspectTargets()
                .join()
                .get("tri://echo.test.svc.cluster.local:7070")
                .getLastError();
        return error != null && error.endsWith("Exception");
    }

    private static URL reference(ApplicationModel application) {
        return URL.valueOf(
                        "tri://localhost/" + Business.class.getName()
                                + "?provided-by=echo&provider-port=7070&provider-namespace=test&protocol=tri&xds.metadata.application=provider-app&xds.metadata.service-port=7071")
                .setScopeModel(application.getInternalModule())
                .addParameter("interface", Business.class.getName());
    }

    private static void setup(Ads ads, int metadataPort, String... hosts) {
        ads.clusters = Arrays.asList(BUSINESS, METADATA);
        ads.routeOverride = route(BUSINESS);
        ads.listenerRoutes.put("echo.test.svc.cluster.local:7071", route(METADATA));
        ads.clusterAssignments.put(METADATA, assignment(METADATA, metadataPort, hosts));
    }

    static RouteConfiguration route(String cluster) {
        return RouteConfiguration.newBuilder()
                .setName(cluster + "-routes")
                .addVirtualHosts(VirtualHost.newBuilder()
                        .addDomains("*")
                        .addRoutes(Route.newBuilder()
                                .setMatch(RouteMatch.newBuilder().setPrefix("/"))
                                .setRoute(RouteAction.newBuilder().setCluster(cluster))))
                .build();
    }

    private static LbEndpoint endpoint(String host, int port) {
        return LbEndpoint.newBuilder()
                .setEndpoint(Endpoint.newBuilder()
                        .setAddress(Address.newBuilder()
                                .setSocketAddress(SocketAddress.newBuilder()
                                        .setAddress(host)
                                        .setPortValue(port))))
                .build();
    }

    private static ClusterLoadAssignment assignment(String cluster, int port, String... hosts) {
        LocalityLbEndpoints.Builder locality = LocalityLbEndpoints.newBuilder();
        for (String host : hosts) locality.addLbEndpoints(endpoint(host, port));
        return ClusterLoadAssignment.newBuilder()
                .setClusterName(cluster)
                .addEndpoints(locality)
                .build();
    }

    private static MetadataInfo metadata(XdsEndpoint business, int metadataPort) {
        return info(business.getPort(), metadataPort);
    }

    private static MetadataInfo info(int businessPort, int metadataPort) {
        MetadataInfo metadata = new MetadataInfo("provider-app");
        MetadataInfo.ServiceInfo service = new MetadataInfo.ServiceInfo(
                Business.class.getName(),
                null,
                null,
                "tri",
                businessPort,
                Business.class.getName(),
                Collections.singletonMap("source-port", String.valueOf(metadataPort)));
        metadata.getServices().put(service.getMatchKey(), service);
        return metadata;
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) fail("Timed out waiting for metadata/graph update");
            Thread.sleep(10);
        }
    }
}
