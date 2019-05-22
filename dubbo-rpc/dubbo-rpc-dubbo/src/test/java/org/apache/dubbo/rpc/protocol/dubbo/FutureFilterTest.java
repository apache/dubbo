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
package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcResult;
import org.apache.dubbo.rpc.protocol.dubbo.filter.FutureFilter;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.config.Constants.ON_THROW_METHOD_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * EventFilterTest.java
 * TODO rely on callback integration test for now
 */
public class FutureFilterTest {
    private static RpcInvocation invocation;
    private Filter eventFilter = new FutureFilter();

    @BeforeAll
    public static void setUp() {
        invocation = new RpcInvocation();
        invocation.setMethodName("echo");
        invocation.setParameterTypes(new Class<?>[]{Enum.class});
        invocation.setArguments(new Object[]{"hello"});
    }

    @Test
    public void testSyncCallback() {
        @SuppressWarnings("unchecked")
        Invoker<DemoService> invoker = mock(Invoker.class);
        given(invoker.isAvailable()).willReturn(true);
        given(invoker.getInterface()).willReturn(DemoService.class);
        RpcResult result = new RpcResult();
        result.setValue("High");
        given(invoker.invoke(invocation)).willReturn(result);
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");
        given(invoker.getUrl()).willReturn(url);

        Result filterResult = eventFilter.invoke(invoker, invocation);
        assertEquals("High", filterResult.getValue());
    }

    @Test
    public void testSyncCallbackHasException() throws RpcException, Throwable {
        Assertions.assertThrows(RuntimeException.class, () -> {
            @SuppressWarnings("unchecked")
            Invoker<DemoService> invoker = mock(Invoker.class);
            given(invoker.isAvailable()).willReturn(true);
            given(invoker.getInterface()).willReturn(DemoService.class);
            RpcResult result = new RpcResult();
            result.setException(new RuntimeException());
            given(invoker.invoke(invocation)).willReturn(result);
            URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1&" + ON_THROW_METHOD_KEY + "=echo");
            given(invoker.getUrl()).willReturn(url);

            eventFilter.invoke(invoker, invocation).recreate();
        });
    }
}
