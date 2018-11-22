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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO load as SPI will be better?
 */
public class Environment {
    private static final Environment INSTANCE = new Environment();

    private Map<String, PropertiesConfiguration> propertiesConfsHolder = new ConcurrentHashMap<>();
    private Map<String, SystemConfiguration> systemConfsHolder = new ConcurrentHashMap<>();
    private Map<String, EnvironmentConfiguration> environmentConfsHolder = new ConcurrentHashMap<>();
    private Map<String, InmemoryConfiguration> externalConfsHolder = new ConcurrentHashMap<>();
    private Map<String, InmemoryConfiguration> appExternalConfsHolder = new ConcurrentHashMap<>();
    private Map<String, InmemoryConfiguration> appConfigs = new ConcurrentHashMap<>();

    private boolean isConfigCenterFirst = true;

    private Map<String, String> externalConfigurationMap = new HashMap<>();
    private Map<String, String> appExternalConfigurationMap = new HashMap<>();

    public static Environment getInstance() {
        return INSTANCE;
    }

    public PropertiesConfiguration getPropertiesConf(String prefix, String id) {
        return propertiesConfsHolder.computeIfAbsent(toKey(prefix, id), k -> new PropertiesConfiguration(prefix, id));
    }

    public SystemConfiguration getSystemConf(String prefix, String id) {
        return systemConfsHolder.computeIfAbsent(toKey(prefix, id), k -> new SystemConfiguration(prefix, id));
    }

    public InmemoryConfiguration getExternalConfiguration(String prefix, String id) {
        return externalConfsHolder.computeIfAbsent(toKey(prefix, id), k -> {
            InmemoryConfiguration configuration = new InmemoryConfiguration(prefix, id);
            configuration.addProperties(externalConfigurationMap);
            return configuration;
        });
    }

    public InmemoryConfiguration getAppExternalConfiguration(String prefix, String id) {
        return appExternalConfsHolder.computeIfAbsent(toKey(prefix, id), k -> {
            InmemoryConfiguration configuration = new InmemoryConfiguration(prefix, id);
            configuration.addProperties(appExternalConfigurationMap);
            return configuration;
        });
    }

    public EnvironmentConfiguration getEnvironmentConf(String prefix, String id) {
        return environmentConfsHolder.computeIfAbsent(toKey(prefix, id), k -> new EnvironmentConfiguration(prefix, id));
    }

    public InmemoryConfiguration getAppConfig(String prefix, String id) {
        return appConfigs.get(toKey(prefix, id));
    }

    public synchronized void setExternalConfiguration(Map<String, String> externalConfiguration) {
        this.externalConfigurationMap = externalConfiguration;
    }

    public synchronized void setAppExternalConfiguration(Map<String, String> appExternalConfiguration) {
        this.appExternalConfigurationMap = appExternalConfiguration;
    }

    public void addAppConfig(String prefix, String id, Map<String, String> properties) {
        appConfigs.computeIfAbsent(toKey(prefix, id), k -> {
            InmemoryConfiguration configuration = new InmemoryConfiguration(prefix, id);
            configuration.addProperties(properties);
            return configuration;
        });
    }

    public void updateExternalConfigurationMap(Map<String, String> externalMap) {
        this.externalConfigurationMap.putAll(externalMap);
    }

    public void updateAppExternalConfigurationMap(Map<String, String> externalMap) {
        this.appExternalConfigurationMap.putAll(externalMap);
    }

    /**
     * Create new instance for each call, since it will be called only at startup, I think there's no big deal of the potential cost.
     * Otherwise, if use cache, we should make sure each Config has a unique id which is difficult to guarantee because is on the user's side,
     * especially when it comes to ServiceConfig and ReferenceConfig.
     *
     * @param prefix
     * @param id
     * @return
     */
    public CompositeConfiguration getStartupCompositeConf(String prefix, String id) {
        CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
        compositeConfiguration.addConfiguration(this.getSystemConf(prefix, id));
        compositeConfiguration.addConfiguration(this.getAppExternalConfiguration(prefix, id));
        compositeConfiguration.addConfiguration(this.getExternalConfiguration(prefix, id));
        compositeConfiguration.addConfiguration(this.getPropertiesConf(prefix, id));

        InmemoryConfiguration appConfig = this.getAppConfig(prefix, id);
        if (appConfig != null) {
            int index = isConfigCenterFirst ? 3 : 1;
            compositeConfiguration.addConfiguration(index, appConfig);
        }
        return compositeConfiguration;
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
