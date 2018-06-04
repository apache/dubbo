package com.alibaba.dubbo.rpc.cluster.loadbalance;

import com.alibaba.dubbo.rpc.Invoker;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class LeastActiveLoadBalanceTest extends LoadBalanceBaseTest{
    @Test
    public void testLeastActiveLoadBalanceSelectOne() {
        int runs = 10000;
        Map<Invoker, AtomicLong> counter = getInvokeCounter(runs, LeastActiveLoadBalance.NAME);
        for (Invoker minvoker : counter.keySet()) {
            Long count = counter.get(minvoker).get();
            Assert.assertTrue("abs diff should < avg", Math.abs(count - runs / (0f + invokers.size())) < runs / (0f + invokers.size()));
        }
    }
}
