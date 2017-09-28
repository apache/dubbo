/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.directory.StaticDirectory;
import com.alibaba.dubbo.rpc.protocol.AbstractInvoker;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * FailoverClusterInvokerTest
 *
 * @author liuchao
 */
@SuppressWarnings("unchecked")
public class FailoverClusterInvokerTest {
    List<Invoker<FailoverClusterInvokerTest>> invokers = new ArrayList<Invoker<FailoverClusterInvokerTest>>();
    int retries = 5;
    URL url = URL.valueOf("test://test:11/test?retries=" + retries);
    Invoker<FailoverClusterInvokerTest> invoker1 = EasyMock.createMock(Invoker.class);
    Invoker<FailoverClusterInvokerTest> invoker2 = EasyMock.createMock(Invoker.class);
    RpcInvocation invocation = new RpcInvocation();
    Directory<FailoverClusterInvokerTest> dic;
    Result result = new RpcResult();

    /**
     * @throws java.lang.Exception
     */

    @Before
    public void setUp() throws Exception {

        dic = EasyMock.createMock(Directory.class);

        EasyMock.expect(dic.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(dic.list(invocation)).andReturn(invokers).anyTimes();
        EasyMock.expect(dic.getInterface()).andReturn(FailoverClusterInvokerTest.class).anyTimes();
        invocation.setMethodName("method1");
        EasyMock.replay(dic);

        invokers.add(invoker1);
        invokers.add(invoker2);
    }


    @Test
    public void testInvokeWithRuntimeException() {
        EasyMock.reset(invoker1);
        EasyMock.expect(invoker1.invoke(invocation)).andThrow(new RuntimeException()).anyTimes();
        EasyMock.expect(invoker1.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker1.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(invoker1.getInterface()).andReturn(FailoverClusterInvokerTest.class).anyTimes();
        EasyMock.replay(invoker1);

        EasyMock.reset(invoker2);
        EasyMock.expect(invoker2.invoke(invocation)).andThrow(new RuntimeException()).anyTimes();
        EasyMock.expect(invoker2.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker2.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(invoker2.getInterface()).andReturn(FailoverClusterInvokerTest.class).anyTimes();
        EasyMock.replay(invoker2);

        FailoverClusterInvoker<FailoverClusterInvokerTest> invoker = new FailoverClusterInvoker<FailoverClusterInvokerTest>(dic);
        try {
            invoker.invoke(invocation);
            fail();
        } catch (RpcException expected) {
            assertEquals(0, expected.getCode());
            assertFalse(expected.getCause() instanceof RpcException);
        }
    }

    @Test()
    public void testInvokeWithRPCException() {

        EasyMock.reset(invoker1);
        EasyMock.expect(invoker1.invoke(invocation)).andThrow(new RpcException()).anyTimes();
        EasyMock.expect(invoker1.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker1.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(invoker1.getInterface()).andReturn(FailoverClusterInvokerTest.class).anyTimes();
        EasyMock.replay(invoker1);

        EasyMock.reset(invoker2);
        EasyMock.expect(invoker2.invoke(invocation)).andReturn(result).anyTimes();
        EasyMock.expect(invoker2.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker2.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(invoker2.getInterface()).andReturn(FailoverClusterInvokerTest.class).anyTimes();
        EasyMock.replay(invoker2);

        FailoverClusterInvoker<FailoverClusterInvokerTest> invoker = new FailoverClusterInvoker<FailoverClusterInvokerTest>(dic);
        for (int i = 0; i < 100; i++) {
            Result ret = invoker.invoke(invocation);
            assertSame(result, ret);
        }
    }

    @Test()
    public void testInvoke_retryTimes() {

        EasyMock.reset(invoker1);
        EasyMock.expect(invoker1.invoke(invocation)).andThrow(new RpcException(RpcException.TIMEOUT_EXCEPTION)).anyTimes();
        EasyMock.expect(invoker1.isAvailable()).andReturn(false).anyTimes();
        EasyMock.expect(invoker1.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(invoker1.getInterface()).andReturn(FailoverClusterInvokerTest.class).anyTimes();
        EasyMock.replay(invoker1);

        EasyMock.reset(invoker2);
        EasyMock.expect(invoker2.invoke(invocation)).andThrow(new RpcException()).anyTimes();
        EasyMock.expect(invoker2.isAvailable()).andReturn(false).anyTimes();
        EasyMock.expect(invoker2.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(invoker2.getInterface()).andReturn(FailoverClusterInvokerTest.class).anyTimes();
        EasyMock.replay(invoker2);

        FailoverClusterInvoker<FailoverClusterInvokerTest> invoker = new FailoverClusterInvoker<FailoverClusterInvokerTest>(dic);
        try {
            Result ret = invoker.invoke(invocation);
            assertSame(result, ret);
            fail();
        } catch (RpcException expected) {
            assertTrue(expected.isTimeout());
            assertTrue(expected.getMessage().indexOf((retries + 1) + " times") > 0);
        }
    }

    @Test()
    public void testNoInvoke() {
        dic = EasyMock.createMock(Directory.class);

        EasyMock.expect(dic.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(dic.list(invocation)).andReturn(null).anyTimes();
        EasyMock.expect(dic.getInterface()).andReturn(FailoverClusterInvokerTest.class).anyTimes();
        invocation.setMethodName("method1");
        EasyMock.replay(dic);

        invokers.add(invoker1);


        FailoverClusterInvoker<FailoverClusterInvokerTest> invoker = new FailoverClusterInvoker<FailoverClusterInvokerTest>(dic);
        try {
            invoker.invoke(invocation);
            fail();
        } catch (RpcException expected) {
            assertFalse(expected.getCause() instanceof RpcException);
        }
    }

    /**
     * 测试在调用重试过程中，directory列表变更，invoke重试时重新进行list选择
     */
    @Test
    public void testInvokerDestoryAndReList() {
        final URL url = URL.valueOf("test://localhost/" + Demo.class.getName() + "?loadbalance=roundrobin&retries=" + retries);
        RpcException exception = new RpcException(RpcException.TIMEOUT_EXCEPTION);
        MockInvoker<Demo> invoker1 = new MockInvoker<Demo>(Demo.class, url);
        invoker1.setException(exception);

        MockInvoker<Demo> invoker2 = new MockInvoker<Demo>(Demo.class, url);
        invoker2.setException(exception);

        final List<Invoker<Demo>> invokers = new ArrayList<Invoker<Demo>>();
        invokers.add(invoker1);
        invokers.add(invoker2);

        Callable<Object> callable = new Callable<Object>() {
            public Object call() throws Exception {
                //模拟invoker全部被destroy掉
                for (Invoker<Demo> invoker : invokers) {
                    invoker.destroy();
                }
                invokers.clear();
                MockInvoker<Demo> invoker3 = new MockInvoker<Demo>(Demo.class, url);
                invokers.add(invoker3);
                return null;
            }
        };
        invoker1.setCallable(callable);
        invoker2.setCallable(callable);

        RpcInvocation inv = new RpcInvocation();
        inv.setMethodName("test");

        Directory<Demo> dic = new MockDirectory<Demo>(url, invokers);

        FailoverClusterInvoker<Demo> clusterinvoker = new FailoverClusterInvoker<Demo>(dic);
        clusterinvoker.invoke(inv);
    }

    public static interface Demo {
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

    public class MockDirectory<T> extends StaticDirectory<T> {
        public MockDirectory(URL url, List<Invoker<T>> invokers) {
            super(url, invokers);
        }

        @Override
        protected List<Invoker<T>> doList(Invocation invocation) throws RpcException {
            return new ArrayList<Invoker<T>>(super.doList(invocation));
        }
    }
}