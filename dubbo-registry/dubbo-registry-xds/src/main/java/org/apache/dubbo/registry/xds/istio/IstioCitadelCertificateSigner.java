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
package org.apache.dubbo.registry.xds.istio;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.xds.XdsCertificateSigner;
import org.apache.dubbo.rpc.RpcException;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import istio.v1.auth.Ca;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class IstioCitadelCertificateSigner implements XdsCertificateSigner {

    private static final Logger logger = LoggerFactory.getLogger(IstioCitadelCertificateSigner.class);

    private final IstioEnv istioEnv;

    private CertPair certPairCache;

    public IstioCitadelCertificateSigner() {
        istioEnv = new IstioEnv();
    }

    @Override
    public CertPair request(URL url) {
        if (certPairCache != null && !certPairCache.isExpire()) {
            return certPairCache;
        }
        synchronized (this) {
            if (certPairCache == null || certPairCache.isExpire()) {
                try {
                    certPairCache = createCert(url);
                } catch (IOException e) {
                    logger.error("Generate Cert from Istio failed.", e);
                    throw new RpcException("Generate Cert from Istio failed.", e);
                }
            }
        }
        return certPairCache;
    }

    public CertPair createCert(URL url) throws IOException {
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
                logger.error("Generate Key with secp256r1 algorithm failed. Please check if your system support. " +
                        "Will attempt to generate with RSA2048.", e);
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
                logger.error("Generate Key with SHA256WithRSA algorithm failed. Please check if your system support.", e);
                throw new RpcException(e);
            }
        }

        String csr = generateCsr(publicKey, signer);

        ManagedChannel channel = NettyChannelBuilder.forTarget(istioEnv.getCaAddr())
                .sslContext(GrpcSslContexts.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build())
                .build();

        Metadata header = new Metadata();
        Metadata.Key<String> key = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        header.put(key, "Bearer " + istioEnv.getServiceAccount());

        key = Metadata.Key.of("ClusterID", Metadata.ASCII_STRING_MARSHALLER);
        header.put(key, istioEnv.getIstioMetaClusterId());

        IstioCertificateServiceGrpc.IstioCertificateServiceStub stub = IstioCertificateServiceGrpc.newStub(channel);

        stub = MetadataUtils.attachHeaders(stub, header);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        StringBuffer publicKeyBuilder = new StringBuffer();
        AtomicBoolean failed = new AtomicBoolean(false);

        stub.createCertificate(
                generateRequest(csr),
                generateResponseObserver(countDownLatch, publicKeyBuilder, failed));

        long expireTime = System.currentTimeMillis() + (long) (istioEnv.getSecretTTL() * istioEnv.getSecretGracePeriodRatio());

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RpcException("Generate Cert Failed. Wait for cert failed.", e);
        }

        if (failed.get()) {
            throw new RpcException("Generate Cert Failed. Send csr request failed. Please check log above.");
        }

        String privateKeyPem = generatePrivatePemKey(privateKey);
        CertPair certPair = new CertPair(privateKeyPem, publicKeyBuilder.toString(), expireTime);

        channel.shutdown();
        return certPair;
    }

    private Ca.IstioCertificateRequest generateRequest(String csr) {
        return Ca.IstioCertificateRequest
                .newBuilder()
                .setCsr(csr)
                .setValidityDuration(istioEnv.getSecretTTL())
                .build();
    }

    private StreamObserver<Ca.IstioCertificateResponse> generateResponseObserver(CountDownLatch countDownLatch, StringBuffer publicKeyBuilder, AtomicBoolean failed) {
        return new StreamObserver<Ca.IstioCertificateResponse>() {
            @Override
            public void onNext(Ca.IstioCertificateResponse istioCertificateResponse) {
                for (int i = 0; i < istioCertificateResponse.getCertChainCount(); i++) {
                    publicKeyBuilder.append(istioCertificateResponse.getCertChainBytes(i).toStringUtf8());
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Receive Cert chain from Istio Citadel. \n" + publicKeyBuilder);
                }
                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                failed.set(true);
                logger.error("Receive error message from Istio Citadel grpc stub.", throwable);
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

    private String generateCsr(PublicKey publicKey, ContentSigner signer) throws IOException {
        GeneralNames subjectAltNames = new GeneralNames(
                new GeneralName[]{
                        new GeneralName(6, istioEnv.getCsrHost())
                });

        ExtensionsGenerator extGen = new ExtensionsGenerator();
        extGen.addExtension(Extension.subjectAlternativeName, true, subjectAltNames);

        PKCS10CertificationRequest request =
                new JcaPKCS10CertificationRequestBuilder(
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
