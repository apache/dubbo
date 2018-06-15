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
package org.apache.dubbo.rpc.protocol.redis;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.embedded.RedisServer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class RedisProtocolTest {
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    private RedisServer redisServer;
    private URL registryUrl;

    @Before
    public void setUp() throws Exception {
        int redisPort = NetUtils.getAvailablePort();
        this.redisServer = new RedisServer(redisPort);
        this.redisServer.start();
        this.registryUrl = URL.valueOf("redis://localhost:" + redisPort);
    }

    @After
    public void tearDown() {
        this.redisServer.stop();
    }

    @Test
    public void testReferClass() {
        Invoker<IDemoService> refer = protocol.refer(IDemoService.class, registryUrl);

        Class<IDemoService> serviceClass = refer.getInterface();
        assertThat(serviceClass.getName(), is("org.apache.dubbo.rpc.protocol.redis.IDemoService"));
    }

    @Test
    public void testInvocation() {
        Invoker<IDemoService> refer = protocol.refer(IDemoService.class,
                registryUrl
                        .addParameter("max.idle", 10)
                        .addParameter("max.active", 20));
        IDemoService demoService = this.proxy.getProxy(refer);

        String value = demoService.get("key");
        assertThat(value, is(nullValue()));

        demoService.set("key", "newValue");
        value = demoService.get("key");
        assertThat(value, is("newValue"));

        demoService.delete("key");
        value = demoService.get("key");
        assertThat(value, is(nullValue()));

        refer.destroy();
    }

    @Test(expected = RpcException.class)
    public void testUnsupportedMethod() {
        Invoker<IDemoService> refer = protocol.refer(IDemoService.class, registryUrl);
        IDemoService demoService = this.proxy.getProxy(refer);

        demoService.unsupported(null);
    }

    @Test(expected = RpcException.class)
    public void testWrongParameters() {
        Invoker<IDemoService> refer = protocol.refer(IDemoService.class, registryUrl);
        IDemoService demoService = this.proxy.getProxy(refer);

        demoService.set("key", "value", "wrongValue");
    }

    @Test(expected = RpcException.class)
    public void testWrongRedis() {
        Invoker<IDemoService> refer = protocol.refer(IDemoService.class, URL.valueOf("redis://localhost:1"));
        IDemoService demoService = this.proxy.getProxy(refer);

        demoService.get("key");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExport() {
        protocol.export(protocol.refer(IDemoService.class, registryUrl));
    }
}