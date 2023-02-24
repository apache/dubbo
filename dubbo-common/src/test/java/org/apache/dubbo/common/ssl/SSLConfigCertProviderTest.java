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
import org.apache.dubbo.common.ssl.impl.SSLConfigCertProvider;
import org.apache.dubbo.common.utils.IOUtils;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class SSLConfigCertProviderTest {
    @Test
    void testSupported() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        SSLConfigCertProvider sslConfigCertProvider = new SSLConfigCertProvider();

        URL url = URL.valueOf("").setScopeModel(applicationModel);
        Assertions.assertFalse(sslConfigCertProvider.isSupport(url));

        SslConfig sslConfig = new SslConfig();
        applicationModel.getApplicationConfigManager().setSsl(sslConfig);
        Assertions.assertTrue(sslConfigCertProvider.isSupport(url));

        frameworkModel.destroy();
    }

    @Test
    void testGetProviderConnectionConfig() throws IOException {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        SSLConfigCertProvider sslConfigCertProvider = new SSLConfigCertProvider();

        URL url = URL.valueOf("").setScopeModel(applicationModel);
        Assertions.assertNull(sslConfigCertProvider.getProviderConnectionConfig(url));

        SslConfig sslConfig = new SslConfig();
        sslConfig.setServerKeyCertChainPath("keyCert");
        sslConfig.setServerPrivateKeyPath("private");
        applicationModel.getApplicationConfigManager().setSsl(sslConfig);
        ProviderCert providerCert = sslConfigCertProvider.getProviderConnectionConfig(url);
        Assertions.assertNull(providerCert);

        sslConfig.setServerKeyCertChainPath(this.getClass().getClassLoader().getResource("certs/cert.pem").getFile());
        sslConfig.setServerPrivateKeyPath(this.getClass().getClassLoader().getResource("certs/key.pem").getFile());
        providerCert = sslConfigCertProvider.getProviderConnectionConfig(url);
        Assertions.assertNotNull(providerCert);
        Assertions.assertArrayEquals(IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("certs/cert.pem")),
                providerCert.getKeyCertChain());
        Assertions.assertArrayEquals(IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("certs/key.pem")),
                providerCert.getPrivateKey());
        Assertions.assertNull(providerCert.getTrustCert());

        sslConfig.setServerTrustCertCollectionPath(this.getClass().getClassLoader().getResource("certs/ca.pem").getFile());
        providerCert = sslConfigCertProvider.getProviderConnectionConfig(url);
        Assertions.assertNotNull(providerCert);
        Assertions.assertArrayEquals(IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("certs/cert.pem")),
                providerCert.getKeyCertChain());
        Assertions.assertArrayEquals(IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("certs/key.pem")),
                providerCert.getPrivateKey());
        Assertions.assertArrayEquals(IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("certs/ca.pem")),
                providerCert.getTrustCert());

        frameworkModel.destroy();
    }

    @Test
    void testGetConsumerConnectionConfig() throws IOException {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        SSLConfigCertProvider sslConfigCertProvider = new SSLConfigCertProvider();

        URL url = URL.valueOf("").setScopeModel(applicationModel);
        Assertions.assertNull(sslConfigCertProvider.getConsumerConnectionConfig(url));

        SslConfig sslConfig = new SslConfig();
        sslConfig.setClientKeyCertChainPath("keyCert");
        sslConfig.setClientPrivateKeyPath("private");
        applicationModel.getApplicationConfigManager().setSsl(sslConfig);
        Cert cert = sslConfigCertProvider.getConsumerConnectionConfig(url);
        Assertions.assertNull(cert);

        sslConfig.setClientKeyCertChainPath(this.getClass().getClassLoader().getResource("certs/cert.pem").getFile());
        sslConfig.setClientPrivateKeyPath(this.getClass().getClassLoader().getResource("certs/key.pem").getFile());
        cert = sslConfigCertProvider.getConsumerConnectionConfig(url);
        Assertions.assertNotNull(cert);
        Assertions.assertArrayEquals(IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("certs/cert.pem")),
                cert.getKeyCertChain());
        Assertions.assertArrayEquals(IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("certs/key.pem")),
                cert.getPrivateKey());

        sslConfig.setClientTrustCertCollectionPath(this.getClass().getClassLoader().getResource("certs/ca.pem").getFile());
        cert = sslConfigCertProvider.getConsumerConnectionConfig(url);
        Assertions.assertNotNull(cert);
        Assertions.assertArrayEquals(IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("certs/cert.pem")),
                cert.getKeyCertChain());
        Assertions.assertArrayEquals(IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("certs/key.pem")),
                cert.getPrivateKey());
        Assertions.assertArrayEquals(IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("certs/ca.pem")),
                cert.getTrustCert());

        frameworkModel.destroy();
    }
}
