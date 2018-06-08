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
package com.alibaba.dubbo.rpc.filter;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.support.DemoService;
import com.alibaba.dubbo.rpc.support.MockInvocation;
import com.alibaba.dubbo.rpc.support.MyInvoker;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * ContextFilterTest.java
 * TODO need to enhance assertion
 */
public class ContextFilterTest {

    Filter contextFilter = new ContextFilter();
    Invoker<DemoService> invoker;
    Invocation invocation;

    @SuppressWarnings("unchecked")
    @Test
    public void testSetContext() {
        invocation = mock(Invocation.class);
        given(invocation.getMethodName()).willReturn("$enumlength");
        given(invocation.getParameterTypes()).willReturn(new Class<?>[]{Enum.class});
        given(invocation.getArguments()).willReturn(new Object[]{"hello"});
        given(invocation.getAttachments()).willReturn(null);

        invoker = mock(Invoker.class);
        given(invoker.isAvailable()).willReturn(true);
        given(invoker.getInterface()).willReturn(DemoService.class);
        RpcResult result = new RpcResult();
        result.setValue("High");
        given(invoker.invoke(invocation)).willReturn(result);
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");
        given(invoker.getUrl()).willReturn(url);

        contextFilter.invoke(invoker, invocation);
        assertNull(RpcContext.getContext().getInvoker());
    }

    @Test
    public void testWithAttachments() {
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");
        Invoker<DemoService> invoker = new MyInvoker<DemoService>(url);
        Invocation invocation = new MockInvocation();
        Result result = contextFilter.invoke(invoker, invocation);
        assertNull(RpcContext.getContext().getInvoker());
    }
}