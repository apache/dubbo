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
import org.apache.dubbo.common.compact.Dubbo2GenericExceptionUtils;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.support.DemoService;
import org.apache.dubbo.rpc.support.Person;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

class GenericImplFilterTest {

    private GenericImplFilter genericImplFilter =
            new GenericImplFilter(ApplicationModel.defaultModel().getDefaultModule());

    @Test
    void testInvokeWithException() throws Exception {

        RpcInvocation invocation = new RpcInvocation(
                "getPerson",
                "org.apache.dubbo.rpc.support.DemoService",
                "org.apache.dubbo.rpc.support.DemoService:dubbo",
                new Class[] {Person.class},
                new Object[] {new Person("dubbo", 10)});

        URL url = URL.valueOf("test://test:11/org.apache.dubbo.rpc.support.DemoService?"
                + "accesslog=true&group=dubbo&version=1.1&generic=true");
        Invoker invoker = Mockito.mock(Invoker.class);

        AppResponse mockRpcResult =
                new AppResponse(Dubbo2GenericExceptionUtils.newGenericException(new RuntimeException("failed")));
        when(invoker.invoke(any(Invocation.class)))
                .thenReturn(AsyncRpcResult.newDefaultAsyncResult(mockRpcResult, invocation));
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.getInterface()).thenReturn(DemoService.class);

        Result asyncResult = genericImplFilter.invoke(invoker, invocation);
        Result result = asyncResult.get();
        genericImplFilter.onResponse(result, invoker, invocation);
        Assertions.assertEquals(RuntimeException.class, result.getException().getClass());
    }
}
