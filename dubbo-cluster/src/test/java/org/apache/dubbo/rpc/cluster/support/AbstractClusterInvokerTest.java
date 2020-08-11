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
package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.filter.DemoService;
import org.apache.dubbo.rpc.cluster.loadbalance.LeastActiveLoadBalance;
import org.apache.dubbo.rpc.cluster.loadbalance.RandomLoadBalance;
import org.apache.dubbo.rpc.cluster.loadbalance.RoundRobinLoadBalance;

import org.junit.jupiter.api.AfterEach;
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

import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.CLUSTER_AVAILABLE_CHECK_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.INVOCATION_NEED_MOCK;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * AbstractClusterInvokerTest
 */
@SuppressWarnings("rawtypes")
public class AbstractClusterInvokerTest {
    List<Invoker<IHelloService>> invokers = new ArrayList<Invoker<IHelloService>>();
    List<Invoker<IHelloService>> selectedInvokers = new ArrayList<Invoker<IHelloService>>();
    AbstractClusterInvoker<IHelloService> cluster;
    AbstractClusterInvoker<IHelloService> cluster_nocheck;
    StaticDirectory<IHelloService> dic;
    RpcInvocation invocation = new RpcInvocation();
    URL url = URL.valueOf("registry://localhost:9090/org.apache.dubbo.rpc.cluster.support.AbstractClusterInvokerTest.IHelloService?refer=" + URL.encode("application=abstractClusterInvokerTest"));

    Invoker<IHelloService> invoker1;
    Invoker<IHelloService> invoker2;
    Invoker<IHelloService> invoker3;
    Invoker<IHelloService> invoker4;
    Invoker<IHelloService> invoker5;
    Invoker<IHelloService> mockedInvoker1;


    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterEach
    public void teardown() throws Exception {
        RpcContext.getContext().clearAttachments();
    }

    @SuppressWarnings({"unchecked"})
    @BeforeEach
    public void setUp() throws Exception {
        invocation.setMethodName("sayHello");

        invoker1 = mock(Invoker.class);
        invoker2 = mock(Invoker.class);
        invoker3 = mock(Invoker.class);
        invoker4 = mock(Invoker.class);
        invoker5 = mock(Invoker.class);
        mockedInvoker1 = mock(Invoker.class);

        URL turl = URL.valueOf("test://test:11/test");

        given(invoker1.isAvailable()).willReturn(false);
        given(invoker1.getInterface()).willReturn(IHelloService.class);
        given(invoker1.getUrl()).willReturn(turl.setPort(1).addParameter("name", "invoker1"));

        given(invoker2.isAvailable()).willReturn(true);
        given(invoker2.getInterface()).willReturn(IHelloService.class);
        given(invoker2.getUrl()).willReturn(turl.setPort(2).addParameter("name", "invoker2"));

        given(invoker3.isAvailable()).willReturn(false);
        given(invoker3.getInterface()).willReturn(IHelloService.class);
        given(invoker3.getUrl()).willReturn(turl.setPort(3).addParameter("name", "invoker3"));

        given(invoker4.isAvailable()).willReturn(true);
        given(invoker4.getInterface()).willReturn(IHelloService.class);
        given(invoker4.getUrl()).willReturn(turl.setPort(4).addParameter("name", "invoker4"));

        given(invoker5.isAvailable()).willReturn(false);
        given(invoker5.getInterface()).willReturn(IHelloService.class);
        given(invoker5.getUrl()).willReturn(turl.setPort(5).addParameter("name", "invoker5"));

        given(mockedInvoker1.isAvailable()).willReturn(false);
        given(mockedInvoker1.getInterface()).willReturn(IHelloService.class);
        given(mockedInvoker1.getUrl()).willReturn(turl.setPort(999).setProtocol("mock"));

        invokers.add(invoker1);
        dic = new StaticDirectory<IHelloService>(url, invokers, null);
        cluster = new AbstractClusterInvoker(dic) {
            @Override
            protected Result doInvoke(Invocation invocation, List invokers, LoadBalance loadbalance)
                    throws RpcException {
                return null;
            }
        };

        cluster_nocheck = new AbstractClusterInvoker(dic, url.addParameterIfAbsent(CLUSTER_AVAILABLE_CHECK_KEY, Boolean.FALSE.toString())) {
            @Override
            protected Result doInvoke(Invocation invocation, List invokers, LoadBalance loadbalance)
                    throws RpcException {
                return null;
            }
        };

    }


