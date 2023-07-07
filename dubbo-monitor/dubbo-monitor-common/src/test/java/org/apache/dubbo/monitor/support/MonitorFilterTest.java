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
package org.apache.dubbo.monitor.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.monitor.Monitor;
import org.apache.dubbo.monitor.MonitorFactory;
import org.apache.dubbo.monitor.MonitorService;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHOD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.monitor.Constants.CONCURRENT_KEY;
import static org.apache.dubbo.monitor.Constants.FAILURE_KEY;
import static org.apache.dubbo.monitor.Constants.SUCCESS_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * MonitorFilterTest
 */
class MonitorFilterTest {

    private volatile URL lastStatistics;

    private volatile Invocation lastInvocation;

    private final Invoker<MonitorService> serviceInvoker = new Invoker<MonitorService>() {
        @Override
        public Class<MonitorService> getInterface() {
            return MonitorService.class;
        }

        public URL getUrl() {
            return URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880?" +
                    APPLICATION_KEY + "=abc&" + SIDE_KEY + "=" + CONSUMER_SIDE)
                .putAttribute(MONITOR_KEY, URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":7070"));
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            lastInvocation = invocation;
            return AsyncRpcResult.newDefaultAsyncResult(invocation);
        }

        @Override
        public void destroy() {
        }
    };

    private MonitorFactory monitorFactory = url -> new Monitor() {
        public URL getUrl() {
            return url;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void destroy() {
        }

        public void collect(URL statistics) {
            MonitorFilterTest.this.lastStatistics = statistics;
        }

        public List<URL> lookup(URL query) {
            return Arrays.asList(MonitorFilterTest.this.lastStatistics);
        }
    };

    @Test
    void testFilter() throws Exception {
        MonitorFilter monitorFilter = new MonitorFilter();
        monitorFilter.setMonitorFactory(monitorFactory);
        Invocation invocation = new RpcInvocation("aaa", MonitorService.class.getName(), "", new Class<?>[0], new Object[0]);
        RpcContext.getServiceContext().setRemoteAddress(NetUtils.getLocalHost(), 20880).setLocalAddress(NetUtils.getLocalHost(), 2345);
        Result result = monitorFilter.invoke(serviceInvoker, invocation);
        result.whenCompleteWithContext((r, t) -> {
            if (t == null) {
                monitorFilter.onResponse(r, serviceInvoker, invocation);
            } else {
                monitorFilter.onError(t, serviceInvoker, invocation);
            }
        });
        while (lastStatistics == null) {
            Thread.sleep(10);
        }
        Assertions.assertEquals("abc", lastStatistics.getParameter(APPLICATION_KEY));
        Assertions.assertEquals(MonitorService.class.getName(), lastStatistics.getParameter(INTERFACE_KEY));
        Assertions.assertEquals("aaa", lastStatistics.getParameter(METHOD_KEY));
        Assertions.assertEquals(NetUtils.getLocalHost() + ":20880", lastStatistics.getParameter(PROVIDER));
        Assertions.assertEquals(NetUtils.getLocalHost(), lastStatistics.getAddress());
        Assertions.assertNull(lastStatistics.getParameter(CONSUMER));
        Assertions.assertEquals(1, lastStatistics.getParameter(SUCCESS_KEY, 0));
        Assertions.assertEquals(0, lastStatistics.getParameter(FAILURE_KEY, 0));
        Assertions.assertEquals(1, lastStatistics.getParameter(CONCURRENT_KEY, 0));
        Assertions.assertEquals(invocation, lastInvocation);
    }

    @Test
    void testSkipMonitorIfNotHasKey() {
        MonitorFilter monitorFilter = new MonitorFilter();
        MonitorFactory mockMonitorFactory = mock(MonitorFactory.class);
        monitorFilter.setMonitorFactory(mockMonitorFactory);
        Invocation invocation = new RpcInvocation("aaa", MonitorService.class.getName(), "", new Class<?>[0], new Object[0]);
        Invoker invoker = mock(Invoker.class);
        given(invoker.getUrl()).willReturn(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880?" + APPLICATION_KEY + "=abc&" + SIDE_KEY + "=" + CONSUMER_SIDE));

        monitorFilter.invoke(invoker, invocation);

        verify(mockMonitorFactory, never()).getMonitor(any(URL.class));
    }

    @Test
    void testGenericFilter() throws Exception {
        MonitorFilter monitorFilter = new MonitorFilter();
        monitorFilter.setMonitorFactory(monitorFactory);
        Invocation invocation = new RpcInvocation("$invoke", MonitorService.class.getName(), "", new Class<?>[]{String.class, String[].class, Object[].class}, new Object[]{"xxx", new String[]{}, new Object[]{}});
        RpcContext.getServiceContext().setRemoteAddress(NetUtils.getLocalHost(), 20880).setLocalAddress(NetUtils.getLocalHost(), 2345);
        Result result = monitorFilter.invoke(serviceInvoker, invocation);
        result.whenCompleteWithContext((r, t) -> {
            if (t == null) {
                monitorFilter.onResponse(r, serviceInvoker, invocation);
            } else {
                monitorFilter.onError(t, serviceInvoker, invocation);
            }
        });
        while (lastStatistics == null) {
            Thread.sleep(10);
        }
        Assertions.assertEquals("abc", lastStatistics.getParameter(APPLICATION_KEY));
        Assertions.assertEquals(MonitorService.class.getName(), lastStatistics.getParameter(INTERFACE_KEY));
        Assertions.assertEquals("xxx", lastStatistics.getParameter(METHOD_KEY));
        Assertions.assertEquals(NetUtils.getLocalHost() + ":20880", lastStatistics.getParameter(PROVIDER));
        Assertions.assertEquals(NetUtils.getLocalHost(), lastStatistics.getAddress());
        Assertions.assertNull(lastStatistics.getParameter(CONSUMER));
        Assertions.assertEquals(1, lastStatistics.getParameter(SUCCESS_KEY, 0));
        Assertions.assertEquals(0, lastStatistics.getParameter(FAILURE_KEY, 0));
        Assertions.assertEquals(1, lastStatistics.getParameter(CONCURRENT_KEY, 0));
        Assertions.assertEquals(invocation, lastInvocation);
    }

    @Test
    void testSafeFailForMonitorCollectFail() {
        MonitorFilter monitorFilter = new MonitorFilter();
        MonitorFactory mockMonitorFactory = mock(MonitorFactory.class);
        Monitor mockMonitor = mock(Monitor.class);
        Mockito.doThrow(new RuntimeException()).when(mockMonitor).collect(any(URL.class));

        monitorFilter.setMonitorFactory(mockMonitorFactory);
        given(mockMonitorFactory.getMonitor(any(URL.class))).willReturn(mockMonitor);
        Invocation invocation = new RpcInvocation("aaa", MonitorService.class.getName(), "", new Class<?>[0], new Object[0]);

        monitorFilter.invoke(serviceInvoker, invocation);
    }

    @Test
    void testOnResponseWithoutStartTime() {
        MonitorFilter monitorFilter = new MonitorFilter();
        MonitorFactory mockMonitorFactory = mock(MonitorFactory.class);
        Monitor mockMonitor = mock(Monitor.class);
        monitorFilter.setMonitorFactory(mockMonitorFactory);
        given(mockMonitorFactory.getMonitor(any(URL.class))).willReturn(mockMonitor);
        Invocation invocation = new RpcInvocation("aaa", MonitorService.class.getName(), "", new Class<?>[0], new Object[0]);

        Result result = monitorFilter.invoke(serviceInvoker, invocation);
        invocation.getAttributes().remove("monitor_filter_start_time");

        monitorFilter.onResponse(result, serviceInvoker, invocation);
    }

    @Test
    void testOnErrorWithoutStartTime() {
        MonitorFilter monitorFilter = new MonitorFilter();
        MonitorFactory mockMonitorFactory = mock(MonitorFactory.class);
        Monitor mockMonitor = mock(Monitor.class);
        monitorFilter.setMonitorFactory(mockMonitorFactory);
        given(mockMonitorFactory.getMonitor(any(URL.class))).willReturn(mockMonitor);
        Invocation invocation = new RpcInvocation("aaa", MonitorService.class.getName(), "", new Class<?>[0], new Object[0]);

        Throwable rpcException = new RpcException();
        monitorFilter.onError(rpcException, serviceInvoker, invocation);
    }
}