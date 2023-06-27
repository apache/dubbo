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
import org.apache.dubbo.common.ssl.AuthPolicy;
import org.apache.dubbo.common.ssl.Cert;
import org.apache.dubbo.common.ssl.CertProvider;
import org.apache.dubbo.common.ssl.ProviderCert;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.nio.charset.StandardCharsets;

@Activate
public class DubboCertProvider implements CertProvider {
    private final DubboCertManager dubboCertManager;


    public DubboCertProvider(FrameworkModel frameworkModel) {
        dubboCertManager = frameworkModel.getBeanFactory().getBean(DubboCertManager.class);
    }

    @Override
    public boolean isSupport(URL address) {
        return dubboCertManager != null && dubboCertManager.isConnected();
    }

    @Override
    public ProviderCert getProviderConnectionConfig(URL localAddress) {
        CertPair certPair = dubboCertManager.generateCert();
        if (certPair == null) {
            return null;
        }
        return new ProviderCert(certPair.getCertificate().getBytes(StandardCharsets.UTF_8),
            certPair.getPrivateKey().getBytes(StandardCharsets.UTF_8),
            certPair.getTrustCerts().getBytes(StandardCharsets.UTF_8), AuthPolicy.NONE);
    }

    @Override
    public Cert getConsumerConnectionConfig(URL remoteAddress) {
        CertPair certPair = dubboCertManager.generateCert();
        if (certPair == null) {
            return null;
        }
        return new Cert(certPair.getCertificate().getBytes(StandardCharsets.UTF_8),
            certPair.getPrivateKey().getBytes(StandardCharsets.UTF_8),
            certPair.getTrustCerts().getBytes(StandardCharsets.UTF_8));
    }
}
