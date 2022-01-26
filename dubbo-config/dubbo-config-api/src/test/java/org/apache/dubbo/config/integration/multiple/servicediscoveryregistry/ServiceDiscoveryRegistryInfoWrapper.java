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

import org.apache.dubbo.config.metadata.MetadataServiceDelegation;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistry;

/**
 * The instance to wrap {@link org.apache.dubbo.registry.client.ServiceDiscoveryRegistry}
 */
public class ServiceDiscoveryRegistryInfoWrapper {

    private ServiceDiscoveryRegistry serviceDiscoveryRegistry;
    private MetadataServiceDelegation inMemoryWritableMetadataService;
    private boolean registered;
    private boolean subscribed;
    private String host;
    private int port;

    public ServiceDiscoveryRegistry getServiceDiscoveryRegistry() {
        return serviceDiscoveryRegistry;
    }

    public void setServiceDiscoveryRegistry(ServiceDiscoveryRegistry serviceDiscoveryRegistry) {
        this.serviceDiscoveryRegistry = serviceDiscoveryRegistry;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
