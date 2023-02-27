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
package org.apache.dubbo.remoting.zookeeper.curator5;

import org.apache.dubbo.common.URL;
<<<<<<< HEAD
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.dubbo.remoting.zookeeper.curator5.Curator5ZookeeperClient;
=======
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
>>>>>>> origin/3.2

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
<<<<<<< HEAD
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.WatchedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

=======
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
>>>>>>> origin/3.2
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

<<<<<<< HEAD
public class Curator5ZookeeperClientTest {
    private TestingServer zkServer;
    private Curator5ZookeeperClient curatorClient;
    CuratorFramework client = null;

    @BeforeEach
    public void setUp() throws Exception {
        int zkServerPort = NetUtils.getAvailablePort();
        zkServer = new TestingServer(zkServerPort, true);
        curatorClient = new Curator5ZookeeperClient(URL.valueOf("zookeeper://127.0.0.1:" +
                zkServerPort + "/org.apache.dubbo.registry.RegistryService"));
        client = CuratorFrameworkFactory.newClient(zkServer.getConnectString(), new ExponentialBackoffRetry(1000, 3));
=======
class Curator5ZookeeperClientTest {
    private static Curator5ZookeeperClient curatorClient;
    private static CuratorFramework client = null;

    private static int zookeeperServerPort1;
    private static String zookeeperConnectionAddress1;

    @BeforeAll
    public static void setUp() throws Exception {
        zookeeperConnectionAddress1 = System.getProperty("zookeeper.connection.address.1");
        zookeeperServerPort1 = Integer.parseInt(zookeeperConnectionAddress1.substring(zookeeperConnectionAddress1.lastIndexOf(":") + 1));
        curatorClient = new Curator5ZookeeperClient(URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService"));
        client = CuratorFrameworkFactory.newClient("127.0.0.1:" + zookeeperServerPort1, new ExponentialBackoffRetry(1000, 3));
>>>>>>> origin/3.2
        client.start();
    }

    @Test
<<<<<<< HEAD
    public void testCheckExists() {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false);
=======
    void testCheckExists() {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false, true);
>>>>>>> origin/3.2
        assertThat(curatorClient.checkExists(path), is(true));
        assertThat(curatorClient.checkExists(path + "/noneexits"), is(false));
    }

    @Test
<<<<<<< HEAD
    public void testChildrenPath() {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false);
        curatorClient.create(path + "/provider1", false);
        curatorClient.create(path + "/provider2", false);
=======
    void testChildrenPath() {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false, true);
        curatorClient.create(path + "/provider1", false, true);
        curatorClient.create(path + "/provider2", false, true);
>>>>>>> origin/3.2

        List<String> children = curatorClient.getChildren(path);
        assertThat(children.size(), is(2));
    }

    @Test
<<<<<<< HEAD
    public void testChildrenListener() throws InterruptedException {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false);
=======
    @Timeout(value = 2)
    public void testChildrenListener() throws InterruptedException {
        String path = "/dubbo/org.apache.dubbo.demo.DemoListenerService/providers";
        curatorClient.create(path, false, true);
>>>>>>> origin/3.2
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        curatorClient.addTargetChildListener(path, new Curator5ZookeeperClient.CuratorWatcherImpl() {

            @Override
            public void process(WatchedEvent watchedEvent) throws Exception {
                countDownLatch.countDown();
            }
        });
<<<<<<< HEAD
        curatorClient.createPersistent(path + "/provider1");
=======
        curatorClient.createPersistent(path + "/provider1", true);
>>>>>>> origin/3.2
        countDownLatch.await();
    }


    @Test
<<<<<<< HEAD
    public void testWithInvalidServer() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            curatorClient = new Curator5ZookeeperClient(URL.valueOf("zookeeper://127.0.0.1:1/service"));
            curatorClient.create("/testPath", true);
        });
    }

    @Test
    public void testWithStoppedServer() throws IOException {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            curatorClient.create("/testPath", true);
            zkServer.stop();
            curatorClient.delete("/testPath");
=======
    void testWithInvalidServer() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            curatorClient = new Curator5ZookeeperClient(URL.valueOf("zookeeper://127.0.0.1:1/service?timeout=1000"));
            curatorClient.create("/testPath", true, true);
>>>>>>> origin/3.2
        });
    }

    @Test
