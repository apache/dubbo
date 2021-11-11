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
package org.apache.dubbo.registry.redis;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;

import org.apache.commons.lang3.SystemUtils;
import org.apache.dubbo.registry.support.AbstractRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static redis.embedded.RedisServer.newRedisServer;

public class RedisRegistryTest {

    private static final String SERVICE = "org.apache.dubbo.test.injvmServie";
    private static final URL SERVICE_URL = URL.valueOf("redis://redis/" + SERVICE + "?notify=false&methods=test1,test2&category=providers,configurators,routers");
    private static final URL PROVIDER_URL_A = URL.valueOf("redis://127.0.0.1:20880/" + SERVICE + "?notify=false&methods=test1,test2");
    private static final URL PROVIDER_URL_B = URL.valueOf("redis://127.0.0.1:20881/" + SERVICE + "?notify=false&methods=test1,test2");

    private RedisServer redisServer;
    private RedisRegistry redisRegistry;
    private URL registryUrl;

    @BeforeEach
    public void setUp() throws Exception {
        int redisPort = 0;
        IOException exception = null;

        for (int i = 0; i < 10; i++) {
            try {
                redisPort = NetUtils.getAvailablePort(30000 + new Random().nextInt(10000));
                redisServer = newRedisServer()
                        .port(redisPort)
                        // set maxheap to fix Windows error 0x70 while starting redis
                        .settingIf(SystemUtils.IS_OS_WINDOWS, "maxheap 128mb")
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
        registryUrl = URL.valueOf("redis://localhost:" + redisPort + "?session=4000");
        redisRegistry = (RedisRegistry) new RedisRegistryFactory().createRegistry(registryUrl);
    }

    @AfterEach
    public void tearDown() throws Exception {
        this.redisServer.stop();
    }

    @Test
    public void testRegister() {
        Set<URL> registered = null;

        for (int i = 0; i < 2; i++) {
            redisRegistry.register(SERVICE_URL);
            registered = redisRegistry.getRegistered();
            assertThat(registered.contains(SERVICE_URL), is(true));
        }

        registered = redisRegistry.getRegistered();
        assertThat(registered.size(), is(1));
    }

    @Test
    public void testAnyHost() {
        assertThrows(IllegalStateException.class, () -> {
            URL errorUrl = URL.valueOf("multicast://0.0.0.0/");
            new RedisRegistryFactory().createRegistry(errorUrl);
        });
    }

    @Test
    public void testSubscribeExpireCache() throws Exception {
        redisRegistry.register(PROVIDER_URL_A);
        redisRegistry.register(PROVIDER_URL_B);

        NotifyListener listener = new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
            }
        };

        redisRegistry.subscribe(SERVICE_URL, listener);

        Field expireCache = RedisRegistry.class.getDeclaredField("expireCache");
        expireCache.setAccessible(true);
        Map<URL, Long> cacheExpire = (Map<URL, Long>)expireCache.get(redisRegistry);

        assertThat(cacheExpire.get(PROVIDER_URL_A) > 0, is(true));
        assertThat(cacheExpire.get(PROVIDER_URL_B) > 0, is(true));

        redisRegistry.unregister(PROVIDER_URL_A);

        boolean success = false;

        for (int i = 0; i < 30; i++) {
            cacheExpire = (Map<URL, Long>)expireCache.get(redisRegistry);
            if (cacheExpire.get(PROVIDER_URL_A) == null) {
                success = true;
                break;
            }
            Thread.sleep(500);
        }
        assertThat(success, is(true));
    }

    @Test
    public void testSubscribeWhenProviderCrash() throws Exception {

        // unit test will fail if doExpire=false
        // Field doExpireField = RedisRegistry.class.getDeclaredField("doExpire");
        // doExpireField.setAccessible(true);
        // doExpireField.set(redisRegistry, false);

        redisRegistry.register(PROVIDER_URL_A);
        redisRegistry.register(PROVIDER_URL_B);
        assertThat(redisRegistry.getRegistered().contains(PROVIDER_URL_A), is(true));
        assertThat(redisRegistry.getRegistered().contains(PROVIDER_URL_B), is(true));

        Set<URL> notifiedUrls = new HashSet<>();
        Object lock = new Object();

        NotifyListener listener = new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                synchronized (lock) {
                    notifiedUrls.clear();
                    notifiedUrls.addAll(urls);
                }
            }
        };

        redisRegistry.subscribe(SERVICE_URL, listener);
        assertThat(redisRegistry.getSubscribed().size(), is(1));

        boolean firstOk = false;
        boolean secondOk = false;

        for (int i = 0; i < 30; i++) {
            synchronized (lock) {
                if (notifiedUrls.contains(PROVIDER_URL_A) && notifiedUrls.contains(PROVIDER_URL_B)) {
                    firstOk = true;
                    break;
                }
            }
            Thread.sleep(500);
        }

        assertThat(firstOk, is(true));

        // kill -9 to providerB
        Field registeredField = AbstractRegistry.class.getDeclaredField("registered");
        registeredField.setAccessible(true);
        ((Set<URL>) registeredField.get(redisRegistry)).remove(PROVIDER_URL_B);

        for (int i = 0; i < 30; i++) {
            synchronized (lock) {
                if (notifiedUrls.contains(PROVIDER_URL_A) && notifiedUrls.size() == 1) {
                    secondOk = true;
                    break;
                }
            }
            Thread.sleep(500);
        }
        assertThat(secondOk, is(true));
    }

    @Test
    public void testSubscribeAndUnsubscribe() {
        NotifyListener listener = new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {

            }
        };
        redisRegistry.subscribe(SERVICE_URL, listener);

        Map<URL, Set<NotifyListener>> subscribed = redisRegistry.getSubscribed();
        assertThat(subscribed.size(), is(1));
        assertThat(subscribed.get(SERVICE_URL).size(), is(1));

        redisRegistry.unsubscribe(SERVICE_URL, listener);
        subscribed = redisRegistry.getSubscribed();
        assertThat(subscribed.get(SERVICE_URL).size(), is(0));
    }

    @Test
    public void testAvailable() {
        redisRegistry.register(SERVICE_URL);
        assertThat(redisRegistry.isAvailable(), is(true));

        redisRegistry.destroy();
        assertThrows(JedisConnectionException.class, () -> redisRegistry.isAvailable());
    }

    @Test
    public void testAvailableWithBackup() {
        URL url = URL.valueOf("redis://redisOne:8880").addParameter(BACKUP_KEY, "redisTwo:8881");
        Registry registry = new RedisRegistryFactory().createRegistry(url);

        Registry finalRegistry = registry;
        assertThrows(JedisConnectionException.class, () -> finalRegistry.isAvailable());

        url = URL.valueOf(this.registryUrl.toFullString()).addParameter(BACKUP_KEY, "redisTwo:8881");
        registry = new RedisRegistryFactory().createRegistry(url);

        assertThat(registry.isAvailable(), is(true));
    }
}
