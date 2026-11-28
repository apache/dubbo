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
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistryDirectory;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.registry.XdsServiceDiscoveryTest.Ads;
import org.apache.dubbo.xds.security.NativeTlsAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import com.google.protobuf.UInt32Value;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.HeaderMatcher;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.WeightedCluster;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XdsRoutingTest {
    private static final String BLUE = "outbound|7070|blue|echo.test.svc.cluster.local";
    private static final String GREEN = "outbound|7070|green|echo.test.svc.cluster.local";

    public interface Business {
        String hello(String name);
    }

    @Test
    void actualTripleCallsHonorPathAuthorityHeaderAndLiveWeightsWithoutRecreatingInvokers() throws Exception {
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig("routing-test"));
        Protocol protocol = application.getExtensionLoader(Protocol.class).getExtension("tri");
        ProxyFactory proxy = application.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        List<Exporter<?>> providers = new ArrayList<>();
        List<AutoCloseable> providerScopes = new ArrayList<>();
        Invoker<Business> consumer = null;
        ServiceDiscoveryRegistryDirectory<Business> directory = null;
        XdsRegistry registry = null;
        try (Ads ads = new Ads()) {
            int bluePort = NetUtils.getAvailablePort();
            int greenPort = NetUtils.getAvailablePort();
            while (greenPort == bluePort) {
                greenPort = NetUtils.getAvailablePort();
            }
            ads.clusters = Arrays.asList(BLUE, GREEN);
            ads.clusterPorts.put(BLUE, bluePort);
            ads.clusterPorts.put(GREEN, greenPort);
            ads.routeOverride = routes(20, 80);
            publish(bluePort, "blue", providers, providerScopes);
            publish(greenPort, "green", providers, providerScopes);
            registry = new XdsRegistry(ads.registry(application), application);
            URL reference = URL.valueOf(
                            "tri://localhost/" + Business.class.getName()
                                    + "?provided-by=echo&provider-port=7070&provider-namespace=test&protocol=tri&xds.metadata.application=provider-app&retries=9")
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
            ServiceDiscoveryRegistryDirectory<Business> currentDirectory = directory;
            await(() -> currentDirectory.getAllInvokers().size() == 2);
            consumer = application
                    .getExtensionLoader(Cluster.class)
                    .getAdaptiveExtension()
                    .join(directory, true);
            Business remote = proxy.getProxy(consumer);
            int blue = 0;
            for (int i = 0; i < 200; i++) {
                if (remote.hello("weighted").equals("blue:weighted")) {
                    blue++;
                }
            }
            assertTrue(blue >= 15 && blue <= 70, "Observed blue replies: " + blue);
            try {
                for (int i = 0; i < 10; i++) {
                    RpcContext.getClientAttachment().setAttachment("x-version", "green");
                    assertEquals("green:header", remote.hello("header"));
                }
            } finally {
                RpcContext.removeClientAttachment();
            }
            List<Invoker<Business>> original = new ArrayList<>(directory.getAllInvokers());
            RouteConfiguration changed = routes(0, 100);
            ads.routeOverride = changed;
            ads.send(XdsResourceType.LDS.getTypeUrl());
            await(() -> NativeTlsAdapter.endpoint(original.get(0).getUrl())
                    .getSnapshot()
                    .getRoutes()
                    .get("routes")
                    .equals(changed));
            for (int i = 0; i < 30; i++) {
                assertEquals("green:updated", remote.hello("updated"));
            }
            assertEquals(new HashSet<>(original), new HashSet<>(directory.getAllInvokers()));
            // An unavailable selected subset must fail, rather than redirecting its weight to another subset.
            ads.emptyClusters.add(BLUE);
            ads.send(XdsResourceType.EDS.getTypeUrl());
            await(() -> currentDirectory.getAllInvokers().size() == 1);
            Invoker<Business> remaining = directory.getAllInvokers().get(0);
            RouteConfiguration unavailable = routes(100, 0);
            ads.routeOverride = unavailable;
            ads.send(XdsResourceType.LDS.getTypeUrl());
            await(() -> NativeTlsAdapter.endpoint(remaining.getUrl())
                    .getSnapshot()
                    .getRoutes()
                    .get("routes")
                    .equals(unavailable));
            assertThrows(RpcException.class, () -> remote.hello("no-subset"));
            RpcContext.getClientAttachment().setAttachment("x-version", "green");
            assertEquals("green:still-ready", remote.hello("still-ready"));
            RouteConfiguration unmatched = routes(0, 100).toBuilder()
                    .setVirtualHosts(
                            0,
                            routes(0, 100).getVirtualHosts(0).toBuilder()
                                    .clearDomains()
                                    .addDomains("unrelated.test:7070"))
                    .build();
            ads.routeOverride = unmatched;
            ads.send(XdsResourceType.LDS.getTypeUrl());
            await(() -> NativeTlsAdapter.endpoint(original.get(0).getUrl())
                    .getSnapshot()
                    .getRoutes()
                    .get("routes")
                    .equals(unmatched));
            assertThrows(RpcException.class, () -> remote.hello("no-route"));
            ads.endpoints();
            await(() -> currentDirectory.getAllInvokers().isEmpty());
            assertThrows(RpcException.class, () -> remote.hello("removed"));
        } finally {
            if (consumer != null) {
                consumer.destroy();
            } else if (directory != null) {
                directory.destroy();
            }
            if (registry != null) {
                registry.destroy();
            }
            providers.forEach(Exporter::unexport);
            for (AutoCloseable scope : providerScopes) {
                scope.close();
            }
            protocol.destroy();
            framework.destroy();
        }
    }

    private static RouteConfiguration routes(int blue, int green) {
        String path = "/" + Business.class.getName() + "/hello";
        Route header = Route.newBuilder()
                .setMatch(RouteMatch.newBuilder()
                        .setPath(path)
                        .addHeaders(
                                HeaderMatcher.newBuilder().setName("x-version").setExactMatch("green")))
                .setRoute(RouteAction.newBuilder().setCluster(GREEN))
                .build();
        Route weighted = Route.newBuilder()
                .setMatch(RouteMatch.newBuilder().setPath(path))
                .setRoute(RouteAction.newBuilder()
                        .setWeightedClusters(WeightedCluster.newBuilder()
                                .addClusters(weight(BLUE, blue))
                                .addClusters(weight(GREEN, green))))
                .build();
        return RouteConfiguration.newBuilder()
                .setName("routes")
                .addVirtualHosts(VirtualHost.newBuilder()
                        .setName("service")
                        .addDomains("echo.test.svc.cluster.local:7070")
                        .addRoutes(header)
                        .addRoutes(weighted))
                .build();
    }

    private static WeightedCluster.ClusterWeight weight(String name, int value) {
        return WeightedCluster.ClusterWeight.newBuilder()
                .setName(name)
                .setWeight(UInt32Value.of(value))
                .build();
    }

    private static void publish(int port, String version, List<Exporter<?>> providers, List<AutoCloseable> scopes) {
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig("provider-app"));
        Protocol protocol = application.getExtensionLoader(Protocol.class).getExtension("tri");
        ProxyFactory proxy = application.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        scopes.add(() -> {
            protocol.destroy();
            framework.destroy();
        });
        MetadataInfo metadata = new MetadataInfo("provider-app");
        MetadataInfo.ServiceInfo info = new MetadataInfo.ServiceInfo(
                Business.class.getName(), null, null, "tri", port, Business.class.getName(), Collections.emptyMap());
        metadata.getServices().put(info.getMatchKey(), info);
        MetadataService service = mock(MetadataService.class);
        when(service.getMetadataInfos()).thenReturn(Collections.singletonList(metadata));
        providers.add(export(
                application,
                protocol,
                proxy,
                MetadataService.class,
                service,
                URL.valueOf("tri://127.0.0.1:" + port + "/" + MetadataService.class.getName()
                        + "?filter=-default&group=provider-app&version=1.0.0")));
        providers.add(export(
                application,
                protocol,
                proxy,
                Business.class,
                value -> version + ":" + value,
                URL.valueOf("tri://127.0.0.1:" + port + "/" + Business.class.getName())));
    }

    static <T> Exporter<T> export(
            ApplicationModel application,
            Protocol protocol,
            ProxyFactory proxy,
            Class<T> type,
            T implementation,
            URL url) {
        url = url.setScopeModel(application.getInternalModule());
        ServiceDescriptor descriptor =
                application.getInternalModule().getServiceRepository().registerService(type);
        ProviderModel provider = new ProviderModel(
                url.getServiceKey(),
                implementation,
                descriptor,
                new ServiceMetadata(),
                ClassUtils.getClassLoader(type));
        application.getInternalModule().getServiceRepository().registerProvider(provider);
        return protocol.export(proxy.getInvoker(implementation, type, url.setServiceModel(provider)));
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean(), "Timed out waiting for route/address update");
    }
}
