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
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.Directory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * AvailableClusterInvokerTest
 */
public class AvailableClusterInvokerTest {

    private List<Invoker<AvailableClusterInvokerTest>> invokers = new ArrayList<>();
    private URL url = URL.valueOf("test://test:11/test");
    private Invoker<AvailableClusterInvokerTest> invoker1 = mock(Invoker.class);
    private Invoker<AvailableClusterInvokerTest> invoker2 = mock(Invoker.class);
    private Invoker<AvailableClusterInvokerTest> invoker3 = mock(Invoker.class);
    private RpcInvocation invocation = new RpcInvocation();
    private Directory<AvailableClusterInvokerTest> dic;
    private Result result = new AppResponse();

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

    private void resetInvokerToException() {
        given(invoker1.invoke(invocation)).willThrow(new RuntimeException());
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.isAvailable()).willReturn(true);
        given(invoker1.getInterface()).willReturn(AvailableClusterInvokerTest.class);

        given(invoker2.invoke(invocation)).willThrow(new RuntimeException());
        given(invoker2.getUrl()).willReturn(url);
        given(invoker2.isAvailable()).willReturn(true);
        given(invoker2.getInterface()).willReturn(AvailableClusterInvokerTest.class);

        given(invoker3.invoke(invocation)).willThrow(new RuntimeException());
        given(invoker3.getUrl()).willReturn(url);
        given(invoker3.isAvailable()).willReturn(true);
        given(invoker3.getInterface()).willReturn(AvailableClusterInvokerTest.class);
    }

    private void resetInvokerToNoException(){
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

    @Test()
    public void testInvokeNoException() {

        resetInvokerToNoException();

        AvailableClusterInvoker<AvailableClusterInvokerTest> invoker = new AvailableClusterInvoker<>(
                dic);
        Result ret = invoker.invoke(invocation);
        Assertions.assertSame(result, ret);
    }

}
