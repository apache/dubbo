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
package com.alibaba.dubbo.rpc.protocol.dubbo.filter;

import static org.junit.Assert.assertEquals;

import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcConstants;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;

/**
 * EventFilterTest.java
 * 
 * @author tony.chenl
 * TODO 暂时依赖callback集成测试，后续补充
 */
public class FutureFilterTest {
    Filter                    eventFilter = new FutureFilter();
    private static Invocation invocation;

    @BeforeClass
    public static void setUp() {
        invocation = EasyMock.createMock(Invocation.class);
        EasyMock.expect(invocation.getMethodName()).andReturn("echo").anyTimes();
        EasyMock.expect(invocation.getParameterTypes()).andReturn(new Class<?>[] { Enum.class }).anyTimes();
        EasyMock.expect(invocation.getArguments()).andReturn(new Object[] { "hello" }).anyTimes();
        EasyMock.expect(invocation.getAttachments()).andReturn(null).anyTimes();
        EasyMock.replay(invocation);
    }

    @Test
    public void testSyncCallback() {
        Invoker<DemoService> invoker = EasyMock.createMock(Invoker.class);
        EasyMock.expect(invoker.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker.getInterface()).andReturn(DemoService.class).anyTimes();
        RpcResult result = new RpcResult();
        result.setResult("High");
        EasyMock.expect(invoker.invoke(invocation)).andReturn(result).anyTimes();
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");
        EasyMock.expect(invoker.getUrl()).andReturn(url).anyTimes();
        EasyMock.replay(invoker);
        Result filterResult = eventFilter.invoke(invoker, invocation);
        assertEquals("High", filterResult.getResult());
    }

    @Test(expected = RuntimeException.class)
    public void testSyncCallbackHasException() throws RpcException, Throwable {
        Invocation invocation = EasyMock.createMock(Invocation.class);
        EasyMock.expect(invocation.getMethodName()).andReturn("echo").anyTimes();
        EasyMock.expect(invocation.getParameterTypes()).andReturn(new Class<?>[] { Enum.class }).anyTimes();
        EasyMock.expect(invocation.getArguments()).andReturn(new Object[] { "hello" }).anyTimes();
        EasyMock.expect(invocation.getAttachments()).andReturn(null).anyTimes();
        EasyMock.replay(invocation);
        Invoker<DemoService> invoker = EasyMock.createMock(Invoker.class);
        EasyMock.expect(invoker.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker.getInterface()).andReturn(DemoService.class).anyTimes();
        RpcResult result = new RpcResult();
        result.setException(new RuntimeException());
        EasyMock.expect(invoker.invoke(invocation)).andReturn(result).anyTimes();
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1&"+RpcConstants.ON_THROW_METHOD_KEY+"=echo");
        EasyMock.expect(invoker.getUrl()).andReturn(url).anyTimes();
        EasyMock.replay(invoker);
        eventFilter.invoke(invoker, invocation).recreate();
    }
}