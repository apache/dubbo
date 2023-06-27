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

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class OrderedPropertiesConfiguration implements Configuration {
    private Properties properties;
    private final ModuleModel moduleModel;

    public OrderedPropertiesConfiguration(ModuleModel moduleModel) {
        this.moduleModel = moduleModel;
        refresh();
    }

    public void refresh() {
        properties = new Properties();
        ExtensionLoader<OrderedPropertiesProvider> propertiesProviderExtensionLoader = moduleModel.getExtensionLoader(OrderedPropertiesProvider.class);
        Set<String> propertiesProviderNames = propertiesProviderExtensionLoader.getSupportedExtensions();
        if (CollectionUtils.isEmpty(propertiesProviderNames)) {
            return;
        }
        List<OrderedPropertiesProvider> orderedPropertiesProviders = new ArrayList<>();
        for (String propertiesProviderName : propertiesProviderNames) {
            orderedPropertiesProviders.add(propertiesProviderExtensionLoader.getExtension(propertiesProviderName));
        }

        //order the propertiesProvider according the priority descending
        orderedPropertiesProviders.sort((a, b) -> b.priority() - a.priority());


        //override the properties.
        for (OrderedPropertiesProvider orderedPropertiesProvider : orderedPropertiesProviders) {
            properties.putAll(orderedPropertiesProvider.initProperties());
        }

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

    /**
     * For ut only
     */
    @Deprecated
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Map<String, String> getProperties() {
        return (Map) properties;
    }
}
