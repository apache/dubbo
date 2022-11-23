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
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Directory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * FailfastClusterInvokerTest
 */
@SuppressWarnings("unchecked")
class FailfastClusterInvokerTest {
    List<Invoker<FailfastClusterInvokerTest>> invokers = new ArrayList<>();
    URL url = URL.valueOf("test://test:11/test");
    Invoker<FailfastClusterInvokerTest> invoker1 = mock(Invoker.class);
    RpcInvocation invocation = new RpcInvocation();
    Directory<FailfastClusterInvokerTest> dic;
    Result result = new AppResponse();

    /**
     * @throws java.lang.Exception
     */

    @BeforeEach
    public void setUp() throws Exception {

        dic = mock(Directory.class);

        given(dic.getUrl()).willReturn(url);
        given(dic.getConsumerUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(invokers);
        given(dic.getInterface()).willReturn(FailfastClusterInvokerTest.class);

        invocation.setMethodName("method1");

        invokers.add(invoker1);
    }

    private void resetInvoker1ToException() {
        given(invoker1.invoke(invocation)).willThrow(new RuntimeException());
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.getInterface()).willReturn(FailfastClusterInvokerTest.class);
    }

    private void resetInvoker1ToNoException() {
        given(invoker1.invoke(invocation)).willReturn(result);
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.getInterface()).willReturn(FailfastClusterInvokerTest.class);
    }

    @Test
    void testInvokeException() {
        Assertions.assertThrows(RpcException.class, () -> {
            resetInvoker1ToException();
            FailfastClusterInvoker<FailfastClusterInvokerTest> invoker = new FailfastClusterInvoker<>(dic);
            invoker.invoke(invocation);
            assertSame(invoker1, RpcContext.getServiceContext().getInvoker());
        });
    }

    @Test
    void testInvokeBizException() {
        given(invoker1.invoke(invocation)).willThrow(new RpcException(RpcException.BIZ_EXCEPTION));
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.getInterface()).willReturn(FailfastClusterInvokerTest.class);
        FailfastClusterInvoker<FailfastClusterInvokerTest> invoker = new FailfastClusterInvoker<>(dic);

        try {
            Result ret = invoker.invoke(invocation);
            assertSame(result, ret);
            fail();
        } catch (RpcException expected) {
            assertEquals(expected.getCode(), RpcException.BIZ_EXCEPTION);
        }
    }

    @Test
    void testInvokeNoException() {

        resetInvoker1ToNoException();

        FailfastClusterInvoker<FailfastClusterInvokerTest> invoker = new FailfastClusterInvoker<>(dic);
        Result ret = invoker.invoke(invocation);
        assertSame(result, ret);
    }

    @Test
    void testNoInvoke() {
        dic = mock(Directory.class);

        given(dic.getUrl()).willReturn(url);
        given(dic.getConsumerUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(null);
        given(dic.getInterface()).willReturn(FailfastClusterInvokerTest.class);

        invocation.setMethodName("method1");

        invokers.add(invoker1);

        resetInvoker1ToNoException();

        FailfastClusterInvoker<FailfastClusterInvokerTest> invoker = new FailfastClusterInvoker<>(dic);
        try {
            invoker.invoke(invocation);
            fail();
        } catch (RpcException expected) {
            assertFalse(expected.getCause() instanceof RpcException);
        }
    }

}
