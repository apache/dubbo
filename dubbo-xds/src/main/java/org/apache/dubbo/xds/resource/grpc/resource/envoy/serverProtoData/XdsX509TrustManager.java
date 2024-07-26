/*
 * Copyright 2019 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.StringUtils;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import com.google.re2j.Pattern;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CertificateValidationContext;
import io.envoyproxy.envoy.type.matcher.v3.RegexMatcher;
import io.envoyproxy.envoy.type.matcher.v3.StringMatcher;

/**
 * Extension of {@link X509ExtendedTrustManager} that implements verification of SANs (subject-alternate-names) against
 * the list in CertificateValidationContext.
 */
final class XdsX509TrustManager extends X509ExtendedTrustManager implements X509TrustManager {

    // ref: io.grpc.okhttp.internal.OkHostnameVerifier and
    // sun.security.x509.GeneralNameInterface
    private static final int ALT_DNS_NAME = 2;
    private static final int ALT_URI_NAME = 6;
    private static final int ALT_IPA_NAME = 7;

    private final X509ExtendedTrustManager delegate;
    private final CertificateValidationContext certContext;

    XdsX509TrustManager(@Nullable CertificateValidationContext certContext, X509ExtendedTrustManager delegate) {
        Assert.notNull(delegate, "delegate is null");
        this.certContext = certContext;
        this.delegate = delegate;
    }

    private static boolean verifyDnsNameInPattern(
            String altNameFromCert, StringMatcher sanToVerifyMatcher) {
        if (StringUtils.isEmpty(altNameFromCert)) {
            return false;
        }
        switch (sanToVerifyMatcher.getMatchPatternCase()) {
            case EXACT:
                return verifyDnsNameExact(altNameFromCert, sanToVerifyMatcher.getExact(),
                        sanToVerifyMatcher.getIgnoreCase());
            case PREFIX:
                return verifyDnsNamePrefix(altNameFromCert, sanToVerifyMatcher.getPrefix(),
                        sanToVerifyMatcher.getIgnoreCase());
            case SUFFIX:
                return verifyDnsNameSuffix(altNameFromCert, sanToVerifyMatcher.getSuffix(),
                        sanToVerifyMatcher.getIgnoreCase());
            case CONTAINS:
                return verifyDnsNameContains(altNameFromCert, sanToVerifyMatcher.getContains(),
                        sanToVerifyMatcher.getIgnoreCase());
            case SAFE_REGEX:
                return verifyDnsNameSafeRegex(altNameFromCert, sanToVerifyMatcher.getSafeRegex());
            default:
                throw new IllegalArgumentException(
                        "Unknown match-pattern-case " + sanToVerifyMatcher.getMatchPatternCase());
        }
    }

    private static boolean verifyDnsNameSafeRegex(
            String altNameFromCert, RegexMatcher sanToVerifySafeRegex) {
        Pattern safeRegExMatch = Pattern.compile(sanToVerifySafeRegex.getRegex());
        return safeRegExMatch.matches(altNameFromCert);
    }

    private static boolean verifyDnsNamePrefix(
            String altNameFromCert, String sanToVerifyPrefix, boolean ignoreCase) {
        if (StringUtils.isEmpty(sanToVerifyPrefix)) {
            return false;
        }
        return ignoreCase ? altNameFromCert.toLowerCase()
                .startsWith(sanToVerifyPrefix.toLowerCase()) : altNameFromCert.startsWith(sanToVerifyPrefix);
    }

    private static boolean verifyDnsNameSuffix(
            String altNameFromCert, String sanToVerifySuffix, boolean ignoreCase) {
        if (StringUtils.isEmpty(sanToVerifySuffix)) {
            return false;
        }
        return ignoreCase ? altNameFromCert.toLowerCase()
                .endsWith(sanToVerifySuffix.toLowerCase()) : altNameFromCert.endsWith(sanToVerifySuffix);
    }

    private static boolean verifyDnsNameContains(
            String altNameFromCert, String sanToVerifySubstring, boolean ignoreCase) {
        if (StringUtils.isEmpty(sanToVerifySubstring)) {
            return false;
        }
        return ignoreCase ? altNameFromCert.toLowerCase()
                .contains(sanToVerifySubstring.toLowerCase()) : altNameFromCert.contains(sanToVerifySubstring);
    }

