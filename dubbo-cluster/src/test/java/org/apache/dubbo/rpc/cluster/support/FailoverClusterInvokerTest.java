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
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * FailoverClusterInvokerTest
 */
@SuppressWarnings("unchecked")
public class FailoverClusterInvokerTest {
    private final int retries = 5;
    private final URL url = URL.valueOf("test://test:11/test?retries=" + retries);
    private final Invoker<FailoverClusterInvokerTest> invoker1 = mock(Invoker.class);
    private final Invoker<FailoverClusterInvokerTest> invoker2 = mock(Invoker.class);
    private final RpcInvocation invocation = new RpcInvocation();
    private final Result expectedResult = new AppResponse();
    private final List<Invoker<FailoverClusterInvokerTest>> invokers = new ArrayList<>();
    private Directory<FailoverClusterInvokerTest> dic;

    /**
     * @throws java.lang.Exception
     */

    @BeforeEach
    public void setUp() throws Exception {

        dic = mock(Directory.class);

        given(dic.getUrl()).willReturn(url);
        given(dic.getConsumerUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(invokers);
        given(dic.getInterface()).willReturn(FailoverClusterInvokerTest.class);
        invocation.setMethodName("method1");

        invokers.add(invoker1);
        invokers.add(invoker2);
    }


    @Test
    public void testInvokeWithRuntimeException() {
        given(invoker1.invoke(invocation)).willThrow(new RuntimeException());
        given(invoker1.isAvailable()).willReturn(true);
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.getInterface()).willReturn(FailoverClusterInvokerTest.class);

        given(invoker2.invoke(invocation)).willThrow(new RuntimeException());
        given(invoker2.isAvailable()).willReturn(true);
        given(invoker2.getUrl()).willReturn(url);
        given(invoker2.getInterface()).willReturn(FailoverClusterInvokerTest.class);

        FailoverClusterInvoker<FailoverClusterInvokerTest> invoker = new FailoverClusterInvoker<>(dic);
        try {
            invoker.invoke(invocation);
            fail();
        } catch (RpcException actualException) {
            assertEquals(0, actualException.getCode());
            assertFalse(actualException.getCause() instanceof RpcException);
        }
    }

    @Test()
    public void testInvokeWithRPCException() {
        given(invoker1.invoke(invocation)).willThrow(new RpcException());
        given(invoker1.isAvailable()).willReturn(true);
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.getInterface()).willReturn(FailoverClusterInvokerTest.class);

        given(invoker2.invoke(invocation)).willReturn(expectedResult);
        given(invoker2.isAvailable()).willReturn(true);
        given(invoker2.getUrl()).willReturn(url);
        given(invoker2.getInterface()).willReturn(FailoverClusterInvokerTest.class);

