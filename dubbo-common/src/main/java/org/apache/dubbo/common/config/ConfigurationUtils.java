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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_SECONDS_KEY;

/**
 * Utilities for manipulating configurations from different sources
 */
public class ConfigurationUtils {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationUtils.class);
    private static Map<String, String> CACHED_DYNAMIC_PROPERTIES = new ConcurrentHashMap<>();

    /**
     * Used to get properties from the jvm
     *
     * @return
     */
    public static Configuration getSystemConfiguration() {
        return ApplicationModel.getEnvironment().getSystemConfiguration();
    }

    /**
     * Used to get properties from the os environment
     *
     * @return
     */
    public static Configuration getEnvConfiguration() {
        return ApplicationModel.getEnvironment().getEnvironmentConfiguration();
    }

    /**
     * Used to get an composite property value.
     * <p>
     * Also see {@link Environment#getConfiguration()}
     *
     * @return
     */
    public static Configuration getGlobalConfiguration() {
        return ApplicationModel.getEnvironment().getConfiguration();
    }

    public static Configuration getDynamicGlobalConfiguration() {
        return ApplicationModel.getEnvironment().getDynamicGlobalConfiguration();
    }

    // FIXME
    @SuppressWarnings("deprecation")
    public static int getServerShutdownTimeout() {
        int timeout = DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
        Configuration configuration = getGlobalConfiguration();
        String value = StringUtils.trim(configuration.getString(SHUTDOWN_WAIT_KEY));

        if (value != null && value.length() > 0) {
            try {
                timeout = Integer.parseInt(value);
            } catch (Exception e) {
                // ignore
            }
        } else {
            value = StringUtils.trim(configuration.getString(SHUTDOWN_WAIT_SECONDS_KEY));
            if (value != null && value.length() > 0) {
                try {
                    timeout = Integer.parseInt(value) * 1000;
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return timeout;
    }

    public static String getCachedDynamicProperty(String key, String defaultValue) {
        String value = CACHED_DYNAMIC_PROPERTIES.computeIfAbsent(key, _k -> ConfigurationUtils.getDynamicProperty(key, ""));
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    public static String getDynamicProperty(String property) {
        return getDynamicProperty(property, null);
    }

    public static String getDynamicProperty(String property, String defaultValue) {
        return StringUtils.trim(getDynamicGlobalConfiguration().getString(property, defaultValue));
    }

    public static String getProperty(String property) {
        return getProperty(property, null);
    }

    public static String getProperty(String property, String defaultValue) {
        return StringUtils.trim(getGlobalConfiguration().getString(property, defaultValue));
    }

    public static int get(String property, int defaultValue) {
        return getGlobalConfiguration().getInt(property, defaultValue);
    }

    public static Map<String, String> parseProperties(String content) throws IOException {
        Map<String, String> map = new HashMap<>();
        if (StringUtils.isEmpty(content)) {
            logger.warn("You specified the config center, but there's not even one single config item in it.");
        } else {
            Properties properties = new Properties();
            properties.load(new StringReader(content));
            properties.stringPropertyNames().forEach(
                    k -> map.put(k, properties.getProperty(k))
            );
        }
        return map;
    }

    public static boolean isEmptyValue(Object value) {
        return value == null ||
                value instanceof String && StringUtils.isBlank((String) value);
    }

    /**
     * Search props and extract sub properties.
     * <pre>
     * # properties
     * dubbo.protocol.name=dubbo
     * dubbo.protocol.port=1234
     *
     * # extract protocol props
     * Map props = getSubProperties("dubbo.protocol.");
     *
     * # result
     * props: {"name": "dubbo", "port" : "1234"}
     *
     * </pre>
     * @param configMaps
     * @param prefix
     * @param <V>
     * @return
     */
    public static <V extends Object> Map<String, V> getSubProperties(Collection<Map<String, V>> configMaps, String prefix) {
        Map<String, V> map = new LinkedHashMap<>();
        for (Map<String, V> configMap : configMaps) {
            getSubProperties(configMap, prefix, map);
        }
        return map;
    }

    public static <V extends Object> Map<String, V> getSubProperties(Map<String, V> configMap, String prefix) {
        return getSubProperties(configMap, prefix, null);
    }

    private static <V extends Object> Map<String, V> getSubProperties(Map<String, V> configMap, String prefix, Map<String, V> resultMap) {
        if (!prefix.endsWith(".")) {
            prefix += ".";
        }

        if (null == resultMap) {
            resultMap = new LinkedHashMap<>();
        }

        if (null != configMap) {
            for(Map.Entry<String, V> entry : configMap.entrySet()) {
                String key = entry.getKey();
                V val = entry.getValue();
                if (StringUtils.startsWithIgnoreCase(key, prefix)
                    && key.length() > prefix.length()
                    && !ConfigurationUtils.isEmptyValue(val)) {

                    String k = key.substring(prefix.length());
                    // convert camelCase/snake_case to kebab-case
                    k = StringUtils.convertToSplitName(k, "-");
                    resultMap.putIfAbsent(k, val);
                }
            }
        }

        return resultMap;
    }

    public static <V extends Object> boolean hasSubProperties(Collection<Map<String, V>> configMaps, String prefix) {
        if (!prefix.endsWith(".")) {
            prefix += ".";
        }
        for (Map<String, V> configMap : configMaps) {
            if (hasSubProperties(configMap, prefix)) {
                return true;
            }
        }
        return false;
    }

    public static <V extends Object> boolean hasSubProperties(Map<String, V> configMap, String prefix) {
        if (!prefix.endsWith(".")) {
            prefix += ".";
        }
        for (Map.Entry<String, V> entry : configMap.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.startsWithIgnoreCase(key, prefix)
                && key.length() > prefix.length()
                && !ConfigurationUtils.isEmptyValue(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Search props and extract config ids
     * <pre>
     * # properties
     * dubbo.registries.registry1.address=xxx
     * dubbo.registries.registry2.port=xxx
     *
     * # extract ids
     * Set configIds = getSubIds("dubbo.registries.")
     *
     * # result
     * configIds: ["registry1", "registry2"]
     * </pre>
     *
     * @param configMaps
     * @param prefix
     * @return
     */
    public static <V extends Object> Set<String> getSubIds(Collection<Map<String, V>> configMaps, String prefix) {
        if (!prefix.endsWith(".")) {
            prefix += ".";
        }
        Set<String> ids = new LinkedHashSet<>();
        for (Map<String, V> configMap : configMaps) {
            for (Map.Entry<String, V> entry : configMap.entrySet()) {
                String key = entry.getKey();
                V val = entry.getValue();
                if (StringUtils.startsWithIgnoreCase(key, prefix)
                    && key.length() > prefix.length()
                    && !ConfigurationUtils.isEmptyValue(val)) {

                    String k = key.substring(prefix.length());
                    int endIndex = k.indexOf(".");
                    if (endIndex > 0) {
                        String id = k.substring(0, endIndex);
                        ids.add(id);
                    }
                }
            }
        }
        return ids;
    }

}
