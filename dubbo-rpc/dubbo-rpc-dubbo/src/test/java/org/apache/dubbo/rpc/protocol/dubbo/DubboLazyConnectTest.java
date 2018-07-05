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
package org.apache.dubbo.rpc.protocol.dubbo;


import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.dubbo.support.ProtocolUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * dubbo protocol lazy connect test
 */
public class DubboLazyConnectTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test(expected = RpcException.class)
    public void testSticky1() {
        URL url = URL.valueOf("dubbo://127.0.0.1:9090/org.apache.dubbo.rpc.protocol.dubbo.IDemoService");
        ProtocolUtils.refer(IDemoService.class, url);
    }

    @Test
    public void testSticky2() {
        URL url = URL.valueOf("dubbo://127.0.0.1:9090/org.apache.dubbo.rpc.protocol.dubbo.IDemoService?" + Constants.LAZY_CONNECT_KEY + "=true");
        ProtocolUtils.refer(IDemoService.class, url);
    }

    @Test(expected = RpcException.class)
    public void testSticky3() {
        URL url = URL.valueOf("dubbo://127.0.0.1:9090/org.apache.dubbo.rpc.protocol.dubbo.IDemoService?" + Constants.LAZY_CONNECT_KEY + "=true");
        IDemoService service = (IDemoService) ProtocolUtils.refer(IDemoService.class, url);
        service.get();
    }

    @Test
    public void testSticky4() {
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("dubbo://127.0.0.1:" + port + "/org.apache.dubbo.rpc.protocol.dubbo.IDemoService?" + Constants.LAZY_CONNECT_KEY + "=true&timeout=20000");

        ProtocolUtils.export(new DemoServiceImpl(), IDemoService.class, url);

        IDemoService service = (IDemoService) ProtocolUtils.refer(IDemoService.class, url);
        Assert.assertEquals("ok", service.get());
    }

    public class DemoServiceImpl implements IDemoService {
        public String get() {
            return "ok";
        }
    }
}