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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
        Assert.assertEquals(1, calculateDefaultWarmupWeight(0));
        Assert.assertEquals(1, calculateDefaultWarmupWeight(13));
        Assert.assertEquals(1, calculateDefaultWarmupWeight(6 * 1000));
        Assert.assertEquals(2, calculateDefaultWarmupWeight(12 * 1000));
        Assert.assertEquals(10, calculateDefaultWarmupWeight(60 * 1000));
        Assert.assertEquals(50, calculateDefaultWarmupWeight(5 * 60 * 1000));
        Assert.assertEquals(50, calculateDefaultWarmupWeight(5 * 60 * 1000 + 23));
        Assert.assertEquals(50, calculateDefaultWarmupWeight(5 * 60 * 1000 + 5999));
        Assert.assertEquals(51, calculateDefaultWarmupWeight(5 * 60 * 1000 + 6000));
        Assert.assertEquals(90, calculateDefaultWarmupWeight(9 * 60 * 1000));
        Assert.assertEquals(98, calculateDefaultWarmupWeight(10 * 60 * 1000 - 12 * 1000));
        Assert.assertEquals(99, calculateDefaultWarmupWeight(10 * 60 * 1000 - 6 * 1000));
        Assert.assertEquals(100, calculateDefaultWarmupWeight(10 * 60 * 1000));
        Assert.assertEquals(100, calculateDefaultWarmupWeight(20 * 60 * 1000));
    }

    /**
     * handle default data
     *
     * @return
     */
    private static int calculateDefaultWarmupWeight(int uptime) {
        return AbstractLoadBalance.calculateWarmupWeight(uptime, Constants.DEFAULT_WARMUP, Constants.DEFAULT_WEIGHT);
    }

}