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

import org.apache.dubbo.auth.v1alpha1.IdentityResponse;
import org.apache.dubbo.common.utils.IOUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.time.Instant;

class AuthorityConnectorTest {
    @Test
    void testConfig() {
        FrameworkModel frameworkModel1 = new FrameworkModel();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new AuthorityConnector(frameworkModel1, null);
        });

        CertConfig certConfig1 = new CertConfig(null, null, null, null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new AuthorityConnector(frameworkModel1, certConfig1);
        });

        CertConfig certConfig2 = new CertConfig("127.0.0.1", "test", null, null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new AuthorityConnector(frameworkModel1, certConfig2);
        });

        CertConfig certConfig3 = new CertConfig("127.0.0.1", "Kubernetes", null, null);
        AuthorityConnector authorityConnector3 = new AuthorityConnector(frameworkModel1, certConfig3) {
            @Override
            protected void connect0(CertConfig certConfig) {
                // do nothing
            }

            @Override
            protected void scheduleRefresh() {
                // do nothing
            }

            @Override
            protected IdentityInfo generateCert() {
                return null;
            }
        };
        Assertions.assertFalse(authorityConnector3.isConnected());

        authorityConnector3.disConnect();
        Assertions.assertFalse(authorityConnector3.isConnected());
        frameworkModel1.destroy();

        FrameworkModel frameworkModel2 = new FrameworkModel();
        CertConfig certConfig4 = new CertConfig("127.0.0.1", null, null, null);
        AuthorityConnector authorityConnector4 = new AuthorityConnector(frameworkModel2, certConfig4) {
            @Override
            protected void connect0(CertConfig certConfig) {
                // do nothing
            }

            @Override
            protected void scheduleRefresh() {
                // do nothing
            }

            @Override
            protected IdentityInfo generateCert() {
                return null;
            }
        };
        Assertions.assertFalse(authorityConnector4.isConnected());

        authorityConnector4.disConnect();
        Assertions.assertFalse(authorityConnector4.isConnected());

        authorityConnector4.disConnect();
        Assertions.assertFalse(authorityConnector4.isConnected());

        frameworkModel2.destroy();
    }

    @Test
    void testDuplicatedRegister0(){
        FrameworkModel frameworkModel = new FrameworkModel();

        CertConfig certConfig = new CertConfig("127.0.0.1", null, null, null);
        new AuthorityConnector(frameworkModel, certConfig) {
            @Override
            protected void connect0(CertConfig certConfig) {
                // do nothing
            }

            @Override
            protected void scheduleRefresh() {
                // do nothing
            }

            @Override
            protected IdentityInfo generateCert() {
                return null;
            }
        };

        Assertions.assertThrows(IllegalStateException.class, () -> {
            new AuthorityConnector(frameworkModel, certConfig) {
                @Override
                protected void connect0(CertConfig certConfig) {
                    // do nothing
                }

                @Override
                protected void scheduleRefresh() {
                    // do nothing
                }

                @Override
                protected IdentityInfo generateCert() {
                    return null;
                }
            };
        });

        frameworkModel.destroy();
    }

    @Test
    void testDuplicatedRegister1(){
        FrameworkModel frameworkModel = new FrameworkModel();

        CertConfig certConfig = new CertConfig("127.0.0.1", null, null, null);
        frameworkModel.getBeanFactory().registerBean(Mockito.mock(AuthenticationGovernor.class));

        Assertions.assertThrows(IllegalStateException.class, () -> {
            new AuthorityConnector(frameworkModel, certConfig) {
                @Override
                protected void connect0(CertConfig certConfig) {
                    // do nothing
                }

                @Override
                protected void scheduleRefresh() {
                    // do nothing
                }

                @Override
                protected IdentityInfo generateCert() {
                    return null;
                }
            };
        });

        frameworkModel.destroy();
    }

    @Test
    void testDuplicatedRegister2(){
        FrameworkModel frameworkModel = new FrameworkModel();

        CertConfig certConfig = new CertConfig("127.0.0.1", null, null, null);
        frameworkModel.getBeanFactory().registerBean(Mockito.mock(AuthorityRuleSync.class));

        Assertions.assertThrows(IllegalStateException.class, () -> {
            new AuthorityConnector(frameworkModel, certConfig) {
                @Override
                protected void connect0(CertConfig certConfig) {
                    // do nothing
                }

                @Override
                protected void scheduleRefresh() {
                    // do nothing
                }

                @Override
                protected IdentityInfo generateCert() {
                    return null;
                }
            };
        });

        frameworkModel.destroy();
    }

    @Test
    void testDuplicatedRegister3(){
        FrameworkModel frameworkModel = new FrameworkModel();

        CertConfig certConfig = new CertConfig("127.0.0.1", null, null, null);
        frameworkModel.getBeanFactory().registerBean(Mockito.mock(AuthorityIdentityFactory.class));

        Assertions.assertThrows(IllegalStateException.class, () -> {
            new AuthorityConnector(frameworkModel, certConfig) {
                @Override
                protected void connect0(CertConfig certConfig) {
                    // do nothing
                }

                @Override
                protected void scheduleRefresh() {
                    // do nothing
                }

                @Override
                protected IdentityInfo generateCert() {
                    return null;
                }
            };
        });

        frameworkModel.destroy();
    }

    @Test
    void testConnectInsecure() throws CertificateException, IOException {
        FrameworkModel frameworkModel = new FrameworkModel();

        MockServer server = MockServer.startServer();
        CertConfig certConfig1 = new CertConfig("127.0.0.1:" + server.getServer().getPort(), null, null, null);
        AuthorityConnector authorityConnector1 = new AuthorityConnector(frameworkModel, certConfig1);

        Assertions.assertNotNull(authorityConnector1.rootChannel);
        Assertions.assertInstanceOf(ManagedChannel.class, authorityConnector1.rootChannel);
        Assertions.assertNotNull(server.getAuthorityService().getRequestRef().get());

        authorityConnector1.disConnect();
        server.getServer().shutdown();
        frameworkModel.destroy();
    }

    @Test
    void testConnectSecure() throws CertificateException, IOException {
        FrameworkModel frameworkModel = new FrameworkModel();

        MockServer server = MockServer.startServer();

        File certFile = new File(System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + "-cert.crt");
        FileWriter fileWriter = new FileWriter(certFile);
        IOUtils.write(fileWriter,
            CertUtils.generatePemKey("CERTIFICATE", server.getCertificate().getEncoded()));
        fileWriter.flush();
        fileWriter.close();

        CertConfig certConfig1 = new CertConfig("localhost:" + server.getServer().getPort(), null, certFile.getAbsolutePath(), null);
        AuthorityConnector authorityConnector1 = new AuthorityConnector(frameworkModel, certConfig1);

        Assertions.assertNotNull(authorityConnector1.rootChannel);
        Assertions.assertInstanceOf(ManagedChannel.class, authorityConnector1.rootChannel);
        Assertions.assertNotNull(server.getAuthorityService().getRequestRef().get());

        authorityConnector1.disConnect();
        server.getServer().shutdown();
        frameworkModel.destroy();
    }

    @Test
    void testConnectInvalidServer() throws CertificateException, IOException {
        FrameworkModel frameworkModel = new FrameworkModel();

        MockServer server = MockServer.startServer();

        File certFile = new File(System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + "-cert.crt");
        FileWriter fileWriter = new FileWriter(certFile);
        IOUtils.write(fileWriter,
            CertUtils.generatePemKey("CERTIFICATE", server.getCertificate().getEncoded()));
        fileWriter.flush();
        fileWriter.close();

        CertConfig certConfig1 = new CertConfig("127.0.0.1:" + server.getServer().getPort(), null, certFile.getAbsolutePath(), null);
        AuthorityConnector authorityConnector1 = new AuthorityConnector(frameworkModel, certConfig1);

        Assertions.assertNotNull(authorityConnector1.rootChannel);
        Assertions.assertInstanceOf(ManagedChannel.class, authorityConnector1.rootChannel);
        Assertions.assertNull(server.getAuthorityService().getRequestRef().get());
        Assertions.assertNull(authorityConnector1.generateCert());

        server.getServer().shutdown();
        frameworkModel.destroy();
    }

    @Test
    void testConnectInvalidCert() throws CertificateException, IOException {
        FrameworkModel frameworkModel = new FrameworkModel();

        MockServer server = MockServer.startServer();

        File certFile = new File(System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + "-cert.crt");
        FileWriter fileWriter = new FileWriter(certFile);
        IOUtils.write(fileWriter,
            CertUtils.generatePemKey("CERTIFICATE", server.getCertificate().getEncoded()));
        fileWriter.flush();
        fileWriter.close();

        CertConfig certConfig1 = new CertConfig("127.0.0.1:" + server.getServer().getPort(), null, certFile.getAbsolutePath() + "invalid", null);
        Assertions.assertThrows(RuntimeException.class, () -> new AuthorityConnector(frameworkModel, certConfig1));

        server.getServer().shutdown();
        frameworkModel.destroy();
    }

    @Test
    void testCertSuccess() throws CertificateException, IOException {
        FrameworkModel frameworkModel = new FrameworkModel();

        MockServer server = MockServer.startServer();

        server.getAuthorityService().getResponseRef().set(
            IdentityResponse.newBuilder()
                .setSuccess(true)
                .setToken("token")
                .addTrustedTokenPublicKeys("trustedTokenPublicKey")
                .setCertPem(CertUtils.generatePemKey("CERTIFICATE", server.getCertificate().getEncoded()))
                .addTrustCerts(CertUtils.generatePemKey("CERTIFICATE", server.getCertificate().getEncoded()))
                .setExpireTime(Instant.now().plusSeconds(60).toEpochMilli())
                .setRefreshTime(Instant.now().plusSeconds(30).toEpochMilli())
                .build());

        File certFile = new File(System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + "-cert.crt");
        FileWriter fileWriter = new FileWriter(certFile);
        IOUtils.write(fileWriter,
            CertUtils.generatePemKey("CERTIFICATE", server.getCertificate().getEncoded()));
        fileWriter.flush();
        fileWriter.close();

        CertConfig certConfig1 = new CertConfig("localhost:" + server.getServer().getPort(), null, certFile.getAbsolutePath(), null);
        AuthorityConnector authorityConnector = new AuthorityConnector(frameworkModel, certConfig1);
        IdentityInfo identityInfo = authorityConnector.generateCert();
        Assertions.assertNotNull(identityInfo);
        Assertions.assertEquals("token", identityInfo.getToken());

        server.getServer().shutdown();
        frameworkModel.destroy();
    }


}
