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

import org.apache.dubbo.auth.v1alpha1.DubboCertificateRequest;
import org.apache.dubbo.auth.v1alpha1.DubboCertificateResponse;
import org.apache.dubbo.auth.v1alpha1.DubboCertificateServiceGrpc;
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
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_SSL_CERT_GENERATE_FAILED;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_SSL_CONNECT_INSECURE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_GENERATE_CERT_ISTIO;

public class DubboCertManager {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DubboCertManager.class);

    private final FrameworkModel frameworkModel;
    /**
     * gRPC channel to Dubbo Cert Authority server
     */
    protected volatile Channel channel;
    /**
     * Cert pair for current Dubbo instance
     */
    protected volatile CertPair certPair;
    /**
     * Path to OpenID Connect Token file
     */
    protected volatile CertConfig certConfig;
    /**
     * Refresh cert pair for current Dubbo instance
     */
    protected volatile ScheduledFuture<?> refreshFuture;

    public DubboCertManager(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    public synchronized void connect(CertConfig certConfig) {
        if (channel != null) {
            logger.error(INTERNAL_ERROR, "", "", "Dubbo Cert Authority server is already connected.");
            return;
        }
        if (certConfig == null) {
            // No cert config, return
            return;
        }
        if (StringUtils.isEmpty(certConfig.getRemoteAddress())) {
            // No remote address configured, return
            return;
        }
        if (StringUtils.isNotEmpty(certConfig.getEnvType()) && !"Kubernetes".equalsIgnoreCase(certConfig.getEnvType())) {
            throw new IllegalArgumentException("Only support Kubernetes env now.");
        }
        // Create gRPC connection
        connect0(certConfig);

        this.certConfig = certConfig;

        // Try to generate cert from remote
        generateCert();
        // Schedule refresh task
        scheduleRefresh();
    }

    /**
     * Create task to refresh cert pair for current Dubbo instance
     */
    protected void scheduleRefresh() {
        FrameworkExecutorRepository repository = frameworkModel.getBeanFactory().getBean(FrameworkExecutorRepository.class);
        refreshFuture = repository.getSharedScheduledExecutor().scheduleAtFixedRate(this::generateCert,
            certConfig.getRefreshInterval(), certConfig.getRefreshInterval(), TimeUnit.MILLISECONDS);
    }

    /**
     * Try to connect to remote certificate authorization
     *
     * @param certConfig certificate authorization address
     */
    protected void connect0(CertConfig certConfig) {
        String caCertPath = certConfig.getCaCertPath();
        String remoteAddress = certConfig.getRemoteAddress();
        logger.info("Try to connect to Dubbo Cert Authority server: " + remoteAddress + ", caCertPath: " + remoteAddress);
        try {
            if (StringUtils.isNotEmpty(caCertPath)) {
                channel = NettyChannelBuilder.forTarget(remoteAddress)
                    .sslContext(
                        GrpcSslContexts.forClient()
                            .trustManager(new File(caCertPath))
                            .build())
                    .build();
            } else {
                logger.warn(CONFIG_SSL_CONNECT_INSECURE, "", "",
                    "No caCertPath is provided, will use insecure connection.");
                channel = NettyChannelBuilder.forTarget(remoteAddress)
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

    public synchronized void disConnect() {
        if (refreshFuture != null) {
            refreshFuture.cancel(true);
            refreshFuture = null;
        }
        if (channel != null) {
            channel = null;
        }
    }

    public boolean isConnected() {
        return certConfig != null && channel != null && certPair != null;
    }

    protected CertPair generateCert() {
        if (certPair != null && !certPair.isExpire()) {
            return certPair;
        }
        synchronized (this) {
            if (certPair == null || certPair.isExpire()) {
                try {
                    logger.info("Try to generate cert from Dubbo Certificate Authority.");
                    CertPair certFromRemote = refreshCert();
                    if (certFromRemote != null) {
                        certPair = certFromRemote;
                    } else {
                        logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Generate Cert from Dubbo Certificate Authority failed.");
                    }
                } catch (Exception e) {
                    logger.error(REGISTRY_FAILED_GENERATE_CERT_ISTIO, "", "", "Generate Cert from Istio failed.", e);
                }
            }
        }
        return certPair;
    }

    /**
     * Request remote certificate authorization to generate cert pair for current Dubbo instance
     *
     * @return cert pair
     * @throws IOException ioException
     */
    protected CertPair refreshCert() throws IOException {
        KeyPair keyPair = signWithEcdsa();

        if (keyPair == null) {
            keyPair = signWithRsa();
        }

        if (keyPair == null) {
            logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Generate Key failed. Please check if your system support.");
            return null;
        }

        String csr = generateCsr(keyPair);
        DubboCertificateServiceGrpc.DubboCertificateServiceBlockingStub stub = DubboCertificateServiceGrpc.newBlockingStub(channel);
        stub = setHeaderIfNeed(stub);

        String privateKeyPem = generatePrivatePemKey(keyPair);
        DubboCertificateResponse certificateResponse = stub.createCertificate(generateRequest(csr));

        if (certificateResponse == null || !certificateResponse.getSuccess()) {
            logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Failed to generate cert from Dubbo Certificate Authority. " +
                "Message: " + (certificateResponse == null ? "null" : certificateResponse.getMessage()));
            return null;
        }
        logger.info("Successfully generate cert from Dubbo Certificate Authority. Cert expire time: " + certificateResponse.getExpireTime());

        return new CertPair(privateKeyPem,
            certificateResponse.getCertPem(),
            String.join("\n", certificateResponse.getTrustCertsList()),
            certificateResponse.getExpireTime());
    }

    private DubboCertificateServiceGrpc.DubboCertificateServiceBlockingStub setHeaderIfNeed(DubboCertificateServiceGrpc.DubboCertificateServiceBlockingStub stub) throws IOException {
        String oidcTokenPath = certConfig.getOidcTokenPath();
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
        return stub;
    }

    /**
     * Generate key pair with RSA
     *
     * @return key pair
     */
    protected static KeyPair signWithRsa() {
        KeyPair keyPair = null;
        try {
            KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance("RSA");
            kpGenerator.initialize(4096);
            java.security.KeyPair keypair = kpGenerator.generateKeyPair();
            PublicKey publicKey = keypair.getPublic();
            PrivateKey privateKey = keypair.getPrivate();
            ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keypair.getPrivate());
            keyPair = new KeyPair(publicKey, privateKey, signer);
        } catch (NoSuchAlgorithmException | OperatorCreationException e) {
            logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Generate Key with SHA256WithRSA algorithm failed. Please check if your system support.", e);
        }
        return keyPair;
    }

    /**
     * Generate key pair with ECDSA
     *
     * @return key pair
     */
    protected static KeyPair signWithEcdsa() {
        KeyPair keyPair = null;
        try {
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(ecSpec, new SecureRandom());
            java.security.KeyPair keypair = g.generateKeyPair();
            PublicKey publicKey = keypair.getPublic();
            PrivateKey privateKey = keypair.getPrivate();
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(privateKey);
            keyPair = new KeyPair(publicKey, privateKey, signer);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | OperatorCreationException e) {
            logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Generate Key with secp256r1 algorithm failed. Please check if your system support. "
                + "Will attempt to generate with RSA2048.", e);
        }
        return keyPair;
    }

    private DubboCertificateRequest generateRequest(String csr) {
        return DubboCertificateRequest.newBuilder().setCsr(csr).setType("CONNECTION").build();
    }

    /**
     * Generate private key in pem encoded
     *
     * @param keyPair key pair
     * @return private key
     * @throws IOException ioException
     */
    private String generatePrivatePemKey(KeyPair keyPair) throws IOException {
        String key = generatePemKey("RSA PRIVATE KEY", keyPair.getPrivateKey().getEncoded());
        if (logger.isDebugEnabled()) {
            logger.debug("Generated Private Key. \n" + key);
        }
        return key;
    }

    /**
     * Generate content in pem encoded
     *
     * @param type    content type
     * @param content content
     * @return encoded data
     * @throws IOException ioException
     */
    private String generatePemKey(String type, byte[] content) throws IOException {
        PemObject pemObject = new PemObject(type, content);
        StringWriter str = new StringWriter();
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(str);
        jcaPEMWriter.writeObject(pemObject);
        jcaPEMWriter.close();
        str.close();
        return str.toString();
    }

    /**
     * Generate CSR (Certificate Sign Request)
     *
     * @param keyPair key pair to request
     * @return csr
     * @throws IOException ioException
     */
    private String generateCsr(KeyPair keyPair) throws IOException {
        PKCS10CertificationRequest request = new JcaPKCS10CertificationRequestBuilder(
            new X500Name("O=" + "cluster.domain"), keyPair.getPublicKey())
            .build(keyPair.getSigner());

        String csr = generatePemKey("CERTIFICATE REQUEST", request.getEncoded());

        if (logger.isDebugEnabled()) {
            logger.debug("CSR Request to Dubbo Certificate Authorization. \n" + csr);
        }
        return csr;
    }

    protected static class KeyPair {
        private final PublicKey publicKey;
        private final PrivateKey privateKey;
        private final ContentSigner signer;

        public KeyPair(PublicKey publicKey, PrivateKey privateKey, ContentSigner signer) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.signer = signer;
        }

        public PublicKey getPublicKey() {
            return publicKey;
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }

        public ContentSigner getSigner() {
            return signer;
        }
    }
}
