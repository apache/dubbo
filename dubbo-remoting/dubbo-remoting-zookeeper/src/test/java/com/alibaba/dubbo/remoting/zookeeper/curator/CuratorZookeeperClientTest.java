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
package com.alibaba.dubbo.remoting.zookeeper.curator;

import com.alibaba.dubbo.common.URL;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @date 2017/10/16
 */
@Ignore
public class CuratorZookeeperClientTest {

    @Test
    public void testCheckExists() {
        CuratorZookeeperClient curatorClient = new CuratorZookeeperClient(URL.valueOf("zookeeper://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService"));
        String path = "/dubbo/com.alibaba.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false);
        Assert.assertTrue(curatorClient.checkExists(path));
        Assert.assertFalse(curatorClient.checkExists(path + "/noneexits"));
    }

    /**
     * create checkExists performance test
     */
    @Test
    public void testCreate() {
        CuratorZookeeperClient curatorClient = new CuratorZookeeperClient(URL.valueOf("zookeeper://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService"));
        String path = "/dubbo/com.alibaba.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false);

        // Repeated execution of create 100 times
        long startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            curatorClient.create(path, true);
        }
        System.out.println("create cost: " + (System.nanoTime() - startTime) / 1000 / 1000);

        //The time of the 100 judgment
        startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            curatorClient.checkExists(path);
        }
        System.out.println("judge cost: " + (System.nanoTime() - startTime) / 1000 / 1000);
    }
}
