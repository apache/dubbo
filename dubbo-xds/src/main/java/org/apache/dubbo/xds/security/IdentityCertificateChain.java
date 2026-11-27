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
package org.apache.dubbo.xds.security;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Separates Istiod's appended trust bundle from the coherent identity chain required by a KeyManager. */
final class IdentityCertificateChain {
    private IdentityCertificateChain() {}

    static byte[] normalize(byte[] pem, byte[] roots) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> certificates =
                    factory.generateCertificates(new ByteArrayInputStream(pem));
            if (certificates.isEmpty()) throw new IllegalArgumentException("Empty xDS identity certificate chain");
            Set<Certificate> authorities = new HashSet<>(factory.generateCertificates(new ByteArrayInputStream(roots)));
            Set<Certificate> seen = new HashSet<>();
            List<X509Certificate> chain = new ArrayList<>();
            for (Certificate value : certificates) {
                X509Certificate candidate = (X509Certificate) value;
                // Equality compares complete DER certificates, not subjects or public keys.
                if (!seen.add(candidate)) continue;
                if (chain.isEmpty()) {
                    chain.add(candidate);
                    continue;
                }
                X509Certificate previous = chain.get(chain.size() - 1);
                if (!selfSigned(previous)
                        && previous.getIssuerX500Principal().equals(candidate.getSubjectX500Principal())) {
                    try {
                        previous.verify(candidate.getPublicKey());
                        chain.add(candidate);
                        continue;
                    } catch (Exception invalidIssuer) {
                        if (!bundleRoot(candidate, authorities)) throw invalidIssuer;
                    }
                }
                // Istiod appends the complete root bundle after its certificate chain. These
                // roots are not extra identity issuers. A retired self-signed CA may still be in
                // the chain file until the next CSR; discarding it must never restore its trust.
                if (!bundleRoot(candidate, authorities)) {
                    throw new IllegalArgumentException("Unrelated certificate in xDS identity chain");
                }
            }
            StringBuilder output = new StringBuilder();
            Base64.Encoder encoder = Base64.getMimeEncoder(64, new byte[] {'\n'});
            for (X509Certificate certificate : chain) {
                output.append("-----BEGIN CERTIFICATE-----\n")
                        .append(encoder.encodeToString(certificate.getEncoded()))
                        .append("\n-----END CERTIFICATE-----\n");
            }
            return output.toString().getBytes(StandardCharsets.US_ASCII);
        } catch (Exception error) {
            throw new IllegalArgumentException("Invalid xDS identity certificate chain", error);
        }
    }

    private static boolean bundleRoot(X509Certificate certificate, Set<Certificate> authorities) {
        return authorities.contains(certificate) || (certificate.getBasicConstraints() >= 0 && selfSigned(certificate));
    }

    private static boolean selfSigned(X509Certificate certificate) {
        if (!certificate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal())) return false;
        try {
            certificate.verify(certificate.getPublicKey());
            return true;
        } catch (Exception notSelfSigned) {
            return false;
        }
    }
}
