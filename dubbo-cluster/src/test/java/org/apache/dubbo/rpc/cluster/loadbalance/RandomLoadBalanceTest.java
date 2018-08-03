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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcStatus;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RandomLoadBalance Test
 */
public class RandomLoadBalanceTest extends LoadBalanceBaseTest {
    @Test
    public void testRandomLoadBalanceSelect() {
        int runs = 1000;
        Map<Invoker, AtomicLong> counter = getInvokeCounter(runs, RandomLoadBalance.NAME);
        for (Invoker minvoker : counter.keySet()) {
            Long count = counter.get(minvoker).get();
            Assert.assertTrue("abs diff should < avg", Math.abs(count - runs / (0f + invokers.size())) < runs / (0f + invokers.size()));
        }

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j <= i; j++) {
                RpcStatus.beginCount(invokers.get(i).getUrl(), invocation.getMethodName());
            }
        }
        counter = getInvokeCounter(runs, LeastActiveLoadBalance.NAME);
        for (Invoker minvoker : counter.keySet()) {
            Long count = counter.get(minvoker).get();
        }
        Assert.assertEquals(runs, counter.get(invoker1).intValue());
        Assert.assertEquals(0, counter.get(invoker2).intValue());
        Assert.assertEquals(0, counter.get(invoker3).intValue());
        Assert.assertEquals(0, counter.get(invoker4).intValue());
        Assert.assertEquals(0, counter.get(invoker5).intValue());
    }

    @Test
    public void testSelectByWeight() {
        int sumInvoker1 = 0;
        int sumInvoker2 = 0;
        int sumInvoker3 = 0;
        int loop = 100000;

        MyRandomLoadBalance lb = new MyRandomLoadBalance();
        for (int i = 0; i < loop; i++) {
            Invoker selected = lb.select(weightInvokers, null, null);

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
        System.out.println(sumInvoker1);
        System.out.println(sumInvoker2);
        System.out.println(sumInvoker3);
        Assert.assertEquals("select failed!", sumInvoker1 + sumInvoker2 + sumInvoker3, loop);
    }

    class MyRandomLoadBalance extends AbstractLoadBalance {

        public static final String NAME = "random";

        private final Random random = new Random();

        @Override
        protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
            int length = invokers.size(); // Number of invokers
            int totalWeight = 0; // The sum of weights
            boolean sameWeight = true; // Every invoker has the same weight?
            for (int i = 0; i < length; i++) {

                // mock weight
                int weight = invokers.get(i).getUrl().getPort();

                totalWeight += weight; // Sum
                if (sameWeight && i > 0
                        && weight != invokers.get(i - 1).getUrl().getPort()) {
                    sameWeight = false;
                }
            }
            if (totalWeight > 0 && !sameWeight) {
                // If (not every invoker has the same weight & at least one invoker's weight>0), select randomly based on totalWeight.
                int offset = random.nextInt(totalWeight);
                // Return a invoker based on the random value.
                for (int i = 0; i < length; i++) {
                    offset -= invokers.get(i).getUrl().getPort();
                    if (offset < 0) {
                        return invokers.get(i);
                    }
                }
            }
            // If all invokers have the same weight value or totalWeight=0, return evenly.
            return invokers.get(random.nextInt(length));
        }
    }

}
