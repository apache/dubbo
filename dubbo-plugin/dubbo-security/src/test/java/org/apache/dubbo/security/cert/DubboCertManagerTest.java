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

import org.apache.dubbo.auth.v1alpha1.DubboCertificateResponse;
import org.apache.dubbo.auth.v1alpha1.DubboCertificateServiceGrpc;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.grpc.Channel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.mockito.Answers.CALLS_REAL_METHODS;

class DubboCertManagerTest {
    @Test
    void test1() {
        FrameworkModel frameworkModel = new FrameworkModel();
        DubboCertManager certManager = new DubboCertManager(frameworkModel) {
            @Override
            protected void connect0(CertConfig certConfig) {
                Assertions.assertEquals("127.0.0.1:30060", certConfig.getRemoteAddress());
                Assertions.assertEquals("caCertPath", certConfig.getCaCertPath());
            }

            @Override
            protected CertPair generateCert() {
                return null;
            }

            @Override
            protected void scheduleRefresh() {

            }
        };
        certManager.connect(new CertConfig("127.0.0.1:30060", null, "caCertPath", "oidc"));
        Assertions.assertEquals(new CertConfig("127.0.0.1:30060", null, "caCertPath", "oidc"), certManager.certConfig);

        certManager.connect(new CertConfig("127.0.0.1:30060", "Kubernetes", "caCertPath", "oidc123"));
        Assertions.assertEquals(new CertConfig("127.0.0.1:30060", "Kubernetes", "caCertPath", "oidc123"), certManager.certConfig);

        certManager.connect(new CertConfig("127.0.0.1:30060", "kubernetes", "caCertPath", "oidc345"));
        Assertions.assertEquals(new CertConfig("127.0.0.1:30060", "kubernetes", "caCertPath", "oidc345"), certManager.certConfig);

        CertConfig certConfig = new CertConfig("127.0.0.1:30060", "vm", "caCertPath", "oidc");
        Assertions.assertThrows(IllegalArgumentException.class, () -> certManager.connect(certConfig));
        Assertions.assertEquals(new CertConfig("127.0.0.1:30060", "kubernetes", "caCertPath", "oidc345"), certManager.certConfig);

        certManager.connect(null);
        Assertions.assertEquals(new CertConfig("127.0.0.1:30060", "kubernetes", "caCertPath", "oidc345"), certManager.certConfig);

        certManager.connect(new CertConfig(null, null, null, null));
        Assertions.assertEquals(new CertConfig("127.0.0.1:30060", "kubernetes", "caCertPath", "oidc345"), certManager.certConfig);

        certManager.channel = Mockito.mock(Channel.class);
        certManager.connect(new CertConfig("error", null, "error", "error"));
        Assertions.assertEquals(new CertConfig("127.0.0.1:30060", "kubernetes", "caCertPath", "oidc345"), certManager.certConfig);

        frameworkModel.destroy();
    }

    @Test
    void testRefresh() {
        FrameworkModel frameworkModel = new FrameworkModel();
        AtomicInteger count = new AtomicInteger(0);
        DubboCertManager certManager = new DubboCertManager(frameworkModel) {
            @Override
            protected CertPair generateCert() {
                count.incrementAndGet();
                return null;
            }
        };

        certManager.certConfig = new CertConfig(null, null, null, null, 10);
        certManager.scheduleRefresh();

        Assertions.assertNotNull(certManager.refreshFuture);
        await().until(() -> count.get() > 1);
        certManager.refreshFuture.cancel(false);
        frameworkModel.destroy();
    }

    @Test
    void testConnect1() {
        FrameworkModel frameworkModel = new FrameworkModel();
        DubboCertManager certManager = new DubboCertManager(frameworkModel);
        CertConfig certConfig = new CertConfig("127.0.0.1:30062", null, null, null);
        certManager.connect0(certConfig);
        Assertions.assertNotNull(certManager.channel);
        Assertions.assertEquals("127.0.0.1:30062", certManager.channel.authority());

        frameworkModel.destroy();
    }

    @Test
    void testConnect2() {
        FrameworkModel frameworkModel = new FrameworkModel();
        DubboCertManager certManager = new DubboCertManager(frameworkModel);
        String file = this.getClass().getClassLoader().getResource("certs/ca.crt").getFile();
        CertConfig certConfig = new CertConfig("127.0.0.1:30062", null, file, null);
        certManager.connect0(certConfig);
        Assertions.assertNotNull(certManager.channel);
        Assertions.assertEquals("127.0.0.1:30062", certManager.channel.authority());

        frameworkModel.destroy();
    }

    @Test
    void testConnect3() {
        FrameworkModel frameworkModel = new FrameworkModel();
        DubboCertManager certManager = new DubboCertManager(frameworkModel);
        String file = this.getClass().getClassLoader().getResource("certs/broken-ca.crt").getFile();
        CertConfig certConfig = new CertConfig("127.0.0.1:30062", null, file, null);
        Assertions.assertThrows(RuntimeException.class, () -> certManager.connect0(certConfig));

        frameworkModel.destroy();
    }

    @Test
    void testDisconnect() {
        FrameworkModel frameworkModel = new FrameworkModel();
        DubboCertManager certManager = new DubboCertManager(frameworkModel);
        ScheduledFuture scheduledFuture = Mockito.mock(ScheduledFuture.class);
        certManager.refreshFuture = scheduledFuture;
        certManager.disConnect();
        Assertions.assertNull(certManager.refreshFuture);
        Mockito.verify(scheduledFuture, Mockito.times(1)).cancel(true);


        certManager.channel = Mockito.mock(Channel.class);
        certManager.disConnect();
        Assertions.assertNull(certManager.channel);

        frameworkModel.destroy();
    }

