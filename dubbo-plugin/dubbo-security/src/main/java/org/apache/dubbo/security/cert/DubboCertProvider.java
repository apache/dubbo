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
import org.apache.dubbo.common.ssl.Cert;
import org.apache.dubbo.common.ssl.CertProvider;
import org.apache.dubbo.common.ssl.ProviderCert;

import java.nio.charset.StandardCharsets;

public class DubboCertProvider implements CertProvider {
    private final DubboCertManager dubboCertManager = new DubboCertManager();

    @Override
    public boolean isSupport(URL address) {
        return true;
    }

    @Override
    public ProviderCert getProviderConnectionConfig(URL localAddress) {
        CertPair certPair = dubboCertManager.generateCert();
        return new ProviderCert(certPair.getPrivateKey().getBytes(StandardCharsets.UTF_8),
            certPair.getPublicKey().getBytes(StandardCharsets.UTF_8),
            certPair.getTrustCerts().getBytes(StandardCharsets.UTF_8), null, true);
    }

    @Override
    public Cert getConsumerConnectionConfig(URL remoteAddress) {
        CertPair certPair = dubboCertManager.generateCert();
        return new Cert(certPair.getPrivateKey().getBytes(StandardCharsets.UTF_8),
            certPair.getPublicKey().getBytes(StandardCharsets.UTF_8),
            certPair.getTrustCerts().getBytes(StandardCharsets.UTF_8));
    }
}
