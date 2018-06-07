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

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.cluster.Directory;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * FailfastClusterInvokerTest
 *
 */
@SuppressWarnings("unchecked")
public class FailfastClusterInvokerTest {
    List<Invoker<FailfastClusterInvokerTest>> invokers = new ArrayList<Invoker<FailfastClusterInvokerTest>>();
    URL url = URL.valueOf("test://test:11/test");
    Invoker<FailfastClusterInvokerTest> invoker1 = mock(Invoker.class);
    RpcInvocation invocation = new RpcInvocation();
    Directory<FailfastClusterInvokerTest> dic;
    Result result = new RpcResult();

    /**
     * @throws java.lang.Exception
     */

    @Before
    public void setUp() throws Exception {

        dic = mock(Directory.class);

        given(dic.getUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(invokers);
        given(dic.getInterface()).willReturn(FailfastClusterInvokerTest.class);

        invocation.setMethodName("method1");

        invokers.add(invoker1);
    }

    private void resetInvoker1ToException() {
        given(invoker1.invoke(invocation)).willThrow(new RuntimeException());
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.getInterface()).willReturn(FailfastClusterInvokerTest.class);
    }

    private void resetInvoker1ToNoException() {
        given(invoker1.invoke(invocation)).willReturn(result);
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.getInterface()).willReturn(FailfastClusterInvokerTest.class);
    }

    @Test(expected = RpcException.class)
    public void testInvokeExceptoin() {
        resetInvoker1ToException();
        FailfastClusterInvoker<FailfastClusterInvokerTest> invoker = new FailfastClusterInvoker<FailfastClusterInvokerTest>(dic);
        invoker.invoke(invocation);
        Assert.assertSame(invoker1, RpcContext.getContext().getInvoker());
    }

    @Test()
    public void testInvokeNoExceptoin() {

        resetInvoker1ToNoException();

        FailfastClusterInvoker<FailfastClusterInvokerTest> invoker = new FailfastClusterInvoker<FailfastClusterInvokerTest>(dic);
        Result ret = invoker.invoke(invocation);
        Assert.assertSame(result, ret);
    }

    @Test()
    public void testNoInvoke() {
        dic = mock(Directory.class);

        given(dic.getUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(null);
        given(dic.getInterface()).willReturn(FailfastClusterInvokerTest.class);

        invocation.setMethodName("method1");

        invokers.add(invoker1);

        resetInvoker1ToNoException();

        FailfastClusterInvoker<FailfastClusterInvokerTest> invoker = new FailfastClusterInvoker<FailfastClusterInvokerTest>(dic);
        try {
            invoker.invoke(invocation);
            fail();
        } catch (RpcException expected) {
            assertFalse(expected.getCause() instanceof RpcException);
        }
    }

}