
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
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Directory;

import org.apache.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * FailbackClusterInvokerTest
 * <p>
 * add annotation @TestMethodOrder, the testARetryFailed Method must to first execution
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FailbackClusterInvokerTest {

    List<Invoker<FailbackClusterInvokerTest>> invokers = new ArrayList<Invoker<FailbackClusterInvokerTest>>();
    URL url = URL.valueOf("test://test:11/test?retries=2&failbacktasks=2");
    Invoker<FailbackClusterInvokerTest> invoker = mock(Invoker.class);
    RpcInvocation invocation = new RpcInvocation();
    Directory<FailbackClusterInvokerTest> dic;
    Result result = new AppResponse();

    /**
     * @throws java.lang.Exception
     */

    @BeforeEach
    public void setUp() throws Exception {

        dic = mock(Directory.class);
        given(dic.getUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(invokers);
        given(dic.getInterface()).willReturn(FailbackClusterInvokerTest.class);

        invocation.setMethodName("method1");

        invokers.add(invoker);
    }

    @AfterEach
    public void tearDown() {

        dic = null;
        invocation = new RpcInvocation();
        invokers.clear();
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
    @Order(1)
    public void testInvokeException() {
        resetInvokerToException();
        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<FailbackClusterInvokerTest>(
                dic);
        invoker.invoke(invocation);
        Assertions.assertNull(RpcContext.getContext().getInvoker());
        DubboAppender.clear();
    }

    @Test
    @Order(2)
    public void testInvokeNoException() {

        resetInvokerToNoException();

        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<FailbackClusterInvokerTest>(
                dic);
        Result ret = invoker.invoke(invocation);
        Assertions.assertSame(result, ret);
    }

    @Test
    @Order(3)
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

    @Disabled
    @Test
    @Order(4)
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
        Assertions.assertNull(RpcContext.getContext().getInvoker());
//        invoker.retryFailed();// when retry the invoker which get from failed map already is not the mocked invoker,so
        //Ensure that the main thread is online
        CountDownLatch countDown = new CountDownLatch(1);
        countDown.await(15000L, TimeUnit.MILLISECONDS);
        LogUtil.stop();
        Assertions.assertEquals(4, LogUtil.findMessage(Level.ERROR, "Failed retry to invoke method"), "must have four error message ");
        Assertions.assertEquals(2, LogUtil.findMessage(Level.ERROR, "Failed retry times exceed threshold"), "must have two error message ");
        Assertions.assertEquals(1, LogUtil.findMessage(Level.ERROR, "Failback background works error"), "must have one error message ");
        // it can be invoke successfully
    }
}