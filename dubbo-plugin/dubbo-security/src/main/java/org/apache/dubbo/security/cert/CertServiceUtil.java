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

        return new IdentityInfo(privateKeyPem,
            certificateResponse.getCertPem(),
            String.join("\n", certificateResponse.getTrustCertsList()),
            certificateResponse.getExpireTime(),
            certificateResponse.getToken(),
            certificateResponse.getTrustedTokenPublicKeysList());
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
