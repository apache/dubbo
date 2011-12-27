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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.easymock.EasyMock;
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
 * FailoverClusterInvokerTest
 * @author liuchao
 *
 */
@SuppressWarnings("unchecked")
public class FailoverClusterInvokerTest {
    List<Invoker<FailoverClusterInvokerTest>> invokers = new ArrayList<Invoker<FailoverClusterInvokerTest>>();
    int retries = 5;
    URL url = URL.valueOf("test://test:11/test?retries="+retries);
    Invoker<FailoverClusterInvokerTest> invoker1 = EasyMock.createMock(Invoker.class);
    Invoker<FailoverClusterInvokerTest> invoker2 = EasyMock.createMock(Invoker.class);
    Invocation invocation;
    Directory<FailoverClusterInvokerTest> dic ;
    Result result = new RpcResult();
    /**
     * @throws java.lang.Exception
     */
    
    @Before
    public void setUp() throws Exception {
        
        dic = EasyMock.createMock(Directory.class);
        invocation = EasyMock.createMock(Invocation.class);
        
        EasyMock.expect(dic.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(dic.list(invocation)).andReturn(invokers).anyTimes();
        EasyMock.expect(dic.getInterface()).andReturn(FailoverClusterInvokerTest.class).anyTimes();
        
        EasyMock.expect(invocation.getMethodName()).andReturn("method1").anyTimes();
        EasyMock.replay(dic,invocation);
        
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
            assertEquals(0,expected.getCode());
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
        for(int i=0;i<100;i++){
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
        try{
            Result ret = invoker.invoke(invocation);
            assertSame(result, ret);
            fail();
        }catch (RpcException expected) {
            assertTrue(expected.isTimeout());
            assertTrue(expected.getMessage().indexOf((retries+1)+" times")>0);
        }
    }
    
    @Test()
    public void testNoInvoke() {
        dic = EasyMock.createMock(Directory.class);
        invocation = EasyMock.createMock(Invocation.class);
        
        EasyMock.expect(dic.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(dic.list(invocation)).andReturn(null).anyTimes();
        EasyMock.expect(dic.getInterface()).andReturn(FailoverClusterInvokerTest.class).anyTimes();
        
        EasyMock.expect(invocation.getMethodName()).andReturn("method1").anyTimes();
        EasyMock.replay(dic,invocation);
        
        invokers.add(invoker1);
        
        
        FailoverClusterInvoker<FailoverClusterInvokerTest> invoker = new FailoverClusterInvoker<FailoverClusterInvokerTest>(dic);
        try {
            invoker.invoke(invocation);
            fail();
        } catch (RpcException expected) {
            expected.printStackTrace();
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
        FailoverClusterInvoker<DemoService> cluster = new FailoverClusterInvoker<DemoService>(directory);
        try {
            cluster.invoke(new RpcInvocation("sayHello", new Class<?>[0], new Object[0]));
            Assert.fail();
        } catch (RpcException e) {
            Assert.assertEquals(RpcException.TIMEOUT_EXCEPTION, e.getCode());
        }
    }
}