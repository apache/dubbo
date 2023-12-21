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
import org.apache.dubbo.metrics.event.MetricsDispatcher;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.ENABLE_CONNECTIVITY_VALIDATION;
import static org.apache.dubbo.rpc.cluster.Constants.CLUSTER_AVAILABLE_CHECK_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.INVOCATION_NEED_MOCK;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * AbstractClusterInvokerTest
 */
@SuppressWarnings("rawtypes")
class MockAbstractClusterInvokerTest {
    List<Invoker<IHelloService>> invokers = new ArrayList<Invoker<IHelloService>>();
    List<Invoker<IHelloService>> selectedInvokers = new ArrayList<Invoker<IHelloService>>();
    AbstractClusterInvoker<IHelloService> cluster;
    AbstractClusterInvoker<IHelloService> cluster_nocheck;
    StaticDirectory<IHelloService> dic;
    RpcInvocation invocation = new RpcInvocation();
    URL url = URL.valueOf(
            "registry://localhost:9090/org.apache.dubbo.rpc.cluster.support.AbstractClusterInvokerTest.IHelloService?refer="
                    + URL.encode("application=abstractClusterInvokerTest"));
    URL consumerUrl = URL.valueOf(
            "dubbo://localhost?application=abstractClusterInvokerTest&refer=application%3DabstractClusterInvokerTest");

    Invoker<IHelloService> invoker1;
    Invoker<IHelloService> invoker2;
    Invoker<IHelloService> invoker3;
    Invoker<IHelloService> invoker4;
    Invoker<IHelloService> invoker5;
    Invoker<IHelloService> mockedInvoker1;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        System.setProperty(ENABLE_CONNECTIVITY_VALIDATION, "false");
    }

    @AfterEach
    public void teardown() throws Exception {
        RpcContext.removeContext();
    }

    @AfterAll
    public static void afterClass() {
        System.clearProperty(ENABLE_CONNECTIVITY_VALIDATION);
    }

    @SuppressWarnings({"unchecked"})
    @BeforeEach
    public void setUp() throws Exception {
        ApplicationModel.defaultModel().getBeanFactory().registerBean(MetricsDispatcher.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("application", "abstractClusterInvokerTest");
        url = url.putAttribute(REFER_KEY, attributes);

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

        cluster_nocheck =
                new AbstractClusterInvoker(
                        dic, url.addParameterIfAbsent(CLUSTER_AVAILABLE_CHECK_KEY, Boolean.FALSE.toString())) {
                    @Override
                    protected Result doInvoke(Invocation invocation, List invokers, LoadBalance loadbalance)
                            throws RpcException {
                        return null;
                    }
                };
    }

    private void initlistsize5() {
        invokers.clear();
        selectedInvokers
                .clear(); // Clear first, previous test case will make sure that the right invoker2 will be used.
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        invokers.add(invoker4);
        invokers.add(invoker5);
    }

    private void initDic() {
        dic.notify(invokers);
        dic.buildRouterChain();
    }

    /**
     * Test mock invoker selector works as expected
     */
    @Test
    void testMockedInvokerSelect() {
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

    public static interface IHelloService {}
}
