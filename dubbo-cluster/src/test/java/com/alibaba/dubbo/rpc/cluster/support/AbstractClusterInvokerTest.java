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
package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;
import com.alibaba.dubbo.rpc.cluster.directory.StaticDirectory;
import com.alibaba.dubbo.rpc.cluster.filter.DemoService;
import com.alibaba.dubbo.rpc.cluster.loadbalance.LeastActiveLoadBalance;
import com.alibaba.dubbo.rpc.cluster.loadbalance.RandomLoadBalance;
import com.alibaba.dubbo.rpc.cluster.loadbalance.RoundRobinLoadBalance;

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
 * AbstractClusterInvokerTest
 *
 */
@SuppressWarnings("rawtypes")
public class AbstractClusterInvokerTest {
    List<Invoker<IHelloService>> invokers = new ArrayList<Invoker<IHelloService>>();
    List<Invoker<IHelloService>> selectedInvokers = new ArrayList<Invoker<IHelloService>>();
    AbstractClusterInvoker<IHelloService> cluster;
    AbstractClusterInvoker<IHelloService> cluster_nocheck;
    Directory<IHelloService> dic;
    RpcInvocation invocation = new RpcInvocation();
    URL url = URL.valueOf("registry://localhost:9090");

    Invoker<IHelloService> invoker1;
    Invoker<IHelloService> invoker2;
    Invoker<IHelloService> invoker3;
    Invoker<IHelloService> invoker4;
    Invoker<IHelloService> invoker5;
    Invoker<IHelloService> mockedInvoker1;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @SuppressWarnings({"unchecked"})
    @Before
    public void setUp() throws Exception {
        invocation.setMethodName("sayHello");

        invoker1 = EasyMock.createMock(Invoker.class);
        invoker2 = EasyMock.createMock(Invoker.class);
        invoker3 = EasyMock.createMock(Invoker.class);
        invoker4 = EasyMock.createMock(Invoker.class);
        invoker5 = EasyMock.createMock(Invoker.class);
        mockedInvoker1 = EasyMock.createMock(Invoker.class);

        URL turl = URL.valueOf("test://test:11/test");

        EasyMock.expect(invoker1.isAvailable()).andReturn(false).anyTimes();
        EasyMock.expect(invoker1.getInterface()).andReturn(IHelloService.class).anyTimes();
        EasyMock.expect(invoker1.getUrl()).andReturn(turl.addParameter("name", "invoker1")).anyTimes();

        EasyMock.expect(invoker2.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker2.getInterface()).andReturn(IHelloService.class).anyTimes();
        EasyMock.expect(invoker2.getUrl()).andReturn(turl.addParameter("name", "invoker2")).anyTimes();

        EasyMock.expect(invoker3.isAvailable()).andReturn(false).anyTimes();
        EasyMock.expect(invoker3.getInterface()).andReturn(IHelloService.class).anyTimes();
        EasyMock.expect(invoker3.getUrl()).andReturn(turl.addParameter("name", "invoker3")).anyTimes();

        EasyMock.expect(invoker4.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker4.getInterface()).andReturn(IHelloService.class).anyTimes();
        EasyMock.expect(invoker4.getUrl()).andReturn(turl.addParameter("name", "invoker4")).anyTimes();

        EasyMock.expect(invoker5.isAvailable()).andReturn(false).anyTimes();
        EasyMock.expect(invoker5.getInterface()).andReturn(IHelloService.class).anyTimes();
        EasyMock.expect(invoker5.getUrl()).andReturn(turl.addParameter("name", "invoker5")).anyTimes();

        EasyMock.expect(mockedInvoker1.isAvailable()).andReturn(false).anyTimes();
        EasyMock.expect(mockedInvoker1.getInterface()).andReturn(IHelloService.class).anyTimes();
        EasyMock.expect(mockedInvoker1.getUrl()).andReturn(turl.setProtocol("mock")).anyTimes();

        EasyMock.replay(invoker1, invoker2, invoker3, invoker4, invoker5, mockedInvoker1);

        invokers.add(invoker1);
        dic = new StaticDirectory<IHelloService>(url, invokers, null);
        cluster = new AbstractClusterInvoker(dic) {
            @Override
            protected Result doInvoke(Invocation invocation, List invokers, LoadBalance loadbalance)
                    throws RpcException {
                return null;
            }
        };

        cluster_nocheck = new AbstractClusterInvoker(dic, url.addParameterIfAbsent(Constants.CLUSTER_AVAILABLE_CHECK_KEY, Boolean.FALSE.toString())) {
            @Override
            protected Result doInvoke(Invocation invocation, List invokers, LoadBalance loadbalance)
                    throws RpcException {
                return null;
            }
        };

    }

