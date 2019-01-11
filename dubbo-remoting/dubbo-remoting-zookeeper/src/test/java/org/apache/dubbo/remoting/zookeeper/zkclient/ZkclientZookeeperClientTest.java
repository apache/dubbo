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
package org.apache.dubbo.remoting.zookeeper.zkclient;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.zookeeper.StateListener;
import org.I0Itec.zkclient.IZkChildListener;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

public class ZkclientZookeeperClientTest {
    private TestingServer zkServer;
    private ZkclientZookeeperClient zkclientZookeeperClient;

    @BeforeEach
    public void setUp() throws Exception {
        int zkServerPort = NetUtils.getAvailablePort();
        zkServer = new TestingServer(zkServerPort, true);
        zkclientZookeeperClient = new ZkclientZookeeperClient(URL.valueOf("zookeeper://127.0.0.1:" +
                zkServerPort + "/org.apache.dubbo.registry.RegistryService"));
    }

    @Test
    public void testCheckExists() {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        zkclientZookeeperClient.create(path, false);
        assertThat(zkclientZookeeperClient.checkExists(path), is(true));
        assertThat(zkclientZookeeperClient.checkExists(path + "/noneexits"), is(false));
    }

    @Test
    public void testDeletePath() {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        zkclientZookeeperClient.create(path, false);
        assertThat(zkclientZookeeperClient.checkExists(path), is(true));

        zkclientZookeeperClient.delete(path);
        assertThat(zkclientZookeeperClient.checkExists(path), is(false));
    }

    @Test
    public void testConnectState() throws Exception {
        assertThat(zkclientZookeeperClient.isConnected(), is(true));
        final CountDownLatch stopLatch = new CountDownLatch(1);
        zkclientZookeeperClient.addStateListener(new StateListener() {
            @Override
            public void stateChanged(int connected) {
                stopLatch.countDown();
            }
        });
        zkServer.stop();
        stopLatch.await();
        assertThat(zkclientZookeeperClient.isConnected(), is(false));
    }

    @Test
    public void testChildrenListener() throws InterruptedException {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/providers";
        zkclientZookeeperClient.create(path, false);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        zkclientZookeeperClient.addTargetChildListener(path, new IZkChildListener() {
            @Override
            public void handleChildChange(String s, List<String> list) throws Exception {
                countDownLatch.countDown();
            }
        });
        zkclientZookeeperClient.createPersistent(path + "/provider1");
        countDownLatch.await();
    }

    @Test
    public void testGetChildren() throws IOException {
        String path = "/dubbo/org.apache.dubbo.demo.DemoService/parentProviders";
        zkclientZookeeperClient.create(path, false);
        for (int i = 0; i < 5; i++) {
            zkclientZookeeperClient.createEphemeral(path + "/server" + i);
        }
        List<String> zookeeperClientChildren = zkclientZookeeperClient.getChildren(path);
        assertThat(zookeeperClientChildren, hasSize(5));
    }

    @Test
    public void testCreateContentPersistent() {
        String path = "/ZkclientZookeeperClient/content.data";
        String content = "createContentTest";
        zkclientZookeeperClient.delete(path);
        assertThat(zkclientZookeeperClient.checkExists(path), is(false));
        assertNull(zkclientZookeeperClient.getContent(path));

        zkclientZookeeperClient.create(path, content, false);
        assertThat(zkclientZookeeperClient.checkExists(path), is(true));
        assertEquals(zkclientZookeeperClient.getContent(path), content);
    }

    @Test
    public void testCreateContentTem() {
        String path = "/ZkclientZookeeperClient/content.data";
        String content = "createContentTest";
        zkclientZookeeperClient.delete(path);
        assertThat(zkclientZookeeperClient.checkExists(path), is(false));
        assertNull(zkclientZookeeperClient.getContent(path));

        zkclientZookeeperClient.create(path, content, true);
        assertThat(zkclientZookeeperClient.checkExists(path), is(true));
        assertEquals(zkclientZookeeperClient.getContent(path), content);
    }

    @AfterEach
    public void tearDown() throws Exception {
        zkclientZookeeperClient.close();
        zkServer.stop();
    }
}
