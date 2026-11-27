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
package org.apache.dubbo.xds.test;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.client.XdsResourceGraph;
import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;
import org.apache.dubbo.xds.client.XdsRuntimePool;
import org.apache.dubbo.xds.registry.DiscoveryTarget;
import org.apache.dubbo.xds.registry.XdsEndpoint;
import org.apache.dubbo.xds.security.NativeCertificate;
import org.apache.dubbo.xds.security.NativeTlsAdapter;
import org.apache.dubbo.xds.security.TlsPolicy;
import org.apache.dubbo.xds.security.XdsTrustManager;

import javax.net.ssl.TrustManagerFactory;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.alibaba.fastjson2.JSON;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CertificateValidationContext;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.StringMatcher;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;

/** Test-only direct endpoint checks; credentials remain in the injected Pod and are never reported. */
final class NativeSecurityProbe {
    private NativeSecurityProbe() {}

    static Map<String, Object> peer(Properties command) throws Exception {
        int port = Integer.parseInt(command.getProperty("port", "7070"));
        DiscoveryTarget target = new DiscoveryTarget(command.getProperty("service", ""), port, "tri");
        BootstrapInfo bootstrap = NativeXdsApplication.diagnosticBootstrap();
        CompletableFuture<Snapshot> ready = new CompletableFuture<>();
        try (XdsRuntimePool.Lease lease = XdsRuntimePool.acquire(bootstrap);
                XdsResourceGraph graph = new XdsResourceGraph(
                        lease.getClient(),
                        bootstrap,
                        target,
                        snapshot -> {
                            if (snapshot.isReady()) ready.complete(snapshot);
                        },
                        ready::completeExceptionally)) {
            Snapshot snapshot = ready.get(30, TimeUnit.SECONDS);
            Cluster cluster = snapshot.getClusters().get(target.defaultCluster());
            if (cluster == null) throw new IllegalStateException("Missing peer default cluster");
            String eds = cluster.getEdsClusterConfig().getServiceName();
            ClusterLoadAssignment assignment = snapshot.getEndpoints().get(eds.isEmpty() ? cluster.getName() : eds);
            if (assignment == null
                    || assignment.getEndpointsCount() != 1
                    || assignment.getEndpoints(0).getLbEndpointsCount() != 1) {
                throw new IllegalStateException("Peer probe requires one actual endpoint");
            }
            LocalityLbEndpoints locality = assignment.getEndpoints(0);
            XdsEndpoint endpoint =
                    new XdsEndpoint(snapshot, cluster, locality, locality.getLbEndpoints(0), lease.getSecurity());
            NativeCertificate certificate = endpoint.getCertificate();
            if (!certificate.isTlsEnabled()) throw new IllegalStateException("Peer probe requires xDS TLS");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "OBSERVED");
            result.put("target", target.toString());
            result.put("workload_port", endpoint.getPort());
            StringBuilder fingerprint = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(cluster.toByteArray())) {
                fingerprint.append(Character.forDigit((b >>> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
            }
            result.put("cluster_sha256", fingerprint.toString());
            result.put("actual_tls_policy", NativeXdsApplication.tlsDescription(TlsPolicy.client(cluster, bootstrap)));
            result.put("actual_xds", handshake(endpoint, certificate, true, null));
            result.put(
                    "pkix_only_test_control",
                    handshake(
                            endpoint,
                            certificate,
                            true,
                            new XdsTrustManager(
                                            certificate.getTrustCert(),
                                            CertificateValidationContext.getDefaultInstance())
                                    .factory()));
            result.put(
                    "control_scope",
                    "Test-only handshake without SAN constraints; normal References retain actual xDS policy");
            return result;
        }
    }

    static void run(String service, Path runtime) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig("native-security-probe"));
        try {
            BootstrapInfo bootstrap = Bootstrapper.load();
            CompletableFuture<Snapshot> ready = new CompletableFuture<>();
            try (XdsRuntimePool.Lease lease = XdsRuntimePool.acquire(bootstrap);
                    XdsResourceGraph graph = new XdsResourceGraph(
                            lease.getClient(),
                            bootstrap,
                            new DiscoveryTarget(service, 7070, "tri"),
                            snapshot -> {
                                if (snapshot.isReady()) ready.complete(snapshot);
                            },
                            ready::completeExceptionally)) {
                Snapshot snapshot = ready.get(30, TimeUnit.SECONDS);
                String name = "outbound|7070|v1|" + service;
                Cluster cluster = snapshot.getClusters().get(name);
                if (cluster == null) throw new IllegalStateException("Expected the native v1 cluster");
                String eds = cluster.getEdsClusterConfig().getServiceName();
                ClusterLoadAssignment assignment = snapshot.getEndpoints().get(eds.isEmpty() ? name : eds);
                LocalityLbEndpoints locality = assignment.getEndpoints(0);
                XdsEndpoint endpoint =
                        new XdsEndpoint(snapshot, cluster, locality, locality.getLbEndpoints(0), lease.getSecurity());
                NativeCertificate certificate = endpoint.getCertificate();
                if (!certificate.isTlsEnabled()) throw new IllegalStateException("Security probe requires xDS TLS");
                result.put("tls_required", true);
                result.put("cluster", name);
                result.put("workload_port", endpoint.getPort());
                result.put(
                        "business",
                        rpc(
                                application,
                                endpoint,
                                NativeEcho.class,
                                "native-xds",
                                "1.0.0",
                                remote -> remote.echo("native-security")));
                result.put(
                        "metadata",
                        rpc(
                                application,
                                endpoint,
                                MetadataService.class,
                                "native-xds-provider",
                                MetadataService.VERSION,
                                remote -> remote.getMetadataInfos().size()));
                result.put("tls_control", handshake(endpoint, certificate, true, null));
                result.put("no_client_certificate", handshake(endpoint, certificate, false, null));
                TrustManagerFactory wrongSan = new XdsTrustManager(
                                certificate.getTrustCert(),
                                CertificateValidationContext.newBuilder()
                                        .addMatchSubjectAltNames(
                                                StringMatcher.newBuilder()
                                                        .setExact(
                                                                "spiffe://cluster.local/ns/dubbo-xds-native/sa/nonexistent-server"))
                                        .build())
                        .factory();
                result.put("wrong_san", handshake(endpoint, certificate, true, wrongSan));
                result.put("san_override", "test-only expected identity; actual server certificate is unchanged");
                byte[] preface = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
                byte[] http2 = Arrays.copyOf(preface, preface.length + 9);
                http2[preface.length + 3] = 4; // Complete empty SETTINGS frame.
                result.put("plaintext_http2", plaintext(endpoint, http2));
                result.put(
                        "plaintext_http1",
                        plaintext(
                                endpoint, "GET / HTTP/1.1\r\nHost: echo\r\n\r\n".getBytes(StandardCharsets.US_ASCII)));
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                Serialization serialization =
                        application.getExtensionLoader(Serialization.class).getExtension("hessian2");
                ObjectOutput output = serialization.serialize(new URL("dubbo", endpoint.getHost(), 20880), body);
                output.writeObject(null);
                output.flushBuffer();
                byte[] binary = new byte[16 + body.size()];
                binary[0] = (byte) 0xda;
                binary[1] = (byte) 0xbb;
                binary[2] = (byte) (0xe0 | serialization.getContentTypeId()); // Request, two-way, event.
                ByteBuffer.wrap(binary).putInt(12, body.size());
                System.arraycopy(body.toByteArray(), 0, binary, 16, body.size());
                result.put("binary_heartbeat_control", binaryControl(endpoint, binary));
                result.put("binary_on_triple_port", plaintext(endpoint, binary));
                result.put("status", "OBSERVED");
            }
        } catch (Exception error) {
            result.put("status", "PROBE_ERROR");
            result.put("error_type", error.getClass().getSimpleName());
        } finally {
            framework.destroy();
        }
        Path temporary = runtime.resolve("security-result.tmp");
        Files.write(temporary, JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8));
        Files.move(temporary, runtime.resolve("security-result.json"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("NATIVE_SECURITY_PROBE_FINISHED status=" + result.get("status"));
        while (!Thread.currentThread().isInterrupted()) Thread.sleep(1000);
    }

    private static <T> Map<String, Object> rpc(
            ApplicationModel application,
            XdsEndpoint endpoint,
            Class<T> type,
            String group,
            String version,
            Function<T, Object> action)
            throws Exception {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("interface", type.getName());
        parameters.put("group", group);
        parameters.put("version", version);
        parameters.put("timeout", "2000");
        parameters.put("retries", "0");
        parameters.put("side", "consumer");
        parameters.put("reference.filter", "-default");
        URL url = new URL("tri", endpoint.getHost(), endpoint.getPort(), type.getName(), parameters)
                .setScopeModel(application.getInternalModule())
                .putAttribute(XdsEndpoint.ATTRIBUTE, endpoint);
        url = NativeTlsAdapter.decorate(url);
        ConsumerModel model = application
                .getInternalModule()
                .registerInternalConsumer(
                        type,
                        url,
                        application.getInternalModule().getServiceRepository().registerService(type));
        Invoker<T> invoker = null;
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            invoker = application
                    .getExtensionLoader(Protocol.class)
                    .getExtension("tri")
                    .refer(type, url.setServiceModel(model));
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
            while (!invoker.isAvailable()) {
                if (System.nanoTime() >= end)
                    throw new IllegalStateException("Direct TLS connection did not become ready");
                Thread.sleep(20);
            }
            result.put("connection_ready", true);
            T remote = application
                    .getExtensionLoader(ProxyFactory.class)
                    .getAdaptiveExtension()
                    .getProxy(invoker);
            try {
                result.put("value", action.apply(remote));
                result.put("status", "OK");
            } catch (Exception error) {
                result.put("status", TriRpcStatus.getStatus(error).code.name());
                result.put("error_type", error.getClass().getSimpleName());
            }
        } finally {
            if (invoker != null) invoker.destroy();
            application.getInternalModule().getServiceRepository().unregisterConsumer(model);
        }
        return result;
    }

    private static Map<String, Object> plaintext(XdsEndpoint endpoint, byte[] request) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()), 2000);
            socket.setSoTimeout(2000);
            result.put("connected", true);
            socket.getOutputStream().write(request);
            socket.getOutputStream().flush();
            result.put("status", socket.getInputStream().read() == -1 ? "CLOSED" : "RESPONSE_BYTES");
        } catch (SocketException closed) {
            if (!Boolean.TRUE.equals(result.get("connected"))) throw closed;
            result.put("status", "RESET");
        }
        return result;
    }

    private static Map<String, Object> binaryControl(XdsEndpoint endpoint, byte[] request) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.getHost(), 20880), 2000);
            socket.setSoTimeout(2000);
            socket.getOutputStream().write(request);
            socket.getOutputStream().flush();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put(
                    "status",
                    socket.getInputStream().read() == 0xda
                                    && socket.getInputStream().read() == 0xbb
                            ? "MAGIC_RESPONSE"
                            : "UNEXPECTED_RESPONSE");
            return result;
        }
    }

    private static Map<String, Object> handshake(
            XdsEndpoint endpoint, NativeCertificate certificate, boolean identity, TrustManagerFactory trust)
            throws Exception {
        SslContextBuilder builder = SslContextBuilder.forClient()
                .sslProvider(SslProvider.JDK)
                .trustManager(trust == null ? certificate.getTrustManagerFactory() : trust)
                .protocols("TLSv1.2")
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        ApplicationProtocolConfig.Protocol.ALPN,
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        "h2"));
        if (identity)
            builder.keyManager(certificate.getKeyCertChainInputStream(), certificate.getPrivateKeyInputStream());
        SslContext context = builder.build();
        EventLoopGroup threads = new NioEventLoopGroup(1);
        Channel channel = null;
        try {
            CompletableFuture<SslHandler> installed = new CompletableFuture<>();
            channel = new Bootstrap()
                    .group(threads)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel channel) {
                            SslHandler ssl = context.newHandler(
                                    channel.alloc(), certificate.getServerName(), endpoint.getPort());
                            ssl.setHandshakeTimeoutMillis(2000);
                            channel.pipeline().addLast(ssl);
                            installed.complete(ssl);
                        }
                    })
                    .connect(endpoint.getHost(), endpoint.getPort())
                    .sync()
                    .channel();
            SslHandler ssl = installed.get(2, TimeUnit.SECONDS);
            if (!ssl.handshakeFuture().await(3, TimeUnit.SECONDS))
                throw new IllegalStateException("TLS observation timed out");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", ssl.handshakeFuture().isSuccess() ? "OK" : "TLS_REJECTED");
            if (ssl.handshakeFuture().isSuccess()) {
                result.put("protocol", ssl.engine().getSession().getProtocol());
                result.put("alpn", ssl.applicationProtocol());
                result.put("tls", NativeLifecycleProbe.tls(channel));
            } else {
                List<String> types = new ArrayList<>();
                boolean mismatch = false;
                for (Throwable cause = ssl.handshakeFuture().cause(); cause != null; cause = cause.getCause()) {
                    types.add(cause.getClass().getSimpleName());
                    mismatch |= cause instanceof CertificateException
                            && "Peer certificate does not match xDS subject alternative names"
                                    .equals(cause.getMessage());
                }
                result.put("error_types", types);
                result.put("san_mismatch", mismatch);
            }
            return result;
        } finally {
            if (channel != null) channel.close().sync();
            threads.shutdownGracefully(0, 1, TimeUnit.SECONDS).sync();
        }
    }
}
