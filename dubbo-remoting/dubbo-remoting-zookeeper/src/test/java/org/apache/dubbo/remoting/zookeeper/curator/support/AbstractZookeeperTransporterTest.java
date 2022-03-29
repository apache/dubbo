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
package org.apache.dubbo.remoting.zookeeper.curator.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.zookeeper.AbstractZookeeperTransporter;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.curator.CuratorZookeeperTransporter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

/**
 * AbstractZookeeperTransporterTest
 */
@DisabledForJreRange(min = JRE.JAVA_16)
public class AbstractZookeeperTransporterTest {
    private ZookeeperClient zookeeperClient;
    private AbstractZookeeperTransporter abstractZookeeperTransporter;

    private static int zookeeperServerPort1, zookeeperServerPort2;
    private static String zookeeperConnectionAddress1, zookeeperConnectionAddress2;

    @BeforeAll
    public static void beforeAll() {
        zookeeperConnectionAddress1 = System.getProperty("zookeeper.connection.address.1");
        zookeeperConnectionAddress2 = System.getProperty("zookeeper.connection.address.2");
        zookeeperServerPort1 = Integer.parseInt(zookeeperConnectionAddress1.substring(zookeeperConnectionAddress1.lastIndexOf(":") + 1));
        zookeeperServerPort2 = Integer.parseInt(zookeeperConnectionAddress2.substring(zookeeperConnectionAddress2.lastIndexOf(":") + 1));
    }

    @BeforeEach
    public void setUp() throws Exception {
        zookeeperClient = new CuratorZookeeperTransporter().connect(URL.valueOf("zookeeper://127.0.0.1:" +
            zookeeperServerPort1 + "/service"));
        abstractZookeeperTransporter = new CuratorZookeeperTransporter();
    }

    @Test
    public void testZookeeperClient() {
        assertThat(zookeeperClient, not(nullValue()));
        zookeeperClient.close();
    }

    @Test
    public void testGetURLBackupAddress() {
        URL url = URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService?backup=127.0.0.1:" + 9099 + "&application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        List<String> stringList = abstractZookeeperTransporter.getURLBackupAddress(url);
        Assertions.assertEquals(stringList.size(), 2);
        Assertions.assertEquals(stringList.get(0), "127.0.0.1:" + zookeeperServerPort1);
        Assertions.assertEquals(stringList.get(1), "127.0.0.1:9099");
    }

    @Test
    public void testGetURLBackupAddressNoBack() {
        URL url = URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService?application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        List<String> stringList = abstractZookeeperTransporter.getURLBackupAddress(url);
        Assertions.assertEquals(stringList.size(), 1);
        Assertions.assertEquals(stringList.get(0), "127.0.0.1:" + zookeeperServerPort1);
    }

