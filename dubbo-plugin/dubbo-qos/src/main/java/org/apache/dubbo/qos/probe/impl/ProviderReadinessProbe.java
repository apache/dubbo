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
package org.apache.dubbo.qos.probe.impl;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.qos.probe.ReadinessProbe;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.Collection;

@Activate
public class ProviderReadinessProbe implements ReadinessProbe {
    private final FrameworkModel frameworkModel;
    private final FrameworkServiceRepository serviceRepository;

    public ProviderReadinessProbe(FrameworkModel frameworkModel) {
        if (frameworkModel != null) {
            this.frameworkModel = frameworkModel;
        } else {
            this.frameworkModel = FrameworkModel.defaultModel();
        }
        this.serviceRepository = this.frameworkModel.getServiceRepository();
    }

    @Override
    public boolean check() {
        Collection<ProviderModel> providerModelList = serviceRepository.allProviderModels();
        if (providerModelList.isEmpty()) {
            return true;
        }

        boolean hasService = false, anyOnline = false;
        for (ProviderModel providerModel : providerModelList) {
            if (providerModel.getModuleModel().isInternal()) {
                continue;
            }
            hasService = true;
            anyOnline = anyOnline ||
                providerModel.getStatedUrl().isEmpty() ||
                providerModel.getStatedUrl().stream().anyMatch(ProviderModel.RegisterStatedURL::isRegistered);
        }

        // no service => check pass
        // has service and any online => check pass
        // has service and none online => check fail
        return !(hasService && !anyOnline);
    }
}
