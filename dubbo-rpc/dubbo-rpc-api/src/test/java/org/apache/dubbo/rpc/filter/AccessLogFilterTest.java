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
package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.DubboAppender;
import org.apache.dubbo.common.utils.LogUtil;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.support.AccessLogData;
import org.apache.dubbo.rpc.support.MockInvocation;
import org.apache.dubbo.rpc.support.MyInvoker;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AccessLogFilterTest.java
 */
public class AccessLogFilterTest {

    Filter accessLogFilter = new AccessLogFilter();

    // Test filter won't throw an exception
    @Test
    public void testInvokeException() {
        Invoker<AccessLogFilterTest> invoker = new MyInvoker<AccessLogFilterTest>(null);
        Invocation invocation = new MockInvocation();
        LogUtil.start();
        accessLogFilter.invoke(invoker, invocation);
        assertEquals(1, LogUtil.findMessage("Exception in AccessLogFilter of service"));
        LogUtil.stop();
        DubboAppender.clear();
    }

    // TODO how to assert thread action
    @Test
    @SuppressWarnings("unchecked")
    public void testDefault() throws NoSuchFieldException, IllegalAccessException {
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1");
        Invoker<AccessLogFilterTest> invoker = new MyInvoker<AccessLogFilterTest>(url);
        Invocation invocation = new MockInvocation();

        Field field = AccessLogFilter.class.getDeclaredField("LOG_ENTRIES");
        ReflectUtils.makeAccessible(field);
        assertTrue(((Map) field.get(AccessLogFilter.class)).isEmpty());

        accessLogFilter.invoke(invoker, invocation);

        Map<String, Set<AccessLogData>> logs = (Map<String, Set<AccessLogData>>) field.get(AccessLogFilter.class);
        assertFalse(logs.isEmpty());
        assertFalse(logs.get("true").isEmpty());
        AccessLogData log = logs.get("true").iterator().next();
        assertEquals("org.apache.dubbo.rpc.support.DemoService", log.getServiceName());
    }

    @Test
    public void testCustom() {
        URL url = URL.valueOf("test://test:11/test?accesslog=custom-access.log");
        Invoker<AccessLogFilterTest> invoker = new MyInvoker<AccessLogFilterTest>(url);
        Invocation invocation = new MockInvocation();
        accessLogFilter.invoke(invoker, invocation);
    }

}
