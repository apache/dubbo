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

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CLOSE_STREAM;

public class QuicSslContexts {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(QuicSslContexts.class);

    public static QuicSslContext buildServerSslContext(URL url) {

        CertManager certManager =
                url.getOrDefaultApplicationModel().getBeanFactory().getBean(CertManager.class);
        ProviderCert providerConnectionConfig = certManager.getProviderConnectionConfig(url, url.toInetSocketAddress());

        QuicSslContextBuilder builder;
        InputStream serverKeyCertChainPathStream = null;
        InputStream serverPrivateKeyPathStream = null;
        InputStream serverTrustCertStream = null;
        Netty4SslContext netty4SslContext = new Netty4SslContext();
        try {
            serverKeyCertChainPathStream = providerConnectionConfig.getKeyCertChainInputStream();
            serverPrivateKeyPathStream = providerConnectionConfig.getPrivateKeyInputStream();
            serverTrustCertStream = providerConnectionConfig.getTrustCertInputStream();
            if (serverKeyCertChainPathStream == null || serverPrivateKeyPathStream == null) {
                SelfSignedCertificate certificate = new SelfSignedCertificate();
                return QuicSslContextBuilder.forServer(certificate.privateKey(), null, certificate.certificate())
                        .applicationProtocols(Http3.supportedApplicationProtocols())
                        .build();
            }
            String password = providerConnectionConfig.getPassword();
            PrivateKey privateKey = netty4SslContext.toPrivateKey0(serverPrivateKeyPathStream, password);
            X509Certificate[] x509Certificates0 = netty4SslContext.toX509Certificates0(serverKeyCertChainPathStream);
            builder = QuicSslContextBuilder.forServer(privateKey, password, x509Certificates0);
            if (serverTrustCertStream != null) {
                builder.trustManager(netty4SslContext.toX509Certificates0(serverTrustCertStream));
                if (providerConnectionConfig.getAuthPolicy() == AuthPolicy.CLIENT_AUTH) {
                    builder.clientAuth(ClientAuth.REQUIRE);
                } else {
                    builder.clientAuth(ClientAuth.OPTIONAL);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not find certificate file or the certificate is invalid.", e);
        } finally {
            safeCloseStream(serverTrustCertStream);
            safeCloseStream(serverKeyCertChainPathStream);
            safeCloseStream(serverPrivateKeyPathStream);
        }

        try {
            return builder.applicationProtocols(Http3.supportedApplicationProtocols())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Build QuicSslContext failed. ", e);
        }
    }

    public static QuicSslContext buildClientSslContext(URL url) throws Exception {
        CertManager certManager =
                url.getOrDefaultFrameworkModel().getBeanFactory().getBean(CertManager.class);
        Cert consumerConnectionConfig = certManager.getConsumerConnectionConfig(url);
        if (consumerConnectionConfig == null) {
            return QuicSslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .applicationProtocols(Http3.supportedApplicationProtocols())
                    .build();
        }
        Netty4SslContext netty4SslContext = new Netty4SslContext();
        QuicSslContextBuilder builder = QuicSslContextBuilder.forClient();
        InputStream clientTrustCertCollectionPath = null;
        InputStream clientCertChainFilePath = null;
        InputStream clientPrivateKeyFilePath = null;
        String password = consumerConnectionConfig.getPassword();
        try {
            clientTrustCertCollectionPath = consumerConnectionConfig.getTrustCertInputStream();
            if (clientTrustCertCollectionPath != null) {
                builder.trustManager(netty4SslContext.toX509Certificates0(clientTrustCertCollectionPath));
            }
            clientCertChainFilePath = consumerConnectionConfig.getKeyCertChainInputStream();
            clientPrivateKeyFilePath = consumerConnectionConfig.getPrivateKeyInputStream();
            PrivateKey privateKey = netty4SslContext.toPrivateKey0(clientPrivateKeyFilePath, password);
            X509Certificate[] x509Certificates = netty4SslContext.toX509Certificates0(clientCertChainFilePath);
            builder.keyManager(privateKey, password, x509Certificates);

            return builder.applicationProtocols(Http3.supportedApplicationProtocols())
                    .build();

        } catch (Exception e) {
            throw new IllegalArgumentException("Could not find certificate file or the certificate is invalid.", e);
        } finally {
            safeCloseStream(clientTrustCertCollectionPath);
            safeCloseStream(clientCertChainFilePath);
            safeCloseStream(clientPrivateKeyFilePath);
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
}
