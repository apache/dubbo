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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @description:
 * @author: chengang6
 * @create: 2019/7/22 19:40
 **/
public class FailoverRpcExceptionTest {
    private List<Invoker<FailoverClusterInvokerTest>> invokers = new ArrayList<Invoker<FailoverClusterInvokerTest>>();
    private int retries = 3;
    private URL url = URL.valueOf("test://test:11/test?retries=" + 3);
    private Invoker<FailoverClusterInvokerTest> invoker1 = mock(Invoker.class);
    private RpcInvocation invocation = new RpcInvocation();
    private Directory<FailoverClusterInvokerTest> dic;
    private Result result = new AppResponse();

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {

        dic = mock(Directory.class);

        given(dic.getUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(invokers);
        given(dic.getInterface()).willReturn(FailoverClusterInvokerTest.class);
        invocation.setMethodName("method1");

        invokers.add(invoker1);

        result.setException(new RpcException("FailoverRpcExceptionTest|RpcException"));
    }

    @Test()
    public void testInvokeWithRPCException() {
        given(invoker1.invoke(invocation)).willReturn(result);
        given(invoker1.isAvailable()).willReturn(true);
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.getInterface()).willReturn(FailoverClusterInvokerTest.class);
        FailoverClusterInvoker<FailoverClusterInvokerTest> invoker = new FailoverClusterInvoker<FailoverClusterInvokerTest>(dic);

        try {
            Result ret = invoker.invoke(invocation);
        } catch (RpcException expected) {
            assertTrue((expected.isTimeout() || expected.getCode() == 0));
            assertTrue(expected.getMessage().indexOf("Tried "+(retries + 1) + " times") > 0);
        }
    }

}
