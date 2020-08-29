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

package org.apache.dubbo.configcenter.support.redis;

import org.apache.commons.lang3.SystemUtils;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.embedded.RedisServer;
import redis.embedded.RedisServerBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RedisDynamicConfigurationTest {
    private static RedisServer redisServer;
    private static URL configCenterUrl;
    private static RedisDynamicConfiguration dynamicConfiguration;
    private static Jedis jedis;

    @BeforeAll
    public static void setUp() throws Exception {
        int redisPort = NetUtils.getAvailablePort();
        RedisServerBuilder builder = RedisServer.builder().port(redisPort);
        if (SystemUtils.IS_OS_WINDOWS) {
            // set maxheap to fix Windows error 0x70 while starting redis
            builder.setting("maxheap 128mb");
        }
        redisServer = builder.build();
        redisServer.start();
        configCenterUrl = URL.valueOf("redis://127.0.0.1:" + redisPort);
        dynamicConfiguration = new RedisDynamicConfiguration(configCenterUrl);
        jedis = new Jedis("127.0.0.1", redisPort);
    }

    @AfterAll
    public static void tearDown() throws Exception {
        dynamicConfiguration.close();
        redisServer.stop();
    }

    @Test
    public void testGetConfig() {
        jedis.set("/dubbo/config/dubbo/foo", "bar");
        // test equals
        assertEquals("bar", dynamicConfiguration.getConfig("foo", "dubbo"));
        // test does not block
        assertEquals("bar", dynamicConfiguration.getConfig("foo", "dubbo"));
        Assertions.assertNull(dynamicConfiguration.getConfig("not-exist", "dubbo"));
    }

    @Test
    public void testPublishConfig() {
        dynamicConfiguration.publishConfig("value", "metadata", "1");
        // test equals
        assertEquals("1", dynamicConfiguration.getConfig("value", "/metadata"));
        assertEquals("1", jedis.get("/dubbo/config/metadata/value"));
    }
}
