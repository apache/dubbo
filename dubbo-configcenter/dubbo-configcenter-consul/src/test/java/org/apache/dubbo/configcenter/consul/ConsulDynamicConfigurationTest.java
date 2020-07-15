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
package org.apache.dubbo.configcenter.consul;

import org.apache.dubbo.common.URL;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.model.kv.Value;
import com.pszymczyk.consul.ConsulProcess;
import com.pszymczyk.consul.ConsulStarterBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
public class ConsulDynamicConfigurationTest {

    private static ConsulProcess consul;
    private static URL configCenterUrl;
    private static ConsulDynamicConfiguration configuration;

    private static Consul client;
    private static KeyValueClient kvClient;

    @BeforeAll
    public static void setUp() throws Exception {
        consul = ConsulStarterBuilder.consulStarter()
                .build()
                .start();
        configCenterUrl = URL.valueOf("consul://127.0.0.1:" + consul.getHttpPort());

        configuration = new ConsulDynamicConfiguration(configCenterUrl);
        client = Consul.builder().withHostAndPort(HostAndPort.fromParts("127.0.0.1", consul.getHttpPort())).build();
        kvClient = client.keyValueClient();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        consul.close();
        configuration.close();
    }

    @Test
    public void testGetConfig() {
        kvClient.putValue("/dubbo/config/dubbo/foo", "bar");
        // test equals
        assertEquals("bar", configuration.getConfig("foo", "dubbo"));
        // test does not block
        assertEquals("bar", configuration.getConfig("foo", "dubbo"));
        Assertions.assertNull(configuration.getConfig("not-exist", "dubbo"));
    }

    @Test
    public void testPublishConfig() {
        configuration.publishConfig("value", "metadata", "1");
        // test equals
        assertEquals("1", configuration.getConfig("value", "/metadata"));
        assertEquals("1", kvClient.getValueAsString("/dubbo/config/metadata/value").get());
    }

    @Test
    public void testAddListener() {
        KVCache cache = KVCache.newCache(kvClient, "/dubbo/config/dubbo/foo");
        cache.addListener(newValues -> {
            // Cache notifies all paths with "foo" the root path
            // If you want to watch only "foo" value, you must filter other paths
            Optional<Value> newValue = newValues.values().stream()
                    .filter(value -> value.getKey().equals("foo"))
                    .findAny();

            newValue.ifPresent(value -> {
                // Values are encoded in key/value store, decode it if needed
                Optional<String> decodedValue = newValue.get().getValueAsString();
                decodedValue.ifPresent(v -> System.out.println(String.format("Value is: %s", v))); //prints "bar"
            });
        });
        cache.start();

        kvClient.putValue("/dubbo/config/dubbo/foo", "new-value");
        kvClient.putValue("/dubbo/config/dubbo/foo/sub", "sub-value");
        kvClient.putValue("/dubbo/config/dubbo/foo/sub2", "sub-value2");
        kvClient.putValue("/dubbo/config/foo", "parent-value");

        System.out.println(kvClient.getKeys("/dubbo/config/dubbo/foo"));
        System.out.println(kvClient.getKeys("/dubbo/config"));
        System.out.println(kvClient.getValues("/dubbo/config/dubbo/foo"));
    }

    @Test
    public void testGetConfigKeys() {
        configuration.publishConfig("v1", "metadata", "1");
        configuration.publishConfig("v2", "metadata", "2");
        configuration.publishConfig("v3", "metadata", "3");
        // test equals
        assertEquals(new TreeSet(Arrays.asList("v1", "v2", "v3")), configuration.getConfigKeys("metadata"));
    }
}
