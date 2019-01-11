/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License")); you may not use this file except in compliance with
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RandomLoadBalance Test
 */
public class RandomLoadBalanceTest extends LoadBalanceBaseTest {
    @Test
    public void testRandomLoadBalanceSelect() {
        int runs = 1000;
        Map<Invoker, AtomicLong> counter = getInvokeCounter(runs, RandomLoadBalance.NAME);
        for (Map.Entry<Invoker, AtomicLong> entry : counter.entrySet()) {
            Long count = entry.getValue().get();
            Assertions.assertTrue(Math.abs(count - runs / (0f + invokers.size())) < runs / (0f + invokers.size()), "abs diff should < avg");
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
    public void testSelectByWeight() {
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
        System.out.println(sumInvoker1 + " " + sumInvoker2 + " " + sumInvoker3);
        Assert.assertEquals("select failed!", sumInvoker1 + sumInvoker2 + sumInvoker3, loop);
    }

    @Test
    public void testNew() {

        for (int i = 1; i < 30; i++) {
            testBinary(i);
        }

      /*old cost11ms	new cost0ms
        old cost17ms	new cost0ms
        old cost10ms	new cost0ms
        old cost6ms	    new cost0ms
        old cost4ms	    new cost0ms
        old cost3ms	    new cost0ms
        old cost15ms	new cost0ms
        old cost40ms	new cost0ms
        old cost20ms	new cost0ms
        old cost2ms	    new cost0ms
        old cost8ms	    new cost0ms
        old cost10ms	new cost0ms
        old cost3ms	    new cost0ms
        old cost9ms	    new cost0ms
        old cost5ms	    new cost0ms
        old cost4ms	    new cost0ms
        old cost7ms	    new cost0ms
        old cost10ms	new cost0ms
        old cost5ms	    new cost0ms
        old cost6ms	    new cost0ms
        old cost5ms	    new cost0ms
        old cost5ms	    new cost0ms
        old cost6ms	    new cost0ms
        old cost7ms	    new cost0ms
        old cost5ms	    new cost0ms
        old cost3ms	    new cost0ms
        old cost8ms	    new cost0ms
        old cost9ms	    new cost0ms
        old cost9ms	    new cost0ms*/

        for (int i = 0; i < 10; i++) {
            testSelectByWeight();
        }
/*      660 5574 3766
        635 5589 3776
        621 5564 3815
        648 5620 3732
        603 5646 3751
        600 5685 3715
        617 5666 3717
        643 5644 3713
        641 5623 3736
        615 5648 3737*/
    }

    private void testBinary(int length) {
        int[] weights = new int[length];
        int[] weightArr = new int[length + 1];
        int totalWeight = 0;
        int loop = 100000;
        weightArr[0] = 0;
        for (int i = 0; i < length; i++) {
            weights[i] = ThreadLocalRandom.current().nextInt(10);
            totalWeight += weights[i];
            weightArr[i + 1] = totalWeight;
        }
        long oldCost = 0L, newCost = 0L;
        for (int i = 0; i < loop; i++) {
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            int findOffset = offset;
            int oldResult = -1;
            //use loop
            long start = System.currentTimeMillis();
            for (int j = 0; j < length; j++) {
                offset -= weights[j];
                if (offset < 0) {
                    oldResult = j;
                    break;
                }
            }
            long endFirst = System.currentTimeMillis();
            //use Binary Search
            int newResult = -1;
            int low = 0, high = length, mid;
            while (low <= high) {
                mid = (low + high) / 2;
                if (findOffset == weightArr[mid]) {
                    newResult = mid;
                    while (++mid <= length && weightArr[mid] == findOffset) {
                        newResult = mid;
                    }
                    break;
                }

                if (mid != length && findOffset > weightArr[mid] && findOffset < weightArr[mid + 1]) {
                    newResult = mid;
                    break;
                }

                if (findOffset < weightArr[mid]) {
                    high = mid - 1;
                    continue;
                }
                if (findOffset > weightArr[mid]) {
                    low = mid + 1;
                }
            }
            long endSecond = System.currentTimeMillis();
            //compare two result
            Assert.assertEquals(oldResult, newResult);
            //calc cost time
            oldCost += endFirst - start;
            newCost = endSecond - start;
        }
        System.out.println("old cost" + oldCost + "ms" + "\t" + "new cost" + newCost + "ms");
    }
}
