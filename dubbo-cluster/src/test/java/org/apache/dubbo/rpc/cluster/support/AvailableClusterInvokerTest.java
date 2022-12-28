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
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Directory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for AvailableClusterInvoker
 */
class AvailableClusterInvokerTest {

    private final URL url = URL.valueOf("test://test:80/test");
    private final Invoker<AvailableClusterInvokerTest> invoker1 = mock(Invoker.class);
    private final Invoker<AvailableClusterInvokerTest> invoker2 = mock(Invoker.class);
    private final Invoker<AvailableClusterInvokerTest> invoker3 = mock(Invoker.class);
    private final RpcInvocation invocation = new RpcInvocation();
    private final Result result = new AppResponse();
    private final List<Invoker<AvailableClusterInvokerTest>> invokers = new ArrayList<>();
    private Directory<AvailableClusterInvokerTest> dic;

    @BeforeEach
    public void setUp() throws Exception {

        dic = mock(Directory.class);

        given(dic.getUrl()).willReturn(url);
        given(dic.getConsumerUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(invokers);
        given(dic.getInterface()).willReturn(AvailableClusterInvokerTest.class);

        invocation.setMethodName("method1");

        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
    }

    private void resetInvokerToNoException() {

        given(invoker1.invoke(invocation)).willReturn(result);
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.isAvailable()).willReturn(true);
        given(invoker1.getInterface()).willReturn(AvailableClusterInvokerTest.class);

        given(invoker2.invoke(invocation)).willReturn(result);
        given(invoker2.getUrl()).willReturn(url);
        given(invoker2.isAvailable()).willReturn(true);
        given(invoker2.getInterface()).willReturn(AvailableClusterInvokerTest.class);

        given(invoker3.invoke(invocation)).willReturn(result);
        given(invoker3.getUrl()).willReturn(url);
        given(invoker3.isAvailable()).willReturn(true);
        given(invoker3.getInterface()).willReturn(AvailableClusterInvokerTest.class);
    }

    @Test
    void testInvokeNoException() {

        resetInvokerToNoException();

        AvailableClusterInvoker<AvailableClusterInvokerTest> invoker = new AvailableClusterInvoker<>(dic);
        Result ret = invoker.invoke(invocation);
        Assertions.assertSame(result, ret);
    }

    @Test
    void testInvokeWithException() {

        // remove invokers for test exception
        dic.list(invocation).removeAll(invokers);

        AvailableClusterInvoker<AvailableClusterInvokerTest> invoker = new AvailableClusterInvoker<>(dic);
        try {
            invoker.invoke(invocation);
            fail();
        } catch (RpcException e) {
            Assertions.assertTrue(e.getMessage().contains("No provider available in"));
            assertFalse(e.getCause() instanceof RpcException);
        }
    }
}
