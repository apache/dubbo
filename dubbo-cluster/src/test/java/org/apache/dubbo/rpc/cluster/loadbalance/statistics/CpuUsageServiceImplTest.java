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
package org.apache.dubbo.rpc.cluster.loadbalance.statistics;

import org.apache.dubbo.common.config.ConfigurationUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


class CpuUsageServiceImplTest {

    @Test
    public void testCallBack() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<Float> results = new ArrayList<>();
        long timeToLive = Long.parseLong(ConfigurationUtils.getProperty("time.to.live"));
        long collectCpuUsageInMill = Long.parseLong(ConfigurationUtils.getProperty("mill.to.collect.cpu.usage"));
        CpuUsageServiceImpl service = new CpuUsageServiceImpl(timeToLive, collectCpuUsageInMill);
        service.addListener("foo.bar", (ip, cpu) -> {
            results.add(cpu);
            latch.countDown();
        });

        latch.await(4, TimeUnit.SECONDS);
        Assertions.assertFalse(results.isEmpty());
    }
}