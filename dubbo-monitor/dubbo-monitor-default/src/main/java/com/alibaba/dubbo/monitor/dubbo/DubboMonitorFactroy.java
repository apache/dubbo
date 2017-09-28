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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.monitor.Monitor;
import com.alibaba.dubbo.monitor.MonitorService;
import com.alibaba.dubbo.monitor.support.AbstractMonitorFactory;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;

/**
 * DefaultMonitorFactroy
 *
 * @author william.liangf
 */
public class DubboMonitorFactroy extends AbstractMonitorFactory {

    private Protocol protocol;

    private ProxyFactory proxyFactory;

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    protected Monitor createMonitor(URL url) {
        url = url.setProtocol(url.getParameter(Constants.PROTOCOL_KEY, "dubbo"));
        if (url.getPath() == null || url.getPath().length() == 0) {
            url = url.setPath(MonitorService.class.getName());
        }
        String filter = url.getParameter(Constants.REFERENCE_FILTER_KEY);
        if (filter == null || filter.length() == 0) {
            filter = "";
        } else {
            filter = filter + ",";
        }
        url = url.addParameters(Constants.CLUSTER_KEY, "failsafe", Constants.CHECK_KEY, String.valueOf(false),
                Constants.REFERENCE_FILTER_KEY, filter + "-monitor");
        Invoker<MonitorService> monitorInvoker = protocol.refer(MonitorService.class, url);
        MonitorService monitorService = proxyFactory.getProxy(monitorInvoker);
        return new DubboMonitor(monitorInvoker, monitorService);
    }

}