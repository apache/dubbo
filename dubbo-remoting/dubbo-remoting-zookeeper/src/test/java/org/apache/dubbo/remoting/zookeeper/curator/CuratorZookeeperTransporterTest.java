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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

public class CuratorZookeeperTransporterTest {
    private TestingServer zkServer;
    private ZookeeperClient zookeeperClient;
    private CuratorZookeeperTransporter curatorZookeeperTransporter;
    private int zkServerPort;

    @Before
    public void setUp() throws Exception {
        zkServerPort = NetUtils.getAvailablePort();
        zkServer = new TestingServer(zkServerPort, true);
        zookeeperClient = new CuratorZookeeperTransporter().connect(URL.valueOf("zookeeper://127.0.0.1:" +
                zkServerPort + "/service"));
        curatorZookeeperTransporter = new CuratorZookeeperTransporter();
    }


    @After
    public void tearDown() throws Exception {
        zkServer.stop();
    }

    @Test
    public void testZookeeperClient() {
        assertThat(zookeeperClient, not(nullValue()));
        zookeeperClient.close();
    }

    @Test
    public void testCreateServerURL() {
        URL url = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort + "/org.apache.dubbo.registry.RegistryService?application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828&timeout=2300");
        URL newUrl = curatorZookeeperTransporter.createServerURL(url);
        Assert.assertEquals(newUrl.getProtocol(), "zookeeper");
        Assert.assertEquals(newUrl.getHost(), "127.0.0.1");
        Assert.assertEquals(newUrl.getPort(), zkServerPort);
        Assert.assertNull(newUrl.getUsername());
        Assert.assertNull(newUrl.getPassword());
        Assert.assertEquals(newUrl.getParameter(Constants.TIMEOUT_KEY, 5000), 2300);
        Assert.assertEquals(newUrl.getParameters().size(), 1);
        Assert.assertEquals(newUrl.getPath(), CuratorZookeeperTransporter.class.getName());
    }

    @Test
    public void testCreateServerURLWhenHasUser() {
        URL url = URL.valueOf("zookeeper://us2:pw2@127.0.0.1:" + zkServerPort + "/org.apache.dubbo.registry.RegistryService?application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL newUrl = curatorZookeeperTransporter.createServerURL(url);
        Assert.assertEquals(newUrl.getProtocol(), "zookeeper");
        Assert.assertEquals(newUrl.getHost(), "127.0.0.1");
        Assert.assertEquals(newUrl.getPort(), zkServerPort);
        Assert.assertEquals(newUrl.getUsername(), "us2");
        Assert.assertEquals(newUrl.getPassword(), "pw2");
        Assert.assertEquals(newUrl.getParameters().size(), 0);
        Assert.assertEquals(newUrl.getPath(), CuratorZookeeperTransporter.class.getName());
    }

    @Test
    public void testRepeatConnect() {
        URL url = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort + "/org.apache.dubbo.registry.RegistryService?application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL url2 = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort + "/org.apache.dubbo.metadata.store.MetadataReport?address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        ZookeeperClient newZookeeperClient = curatorZookeeperTransporter.connect(url);
        Assert.assertEquals(curatorZookeeperTransporter.zookeeperClientMap.size(), 1);
        Assert.assertEquals(curatorZookeeperTransporter.zookeeperClientMap.get(url.toServerIdentityString()).zookeeperClient, newZookeeperClient);
        Assert.assertTrue(curatorZookeeperTransporter.zookeeperClientMap.get(url.toServerIdentityString()).originalURLs.contains(url));
        Assert.assertTrue(curatorZookeeperTransporter.zookeeperClientMap.get(url.toServerIdentityString()).originalURLs.size() == 1);

        ZookeeperClient newZookeeperClient2 = curatorZookeeperTransporter.connect(url2);
        Assert.assertEquals(newZookeeperClient, newZookeeperClient2);
        Assert.assertEquals(curatorZookeeperTransporter.zookeeperClientMap.size(), 1);
        Assert.assertEquals(curatorZookeeperTransporter.zookeeperClientMap.get(url2.toServerIdentityString()).zookeeperClient, newZookeeperClient);
        Assert.assertTrue(curatorZookeeperTransporter.zookeeperClientMap.get(url2.toServerIdentityString()).originalURLs.contains(url2));
        Assert.assertTrue(curatorZookeeperTransporter.zookeeperClientMap.get(url2.toServerIdentityString()).originalURLs.size() == 2);
    }

    @Test
    public void testNotRepeatConnect() throws Exception {
        int zkServerPort2 = NetUtils.getAvailablePort();
        TestingServer zkServer2 = new TestingServer(zkServerPort2, true);

        URL url = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort + "/org.apache.dubbo.registry.RegistryService?application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL url2 = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort2 + "/org.apache.dubbo.metadata.store.MetadataReport?address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        ZookeeperClient newZookeeperClient = curatorZookeeperTransporter.connect(url);
        Assert.assertEquals(curatorZookeeperTransporter.zookeeperClientMap.size(), 1);
        Assert.assertEquals(curatorZookeeperTransporter.zookeeperClientMap.get(url.toServerIdentityString()).zookeeperClient, newZookeeperClient);
        Assert.assertTrue(curatorZookeeperTransporter.zookeeperClientMap.get(url.toServerIdentityString()).originalURLs.contains(url));
        Assert.assertTrue(curatorZookeeperTransporter.zookeeperClientMap.get(url.toServerIdentityString()).originalURLs.size() == 1);

        ZookeeperClient newZookeeperClient2 = curatorZookeeperTransporter.connect(url2);
        Assert.assertNotEquals(newZookeeperClient, newZookeeperClient2);
        Assert.assertEquals(curatorZookeeperTransporter.zookeeperClientMap.size(), 2);
        Assert.assertEquals(curatorZookeeperTransporter.zookeeperClientMap.get(url2.toServerIdentityString()).zookeeperClient, newZookeeperClient2);
        Assert.assertTrue(curatorZookeeperTransporter.zookeeperClientMap.get(url2.toServerIdentityString()).originalURLs.contains(url2));
        Assert.assertTrue(curatorZookeeperTransporter.zookeeperClientMap.get(url2.toServerIdentityString()).originalURLs.size() == 1);

        zkServer2.stop();
    }


}
