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
package org.apache.dubbo.mcp.tool;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.mcp.JsonSchemaType;
import org.apache.dubbo.mcp.McpConstant;
import org.apache.dubbo.mcp.annotations.McpToolParam;
import org.apache.dubbo.mcp.core.McpServiceFilter;
import org.apache.dubbo.mcp.util.TypeSchemaUtils;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

public class DubboServiceToolRegistry {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(DubboServiceToolRegistry.class);

    private final McpAsyncServer mcpServer;
    private final DubboOpenApiToolConverter toolConverter;
    private final DubboMcpGenericCaller genericCaller;
    private final McpServiceFilter mcpServiceFilter;
    private final Map<String, McpServerFeatures.AsyncToolSpecification> registeredTools = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> serviceToToolsMapping = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public DubboServiceToolRegistry(
            McpAsyncServer mcpServer,
            DubboOpenApiToolConverter toolConverter,
            DubboMcpGenericCaller genericCaller,
            McpServiceFilter mcpServiceFilter) {
        this.mcpServer = mcpServer;
        this.toolConverter = toolConverter;
        this.genericCaller = genericCaller;
        this.mcpServiceFilter = mcpServiceFilter;
        this.objectMapper = new ObjectMapper();
    }

    public int registerService(ProviderModel providerModel) {
        ServiceDescriptor serviceDescriptor = providerModel.getServiceModel();
        List<URL> statedURLs = providerModel.getServiceUrls();

        if (statedURLs == null || statedURLs.isEmpty()) {
            return 0;
        }

        try {
            URL url = statedURLs.get(0);
            int registeredCount = 0;
            String serviceKey = getServiceKey(providerModel);
            Set<String> toolNames = new HashSet<>();

            Class<?> serviceInterface = serviceDescriptor.getServiceInterfaceClass();
            if (serviceInterface == null) {
                return 0;
            }

            Method[] methods = serviceInterface.getDeclaredMethods();
            boolean shouldRegisterServiceLevel = mcpServiceFilter.shouldExposeAsMcpTool(providerModel);

            for (Method method : methods) {
                if (mcpServiceFilter.shouldExposeMethodAsMcpTool(providerModel, method)) {
                    McpServiceFilter.McpToolConfig toolConfig =
                            mcpServiceFilter.getMcpToolConfig(providerModel, method);

                    String toolName = registerMethodAsTool(providerModel, method, url, toolConfig);
                    if (toolName != null) {
                        toolNames.add(toolName);
                        registeredCount++;
                    }
                }
            }

            if (registeredCount == 0 && shouldRegisterServiceLevel) {
                Set<String> serviceToolNames = registerServiceLevelTools(providerModel, url);
                toolNames.addAll(serviceToolNames);
                registeredCount = serviceToolNames.size();
            }

            if (registeredCount > 0) {
                serviceToToolsMapping.put(serviceKey, toolNames);
                logger.info(
                        "Registered {} MCP tools for service: {}",
                        registeredCount,
                        serviceDescriptor.getInterfaceName());
            }

            return registeredCount;

        } catch (Exception e) {
            logger.error(
                    LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                    "",
                    "",
                    "Failed to register service as MCP tools: " + serviceDescriptor.getInterfaceName(),
                    e);
            return 0;
        }
    }

    public void unregisterService(ProviderModel providerModel) {
        String serviceKey = getServiceKey(providerModel);
        Set<String> toolNames = serviceToToolsMapping.remove(serviceKey);

        if (toolNames == null || toolNames.isEmpty()) {
            return;
        }

        int unregisteredCount = 0;
        for (String toolName : toolNames) {
            try {
                McpServerFeatures.AsyncToolSpecification toolSpec = registeredTools.remove(toolName);
                if (toolSpec != null) {
                    mcpServer.removeTool(toolName).block();
                    unregisteredCount++;
                }
            } catch (Exception e) {
                logger.error(
                        LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                        "",
                        "",
                        "Failed to unregister MCP tool: " + toolName,
                        e);
            }
        }

        if (unregisteredCount > 0) {
            logger.info(
                    "Unregistered {} MCP tools for service: {}",
                    unregisteredCount,
                    providerModel.getServiceModel().getInterfaceName());
        }
    }

    private String getServiceKey(ProviderModel providerModel) {
        return providerModel.getServiceKey();
    }

