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
package com.alibaba.dubbo.monitor.filter.support.dubbo;

import junit.framework.Assert;

import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.monitor.MonitorService;
import com.alibaba.dubbo.monitor.support.dubbo.DubboMonitor;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
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
            return URL.valueOf("dubbo://127.0.0.1:7070?interval=1");
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

        public void count(URL statistics) {
            DubboMonitorTest.this.lastStatistics = statistics;
        }
        
    };
    
    @Test
    public void testCount() throws Exception {
        DubboMonitor monitor = new DubboMonitor(monitorInvoker, monitorService);
        URL statistics = new URL("dubbo", "10.20.153.10", 0)
            .addParameter(MonitorService.APPLICATION, "morgan")
            .addParameter(MonitorService.INTERFACE, "MemberService")
            .addParameter(MonitorService.METHOD, "findPerson")
            .addParameter(MonitorService.CLIENT, "10.20.153.11")
            .addParameter(MonitorService.SUCCESS, 1)
            .addParameter(MonitorService.FAILURE, 0)
            .addParameter(MonitorService.ELAPSED, 3)
            .addParameter(MonitorService.MAX_ELAPSED, 3)
            .addParameter(MonitorService.CONCURRENT, 1)
            .addParameter(MonitorService.MAX_CONCURRENT, 1);
        monitor.count(statistics);
        while (lastStatistics == null) {
            Thread.sleep(10);
        }
        Assert.assertEquals(statistics, lastStatistics);
        monitor.destroy();
    }

}