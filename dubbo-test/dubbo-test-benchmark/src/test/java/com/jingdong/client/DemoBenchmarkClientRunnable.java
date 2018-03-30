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
package com.jingdong.client;

import com.alibaba.dubbo.rpc.benchmark.AbstractClientRunnable;
import com.alibaba.dubbo.rpc.benchmark.DemoService;
import com.alibaba.dubbo.rpc.benchmark.ServiceFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * DemoBenchmarkClient.java
 */
public class DemoBenchmarkClientRunnable extends AbstractClientRunnable {

    public DemoBenchmarkClientRunnable(String targetIP, int targetPort, int clientNums, int rpcTimeout,
                                       CyclicBarrier barrier, CountDownLatch latch, long startTime,
                                       long endTime) {
        super(targetIP, targetPort, clientNums, rpcTimeout, barrier, latch, startTime, endTime);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Object invoke(ServiceFactory serviceFactory) {
        DemoService demoService = (DemoService) serviceFactory.get(DemoService.class);
        return demoService.sendRequest("hello");
    }

}
