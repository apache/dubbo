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

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcResult;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.embedded.RedisServer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class RedisProtocolTest {
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    private RedisServer redisServer;
    private URL registryUrl;

    @Rule
    public TestName name = new TestName();

    @Before
    public void setUp() throws Exception {
        int redisPort = NetUtils.getAvailablePort();
        if (name.getMethodName().equals("testAuthRedis") || name.getMethodName().equals("testWrongAuthRedis")) {
            String password = "123456";
            this.redisServer = RedisServer.builder().port(redisPort).setting("requirepass " + password).build();
            this.registryUrl = URL.valueOf("redis://username:"+password+"@localhost:"+redisPort+"?db.index=0");
        } else {
            this.redisServer = RedisServer.builder().port(redisPort).build();
            this.registryUrl = URL.valueOf("redis://localhost:" + redisPort);
        }
        this.redisServer.start();
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

    @Test
    public void testAuthRedis() {
        // default db.index=0
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

        //change db.index=1
        String password = "123456";
        int database = 1;
        this.registryUrl = this.registryUrl.setPassword(password).addParameter("db.index", database);
        refer = protocol.refer(IDemoService.class,
                registryUrl
                        .addParameter("max.idle", 10)
                        .addParameter("max.active", 20));
        demoService = this.proxy.getProxy(refer);

        demoService.set("key", "newValue");
        value = demoService.get("key");
        assertThat(value, is("newValue"));

        // jedis gets the result comparison
        JedisPool pool = new JedisPool(new GenericObjectPoolConfig(), "localhost", registryUrl.getPort(), 2000, password, database, (String)null);
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            byte[] valueByte = jedis.get("key".getBytes());
            Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(this.registryUrl.getParameter(Constants.SERIALIZATION_KEY, "java"));
            ObjectInput oin = serialization.deserialize(this.registryUrl, new ByteArrayInputStream(valueByte));
            String actual = (String) oin.readObject();
            assertThat(value, is(actual));
        } catch(Exception e) {
            Assert.fail("jedis gets the result comparison is error!");
        } finally {
            if (jedis != null) {
                jedis.close();
            }
            pool.destroy();
        }

        demoService.delete("key");
        value = demoService.get("key");
        assertThat(value, is(nullValue()));

        refer.destroy();
    }

    @Test
    public void testWrongAuthRedis() {
        String password = "1234567";
        this.registryUrl = this.registryUrl.setPassword(password);
        Invoker<IDemoService> refer = protocol.refer(IDemoService.class,
                registryUrl
                        .addParameter("max.idle", 10)
                        .addParameter("max.active", 20));
        IDemoService demoService = this.proxy.getProxy(refer);

        try {
            String value = demoService.get("key");
            assertThat(value, is(nullValue()));
        } catch (RpcException e) {
            if (e.getCause() instanceof JedisConnectionException && e.getCause().getCause() instanceof JedisDataException) {
                Assert.assertEquals("ERR invalid password" , e.getCause().getCause().getMessage());
            } else {
                Assert.fail("no invalid password exception!");
            }
        }

        refer.destroy();
    }
}