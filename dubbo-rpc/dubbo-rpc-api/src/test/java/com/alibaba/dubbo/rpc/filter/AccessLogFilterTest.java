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

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.LogUtil;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.support.MockInvocation;
import com.alibaba.dubbo.rpc.support.MyInvoker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * AccessLogFilterTest.java
 *
 * @author tony.chenl
 */
public class AccessLogFilterTest {

    Filter accessLogFilter = new AccessLogFilter();

    // 测试filter不会抛出异常
    @Test
    public void testInvokeException() {
        Invoker<AccessLogFilterTest> invoker = new MyInvoker<AccessLogFilterTest>(null);
        Invocation invocation = new MockInvocation();
        LogUtil.start();
        accessLogFilter.invoke(invoker, invocation);
        assertEquals(1, LogUtil.findMessage("Exception in AcessLogFilter of service"));
        LogUtil.stop();
    }

    // TODO how to assert thread action
    @Test
    public void testDefault() {
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1");
        Invoker<AccessLogFilterTest> invoker = new MyInvoker<AccessLogFilterTest>(url);
        Invocation invocation = new MockInvocation();
        accessLogFilter.invoke(invoker, invocation);
    }

    @Test
    public void testCustom() {
        URL url = URL.valueOf("test://test:11/test?accesslog=alibaba");
        Invoker<AccessLogFilterTest> invoker = new MyInvoker<AccessLogFilterTest>(url);
        Invocation invocation = new MockInvocation();
        accessLogFilter.invoke(invoker, invocation);
    }

}