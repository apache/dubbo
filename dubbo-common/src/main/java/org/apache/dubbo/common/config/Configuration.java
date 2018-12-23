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

/**
 * Configuration interface, to fetch the value for the specified key.
 */
public interface Configuration {
    /**
     * Get a string associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated string.
     */
    String getString(String key);

    /**
     * Get a string associated with the given configuration key.
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * @param key          The configuration key.
     * @param defaultValue The default value.
     * @return The associated string if key is found and has valid
     * format, default value otherwise.
     */
    String getString(String key, String defaultValue);

    /**
     * Gets a property from the configuration. This is the most basic get
     * method for retrieving values of properties. In a typical implementation
     * of the {@code Configuration} interface the other get methods (that
     * return specific data types) will internally make use of this method. On
     * this level variable substitution is not yet performed. The returned
     * object is an internal representation of the property value for the passed
     * in key. It is owned by the {@code Configuration} object. So a caller
     * should not modify this object. It cannot be guaranteed that this object
     * will stay constant over time (i.e. further update operations on the
     * configuration may change its internal state).
     *
     * @param key property to retrieve
     * @return the value to which this configuration maps the specified key, or
     * null if the configuration contains no mapping for this key.
     */
    Object getProperty(String key);

    /**
     * Gets a property from the configuration. The default value will return if the configuration doesn't contain
     * the mapping for the specified key.
     *
     * @param key property to retrieve
     * @param defaultValue default value
     * @return the value to which this configuration maps the specified key, or default value if the configuration
     * contains no mapping for this key.
     */
    Object getProperty(String key, Object defaultValue);

    /**
     * Check if the configuration contains the specified key.
     *
     * @param key the key whose presence in this configuration is to be tested
     * @return {@code true} if the configuration contains a value for this
     * key, {@code false} otherwise
     */
    boolean containsKey(String key);


}
