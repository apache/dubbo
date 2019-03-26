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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.WatchedEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class CuratorZookeeperClientTest {
    private TestingServer zkServer;
    private CuratorZookeeperClient curatorClient;

    @Before
    public void setUp() throws Exception {
        int zkServerPort = NetUtils.getAvailablePort();
        zkServer = new TestingServer(zkServerPort, true);
        curatorClient = new CuratorZookeeperClient(URL.valueOf("zookeeper://127.0.0.1:" +
                zkServerPort + "/org.apache.dubbo.registry.RegistryService"));
    }

    @Test
    public void testCheckExists() {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false);
        assertThat(curatorClient.checkExists(path), is(true));
        assertThat(curatorClient.checkExists(path + "/noneexits"), is(false));
    }

    @Test
    public void testChildrenPath() {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false);
        curatorClient.create(path + "/provider1", false);
        curatorClient.create(path + "/provider2", false);

        List<String> children = curatorClient.getChildren(path);
        assertThat(children.size(), is(2));
    }

    @Test
    public void testChildrenListener() throws InterruptedException {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        curatorClient.addTargetChildListener(path, new CuratorWatcher() {
            @Override
            public void process(WatchedEvent watchedEvent) throws Exception {
                countDownLatch.countDown();
            }
        });
        curatorClient.createPersistent(path + "/provider1");
        countDownLatch.await();
    }


    @Test(expected = IllegalStateException.class)
    public void testWithInvalidServer() {
        curatorClient = new CuratorZookeeperClient(URL.valueOf("zookeeper://127.0.0.1:1/service"));
        curatorClient.create("/testPath", true);
    }

    @Test(expected = IllegalStateException.class)
    public void testWithStoppedServer() throws IOException {
        curatorClient.create("/testPath", true);
        zkServer.stop();
        curatorClient.delete("/testPath");
    }

    @Test
    public void testRemoveChildrenListener() {
        ChildListener childListener = mock(ChildListener.class);
        curatorClient.addChildListener("/children", childListener);
        curatorClient.removeChildListener("/children", childListener);
    }

    @Test
    public void testCreateExistingPath() {
        curatorClient.create("/pathOne", false);
        curatorClient.create("/pathOne", false);
    }

    @Test
    public void testConnectedStatus() {
        curatorClient.createEphemeral("/testPath");
        boolean connected = curatorClient.isConnected();
        assertThat(connected, is(true));
    }

    @Test
    public void testCreateContent4Persistent() {
        String path = "/curatorTest4CrContent/content.data";
        String content = "createContentTest";
        curatorClient.delete(path);
        assertThat(curatorClient.checkExists(path), is(false));
        assertNull(curatorClient.getContent(path));

        curatorClient.create(path, content, false);
        assertThat(curatorClient.checkExists(path), is(true));
        assertEquals(curatorClient.getContent(path), content);
    }

    @Test
    public void testCreateContent4Temp() {
        String path = "/curatorTest4CrContent/content.data";
        String content = "createContentTest";
        curatorClient.delete(path);
        assertThat(curatorClient.checkExists(path), is(false));
        assertNull(curatorClient.getContent(path));

        curatorClient.create(path, content, true);
        assertThat(curatorClient.checkExists(path), is(true));
        assertEquals(curatorClient.getContent(path), content);
    }

    @After
    public void tearDown() throws Exception {
        curatorClient.close();
        zkServer.stop();
    }
}
