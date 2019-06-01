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
package org.apache.dubbo.registry.multiple;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.redis.RedisRegistry;
import org.apache.dubbo.registry.zookeeper.ZookeeperRegistry;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.curator.CuratorZookeeperClient;

import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.embedded.RedisServer;

import java.util.ArrayList;
import java.util.List;

/**
 * 2019-04-30
 */
public class MultipleRegistry2S2RTest {

    private static final String SERVICE_NAME = "org.apache.dubbo.registry.MultipleService2S2R";
    private static final String SERVICE2_NAME = "org.apache.dubbo.registry.MultipleService2S2R2";

    private static TestingServer zkServer;
    private static RedisServer redisServer;
    static int zkServerPort;
    static int redisServerPort;

    private static String zookeeperRegistryURLStr;
    private static String redisRegistryURLStr;

    private static MultipleRegistry multipleRegistry;
    // for test content
    private static ZookeeperClient zookeeperClient;

    private static ZookeeperRegistry zookeeperRegistry;
    private static RedisRegistry redisRegistry;


    @BeforeAll
    public static void setUp() throws Exception {
        zkServerPort = NetUtils.getAvailablePort();
        zkServer = new TestingServer(zkServerPort, true);
        zookeeperRegistryURLStr = "zookeeper://127.0.0.1:" + zkServerPort;

        redisServerPort = NetUtils.getAvailablePort();
        redisServer = new RedisServer(redisServerPort);
        redisServer.start();
        redisRegistryURLStr = "redis://127.0.0.1:" + redisServerPort;


        URL url = URL.valueOf("multiple://127.0.0.1?application=vic&" +
                MultipleRegistry.REGISTRY_FOR_SERVICE + "=" + zookeeperRegistryURLStr + "," + redisRegistryURLStr + "&"
                + MultipleRegistry.REGISTRY_FOR_REFERENCE + "=" + zookeeperRegistryURLStr + "," + redisRegistryURLStr);
        multipleRegistry = (MultipleRegistry) new MultipleRegistryFactory().createRegistry(url);

        // for test validation
        zookeeperClient = new CuratorZookeeperClient(URL.valueOf(zookeeperRegistryURLStr));
        zookeeperRegistry = MultipleRegistryTestUtil.getZookeeperRegistry(multipleRegistry.getServiceRegistries().values());
        redisRegistry = MultipleRegistryTestUtil.getRedisRegistry(multipleRegistry.getServiceRegistries().values());
    }

    @AfterAll
    public static void tearDown() throws Exception {
        zkServer.stop();
        redisServer.stop();
    }

    @Test
    public void testParamConfig() {

        Assertions.assertEquals(2, multipleRegistry.origReferenceRegistryURLs.size());
        Assertions.assertTrue(multipleRegistry.origReferenceRegistryURLs.contains(zookeeperRegistryURLStr));
        Assertions.assertTrue(multipleRegistry.origReferenceRegistryURLs.contains(redisRegistryURLStr));

        Assertions.assertEquals(2, multipleRegistry.origServiceRegistryURLs.size());
        Assertions.assertTrue(multipleRegistry.origServiceRegistryURLs.contains(zookeeperRegistryURLStr));
        Assertions.assertTrue(multipleRegistry.origServiceRegistryURLs.contains(redisRegistryURLStr));

        Assertions.assertEquals(2, multipleRegistry.effectReferenceRegistryURLs.size());
        Assertions.assertTrue(multipleRegistry.effectReferenceRegistryURLs.contains(zookeeperRegistryURLStr));
        Assertions.assertTrue(multipleRegistry.effectReferenceRegistryURLs.contains(redisRegistryURLStr));

        Assertions.assertEquals(2, multipleRegistry.effectServiceRegistryURLs.size());
        Assertions.assertTrue(multipleRegistry.effectServiceRegistryURLs.contains(zookeeperRegistryURLStr));
        Assertions.assertTrue(multipleRegistry.effectServiceRegistryURLs.contains(redisRegistryURLStr));

        Assertions.assertTrue(multipleRegistry.getServiceRegistries().containsKey(zookeeperRegistryURLStr));
        Assertions.assertTrue(multipleRegistry.getServiceRegistries().containsKey(redisRegistryURLStr));
        Assertions.assertEquals(2, multipleRegistry.getServiceRegistries().values().size());
//        java.util.Iterator<Registry> registryIterable = multipleRegistry.getServiceRegistries().values().iterator();
//        Registry firstRegistry = registryIterable.next();
//        Registry secondRegistry = registryIterable.next();
        Assertions.assertNotNull(MultipleRegistryTestUtil.getZookeeperRegistry(multipleRegistry.getServiceRegistries().values()));
        Assertions.assertNotNull(MultipleRegistryTestUtil.getRedisRegistry(multipleRegistry.getServiceRegistries().values()));
        Assertions.assertNotNull(MultipleRegistryTestUtil.getZookeeperRegistry(multipleRegistry.getReferenceRegistries().values()));
        Assertions.assertNotNull(MultipleRegistryTestUtil.getRedisRegistry(multipleRegistry.getReferenceRegistries().values()));

        Assertions.assertEquals(MultipleRegistryTestUtil.getZookeeperRegistry(multipleRegistry.getServiceRegistries().values()),
                MultipleRegistryTestUtil.getZookeeperRegistry(multipleRegistry.getReferenceRegistries().values()));

        Assertions.assertEquals(MultipleRegistryTestUtil.getRedisRegistry(multipleRegistry.getServiceRegistries().values()),
                MultipleRegistryTestUtil.getRedisRegistry(multipleRegistry.getReferenceRegistries().values()));

        Assertions.assertEquals(multipleRegistry.getApplicationName(), "vic");

        Assertions.assertTrue(multipleRegistry.isAvailable());
    }

