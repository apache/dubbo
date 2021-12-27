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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;

import java.util.Collection;
import java.util.List;

public class BaseOnline implements BaseCommand {
    private static final Logger logger = LoggerFactory.getLogger(BaseOnline.class);
    public FrameworkServiceRepository serviceRepository;

    public BaseOnline(FrameworkModel frameworkModel) {
        this.serviceRepository = frameworkModel.getServiceRepository();
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        logger.info("receive online command");
        String servicePattern = ".*";
        if (ArrayUtils.isNotEmpty(args)) {
            servicePattern = "" + args[0];
        }

        boolean hasService = doExecute(servicePattern);
        if (hasService) {
            return "OK";
        } else {
            return "service not found";
        }
    }

    public boolean online(String servicePattern) {
        boolean hasService = false;

        Collection<ProviderModel> providerModelList = serviceRepository.allProviderModels();
        for (ProviderModel providerModel : providerModelList) {
            ServiceMetadata metadata = providerModel.getServiceMetadata();
            if (metadata.getServiceKey().matches(servicePattern) || metadata.getDisplayServiceKey().matches(servicePattern)) {
                hasService = true;
                List<ProviderModel.RegisterStatedURL> statedUrls = providerModel.getStatedUrl();
                for (ProviderModel.RegisterStatedURL statedURL : statedUrls) {
                    if (!statedURL.isRegistered()) {
                        doExport(statedURL);
                    }
                }
            }
        }

        return hasService;
    }

    protected boolean doExecute(String servicePattern) {
        return this.online(servicePattern);
    }

    protected void doExport(ProviderModel.RegisterStatedURL statedURL) {
        RegistryFactory registryFactory =
            statedURL.getRegistryUrl().getOrDefaultApplicationModel().getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
        Registry registry = registryFactory.getRegistry(statedURL.getRegistryUrl());
        registry.register(statedURL.getProviderUrl());
        statedURL.setRegistered(true);
    }
}
