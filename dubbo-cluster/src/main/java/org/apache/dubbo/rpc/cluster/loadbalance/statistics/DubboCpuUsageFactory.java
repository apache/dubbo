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
package org.apache.dubbo.rpc.cluster.loadbalance.statistics;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.CpuUsageFactory;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.remoting.Constants.CHECK_KEY;
import static org.apache.dubbo.rpc.Constants.REFERENCE_FILTER_KEY;

public class DubboCpuUsageFactory implements CpuUsageFactory {

    private Protocol protocol;

    private ProxyFactory proxyFactory;

    @Override
    public CpuUsageService createCpuUsageService(URL url) {
        URLBuilder urlBuilder = URLBuilder.from(url);
        urlBuilder.setProtocol(url.getParameter(PROTOCOL_KEY, DUBBO_PROTOCOL));
        urlBuilder.addParameters(CHECK_KEY, String.valueOf(false), REFERENCE_FILTER_KEY, "");
        if (StringUtils.isEmpty(url.getPath())) {
            urlBuilder.setPath(CpuUsageService.class.getName());
        }
        Invoker<CpuUsageService> cpuUsageInvoker = protocol.refer(CpuUsageService.class, urlBuilder.build());
        return proxyFactory.getProxy(cpuUsageInvoker);
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }
}
