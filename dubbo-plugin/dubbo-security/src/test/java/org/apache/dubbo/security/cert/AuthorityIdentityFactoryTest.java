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

import org.apache.dubbo.auth.v1alpha1.AuthorityServiceGrpc;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.grpc.Channel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;

class AuthorityIdentityFactoryTest {
    @Test
    void testGenerate() throws SSLException {
        FrameworkModel frameworkModel = new FrameworkModel();
        AtomicInteger count = new AtomicInteger(0);
        AtomicReference<IdentityInfo> identityInfoRef = new AtomicReference<>();
        MockIdentityInfo identityInfo = new MockIdentityInfo();
        identityInfo.setExpire(false);
        identityInfo.setNeedRefresh(false);
        identityInfoRef.set(identityInfo);

        CertConfig certConfig = Mockito.mock(CertConfig.class);
        Mockito.when(certConfig.getRefreshInterval()).thenReturn(1);
        AuthorityConnector connector = Mockito.mock(AuthorityConnector.class);
        Mockito.when(connector.generateChannel()).thenReturn(Mockito.mock(io.grpc.Channel.class));
        AuthorityIdentityFactory authorityIdentityFactory = new AuthorityIdentityFactory(frameworkModel, certConfig, connector) {
            @Override
            protected IdentityInfo generateIdentity0() throws IOException {
                count.incrementAndGet();
                return identityInfoRef.get();
            }
        };

        Assertions.assertEquals(identityInfo, authorityIdentityFactory.generateIdentity());
        Assertions.assertEquals(1, count.get());
        Assertions.assertTrue(authorityIdentityFactory.isConnected());

        MockIdentityInfo identityInfoNew = new MockIdentityInfo();
        identityInfoNew.setExpire(false);
        identityInfoNew.setNeedRefresh(false);
        identityInfoRef.set(identityInfoNew);

        identityInfo.setExpire(true);
        await().until(() -> 2 == count.get());
        Assertions.assertEquals(identityInfoNew, authorityIdentityFactory.generateIdentity());

        authorityIdentityFactory.disConnect();
        identityInfoNew.setExpire(true);
        Assertions.assertNull(authorityIdentityFactory.generateIdentity());
        Assertions.assertFalse(authorityIdentityFactory.isConnected());

        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) frameworkModel
            .getBeanFactory().getBean(FrameworkExecutorRepository.class).getSharedScheduledExecutor();

        Assertions.assertNull(authorityIdentityFactory.generateIdentity());
        await().until(() -> executor.getTaskCount() == executor.getCompletedTaskCount());
        Assertions.assertFalse(authorityIdentityFactory.isConnected());

        authorityIdentityFactory.disConnect();
        Assertions.assertFalse(authorityIdentityFactory.isConnected());