    @Test
    public void testSelect_Invokersize0() throws Exception {
        {
            Invoker invoker = cluster.select(null, null, null, null);
            Assert.assertEquals(null, invoker);
        }
        {
            invokers.clear();
            selectedInvokers.clear();
            Invoker invoker = cluster.select(null, null, invokers, null);
            Assert.assertEquals(null, invoker);
        }
    }

    @Test
    public void testSelect_Invokersize1() throws Exception {
        invokers.clear();
        invokers.add(invoker1);
        Invoker invoker = cluster.select(null, null, invokers, null);
        Assert.assertEquals(invoker1, invoker);
    }

    @Test
    public void testSelect_Invokersize2AndselectNotNull() throws Exception {
        invokers.clear();
        invokers.add(invoker1);
        invokers.add(invoker2);
        {
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            Invoker invoker = cluster.select(null, null, invokers, selectedInvokers);
            Assert.assertEquals(invoker2, invoker);
        }
        {
            selectedInvokers.clear();
            selectedInvokers.add(invoker2);
            Invoker invoker = cluster.select(null, null, invokers, selectedInvokers);
            Assert.assertEquals(invoker1, invoker);
        }
    }

    @Test
    public void testSelect_multiInvokers() throws Exception {
        testSelect_multiInvokers(RoundRobinLoadBalance.NAME);
        testSelect_multiInvokers(LeastActiveLoadBalance.NAME);
        testSelect_multiInvokers(RandomLoadBalance.NAME);
    }

