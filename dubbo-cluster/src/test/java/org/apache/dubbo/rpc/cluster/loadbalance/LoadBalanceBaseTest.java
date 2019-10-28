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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcStatus;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.dubbo.rpc.cluster.Constants.DEFAULT_WARMUP;
import static org.apache.dubbo.rpc.cluster.Constants.DEFAULT_WEIGHT;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * RoundRobinLoadBalanceTest
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class LoadBalanceBaseTest {
    Invocation invocation;
    List<Invoker<LoadBalanceBaseTest>> invokers = new ArrayList<Invoker<LoadBalanceBaseTest>>();
    Invoker<LoadBalanceBaseTest> invoker1;
    Invoker<LoadBalanceBaseTest> invoker2;
    Invoker<LoadBalanceBaseTest> invoker3;
    Invoker<LoadBalanceBaseTest> invoker4;
    Invoker<LoadBalanceBaseTest> invoker5;

    RpcStatus weightTestRpcStatus1;
    RpcStatus weightTestRpcStatus2;
    RpcStatus weightTestRpcStatus3;

    RpcInvocation weightTestInvocation;

    /**
     * @throws java.lang.Exception
     */
    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {

        invocation = mock(Invocation.class);
        given(invocation.getMethodName()).willReturn("method1");
        given(invocation.getArguments()).willReturn(new Object[] {"arg1","arg2","arg3"});

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
        given(invoker1.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(invoker1.getUrl()).willReturn(url1);

        given(invoker2.isAvailable()).willReturn(true);
        given(invoker2.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(invoker2.getUrl()).willReturn(url2);

        given(invoker3.isAvailable()).willReturn(true);
        given(invoker3.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(invoker3.getUrl()).willReturn(url3);

        given(invoker4.isAvailable()).willReturn(true);
        given(invoker4.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(invoker4.getUrl()).willReturn(url4);

        given(invoker5.isAvailable()).willReturn(true);
        given(invoker5.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(invoker5.getUrl()).willReturn(url5);

        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        invokers.add(invoker4);
        invokers.add(invoker5);
    }

    public Map<Invoker, AtomicLong> getInvokeCounter(int runs, String loadbalanceName) {
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

    protected AbstractLoadBalance getLoadBalance(String loadbalanceName) {
        return (AbstractLoadBalance) ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(loadbalanceName);
    }

    @Test
    public void testLoadBalanceWarmup() {
        Assertions.assertEquals(1, calculateDefaultWarmupWeight(0));
        Assertions.assertEquals(1, calculateDefaultWarmupWeight(13));
        Assertions.assertEquals(1, calculateDefaultWarmupWeight(6 * 1000));
        Assertions.assertEquals(2, calculateDefaultWarmupWeight(12 * 1000));
        Assertions.assertEquals(10, calculateDefaultWarmupWeight(60 * 1000));
        Assertions.assertEquals(50, calculateDefaultWarmupWeight(5 * 60 * 1000));
        Assertions.assertEquals(50, calculateDefaultWarmupWeight(5 * 60 * 1000 + 23));
        Assertions.assertEquals(50, calculateDefaultWarmupWeight(5 * 60 * 1000 + 5999));
        Assertions.assertEquals(51, calculateDefaultWarmupWeight(5 * 60 * 1000 + 6000));
        Assertions.assertEquals(90, calculateDefaultWarmupWeight(9 * 60 * 1000));
        Assertions.assertEquals(98, calculateDefaultWarmupWeight(10 * 60 * 1000 - 12 * 1000));
        Assertions.assertEquals(99, calculateDefaultWarmupWeight(10 * 60 * 1000 - 6 * 1000));
        Assertions.assertEquals(100, calculateDefaultWarmupWeight(10 * 60 * 1000));
        Assertions.assertEquals(100, calculateDefaultWarmupWeight(20 * 60 * 1000));
    }

    /**
     * handle default data
     *
     * @return
     */
    private static int calculateDefaultWarmupWeight(int uptime) {
        return AbstractLoadBalance.calculateWarmupWeight(uptime, DEFAULT_WARMUP, DEFAULT_WEIGHT);
    }

    /*------------------------------------test invokers for weight---------------------------------------*/

    protected static class InvokeResult {
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

    protected List<Invoker<LoadBalanceBaseTest>> weightInvokers = new ArrayList<Invoker<LoadBalanceBaseTest>>();
    protected Invoker<LoadBalanceBaseTest> weightInvoker1;
    protected Invoker<LoadBalanceBaseTest> weightInvoker2;
    protected Invoker<LoadBalanceBaseTest> weightInvoker3;
    protected Invoker<LoadBalanceBaseTest> weightInvokerTmp;

    @BeforeEach
    public void before() throws Exception {
        weightInvoker1 = mock(Invoker.class, Mockito.withSettings().stubOnly());
        weightInvoker2 = mock(Invoker.class, Mockito.withSettings().stubOnly());
        weightInvoker3 = mock(Invoker.class, Mockito.withSettings().stubOnly());
        weightInvokerTmp = mock(Invoker.class, Mockito.withSettings().stubOnly());

        weightTestInvocation = new RpcInvocation();
        weightTestInvocation.setMethodName("test");

        URL url1 = URL.valueOf("test1://127.0.0.1:11/DemoService?weight=1&active=0");
        URL url2 = URL.valueOf("test2://127.0.0.1:12/DemoService?weight=9&active=0");
        URL url3 = URL.valueOf("test3://127.0.0.1:13/DemoService?weight=6&active=1");
        URL urlTmp = URL.valueOf("test4://127.0.0.1:9999/DemoService?weight=11&active=0");

        given(weightInvoker1.isAvailable()).willReturn(true);
        given(weightInvoker1.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(weightInvoker1.getUrl()).willReturn(url1);

        given(weightInvoker2.isAvailable()).willReturn(true);
        given(weightInvoker2.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(weightInvoker2.getUrl()).willReturn(url2);

        given(weightInvoker3.isAvailable()).willReturn(true);
        given(weightInvoker3.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(weightInvoker3.getUrl()).willReturn(url3);

        given(weightInvokerTmp.isAvailable()).willReturn(true);
        given(weightInvokerTmp.getInterface()).willReturn(LoadBalanceBaseTest.class);
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

    protected Map<Invoker, InvokeResult> getWeightedInvokeResult(int runs, String loadbalanceName) {
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
        URL url = weightInvokers.get(0).getUrl();
        for (int i = 0; i < runs; i++) {
            Invoker sinvoker = lb.select(weightInvokers, url, weightTestInvocation);
            counter.get(sinvoker).getCount().incrementAndGet();
        }
        return counter;
    }

}
