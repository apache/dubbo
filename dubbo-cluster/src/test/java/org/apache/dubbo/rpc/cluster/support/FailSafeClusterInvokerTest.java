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
import org.apache.dubbo.common.utils.LogUtil;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.filter.DemoService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * FailfastClusterInvokerTest
 *
 */
@SuppressWarnings("unchecked")
public class FailSafeClusterInvokerTest {
    List<Invoker<DemoService>> invokers = new ArrayList<Invoker<DemoService>>();
    URL url = URL.valueOf("test://test:11/test");
    Invoker<DemoService> invoker = mock(Invoker.class);
    RpcInvocation invocation = new RpcInvocation();
    Directory<DemoService> dic;
    Result result = new AppResponse();

    /**
     * @throws java.lang.Exception
     */

    @BeforeEach
    public void setUp() throws Exception {

        dic = mock(Directory.class);

        given(dic.getUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(invokers);
        given(dic.getInterface()).willReturn(DemoService.class);
        invocation.setMethodName("method1");

        invokers.add(invoker);
    }

    private void resetInvokerToException() {
        given(invoker.invoke(invocation)).willThrow(new RuntimeException());
        given(invoker.getUrl()).willReturn(url);
        given(invoker.getInterface()).willReturn(DemoService.class);
    }

    private void resetInvokerToNoException() {
        given(invoker.invoke(invocation)).willReturn(result);
        given(invoker.getUrl()).willReturn(url);
        given(invoker.getInterface()).willReturn(DemoService.class);
    }

    //TODO assert error log
    @Test
    public void testInvokeExceptoin() {
        resetInvokerToException();
        FailsafeClusterInvoker<DemoService> invoker = new FailsafeClusterInvoker<DemoService>(dic);
        invoker.invoke(invocation);
        Assertions.assertNull(RpcContext.getContext().getInvoker());
    }

    @Test()
    public void testInvokeNoExceptoin() {

        resetInvokerToNoException();

        FailsafeClusterInvoker<DemoService> invoker = new FailsafeClusterInvoker<DemoService>(dic);
        Result ret = invoker.invoke(invocation);
        Assertions.assertSame(result, ret);
    }

    @Test()
    public void testNoInvoke() {
        dic = mock(Directory.class);

        given(dic.getUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(null);
        given(dic.getInterface()).willReturn(DemoService.class);

        invocation.setMethodName("method1");

        resetInvokerToNoException();

        FailsafeClusterInvoker<DemoService> invoker = new FailsafeClusterInvoker<DemoService>(dic);
        LogUtil.start();
        invoker.invoke(invocation);
        assertTrue(LogUtil.findMessage("No provider") > 0);
        LogUtil.stop();
    }

}