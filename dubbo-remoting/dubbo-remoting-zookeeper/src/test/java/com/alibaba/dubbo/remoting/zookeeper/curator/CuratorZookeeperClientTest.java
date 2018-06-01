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
package com.alibaba.dubbo.remoting.zookeeper.curator;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.core.Is.is;

@Ignore
public class CuratorZookeeperClientTest {
    private TestingServer zkServer;
    private int zkServerPort;

    @Before
    public void setUp() throws Exception {
        zkServerPort = NetUtils.getAvailablePort();
        zkServer = new TestingServer(this.zkServerPort, true);
    }

    @Test
    public void testCheckExists() {
        CuratorZookeeperClient curatorClient = new CuratorZookeeperClient(URL.valueOf("zookeeper://127.0.0.1:" + this.zkServerPort + "/com.alibaba.dubbo.registry.RegistryService"));
        String path = "/dubbo/com.alibaba.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false);
        Assert.assertThat(curatorClient.checkExists(path), is(true));
        Assert.assertThat(curatorClient.checkExists(path + "/noneexits"), is(false));
    }


    @After
    public void tearDown() throws Exception {
        zkServer.stop();
    }
}
