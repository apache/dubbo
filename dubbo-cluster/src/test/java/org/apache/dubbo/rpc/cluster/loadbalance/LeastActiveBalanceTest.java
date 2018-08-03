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
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class LeastActiveBalanceTest extends LoadBalanceBaseTest {
    @Ignore
    @Test
    public void testLeastActiveLoadBalance_select() {
        int runs = 10000;
        Map<Invoker, AtomicLong> counter = getInvokeCounter(runs, LeastActiveLoadBalance.NAME);
        for (Invoker minvoker : counter.keySet()) {
            Long count = counter.get(minvoker).get();
            //            System.out.println(count);
            Assert.assertTrue("abs diff shoud < avg",
                    Math.abs(count - runs / (0f + invokers.size())) < runs / (0f + invokers.size()));
        }
    }

    @Test
    public void testSelectByWeight() {
        int sumInvoker1 = 0;
        int sumInvoker2 = 0;
        int loop = 100000;

        MyLeastActiveLoadBalance lb = new MyLeastActiveLoadBalance();
        for (int i = 0; i < 100000; i++) {
            Invoker selected = lb.select(weightInvokers, null, null);

            if (selected.getUrl().getProtocol().equals("test1")) {
                sumInvoker1++;
            }

            if (selected.getUrl().getProtocol().equals("test2")) {
                sumInvoker2++;
            }
            // never select invoker3 because it's active is more than invoker1 and invoker2
            Assert.assertTrue("select is not the least active one", !selected.getUrl().getProtocol().equals("test3"));
        }

        // the sumInvoker1 : sumInvoker2 approximately equal to 1: 9
        System.out.println(sumInvoker1);
        System.out.println(sumInvoker2);

        Assert.assertEquals("select failed!", sumInvoker1 + sumInvoker2, loop);
    }

    class MyLeastActiveLoadBalance extends AbstractLoadBalance {

        private final Random random = new Random();

        @Override
        protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
            int length = invokers.size(); // Number of invokers
            int leastActive = -1; // The least active value of all invokers
            int leastCount = 0; // The number of invokers having the same least active value (leastActive)
            int[] leastIndexs = new int[length]; // The index of invokers having the same least active value (leastActive)
            int totalWeightAfterWarmUp = 0; // The sum of after warmup weights
            int firstWeightAfterWarmUp = 0; // Initial value, used for comparision
            boolean sameWeight = true; // Every invoker has the same weight value?
            for (int i = 0; i < length; i++) {
                Invoker<T> invoker = invokers.get(i);

                // mock active is invoker's url.getHost
                int active = Integer.valueOf(invoker.getUrl().getHost()); // Active number

                // mock weight is invoker's url.getPort
                int afterWarmup = invoker.getUrl().getPort();

                if (leastActive == -1 || active < leastActive) { // Restart, when find a invoker having smaller least active value.
                    leastActive = active; // Record the current least active value
                    leastCount = 1; // Reset leastCount, count again based on current leastCount
                    leastIndexs[0] = i; // Reset
                    totalWeightAfterWarmUp = afterWarmup; // Reset
                    firstWeightAfterWarmUp = afterWarmup; // Record the weight the first invoker
                    sameWeight = true; // Reset, every invoker has the same weight value?
                } else if (active == leastActive) { // If current invoker's active value equals with leaseActive, then accumulating.
                    leastIndexs[leastCount++] = i; // Record index number of this invoker
                    totalWeightAfterWarmUp += afterWarmup; // Add this invoker's after warmup weight to totalWeightAfterWarmUp.
                    // If every invoker has the same weight?
                    if (sameWeight && i > 0
                            && afterWarmup != firstWeightAfterWarmUp) {
                        sameWeight = false;
                    }
                }
            }
            // assert(leastCount > 0)
            if (leastCount == 1) {
                // If we got exactly one invoker having the least active value, return this invoker directly.
                return invokers.get(leastIndexs[0]);
            }
            if (!sameWeight && totalWeightAfterWarmUp > 0) {
                // If (not every invoker has the same weight & at least one invoker's weight>0), select randomly based on totalWeightAfterWarmUp.
                int offsetWeight = random.nextInt(totalWeightAfterWarmUp) + 1;
                // Return a invoker based on the random value.
                for (int i = 0; i < leastCount; i++) {
                    int leastIndex = leastIndexs[i];

                    // mock weight is invoker's url.getPort
                    offsetWeight -= invokers.get(leastIndex).getUrl().getPort();
                    if (offsetWeight <= 0)
                        return invokers.get(leastIndex);
                }
                // assert that at most loop 'leastCount' counts
                Assert.assertTrue("leastCount is still > 0", leastCount < 0);
            }
            // If all invokers have the same weight value or totalWeightAfterWarmUp=0, return evenly.
            return invokers.get(leastIndexs[random.nextInt(leastCount)]);
        }
    }
}
