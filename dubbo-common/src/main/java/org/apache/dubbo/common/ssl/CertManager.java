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
package org.apache.dubbo.common.ssl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.net.SocketAddress;
import java.util.List;

public class CertManager {
    private final List<CertProvider> certProviders;

    public CertManager(FrameworkModel frameworkModel) {
        this.certProviders = frameworkModel.getExtensionLoader(CertProvider.class).getActivateExtensions();
    }

    public ProviderCert getProviderConnectionConfig(URL localAddress, SocketAddress remoteAddress) {
        for (CertProvider certProvider : certProviders) {
            if (certProvider.isSupport(localAddress)) {
                ProviderCert cert = certProvider.getProviderConnectionConfig(localAddress);
                if (cert != null) {
                    return cert;
                }
            }
        }
        return null;
    }

    public Cert getConsumerConnectionConfig(URL remoteAddress) {
        for (CertProvider certProvider : certProviders) {
            if (certProvider.isSupport(remoteAddress)) {
                Cert cert = certProvider.getConsumerConnectionConfig(remoteAddress);
                if (cert != null) {
                    return cert;
                }
            }
        }
        return null;
    }
}
