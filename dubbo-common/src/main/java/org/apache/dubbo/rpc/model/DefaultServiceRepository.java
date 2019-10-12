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

import java.util.List;
import java.util.Map;

public class DefaultServiceRepository implements ServiceRepository {

    // services
    Map<String, ServiceModel> services;

    // consumers
    Map<String, ConsumerModel> consumers;

    // providers
    Map<String, ProviderModel> providers;


    @Override
    public void registerService(Class<?> interfaceClazz) {

    }

    @Override
    public void registerConsumer(String serviceKey, Object proxyObject, ServiceModel serviceModel, Map<String, Object> attributes) {

    }

    @Override
    public void registerProvider(String serviceKey, Object serviceInstance, ServiceModel serviceModel) {

    }

    @Override
    public List<ServiceModel> getAllServices() {
        return null;
    }

    @Override
    public ServiceModel lookupService(String interfaceName) {
        return null;
    }

    @Override
    public MethodModel lookupMethod(String interfaceName, String methodName) {
        return null;
    }

    @Override
    public List<ProviderModel> getExportedServices() {
        return null;
    }

    @Override
    public ProviderModel lookupExportedService(String serviceKey) {
        return null;
    }

    @Override
    public List<ConsumerModel> getReferredServices() {
        return null;
    }

    @Override
    public ConsumerModel lookupReferredService(String serviceKey) {
        return null;
    }
}
