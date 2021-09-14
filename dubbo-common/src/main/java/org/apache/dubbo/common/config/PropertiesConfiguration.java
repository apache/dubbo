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

import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.Map;
import java.util.Properties;

/**
 * Configuration from system properties and dubbo.properties
 */
public class PropertiesConfiguration implements Configuration {

    private Properties properties;
    private final ScopeModel scopeModel;

    public PropertiesConfiguration(ScopeModel scopeModel) {
        this.scopeModel = scopeModel;
        refresh();
    }

    public void refresh() {
        properties = ConfigUtils.getProperties(scopeModel.getClassLoaders());
    }

    @Override
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    @Override
    public Object getInternalProperty(String key) {
        return properties.getProperty(key);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public String remove(String key) {
        return (String) properties.remove(key);
    }

    @Deprecated
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Map<String, String> getProperties() {
        return (Map) properties;
    }
}