    @Test
    void testConnected() {
        FrameworkModel frameworkModel = new FrameworkModel();
        DubboCertManager certManager = new DubboCertManager(frameworkModel);

        Assertions.assertFalse(certManager.isConnected());

        certManager.certConfig = Mockito.mock(CertConfig.class);
        Assertions.assertFalse(certManager.isConnected());

        certManager.channel = Mockito.mock(Channel.class);
        Assertions.assertFalse(certManager.isConnected());

        certManager.certPair = Mockito.mock(CertPair.class);
        Assertions.assertTrue(certManager.isConnected());

        frameworkModel.destroy();
    }

    @Test
    void testGenerateCert() {
        FrameworkModel frameworkModel = new FrameworkModel();

        AtomicBoolean exception = new AtomicBoolean(false);
        AtomicReference<CertPair> certPairReference = new AtomicReference<>();
        DubboCertManager certManager = new DubboCertManager(frameworkModel) {
            @Override
            protected CertPair refreshCert() throws IOException {
                if (exception.get()) {
                    throw new IOException("test");
                }
                return certPairReference.get();
            }
        };

        CertPair certPair = new CertPair("", "", "", Long.MAX_VALUE);
        certPairReference.set(certPair);

        Assertions.assertEquals(certPair, certManager.generateCert());

        certManager.certPair = new CertPair("", "", "", Long.MAX_VALUE - 10000);
        Assertions.assertEquals(new CertPair("", "", "", Long.MAX_VALUE - 10000), certManager.generateCert());

        certManager.certPair = new CertPair("", "", "", 0);
        Assertions.assertEquals(certPair, certManager.generateCert());

        certManager.certPair = new CertPair("", "", "", 0);
        certPairReference.set(null);
        Assertions.assertEquals(new CertPair("", "", "", 0), certManager.generateCert());

        exception.set(true);
        Assertions.assertEquals(new CertPair("", "", "", 0), certManager.generateCert());

        frameworkModel.destroy();
    }

    @Test
    void testSignWithRsa() {
        DubboCertManager.KeyPair keyPair = DubboCertManager.signWithRsa();
        Assertions.assertNotNull(keyPair);
        Assertions.assertNotNull(keyPair.getPrivateKey());
        Assertions.assertNotNull(keyPair.getPublicKey());
        Assertions.assertNotNull(keyPair.getSigner());
    }

    @Test
    void testSignWithEcdsa() {
        DubboCertManager.KeyPair keyPair = DubboCertManager.signWithEcdsa();
        Assertions.assertNotNull(keyPair);
        Assertions.assertNotNull(keyPair.getPrivateKey());
        Assertions.assertNotNull(keyPair.getPublicKey());
        Assertions.assertNotNull(keyPair.getSigner());
    }


    @Test
    void testRefreshCert() throws IOException {
        try (MockedStatic<DubboCertManager> managerMock = Mockito.mockStatic(DubboCertManager.class, CALLS_REAL_METHODS)) {
            FrameworkModel frameworkModel = new FrameworkModel();
            DubboCertManager certManager = new DubboCertManager(frameworkModel);
            managerMock.when(DubboCertManager::signWithEcdsa).thenReturn(null);
            managerMock.when(DubboCertManager::signWithRsa).thenReturn(null);

            Assertions.assertNull(certManager.refreshCert());

            managerMock.when(DubboCertManager::signWithEcdsa).thenCallRealMethod();

            certManager.channel = Mockito.mock(Channel.class);
            try (MockedStatic<DubboCertificateServiceGrpc> mockGrpc = Mockito.mockStatic(DubboCertificateServiceGrpc.class, CALLS_REAL_METHODS)) {
                DubboCertificateServiceGrpc.DubboCertificateServiceBlockingStub stub = Mockito.mock(DubboCertificateServiceGrpc.DubboCertificateServiceBlockingStub.class);
                mockGrpc.when(() -> DubboCertificateServiceGrpc.newBlockingStub(Mockito.any(Channel.class))).thenReturn(stub);
                Mockito.when(stub.createCertificate(Mockito.any())).thenReturn(
                    DubboCertificateResponse.newBuilder()
                    .setSuccess(false).build());

                certManager.certConfig = new CertConfig(null, null, null, null);
                Assertions.assertNull(certManager.refreshCert());

                String file = this.getClass().getClassLoader().getResource("certs/token").getFile();
                Mockito.when(stub.withInterceptors(Mockito.any())).thenReturn(stub);
                certManager.certConfig = new CertConfig(null, null, null, file);

                Assertions.assertNull(certManager.refreshCert());
                Mockito.verify(stub, Mockito.times(1)).withInterceptors(Mockito.any());

                Mockito.when(stub.createCertificate(Mockito.any())).thenReturn(
                    DubboCertificateResponse.newBuilder()
                        .setSuccess(true)
                        .setCertPem("certPem")
                        .addTrustCerts("trustCerts")
                        .setExpireTime(123456).build());
                CertPair certPair = certManager.refreshCert();
                Assertions.assertNotNull(certPair);
                Assertions.assertEquals("certPem", certPair.getCertificate());
                Assertions.assertEquals("trustCerts", certPair.getTrustCerts());
                Assertions.assertEquals(123456, certPair.getExpireTime());

                Mockito.when(stub.createCertificate(Mockito.any())).thenReturn(null);
                Assertions.assertNull(certManager.refreshCert());
            }

            frameworkModel.destroy();
        }
    }
}
