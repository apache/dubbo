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
import org.apache.dubbo.common.ssl.AuthPolicy;
import org.apache.dubbo.common.ssl.Cert;
import org.apache.dubbo.common.ssl.ProviderCert;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

class AuthorityCertProviderTest {
    @Test
    void testEnable() {
        FrameworkModel frameworkModel = new FrameworkModel();
        AuthorityCertProvider provider = new AuthorityCertProvider(frameworkModel);

        AuthorityIdentityFactory authorityIdentityFactory = Mockito.mock(AuthorityIdentityFactory.class);
        frameworkModel.getBeanFactory().registerBean(authorityIdentityFactory);

        Mockito.when(authorityIdentityFactory.isConnected()).thenReturn(true);
        Assertions.assertTrue(provider.isSupport(null));

        Mockito.when(authorityIdentityFactory.isConnected()).thenReturn(false);
        Assertions.assertFalse(provider.isSupport(null));

        frameworkModel.destroy();
    }

    @Test
    void testEnable1() {
        ClassLoader originClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader newClassLoader = new ClassLoader(originClassLoader) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.startsWith("io.grpc.Channel")) {
                    throw new ClassNotFoundException("Test");
                }
                return super.loadClass(name);
            }
        };
        Thread.currentThread().setContextClassLoader(newClassLoader);
        try (MockedConstruction<AuthorityIdentityFactory> construction =
                 Mockito.mockConstruction(AuthorityIdentityFactory.class, (mock, context) -> {
                     // ignore
                 })) {
            FrameworkModel frameworkModel = new FrameworkModel();
            AuthorityCertProvider provider = new AuthorityCertProvider(frameworkModel);

            Assertions.assertFalse(provider.isSupport(null));

            frameworkModel.destroy();
        }
        Thread.currentThread().setContextClassLoader(originClassLoader);
    }

    @Test
    void testEnable2() {
        ClassLoader originClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader newClassLoader = new ClassLoader(originClassLoader) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.startsWith("org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder")) {
                    throw new ClassNotFoundException("Test");
                }
                return super.loadClass(name);
            }
        };
        Thread.currentThread().setContextClassLoader(newClassLoader);
        try (MockedConstruction<AuthorityIdentityFactory> construction =
                 Mockito.mockConstruction(AuthorityIdentityFactory.class, (mock, context) -> {
                     // ignore
                 })) {
            FrameworkModel frameworkModel = new FrameworkModel();
            AuthorityCertProvider provider = new AuthorityCertProvider(frameworkModel);

            Assertions.assertFalse(provider.isSupport(null));

            frameworkModel.destroy();
        }
        Thread.currentThread().setContextClassLoader(originClassLoader);
    }

    @Test
    void getProviderConnectionConfigTest() {
        FrameworkModel frameworkModel = new FrameworkModel();
        AuthorityCertProvider provider = new AuthorityCertProvider(frameworkModel);
        Assertions.assertNull(provider.getProviderConnectionConfig(null));

        AuthorityIdentityFactory authorityIdentityFactory = Mockito.mock(AuthorityIdentityFactory.class);
        frameworkModel.getBeanFactory().registerBean(authorityIdentityFactory);
        IdentityInfo identityInfo = IdentityInfo.builder()
            .setPrivateKey("privateKey")
            .setCertificate("publicKey")
            .setTrustCerts("trustCerts")
            .build();
        Mockito.when(authorityIdentityFactory.isConnected()).thenReturn(true);
        Mockito.when(authorityIdentityFactory.generateIdentity()).thenReturn(identityInfo);

        AuthenticationGovernor authenticationGovernor = Mockito.mock(AuthenticationGovernor.class);
        frameworkModel.getBeanFactory().registerBean(authenticationGovernor);
        Mockito.when(authenticationGovernor.getPortPolicy(12345)).thenReturn(AuthPolicy.NONE);

        ProviderCert providerConnectionConfig = provider.getProviderConnectionConfig(URL.valueOf("127.0.0.1:12345"));
        Assertions.assertArrayEquals("privateKey".getBytes(), providerConnectionConfig.getPrivateKey());
        Assertions.assertArrayEquals("publicKey".getBytes(), providerConnectionConfig.getKeyCertChain());
        Assertions.assertArrayEquals("trustCerts".getBytes(), providerConnectionConfig.getTrustCert());
        Assertions.assertEquals(AuthPolicy.NONE, providerConnectionConfig.getAuthPolicy());

        frameworkModel.destroy();
    }

    @Test
    void getConsumerConnectionConfigTest() {
        FrameworkModel frameworkModel = new FrameworkModel();
        AuthorityCertProvider provider = new AuthorityCertProvider(frameworkModel);
        Assertions.assertNull(provider.getConsumerConnectionConfig(null));

        AuthorityIdentityFactory authorityIdentityFactory = Mockito.mock(AuthorityIdentityFactory.class);
        frameworkModel.getBeanFactory().registerBean(authorityIdentityFactory);
        IdentityInfo identityInfo = IdentityInfo.builder()
            .setPrivateKey("privateKey")
            .setCertificate("publicKey")
            .setTrustCerts("trustCerts")
            .build();
        Mockito.when(authorityIdentityFactory.isConnected()).thenReturn(true);
        Mockito.when(authorityIdentityFactory.generateIdentity()).thenReturn(identityInfo);
        Cert connectionConfig = provider.getConsumerConnectionConfig(null);
        Assertions.assertArrayEquals("privateKey".getBytes(), connectionConfig.getPrivateKey());
        Assertions.assertArrayEquals("publicKey".getBytes(), connectionConfig.getKeyCertChain());
        Assertions.assertArrayEquals("trustCerts".getBytes(), connectionConfig.getTrustCert());

        frameworkModel.destroy();
    }

    @Test
    void testInit() {
        FrameworkModel frameworkModel = new FrameworkModel();
        AuthorityCertProvider provider = new AuthorityCertProvider(frameworkModel);
        Assertions.assertNull(provider.getConsumerConnectionConfig(null));
        Assertions.assertNull(provider.getProviderConnectionConfig(null));

        AuthorityIdentityFactory authorityIdentityFactory = Mockito.mock(AuthorityIdentityFactory.class);
        frameworkModel.getBeanFactory().registerBean(authorityIdentityFactory);

        Assertions.assertNull(provider.getConsumerConnectionConfig(null));
        Assertions.assertNull(provider.getProviderConnectionConfig(null));

        AuthenticationGovernor authenticationGovernor = Mockito.mock(AuthenticationGovernor.class);
        frameworkModel.getBeanFactory().registerBean(authenticationGovernor);
        Mockito.when(authenticationGovernor.getPortPolicy(12345)).thenReturn(AuthPolicy.NONE);

        Assertions.assertNull(provider.getConsumerConnectionConfig(null));
        Assertions.assertNull(provider.getProviderConnectionConfig(null));

        IdentityInfo identityInfo = IdentityInfo.builder()
            .setPrivateKey("privateKey")
            .setCertificate("publicKey")
            .setTrustCerts("trustCerts")
            .build();
        Mockito.when(authorityIdentityFactory.generateIdentity()).thenReturn(identityInfo);
        Cert connectionConfig = provider.getConsumerConnectionConfig(null);
        Assertions.assertArrayEquals("privateKey".getBytes(), connectionConfig.getPrivateKey());
        Assertions.assertArrayEquals("publicKey".getBytes(), connectionConfig.getKeyCertChain());
        Assertions.assertArrayEquals("trustCerts".getBytes(), connectionConfig.getTrustCert());

        ProviderCert providerConnectionConfig = provider.getProviderConnectionConfig(URL.valueOf("127.0.0.1:12345"));
        Assertions.assertArrayEquals("privateKey".getBytes(), providerConnectionConfig.getPrivateKey());
        Assertions.assertArrayEquals("publicKey".getBytes(), providerConnectionConfig.getKeyCertChain());
        Assertions.assertArrayEquals("trustCerts".getBytes(), providerConnectionConfig.getTrustCert());
        Assertions.assertEquals(AuthPolicy.NONE, providerConnectionConfig.getAuthPolicy());

        frameworkModel.destroy();
    }
}
