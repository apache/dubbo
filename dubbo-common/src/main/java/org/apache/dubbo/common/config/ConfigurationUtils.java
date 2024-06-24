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

import org.apache.dubbo.common.config.configcenter.DynamicConfigurationFactory;
import org.apache.dubbo.common.extension.ExtensionAccessor;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_SECONDS_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_PROPERTY_TYPE_MISMATCH;

/**
 * Utilities for manipulating configurations from different sources
 */
public final class ConfigurationUtils {

    /**
     * Forbids instantiation.
     */
    private ConfigurationUtils() {
        throw new UnsupportedOperationException("No instance of 'ConfigurationUtils' for you! ");
    }

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ConfigurationUtils.class);
    private static final Set<String> securityKey;

    private static volatile long expectedShutdownTime = Long.MAX_VALUE;

    static {
        Set<String> keys = new HashSet<>();
        keys.add("accesslog");
        keys.add("router");
        keys.add("rule");
        keys.add("runtime");
        keys.add("type");
        securityKey = Collections.unmodifiableSet(keys);
    }

    /**
     * Used to get properties from the jvm
     *
     * @return
     */
    public static Configuration getSystemConfiguration(ScopeModel scopeModel) {
        return getScopeModelOrDefaultApplicationModel(scopeModel)
                .modelEnvironment()
                .getSystemConfiguration();
    }

    /**
     * Used to get properties from the os environment
     *
     * @return
     */
    public static Configuration getEnvConfiguration(ScopeModel scopeModel) {
        return getScopeModelOrDefaultApplicationModel(scopeModel)
                .modelEnvironment()
                .getEnvironmentConfiguration();
    }

    /**
     * Used to get a composite property value.
     * <p>
     * Also see {@link Environment#getConfiguration()}
     *
     * @return
     */
    public static Configuration getGlobalConfiguration(ScopeModel scopeModel) {
        return getScopeModelOrDefaultApplicationModel(scopeModel)
                .modelEnvironment()
                .getConfiguration();
    }

    public static Configuration getDynamicGlobalConfiguration(ScopeModel scopeModel) {
        return scopeModel.modelEnvironment().getDynamicGlobalConfiguration();
    }

    // FIXME

    /**
     * Server shutdown wait timeout mills
     *
     * @return
     */
    @SuppressWarnings("deprecation")
    public static int getServerShutdownTimeout(ScopeModel scopeModel) {
        if (expectedShutdownTime < System.currentTimeMillis()) {
            return 1;
        }
        int timeout = DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
        Configuration configuration = getGlobalConfiguration(scopeModel);
        String value = StringUtils.trim(configuration.getString(SHUTDOWN_WAIT_KEY));

        if (StringUtils.isNotEmpty(value)) {
            try {
                timeout = Integer.parseInt(value);
            } catch (Exception e) {
                // ignore
            }
        } else {
            value = StringUtils.trim(configuration.getString(SHUTDOWN_WAIT_SECONDS_KEY));
            if (StringUtils.isNotEmpty(value)) {
                try {
                    timeout = Integer.parseInt(value) * 1000;
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        if (expectedShutdownTime - System.currentTimeMillis() < timeout) {
            return (int) Math.max(1, expectedShutdownTime - System.currentTimeMillis());
        }

        return timeout;
    }

    public static int reCalShutdownTime(int expected) {
        // already timeout
        if (expectedShutdownTime < System.currentTimeMillis()) {
            return 1;
        }

        if (expectedShutdownTime - System.currentTimeMillis() < expected) {
            // the shutdown time rest is less than expected
            return (int) Math.max(1, expectedShutdownTime - System.currentTimeMillis());
        }

        // return the expected
        return expected;
    }

    public static void setExpectedShutdownTime(long expectedShutdownTime) {
        ConfigurationUtils.expectedShutdownTime = expectedShutdownTime;
    }

    public static String getCachedDynamicProperty(ScopeModel realScopeModel, String key, String defaultValue) {
        ScopeModel scopeModel = getScopeModelOrDefaultApplicationModel(realScopeModel);
        ConfigurationCache configurationCache = scopeModel.getBeanFactory().getBean(ConfigurationCache.class);
        String value = configurationCache.computeIfAbsent(
                key, _k -> ConfigurationUtils.getDynamicProperty(scopeModel, _k, ""));
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    private static ScopeModel getScopeModelOrDefaultApplicationModel(ScopeModel realScopeModel) {
        if (realScopeModel == null) {
            return ApplicationModel.defaultModel();
        }
        return realScopeModel;
    }

    public static String getDynamicProperty(ScopeModel scopeModel, String property) {
        return getDynamicProperty(scopeModel, property, null);
    }

    public static String getDynamicProperty(ScopeModel scopeModel, String property, String defaultValue) {
        return StringUtils.trim(getDynamicGlobalConfiguration(scopeModel).getString(property, defaultValue));
    }

    public static String getProperty(ScopeModel scopeModel, String property) {
        return getProperty(scopeModel, property, null);
    }

    public static String getProperty(ScopeModel scopeModel, String property, String defaultValue) {
        return StringUtils.trim(getGlobalConfiguration(scopeModel).getString(property, defaultValue));
    }

    public static int get(ScopeModel scopeModel, String property, int defaultValue) {
        return getGlobalConfiguration(scopeModel).getInt(property, defaultValue);
    }

    public static Map<String, String> parseProperties(String content) throws IOException {
        Map<String, String> map = new HashMap<>();
        if (StringUtils.isEmpty(content)) {
            logger.info("Config center was specified, but no config item found.");
        } else {
            Properties properties = new Properties();
            properties.load(new StringReader(content));
            properties.stringPropertyNames().forEach(k -> {
                boolean deny = false;
                // check whether property name is safe or not based on the last fragment kebab-case comparison.
                String[] fragments = k.split("\\.");
                if (securityKey.contains(StringUtils.convertToSplitName(fragments[fragments.length - 1], "-"))) {
                    deny = true;
                    logger.warn(
                            COMMON_PROPERTY_TYPE_MISMATCH,
                            "security properties are not allowed to be set",
                            "",
                            String.format("'%s' is not allowed to be set as it is on the security key list.", k));
                }
                if (!deny) {
                    map.put(k, properties.getProperty(k));
                }
            });
        }
        return map;
    }

    public static boolean isEmptyValue(Object value) {
        return value == null || value instanceof String && StringUtils.isBlank((String) value);
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
     *
     * @param configMaps
     * @param prefix
     * @param <V>
     * @return
     */
    public static <V extends Object> Map<String, V> getSubProperties(
            Collection<Map<String, V>> configMaps, String prefix) {
        Map<String, V> map = new LinkedHashMap<>();
        for (Map<String, V> configMap : configMaps) {
            getSubProperties(configMap, prefix, map);
        }
        return map;
    }

    public static <V extends Object> Map<String, V> getSubProperties(Map<String, V> configMap, String prefix) {
        return getSubProperties(configMap, prefix, null);
    }

    private static <V extends Object> Map<String, V> getSubProperties(
            Map<String, V> configMap, String prefix, Map<String, V> resultMap) {
        if (!prefix.endsWith(".")) {
            prefix += ".";
        }

        if (null == resultMap) {
            resultMap = new LinkedHashMap<>();
        }

        if (CollectionUtils.isNotEmptyMap(configMap)) {
            Map<String, V> copy;
            synchronized (configMap) {
                copy = new HashMap<>(configMap);
            }
            for (Map.Entry<String, V> entry : copy.entrySet()) {
                String key = entry.getKey();
                V val = entry.getValue();
                if (StringUtils.startsWithIgnoreCase(key, prefix)
                        && key.length() > prefix.length()
                        && !ConfigurationUtils.isEmptyValue(val)) {

                    String k = key.substring(prefix.length());
                    // convert camelCase/snake_case to kebab-case
                    String newK = StringUtils.convertToSplitName(k, "-");
                    resultMap.putIfAbsent(newK, val);
                    if (!Objects.equals(k, newK)) {
                        resultMap.putIfAbsent(k, val);
                    }
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
        Map<String, V> copy;
        synchronized (configMap) {
            copy = new HashMap<>(configMap);
        }
        for (Map.Entry<String, V> entry : copy.entrySet()) {
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
            Map<String, V> copy;
            synchronized (configMap) {
                copy = new HashMap<>(configMap);
            }
            for (Map.Entry<String, V> entry : copy.entrySet()) {
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

    /**
     * Get an instance of {@link DynamicConfigurationFactory} by the specified name. If not found, take the default
     * extension of {@link DynamicConfigurationFactory}
     *
     * @param name the name of extension of {@link DynamicConfigurationFactory}
     * @return non-null
     * @see 2.7.4
     */
    public static DynamicConfigurationFactory getDynamicConfigurationFactory(
            ExtensionAccessor extensionAccessor, String name) {
        ExtensionLoader<DynamicConfigurationFactory> loader =
                extensionAccessor.getExtensionLoader(DynamicConfigurationFactory.class);
        return loader.getOrDefaultExtension(name);
    }

    /**
     * For compact single instance
     *
     * @deprecated Replaced to {@link ConfigurationUtils#getSystemConfiguration(ScopeModel)}
     */
    @Deprecated
    public static Configuration getSystemConfiguration() {
        return ApplicationModel.defaultModel().modelEnvironment().getSystemConfiguration();
    }

    /**
     * For compact single instance
     *
     * @deprecated Replaced to {@link ConfigurationUtils#getEnvConfiguration(ScopeModel)}
     */
    @Deprecated
    public static Configuration getEnvConfiguration() {
        return ApplicationModel.defaultModel().modelEnvironment().getEnvironmentConfiguration();
    }

    /**
     * For compact single instance
     *
     * @deprecated Replaced to {@link ConfigurationUtils#getGlobalConfiguration(ScopeModel)}
     */
    @Deprecated
    public static Configuration getGlobalConfiguration() {
        return ApplicationModel.defaultModel().modelEnvironment().getConfiguration();
    }

    /**
     * For compact single instance
     *
     * @deprecated Replaced to {@link ConfigurationUtils#getDynamicGlobalConfiguration(ScopeModel)}
     */
    @Deprecated
    public static Configuration getDynamicGlobalConfiguration() {
        return ApplicationModel.defaultModel()
                .getDefaultModule()
                .modelEnvironment()
                .getDynamicGlobalConfiguration();
    }

    /**
     * For compact single instance
     *
     * @deprecated Replaced to {@link ConfigurationUtils#getCachedDynamicProperty(ScopeModel, String, String)}
     */
    @Deprecated
    public static String getCachedDynamicProperty(String key, String defaultValue) {
        return getCachedDynamicProperty(ApplicationModel.defaultModel(), key, defaultValue);
    }

    /**
     * For compact single instance
     *
     * @deprecated Replaced to {@link ConfigurationUtils#getDynamicProperty(ScopeModel, String)}
     */
    @Deprecated
    public static String getDynamicProperty(String property) {
        return getDynamicProperty(ApplicationModel.defaultModel(), property);
    }

    /**
     * For compact single instance
     *
     * @deprecated Replaced to {@link ConfigurationUtils#getDynamicProperty(ScopeModel, String, String)}
     */
    @Deprecated
    public static String getDynamicProperty(String property, String defaultValue) {
        return getDynamicProperty(ApplicationModel.defaultModel(), property, defaultValue);
    }

    /**
     * For compact single instance
     *
     * @deprecated Replaced to {@link ConfigurationUtils#getProperty(ScopeModel, String)}
     */
    @Deprecated
    public static String getProperty(String property) {
        return getProperty(ApplicationModel.defaultModel(), property);
    }

    /**
     * For compact single instance
     *
     * @deprecated Replaced to {@link ConfigurationUtils#getProperty(ScopeModel, String, String)}
     */
    @Deprecated
    public static String getProperty(String property, String defaultValue) {
        return getProperty(ApplicationModel.defaultModel(), property, defaultValue);
    }

    /**
     * For compact single instance
     *
     * @deprecated Replaced to {@link ConfigurationUtils#get(ScopeModel, String, int)}
     */
    @Deprecated
    public static int get(String property, int defaultValue) {
        return get(ApplicationModel.defaultModel(), property, defaultValue);
    }
}
