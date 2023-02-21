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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.FrameworkModel;

import com.google.protobuf.ProtocolStringList;
import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_GENERATE_CERT_ISTIO;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_GENERATE_KEY_ISTIO;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_RECEIVE_ERROR_MSG_ISTIO;

public class DubboCertManager {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DubboCertManager.class);

    private final FrameworkModel frameworkModel;
    private volatile Channel channel;
    private volatile CertPair certPair;

    public DubboCertManager(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    public void connect(String remoteAddress, int port) {
        if (channel != null) {
            return;
        }
        synchronized (this) {
            if (channel != null) {
                return;
            }
            channel = NettyChannelBuilder.forAddress(remoteAddress, port)
                .usePlaintext()
                .build();
            generateCert();
            FrameworkExecutorRepository repository = frameworkModel.getBeanFactory().getBean(FrameworkExecutorRepository.class);
            repository.getSharedScheduledExecutor().scheduleAtFixedRate(this::generateCert, 30, 30, TimeUnit.SECONDS);
        }
    }

    public void disConnect() {

    }

    public boolean isConnected() {
        return channel != null;
    }

    public CertPair generateCert() {
        if (certPair != null && !certPair.isExpire()) {
            return certPair;
        }
        return doGenerateCert();
    }

    private CertPair doGenerateCert() {
        synchronized (this) {
            if (certPair == null || certPair.isExpire()) {
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

    public CertPair createCert() throws IOException {
        PublicKey publicKey = null;
        PrivateKey privateKey = null;
        ContentSigner signer = null;

//        try {
//            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
//            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
//            g.initialize(ecSpec, new SecureRandom());
//            KeyPair keypair = g.generateKeyPair();
//            publicKey = keypair.getPublic();
//            privateKey = keypair.getPrivate();
//            signer = new JcaContentSignerBuilder("SHA256withECDSA").build(keypair.getPrivate());
//        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | OperatorCreationException e) {
//            logger.error(REGISTRY_FAILED_GENERATE_KEY_ISTIO, "", "", "Generate Key with secp256r1 algorithm failed. Please check if your system support. "
//                + "Will attempt to generate with RSA2048.", e);
//        }

        if (publicKey == null) {
            try {
                KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance("RSA");
                kpGenerator.initialize(4096);
                KeyPair keypair = kpGenerator.generateKeyPair();
                publicKey = keypair.getPublic();
                privateKey = keypair.getPrivate();
                signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keypair.getPrivate());
            } catch (NoSuchAlgorithmException | OperatorCreationException e) {
                logger.error(REGISTRY_FAILED_GENERATE_KEY_ISTIO, "", "", "Generate Key with SHA256WithRSA algorithm failed. Please check if your system support.", e);
                throw new RpcException(e);
            }
        }

        String csr = generateCsr(publicKey, signer);
        DubboCertificateServiceGrpc.DubboCertificateServiceStub stub = DubboCertificateServiceGrpc.newStub(channel);

        String privateKeyPem = generatePrivatePemKey(privateKey);
        CompletableFuture<CertPair> future = new CompletableFuture<>();
        stub.createCertificate(generateRequest(csr), generateResponseObserver(future, privateKeyPem));
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private DubboCertificateRequest generateRequest(String csr) {
        return DubboCertificateRequest.newBuilder().setCsr(csr).setType("CONNECTION").build();
    }

    private StreamObserver<DubboCertificateResponse> generateResponseObserver(CompletableFuture<CertPair> future, String privateKey) {
        return new StreamObserver<DubboCertificateResponse>() {
            @Override
            public void onNext(DubboCertificateResponse dubboCertificateResponse) {
                String publicKey = dubboCertificateResponse.getPublicKey();
                ProtocolStringList trustCertsList = dubboCertificateResponse.getTrustCertsList();
                long expireTime = dubboCertificateResponse.getExpireTime();
                CertPair pair = new CertPair(privateKey, publicKey, String.join("\n", trustCertsList), System.currentTimeMillis(), expireTime);
                future.complete(pair);
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error(REGISTRY_RECEIVE_ERROR_MSG_ISTIO, "", "", "Receive error message from Istio Citadel grpc stub.", throwable);
                future.completeExceptionally(throwable);
            }

            @Override
            public void onCompleted() {
                if (future.isDone()) {
                    future.complete(null);
                }
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
        PKCS10CertificationRequest request = new JcaPKCS10CertificationRequestBuilder(
            new X500Name("O=" + "cluster.domain"), publicKey).build(signer);

        String csr = generatePemKey("CERTIFICATE REQUEST", request.getEncoded());

        if (logger.isDebugEnabled()) {
            logger.debug("CSR Request to Istio Citadel. \n" + csr);
        }
        return csr;
    }
}
