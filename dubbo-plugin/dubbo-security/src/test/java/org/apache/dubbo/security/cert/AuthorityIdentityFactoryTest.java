///*
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.apache.dubbo.security.cert;
//
//import org.apache.dubbo.auth.v1alpha1.DubboCertificateResponse;
//import org.apache.dubbo.auth.v1alpha1.DubboCertificateServiceGrpc;
//import org.apache.dubbo.rpc.model.FrameworkModel;
//
//import io.grpc.Channel;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.mockito.MockedStatic;
//import org.mockito.Mockito;
//
//import java.io.IOException;
//import java.util.Collections;
//import java.util.concurrent.ScheduledFuture;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.atomic.AtomicReference;
//
//import static org.awaitility.Awaitility.await;
//import static org.mockito.Answers.CALLS_REAL_METHODS;
//
//class AuthorityIdentityFactoryTest {
//    @Test
//    void test1() {
//        FrameworkModel frameworkModel = new FrameworkModel();
//        AuthorityIdentityFactory certManager = new AuthorityIdentityFactory(frameworkModel) {
//            @Override
//            protected void connect0(CertConfig certConfig) {
//                Assertions.assertEquals("127.0.0.1:30060", certConfig.getRemoteAddress());
//                Assertions.assertEquals("caCertPath", certConfig.getCaCertPath());
//            }
//
//            @Override
//            protected IdentityInfo generateIdentity() {
//                return null;
//            }
//
//            @Override
//            protected void scheduleRefresh() {
//
//            }
//        };
//        certManager.connect(new CertConfig("127.0.0.1:30060", null, "caCertPath", "oidc"));
//        Assertions.assertEquals(new CertConfig("127.0.0.1:30060", null, "caCertPath", "oidc"), certManager.certConfig);
//
//        certManager.connect(new CertConfig("127.0.0.1:30060", "Kubernetes", "caCertPath", "oidc123"));
//        Assertions.assertEquals(new CertConfig("127.0.0.1:30060", "Kubernetes", "caCertPath", "oidc123"), certManager.certConfig);
//
//        certManager.connect(new CertConfig("127.0.0.1:30060", "kubernetes", "caCertPath", "oidc345"));
//        Assertions.assertEquals(new CertConfig("127.0.0.1:30060", "kubernetes", "caCertPath", "oidc345"), certManager.certConfig);
//
//        CertConfig certConfig = new CertConfig("127.0.0.1:30060", "vm", "caCertPath", "oidc");
//        Assertions.assertThrows(IllegalArgumentException.class, () -> certManager.connect(certConfig));
//        Assertions.assertEquals(new CertConfig("127.0.0.1:30060", "kubernetes", "caCertPath", "oidc345"), certManager.certConfig);
//
//        certManager.connect(null);
//        Assertions.assertEquals(new CertConfig("127.0.0.1:30060", "kubernetes", "caCertPath", "oidc345"), certManager.certConfig);
//
//        certManager.connect(new CertConfig(null, null, null, null));
//        Assertions.assertEquals(new CertConfig("127.0.0.1:30060", "kubernetes", "caCertPath", "oidc345"), certManager.certConfig);
//
//        certManager.channel = Mockito.mock(Channel.class);
//        certManager.connect(new CertConfig("error", null, "error", "error"));
//        Assertions.assertEquals(new CertConfig("127.0.0.1:30060", "kubernetes", "caCertPath", "oidc345"), certManager.certConfig);
//
//        frameworkModel.destroy();
//    }
//
//    @Test
//    void testRefresh() {
//        FrameworkModel frameworkModel = new FrameworkModel();
//        AtomicInteger count = new AtomicInteger(0);
//        AuthorityIdentityFactory certManager = new AuthorityIdentityFactory(frameworkModel) {
//            @Override
//            protected IdentityInfo generateIdentity() {
//                count.incrementAndGet();
//                return null;
//            }
//        };
//
//        certManager.certConfig = new CertConfig(null, null, null, null, 10);
//        certManager.scheduleRefresh();
//
//        Assertions.assertNotNull(certManager.refreshFuture);
//        await().until(() -> count.get() > 1);
//        certManager.refreshFuture.cancel(false);
//        frameworkModel.destroy();
//    }
//
//    @Test
//    void testConnect1() {
//        FrameworkModel frameworkModel = new FrameworkModel();
//        AuthorityIdentityFactory certManager = new AuthorityIdentityFactory(frameworkModel);
//        CertConfig certConfig = new CertConfig("127.0.0.1:30062", null, null, null);
//        certManager.connect0(certConfig);
//        Assertions.assertNotNull(certManager.channel);
//        Assertions.assertEquals("127.0.0.1:30062", certManager.channel.authority());
//
//        frameworkModel.destroy();
//    }
//
//    @Test
//    void testConnect2() {
//        FrameworkModel frameworkModel = new FrameworkModel();
//        AuthorityIdentityFactory certManager = new AuthorityIdentityFactory(frameworkModel);
//        String file = this.getClass().getClassLoader().getResource("certs/ca.crt").getFile();
//        CertConfig certConfig = new CertConfig("127.0.0.1:30062", null, file, null);
//        certManager.connect0(certConfig);
//        Assertions.assertNotNull(certManager.channel);
//        Assertions.assertEquals("127.0.0.1:30062", certManager.channel.authority());
//
//        frameworkModel.destroy();
//    }
//
//    @Test
//    void testConnect3() {
//        FrameworkModel frameworkModel = new FrameworkModel();
//        AuthorityIdentityFactory certManager = new AuthorityIdentityFactory(frameworkModel);
//        String file = this.getClass().getClassLoader().getResource("certs/broken-ca.crt").getFile();
//        CertConfig certConfig = new CertConfig("127.0.0.1:30062", null, file, null);
//        Assertions.assertThrows(RuntimeException.class, () -> certManager.connect0(certConfig));
//
//        frameworkModel.destroy();
//    }
//
//    @Test
//    void testDisconnect() {
//        FrameworkModel frameworkModel = new FrameworkModel();
//        AuthorityIdentityFactory certManager = new AuthorityIdentityFactory(frameworkModel);
//        ScheduledFuture scheduledFuture = Mockito.mock(ScheduledFuture.class);
//        certManager.refreshFuture = scheduledFuture;
//        certManager.disConnect();
//        Assertions.assertNull(certManager.refreshFuture);
//        Mockito.verify(scheduledFuture, Mockito.times(1)).cancel(true);
//
//
//        certManager.channel = Mockito.mock(Channel.class);
//        certManager.disConnect();
//        Assertions.assertNull(certManager.channel);
//
//        frameworkModel.destroy();
//    }
//
//    @Test
//    void testConnected() {
//        FrameworkModel frameworkModel = new FrameworkModel();
//        AuthorityIdentityFactory certManager = new AuthorityIdentityFactory(frameworkModel);
//
//        Assertions.assertFalse(certManager.isConnected());
//
//        certManager.certConfig = Mockito.mock(CertConfig.class);
//        Assertions.assertFalse(certManager.isConnected());
//
//        certManager.channel = Mockito.mock(Channel.class);
//        Assertions.assertFalse(certManager.isConnected());
//
//        certManager.identityInfo = Mockito.mock(IdentityInfo.class);
//        Assertions.assertTrue(certManager.isConnected());
//
//        frameworkModel.destroy();
//    }
//
//    @Test
//    void testGenerateCert() {
//        FrameworkModel frameworkModel = new FrameworkModel();
//
//        AtomicBoolean exception = new AtomicBoolean(false);
//        AtomicReference<IdentityInfo> certPairReference = new AtomicReference<>();
//        AuthorityIdentityFactory certManager = new AuthorityIdentityFactory(frameworkModel) {
//            @Override
//            protected IdentityInfo refreshCert() throws IOException {
//                if (exception.get()) {
//                    throw new IOException("test");
//                }
//                return certPairReference.get();
//            }
//        };
//
//        IdentityInfo identityInfo = new IdentityInfo("", "", "", Long.MAX_VALUE, "", Collections.emptyList());
//        certPairReference.set(identityInfo);
//
//        Assertions.assertEquals(identityInfo, certManager.generateIdentity());
//
//        certManager.identityInfo = new IdentityInfo("", "", "", Long.MAX_VALUE - 10000, "", Collections.emptyList());
//        Assertions.assertEquals(new IdentityInfo("", "", "", Long.MAX_VALUE - 10000, "", Collections.emptyList()), certManager.generateIdentity());
//
//        certManager.identityInfo = new IdentityInfo("", "", "", 0, "", Collections.emptyList());
//        Assertions.assertEquals(identityInfo, certManager.generateIdentity());
//
//        certManager.identityInfo = new IdentityInfo("", "", "", 0, "", Collections.emptyList());
//        certPairReference.set(null);
//        Assertions.assertEquals(new IdentityInfo("", "", "", 0, "", Collections.emptyList()), certManager.generateIdentity());
//
//        exception.set(true);
//        Assertions.assertEquals(new IdentityInfo("", "", "", 0, "", Collections.emptyList()), certManager.generateIdentity());
//
//        frameworkModel.destroy();
//    }
//
//    @Test
//    void testSignWithRsa() {
//        CertUtils.KeyPair keyPair = CertUtils.signWithRsa();
//        Assertions.assertNotNull(keyPair);
//        Assertions.assertNotNull(keyPair.getPrivateKey());
//        Assertions.assertNotNull(keyPair.getPublicKey());
//        Assertions.assertNotNull(keyPair.getSigner());
//    }
//
//    @Test
//    void testSignWithEcdsa() {
//        CertUtils.KeyPair keyPair = CertUtils.signWithEcdsa();
//        Assertions.assertNotNull(keyPair);
//        Assertions.assertNotNull(keyPair.getPrivateKey());
//        Assertions.assertNotNull(keyPair.getPublicKey());
//        Assertions.assertNotNull(keyPair.getSigner());
//    }
//
//
//    @Test
//    void testRefreshCert() throws IOException {
//        try (MockedStatic<AuthorityIdentityFactory> managerMock = Mockito.mockStatic(AuthorityIdentityFactory.class, CALLS_REAL_METHODS)) {
//            FrameworkModel frameworkModel = new FrameworkModel();
//            AuthorityIdentityFactory certManager = new AuthorityIdentityFactory(frameworkModel);
//            managerMock.when(CertUtils::signWithEcdsa).thenReturn(null);
//            managerMock.when(CertUtils::signWithRsa).thenReturn(null);
//
//            Assertions.assertNull(certManager.refreshCert());
//
//            managerMock.when(AuthorityIdentityFactory::signWithEcdsa).thenCallRealMethod();
//
//            certManager.channel = Mockito.mock(Channel.class);
//            try (MockedStatic<DubboCertificateServiceGrpc> mockGrpc = Mockito.mockStatic(DubboCertificateServiceGrpc.class, CALLS_REAL_METHODS)) {
//                DubboCertificateServiceGrpc.DubboCertificateServiceBlockingStub stub = Mockito.mock(DubboCertificateServiceGrpc.DubboCertificateServiceBlockingStub.class);
//                mockGrpc.when(() -> DubboCertificateServiceGrpc.newBlockingStub(Mockito.any(Channel.class))).thenReturn(stub);
//                Mockito.when(stub.createCertificate(Mockito.any())).thenReturn(
//                    DubboCertificateResponse.newBuilder()
//                    .setSuccess(false).build());
//
//                certManager.certConfig = new CertConfig(null, null, null, null);
//                Assertions.assertNull(certManager.refreshCert());
//
//                String file = this.getClass().getClassLoader().getResource("certs/token").getFile();
//                Mockito.when(stub.withInterceptors(Mockito.any())).thenReturn(stub);
//                certManager.certConfig = new CertConfig(null, null, null, file);
//
//                Assertions.assertNull(certManager.refreshCert());
//                Mockito.verify(stub, Mockito.times(1)).withInterceptors(Mockito.any());
//
//                Mockito.when(stub.createCertificate(Mockito.any())).thenReturn(
//                    DubboCertificateResponse.newBuilder()
//                        .setSuccess(true)
//                        .setCertPem("certPem")
//                        .addTrustCerts("trustCerts")
//                        .setExpireTime(123456).build());
//                IdentityInfo identityInfo = certManager.refreshCert();
//                Assertions.assertNotNull(identityInfo);
//                Assertions.assertEquals("certPem", identityInfo.getCertificate());
//                Assertions.assertEquals("trustCerts", identityInfo.getTrustCerts());
//                Assertions.assertEquals(123456, identityInfo.getExpireTime());
//
//                Mockito.when(stub.createCertificate(Mockito.any())).thenReturn(null);
//                Assertions.assertNull(certManager.refreshCert());
//            }
//
//            frameworkModel.destroy();
//        }
//    }
//}
