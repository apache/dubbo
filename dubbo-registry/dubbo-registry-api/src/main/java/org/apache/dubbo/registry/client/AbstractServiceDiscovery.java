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

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.isInstanceUpdated;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.resetInstanceUpdateKey;

public abstract class AbstractServiceDiscovery implements ServiceDiscovery {

    protected ServiceInstance serviceInstance;

    @Override
    public ServiceInstance getLocalInstance() {
        return serviceInstance;
    }

    /**
     * 服务注册
     * @param serviceInstance an instance of {@link ServiceInstance} to be registered
     * @throws RuntimeException
     */
    @Override
    public final void register(ServiceInstance serviceInstance) throws RuntimeException {
        /**
         * 服务注册
         */
        doRegister(serviceInstance);
        this.serviceInstance = serviceInstance;
    }

    /**
     * It should be implement in kinds of service discovers.
     */
    public abstract void doRegister(ServiceInstance serviceInstance);

    /**
     *
     * @param serviceInstance the registered {@link ServiceInstance}
     * @throws RuntimeException
     */
    @Override
    public final void update(ServiceInstance serviceInstance) throws RuntimeException {
        /**
         * 比对是否需要更新   即revision是否有变化
         */
        if (!isInstanceUpdated(serviceInstance)) {
            return;
        }
        /**
         * 更新注册服务信息
         */
        doUpdate(serviceInstance);
        /**
         *
         */
        resetInstanceUpdateKey(serviceInstance);
        this.serviceInstance = serviceInstance;
    }

    /**
     * It should be implement in kinds of service discovers.
     */
    public abstract void doUpdate(ServiceInstance serviceInstance);
}
