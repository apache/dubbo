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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

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
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_SSL_CERT_GENERATE_FAILED;

public class CertUtils {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(CertUtils.class);

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

    /**
     * Generate private key in pem encoded
     *
     * @param keyPair key pair
     * @return private key
     * @throws IOException ioException
     */
    protected static String generatePrivatePemKey(KeyPair keyPair) throws IOException {
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
    protected static String generatePemKey(String type, byte[] content) throws IOException {
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
    protected static String generateCsr(KeyPair keyPair) throws IOException {
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
