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

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceRepository;

import java.util.Collection;
import java.util.List;

@Cmd(name = "offline", summary = "offline dubbo", example = {
        "offline dubbo",
        "offline xx.xx.xxx.service"
})
public class Offline implements BaseCommand {
    private Logger logger = LoggerFactory.getLogger(Offline.class);
    private static RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
    private static ServiceRepository serviceRepository = ApplicationModel.getServiceRepository();

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        logger.info("receive offline command");
        String servicePattern = ".*";
        if (args != null && args.length > 0) {
            servicePattern = args[0];
        }

        boolean hasService = offline(servicePattern);

        if (hasService) {
            return "OK";
        } else {
            return "service not found";
        }
    }

    public static boolean offline(String servicePattern) {
        boolean hasService = false;

        Collection<ProviderModel> providerModelList = serviceRepository.getExportedServices();
        for (ProviderModel providerModel : providerModelList) {
            if (providerModel.getServiceMetadata().getDisplayServiceKey().matches(servicePattern)) {
                hasService = true;
                List<ProviderModel.RegisterStatedURL> statedUrls = providerModel.getStatedUrl();
                for (ProviderModel.RegisterStatedURL statedURL : statedUrls) {
                    if (statedURL.isRegistered()) {
                        Registry registry = registryFactory.getRegistry(statedURL.getRegistryUrl());
                        registry.unregister(statedURL.getProviderUrl());
                        statedURL.setRegistered(false);
                    }
                }
            }
        }

        return hasService;
    }
}
