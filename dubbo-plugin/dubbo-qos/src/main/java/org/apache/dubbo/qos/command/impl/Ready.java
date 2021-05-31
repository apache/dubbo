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
package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.qos.textui.TTable;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Cmd(name = "ready",summary = "Judge if application or service has started? ")
public class Ready implements BaseCommand {

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        String serviceName = args.length > 0 ? args[0] : null;
        if (StringUtils.isEmpty(serviceName)) {
            // judge application has started
            return DubboBootstrap.getInstance().isReady() ? "true" : "false";
        } else {
            // judge service has started
            Map<String, Boolean> serviceReadyMap = isServiceReady(serviceName);
            if (serviceReadyMap == null || serviceReadyMap.size() <= 0) {
                return "can't match service=" + serviceName;
            }
            return buildUiText(serviceReadyMap);
        }
    }

    private String buildUiText(Map<String, Boolean> serviceReadyMap) {
        TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(TTable.Align.MIDDLE),
                new TTable.ColumnDefine(TTable.Align.MIDDLE)
        });

        //Header
        tTable.addRow("Provider Service Name", "STATUS");
        for (Map.Entry<String, Boolean> entry : serviceReadyMap.entrySet()) {
            String status = Boolean.TRUE.equals(entry.getValue()) ? "TRUE" : "FALSE";
            tTable.addRow(entry.getKey(),status);
        }
        return tTable.rendering();
    }

    /**
     * judge service provider is started
     * @param serviceName service name,eg: org.apache.dubbo.demo.DemoService
     * @return Map[serviceKey,isStarted] eg:[org.apache.dubbo.demo.DemoService,true] or [group1/org.apache.dubbo.demo.DemoService,false]
     */
    private Map<String,Boolean> isServiceReady(String serviceName) {
        Map<String,Boolean> res = new HashMap<>();
        for (ProviderModel providerModel : ApplicationModel.allProviderModels()) {
            String serviceKey = providerModel.getServiceKey();
            String interfaceName = providerModel.getServiceConfig().getInterface();
            if (interfaceName.equals(serviceName)) {
                List<URL> needRegistryURLs = ConfigValidationUtils.loadRegistries(providerModel.getServiceConfig(), true);
                List<URL> registeredRegistryURLs = providerModel.getStatedUrl().stream()
                        .filter(x -> Boolean.TRUE.equals(x.isRegistered()))
                        .map(ProviderModel.RegisterStatedURL::getRegistryUrl)
                        .collect(Collectors.toList());
                if (needRegistryURLs.size() == registeredRegistryURLs.size()) {
                    res.put(serviceKey,true);
                } else {
                    res.put(serviceKey,false);
                }
            }
        }
        return res;
    }

}
