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
package com.alibaba.dubbo.rpc.filter;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.support.DemoService;

import org.easymock.EasyMock;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * ExceptionFilterTest
 *
 * @author william.liangf
 */
public class ExceptionFilterTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testRpcException() {
        Logger logger = EasyMock.createMock(Logger.class);
        RpcContext.getContext().setRemoteAddress("127.0.0.1", 1234);
        RpcException exception = new RpcException("TestRpcException");
        logger.error(EasyMock.eq("Got unchecked and undeclared exception which called by 127.0.0.1. service: " + DemoService.class.getName() + ", method: sayHello, exception: " + RpcException.class.getName() + ": TestRpcException"), EasyMock.eq(exception));
        ExceptionFilter exceptionFilter = new ExceptionFilter(logger);
        RpcInvocation invocation = new RpcInvocation("sayHello", new Class<?>[]{String.class}, new Object[]{"world"});
        Invoker<DemoService> invoker = EasyMock.createMock(Invoker.class);
        EasyMock.expect(invoker.getInterface()).andReturn(DemoService.class);
        EasyMock.expect(invoker.invoke(EasyMock.eq(invocation))).andThrow(exception);

        EasyMock.replay(logger, invoker);

        try {
            exceptionFilter.invoke(invoker, invocation);
        } catch (RpcException e) {
            assertEquals("TestRpcException", e.getMessage());
        }
        EasyMock.verify(logger, invoker);
        RpcContext.removeContext();
    }

}