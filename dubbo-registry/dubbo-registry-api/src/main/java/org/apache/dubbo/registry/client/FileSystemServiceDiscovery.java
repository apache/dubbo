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
import org.apache.dubbo.common.config.configcenter.DynamicConfigurationFactory;
import org.apache.dubbo.common.config.configcenter.file.FileSystemDynamicConfiguration;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;

import java.util.Set;

import static com.alibaba.fastjson.JSON.toJSONString;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * File System {@link ServiceDiscovery} implementation
 *
 * @see FileSystemDynamicConfiguration
 * @since 2.7.4
 */
public class FileSystemServiceDiscovery implements ServiceDiscovery, EventListener<ServiceInstancesChangedEvent> {

    private FileSystemDynamicConfiguration dynamicConfiguration;

    @Override
    public void onEvent(ServiceInstancesChangedEvent event) {

    }

    @Override
    public void initialize(URL registryURL) throws Exception {
        dynamicConfiguration = createDynamicConfiguration(registryURL);
    }

    @Override
    public void destroy() throws Exception {
        dynamicConfiguration.close();
    }

    private String getConfigKey(ServiceInstance serviceInstance) {
        return serviceInstance.getId();
    }

    private String getConfigGroup(ServiceInstance serviceInstance) {
        return serviceInstance.getServiceName();
    }

    @Override
    public void register(ServiceInstance serviceInstance) throws RuntimeException {
        String key = getConfigKey(serviceInstance);
        String group = getConfigGroup(serviceInstance);
        String content = toJSONString(serviceInstance);
        dynamicConfiguration.publishConfig(key, group, content);
    }

    @Override
    public void update(ServiceInstance serviceInstance) throws RuntimeException {
        register(serviceInstance);
    }

    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {
        String key = getConfigKey(serviceInstance);
        String group = getConfigGroup(serviceInstance);
        dynamicConfiguration.removeConfig(key, group);
    }

    @Override
    public Set<String> getServices() {
        return null;
    }

    @Override
    public void addServiceInstancesChangedListener(String serviceName, ServiceInstancesChangedListener listener) throws
            NullPointerException, IllegalArgumentException {

    }

    private static FileSystemDynamicConfiguration createDynamicConfiguration(URL connectionURL) {
        String protocol = connectionURL.getProtocol();
        DynamicConfigurationFactory factory = getExtensionLoader(DynamicConfigurationFactory.class).getExtension(protocol);
        return (FileSystemDynamicConfiguration) factory.getDynamicConfiguration(connectionURL);
    }
}
