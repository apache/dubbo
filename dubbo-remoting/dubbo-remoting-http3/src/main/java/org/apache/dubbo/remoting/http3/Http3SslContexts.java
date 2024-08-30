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
package org.apache.dubbo.remoting.http3;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.ssl.AuthPolicy;
import org.apache.dubbo.common.ssl.Cert;
import org.apache.dubbo.common.ssl.CertManager;
import org.apache.dubbo.common.ssl.ProviderCert;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;

import java.io.InputStream;
import java.util.List;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.ApplicationProtocolNegotiator;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;

public final class Http3SslContexts extends SslContext {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(Http3SslContexts.class);

    private Http3SslContexts() {}

    public static QuicSslContext buildServerSslContext(URL url) {
        CertManager certManager = getCertManager(url);
        ProviderCert cert = certManager.getProviderConnectionConfig(url, url.toInetSocketAddress());
        if (cert == null) {
            return buildSelfSignedServerSslContext(url);
        }
        QuicSslContextBuilder builder;
        try {
            try (InputStream privateKeyIn = cert.getPrivateKeyInputStream();
                    InputStream keyCertChainIn = cert.getKeyCertChainInputStream()) {
                if (keyCertChainIn == null || privateKeyIn == null) {
                    return buildSelfSignedServerSslContext(url);
                }
                builder = QuicSslContextBuilder.forServer(
                        toPrivateKey(privateKeyIn, cert.getPassword()),
                        cert.getPassword(),
                        toX509Certificates(keyCertChainIn));
                try (InputStream trustCertIn = cert.getTrustCertInputStream()) {
                    if (trustCertIn != null) {
                        ClientAuth clientAuth = cert.getAuthPolicy() == AuthPolicy.CLIENT_AUTH
                                ? ClientAuth.REQUIRE
                                : ClientAuth.OPTIONAL;
                        builder.trustManager(toX509Certificates(trustCertIn)).clientAuth(clientAuth);
                    }
                }
            }
        } catch (IllegalStateException t) {
            throw t;
        } catch (Throwable t) {
            throw new IllegalArgumentException("Could not find certificate file or the certificate is invalid.", t);
        }
        try {
            return builder.applicationProtocols(Http3.supportedApplicationProtocols())
                    .build();
        } catch (Throwable t) {
            throw new IllegalStateException("Build SslSession failed.", t);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private static QuicSslContext buildSelfSignedServerSslContext(URL url) {
        LOGGER.info("Provider cert not configured, build self signed sslContext, url=[{}]", url.toString(""));
        SelfSignedCertificate certificate;
        try {
            certificate = new SelfSignedCertificate();
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to create self signed certificate, Please import bcpkix jar", e);
        }
        return QuicSslContextBuilder.forServer(certificate.privateKey(), null, certificate.certificate())
                .applicationProtocols(Http3.supportedApplicationProtocols())
                .build();
    }

    public static QuicSslContext buildClientSslContext(URL url) {
        CertManager certManager = getCertManager(url);
        Cert cert = certManager.getConsumerConnectionConfig(url);
        QuicSslContextBuilder builder = QuicSslContextBuilder.forClient();
        try {
            if (cert == null) {
                LOGGER.info("Consumer cert not configured, build insecure sslContext, url=[{}]", url.toString(""));
                builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
            } else {
                try (InputStream trustCertIn = cert.getTrustCertInputStream();
                        InputStream privateKeyIn = cert.getPrivateKeyInputStream();
                        InputStream keyCertChainIn = cert.getKeyCertChainInputStream()) {
                    if (trustCertIn != null) {
                        builder.trustManager(toX509Certificates(trustCertIn));
                    }
                    builder.keyManager(
                            toPrivateKey(privateKeyIn, cert.getPassword()),
                            cert.getPassword(),
                            toX509Certificates(keyCertChainIn));
                }
            }
        } catch (Throwable t) {
            throw new IllegalArgumentException("Could not find certificate file or the certificate is invalid.", t);
        }
        try {
            return builder.applicationProtocols(Http3.supportedApplicationProtocols())
                    .build();
        } catch (Throwable t) {
            throw new IllegalStateException("Build SslSession failed.", t);
        }
    }

    private static CertManager getCertManager(URL url) {
        return url.getOrDefaultFrameworkModel().getBeanFactory().getBean(CertManager.class);
    }

    @Override
    public boolean isClient() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> cipherSuites() {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ApplicationProtocolNegotiator applicationProtocolNegotiator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SSLEngine newEngine(ByteBufAllocator alloc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SSLEngine newEngine(ByteBufAllocator alloc, String peerHost, int peerPort) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SSLSessionContext sessionContext() {
        throw new UnsupportedOperationException();
    }
}
