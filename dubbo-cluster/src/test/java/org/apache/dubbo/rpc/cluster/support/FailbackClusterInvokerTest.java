
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
import org.apache.dubbo.common.constants.CommonConstants;
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.RETRIES_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * FailbackClusterInvokerTest
 * <p>
 * add annotation @TestMethodOrder, the testARetryFailed Method must to first execution
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FailbackClusterInvokerTest {

    List<Invoker<FailbackClusterInvokerTest>> invokers = new ArrayList<>();
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
        given(dic.getConsumerUrl()).willReturn(url);
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
    void testInvokeWithIllegalRetriesParam() {
        URL url = URL.valueOf("test://test:11/test?retries=-1&failbacktasks=2");
        Directory<FailbackClusterInvokerTest> dic = mock(Directory.class);
        given(dic.getUrl()).willReturn(url);
        given(dic.getConsumerUrl()).willReturn(url);
        given(dic.getInterface()).willReturn(FailbackClusterInvokerTest.class);
        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<>(dic);
        invoker.invoke(invocation);
        Assertions.assertNull(RpcContext.getServiceContext().getInvoker());
        DubboAppender.clear();
    }

    @Test
    void testInvokeWithIllegalFailbacktasksParam() {
        URL url = URL.valueOf("test://test:11/test?retries=2&failbacktasks=-1");
        Directory<FailbackClusterInvokerTest> dic = mock(Directory.class);
        given(dic.getUrl()).willReturn(url);
        given(dic.getConsumerUrl()).willReturn(url);
        given(dic.getInterface()).willReturn(FailbackClusterInvokerTest.class);
        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<>(dic);
        invoker.invoke(invocation);
        Assertions.assertNull(RpcContext.getServiceContext().getInvoker());
        DubboAppender.clear();
    }

    @Test
    @Order(1)
    public void testInvokeException() {
        resetInvokerToException();
        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<>(dic);
        invoker.invoke(invocation);
        Assertions.assertNull(RpcContext.getServiceContext().getInvoker());
        DubboAppender.clear();
    }

    @Test
    @Order(2)
    public void testInvokeNoException() {

        resetInvokerToNoException();

        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<>(dic);
        Result ret = invoker.invoke(invocation);
        Assertions.assertSame(result, ret);
    }

    @Test
    @Order(3)
    public void testNoInvoke() {
        dic = mock(Directory.class);

        given(dic.getUrl()).willReturn(url);
        given(dic.getConsumerUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(null);
        given(dic.getInterface()).willReturn(FailbackClusterInvokerTest.class);

        invocation.setMethodName("method1");

        invokers.add(invoker);

        resetInvokerToNoException();

        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<>(dic);
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

        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<>(dic);
        LogUtil.start();
        DubboAppender.clear();
        invoker.invoke(invocation);
        invoker.invoke(invocation);
        invoker.invoke(invocation);
        Assertions.assertNull(RpcContext.getServiceContext().getInvoker());
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



    private long getRetryFailedPeriod() throws NoSuchFieldException, IllegalAccessException {
        Field retryFailedPeriod = FailbackClusterInvoker.class.getDeclaredField("RETRY_FAILED_PERIOD");
        retryFailedPeriod.setAccessible(true);
        return retryFailedPeriod.getLong(FailbackClusterInvoker.class);
    }

    @Test
    @Order(5)
    public void testInvokeRetryTimesWithZeroValue() throws InterruptedException, NoSuchFieldException,
            IllegalAccessException {
        int retries = 0;
        resetInvokerToException();
        given(dic.getConsumerUrl()).willReturn(url.addParameter(RETRIES_KEY, retries));

        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<>(dic);
        LogUtil.start();
        DubboAppender.clear();

        invocation.setMethodName("testInvokeRetryTimesWithZeroValue");
        invoker.invoke(invocation);

        CountDownLatch countDown = new CountDownLatch(1);
        countDown.await(getRetryFailedPeriod() * (retries + 1), TimeUnit.SECONDS);
        LogUtil.stop();
        Assertions.assertEquals(0, LogUtil.findMessage(Level.INFO, "Attempt to retry to invoke method " +
                "testInvokeRetryTimesWithZeroValue"), "No retry messages allowed");
    }

    @Test
    @Order(6)
    public void testInvokeRetryTimesWithTwoValue() throws InterruptedException, NoSuchFieldException,
            IllegalAccessException {
        int retries = 2;
        resetInvokerToException();
        given(dic.getConsumerUrl()).willReturn(url.addParameter(RETRIES_KEY, retries));

        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<>(dic);
        LogUtil.start();
        DubboAppender.clear();

        invocation.setMethodName("testInvokeRetryTimesWithTwoValue");
        invoker.invoke(invocation);

        CountDownLatch countDown = new CountDownLatch(1);
        countDown.await(getRetryFailedPeriod() * (retries + 1), TimeUnit.SECONDS);
        LogUtil.stop();
        Assertions.assertEquals(2, LogUtil.findMessage(Level.INFO, "Attempt to retry to invoke method " +
                "testInvokeRetryTimesWithTwoValue"), "Must have two error message ");
    }

    @Test
    @Order(7)
    public void testInvokeRetryTimesWithDefaultValue() throws InterruptedException, NoSuchFieldException,
            IllegalAccessException {
        resetInvokerToException();
        given(dic.getConsumerUrl()).willReturn(URL.valueOf("test://test:11/test"));

        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<>(dic);
        LogUtil.start();
        DubboAppender.clear();

        invocation.setMethodName("testInvokeRetryTimesWithDefaultValue");
        invoker.invoke(invocation);

        CountDownLatch countDown = new CountDownLatch(1);
        countDown.await(getRetryFailedPeriod() * (CommonConstants.DEFAULT_FAILBACK_TIMES + 1), TimeUnit.SECONDS);
        LogUtil.stop();
        Assertions.assertEquals(3, LogUtil.findMessage(Level.INFO, "Attempt to retry to invoke method " +
                "testInvokeRetryTimesWithDefaultValue"), "Must have three error message ");
    }

    @Test
    @Order(8)
    public void testInvokeRetryTimesWithIllegalValue() throws InterruptedException, NoSuchFieldException,
            IllegalAccessException {
        resetInvokerToException();
        given(dic.getConsumerUrl()).willReturn(url.addParameter(RETRIES_KEY, -100));

        FailbackClusterInvoker<FailbackClusterInvokerTest> invoker = new FailbackClusterInvoker<>(dic);
        LogUtil.start();
        DubboAppender.clear();

        invocation.setMethodName("testInvokeRetryTimesWithIllegalValue");
        invoker.invoke(invocation);

        CountDownLatch countDown = new CountDownLatch(1);
        countDown.await(getRetryFailedPeriod() * (CommonConstants.DEFAULT_FAILBACK_TIMES + 1), TimeUnit.SECONDS);
        LogUtil.stop();
        Assertions.assertEquals(3, LogUtil.findMessage(Level.INFO, "Attempt to retry to invoke method " +
                "testInvokeRetryTimesWithIllegalValue"), "Must have three error message ");
    }
}