    @Test
    public void testCloseAvailablecheck() {
        LoadBalance lb = EasyMock.createMock(LoadBalance.class);
        EasyMock.expect(lb.select(invokers, url, invocation)).andReturn(invoker1);
        EasyMock.replay(lb);
        initlistsize5();

        Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers);
        Assert.assertEquals(false, sinvoker.isAvailable());
        Assert.assertEquals(invoker1, sinvoker);

    }

    @Test
    public void testDonotSelectAgainAndNoCheckAvailable() {

        LoadBalance lb = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(RoundRobinLoadBalance.NAME);
        initlistsize5();
        {
            //Boundary condition test .
            selectedInvokers.clear();
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker4);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertSame(invoker1, sinvoker);
        }
        {
            //Boundary condition test .
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker4);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertSame(invoker2, sinvoker);
        }
        {
            //Boundary condition test .
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker4);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertSame(invoker3, sinvoker);
        }
        {
            //Boundary condition test .
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker4);
            Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertSame(invoker5, sinvoker);
        }
        {
            //Boundary condition test .
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker4);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertTrue(invokers.contains(sinvoker));
        }

    }

    @Test
    public void testSelectAgainAndCheckAvailable() {

        LoadBalance lb = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(RoundRobinLoadBalance.NAME);
        initlistsize5();
        {
            //Boundary condition test .
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertTrue(sinvoker == invoker4);
        }
        {
            //Boundary condition test .
            selectedInvokers.clear();
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker4);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertTrue(sinvoker == invoker2 || sinvoker == invoker4);
        }
        {
            //Boundary condition test .
            for (int i = 0; i < 100; i++) {
                selectedInvokers.clear();
                Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
                Assert.assertTrue(sinvoker == invoker2 || sinvoker == invoker4);
            }
        }
        {
            //Boundary condition test .
            for (int i = 0; i < 100; i++) {
                selectedInvokers.clear();
                selectedInvokers.add(invoker1);
                selectedInvokers.add(invoker3);
                selectedInvokers.add(invoker5);
                Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
                Assert.assertTrue(sinvoker == invoker2 || sinvoker == invoker4);
            }
        }
        {
            //Boundary condition test .
            for (int i = 0; i < 100; i++) {
                selectedInvokers.clear();
                selectedInvokers.add(invoker1);
                selectedInvokers.add(invoker3);
                selectedInvokers.add(invoker2);
                selectedInvokers.add(invoker4);
                selectedInvokers.add(invoker5);
                Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
                Assert.assertTrue(sinvoker == invoker2 || sinvoker == invoker4);
            }
        }
    }


    public void testSelect_multiInvokers(String lbname) throws Exception {

        int min = 1000, max = 5000;
        Double d = (Math.random() * (max - min + 1) + min);
        int runs = d.intValue();
        Assert.assertTrue(runs > min);
        LoadBalance lb = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(lbname);
        initlistsize5();
        for (int i = 0; i < runs; i++) {
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertEquals(true, sinvoker.isAvailable());
        }
        for (int i = 0; i < runs; i++) {
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertEquals(true, sinvoker.isAvailable());
        }
        for (int i = 0; i < runs; i++) {
            selectedInvokers.clear();
            selectedInvokers.add(invoker2);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertEquals(true, sinvoker.isAvailable());
        }
        for (int i = 0; i < runs; i++) {
            selectedInvokers.clear();
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker4);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertEquals(true, sinvoker.isAvailable());
        }
        for (int i = 0; i < runs; i++) {
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertEquals(true, sinvoker.isAvailable());
        }
        for (int i = 0; i < runs; i++) {
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker3);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertEquals(true, sinvoker.isAvailable());
        }
    }

    /**
     * Test balance.
     */
    @Test
    public void testSelectBalance() {

        LoadBalance lb = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(RoundRobinLoadBalance.NAME);
        initlistsize5();

        Map<Invoker, AtomicLong> counter = new ConcurrentHashMap<Invoker, AtomicLong>();
        for (Invoker invoker : invokers) {
            counter.put(invoker, new AtomicLong(0));
        }
        int runs = 1000;
        for (int i = 0; i < runs; i++) {
            selectedInvokers.clear();
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            counter.get(sinvoker).incrementAndGet();
        }

        for (Invoker minvoker : counter.keySet()) {
            Long count = counter.get(minvoker).get();
//            System.out.println(count);
            if (minvoker.isAvailable())
                Assert.assertTrue("count should > avg", count > runs / invokers.size());
        }

        Assert.assertEquals(runs, counter.get(invoker2).get() + counter.get(invoker4).get());
        ;

    }

    private void initlistsize5() {
        invokers.clear();
        selectedInvokers.clear();//Clear first, previous test case will make sure that the right invoker2 will be used.
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        invokers.add(invoker4);
        invokers.add(invoker5);
    }

    @Test()
    public void testTimeoutExceptionCode() {
        List<Invoker<DemoService>> invokers = new ArrayList<Invoker<DemoService>>();
        invokers.add(new Invoker<DemoService>() {

            public Class<DemoService> getInterface() {
                return DemoService.class;
            }

            public URL getUrl() {
                return URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/" + DemoService.class.getName());
            }

            public boolean isAvailable() {
                return false;
            }

            public Result invoke(Invocation invocation) throws RpcException {
                throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "test timeout");
            }

            public void destroy() {
            }
        });
        Directory<DemoService> directory = new StaticDirectory<DemoService>(invokers);
        FailoverClusterInvoker<DemoService> failoverClusterInvoker = new FailoverClusterInvoker<DemoService>(directory);
        try {
            failoverClusterInvoker.invoke(new RpcInvocation("sayHello", new Class<?>[0], new Object[0]));
            Assert.fail();
        } catch (RpcException e) {
            Assert.assertEquals(RpcException.TIMEOUT_EXCEPTION, e.getCode());
        }
        ForkingClusterInvoker<DemoService> forkingClusterInvoker = new ForkingClusterInvoker<DemoService>(directory);
        try {
            forkingClusterInvoker.invoke(new RpcInvocation("sayHello", new Class<?>[0], new Object[0]));
            Assert.fail();
        } catch (RpcException e) {
            Assert.assertEquals(RpcException.TIMEOUT_EXCEPTION, e.getCode());
        }
        FailfastClusterInvoker<DemoService> failfastClusterInvoker = new FailfastClusterInvoker<DemoService>(directory);
        try {
            failfastClusterInvoker.invoke(new RpcInvocation("sayHello", new Class<?>[0], new Object[0]));
            Assert.fail();
        } catch (RpcException e) {
            Assert.assertEquals(RpcException.TIMEOUT_EXCEPTION, e.getCode());
        }
    }

    /**
     * Test mock invoker selector works as expected
     */
    @Test
    public void testMockedInvokerSelect() {
        initlistsize5();
        invokers.add(mockedInvoker1);

        RpcInvocation mockedInvocation = new RpcInvocation();
        mockedInvocation.setMethodName("sayHello");
        mockedInvocation.setAttachment(Constants.INVOCATION_NEED_MOCK, "true");
        List<Invoker<IHelloService>> mockedInvokers = dic.list(mockedInvocation);
        Assert.assertEquals(1, mockedInvokers.size());

        List<Invoker<IHelloService>> invokers = dic.list(invocation);
        Assert.assertEquals(5, invokers.size());
    }

    public static interface IHelloService {
    }
}