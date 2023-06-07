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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.ssl.Cert;
import org.apache.dubbo.common.ssl.CertProvider;
import org.apache.dubbo.common.ssl.ProviderCert;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.nio.charset.StandardCharsets;

@Activate
public class AuthorityCertProvider implements CertProvider {
    private final FrameworkModel frameworkModel;
    private volatile AuthorityIdentityFactory authorityIdentityFactory;
    private volatile AuthenticationGovernor authenticationGovernor;

    public AuthorityCertProvider(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public boolean isSupport(URL address) {
        obtainAuthorityCertFactory();
        obtainAuthenticationGovernor();
        return authorityIdentityFactory != null && authorityIdentityFactory.isConnected();
    }

    @Override
    public ProviderCert getProviderConnectionConfig(URL localAddress) {
        obtainAuthorityCertFactory();
        obtainAuthenticationGovernor();
        if (authorityIdentityFactory == null) {
            return null;
        }
        if (authenticationGovernor == null) {
            return null;
        }

        IdentityInfo identityInfo = authorityIdentityFactory.generateIdentity();
        if (identityInfo == null) {
            return null;
        }
        return new ProviderCert(identityInfo.getCertificate().getBytes(StandardCharsets.UTF_8),
            identityInfo.getPrivateKey().getBytes(StandardCharsets.UTF_8),
            identityInfo.getTrustCerts().getBytes(StandardCharsets.UTF_8),
            authenticationGovernor.getPortPolicy(localAddress.getPort()));
    }

    @Override
    public Cert getConsumerConnectionConfig(URL remoteAddress) {
        obtainAuthorityCertFactory();
        if (authorityIdentityFactory == null) {
            return null;
        }
        IdentityInfo identityInfo = authorityIdentityFactory.generateIdentity();
        if (identityInfo == null) {
            return null;
        }
        return new Cert(identityInfo.getCertificate().getBytes(StandardCharsets.UTF_8),
            identityInfo.getPrivateKey().getBytes(StandardCharsets.UTF_8),
            identityInfo.getTrustCerts().getBytes(StandardCharsets.UTF_8));
    }

    private void obtainAuthorityCertFactory() {
        if (authorityIdentityFactory == null) {
            authorityIdentityFactory = frameworkModel.getBeanFactory().getBean(AuthorityIdentityFactory.class);
        }
    }

    private void obtainAuthenticationGovernor() {
        if (authenticationGovernor == null) {
            authenticationGovernor = frameworkModel.getBeanFactory().getBean(AuthenticationGovernor.class);
        }
    }
}
