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
package org.apache.dubbo.common.config.configcenter.wrapper;

import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * support multiple config center, simply iterating each concrete config center.
 */
public class CompositeDynamicConfiguration implements DynamicConfiguration {

    public static final String NAME = "COMPOSITE";

    private Set<DynamicConfiguration> configurations = new HashSet<>();

    public void addConfiguration(DynamicConfiguration configuration) {
        if (configuration != null) {
            this.configurations.add(configuration);
        }
    }

    @Override
    public void addListener(String key, String group, ConfigurationListener listener) {
        iterateListenerOperation(configuration -> configuration.addListener(key, group, listener));
    }

    @Override
    public void removeListener(String key, String group, ConfigurationListener listener) {
        iterateListenerOperation(configuration -> configuration.removeListener(key, group, listener));
    }

    @Override
    public String getConfig(String key, String group, long timeout) throws IllegalStateException {
        return (String) iterateConfigOperation(configuration -> configuration.getConfig(key, group, timeout));
    }

    @Override
    public String getProperties(String key, String group, long timeout) throws IllegalStateException {
        return (String) iterateConfigOperation(configuration -> configuration.getProperties(key, group, timeout));
    }

    @Override
    public Object getInternalProperty(String key) {
        return iterateConfigOperation(configuration -> configuration.getInternalProperty(key));
    }

    @Override
    public boolean publishConfig(String key, String group, String content) throws UnsupportedOperationException {
        boolean publishedAll = true;
        for (DynamicConfiguration configuration : configurations) {
            if (!configuration.publishConfig(key, group, content)) {
                publishedAll = false;
            }
        }
        return publishedAll;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SortedSet<String> getConfigKeys(String group) throws UnsupportedOperationException {
        return (SortedSet<String>) iterateConfigOperation(configuration -> configuration.getConfigKeys(group));
    }

    private void iterateListenerOperation(Consumer<DynamicConfiguration> consumer) {
        for (DynamicConfiguration configuration : configurations) {
            consumer.accept(configuration);
        }
    }

    private Object iterateConfigOperation(Function<DynamicConfiguration, Object> func) {
        Object value = null;
        for (DynamicConfiguration configuration : configurations) {
            value = func.apply(configuration);
            if (value != null) {
                break;
            }
        }
        return value;
    }
}
