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
package org.apache.dubbo.rpc.executor;

import org.apache.dubbo.common.ServiceKey;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.List;
import java.util.concurrent.Executor;

public abstract class AbstractIsolationExecutorSupport implements ExecutorSupport {
    private final URL url;
    private final ExecutorRepository executorRepository;
    private final FrameworkServiceRepository frameworkServiceRepository;

    public AbstractIsolationExecutorSupport(URL url) {
        this.url = url;
        this.executorRepository = ExecutorRepository.getInstance(url.getOrDefaultApplicationModel());
        this.frameworkServiceRepository = url.getOrDefaultFrameworkModel().getServiceRepository();
    }

    @Override
    public Executor getExecutor(Object data) {

        ProviderModel providerModel = getProviderModel(data);
        if (providerModel == null) {
            return executorRepository.getExecutor(url);
        }

        List<URL> serviceUrls = providerModel.getServiceUrls();
        if (serviceUrls == null || serviceUrls.isEmpty()) {
            return executorRepository.getExecutor(url);
        }

        for (URL serviceUrl : serviceUrls) {
            if (serviceUrl.getProtocol().equals(url.getProtocol()) && serviceUrl.getPort() == url.getPort()) {
                return executorRepository.getExecutor(providerModel, serviceUrl);
            }
        }
        return executorRepository.getExecutor(providerModel, serviceUrls.get(0));
    }

    protected ServiceKey getServiceKey(Object data) {
        return null;
    }

    protected ProviderModel getProviderModel(Object data) {
        ServiceKey serviceKey = getServiceKey(data);
        if (serviceKey == null) {
            return null;
        }
        return frameworkServiceRepository.lookupExportedService(serviceKey.toString());
    }}
