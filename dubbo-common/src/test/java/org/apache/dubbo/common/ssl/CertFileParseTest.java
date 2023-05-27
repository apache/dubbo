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
import org.apache.dubbo.common.ssl.util.JdkSslUtils;
import org.apache.dubbo.common.ssl.util.pem.SSLContextBuilderByPem;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;



public class CertFileParseTest {

    @Test
    void testGetProviderConnectionConfig() {

        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        SSLConfigCertProvider sslConfigCertProvider = new SSLConfigCertProvider();

        URL url = URL.valueOf("").setScopeModel(applicationModel);
        Assertions.assertNull(sslConfigCertProvider.getProviderConnectionConfig(url));

        SslConfig sslConfig = new SslConfig();

        sslConfig.setServerKeyPassword("1234567890");

        sslConfig.setServerKeyCertChainPath(this.getClass().getClassLoader().getResource("certs/server_keystore.jks").getFile());
        sslConfig.setServerPrivateKeyPath(this.getClass().getClassLoader().getResource("certs/server_keystore.jks").getFile());
        sslConfig.setServerTrustCertCollectionPath(this.getClass().getClassLoader().getResource("certs/server_truststore.jks").getFile());
        applicationModel.getApplicationConfigManager().setSsl(sslConfig);

        ProviderCert providerCert = sslConfigCertProvider.getProviderConnectionConfig(url);
        Assertions.assertNotNull(providerCert);

        JdkSslUtils.buildJdkSSLContext(null, providerCert.getPrivateKeyInputStream(), providerCert.getTrustCertInputStream(), providerCert.getPassword());
        frameworkModel.destroy();
    }

    @Test
    void testGetConsumerConnectionConfig() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        SSLConfigCertProvider sslConfigCertProvider = new SSLConfigCertProvider();

        URL url = URL.valueOf("").setScopeModel(applicationModel);

        SslConfig sslConfig = new SslConfig();
        sslConfig.setClientKeyPassword("1234567890");
        sslConfig.setClientPrivateKeyPath(this.getClass().getClassLoader().getResource("certs/client_keystore.jks").getFile());
        sslConfig.setClientKeyCertChainPath(this.getClass().getClassLoader().getResource("certs/client_keystore.jks").getFile());
        sslConfig.setClientTrustCertCollectionPath(this.getClass().getClassLoader().getResource("certs/client_truststore.jks").getFile());

        applicationModel.getApplicationConfigManager().setSsl(sslConfig);
        Cert cert = sslConfigCertProvider.getConsumerConnectionConfig(url);


        JdkSslUtils.buildJdkSSLContext(null, cert.getPrivateKeyInputStream(), cert.getTrustCertInputStream(), cert.getPassword());

        frameworkModel.destroy();
    }

    @Test
    void testGetConsumerConnectionPemConfig() throws Exception {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        SSLConfigCertProvider sslConfigCertProvider = new SSLConfigCertProvider();

        URL url = URL.valueOf("").setScopeModel(applicationModel);

        SslConfig sslConfig = new SslConfig();
        applicationModel.getApplicationConfigManager().setSsl(sslConfig);


        sslConfig.setClientKeyCertChainPath(this.getClass().getClassLoader().getResource("certs/cert.pem").getFile());
        sslConfig.setClientPrivateKeyPath(this.getClass().getClassLoader().getResource("certs/key.pem").getFile());
        sslConfig.setClientTrustCertCollectionPath(this.getClass().getClassLoader().getResource("certs/ca.pem").getFile());
        Cert cert = sslConfigCertProvider.getConsumerConnectionConfig(url);

        SSLContextBuilderByPem.buildSSLContextByPem(cert.getKeyCertChainInputStream(), cert.getPrivateKeyInputStream(), cert.getTrustCertInputStream(), cert.getPassword());

        frameworkModel.destroy();
    }
}