<<<<<<< HEAD
    public void testRemoveChildrenListener() {
=======
    void testRemoveChildrenListener() {
>>>>>>> origin/3.2
        ChildListener childListener = mock(ChildListener.class);
        curatorClient.addChildListener("/children", childListener);
        curatorClient.removeChildListener("/children", childListener);
    }

    @Test
<<<<<<< HEAD
    public void testCreateExistingPath() {
        curatorClient.create("/pathOne", false);
        curatorClient.create("/pathOne", false);
    }

    @Test
    public void testConnectedStatus() {
        curatorClient.createEphemeral("/testPath");
=======
    void testCreateExistingPath() {
        curatorClient.create("/pathOne", false, true);
        curatorClient.create("/pathOne", false, true);
    }

    @Test
    void testConnectedStatus() {
        curatorClient.createEphemeral("/testPath", true);
>>>>>>> origin/3.2
        boolean connected = curatorClient.isConnected();
        assertThat(connected, is(true));
    }

    @Test
<<<<<<< HEAD
    public void testCreateContent4Persistent() {
=======
    void testCreateContent4Persistent() {
>>>>>>> origin/3.2
        String path = "/curatorTest4CrContent/content.data";
        String content = "createContentTest";
        curatorClient.delete(path);
        assertThat(curatorClient.checkExists(path), is(false));
        assertNull(curatorClient.getContent(path));

<<<<<<< HEAD
        curatorClient.create(path, content, false);
=======
        curatorClient.createOrUpdate(path, content, false);
>>>>>>> origin/3.2
        assertThat(curatorClient.checkExists(path), is(true));
        assertEquals(curatorClient.getContent(path), content);
    }

    @Test
<<<<<<< HEAD
    public void testCreateContent4Temp() {
=======
    void testCreateContent4Temp() {
>>>>>>> origin/3.2
        String path = "/curatorTest4CrContent/content.data";
        String content = "createContentTest";
        curatorClient.delete(path);
        assertThat(curatorClient.checkExists(path), is(false));
        assertNull(curatorClient.getContent(path));

<<<<<<< HEAD
        curatorClient.create(path, content, true);
=======
        curatorClient.createOrUpdate(path, content, true);
>>>>>>> origin/3.2
        assertThat(curatorClient.checkExists(path), is(true));
        assertEquals(curatorClient.getContent(path), content);
    }

<<<<<<< HEAD
    @AfterEach
    public void tearDown() throws Exception {
        curatorClient.close();
        zkServer.stop();
    }

    @Test
    public void testAddTargetDataListener() throws Exception {
=======
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

    @Test
    void testAddTargetDataListener() throws Exception {
>>>>>>> origin/3.2
        String listenerPath = "/dubbo/service.name/configuration";
        String path = listenerPath + "/dat/data";
        String value = "vav";

<<<<<<< HEAD
        curatorClient.create(path + "/d.json", value, true);
=======
        curatorClient.createOrUpdate(path + "/d.json", value, true);
>>>>>>> origin/3.2
        String valueFromCache = curatorClient.getContent(path + "/d.json");
        Assertions.assertEquals(value, valueFromCache);
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        curatorClient.addTargetDataListener(path + "/d.json", new Curator5ZookeeperClient.NodeCacheListenerImpl() {
<<<<<<< HEAD

=======
>>>>>>> origin/3.2
            @Override
            public void nodeChanged() throws Exception {
                atomicInteger.incrementAndGet();
            }
        });

        valueFromCache = curatorClient.getContent(path + "/d.json");
        Assertions.assertNotNull(valueFromCache);

<<<<<<< HEAD
        Thread.sleep(100);
        curatorClient.getClient().setData().forPath(path + "/d.json", "foo".getBytes());
        Thread.sleep(100);
        curatorClient.getClient().setData().forPath(path + "/d.json", "bar".getBytes());
        curatorClient.delete(path + "/d.json");
        valueFromCache = curatorClient.getContent(path + "/d.json");
        Assertions.assertNull(valueFromCache);
        Thread.sleep(2000L);
        Assertions.assertTrue(3L <= atomicInteger.get());
=======
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
        Curator5ZookeeperClient curatorClient = new Curator5ZookeeperClient(URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService")) {
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
    void testPersistentCas2() throws Exception {
        // test update failed when others create success
        String path = "/dubbo/mapping/org.apache.dubbo.demo.DemoService";
        Curator5ZookeeperClient curatorClient = new Curator5ZookeeperClient(URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService"));
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
        Curator5ZookeeperClient curatorClient = new Curator5ZookeeperClient(URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService")) {
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
        Curator5ZookeeperClient curatorClient = new Curator5ZookeeperClient(URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService")) {
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
    void testEphemeralCas2() throws Exception {
        // test update failed when others create success
        String path = "/dubbo/mapping/org.apache.dubbo.demo.DemoService";
        Curator5ZookeeperClient curatorClient = new Curator5ZookeeperClient(URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService"));
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
        Curator5ZookeeperClient curatorClient = new Curator5ZookeeperClient(URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService")) {
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

    @AfterAll
    public static void testWithStoppedServer() {
        curatorClient.close();
>>>>>>> origin/3.2
    }
}
