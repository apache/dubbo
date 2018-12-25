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
import org.apache.dubbo.common.utils.DubboAppender;
import org.apache.dubbo.common.utils.LogUtil;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcResult;
import org.apache.dubbo.rpc.cluster.Directory;

import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

 /**
  * FailbackClusterInvokerTest
  *
  * add annotation @FixMethodOrder, the testARetryFailed Method must to first execution
 */
@SuppressWarnings("unchecked")
@FixMethodOrder(org.junit.runners.MethodSorters.NAME_ASCENDING)
public class FailbackClusterInvokerTest {

    List<Invoker<FailbackClusterInvokerTest>> invokers = new ArrayList<Invoker<FailbackClusterInvokerTest>>();
    URL url = URL.valueOf("test://test:11/test?retries=2&failbacktasks=2");
    Invoker<FailbackClusterInvokerTest> invoker = mock(Invoker.class);
    RpcInvocation invocation = new RpcInvocation();
    Directory<FailbackClusterInvokerTest> dic;
    Result result = new RpcResult();

    /**
     * @throws java.lang.Exception
     */

    @Before
    public void setUp() throws Exception {

        dic = mock(Directory.class);
        given(dic.getUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(invokers);
        given(dic.getInterface()).willReturn(FailbackClusterInvokerTest.class);

        invocation.setMethodName("method1");

        invokers.add(invoker);
    }

    private void resetInvokerToException() {
        given(invoker.invoke(invocation)).willThrow(new RuntimeException());
        given(invoker.getUrl()).willReturn(url);
        given(invoker.getInterface()).willReturn(FailbackClusterInvokerTest.class);
    }

    private void resetInvokerToNoException() {
        given(invoker.invoke(invocation)).willReturn(result);
        given(invoker.getUrl()).willReturn(url);
        given(invoker.getInterface()).willReturn(FailbackClusterInvokerTest.class);
    }

    @Test
    public void testInvokeException() {
        resetInvokerToException();
        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<FailbackClusterInvokerTest>(
                dic);
        invoker.invoke(invocation);
        Assert.assertNull(RpcContext.getContext().getInvoker());
        DubboAppender.clear();
    }

    @Test()
    public void testInvokeNoException() {

        resetInvokerToNoException();

        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<FailbackClusterInvokerTest>(
                dic);
        Result ret = invoker.invoke(invocation);
        Assert.assertSame(result, ret);
    }

    @Test()
    public void testNoInvoke() {
        dic = mock(Directory.class);

        given(dic.getUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(null);
        given(dic.getInterface()).willReturn(FailbackClusterInvokerTest.class);

        invocation.setMethodName("method1");

        invokers.add(invoker);

        resetInvokerToNoException();

        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<FailbackClusterInvokerTest>(
                dic);
        LogUtil.start();
        DubboAppender.clear();
        invoker.invoke(invocation);
        assertEquals(1, LogUtil.findMessage("Failback to invoke"));
        LogUtil.stop();
    }

    @Test()
    public void testARetryFailed() throws Exception {
        //Test retries and

        resetInvokerToException();

        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<FailbackClusterInvokerTest>(
                dic);
        LogUtil.start();
        DubboAppender.clear();
        invoker.invoke(invocation);
        invoker.invoke(invocation);
        invoker.invoke(invocation);
        Assert.assertNull(RpcContext.getContext().getInvoker());
//        invoker.retryFailed();// when retry the invoker which get from failed map already is not the mocked invoker,so
        //Ensure that the main thread is online
        CountDownLatch countDown = new CountDownLatch(1);
        countDown.await(15000L, TimeUnit.MILLISECONDS);
        LogUtil.stop();
        Assert.assertEquals("must have four error message ", 4, LogUtil.findMessage(Level.ERROR, "Failed retry to invoke method"));
        Assert.assertEquals("must have two error message ", 2, LogUtil.findMessage(Level.ERROR, "Failed retry times exceed threshold"));
        Assert.assertEquals("must have one error message ", 1, LogUtil.findMessage(Level.ERROR, "Failback background works error"));
        // it can be invoke successfully
    }
}