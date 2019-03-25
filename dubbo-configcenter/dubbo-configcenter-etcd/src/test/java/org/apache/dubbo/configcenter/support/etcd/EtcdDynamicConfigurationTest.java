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

package org.apache.dubbo.configcenter.support.etcd;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.configcenter.ConfigChangeEvent;
import org.apache.dubbo.configcenter.ConfigurationListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Unit test for etcd config center support
 * TODO Integrate with https://github.com/etcd-io/jetcd#launcher or using mock data.
 */
@Disabled
public class EtcdDynamicConfigurationTest {

    private static final String ENDPOINT = "http://127.0.0.1:2379";

    private static EtcdDynamicConfiguration config;

    private static Client etcdClient;

    @Test
    public void testGetConfig() {
        put("/dubbo/config/org.apache.dubbo.etcd.testService/configurators", "hello");
        put("/dubbo/config/test/dubbo.properties", "aaa=bbb");
        Assertions.assertEquals("hello", config.getConfig("org.apache.dubbo.etcd.testService.configurators"));
        Assertions.assertEquals("aaa=bbb", config.getConfig("dubbo.properties", "test"));
    }

    @Test
    public void testAddListener() throws Exception {
        CountDownLatch latch = new CountDownLatch(4);
        TestListener listener1 = new TestListener(latch);
        TestListener listener2 = new TestListener(latch);
        TestListener listener3 = new TestListener(latch);
        TestListener listener4 = new TestListener(latch);
        config.addListener("AService.configurators", listener1);
        config.addListener("AService.configurators", listener2);
        config.addListener("testapp.tagrouters", listener3);
        config.addListener("testapp.tagrouters", listener4);

        put("/dubbo/config/AService/configurators", "new value1");
        Thread.sleep(200);
        put("/dubbo/config/testapp/tagrouters", "new value2");
        Thread.sleep(200);
        put("/dubbo/config/testapp", "new value3");

        Thread.sleep(1000);

        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(1, listener1.getCount("/dubbo/config/AService/configurators"));
        Assertions.assertEquals(1, listener2.getCount("/dubbo/config/AService/configurators"));
        Assertions.assertEquals(1, listener3.getCount("/dubbo/config/testapp/tagrouters"));
        Assertions.assertEquals(1, listener4.getCount("/dubbo/config/testapp/tagrouters"));

        Assertions.assertEquals("new value1", listener1.getValue());
        Assertions.assertEquals("new value1", listener2.getValue());
        Assertions.assertEquals("new value2", listener3.getValue());
        Assertions.assertEquals("new value2", listener4.getValue());
    }

    private class TestListener implements ConfigurationListener {
        private CountDownLatch latch;
        private String value;
        private Map<String, Integer> countMap = new HashMap<>();

        public TestListener(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void process(ConfigChangeEvent event) {
            Integer count = countMap.computeIfAbsent(event.getKey(), k -> 0);
            countMap.put(event.getKey(), ++count);
            value = event.getValue();
            latch.countDown();
        }

        public int getCount(String key) {
            return countMap.get(key);
        }

        public String getValue() {
            return value;
        }
    }

    static void put(String key, String value) {
        try {
            etcdClient.getKVClient()
                    .put(ByteSequence.from(key, UTF_8), ByteSequence.from(value, UTF_8))
                    .get();
        } catch (Exception e) {
            System.out.println("Error put value to etcd.");
        }
    }

    @BeforeAll
    static void setUp() {
        etcdClient = Client.builder().endpoints(ENDPOINT).build();
        // timeout in 15 seconds.
        URL url = URL.valueOf("etcd3://127.0.0.1:2379/org.apache.dubbo.etcd.testService")
                .addParameter(Constants.SESSION_TIMEOUT_KEY, 15000);
        config = new EtcdDynamicConfiguration(url);
    }

    @AfterAll
    static void tearDown() {
        etcdClient.close();
    }
}
