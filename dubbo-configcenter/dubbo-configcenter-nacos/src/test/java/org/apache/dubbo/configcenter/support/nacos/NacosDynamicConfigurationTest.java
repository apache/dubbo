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

package org.apache.dubbo.configcenter.support.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.configcenter.ConfigChangeEvent;
import org.apache.dubbo.configcenter.ConfigurationListener;

import org.apache.dubbo.configcenter.DynamicConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


/**
 * Unit test for nacos config center support
 */
//FIXME: waiting for embedded Nacos suport, then we can open the switch.
@Disabled("https://github.com/alibaba/nacos/issues/1188")
public class NacosDynamicConfigurationTest {
    private static final String SESSION_TIMEOUT_KEY = "session";

    private static NacosDynamicConfiguration config;

    /**
     * A test client to put data to Nacos server for testing purpose
     */
    private static ConfigService nacosClient;

    @Test
    public void testGetConfig() throws Exception {
        put("org.apache.dubbo.nacos.testService.configurators", "hello");
        Thread.sleep(200);
        put("dubbo.properties", "test", "aaa=bbb");
        Thread.sleep(200);
        put("xxxx:org.apache.dubbo.demo.DemoService:1.0.0.test.configurators", "helloworld");
        Thread.sleep(200);
        Assertions.assertEquals("hello", config.getConfig("org.apache.dubbo.nacos.testService.configurators"));
        Assertions.assertEquals("aaa=bbb", config.getConfig("dubbo.properties", "test"));
        Assertions.assertEquals("helloworld", config.getConfig("xxxx*org.apache.dubbo.demo.DemoService:1.0.0.test.configurators"));
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

        put("AService.configurators", "new value1");
        Thread.sleep(200);
        put("testapp.tagrouters", "new value2");
        Thread.sleep(200);
        put("testapp", "new value3");
        Thread.sleep(5000);

        latch.await();

        Assertions.assertEquals(1, listener1.getCount("AService.configurators"));
        Assertions.assertEquals(1, listener2.getCount("AService.configurators"));
        Assertions.assertEquals(1, listener3.getCount("testapp.tagrouters"));
        Assertions.assertEquals(1, listener4.getCount("testapp.tagrouters"));

        Assertions.assertEquals("new value1", listener1.getValue());
        Assertions.assertEquals("new value1", listener2.getValue());
        Assertions.assertEquals("new value2", listener3.getValue());
        Assertions.assertEquals("new value2", listener4.getValue());

    }

    private void put(String key, String value) {
        put(key, DynamicConfiguration.DEFAULT_GROUP, value);
    }

    private void put(String key, String group, String value) {
        try {
            nacosClient.publishConfig(key, group, value);
        } catch (Exception e) {
            System.out.println("Error put value to nacos.");
        }
    }

    @BeforeAll
    public static void setUp() {
        String urlForDubbo = "nacos://" + "127.0.0.1:8848" + "/org.apache.dubbo.nacos.testService";
        // timeout in 15 seconds.
        URL url = URL.valueOf(urlForDubbo)
                .addParameter(SESSION_TIMEOUT_KEY, 15000);
        config = new NacosDynamicConfiguration(url);


        try {
            nacosClient = NacosFactory.createConfigService("127.0.0.1:8848");
        } catch (NacosException e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    public static void tearDown() {

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
            System.out.println(this + ": " + event);
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

}
