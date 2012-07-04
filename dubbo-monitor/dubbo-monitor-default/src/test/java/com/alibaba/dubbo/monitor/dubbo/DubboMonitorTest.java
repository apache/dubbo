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
package com.alibaba.dubbo.monitor.dubbo;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.monitor.Monitor;
import com.alibaba.dubbo.monitor.MonitorFactory;
import com.alibaba.dubbo.monitor.MonitorService;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * DubboMonitorTest
 * 
 * @author william.liangf
 */
public class DubboMonitorTest {
    
    private volatile URL lastStatistics;
    
    private final Invoker<MonitorService> monitorInvoker = new Invoker<MonitorService>() {
        public Class<MonitorService> getInterface() {
            return MonitorService.class;
        }
        public URL getUrl() {
            return URL.valueOf("dubbo://127.0.0.1:7070?interval=20");
        }
        public boolean isAvailable() {
            return false;
        }
        public Result invoke(Invocation invocation) throws RpcException {
            return null;
        }
        public void destroy() {
        }
    };
    
    private final MonitorService monitorService = new MonitorService() {

        public void collect(URL statistics) {
            DubboMonitorTest.this.lastStatistics = statistics;
        }

		public List<URL> lookup(URL query) {
			return Arrays.asList(DubboMonitorTest.this.lastStatistics);
		}
        
    };
    
    @Test
    public void testCount() throws Exception {
        DubboMonitor monitor = new DubboMonitor(monitorInvoker, monitorService);
        URL statistics = new URL("dubbo", "10.20.153.10", 0)
            .addParameter(MonitorService.APPLICATION, "morgan")
            .addParameter(MonitorService.INTERFACE, "MemberService")
            .addParameter(MonitorService.METHOD, "findPerson")
            .addParameter(MonitorService.CONSUMER, "10.20.153.11")
            .addParameter(MonitorService.SUCCESS, 1)
            .addParameter(MonitorService.FAILURE, 0)
            .addParameter(MonitorService.ELAPSED, 3)
            .addParameter(MonitorService.MAX_ELAPSED, 3)
            .addParameter(MonitorService.CONCURRENT, 1)
            .addParameter(MonitorService.MAX_CONCURRENT, 1);
        monitor.collect(statistics);
        while (lastStatistics == null) {
            Thread.sleep(10);
        }
        Assert.assertEquals(lastStatistics.getParameter(MonitorService.APPLICATION), "morgan");
        Assert.assertEquals(lastStatistics.getProtocol(), "dubbo");
        Assert.assertEquals(lastStatistics.getHost(), "10.20.153.10");
        Assert.assertEquals(lastStatistics.getParameter(MonitorService.APPLICATION), "morgan");
        Assert.assertEquals(lastStatistics.getParameter(MonitorService.INTERFACE), "MemberService");
        Assert.assertEquals(lastStatistics.getParameter(MonitorService.METHOD), "findPerson");
        Assert.assertEquals(lastStatistics.getParameter(MonitorService.CONSUMER), "10.20.153.11");
        Assert.assertEquals(lastStatistics.getParameter(MonitorService.SUCCESS), "1");
        Assert.assertEquals(lastStatistics.getParameter(MonitorService.FAILURE), "0");
        Assert.assertEquals(lastStatistics.getParameter(MonitorService.ELAPSED), "3");
        Assert.assertEquals(lastStatistics.getParameter(MonitorService.MAX_ELAPSED), "3");
        Assert.assertEquals(lastStatistics.getParameter(MonitorService.CONCURRENT), "1");
        Assert.assertEquals(lastStatistics.getParameter(MonitorService.MAX_CONCURRENT), "1");
        monitor.destroy();
    }
    
    @Test
    public void testMonitorFactory() throws Exception {
        MockMonitorService monitorService = new MockMonitorService();
        URL statistics = new URL("dubbo", "10.20.153.10", 0)
                .addParameter(MonitorService.APPLICATION, "morgan")
                .addParameter(MonitorService.INTERFACE, "MemberService")
                .addParameter(MonitorService.METHOD, "findPerson")
                .addParameter(MonitorService.CONSUMER, "10.20.153.11")
                .addParameter(MonitorService.SUCCESS, 1)
                .addParameter(MonitorService.FAILURE, 0)
                .addParameter(MonitorService.ELAPSED, 3)
                .addParameter(MonitorService.MAX_ELAPSED, 3)
                .addParameter(MonitorService.CONCURRENT, 1)
                .addParameter(MonitorService.MAX_CONCURRENT, 1);
        
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        MonitorFactory monitorFactory = ExtensionLoader.getExtensionLoader(MonitorFactory.class).getAdaptiveExtension();

        Exporter<MonitorService> exporter = protocol.export(proxyFactory.getInvoker(monitorService, MonitorService.class, URL.valueOf("dubbo://127.0.0.1:17979/" + MonitorService.class.getName())));
        try {
            Monitor monitor = monitorFactory.getMonitor(URL.valueOf("dubbo://127.0.0.1:17979?interval=10"));
            try {
                monitor.collect(statistics);
                int i = 0;
                while(monitorService.getStatistics() == null && i < 200) {
                    i ++;
                    Thread.sleep(10);
                }
                URL result = monitorService.getStatistics();
                Assert.assertEquals(1, result.getParameter(MonitorService.SUCCESS, 0));
                Assert.assertEquals(3, result.getParameter(MonitorService.ELAPSED, 0));
            } finally {
                monitor.destroy();
            }
        } finally {
            exporter.unexport();
        }
    }

}