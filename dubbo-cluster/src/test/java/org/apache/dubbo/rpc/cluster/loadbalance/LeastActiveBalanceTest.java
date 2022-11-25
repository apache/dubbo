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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

class LeastActiveBalanceTest extends LoadBalanceBaseTest {
    @Disabled
    @Test
    void testLeastActiveLoadBalance_select() {
        int runs = 10000;
        Map<Invoker, AtomicLong> counter = getInvokeCounter(runs, LeastActiveLoadBalance.NAME);
        for (Map.Entry<Invoker, AtomicLong> entry : counter.entrySet()) {
            Long count = entry.getValue().get();
            //            System.out.println(count);
            Assertions.assertTrue(
                    Math.abs(count - runs / (0f + invokers.size())) < runs / (0f + invokers.size()), "abs diff should < avg");
        }
    }

    @Test
    void testSelectByWeight() {
        int sumInvoker1 = 0;
        int sumInvoker2 = 0;
        int loop = 10000;

        LeastActiveLoadBalance lb = new LeastActiveLoadBalance();
        for (int i = 0; i < loop; i++) {
            Invoker selected = lb.select(weightInvokers, null, weightTestInvocation);

            if (selected.getUrl().getProtocol().equals("test1")) {
                sumInvoker1++;
            }

            if (selected.getUrl().getProtocol().equals("test2")) {
                sumInvoker2++;
            }
            // never select invoker3 because it's active is more than invoker1 and invoker2
            Assertions.assertTrue(!selected.getUrl().getProtocol().equals("test3"), "select is not the least active one");
        }

        // the sumInvoker1 : sumInvoker2 approximately equal to 1: 9
        System.out.println(sumInvoker1);
        System.out.println(sumInvoker2);

        Assertions.assertEquals(sumInvoker1 + sumInvoker2, loop, "select failed!");
    }
}
