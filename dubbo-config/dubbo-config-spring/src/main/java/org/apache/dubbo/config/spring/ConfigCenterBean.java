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
package org.apache.dubbo.config.spring;

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Starting from 2.7.0+, export and refer will only be executed when Spring is fully initialized.
 * <p>
 * Each Config bean will get refreshed on the start of the exporting and referring process.
 * <p>
 * So it's ok for this bean not to be the first Dubbo Config bean being initialized.
 * <p>
 */
public class ConfigCenterBean extends ConfigCenterConfig implements ApplicationContextAware, DisposableBean, EnvironmentAware {

    private transient ApplicationContext applicationContext;

    private Boolean includeSpringEnv = false;

    public ConfigCenterBean() {
    }

    public ConfigCenterBean(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void setEnvironment(Environment environment) {
        if (includeSpringEnv) {
            // Get PropertySource mapped to 'dubbo.properties' in Spring Environment.
            setExternalConfig(getConfigurations(getConfigFile(), environment));
            // Get PropertySource mapped to 'application.dubbo.properties' in Spring Environment.
            setAppExternalConfig(getConfigurations(StringUtils.isNotEmpty(getAppConfigFile()) ? getAppConfigFile() : ("application." + getConfigFile()), environment));
        }
    }

    private Map<String, String> getConfigurations(String key, Environment environment) {
        Object rawProperties = environment.getProperty(key, Object.class);
        Map<String, String> externalProperties = new HashMap<>();
        try {
            if (rawProperties instanceof Map) {
                externalProperties.putAll((Map<String, String>) rawProperties);
            } else if (rawProperties instanceof String) {
                externalProperties.putAll(ConfigurationUtils.parseProperties((String) rawProperties));
            }

            if (environment instanceof ConfigurableEnvironment && externalProperties.isEmpty()) {
                ConfigurableEnvironment configurableEnvironment = (ConfigurableEnvironment) environment;
                PropertySource propertySource = configurableEnvironment.getPropertySources().get(key);
                if (propertySource != null) {
                    Object source = propertySource.getSource();
                    if (source instanceof Map) {
                        ((Map<String, Object>) source).forEach((k, v) -> {
                            externalProperties.put(k, (String) v);
                        });
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return externalProperties;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public Boolean getIncludeSpringEnv() {
        return includeSpringEnv;
    }

    public void setIncludeSpringEnv(Boolean includeSpringEnv) {
        this.includeSpringEnv = includeSpringEnv;
    }

}
