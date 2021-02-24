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
package org.apache.dubbo.common.config.configcenter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import static org.apache.dubbo.common.config.configcenter.DynamicConfigurationFactory.getDynamicConfigurationFactory;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * Dynamic Configuration
 * <br/>
 * From the use scenario internally inside framework, there're mainly three kinds of methods:
 * <ol>
 * <li>{@link #getProperties(String, String, long)}, get configuration file from Config Center at start up.</li>
 * <li>{@link #addListener(String, String, ConfigurationListener)}/ {@link #removeListener(String, String, ConfigurationListener)}
 * , add or remove listeners for governance rules or config items that need to watch.</li>
 * <li>{@link #getProperty(String, Object)}, get a single config item.</li>
 * <li>{@link #getConfig(String, String, long)}, get the specified config</li>
 * </ol>
 *
 * @see AbstractDynamicConfiguration
 */
public interface DynamicConfiguration extends Configuration, AutoCloseable {

    String DEFAULT_GROUP = "dubbo";

    /**
     * {@link #addListener(String, String, ConfigurationListener)}
     *
     * @param key      the key to represent a configuration
     * @param listener configuration listener
     */
    default void addListener(String key, ConfigurationListener listener) {
        addListener(key, getDefaultGroup(), listener);
    }


    /**
     * {@link #removeListener(String, String, ConfigurationListener)}
     *
     * @param key      the key to represent a configuration
     * @param listener configuration listener
     */
    default void removeListener(String key, ConfigurationListener listener) {
        removeListener(key, getDefaultGroup(), listener);
    }

    /**
     * Register a configuration listener for a specified key
     * The listener only works for service governance purpose, so the target group would always be the value user
     * specifies at startup or 'dubbo' by default. This method will only register listener, which means it will not
     * trigger a notification that contains the current value.
     *
     * @param key      the key to represent a configuration
     * @param group    the group where the key belongs to
     * @param listener configuration listener
     */
    void addListener(String key, String group, ConfigurationListener listener);

    /**
     * Stops one listener from listening to value changes in the specified key.
     *
     * @param key      the key to represent a configuration
     * @param group    the group where the key belongs to
     * @param listener configuration listener
     */
    void removeListener(String key, String group, ConfigurationListener listener);

    /**
     * Get the configuration mapped to the given key and the given group with {@link #getDefaultTimeout() the default
     * timeout}
     *
     * @param key   the key to represent a configuration
     * @param group the group where the key belongs to
     * @return target configuration mapped to the given key and the given group
     */
    default String getConfig(String key, String group) {
        return getConfig(key, group, getDefaultTimeout());
    }

    /**
     * Get the configuration mapped to the given key and the given group. If the
     * configuration fails to fetch after timeout exceeds, IllegalStateException will be thrown.
     *
     * @param key     the key to represent a configuration
     * @param group   the group where the key belongs to
     * @param timeout timeout value for fetching the target config
     * @return target configuration mapped to the given key and the given group, IllegalStateException will be thrown
     * if timeout exceeds.
     */
    String getConfig(String key, String group, long timeout) throws IllegalStateException;

    /**
     * This method are mostly used to get a compound config file with {@link #getDefaultTimeout() the default timeout},
     * such as a complete dubbo.properties file.
     */
    default String getProperties(String key, String group) throws IllegalStateException {
        return getProperties(key, group, getDefaultTimeout());
    }

    /**
     * This method are mostly used to get a compound config file, such as a complete dubbo.properties file.
     *
     * @revision 2.7.4
     */
    default String getProperties(String key, String group, long timeout) throws IllegalStateException {
        return getConfig(key, group, timeout);
    }

    /**
     * Publish Config mapped to the given key under the {@link #getDefaultGroup() default group}
     *
     * @param key     the key to represent a configuration
     * @param content the content of configuration
     * @return <code>true</code> if success, or <code>false</code>
     * @throws UnsupportedOperationException If the under layer does not support
     * @since 2.7.5
     */
    default boolean publishConfig(String key, String content) throws UnsupportedOperationException {
        return publishConfig(key, getDefaultGroup(), content);
    }

    /**
     * Publish Config mapped to the given key and the given group.
     *
     * @param key     the key to represent a configuration
     * @param group   the group where the key belongs to
     * @param content the content of configuration
     * @return <code>true</code> if success, or <code>false</code>
     * @throws UnsupportedOperationException If the under layer does not support
     * @since 2.7.5
     */
    default boolean publishConfig(String key, String group, String content) throws UnsupportedOperationException {
        return false;
    }

    /**
     * Get the config keys by the specified group
     *
     * @param group the specified group
     * @return the read-only non-null sorted {@link Set set} of config keys
     * @throws UnsupportedOperationException If the under layer does not support
     * @since 2.7.5
     */
    default SortedSet<String> getConfigKeys(String group) throws UnsupportedOperationException {
        return Collections.emptySortedSet();
    }

    /**
     * Get the default group for the operations
     *
     * @return The default value is {@link #DEFAULT_GROUP "dubbo"}
     * @since 2.7.5
     */
    default String getDefaultGroup() {
        return DEFAULT_GROUP;
    }

    /**
     * Get the default timeout for the operations in milliseconds
     *
     * @return The default value is <code>-1L</code>
     * @since 2.7.5
     */
    default long getDefaultTimeout() {
        return -1L;
    }

    /**
     * Close the configuration
     *
     * @throws Exception
     * @since 2.7.5
     */
    @Override
    default void close() throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Find DynamicConfiguration instance
     *
     * @return DynamicConfiguration instance
     */
    static DynamicConfiguration getDynamicConfiguration() {
        Optional<DynamicConfiguration> optional = ApplicationModel.getEnvironment().getDynamicConfiguration();
        return optional.orElseGet(() -> getExtensionLoader(DynamicConfigurationFactory.class)
                .getDefaultExtension()
                .getDynamicConfiguration(null));
    }

    /**
     * Get the instance of {@link DynamicConfiguration} by the specified connection {@link URL}
     *
     * @param connectionURL
     * @return non-null
     * @since 2.7.5
     */
    static DynamicConfiguration getDynamicConfiguration(URL connectionURL) {
        String protocol = connectionURL.getProtocol();
        DynamicConfigurationFactory factory = getDynamicConfigurationFactory(protocol);
        return factory.getDynamicConfiguration(connectionURL);
    }

    /**
     * The format is '{interfaceName}:[version]:[group]'
     *
     * @return
     */
    static String getRuleKey(URL url) {
        return url.getColonSeparatedKey();
    }

    /**
     * @param key   the key to represent a configuration
     * @param group the group where the key belongs to
     * @return <code>true</code> if success, or <code>false</code>
     * @since 2.7.8
     */
    default boolean removeConfig(String key, String group) {
        return true;
    }
}
