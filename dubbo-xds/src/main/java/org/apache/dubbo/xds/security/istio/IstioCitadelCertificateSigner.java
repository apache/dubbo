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
package org.apache.dubbo.xds.security.istio;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.xds.istio.IstioEnv;
import org.apache.dubbo.xds.security.api.CertPair;
import org.apache.dubbo.xds.security.api.CertSource;
import org.apache.dubbo.xds.security.api.TrustSource;
import org.apache.dubbo.xds.security.api.X509CertChains;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import istio.v1.auth.IstioCertificateRequest;
import istio.v1.auth.IstioCertificateResponse;
import istio.v1.auth.IstioCertificateServiceGrpc;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_GENERATE_CERT_ISTIO;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_GENERATE_KEY_ISTIO;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_RECEIVE_ERROR_MSG_ISTIO;

public class IstioCitadelCertificateSigner implements CertSource, TrustSource {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(IstioCitadelCertificateSigner.class);

    private final IstioEnv istioEnv;

    private volatile CertPair certPair;

    private volatile X509CertChains trustChain;

    public IstioCitadelCertificateSigner() {
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);
        long refreshRate =
                IstioEnv.getInstance().getSecretTTL() - IstioEnv.getInstance().getTryRefreshBeforeCertExpireAt();
        if (refreshRate <= 0) {
            refreshRate = IstioEnv.getInstance().getSecretTTL();
        }
        scheduledThreadPool.scheduleAtFixedRate(new GenerateCertTask(), 0, refreshRate, TimeUnit.SECONDS);
        this.istioEnv = IstioEnv.getInstance();
    }

    @Override
    public CertPair getCert(URL url) {
        if (certPair != null && !certPair.isExpire()) {
            return certPair;
        }
        return doGenerateCert();
    }

    @Override
    public X509CertChains getTrustCerts(URL url) {
        getCert(url);
        return trustChain;
    }

    private class GenerateCertTask implements Runnable {
        @Override
        public void run() {
            doGenerateCert();
        }
    }

    private CertPair doGenerateCert() {
        synchronized (this) {
            if (certPair == null || certPair.isExpire() || canTryUpdate(certPair.getExpireTime())) {
                try {
                    certPair = createCert();
                } catch (IOException e) {
                    logger.error(REGISTRY_FAILED_GENERATE_CERT_ISTIO, "", "", "Generate Cert from Istio failed.", e);
                    throw new RpcException("Generate Cert from Istio failed.", e);
                }
            }
        }
        return certPair;
    }

    public boolean canTryUpdate(Long expireAt) {
        Long refreshBeforeCertExpireAt = IstioEnv.getInstance().getTryRefreshBeforeCertExpireAt();

        long min = 0;
        long max = expireAt;
        long rand = min + (long) (Math.random() * (max - min + 1));
        ;

        return System.currentTimeMillis() - expireAt < (refreshBeforeCertExpireAt - rand);
    }

    public CertPair createCert() throws IOException {
        PublicKey publicKey = null;
        PrivateKey privateKey = null;
        ContentSigner signer = null;

        if (istioEnv.isECCFirst()) {
            try {
                ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
                KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
                g.initialize(ecSpec, new SecureRandom());
                KeyPair keypair = g.generateKeyPair();
                publicKey = keypair.getPublic();
                privateKey = keypair.getPrivate();
                signer = new JcaContentSignerBuilder("SHA256withECDSA").build(keypair.getPrivate());
            } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | OperatorCreationException e) {
                logger.error(
                        REGISTRY_FAILED_GENERATE_KEY_ISTIO,
                        "",
                        "",
                        "Generate Key with secp256r1 algorithm failed. Please check if your system support. "
                                + "Will attempt to generate with RSA2048.",
                        e);
            }
        }

        if (publicKey == null) {
            try {
                KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance("RSA");
                kpGenerator.initialize(istioEnv.getRasKeySize());
                KeyPair keypair = kpGenerator.generateKeyPair();
                publicKey = keypair.getPublic();
                privateKey = keypair.getPrivate();
                signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keypair.getPrivate());
            } catch (NoSuchAlgorithmException | OperatorCreationException e) {
                logger.error(
                        REGISTRY_FAILED_GENERATE_KEY_ISTIO,
                        "",
                        "",
                        "Generate Key with SHA256WithRSA algorithm failed. Please check if your system support.",
                        e);
                throw new RpcException(e);
            }
        }

        String csr = generateCsr(publicKey, signer);

        String caCert = istioEnv.getCaCert();
        ManagedChannel channel;
        if (StringUtils.isNotEmpty(caCert)) {
            channel = NettyChannelBuilder.forTarget(istioEnv.getCaAddr())
                    .sslContext(GrpcSslContexts.forClient()
                            //
                            .trustManager(new ByteArrayInputStream(caCert.getBytes(StandardCharsets.UTF_8)))
                            .build())
                    .build();
        } else {
            channel = NettyChannelBuilder.forTarget(istioEnv.getCaAddr())
                    .sslContext(GrpcSslContexts.forClient()
                            // Currently, we can't verify istio server's CA if istio configured USE_FIRST_PARTY_JWT,
                            // because istio always returns its own CA (third party CA).
                            // If we use k8s CA here, client side verification will fail.
                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                            .build())
                    .build();
        }

        // Istio always use SA token(JWT) to verify xDS client.
        IstioCertificateServiceGrpc.IstioCertificateServiceStub stub =
                IstioCertificateServiceGrpc.newStub(channel).withInterceptors(getJwtHeaderInterceptor());

        CountDownLatch countDownLatch = new CountDownLatch(1);
        StringBuffer publicKeyBuilder = new StringBuffer();
        AtomicBoolean failed = new AtomicBoolean(false);

        StreamObserver observer = generateResponseObserver(countDownLatch, publicKeyBuilder, failed);
        stub.createCertificate(generateRequest(csr), observer);

        long expireTime =
                System.currentTimeMillis() + (long) (istioEnv.getSecretTTL() * istioEnv.getSecretGracePeriodRatio());

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RpcException("Generate Cert Failed. Wait for cert failed.", e);
        }

        if (failed.get()) {
            throw new RpcException("Generate Cert Failed. Send csr request failed. Please check log above.");
        }

        String privateKeyPem = generatePrivatePemKey(privateKey);
        CertPair certPair =
                new CertPair(privateKeyPem, publicKeyBuilder.toString(), System.currentTimeMillis(), expireTime);

        channel.shutdown();
        return certPair;
    }

    private void updateTrust(List<String> trustChains) {
        try {
            this.trustChain = new X509CertChains(trustChains);
        } catch (Exception e) {
            logger.error(REGISTRY_FAILED_GENERATE_KEY_ISTIO, "Got exception when resolving trust chains from istio", e);
        }
    }

    private ClientInterceptor getJwtHeaderInterceptor() {
        Metadata headerWithJwt = new Metadata();
        Metadata.Key<String> key = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        headerWithJwt.put(key, "Bearer " + istioEnv.getServiceAccount());

        key = Metadata.Key.of("ClusterID", Metadata.ASCII_STRING_MARSHALLER);
        headerWithJwt.put(key, istioEnv.getIstioMetaClusterId());
        return MetadataUtils.newAttachHeadersInterceptor(headerWithJwt);
    }

    private IstioCertificateRequest generateRequest(String csr) {
        return IstioCertificateRequest.newBuilder()
                .setCsr(csr)
                .setValidityDuration(istioEnv.getSecretTTL())
                .build();
    }

    private StreamObserver<IstioCertificateResponse> generateResponseObserver(
            CountDownLatch countDownLatch, StringBuffer publicKeyBuilder, AtomicBoolean failed) {
        return new StreamObserver<IstioCertificateResponse>() {
            @Override
            public void onNext(IstioCertificateResponse istioCertificateResponse) {
                for (int i = 0; i < istioCertificateResponse.getCertChainCount(); i++) {
                    publicKeyBuilder.append(
                            istioCertificateResponse.getCertChainBytes(i).toStringUtf8());
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Receive Cert chain from Istio Citadel. \n" + publicKeyBuilder);
                }
                ;
                updateTrust(istioCertificateResponse.getCertChainList());
                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                failed.set(true);
                logger.error(
                        REGISTRY_RECEIVE_ERROR_MSG_ISTIO,
                        "",
                        "",
                        "Receive error message from Istio Citadel grpc stub.",
                        throwable);
                countDownLatch.countDown();
            }

            @Override
            public void onCompleted() {
                countDownLatch.countDown();
            }
        };
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

    public String generateCsr(PublicKey publicKey, ContentSigner signer) throws IOException {
        GeneralNames subjectAltNames = new GeneralNames(new GeneralName[] {new GeneralName(6, istioEnv.getCsrHost())});

        ExtensionsGenerator extGen = new ExtensionsGenerator();
        extGen.addExtension(Extension.subjectAlternativeName, true, subjectAltNames);

        PKCS10CertificationRequest request = new JcaPKCS10CertificationRequestBuilder(
                        new X500Name("O=" + istioEnv.getTrustDomain()), publicKey)
                .addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate())
                .build(signer);

        String csr = generatePemKey("CERTIFICATE REQUEST", request.getEncoded());

        if (logger.isDebugEnabled()) {
            logger.debug("CSR Request to Istio Citadel. \n" + csr);
        }
        return csr;
    }
}
