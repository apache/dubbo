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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.isInstanceUpdated;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.resetInstanceUpdateKey;

public abstract class AbstractServiceDiscovery implements ServiceDiscovery {

    private volatile boolean isDestroy;

    protected volatile ServiceInstance serviceInstance;

    @Override
    public final ServiceInstance getLocalInstance() {
        return serviceInstance;
    }

    @Override
    public final void initialize(URL registryURL) throws Exception {
        doInitialize(registryURL);
    }

    public abstract void doInitialize(URL registryURL) throws Exception;

    @Override
    public final void register(ServiceInstance serviceInstance) throws RuntimeException {
        if (ServiceInstanceMetadataUtils.getExportedServicesRevision(serviceInstance) == null) {
            ServiceInstanceMetadataUtils.calInstanceRevision(this, serviceInstance);
        }
        doRegister(serviceInstance);
        this.serviceInstance = serviceInstance;
    }

    public abstract void doRegister(ServiceInstance serviceInstance) throws RuntimeException;


    @Override
    public final void update(ServiceInstance serviceInstance) throws RuntimeException {
        if (this.serviceInstance == null) {
            this.register(serviceInstance);
            return;
        }
        if (!isInstanceUpdated(serviceInstance)) {
            return;
        }
        doUpdate(serviceInstance);
        resetInstanceUpdateKey(serviceInstance);
        this.serviceInstance = serviceInstance;
    }

    public abstract void doUpdate(ServiceInstance serviceInstance) throws RuntimeException;

    @Override
    public final void unregister(ServiceInstance serviceInstance) throws RuntimeException {
        doUnregister(serviceInstance);
    }

    public abstract void doUnregister(ServiceInstance serviceInstance);

    @Override
    public final void destroy() throws Exception {
        isDestroy = true;
        doDestroy();
    }

    public abstract void doDestroy() throws Exception;

    @Override
    public final boolean isDestroy() {
        return isDestroy;
    }
}
