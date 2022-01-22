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
import org.apache.dubbo.common.status.Status;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.status.RegistryStatusChecker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class ZookeeperRegistryTest {
    private static String zookeeperConnectionAddress1;
    private ZookeeperRegistry zookeeperRegistry;
    private String service = "org.apache.dubbo.test.injvmServie";
    private URL serviceUrl = URL.valueOf("zookeeper://zookeeper/" + service + "?notify=false&methods=test1,test2");
    private URL anyUrl = URL.valueOf("zookeeper://zookeeper/*");
    private URL registryUrl;
    private ZookeeperRegistryFactory zookeeperRegistryFactory;
    private NotifyListener listener;

    @BeforeAll
    public static void beforeAll() {
        zookeeperConnectionAddress1 = System.getProperty("zookeeper.connection.address.1");
    }

    @BeforeEach
    public void setUp() throws Exception {
        this.registryUrl = URL.valueOf(zookeeperConnectionAddress1);
        zookeeperRegistryFactory = new ZookeeperRegistryFactory(ApplicationModel.defaultModel());
        this.zookeeperRegistry = (ZookeeperRegistry) zookeeperRegistryFactory.createRegistry(registryUrl);
    }

    @Test
    public void testAnyHost() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            URL errorUrl = URL.valueOf("multicast://0.0.0.0/");
            new ZookeeperRegistryFactory(ApplicationModel.defaultModel()).createRegistry(errorUrl);
        });
    }

    @Test
    public void testRegister() {
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
    public void testSubscribe() {
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
    public void testAvailable() {
        zookeeperRegistry.register(serviceUrl);
        assertThat(zookeeperRegistry.isAvailable(), is(true));

        zookeeperRegistry.destroy();
        assertThat(zookeeperRegistry.isAvailable(), is(false));
    }

    @Test
    public void testLookup() {
        List<URL> lookup = zookeeperRegistry.lookup(serviceUrl);
        assertThat(lookup.size(), is(1));

        zookeeperRegistry.register(serviceUrl);
        lookup = zookeeperRegistry.lookup(serviceUrl);
        assertThat(lookup.size(), is(1));
    }

    @Test
    public void testLookupIllegalUrl() {
        try {
            zookeeperRegistry.lookup(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                containsString("lookup url == null"));
        }
    }

    @Test
    public void testLookupWithException() {
        URL errorUrl = URL.valueOf("multicast://0.0.0.0/");
        Assertions.assertThrows(RpcException.class, () -> zookeeperRegistry.lookup(errorUrl));
    }

    @Disabled
    @Test
    /*
      This UT is unstable, consider remove it later.
      @see https://github.com/apache/dubbo/issues/1787
     */
    public void testStatusChecker() {
        RegistryStatusChecker registryStatusChecker = new RegistryStatusChecker(ApplicationModel.defaultModel());
        Status status = registryStatusChecker.check();
        assertThat(status.getLevel(), is(Status.Level.UNKNOWN));

        Registry registry = zookeeperRegistryFactory.getRegistry(registryUrl);
        assertThat(registry, not(nullValue()));

        status = registryStatusChecker.check();
        assertThat(status.getLevel(), is(Status.Level.ERROR));

        registry.register(serviceUrl);
        status = registryStatusChecker.check();
        assertThat(status.getLevel(), is(Status.Level.OK));
    }

    @Test
    public void testSubscribeAnyValue() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        zookeeperRegistry.register(serviceUrl);
        zookeeperRegistry.subscribe(anyUrl, urls -> latch.countDown());
        zookeeperRegistry.register(serviceUrl);
        latch.await();
    }

    @Test
    public void testDestroy() {
        zookeeperRegistry.destroy();
        assertThat(zookeeperRegistry.isAvailable(), is(false));
    }


    @Test
    public void testDoRegisterWithException() {
        Assertions.assertThrows(RpcException.class, () -> {
            URL errorUrl = URL.valueOf("multicast://0.0.0.0/");
            zookeeperRegistry.doRegister(errorUrl);
        });
    }

    @Test
    public void testDoUnregisterWithException() {
        Assertions.assertThrows(RpcException.class, () -> {
            URL errorUrl = URL.valueOf("multicast://0.0.0.0/");
            zookeeperRegistry.doUnregister(errorUrl);
        });
    }

    @Test
    public void testDoSubscribeWithException() {
        Assertions.assertThrows(RpcException.class,
            () -> zookeeperRegistry.doSubscribe(anyUrl, listener));
    }
}
