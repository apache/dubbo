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
package org.apache.dubbo.config.spring.util;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * {@link PropertySources} Utilities
 *
 * @see PropertySources
 * @since 2.5.8
 */
public abstract class PropertySourcesUtils {

    /**
     * Get Sub {@link Properties}
     *
     * @param propertySources {@link PropertySource} Iterable
     * @param prefix          the prefix of property name
     * @return Map<String                                                                                                                                                                                                                                                               ,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               String>
     * @see Properties
     */
    public static Map<String, String> getSubProperties(Iterable<PropertySource<?>> propertySources, String prefix) {

        Map<String, String> subProperties = new LinkedHashMap<String, String>();

        String normalizedPrefix = normalizePrefix(prefix);

        PropertySourcesPropertyResolver propertyResolver = new PropertySourcesPropertyResolver((PropertySources) propertySources);

        for (PropertySource<?> source : propertySources) {
            if (source instanceof EnumerablePropertySource) {
                for (String name : ((EnumerablePropertySource<?>) source).getPropertyNames()) {
                    if (name.startsWith(normalizedPrefix)) {
                        String subName = name.substring(normalizedPrefix.length());
                        String value = propertyResolver.getProperty(name);
                        subProperties.putIfAbsent(subName, value);
                    }
                }
            }
        }

        return subProperties;

    }

    /**
     * Normalize the prefix
     *
     * @param prefix the prefix
     * @return the prefix
     */
    public static String normalizePrefix(String prefix) {
        return prefix.endsWith(".") ? prefix : prefix + ".";
    }

}
