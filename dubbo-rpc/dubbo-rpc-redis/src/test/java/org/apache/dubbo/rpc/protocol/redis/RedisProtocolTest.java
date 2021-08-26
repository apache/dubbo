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
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcException;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.embedded.RedisServer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static redis.embedded.RedisServer.newRedisServer;

public class RedisProtocolTest {

    private static final String
            REDIS_URL_TEMPLATE = "redis://%slocalhost:%d",
            REDIS_PASSWORD = "123456",
            REDIS_URL_AUTH_SECTION = "username:" + REDIS_PASSWORD + "@";

    private static final Protocol PROTOCOL = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private static final ProxyFactory PROXY = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    private RedisServer redisServer;
    private URL registryUrl;

    @BeforeEach
    public void setUp(final TestInfo testInfo) throws IOException {
        final boolean usesAuthentication = usesAuthentication(testInfo);
        int redisPort = 0;
        IOException exception = null;

        for (int i = 0; i < 10; i++) {
            try {
                redisPort = NetUtils.getAvailablePort(30000 + new Random().nextInt(10000));
                redisServer = newRedisServer()
                        .port(redisPort)
                        // set maxheap to fix Windows error 0x70 while starting redis
                        .settingIf(SystemUtils.IS_OS_WINDOWS, "maxheap 128mb")
                        .settingIf(usesAuthentication, "requirepass " + REDIS_PASSWORD)
                        .build();
                this.redisServer.start();
                exception = null;
            } catch (IOException e) {
                e.printStackTrace();
                exception = e;
            }
            if (exception == null) {
                break;
            }
        }

        Assertions.assertNull(exception);
        registryUrl = newRedisUrl(usesAuthentication, redisPort);
    }

    private static boolean usesAuthentication(final TestInfo testInfo) {
        final String methodName = testInfo.getTestMethod().get().getName();
        return "testAuthRedis".equals(methodName) || "testWrongAuthRedis".equals(methodName);
    }
    private static URL newRedisUrl(final boolean usesAuthentication, final int redisPort) {
        final String urlAuthSection = usesAuthentication ? REDIS_URL_AUTH_SECTION : "";
        final String urlSuffix = usesAuthentication ? "?db.index=0" : "";
        return URL.valueOf(String.format(REDIS_URL_TEMPLATE, urlAuthSection, redisPort) + urlSuffix);
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.redisServer.stop();
    }
    @Test
    public void testReferClass() {
        Invoker<IDemoService> refer = PROTOCOL.refer(IDemoService.class, registryUrl);

        Class<IDemoService> serviceClass = refer.getInterface();
        assertThat(serviceClass.getName(), is("org.apache.dubbo.rpc.protocol.redis.IDemoService"));
    }

    @Test
    public void testInvocation() {
        Invoker<IDemoService> refer = PROTOCOL.refer(IDemoService.class,
                registryUrl
                        .addParameter("max.idle", 10)
                        .addParameter("max.active", 20));
        IDemoService demoService = PROXY.getProxy(refer);

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

    @Test
    public void testUnsupportedMethod() {
        Assertions.assertThrows(RpcException.class, () -> {
            Invoker<IDemoService> refer = PROTOCOL.refer(IDemoService.class, registryUrl);
            IDemoService demoService = this.PROXY.getProxy(refer);

            demoService.unsupported(null);
        });
    }

    @Test
    public void testWrongParameters() {
        Assertions.assertThrows(RpcException.class, () -> {
            Invoker<IDemoService> refer = PROTOCOL.refer(IDemoService.class, registryUrl);
            IDemoService demoService = this.PROXY.getProxy(refer);

            demoService.set("key", "value", "wrongValue");
        });
    }

    @Test
    public void testWrongRedis() {
        Assertions.assertThrows(RpcException.class, () -> {
            Invoker<IDemoService> refer = PROTOCOL.refer(IDemoService.class, URL.valueOf("redis://localhost:1"));
            IDemoService demoService = this.PROXY.getProxy(refer);

            demoService.get("key");
        });
    }

    @Test
    public void testExport() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> PROTOCOL.export(PROTOCOL.refer(IDemoService.class, registryUrl)));
    }

    @Test
    public void testAuthRedis() {
        // default db.index=0
        Invoker<IDemoService> refer = PROTOCOL.refer(IDemoService.class,
                registryUrl
                        .addParameter("max.idle", 10)
                        .addParameter("max.active", 20));
        IDemoService demoService = this.PROXY.getProxy(refer);

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
        refer = PROTOCOL.refer(IDemoService.class,
                registryUrl
                        .addParameter("max.idle", 10)
                        .addParameter("max.active", 20));
        demoService = this.PROXY.getProxy(refer);

        demoService.set("key", "newValue");
        value = demoService.get("key");
        assertThat(value, is("newValue"));

        // jedis gets the result comparison
        JedisPool pool = new JedisPool(new GenericObjectPoolConfig(), "localhost", registryUrl.getPort(), 2000, password, database, (String) null);
        try (Jedis jedis = pool.getResource()) {
            byte[] valueByte = jedis.get("key".getBytes());
            Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(this.registryUrl.getParameter(Constants.SERIALIZATION_KEY, "java"));
            ObjectInput oin = serialization.deserialize(this.registryUrl, new ByteArrayInputStream(valueByte));
            String actual = (String) oin.readObject();
            assertThat(value, is(actual));
        } catch (Exception e) {
            Assertions.fail("jedis gets the result comparison is error!");
        } finally {
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
        Invoker<IDemoService> refer = PROTOCOL.refer(IDemoService.class,
                registryUrl
                        .addParameter("max.idle", 10)
                        .addParameter("max.active", 20));
        IDemoService demoService = this.PROXY.getProxy(refer);

        try {
            String value = demoService.get("key");
            assertThat(value, is(nullValue()));
        } catch (RpcException e) {
            if (e.getCause() instanceof JedisConnectionException && e.getCause().getCause() instanceof JedisDataException) {
                Assertions.assertEquals("ERR invalid password", e.getCause().getCause().getMessage());
            } else {
                Assertions.fail("no invalid password exception!");
            }
        }

        refer.destroy();
    }
}
