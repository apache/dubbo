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

package org.apache.dubbo.security.cert;

import org.apache.dubbo.auth.v1alpha1.AuthorityServiceGrpc;
import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.IOUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.OpenSsl;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.grpc.stub.AbstractStub;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.Security;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_SSL_CERT_GENERATE_FAILED;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_SSL_CONNECT_INSECURE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_GENERATE_CERT_ISTIO;

public class AuthorityConnector {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AuthorityConnector.class);
    private final FrameworkModel frameworkModel;

    /**
     * Path to OpenID Connect Token file
     */
    protected final CertConfig certConfig;

    /**
     * gRPC channel to Dubbo Cert Authority server
     */
    protected volatile Channel rootChannel;

    /**
     * Cert pair for current Dubbo instance
     */
    protected volatile IdentityInfo rootIdentityInfo;

    /**
     * Refresh cert pair for current Dubbo instance
     */
    protected volatile ScheduledFuture<?> refreshFuture;

    public AuthorityConnector(FrameworkModel frameworkModel, CertConfig certConfig) {
        this.frameworkModel = frameworkModel;
        this.certConfig = certConfig;

        connect();
        registerBean(frameworkModel, certConfig);
    }

    private void registerBean(FrameworkModel frameworkModel, CertConfig certConfig) {
        ScopeBeanFactory beanFactory = frameworkModel.getBeanFactory();

        if (beanFactory.getBean(AuthenticationGovernor.class) != null ||
            beanFactory.getBean(AuthorityRuleSync.class) != null ||
            beanFactory.getBean(AuthorityIdentityFactory.class) != null) {
            throw new IllegalStateException("AuthorityConnector has been registered.");
        }

        beanFactory.registerBean(new AuthorityIdentityFactory(frameworkModel, certConfig, this));
        beanFactory.registerBean(new AuthorityRuleSync(frameworkModel, certConfig, this));
        beanFactory.registerBean(AuthenticationGovernor.class);
    }

    private void connect() {
        if (certConfig == null) {
            throw new IllegalArgumentException("No cert config found.");
        }
        if (StringUtils.isEmpty(certConfig.getRemoteAddress())) {
            throw new IllegalArgumentException("No remote address found.");
        }
        if (StringUtils.isNotEmpty(certConfig.getEnvType()) && !"Kubernetes".equalsIgnoreCase(certConfig.getEnvType())) {
            throw new IllegalArgumentException("Only support Kubernetes env now.");
        }
        // Create gRPC connection
        connect0(certConfig);

        // Try to generate cert from remote
        generateCert();
        // Schedule refresh task
        scheduleRefresh();
    }

    protected void connect0(CertConfig certConfig) {
        String caCertPath = certConfig.getCaCertPath();
        String remoteAddress = certConfig.getRemoteAddress();
        logger.info("Try to connect to Dubbo Cert Authority server: " + remoteAddress + ", caCertPath: " + remoteAddress);
        try {
            if (StringUtils.isNotEmpty(caCertPath)) {
                rootChannel = NettyChannelBuilder.forTarget(remoteAddress)
                    .sslContext(
                        GrpcSslContexts.forClient()
                            .trustManager(new File(caCertPath))
                            .sslProvider(findSslProvider())
                            .build())
                    .build();
            } else {
                logger.warn(CONFIG_SSL_CONNECT_INSECURE, "", "",
                    "No caCertPath is provided, will use insecure connection.");
                rootChannel = NettyChannelBuilder.forTarget(remoteAddress)
                    .sslContext(GrpcSslContexts.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .sslProvider(findSslProvider())
                        .build())
                    .build();
            }
        } catch (Exception e) {
            logger.error(LoggerCodeConstants.CONFIG_SSL_PATH_LOAD_FAILED, "", "", "Failed to load SSL cert file.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Create task to refresh cert pair for current Dubbo instance
     */
    protected void scheduleRefresh() {
        FrameworkExecutorRepository repository = frameworkModel.getBeanFactory().getBean(FrameworkExecutorRepository.class);
        refreshFuture = repository.getSharedScheduledExecutor().scheduleAtFixedRate(this::refreshCert,
            certConfig.getRefreshInterval(), certConfig.getRefreshInterval(), TimeUnit.MILLISECONDS);
    }

    public synchronized void disConnect() {
        frameworkModel.getBeanFactory().getBean(AuthorityIdentityFactory.class).disConnect();
        frameworkModel.getBeanFactory().getBean(AuthorityRuleSync.class).disConnect();
        if (refreshFuture != null) {
            refreshFuture.cancel(true);
            refreshFuture = null;
        }
        if (rootChannel != null) {
            rootChannel = null;
        }
    }

    public boolean isConnected() {
        return rootChannel != null && rootIdentityInfo != null;
    }

    protected IdentityInfo generateCert() {
        if (rootIdentityInfo != null && !rootIdentityInfo.isExpire()) {
            return rootIdentityInfo;
        }
        synchronized (this) {
            if (rootIdentityInfo == null || rootIdentityInfo.isExpire()) {
                logger.info("Try to generate cert from Dubbo Certificate Authority.");
                IdentityInfo certFromRemote = null;
                try {
                    certFromRemote = generateCert0();
                } catch (Exception e) {
                    logger.error(REGISTRY_FAILED_GENERATE_CERT_ISTIO, "", "", "Generate Cert from Istio failed.", e);
                }
                if (certFromRemote != null && !certFromRemote.isExpire()) {
                    rootIdentityInfo = certFromRemote;
                } else {
                    if (rootIdentityInfo != null && rootIdentityInfo.isExpire()) {
                        rootIdentityInfo = null;
                    }
                    logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Generate Cert from Dubbo Certificate Authority failed.");
                }
            }
        }
        return rootIdentityInfo;
    }

    protected IdentityInfo refreshCert() {
        if (rootIdentityInfo != null && !rootIdentityInfo.needRefresh() && !rootIdentityInfo.isExpire()) {
            return rootIdentityInfo;
        }
        synchronized (this) {
            if (rootIdentityInfo == null || rootIdentityInfo.needRefresh() || rootIdentityInfo.isExpire()) {
                logger.info("Try to generate cert from Dubbo Certificate Authority.");
                IdentityInfo certFromRemote = null;
                try {
                    certFromRemote = generateCert0();
                } catch (Exception e) {
                    logger.error(REGISTRY_FAILED_GENERATE_CERT_ISTIO, "", "", "Generate Cert from Istio failed.", e);
                }
                if (certFromRemote != null && !certFromRemote.isExpire()) {
                    rootIdentityInfo = certFromRemote;
                } else {
                    if (rootIdentityInfo != null && rootIdentityInfo.isExpire()) {
                        rootIdentityInfo = null;
                    }
                    logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Generate Cert from Dubbo Certificate Authority failed.");
                }
            }
        }
        return rootIdentityInfo;
    }

    private IdentityInfo generateCert0() throws IOException {
        AuthorityServiceGrpc.AuthorityServiceBlockingStub stub = AuthorityServiceGrpc.newBlockingStub(rootChannel);
        stub = setHeaderIfNeed(stub, certConfig);
        return CertServiceUtil.refreshCert(stub, "CLIENT");
    }

    public Channel generateChannel() throws SSLException {
        if (!isConnected()) {
            return null;
        }

        IdentityInfo identityInfo = generateCert();
        if (identityInfo == null) {
            return null;
        }

        return NettyChannelBuilder.forTarget(certConfig.getRemoteAddress())
            .sslContext(
                GrpcSslContexts.forClient()
                    .trustManager(new ByteArrayInputStream(identityInfo.getTrustCerts().getBytes(StandardCharsets.UTF_8)))
                    .keyManager(new ByteArrayInputStream(identityInfo.getCertificate().getBytes(StandardCharsets.UTF_8)),
                        new ByteArrayInputStream(identityInfo.getPrivateKey().getBytes(StandardCharsets.UTF_8)))
                    .sslProvider(findSslProvider())
                    .build())
            .build();
    }

    private static AuthorityServiceGrpc.AuthorityServiceBlockingStub setHeaderIfNeed(
        AuthorityServiceGrpc.AuthorityServiceBlockingStub stub, CertConfig certConfig) throws IOException {
        if (certConfig == null) {
            return stub;
        }

        String oidcTokenPath = certConfig.getOidcTokenPath();
        String oidcTokenType = certConfig.getOidcTokenType();
        if (StringUtils.isNotEmpty(oidcTokenPath)) {
            Metadata header = new Metadata();
            Metadata.Key<String> key = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
            header.put(key, "Bearer " +
                IOUtils.read(new FileReader(oidcTokenPath))
                    .replace("\n", "")
                    .replace("\t", "")
                    .replace("\r", "")
                    .trim());

            stub = stub.withInterceptors(newAttachHeadersInterceptor(header));
            logger.info("Use oidc token from " + oidcTokenPath + " to connect to Dubbo Certificate Authority.");
        } else {
            logger.warn(CONFIG_SSL_CONNECT_INSECURE, "", "",
                "Use insecure connection to connect to Dubbo Certificate Authority. Reason: No oidc token is provided.");
        }

        if (StringUtils.isNotEmpty(oidcTokenType)) {
            Metadata header = new Metadata();
            Metadata.Key<String> key = Metadata.Key.of("authorization-type", Metadata.ASCII_STRING_MARSHALLER);
            header.put(key, oidcTokenType);
            stub = stub.withInterceptors(newAttachHeadersInterceptor(header));
        }

        return stub;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractStub> T setHeaders(T t) {
        if (rootIdentityInfo == null) {
            return t;
        }

        Metadata header = new Metadata();
        Metadata.Key<String> key = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        header.put(key, "Bearer " + rootIdentityInfo.getToken());
        key = Metadata.Key.of("authorization-type", Metadata.ASCII_STRING_MARSHALLER);
        header.put(key, "dubbo-jwt");

        return (T) t.withInterceptors(newAttachHeadersInterceptor(header));
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
}