    private String registerMethodAsTool(
            ProviderModel providerModel, Method method, URL url, McpServiceFilter.McpToolConfig toolConfig) {
        try {
            String toolName = toolConfig.getToolName();
            if (toolName == null || toolName.isEmpty()) {
                toolName = method.getName();
            }

            if (registeredTools.containsKey(toolName)) {
                return null;
            }

            String description = toolConfig.getDescription();
            if (description == null || description.isEmpty()) {
                description = generateDefaultDescription(method, providerModel);
            }

            McpSchema.Tool mcpTool = new McpSchema.Tool(toolName, description, generateToolSchema(method));

            McpServerFeatures.AsyncToolSpecification toolSpec =
                    createMethodToolSpecification(mcpTool, providerModel, method, url);

            mcpServer.addTool(toolSpec).block();
            registeredTools.put(toolName, toolSpec);

            return toolName;

        } catch (Exception e) {
            logger.error(
                    LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                    "",
                    "",
                    "Failed to register method as MCP tool: " + method.getName(),
                    e);
            return null;
        }
    }

    private Set<String> registerServiceLevelTools(ProviderModel providerModel, URL url) {
        ServiceDescriptor serviceDescriptor = providerModel.getServiceModel();
        Set<String> toolNames = new HashSet<>();

        McpServiceFilter.McpToolConfig serviceConfig = mcpServiceFilter.getMcpToolConfig(providerModel);

        Map<String, McpSchema.Tool> tools = toolConverter.convertToTools(serviceDescriptor, url, serviceConfig);

        if (tools.isEmpty()) {
            return toolNames;
        }

        for (Map.Entry<String, McpSchema.Tool> entry : tools.entrySet()) {
            McpSchema.Tool tool = entry.getValue();
            String toolId = tool.name();

            if (registeredTools.containsKey(toolId)) {
                continue;
            }

            try {
                Operation operation = toolConverter.getOperationByToolName(toolId);
                if (operation == null) {
                    logger.error(
                            LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                            "",
                            "",
                            "Could not find Operation metadata for tool: " + tool + ". Skipping registration");
                    continue;
                }

                McpServerFeatures.AsyncToolSpecification toolSpec =
                        createServiceToolSpecification(tool, operation, url);
                mcpServer.addTool(toolSpec).block();
                registeredTools.put(toolId, toolSpec);
                toolNames.add(toolId);

            } catch (Exception e) {
                logger.error(
                        LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                        "",
                        "",
                        "Failed to register MCP tool: " + toolId,
                        e);
            }
        }

        return toolNames;
    }

    private McpServerFeatures.AsyncToolSpecification createMethodToolSpecification(
            McpSchema.Tool mcpTool, ProviderModel providerModel, Method method, URL url) {

        final String interfaceName = providerModel.getServiceModel().getInterfaceName();
        final String methodName = method.getName();
        final Class<?>[] parameterClasses = method.getParameterTypes();
        final List<String> orderedJavaParameterNames = getStrings(method);
        final String group = url.getGroup();
        final String version = url.getVersion();

        return getAsyncToolSpecification(
                mcpTool, interfaceName, methodName, parameterClasses, orderedJavaParameterNames, group, version);
    }

    private McpServerFeatures.AsyncToolSpecification getAsyncToolSpecification(
            McpSchema.Tool mcpTool,
            String interfaceName,
            String methodName,
            Class<?>[] parameterClasses,
            List<String> orderedJavaParameterNames,
            String group,
            String version) {
        BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> callFunction =
                (exchange, mcpProvidedParameters) -> {
                    try {
                        Object result = genericCaller.execute(
                                interfaceName,
                                methodName,
                                orderedJavaParameterNames,
                                parameterClasses,
                                mcpProvidedParameters,
                                group,
                                version);
                        String resultJson = (result != null) ? result.toString() : "null";
                        return Mono.just(new McpSchema.CallToolResult(resultJson, true));
                    } catch (Exception e) {
                        logger.error(
                                LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                                "",
                                "",
                                String.format(
                                        "Error executing tool %s (interface: %s, method: %s): %s",
                                        mcpTool.name(), interfaceName, methodName, e.getMessage()),
                                e);
                        return Mono.just(
                                new McpSchema.CallToolResult("Tool execution failed: " + e.getMessage(), false));
                    }
                };

        return new McpServerFeatures.AsyncToolSpecification(mcpTool, callFunction);
    }

