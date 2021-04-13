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
package org.apache.dubbo.config.bootstrap.builders;

import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.RegistryConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a builder for build {@link ModuleConfig}.
 *
 * @since 2.7
 */
public class ModuleBuilder extends AbstractBuilder<ModuleConfig, ModuleBuilder> {
    /**
     * Module name
     */
    private String name;

    /**
     * Module version
     */
    private String version;

    /**
     * Module owner
     */
    private String owner;

    /**
     * Module's organization
     */
    private String organization;

    /**
     * Registry centers
     */
    private List<RegistryConfig> registries;

    /**
     * Monitor center
     */
    private MonitorConfig monitor;

    /**
     * If it's default
     */
    private Boolean isDefault;

    public ModuleBuilder name(String name) {
        this.name = name;
        return getThis();
    }

    public ModuleBuilder version(String version) {
        this.version = version;
        return getThis();
    }

    public ModuleBuilder owner(String owner) {
        this.owner = owner;
        return getThis();
    }

    public ModuleBuilder organization(String organization) {
        this.organization = organization;
        return getThis();
    }

    public ModuleBuilder addRegistries(List<? extends RegistryConfig> registries) {
        if (this.registries == null) {
            this.registries = new ArrayList<>();
        }
        this.registries.addAll(registries);
        return getThis();
    }

    public ModuleBuilder addRegistry(RegistryConfig registry) {
        if (this.registries == null) {
            this.registries = new ArrayList<>();
        }
        this.registries.add(registry);
        return getThis();
    }

    public ModuleBuilder monitor(MonitorConfig monitor) {
        this.monitor = monitor;
        return getThis();
    }

    public ModuleBuilder isDefault(Boolean isDefault) {
        this.isDefault = isDefault;
        return getThis();
    }

    public ModuleConfig build() {
        ModuleConfig moduleConfig = new ModuleConfig();
        super.build(moduleConfig);

        moduleConfig.setDefault(isDefault);
        moduleConfig.setMonitor(monitor);
        moduleConfig.setName(name);
        moduleConfig.setOrganization(organization);
        moduleConfig.setOwner(owner);
        moduleConfig.setRegistries(registries);
        moduleConfig.setVersion(version);

        return moduleConfig;
    }

    @Override
    protected ModuleBuilder getThis() {
        return this;
    }
}
