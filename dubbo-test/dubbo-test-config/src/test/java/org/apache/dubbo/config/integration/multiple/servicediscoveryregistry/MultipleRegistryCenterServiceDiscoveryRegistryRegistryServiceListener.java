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
package org.apache.dubbo.config.integration.multiple.servicediscoveryregistry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryServiceListener;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistry;

import static org.apache.dubbo.config.integration.Constants.MULTIPLE_CONFIG_CENTER_SERVICE_DISCOVERY_REGISTRY;


@Activate(value = MULTIPLE_CONFIG_CENTER_SERVICE_DISCOVERY_REGISTRY)
public class MultipleRegistryCenterServiceDiscoveryRegistryRegistryServiceListener implements RegistryServiceListener {

    private ServiceDiscoveryRegistryStorage storage = new ServiceDiscoveryRegistryStorage();

    /**
     * Create an {@link ServiceDiscoveryRegistryInfoWrapper} instance.
     */
    private ServiceDiscoveryRegistryInfoWrapper createServiceDiscoveryRegistryInfoWrapper(ServiceDiscoveryRegistry serviceDiscoveryRegistry){
        URL url = serviceDiscoveryRegistry.getUrl();
        String host = url.getHost();
        int port = url.getPort();
        ServiceDiscoveryRegistryInfoWrapper serviceDiscoveryRegistryInfoWrapper = new ServiceDiscoveryRegistryInfoWrapper();
        serviceDiscoveryRegistryInfoWrapper.setHost(host);
        serviceDiscoveryRegistryInfoWrapper.setPort(port);
        serviceDiscoveryRegistryInfoWrapper.setServiceDiscoveryRegistry(serviceDiscoveryRegistry);
        serviceDiscoveryRegistryInfoWrapper.setRegistered(true);
        return serviceDiscoveryRegistryInfoWrapper;
    }

    /**
     * Checks if the registry is checked application
     */
    private boolean isCheckedApplication(Registry registry){
        return registry.getUrl().getApplication()
            .equals(MultipleRegistryCenterServiceDiscoveryRegistryIntegrationTest
                .PROVIDER_APPLICATION_NAME);
    }

    public void onRegister(URL url, Registry registry) {
        if (registry instanceof ServiceDiscoveryRegistry && isCheckedApplication(registry)) {
            ServiceDiscoveryRegistry serviceDiscoveryRegistry = (ServiceDiscoveryRegistry) registry;
            String host = serviceDiscoveryRegistry.getUrl().getHost();
            int port = serviceDiscoveryRegistry.getUrl().getPort();
            if (!storage.contains(host, port)) {
                storage.put(host, port, createServiceDiscoveryRegistryInfoWrapper(serviceDiscoveryRegistry));
            }
            storage.get(host, port).setRegistered(true);
        }
    }

    public void onUnregister(URL url, Registry registry) {
        if (registry instanceof ServiceDiscoveryRegistry && isCheckedApplication(registry)) {
            String host = registry.getUrl().getHost();
            int port = registry.getUrl().getPort();
            storage.get(host, port).setRegistered(false);
        }
    }

    public void onSubscribe(URL url, Registry registry) {
        if (registry instanceof ServiceDiscoveryRegistry && isCheckedApplication(registry)) {
            ServiceDiscoveryRegistry serviceDiscoveryRegistry = (ServiceDiscoveryRegistry) registry;
            String host = serviceDiscoveryRegistry.getUrl().getHost();
            int port = serviceDiscoveryRegistry.getUrl().getPort();
            if (!storage.contains(host, port)) {
                storage.put(host, port, createServiceDiscoveryRegistryInfoWrapper(serviceDiscoveryRegistry));
            }
            storage.get(host, port).setSubscribed(true);
        }
    }

    public void onUnsubscribe(URL url, Registry registry) {
        if (registry instanceof ServiceDiscoveryRegistry && isCheckedApplication(registry)) {
            String host = registry.getUrl().getHost();
            int port = registry.getUrl().getPort();
            storage.get(host, port).setSubscribed(false);
        }
    }

    /**
     * Return the stored {@link ServiceDiscoveryRegistryInfoWrapper} instances.
     */
    public ServiceDiscoveryRegistryStorage getStorage() {
        return storage;
    }
}
