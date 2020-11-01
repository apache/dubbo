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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdClusterFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.dubbo.remoting.etcd.Constants.SESSION_TIMEOUT_KEY;

/**
 * Unit test for etcd config center support
 * Integrate with https://github.com/etcd-io/jetcd#launcher
 */
public class EtcdDynamicConfigurationTest {

    private static EtcdDynamicConfiguration config;

    public EtcdCluster etcdCluster = EtcdClusterFactory.buildCluster(getClass().getSimpleName(), 3, false);

    private static Client client;

    @Test
    public void testGetConfig() {

        put("/dubbo/config/org.apache.dubbo.etcd.testService/configurators", "hello");
        put("/dubbo/config/test/dubbo.properties", "aaa=bbb");
        Assert.assertEquals("hello", config.getConfig("org.apache.dubbo.etcd.testService.configurators", DynamicConfiguration.DEFAULT_GROUP));
        Assert.assertEquals("aaa=bbb", config.getConfig("dubbo.properties", "test"));
    }

    private ConfigurationListener mockListener(CountDownLatch latch, String[] value, Map<String, Integer> countMap) {
        ConfigurationListener listener = Mockito.mock(ConfigurationListener.class);
        Mockito.doAnswer(invo -> {
            ConfigChangedEvent event = invo.getArgument(0);
            Integer count = countMap.computeIfAbsent(event.getKey(), k -> new Integer(0));
            countMap.put(event.getKey(), ++count);
            value[0] = event.getContent();
            latch.countDown();
            return null;
        }).when(listener).process(Mockito.any());
        return listener;
    }

    @Test
    public void testAddListener() throws Exception {
        CountDownLatch latch = new CountDownLatch(4);
        String[] value1 = new String[1], value2 = new String[1], value3 = new String[1], value4 = new String[1];
        Map<String, Integer> countMap1 = new HashMap<>(), countMap2 = new HashMap<>(), countMap3 = new HashMap<>(), countMap4 = new HashMap<>();
        ConfigurationListener listener1 = mockListener(latch, value1, countMap1);
        ConfigurationListener listener2 = mockListener(latch, value2, countMap2);
        ConfigurationListener listener3 = mockListener(latch, value3, countMap3);
        ConfigurationListener listener4 = mockListener(latch, value4, countMap4);
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

        Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
        Assert.assertEquals(1, (int) countMap1.get("/dubbo/config/AService/configurators"));
        Assert.assertEquals(1, (int) countMap2.get("/dubbo/config/AService/configurators"));
        Assert.assertEquals(1, (int) countMap3.get("/dubbo/config/testapp/tagrouters"));
        Assert.assertEquals(1, (int) countMap4.get("/dubbo/config/testapp/tagrouters"));

        Assert.assertEquals("new value1", value1[0]);
        Assert.assertEquals("new value1", value2[0]);
        Assert.assertEquals("new value2", value3[0]);
        Assert.assertEquals("new value2", value4[0]);
    }

    private class TestListener implements ConfigurationListener {
        private CountDownLatch latch;
        private String value;
        private Map<String, Integer> countMap = new HashMap<>();

        public TestListener(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void process(ConfigChangedEvent event) {
            Integer count = countMap.computeIfAbsent(event.getKey(), k -> 0);
            countMap.put(event.getKey(), ++count);
            value = event.getContent();
            latch.countDown();
        }

        public int getCount(String key) {
            return countMap.get(key);
        }

        public String getValue() {
            return value;
        }
    }

    private void put(String key, String value) {
        try {
            client.getKVClient().put(ByteSequence.from(key, UTF_8), ByteSequence.from(value, UTF_8)).get();
        } catch (Exception e) {
            System.out.println("Error put value to etcd.");
        }
    }

    @Before
    public void setUp() {

        etcdCluster.start();

        client = Client.builder().endpoints(etcdCluster.getClientEndpoints()).build();

        List<URI> clientEndPoints = etcdCluster.getClientEndpoints();

        String ipAddress = clientEndPoints.get(0).getHost() + ":" + clientEndPoints.get(0).getPort();
        String urlForDubbo = "etcd3://" + ipAddress + "/org.apache.dubbo.etcd.testService";

        // timeout in 15 seconds.
        URL url = URL.valueOf(urlForDubbo)
                .addParameter(SESSION_TIMEOUT_KEY, 15000);
        config = new EtcdDynamicConfiguration(url);
    }

    @After
    public void tearDown() {
        etcdCluster.close();
    }

}
