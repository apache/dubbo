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
import org.apache.dubbo.common.ssl.Cert;
import org.apache.dubbo.common.ssl.CertManager;
import org.apache.dubbo.common.ssl.ProviderCert;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.util.CharsetUtil;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CLOSE_STREAM;

public class QuicSslContexts {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(QuicSslContexts.class);

    public static QuicSslContext buildServerSslContext(URL url) throws Exception {
        CertManager certManager =
                url.getOrDefaultApplicationModel().getBeanFactory().getBean(CertManager.class);
        ProviderCert providerConnectionConfig = certManager.getProviderConnectionConfig(url, url.toInetSocketAddress());
        if (providerConnectionConfig == null) {
            SelfSignedCertificate certificate = new SelfSignedCertificate();
            logger.warn("Provider certificate is incorrect. using self-signed certificate.");
            return QuicSslContextBuilder.forServer(certificate.privateKey(), null, certificate.certificate())
                    .applicationProtocols(Http3.supportedApplicationProtocols())
                    .build();
        }
        QuicSslContextBuilder quicSslContextBuilder;
        InputStream serverKeyCertChainPathStream = null;
        InputStream serverPrivateKeyPathStream = null;
        InputStream serverTrustCertStream = null;
        try {
            serverKeyCertChainPathStream = providerConnectionConfig.getKeyCertChainInputStream();
            serverPrivateKeyPathStream = providerConnectionConfig.getPrivateKeyInputStream();
            serverTrustCertStream = providerConnectionConfig.getTrustCertInputStream();
            String password = providerConnectionConfig.getPassword();

            KeyManagerFactory keyManagerFactory =
                    buildKeyManagerFactory(serverKeyCertChainPathStream, serverPrivateKeyPathStream, password);
            TrustManagerFactory trustManagerFactory =
                    serverTrustCertStream != null ? buildTrustManagerFactory(serverTrustCertStream) : null;
            quicSslContextBuilder = QuicSslContextBuilder.forServer(keyManagerFactory, password);
            if (trustManagerFactory != null) {
                // TODO: forward test for ClientAuth.REQUIRE
                quicSslContextBuilder.trustManager(trustManagerFactory).clientAuth(ClientAuth.OPTIONAL);
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Could not find certificate file or the certificate is invalid.", e);
        } finally {
            safeCloseStream(serverKeyCertChainPathStream);
            safeCloseStream(serverPrivateKeyPathStream);
            safeCloseStream(serverTrustCertStream);
        }

        try {
            return quicSslContextBuilder
                    .applicationProtocols(Http3.supportedApplicationProtocols())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Build QuicSslContext failed. I will create an SelfTrustManager", e);
        }
    }

    public static QuicSslContext buildClientSslContext(URL url) {
        CertManager certManager =
                url.getOrDefaultFrameworkModel().getBeanFactory().getBean(CertManager.class);
        Cert consumerConnectionConfig = certManager.getConsumerConnectionConfig(url);

        if (consumerConnectionConfig == null) {
            logger.warn("Provider certificate is incorrect. using insecure certificate.");
            return QuicSslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .applicationProtocols(Http3.supportedApplicationProtocols())
                    .build();
        }
        QuicSslContextBuilder builder = QuicSslContextBuilder.forClient();
        InputStream clientTrustCertCollectionPath = null;
        try {
            clientTrustCertCollectionPath = consumerConnectionConfig.getTrustCertInputStream();
            if (clientTrustCertCollectionPath != null) {
                TrustManagerFactory trustManagerFactory = buildTrustManagerFactory(clientTrustCertCollectionPath);
                builder.trustManager(trustManagerFactory);
            } else {
                return QuicSslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .applicationProtocols(Http3.supportedApplicationProtocols())
                        .build();
            }

            return builder.applicationProtocols(Http3.supportedApplicationProtocols())
                    .build();

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not find certificate file or the quic certificate is invalid. I will create an InsecureTrustManager",
                    e);
        } finally {
            safeCloseStream(clientTrustCertCollectionPath);
        }
    }

    private static void safeCloseStream(InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException e) {
            logger.warn(TRANSPORT_FAILED_CLOSE_STREAM, "", "", "Failed to close a stream.", e);
        }
    }

    public static KeyManagerFactory buildKeyManagerFactory(
            InputStream keyCertChainStream, InputStream privateKeyStream, String password) throws Exception {
        X509Certificate[] certChain = getCertificates(keyCertChainStream);
        PrivateKey key = getPrivateKey(privateKeyStream, "RSA");

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setKeyEntry("key", key, password != null ? password.toCharArray() : null, certChain);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password != null ? password.toCharArray() : null);
        return keyManagerFactory;
    }

    public static TrustManagerFactory buildTrustManagerFactory(InputStream trustCertStream) throws Exception {
        X509Certificate[] certs = getCertificates(trustCertStream);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        int i = 0;
        for (X509Certificate cert : certs) {
            keyStore.setCertificateEntry("cert-" + i++, cert);
        }

        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        return trustManagerFactory;
    }

    public static PrivateKey getPrivateKey(InputStream keyStream, String algorithm) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(keyStream, CharsetUtil.US_ASCII));
        StringBuilder keyPEM = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.contains("-----BEGIN PRIVATE KEY-----") || line.contains("-----END PRIVATE KEY-----")) {
                continue;
            }
            keyPEM.append(line.trim());
        }

        byte[] decodedKey = Base64.getDecoder().decode(keyPEM.toString());
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        return keyFactory.generatePrivate(keySpec);
    }

    public static X509Certificate[] getCertificates(InputStream certStream) throws Exception {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certs = certFactory.generateCertificates(certStream);
        return certs.toArray(new X509Certificate[0]);
    }
}
