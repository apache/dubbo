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
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;

import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterEach;
=======
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;

import org.junit.jupiter.api.BeforeAll;
>>>>>>> origin/3.2
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

<<<<<<< HEAD
public class Curator5ZookeeperTransporterTest {
    private TestingServer zkServer;
    private ZookeeperClient zookeeperClient;
    private Curator5ZookeeperTransporter curatorZookeeperTransporter;
    private int zkServerPort;

    @BeforeEach
    public void setUp() throws Exception {
        zkServerPort = NetUtils.getAvailablePort();
        zkServer = new TestingServer(zkServerPort, true);
        zookeeperClient = new Curator5ZookeeperTransporter().connect(URL.valueOf("zookeeper://127.0.0.1:" +
                zkServerPort + "/service"));
=======
class Curator5ZookeeperTransporterTest {
    private ZookeeperClient zookeeperClient;
    private Curator5ZookeeperTransporter curatorZookeeperTransporter;
    private static String zookeeperConnectionAddress1;

    @BeforeAll
    public static void beforeAll() {
        zookeeperConnectionAddress1 = System.getProperty("zookeeper.connection.address.1");
    }

    @BeforeEach
    public void setUp() throws Exception {
        zookeeperClient = new Curator5ZookeeperTransporter().connect(URL.valueOf(zookeeperConnectionAddress1 + "/service"));
>>>>>>> origin/3.2
        curatorZookeeperTransporter = new Curator5ZookeeperTransporter();
    }

    @Test
<<<<<<< HEAD
    public void testZookeeperClient() {
        assertThat(zookeeperClient, not(nullValue()));
        zookeeperClient.close();
    }

    @AfterEach
    public void tearDown() throws Exception {
        zkServer.stop();
    }
=======
    void testZookeeperClient() {
        assertThat(zookeeperClient, not(nullValue()));
        zookeeperClient.close();
    }
>>>>>>> origin/3.2
}
