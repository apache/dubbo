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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for sensitive parameters that should be hidden in URL string representations
 * to prevent credential leakage in logs and exceptions.
 * <p>
 * Supports both default sensitive parameters and custom configurations through:
 * - System properties
 * - Environment variables
 * - Programmatic configuration
 * - Dubbo configuration system
 * </p>
 */
public class SensitiveParameterConfig extends AbstractConfig {

    private static final long serialVersionUID = 1L;

    /**
     * System property key for configuring custom sensitive parameters
     */
    public static final String SENSITIVE_PARAMS_PROPERTY = "dubbo.url.sensitive.parameters";

    /**
     * Environment variable key for configuring custom sensitive parameters
     */
    public static final String SENSITIVE_PARAMS_ENV = "DUBBO_URL_SENSITIVE_PARAMETERS";

    /**
     * Default sensitive parameter names
     */
    private static final Set<String> DEFAULT_SENSITIVE_PARAMETERS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    CommonConstants.USERNAME_KEY, // "username"
                    CommonConstants.PASSWORD_KEY, // "password"
                    CommonConstants.ACCESS_KEY, // "accessKey"
                    CommonConstants.SECRET_KEY // "secretKey"
                    )));

    /**
     * Cache for sensitive parameter checks to improve performance
     */
    private static final ConcurrentHashMap<String, Boolean> SENSITIVE_CACHE = new ConcurrentHashMap<>();

    /**
     * Custom sensitive parameters configured at runtime
     */
    private static volatile Set<String> customSensitiveParameters = null;

    /**
     * Global instance of the configuration
     */
    private static volatile SensitiveParameterConfig instance;

    /**
     * Configuration property for additional sensitive parameters (comma-separated)
     */
    private String additionalParameters;

    public SensitiveParameterConfig() {
        super();
    }

    public SensitiveParameterConfig(ScopeModel scopeModel) {
        super(scopeModel);
    }

    /**
     * Get global instance of SensitiveParameterConfig
     */
    public static SensitiveParameterConfig getInstance() {
        if (instance == null) {
            synchronized (SensitiveParameterConfig.class) {
                if (instance == null) {
                    instance = new SensitiveParameterConfig();
                    instance.refresh(); // Initialize with default configuration
                }
            }
        }
        return instance;
    }

    /**
     * Get additional sensitive parameters
     */
    public String getAdditionalParameters() {
        return additionalParameters;
    }

    /**
     * Set additional sensitive parameters (comma-separated)
     */
    public void setAdditionalParameters(String additionalParameters) {
        this.additionalParameters = additionalParameters;
        updateCustomSensitiveParameters();
    }

    /**
     * Update the static custom sensitive parameters when configuration changes
     */
    private void updateCustomSensitiveParameters() {
        Set<String> newCustomParams = loadCustomSensitiveParameters();

        // Add configured additional parameters
        if (StringUtils.isNotEmpty(additionalParameters)) {
            String[] paramArray = additionalParameters.split(",");
            Set<String> mutableParams = new HashSet<>(newCustomParams);
            for (String param : paramArray) {
                if (StringUtils.isNotEmpty(param)) {
                    mutableParams.add(param.trim());
                }
            }
            newCustomParams = Collections.unmodifiableSet(mutableParams);
        }

        customSensitiveParameters = newCustomParams;
        clearCache();
    }

    @Override
    protected void checkDefault() {
        super.checkDefault();
        // Initialize custom parameters when configuration is refreshed
        updateCustomSensitiveParameters();
    }

    /**
     * Check if a parameter key is considered sensitive
     *
     * @param key the parameter key to check
     * @return true if the parameter is sensitive and should be hidden
     */
    public static boolean isSensitiveParameter(String key) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }

        // Use cache for performance
        return SENSITIVE_CACHE.computeIfAbsent(key, k -> {
            // Check default sensitive parameters
            if (DEFAULT_SENSITIVE_PARAMETERS.contains(k)) {
                return true;
            }

            // Check custom configured parameters
            Set<String> customParams = getCustomSensitiveParameters();
            return customParams.contains(k);
        });
    }

    /**
     * Add custom sensitive parameter names
     *
     * @param parameterNames the parameter names to add as sensitive
     */
    public static void addSensitiveParameters(String... parameterNames) {
        if (parameterNames == null || parameterNames.length == 0) {
            return;
        }

        Set<String> currentCustom = getCustomSensitiveParameters();
        Set<String> newCustom = new HashSet<>(currentCustom);

        for (String param : parameterNames) {
            if (StringUtils.isNotEmpty(param)) {
                newCustom.add(param.trim());
            }
        }

        customSensitiveParameters = Collections.unmodifiableSet(newCustom);

        // Clear cache to reflect new configuration
        clearCache();
    }

    /**
     * Remove custom sensitive parameter names
     *
     * @param parameterNames the parameter names to remove from sensitive list
     */
    public static void removeSensitiveParameters(String... parameterNames) {
        if (parameterNames == null || parameterNames.length == 0) {
            return;
        }

        Set<String> currentCustom = getCustomSensitiveParameters();
        Set<String> newCustom = new HashSet<>(currentCustom);

        for (String param : parameterNames) {
            if (StringUtils.isNotEmpty(param)) {
                newCustom.remove(param.trim());
            }
        }

        customSensitiveParameters = Collections.unmodifiableSet(newCustom);

        // Clear cache to reflect new configuration
        clearCache();
    }

    /**
     * Get all sensitive parameter names (default + custom)
     *
     * @return unmodifiable set of all sensitive parameter names
     */
    public static Set<String> getAllSensitiveParameters() {
        Set<String> allParams = new HashSet<>(DEFAULT_SENSITIVE_PARAMETERS);
        allParams.addAll(getCustomSensitiveParameters());
        return Collections.unmodifiableSet(allParams);
    }

    /**
     * Get default sensitive parameter names
     *
     * @return unmodifiable set of default sensitive parameter names
     */
    public static Set<String> getDefaultSensitiveParameters() {
        return DEFAULT_SENSITIVE_PARAMETERS;
    }

    /**
     * Get custom sensitive parameter names
     *
     * @return unmodifiable set of custom sensitive parameter names
     */
    public static Set<String> getCustomSensitiveParameters() {
        if (customSensitiveParameters == null) {
            synchronized (SensitiveParameterConfig.class) {
                if (customSensitiveParameters == null) {
                    customSensitiveParameters = loadCustomSensitiveParameters();
                }
            }
        }
        return customSensitiveParameters;
    }

    /**
     * Reset to default configuration (clear all custom parameters and reload from system properties)
     */
    public static void resetToDefaults() {
        customSensitiveParameters = null; // Set to null to force reload from system properties
        clearCache();
    }

    /**
     * Clear the internal cache
     */
    public static void clearCache() {
        SENSITIVE_CACHE.clear();
    }

    /**
     * Load custom sensitive parameters from system properties and environment variables
     */
    private static Set<String> loadCustomSensitiveParameters() {
        Set<String> params = new HashSet<>();

        // Load from system property
        String systemProperty = System.getProperty(SENSITIVE_PARAMS_PROPERTY);
        if (StringUtils.isNotEmpty(systemProperty)) {
            String[] paramArray = systemProperty.split(",");
            for (String param : paramArray) {
                if (StringUtils.isNotEmpty(param)) {
                    params.add(param.trim());
                }
            }
        }

        // Load from environment variable
        String envVariable = System.getenv(SENSITIVE_PARAMS_ENV);
        if (StringUtils.isNotEmpty(envVariable)) {
            String[] paramArray = envVariable.split(",");
            for (String param : paramArray) {
                if (StringUtils.isNotEmpty(param)) {
                    params.add(param.trim());
                }
            }
        }

        return Collections.unmodifiableSet(params);
    }
}
