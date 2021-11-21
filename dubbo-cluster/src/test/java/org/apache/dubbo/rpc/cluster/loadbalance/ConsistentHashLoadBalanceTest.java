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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("rawtypes")
public class ConsistentHashLoadBalanceTest extends LoadBalanceBaseTest {

    @Test
    public void testConsistentHashLoadBalance() {
        int runs = 10000;
        Map<Invoker, AtomicLong> counter = getInvokeCounter(runs, ConsistentHashLoadBalance.NAME);
        double overloadRatioAllowed = 1.5F;
        int serverCount = counter.size();
        double overloadThread = ((double) runs * overloadRatioAllowed)/((double) serverCount);
        for (Invoker invoker : counter.keySet()) {
            Long count = counter.get(invoker).get();
            Assertions.assertTrue(count < (overloadThread + 1L),
                "count of request accept by each invoker will not be higher than (overloadRatioAllowed * average + 1)");
        }

    }

    // https://github.com/apache/dubbo/issues/5429
    @Test
    void testNormalWhenRouterEnabled() {
        ConsistentHashLoadBalance lb = (ConsistentHashLoadBalance) getLoadBalance(ConsistentHashLoadBalance.NAME);
        URL url = invokers.get(0).getUrl();
        RouterChain<LoadBalanceBaseTest> routerChain = RouterChain.buildChain(LoadBalanceBaseTest.class, url);
        Invoker<LoadBalanceBaseTest> result = lb.select(invokers, url, invocation);
        int originalHashCode = lb.getCorrespondingHashCode(invokers);

        for (int i = 0; i < 100; i++) {
            routerChain.setInvokers(new BitList<>(invokers));
            List<Invoker<LoadBalanceBaseTest>> routeInvokers = routerChain.route(url, new BitList<>(invokers), invocation);

            Assertions.assertEquals(originalHashCode, lb.getCorrespondingHashCode(routeInvokers));
        }
    }
}
