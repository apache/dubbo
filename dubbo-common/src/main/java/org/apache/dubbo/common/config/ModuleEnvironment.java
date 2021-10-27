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
package org.apache.dubbo.common.config;

import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.context.ModuleExt;
import org.apache.dubbo.common.extension.DisableInject;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ModuleEnvironment extends Environment implements ModuleExt {

    // delegate

    private static final Logger logger = LoggerFactory.getLogger(ModuleEnvironment.class);

    public static final String NAME = "moduleEnvironment";

    private final ModuleModel moduleModel;

    private Environment applicationDelegate;

    private OrderedPropertiesConfiguration orderedPropertiesConfiguration;

    private CompositeConfiguration dynamicGlobalConfiguration;

    private DynamicConfiguration dynamicConfiguration;

    public ModuleEnvironment(ModuleModel moduleModel) {
        super(moduleModel);
        this.moduleModel = moduleModel;
        this.applicationDelegate = moduleModel.getApplicationModel().getModelEnvironment();
    }

    @Override
    public void initialize() throws IllegalStateException {
        this.orderedPropertiesConfiguration = new OrderedPropertiesConfiguration(moduleModel);
    }

    @Override
    public Configuration getPrefixedConfiguration(AbstractConfig config, String prefix) {
        CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
        compositeConfiguration.addConfiguration(applicationDelegate.getPrefixedConfiguration(config, prefix));
        compositeConfiguration.addConfiguration(orderedPropertiesConfiguration);

        return new PrefixedConfiguration(compositeConfiguration, prefix);
    }

    @Override
    public CompositeConfiguration getConfiguration() {
        if (globalConfiguration == null) {
            CompositeConfiguration configuration = new CompositeConfiguration();
            configuration.addConfiguration(applicationDelegate.getConfiguration());
            configuration.addConfiguration(orderedPropertiesConfiguration);
            globalConfiguration = configuration;
        }
        return globalConfiguration;
    }

    @Override
    public List<Map<String, String>> getConfigurationMaps(AbstractConfig config, String prefix) {
        List<Map<String, String>> maps = applicationDelegate.getConfigurationMaps(config, prefix);
        maps.add(orderedPropertiesConfiguration.getProperties());
        return maps;
    }

    @Override
    public Configuration getDynamicGlobalConfiguration() {
        if (dynamicConfiguration == null) {
            return applicationDelegate.getDynamicGlobalConfiguration();
        }
        if (dynamicGlobalConfiguration == null) {
            if (dynamicConfiguration == null) {
                if (logger.isWarnEnabled()) {
                    logger.warn("dynamicConfiguration is null , return globalConfiguration.");
                }
                return getConfiguration();
            }
            dynamicGlobalConfiguration = new CompositeConfiguration();
            dynamicGlobalConfiguration.addConfiguration(dynamicConfiguration);
            dynamicGlobalConfiguration.addConfiguration(getConfiguration());
        }
        return dynamicGlobalConfiguration;
    }

    @Override
    public Optional<DynamicConfiguration> getDynamicConfiguration() {
        if (dynamicConfiguration == null) {
            return applicationDelegate.getDynamicConfiguration();
        }
        return Optional.ofNullable(dynamicConfiguration);
    }

    @Override
    @DisableInject
    public void setDynamicConfiguration(DynamicConfiguration dynamicConfiguration) {
        this.dynamicConfiguration = dynamicConfiguration;
    }

    @Override
    public void destroy() throws IllegalStateException {
        super.destroy();
        this.orderedPropertiesConfiguration = null;
        this.globalConfiguration = null;
        this.dynamicGlobalConfiguration = null;
        this.dynamicConfiguration = null;
    }

    @Override
    public void setLocalMigrationRule(String localMigrationRule) {
        applicationDelegate.setLocalMigrationRule(localMigrationRule);
    }

    @Override
    public void setExternalConfigMap(Map<String, String> externalConfiguration) {
        applicationDelegate.setExternalConfigMap(externalConfiguration);
    }

    @Override
    public void setAppExternalConfigMap(Map<String, String> appExternalConfiguration) {
        applicationDelegate.setAppExternalConfigMap(appExternalConfiguration);
    }

    @Override
    public void setAppConfigMap(Map<String, String> appConfiguration) {
        applicationDelegate.setAppConfigMap(appConfiguration);
    }

    @Override
    public Map<String, String> getExternalConfigMap() {
        return applicationDelegate.getExternalConfigMap();
    }

    @Override
    public Map<String, String> getAppExternalConfigMap() {
        return applicationDelegate.getAppExternalConfigMap();
    }

    @Override
    public Map<String, String> getAppConfigMap() {
        return applicationDelegate.getAppConfigMap();
    }

    @Override
    public void updateExternalConfigMap(Map<String, String> externalMap) {
        applicationDelegate.updateExternalConfigMap(externalMap);
    }

    @Override
    public void updateAppExternalConfigMap(Map<String, String> externalMap) {
        applicationDelegate.updateAppExternalConfigMap(externalMap);
    }

    @Override
    public void updateAppConfigMap(Map<String, String> map) {
        applicationDelegate.updateAppConfigMap(map);
    }

    @Override
    public PropertiesConfiguration getPropertiesConfiguration() {
        return applicationDelegate.getPropertiesConfiguration();
    }

    @Override
    public SystemConfiguration getSystemConfiguration() {
        return applicationDelegate.getSystemConfiguration();
    }

    @Override
    public EnvironmentConfiguration getEnvironmentConfiguration() {
        return applicationDelegate.getEnvironmentConfiguration();
    }

    @Override
    public InmemoryConfiguration getExternalConfiguration() {
        return applicationDelegate.getExternalConfiguration();
    }

    @Override
    public InmemoryConfiguration getAppExternalConfiguration() {
        return applicationDelegate.getAppExternalConfiguration();
    }

    @Override
    public InmemoryConfiguration getAppConfiguration() {
        return applicationDelegate.getAppConfiguration();
    }

    @Override
    public String getLocalMigrationRule() {
        return applicationDelegate.getLocalMigrationRule();
    }

    @Override
    public void refreshClassLoaders() {
        orderedPropertiesConfiguration.refresh();
        applicationDelegate.refreshClassLoaders();
        this.globalConfiguration = null;
        this.globalConfigurationMaps = null;
        this.dynamicGlobalConfiguration = null;
    }
}
