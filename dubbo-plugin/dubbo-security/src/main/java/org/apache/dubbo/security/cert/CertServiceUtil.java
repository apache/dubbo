package org.apache.dubbo.security.cert;

import org.apache.dubbo.auth.v1alpha1.AuthorityServiceGrpc;
import org.apache.dubbo.auth.v1alpha1.IdentityRequest;
import org.apache.dubbo.auth.v1alpha1.IdentityResponse;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.IOUtils;
import org.apache.dubbo.common.utils.StringUtils;

import io.grpc.Channel;
import io.grpc.Metadata;

import java.io.FileReader;
import java.io.IOException;

import static io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_SSL_CERT_GENERATE_FAILED;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_SSL_CONNECT_INSECURE;
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
    protected static CertPair refreshCert(Channel channel, CertConfig certConfig, String type) throws IOException {
        CertUtils.KeyPair keyPair = signWithEcdsa();

        if (keyPair == null) {
            keyPair = signWithRsa();
        }

        if (keyPair == null) {
            logger.error(CONFIG_SSL_CERT_GENERATE_FAILED, "", "", "Generate Key failed. Please check if your system support.");
            return null;
        }

        String csr = generateCsr(keyPair);
        AuthorityServiceGrpc.AuthorityServiceBlockingStub stub = AuthorityServiceGrpc.newBlockingStub(channel);
        stub = setHeaderIfNeed(stub, certConfig);

        String privateKeyPem = generatePrivatePemKey(keyPair);
        IdentityResponse certificateResponse = stub.createIdentity(generateRequest(csr, type));

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

    /**
     * Generate key pair with RSA
     *
     * @return key pair
     */
    private static IdentityRequest generateRequest(String csr, String type) {
        return IdentityRequest.newBuilder().setCsr(csr).setType(type).build();
    }
}