    private static List<String> getStrings(Method method) {
        final List<String> orderedJavaParameterNames = new ArrayList<>();
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            java.lang.reflect.Parameter parameter = parameters[i];
            String paramName;
            McpToolParam mcpToolParam = parameter.getAnnotation(McpToolParam.class);
            if (mcpToolParam != null && !mcpToolParam.name().isEmpty()) {
                paramName = mcpToolParam.name();
            } else if (parameter.isNamePresent()) {
                paramName = parameter.getName();
            } else {
                paramName = McpConstant.DEFAULT_TOOL_NAME_PREFIX + i;
            }
            orderedJavaParameterNames.add(paramName);
        }
        return orderedJavaParameterNames;
    }

    private McpServerFeatures.AsyncToolSpecification createServiceToolSpecification(
            McpSchema.Tool mcpTool, Operation operation, URL url) {

        final MethodMeta methodMeta = operation.getMeta();
        if (methodMeta == null) {
            throw new IllegalStateException("MethodMeta not found in Operation for tool: " + mcpTool.name());
        }
        final ServiceMeta serviceMeta = methodMeta.getServiceMeta();
        final String interfaceName = serviceMeta.getServiceInterface();
        final String methodName = methodMeta.getMethod().getName();
        final Class<?>[] parameterClasses = methodMeta.getMethod().getParameterTypes();

        final List<String> orderedJavaParameterNames = new ArrayList<>();
        if (methodMeta.getParameters() != null) {
            for (ParameterMeta javaParamMeta : methodMeta.getParameters()) {
                orderedJavaParameterNames.add(javaParamMeta.getName());
            }
        }

        final String group =
                serviceMeta.getUrl() != null ? serviceMeta.getUrl().getGroup() : (url != null ? url.getGroup() : null);
        final String version = serviceMeta.getUrl() != null
                ? serviceMeta.getUrl().getVersion()
                : (url != null ? url.getVersion() : null);

        return getAsyncToolSpecification(
                mcpTool, interfaceName, methodName, parameterClasses, orderedJavaParameterNames, group, version);
    }

    private String generateDefaultDescription(Method method, ProviderModel providerModel) {
        return String.format(
                McpConstant.DEFAULT_TOOL_DESCRIPTION_TEMPLATE,
                method.getName(),
                providerModel.getServiceModel().getInterfaceName());
    }

    private String generateToolSchema(Method method) {
        Map<String, Object> schemaMap = new HashMap<>();
        schemaMap.put(McpConstant.SCHEMA_PROPERTY_TYPE, JsonSchemaType.OBJECT_SCHEMA.getJsonSchemaType());

        Map<String, Object> properties = new HashMap<>();
        List<String> requiredParams = new ArrayList<>();

        generateSchemaFromMethodSignature(method, properties, requiredParams);

        schemaMap.put(McpConstant.SCHEMA_PROPERTY_PROPERTIES, properties);

        if (!requiredParams.isEmpty()) {
            schemaMap.put(McpConstant.SCHEMA_PROPERTY_REQUIRED, requiredParams);
        }

        try {
            return objectMapper.writeValueAsString(schemaMap);
        } catch (Exception e) {
            logger.error(
                    LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                    "",
                    "",
                    "Failed to generate tool schema for method " + method.getName() + ": " + e.getMessage(),
                    e);
            return "{\"type\":\"object\",\"properties\":{}}";
        }
    }

    private void generateSchemaFromMethodSignature(
            Method method, Map<String, Object> properties, List<String> requiredParams) {
        Class<?>[] paramTypes = method.getParameterTypes();
        java.lang.reflect.Type[] genericTypes = method.getGenericParameterTypes();
        java.lang.annotation.Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < paramTypes.length; i++) {
            String paramName = null;
            String paramDescription = null;
            boolean isRequired = false;

            for (java.lang.annotation.Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof McpToolParam mcpToolParam) {
                    if (!mcpToolParam.name().isEmpty()) {
                        paramName = mcpToolParam.name();
                    }
                    if (!mcpToolParam.description().isEmpty()) {
                        paramDescription = mcpToolParam.description();
                    }
                    isRequired = mcpToolParam.required();
                    break;
                }
            }

            if (paramName == null) {
                paramName = getParameterName(method, i);
                if (paramName == null || paramName.isEmpty()) {
                    paramName = McpConstant.DEFAULT_TOOL_NAME_PREFIX + i;
                }
            }

            if (paramDescription == null) {
                paramDescription = String.format(
                        McpConstant.DEFAULT_PARAMETER_DESCRIPTION_TEMPLATE, i, paramTypes[i].getSimpleName());
            }

            TypeSchemaUtils.TypeSchemaInfo schemaInfo =
                    TypeSchemaUtils.resolveTypeSchema(paramTypes[i], genericTypes[i], paramDescription);

            properties.put(paramName, TypeSchemaUtils.toSchemaMap(schemaInfo));

            if (isRequired) {
                requiredParams.add(paramName);
            }
        }
    }

    private String getParameterName(Method method, int index) {
        if (method.getParameters().length > index) {
            return method.getParameters()[index].getName();
        }
        return null;
    }

    public void clearRegistry() {
        for (String toolId : registeredTools.keySet()) {
            try {
                mcpServer.removeTool(toolId).block();
            } catch (Exception e) {
                logger.error(
                        LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                        "",
                        "",
                        "Failed to unregister MCP tool: " + toolId,
                        e);
            }
        }
        registeredTools.clear();
    }
}