        frameworkModel.destroy();
    }

    @Test
    void testRefresh() throws SSLException {
        FrameworkModel frameworkModel = new FrameworkModel();
        AtomicInteger count = new AtomicInteger(0);
        AtomicReference<IdentityInfo> identityInfoRef = new AtomicReference<>();
        MockIdentityInfo identityInfo = new MockIdentityInfo();
        identityInfo.setExpire(false);
        identityInfo.setNeedRefresh(false);
        identityInfoRef.set(identityInfo);

        CertConfig certConfig = Mockito.mock(CertConfig.class);
        Mockito.when(certConfig.getRefreshInterval()).thenReturn(1);
        AuthorityConnector connector = Mockito.mock(AuthorityConnector.class);
        Mockito.when(connector.generateChannel()).thenReturn(Mockito.mock(io.grpc.Channel.class));
        AuthorityIdentityFactory authorityIdentityFactory = new AuthorityIdentityFactory(frameworkModel, certConfig, connector) {
            @Override
            protected IdentityInfo generateIdentity0() throws IOException {
                count.incrementAndGet();
                return identityInfoRef.get();
            }
        };

        Assertions.assertEquals(identityInfo, authorityIdentityFactory.generateIdentity());
        Assertions.assertEquals(1, count.get());
        Assertions.assertTrue(authorityIdentityFactory.isConnected());

        MockIdentityInfo identityInfoNew = new MockIdentityInfo();
        identityInfoNew.setExpire(false);
        identityInfoNew.setNeedRefresh(false);
        identityInfoRef.set(identityInfoNew);

        identityInfo.setNeedRefresh(true);
        await().until(() -> 2 == count.get());
        await().until(() -> identityInfoNew == authorityIdentityFactory.generateIdentity());

        authorityIdentityFactory.disConnect();
        identityInfoNew.setNeedRefresh(true);
        Assertions.assertEquals(identityInfoNew, authorityIdentityFactory.generateIdentity());

        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) frameworkModel
            .getBeanFactory().getBean(FrameworkExecutorRepository.class).getSharedScheduledExecutor();

        await().until(() -> executor.getTaskCount() == executor.getCompletedTaskCount());
        Assertions.assertFalse(authorityIdentityFactory.isConnected());

        frameworkModel.destroy();
    }

    @Test
    void generateFailed() throws SSLException {
        FrameworkModel frameworkModel = new FrameworkModel();
        AtomicInteger count = new AtomicInteger(0);
        AtomicReference<IdentityInfo> identityInfoRef = new AtomicReference<>();

        CertConfig certConfig = Mockito.mock(CertConfig.class);
        Mockito.when(certConfig.getRefreshInterval()).thenReturn(1);
        AuthorityConnector connector = Mockito.mock(AuthorityConnector.class);
        Mockito.when(connector.generateChannel()).thenReturn(Mockito.mock(io.grpc.Channel.class));
        AuthorityIdentityFactory authorityIdentityFactory = new AuthorityIdentityFactory(frameworkModel, certConfig, connector) {
            @Override
            protected IdentityInfo generateIdentity0() throws IOException {
                count.incrementAndGet();
                return identityInfoRef.get();
            }
        };

        Assertions.assertNull(authorityIdentityFactory.generateIdentity());

        MockIdentityInfo identityInfo = new MockIdentityInfo();
        identityInfo.setExpire(false);
        identityInfo.setNeedRefresh(false);
        identityInfoRef.set(identityInfo);

        Assertions.assertEquals(identityInfo, authorityIdentityFactory.generateIdentity());

        identityInfoRef.set(null);
        identityInfo.setNeedRefresh(true);
        Assertions.assertEquals(identityInfo, authorityIdentityFactory.generateIdentity());

        identityInfo.setExpire(true);
        Assertions.assertNull(authorityIdentityFactory.generateIdentity());
        Assertions.assertNull(authorityIdentityFactory.generateIdentity());

        MockIdentityInfo identityInfo2 = new MockIdentityInfo();
        identityInfo2.setExpire(false);
        identityInfo2.setNeedRefresh(false);
        identityInfoRef.set(identityInfo2);

        Assertions.assertEquals(identityInfo2, authorityIdentityFactory.generateIdentity());

        identityInfoRef.set(null);
        identityInfo2.setExpire(true);
        Assertions.assertNull(authorityIdentityFactory.generateIdentity());

        frameworkModel.destroy();
    }

    @Test
    void testTimeLine1() throws SSLException {
        FrameworkModel frameworkModel = new FrameworkModel();
        AtomicInteger count = new AtomicInteger(0);
        AtomicReference<IdentityInfo> identityInfoRef = new AtomicReference<>();
        AtomicReference<RuntimeException> exceptionRef = new AtomicReference<>();

        CertConfig certConfig = Mockito.mock(CertConfig.class);
        Mockito.when(certConfig.getRefreshInterval()).thenReturn(1);
        AuthorityConnector connector = Mockito.mock(AuthorityConnector.class);
        Mockito.when(connector.generateChannel()).thenReturn(Mockito.mock(io.grpc.Channel.class));
        AuthorityIdentityFactory authorityIdentityFactory = new AuthorityIdentityFactory(frameworkModel, certConfig, connector) {
            @Override
            protected IdentityInfo generateIdentity0() throws IOException {
                count.incrementAndGet();
                if (Objects.nonNull(exceptionRef.get())) {
                    throw exceptionRef.get();
                }
                return identityInfoRef.get();
            }
        };

        // init failed
        Assertions.assertNull(authorityIdentityFactory.generateIdentity());
        exceptionRef.set(new RuntimeException());
        Assertions.assertNull(authorityIdentityFactory.generateIdentity());

        // init success
        exceptionRef.set(null);
        MockIdentityInfo identityInfo = new MockIdentityInfo();
        identityInfo.setExpire(false);
        identityInfo.setNeedRefresh(false);
        identityInfoRef.set(identityInfo);
        Assertions.assertEquals(identityInfo, authorityIdentityFactory.generateIdentity());

        // need refresh
        identityInfo.setNeedRefresh(true);
        Assertions.assertEquals(identityInfo, authorityIdentityFactory.generateIdentity());

        // cert refresh
        MockIdentityInfo identityInfo2 = new MockIdentityInfo();
        identityInfo2.setExpire(false);
        identityInfo2.setNeedRefresh(false);
        identityInfoRef.set(identityInfo2);
        await().until(() -> identityInfo2 == authorityIdentityFactory.generateIdentity());

        // need refresh, ref null
        identityInfoRef.set(null);
        identityInfo2.setExpire(false);
        identityInfo2.setNeedRefresh(true);
        Assertions.assertEquals(identityInfo2, authorityIdentityFactory.generateIdentity());

        // need refresh, exception
        exceptionRef.set(new RuntimeException());
        Assertions.assertEquals(identityInfo2, authorityIdentityFactory.generateIdentity());

        // expire, exception
        identityInfo2.setExpire(true);
        Assertions.assertNull(authorityIdentityFactory.generateIdentity());

        // expire, ref null
        exceptionRef.set(null);
        Assertions.assertNull(authorityIdentityFactory.generateIdentity());

        // refresh expire
        MockIdentityInfo identityInfo3 = new MockIdentityInfo();
        identityInfo3.setExpire(true);
        identityInfo3.setNeedRefresh(true);
        identityInfoRef.set(identityInfo3);
        Assertions.assertNull(authorityIdentityFactory.generateIdentity());

        // refresh need refresh
        identityInfo3.setExpire(false);
        Assertions.assertEquals(identityInfo3, authorityIdentityFactory.generateIdentity());

        // refresh success
        MockIdentityInfo identityInfo4 = new MockIdentityInfo();
        identityInfo4.setExpire(false);
        identityInfo4.setNeedRefresh(false);
        identityInfoRef.set(identityInfo4);
        await().until(()-> identityInfo4 == authorityIdentityFactory.generateIdentity());

        frameworkModel.destroy();
    }

    @Test
    void testTimeLine2() throws SSLException {
        FrameworkModel frameworkModel = new FrameworkModel();
        AtomicInteger count = new AtomicInteger(0);
        AtomicReference<IdentityInfo> identityInfoRef = new AtomicReference<>();
        AtomicReference<RuntimeException> exceptionRef = new AtomicReference<>();

        CertConfig certConfig = Mockito.mock(CertConfig.class);
        Mockito.when(certConfig.getRefreshInterval()).thenReturn(1);
        AuthorityConnector connector = Mockito.mock(AuthorityConnector.class);
        Mockito.when(connector.generateChannel()).thenReturn(Mockito.mock(io.grpc.Channel.class));
        AuthorityIdentityFactory authorityIdentityFactory = new AuthorityIdentityFactory(frameworkModel, certConfig, connector) {
            @Override
            protected IdentityInfo generateIdentity0() throws IOException {
                count.incrementAndGet();
                if (Objects.nonNull(exceptionRef.get())) {
                    throw exceptionRef.get();
                }
                return identityInfoRef.get();
            }
        };

        // init failed
        Assertions.assertNull(authorityIdentityFactory.getIdentityInfo());
        exceptionRef.set(new RuntimeException());
        int c1 = count.get();
        await().until(() -> c1 + 10 < count.get());
        Assertions.assertNull(authorityIdentityFactory.getIdentityInfo());

        // init success
        exceptionRef.set(null);
        MockIdentityInfo identityInfo = new MockIdentityInfo();
        identityInfo.setExpire(false);
        identityInfo.setNeedRefresh(false);
        identityInfoRef.set(identityInfo);
        await().until(() -> identityInfo == authorityIdentityFactory.getIdentityInfo());

        // need refresh
        identityInfo.setExpire(false);
        identityInfo.setNeedRefresh(true);
        int c3 = count.get();
        await().until(() -> c3 + 10 < count.get());
        Assertions.assertEquals(identityInfo, authorityIdentityFactory.getIdentityInfo());

        // cert refresh
        MockIdentityInfo identityInfo2 = new MockIdentityInfo();
        identityInfo2.setExpire(false);
        identityInfo2.setNeedRefresh(false);
        identityInfoRef.set(identityInfo2);
        await().until(() -> identityInfo2 == authorityIdentityFactory.getIdentityInfo());

        // need refresh, ref null
        identityInfo2.setExpire(false);
        identityInfo2.setNeedRefresh(true);
        identityInfoRef.set(null);
        int c4 = count.get();
        await().until(() -> c4 + 10 < count.get());
        Assertions.assertEquals(identityInfo2, authorityIdentityFactory.getIdentityInfo());

        // need refresh, exception
        exceptionRef.set(new RuntimeException());
        int c5 = count.get();
        await().until(() -> c5 + 10 < count.get());
        Assertions.assertEquals(identityInfo2, authorityIdentityFactory.getIdentityInfo());

        // expire, exception
        identityInfo2.setExpire(true);
        await().until(() -> null == authorityIdentityFactory.getIdentityInfo());

        // expire, ref null
        exceptionRef.set(null);
        int c7 = count.get();
        await().until(() -> c7 + 10 < count.get());
        Assertions.assertNull(authorityIdentityFactory.getIdentityInfo());

        // refresh expire
        MockIdentityInfo identityInfo3 = new MockIdentityInfo();
        identityInfo3.setExpire(true);
        identityInfo3.setNeedRefresh(true);
        identityInfoRef.set(identityInfo3);
        int c8 = count.get();
        await().until(() -> c8 + 10 < count.get());
        Assertions.assertNull(authorityIdentityFactory.getIdentityInfo());

        // refresh need refresh
        identityInfo3.setExpire(false);
        await().until(() -> identityInfo3 == authorityIdentityFactory.getIdentityInfo());

        // refresh success
        MockIdentityInfo identityInfo4 = new MockIdentityInfo();
        identityInfo4.setExpire(false);
        identityInfo4.setNeedRefresh(false);
        identityInfoRef.set(identityInfo4);
        await().until(()-> identityInfo4 == authorityIdentityFactory.getIdentityInfo());

        frameworkModel.destroy();
    }

    @Test
    void testGenerateIdentity() throws IOException {
        FrameworkModel frameworkModel = new FrameworkModel();

        CertConfig certConfig = Mockito.mock(CertConfig.class);
        Mockito.when(certConfig.getRefreshInterval()).thenReturn(Integer.MAX_VALUE);
        AuthorityConnector connector = Mockito.mock(AuthorityConnector.class);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(connector.generateChannel()).thenReturn(channel);
        Mockito.when(connector.isConnected()).thenReturn(true);

        AuthorityServiceGrpc.AuthorityServiceBlockingStub stub = Mockito.mock(AuthorityServiceGrpc.AuthorityServiceBlockingStub.class);
        Mockito.when(connector.setHeaders(Mockito.any())).thenReturn(stub);

        try (MockedStatic<CertServiceUtil> mocked = Mockito.mockStatic(CertServiceUtil.class)) {
            IdentityInfo identityInfo = new MockIdentityInfo();
            mocked.when(()->CertServiceUtil.refreshCert(stub, "CONNECTION")).thenReturn(identityInfo);
            AuthorityIdentityFactory authorityIdentityFactory = new AuthorityIdentityFactory(frameworkModel, certConfig, connector);
            Assertions.assertEquals(identityInfo, authorityIdentityFactory.generateIdentity0());
        }

        frameworkModel.destroy();
    }
}
