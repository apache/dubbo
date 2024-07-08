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
package org.apache.dubbo.spring.boot.actuate.endpoint.metadata;

import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ProviderModel.RegisterStatedURL;
import org.apache.dubbo.rpc.model.ServiceMetadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class DubboOnlineMetadata extends AbstractDubboMetadata {

    @Autowired
    public ApplicationModel applicationModel;

    public boolean online() {
        System.out.println("执行上线");
        String servicePattern = ".*";
        boolean hasService = false;

        Collection<ProviderModel> providerModelList
                = applicationModel.getApplicationServiceRepository().allProviderModels();
        System.out.println(providerModelList);
        for (ProviderModel providerModel : providerModelList) {
            ServiceMetadata metadata = providerModel.getServiceMetadata();
            if (metadata.getServiceKey().matches(servicePattern)
                    || metadata.getServiceKey().matches(servicePattern)) {
                hasService = true;
                List<RegisterStatedURL> statedUrls = providerModel.getStatedUrl();
                for (ProviderModel.RegisterStatedURL statedUrl : statedUrls) {
                    if(!statedUrl.isRegistered()){
                        doExport(statedUrl);
                    }
                }
            }
        }

        return hasService;
    }

    protected void doExport(ProviderModel.RegisterStatedURL statedURL){
        RegistryFactory registryFactory = statedURL
                .getRegistryUrl()
                .getOrDefaultApplicationModel()
                .getExtensionLoader(RegistryFactory.class)
                .getAdaptiveExtension();
        Registry registry = registryFactory.getRegistry(statedURL.getRegistryUrl());
        registry.register(statedURL.getProviderUrl());
        statedURL.setRegistered(true);
    }
}
