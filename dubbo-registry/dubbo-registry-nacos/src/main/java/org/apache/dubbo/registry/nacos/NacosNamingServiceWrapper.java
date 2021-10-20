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
package org.apache.dubbo.registry.nacos;

import org.apache.dubbo.common.utils.StringUtils;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;

import java.util.List;

public class NacosNamingServiceWrapper {

    private static final String INNERCLASS_SYMBOL = "$";

    private static final String INNERCLASS_COMPATIBLE_SYMBOL = "___";

    private final NamingService namingService;

    public NacosNamingServiceWrapper(NamingService namingService) {
        this.namingService = namingService;
    }


    public String getServerStatus() {
        return namingService.getServerStatus();
    }

    public void subscribe(String serviceName, EventListener eventListener) throws NacosException {
        namingService.subscribe(handleInnerSymbol(serviceName), eventListener);
    }

    public void subscribe(String serviceName, String group, EventListener eventListener) throws NacosException {
        namingService.subscribe(handleInnerSymbol(serviceName), group, eventListener);
    }

    public List<Instance> getAllInstances(String serviceName, String group) throws NacosException {
        return namingService.getAllInstances(handleInnerSymbol(serviceName), group);
    }

    public void registerInstance(String serviceName, String group, Instance instance) throws NacosException {
        namingService.registerInstance(handleInnerSymbol(serviceName), group, instance);
    }

    public void deregisterInstance(String serviceName, String group, String ip, int port) throws NacosException {
        namingService.deregisterInstance(handleInnerSymbol(serviceName), group, ip, port);
    }


    public void deregisterInstance(String serviceName, String group, Instance instance) throws NacosException {
        namingService.deregisterInstance(handleInnerSymbol(serviceName), group, instance);
    }

    public ListView<String> getServicesOfServer(int pageNo, int pageSize, String parameter) throws NacosException {
        return namingService.getServicesOfServer(pageNo, pageSize, parameter);
    }

    public List<Instance> selectInstances(String serviceName, boolean healthy) throws NacosException {
        return namingService.selectInstances(handleInnerSymbol(serviceName), healthy);
    }

    public List<Instance> selectInstances(String serviceName, String group, boolean healthy) throws NacosException {
        return namingService.selectInstances(handleInnerSymbol(serviceName), group, healthy);
    }

    public void shutdown() throws NacosException {
        this.namingService.shutDown();
    }

    /**
     * see https://github.com/apache/dubbo/issues/7129
     * nacos service name just support `0-9a-zA-Z-._:`, grpc interface is inner interface, need compatible.
     */
    private String handleInnerSymbol(String serviceName) {
        if (StringUtils.isEmpty(serviceName)) {
            return null;
        }
        return serviceName.replace(INNERCLASS_SYMBOL, INNERCLASS_COMPATIBLE_SYMBOL);
    }
}
