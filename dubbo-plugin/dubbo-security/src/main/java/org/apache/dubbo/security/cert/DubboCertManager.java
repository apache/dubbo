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
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.FrameworkModel;

import com.google.protobuf.ProtocolStringList;
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

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
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
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_GENERATE_CERT_ISTIO;

public class DubboCertManager {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DubboCertManager.class);

    private final FrameworkModel frameworkModel;
    private volatile Channel channel;
    private volatile CertPair certPair;

    private volatile String oidcTokenPath;
    private volatile ScheduledFuture<?> refreshFuture;

    public DubboCertManager(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    public synchronized void connect(String remoteAddress, String envType, String caCertPath, String oidcTokenPath) {
        if (channel != null) {
            return;
        }
        synchronized (this) {
            if (channel != null) {
                return;
            }
            if (StringUtils.isNotEmpty(envType) && !"Kubernetes".equals(envType)) {
                throw new IllegalArgumentException("Only support Kubernetes env now.");
            }
            logger.info("Try to connect to Dubbo Cert Authority server: " + remoteAddress + ", caCertPath: " + caCertPath);
            try {
                if (StringUtils.isNotEmpty(caCertPath)) {
                    channel = NettyChannelBuilder.forTarget(remoteAddress)
                        .sslContext(
                            GrpcSslContexts.forClient()
                                .trustManager(new File(caCertPath))
                                .build())
                        .build();
                } else {
                    channel = NettyChannelBuilder.forTarget(remoteAddress)
                        .sslContext(GrpcSslContexts.forClient()
                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                            .build())
                        .build();
                }
            } catch (SSLException e) {
                logger.error(LoggerCodeConstants.CONFIG_SSL_PATH_LOAD_FAILED, "", "", "Failed to load SSL cert file.", e);
                throw new RuntimeException(e);
            }
            this.oidcTokenPath = oidcTokenPath;

            generateCert();
            FrameworkExecutorRepository repository = frameworkModel.getBeanFactory().getBean(FrameworkExecutorRepository.class);
            refreshFuture = repository.getSharedScheduledExecutor().scheduleAtFixedRate(this::generateCert, 30, 30, TimeUnit.SECONDS);
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
        return channel != null && certPair != null;
    }

    public CertPair generateCert() {
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

    public CertPair refreshCert() throws IOException {
        PublicKey publicKey = null;
        PrivateKey privateKey = null;
        ContentSigner signer = null;

        try {
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(ecSpec, new SecureRandom());
            KeyPair keypair = g.generateKeyPair();
            publicKey = keypair.getPublic();
            privateKey = keypair.getPrivate();
            signer = new JcaContentSignerBuilder("SHA256withECDSA").build(keypair.getPrivate());
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | OperatorCreationException e) {
            logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Generate Key with secp256r1 algorithm failed. Please check if your system support. "
                + "Will attempt to generate with RSA2048.", e);
        }

        if (publicKey == null) {
            try {
                KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance("RSA");
                kpGenerator.initialize(4096);
                KeyPair keypair = kpGenerator.generateKeyPair();
                publicKey = keypair.getPublic();
                privateKey = keypair.getPrivate();
                signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keypair.getPrivate());
            } catch (NoSuchAlgorithmException | OperatorCreationException e) {
                logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Generate Key with SHA256WithRSA algorithm failed. Please check if your system support.", e);
                throw new RpcException(e);
            }
        }

        if (publicKey == null || privateKey == null) {
            logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Generate Key failed. Please check if your system support.");
            return null;
        }

        String csr = generateCsr(publicKey, signer);
        DubboCertificateServiceGrpc.DubboCertificateServiceBlockingStub stub = DubboCertificateServiceGrpc.newBlockingStub(channel);

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
        }

        String privateKeyPem = generatePrivatePemKey(privateKey);
        DubboCertificateResponse certificateResponse = stub.createCertificate(generateRequest(csr));

        if (!certificateResponse.getSuccess()) {
            logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Failed to generate cert from Dubbo Certificate Authority. Message: " + certificateResponse.getMessage());
            return null;
        }
        logger.info("Successfully generate cert from Dubbo Certificate Authority. Cert expire time: " + certificateResponse.getExpireTime());

        ProtocolStringList trustCertsList = certificateResponse.getTrustCertsList();
        return new CertPair(privateKeyPem, certificateResponse.getCertPem(), String.join("\n", trustCertsList), certificateResponse.getExpireTime());
    }

    private DubboCertificateRequest generateRequest(String csr) {
        return DubboCertificateRequest.newBuilder().setCsr(csr).setType("CONNECTION").build();
    }

    private String generatePrivatePemKey(PrivateKey privateKey) throws IOException {
        String key = generatePemKey("RSA PRIVATE KEY", privateKey.getEncoded());
        if (logger.isDebugEnabled()) {
            logger.debug("Generated Private Key. \n" + key);
        }
        return key;
    }

    private String generatePemKey(String type, byte[] content) throws IOException {
        PemObject pemObject = new PemObject(type, content);
        StringWriter str = new StringWriter();
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(str);
        jcaPEMWriter.writeObject(pemObject);
        jcaPEMWriter.close();
        str.close();
        return str.toString();
    }

    private String generateCsr(PublicKey publicKey, ContentSigner signer) throws IOException {
        PKCS10CertificationRequest request = new JcaPKCS10CertificationRequestBuilder(
            new X500Name("O=" + "cluster.domain"), publicKey).build(signer);

        String csr = generatePemKey("CERTIFICATE REQUEST", request.getEncoded());

        if (logger.isDebugEnabled()) {
            logger.debug("CSR Request to Istio Citadel. \n" + csr);
        }
        return csr;
    }
}
