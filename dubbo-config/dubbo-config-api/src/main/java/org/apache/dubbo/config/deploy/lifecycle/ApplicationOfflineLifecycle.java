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
package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.deploy.lifecycle.event.AppPreDestroyEvent;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Application offline lifecycle.
 */
@Activate(order = -1000)
public class ApplicationOfflineLifecycle implements ApplicationLifecycle {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ApplicationOfflineLifecycle.class);

    @Override
    public boolean needInitialize() {
        return true;
    }

    @Override
    public void preDestroy(AppPreDestroyEvent preDestroyContext) {
        ApplicationModel applicationModel = preDestroyContext.getApplicationModel();

        offline(applicationModel);
        unregisterMetadataServiceInstance(preDestroyContext);

        RegistryApplicationLifecycle registryApplicationLifecycle = applicationModel.getBeanFactory().getBean(RegistryApplicationLifecycle.class);
        Future<?> asyncMetadataFuture = null;

        if(registryApplicationLifecycle != null){
            asyncMetadataFuture =  registryApplicationLifecycle.getAsyncMetadataFuture();
        }
        if (asyncMetadataFuture != null) {
            asyncMetadataFuture.cancel(true);
        }
    }

    private void offline(ApplicationModel applicationModel) {
        try {
            for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
                ModuleServiceRepository serviceRepository = moduleModel.getServiceRepository();
                List<ProviderModel> exportedServices = serviceRepository.getExportedServices();
                for (ProviderModel exportedService : exportedServices) {
                    List<ProviderModel.RegisterStatedURL> statedUrls = exportedService.getStatedUrl();
                    for (ProviderModel.RegisterStatedURL statedURL : statedUrls) {
                        if (statedURL.isRegistered()) {
                            doOffline(statedURL);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logger.error(LoggerCodeConstants.INTERNAL_ERROR, "", "", "Exceptions occurred when unregister services.", t);
        }
    }

    private void doOffline(ProviderModel.RegisterStatedURL statedURL) {
        RegistryFactory registryFactory =
            statedURL.getRegistryUrl().getOrDefaultApplicationModel().getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
        Registry registry = registryFactory.getRegistry(statedURL.getRegistryUrl());
        registry.unregister(statedURL.getProviderUrl());
        statedURL.setRegistered(false);
    }

    private void unregisterMetadataServiceInstance(AppPreDestroyEvent preDestroyContextEvent) {
        if (preDestroyContextEvent.registered().get()) {
            ServiceInstanceMetadataUtils.unregisterMetadataAndInstance(preDestroyContextEvent.getApplicationModel());
        }
    }

}
