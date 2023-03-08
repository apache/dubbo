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

import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;

class CertDeployerListenerTest {
    @Test
    void testEmpty1() {
        AtomicReference<DubboCertManager> reference = new AtomicReference<>();
        try (MockedConstruction<DubboCertManager> construction =
                 Mockito.mockConstruction(DubboCertManager.class, (mock, context) -> {
                     reference.set(mock);
                 })) {
            FrameworkModel frameworkModel = new FrameworkModel();
            ApplicationModel applicationModel = frameworkModel.newApplication();
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("test"));
            applicationModel.getDeployer().start();
            Mockito.verify(reference.get(), Mockito.times(0))
                .connect(Mockito.any());
            applicationModel.getDeployer().stop();
            Mockito.verify(reference.get(), Mockito.atLeast(1)).disConnect();
            frameworkModel.destroy();
        }
    }

    @Test
    void testEmpty2() {
        AtomicReference<DubboCertManager> reference = new AtomicReference<>();
        try (MockedConstruction<DubboCertManager> construction =
                 Mockito.mockConstruction(DubboCertManager.class, (mock, context) -> {
                     reference.set(mock);
                 })) {
            FrameworkModel frameworkModel = new FrameworkModel();
            ApplicationModel applicationModel = frameworkModel.newApplication();
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("test"));
            applicationModel.getApplicationConfigManager().setSsl(new SslConfig());
            applicationModel.getDeployer().start();
            Mockito.verify(reference.get(), Mockito.times(0))
                .connect(Mockito.any());
            applicationModel.getDeployer().stop();
            Mockito.verify(reference.get(), Mockito.atLeast(1)).disConnect();
            frameworkModel.destroy();
        }
    }

    @Test
    void testCreate() {
        AtomicReference<DubboCertManager> reference = new AtomicReference<>();
        try (MockedConstruction<DubboCertManager> construction =
                 Mockito.mockConstruction(DubboCertManager.class, (mock, context) -> {
                     reference.set(mock);
                 })) {
            FrameworkModel frameworkModel = new FrameworkModel();
            ApplicationModel applicationModel = frameworkModel.newApplication();
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("test"));
            SslConfig sslConfig = new SslConfig();
            sslConfig.setCaAddress("127.0.0.1:30060");
            applicationModel.getApplicationConfigManager().setSsl(sslConfig);

            applicationModel.getDeployer().start();
            Mockito.verify(reference.get(), Mockito.times(1))
                .connect(Mockito.any());
            applicationModel.getDeployer().stop();
            Mockito.verify(reference.get(), Mockito.atLeast(1))
                .disConnect();
            frameworkModel.destroy();
        }
    }

    @Test
    void testFailure() {
        AtomicReference<DubboCertManager> reference = new AtomicReference<>();
        try (MockedConstruction<DubboCertManager> construction =
                 Mockito.mockConstruction(DubboCertManager.class, (mock, context) -> {
                     reference.set(mock);
                 })) {
            FrameworkModel frameworkModel = new FrameworkModel();
            ApplicationModel applicationModel = frameworkModel.newApplication();
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("test"));
            SslConfig sslConfig = new SslConfig();
            sslConfig.setCaAddress("127.0.0.1:30060");
            applicationModel.getApplicationConfigManager().setSsl(sslConfig);
            applicationModel.getApplicationConfigManager().addMetadataReport(new MetadataReportConfig("absent"));

            ApplicationDeployer deployer = applicationModel.getDeployer();
            Assertions.assertThrows(IllegalArgumentException.class, deployer::start);
            Mockito.verify(reference.get(), Mockito.times(1))
                .connect(Mockito.any());
            Mockito.verify(reference.get(), Mockito.atLeast(1))
                .disConnect();
            frameworkModel.destroy();
        }
    }

    @Test
    void testNotFound1() {
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
        try (MockedConstruction<DubboCertManager> construction =
                 Mockito.mockConstruction(DubboCertManager.class, (mock, context) -> {
                     // ignore
                 })) {
            FrameworkModel frameworkModel = new FrameworkModel();
            ApplicationModel applicationModel = frameworkModel.newApplication();
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("test"));
            SslConfig sslConfig = new SslConfig();
            sslConfig.setCaAddress("127.0.0.1:30060");
            applicationModel.getApplicationConfigManager().setSsl(sslConfig);

            applicationModel.getDeployer().start();
            applicationModel.getDeployer().stop();
            Assertions.assertEquals(0, construction.constructed().size());
            frameworkModel.destroy();
        }
        Thread.currentThread().setContextClassLoader(originClassLoader);
    }

    @Test
    void testNotFound2() {
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
        try (MockedConstruction<DubboCertManager> construction =
                 Mockito.mockConstruction(DubboCertManager.class, (mock, context) -> {
                     // ignore
                 })) {
            FrameworkModel frameworkModel = new FrameworkModel();
            ApplicationModel applicationModel = frameworkModel.newApplication();
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("test"));
            SslConfig sslConfig = new SslConfig();
            sslConfig.setCaAddress("127.0.0.1:30060");
            applicationModel.getApplicationConfigManager().setSsl(sslConfig);

            applicationModel.getDeployer().start();
            applicationModel.getDeployer().stop();
            Assertions.assertEquals(0, construction.constructed().size());
            frameworkModel.destroy();
        }
        Thread.currentThread().setContextClassLoader(originClassLoader);
    }

    @Test
    void testParams1() {
        AtomicReference<DubboCertManager> reference = new AtomicReference<>();
        try (MockedConstruction<DubboCertManager> construction =
                 Mockito.mockConstruction(DubboCertManager.class, (mock, context) -> {
                     reference.set(mock);
                 })) {
            FrameworkModel frameworkModel = new FrameworkModel();
            ApplicationModel applicationModel = frameworkModel.newApplication();
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("test"));
            SslConfig sslConfig = new SslConfig();
            sslConfig.setCaAddress("127.0.0.1:30060");
            sslConfig.setCaCertPath("certs/ca.crt");
            sslConfig.setOidcTokenPath("token");
            sslConfig.setEnvType("test");
            applicationModel.getApplicationConfigManager().setSsl(sslConfig);

            applicationModel.getDeployer().start();
            Mockito.verify(reference.get(), Mockito.times(1))
                .connect(new CertConfig("127.0.0.1:30060", "test", "certs/ca.crt", "token"));
            applicationModel.getDeployer().stop();
            Mockito.verify(reference.get(), Mockito.atLeast(1))
                .disConnect();
            frameworkModel.destroy();
        }
    }

    @Disabled("Enable me until properties from envs work.")
    @Test
    void testParams2() {
        AtomicReference<DubboCertManager> reference = new AtomicReference<>();
        try (MockedConstruction<DubboCertManager> construction =
                 Mockito.mockConstruction(DubboCertManager.class, (mock, context) -> {
                     reference.set(mock);
                 })) {
            System.setProperty("dubbo.ssl.ca-address", "127.0.0.1:30060");
            System.setProperty("dubbo.ssl.ca-cert-path", "certs/ca.crt");
            System.setProperty("dubbo.ssl.oidc-token-path", "token");
            System.setProperty("dubbo.ssl.env-type", "test");
            FrameworkModel frameworkModel = new FrameworkModel();
            ApplicationModel applicationModel = frameworkModel.newApplication();
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("test"));

            applicationModel.getDeployer().start();
            Mockito.verify(reference.get(), Mockito.times(1))
                .connect(new CertConfig("127.0.0.1:30060", "test", "certs/ca.crt", "token"));
            applicationModel.getDeployer().stop();
            Mockito.verify(reference.get(), Mockito.atLeast(1)).disConnect();
            frameworkModel.destroy();
            System.clearProperty("dubbo.ssl.ca-address");
            System.clearProperty("dubbo.ssl.ca-cert-path");
            System.clearProperty("dubbo.ssl.oidc-token-path");
            System.clearProperty("dubbo.ssl.env-type");
        }
    }
}