        FailoverClusterInvoker<FailoverClusterInvokerTest> invoker = new FailoverClusterInvoker<>(dic);
        for (int i = 0; i < 100; i++) {
            Result actualResult = invoker.invoke(invocation);
            assertSame(expectedResult, actualResult);
        }
    }

    @Test()
    public void testInvoke_retryTimes() {
        given(invoker1.invoke(invocation)).willThrow(new RpcException(RpcException.TIMEOUT_EXCEPTION));
        given(invoker1.isAvailable()).willReturn(false);
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.getInterface()).willReturn(FailoverClusterInvokerTest.class);

        given(invoker2.invoke(invocation)).willThrow(new RpcException());
        given(invoker2.isAvailable()).willReturn(false);
        given(invoker2.getUrl()).willReturn(url);
        given(invoker2.getInterface()).willReturn(FailoverClusterInvokerTest.class);

        FailoverClusterInvoker<FailoverClusterInvokerTest> invoker = new FailoverClusterInvoker<>(dic);
        try {
            Result actualResult = invoker.invoke(invocation);
            assertSame(expectedResult, actualResult);
            fail();
        } catch (RpcException actualException) {
            assertTrue((actualException.isTimeout() || actualException.getCode() == 0));
            assertTrue(actualException.getMessage().indexOf((retries + 1) + " times") > 0);
        }
    }

    @Test()
    public void testInvoke_retryTimes2() {
        int finalRetries = 1;
        given(invoker1.invoke(invocation)).willThrow(new RpcException(RpcException.TIMEOUT_EXCEPTION));
        given(invoker1.isAvailable()).willReturn(false);
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.getInterface()).willReturn(FailoverClusterInvokerTest.class);

        given(invoker2.invoke(invocation)).willThrow(new RpcException());
        given(invoker2.isAvailable()).willReturn(false);
        given(invoker2.getUrl()).willReturn(url);
        given(invoker2.getInterface()).willReturn(FailoverClusterInvokerTest.class);

        RpcContext rpcContext = RpcContext.getContext();
        rpcContext.setAttachment("retries", finalRetries);

        FailoverClusterInvoker<FailoverClusterInvokerTest> invoker = new FailoverClusterInvoker<>(dic);
        try {
            Result actualResult = invoker.invoke(invocation);
            assertSame(expectedResult, actualResult);
            fail();
        } catch (RpcException actualException) {
            assertTrue((actualException.isTimeout() || actualException.getCode() == 0));
            assertTrue(actualException.getMessage().indexOf((finalRetries + 1) + " times") > 0);
        }
    }

    @Test()
    public void testInvoke_retryTimes_withBizException() {
        given(invoker1.invoke(invocation)).willThrow(new RpcException(RpcException.BIZ_EXCEPTION));
        given(invoker1.isAvailable()).willReturn(false);
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.getInterface()).willReturn(FailoverClusterInvokerTest.class);

        given(invoker2.invoke(invocation)).willThrow(new RpcException(RpcException.BIZ_EXCEPTION));
        given(invoker2.isAvailable()).willReturn(false);
        given(invoker2.getUrl()).willReturn(url);
        given(invoker2.getInterface()).willReturn(FailoverClusterInvokerTest.class);

        FailoverClusterInvoker<FailoverClusterInvokerTest> invoker = new FailoverClusterInvoker<>(dic);
        try {
            Result actualResult = invoker.invoke(invocation);
            assertSame(expectedResult, actualResult);
            fail();
        } catch (RpcException actualException) {
            assertEquals(RpcException.BIZ_EXCEPTION, actualException.getCode());
        }
    }

    @Test()
    public void testInvoke_without_retry() {
        int withoutRetry = 0;
        final URL url = URL.valueOf("test://localhost/" + Demo.class.getName() + "?loadbalance=roundrobin&retries=" + withoutRetry);
        RpcException exception = new RpcException(RpcException.TIMEOUT_EXCEPTION);
        MockInvoker<Demo> invoker1 = new MockInvoker<>(Demo.class, url);
        invoker1.setException(exception);

        MockInvoker<Demo> invoker2 = new MockInvoker<>(Demo.class, url);
        invoker2.setException(exception);

        final List<Invoker<Demo>> invokers = new ArrayList<>();
        invokers.add(invoker1);
        invokers.add(invoker2);

        try {
            Directory<Demo> dic = new MockDirectory<>(url, invokers);
            FailoverClusterInvoker<Demo> clusterInvoker = new FailoverClusterInvoker<>(dic);
            RpcInvocation inv = new RpcInvocation();
            inv.setMethodName("test");
            clusterInvoker.invoke(inv);
        } catch (RpcException actualException) {
            assertTrue(actualException.getCause() instanceof RpcException);
            assertEquals(RpcException.TIMEOUT_EXCEPTION, actualException.getCode());
        }
    }

    @Test()
    public void testInvoke_when_retry_illegal() {
        int illegalRetry = -1;
        final URL url = URL.valueOf("test://localhost/" + Demo.class.getName() + "?loadbalance=roundrobin&retries=" + illegalRetry);
        RpcException exception = new RpcException(RpcException.TIMEOUT_EXCEPTION);
        MockInvoker<Demo> invoker1 = new MockInvoker<>(Demo.class, url);
        invoker1.setException(exception);

        MockInvoker<Demo> invoker2 = new MockInvoker<>(Demo.class, url);
        invoker2.setException(exception);

        final List<Invoker<Demo>> invokers = new ArrayList<>();
        invokers.add(invoker1);
        invokers.add(invoker2);

        try {
            Directory<Demo> dic = new MockDirectory<>(url, invokers);
            FailoverClusterInvoker<Demo> clusterInvoker = new FailoverClusterInvoker<>(dic);
            RpcInvocation inv = new RpcInvocation();
            inv.setMethodName("test");
            clusterInvoker.invoke(inv);
        } catch (RpcException actualException) {
            assertTrue(actualException.getCause() instanceof RpcException);
            assertEquals(RpcException.TIMEOUT_EXCEPTION, actualException.getCode());
        }
    }

    @Test()
    public void testNoInvoke() {
        dic = mock(Directory.class);

        given(dic.getUrl()).willReturn(url);
        given(dic.getConsumerUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(null);
        given(dic.getInterface()).willReturn(FailoverClusterInvokerTest.class);
        invocation.setMethodName("method1");

        invokers.add(invoker1);


        FailoverClusterInvoker<FailoverClusterInvokerTest> invoker = new FailoverClusterInvoker<>(dic);
        try {
            invoker.invoke(invocation);
            fail();
        } catch (RpcException actualException) {
            assertFalse(actualException.getCause() instanceof RpcException);
        }
    }

    /**
     * When invokers in directory changes after a failed request but just before a retry effort,
     * then we should reselect from the latest invokers before retry.
     */
    @Test
    public void testInvokerDestroyAndReList() {
        final URL url = URL.valueOf("test://localhost/" + Demo.class.getName() + "?loadbalance=roundrobin&retries=" + retries);
        RpcException exception = new RpcException(RpcException.TIMEOUT_EXCEPTION);
        MockInvoker<Demo> invoker1 = new MockInvoker<>(Demo.class, url);
        invoker1.setException(exception);

        MockInvoker<Demo> invoker2 = new MockInvoker<>(Demo.class, url);
        invoker2.setException(exception);

        final List<Invoker<Demo>> invokers = new ArrayList<>();
        invokers.add(invoker1);
        invokers.add(invoker2);

        MockDirectory<Demo> dic = new MockDirectory<>(url, invokers);

        Callable<Object> callable = () -> {
            //Simulation: all invokers are destroyed
            for (Invoker<Demo> invoker : invokers) {
                invoker.destroy();
            }
            invokers.clear();
            MockInvoker<Demo> invoker3 = new MockInvoker<>(Demo.class, url);
            invoker3.setResult(AsyncRpcResult.newDefaultAsyncResult(mock(RpcInvocation.class)));
            invokers.add(invoker3);
            dic.notify(invokers);
            return null;
        };
        invoker1.setCallable(callable);
        invoker2.setCallable(callable);

        RpcInvocation inv = new RpcInvocation();
        inv.setMethodName("test");

        FailoverClusterInvoker<Demo> clusterInvoker = new FailoverClusterInvoker<>(dic);
        clusterInvoker.invoke(inv);
    }

    public interface Demo {
    }

    public static class MockInvoker<T> extends AbstractInvoker<T> {
        URL url;
        boolean available = true;
        boolean destoryed = false;
        Result result;
        RpcException exception;
        Callable<?> callable;

        public MockInvoker(Class<T> type, URL url) {
            super(type, url);
        }

        public void setResult(Result result) {
            this.result = result;
        }

        public void setException(RpcException exception) {
            this.exception = exception;
        }

        public void setCallable(Callable<?> callable) {
            this.callable = callable;
        }

        @Override
        protected Result doInvoke(Invocation invocation) throws Throwable {
            if (callable != null) {
                try {
                    callable.call();
                } catch (Exception e) {
                    throw new RpcException(e);
                }
            }
            if (exception != null) {
                throw exception;
            } else {
                return result;
            }
        }
    }

    public static class MockDirectory<T> extends StaticDirectory<T> {
        public MockDirectory(URL url, List<Invoker<T>> invokers) {
            super(url, invokers);
        }

        @Override
        protected List<Invoker<T>> doList(BitList<Invoker<T>> invokers, Invocation invocation) throws RpcException {
            return super.doList(invokers, invocation);
        }
    }
}
