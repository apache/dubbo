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
import org.apache.dubbo.common.ssl.CertManager;
import org.apache.dubbo.common.ssl.CertProvider;
import org.apache.dubbo.remoting.transport.netty4.ssl.SslContexts;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;
import org.apache.dubbo.xds.client.XdsRuntimePool;
import org.apache.dubbo.xds.registry.DiscoveryTarget;
import org.apache.dubbo.xds.registry.XdsEndpoint;
import org.apache.dubbo.xds.security.CertificateFiles.Material;
import org.apache.dubbo.xds.security.CertificateFiles.Status;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.UnknownFieldSet;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Address;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.TransportSocket;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.Endpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.FilterChain;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CertificateProviderPluginInstance;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CertificateValidationContext;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CommonTlsContext;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.DownstreamTlsContext;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.UpstreamTlsContext;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.StringMatcher;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XdsSecurityTest {
    private static final String PRINCIPAL = "spiffe://cluster.local/ns/test/sa/provider";
    private static final AtomicLong SERIAL = new AtomicLong(1);

    @TempDir
    Path directory;

    @Test
    void trustRequiresBothTrustedChainAndMatchingSan() throws Exception {
        Authority ca = new Authority();
        Identity identity = ca.identity(PRINCIPAL);
        CertificateValidationContext validation = validation(PRINCIPAL);
        XdsTrustManager trust = new XdsTrustManager(pem(ca.certificate), validation);
        trust.checkServerTrusted(new X509Certificate[] {identity.certificate}, "RSA");
        trust.checkClientTrusted(new X509Certificate[] {identity.certificate}, "RSA");
        assertThrows(
                CertificateException.class, () -> new XdsTrustManager(pem(ca.certificate), validation("spiffe://wrong"))
                        .checkServerTrusted(new X509Certificate[] {identity.certificate}, "RSA"));
        Authority stranger = new Authority();
        assertThrows(CertificateException.class, () -> new XdsTrustManager(pem(stranger.certificate), validation)
                .checkServerTrusted(new X509Certificate[] {identity.certificate}, "RSA"));
        assertThrows(IllegalArgumentException.class, () -> new XdsTrustManager(new byte[0], validation));
    }

    @Test
    void certificateRotationRetainsCoherentSnapshotAndNormalizesAgentKey() throws Exception {
        Authority ca = new Authority();
        Identity first = ca.identity(PRINCIPAL);
        Identity second = ca.identity(PRINCIPAL);
        writeIdentity(first, ca);
        CertificateFiles files =
                new CertificateFiles(bootstrap().getCertificateProviders().get("default"));
        assertTrue(files.refresh());
        Material old = files.current();
        assertTrue(new String(old.key, StandardCharsets.US_ASCII).contains("BEGIN PRIVATE KEY"));
        Files.write(directory.resolve("cert.pem"), pem(second.certificate));
        assertFalse(files.refresh());
        assertSame(old, files.current());
        assertNotNull(files.getLastError());
        Files.write(directory.resolve("key.pem"), pem(second.keys.getPrivate()));
        assertTrue(files.refresh());
        assertNotEquals(old.getFingerprint(), files.current().getFingerprint());
        Material rotated = files.current();
        Files.write(directory.resolve("key.pem"), new byte[1024 * 1024 + 1]);
        assertFalse(files.refresh());
        assertSame(rotated, files.current());
    }

    @Test
    void certificateInspectionKeepsAcceptedValidityAndClearsRecoveredRefreshErrors() throws Exception {
        Authority ca = new Authority();
        Identity identity = ca.identity(PRINCIPAL);
        writeIdentity(identity, ca);
        CertificateFiles files =
                new CertificateFiles(bootstrap().getCertificateProviders().get("default"));
        assertEquals(Status.State.NOT_LOADED, files.inspect().getState());
        assertNull(files.inspect().getIdentityNotAfterMillis());
        assertTrue(files.refresh());
        Status accepted = files.inspect();
        assertEquals(Status.State.READY, accepted.getState());
        assertEquals(identity.certificate.getNotBefore().getTime(), accepted.getIdentityNotBeforeMillis());
        assertEquals(identity.certificate.getNotAfter().getTime(), accepted.getIdentityNotAfterMillis());
        assertNotNull(accepted.getLastSuccessfulRefreshMillis());
        assertNull(accepted.getLastRefreshError());
        assertFalse(files.refresh());
        assertEquals(accepted.getLastSuccessfulRefreshMillis(), files.inspect().getLastSuccessfulRefreshMillis());
        Material retained = files.current();
        Files.delete(directory.resolve("key.pem"));
        assertFalse(files.refresh());
        Status interrupted = files.inspect();
        assertEquals(Status.State.READY, interrupted.getState());
        assertEquals("NoSuchFileException", interrupted.getLastRefreshError());
        assertEquals(accepted.getIdentityNotAfterMillis(), interrupted.getIdentityNotAfterMillis());
        assertSame(retained, files.current());
        assertNull(accepted.getLastRefreshError());
        Files.write(directory.resolve("key.pem"), pem(identity.keys.getPrivate()));
        assertFalse(files.refresh(), "Same accepted bytes do not create a new generation");
        assertEquals(Status.State.READY, files.inspect().getState());
        assertNull(files.inspect().getLastRefreshError());
        assertEquals(accepted.getLastSuccessfulRefreshMillis(), files.inspect().getLastSuccessfulRefreshMillis());
        assertEquals("NoSuchFileException", interrupted.getLastRefreshError());
    }

    @Test
    void runtimeCertificateInspectionDoesNotLoadFilesAndSharedOwnersObserveRecovery() throws Exception {
        BootstrapInfo bootstrap = bootstrap("900s");
        try (XdsRuntimePool.Lease first = XdsRuntimePool.acquire(bootstrap);
                XdsRuntimePool.Lease second = XdsRuntimePool.acquire(bootstrap)) {
            Map<String, Status> unloaded = first.getSecurity().inspectCertificateProviders();
            assertEquals(Status.State.NOT_LOADED, unloaded.get("default").getState());
            assertNull(unloaded.get("default").getLastRefreshError());
            assertThrows(UnsupportedOperationException.class, unloaded::clear);
            assertEquals(0, first.getSecurity().getEpoch());
            TlsPolicy policy = TlsPolicy.client(cluster(common(PRINCIPAL)), bootstrap);
            assertThrows(IllegalStateException.class, () -> first.getSecurity().certificate(policy));
            Status missing = second.getSecurity().inspectCertificateProviders().get("default");
            assertEquals(Status.State.UNAVAILABLE, missing.getState());
            assertEquals("NoSuchFileException", missing.getLastRefreshError());
            assertNull(missing.getLastSuccessfulRefreshMillis());
            Authority ca = new Authority();
            CountDownLatch ready = new CountDownLatch(1);
            try (AutoCloseable recovery = second.getSecurity().watchCertificates(ready::countDown)) {
                writeIdentity(ca.identity(PRINCIPAL), ca);
                assertTrue(ready.await(5, TimeUnit.SECONDS));
            }
            assertEquals(
                    Status.State.READY,
                    first.getSecurity()
                            .inspectCertificateProviders()
                            .get("default")
                            .getState());
            assertNull(second.getSecurity()
                    .inspectCertificateProviders()
                    .get("default")
                    .getLastRefreshError());
            assertEquals(Status.State.NOT_LOADED, unloaded.get("default").getState());
            second.getSecurity().close();
            assertTrue(first.getSecurity().inspectCertificateProviders().isEmpty());
        }
    }

    @Test
    void expiredAcceptedIdentityRemainsObservableButCannotBeUsed() throws Exception {
        Authority ca = new Authority();
        long now = System.currentTimeMillis();
        Identity identity = ca.identity(PRINCIPAL, now - 60_000, now + 5000);
        writeIdentity(identity, ca);
        CertificateFiles files =
                new CertificateFiles(bootstrap().getCertificateProviders().get("default"));
        assertTrue(files.refresh());
        Status ready = files.inspect();
        long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(6);
        while (System.currentTimeMillis() <= identity.certificate.getNotAfter().getTime() && System.nanoTime() < limit)
            Thread.sleep(20);
        Status expired = files.inspect();
        assertEquals(Status.State.EXPIRED, expired.getState());
        assertEquals(ready.getIdentityNotAfterMillis(), expired.getIdentityNotAfterMillis());
        assertThrows(IllegalStateException.class, files::current);
        assertFalse(files.refresh());
        Status unavailable = files.inspect();
        assertEquals(Status.State.UNAVAILABLE, unavailable.getState());
        assertEquals("CertificateExpiredException", unavailable.getLastRefreshError());
        assertEquals(ready.getIdentityNotAfterMillis(), unavailable.getIdentityNotAfterMillis());
        assertEquals(Status.State.READY, ready.getState());
        writeIdentity(ca.identity(PRINCIPAL), ca);
        assertTrue(files.refresh());
        assertEquals(Status.State.READY, files.inspect().getState());
        assertNull(files.inspect().getLastRefreshError());
    }

    @Test
    void rejectsConflictingProvidersAndUnknownSecurityRestrictions() throws Exception {
        BootstrapInfo bootstrap = bootstrap();
        CommonTlsContext common = common(PRINCIPAL);
        assertTrue(TlsPolicy.client(cluster(common), bootstrap).isTls());
        CommonTlsContext conflicting = common.toBuilder()
                .setTlsCertificateCertificateProviderInstance(CommonTlsContext.CertificateProviderInstance.newBuilder()
                        .setInstanceName("other")
                        .setCertificateName("default"))
                .build();
        assertThrows(IllegalArgumentException.class, () -> TlsPolicy.client(cluster(conflicting), bootstrap));
        CommonTlsContext mismatchedRoot = common.toBuilder()
                .setCombinedValidationContext(common.getCombinedValidationContext().toBuilder()
                        .setValidationContextCertificateProviderInstance(
                                CommonTlsContext.CertificateProviderInstance.newBuilder()
                                        .setInstanceName("default")
                                        .setCertificateName("different")))
                .build();
        assertThrows(IllegalArgumentException.class, () -> TlsPolicy.client(cluster(mismatchedRoot), bootstrap));
        CommonTlsContext unknown = common.toBuilder()
                .setUnknownFields(UnknownFieldSet.newBuilder()
                        .addField(
                                10000,
                                UnknownFieldSet.Field.newBuilder().addVarint(1).build())
                        .build())
                .build();
        assertThrows(IllegalArgumentException.class, () -> TlsPolicy.client(cluster(unknown), bootstrap));
        CertificateValidationContext unsupported =
                common.getCombinedValidationContext().getDefaultValidationContext().toBuilder()
                        .setRequireSignedCertificateTimestamp(BoolValue.of(true))
                        .build();
        CommonTlsContext ct = common.toBuilder()
                .setCombinedValidationContext(
                        common.getCombinedValidationContext().toBuilder().setDefaultValidationContext(unsupported))
                .build();
        assertThrows(IllegalArgumentException.class, () -> TlsPolicy.client(cluster(ct), bootstrap));
    }

    @Test
    void realMutualTlsHandshakeRejectsWrongSanAndMissingClientIdentity() throws Exception {
        Authority ca = new Authority();
        Identity identity = ca.identity(PRINCIPAL);
        writeIdentity(identity, ca);
        BootstrapInfo bootstrap = bootstrap();
        FilterChain inbound = FilterChain.newBuilder()
                .setTransportSocket(TransportSocket.newBuilder()
                        .setName("envoy.transport_sockets.tls")
                        .setTypedConfig(Any.pack(DownstreamTlsContext.newBuilder()
                                .setCommonTlsContext(common(PRINCIPAL))
                                .setRequireClientCertificate(BoolValue.of(true))
                                .build())))
                .build();
        try (XdsRuntimePool.Lease lease = XdsRuntimePool.acquire(bootstrap)) {
            NativeCertificate server = lease.getSecurity().certificate(TlsPolicy.server(inbound, bootstrap));
            NativeCertificate client =
                    lease.getSecurity().certificate(TlsPolicy.client(cluster(common(PRINCIPAL)), bootstrap));
            SslContext serverContext = SslContexts.buildServerSslContext(server);
            assertTrue(handshake(serverContext, client, true));
            NativeCertificate wrong =
                    lease.getSecurity().certificate(TlsPolicy.client(cluster(common("spiffe://wrong")), bootstrap));
            assertFalse(handshake(serverContext, wrong, true));
            assertFalse(handshake(serverContext, client, false));
        }
        assertEquals(0, XdsRuntimePool.activeRuntimeCount());
    }

    @Test
    void agentIntermediateChainWithAppendedRootsBuildsNativeTls() throws Exception {
        Authority root = new Authority();
        Authority other = new Authority();
        Authority intermediate = new Authority(root);
        Identity identity = intermediate.identity(PRINCIPAL);
        byte[] roots = concatenate(pem(root.certificate), pem(other.certificate));
        Files.write(
                directory.resolve("cert.pem"),
                concatenate(
                        pem(identity.certificate),
                        pem(intermediate.certificate),
                        pem(root.certificate),
                        pem(root.certificate),
                        pem(other.certificate)));
        Files.write(directory.resolve("key.pem"), pem(identity.keys.getPrivate()));
        Files.write(directory.resolve("root.pem"), roots);
        BootstrapInfo bootstrap = bootstrap();
        FilterChain inbound = FilterChain.newBuilder()
                .setTransportSocket(TransportSocket.newBuilder()
                        .setName("envoy.transport_sockets.tls")
                        .setTypedConfig(Any.pack(DownstreamTlsContext.newBuilder()
                                .setCommonTlsContext(common(PRINCIPAL))
                                .setRequireClientCertificate(BoolValue.of(true))
                                .build())))
                .build();
        try (XdsRuntimePool.Lease lease = XdsRuntimePool.acquire(bootstrap)) {
            NativeCertificate server = lease.getSecurity().certificate(TlsPolicy.server(inbound, bootstrap));
            NativeCertificate client =
                    lease.getSecurity().certificate(TlsPolicy.client(cluster(common(PRINCIPAL)), bootstrap));
            assertTrue(handshake(SslContexts.buildServerSslContext(server), client, true));
            assertArrayEquals(roots, client.getTrustCert());
            NativeCertificate wrongSan =
                    lease.getSecurity().certificate(TlsPolicy.client(cluster(common("spiffe://wrong")), bootstrap));
            assertFalse(handshake(SslContexts.buildServerSslContext(server), wrongSan, true));
            assertThrows(CertificateException.class, () -> new XdsTrustManager(
                            pem(other.certificate), validation(PRINCIPAL))
                    .checkServerTrusted(new X509Certificate[] {identity.certificate, intermediate.certificate}, "RSA"));
        }
        assertEquals(0, XdsRuntimePool.activeRuntimeCount());
    }

    @Test
    void appendedUnrelatedWorkloadCertificateIsNotAnIdentityChain() throws Exception {
        Authority root = new Authority();
        Authority other = new Authority();
        Identity identity = root.identity(PRINCIPAL);
        Identity unrelated = other.identity(PRINCIPAL);
        writeIdentity(identity, root);
        Files.write(
                directory.resolve("cert.pem"),
                concatenate(pem(identity.certificate), pem(root.certificate), pem(unrelated.certificate)));
        Files.write(directory.resolve("root.pem"), concatenate(pem(root.certificate), pem(other.certificate)));
        BootstrapInfo bootstrap = bootstrap();
        try (XdsRuntimePool.Lease lease = XdsRuntimePool.acquire(bootstrap)) {
            assertThrows(IllegalArgumentException.class, () -> lease.getSecurity()
                    .certificate(TlsPolicy.client(cluster(common(PRINCIPAL)), bootstrap)));
        }
    }

    @Test
    void retiredRootAppendedToIdentityDoesNotRestoreTrust() throws Exception {
        Authority root = new Authority();
        Authority retired = new Authority();
        Authority intermediate = new Authority(root);
        Identity identity = intermediate.identity(PRINCIPAL);
        writeIdentity(identity, root);
        // ROOTCA can retire A before the next CSR replaces a chain ending in the former A+B bundle.
        Files.write(
                directory.resolve("cert.pem"),
                concatenate(
                        pem(identity.certificate),
                        pem(intermediate.certificate),
                        pem(root.certificate),
                        pem(retired.certificate),
                        pem(root.certificate)));
        BootstrapInfo bootstrap = bootstrap();
        FilterChain inbound = FilterChain.newBuilder()
                .setTransportSocket(TransportSocket.newBuilder()
                        .setName("envoy.transport_sockets.tls")
                        .setTypedConfig(Any.pack(DownstreamTlsContext.newBuilder()
                                .setCommonTlsContext(common(PRINCIPAL))
                                .setRequireClientCertificate(BoolValue.of(true))
                                .build())))
                .build();
        try (XdsRuntimePool.Lease lease = XdsRuntimePool.acquire(bootstrap)) {
            NativeCertificate server = lease.getSecurity().certificate(TlsPolicy.server(inbound, bootstrap));
            NativeCertificate client =
                    lease.getSecurity().certificate(TlsPolicy.client(cluster(common(PRINCIPAL)), bootstrap));
            assertTrue(handshake(SslContexts.buildServerSslContext(server), client, true));
            assertArrayEquals(pem(root.certificate), client.getTrustCert());
            Identity retiredIdentity = retired.identity(PRINCIPAL);
            assertThrows(
                    CertificateException.class, () -> new XdsTrustManager(client.getTrustCert(), validation(PRINCIPAL))
                            .checkClientTrusted(new X509Certificate[] {retiredIdentity.certificate}, "RSA"));
        }
    }

    private static byte[] concatenate(byte[]... certificates) throws Exception {
        StringWriter output = new StringWriter();
        for (byte[] certificate : certificates) output.write(new String(certificate, StandardCharsets.US_ASCII));
        return output.toString().getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void nativeCertProviderBuildsTlsAndSeparatesPoliciesAtTheSameSocket() throws Exception {
        Authority ca = new Authority();
        writeIdentity(ca.identity(PRINCIPAL), ca);
        FrameworkModel framework = new FrameworkModel();
        framework.newApplication();
        try (XdsRuntimePool.Lease lease = XdsRuntimePool.acquire(bootstrap())) {
            URL first = clientUrl(lease, cluster(common(PRINCIPAL)), framework);
            URL other = clientUrl(lease, cluster(common("spiffe://different-policy")), framework);
            assertTrue(framework.getExtensionLoader(CertProvider.class).hasExtension("xds"));
            assertTrue(framework
                    .getExtensionLoader(CertProvider.class)
                    .getExtension("xds")
                    .isSupport(first));
            assertNotNull(SslContexts.buildClientSslContext(first));
            assertNotNull(SslContexts.buildClientSslContext(other));
            assertEquals(first.getAddress(), other.getAddress());
            assertNotEquals(first.getAttribute("connection.identity"), other.getAttribute("connection.identity"));
            assertEquals("echo.test.svc.cluster.local", first.getAttribute("ssl.server-name"));
            CertManager manager = framework.getBeanFactory().getBean(CertManager.class);
            assertSame(NativeTlsAdapter.endpoint(first).getCertificate(), manager.getConsumerConnectionConfig(first));
            URL plaintext = clientUrl(
                    lease,
                    cluster(common(PRINCIPAL)).toBuilder()
                            .clearTransportSocket()
                            .build(),
                    framework);
            assertNull(SslContexts.buildClientSslContext(plaintext));
            assertFalse(manager.getConsumerConnectionConfig(plaintext).isTlsEnabled());
        } finally {
            framework.destroy();
        }
    }

    @Test
    void sharedRuntimeNotifiesOnlyAfterAValidCertificateRotation() throws Exception {
        Authority ca = new Authority();
        Identity first = ca.identity(PRINCIPAL);
        Identity next = ca.identity(PRINCIPAL);
        writeIdentity(first, ca);
        BootstrapInfo bootstrap = bootstrap();
        try (XdsRuntimePool.Lease owner = XdsRuntimePool.acquire(bootstrap);
                XdsRuntimePool.Lease secondOwner = XdsRuntimePool.acquire(bootstrap)) {
            assertSame(owner.getSecurity(), secondOwner.getSecurity());
            assertEquals(1, XdsRuntimePool.activeRuntimeCount());
            TlsPolicy policy = TlsPolicy.client(cluster(common(PRINCIPAL)), bootstrap);
            NativeCertificate old = owner.getSecurity().certificate(policy);
            CountDownLatch rotated = new CountDownLatch(1);
            try (AutoCloseable watch = owner.getSecurity().watchCertificates(rotated::countDown)) {
                long epoch = owner.getSecurity().getEpoch();
                Files.write(directory.resolve("cert.pem"), pem(next.certificate));
                assertFalse(rotated.await(300, TimeUnit.MILLISECONDS));
                assertEquals(epoch, owner.getSecurity().getEpoch());
                Files.write(directory.resolve("key.pem"), pem(next.keys.getPrivate()));
                assertTrue(rotated.await(5, TimeUnit.SECONDS));
                assertTrue(owner.getSecurity().getEpoch() > epoch);
                assertNotEquals(
                        old.getVersion(),
                        secondOwner.getSecurity().certificate(policy).getVersion());
                old.checkValidity();
            }
        }
        assertEquals(0, XdsRuntimePool.activeRuntimeCount());
    }

    @Test
    void independentRootRolloverSupportsOverlapAndRejectsRetiredPeers() throws Exception {
        Authority oldAuthority = new Authority();
        Authority newAuthority = new Authority();
        Identity oldIdentity = oldAuthority.identity(PRINCIPAL);
        Identity newIdentity = newAuthority.identity(PRINCIPAL);
        writeIdentity(oldIdentity, oldAuthority);
        BootstrapInfo bootstrap = bootstrap();
        FilterChain inbound = FilterChain.newBuilder()
                .setTransportSocket(TransportSocket.newBuilder()
                        .setName("envoy.transport_sockets.tls")
                        .setTypedConfig(Any.pack(DownstreamTlsContext.newBuilder()
                                .setCommonTlsContext(common(PRINCIPAL))
                                .setRequireClientCertificate(BoolValue.of(true))
                                .build())))
                .build();
        try (XdsRuntimePool.Lease lease = XdsRuntimePool.acquire(bootstrap)) {
            TlsPolicy clientPolicy = TlsPolicy.client(cluster(common(PRINCIPAL)), bootstrap);
            TlsPolicy serverPolicy = TlsPolicy.server(inbound, bootstrap);
            NativeCertificate initialClient = lease.getSecurity().certificate(clientPolicy);
            assertTrue(handshake(
                    SslContexts.buildServerSslContext(lease.getSecurity().certificate(serverPolicy)),
                    initialClient,
                    true));

            // Roots alone change first. The identity remains A while both independent CAs are trusted.
            long epoch = lease.getSecurity().getEpoch();
            byte[] overlap = (new String(pem(oldAuthority.certificate), StandardCharsets.US_ASCII)
                            + new String(pem(newAuthority.certificate), StandardCharsets.US_ASCII))
                    .getBytes(StandardCharsets.US_ASCII);
            Files.write(directory.resolve("root.pem"), overlap);
            awaitEpoch(lease, epoch);
            NativeCertificate overlapClient = lease.getSecurity().certificate(clientPolicy);
            NativeCertificate overlapServer = lease.getSecurity().certificate(serverPolicy);
            assertArrayEquals(initialClient.getKeyCertChain(), overlapClient.getKeyCertChain());
            assertFalse(initialClient.getVersion().equals(overlapClient.getVersion()));
            assertTrue(handshake(SslContexts.buildServerSslContext(overlapServer), overlapClient, true));

            // Issue B's identity while A+B remain trusted: both directions of mixed A/B mTLS must work.
            epoch = lease.getSecurity().getEpoch();
            Files.write(directory.resolve("cert.pem"), pem(newIdentity.certificate));
            Files.write(directory.resolve("key.pem"), pem(newIdentity.keys.getPrivate()));
            awaitEpoch(lease, epoch);
            NativeCertificate newClient = lease.getSecurity().certificate(clientPolicy);
            NativeCertificate newServer = lease.getSecurity().certificate(serverPolicy);
            assertTrue(handshake(SslContexts.buildServerSslContext(overlapServer), newClient, true));
            assertTrue(handshake(SslContexts.buildServerSslContext(newServer), overlapClient, true));
            assertFalse(handshake(SslContexts.buildServerSslContext(newServer), initialClient, true));

            // Retire A without changing B's identity. New contexts must reject either old peer role.
            epoch = lease.getSecurity().getEpoch();
            Files.write(directory.resolve("root.pem"), pem(newAuthority.certificate));
            awaitEpoch(lease, epoch);
            NativeCertificate finalClient = lease.getSecurity().certificate(clientPolicy);
            NativeCertificate finalServer = lease.getSecurity().certificate(serverPolicy);
            assertArrayEquals(newClient.getKeyCertChain(), finalClient.getKeyCertChain());
            assertFalse(newClient.getVersion().equals(finalClient.getVersion()));
            assertTrue(handshake(SslContexts.buildServerSslContext(finalServer), finalClient, true));
            assertFalse(handshake(SslContexts.buildServerSslContext(finalServer), overlapClient, true));
            assertFalse(handshake(SslContexts.buildServerSslContext(overlapServer), finalClient, true));
            NativeCertificate wrongSan =
                    lease.getSecurity().certificate(TlsPolicy.client(cluster(common("spiffe://wrong")), bootstrap));
            assertFalse(handshake(SslContexts.buildServerSslContext(finalServer), wrongSan, true));
        }
        assertEquals(0, XdsRuntimePool.activeRuntimeCount());
    }

    private static void awaitEpoch(XdsRuntimePool.Lease lease, long previous) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (lease.getSecurity().getEpoch() <= previous && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(lease.getSecurity().getEpoch() > previous, "Certificate update did not notify the shared runtime");
    }

    @Test
    void missingAgentFilesRecoverWithoutWaitingForTheNormalRefreshInterval() throws Exception {
        Authority ca = new Authority();
        BootstrapInfo bootstrap = bootstrap("900s");
        try (XdsRuntimePool.Lease lease = XdsRuntimePool.acquire(bootstrap)) {
            TlsPolicy policy = TlsPolicy.client(cluster(common(PRINCIPAL)), bootstrap);
            assertThrows(IllegalStateException.class, () -> lease.getSecurity().certificate(policy));
            CountDownLatch ready = new CountDownLatch(1);
            try (AutoCloseable watch = lease.getSecurity().watchCertificates(ready::countDown)) {
                writeIdentity(ca.identity(PRINCIPAL), ca);
                assertTrue(ready.await(5, TimeUnit.SECONDS));
                assertTrue(lease.getSecurity().certificate(policy).isTlsEnabled());
            }
        }
    }

    private static URL clientUrl(XdsRuntimePool.Lease lease, Cluster cluster, FrameworkModel framework) {
        Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getClusters()).thenReturn(Collections.singletonMap(cluster.getName(), cluster));
        when(snapshot.getTarget()).thenReturn(new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "tri"));
        LbEndpoint lb = LbEndpoint.newBuilder()
                .setEndpoint(Endpoint.newBuilder()
                        .setAddress(Address.newBuilder()
                                .setSocketAddress(SocketAddress.newBuilder()
                                        .setAddress("127.0.0.1")
                                        .setPortValue(17070))))
                .build();
        XdsEndpoint endpoint =
                new XdsEndpoint(snapshot, cluster, LocalityLbEndpoints.getDefaultInstance(), lb, lease.getSecurity());
        URL url = URL.valueOf("tri://127.0.0.1:17070/SecurityTest")
                .setScopeModel(framework.getApplicationModels().get(0));
        url = url.putAttribute(XdsEndpoint.ATTRIBUTE, endpoint);
        return NativeTlsAdapter.decorate(url);
    }

    private static boolean handshake(SslContext server, NativeCertificate client, boolean identity) throws Exception {
        SslContextBuilder builder = SslContextBuilder.forClient()
                .protocols("TLSv1.2")
                .trustManager(client.getTrustManagerFactory())
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        ApplicationProtocolConfig.Protocol.ALPN,
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        "h2"));
        if (identity) {
            builder.keyManager(
                    new ByteArrayInputStream(client.getKeyCertChain()),
                    new ByteArrayInputStream(client.getPrivateKey()));
        }
        SslContext context = builder.build();
        EventLoopGroup group = new NioEventLoopGroup(2);
        Channel listener = null;
        Channel connection = null;
        try {
            listener = new ServerBootstrap()
                    .group(group)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            channel.pipeline().addLast(server.newHandler(channel.alloc()));
                        }
                    })
                    .bind("127.0.0.1", 0)
                    .sync()
                    .channel();
            connection = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            channel.pipeline().addLast(context.newHandler(channel.alloc()));
                        }
                    })
                    .connect((InetSocketAddress) listener.localAddress())
                    .sync()
                    .channel();
            SslHandler handler = connection.pipeline().get(SslHandler.class);
            assertTrue(handler.handshakeFuture().await(5, TimeUnit.SECONDS));
            if (handler.handshakeFuture().isSuccess()) {
                assertEquals("h2", handler.applicationProtocol());
            }
            return handler.handshakeFuture().isSuccess();
        } finally {
            if (connection != null) {
                connection.close().sync();
            }
            if (listener != null) {
                listener.close().sync();
            }
            group.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
        }
    }

    private BootstrapInfo bootstrap() throws Exception {
        return bootstrap("0.1s");
    }

    private BootstrapInfo bootstrap(String interval) throws Exception {
        return Bootstrapper.parse(
                "{\"xds_servers\":[{\"server_uri\":\"127.0.0.1:1\",\"channel_creds\":[{\"type\":\"insecure\"}],\"server_features\":[\"xds_v3\"]}],"
                        + "\"node\":{\"id\":\"security-test\"},\"certificate_providers\":{\"default\":{\"plugin_name\":\"file_watcher\",\"config\":{"
                        + "\"certificate_file\":\"" + directory.resolve("cert.pem") + "\",\"private_key_file\":\""
                        + directory.resolve("key.pem")
                        + "\",\"ca_certificate_file\":\"" + directory.resolve("root.pem")
                        + "\",\"refresh_interval\":\"" + interval + "\"}}}}");
    }

    private void writeIdentity(Identity identity, Authority ca) throws Exception {
        Files.write(directory.resolve("cert.pem"), pem(identity.certificate));
        Files.write(directory.resolve("key.pem"), pem(identity.keys.getPrivate()));
        Files.write(directory.resolve("root.pem"), pem(ca.certificate));
    }

    private static CertificateValidationContext validation(String principal) {
        return CertificateValidationContext.newBuilder()
                .addMatchSubjectAltNames(StringMatcher.newBuilder().setExact(principal))
                .build();
    }

    static CommonTlsContext common(String principal) {
        CertificateProviderPluginInstance identity = CertificateProviderPluginInstance.newBuilder()
                .setInstanceName("default")
                .setCertificateName("default")
                .build();
        CertificateValidationContext validation = validation(principal).toBuilder()
                .setCaCertificateProviderInstance(CertificateProviderPluginInstance.newBuilder()
                        .setInstanceName("default")
                        .setCertificateName("ROOTCA"))
                .build();
        return CommonTlsContext.newBuilder()
                .setTlsCertificateProviderInstance(identity)
                .setTlsCertificateCertificateProviderInstance(CommonTlsContext.CertificateProviderInstance.newBuilder()
                        .setInstanceName("default")
                        .setCertificateName("default"))
                .setCombinedValidationContext(CommonTlsContext.CombinedCertificateValidationContext.newBuilder()
                        .setDefaultValidationContext(validation)
                        .setValidationContextCertificateProviderInstance(
                                CommonTlsContext.CertificateProviderInstance.newBuilder()
                                        .setInstanceName("default")
                                        .setCertificateName("ROOTCA")))
                .build();
    }

    static Cluster cluster(CommonTlsContext context) {
        return Cluster.newBuilder()
                .setName("outbound|7070||echo.test.svc.cluster.local")
                .setTransportSocket(TransportSocket.newBuilder()
                        .setName("envoy.transport_sockets.tls")
                        .setTypedConfig(Any.pack(UpstreamTlsContext.newBuilder()
                                .setCommonTlsContext(context)
                                .setSni("echo.test.svc.cluster.local")
                                .build())))
                .build();
    }

    static byte[] pem(Object value) throws Exception {
        StringWriter output = new StringWriter();
        try (JcaPEMWriter writer = new JcaPEMWriter(output)) {
            writer.writeObject(value);
        }
        return output.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private static KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    static final class Identity {
        final KeyPair keys;
        final X509Certificate certificate;

        Identity(KeyPair keys, X509Certificate certificate) {
            this.keys = keys;
            this.certificate = certificate;
        }
    }

    static final class Authority {
        final KeyPair keys = keyPair();
        final X500Name name = new X500Name("CN=xds-test-root-" + SERIAL.getAndIncrement());
        final X509Certificate certificate;

        Authority() throws Exception {
            this(null);
        }

        Authority(Authority parent) throws Exception {
            JcaX509v3CertificateBuilder builder = builder(parent == null ? name : parent.name, name, keys);
            builder.addExtension(
                    Extension.basicConstraints,
                    true,
                    parent == null ? new BasicConstraints(true) : new BasicConstraints(0));
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
            certificate = parent == null ? sign(builder) : parent.sign(builder);
        }

        Identity identity(String principal) throws Exception {
            long now = System.currentTimeMillis();
            return identity(principal, now - 60_000, now + 3_600_000);
        }

        Identity identity(String principal, long notBefore, long notAfter) throws Exception {
            KeyPair leaf = keyPair();
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    name,
                    BigInteger.valueOf(SERIAL.getAndIncrement()),
                    new Date(notBefore),
                    new Date(notAfter),
                    new X500Name("CN=xds-test-workload"),
                    leaf.getPublic());
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            builder.addExtension(
                    Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
            builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[] {
                KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth
            }));
            builder.addExtension(
                    Extension.subjectAlternativeName,
                    false,
                    new GeneralNames(new GeneralName(GeneralName.uniformResourceIdentifier, principal)));
            return new Identity(leaf, sign(builder));
        }

        private X509Certificate sign(JcaX509v3CertificateBuilder builder) throws Exception {
            return new JcaX509CertificateConverter()
                    .getCertificate(
                            builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(keys.getPrivate())));
        }

        private static JcaX509v3CertificateBuilder builder(X500Name issuer, X500Name subject, KeyPair pair) {
            long now = System.currentTimeMillis();
            return new JcaX509v3CertificateBuilder(
                    issuer,
                    BigInteger.valueOf(SERIAL.getAndIncrement()),
                    new Date(now - 60_000),
                    new Date(now + 3_600_000),
                    subject,
                    pair.getPublic());
        }
    }
}
