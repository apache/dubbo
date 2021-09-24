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
package org.apache.dubbo.spring.boot.util;

import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The utilities class for {@link Environment}
 *
 * @see Environment
 * @since 2.7.0
 */
public abstract class EnvironmentUtils {

    /**
     * Extras The properties from {@link ConfigurableEnvironment}
     *
     * @param environment {@link ConfigurableEnvironment}
     * @return Read-only Map
     */
    public static Map<String, Object> extractProperties(ConfigurableEnvironment environment) {
        return Collections.unmodifiableMap(doExtraProperties(environment));
    }

//    /**
//     * Gets {@link PropertySource} Map , the {@link PropertySource#getName()} as key
//     *
//     * @param environment {@link ConfigurableEnvironment}
//     * @return Read-only Map
//     */
//    public static Map<String, PropertySource<?>> getPropertySources(ConfigurableEnvironment environment) {
//        return Collections.unmodifiableMap(doGetPropertySources(environment));
//    }

    private static Map<String, Object> doExtraProperties(ConfigurableEnvironment environment) {

        Map<String, Object> properties = new LinkedHashMap<>(); // orderly

        Map<String, PropertySource<?>> map = doGetPropertySources(environment);

        for (PropertySource<?> source : map.values()) {

            if (source instanceof EnumerablePropertySource) {

                EnumerablePropertySource propertySource = (EnumerablePropertySource) source;

                String[] propertyNames = propertySource.getPropertyNames();

                if (ObjectUtils.isEmpty(propertyNames)) {
                    continue;
                }

                for (String propertyName : propertyNames) {

                    if (!properties.containsKey(propertyName)) { // put If absent
                        properties.put(propertyName, propertySource.getProperty(propertyName));
                    }

                }

            }

        }

        return properties;

    }

    private static Map<String, PropertySource<?>> doGetPropertySources(ConfigurableEnvironment environment) {
        Map<String, PropertySource<?>> map = new LinkedHashMap<String, PropertySource<?>>();
        MutablePropertySources sources = environment.getPropertySources();
        for (PropertySource<?> source : sources) {
            extract("", map, source);
        }
        return map;
    }

    private static void extract(String root, Map<String, PropertySource<?>> map,
                                PropertySource<?> source) {
        if (source instanceof CompositePropertySource) {
            for (PropertySource<?> nest : ((CompositePropertySource) source)
                    .getPropertySources()) {
                extract(source.getName() + ":", map, nest);
            }
        } else {
            map.put(root + source.getName(), source);
        }
    }

}
