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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.directory.StaticDirectory;
import com.alibaba.dubbo.rpc.cluster.filter.DemoService;

/**
 * ForkingClusterInvokerTest
 * 
 * @author tony.chenl
 */
@SuppressWarnings("unchecked")
public class ForkingClusterInvokerTest {

    List<Invoker<ForkingClusterInvokerTest>> invokers = new ArrayList<Invoker<ForkingClusterInvokerTest>>();
    URL                                      url      = URL.valueOf("test://test:11/test?forks=2");
    Invoker<ForkingClusterInvokerTest>       invoker1 = EasyMock.createMock(Invoker.class);
    Invoker<ForkingClusterInvokerTest>       invoker2 = EasyMock.createMock(Invoker.class);
    Invoker<ForkingClusterInvokerTest>       invoker3 = EasyMock.createMock(Invoker.class);
    Invocation                               invocation;
    Directory<ForkingClusterInvokerTest>     dic;
    Result                                   result   = new RpcResult();

    /**
     * @throws java.lang.Exception
     */

    @Before
    public void setUp() throws Exception {

        dic = EasyMock.createMock(Directory.class);
        invocation = EasyMock.createMock(Invocation.class);

        EasyMock.expect(dic.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(dic.list(invocation)).andReturn(invokers).anyTimes();
        EasyMock.expect(dic.getInterface()).andReturn(ForkingClusterInvokerTest.class).anyTimes();

        EasyMock.expect(invocation.getMethodName()).andReturn("method1").anyTimes();
        EasyMock.replay(dic, invocation);

        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);

    }

    @After
    public void tearDown() {
        EasyMock.verify(invoker1, dic, invocation);

    }

    private void resetInvokerToException() {
        EasyMock.reset(invoker1);
        EasyMock.expect(invoker1.invoke(invocation)).andThrow(new RuntimeException()).anyTimes();
        EasyMock.expect(invoker1.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(invoker1.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker1.getInterface()).andReturn(ForkingClusterInvokerTest.class).anyTimes();
        EasyMock.replay(invoker1);
        EasyMock.reset(invoker2);
        EasyMock.expect(invoker2.invoke(invocation)).andThrow(new RuntimeException()).anyTimes();
        EasyMock.expect(invoker2.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(invoker2.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker2.getInterface()).andReturn(ForkingClusterInvokerTest.class).anyTimes();
        EasyMock.replay(invoker2);
        EasyMock.reset(invoker3);
        EasyMock.expect(invoker3.invoke(invocation)).andThrow(new RuntimeException()).anyTimes();
        EasyMock.expect(invoker3.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(invoker3.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker3.getInterface()).andReturn(ForkingClusterInvokerTest.class).anyTimes();
        EasyMock.replay(invoker3);
    }

    private void resetInvokerToNoException() {
        EasyMock.reset(invoker1);
        EasyMock.expect(invoker1.invoke(invocation)).andReturn(result).anyTimes();
        EasyMock.expect(invoker1.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(invoker1.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker1.getInterface()).andReturn(ForkingClusterInvokerTest.class).anyTimes();
        EasyMock.replay(invoker1);
        EasyMock.reset(invoker2);
        EasyMock.expect(invoker2.invoke(invocation)).andReturn(result).anyTimes();
        EasyMock.expect(invoker2.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(invoker2.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker2.getInterface()).andReturn(ForkingClusterInvokerTest.class).anyTimes();
        EasyMock.replay(invoker2);
        EasyMock.reset(invoker3);
        EasyMock.expect(invoker3.invoke(invocation)).andReturn(result).anyTimes();
        EasyMock.expect(invoker3.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(invoker3.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker3.getInterface()).andReturn(ForkingClusterInvokerTest.class).anyTimes();
        EasyMock.replay(invoker3);
    }

    @Test
    public void testInvokeExceptoin() {
        resetInvokerToException();
        ForkingClusterInvoker<ForkingClusterInvokerTest> invoker = new ForkingClusterInvoker<ForkingClusterInvokerTest>(
                                                                                     dic);
        
        try {
            invoker.invoke(invocation);
            Assert.fail();
        } catch (RpcException e) {
            Assert.assertTrue(e.getMessage().contains("Failed to forking invoke provider"));
        }
    }

    @Test()
    public void testInvokeNoExceptoin() {

        resetInvokerToNoException();

        ForkingClusterInvoker<ForkingClusterInvokerTest> invoker = new ForkingClusterInvoker<ForkingClusterInvokerTest>(
                                                                                                                        dic);
        Result ret = invoker.invoke(invocation);
        Assert.assertSame(result, ret);
    }
    
    @Test()
    public void testNoInvoke() {
        dic = EasyMock.createMock(Directory.class);
        invocation = EasyMock.createMock(Invocation.class);

        EasyMock.expect(dic.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(dic.list(invocation)).andReturn(null).anyTimes();
        EasyMock.expect(dic.getInterface()).andReturn(ForkingClusterInvokerTest.class).anyTimes();

        EasyMock.expect(invocation.getMethodName()).andReturn("method1").anyTimes();
        EasyMock.replay(dic, invocation);

        resetInvokerToNoException();

        ForkingClusterInvoker<ForkingClusterInvokerTest> invoker = new ForkingClusterInvoker<ForkingClusterInvokerTest>(
                                                                                                                        dic);
        try {
            invoker.invoke(invocation);
        } catch (RpcException expected) {
            assertTrue(expected.getMessage().contains("No provider"));
        }
    }
    
    @Test()
    public void testTimeoutExceptionCode() {
        List<Invoker<DemoService>> invokers = new ArrayList<Invoker<DemoService>>();
        invokers.add(new Invoker<DemoService>() {

            public Class<DemoService> getInterface() {
                return DemoService.class;
            }

            public URL getUrl() {
                return URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/" + DemoService.class.getName());
            }

            public boolean isAvailable() {
                return false;
            }

            public Result invoke(Invocation invocation) throws RpcException {
                throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "test timeout");
            }

            public void destroy() {
            }
        });
        Directory<DemoService> directory = new StaticDirectory<DemoService>(invokers);
        ForkingClusterInvoker<DemoService> cluster = new ForkingClusterInvoker<DemoService>(directory);
        try {
            cluster.invoke(new RpcInvocation("sayHello", new Class<?>[0], new Object[0]));
            Assert.fail();
        } catch (RpcException e) {
            Assert.assertEquals(RpcException.TIMEOUT_EXCEPTION, e.getCode());
        }
    }
}