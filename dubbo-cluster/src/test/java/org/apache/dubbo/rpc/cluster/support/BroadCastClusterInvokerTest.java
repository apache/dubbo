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
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.filter.DemoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @see BroadcastClusterInvoker
 */
public class BroadCastClusterInvokerTest {
    private URL url;
    private Directory<DemoService> dic;
    private RpcInvocation invocation;
    private BroadcastClusterInvoker clusterInvoker;

    private MockInvoker invoker1;
    private MockInvoker invoker2;
    private MockInvoker invoker3;
    private MockInvoker invoker4;


    @BeforeEach
    public void setUp() throws Exception {

        dic = mock(Directory.class);

        invoker1 = new MockInvoker();
        invoker2 = new MockInvoker();
        invoker3 = new MockInvoker();
        invoker4 = new MockInvoker();

        url = URL.valueOf("test://127.0.0.1:8080/test");
        given(dic.getUrl()).willReturn(url);
        given(dic.getConsumerUrl()).willReturn(url);
        given(dic.getInterface()).willReturn(DemoService.class);

        invocation = new RpcInvocation();
        invocation.setMethodName("test");

        clusterInvoker = new BroadcastClusterInvoker(dic);
    }


    @Test
    public void testNormal() {
        given(dic.list(invocation)).willReturn(Arrays.asList(invoker1, invoker2, invoker3, invoker4));
        // Every invoker will be called
        clusterInvoker.invoke(invocation);
        assertTrue(invoker1.isInvoked());
        assertTrue(invoker2.isInvoked());
        assertTrue(invoker3.isInvoked());
        assertTrue(invoker4.isInvoked());
    }

    @Test
    public void testEx() {
        given(dic.list(invocation)).willReturn(Arrays.asList(invoker1, invoker2, invoker3, invoker4));
        invoker1.invokeThrowEx();
        assertThrows(RpcException.class, () -> {
            clusterInvoker.invoke(invocation);
        });
        // The default failure percentage is 100, even if a certain invoker#invoke throws an exception, other invokers will still be called
        assertTrue(invoker1.isInvoked());
        assertTrue(invoker2.isInvoked());
        assertTrue(invoker3.isInvoked());
        assertTrue(invoker4.isInvoked());
    }

    @Test
    public void testFailPercent() {
        given(dic.list(invocation)).willReturn(Arrays.asList(invoker1, invoker2, invoker3, invoker4));
        // We set the failure percentage to 75, which means that when the number of call failures is 4*(75/100) = 3,
        // an exception will be thrown directly and subsequent invokers will not be called.
        url = url.addParameter("broadcast.fail.percent", 75);
        given(dic.getConsumerUrl()).willReturn(url);
        invoker1.invokeThrowEx();
        invoker2.invokeThrowEx();
        invoker3.invokeThrowEx();
        invoker4.invokeThrowEx();
        assertThrows(RpcException.class, () -> {
            clusterInvoker.invoke(invocation);
        });
        assertTrue(invoker1.isInvoked());
        assertTrue(invoker2.isInvoked());
        assertTrue(invoker3.isInvoked());
        assertFalse(invoker4.isInvoked());
    }
}

class MockInvoker implements Invoker<DemoService> {
    private static int count = 0;
    private URL url = URL.valueOf("test://127.0.0.1:8080/test");
    private boolean throwEx = false;
    private boolean invoked = false;

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void destroy() {

    }

    @Override
    public Class<DemoService> getInterface() {
        return DemoService.class;
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        invoked = true;
        if (throwEx) {
            throwEx = false;
            throw new RpcException();
        }
        return null;
    }

    public void invokeThrowEx() {
        throwEx = true;
    }

    public boolean isInvoked() {
        return invoked;
    }
}
