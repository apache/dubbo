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
package org.apache.dubbo.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

/**
 *
 */
public class ZKTools {
    private static CuratorFramework client;

    public static void main(String[] args) throws Exception {
        client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", 60 * 1000, 60 * 1000,
                new ExponentialBackoffRetry(1000, 3));
        client.start();

//        testConsumerConfig();
//        testPathCache();
        testTreeCache();
//        testCuratorListener();
        System.in.read();
    }

    public static void testProviderConfig() {
        String str = "{\n" +
                "\t\"service\": \"org.apache.dubbo.demo.DemoService\",\n" +
                "\t\"items\": [{\n" +
                "\t\t\"addresses\": [\"30.5.120.49\"],\n" +
                "\t\t\"rules\": [{\n" +
                "\t\t\t\"key\": \"weight\",\n" +
                "\t\t\t\"value\": 500\n" +
                "\t\t}],\n" +
                "\t\t\"app\": \"demo-provider\",\n" +
                "\t\t\"side\": \"provider\"\n" +
                "\t}]\n" +
                "}";
        try {
            String path = "/dubbo/config/demo-provider/org.apache.dubbo.demo.DemoService.CONFIGURATORS";
            if (client.checkExists().forPath(path) == null) {
                client.create().creatingParentsIfNeeded().forPath(path);
            }
            setData(path, str);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testConsumerConfig() {
        String str = "{\n" +
                "\t\"service\": \"org.apache.dubbo.demo.DemoService\",\n" +
                "\t\"items\": [{\n" +
                "\t\t\"addresses\": [\"30.5.120.48\"],\n" +
                "\t\t\"rules\": [{\n" +
                "\t\t\t\"key\": \"loadbalance\",\n" +
                "\t\t\t\"value\": \"roundrobin\"\n" +
                "\t\t}],\n" +
                "\t\t\"app\": \"demo-consumer\",\n" +
                "\t\t\"side\": \"consumer\"\n" +
                "\t}]\n" +
                "}";
        try {
            String path = "/dubbo/config/demo-consumer/org.apache.dubbo.demo.DemoService.CONFIGURATORS";
            if (client.checkExists().forPath(path) == null) {
                client.create().creatingParentsIfNeeded().forPath(path);
            }
            setData(path, str);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setData(String path, String data) throws Exception {
        client.setData().forPath(path, data.getBytes());
    }

    public static void testPathCache() throws Exception {
        CuratorFramework client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", 60 * 1000, 60 * 1000,
                new ExponentialBackoffRetry(1000, 3));
        client.start();
        PathChildrenCache pathChildrenCache = new PathChildrenCache(client, "/dubbo/config", true);
        pathChildrenCache.start(true);
        pathChildrenCache.getListenable().addListener((zkClient, event) -> {
            System.out.println(event.getData().getPath());
        }, Executors.newFixedThreadPool(1));
        List<ChildData> dataList = pathChildrenCache.getCurrentData();
        dataList.stream().map(ChildData::getPath).forEach(System.out::println);
    }

    public static void testTreeCache() throws Exception {
        CuratorFramework client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", 60 * 1000, 60 * 1000,
                new ExponentialBackoffRetry(1000, 3));
        client.start();

        CountDownLatch latch = new CountDownLatch(1);

        TreeCache treeCache = TreeCache.newBuilder(client, "/dubbo/config").setCacheData(true).build();
        treeCache.start();
        treeCache.getListenable().addListener(new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {

                TreeCacheEvent.Type type = event.getType();
                ChildData data = event.getData();

                if (type == TreeCacheEvent.Type.INITIALIZED) {
                    latch.countDown();
                }

                System.out.println(data.getPath() + "\n");

                if (data.getPath().split("/").length == 5) {
                    byte[] value = data.getData();
                    String stringValue = new String(value, "utf-8");

                    // fire event to all listeners
                    Map<String, Object> added = null;
                    Map<String, Object> changed = null;
                    Map<String, Object> deleted = null;

                    switch (type) {
                        case NODE_ADDED:
                            added = new HashMap<>(1);
                            added.put(pathToKey(data.getPath()), stringValue);
                            added.forEach((k, v) -> System.out.println(k + "  " + v));
                            break;
                        case NODE_REMOVED:
                            deleted = new HashMap<>(1);
                            deleted.put(pathToKey(data.getPath()), stringValue);
                            deleted.forEach((k, v) -> System.out.println(k + "  " + v));
                            break;
                        case NODE_UPDATED:
                            changed = new HashMap<>(1);
                            changed.put(pathToKey(data.getPath()), stringValue);
                            changed.forEach((k, v) -> System.out.println(k + "  " + v));
                    }
                }
            }
        });

        latch.await();

       /* Map<String, ChildData> dataMap = treeCache.getCurrentChildren("/dubbo/config");
        dataMap.forEach((k, v) -> {
            System.out.println(k);
            treeCache.getCurrentChildren("/dubbo/config/" + k).forEach((ck, cv) -> {
                System.out.println(ck);
            });
        });*/
    }

    private static String pathToKey(String path) {
        if (StringUtils.isEmpty(path)) {
            return path;
        }
        return path.replace("/dubbo/config/", "").replaceAll("/", ".");
    }

    public static void testCuratorListener() throws Exception {
        CuratorFramework client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", 60 * 1000, 60 * 1000,
                new ExponentialBackoffRetry(1000, 3));
        client.start();

        List<String> children = client.getChildren().forPath("/dubbo/config");
        children.forEach(System.out::println);
/*

        client.getCuratorListenable().addListener(new CuratorListener() {
            @Override
            public void eventReceived(CuratorFramework curatorFramework, CuratorEvent curatorEvent) throws Exception {
                curatorEvent.get
            }
        });
*/

        /*client.getChildren().usingWatcher(new CuratorWatcher() {
            @Override
            public void process(WatchedEvent watchedEvent) throws Exception {
                System.out.println(watchedEvent.getPath());
                client.getChildren().usingWatcher(this).forPath("/dubbo/config");
                System.out.println(watchedEvent.getWrapper().getPath());
            }
        }).forPath("/dubbo/config");*/
    }
}