    @Test
    public void testBindingAttachment() {
        final String attachKey = "attach";
        final String attachValue = "value";

        // setup attachment
        RpcContext.getContext().setAttachment(attachKey, attachValue);
        Map<String, Object> attachments = RpcContext.getContext().getObjectAttachments();
        Assertions.assertTrue( attachments != null && attachments.size() == 1,"set attachment failed!");

        cluster = new AbstractClusterInvoker(dic) {
            @Override
            protected Result doInvoke(Invocation invocation, List invokers, LoadBalance loadbalance)
                    throws RpcException {
                // attachment will be bind to invocation
                String value = invocation.getAttachment(attachKey);
                Assertions.assertNotNull(value);
                Assertions.assertEquals(attachValue, value, "binding attachment failed!");
                return null;
            }
        };

        // invoke
        cluster.invoke(invocation);
    }

    @Test
    public void testSelect_Invokersize0() throws Exception {
        LoadBalance l = cluster.initLoadBalance(invokers, invocation);
        Assertions.assertNotNull(l,"cluster.initLoadBalance returns null!");
        {
            Invoker invoker = cluster.select(l, null, null, null);
            Assertions.assertNull(invoker);
        }
        {
            invokers.clear();
            selectedInvokers.clear();
            Invoker invoker = cluster.select(l, null, invokers, null);
            Assertions.assertNull(invoker);
        }
    }

    @Test
    public void testSelect_Invokersize1() throws Exception {
        invokers.clear();
        invokers.add(invoker1);
        LoadBalance l = cluster.initLoadBalance(invokers, invocation);
        Assertions.assertNotNull(l,"cluster.initLoadBalance returns null!");
        Invoker invoker = cluster.select(l, null, invokers, null);
        Assertions.assertEquals(invoker1, invoker);
    }

