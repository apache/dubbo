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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("rawtypes")
class ConsistentHashLoadBalanceTest extends LoadBalanceBaseTest {

    @Test
    void testConsistentHashLoadBalanceInGenericCall() {
        int runs = 10000;
        Map<Invoker, AtomicLong> genericInvokeCounter = getGenericInvokeCounter(runs, ConsistentHashLoadBalance.NAME);
        Map<Invoker, AtomicLong> invokeCounter = getInvokeCounter(runs, ConsistentHashLoadBalance.NAME);

        Invoker genericHitted = findHitted(genericInvokeCounter);
        Invoker hitted = findHitted(invokeCounter);

        Assertions.assertEquals(hitted,
            genericHitted, "hitted should equals to genericHitted");
    }

    private Invoker findHitted(Map<Invoker,AtomicLong> invokerCounter) {
        Invoker invoker = null;

        for (Map.Entry<Invoker,AtomicLong> entry : invokerCounter.entrySet()) {
            if (entry.getValue().longValue() > 0) {
                invoker = entry.getKey();
                break;
            }
        }

        Assertions.assertNotNull(invoker,"invoker should be found");

        return null;
    }

    @Test
    void testConsistentHashLoadBalance() {
        int runs = 10000;
        long unHitedInvokerCount = 0;
        Map<Invoker, Long> hitedInvokers = new HashMap<>();
        Map<Invoker, AtomicLong> counter = getInvokeCounter(runs, ConsistentHashLoadBalance.NAME);
        for (Invoker minvoker : counter.keySet()) {
            Long count = counter.get(minvoker).get();

            if (count == 0) {
                unHitedInvokerCount++;
            } else {
                hitedInvokers.put(minvoker, count);
            }
        }

        Assertions.assertEquals(counter.size() - 1,
            unHitedInvokerCount, "the number of unHitedInvoker should be counter.size() - 1");
        Assertions.assertEquals(1, hitedInvokers.size(), "the number of hitedInvoker should be 1");
        Assertions.assertEquals(runs,
            hitedInvokers.values().iterator().next().intValue(), "the number of hited count should be the number of runs");
    }

    // https://github.com/apache/dubbo/issues/5429
    @Test
    void testNormalWhenRouterEnabled() {
        LoadBalance lb = getLoadBalance(ConsistentHashLoadBalance.NAME);
        URL url = invokers.get(0).getUrl();
        RouterChain<LoadBalanceBaseTest> routerChain = RouterChain.buildChain(LoadBalanceBaseTest.class, url);
        Invoker<LoadBalanceBaseTest> result = lb.select(invokers, url, invocation);

        for (int i = 0; i < 100; i++) {
            routerChain.setInvokers(new BitList<>(invokers), () -> {});
            List<Invoker<LoadBalanceBaseTest>> routeInvokers = routerChain.getSingleChain(url, new BitList<>(invokers), invocation)
                .route(url, new BitList<>(invokers), invocation);
            Invoker<LoadBalanceBaseTest> finalInvoker = lb.select(routeInvokers, url, invocation);
            Assertions.assertEquals(result, finalInvoker);
        }
    }
}
