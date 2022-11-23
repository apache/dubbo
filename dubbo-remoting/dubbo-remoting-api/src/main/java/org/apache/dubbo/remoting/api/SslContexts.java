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
package org.apache.dubbo.remoting.api;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.context.ConfigManager;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Provider;
import java.security.Security;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CLOSE_STREAM;

public class SslContexts {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(SslContexts.class);

    public static SslContext buildServerSslContext(URL url) {
        ConfigManager globalConfigManager = url.getOrDefaultApplicationModel().getApplicationConfigManager();
        SslConfig sslConfig = globalConfigManager.getSsl().orElseThrow(() -> new IllegalStateException("Ssl enabled, but no ssl cert information provided!"));

        SslContextBuilder sslClientContextBuilder;
        InputStream serverKeyCertChainPathStream = null;
        InputStream serverPrivateKeyPathStream = null;
        InputStream serverTrustCertStream = null;
        try {
            serverKeyCertChainPathStream = sslConfig.getServerKeyCertChainPathStream();
            serverPrivateKeyPathStream = sslConfig.getServerPrivateKeyPathStream();
            serverTrustCertStream = sslConfig.getServerTrustCertCollectionPathStream();
            String password = sslConfig.getServerKeyPassword();
            if (password != null) {
                sslClientContextBuilder = SslContextBuilder.forServer(serverKeyCertChainPathStream,
                    serverPrivateKeyPathStream, password);
            } else {
                sslClientContextBuilder = SslContextBuilder.forServer(serverKeyCertChainPathStream,
                    serverPrivateKeyPathStream);
            }

            if (serverTrustCertStream != null) {
                sslClientContextBuilder.trustManager(serverTrustCertStream);
                sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not find certificate file or the certificate is invalid.", e);
        } finally {
            safeCloseStream(serverTrustCertStream);
            safeCloseStream(serverKeyCertChainPathStream);
            safeCloseStream(serverPrivateKeyPathStream);
        }
        try {
            return sslClientContextBuilder.sslProvider(findSslProvider()).build();
        } catch (SSLException e) {
            throw new IllegalStateException("Build SslSession failed.", e);
        }
    }

    public static SslContext buildClientSslContext(URL url) {
        ConfigManager globalConfigManager = url.getOrDefaultApplicationModel().getApplicationConfigManager();
        SslConfig sslConfig = globalConfigManager.getSsl().orElseThrow(() -> new IllegalStateException("Ssl enabled, but no ssl cert information provided!"));

        SslContextBuilder builder = SslContextBuilder.forClient();
        InputStream clientTrustCertCollectionPath = null;
        InputStream clientCertChainFilePath = null;
        InputStream clientPrivateKeyFilePath = null;
        try {
            clientTrustCertCollectionPath = sslConfig.getClientTrustCertCollectionPathStream();
            if (clientTrustCertCollectionPath != null) {
                builder.trustManager(clientTrustCertCollectionPath);
            }

            clientCertChainFilePath = sslConfig.getClientKeyCertChainPathStream();
            clientPrivateKeyFilePath = sslConfig.getClientPrivateKeyPathStream();
            if (clientCertChainFilePath != null && clientPrivateKeyFilePath != null) {
                String password = sslConfig.getClientKeyPassword();
                if (password != null) {
                    builder.keyManager(clientCertChainFilePath, clientPrivateKeyFilePath, password);
                } else {
                    builder.keyManager(clientCertChainFilePath, clientPrivateKeyFilePath);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not find certificate file or find invalid certificate.", e);
        } finally {
            safeCloseStream(clientTrustCertCollectionPath);
            safeCloseStream(clientCertChainFilePath);
            safeCloseStream(clientPrivateKeyFilePath);
        }
        try {
            return builder.sslProvider(findSslProvider()).build();
        } catch (SSLException e) {
            throw new IllegalStateException("Build SslSession failed.", e);
        }
    }

    /**
     * Returns OpenSSL if available, otherwise returns the JDK provider.
     */
    private static SslProvider findSslProvider() {
        if (OpenSsl.isAvailable()) {
            logger.debug("Using OPENSSL provider.");
            return SslProvider.OPENSSL;
        }
        if (checkJdkProvider()) {
            logger.debug("Using JDK provider.");
            return SslProvider.JDK;
        }
        throw new IllegalStateException(
            "Could not find any valid TLS provider, please check your dependency or deployment environment, " +
                "usually netty-tcnative, Conscrypt, or Jetty NPN/ALPN is needed.");
    }

    private static boolean checkJdkProvider() {
        Provider[] jdkProviders = Security.getProviders("SSLContext.TLS");
        return (jdkProviders != null && jdkProviders.length > 0);
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
