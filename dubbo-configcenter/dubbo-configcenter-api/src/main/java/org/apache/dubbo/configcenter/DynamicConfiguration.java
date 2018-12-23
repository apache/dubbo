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
package org.apache.dubbo.configcenter;

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.extension.ExtensionLoader;

import java.util.Optional;

/**
 * Dynamic configuration
 */
public interface DynamicConfiguration extends Configuration {

    /**
     * {@link #addListener(String, String, ConfigurationListener)}
     * @param key      the key to represent a configuration
     * @param listener configuration listener
     */
    void addListener(String key, ConfigurationListener listener);


    /**
     * {@link #removeListener(String, String, ConfigurationListener)}
     *
     * @param key
     * @param listener
     */
    void removeListener(String key, ConfigurationListener listener);

    /**
     * Register a configuration listener for a specified key
     * The listener only works for service governance purpose, so the target group would always be the value user specifies at startup or 'dubbo' by default.
     * This method will only register listener, which means it will not trigger a notification that contains the current value.
     *
     * @param key
     * @param group
     * @param listener
     */
    void addListener(String key, String group, ConfigurationListener listener);

    /**
     * Stops one listener from listening to value changes in the specified key.
     *
     * @param key
     * @param group
     * @param listener
     */
    void removeListener(String key, String group, ConfigurationListener listener);

    /**
     * Get the configuration mapped to the given key
     *
     * @param key property key
     * @return target configuration mapped to the given key
     */
    String getConfig(String key);

    /**
     * Get the configuration mapped to the given key and the given group
     *
     * @param key   property key
     * @param group group
     * @return target configuration mapped to the given key and the given group
     */
    String getConfig(String key, String group);

    /**
     * Get the configuration mapped to the given key and the given group. If the
     * configuration fails to fetch after timeout exceeds, IllegalStateException will be thrown.
     *
     * @param key      property key
     * @param group    group
     * @param timeout  timeout value for fetching the target config
     * @return target configuration mapped to the given key and the given group, IllegalStateException will be thrown
     * if timeout exceeds.
     */
    String getConfig(String key, String group, long timeout) throws IllegalStateException;

    /**
     * I think this method is strongly related to DynamicConfiguration, so we should put it directly in the definition of this interface instead of a separated utility class.
     *
     * @return
     */
    static DynamicConfiguration getDynamicConfiguration() {
        Optional<Configuration> optional = Environment.getInstance().getDynamicConfiguration();
        return (DynamicConfiguration) optional.orElseGet(() -> ExtensionLoader.getExtensionLoader(DynamicConfigurationFactory.class)
                .getDefaultExtension()
                .getDynamicConfiguration(null));
    }
}
