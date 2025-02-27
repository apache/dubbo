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
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.remoting.zookeeper.curator5.Curator5ZookeeperClient.CuratorWatcherImpl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Lists;
import org.apache.curator.CuratorZookeeperClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.WatcherRemoveCuratorFramework;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.api.DeleteBuilder;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.framework.api.Pathable;
import org.apache.curator.framework.api.SetDataBuilder;
import org.apache.curator.framework.api.WatchPathable;
import org.apache.curator.framework.listen.StandardListenerManager;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstructionWithAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class Curator5ZookeeperClientTest {
    private static Curator5ZookeeperClient curatorClient;

    private static int zookeeperServerMockPort1;
    private static String zookeeperConnectionAddress1;

    private static MockedStatic<CuratorFrameworkFactory> curatorFrameworkFactoryMockedStatic;

    CuratorFrameworkFactory.Builder spyBuilder = CuratorFrameworkFactory.builder();

    private CuratorFramework mockCuratorFramework;
    private CreateBuilder mockCreateBuilder;
    private ExistsBuilder mockExistsBuilder;
    private GetChildrenBuilder mockGetChildrenBuilder;
    private DeleteBuilder mockDeleteBuilder;
    private GetDataBuilder mockGetDataBuilder;
    private SetDataBuilder mockSetDataBuilder;
    private CuratorZookeeperClient mockCuratorZookeeperClient;
    private WatcherRemoveCuratorFramework mockWatcherRemoveCuratorFramework;
    private Answer<String> createAnswer;

    @BeforeAll
    public static void setUp() throws Exception {
        zookeeperServerMockPort1 = 2181;
        zookeeperConnectionAddress1 = "zookeeper://localhost:" + zookeeperServerMockPort1;

        // mock begin
        // create mock bean begin
        CuratorFrameworkFactory.Builder realBuilder = CuratorFrameworkFactory.builder();
        CuratorFrameworkFactory.Builder spyBuilder = spy(realBuilder);

        curatorFrameworkFactoryMockedStatic = mockStatic(CuratorFrameworkFactory.class);
        curatorFrameworkFactoryMockedStatic
                .when(CuratorFrameworkFactory::builder)
                .thenReturn(spyBuilder);
    }

    @BeforeEach
    public void init() throws Exception {
        mockCreateBuilder = mock(CreateBuilder.class);
        mockExistsBuilder = mock(ExistsBuilder.class);
        mockDeleteBuilder = mock(DeleteBuilder.class);
        mockCuratorFramework = mock(CuratorFramework.class);
        mockGetChildrenBuilder = mock(GetChildrenBuilder.class);
        mockGetDataBuilder = mock(GetDataBuilder.class);
        mockCuratorZookeeperClient = mock(CuratorZookeeperClient.class);
        mockWatcherRemoveCuratorFramework = mock(WatcherRemoveCuratorFramework.class);
        mockSetDataBuilder = mock(SetDataBuilder.class);
        doReturn(mockCuratorFramework).when(spyBuilder).build();
        when(mockCuratorFramework.blockUntilConnected(anyInt(), any())).thenReturn(true);
        when(mockCuratorFramework.getConnectionStateListenable()).thenReturn(StandardListenerManager.standard());
        when(mockCuratorFramework.create()).thenReturn(mockCreateBuilder);
        when(mockCuratorFramework.checkExists()).thenReturn(mockExistsBuilder);
        when(mockCuratorFramework.getChildren()).thenReturn(mockGetChildrenBuilder);
        when(mockCuratorFramework.getZookeeperClient()).thenReturn(mockCuratorZookeeperClient);
        when(mockCuratorFramework.newWatcherRemoveCuratorFramework()).thenReturn(mockWatcherRemoveCuratorFramework);
        when(mockCuratorZookeeperClient.isConnected()).thenReturn(true);
        when(mockCuratorFramework.delete()).thenReturn(mockDeleteBuilder);
        when(mockCreateBuilder.withMode(any())).thenReturn(mockCreateBuilder);
        when(mockDeleteBuilder.deletingChildrenIfNeeded()).thenReturn(mockDeleteBuilder);
        when(mockDeleteBuilder.forPath(any())).then((Answer<Void>) invocationOnMock -> null);
        when(mockCuratorFramework.getData()).thenReturn(mockGetDataBuilder);
        when(mockCuratorFramework.setData()).thenReturn(mockSetDataBuilder);
        when(mockSetDataBuilder.withVersion(anyInt())).thenReturn(mockSetDataBuilder);
        List<String> paths = new ArrayList<>();
        createAnswer = invocationOnMock -> {
            String param = invocationOnMock.getArgument(0);
            if (paths.contains(param)) {
                throw new NodeExistsException("node existed: " + param);
            }
            paths.add(invocationOnMock.getArgument(0));
            return invocationOnMock.getArgument(0);
        };
        when(mockCreateBuilder.forPath(anyString())).thenAnswer(createAnswer);
        when(mockCreateBuilder.forPath(anyString(), any())).thenAnswer(createAnswer);
        when(mockExistsBuilder.forPath(anyString())).thenAnswer(i -> {
            if (paths.contains(i.getArgument(0))) {
                return new Stat();
            }
            return null;
        });
        when(mockDeleteBuilder.forPath(anyString())).thenAnswer(i -> {
            if (paths.contains(i.getArgument(0))) {
                paths.remove(i.getArgument(0));
            }
            return null;
        });

        curatorClient = new Curator5ZookeeperClient(
                URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService"));
    }

    @Test
    void testCheckExists() {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false, true);
        assertThat(curatorClient.checkExists(path), is(true));
        assertThat(curatorClient.checkExists(path + "/noneexits"), is(false));
    }

    @Test
    void testChildrenPath() throws Exception {
        when(mockGetChildrenBuilder.forPath(any())).thenReturn(Lists.newArrayList("provider1", "provider2"));
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false, true);
        curatorClient.create(path + "/provider1", false, true);
        curatorClient.create(path + "/provider2", false, true);

        List<String> children = curatorClient.getChildren(path);
        assertThat(children.size(), is(2));
    }

    @Test
    @Timeout(value = 2)
    public void testChildrenListener() throws Exception {
        String path = "/dubbo/org.apache.dubbo.demo.DemoListenerService/providers";
        curatorClient.create(path, false, true);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        when(mockGetChildrenBuilder.usingWatcher(any(CuratorWatcher.class))).thenReturn(mockGetChildrenBuilder);
        when(mockGetChildrenBuilder.forPath(any())).thenReturn(Lists.newArrayList("providers"));
        CuratorWatcherImpl watcher = new CuratorWatcherImpl() {

            @Override
            public void process(WatchedEvent watchedEvent) {
                countDownLatch.countDown();
            }
        };
        curatorClient.addTargetChildListener(path, watcher);
        watcher.process(new WatchedEvent(Event.EventType.NodeDeleted, KeeperState.Closed, "providers"));
        curatorClient.createPersistent(path + "/provider1", true);
        countDownLatch.await();
    }

    @Test
    void testWithInvalidServer() throws InterruptedException {
        when(mockCuratorFramework.blockUntilConnected(anyInt(), any())).thenReturn(false);
        Assertions.assertThrows(IllegalStateException.class, () -> {
            curatorClient = new Curator5ZookeeperClient(URL.valueOf("zookeeper://127.0.0.1:1/service?timeout=1000"));
            curatorClient.create("/testPath", true, true);
        });
    }

    @Test
    void testRemoveChildrenListener() throws Exception {
        ChildListener childListener = mock(ChildListener.class);
        when(mockGetChildrenBuilder.usingWatcher(any(CuratorWatcher.class))).thenReturn(mockGetChildrenBuilder);
        when(mockGetChildrenBuilder.forPath(any())).thenReturn(Lists.newArrayList("children"));
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
    void testCreateContent4Persistent() throws Exception {
        String path = "/curatorTest4CrContent/content.data";
        String content = "createContentTest";
        curatorClient.delete(path);
        assertThat(curatorClient.checkExists(path), is(false));
        assertNull(curatorClient.getContent(path));

        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> content.getBytes());
        curatorClient.createOrUpdate(path, content, false);
        assertThat(curatorClient.checkExists(path), is(true));
        assertEquals(curatorClient.getContent(path), content);
    }

    @Test
    void testCreateContent4Temp() throws Exception {
        String path = "/curatorTest4CrContent/content.data";
        String content = "createContentTest";
        curatorClient.delete(path);
        assertThat(curatorClient.checkExists(path), is(false));
        assertNull(curatorClient.getContent(path));

        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> content.getBytes());
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

    @Test
    void testAddTargetDataListener() throws Exception {
        String listenerPath = "/dubbo/service.name/configuration";
        String path = listenerPath + "/dat/data";
        String value = "vav";

        curatorClient.createOrUpdate(path + "/d.json", value, true);
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> value.getBytes());
        String valueFromCache = curatorClient.getContent(path + "/d.json");
        Assertions.assertEquals(value, valueFromCache);
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        NodeCache mockNodeCache = mock(NodeCache.class);
        MockedConstruction<NodeCache> mockedConstruction =
                mockConstructionWithAnswer(NodeCache.class, invocationOnMock -> invocationOnMock
                        .getMethod()
                        .invoke(mockNodeCache, invocationOnMock.getArguments()));
        when(mockNodeCache.getListenable()).thenReturn(StandardListenerManager.standard());
        Curator5ZookeeperClient.NodeCacheListenerImpl nodeCacheListener =
                new Curator5ZookeeperClient.NodeCacheListenerImpl() {
                    @Override
                    public void nodeChanged() {
                        atomicInteger.incrementAndGet();
                    }
                };
        curatorClient.addTargetDataListener(path + "/d.json", nodeCacheListener);

        valueFromCache = curatorClient.getContent(path + "/d.json");
        Assertions.assertNotNull(valueFromCache);

        int currentCount1 = atomicInteger.get();
        when(mockSetDataBuilder.forPath(any(), any())).then(invocationOnMock -> {
            nodeCacheListener.nodeChanged();
            return null;
        });
        curatorClient.getClient().setData().forPath(path + "/d.json", "foo".getBytes());
        await().until(() -> atomicInteger.get() > currentCount1);
        int currentCount2 = atomicInteger.get();
        curatorClient.getClient().setData().forPath(path + "/d.json", "bar".getBytes());
        await().until(() -> atomicInteger.get() > currentCount2);
        int currentCount3 = atomicInteger.get();
        when(mockDeleteBuilder.forPath(any())).then(invocationOnMock -> {
            nodeCacheListener.nodeChanged();
            return null;
        });
        curatorClient.delete(path + "/d.json");
        when(mockGetDataBuilder.forPath(any())).thenReturn(null);
        valueFromCache = curatorClient.getContent(path + "/d.json");
        Assertions.assertNull(valueFromCache);
        await().until(() -> atomicInteger.get() > currentCount3);
        mockedConstruction.close();
    }

    @Test
    void testPersistentCas1() throws Exception {
        // test create failed when others create success
        String path = "/dubbo/mapping/org.apache.dubbo.demo.DemoService";
        AtomicReference<Runnable> runnable = new AtomicReference<>();
        Curator5ZookeeperClient curatorClient =
                new Curator5ZookeeperClient(
                        URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService")) {
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
                mockCuratorFramework.create().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version x".getBytes());
        when(mockCreateBuilder.forPath(any())).then(invocationOnMock -> {
            String value;
            try {
                value = createAnswer.answer(invocationOnMock);
            } catch (Exception e) {
                throw e;
            }
            try {
                runnable.get().run();
            } catch (Exception ignored) {

            }
            return value;
        });

        Assertions.assertThrows(
                IllegalStateException.class, () -> curatorClient.createOrUpdate(path, "version 1", false, 0));
        Assertions.assertEquals("version x", curatorClient.getContent(path));
        mockCuratorFramework.setData().forPath(path, "version 1".getBytes(StandardCharsets.UTF_8));
        when(mockGetDataBuilder.storingStatIn(any())).thenReturn(new WatchPathable<byte[]>() {
            @Override
            public byte[] forPath(String s) throws Exception {
                return mockGetDataBuilder.forPath(s);
            }

            @Override
            public Pathable<byte[]> watched() {
                return null;
            }

            @Override
            public Pathable<byte[]> usingWatcher(Watcher watcher) {
                return null;
            }

            @Override
            public Pathable<byte[]> usingWatcher(CuratorWatcher curatorWatcher) {
                return null;
            }
        });
        ConfigItem configItem = curatorClient.getConfigItem(path);
        runnable.set(() -> {
            try {
                mockCuratorFramework.setData().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        when(mockSetDataBuilder.forPath(any(), any())).thenThrow(new IllegalStateException());
        int version1 = ((Stat) configItem.getTicket()).getVersion();
        Assertions.assertThrows(
                IllegalStateException.class, () -> curatorClient.createOrUpdate(path, "version 2", false, version1));
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version x".getBytes());
        Assertions.assertEquals("version x", curatorClient.getContent(path));

        runnable.set(null);
        configItem = curatorClient.getConfigItem(path);
        int version2 = ((Stat) configItem.getTicket()).getVersion();
        doReturn(null).when(mockSetDataBuilder).forPath(any(), any());
        curatorClient.createOrUpdate(path, "version 2", false, version2);
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version 2".getBytes());
        Assertions.assertEquals("version 2", curatorClient.getContent(path));

        curatorClient.close();
    }

    @Test
    void testPersistentCas2() throws Exception {
        // test update failed when others create success
        String path = "/dubbo/mapping/org.apache.dubbo.demo.DemoService";
        Curator5ZookeeperClient curatorClient = new Curator5ZookeeperClient(
                URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService"));
        curatorClient.delete(path);

        curatorClient.createOrUpdate(path, "version x", false);
        Assertions.assertThrows(
                IllegalStateException.class, () -> curatorClient.createOrUpdate(path, "version 1", false, null));
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version x".getBytes());
        Assertions.assertEquals("version x", curatorClient.getContent(path));

        curatorClient.close();
    }

    @Test
    void testPersistentNonVersion() throws Exception {
        String path = "/dubbo/metadata/org.apache.dubbo.demo.DemoService";
        AtomicReference<Runnable> runnable = new AtomicReference<>();
        Curator5ZookeeperClient curatorClient =
                new Curator5ZookeeperClient(
                        URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService")) {
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

        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version x".getBytes());
        when(mockCreateBuilder.forPath(any())).then(invocationOnMock -> {
            String value;
            try {
                value = createAnswer.answer(invocationOnMock);
            } catch (Exception e) {
                throw e;
            }
            try {
                runnable.get().run();
            } catch (Exception ignored) {

            }
            return value;
        });
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version 0".getBytes());
        curatorClient.createOrUpdate(path, "version 0", false);
        Assertions.assertEquals("version 0", curatorClient.getContent(path));
        curatorClient.delete(path);

        runnable.set(() -> {
            try {
                mockCuratorFramework.create().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version 1".getBytes());
        curatorClient.createOrUpdate(path, "version 1", false);
        Assertions.assertEquals("version 1", curatorClient.getContent(path));

        runnable.set(() -> {
            try {
                mockCuratorFramework.setData().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        curatorClient.createOrUpdate(path, "version 2", false);
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version 2".getBytes());
        Assertions.assertEquals("version 2", curatorClient.getContent(path));

        runnable.set(null);
        curatorClient.createOrUpdate(path, "version 3", false);
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version 3".getBytes());
        Assertions.assertEquals("version 3", curatorClient.getContent(path));

        curatorClient.close();
    }

    @Test
    void testEphemeralCas1() throws Exception {
        // test create failed when others create success
        String path = "/dubbo/mapping/org.apache.dubbo.demo.DemoService";
        AtomicReference<Runnable> runnable = new AtomicReference<>();
        Curator5ZookeeperClient curatorClient =
                new Curator5ZookeeperClient(
                        URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService")) {
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
        when(mockCreateBuilder.forPath(any())).then(invocationOnMock -> {
            String value;
            try {
                value = createAnswer.answer(invocationOnMock);
            } catch (Exception e) {
                throw e;
            }
            try {
                runnable.get().run();
            } catch (Exception ignored) {

            }
            return value;
        });
        runnable.set(() -> {
            try {
                mockCuratorFramework.create().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Assertions.assertThrows(
                IllegalStateException.class, () -> curatorClient.createOrUpdate(path, "version 1", true, 0));
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version x".getBytes());
        Assertions.assertEquals("version x", curatorClient.getContent(path));

        mockCuratorFramework.setData().forPath(path, "version 1".getBytes(StandardCharsets.UTF_8));
        when(mockGetDataBuilder.storingStatIn(any())).thenReturn(new WatchPathable<byte[]>() {
            @Override
            public byte[] forPath(String s) throws Exception {
                return mockGetDataBuilder.forPath(s);
            }

            @Override
            public Pathable<byte[]> watched() {
                return null;
            }

            @Override
            public Pathable<byte[]> usingWatcher(Watcher watcher) {
                return null;
            }

            @Override
            public Pathable<byte[]> usingWatcher(CuratorWatcher curatorWatcher) {
                return null;
            }
        });
        ConfigItem configItem = curatorClient.getConfigItem(path);
        runnable.set(() -> {
            try {
                mockCuratorFramework.setData().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        int version1 = ((Stat) configItem.getTicket()).getVersion();
        when(mockSetDataBuilder.forPath(any(), any())).thenThrow(new IllegalStateException());
        Assertions.assertThrows(
                IllegalStateException.class, () -> curatorClient.createOrUpdate(path, "version 2", true, version1));
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version x".getBytes());
        Assertions.assertEquals("version x", curatorClient.getContent(path));

        runnable.set(null);
        configItem = curatorClient.getConfigItem(path);
        int version2 = ((Stat) configItem.getTicket()).getVersion();
        doReturn(null).when(mockSetDataBuilder).forPath(any(), any());
        curatorClient.createOrUpdate(path, "version 2", true, version2);
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version 2".getBytes());
        Assertions.assertEquals("version 2", curatorClient.getContent(path));

        curatorClient.close();
    }

    @Test
    void testEphemeralCas2() throws Exception {
        // test update failed when others create success
        String path = "/dubbo/mapping/org.apache.dubbo.demo.DemoService";
        Curator5ZookeeperClient curatorClient = new Curator5ZookeeperClient(
                URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService"));
        curatorClient.delete(path);

        curatorClient.createOrUpdate(path, "version x", true);
        Assertions.assertThrows(
                IllegalStateException.class, () -> curatorClient.createOrUpdate(path, "version 1", true, null));
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version x".getBytes());
        Assertions.assertEquals("version x", curatorClient.getContent(path));

        curatorClient.close();
    }

    @Test
    void testEphemeralNonVersion() throws Exception {
        String path = "/dubbo/metadata/org.apache.dubbo.demo.DemoService";
        AtomicReference<Runnable> runnable = new AtomicReference<>();
        Curator5ZookeeperClient curatorClient =
                new Curator5ZookeeperClient(
                        URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService")) {
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
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version 0".getBytes());
        Assertions.assertEquals("version 0", curatorClient.getContent(path));
        curatorClient.delete(path);

        runnable.set(() -> {
            try {
                mockCuratorFramework.create().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        curatorClient.createOrUpdate(path, "version 1", true);
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version 1".getBytes());
        Assertions.assertEquals("version 1", curatorClient.getContent(path));

        runnable.set(() -> {
            try {
                mockCuratorFramework.setData().forPath(path, "version x".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        curatorClient.createOrUpdate(path, "version 2", true);
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version 2".getBytes());
        Assertions.assertEquals("version 2", curatorClient.getContent(path));

        runnable.set(null);
        curatorClient.createOrUpdate(path, "version 3", true);
        when(mockGetDataBuilder.forPath(any())).then(invocationOnMock -> "version 3".getBytes());
        Assertions.assertEquals("version 3", curatorClient.getContent(path));

        curatorClient.close();
    }

    @AfterAll
    public static void testWithStoppedServer() {
        curatorFrameworkFactoryMockedStatic.close();
        curatorClient.close();
    }
}
