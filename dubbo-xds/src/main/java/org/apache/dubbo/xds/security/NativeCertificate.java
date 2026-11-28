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

import org.apache.dubbo.common.ssl.AuthPolicy;
import org.apache.dubbo.common.ssl.ProviderCert;
import org.apache.dubbo.xds.security.CertificateFiles.Material;

import javax.net.ssl.TrustManagerFactory;

/** Immutable connection credentials; the plaintext value deliberately stops global SSL fallback. */
public final class NativeCertificate extends ProviderCert {
    private static final NativeCertificate PLAINTEXT = new NativeCertificate();
    private final TlsPolicy policy;
    private final Material identity;
    private final TrustManagerFactory trustManager;
    private final String version;

    private NativeCertificate() {
        super(null, null, null, AuthPolicy.NONE);
        policy = null;
        identity = null;
        trustManager = null;
        version = "plaintext";
    }

    NativeCertificate(TlsPolicy policy, Material identity, Material roots) {
        super(
                IdentityCertificateChain.normalize(identity.chain, roots.roots),
                identity.key,
                roots.roots,
                policy.isClientCertificateRequired() ? AuthPolicy.CLIENT_AUTH : AuthPolicy.SERVER_AUTH);
        this.policy = policy;
        this.identity = identity;
        trustManager = new XdsTrustManager(roots.roots, policy.getValidation()).factory();
        version = identity.getFingerprint() + "/" + roots.getFingerprint();
    }

    static NativeCertificate plaintext() {
        return PLAINTEXT;
    }

    public void checkValidity() {
        if (identity != null) {
            try {
                identity.leaf.checkValidity();
            } catch (Exception error) {
                throw new IllegalStateException("xDS identity certificate has expired", error);
            }
        }
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean isTlsEnabled() {
        return policy != null;
    }

    @Override
    public TrustManagerFactory getTrustManagerFactory() {
        return trustManager;
    }

    @Override
    public String[] getApplicationProtocols() {
        return policy == null ? new String[0] : new String[] {"h2"};
    }

    @Override
    public String getServerName() {
        return policy == null ? null : policy.getServerName();
    }
}
