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
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcStatus;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class WeightedLeastActiveBalanceTest extends LoadBalanceBaseTest {

    @BeforeEach
    public void before() throws Exception {
        weightInvoker1 = mock(Invoker.class, Mockito.withSettings().stubOnly());
        weightInvoker2 = mock(Invoker.class, Mockito.withSettings().stubOnly());
        weightInvoker3 = mock(Invoker.class, Mockito.withSettings().stubOnly());

        weightTestInvocation = new RpcInvocation();
        weightTestInvocation.setMethodName("test");

        URL url1 = URL.valueOf("test1://127.0.0.1:11/DemoService?weight=3&active=1");
        URL url2 = URL.valueOf("test2://127.0.0.1:12/DemoService?weight=9&active=1");
        URL url3 = URL.valueOf("test3://127.0.0.1:13/DemoService?weight=6&active=1");

        given(weightInvoker1.isAvailable()).willReturn(true);
        given(weightInvoker1.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(weightInvoker1.getUrl()).willReturn(url1);

        given(weightInvoker2.isAvailable()).willReturn(true);
        given(weightInvoker2.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(weightInvoker2.getUrl()).willReturn(url2);

        given(weightInvoker3.isAvailable()).willReturn(true);
        given(weightInvoker3.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(weightInvoker3.getUrl()).willReturn(url3);

        weightInvokers.add(weightInvoker1);
        weightInvokers.add(weightInvoker2);
        weightInvokers.add(weightInvoker3);

        RpcStatus.beginCount(weightInvoker1.getUrl(), weightTestInvocation.getMethodName());
        RpcStatus.beginCount(weightInvoker2.getUrl(), weightTestInvocation.getMethodName());
        RpcStatus.beginCount(weightInvoker3.getUrl(), weightTestInvocation.getMethodName());
    }

    @Disabled
    @Test
    void testWeightedLeastActiveLoadBalance_select() {
        int runs = 10000;
        Map<Invoker, AtomicLong> counter = getInvokeCounter(runs, WeightedLeastActiveLoadBalance.NAME);
        for (Map.Entry<Invoker, AtomicLong> entry : counter.entrySet()) {
            Long count = entry.getValue().get();
            //            System.out.println(count);
            Assertions.assertTrue(
                    Math.abs(count - runs / (0f + invokers.size())) < runs / (0f + invokers.size()),
                    "abs diff should < avg");
        }
    }

    @Test
    void testSelectDirectly() {

        int sumInvoker1 = 0;
        int loop = 10000;

        WeightedLeastActiveLoadBalance lb = new WeightedLeastActiveLoadBalance();
        for (int i = 0; i < loop; i++) {
            Invoker selected = lb.select(weightInvokers, null, weightTestInvocation);

            if (selected.getUrl().getProtocol().equals("test1")) {
                sumInvoker1++;
            }

            Assertions.assertTrue(
                    !selected.getUrl().getProtocol().equals("test2"), "select is not the least active one");
            Assertions.assertTrue(
                    !selected.getUrl().getProtocol().equals("test3"), "select is not the least active one");
        }

        Assertions.assertEquals(sumInvoker1, loop, "select failed!");
    }

    @Test
    void testSelectByRandom() {
        Invoker<LoadBalanceBaseTest> weightInvoker4 =
                mock(Invoker.class, Mockito.withSettings().stubOnly());
        Invoker<LoadBalanceBaseTest> weightInvoker5 =
                mock(Invoker.class, Mockito.withSettings().stubOnly());
        URL url4 = URL.valueOf("test4://127.0.0.1:14/DemoService?weight=2&active=1");
        URL url5 = URL.valueOf("test5://127.0.0.1:15/DemoService?weight=1&active=2");
        given(weightInvoker4.isAvailable()).willReturn(true);
        given(weightInvoker4.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(weightInvoker4.getUrl()).willReturn(url4);
        RpcStatus.beginCount(weightInvoker4.getUrl(), weightTestInvocation.getMethodName());

        given(weightInvoker5.isAvailable()).willReturn(true);
        given(weightInvoker5.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(weightInvoker5.getUrl()).willReturn(url5);
        RpcStatus.beginCount(weightInvoker5.getUrl(), weightTestInvocation.getMethodName());
        RpcStatus.beginCount(weightInvoker5.getUrl(), weightTestInvocation.getMethodName());

        weightInvokers.add(weightInvoker4);
        weightInvokers.add(weightInvoker5);

        int sumInvoker4 = 0;
        int sumInvoker5 = 0;
        int loop = 10000;

        WeightedLeastActiveLoadBalance lb = new WeightedLeastActiveLoadBalance();
        for (int i = 0; i < loop; i++) {
            Invoker selected = lb.select(weightInvokers, null, weightTestInvocation);

            if (selected.getUrl().getProtocol().equals("test4")) {
                sumInvoker4++;
            }
            if (selected.getUrl().getProtocol().equals("test5")) {
                sumInvoker5++;
            }
        }

        // the sumInvoker1 : sumInvoker2 approximately equal to 1: 1
        System.out.println(sumInvoker4);
        System.out.println(sumInvoker5);

        Assertions.assertEquals(sumInvoker4 + sumInvoker5, loop, "select failed!");
    }

    @Test
    void testSelectByWeight() {
        Invoker<LoadBalanceBaseTest> weightInvoker6 =
                mock(Invoker.class, Mockito.withSettings().stubOnly());
        Invoker<LoadBalanceBaseTest> weightInvoker7 =
                mock(Invoker.class, Mockito.withSettings().stubOnly());
        URL url6 = URL.valueOf("test6://127.0.0.1:14/DemoService?weight=2&active=0");
        URL url7 = URL.valueOf("test7://127.0.0.1:14/DemoService?weight=8&active=0");
        given(weightInvoker6.isAvailable()).willReturn(true);
        given(weightInvoker6.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(weightInvoker6.getUrl()).willReturn(url6);

        given(weightInvoker7.isAvailable()).willReturn(true);
        given(weightInvoker7.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(weightInvoker7.getUrl()).willReturn(url7);
        weightInvokers.add(weightInvoker6);
        weightInvokers.add(weightInvoker7);

        int sumInvoker4 = 0;
        int sumInvoker5 = 0;
        int loop = 10000;

        WeightedLeastActiveLoadBalance lb = new WeightedLeastActiveLoadBalance();
        for (int i = 0; i < loop; i++) {
            Invoker selected = lb.select(weightInvokers, null, weightTestInvocation);

            if (selected.getUrl().getProtocol().equals("test6")) {
                sumInvoker4++;
            }
            if (selected.getUrl().getProtocol().equals("test7")) {
                sumInvoker5++;
            }
        }

        // the sumInvoker1 : sumInvoker2 approximately equal to 1: 4
        System.out.println(sumInvoker4);
        System.out.println(sumInvoker5);

        Assertions.assertEquals(sumInvoker4 + sumInvoker5, loop, "select failed!");
    }
}
