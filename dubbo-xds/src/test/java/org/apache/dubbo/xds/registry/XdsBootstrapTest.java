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

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.metadata.MetadataServiceDelegation;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.registry.XdsServiceDiscoveryTest.Ads;
import org.apache.dubbo.xds.security.NativeSecurityTestSupport;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class XdsBootstrapTest {
    private static final String CLUSTER = "outbound|7070||echo.test.svc.cluster.local";

    @TempDir
    Path certificates;

    public interface Business {
        String echo(String value);

        CompletableFuture<String> asyncEcho(String value);
    }

    @Test
    void normalBootstrapExportsRealMetadataAndReferenceUsesNativeDiscoveryTlsAndRouting() throws Exception {
        normalBootstrap(false);
    }

    @Test
    void normalBootstrapSupportsTwoReferencesToTheSameServiceWithDifferentProtocols() throws Exception {
        normalBootstrap(true);
    }

    private void normalBootstrap(boolean dual) throws Exception {
        NativeSecurityTestSupport.writeIdentity(certificates);
        FrameworkModel providerFramework = new FrameworkModel();
        FrameworkModel consumerFramework = new FrameworkModel();
        DubboBootstrap provider = DubboBootstrap.newInstance(providerFramework);
        DubboBootstrap consumer = DubboBootstrap.newInstance(consumerFramework);
        AtomicInteger calls = new AtomicInteger();
        try (Ads ads = new Ads()) {
            int port = NetUtils.getAvailablePort();
            int binaryPort = NetUtils.getAvailablePort();
            String bootstrap = NativeSecurityTestSupport.bootstrap(certificates, ads.server.getPort());
            String inbound = "xds.istio.io/grpc/lds/inbound/0.0.0.0:" + port;
            ads.workloadPort = port;
            if (dual) {
                String binaryCluster = "outbound|7071||echo.test.svc.cluster.local";
                ads.clusters = Arrays.asList(CLUSTER, binaryCluster);
                ads.clusterPorts.put(CLUSTER, port);
                ads.clusterPorts.put(binaryCluster, binaryPort);
                ads.routeOverride = XdsMetadataPortTest.route(CLUSTER);
            }
            ads.clusterOverrides.put(CLUSTER, NativeSecurityTestSupport.cluster(CLUSTER, false));
            ads.listenerOverrides.put(inbound, NativeSecurityTestSupport.listener(port, false));
            ads.endpoints("127.0.0.1");
            ApplicationConfig providerApp = new ApplicationConfig("sdk-provider");
            providerApp.setOnlyUseMetadataV2(false);
            providerApp.setMetadataServiceProtocol("tri");
            providerApp.setMetadataServicePort(port);
            ServiceConfig<Business> service = new ServiceConfig<>();
            service.setInterface(Business.class);
            service.setRef(new Business() {
                public String echo(String value) {
                    calls.incrementAndGet();
                    return "sdk:" + value;
                }

                public CompletableFuture<String> asyncEcho(String value) {
                    calls.incrementAndGet();
                    return CompletableFuture.completedFuture("sdk:" + value);
                }
            });
            service.setGroup("business-group");
            service.setVersion("1.2.3");
            provider.application(providerApp)
                    .registry(registry(bootstrap))
                    .protocol(new ProtocolConfig("tri", port))
                    .metrics(metrics())
                    .service(service);
            if (dual) provider.protocol(new ProtocolConfig("dubbo", binaryPort));
            provider.start();
            assertTrue(service.isExported());
            MetadataService metadata =
                    provider.getApplicationModel().getBeanFactory().getBean(MetadataServiceDelegation.class);
            assertFalse(metadata.getMetadataInfos().isEmpty());
            assertTrue(metadata.getMetadataInfos().stream()
                    .flatMap(info -> info.getServices().values().stream())
                    .anyMatch(info -> Business.class.getName().equals(info.getName())
                            && info.getProtocol().equals("tri")
                            && info.getPort() == port
                            && "business-group".equals(info.getGroup())
                            && "1.2.3".equals(info.getVersion())));
            ReferenceConfig<Business> reference = new ReferenceConfig<>();
            reference.setInterface(Business.class);
            reference.setGroup("business-group");
            reference.setVersion("1.2.3");
            reference.setProtocol("tri");
            reference.setInjvm(false);
            reference.setProvidedBy("echo");
            reference.setTimeout(3000);
            reference.setRetries(0);
            Map<String, String> params = new HashMap<>();
            params.put("provider-namespace", "test");
            params.put("provider-port", "7070");
            params.put("xds.metadata.application", "sdk-provider");
            reference.setParameters(params);
            ReferenceConfig<Business> binary = null;
            if (dual) {
                binary = new ReferenceConfig<>();
                binary.setInterface(Business.class);
                binary.setGroup("business-group");
                binary.setVersion("1.2.3");
                binary.setProtocol("dubbo");
                binary.setInjvm(false);
                binary.setProvidedBy("echo");
                binary.setTimeout(3000);
                binary.setRetries(0);
                Map<String, String> binaryParams = new HashMap<>(params);
                binaryParams.put("provider-port", "7071");
                binaryParams.put("xds.metadata.protocol", "tri");
                binaryParams.put("xds.metadata.service-port", "7070");
                binary.setParameters(binaryParams);
                consumer.reference(binary);
            }
            consumer.application(new ApplicationConfig("sdk-consumer"))
                    .registry(registry(bootstrap))
                    .metrics(metrics())
                    .reference(reference)
                    .start();
            Business remote = reference.get();
            assertEquals("sdk:sync", remote.echo("sync"));
            assertEquals("sdk:async", remote.asyncEcho("async").get(5, TimeUnit.SECONDS));
            assertEquals(2, calls.get());
            if (dual) {
                assertEquals("sdk:binary", binary.get().echo("binary"));
                assertEquals(
                        "sdk:binary-async",
                        binary.get().asyncEcho("binary-async").get(5, TimeUnit.SECONDS));
                assertEquals(4, calls.get());
            }
            assertTrue(ads.names.get(XdsResourceType.LDS.getTypeUrl()).contains(inbound));
            assertTrue(ads.names.get(XdsResourceType.LDS.getTypeUrl()).contains("echo.test.svc.cluster.local:7070"));
            ads.listenerOverrides.put(inbound, NativeSecurityTestSupport.listener(port, true));
            ads.send(XdsResourceType.LDS.getTypeUrl());
            await(() -> {
                try {
                    remote.echo("denied");
                    return false;
                } catch (RuntimeException error) {
                    return TriRpcStatus.getStatus(error).code == TriRpcStatus.Code.PERMISSION_DENIED;
                } finally {
                    RpcContext.removeContext();
                }
            });
            int denied = calls.get();
            ads.listenerOverrides.put(inbound, NativeSecurityTestSupport.listener(port, false));
            ads.send(XdsResourceType.LDS.getTypeUrl());
            await(() -> {
                try {
                    return "sdk:restored".equals(remote.echo("restored"));
                } catch (RuntimeException error) {
                    return false;
                } finally {
                    RpcContext.removeContext();
                }
            });
            assertTrue(calls.get() > denied);
        } finally {
            consumer.destroy();
            provider.destroy();
            consumerFramework.destroy();
            providerFramework.destroy();
            RpcContext.removeContext();
        }
    }

    private static MetricsConfig metrics() {
        MetricsConfig metrics = new MetricsConfig();
        metrics.setExportMetricsService(false);
        return metrics;
    }

    private static RegistryConfig registry(String bootstrap) {
        RegistryConfig config = new RegistryConfig("xds://localhost");
        config.setRegisterMode("instance");
        config.setUseAsConfigCenter(false);
        config.setUseAsMetadataCenter(false);
        Map<String, String> params = new HashMap<>();
        params.put("xds.bootstrap.config", bootstrap);
        params.put("registry-type", "service");
        params.put("file-cache", "false");
        config.setParameters(params);
        return config;
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(25);
        assertTrue(condition.getAsBoolean());
    }
}
