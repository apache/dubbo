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
package org.apache.dubbo.remoting.zookeeper.curator;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.remoting.zookeeper.ChildListener;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

@DisabledForJreRange(min = JRE.JAVA_16)
class CuratorZookeeperClientTest {
    private CuratorZookeeperClient curatorClient;
    CuratorFramework client = null;

    private static int zookeeperServerPort1;
    private static String zookeeperConnectionAddress1;

    @BeforeAll
    public static void beforeAll() {
        zookeeperConnectionAddress1 = System.getProperty("zookeeper.connection.address.1");
        zookeeperServerPort1 = Integer.parseInt(zookeeperConnectionAddress1.substring(zookeeperConnectionAddress1.lastIndexOf(":") + 1));
    }

    @BeforeEach
    public void setUp() throws Exception {
        curatorClient = new CuratorZookeeperClient(URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService"));
        client = CuratorFrameworkFactory.newClient("127.0.0.1:" + zookeeperServerPort1, new ExponentialBackoffRetry(1000, 3));
        client.start();
    }

    @Test
    void testCheckExists() {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false, true);
        assertThat(curatorClient.checkExists(path), is(true));
        assertThat(curatorClient.checkExists(path + "/noneexits"), is(false));
    }

    @Test
    void testChildrenPath() {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false, true);
        curatorClient.create(path + "/provider1", false, true);
        curatorClient.create(path + "/provider2", false, true);

