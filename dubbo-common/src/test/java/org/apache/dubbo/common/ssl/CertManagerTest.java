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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CertManagerTest {
    private FrameworkModel frameworkModel;
    private URL url;
    @BeforeEach
    void setup() {
        FirstCertProvider.setProviderCert(null);
        FirstCertProvider.setCert(null);
        FirstCertProvider.setSupport(false);

        SecondCertProvider.setProviderCert(null);
        SecondCertProvider.setCert(null);
        SecondCertProvider.setSupport(false);

        frameworkModel = new FrameworkModel();
        url = URL.valueOf("dubbo://").setScopeModel(frameworkModel.newApplication());
    }

    @AfterEach
    void teardown() {
        frameworkModel.destroy();
    }

    @Test
    void testGetConsumerConnectionConfig() {
        CertManager certManager = new CertManager(frameworkModel);

        Assertions.assertNull(certManager.getConsumerConnectionConfig(url));

        Cert cert1 = Mockito.mock(Cert.class);
        FirstCertProvider.setCert(cert1);
        Assertions.assertNull(certManager.getConsumerConnectionConfig(url));

        FirstCertProvider.setSupport(true);
        Assertions.assertEquals(cert1, certManager.getConsumerConnectionConfig(url));

        Cert cert2 = Mockito.mock(Cert.class);
        SecondCertProvider.setCert(cert2);
        Assertions.assertEquals(cert1, certManager.getConsumerConnectionConfig(url));

        SecondCertProvider.setSupport(true);
        Assertions.assertEquals(cert1, certManager.getConsumerConnectionConfig(url));

        FirstCertProvider.setSupport(false);
        Assertions.assertEquals(cert2, certManager.getConsumerConnectionConfig(url));

        FirstCertProvider.setSupport(true);
        FirstCertProvider.setCert(null);
        Assertions.assertEquals(cert2, certManager.getConsumerConnectionConfig(url));
    }

    @Test
    void testGetProviderConnectionConfig() {
        CertManager certManager = new CertManager(frameworkModel);

        Assertions.assertNull(certManager.getProviderConnectionConfig(url, null));

        ProviderCert providerCert1 = Mockito.mock(ProviderCert.class);
        FirstCertProvider.setProviderCert(providerCert1);
        Assertions.assertNull(certManager.getProviderConnectionConfig(url, null));

        FirstCertProvider.setSupport(true);
        Assertions.assertEquals(providerCert1, certManager.getProviderConnectionConfig(url, null));

        ProviderCert providerCert2 = Mockito.mock(ProviderCert.class);
        SecondCertProvider.setProviderCert(providerCert2);
        Assertions.assertEquals(providerCert1, certManager.getProviderConnectionConfig(url, null));

        SecondCertProvider.setSupport(true);
        Assertions.assertEquals(providerCert1, certManager.getProviderConnectionConfig(url, null));

        FirstCertProvider.setSupport(false);
        Assertions.assertEquals(providerCert2, certManager.getProviderConnectionConfig(url, null));

        FirstCertProvider.setSupport(true);
        FirstCertProvider.setProviderCert(null);
        Assertions.assertEquals(providerCert2, certManager.getProviderConnectionConfig(url, null));
    }
}
