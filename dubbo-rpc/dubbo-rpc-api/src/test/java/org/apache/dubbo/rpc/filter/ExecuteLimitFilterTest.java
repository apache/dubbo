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
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcStatus;
import org.apache.dubbo.rpc.support.BlockMyInvoker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class ExecuteLimitFilterTest {

    private ExecuteLimitFilter executeLimitFilter = new ExecuteLimitFilter();

    @Test
    void testNoExecuteLimitInvoke() {
        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.invoke(any(Invocation.class))).thenReturn(new AppResponse("result"));
        when(invoker.getUrl()).thenReturn(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1"));

        Invocation invocation = Mockito.mock(Invocation.class);
        when(invocation.getMethodName()).thenReturn("testNoExecuteLimitInvoke");

        Result result = executeLimitFilter.invoke(invoker, invocation);
        Assertions.assertEquals("result", result.getValue());
    }

    @Test
    void testExecuteLimitInvoke() {
        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.invoke(any(Invocation.class))).thenReturn(new AppResponse("result"));
        when(invoker.getUrl()).thenReturn(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&executes=10"));

        Invocation invocation = Mockito.mock(Invocation.class);
        when(invocation.getMethodName()).thenReturn("testExecuteLimitInvoke");

        Result result = executeLimitFilter.invoke(invoker, invocation);
        Assertions.assertEquals("result", result.getValue());
    }

    @Test
    void testExecuteLimitInvokeWithException() {
        Invoker invoker = Mockito.mock(Invoker.class);
        doThrow(new RpcException())
                .when(invoker).invoke(any(Invocation.class));

        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&executes=10");
        when(invoker.getUrl()).thenReturn(url);

        Invocation invocation = Mockito.mock(Invocation.class);
        when(invocation.getMethodName()).thenReturn("testExecuteLimitInvokeWitException");

        try {
            executeLimitFilter.invoke(invoker, invocation);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof RpcException);
            executeLimitFilter.onError(e, invoker, invocation);
        }
        Assertions.assertEquals(1, RpcStatus.getStatus(url, invocation.getMethodName()).getFailed());
        RpcStatus.removeStatus(url, invocation.getMethodName());
    }

    @Test
    void testMoreThanExecuteLimitInvoke() {
        int maxExecute = 10;
        int totalExecute = 20;
        final AtomicInteger failed = new AtomicInteger(0);

        final Invocation invocation = Mockito.mock(RpcInvocation.class);
        when(invocation.getMethodName()).thenReturn("testMoreThanExecuteLimitInvoke");

        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&executes=" + maxExecute);
        final Invoker<ExecuteLimitFilter> invoker = new BlockMyInvoker<>(url, 1000);

        final CountDownLatch latchThatWaitsAllThreadStarted = new CountDownLatch(1);
        final CountDownLatch latchThatWaitsAllThreadFinished = new CountDownLatch(totalExecute);

        for (int i = 0; i < totalExecute; i++) {

            Thread thread = new Thread(() -> {
                try {
                    latchThatWaitsAllThreadStarted.await();
                    executeLimitFilter.invoke(invoker, invocation);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (RpcException expected) {
                    failed.incrementAndGet();
                } finally {
                    latchThatWaitsAllThreadFinished.countDown();
                }
            });

            thread.start();
        }

        latchThatWaitsAllThreadStarted.countDown();

        try {
            latchThatWaitsAllThreadFinished.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Assertions.assertEquals(totalExecute - maxExecute, failed.get());
    }
}