    @Test
    public void testRegistryAndUnRegistry() throws InterruptedException {
        URL serviceUrl = URL.valueOf("http2://multiple/" + SERVICE_NAME + "?notify=false&methods=test1,test2&category=providers");
//        URL serviceUrl2 = URL.valueOf("http2://multiple2/" + SERVICE_NAME + "?notify=false&methods=test1,test2&category=providers");
        multipleRegistry.register(serviceUrl);

        String path = "/dubbo/" + SERVICE_NAME + "/providers";
        List<String> providerList = zookeeperClient.getChildren(path);
        Assertions.assertTrue(!providerList.isEmpty());
        System.out.println(providerList.get(0));

        Assertions.assertNotNull(MultipleRegistryTestUtil.getRedisHashContent(redisServerPort, path, serviceUrl.toFullString()));

        final List<URL> list = new ArrayList<URL>();
        multipleRegistry.subscribe(serviceUrl, new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                System.out.println("invoke notify: " + urls);
                list.clear();
                list.addAll(urls);
            }
        });
        Thread.sleep(1500);
        Assertions.assertEquals(2, list.size());

        multipleRegistry.unregister(serviceUrl);
        Thread.sleep(1500);
        Assertions.assertEquals(1, list.size());
        List<URL> urls = MultipleRegistryTestUtil.getProviderURLsFromNotifyURLS(list);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("empty", list.get(0).getProtocol());
    }

    @Test
    public void testSubscription() throws InterruptedException {
        URL serviceUrl = URL.valueOf("http2://multiple/" + SERVICE2_NAME + "?notify=false&methods=test1,test2&category=providers");
//        URL serviceUrl2 = URL.valueOf("http2://multiple2/" + SERVICE_NAME + "?notify=false&methods=test1,test2&category=providers");
        multipleRegistry.register(serviceUrl);

        String path = "/dubbo/" + SERVICE2_NAME + "/providers";
        List<String> providerList = zookeeperClient.getChildren(path);
        Assertions.assertTrue(!providerList.isEmpty());
        System.out.println(providerList.get(0));

        Assertions.assertNotNull(MultipleRegistryTestUtil.getRedisHashContent(redisServerPort, path, serviceUrl.toFullString()));

        final List<URL> list = new ArrayList<URL>();
        multipleRegistry.subscribe(serviceUrl, new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                System.out.println("invoke notify: " + urls);
                list.clear();
                list.addAll(urls);
            }
        });
        Thread.sleep(1500);
        Assertions.assertEquals(2, list.size());

        List<Registry> serviceRegistries = new ArrayList<Registry>(multipleRegistry.getServiceRegistries().values());
        serviceRegistries.get(0).unregister(serviceUrl);
        Thread.sleep(1500);
        Assertions.assertEquals(1, list.size());
        List<URL> urls = MultipleRegistryTestUtil.getProviderURLsFromNotifyURLS(list);
        Assertions.assertEquals(1, list.size());
        Assertions.assertTrue(!"empty".equals(list.get(0).getProtocol()));

        serviceRegistries.get(1).unregister(serviceUrl);
        Thread.sleep(1500);
        Assertions.assertEquals(1, list.size());
        urls = MultipleRegistryTestUtil.getProviderURLsFromNotifyURLS(list);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("empty", list.get(0).getProtocol());
    }

}