    @Test
    public void testFetchAndUpdateZookeeperClientCache() throws Exception {
        URL url = URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService?backup=127.0.0.1:" + zookeeperServerPort1 + ",127.0.0.1:" + zookeeperServerPort2 + "&application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        ZookeeperClient newZookeeperClient = abstractZookeeperTransporter.connect(url);
        //just for connected
        newZookeeperClient.getContent("/dubbo/test");
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 2);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort1), newZookeeperClient);

        URL url2 = URL.valueOf("zookeeper://127.0.0.1:" + zookeeperServerPort1 + "/org.apache.dubbo.metadata.store.MetadataReport?address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        checkFetchAndUpdateCacheNotNull(url2);
        URL url3 = URL.valueOf("zookeeper://127.0.0.1:8778/org.apache.dubbo.metadata.store.MetadataReport?backup=127.0.0.1:" + zookeeperServerPort2 + "&address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        checkFetchAndUpdateCacheNotNull(url3);
    }

    private void checkFetchAndUpdateCacheNotNull(URL url) {
        List<String> addressList = abstractZookeeperTransporter.getURLBackupAddress(url);
        ZookeeperClient zookeeperClient = abstractZookeeperTransporter.fetchAndUpdateZookeeperClientCache(addressList);
        Assertions.assertNotNull(zookeeperClient);
    }

    @Test
    public void testRepeatConnect() {
        URL url = URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService?application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL url2 = URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.metadata.store.MetadataReport?address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        ZookeeperClient newZookeeperClient = abstractZookeeperTransporter.connect(url);
        //just for connected
        newZookeeperClient.getContent("/dubbo/test");
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 1);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort1), newZookeeperClient);
        Assertions.assertTrue(newZookeeperClient.isConnected());

        ZookeeperClient newZookeeperClient2 = abstractZookeeperTransporter.connect(url2);
        //just for connected
        newZookeeperClient2.getContent("/dubbo/test");
        Assertions.assertEquals(newZookeeperClient, newZookeeperClient2);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 1);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort1), newZookeeperClient);
    }

    @Test
    public void testNotRepeatConnect() throws Exception {
        URL url = URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService?application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL url2 = URL.valueOf(zookeeperConnectionAddress2 + "/org.apache.dubbo.metadata.store.MetadataReport?address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        ZookeeperClient newZookeeperClient = abstractZookeeperTransporter.connect(url);
        //just for connected
        newZookeeperClient.getContent("/dubbo/test");
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 1);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort1), newZookeeperClient);

        ZookeeperClient newZookeeperClient2 = abstractZookeeperTransporter.connect(url2);
        //just for connected
        newZookeeperClient2.getContent("/dubbo/test");
        Assertions.assertNotEquals(newZookeeperClient, newZookeeperClient2);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 2);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort2), newZookeeperClient2);

    }

    @Test
    public void testRepeatConnectForBackUpAdd() throws Exception {

        URL url = URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService?backup=127.0.0.1:" + zookeeperServerPort1 + "&application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL url2 = URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.metadata.store.MetadataReport?backup=127.0.0.1:" + zookeeperServerPort2 + "&address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        ZookeeperClient newZookeeperClient = abstractZookeeperTransporter.connect(url);
        //just for connected
        newZookeeperClient.getContent("/dubbo/test");
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 1);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort1), newZookeeperClient);

        ZookeeperClient newZookeeperClient2 = abstractZookeeperTransporter.connect(url2);
        //just for connected
        newZookeeperClient2.getContent("/dubbo/test");
        Assertions.assertEquals(newZookeeperClient, newZookeeperClient2);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 2);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort2), newZookeeperClient2);

    }

    @Test
    public void testRepeatConnectForNoMatchBackUpAdd() throws Exception {

        URL url = URL.valueOf(zookeeperConnectionAddress1 + "/org.apache.dubbo.registry.RegistryService?backup=127.0.0.1:" + zookeeperServerPort1 + "&application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT&timestamp=1547102428828");
        URL url2 = URL.valueOf(zookeeperConnectionAddress2 + "/org.apache.dubbo.metadata.store.MetadataReport?address=zookeeper://127.0.0.1:2181&application=metadatareport-local-xml-provider2&cycle-report=false&interface=org.apache.dubbo.metadata.store.MetadataReport&retry-period=4590&retry-times=23&sync-report=true");
        ZookeeperClient newZookeeperClient = abstractZookeeperTransporter.connect(url);
        //just for connected
        newZookeeperClient.getContent("/dubbo/test");
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 1);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort1), newZookeeperClient);

        ZookeeperClient newZookeeperClient2 = abstractZookeeperTransporter.connect(url2);
        //just for connected
        newZookeeperClient2.getContent("/dubbo/test");
        Assertions.assertNotEquals(newZookeeperClient, newZookeeperClient2);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().size(), 2);
        Assertions.assertEquals(abstractZookeeperTransporter.getZookeeperClientMap().get("127.0.0.1:" + zookeeperServerPort2), newZookeeperClient2);

    }

    @Test
    public void testSameHostWithDifferentUser() throws Exception {
        URL url1 = URL.valueOf("zookeeper://us1:pw1@127.0.0.1:" + zookeeperServerPort1 + "/path1");
        URL url2 = URL.valueOf("zookeeper://us2:pw2@127.0.0.1:" + zookeeperServerPort1 + "/path2");
        ZookeeperClient client1 = abstractZookeeperTransporter.connect(url1);
        ZookeeperClient client2 = abstractZookeeperTransporter.connect(url2);
        assertThat(client1, not(client2));
    }
}
