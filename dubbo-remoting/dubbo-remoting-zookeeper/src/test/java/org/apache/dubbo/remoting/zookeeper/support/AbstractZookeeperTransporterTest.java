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
package org.apache.dubbo.remoting.zookeeper.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;
import org.apache.dubbo.remoting.zookeeper.curator.CuratorZookeeperTransporter;

import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

/**
 * AbstractZookeeperTransporterTest
 */
public class AbstractZookeeperTransporterTest {
    private TestingServer zkServer;
    private ZookeeperClient zookeeperClient;
    private AbstractZookeeperTransporter abstractZookeeperTransporter;
    private int zkServerPort;

    @BeforeEach
    public void setUp() throws Exception {
        zkServerPort = NetUtils.getAvailablePort();
        zkServer = new TestingServer(zkServerPort, true);
        zookeeperClient = new CuratorZookeeperTransporter().connect(URL.valueOf("zookeeper://127.0.0.1:" +
                zkServerPort + "/service"));
        abstractZookeeperTransporter = new CuratorZookeeperTransporter();
    }


    @AfterEach
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
        URL newUrl = abstractZookeeperTransporter.toClientURL(url);
        Assertions.assertEquals(newUrl.getProtocol(), "zookeeper");
        Assertions.assertEquals(newUrl.getHost(), "127.0.0.1");
        Assertions.assertEquals(newUrl.getPort(), zkServerPort);
        Assertions.assertNull(newUrl.getUsername());
        Assertions.assertNull(newUrl.getPassword());
        Assertions.assertEquals(newUrl.getParameter(TIMEOUT_KEY, 5000), 2300);
        Assertions.assertEquals(newUrl.getParameters().size(), 1);
        Assertions.assertEquals(newUrl.getPath(), ZookeeperTransporter.class.getName());
    }


    @Test
    public void testToCreateURLWhenHasUser() {
        URL url = URL.valueOf("zookeeper://us2:pw2@127.0.0.1:" + zkServerPort + "/org.apache.dubbo.registry.RegistryService?application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL newUrl = abstractZookeeperTransporter.toClientURL(url);
        Assertions.assertEquals(newUrl.getProtocol(), "zookeeper");
        Assertions.assertEquals(newUrl.getHost(), "127.0.0.1");
        Assertions.assertEquals(newUrl.getPort(), zkServerPort);
        Assertions.assertEquals(newUrl.getUsername(), "us2");
        Assertions.assertEquals(newUrl.getPassword(), "pw2");
        Assertions.assertEquals(newUrl.getParameters().size(), 0);
        Assertions.assertEquals(newUrl.getPath(), ZookeeperTransporter.class.getName());
    }

    @Test
    public void testGetURLBackupAddress() {
        URL url = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort + "/org.apache.dubbo.registry.RegistryService?backup=127.0.0.1:" + 9099 + "&application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        List<String> stringList = abstractZookeeperTransporter.getURLBackupAddress(url);
        Assertions.assertEquals(stringList.size(), 2);
        Assertions.assertEquals(stringList.get(0), "127.0.0.1:" + zkServerPort);
        Assertions.assertEquals(stringList.get(1), "127.0.0.1:9099");
    }

    @Test
    public void testGetURLBackupAddressNoBack() {
        URL url = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort + "/org.apache.dubbo.registry.RegistryService?application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        List<String> stringList = abstractZookeeperTransporter.getURLBackupAddress(url);
        Assertions.assertEquals(stringList.size(), 1);
        Assertions.assertEquals(stringList.get(0), "127.0.0.1:" + zkServerPort);
    }

    @Test
    public void testFetchAndUpdateZookeeperClientCache() throws Exception {
        int zkServerPort2 = NetUtils.getAvailablePort();
        TestingServer zkServer2 = new TestingServer(zkServerPort2, true);

        int zkServerPort3 = NetUtils.getAvailablePort();
        TestingServer zkServer3 = new TestingServer(zkServerPort3, true);

        URL url = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort + "/org.apache.dubbo.registry.RegistryService?backup=127.0.0.1:" + zkServerPort3 + ",127.0.0.1:" + zkServerPort2 + "&application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        ZookeeperClient newZookeeperClient = abstractZookeeperTransporter.connect(url);
        //just for connected
        newZookeeperClient.getContent("/dubbo/test");
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 3);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zkServerPort), newZookeeperClient);

        URL url2 = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort + "/org.apache.dubbo.metadata.store.MetadataReport?address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        checkFetchAndUpdateCacheNotNull(url2);
        URL url3 = URL.valueOf("zookeeper://127.0.0.1:8778/org.apache.dubbo.metadata.store.MetadataReport?backup=127.0.0.1:" + zkServerPort3 + "&address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        checkFetchAndUpdateCacheNotNull(url3);

        zkServer2.stop();
        zkServer3.stop();
    }

    private void checkFetchAndUpdateCacheNotNull(URL url) {
        List<String> addressList = abstractZookeeperTransporter.getURLBackupAddress(url);
        ZookeeperClient zookeeperClient = abstractZookeeperTransporter.fetchAndUpdateZookeeperClientCache(addressList);
        Assertions.assertNotNull(zookeeperClient);
    }

    @Test
    public void testRepeatConnect() {
        URL url = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort + "/org.apache.dubbo.registry.RegistryService?application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL url2 = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort + "/org.apache.dubbo.metadata.store.MetadataReport?address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        ZookeeperClient newZookeeperClient = abstractZookeeperTransporter.connect(url);
        //just for connected
        newZookeeperClient.getContent("/dubbo/test");
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 1);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zkServerPort), newZookeeperClient);
        Assertions.assertTrue(newZookeeperClient.isConnected());

        ZookeeperClient newZookeeperClient2 = abstractZookeeperTransporter.connect(url2);
        //just for connected
        newZookeeperClient2.getContent("/dubbo/test");
        Assertions.assertEquals(newZookeeperClient, newZookeeperClient2);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 1);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zkServerPort), newZookeeperClient);
    }

    @Test
    public void testNotRepeatConnect() throws Exception {
        int zkServerPort2 = NetUtils.getAvailablePort();
        TestingServer zkServer2 = new TestingServer(zkServerPort2, true);

        URL url = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort + "/org.apache.dubbo.registry.RegistryService?application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL url2 = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort2 + "/org.apache.dubbo.metadata.store.MetadataReport?address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        ZookeeperClient newZookeeperClient = abstractZookeeperTransporter.connect(url);
        //just for connected
        newZookeeperClient.getContent("/dubbo/test");
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 1);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zkServerPort), newZookeeperClient);

        ZookeeperClient newZookeeperClient2 = abstractZookeeperTransporter.connect(url2);
        //just for connected
        newZookeeperClient2.getContent("/dubbo/test");
        Assertions.assertNotEquals(newZookeeperClient, newZookeeperClient2);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 2);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zkServerPort2), newZookeeperClient2);

        zkServer2.stop();
    }

    @Test
    public void testRepeatConnectForBackUpAdd() throws Exception {
        int zkServerPort2 = NetUtils.getAvailablePort();
        TestingServer zkServer2 = new TestingServer(zkServerPort2, true);

        int zkServerPort3 = NetUtils.getAvailablePort();
        TestingServer zkServer3 = new TestingServer(zkServerPort3, true);

        URL url = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort + "/org.apache.dubbo.registry.RegistryService?backup=127.0.0.1:" + zkServerPort2 + "&application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL url2 = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort2 + "/org.apache.dubbo.metadata.store.MetadataReport?backup=127.0.0.1:" + zkServerPort3 + "&address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        ZookeeperClient newZookeeperClient = abstractZookeeperTransporter.connect(url);
        //just for connected
        newZookeeperClient.getContent("/dubbo/test");
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 2);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zkServerPort), newZookeeperClient);

        ZookeeperClient newZookeeperClient2 = abstractZookeeperTransporter.connect(url2);
        //just for connected
        newZookeeperClient2.getContent("/dubbo/test");
        Assertions.assertEquals(newZookeeperClient, newZookeeperClient2);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 3);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zkServerPort2), newZookeeperClient2);

        zkServer2.stop();
        zkServer3.stop();
    }

    @Test
    public void testRepeatConnectForNoMatchBackUpAdd() throws Exception {
        int zkServerPort2 = NetUtils.getAvailablePort();
        TestingServer zkServer2 = new TestingServer(zkServerPort2, true);

        int zkServerPort3 = NetUtils.getAvailablePort();
        TestingServer zkServer3 = new TestingServer(zkServerPort3, true);

        URL url = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort + "/org.apache.dubbo.registry.RegistryService?backup=127.0.0.1:" + zkServerPort3 + "&application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL url2 = URL.valueOf("zookeeper://127.0.0.1:" + zkServerPort2 + "/org.apache.dubbo.metadata.store.MetadataReport?address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        ZookeeperClient newZookeeperClient = abstractZookeeperTransporter.connect(url);
        //just for connected
        newZookeeperClient.getContent("/dubbo/test");
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 2);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zkServerPort), newZookeeperClient);

        ZookeeperClient newZookeeperClient2 = abstractZookeeperTransporter.connect(url2);
        //just for connected
        newZookeeperClient2.getContent("/dubbo/test");
        Assertions.assertNotEquals(newZookeeperClient, newZookeeperClient2);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 3);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zkServerPort2), newZookeeperClient2);

        zkServer2.stop();
        zkServer3.stop();
    }
}
