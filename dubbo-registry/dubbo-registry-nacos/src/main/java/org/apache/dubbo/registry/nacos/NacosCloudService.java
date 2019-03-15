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
package org.apache.dubbo.registry.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

import org.apache.dubbo.registry.support.cloud.CloudServiceDiscovery;
import org.apache.dubbo.registry.support.cloud.CloudServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;
import static org.apache.dubbo.common.Constants.CONSUMERS_CATEGORY;
import static org.apache.dubbo.common.Constants.PROVIDERS_CATEGORY;

/**
 * Nacos Cloud Service implements {@link CloudServiceRegistry} and {@link CloudServiceDiscovery}
 */
class NacosCloudService implements CloudServiceRegistry<NacosServiceInstance>, CloudServiceDiscovery<NacosServiceInstance> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final NamingService namingService;

    public NacosCloudService(NamingService namingService) {
        this.namingService = namingService;
    }

    @Override
    public List<String> getServices() {
        return execute(namingService ->
                namingService.getServicesOfServer(0, Integer.MAX_VALUE).getData()
        );
    }

    @Override
    public List<NacosServiceInstance> getServiceInstances(String serviceName) {
        return execute(namingService ->
                namingService.selectInstances(serviceName, true)
                        .stream()
                        .filter(Instance::isEnabled)
                        .map(NacosServiceInstance::new)
                        .collect(Collectors.toList())
        );
    }

    @Override
    public boolean supports(String serviceName) {
        return startsWithIgnoreCase(serviceName, PROVIDERS_CATEGORY) ||
                startsWithIgnoreCase(serviceName, CONSUMERS_CATEGORY);
    }

    @Override
    public void register(NacosServiceInstance serviceInstance) {
        execute(namingService -> {
            namingService.registerInstance(serviceInstance.getServiceName(), serviceInstance.getSource());
            return null;
        });
    }

    @Override
    public void deregister(NacosServiceInstance serviceInstance) {
        execute(namingService -> {
            namingService.deregisterInstance(serviceInstance.getServiceName(),
                    serviceInstance.getHost(), serviceInstance.getPort());
            return null;
        });
    }

    @Override
    public boolean isHealthy(NacosServiceInstance serviceInstance) {
        return serviceInstance.getSource().isHealthy();
    }

    @Override
    public boolean isAvailable() {
        return "UP".equals(namingService.getServerStatus());
    }

    @Override
    public void close() throws Exception {
        // DO NOTHING
    }

    private <T> T execute(NamingServiceFunction<T> function) {
        try {
            return function.apply(namingService);
        } catch (NacosException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getErrMsg(), e);
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * {@link NamingService} Function
     */
    interface NamingServiceFunction<T> {

        /**
         * Callback
         *
         * @param namingService {@link NamingService}
         * @throws NacosException
         */
        T apply(NamingService namingService) throws NacosException;

    }
}
