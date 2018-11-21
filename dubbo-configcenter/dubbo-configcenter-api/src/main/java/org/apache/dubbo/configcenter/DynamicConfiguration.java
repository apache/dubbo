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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.extension.SPI;

/**
 * Dynamic configuration
 */
@SPI("nop")
public interface DynamicConfiguration extends Configuration {

    /**
     * Init dynamic configuration from URL
     *
     * @param url the url in which info for initializing dynamic configuration is contained.
     */
    void initWith(URL url);

    /**
     * Register a configuration listener for a specified key
     *
     * @param key      the key to represent a configuration
     * @param listener configuration listener
     */
    void addListener(String key, ConfigurationListener listener);

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
     * Get the configuration mapped to the given key, and notify the passed-in listener
     *
     * @param key      property key
     * @param listener configuration listener
     * @return
     */
    String getConfig(String key, ConfigurationListener listener);

    /**
     * Get the configuration mapped to the given key and the given group, and notify the passed-in listener
     *
     * @param key      property key
     * @param group    group
     * @param listener configuration listener
     * @return target configuration mapped to the given key and the given group
     */
    String getConfig(String key, String group, ConfigurationListener listener);

    /**
     * Get the configuration mapped to the given key and the given group, and notify the passed-in listener. If the
     * configuration fails to fetch after timeout exceeds, IllegalStateException will be thrown.
     *
     * @param key      property key
     * @param group    group
     * @param listener configuration listener
     * @param timeout  timeout value for fetching the target config
     * @return target configuration mapped to the given key and the given group, IllegalStateException will be thrown
     * if timeout exceeds.
     */
    String getConfig(String key, String group, ConfigurationListener listener, long timeout);
}
