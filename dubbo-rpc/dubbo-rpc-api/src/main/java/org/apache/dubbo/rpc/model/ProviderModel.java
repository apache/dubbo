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
package org.apache.dubbo.rpc.model;

import java.util.Set;

/**
 * ProviderModel which is about published services
 */
public class ProviderModel {
    private final String serviceKey;
    private final Object serviceInstance;
    private final ServiceModel serviceModel;

    public ProviderModel(String serviceKey, Object serviceInstance, ServiceModel serviceModel) {
        if (null == serviceInstance) {
            throw new IllegalArgumentException("Service[" + serviceKey + "]Target is NULL.");
        }

        this.serviceKey = serviceKey;
        this.serviceInstance = serviceInstance;
        this.serviceModel = serviceModel;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public Class<?> getServiceInterfaceClass() {
        return serviceModel.getServiceInterfaceClass();
    }

    public Object getServiceInstance() {
        return serviceInstance;
    }

    public Set<MethodModel> getAllMethods() {
        return serviceModel.getAllMethods();
    }

    public ServiceModel getServiceModel() {
        return serviceModel;
    }
}
