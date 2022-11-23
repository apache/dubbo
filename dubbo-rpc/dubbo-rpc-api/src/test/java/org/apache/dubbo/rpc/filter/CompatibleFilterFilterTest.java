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
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.support.DemoService;
import org.apache.dubbo.rpc.support.Type;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * CompatibleFilterTest.java
 */
class CompatibleFilterFilterTest {
    private CompatibleFilter compatibleFilter = new CompatibleFilter();
    private Invocation invocation;
    private Invoker invoker;

    @AfterEach
    public void tearDown() {
        Mockito.reset(invocation, invoker);
    }

    @Test
    void testInvokerGeneric() {
        invocation = mock(RpcInvocation.class);
        given(invocation.getMethodName()).willReturn("$enumlength");
        given(invocation.getParameterTypes()).willReturn(new Class<?>[]{Enum.class});
        given(invocation.getArguments()).willReturn(new Object[]{"hello"});

        invoker = mock(Invoker.class);
        given(invoker.isAvailable()).willReturn(true);
        given(invoker.getInterface()).willReturn(DemoService.class);
        AppResponse result = new AppResponse();
        result.setValue("High");
        given(invoker.invoke(invocation)).willReturn(result);
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");
        given(invoker.getUrl()).willReturn(url);

        Result filterResult = compatibleFilter.invoke(invoker, invocation);
        assertEquals(filterResult, result);
    }

    @Test
    void testResultHasException() {
        invocation = mock(RpcInvocation.class);
        given(invocation.getMethodName()).willReturn("enumlength");
        given(invocation.getParameterTypes()).willReturn(new Class<?>[]{Enum.class});
        given(invocation.getArguments()).willReturn(new Object[]{"hello"});

        invoker = mock(Invoker.class);
        given(invoker.isAvailable()).willReturn(true);
        given(invoker.getInterface()).willReturn(DemoService.class);
        AppResponse result = new AppResponse();
        result.setException(new RuntimeException());
        result.setValue("High");
        given(invoker.invoke(invocation)).willReturn(result);
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");
        given(invoker.getUrl()).willReturn(url);

        Result filterResult = compatibleFilter.invoke(invoker, invocation);
        assertEquals(filterResult, result);
    }

    @Test
    void testInvokerJsonPojoSerialization() throws Exception {
        invocation = mock(RpcInvocation.class);
        given(invocation.getMethodName()).willReturn("enumlength");
        given(invocation.getParameterTypes()).willReturn(new Class<?>[]{Type[].class});
        given(invocation.getArguments()).willReturn(new Object[]{"hello"});

        invoker = mock(Invoker.class);
        given(invoker.isAvailable()).willReturn(true);
        given(invoker.getInterface()).willReturn(DemoService.class);
        AppResponse result = new AppResponse();
        result.setValue("High");
        AsyncRpcResult defaultAsyncResult = AsyncRpcResult.newDefaultAsyncResult(result, invocation);
        given(invoker.invoke(invocation)).willReturn(defaultAsyncResult);
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1&serialization=json");
        given(invoker.getUrl()).willReturn(url);

        Result asyncResult = compatibleFilter.invoke(invoker, invocation);
        AppResponse appResponse = (AppResponse) asyncResult.get();
        compatibleFilter.onResponse(appResponse, invoker, invocation);
        assertEquals(Type.High, appResponse.getValue());
    }

    @Test
    void testInvokerNonJsonEnumSerialization() throws Exception {
        invocation = mock(RpcInvocation.class);
        given(invocation.getMethodName()).willReturn("enumlength");
        given(invocation.getParameterTypes()).willReturn(new Class<?>[]{Type[].class});
        given(invocation.getArguments()).willReturn(new Object[]{"hello"});

        invoker = mock(Invoker.class);
        given(invoker.isAvailable()).willReturn(true);
        given(invoker.getInterface()).willReturn(DemoService.class);
        AppResponse result = new AppResponse();
        result.setValue("High");
        AsyncRpcResult defaultAsyncResult = AsyncRpcResult.newDefaultAsyncResult(result, invocation);
        given(invoker.invoke(invocation)).willReturn(defaultAsyncResult);
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");
        given(invoker.getUrl()).willReturn(url);

        Result asyncResult = compatibleFilter.invoke(invoker, invocation);
        AppResponse appResponse = (AppResponse) asyncResult.get();
        compatibleFilter.onResponse(appResponse, invoker, invocation);
        assertEquals(Type.High, appResponse.getValue());
    }

    @Test
    void testInvokerNonJsonNonPojoSerialization() {
        invocation = mock(RpcInvocation.class);
        given(invocation.getMethodName()).willReturn("echo");
        given(invocation.getParameterTypes()).willReturn(new Class<?>[]{String.class});
        given(invocation.getArguments()).willReturn(new Object[]{"hello"});

        invoker = mock(Invoker.class);
        given(invoker.isAvailable()).willReturn(true);
        given(invoker.getInterface()).willReturn(DemoService.class);
        AppResponse result = new AppResponse();
        result.setValue(new String[]{"High"});
        given(invoker.invoke(invocation)).willReturn(result);
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");
        given(invoker.getUrl()).willReturn(url);

        Result filterResult = compatibleFilter.invoke(invoker, invocation);
        assertArrayEquals(new String[]{"High"}, (String[]) filterResult.getValue());
    }

    @Test
    void testInvokerNonJsonPojoSerialization() {
        invocation = mock(RpcInvocation.class);
        given(invocation.getMethodName()).willReturn("echo");
        given(invocation.getParameterTypes()).willReturn(new Class<?>[]{String.class});
        given(invocation.getArguments()).willReturn(new Object[]{"hello"});

        invoker = mock(Invoker.class);
        given(invoker.isAvailable()).willReturn(true);
        given(invoker.getInterface()).willReturn(DemoService.class);
        AppResponse result = new AppResponse();
        result.setValue("hello");
        given(invoker.invoke(invocation)).willReturn(result);
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");
        given(invoker.getUrl()).willReturn(url);

        Result filterResult = compatibleFilter.invoke(invoker, invocation);
        assertEquals("hello", filterResult.getValue());
    }
}
