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
package org.apache.dubbo.registry.zookeeper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.remoting.zookeeper.curator5.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.curator5.ZookeeperClientManager;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZookeeperRegistryTest {
    private static String zookeeperConnectionAddress1;
    private ZookeeperRegistry zookeeperRegistry;
    private String service = "org.apache.dubbo.test.injvmServie";
    private String url = "zookeeper://zookeeper/" + service + "?notify=false&methods=test1,test2";
    private URL serviceUrl = URL.valueOf(url);
    private URL anyUrl = URL.valueOf("zookeeper://zookeeper/*");
    private URL registryUrl;
    private ZookeeperRegistryFactory zookeeperRegistryFactory;
    private NotifyListener listener;

    // mock object
    private static ZookeeperClientManager mockZookeeperClientManager;
    private static ZookeeperClient mockZookeeperClient;

    @BeforeAll
    public static void beforeAll() {
        zookeeperConnectionAddress1 = "zookeeper://localhost:" + "2181";
    }

    @BeforeEach
    public void setUp() throws Exception {
        mockZookeeperClientManager = mock(ZookeeperClientManager.class);
        mockZookeeperClient = mock(ZookeeperClient.class);
        this.registryUrl = URL.valueOf(zookeeperConnectionAddress1);
        zookeeperRegistryFactory = new ZookeeperRegistryFactory(ApplicationModel.defaultModel());
        zookeeperRegistryFactory.setZookeeperTransporter(mockZookeeperClientManager);
        when(mockZookeeperClientManager.connect(registryUrl)).thenReturn(mockZookeeperClient);
        this.zookeeperRegistry = (ZookeeperRegistry) zookeeperRegistryFactory.createRegistry(registryUrl);
    }

    @Test
    void testAnyHost() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            URL errorUrl = URL.valueOf("multicast://0.0.0.0/");
            new ZookeeperRegistryFactory(ApplicationModel.defaultModel()).createRegistry(errorUrl);
        });
    }

    @Test
    void testRegister() {
        Set<URL> registered;

        for (int i = 0; i < 2; i++) {
            zookeeperRegistry.register(serviceUrl);
            registered = zookeeperRegistry.getRegistered();
            assertThat(registered.contains(serviceUrl), is(true));
        }

        registered = zookeeperRegistry.getRegistered();
        assertThat(registered.size(), is(1));
    }

    @Test
    void testSubscribe() {
        NotifyListener listener = mock(NotifyListener.class);
        zookeeperRegistry.subscribe(serviceUrl, listener);

        Map<URL, Set<NotifyListener>> subscribed = zookeeperRegistry.getSubscribed();
        assertThat(subscribed.size(), is(1));
        assertThat(subscribed.get(serviceUrl).size(), is(1));

        zookeeperRegistry.unsubscribe(serviceUrl, listener);
        subscribed = zookeeperRegistry.getSubscribed();
        assertThat(subscribed.size(), is(1));
        assertThat(subscribed.get(serviceUrl).size(), is(0));
    }

    @Test
    void testAvailable() {
        zookeeperRegistry.register(serviceUrl);
        when(mockZookeeperClient.isConnected()).thenReturn(true);
        assertThat(zookeeperRegistry.isAvailable(), is(true));

        zookeeperRegistry.destroy();
        assertThat(zookeeperRegistry.isAvailable(), is(false));
    }

    @Test
    void testLookup() {
        when(mockZookeeperClient.getChildren(any())).thenReturn(Lists.newArrayList(url));
        List<URL> lookup = zookeeperRegistry.lookup(serviceUrl);
        assertThat(lookup.size(), is(1));

        zookeeperRegistry.register(serviceUrl);
        lookup = zookeeperRegistry.lookup(serviceUrl);
        assertThat(lookup.size(), is(1));
    }

    @Test
    void testLookupIllegalUrl() {
        try {
            zookeeperRegistry.lookup(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("lookup url == null"));
        }
    }

    @Test
    void testLookupWithException() {
        URL errorUrl = URL.valueOf("multicast://0.0.0.0/");
        when(mockZookeeperClient.getChildren(any())).thenThrow(new IllegalStateException());
        Assertions.assertThrows(RpcException.class, () -> zookeeperRegistry.lookup(errorUrl));
    }

    @Test
    void testSubscribeAnyValue() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        zookeeperRegistry.register(serviceUrl);
        zookeeperRegistry.subscribe(anyUrl, urls -> latch.countDown());
        doAnswer(invocationOnMock -> {
                    latch.countDown();
                    return null;
                })
                .when(mockZookeeperClient)
                .create(any(), anyBoolean(), anyBoolean());
        zookeeperRegistry.register(serviceUrl);
        latch.await();
    }

    @Test
    void testDestroy() {
        zookeeperRegistry.destroy();
        assertThat(zookeeperRegistry.isAvailable(), is(false));
    }

    @Test
    void testDoRegisterWithException() {
        doThrow(new IllegalStateException()).when(mockZookeeperClient).create(any(), anyBoolean(), anyBoolean());
        Assertions.assertThrows(RpcException.class, () -> {
            URL errorUrl = URL.valueOf("multicast://0.0.0.0/");
            zookeeperRegistry.doRegister(errorUrl);
        });
    }

    @Test
    void testDoUnregisterWithException() {
        doThrow(new IllegalStateException()).when(mockZookeeperClient).delete(any());
        Assertions.assertThrows(RpcException.class, () -> {
            URL errorUrl = URL.valueOf("multicast://0.0.0.0/");
            zookeeperRegistry.doUnregister(errorUrl);
        });
    }

    @Test
    void testDoSubscribeWithException() {
        Assertions.assertThrows(RpcException.class, () -> zookeeperRegistry.doSubscribe(anyUrl, listener));
    }
}
