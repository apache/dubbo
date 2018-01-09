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
package com.alibaba.dubbo.rpc.cluster.loadbalance;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcStatus;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;

import junit.framework.Assert;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RoundRobinLoadBalanceTest
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class LoadBalanceTest {
    Invocation invocation;
    List<Invoker<LoadBalanceTest>> invokers = new ArrayList<Invoker<LoadBalanceTest>>();
    Invoker<LoadBalanceTest> invoker1;
    Invoker<LoadBalanceTest> invoker2;
    Invoker<LoadBalanceTest> invoker3;
    Invoker<LoadBalanceTest> invoker4;
    Invoker<LoadBalanceTest> invoker5;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        invocation = EasyMock.createMock(Invocation.class);
        EasyMock.expect(invocation.getMethodName()).andReturn("method1").anyTimes();

        invoker1 = EasyMock.createMock(Invoker.class);
        invoker2 = EasyMock.createMock(Invoker.class);
        invoker3 = EasyMock.createMock(Invoker.class);
        invoker4 = EasyMock.createMock(Invoker.class);
        invoker5 = EasyMock.createMock(Invoker.class);

        URL url1 = URL.valueOf("test://127.0.0.1:1/DemoService");
        URL url2 = URL.valueOf("test://127.0.0.1:2/DemoService");
        URL url3 = URL.valueOf("test://127.0.0.1:3/DemoService");
        URL url4 = URL.valueOf("test://127.0.0.1:4/DemoService");
        URL url5 = URL.valueOf("test://127.0.0.1:5/DemoService");

        EasyMock.expect(invoker1.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker1.getInterface()).andReturn(LoadBalanceTest.class).anyTimes();
        EasyMock.expect(invoker1.getUrl()).andReturn(url1).anyTimes();

        EasyMock.expect(invoker2.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker2.getInterface()).andReturn(LoadBalanceTest.class).anyTimes();
        EasyMock.expect(invoker2.getUrl()).andReturn(url2).anyTimes();

        EasyMock.expect(invoker3.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker3.getInterface()).andReturn(LoadBalanceTest.class).anyTimes();
        EasyMock.expect(invoker3.getUrl()).andReturn(url3).anyTimes();

        EasyMock.expect(invoker4.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker4.getInterface()).andReturn(LoadBalanceTest.class).anyTimes();
        EasyMock.expect(invoker4.getUrl()).andReturn(url4).anyTimes();

        EasyMock.expect(invoker5.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker5.getInterface()).andReturn(LoadBalanceTest.class).anyTimes();
        EasyMock.expect(invoker5.getUrl()).andReturn(url5).anyTimes();

        EasyMock.replay(invocation, invoker1, invoker2, invoker3, invoker4, invoker5);

        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        invokers.add(invoker4);
        invokers.add(invoker5);
    }

    @Test
    public void testRoundRobinLoadBalance_select() {
        int runs = 10000;
        Map<Invoker, AtomicLong> counter = getInvokeCounter(runs, RoundRobinLoadBalance.NAME);
        for (Invoker minvoker : counter.keySet()) {
            Long count = counter.get(minvoker).get();
            Assert.assertTrue("abs diff shoud < 1", Math.abs(count - runs / (0f + invokers.size())) < 1f);
        }
    }

    @Test
    public void testRandomLoadBalance_select() {
        int runs = 1000;
        Map<Invoker, AtomicLong> counter = getInvokeCounter(runs, RandomLoadBalance.NAME);
        for (Invoker minvoker : counter.keySet()) {
            Long count = counter.get(minvoker).get();
            // System.out.println(count);
            Assert.assertTrue("abs diff shoud < avg",
                Math.abs(count - runs / (0f + invokers.size())) < runs / (0f + invokers.size()));
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

    public Map<Invoker, AtomicLong> getInvokeCounter(int runs, String loadbalanceName) {
        Map<Invoker, AtomicLong> counter = new ConcurrentHashMap<Invoker, AtomicLong>();
        LoadBalance lb = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(loadbalanceName);
        for (Invoker invoker : invokers) {
            counter.put(invoker, new AtomicLong(0));
        }
        for (int i = 0; i < runs; i++) {
            Invoker sinvoker = lb.select(invokers, invokers.get(0).getUrl(), invocation);
            counter.get(sinvoker).incrementAndGet();
        }
        return counter;
    }

    @Test
    public void testLoadBalanceWarmup() {
        Assert.assertEquals(1,
            AbstractLoadBalance.calculateWarmupWeight(0, Constants.DEFAULT_WARMUP, Constants.DEFAULT_WEIGHT));
        Assert.assertEquals(1,
            AbstractLoadBalance.calculateWarmupWeight(13, Constants.DEFAULT_WARMUP, Constants.DEFAULT_WEIGHT));
        Assert.assertEquals(1,
            AbstractLoadBalance.calculateWarmupWeight(6 * 1000, Constants.DEFAULT_WARMUP, Constants.DEFAULT_WEIGHT));
        Assert.assertEquals(2,
            AbstractLoadBalance.calculateWarmupWeight(12 * 1000, Constants.DEFAULT_WARMUP, Constants.DEFAULT_WEIGHT));
        Assert.assertEquals(10,
            AbstractLoadBalance.calculateWarmupWeight(60 * 1000, Constants.DEFAULT_WARMUP, Constants.DEFAULT_WEIGHT));
        Assert.assertEquals(50, AbstractLoadBalance
            .calculateWarmupWeight(5 * 60 * 1000, Constants.DEFAULT_WARMUP, Constants.DEFAULT_WEIGHT));
        Assert.assertEquals(50, AbstractLoadBalance
            .calculateWarmupWeight(5 * 60 * 1000 + 23, Constants.DEFAULT_WARMUP, Constants.DEFAULT_WEIGHT));
        Assert.assertEquals(50, AbstractLoadBalance
            .calculateWarmupWeight(5 * 60 * 1000 + 5999, Constants.DEFAULT_WARMUP, Constants.DEFAULT_WEIGHT));
        Assert.assertEquals(51, AbstractLoadBalance
            .calculateWarmupWeight(5 * 60 * 1000 + 6000, Constants.DEFAULT_WARMUP, Constants.DEFAULT_WEIGHT));
        Assert.assertEquals(90, AbstractLoadBalance
            .calculateWarmupWeight(9 * 60 * 1000, Constants.DEFAULT_WARMUP, Constants.DEFAULT_WEIGHT));
        Assert.assertEquals(98, AbstractLoadBalance
            .calculateWarmupWeight(10 * 60 * 1000 - 12 * 1000, Constants.DEFAULT_WARMUP, Constants.DEFAULT_WEIGHT));
        Assert.assertEquals(99, AbstractLoadBalance
            .calculateWarmupWeight(10 * 60 * 1000 - 6 * 1000, Constants.DEFAULT_WARMUP, Constants.DEFAULT_WEIGHT));
        Assert.assertEquals(100, AbstractLoadBalance
            .calculateWarmupWeight(10 * 60 * 1000, Constants.DEFAULT_WARMUP, Constants.DEFAULT_WEIGHT));
        Assert.assertEquals(100, AbstractLoadBalance
            .calculateWarmupWeight(20 * 60 * 1000, Constants.DEFAULT_WARMUP, Constants.DEFAULT_WEIGHT));
    }

}