    private static boolean verifyDnsNameExact(
            String altNameFromCert, String sanToVerifyExact, boolean ignoreCase) {
        if (StringUtils.isEmpty(sanToVerifyExact)) {
            return false;
        }
        return ignoreCase ? sanToVerifyExact.equalsIgnoreCase(altNameFromCert) :
                sanToVerifyExact.equals(altNameFromCert);
    }

    private static boolean verifyDnsNameInSanList(
            String altNameFromCert, List<StringMatcher> verifySanList) {
        for (StringMatcher verifySan : verifySanList) {
            if (verifyDnsNameInPattern(altNameFromCert, verifySan)) {
                return true;
            }
        }
        return false;
    }

    private static boolean verifyOneSanInList(
            List<?> entry,
            List<StringMatcher> verifySanList) throws CertificateParsingException {
        // from OkHostnameVerifier.getSubjectAltNames
        if (entry == null || entry.size() < 2) {
            throw new CertificateParsingException("Invalid SAN entry");
        }
        Integer altNameType = (Integer) entry.get(0);
        if (altNameType == null) {
            throw new CertificateParsingException("Invalid SAN entry: null altNameType");
        }
        switch (altNameType) {
            case ALT_DNS_NAME:
            case ALT_URI_NAME:
            case ALT_IPA_NAME:
                return verifyDnsNameInSanList((String) entry.get(1), verifySanList);
            default:
                return false;
        }
    }

    // logic from Envoy::Extensions::TransportSockets::Tls::ContextImpl::verifySubjectAltName
    private static void verifySubjectAltNameInLeaf(
            X509Certificate cert, List<StringMatcher> verifyList) throws CertificateException {
        Collection<List<?>> names = cert.getSubjectAlternativeNames();
        if (names == null || names.isEmpty()) {
            throw new CertificateException("Peer certificate SAN check failed");
        }
        for (List<?> name : names) {
            if (verifyOneSanInList(name, verifyList)) {
                return;
            }
        }
        // at this point there's no match
        throw new CertificateException("Peer certificate SAN check failed");
    }

    /**
     * Verifies SANs in the peer cert chain against verify_subject_alt_name in the certContext. This is called from
     * various check*Trusted methods.
     */
    void verifySubjectAltNameInChain(X509Certificate[] peerCertChain) throws CertificateException {
        if (certContext == null) {
            return;
        }
        List<StringMatcher> verifyList = certContext.getMatchSubjectAltNamesList();
        if (verifyList.isEmpty()) {
            return;
        }
        if (peerCertChain == null || peerCertChain.length < 1) {
            throw new CertificateException("Peer certificate(s) missing");
        }
        // verify SANs only in the top cert (leaf cert)
        verifySubjectAltNameInLeaf(peerCertChain[0], verifyList);
    }

    @Override
    public void checkClientTrusted(
            X509Certificate[] chain,
            String authType,
            Socket socket) throws CertificateException {
        delegate.checkClientTrusted(chain, authType, socket);
        verifySubjectAltNameInChain(chain);
    }

    @Override
    public void checkClientTrusted(
            X509Certificate[] chain,
            String authType,
            SSLEngine sslEngine) throws CertificateException {
        delegate.checkClientTrusted(chain, authType, sslEngine);
        verifySubjectAltNameInChain(chain);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        delegate.checkClientTrusted(chain, authType);
        verifySubjectAltNameInChain(chain);
    }

    @Override
    public void checkServerTrusted(
            X509Certificate[] chain,
            String authType,
            Socket socket) throws CertificateException {
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;
            SSLParameters sslParams = sslSocket.getSSLParameters();
            if (sslParams != null) {
                sslParams.setEndpointIdentificationAlgorithm(null);
                sslSocket.setSSLParameters(sslParams);
            }
        }
        delegate.checkServerTrusted(chain, authType, socket);
        verifySubjectAltNameInChain(chain);
    }

    @Override
    public void checkServerTrusted(
            X509Certificate[] chain,
            String authType,
            SSLEngine sslEngine) throws CertificateException {
        SSLParameters sslParams = sslEngine.getSSLParameters();
        if (sslParams != null) {
            sslParams.setEndpointIdentificationAlgorithm(null);
            sslEngine.setSSLParameters(sslParams);
        }
        delegate.checkServerTrusted(chain, authType, sslEngine);
        verifySubjectAltNameInChain(chain);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        delegate.checkServerTrusted(chain, authType);
        verifySubjectAltNameInChain(chain);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }
}
