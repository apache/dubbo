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
package com.alibaba.dubbo.rpc.protocol.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.protocol.dubbo.filter.FutureFilter;
import com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService;

import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * EventFilterTest.java
 *
 * @author tony.chenl
 *         TODO 暂时依赖callback集成测试，后续补充
 */
public class FutureFilterTest {
    private static RpcInvocation invocation;
    Filter eventFilter = new FutureFilter();

    @BeforeClass
    public static void setUp() {
        invocation = new RpcInvocation();
        invocation.setMethodName("echo");
        invocation.setParameterTypes(new Class<?>[]{Enum.class});
        invocation.setArguments(new Object[]{"hello"});
    }

    @Test
    public void testSyncCallback() {
        @SuppressWarnings("unchecked")
        Invoker<DemoService> invoker = EasyMock.createMock(Invoker.class);
        EasyMock.expect(invoker.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker.getInterface()).andReturn(DemoService.class).anyTimes();
        RpcResult result = new RpcResult();
        result.setValue("High");
        EasyMock.expect(invoker.invoke(invocation)).andReturn(result).anyTimes();
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");
        EasyMock.expect(invoker.getUrl()).andReturn(url).anyTimes();
        EasyMock.replay(invoker);
        Result filterResult = eventFilter.invoke(invoker, invocation);
        assertEquals("High", filterResult.getValue());
    }

    @Test(expected = RuntimeException.class)
    public void testSyncCallbackHasException() throws RpcException, Throwable {
        @SuppressWarnings("unchecked")
        Invoker<DemoService> invoker = EasyMock.createMock(Invoker.class);
        EasyMock.expect(invoker.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker.getInterface()).andReturn(DemoService.class).anyTimes();
        RpcResult result = new RpcResult();
        result.setException(new RuntimeException());
        EasyMock.expect(invoker.invoke(invocation)).andReturn(result).anyTimes();
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1&" + Constants.ON_THROW_METHOD_KEY + "=echo");
        EasyMock.expect(invoker.getUrl()).andReturn(url).anyTimes();
        EasyMock.replay(invoker);
        eventFilter.invoke(invoker, invocation).recreate();
    }
}