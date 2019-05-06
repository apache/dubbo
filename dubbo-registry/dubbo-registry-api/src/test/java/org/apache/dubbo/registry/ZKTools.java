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
package org.apache.dubbo.registry;

import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class ZKTools {
    private static CuratorFramework client;
    private static ExecutorService executor = Executors.newFixedThreadPool(1, new NamedThreadFactory("ZKTools-test", true));

    public static void main(String[] args) throws Exception {
        client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", 60 * 1000, 60 * 1000,
                new ExponentialBackoffRetry(1000, 3));
        client.start();

        client.getCuratorListenable().addListener(new CuratorListener() {
            @Override
            public void eventReceived(CuratorFramework client, CuratorEvent event) throws Exception {
                System.out.println("event notification: " + event.getPath());
                System.out.println(event);
            }
        }, executor);

        tesConditionRule();

//        testStartupConfig();
//        testProviderConfig();
//        testPathCache();
//        testTreeCache();
//        testCuratorListener();
//       Thread.sleep(100000);
    }

    public static void testStartupConfig() {
        String str = "dubbo.registry.address=zookeeper://127.0.0.1:2181\n" +
                "dubbo.registry.group=dubboregistrygroup1\n" +
                "dubbo.metadata-report.address=zookeeper://127.0.0.1:2181\n" +
                "dubbo.protocol.port=20990\n" +
                "dubbo.service.org.apache.dubbo.demo.DemoService.timeout=9999\n";

//        System.out.println(str);

        try {
            String path = "/dubboregistrygroup1/config/dubbo/dubbo.properties";
            if (client.checkExists().forPath(path) == null) {
                client.create().creatingParentsIfNeeded().forPath(path);
            }
            setData(path, str);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testProviderConfig() {
        String str = "---\n" +
                "apiVersion: v2.7\n" +
                "scope: service\n" +
                "key: dd-test/org.apache.dubbo.demo.DemoService:1.0.4\n" +
                "enabled: true\n" +
                "configs:\n" +
                "- addresses: ['0.0.0.0:20880']\n" +
                "  side: provider\n" +
                "  parameters:\n" +
                "    timeout: 6000\n" +
                "...";

//        System.out.println(str);

        try {
            String path = "/dubbo/config/dd-test*org.apache.dubbo.demo.DemoService:1.0.4/configurators";
            if (client.checkExists().forPath(path) == null) {
                client.create().creatingParentsIfNeeded().inBackground().forPath(path);
            }
            setData(path, str);

            String pathaa = "/dubboregistrygroup1/config/aaa/dubbo.properties";
            if (client.checkExists().forPath(pathaa) == null) {
                client.create().creatingParentsIfNeeded().forPath(pathaa);
            }
            setData(pathaa, "aaaa");

            String pathaaa = "/dubboregistrygroup1/config/aaa";
            if (client.checkExists().forPath(pathaaa) == null) {
                client.create().creatingParentsIfNeeded().inBackground().forPath(pathaaa);
            }
            setData(pathaaa, "aaaa");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testConsumerConfig() {
        String serviceStr = "---\n" +
                "scope: service\n" +
                "key: org.apache.dubbo.demo.DemoService\n" +
                "configs:\n" +
                " - addresses: [30.5.121.156]\n" +
                "   side: consumer\n" +
                "   rules:\n" +
                "    cluster:\n" +
                "     loadbalance: random\n" +
                "     cluster: failfast\n" +
                "    config:\n" +
                "     timeout: 9999\n" +
                "     weight: 222\n" +
                "...";
        String appStr = "---\n" +
                "scope: application\n" +
                "key: demo-consumer\n" +
                "configs:\n" +
                " - addresses: [30.5.121.156]\n" +
                "   services: [org.apache.dubbo.demo.DemoService]\n" +
                "   side: consumer\n" +
                "   rules:\n" +
                "    cluster:\n" +
                "     loadbalance: random\n" +
                "     cluster: failfast\n" +
                "    config:\n" +
                "     timeout: 4444\n" +
                "     weight: 222\n" +
                "...";
        try {
            String servicePath = "/dubbo/config/org.apache.dubbo.demo.DemoService/configurators";
            if (client.checkExists().forPath(servicePath) == null) {
                client.create().creatingParentsIfNeeded().forPath(servicePath);
            }
            setData(servicePath, serviceStr);

            String appPath = "/dubbo/config/demo-consumer/configurators";
            if (client.checkExists().forPath(appPath) == null) {
                client.create().creatingParentsIfNeeded().forPath(appPath);
            }
            setData(appPath, appStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void tesConditionRule() {
        String serviceStr = "---\n" +
                "scope: application\n" +
                "force: true\n" +
                "runtime: false\n" +
                "conditions:\n" +
                "  - method!=sayHello =>\n" +
                "  - method=routeMethod1 => 30.5.121.156:20880\n" +
                "...";
        try {
            String servicePath = "/dubbo/config/demo-consumer/routers";
            if (client.checkExists().forPath(servicePath) == null) {
                client.create().creatingParentsIfNeeded().forPath(servicePath);
            }
            setData(servicePath, serviceStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setData(String path, String data) throws Exception {
        client.setData().inBackground().forPath(path, data.getBytes());
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
