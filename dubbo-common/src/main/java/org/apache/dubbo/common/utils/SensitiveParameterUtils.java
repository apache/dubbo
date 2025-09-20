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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for sensitive parameter operations.
 * Provides methods to check if URL parameters should be hidden from logs and exceptions
 * to prevent credential leakage.
 */
public class SensitiveParameterUtils {

    /**
     * Default sensitive parameter names that should be hidden in URL representations.
     */
    private static final Set<String> DEFAULT_SENSITIVE_PARAMETERS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    "accessKey",
                    "access_key",
                    "accesskey",
                    "secretKey",
                    "secret_key",
                    "secretkey",
                    "password",
                    "passwd",
                    "pwd",
                    "username",
                    "user",
                    "token",
                    "auth_token",
                    "authToken",
                    "credential",
                    "credentials",
                    "secret",
                    "private_key",
                    "privateKey")));

    /**
     * Checks if a parameter name is considered sensitive.
     * This includes both default sensitive parameters and any additional ones
     * configured through ApplicationConfig.
     *
     * @param parameterName the parameter name to check
     * @return true if the parameter is considered sensitive
     */
    public static boolean isSensitiveParameter(String parameterName) {
        if (StringUtils.isEmpty(parameterName)) {
            return false;
        }

        // Check default sensitive parameters
        if (DEFAULT_SENSITIVE_PARAMETERS.contains(parameterName)) {
            return true;
        }

        // Check additional configured parameters
        return isAdditionalSensitiveParameter(parameterName);
    }

    /**
     * Checks if a parameter name is in the additional sensitive parameters list.
     *
     * @param parameterName the parameter name to check
     * @return true if the parameter is in additional sensitive parameters
     */
    private static boolean isAdditionalSensitiveParameter(String parameterName) {
        try {
            // Get configuration from ApplicationModel
            ApplicationModel applicationModel = ApplicationModel.defaultModel();
            if (applicationModel != null) {
                ApplicationConfig config =
                        applicationModel.getConfigManager().getApplication().orElse(null);

                if (config != null && StringUtils.isNotEmpty(config.getAdditionalSensitiveParameters())) {
                    String[] additionalParams =
                            config.getAdditionalSensitiveParameters().split(",");
                    for (String param : additionalParams) {
                        if (parameterName.equals(param.trim())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log but don't fail - fall back to default behavior
            // In a real implementation, you might want to use a logger here
        }

        return false;
    }

    /**
     * Gets all configured sensitive parameter names.
     *
     * @return a set containing all sensitive parameter names
     */
    public static Set<String> getAllSensitiveParameters() {
        Set<String> allSensitive = new HashSet<>(DEFAULT_SENSITIVE_PARAMETERS);

        try {
            ApplicationModel applicationModel = ApplicationModel.defaultModel();
            if (applicationModel != null) {
                ApplicationConfig config =
                        applicationModel.getConfigManager().getApplication().orElse(null);

                if (config != null && StringUtils.isNotEmpty(config.getAdditionalSensitiveParameters())) {
                    String[] additionalParams =
                            config.getAdditionalSensitiveParameters().split(",");
                    for (String param : additionalParams) {
                        String trimmedParam = param.trim();
                        if (StringUtils.isNotEmpty(trimmedParam)) {
                            allSensitive.add(trimmedParam);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log but don't fail - return default parameters only
        }

        return Collections.unmodifiableSet(allSensitive);
    }

    /**
     * Gets the default sensitive parameter names.
     *
     * @return a set containing default sensitive parameter names
     */
    public static Set<String> getDefaultSensitiveParameters() {
        return DEFAULT_SENSITIVE_PARAMETERS;
    }
}