        List<String> children = curatorClient.getChildren(path);
        assertThat(children.size(), is(2));
    }

    @Test
    @Disabled("Global registry center")
    public void testChildrenListener() throws InterruptedException {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false, true);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        curatorClient.addTargetChildListener(path, new CuratorZookeeperClient.CuratorWatcherImpl() {

            @Override
            public void process(WatchedEvent watchedEvent) {
                countDownLatch.countDown();
            }
        });
        curatorClient.createPersistent(path + "/provider1", true);
        countDownLatch.await();
    }


    @Test
    void testWithInvalidServer() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            curatorClient = new CuratorZookeeperClient(URL.valueOf("zookeeper://127.0.0.1:1/service"));
            curatorClient.create("/testPath", true, true);
        });
    }

    @Test
    @Disabled("Global registry center cannot stop")
    public void testWithStoppedServer() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            curatorClient.create("/testPath", true, true);
            curatorClient.delete("/testPath");
        });
    }

    @Test
    void testRemoveChildrenListener() {
        ChildListener childListener = mock(ChildListener.class);
        curatorClient.addChildListener("/children", childListener);
        curatorClient.removeChildListener("/children", childListener);
    }

    @Test
    void testCreateExistingPath() {
        curatorClient.create("/pathOne", false, true);
        curatorClient.create("/pathOne", false, true);
    }

    @Test
    void testConnectedStatus() {
        curatorClient.createEphemeral("/testPath", true);
        boolean connected = curatorClient.isConnected();
        assertThat(connected, is(true));
    }

    @Test
    void testCreateContent4Persistent() {
        String path = "/curatorTest4CrContent/content.data";
        String content = "createContentTest";
        curatorClient.delete(path);
        assertThat(curatorClient.checkExists(path), is(false));
        assertNull(curatorClient.getContent(path));

        curatorClient.createOrUpdate(path, content, false);
        assertThat(curatorClient.checkExists(path), is(true));
        assertEquals(curatorClient.getContent(path), content);
    }

    @Test
    void testCreateContent4Temp() {
        String path = "/curatorTest4CrContent/content.data";
        String content = "createContentTest";
        curatorClient.delete(path);
        assertThat(curatorClient.checkExists(path), is(false));
        assertNull(curatorClient.getContent(path));

        curatorClient.createOrUpdate(path, content, true);
        assertThat(curatorClient.checkExists(path), is(true));
        assertEquals(curatorClient.getContent(path), content);
    }

    @Test
    void testCreatePersistentFailed() {
        String path = "/dubbo/test/path";
        curatorClient.delete(path);
        curatorClient.create(path, false, true);
        Assertions.assertTrue(curatorClient.checkExists(path));

        curatorClient.createPersistent(path, true);
        Assertions.assertTrue(curatorClient.checkExists(path));

        curatorClient.createPersistent(path, true);
        Assertions.assertTrue(curatorClient.checkExists(path));

        Assertions.assertThrows(IllegalStateException.class, () -> {
            curatorClient.createPersistent(path, false);
        });
        Assertions.assertTrue(curatorClient.checkExists(path));
    }

    @Test
    void testCreateEphemeralFailed() {
        String path = "/dubbo/test/path";
        curatorClient.delete(path);
        curatorClient.create(path, true, true);
        Assertions.assertTrue(curatorClient.checkExists(path));

        curatorClient.createEphemeral(path, true);
        Assertions.assertTrue(curatorClient.checkExists(path));

        curatorClient.createEphemeral(path, true);
        Assertions.assertTrue(curatorClient.checkExists(path));

        Assertions.assertThrows(IllegalStateException.class, () -> {
            curatorClient.createEphemeral(path, false);
        });
        Assertions.assertTrue(curatorClient.checkExists(path));
    }

    @AfterEach
    public void tearDown() throws Exception {
        curatorClient.close();
    }

    @Test
    void testAddTargetDataListener() throws Exception {
        String listenerPath = "/dubbo/service.name/configuration";
        String path = listenerPath + "/dat/data";
        String value = "vav";

        curatorClient.createOrUpdate(path + "/d.json", value, true);
        String valueFromCache = curatorClient.getContent(path + "/d.json");
        Assertions.assertEquals(value, valueFromCache);
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        curatorClient.addTargetDataListener(path + "/d.json", new CuratorZookeeperClient.NodeCacheListenerImpl() {

            @Override
            public void nodeChanged() {
                atomicInteger.incrementAndGet();
            }
        });

        valueFromCache = curatorClient.getContent(path + "/d.json");
        Assertions.assertNotNull(valueFromCache);

        int currentCount1 = atomicInteger.get();
        curatorClient.getClient().setData().forPath(path + "/d.json", "foo".getBytes());
        await().until(() -> atomicInteger.get() > currentCount1);
        int currentCount2 = atomicInteger.get();
        curatorClient.getClient().setData().forPath(path + "/d.json", "bar".getBytes());
        await().until(() -> atomicInteger.get() > currentCount2);
        int currentCount3 = atomicInteger.get();
        curatorClient.delete(path + "/d.json");
        valueFromCache = curatorClient.getContent(path + "/d.json");
        Assertions.assertNull(valueFromCache);
        await().until(() -> atomicInteger.get() > currentCount3);
    }


    @Test
    void testPersistentCas1() throws Exception {
        // test create failed when others create success
        String path = "/dubbo/mapping/org.apache.dubbo.demo.DemoService";
        AtomicReference<Runnable> runnable = new AtomicReference<>();
        CuratorZookeeperClient curatorClient = new CuratorZookeeperClient(URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService")) {
            @Override
            protected void createPersistent(String path, String data, boolean faultTolerant) {
                if (runnable.get() != null) {
                    runnable.get().run();
                }
                super.createPersistent(path, data, faultTolerant);
            }

            @Override
            protected void update(String path, String data, int version) {
                if (runnable.get() != null) {
                    runnable.get().run();
                }
                super.update(path, data, version);
            }
        };
        curatorClient.delete(path);

        runnable.set(() -> {
            try {
                client.create().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Assertions.assertThrows(IllegalStateException.class, () -> curatorClient.createOrUpdate(path, "version 1", false, 0));
        Assertions.assertEquals("version x", curatorClient.getContent(path));

        client.setData().forPath(path, "version 1".getBytes(StandardCharsets.UTF_8));

        ConfigItem configItem = curatorClient.getConfigItem(path);
        runnable.set(() -> {
            try {
                client.setData().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        int version1 = ((Stat) configItem.getTicket()).getVersion();
        Assertions.assertThrows(IllegalStateException.class, () -> curatorClient.createOrUpdate(path, "version 2", false, version1));
        Assertions.assertEquals("version x", curatorClient.getContent(path));

        runnable.set(null);
        configItem = curatorClient.getConfigItem(path);
        int version2 = ((Stat) configItem.getTicket()).getVersion();
        curatorClient.createOrUpdate(path, "version 2", false, version2);
        Assertions.assertEquals("version 2", curatorClient.getContent(path));

        curatorClient.close();
    }

    @Test
    void testPersistentCas2() {
        // test update failed when others create success
        String path = "/dubbo/mapping/org.apache.dubbo.demo.DemoService";
        CuratorZookeeperClient curatorClient = new CuratorZookeeperClient(URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService"));
        curatorClient.delete(path);

        curatorClient.createOrUpdate(path, "version x", false);
        Assertions.assertThrows(IllegalStateException.class, () -> curatorClient.createOrUpdate(path, "version 1", false, null));
        Assertions.assertEquals("version x", curatorClient.getContent(path));

        curatorClient.close();
    }

    @Test
    void testPersistentNonVersion() {
        String path = "/dubbo/metadata/org.apache.dubbo.demo.DemoService";
        AtomicReference<Runnable> runnable = new AtomicReference<>();
        CuratorZookeeperClient curatorClient = new CuratorZookeeperClient(URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService")) {
            @Override
            protected void createPersistent(String path, String data, boolean faultTolerant) {
                if (runnable.get() != null) {
                    runnable.get().run();
                }
                super.createPersistent(path, data, faultTolerant);
            }

            @Override
            protected void update(String path, String data, int version) {
                if (runnable.get() != null) {
                    runnable.get().run();
                }
                super.update(path, data, version);
            }
        };
        curatorClient.delete(path);

        curatorClient.createOrUpdate(path, "version 0", false);
        Assertions.assertEquals("version 0", curatorClient.getContent(path));
        curatorClient.delete(path);

        runnable.set(() -> {
            try {
                client.create().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        curatorClient.createOrUpdate(path, "version 1", false);
        Assertions.assertEquals("version 1", curatorClient.getContent(path));

        runnable.set(() -> {
            try {
                client.setData().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        curatorClient.createOrUpdate(path, "version 2", false);
        Assertions.assertEquals("version 2", curatorClient.getContent(path));

        runnable.set(null);
        curatorClient.createOrUpdate(path, "version 3", false);
        Assertions.assertEquals("version 3", curatorClient.getContent(path));

        curatorClient.close();
    }

    @Test
    void testEphemeralCas1() throws Exception {
        // test create failed when others create success
        String path = "/dubbo/mapping/org.apache.dubbo.demo.DemoService";
        AtomicReference<Runnable> runnable = new AtomicReference<>();
        CuratorZookeeperClient curatorClient = new CuratorZookeeperClient(URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService")) {
            @Override
            protected void createEphemeral(String path, String data, boolean faultTolerant) {
                if (runnable.get() != null) {
                    runnable.get().run();
                }
                super.createPersistent(path, data, faultTolerant);
            }

            @Override
            protected void update(String path, String data, int version) {
                if (runnable.get() != null) {
                    runnable.get().run();
                }
                super.update(path, data, version);
            }
        };
        curatorClient.delete(path);

        runnable.set(() -> {
            try {
                client.create().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Assertions.assertThrows(IllegalStateException.class, () -> curatorClient.createOrUpdate(path, "version 1", true, 0));
        Assertions.assertEquals("version x", curatorClient.getContent(path));

        client.setData().forPath(path, "version 1".getBytes(StandardCharsets.UTF_8));

        ConfigItem configItem = curatorClient.getConfigItem(path);
        runnable.set(() -> {
            try {
                client.setData().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        int version1 = ((Stat) configItem.getTicket()).getVersion();
        Assertions.assertThrows(IllegalStateException.class, () -> curatorClient.createOrUpdate(path, "version 2", true, version1));
        Assertions.assertEquals("version x", curatorClient.getContent(path));

        runnable.set(null);
        configItem = curatorClient.getConfigItem(path);
        int version2 = ((Stat) configItem.getTicket()).getVersion();
        curatorClient.createOrUpdate(path, "version 2", true, version2);
        Assertions.assertEquals("version 2", curatorClient.getContent(path));

        curatorClient.close();
    }

    @Test
    void testEphemeralCas2() {
        // test update failed when others create success
        String path = "/dubbo/mapping/org.apache.dubbo.demo.DemoService";
        CuratorZookeeperClient curatorClient = new CuratorZookeeperClient(URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService"));
        curatorClient.delete(path);

        curatorClient.createOrUpdate(path, "version x", true);
        Assertions.assertThrows(IllegalStateException.class, () -> curatorClient.createOrUpdate(path, "version 1", true, null));
        Assertions.assertEquals("version x", curatorClient.getContent(path));

        curatorClient.close();
    }

    @Test
    void testEphemeralNonVersion() {
        String path = "/dubbo/metadata/org.apache.dubbo.demo.DemoService";
        AtomicReference<Runnable> runnable = new AtomicReference<>();
        CuratorZookeeperClient curatorClient = new CuratorZookeeperClient(URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService")) {
            @Override
            protected void createPersistent(String path, String data, boolean faultTolerant) {
                if (runnable.get() != null) {
                    runnable.get().run();
                }
                super.createPersistent(path, data, faultTolerant);
            }

            @Override
            protected void update(String path, String data, int version) {
                if (runnable.get() != null) {
                    runnable.get().run();
                }
                super.update(path, data, version);
            }
        };
        curatorClient.delete(path);

        curatorClient.createOrUpdate(path, "version 0", true);
        Assertions.assertEquals("version 0", curatorClient.getContent(path));
        curatorClient.delete(path);

        runnable.set(() -> {
            try {
                client.create().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        curatorClient.createOrUpdate(path, "version 1", true);
        Assertions.assertEquals("version 1", curatorClient.getContent(path));

        runnable.set(() -> {
            try {
                client.setData().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        curatorClient.createOrUpdate(path, "version 2", true);
        Assertions.assertEquals("version 2", curatorClient.getContent(path));

        runnable.set(null);
        curatorClient.createOrUpdate(path, "version 3", true);
        Assertions.assertEquals("version 3", curatorClient.getContent(path));

        curatorClient.close();
    }

}
