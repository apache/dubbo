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
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.support.BlockMyInvoker;
import org.apache.dubbo.rpc.support.MockInvocation;
import org.apache.dubbo.rpc.support.MyInvoker;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertNotSame;

/**
 * ActiveLimitFilterTest.java
 */
public class ActiveLimitFilterTest {

    private static volatile int count = 0;
    Filter activeLimitFilter = new ActiveLimitFilter();

    @Test
    public void testInvokeNoActives() {
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&actives=0");
        Invoker<ActiveLimitFilterTest> invoker = new MyInvoker<ActiveLimitFilterTest>(url);
        Invocation invocation = new MockInvocation();
        activeLimitFilter.invoke(invoker, invocation);
    }

    @Test
    public void testInvokeLessActives() {
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&actives=10");
        Invoker<ActiveLimitFilterTest> invoker = new MyInvoker<ActiveLimitFilterTest>(url);
        Invocation invocation = new MockInvocation();
        activeLimitFilter.invoke(invoker, invocation);
    }

    @Test
    public void testInvokeGreaterActives() {
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
                            count++;
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
        assertNotSame(0, count);
    }
}