    @Test
    public void testSelect_Invokersize2AndselectNotNull() throws Exception {
        invokers.clear();
        invokers.add(invoker2);
        invokers.add(invoker4);
        LoadBalance l = cluster.initLoadBalance(invokers, invocation);
        Assertions.assertNotNull(l,"cluster.initLoadBalance returns null!");
        {
            selectedInvokers.clear();
            selectedInvokers.add(invoker4);
            Invoker invoker = cluster.select(l, invocation, invokers, selectedInvokers);
            Assertions.assertEquals(invoker2, invoker);
        }
        {
            selectedInvokers.clear();
            selectedInvokers.add(invoker2);
            Invoker invoker = cluster.select(l, invocation, invokers, selectedInvokers);
            Assertions.assertEquals(invoker4, invoker);
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
        LoadBalance lb = mock(LoadBalance.class);
        Map<String, String> queryMap = StringUtils.parseQueryString(url.getParameterAndDecoded(REFER_KEY));
        URL tmpUrl = url.addParameters(queryMap).removeParameter(REFER_KEY).removeParameter(MONITOR_KEY);
        given(lb.select(invokers, tmpUrl, invocation)).willReturn(invoker1);
        initlistsize5();

        Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers);
        Assertions.assertFalse(sinvoker.isAvailable());
        Assertions.assertEquals(invoker1, sinvoker);

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
            Assertions.assertSame(invoker1, sinvoker);
        }
        {
            //Boundary condition test .
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker4);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers);
            Assertions.assertSame(invoker2, sinvoker);
        }
        {
            //Boundary condition test .
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker4);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers);
            Assertions.assertSame(invoker3, sinvoker);
        }
        {
            //Boundary condition test .
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker4);
            Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers);
            Assertions.assertSame(invoker5, sinvoker);
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
            Assertions.assertTrue(invokers.contains(sinvoker));
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
            Assertions.assertSame(sinvoker, invoker4);
        }
        {
            //Boundary condition test .
            selectedInvokers.clear();
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker4);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assertions.assertTrue(sinvoker == invoker2 || sinvoker == invoker4);
        }
        {
            //Boundary condition test .
            for (int i = 0; i < 100; i++) {
                selectedInvokers.clear();
                Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
                Assertions.assertTrue(sinvoker == invoker2 || sinvoker == invoker4);
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
                Assertions.assertTrue(sinvoker == invoker2 || sinvoker == invoker4);
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
                Assertions.assertTrue(sinvoker == invoker2 || sinvoker == invoker4);
            }
        }
    }


    public void testSelect_multiInvokers(String lbname) throws Exception {

        int min = 1000, max = 5000;
        Double d = (Math.random() * (max - min + 1) + min);
        int runs = d.intValue();
        Assertions.assertTrue(runs > min);
        LoadBalance lb = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(lbname);
        initlistsize5();
        for (int i = 0; i < runs; i++) {
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assertions.assertTrue(sinvoker.isAvailable());

            Mockito.clearInvocations(invoker1, invoker2, invoker3, invoker4, invoker5);
        }
        for (int i = 0; i < runs; i++) {
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assertions.assertTrue(sinvoker.isAvailable());

            Mockito.clearInvocations(invoker1, invoker2, invoker3, invoker4, invoker5);
        }
        for (int i = 0; i < runs; i++) {
            selectedInvokers.clear();
            selectedInvokers.add(invoker2);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assertions.assertTrue(sinvoker.isAvailable());

            Mockito.clearInvocations(invoker1, invoker2, invoker3, invoker4, invoker5);
        }
        for (int i = 0; i < runs; i++) {
            selectedInvokers.clear();
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker4);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assertions.assertTrue(sinvoker.isAvailable());

            Mockito.clearInvocations(invoker1, invoker2, invoker3, invoker4, invoker5);
        }
        for (int i = 0; i < runs; i++) {
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assertions.assertTrue(sinvoker.isAvailable());

            Mockito.clearInvocations(invoker1, invoker2, invoker3, invoker4, invoker5);
        }
        for (int i = 0; i < runs; i++) {

            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker3);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assertions.assertTrue(sinvoker.isAvailable());

            Mockito.clearInvocations(invoker1, invoker2, invoker3, invoker4, invoker5);
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

        for (Map.Entry<Invoker, AtomicLong> entry : counter.entrySet()) {
            Long count = entry.getValue().get();
//            System.out.println(count);
            if (entry.getKey().isAvailable())
                Assertions.assertTrue(count > runs / invokers.size(),"count should > avg");
        }

        Assertions.assertEquals(runs, counter.get(invoker2).get() + counter.get(invoker4).get());

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

    private void initDic() {
        dic.buildRouterChain();
    }

    @Test()
    public void testTimeoutExceptionCode() {
        List<Invoker<DemoService>> invokers = new ArrayList<Invoker<DemoService>>();
        invokers.add(new Invoker<DemoService>() {

            @Override
            public Class<DemoService> getInterface() {
                return DemoService.class;
            }

            public URL getUrl() {
                return URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/" + DemoService.class.getName());
            }

            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public Result invoke(Invocation invocation) throws RpcException {
                throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "test timeout");
            }

            @Override
            public void destroy() {
            }
        });
        Directory<DemoService> directory = new StaticDirectory<DemoService>(invokers);
        FailoverClusterInvoker<DemoService> failoverClusterInvoker = new FailoverClusterInvoker<DemoService>(directory);
        try {
            failoverClusterInvoker.invoke(new RpcInvocation("sayHello", DemoService.class.getName(), new Class<?>[0], new Object[0]));
            Assertions.fail();
        } catch (RpcException e) {
            Assertions.assertEquals(RpcException.TIMEOUT_EXCEPTION, e.getCode());
        }
        ForkingClusterInvoker<DemoService> forkingClusterInvoker = new ForkingClusterInvoker<DemoService>(directory);
        try {
            forkingClusterInvoker.invoke(new RpcInvocation("sayHello", DemoService.class.getName(), new Class<?>[0], new Object[0]));
            Assertions.fail();
        } catch (RpcException e) {
            Assertions.assertEquals(RpcException.TIMEOUT_EXCEPTION, e.getCode());
        }
        FailfastClusterInvoker<DemoService> failfastClusterInvoker = new FailfastClusterInvoker<DemoService>(directory);
        try {
            failfastClusterInvoker.invoke(new RpcInvocation("sayHello", DemoService.class.getName(), new Class<?>[0], new Object[0]));
            Assertions.fail();
        } catch (RpcException e) {
            Assertions.assertEquals(RpcException.TIMEOUT_EXCEPTION, e.getCode());
        }
    }

    /**
     * Test mock invoker selector works as expected
     */
    @Test
    public void testMockedInvokerSelect() {
        initlistsize5();
        invokers.add(mockedInvoker1);

        initDic();

        RpcInvocation mockedInvocation = new RpcInvocation();
        mockedInvocation.setMethodName("sayHello");
        mockedInvocation.setAttachment(INVOCATION_NEED_MOCK, "true");
        List<Invoker<IHelloService>> mockedInvokers = dic.list(mockedInvocation);
        Assertions.assertEquals(1, mockedInvokers.size());

        List<Invoker<IHelloService>> invokers = dic.list(invocation);
        Assertions.assertEquals(5, invokers.size());
    }

    public static interface IHelloService {
    }
}
