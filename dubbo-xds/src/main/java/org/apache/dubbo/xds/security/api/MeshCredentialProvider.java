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
package org.apache.dubbo.xds.security.api;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.ssl.AuthPolicy;
import org.apache.dubbo.common.ssl.Cert;
import org.apache.dubbo.common.ssl.CertProvider;
import org.apache.dubbo.common.ssl.ProviderCert;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.nio.charset.StandardCharsets;

@Activate
public class MeshCredentialProvider implements CertProvider {

    private URL credentialSourceUrl;

    private final TrustSource trustSource;

    private final CertSource certSource;

    private final ServiceAccountJwtSource jwtSource;

    public MeshCredentialProvider(FrameworkModel frameworkModel) {
        this.trustSource = frameworkModel.getExtensionLoader(TrustSource.class).getAdaptiveExtension();
        this.certSource = frameworkModel.getExtensionLoader(CertSource.class).getAdaptiveExtension();
        this.jwtSource =
                frameworkModel.getExtensionLoader(ServiceAccountJwtSource.class).getAdaptiveExtension();
        // TODO for test
        this.credentialSourceUrl = URL.valueOf("xds://localhost:15012?signer=istio");
    }

    @Override
    public boolean isSupport(URL address) {
        String security = address.getParameter("security");
        String mesh = address.getParameter("mesh");
        return "mTLS".equals(security) && StringUtils.isNotEmpty(mesh);
    }

    @Override
    public ProviderCert getProviderConnectionConfig(URL localAddress) {
        return getServiceCredential(localAddress, AuthPolicy.CLIENT_AUTH);
    }

    @Override
    public Cert getConsumerConnectionConfig(URL remoteAddress) {
        return getServiceCredential(remoteAddress, AuthPolicy.SERVER_AUTH);
    }

    private ProviderCert getServiceCredential(URL remoteUrl,AuthPolicy authPolicy) {
        remoteUrl = credentialSourceUrl; //TODO for test
        CertPair cert = certSource.getCert(remoteUrl);
        return new ProviderCert(
                cert.getPublicKey().getBytes(StandardCharsets.UTF_8),
                cert.getPrivateKey().getBytes(StandardCharsets.UTF_8),
                trustSource.getTrustCerts(remoteUrl).readAsBytes(),
                authPolicy);
    }

    public void setUrl(URL credentialSourceUrl) {
        this.credentialSourceUrl = credentialSourceUrl;
    }

    public String getSaJwt() {
        return jwtSource.getSaJwt(credentialSourceUrl);
    }

    public CertPair getCert() {
        return certSource.getCert(credentialSourceUrl);
    }

    public X509CertChains getTrust() {
        return trustSource.getTrustCerts(credentialSourceUrl);
    }
}
