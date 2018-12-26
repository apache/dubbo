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
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcStatus;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;
import com.alibaba.fastjson.JSON;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * RoundRobinLoadBalanceTest
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

    RpcStatus weightTestRpcStatus1;
    RpcStatus weightTestRpcStatus2;
    RpcStatus weightTestRpcStatus3;
    RpcInvocation weightTestInvocation;
    
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

        invocation = mock(Invocation.class);
        given(invocation.getMethodName()).willReturn("method1");

        invoker1 = mock(Invoker.class);
        invoker2 = mock(Invoker.class);
        invoker3 = mock(Invoker.class);
        invoker4 = mock(Invoker.class);
        invoker5 = mock(Invoker.class);

        URL url1 = URL.valueOf("test://127.0.0.1:1/DemoService");
        URL url2 = URL.valueOf("test://127.0.0.1:2/DemoService");
        URL url3 = URL.valueOf("test://127.0.0.1:3/DemoService");
        URL url4 = URL.valueOf("test://127.0.0.1:4/DemoService");
        URL url5 = URL.valueOf("test://127.0.0.1:5/DemoService");

        given(invoker1.isAvailable()).willReturn(true);
        given(invoker1.getInterface()).willReturn(LoadBalanceTest.class);
        given(invoker1.getUrl()).willReturn(url1);

        given(invoker2.isAvailable()).willReturn(true);
        given(invoker2.getInterface()).willReturn(LoadBalanceTest.class);
        given(invoker2.getUrl()).willReturn(url2);

        given(invoker3.isAvailable()).willReturn(true);
        given(invoker3.getInterface()).willReturn(LoadBalanceTest.class);
        given(invoker3.getUrl()).willReturn(url3);

        given(invoker4.isAvailable()).willReturn(true);
        given(invoker4.getInterface()).willReturn(LoadBalanceTest.class);
        given(invoker4.getUrl()).willReturn(url4);

        given(invoker5.isAvailable()).willReturn(true);
        given(invoker5.getInterface()).willReturn(LoadBalanceTest.class);
        given(invoker5.getUrl()).willReturn(url5);

        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        invokers.add(invoker4);
        invokers.add(invoker5);
    }
    
    private AbstractLoadBalance getLoadBalance(String loadbalanceName) {
        return (AbstractLoadBalance) ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(loadbalanceName);
    }
    
    @Test
    public void testRoundRobinLoadBalance_select() {
        int runs = 1000;
        Map<Invoker, AtomicLong> counter = getInvokeCounter(runs, RoundRobinLoadBalance.NAME);
        for (Invoker minvoker : counter.keySet()) {
            Long count = counter.get(minvoker).get();
            Assert.assertTrue("abs diff shoud < 1", Math.abs(count - runs / (0f + invokers.size())) < 1f);
        }
    }
    
    private void assertStrictWRRResult(int runs, Map<Invoker, InvokeResult> resultMap) {
        for (InvokeResult invokeResult : resultMap.values()) {
            // Because it's a strictly round robin, so the abs delta should be < 10 too
            Assert.assertTrue("delta with expected count should < 10", 
                    Math.abs(invokeResult.getExpected(runs) - invokeResult.getCount().get()) < 10);
        }
    }
    
    /**
     * a multi-threaded test on weighted round robin
     */
    @Test
    public void testRoundRobinLoadBalanceWithWeight() {
        final Map<Invoker, InvokeResult> totalMap = new HashMap<Invoker, InvokeResult>();
        final AtomicBoolean shouldBegin = new AtomicBoolean(false);
        final int runs = 1000;
        List<Thread> threads = new ArrayList<Thread>();
        int threadNum = 10;
        for (int i = 0; i < threadNum; i ++) {
            threads.add(new Thread() {
                @Override
                public void run() {
                    while (!shouldBegin.get()) {
                        try {
                            sleep(5);
                        } catch (InterruptedException e) {
                        }
                    }
                    Map<Invoker, InvokeResult> resultMap = getWeightedInvokeResult(runs, RoundRobinLoadBalance.NAME);
                    synchronized (totalMap) {
                        for (Entry<Invoker, InvokeResult> entry : resultMap.entrySet()) {
                            if (!totalMap.containsKey(entry.getKey())) {
                                totalMap.put(entry.getKey(), entry.getValue());
                            } else {
                                totalMap.get(entry.getKey()).getCount().addAndGet(entry.getValue().getCount().get());
                            }
                        }
                    }
                }
            });
        }
        for (Thread thread : threads) {
            thread.start();
        }
        // let's rock it!
        shouldBegin.set(true);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
        assertStrictWRRResult(runs * threadNum, totalMap);
    }
    
    @Test
    public void testRoundRobinLoadBalanceWithWeightShouldNotRecycle() {
        int runs = 1000;
        //tmperately add a new invoker
        weightInvokers.add(weightInvokerTmp);
        try {
            Map<Invoker, InvokeResult> resultMap = getWeightedInvokeResult(runs, RoundRobinLoadBalance.NAME);
            assertStrictWRRResult(runs, resultMap);
            RoundRobinLoadBalance lb = (RoundRobinLoadBalance)getLoadBalance(RoundRobinLoadBalance.NAME);
            Assert.assertEquals(weightInvokers.size(), lb.getInvokerAddrList(weightInvokers, weightTestInvocation).size());
            
            //remove the last invoker and retry
            weightInvokers.remove(weightInvokerTmp);
            resultMap = getWeightedInvokeResult(runs, RoundRobinLoadBalance.NAME);
            assertStrictWRRResult(runs, resultMap);
            Assert.assertNotEquals(weightInvokers.size(), lb.getInvokerAddrList(weightInvokers, weightTestInvocation).size());
        } finally {
            weightInvokers.remove(weightInvokerTmp);
        }
    }
    
    @Test
    public void testRoundRobinLoadBalanceWithWeightShouldRecycle() {
        {
            Field recycleTimeField = null;
            try {
                //change recycle time to 1 ms
                recycleTimeField = RoundRobinLoadBalance.class.getDeclaredField("RECYCLE_PERIOD");
                recycleTimeField.setAccessible(true);
                recycleTimeField.setInt(RoundRobinLoadBalance.class, 10);
            } catch (NoSuchFieldException e) {
                Assert.assertTrue("getField failed", true);
            } catch (SecurityException e) {
                Assert.assertTrue("getField failed", true);
            } catch (IllegalArgumentException e) {
                Assert.assertTrue("getField failed", true);
            } catch (IllegalAccessException e) {
                Assert.assertTrue("getField failed", true);
            }
        }
        int runs = 1000;
        //temporarily add a new invoker
        weightInvokers.add(weightInvokerTmp);
        try {
            Map<Invoker, InvokeResult> resultMap = getWeightedInvokeResult(runs, RoundRobinLoadBalance.NAME);
            assertStrictWRRResult(runs, resultMap);
            RoundRobinLoadBalance lb = (RoundRobinLoadBalance)getLoadBalance(RoundRobinLoadBalance.NAME);
            Assert.assertEquals(weightInvokers.size(), lb.getInvokerAddrList(weightInvokers, weightTestInvocation).size());
            
            //remove the tmp invoker and retry, should recycle its cache
            weightInvokers.remove(weightInvokerTmp);
            resultMap = getWeightedInvokeResult(runs, RoundRobinLoadBalance.NAME);
            assertStrictWRRResult(runs, resultMap);
            Assert.assertEquals(weightInvokers.size(), lb.getInvokerAddrList(weightInvokers, weightTestInvocation).size());
        } finally {
            weightInvokers.remove(weightInvokerTmp);
        }
    }

    @Test
    public void testSelectByWeightLeastActive() {
        int sumInvoker1 = 0;
        int sumInvoker2 = 0;
        int loop = 1000;
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
            Assert.assertTrue("select is not the least active one", !selected.getUrl().getProtocol().equals("test3"));
        }
        // the sumInvoker1 : sumInvoker2 approximately equal to 1: 9
        System.out.println(sumInvoker1);
        System.out.println(sumInvoker2);
        Assert.assertEquals("select failed!", sumInvoker1 + sumInvoker2, loop);
    }

    @Test
    public void testSelectByWeightRandom() {
        int sumInvoker1 = 0;
        int sumInvoker2 = 0;
        int sumInvoker3 = 0;
        int loop = 1000;
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
        System.out.println(sumInvoker1);
        System.out.println(sumInvoker2);
        System.out.println(sumInvoker3);
        Assert.assertEquals("select failed!", sumInvoker1 + sumInvoker2 + sumInvoker3, loop);
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
        int runs = 1000;
        Map<Invoker, AtomicLong> counter = getInvokeCounter(runs, LeastActiveLoadBalance.NAME);
        for (Invoker minvoker : counter.keySet()) {
            Long count = counter.get(minvoker).get();
            //            System.out.println(count);
            Assert.assertTrue("abs diff shoud < avg",
                    Math.abs(count - runs / (0f + invokers.size())) < runs / (0f + invokers.size()));
        }
    }
    
    private Map<Invoker, AtomicLong> getInvokeCounter(int runs, String loadbalanceName) {
        Map<Invoker, AtomicLong> counter = new ConcurrentHashMap<Invoker, AtomicLong>();
        LoadBalance lb = getLoadBalance(loadbalanceName);
        for (Invoker invoker : invokers) {
            counter.put(invoker, new AtomicLong(0));
        }
        URL url = invokers.get(0).getUrl();
        for (int i = 0; i < runs; i++) {
            Invoker sinvoker = lb.select(invokers, url, invocation);
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
    
    /*------------------------------------test invokers for weight---------------------------------------*/

    protected List<Invoker<LoadBalanceTest>> weightInvokers = new ArrayList<Invoker<LoadBalanceTest>>();
    protected Invoker<LoadBalanceTest> weightInvoker1;
    protected Invoker<LoadBalanceTest> weightInvoker2;
    protected Invoker<LoadBalanceTest> weightInvoker3;
    protected Invoker<LoadBalanceTest> weightInvokerTmp;

    @Before
    public void setUpWeightInvokers() throws Exception {
        weightInvoker1 = mock(Invoker.class);
        weightInvoker2 = mock(Invoker.class);
        weightInvoker3 = mock(Invoker.class);
        weightInvokerTmp = mock(Invoker.class);
        
        weightTestInvocation = new RpcInvocation();
        weightTestInvocation.setMethodName("test");
        
        URL url1 = URL.valueOf("test1://127.0.0.1:11/DemoService?weight=11&active=0");
        URL url2 = URL.valueOf("test2://127.0.0.1:12/DemoService?weight=97&active=0");
        URL url3 = URL.valueOf("test3://127.0.0.1:13/DemoService?weight=67&active=1");
        URL urlTmp = URL.valueOf("test4://127.0.0.1:9999/DemoService?weight=601&active=0");
        
        given(weightInvoker1.isAvailable()).willReturn(true);
        given(weightInvoker1.getInterface()).willReturn(LoadBalanceTest.class);
        given(weightInvoker1.getUrl()).willReturn(url1);
        
        given(weightInvoker2.isAvailable()).willReturn(true);
        given(weightInvoker2.getInterface()).willReturn(LoadBalanceTest.class);
        given(weightInvoker2.getUrl()).willReturn(url2);
        
        given(weightInvoker3.isAvailable()).willReturn(true);
        given(weightInvoker3.getInterface()).willReturn(LoadBalanceTest.class);
        given(weightInvoker3.getUrl()).willReturn(url3);
        
        given(weightInvokerTmp.isAvailable()).willReturn(true);
        given(weightInvokerTmp.getInterface()).willReturn(LoadBalanceTest.class);
        given(weightInvokerTmp.getUrl()).willReturn(urlTmp);
        
        weightInvokers.add(weightInvoker1);
        weightInvokers.add(weightInvoker2);
        weightInvokers.add(weightInvoker3);
        
        weightTestRpcStatus1 = RpcStatus.getStatus(weightInvoker1.getUrl(), weightTestInvocation.getMethodName());
        weightTestRpcStatus2 = RpcStatus.getStatus(weightInvoker2.getUrl(), weightTestInvocation.getMethodName());
        weightTestRpcStatus3 = RpcStatus.getStatus(weightInvoker3.getUrl(), weightTestInvocation.getMethodName());
        
        // weightTestRpcStatus3 active is 1
        RpcStatus.beginCount(weightInvoker3.getUrl(), weightTestInvocation.getMethodName());
    }
    
    private static class InvokeResult {
        private AtomicLong count = new AtomicLong();
        private int weight = 0;
        private int totalWeight = 0;

        public InvokeResult(int weight) {
            this.weight = weight;
        }

        public AtomicLong getCount() {
            return count;
        }
        
        public int getWeight() {
            return weight;
        }
        
        public int getTotalWeight() {
            return totalWeight;
        }
        
        public void setTotalWeight(int totalWeight) {
            this.totalWeight = totalWeight;
        }
        
        public int getExpected(int runCount) {
            return getWeight() * runCount / getTotalWeight();
        }
        
        public float getDeltaPercentage(int runCount) {
            int expected = getExpected(runCount);
            return Math.abs((expected - getCount().get()) * 100.0f / expected);
        }
        
        @Override
        public String toString() {
            return JSON.toJSONString(this);
        }
    }
    
    private Map<Invoker, InvokeResult> getWeightedInvokeResult(int runs, String loadbalanceName) {
        Map<Invoker, InvokeResult> counter = new ConcurrentHashMap<Invoker, InvokeResult>();
        AbstractLoadBalance lb = getLoadBalance(loadbalanceName);
        int totalWeight = 0;
        for (int i = 0; i < weightInvokers.size(); i ++) {
            InvokeResult invokeResult = new InvokeResult(lb.getWeight(weightInvokers.get(i), weightTestInvocation));
            counter.put(weightInvokers.get(i), invokeResult);
            totalWeight += invokeResult.getWeight();
        }
        for (InvokeResult invokeResult : counter.values()) {
            invokeResult.setTotalWeight(totalWeight);
        }
        for (int i = 0; i < runs; i++) {
            Invoker sinvoker = lb.select(weightInvokers, weightInvokers.get(0).getUrl(), weightTestInvocation);
            counter.get(sinvoker).getCount().incrementAndGet();
        }
        return counter;
    }

}