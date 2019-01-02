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
package org.apache.dubbo.config;

import org.apache.dubbo.common.utils.StringUtils;

import java.util.Map;
import java.util.Optional;

import static org.apache.dubbo.common.Constants.DEFAULT_KEY;

/**
 * TODO
 * Experimental API, should only being used internally at present, will consider open to end user in the following version.
 */
public class DubboBuilder {
    private ApplicationConfig application;
    private MonitorConfig monitor;
    private ModuleConfig module;
    private ConfigCenterConfig configCenter;

    private Map<String, ProtocolConfig> protocols;
    private Map<String, RegistryConfig> registries;
    private Map<String, ProviderConfig> providers;
    private Map<String, ConsumerConfig> consumers;

    public ApplicationConfig getApplication() {
        return application;
    }

    public void setApplication(ApplicationConfig application) {
        this.application = application;
    }

    public MonitorConfig getMonitor() {
        return monitor;
    }

    public void setMonitor(MonitorConfig monitor) {
        this.monitor = monitor;
    }

    public ModuleConfig getModule() {
        return module;
    }

    public void setModule(ModuleConfig module) {
        this.module = module;
    }

    public ConfigCenterConfig getConfigCenter() {
        return configCenter;
    }

    public void setConfigCenter(ConfigCenterConfig configCenter) {
        this.configCenter = configCenter;
    }

    public Optional<ProviderConfig> getProvider(String id) {
        return Optional.ofNullable(providers.get(id));
    }

    public Optional<ProviderConfig> getDefaultProvider() {
        return Optional.ofNullable(providers.get(DEFAULT_KEY));
    }

    public void addProviderConfig(ProviderConfig providerConfig) {
        if (providerConfig == null) {
            return;
        }
        if (providerConfig.isDefault() == null || providerConfig.isDefault()) {
            if (providers.containsKey(DEFAULT_KEY)) {
                throw new IllegalStateException("Duplicate ProviderConfig found, there already has one default ProviderConfig, " + providerConfig);
            } else {
                providers.put(DEFAULT_KEY, providerConfig);
            }
        } else {
            if (StringUtils.isNotEmpty(providerConfig.getId())) {
                if (providers.containsKey(providerConfig.getId())) {
                    throw new IllegalStateException("Duplicate ProviderConfig found, there already has one ProviderConfig with the same id, " + providerConfig);
                } else {
                    providers.put(providerConfig.getId(), providerConfig);
                }
            } else {
                throw new IllegalStateException("A ProviderConfig should either has an id or it's the default one, " + providerConfig);
            }
        }
    }

    public Optional<ConsumerConfig> getConsumer(String id) {
        return Optional.ofNullable(consumers.get(id));
    }

    public Optional<ConsumerConfig> getDefaultConsumer() {
        return Optional.ofNullable(consumers.get(DEFAULT_KEY));
    }

    public void addConsumer(ConsumerConfig consumerConfig) {
        if (consumerConfig == null) {
            return;
        }
        if (consumerConfig.isDefault() == null || consumerConfig.isDefault()) {
            if (consumers.containsKey(DEFAULT_KEY)) {
                throw new IllegalStateException("Duplicate ConsumerConfig found, there already has one default ConsumerConfig, " + consumerConfig);
            } else {
                consumers.put(DEFAULT_KEY, consumerConfig);
            }
        } else {
            if (StringUtils.isNotEmpty(consumerConfig.getId())) {
                if (consumers.containsKey(consumerConfig.getId())) {
                    throw new IllegalStateException("Duplicate ConsumerConfig found, there already has one ConsumerConfig with the same id, " + consumerConfig);
                } else {
                    consumers.put(consumerConfig.getId(), consumerConfig);
                }
            } else {
                throw new IllegalStateException("A ConsumerConfig should either has an id or it's the default one, " + consumerConfig);
            }
        }
    }

    public Optional<ProtocolConfig> getProtocol(String id) {
        return Optional.ofNullable(protocols.get(id));
    }

    public Optional<ProtocolConfig> getDefaultProtocol() {
        return Optional.ofNullable(protocols.get(DEFAULT_KEY));
    }

    public void addProtocol(ProtocolConfig protocolConfig) {
        if (protocolConfig == null) {
            return;
        }
        if (protocolConfig.isDefault() == null || protocolConfig.isDefault()) {
            if (protocols.containsKey(DEFAULT_KEY)) {
                throw new IllegalStateException("Duplicate ProtocolConfig found, there already has one default ProtocolConfig, " + protocolConfig);
            } else {
                protocols.put(DEFAULT_KEY, protocolConfig);
            }
        } else {
            if (StringUtils.isNotEmpty(protocolConfig.getId())) {
                if (protocols.containsKey(protocolConfig.getId())) {
                    throw new IllegalStateException("Duplicate ProtocolConfig found, there already has one ProtocolConfig with the same id, " + protocolConfig);
                } else {
                    protocols.put(protocolConfig.getId(), protocolConfig);
                }
            } else {
                throw new IllegalStateException("A ProtocolConfig should either has an id or it's the default one, " + protocolConfig);
            }
        }
    }

    public Optional<RegistryConfig> getRegistry(String id) {
        return Optional.ofNullable(registries.get(id));
    }

    public Optional<RegistryConfig> getDefaultRegistry() {
        return Optional.ofNullable(registries.get(DEFAULT_KEY));
    }

    public void addRegistry(RegistryConfig registryConfig) {
        if (registryConfig == null) {
            return;
        }
        if (registryConfig.isDefault() == null || registryConfig.isDefault()) {
            if (registries.containsKey(DEFAULT_KEY)) {
                throw new IllegalStateException("Duplicate RegistryConfig found, there already has one default RegistryConfig, " + registryConfig);
            } else {
                registries.put(DEFAULT_KEY, registryConfig);
            }
        } else {
            if (StringUtils.isNotEmpty(registryConfig.getId())) {
                if (registries.containsKey(registryConfig.getId())) {
                    throw new IllegalStateException("Duplicate RegistryConfig found, there already has one RegistryConfig with the same id, " + registryConfig);
                } else {
                    registries.put(registryConfig.getId(), registryConfig);
                }
            } else {
                throw new IllegalStateException("A RegistryConfig should either has an id or it's the default one, " + registryConfig);
            }
        }
    }

    public Map<String, ProtocolConfig> getProtocols() {
        return protocols;
    }

    public Map<String, RegistryConfig> getRegistries() {
        return registries;
    }

    public Map<String, ProviderConfig> getProviders() {
        return providers;
    }

    public Map<String, ConsumerConfig> getConsumers() {
        return consumers;
    }
}
