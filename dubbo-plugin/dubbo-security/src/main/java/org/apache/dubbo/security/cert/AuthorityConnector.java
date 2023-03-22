package org.apache.dubbo.security.cert;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.OpenSsl;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.Security;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
    protected volatile CertPair rootCertPair;

    /**
     * Refresh cert pair for current Dubbo instance
     */
    protected volatile ScheduledFuture<?> refreshFuture;

    public AuthorityConnector(FrameworkModel frameworkModel, CertConfig certConfig) {
        this.frameworkModel = frameworkModel;
        this.certConfig = certConfig;

        connect();
        ScopeBeanFactory beanFactory = frameworkModel.getBeanFactory();
        beanFactory.registerBean(new AuthorityCertFactory(frameworkModel, certConfig, this));
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

    private void connect0(CertConfig certConfig) {
        String caCertPath = certConfig.getCaCertPath();
        String remoteAddress = certConfig.getRemoteAddress();
        logger.info("Try to connect to Dubbo Cert Authority server: " + remoteAddress + ", caCertPath: " + remoteAddress);
        try {
            if (StringUtils.isNotEmpty(caCertPath)) {
                rootChannel = NettyChannelBuilder.forTarget(remoteAddress)
                    .sslContext(
                        GrpcSslContexts.forClient()
                            .trustManager(new File(caCertPath))
                            .build())
                    .build();
            } else {
                logger.warn(CONFIG_SSL_CONNECT_INSECURE, "", "",
                    "No caCertPath is provided, will use insecure connection.");
                rootChannel = NettyChannelBuilder.forTarget(remoteAddress)
                    .sslContext(GrpcSslContexts.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
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
        refreshFuture = repository.getSharedScheduledExecutor().scheduleAtFixedRate(this::generateCert,
            certConfig.getRefreshInterval(), certConfig.getRefreshInterval(), TimeUnit.MILLISECONDS);
    }

    public synchronized void disConnect() {
        frameworkModel.getBeanFactory().getBean(AuthorityCertFactory.class).disConnect();
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
        return certConfig != null && rootChannel != null && rootCertPair != null;
    }

    protected CertPair generateCert() {
        if (rootCertPair != null && !rootCertPair.isExpire()) {
            return rootCertPair;
        }
        synchronized (this) {
            if (rootCertPair == null || rootCertPair.isExpire()) {
                try {
                    logger.info("Try to generate cert from Dubbo Certificate Authority.");
                    CertPair certFromRemote = CertServiceUtil.refreshCert(rootChannel, certConfig, "CLIENT");
                    if (certFromRemote != null) {
                        rootCertPair = certFromRemote;
                    } else {
                        logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Generate Cert from Dubbo Certificate Authority failed.");
                    }
                } catch (Exception e) {
                    logger.error(REGISTRY_FAILED_GENERATE_CERT_ISTIO, "", "", "Generate Cert from Istio failed.", e);
                }
            }
        }
        return rootCertPair;
    }

    public Channel generateChannel() throws SSLException {
        return NettyChannelBuilder.forTarget(certConfig.getRemoteAddress())
            .sslContext(
                GrpcSslContexts.forClient()
                    .trustManager(new ByteArrayInputStream(rootCertPair.getTrustCerts().getBytes(StandardCharsets.UTF_8)))
                    .keyManager(new ByteArrayInputStream(rootCertPair.getCertificate().getBytes(StandardCharsets.UTF_8)),
                        new ByteArrayInputStream(rootCertPair.getPrivateKey().getBytes(StandardCharsets.UTF_8)))
                    .sslProvider(findSslProvider())
                    .build())
            .build();
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
