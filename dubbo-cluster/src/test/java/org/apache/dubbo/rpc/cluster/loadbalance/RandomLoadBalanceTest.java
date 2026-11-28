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
package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcStatus;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * RandomLoadBalance Test
 */
class RandomLoadBalanceTest extends LoadBalanceBaseTest {
    @Test
    void testRandomLoadBalanceSelect() {
        int runs = 1000;
        Map<Invoker, AtomicLong> counter = getInvokeCounter(runs, RandomLoadBalance.NAME);
        for (Map.Entry<Invoker, AtomicLong> entry : counter.entrySet()) {
            Long count = entry.getValue().get();
            Assertions.assertTrue(
                    Math.abs(count - runs / (0f + invokers.size())) < runs / (0f + invokers.size()),
                    "abs diff should < avg");
        }

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j <= i; j++) {
                RpcStatus.beginCount(invokers.get(i).getUrl(), invocation.getMethodName());
            }
        }
        counter = getInvokeCounter(runs, LeastActiveLoadBalance.NAME);
        for (Map.Entry<Invoker, AtomicLong> entry : counter.entrySet()) {
            Long count = entry.getValue().get();
        }
        Assertions.assertEquals(runs, counter.get(invoker1).intValue());
        Assertions.assertEquals(0, counter.get(invoker2).intValue());
        Assertions.assertEquals(0, counter.get(invoker3).intValue());
        Assertions.assertEquals(0, counter.get(invoker4).intValue());
        Assertions.assertEquals(0, counter.get(invoker5).intValue());
    }

    @Test
    void testSelectByWeight() {
        int sumInvoker1 = 0;
        int sumInvoker2 = 0;
        int sumInvoker3 = 0;
        int loop = 10000;

        RandomLoadBalance lb = new RandomLoadBalance();
        for (int i = 0; i < loop; i++) {
            Invoker selected = lb.select(weightInvokers, null, weightTestInvocation);

            if (selected.getUrl().getProtocol().equals("test1")) {
                sumInvoker1++;
            }

            if (selected.getUrl().getProtocol().equals("test2")) {
                sumInvoker2++;
            }

            if (selected.getUrl().getProtocol().equals("test3")) {
                sumInvoker3++;
            }
        }

        // 1 : 9 : 6
        Assertions.assertEquals(sumInvoker1 + sumInvoker2 + sumInvoker3, loop, "select failed!");
    }

    @Test
    void testSelectByWeightWithBitList() {
        int loop = 10000;
        int sumInvoker1 = 0;
        int sumInvoker2 = 0;
        int sumInvoker3 = 0;

        RandomLoadBalance lb = new RandomLoadBalance();
        // BitList#get(index) is O(index), make sure weighted selection still behaves correctly on it.
        BitList<Invoker<LoadBalanceBaseTest>> bitList = new BitList<>(weightInvokers);
        for (int i = 0; i < loop; i++) {
            Invoker selected = lb.select(bitList, null, weightTestInvocation);

            if (selected.getUrl().getProtocol().equals("test1")) {
                sumInvoker1++;
            }

            if (selected.getUrl().getProtocol().equals("test2")) {
                sumInvoker2++;
            }

            if (selected.getUrl().getProtocol().equals("test3")) {
                sumInvoker3++;
            }
        }

        // 1 : 9 : 6
        Assertions.assertEquals(sumInvoker1 + sumInvoker2 + sumInvoker3, loop, "select failed!");
        Assertions.assertEquals(0.0625d, sumInvoker1 * 1.0d / loop, 0.03d);
        Assertions.assertEquals(0.5625d, sumInvoker2 * 1.0d / loop, 0.03d);
        Assertions.assertEquals(0.3750d, sumInvoker3 * 1.0d / loop, 0.03d);
    }

    /**
     * The list is large enough that reading it by index once per invoker is detectable: with 1024 invokers
     * the index-based loop reads 1024 entries per selection instead of a constant number.
     */
    @Test
    void testSelectByWeightWithLargeBitList() {
        int size = 1024;
        int loop = 1000;
        CountingBitList<Invoker<LoadBalanceBaseTest>> bitList = buildWeightBitList(size);

        RandomLoadBalance lb = new RandomLoadBalance();
        int sumInvoker1 = 0;
        int sumInvoker2 = 0;
        int sumInvoker3 = 0;

        for (int i = 0; i < loop; i++) {
            Invoker selected = lb.select(bitList, null, weightTestInvocation);

            if (selected.getUrl().getProtocol().equals("test1")) {
                sumInvoker1++;
            }

            if (selected.getUrl().getProtocol().equals("test2")) {
                sumInvoker2++;
            }

            if (selected.getUrl().getProtocol().equals("test3")) {
                sumInvoker3++;
            }
        }

        // 1 : 2 : 3
        Assertions.assertEquals(sumInvoker1 + sumInvoker2 + sumInvoker3, loop, "select failed!");
        Assertions.assertEquals(1.0d / 6, sumInvoker1 * 1.0d / loop, 0.07d);
        Assertions.assertEquals(2.0d / 6, sumInvoker2 * 1.0d / loop, 0.07d);
        Assertions.assertEquals(3.0d / 6, sumInvoker3 * 1.0d / loop, 0.07d);
        Assertions.assertTrue(
                bitList.getIndexAccesses() < loop * 3L,
                "BitList accessed by index " + bitList.getIndexAccesses()
                        + " times, a selection must not read it once per invoker");
    }
}
