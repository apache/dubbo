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
package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcStatus;
import org.apache.dubbo.rpc.support.BlockMyInvoker;
import org.apache.dubbo.rpc.support.MockInvocation;
import org.apache.dubbo.rpc.support.MyInvoker;
import org.apache.dubbo.rpc.support.RuntimeExceptionInvoker;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * ActiveLimitFilterTest.java
 */
class ActiveLimitFilterTest {

    ActiveLimitFilter activeLimitFilter = new ActiveLimitFilter();

    @Test
    void testInvokeNoActives() {
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&actives=0");
        Invoker<ActiveLimitFilterTest> invoker = new MyInvoker<ActiveLimitFilterTest>(url);
        Invocation invocation = new MockInvocation();
        activeLimitFilter.invoke(invoker, invocation);
    }

    @Test
    void testInvokeLessActives() {
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&actives=10");
        Invoker<ActiveLimitFilterTest> invoker = new MyInvoker<ActiveLimitFilterTest>(url);
        Invocation invocation = new MockInvocation();
        activeLimitFilter.invoke(invoker, invocation);
    }

    @Test
    void testInvokeGreaterActives() {
        AtomicInteger count = new AtomicInteger(0);
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&actives=1&timeout=1");
        final Invoker<ActiveLimitFilterTest> invoker = new BlockMyInvoker<ActiveLimitFilterTest>(url, 100);
        final Invocation invocation = new MockInvocation();
        final CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < 100; i++) {
            Thread thread = new Thread(new Runnable() {

                public void run() {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < 100; i++) {
                        try {
                            activeLimitFilter.invoke(invoker, invocation);
                        } catch (RpcException expected) {
                            count.incrementAndGet();
                        }
                    }
                }
            });
            thread.start();
        }
        latch.countDown();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertNotSame(0, count.intValue());
    }

    @Test
    void testInvokeTimeOut() {
        int totalThread = 100;
        int maxActives = 10;
        long timeout = 1;
        long blockTime = 100;
        AtomicInteger count = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latchBlocking = new CountDownLatch(totalThread);
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&actives=" + maxActives
                + "&timeout=" + timeout);
        final Invoker<ActiveLimitFilterTest> invoker = new BlockMyInvoker<ActiveLimitFilterTest>(url, blockTime);
        final Invocation invocation = new MockInvocation();
        RpcStatus.removeStatus(url);
        RpcStatus.removeStatus(url, invocation.getMethodName());
        for (int i = 0; i < totalThread; i++) {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {
                            Result asyncResult = activeLimitFilter.invoke(invoker, invocation);
                            Result result = asyncResult.get();
                            activeLimitFilter.onResponse(result, invoker, invocation);
                        } catch (RpcException expected) {
                            count.incrementAndGet();
                        } catch (Exception e) {
                            fail();
                        }
                    } finally {
                        latchBlocking.countDown();
                    }
                }
            });
            thread.start();
        }
        latch.countDown();

        try {
            latchBlocking.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(90, count.intValue());
    }

    @Test
    void testInvokeNotTimeOut() {
        int totalThread = 100;
        int maxActives = 10;
        long timeout = 100000;
        long blockTime = 0;
        AtomicInteger count = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latchBlocking = new CountDownLatch(totalThread);
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&actives=" + maxActives
                + "&timeout=" + timeout);
        final Invoker<ActiveLimitFilterTest> invoker = new BlockMyInvoker<ActiveLimitFilterTest>(url, blockTime);
        final Invocation invocation = new MockInvocation();
        for (int i = 0; i < totalThread; i++) {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {
                            Result asyncResult = activeLimitFilter.invoke(invoker, invocation);
                            Result result = asyncResult.get();
                            activeLimitFilter.onResponse(result, invoker, invocation);
                        } catch (RpcException expected) {
                            count.incrementAndGet();
                            activeLimitFilter.onError(expected, invoker, invocation);
                        } catch (Exception e) {
                            fail();
                        }
                    } finally {
                        latchBlocking.countDown();
                    }
                }
            });
            thread.start();
        }
        latch.countDown();

        try {
            latchBlocking.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(0, count.intValue());
    }

    @Test
    void testInvokeRuntimeException() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&actives=0");
            Invoker<ActiveLimitFilterTest> invoker = new RuntimeExceptionInvoker(url);
            Invocation invocation = new MockInvocation();
            RpcStatus count = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName());
            int beforeExceptionActiveCount = count.getActive();
            activeLimitFilter.invoke(invoker, invocation);
            int afterExceptionActiveCount = count.getActive();
            assertEquals(
                    beforeExceptionActiveCount,
                    afterExceptionActiveCount,
                    "After exception active count should be same");
        });
    }

    @Test
    void testInvokeRuntimeExceptionWithActiveCountMatch() {
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&actives=0");
        Invoker<ActiveLimitFilterTest> invoker = new RuntimeExceptionInvoker(url);
        Invocation invocation = new MockInvocation();
        RpcStatus count = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName());
        int beforeExceptionActiveCount = count.getActive();
        try {
            activeLimitFilter.invoke(invoker, invocation);
        } catch (RuntimeException ex) {
            activeLimitFilter.onError(ex, invoker, invocation);
            int afterExceptionActiveCount = count.getActive();
            assertEquals(
                    beforeExceptionActiveCount,
                    afterExceptionActiveCount,
                    "After exception active count should be same");
        }
    }

    @Test
    void testOnErrorReleasesCountWhenDownstreamThrowsLimitExceeded() {
        URL url = URL.valueOf("test://test:11/limit-leak?accesslog=true&group=dubbo&version=1.1&actives=1&timeout=1000");
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("invoke");

        @SuppressWarnings("unchecked")
        Invoker<ActiveLimitFilterTest> target = mock(Invoker.class);
        given(target.getUrl()).willReturn(url);
        given(target.getInterface()).willReturn(ActiveLimitFilterTest.class);
        given(target.invoke(any(Invocation.class))).willThrow(
                new RpcException(RpcException.LIMIT_EXCEEDED_EXCEPTION, "downstream rejected"));

        assertThrows(RpcException.class, () -> activeLimitFilter.invoke(target, invocation));
        // beginCount succeeded in invoke(); the downstream LIMIT_EXCEEDED must
        // still release the taken slot instead of leaking it.
        activeLimitFilter.onError(
                new RpcException(RpcException.LIMIT_EXCEEDED_EXCEPTION, "downstream rejected"),
                target,
                invocation);

        assertEquals(0, RpcStatus.getStatus(url, "invoke").getActive());
    }

    @Test
    void testOnErrorKeepsCountWhenFilterItselfRejected() {
        URL url = URL.valueOf("test://test:11/limit-self-reject?accesslog=true&group=dubbo&version=1.1&actives=1&timeout=1");
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("invoke");
        assertTrue(RpcStatus.beginCount(url, "invoke", 1));

        @SuppressWarnings("unchecked")
        Invoker<ActiveLimitFilterTest> target = mock(Invoker.class);
        given(target.getUrl()).willReturn(url);
        given(target.getInterface()).willReturn(ActiveLimitFilterTest.class);

        // Admission fails inside invoke() before the counted marker is set
        assertThrows(RpcException.class, () -> activeLimitFilter.invoke(target, invocation));
        activeLimitFilter.onError(
                new RpcException(RpcException.LIMIT_EXCEEDED_EXCEPTION, "self rejected"), target, invocation);

        // The slot held by the concurrent invocation stays untouched (no double release)
        assertEquals(1, RpcStatus.getStatus(url, "invoke").getActive());
        RpcStatus.endCount(url, "invoke", 0, false);
    }
}
