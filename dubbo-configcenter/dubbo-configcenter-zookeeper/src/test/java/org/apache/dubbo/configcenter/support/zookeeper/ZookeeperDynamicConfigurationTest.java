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
package org.apache.dubbo.configcenter.support.zookeeper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.config.configcenter.DynamicConfigurationFactory;
import org.apache.dubbo.common.extension.ExtensionLoader;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TODO refactor using mockito
 */
@Disabled("Disabled Due to Zookeeper in Github Actions")
class ZookeeperDynamicConfigurationTest {
    private static CuratorFramework client;

    private static URL configUrl;
    private static DynamicConfiguration configuration;
    private static int zookeeperServerPort1;
    private static String zookeeperConnectionAddress1;

    @BeforeAll
    public static void setUp() throws Exception {
        zookeeperConnectionAddress1 = System.getProperty("zookeeper.connection.address.1");
        zookeeperServerPort1 = Integer.parseInt(zookeeperConnectionAddress1.substring(zookeeperConnectionAddress1.lastIndexOf(":") + 1));

        client = CuratorFrameworkFactory.newClient("127.0.0.1:" + zookeeperServerPort1, 60 * 1000, 60 * 1000,
            new ExponentialBackoffRetry(1000, 3));
        client.start();

        try {
            setData("/dubbo/config/dubbo/dubbo.properties", "The content from dubbo.properties");
            setData("/dubbo/config/dubbo/service:version:group.configurators", "The content from configurators");
            setData("/dubbo/config/appname", "The content from higher level node");
            setData("/dubbo/config/dubbo/appname.tag-router", "The content from appname tagrouters");
            setData("/dubbo/config/dubbo/never.change.DemoService.configurators", "Never change value from configurators");
        } catch (Exception e) {
            e.printStackTrace();
        }

        configUrl = URL.valueOf(zookeeperConnectionAddress1);
        configuration = ExtensionLoader.getExtensionLoader(DynamicConfigurationFactory.class).getExtension(configUrl.getProtocol())
            .getDynamicConfiguration(configUrl);
    }

    private static void setData(String path, String data) throws Exception {
        if (client.checkExists().forPath(path) == null) {
            client.create().creatingParentsIfNeeded().forPath(path);
        }
        client.setData().forPath(path, data.getBytes());
    }

    private ConfigurationListener mockListener(CountDownLatch latch, String[] value, Map<String, Integer> countMap) {
        ConfigurationListener listener = Mockito.mock(ConfigurationListener.class);
        Mockito.doAnswer(invoke -> {
            ConfigChangedEvent event = invoke.getArgument(0);
            Integer count = countMap.computeIfAbsent(event.getKey(), k -> 0);
            countMap.put(event.getKey(), ++count);
            value[0] = event.getContent();
            latch.countDown();
            return null;
        }).when(listener).process(Mockito.any());
        return listener;
    }

    @Test
    void testGetConfig() {
        Assertions.assertEquals("The content from dubbo.properties", configuration.getConfig("dubbo.properties", "dubbo"));
    }

    @Test
    void testAddListener() throws Exception {
        CountDownLatch latch = new CountDownLatch(4);

        String[] value1 = new String[1], value2 = new String[1], value3 = new String[1], value4 = new String[1];
        Map<String, Integer> countMap1 = new HashMap<>(), countMap2 = new HashMap<>(), countMap3 = new HashMap<>(), countMap4 = new HashMap<>();
        ConfigurationListener listener1 = mockListener(latch, value1, countMap1);
        ConfigurationListener listener2 = mockListener(latch, value2, countMap2);
        ConfigurationListener listener3 = mockListener(latch, value3, countMap3);
        ConfigurationListener listener4 = mockListener(latch, value4, countMap4);

        configuration.addListener("service:version:group.configurators", listener1);
        configuration.addListener("service:version:group.configurators", listener2);
        configuration.addListener("appname.tag-router", listener3);
        configuration.addListener("appname.tag-router", listener4);

        Thread.sleep(100);
        setData("/dubbo/config/dubbo/service:version:group.configurators", "new value1");
        Thread.sleep(100);
        setData("/dubbo/config/dubbo/appname.tag-router", "new value2");
        Thread.sleep(100);
        setData("/dubbo/config/appname", "new value3");

        Thread.sleep(5000);

        latch.await();

        Assertions.assertEquals(1, countMap1.get("service:version:group.configurators"));
        Assertions.assertEquals(1, countMap2.get("service:version:group.configurators"));
        Assertions.assertEquals(1, countMap3.get("appname.tag-router"));
        Assertions.assertEquals(1, countMap4.get("appname.tag-router"));

        Assertions.assertEquals("new value1", value1[0]);
        Assertions.assertEquals("new value1", value2[0]);
        Assertions.assertEquals("new value2", value3[0]);
        Assertions.assertEquals("new value2", value4[0]);
    }

    @Test
    void testPublishConfig() {
        String key = "user-service";
        String group = "org.apache.dubbo.service.UserService";
        String content = "test";

        assertTrue(configuration.publishConfig(key, group, content));
        assertEquals("test", configuration.getProperties(key, group));
    }

    @Test
    void testPublishConfigCas() {
        String key = "user-service-cas";
        String group = "org.apache.dubbo.service.UserService";
        String content = "test";
        ConfigItem configItem = configuration.getConfigItem(key, group);
        assertTrue(configuration.publishConfigCas(key, group, content, configItem.getTicket()));
        configItem = configuration.getConfigItem(key, group);
        assertEquals("test", configItem.getContent());
        assertTrue(configuration.publishConfigCas(key, group, "newtest", configItem.getTicket()));
        assertFalse(configuration.publishConfigCas(key, group, "newtest2", configItem.getTicket()));
        assertEquals("newtest", configuration.getConfigItem(key, group).getContent());
    }
//
//    @Test
//    public void testGetConfigKeysAndContents() {
//
//        String group = "mapping";
//        String key = "org.apache.dubbo.service.UserService";
//        String content = "app1";
//
//        String key2 = "org.apache.dubbo.service.UserService2";
//
//        assertTrue(configuration.publishConfig(key, group, content));
//        assertTrue(configuration.publishConfig(key2, group, content));
//
//        Set<String> configKeys = configuration.getConfigKeys(group);
//
//        assertEquals(new TreeSet(asList(key, key2)), configKeys);
//    }

}