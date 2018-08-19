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
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcResult;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.cluster.Directory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * ForkingClusterInvokerTest
 */
@SuppressWarnings("unchecked")
public class ForkingClusterInvokerTest {

    List<Invoker<ForkingClusterInvokerTest>> invokers = new ArrayList<Invoker<ForkingClusterInvokerTest>>();
    URL url = URL.valueOf("test://test:11/test?forks=2");
    Invoker<ForkingClusterInvokerTest> invoker1 = mock(Invoker.class);
    Invoker<ForkingClusterInvokerTest> invoker2 = mock(Invoker.class);
    Invoker<ForkingClusterInvokerTest> invoker3 = mock(Invoker.class);
    RpcInvocation invocation = new RpcInvocation();
    Directory<ForkingClusterInvokerTest> dic;
    Result result = new RpcResult();

    /**
     * @throws java.lang.Exception
     */

    @Before
    public void setUp() throws Exception {

        dic = mock(Directory.class);

        given(dic.getUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(invokers);
        given(dic.getInterface()).willReturn(ForkingClusterInvokerTest.class);

        invocation.setMethodName("method1");

        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);

    }

    private void resetInvokerToException() {
        given(invoker1.invoke(invocation)).willThrow(new RuntimeException());
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.isAvailable()).willReturn(true);
        given(invoker1.getInterface()).willReturn(ForkingClusterInvokerTest.class);

        given(invoker2.invoke(invocation)).willThrow(new RuntimeException());
        given(invoker2.getUrl()).willReturn(url);
        given(invoker2.isAvailable()).willReturn(true);
        given(invoker2.getInterface()).willReturn(ForkingClusterInvokerTest.class);

        given(invoker3.invoke(invocation)).willThrow(new RuntimeException());
        given(invoker3.getUrl()).willReturn(url);
        given(invoker3.isAvailable()).willReturn(true);
        given(invoker3.getInterface()).willReturn(ForkingClusterInvokerTest.class);
    }

    private void resetInvokerToNoException() {
        given(invoker1.invoke(invocation)).willReturn(result);
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.isAvailable()).willReturn(true);
        given(invoker1.getInterface()).willReturn(ForkingClusterInvokerTest.class);

        given(invoker2.invoke(invocation)).willReturn(result);
        given(invoker2.getUrl()).willReturn(url);
        given(invoker2.isAvailable()).willReturn(true);
        given(invoker2.getInterface()).willReturn(ForkingClusterInvokerTest.class);

        given(invoker3.invoke(invocation)).willReturn(result);
        given(invoker3.getUrl()).willReturn(url);
        given(invoker3.isAvailable()).willReturn(true);
        given(invoker3.getInterface()).willReturn(ForkingClusterInvokerTest.class);
    }

    @Test
    public void testInvokeException() {
        resetInvokerToException();
        ForkingClusterInvoker<ForkingClusterInvokerTest> invoker = new ForkingClusterInvoker<ForkingClusterInvokerTest>(
                dic);

        try {
            invoker.invoke(invocation);
            Assert.fail();
        } catch (RpcException expected) {
            Assert.assertTrue(expected.getMessage().contains("Failed to forking invoke provider"));
            assertFalse(expected.getCause() instanceof RpcException);
        }
    }

    @Test
    public void testClearRpcContext() {
        resetInvokerToException();
        ForkingClusterInvoker<ForkingClusterInvokerTest> invoker = new ForkingClusterInvoker<ForkingClusterInvokerTest>(
                dic);

        String attachKey = "attach";
        String attachValue = "value";

        RpcContext.getContext().setAttachment(attachKey, attachValue);

        Map<String, String> attachments = RpcContext.getContext().getAttachments();
        Assert.assertTrue("set attachment failed!", attachments != null && attachments.size() == 1);
        try {
            invoker.invoke(invocation);
            Assert.fail();
        } catch (RpcException expected) {
            Assert.assertTrue("Successed to forking invoke provider !", expected.getMessage().contains("Failed to forking invoke provider"));
            assertFalse(expected.getCause() instanceof RpcException);
        }
        Map<String, String> afterInvoke = RpcContext.getContext().getAttachments();
        Assert.assertTrue("clear attachment failed!", afterInvoke != null && afterInvoke.size() == 0);
    }

    @Test()
    public void testInvokeNoException() {

        resetInvokerToNoException();

        ForkingClusterInvoker<ForkingClusterInvokerTest> invoker = new ForkingClusterInvoker<ForkingClusterInvokerTest>(
                dic);
        Result ret = invoker.invoke(invocation);
        Assert.assertSame(result, ret);
    }

}