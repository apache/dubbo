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
package org.apache.dubbo.rpc.protocol.dubbo.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class TraceFilterTest {

    private MockChannel mockChannel;
    private static final String TRACE_MAX = "trace.max";
    private static final String TRACE_COUNT = "trace.count";
    private static final String TRACERS_FIELD_NAME = "TRACERS";

    @BeforeEach
    public void setUp() {
        URL url = URL.valueOf("dubbo://127.0.0.1:20884/demo");
        mockChannel = new MockChannel(url);
    }

    @AfterEach
    public void tearDown() {
        mockChannel.close();
    }

    @Test
    void testAddAndRemoveTracer() throws Exception {
        String method = "sayHello";
        Class<?> type = DemoService.class;
        String key = type.getName() + "." + method;

        // add tracer
        TraceFilter.addTracer(type, method, mockChannel, 100);

        Assertions.assertEquals(100, mockChannel.getAttribute(TRACE_MAX));
        Assertions.assertTrue(mockChannel.getAttribute(TRACE_COUNT) instanceof AtomicInteger);

        Field tracers = TraceFilter.class.getDeclaredField(TRACERS_FIELD_NAME);
        tracers.setAccessible(true);
        ConcurrentHashMap<String, Set<Channel>> o = (ConcurrentHashMap<String, Set<Channel>>) tracers.get(new ConcurrentHashMap<String, Set<Channel>>());

        Assertions.assertTrue(o.containsKey(key));
        Set<Channel> channels = o.get(key);
        Assertions.assertNotNull(channels);
        Assertions.assertTrue(channels.contains(mockChannel));

        // remove tracer
        TraceFilter.removeTracer(type, method, mockChannel);
        Assertions.assertNull(mockChannel.getAttribute(TRACE_MAX));
        Assertions.assertNull(mockChannel.getAttribute(TRACE_COUNT));
        Assertions.assertFalse(channels.contains(mockChannel));
    }

    @Test
    void testInvoke() throws Exception {
        String method = "sayHello";
        Class<?> type = DemoService.class;
        String key = type.getName() + "." + method;
        // add tracer
        TraceFilter.addTracer(type, method, mockChannel, 2);

        Invoker<DemoService> mockInvoker = mock(Invoker.class);
        Invocation mockInvocation = mock(Invocation.class);
        Result mockResult = mock(Result.class);
        TraceFilter filter = new TraceFilter();

        given(mockInvoker.getInterface()).willReturn(DemoService.class);
        given(mockInvocation.getMethodName()).willReturn(method);
        given(mockInvocation.getArguments()).willReturn(new Object[0]);
        given(mockInvoker.invoke(mockInvocation)).willReturn(mockResult);
        given(mockResult.getValue()).willReturn("result");

        // test invoke
        filter.invoke(mockInvoker, mockInvocation);
        String message = listToString(mockChannel.getReceivedObjects());
        String expectMessage = "org.apache.dubbo.rpc.protocol.dubbo.support.DemoService.sayHello([]) -> \"result\"";
        System.out.println("actual message: " + message);
        Assertions.assertTrue(message.contains(expectMessage));
        Assertions.assertTrue(message.contains("elapsed:"));
        AtomicInteger traceCount = (AtomicInteger) mockChannel.getAttribute(TRACE_COUNT);
        Assertions.assertEquals(1, traceCount.get());

        // test remove channel when count >= max - 1
        filter.invoke(mockInvoker, mockInvocation);
        Field tracers = TraceFilter.class.getDeclaredField(TRACERS_FIELD_NAME);
        tracers.setAccessible(true);
        ConcurrentHashMap<String, Set<Channel>> o = (ConcurrentHashMap<String, Set<Channel>>) tracers.get(new ConcurrentHashMap<String, Set<Channel>>());
        Assertions.assertTrue(o.containsKey(key));
        Set<Channel> channels = o.get(key);
        Assertions.assertNotNull(channels);
        Assertions.assertFalse(channels.contains(mockChannel));
    }

    private static String listToString(List<Object> objectList) {
        StringBuilder sb = new StringBuilder();
        if (CollectionUtils.isEmpty(objectList)) {
            return "";
        }

        objectList.forEach(o -> {
            sb.append(o.toString());
        });
        return sb.toString();
    }
}
