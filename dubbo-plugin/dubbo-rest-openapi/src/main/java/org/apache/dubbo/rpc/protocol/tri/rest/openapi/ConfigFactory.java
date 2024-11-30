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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi;

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.nested.OpenAPIConfig;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_OPENAPI_PREFIX;

public final class ConfigFactory {

    private static Map<String, Method> CONFIG_METHODS;

    private final FrameworkModel frameworkModel;
    private volatile Map<String, OpenAPIConfig> configMap;

    public ConfigFactory(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    private static Environment getEnvironment(FrameworkModel frameworkModel) {
        return frameworkModel.defaultApplication().modelEnvironment();
    }

    public OpenAPIConfig getConfig(String group) {
        return getConfigMap().get(group);
    }

    public OpenAPIConfig getGlobalConfig() {
        return getConfigMap().get(Constants.GLOBAL_GROUP);
    }

    private Map<String, OpenAPIConfig> getConfigMap() {
        if (configMap == null) {
            synchronized (this) {
                if (configMap == null) {
                    configMap = readConfigMap();
                }
            }
        }
        return configMap;
    }

    private Map<String, OpenAPIConfig> readConfigMap() {
        Map<String, OpenAPIConfig> map = new HashMap<>();

        Environment environment = getEnvironment(frameworkModel);
        Configuration configuration = environment.getConfiguration();
        List<Map<String, String>> configMaps = environment.getConfigurationMaps();

        Set<String> allKeys = new HashSet<>();
        for (Map<String, String> configMap : configMaps) {
            for (String key : configMap.keySet()) {
                if (key.startsWith(H2_SETTINGS_OPENAPI_PREFIX)) {
                    allKeys.add(key);
                }
            }
        }

        int len = H2_SETTINGS_OPENAPI_PREFIX.length();
        Map<Pair<String, String>, TreeMap<Integer, String>> valuesMap = new HashMap<>();
        for (String fullKey : allKeys) {
            if (fullKey.length() > len) {
                char c = fullKey.charAt(len);
                String group, key;
                if (c == '.') {
                    group = StringUtils.EMPTY_STRING;
                    key = fullKey.substring(len + 1);
                } else if (c == 's') {
                    int end = fullKey.indexOf('.', len + 1);
                    group = fullKey.substring(len + 1, end);
                    key = fullKey.substring(end + 1);
                } else {
                    continue;
                }

                int brkStart = key.lastIndexOf('[');
                if (brkStart > 0) {
                    try {
                        String value = configuration.getString(fullKey);
                        if (StringUtils.isEmpty(value)) {
                            continue;
                        }
                        int index = Integer.parseInt(key.substring(brkStart + 1, key.length() - 1));
                        valuesMap
                                .computeIfAbsent(Pair.of(group, key.substring(0, brkStart)), k -> new TreeMap<>())
                                .put(index, value);
                    } catch (NumberFormatException ignored) {
                    }
                    continue;
                }

                applyConfigValue(map, group, key, configuration.getString(fullKey));
            }
        }
        for (Map.Entry<Pair<String, String>, TreeMap<Integer, String>> entry : valuesMap.entrySet()) {
            Pair<String, String> pair = entry.getKey();
            String value = StringUtils.join(entry.getValue().values(), ",");
            applyConfigValue(map, pair.getKey(), pair.getValue(), value);
        }
        map.computeIfAbsent(Constants.GLOBAL_GROUP, k -> new OpenAPIConfig());
        return map;
    }

    private static void applyConfigValue(Map<String, OpenAPIConfig> map, String group, String key, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        OpenAPIConfig config = map.computeIfAbsent(group, k -> new OpenAPIConfig());
        int index = key.indexOf("settings.");
        if (index == 0) {
            Map<String, String> settings = config.getSettings();
            if (settings == null) {
                config.setSettings(settings = new HashMap<>());
            }
            settings.put(key.substring(9), value);
            return;
        }

        Map<String, Method> configMethods = CONFIG_METHODS;
        if (configMethods == null) {
            configMethods = new HashMap<>();
            for (Method method : OpenAPIConfig.class.getMethods()) {
                String name = toConfigName(method);
                if (name != null) {
                    configMethods.put(name, method);
                }
            }
            CONFIG_METHODS = configMethods;
        }

        Method method = configMethods.get(key);
        if (method == null) {
            return;
        }

        Class<?> valueType = method.getParameterTypes()[0];
        try {
            if (valueType == String.class) {
                method.invoke(config, value);
            } else if (valueType == Boolean.class) {
                method.invoke(config, StringUtils.toBoolean(value, false));
            } else if (valueType.isArray()) {
                method.invoke(config, new Object[] {StringUtils.tokenize(value)});
            }
        } catch (Throwable ignored) {
        }
    }

    private static String toConfigName(Method method) {
        if (method.getParameterCount() != 1) {
            return null;
        }
        String name = method.getName();
        if (!name.startsWith("set")) {
            return null;
        }
        int len = name.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 3; i < len; i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 3) {
                    sb.append('-');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
