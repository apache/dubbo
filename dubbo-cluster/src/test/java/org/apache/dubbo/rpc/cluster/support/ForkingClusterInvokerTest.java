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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * ForkingClusterInvokerTest
 */
@SuppressWarnings("unchecked")
class ForkingClusterInvokerTest {

    private List<Invoker<ForkingClusterInvokerTest>> invokers = new ArrayList<>();
    private URL url = URL.valueOf("test://test:11/test?forks=2");
    private Invoker<ForkingClusterInvokerTest> invoker1 = mock(Invoker.class);
    private Invoker<ForkingClusterInvokerTest> invoker2 = mock(Invoker.class);
    private Invoker<ForkingClusterInvokerTest> invoker3 = mock(Invoker.class);
    private RpcInvocation invocation = new RpcInvocation();
    private Directory<ForkingClusterInvokerTest> dic;
    private Result result = new AppResponse();

    @BeforeEach
    public void setUp() throws Exception {

        dic = mock(Directory.class);

        given(dic.getUrl()).willReturn(url);
        given(dic.getConsumerUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(invokers);
        given(dic.getInterface()).willReturn(ForkingClusterInvokerTest.class);

        invocation.setMethodName("method1");

        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);

    }

    private void resetInvokerToException() {
        given(invoker1.invoke(invocation)).willThrow(new RuntimeException());
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.isAvailable()).willReturn(true);
        given(invoker1.getInterface()).willReturn(ForkingClusterInvokerTest.class);

        given(invoker2.invoke(invocation)).willThrow(new RuntimeException());
        given(invoker2.getUrl()).willReturn(url);
        given(invoker2.isAvailable()).willReturn(true);
        given(invoker2.getInterface()).willReturn(ForkingClusterInvokerTest.class);

        given(invoker3.invoke(invocation)).willThrow(new RuntimeException());
        given(invoker3.getUrl()).willReturn(url);
        given(invoker3.isAvailable()).willReturn(true);
        given(invoker3.getInterface()).willReturn(ForkingClusterInvokerTest.class);
    }

    private void resetInvokerToNoException() {
        given(invoker1.invoke(invocation)).willReturn(result);
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.isAvailable()).willReturn(true);
        given(invoker1.getInterface()).willReturn(ForkingClusterInvokerTest.class);

        given(invoker2.invoke(invocation)).willReturn(result);
        given(invoker2.getUrl()).willReturn(url);
        given(invoker2.isAvailable()).willReturn(true);
        given(invoker2.getInterface()).willReturn(ForkingClusterInvokerTest.class);

        given(invoker3.invoke(invocation)).willReturn(result);
        given(invoker3.getUrl()).willReturn(url);
        given(invoker3.isAvailable()).willReturn(true);
        given(invoker3.getInterface()).willReturn(ForkingClusterInvokerTest.class);
    }

    @Test
    void testInvokeException() {
        resetInvokerToException();
        ForkingClusterInvoker<ForkingClusterInvokerTest> invoker = new ForkingClusterInvoker<>(dic);

        try {
            invoker.invoke(invocation);
            Assertions.fail();
        } catch (RpcException expected) {
            Assertions.assertTrue(expected.getMessage().contains("Failed to forking invoke provider"));
            assertFalse(expected.getCause() instanceof RpcException);
        }
    }

    @Test
    void testClearRpcContext() {
        resetInvokerToException();
        ForkingClusterInvoker<ForkingClusterInvokerTest> invoker = new ForkingClusterInvoker<>(dic);

        String attachKey = "attach";
        String attachValue = "value";

        RpcContext.getClientAttachment().setAttachment(attachKey, attachValue);

        Map<String, Object> attachments = RpcContext.getClientAttachment().getObjectAttachments();
        Assertions.assertTrue(attachments != null && attachments.size() == 1, "set attachment failed!");
        try {
            invoker.invoke(invocation);
            Assertions.fail();
        } catch (RpcException expected) {
            Assertions.assertTrue(expected.getMessage().contains("Failed to forking invoke provider"), "Succeeded to forking invoke provider !");
            assertFalse(expected.getCause() instanceof RpcException);
        }
        Map<String, Object> afterInvoke = RpcContext.getClientAttachment().getObjectAttachments();
        Assertions.assertTrue(afterInvoke != null && afterInvoke.size() == 0, "clear attachment failed!");
    }

    @Test
    void testInvokeNoException() {

        resetInvokerToNoException();

        ForkingClusterInvoker<ForkingClusterInvokerTest> invoker = new ForkingClusterInvoker<>(dic);
        Result ret = invoker.invoke(invocation);
        Assertions.assertSame(result, ret);
    }

    @Test
    void testInvokeWithIllegalForksParam() {
        URL url = URL.valueOf("test://test:11/test?forks=-1");
        given(dic.getUrl()).willReturn(url);
        given(dic.getConsumerUrl()).willReturn(url);
        given(invoker1.invoke(invocation)).willReturn(result);
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.isAvailable()).willReturn(true);
        given(invoker1.getInterface()).willReturn(ForkingClusterInvokerTest.class);

        ForkingClusterInvoker<ForkingClusterInvokerTest> invoker = new ForkingClusterInvoker<>(dic);
        Result ret = invoker.invoke(invocation);
        Assertions.assertSame(result, ret);
    }

}
