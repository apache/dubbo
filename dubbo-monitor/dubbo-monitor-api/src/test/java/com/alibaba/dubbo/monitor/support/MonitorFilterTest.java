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
package com.alibaba.dubbo.monitor.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.monitor.Monitor;
import com.alibaba.dubbo.monitor.MonitorFactory;
import com.alibaba.dubbo.monitor.MonitorService;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;

import junit.framework.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

/**
 * MonitorFilterTest
 *
 * @author william.liangf
 */
public class MonitorFilterTest {

    private volatile URL lastStatistics;

    private volatile Invocation lastInvocation;

    private final Invoker<MonitorService> serviceInvoker = new Invoker<MonitorService>() {
        public Class<MonitorService> getInterface() {
            return MonitorService.class;
        }

        public URL getUrl() {
            try {
                return URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880?" + Constants.APPLICATION_KEY + "=abc&" + Constants.SIDE_KEY + "=" + Constants.CONSUMER_SIDE + "&" + Constants.MONITOR_KEY + "=" + URLEncoder.encode("dubbo://" + NetUtils.getLocalHost() + ":7070", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }

        public boolean isAvailable() {
            return false;
        }

        public Result invoke(Invocation invocation) throws RpcException {
            lastInvocation = invocation;
            return null;
        }

        public void destroy() {
        }
    };

    private MonitorFactory monitorFactory = new MonitorFactory() {
        public Monitor getMonitor(final URL url) {
            return new Monitor() {
                public URL getUrl() {
                    return url;
                }

                public boolean isAvailable() {
                    return true;
                }

                public void destroy() {
                }

                public void collect(URL statistics) {
                    MonitorFilterTest.this.lastStatistics = statistics;
                }

                public List<URL> lookup(URL query) {
                    return Arrays.asList(MonitorFilterTest.this.lastStatistics);
                }
            };
        }
    };

    @Test
    public void testFilter() throws Exception {
        MonitorFilter monitorFilter = new MonitorFilter();
        monitorFilter.setMonitorFactory(monitorFactory);
        Invocation invocation = new RpcInvocation("aaa", new Class<?>[0], new Object[0]);
        RpcContext.getContext().setRemoteAddress(NetUtils.getLocalHost(), 20880).setLocalAddress(NetUtils.getLocalHost(), 2345);
        monitorFilter.invoke(serviceInvoker, invocation);
        while (lastStatistics == null) {
            Thread.sleep(10);
        }
        Assert.assertEquals("abc", lastStatistics.getParameter(MonitorService.APPLICATION));
        Assert.assertEquals(MonitorService.class.getName(), lastStatistics.getParameter(MonitorService.INTERFACE));
        Assert.assertEquals("aaa", lastStatistics.getParameter(MonitorService.METHOD));
        Assert.assertEquals(NetUtils.getLocalHost() + ":20880", lastStatistics.getParameter(MonitorService.PROVIDER));
        Assert.assertEquals(NetUtils.getLocalHost(), lastStatistics.getAddress());
        Assert.assertEquals(null, lastStatistics.getParameter(MonitorService.CONSUMER));
        Assert.assertEquals(1, lastStatistics.getParameter(MonitorService.SUCCESS, 0));
        Assert.assertEquals(0, lastStatistics.getParameter(MonitorService.FAILURE, 0));
        Assert.assertEquals(1, lastStatistics.getParameter(MonitorService.CONCURRENT, 0));
        Assert.assertEquals(invocation, lastInvocation);
    }

    @Test
    public void testGenericFilter() throws Exception {
        MonitorFilter monitorFilter = new MonitorFilter();
        monitorFilter.setMonitorFactory(monitorFactory);
        Invocation invocation = new RpcInvocation("$invoke", new Class<?>[]{String.class, String[].class, Object[].class}, new Object[]{"xxx", new String[]{}, new Object[]{}});
        RpcContext.getContext().setRemoteAddress(NetUtils.getLocalHost(), 20880).setLocalAddress(NetUtils.getLocalHost(), 2345);
        monitorFilter.invoke(serviceInvoker, invocation);
        while (lastStatistics == null) {
            Thread.sleep(10);
        }
        Assert.assertEquals("abc", lastStatistics.getParameter(MonitorService.APPLICATION));
        Assert.assertEquals(MonitorService.class.getName(), lastStatistics.getParameter(MonitorService.INTERFACE));
        Assert.assertEquals("xxx", lastStatistics.getParameter(MonitorService.METHOD));
        Assert.assertEquals(NetUtils.getLocalHost() + ":20880", lastStatistics.getParameter(MonitorService.PROVIDER));
        Assert.assertEquals(NetUtils.getLocalHost(), lastStatistics.getAddress());
        Assert.assertEquals(null, lastStatistics.getParameter(MonitorService.CONSUMER));
        Assert.assertEquals(1, lastStatistics.getParameter(MonitorService.SUCCESS, 0));
        Assert.assertEquals(0, lastStatistics.getParameter(MonitorService.FAILURE, 0));
        Assert.assertEquals(1, lastStatistics.getParameter(MonitorService.CONCURRENT, 0));
        Assert.assertEquals(invocation, lastInvocation);
    }

}