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
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistryDirectory;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.TriRpcStatus;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XdsDualProtocolTest {
    private static final String TRI = "outbound|7070||echo.test.svc.cluster.local";
    private static final String BINARY = "outbound|7071||echo.test.svc.cluster.local";
    private static final String TRI_KEY = "tri://echo.test.svc.cluster.local:7070";
    private static final String BINARY_KEY = "dubbo://echo.test.svc.cluster.local:7071";

    @TempDir
    Path certificates;

    public interface Business {
        String hello(String value);
    }

    public interface OtherPort {
        String hello(String value);
    }

    @Test
    void eachServicePortUsesItsOwnProtocolAndPointToPointMetadata() throws Exception {
        try (Fixture f = new Fixture(false)) {
            assertEquals("tri:triple", f.triple.hello("triple"));
            assertEquals("dubbo:binary", f.binary.hello("binary"));
            assertTrue(f.triReads.get() > 0);
            assertTrue(f.binaryReads.get() > 0);
            f.assertSeparated();
            XdsServiceDiscovery discovery = (XdsServiceDiscovery) f.registry.getServiceDiscovery();
            assertThrows(
                    IllegalArgumentException.class,
                    () -> discovery.bindReference(
                            URL.valueOf("tri://localhost?protocol=tri&provided-by=" + BINARY_KEY)));
            assertTrue(f.ads.names.get(XdsResourceType.LDS.getTypeUrl()).contains("echo.test.svc.cluster.local:7070"));
            assertFalse(f.ads.names.get(XdsResourceType.LDS.getTypeUrl()).contains("echo.test.svc.cluster.local:7071"));
            f.ads.emptyClusters.add(BINARY);
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> f.binaryDirectory.getAllInvokers().isEmpty());
            RpcException unavailable = assertThrows(RpcException.class, () -> f.binary.hello("withdrawn"));
            assertEquals(TriRpcStatus.Code.UNAVAILABLE, TriRpcStatus.getStatus(unavailable).code);
            assertTrue(unavailable.isNetwork());
            assertEquals("tri:still-ready", f.triple.hello("still-ready"));
            f.ads.emptyClusters.remove(BINARY);
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> ready(f.binaryDirectory));
            assertEquals("dubbo:restored", f.binary.hello("restored"));
        }
    }

    @Test
    void tripleSubsetWithoutEndpointsDoesNotRemoveBinaryMetadataAddresses() throws Exception {
        NativeSecurityTestSupport.writeIdentity(certificates);
        try (Fixture f = new Fixture(true, 100)) {
            String selected = "outbound|7070|selected|echo.test.svc.cluster.local";
            f.ads.clusters = Arrays.asList(TRI, BINARY, selected);
            f.ads.clusterPorts.put(selected, f.triPort);
            f.ads.emptyClusters.add(selected);
            f.ads.routeOverride = XdsMetadataPortTest.route(selected);
            f.ads.send(XdsResourceType.LDS.getTypeUrl());
            f.ads.send(XdsResourceType.CDS.getTypeUrl());
            f.ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> f.triDirectory.getAllInvokers().isEmpty());
            int reads = f.triReads.get();
            await(() -> f.triReads.get() >= reads + 3);
            XdsServiceDiscovery discovery = (XdsServiceDiscovery) f.registry.getServiceDiscovery();
            assertEquals(1, discovery.inspectTargets().join().get(BINARY_KEY).getAvailableInstances());
            assertTrue(ready(f.binaryDirectory));
            assertEquals("dubbo:independent", f.binary.hello("independent"));
            assertEquals(0, f.binaryReads.get());
        }
    }

    @Test
    void binaryBusinessCanExplicitlyUseTheTripleMetadataServicePort() throws Exception {
        try (Fixture f = new Fixture(true)) {
            assertEquals("tri:triple", f.triple.hello("triple"));
            assertEquals("dubbo:binary", f.binary.hello("binary"));
            assertTrue(f.triReads.get() >= 2);
            assertEquals(0, f.binaryReads.get());
            f.assertSeparated();
        }
    }

    @Test
    void binaryTlsRequirementRemovesOnlyBinaryAddressesWithoutPlaintextFallback() throws Exception {
        NativeSecurityTestSupport.writeIdentity(certificates);
        try (Fixture f = new Fixture(false)) {
            XdsServiceDiscovery discovery = (XdsServiceDiscovery) f.registry.getServiceDiscovery();
            f.ads.clusterOverrides.put(BINARY, NativeSecurityTestSupport.cluster(BINARY, false));
            f.ads.send(XdsResourceType.CDS.getTypeUrl());
            await(() -> f.binaryDirectory.getAllInvokers().isEmpty());
            assertEquals(
                    "IllegalStateException",
                    discovery.inspectTargets().join().get(BINARY_KEY).getLastError());
            assertEquals("tri:policy-isolated", f.triple.hello("policy-isolated"));
            f.ads.clusterOverrides.remove(BINARY);
            f.ads.send(XdsResourceType.CDS.getTypeUrl());
            await(() -> ready(f.binaryDirectory));
            assertEquals("dubbo:restored", f.binary.hello("restored"));
        }
    }

    private final class Fixture implements AutoCloseable {
        final FrameworkModel framework = new FrameworkModel();
        final FrameworkModel peerFramework = new FrameworkModel();
        final ApplicationModel application = application(framework, "dual-client");
        final ApplicationModel peer = application(peerFramework, "provider-app");
        final Ads ads = new Ads();
        final List<Exporter<?>> exporters = new ArrayList<>();
        final List<Invoker<Business>> consumers = new ArrayList<>();
        final int triPort = NetUtils.getAvailablePort();
        final int binaryPort = NetUtils.getAvailablePort();
        final AtomicInteger triReads = new AtomicInteger();
        final AtomicInteger binaryReads = new AtomicInteger();
        XdsRegistry registry;
        ServiceDiscoveryRegistryDirectory<Business> triDirectory;
        ServiceDiscoveryRegistryDirectory<Business> binaryDirectory;
        Business triple;
        Business binary;

        Fixture(boolean tripleMetadata) throws Exception {
            this(tripleMetadata, 30000);
        }

        Fixture(boolean tripleMetadata, long refreshMillis) throws Exception {
            try {
                ads.clusters = Arrays.asList(TRI, BINARY);
                ads.clusterPorts.put(TRI, triPort);
                ads.clusterPorts.put(BINARY, binaryPort);
                ads.workloadPort = triPort;
                ads.routeOverride = XdsMetadataPortTest.route(TRI);
                MetadataInfo info = new MetadataInfo("provider-app");
                info.addService(provider("tri", triPort, Business.class));
                info.addService(provider("dubbo", binaryPort, Business.class));
                // These are advertised on the other protocol's socket and must not enter either cache.
                info.addService(provider("tri", binaryPort, OtherPort.class));
                info.addService(provider("dubbo", triPort, OtherPort.class));
                ProxyFactory proxy = peer.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
                for (String protocolName : Arrays.asList("tri", "dubbo")) {
                    Protocol protocol = peer.getExtensionLoader(Protocol.class).getExtension(protocolName);
                    int port = "tri".equals(protocolName) ? triPort : binaryPort;
                    Business implementation = value -> protocolName + ":" + value;
                    exporters.add(XdsRoutingTest.export(
                            peer,
                            protocol,
                            proxy,
                            Business.class,
                            implementation,
                            provider(protocolName, port, Business.class)));
                    MetadataService metadata = mock(MetadataService.class);
                    AtomicInteger reads = "tri".equals(protocolName) ? triReads : binaryReads;
                    when(metadata.getMetadataInfos()).thenAnswer(ignored -> {
                        reads.incrementAndGet();
                        return Collections.singletonList(info);
                    });
                    exporters.add(XdsRoutingTest.export(
                            peer,
                            protocol,
                            proxy,
                            MetadataService.class,
                            metadata,
                            provider(protocolName, port, MetadataService.class)
                                    .addParameter("filter", "-default")
                                    .addParameter("group", "provider-app")
                                    .addParameter("version", "1.0.0")));
                }
                URL registryUrl = ads.registry(application)
                        .addParameter("xds.metadata.refresh", Long.toString(refreshMillis))
                        .addParameter(
                                "xds.bootstrap.config",
                                NativeSecurityTestSupport.bootstrap(certificates, ads.server.getPort()))
                        .addParameter("xds.metadata.ttl", "60000");
                registry = new XdsRegistry(registryUrl, application);
                triDirectory = directory("tri", 7070, false, registryUrl);
                binaryDirectory = directory("dubbo", 7071, tripleMetadata, registryUrl);
                ads.endpoints("127.0.0.1");
                await(() -> ready(triDirectory) && ready(binaryDirectory));
                triple = consumer(triDirectory);
                binary = consumer(binaryDirectory);
            } catch (Exception | Error error) {
                close();
                throw error;
            }
        }

        ServiceDiscoveryRegistryDirectory<Business> directory(
                String protocolName, int port, boolean tripleMetadata, URL registryUrl) {
            URL reference = URL.valueOf(protocolName + "://localhost/" + Business.class.getName())
                    .addParameter("interface", Business.class.getName())
                    .addParameter("provided-by", "echo")
                    .addParameter("provider-namespace", "test")
                    .addParameter("provider-port", port)
                    .addParameter("protocol", protocolName)
                    .addParameter("xds.metadata.application", "provider-app")
                    .addParameter("timeout", "2000")
                    .addParameter("retries", "0")
                    .setScopeModel(application.getInternalModule());
            if (tripleMetadata)
                reference = reference
                        .addParameter("xds.metadata.protocol", "tri")
                        .addParameter("xds.metadata.service-port", "7070");
            ServiceDescriptor descriptor =
                    application.getInternalModule().getServiceRepository().registerService(Business.class);
            reference = reference.setServiceModel(
                    application.getInternalModule().registerInternalConsumer(Business.class, reference, descriptor));
            URL url = registryUrl
                    .setPath(Business.class.getName())
                    .setScopeModel(application.getInternalModule())
                    .setServiceModel(reference.getServiceModel())
                    .addParameterAndEncoded("refer", reference.toParameterString());
            ServiceDiscoveryRegistryDirectory<Business> directory =
                    new ServiceDiscoveryRegistryDirectory<>(Business.class, url);
            directory.setRegistry(registry);
            directory.setProtocol(application.getExtensionLoader(Protocol.class).getExtension(protocolName));
            directory.buildRouterChain(reference);
            directory.subscribe(reference);
            return directory;
        }

        Business consumer(ServiceDiscoveryRegistryDirectory<Business> directory) {
            Invoker<Business> invoker = application
                    .getExtensionLoader(Cluster.class)
                    .getAdaptiveExtension()
                    .join(directory, true);
            consumers.add(invoker);
            return application
                    .getExtensionLoader(ProxyFactory.class)
                    .getAdaptiveExtension()
                    .getProxy(invoker);
        }

        void assertSeparated() {
            assertEquals(triPort, triDirectory.getAllInvokers().get(0).getUrl().getPort());
            assertEquals("tri", triDirectory.getAllInvokers().get(0).getUrl().getProtocol());
            assertEquals(
                    binaryPort, binaryDirectory.getAllInvokers().get(0).getUrl().getPort());
            assertEquals(
                    "dubbo", binaryDirectory.getAllInvokers().get(0).getUrl().getProtocol());
            XdsServiceDiscovery discovery = (XdsServiceDiscovery) registry.getServiceDiscovery();
            for (String target : Arrays.asList(TRI_KEY, BINARY_KEY)) {
                MetadataInfo info = discovery.getInstances(target).get(0).getServiceMetadata();
                assertEquals(1, info.getServices().size());
                MetadataInfo.ServiceInfo service =
                        info.getServices().values().iterator().next();
                assertEquals(Business.class.getName(), service.getName());
                assertEquals(target.equals(TRI_KEY) ? "tri" : "dubbo", service.getProtocol());
                assertEquals(target.equals(TRI_KEY) ? triPort : binaryPort, service.getPort());
            }
        }

        public void close() throws Exception {
            consumers.forEach(Invoker::destroy);
            if (triDirectory != null) triDirectory.destroy();
            if (binaryDirectory != null) binaryDirectory.destroy();
            if (registry != null) registry.destroy();
            exporters.forEach(Exporter::unexport);
            peerFramework.destroy();
            framework.destroy();
            ads.close();
        }
    }

    private static URL provider(String protocol, int port, Class<?> type) {
        return URL.valueOf(protocol + "://127.0.0.1:" + port + "/" + type.getName());
    }

    private static ApplicationModel application(FrameworkModel framework, String name) {
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig(name));
        return application;
    }

    private static boolean ready(ServiceDiscoveryRegistryDirectory<?> directory) {
        return directory.getAllInvokers().size() == 1
                && directory.getAllInvokers().get(0).isAvailable();
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(10);
        assertTrue(condition.getAsBoolean());
    }
}
