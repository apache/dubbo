/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.filter;

import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.support.MockInvocation;
import com.alibaba.dubbo.rpc.support.MyInvoker;

/**
 * ActiveLimitFilterTest.java
 * 
 * @author tony.chenl
 */
public class ActiveLimitFilterTest {

    Filter                      activeLimitFilter = new ActiveLimitFilter();
    private static volatile int count             = 0;

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
        final Invoker<ActiveLimitFilterTest> invoker = new MyInvoker<ActiveLimitFilterTest>(url);
        final Invocation invocation = new MockInvocation();
        for (int i = 0; i < 100; i++) {
            Thread thread = new Thread(new Runnable() {

                public void run() {
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
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertNotSame(0, count);
    }
}