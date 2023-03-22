package org.apache.dubbo.security.cert;

import org.apache.dubbo.common.URL;

import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

public class Endpoint {
    private static final String SPIFFE_SCHEME = "spiffe://";
    private static final String DUBBO_SCHEME = "dubbo://";
    private static final String NAMESPACE_SEGMENT = "ns";
    private static final String SERVICE_ACCOUNT_SEGMENT = "sa";
    private final String trustDomain;
    private final String namespace;
    private final String serviceAccount;
    private final URL dubboUrl;
    private final InetSocketAddress socketAddress;

    private Endpoint(String spiffeUrl, URL dubboUrl, InetSocketAddress socketAddress) {
        this.dubboUrl = dubboUrl;
        this.socketAddress = socketAddress;

        String[] tags = null;

        if (spiffeUrl != null && spiffeUrl.startsWith(SPIFFE_SCHEME)) {
            spiffeUrl = spiffeUrl.substring(SPIFFE_SCHEME.length());
            tags = spiffeUrl.split("/");
        }

        if (tags == null || tags.length != 5) {
            trustDomain = null;
            namespace = null;
            serviceAccount = null;
            return;
        }

        if (!NAMESPACE_SEGMENT.equals(tags[1]) || !SERVICE_ACCOUNT_SEGMENT.equals(tags[3])) {
            trustDomain = null;
            namespace = null;
            serviceAccount = null;
            return;
        }

        trustDomain = tags[0];
        namespace = tags[2];
        serviceAccount = tags[4];
    }

    public static Endpoint decodeFromCertificates(Certificate[] certificates, InetSocketAddress socketAddress) throws CertificateParsingException {
        String spiffeUrl = null;
        URL dubboUrl = null;

        for (Certificate certificate : certificates) {
            if (certificate instanceof X509Certificate) {
                X509Certificate x509Certificate = (X509Certificate) certificate;
                Collection<List<?>> sans = x509Certificate.getSubjectAlternativeNames();
                for (List<?> san : sans) {
                    for (Object name : san) {
                        if (name instanceof String) {
                            String sanStr = (String) name;
                            if (sanStr.startsWith(SPIFFE_SCHEME)) {
                                spiffeUrl = sanStr;
                            }
                            if (sanStr.startsWith(DUBBO_SCHEME)) {
                                dubboUrl = URL.valueOf(sanStr);
                            }
                        }
                    }
                }
            }
        }

        return new Endpoint(spiffeUrl, dubboUrl, socketAddress);
    }

    public String getPrincipal() {
        return trustDomain + "/" + NAMESPACE_SEGMENT + "/" + namespace + "/" + SERVICE_ACCOUNT_SEGMENT + "/" + serviceAccount;
    }

    public String getNamespace() {
        if (namespace != null) {
            return namespace;
        }
        if (dubboUrl != null) {
            return dubboUrl.getParameter("kubernetesEnv.namespace");
        }
        return null;
    }

    public String getIp() {
        if (socketAddress != null) {
            return socketAddress.getAddress().getHostAddress();
        }
        if (dubboUrl != null) {
            return dubboUrl.getHost();
        }
        return null;
    }

    public String getParameter(String key) {
        if (dubboUrl != null) {
            return dubboUrl.getParameter(key);
        }
        return null;
    }
}
