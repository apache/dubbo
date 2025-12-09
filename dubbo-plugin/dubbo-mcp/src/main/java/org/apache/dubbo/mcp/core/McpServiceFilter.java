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
package org.apache.dubbo.mcp.core;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.mcp.McpConstant;
import org.apache.dubbo.mcp.annotations.McpTool;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class McpServiceFilter {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(McpServiceFilter.class);

    private final Configuration configuration;
    private final Pattern[] includePatterns;
    private final Pattern[] excludePatterns;
    private final boolean defaultEnabled;

    public McpServiceFilter(ApplicationModel applicationModel) {
        this.configuration = ConfigurationUtils.getGlobalConfiguration(applicationModel);
        this.defaultEnabled = configuration.getBoolean(McpConstant.SETTINGS_MCP_DEFAULT_ENABLED, true);

        String includeStr = configuration.getString(McpConstant.SETTINGS_MCP_INCLUDE_PATTERNS, "");
        String excludeStr = configuration.getString(McpConstant.SETTINGS_MCP_EXCLUDE_PATTERNS, "");

        this.includePatterns = parsePatterns(includeStr);
        this.excludePatterns = parsePatterns(excludeStr);
    }

    /**
     * Check if service should be exposed as MCP tool.
     * Priority: URL Parameters > Annotations > Configuration File > Default
     */
    public boolean shouldExposeAsMcpTool(ProviderModel providerModel) {
        String interfaceName = providerModel.getServiceModel().getInterfaceName();

        if (isMatchedByPatterns(interfaceName, excludePatterns)) {
            return false;
        }

        URL serviceUrl = getServiceUrl(providerModel);
        if (serviceUrl != null) {
            String urlValue = serviceUrl.getParameter(McpConstant.PARAM_MCP_ENABLED);
            if (urlValue != null && StringUtils.isNotEmpty(urlValue)) {
                return Boolean.parseBoolean(urlValue);
            }
        }

        Object serviceBean = providerModel.getServiceInstance();
        if (serviceBean != null) {
            DubboService dubboService = serviceBean.getClass().getAnnotation(DubboService.class);
            if (dubboService != null && dubboService.mcpEnabled()) {
                return true;
            }
        }

        String serviceSpecificKey = McpConstant.SETTINGS_MCP_SERVICE_PREFIX + "." + interfaceName + ".enabled";
        if (configuration.containsKey(serviceSpecificKey)) {
            return configuration.getBoolean(serviceSpecificKey, false);
        }

        if (configuration.containsKey(McpConstant.PARAM_MCP_ENABLED)) {
            return configuration.getBoolean(McpConstant.PARAM_MCP_ENABLED, false);
        }

        if (includePatterns.length > 0) {
            return isMatchedByPatterns(interfaceName, includePatterns);
        }

        return defaultEnabled;
    }

    /**
     * Check if specific method should be exposed as MCP tool.
     * Priority: @McpTool(enabled=false) > URL Parameters > @McpTool(enabled=true) > Configuration > Service-level
     */
    public boolean shouldExposeMethodAsMcpTool(ProviderModel providerModel, Method method) {
        String interfaceName = providerModel.getServiceModel().getInterfaceName();
        String methodName = method.getName();

        if (isMatchedByPatterns(interfaceName, excludePatterns)) {
            return false;
        }

        McpTool methodMcpTool = getMethodMcpTool(providerModel, method);

        if (methodMcpTool != null && !methodMcpTool.enabled()) {
            return false;
        }

        URL serviceUrl = getServiceUrl(providerModel);
        if (serviceUrl != null) {
            String methodUrlValue = serviceUrl.getMethodParameter(methodName, McpConstant.PARAM_MCP_ENABLED);

            if (methodUrlValue != null && StringUtils.isNotEmpty(methodUrlValue)) {
                return Boolean.parseBoolean(methodUrlValue);
            }

            String serviceLevelValue = serviceUrl.getParameter(McpConstant.PARAM_MCP_ENABLED);

            if (serviceLevelValue != null && StringUtils.isNotEmpty(serviceLevelValue)) {
                return Boolean.parseBoolean(serviceLevelValue);
            }
        }

        if (methodMcpTool != null && methodMcpTool.enabled()) {
            return true;
        }

        String methodConfigKey =
                McpConstant.SETTINGS_MCP_SERVICE_PREFIX + "." + interfaceName + ".methods." + methodName + ".enabled";
        if (configuration.containsKey(methodConfigKey)) {
            return configuration.getBoolean(methodConfigKey, false);
        }

        if (shouldExposeAsMcpTool(providerModel)) {
            return Modifier.isPublic(method.getModifiers());
        }

        return false;
    }

    /**
     * Get @McpTool annotation from method, checking both interface and implementation class.
     */
    private McpTool getMethodMcpTool(ProviderModel providerModel, Method method) {
        String methodName = method.getName();
        Class<?>[] paramTypes = method.getParameterTypes();

        Object serviceBean = providerModel.getServiceInstance();
        if (serviceBean != null) {
            try {
                Method implMethod = serviceBean.getClass().getMethod(methodName, paramTypes);
                McpTool implMcpTool = implMethod.getAnnotation(McpTool.class);
                if (implMcpTool != null) {
                    return implMcpTool;
                }
            } catch (NoSuchMethodException e) {
                // Method not found in implementation class
                logger.error(
                        LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                        "",
                        "",
                        "Method not found in implementation class: " + methodName + " with parameters: "
                                + Arrays.toString(paramTypes),
                        e);
            }
        }

        McpTool interfaceMcpTool = method.getAnnotation(McpTool.class);
        if (interfaceMcpTool != null) {
            return interfaceMcpTool;
        }

        Class<?> serviceInterface = providerModel.getServiceModel().getServiceInterfaceClass();
        if (serviceInterface != null) {
            try {
                Method interfaceMethod = serviceInterface.getMethod(methodName, paramTypes);
                return interfaceMethod.getAnnotation(McpTool.class);
            } catch (NoSuchMethodException e) {
                // Method not found in service interface
            }
        }

        return null;
    }

    public McpToolConfig getMcpToolConfig(ProviderModel providerModel, Method method) {
        String interfaceName = providerModel.getServiceModel().getInterfaceName();
        McpToolConfig config = new McpToolConfig();

        config.setToolName(method.getName());

        McpTool mcpTool = getMethodMcpTool(providerModel, method);
        if (mcpTool != null) {
            if (StringUtils.isNotEmpty(mcpTool.name())) {
                config.setToolName(mcpTool.name());
            }
            if (StringUtils.isNotEmpty(mcpTool.description())) {
                config.setDescription(mcpTool.description());
            }
            if (mcpTool.tags().length > 0) {
                config.setTags(Arrays.asList(mcpTool.tags()));
            }
            config.setPriority(mcpTool.priority());
        }

        String methodPrefix =
                McpConstant.SETTINGS_MCP_SERVICE_PREFIX + "." + interfaceName + ".methods." + method.getName() + ".";

        String configToolName = configuration.getString(methodPrefix + "name");
        if (StringUtils.isNotEmpty(configToolName)) {
            config.setToolName(configToolName);
        }

        String configDescription = configuration.getString(methodPrefix + "description");
        if (StringUtils.isNotEmpty(configDescription)) {
            config.setDescription(configDescription);
        }

        String configTags = configuration.getString(methodPrefix + "tags");
        if (StringUtils.isNotEmpty(configTags)) {
            config.setTags(Arrays.asList(configTags.split(",")));
        }

        URL serviceUrl = getServiceUrl(providerModel);
        if (serviceUrl != null) {
            String urlToolName = serviceUrl.getMethodParameter(method.getName(), McpConstant.PARAM_MCP_TOOL_NAME);
            if (urlToolName != null && StringUtils.isNotEmpty(urlToolName)) {
                config.setToolName(urlToolName);
            }

            String urlDescription = serviceUrl.getMethodParameter(method.getName(), McpConstant.PARAM_MCP_DESCRIPTION);
            if (urlDescription != null && StringUtils.isNotEmpty(urlDescription)) {
                config.setDescription(urlDescription);
            }

            String urlTags = serviceUrl.getMethodParameter(method.getName(), McpConstant.PARAM_MCP_TAGS);
            if (urlTags != null && StringUtils.isNotEmpty(urlTags)) {
                config.setTags(Arrays.asList(urlTags.split(",")));
            }

            String urlPriority = serviceUrl.getMethodParameter(method.getName(), McpConstant.PARAM_MCP_PRIORITY);
            if (urlPriority != null && StringUtils.isNotEmpty(urlPriority)) {
                try {
                    config.setPriority(Integer.parseInt(urlPriority));
                } catch (NumberFormatException e) {
                    logger.warn(
                            LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                            "",
                            "",
                            "Invalid URL priority value: " + urlPriority + " for method: " + method.getName());
                }
            }
        }

        return config;
    }

    public McpToolConfig getMcpToolConfig(ProviderModel providerModel) {
        String interfaceName = providerModel.getServiceModel().getInterfaceName();
        McpToolConfig config = new McpToolConfig();

        String servicePrefix = McpConstant.SETTINGS_MCP_SERVICE_PREFIX + "." + interfaceName + ".";

        String configToolName = configuration.getString(servicePrefix + "name");
        if (StringUtils.isNotEmpty(configToolName)) {
            config.setToolName(configToolName);
        }

        String configDescription = configuration.getString(servicePrefix + "description");
        if (StringUtils.isNotEmpty(configDescription)) {
            config.setDescription(configDescription);
        }

        String configTags = configuration.getString(servicePrefix + "tags");
        if (StringUtils.isNotEmpty(configTags)) {
            config.setTags(Arrays.asList(configTags.split(",")));
        }

        URL serviceUrl = getServiceUrl(providerModel);
        if (serviceUrl != null) {
            String urlToolName = serviceUrl.getParameter(McpConstant.PARAM_MCP_TOOL_NAME);
            if (urlToolName != null && StringUtils.isNotEmpty(urlToolName)) {
                config.setToolName(urlToolName);
            }

            String urlDescription = serviceUrl.getParameter(McpConstant.PARAM_MCP_DESCRIPTION);
            if (urlDescription != null && StringUtils.isNotEmpty(urlDescription)) {
                config.setDescription(urlDescription);
            }

            String urlTags = serviceUrl.getParameter(McpConstant.PARAM_MCP_TAGS);
            if (urlTags != null && StringUtils.isNotEmpty(urlTags)) {
                config.setTags(Arrays.asList(urlTags.split(",")));
            }

            String urlPriority = serviceUrl.getParameter(McpConstant.PARAM_MCP_PRIORITY);
            if (urlPriority != null && StringUtils.isNotEmpty(urlPriority)) {
                try {
                    config.setPriority(Integer.parseInt(urlPriority));
                } catch (NumberFormatException e) {
                    logger.warn(
                            LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                            "",
                            "",
                            "Invalid URL priority value: " + urlPriority + " for service: " + interfaceName);
                }
            }
        }

        return config;
    }

    private URL getServiceUrl(ProviderModel providerModel) {
        List<URL> serviceUrls = providerModel.getServiceUrls();
        if (serviceUrls != null && !serviceUrls.isEmpty()) {
            return serviceUrls.get(0);
        }
        return null;
    }

    private Pattern[] parsePatterns(String patternStr) {
        if (StringUtils.isEmpty(patternStr)) {
            return new Pattern[0];
        }

        return Arrays.stream(patternStr.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .map(pattern -> Pattern.compile(pattern.replace("*", ".*")))
                .toArray(Pattern[]::new);
    }

    private boolean isMatchedByPatterns(String text, Pattern[] patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(text).matches()) {
                return true;
            }
        }
        return false;
    }

    public static class McpToolConfig {
        private String toolName;
        private String description;
        private List<String> tags;
        private int priority = 0;

        public String getToolName() {
            return toolName;
        }

        public void setToolName(String toolName) {
            this.toolName = toolName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }
    }
}
