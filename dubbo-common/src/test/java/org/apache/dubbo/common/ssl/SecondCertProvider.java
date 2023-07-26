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
import org.apache.dubbo.common.extension.Activate;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Activate(order = 10000)
public class SecondCertProvider implements CertProvider {
    private static final AtomicBoolean isSupport = new AtomicBoolean(false);
    private static final AtomicReference<ProviderCert> providerCert = new AtomicReference<>();
    private static final AtomicReference<Cert> cert = new AtomicReference<>();
    @Override
    public boolean isSupport(URL address) {
        return isSupport.get();
    }

    @Override
    public ProviderCert getProviderConnectionConfig(URL localAddress) {
        return providerCert.get();
    }

    @Override
    public Cert getConsumerConnectionConfig(URL remoteAddress) {
        return cert.get();
    }

    public static void setSupport(boolean support) {
        isSupport.set(support);
    }

    public static void setProviderCert(ProviderCert providerCert) {
        SecondCertProvider.providerCert.set(providerCert);
    }

    public static void setCert(Cert cert) {
        SecondCertProvider.cert.set(cert);
    }
}
