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
package org.apache.dubbo.config.context;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.CompositeConfiguration;
import org.apache.dubbo.common.config.EnvironmentConfiguration;
import org.apache.dubbo.common.config.InmemoryConfiguration;
import org.apache.dubbo.common.config.PropertiesConfiguration;
import org.apache.dubbo.common.config.SystemConfiguration;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.governance.DynamicConfiguration;
import org.apache.dubbo.governance.DynamicConfigurationFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO load as SPI will be better?
 */
public class Environment {
    private static final Environment INSTANCE = new Environment();

    private volatile Map<String, PropertiesConfiguration> propertiesConfsHolder = new ConcurrentHashMap<>();
    private volatile Map<String, SystemConfiguration> systemConfsHolder = new ConcurrentHashMap<>();
    private volatile Map<String, EnvironmentConfiguration> environmentConfsHolder = new ConcurrentHashMap<>();
    private volatile Map<String, CompositeConfiguration> startupCompositeConfsHolder = new ConcurrentHashMap<>();
    private volatile Map<String, CompositeConfiguration> runtimeCompositeConfsHolder = new ConcurrentHashMap<>();

    private volatile InmemoryConfiguration externalConfiguration = new InmemoryConfiguration();

    private volatile boolean isConfigCenterFirst;

    public static Environment getInstance() {
        return INSTANCE;
    }

    public PropertiesConfiguration getPropertiesConf(String prefix, String id) {
        return propertiesConfsHolder.computeIfAbsent(toKey(prefix, id), k -> new PropertiesConfiguration(prefix, id));
    }

    public SystemConfiguration getSystemConf(String prefix, String id) {
        return systemConfsHolder.computeIfAbsent(toKey(prefix, id), k -> new SystemConfiguration(prefix, id));
    }

    public EnvironmentConfiguration getEnvironmentConf(String prefix, String id) {
        return environmentConfsHolder.computeIfAbsent(toKey(prefix, id), k -> new EnvironmentConfiguration(prefix, id));
    }

    public void updateExternalConfiguration(Map<String, String> externalMap) {
        this.externalConfiguration.addProperties(externalMap);
    }

    public CompositeConfiguration getStartupCompositeConf(String prefix, String id) {
        return startupCompositeConfsHolder.computeIfAbsent(toKey(prefix, id), k -> {
            CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
            compositeConfiguration.addConfiguration(this.getSystemConf(prefix, id));
            compositeConfiguration.addConfiguration(this.externalConfiguration);
            compositeConfiguration.addConfiguration(this.getPropertiesConf(prefix, id));
            return compositeConfiguration;
        });
    }

    public CompositeConfiguration getRuntimeCompositeConf(URL url) {
        return runtimeCompositeConfsHolder.computeIfAbsent(url.toIdentityString(), k -> {
            CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
            compositeConfiguration.addConfiguration(getDynamicConfiguration());
            compositeConfiguration.addConfiguration(this.getSystemConf(null, null));
            compositeConfiguration.addConfiguration(url.toConfiguration());
            compositeConfiguration.addConfiguration(this.getPropertiesConf(null, null));
            return compositeConfiguration;
        });
    }

    /**
     * If user opens DynamicConfig, the extension instance must has been created during the initialization of ConfigCenterConfig with the right extension type user specified.
     * If no DynamicConfig presents, NopDynamicConfiguration will be used.
     *
     * @return
     */
    public DynamicConfiguration getDynamicConfiguration() {
        ExtensionLoader<DynamicConfigurationFactory> factoryLoader = ExtensionLoader.getExtensionLoader(DynamicConfigurationFactory.class);
        Set<Object> factories = factoryLoader.getLoadedExtensionInstances();
        if (CollectionUtils.isEmpty(factories)) {
            return factoryLoader.getDefaultExtension().getDynamicConfiguration(null);
        }

        return ((DynamicConfigurationFactory) factories.iterator().next()).getExistedDynamicConfiguration();
    }

    private static String toKey(String keypart1, String keypart2) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(keypart1)) {
            sb.append(keypart1);
        }
        if (StringUtils.isNotEmpty(keypart2)) {
            sb.append(keypart2);
        }

        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '.') {
            sb.append(".");
        }

        if (sb.length() > 0) {
            return sb.toString();
        }
        return Constants.DUBBO;
    }

    public boolean isConfigCenterFirst() {
        return isConfigCenterFirst;
    }

    public void setConfigCenterFirst(boolean configCenterFirst) {
        isConfigCenterFirst = configCenterFirst;
    }
}
