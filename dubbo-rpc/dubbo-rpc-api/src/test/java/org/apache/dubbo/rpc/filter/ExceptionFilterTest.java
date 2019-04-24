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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.support.DemoService;
import org.apache.dubbo.rpc.support.LocalException;

import com.alibaba.com.caucho.hessian.HessianException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ExceptionFilterTest
 */
public class ExceptionFilterTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testRpcException() {
        Logger logger = mock(Logger.class);
        RpcContext.getContext().setRemoteAddress("127.0.0.1", 1234);
        RpcException exception = new RpcException("TestRpcException");

        ExceptionFilter exceptionFilter = new ExceptionFilter();
        RpcInvocation invocation = new RpcInvocation("sayHello", new Class<?>[]{String.class}, new Object[]{"world"});
        Invoker<DemoService> invoker = mock(Invoker.class);
        given(invoker.getInterface()).willReturn(DemoService.class);
        given(invoker.invoke(eq(invocation))).willThrow(exception);

        try {
            exceptionFilter.invoke(invoker, invocation);
        } catch (RpcException e) {
            assertEquals("TestRpcException", e.getMessage());
            ((ExceptionFilter.ExceptionListener) exceptionFilter.listener()).setLogger(logger);
            exceptionFilter.listener().onError(e, invoker, invocation);
        }

        Mockito.verify(logger).error(eq("Got unchecked and undeclared exception which called by 127.0.0.1. service: "
                + DemoService.class.getName() + ", method: sayHello, exception: "
                + RpcException.class.getName() + ": TestRpcException"), eq(exception));
        RpcContext.removeContext();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testJavaException() {

        ExceptionFilter exceptionFilter = new ExceptionFilter();
        RpcInvocation invocation = new RpcInvocation("sayHello", new Class<?>[]{String.class}, new Object[]{"world"});

        AppResponse appResponse = new AppResponse();
        appResponse.setException(new IllegalArgumentException("java"));

        Invoker<DemoService> invoker = mock(Invoker.class);
        when(invoker.invoke(invocation)).thenReturn(appResponse);
        when(invoker.getInterface()).thenReturn(DemoService.class);

        Result newResult = exceptionFilter.invoke(invoker, invocation);

        Assertions.assertEquals(appResponse.getException(), newResult.getException());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRuntimeException() {

        ExceptionFilter exceptionFilter = new ExceptionFilter();
        RpcInvocation invocation = new RpcInvocation("sayHello", new Class<?>[]{String.class}, new Object[]{"world"});

        AppResponse appResponse = new AppResponse();
        appResponse.setException(new LocalException("localException"));

        Invoker<DemoService> invoker = mock(Invoker.class);
        when(invoker.invoke(invocation)).thenReturn(appResponse);
        when(invoker.getInterface()).thenReturn(DemoService.class);

        Result newResult = exceptionFilter.invoke(invoker, invocation);

        Assertions.assertEquals(appResponse.getException(), newResult.getException());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConvertToRunTimeException() throws Exception {

        ExceptionFilter exceptionFilter = new ExceptionFilter();
        RpcInvocation invocation = new RpcInvocation("sayHello", new Class<?>[]{String.class}, new Object[]{"world"});

        AppResponse mockRpcResult = new AppResponse();
        mockRpcResult.setException(new HessianException("hessian"));
        Result mockAsyncResult = AsyncRpcResult.newDefaultAsyncResult(mockRpcResult, invocation);


        Invoker<DemoService> invoker = mock(Invoker.class);
        when(invoker.invoke(invocation)).thenReturn(mockAsyncResult);
        when(invoker.getInterface()).thenReturn(DemoService.class);

        Result asyncResult = exceptionFilter.invoke(invoker, invocation);

        AppResponse appResponse = (AppResponse) asyncResult.get();
        exceptionFilter.listener().onResponse(appResponse, invoker, invocation);

        Assertions.assertFalse(appResponse.getException() instanceof HessianException);

        Assertions.assertEquals(appResponse.getException().getClass(), RuntimeException.class);
    }

}