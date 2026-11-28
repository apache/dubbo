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
package org.apache.dubbo.xds.security;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.client.XdsRuntimePool;
import org.apache.dubbo.xds.registry.DiscoveryTarget;
import org.apache.dubbo.xds.registry.XdsEndpoint;
import org.apache.dubbo.xds.security.XdsSecurityTest.Authority;
import org.apache.dubbo.xds.security.XdsSecurityTest.Identity;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Message;
import com.google.protobuf.Struct;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Address;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.TransportSocket;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.Endpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Filter;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.FilterChain;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Listener;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.rbac.v3.Permission;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.rbac.v3.Policy;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.rbac.v3.Principal;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.rbac.v3.RBAC;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.HeaderMatcher;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.NonForwardingAction;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.router.v3.Router;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CommonTlsContext;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.DownstreamTlsContext;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.StringMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import static org.apache.dubbo.xds.security.XdsSecurityTest.cluster;
import static org.apache.dubbo.xds.security.XdsSecurityTest.common;
import static org.apache.dubbo.xds.security.XdsSecurityTest.pem;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InboundSecurityTest {
    static final String PRINCIPAL = "spiffe://cluster.local/ns/test/sa/provider";

    @TempDir
    Path directory;

    public interface Business {
        String hello(String name);
    }

    @Test
    void metadataAndBusinessUseMutualTlsAndCurrentRbacOnExistingConnections() throws Exception {
        int port = NetUtils.getAvailablePort();
        Authority authority = new Authority();
        writeIdentity(directory, authority.identity(PRINCIPAL), authority);
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig("provider-app"));
        Protocol protocol = application.getExtensionLoader(Protocol.class).getExtension("tri");
        ProxyFactory proxy = application.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        List<Invoker<?>> consumers = new ArrayList<>();
        List<Exporter<?>> providers = new ArrayList<>();
        AtomicInteger metadataCalls = new AtomicInteger();
        try (Ads ads = new Ads(listener(port, allowHttps()))) {
            String config = bootstrapJson(directory, ads.server.getPort(), "inbound-test");
            RegistryConfig registry = new RegistryConfig("xds://localhost");
            registry.setParameters(Collections.singletonMap("xds.bootstrap.config", config));
            application.getApplicationConfigManager().addRegistry(registry);
            // Internal metadata is exported before RegistryProtocol, with no default business filters.
            MetadataService metadata = mock(MetadataService.class);
            when(metadata.getMetadataInfos()).thenAnswer(call -> {
                metadataCalls.incrementAndGet();
                return Collections.singletonList(new MetadataInfo("provider-app"));
            });
            URL metadataUrl = url(application, port, MetadataService.class)
                    .addParameter("group", "provider-app")
                    .addParameter("version", "1.0.0")
                    .addParameter("filter", "-default");
            providers.add(export(application, protocol, proxy, MetadataService.class, metadata, metadataUrl));
            providers.add(export(
                    application,
                    protocol,
                    proxy,
                    Business.class,
                    name -> "hello " + name,
                    url(application, port, Business.class)));
            BootstrapInfo bootstrap = Bootstrapper.parse(config);
            try (XdsRuntimePool.Lease owner = XdsRuntimePool.acquire(bootstrap)) {
                assertEquals(1, XdsRuntimePool.activeRuntimeCount());
                assertEquals(1, ads.streams.get());
                MetadataService remote = reference(
                        application, protocol, proxy, MetadataService.class, clientUrl(owner, metadataUrl), consumers);
                Business business = reference(
                        application,
                        protocol,
                        proxy,
                        Business.class,
                        clientUrl(owner, url(application, port, Business.class)),
                        consumers);
                assertEquals("provider-app", remote.getMetadataInfos().get(0).getApp());
                assertEquals("hello xds", business.hello("xds"));
                int executed = metadataCalls.get();
                MetadataService plaintext =
                        reference(application, protocol, proxy, MetadataService.class, metadataUrl, consumers);
                assertThrows(RpcException.class, plaintext::getMetadataInfos);
                assertEquals(executed, metadataCalls.get());
                Path other = Files.createDirectory(directory.resolve("other"));
                writeIdentity(other, authority.identity("spiffe://cluster.local/ns/test/sa/unauthorized"), authority);
                // An isolated synthetic client runtime does not watch ADS; the server shares the real runtime.
                try (XdsRuntimePool.Lease unauthorized =
                        XdsRuntimePool.acquire(Bootstrapper.parse(bootstrapJson(other, 1, "unauthorized")))) {
                    MetadataService wrongIdentity = reference(
                            application,
                            protocol,
                            proxy,
                            MetadataService.class,
                            clientUrl(unauthorized, metadataUrl),
                            consumers);
                    assertPermissionDenied(wrongIdentity::getMetadataInfos);
                    assertEquals(executed, metadataCalls.get());
                }
                InboundPolicy previous = XdsServerBindings.binding(port).policy;
                ads.send("deny", listener(port, deny(), allowHttps()));
                await(() -> XdsServerBindings.binding(port).policy != previous);
                assertPermissionDenied(remote::getMetadataInfos);
                assertPermissionDenied(() -> business.hello("denied"));
                assertEquals(executed, metadataCalls.get());
                InboundPolicy denied = XdsServerBindings.binding(port).policy;
                ads.send("restore", listener(port, allowHttps()));
                await(() -> XdsServerBindings.binding(port).policy != denied);
                assertEquals("provider-app", remote.getMetadataInfos().get(0).getApp());
                assertEquals("hello again", business.hello("again"));
                InboundPolicy good = XdsServerBindings.binding(port).policy;
                Listener invalid = unknownFilter(listener(port, allow()));
                ads.send("unsupported", invalid);
                DiscoveryRequest nack = ads.await("unsupported");
                assertTrue(nack.hasErrorDetail());
                assertEquals("restore", nack.getVersionInfo());
                assertSame(good, XdsServerBindings.binding(port).policy);
                assertEquals("provider-app", remote.getMetadataInfos().get(0).getApp());

                // Keep A's existing HTTP/2 calls alive while the server starts accepting B.
                Authority replacement = new Authority();
                long epoch = owner.getSecurity().getEpoch();
                Files.write(
                        directory.resolve("root.pem"),
                        (new String(pem(authority.certificate), StandardCharsets.US_ASCII)
                                        + new String(pem(replacement.certificate), StandardCharsets.US_ASCII))
                                .getBytes(StandardCharsets.US_ASCII));
                await(() -> owner.getSecurity().getEpoch() > epoch);
                assertEquals("provider-app", remote.getMetadataInfos().get(0).getApp());
                assertEquals("hello overlap", business.hello("overlap"));

                Identity replacementIdentity = replacement.identity(PRINCIPAL);
                long overlapEpoch = owner.getSecurity().getEpoch();
                Files.write(directory.resolve("cert.pem"), pem(replacementIdentity.certificate));
                Files.write(directory.resolve("key.pem"), pem(replacementIdentity.keys.getPrivate()));
                await(() -> owner.getSecurity().getEpoch() > overlapEpoch);
                MetadataService replacementRemote = reference(
                        application, protocol, proxy, MetadataService.class, clientUrl(owner, metadataUrl), consumers);
                Business replacementBusiness = reference(
                        application,
                        protocol,
                        proxy,
                        Business.class,
                        clientUrl(owner, url(application, port, Business.class)),
                        consumers);
                assertEquals(
                        "provider-app",
                        replacementRemote.getMetadataInfos().get(0).getApp());
                assertEquals("hello new-root", replacementBusiness.hello("new-root"));
                assertEquals("provider-app", remote.getMetadataInfos().get(0).getApp());

                // Retiring A must deny new RPCs on A's already-established TLS connection.
                long replacementEpoch = owner.getSecurity().getEpoch();
                Files.write(directory.resolve("root.pem"), pem(replacement.certificate));
                await(() -> owner.getSecurity().getEpoch() > replacementEpoch);
                executed = metadataCalls.get();
                assertPermissionDenied(remote::getMetadataInfos);
                assertPermissionDenied(() -> business.hello("retired-root"));
                assertEquals(executed, metadataCalls.get());
                assertEquals(
                        "provider-app",
                        replacementRemote.getMetadataInfos().get(0).getApp());
                assertEquals("hello retained-root", replacementBusiness.hello("retained-root"));
                remote = replacementRemote;
                ads.send("deleted");
                await(() -> XdsServerBindings.binding(port).policy == null);
                executed = metadataCalls.get();
                assertPermissionDenied(remote::getMetadataInfos);
                assertEquals(executed, metadataCalls.get());
                providers.forEach(Exporter::unexport);
                assertTrue(XdsServerBindings.isManaged(port));
                assertTrue(XdsServerBindings.binding(port).closed.get());
            }
        } finally {
            consumers.forEach(Invoker::destroy);
            providers.forEach(Exporter::unexport);
            protocol.destroy();
            framework.destroy();
        }
        assertFalse(XdsServerBindings.isManaged(port));
        assertEquals(0, XdsRuntimePool.activeRuntimeCount());
    }

    @Test
    void explicitPlaintextStillGuardsHttp2RpcsAndRejectsHttp1Bypass() throws Exception {
        int port = NetUtils.getAvailablePort();
        Listener tls = listener(port);
        Listener plaintext = tls.toBuilder()
                .setFilterChains(0, tls.getFilterChains(0).toBuilder().clearTransportSocket())
                .build();
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig("plaintext-test"));
        Protocol protocol = application.getExtensionLoader(Protocol.class).getExtension("tri");
        ProxyFactory proxy = application.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        List<Invoker<?>> consumers = new ArrayList<>();
        Exporter<Business> provider = null;
        AtomicInteger calls = new AtomicInteger();
        try (Ads ads = new Ads(plaintext)) {
            RegistryConfig registry = new RegistryConfig("xds://localhost");
            registry.setParameters(Collections.singletonMap(
                    "xds.bootstrap.config", bootstrapJson(directory, ads.server.getPort(), "plaintext-test")));
            application.getApplicationConfigManager().addRegistry(registry);
            URL url = url(application, port, Business.class);
            provider = export(
                    application,
                    protocol,
                    proxy,
                    Business.class,
                    value -> {
                        calls.incrementAndGet();
                        return "hello " + value;
                    },
                    url);
            Business business = reference(application, protocol, proxy, Business.class, url, consumers);
            assertEquals("hello h2", business.hello("h2"));
            try (Socket socket = new Socket("127.0.0.1", port)) {
                socket.setSoTimeout(2000);
                socket.getOutputStream()
                        .write(("GET /" + Business.class.getName()
                                        + "/hello?name=bypass HTTP/1.1\r\nHost: localhost\r\n\r\n")
                                .getBytes(StandardCharsets.US_ASCII));
                assertEquals(-1, socket.getInputStream().read());
            }
            assertEquals(1, calls.get());
            InboundPolicy previous = XdsServerBindings.binding(port).policy;
            Listener denied = listener(port, deny());
            denied = denied.toBuilder()
                    .setFilterChains(0, denied.getFilterChains(0).toBuilder().clearTransportSocket())
                    .build();
            ads.send("plain-deny", denied);
            await(() -> XdsServerBindings.binding(port).policy != previous);
            assertPermissionDenied(() -> business.hello("denied"));
            assertEquals(1, calls.get());
        } finally {
            consumers.forEach(Invoker::destroy);
            if (provider != null) {
                provider.unexport();
            }
            protocol.destroy();
            framework.destroy();
        }
        assertFalse(XdsServerBindings.isManaged(port));
        assertEquals(0, XdsRuntimePool.activeRuntimeCount());
    }

    @Test
    @SuppressWarnings("unchecked")
    void invalidInitialLdsNeverDelegatesExportOrFallsBackToPlaintext() throws Exception {
        int port = NetUtils.getAvailablePort();
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = framework.newApplication();
        Protocol delegate = mock(Protocol.class);
        NativeSecurityProtocol protocol = new NativeSecurityProtocol(delegate);
        try (Ads ads = new Ads(unknownFilter(listener(port, allow())))) {
            URL provider = url(application, port, Business.class)
                    .addParameter("xds.enabled", "true")
                    .addParameter(
                            "xds.bootstrap.config", bootstrapJson(directory, ads.server.getPort(), "invalid-start"))
                    .addParameter("xds.bootstrap.wait", "500");
            Invoker<Business> invoker = mock(Invoker.class);
            when(invoker.getUrl()).thenReturn(provider);
            assertThrows(IllegalStateException.class, () -> protocol.export(invoker));
            verify(delegate, never()).export(any());
            assertTrue(ads.await("initial").hasErrorDetail());
            assertTrue(XdsServerBindings.binding(port).closed.get());
            assertNull(XdsServerBindings.binding(port).policy);
        } finally {
            protocol.destroy();
            framework.destroy();
        }
        assertFalse(XdsServerBindings.isManaged(port));
        assertEquals(0, XdsRuntimePool.activeRuntimeCount());
    }

    @Test
    void rejectsWrongListenerAddressAndUnsupportedRestrictionsBeforeAck() throws Exception {
        BootstrapInfo bootstrap = Bootstrapper.parse(bootstrapJson(directory, 1, "parse-test"));
        Listener valid = listener(17070, allow());
        assertEquals(17070, InboundPolicy.parse(valid, bootstrap).getPort());
        assertThrows(
                IllegalArgumentException.class,
                () -> InboundPolicy.parse(
                        valid.toBuilder().setAddress(address(17071)).build(), bootstrap));
        assertThrows(IllegalArgumentException.class, () -> InboundPolicy.parse(unknownFilter(valid), bootstrap));
    }

    private static void assertPermissionDenied(Executable call) {
        RpcException error = assertThrows(RpcException.class, call);
        assertTrue(
                error.getMessage().contains("PERMISSION_DENIED")
                        || error.getMessage().contains("xDS access denied"),
                error.getMessage());
    }

    static Listener listener(int port, RBAC... policies) {
        HttpConnectionManager.Builder hcm = HttpConnectionManager.newBuilder()
                .setStatPrefix("grpc")
                .setRouteConfig(RouteConfiguration.newBuilder()
                        .setName("inbound")
                        .addVirtualHosts(VirtualHost.newBuilder()
                                .setName("inbound")
                                .addDomains("*")
                                .addRoutes(Route.newBuilder()
                                        .setMatch(RouteMatch.newBuilder().setPrefix("/"))
                                        .setNonForwardingAction(NonForwardingAction.getDefaultInstance()))));
        for (RBAC policy : policies) {
            hcm.addHttpFilters(HttpFilter.newBuilder()
                    .setName("envoy.filters.http.rbac." + policy.getAction())
                    .setTypedConfig(Any.pack(io.grpc
                            .xds
                            .shaded
                            .io
                            .envoyproxy
                            .envoy
                            .extensions
                            .filters
                            .http
                            .rbac
                            .v3
                            .RBAC
                            .newBuilder()
                            .setRules(policy)
                            .build())));
        }
        hcm.addHttpFilters(HttpFilter.newBuilder()
                .setName("envoy.filters.http.router")
                .setTypedConfig(Any.pack(Router.getDefaultInstance())));
        CommonTlsContext common = common(PRINCIPAL);
        // grpcgen authenticates against the mesh roots; service accounts are constrained by RBAC.
        common = common.toBuilder()
                .setCombinedValidationContext(common.getCombinedValidationContext().toBuilder()
                        .setDefaultValidationContext(
                                common.getCombinedValidationContext().getDefaultValidationContext().toBuilder()
                                        .clearMatchSubjectAltNames()))
                .build();
        return Listener.newBuilder()
                .setName("xds.istio.io/grpc/lds/inbound/0.0.0.0:" + port)
                .setAddress(address(port))
                .addFilterChains(FilterChain.newBuilder()
                        .setTransportSocket(TransportSocket.newBuilder()
                                .setName("envoy.transport_sockets.tls")
                                .setTypedConfig(Any.pack(DownstreamTlsContext.newBuilder()
                                        .setCommonTlsContext(common)
                                        .setRequireClientCertificate(BoolValue.of(true))
                                        .build())))
                        .addFilters(Filter.newBuilder()
                                .setName("envoy.filters.network.http_connection_manager")
                                .setTypedConfig(Any.pack(hcm.build()))))
                .build();
    }

    static RBAC allow() {
        return RBAC.newBuilder()
                .setAction(RBAC.Action.ALLOW)
                .putPolicies(
                        "allowed-service-account",
                        Policy.newBuilder()
                                .addPermissions(Permission.newBuilder().setAny(true))
                                .addPrincipals(Principal.newBuilder()
                                        .setAuthenticated(Principal.Authenticated.newBuilder()
                                                .setPrincipalName(StringMatcher.newBuilder()
                                                        .setExact(PRINCIPAL))))
                                .build())
                .build();
    }

    private static RBAC allowHttps() {
        Policy policy = allow().getPoliciesOrThrow("allowed-service-account").toBuilder()
                .clearPermissions()
                .addPermissions(Permission.newBuilder()
                        .setHeader(HeaderMatcher.newBuilder().setName(":scheme").setExactMatch("https")))
                .build();
        return allow().toBuilder()
                .putPolicies("allowed-service-account", policy)
                .build();
    }

    static RBAC deny() {
        return RBAC.newBuilder()
                .setAction(RBAC.Action.DENY)
                .putPolicies(
                        "deny-all",
                        Policy.newBuilder()
                                .addPermissions(Permission.newBuilder().setAny(true))
                                .addPrincipals(Principal.newBuilder().setAny(true))
                                .build())
                .build();
    }

    private static Listener unknownFilter(Listener listener) throws Exception {
        FilterChain.Builder chain = listener.getFilterChains(0).toBuilder();
        HttpConnectionManager.Builder hcm =
                chain.getFilters(0).getTypedConfig().unpack(HttpConnectionManager.class).toBuilder();
        hcm.addHttpFilters(
                0,
                HttpFilter.newBuilder()
                        .setName("unsupported-required-auth")
                        .setTypedConfig(Any.pack(Struct.getDefaultInstance()))
                        .build());
        return listener.toBuilder()
                .setFilterChains(
                        0, chain.setFilters(0, chain.getFilters(0).toBuilder().setTypedConfig(Any.pack(hcm.build()))))
                .build();
    }

    private static Address address(int port) {
        return Address.newBuilder()
                .setSocketAddress(
                        SocketAddress.newBuilder().setAddress("0.0.0.0").setPortValue(port))
                .build();
    }

    private static URL url(ApplicationModel application, int port, Class<?> type) {
        return URL.valueOf("tri://127.0.0.1:" + port + "/" + type.getName() + "?timeout=2000&retries=0&check=false")
                .setScopeModel(application.getInternalModule());
    }

    private static <T> Exporter<T> export(
            ApplicationModel application,
            Protocol protocol,
            ProxyFactory proxy,
            Class<T> type,
            T implementation,
            URL url) {
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

    private static <T> T reference(
            ApplicationModel application,
            Protocol protocol,
            ProxyFactory proxy,
            Class<T> type,
            URL url,
            List<Invoker<?>> consumers)
            throws Exception {
        ServiceDescriptor descriptor =
                application.getInternalModule().getServiceRepository().registerService(type);
        url = url.setServiceModel(application.getInternalModule().registerInternalConsumer(type, url, descriptor));
        Invoker<T> invoker = protocol.refer(type, url);
        consumers.add(invoker);
        // This transport-entry fixture bypasses the SDK call gate; native construction is asynchronous.
        if (Boolean.TRUE.equals(url.getAttribute(ConnectionStateProvider.ASYNC_CONNECT_ATTRIBUTE))) {
            await(invoker::isAvailable);
        }
        return proxy.getProxy(invoker);
    }

    private static URL clientUrl(XdsRuntimePool.Lease runtime, URL url) {
        Snapshot snapshot = mock(Snapshot.class);
        Cluster selected = cluster(common(PRINCIPAL));
        when(snapshot.getClusters()).thenReturn(Collections.singletonMap(selected.getName(), selected));
        when(snapshot.getTarget()).thenReturn(new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "tri"));
        LbEndpoint lb = LbEndpoint.newBuilder()
                .setEndpoint(Endpoint.newBuilder()
                        .setAddress(Address.newBuilder()
                                .setSocketAddress(SocketAddress.newBuilder()
                                        .setAddress(url.getHost())
                                        .setPortValue(url.getPort()))))
                .build();
        XdsEndpoint endpoint = new XdsEndpoint(
                snapshot, selected, LocalityLbEndpoints.getDefaultInstance(), lb, runtime.getSecurity());
        return NativeTlsAdapter.decorate(url.putAttribute(XdsEndpoint.ATTRIBUTE, endpoint));
    }

    static String bootstrapJson(Path directory, int adsPort, String node) {
        return "{\"xds_servers\":[{\"server_uri\":\"127.0.0.1:" + adsPort
                + "\",\"channel_creds\":[{\"type\":\"insecure\"}],\"server_features\":[\"xds_v3\"]}],"
                + "\"node\":{\"id\":\"" + node + "\"},"
                + "\"server_listener_resource_name_template\":\"xds.istio.io/grpc/lds/inbound/%s\","
                + "\"certificate_providers\":{\"default\":{\"plugin_name\":\"file_watcher\",\"config\":{"
                + "\"certificate_file\":\"" + directory.resolve("cert.pem")
                + "\",\"private_key_file\":\"" + directory.resolve("key.pem")
                + "\",\"ca_certificate_file\":\"" + directory.resolve("root.pem")
                + "\",\"refresh_interval\":\"0.1s\"}}}}";
    }

    static void writeIdentity(Path path, Identity identity, Authority authority) throws Exception {
        Files.write(path.resolve("cert.pem"), pem(identity.certificate));
        Files.write(path.resolve("key.pem"), pem(identity.keys.getPrivate()));
        Files.write(path.resolve("root.pem"), pem(authority.certificate));
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < end) {
            Thread.sleep(20);
        }
        assertTrue(condition.getAsBoolean(), "Timed out waiting for policy application");
    }

    private static final class Ads implements AutoCloseable {
        final BlockingQueue<DiscoveryRequest> requests = new LinkedBlockingQueue<>();
        final AtomicInteger streams = new AtomicInteger();
        final Server server;
        volatile StreamObserver<DiscoveryResponse> response;

        Ads(Listener initial) throws Exception {
            server = NettyServerBuilder.forPort(0)
                    .addService(new AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceImplBase() {
                        @Override
                        public StreamObserver<DiscoveryRequest> streamAggregatedResources(
                                StreamObserver<DiscoveryResponse> observer) {
                            response = observer;
                            streams.incrementAndGet();
                            return new StreamObserver<DiscoveryRequest>() {
                                boolean initialized;

                                @Override
                                public void onNext(DiscoveryRequest request) {
                                    requests.add(request);
                                    if (!initialized
                                            && request.getResourceNamesList().contains(initial.getName())) {
                                        initialized = true;
                                        send("initial", initial);
                                    }
                                }

                                @Override
                                public void onError(Throwable error) {}

                                @Override
                                public void onCompleted() {
                                    observer.onCompleted();
                                }
                            };
                        }
                    })
                    .build()
                    .start();
        }

        void send(String version, Message... listeners) {
            DiscoveryResponse.Builder result = DiscoveryResponse.newBuilder()
                    .setTypeUrl(XdsResourceType.LDS.getTypeUrl())
                    .setVersionInfo(version)
                    .setNonce(version);
            Arrays.stream(listeners).forEach(listener -> result.addResources(Any.pack(listener)));
            response.onNext(result.build());
        }

        DiscoveryRequest await(String nonce) throws Exception {
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < end) {
                DiscoveryRequest request = requests.poll(100, TimeUnit.MILLISECONDS);
                if (request != null && nonce.equals(request.getResponseNonce())) {
                    return request;
                }
            }
            throw new AssertionError("Timed out waiting for ACK/NACK");
        }

        @Override
        public void close() throws Exception {
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
