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
import org.apache.dubbo.auth.v1alpha1.IdentityRequest;
import org.apache.dubbo.auth.v1alpha1.IdentityResponse;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.io.IOException;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_SSL_CERT_GENERATE_FAILED;
import static org.apache.dubbo.security.cert.CertUtils.generateCsr;
import static org.apache.dubbo.security.cert.CertUtils.generatePrivatePemKey;
import static org.apache.dubbo.security.cert.CertUtils.signWithEcdsa;
import static org.apache.dubbo.security.cert.CertUtils.signWithRsa;

public class CertServiceUtil {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(CertServiceUtil.class);

    /**
     * Request remote certificate authorization to generate cert pair for current Dubbo instance
     *
     * @return cert pair
     * @throws IOException ioException
     */
    protected static IdentityInfo refreshCert(AuthorityServiceGrpc.AuthorityServiceBlockingStub stub, String type) throws IOException {
        CertUtils.KeyPair keyPair = signWithEcdsa();

        if (keyPair == null) {
            keyPair = signWithRsa();
        }

        if (keyPair == null) {
            logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Generate Key failed. Please check if your system support.");
            return null;
        }

        String csr = generateCsr(keyPair);

        String privateKeyPem = generatePrivatePemKey(keyPair);
        IdentityResponse certificateResponse = stub.createIdentity(generateRequest(csr, type));

        if (certificateResponse == null || !certificateResponse.getSuccess()) {
            logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Failed to generate cert from Dubbo Certificate Authority. " +
                "Message: " + (certificateResponse == null ? "null" : certificateResponse.getMessage()));
            return null;
        }
        logger.info("Successfully generate cert from Dubbo Certificate Authority. Cert expire time: " + certificateResponse.getExpireTime());

        return IdentityInfo.builder()
            .setPrivateKey(privateKeyPem)
            .setCertificate(certificateResponse.getCertPem())
            .setTrustCerts(String.join("\n", certificateResponse.getTrustCertsList()))
            .setRefreshTime(certificateResponse.getRefreshTime())
            .setExpireTime(certificateResponse.getExpireTime())
            .setToken(certificateResponse.getToken())
            .setTrustedTokenPublicKeys(certificateResponse.getTrustedTokenPublicKeysList())
            .build();
    }

    /**
     * Generate key pair with RSA
     *
     * @return key pair
     */
    private static IdentityRequest generateRequest(String csr, String type) {
        return IdentityRequest.newBuilder().setCsr(csr).setType(type).build();
    }
}
