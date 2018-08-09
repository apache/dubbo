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
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 *
 */
public class ZKTools {
    private static CuratorFramework client;

    public static void main(String[] args) {
        client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", 60 * 1000, 60 * 1000,
                new ExponentialBackoffRetry(1000, 3));
        client.start();

        testConsumerConfig();
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
}
