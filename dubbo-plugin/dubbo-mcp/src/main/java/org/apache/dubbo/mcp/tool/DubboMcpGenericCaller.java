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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION;

public class DubboMcpGenericCaller {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(DubboMcpGenericCaller.class);

    private final ApplicationConfig applicationConfig;

    private final Map<String, GenericService> serviceCache = new ConcurrentHashMap<>();

    public DubboMcpGenericCaller(ApplicationModel applicationModel) {
        if (applicationModel == null) {
            logger.error(
                    COMMON_UNEXPECTED_EXCEPTION, "", "", "ApplicationModel cannot be null for DubboMcpGenericCaller.");
            throw new IllegalArgumentException("ApplicationModel cannot be null.");
        }
        this.applicationConfig = applicationModel.getCurrentConfig();
        if (this.applicationConfig == null) {

            String errMsg = "ApplicationConfig is null in the provided ApplicationModel. Application Name: "
                    + (applicationModel.getApplicationName() != null ? applicationModel.getApplicationName() : "N/A");
            logger.error(COMMON_UNEXPECTED_EXCEPTION, "", "", errMsg);
            throw new IllegalStateException(errMsg);
        }
    }

    public Object execute(
            String interfaceName,
            String methodName,
            List<String> orderedJavaParameterNames,
            Class<?>[] parameterJavaTypes,
            Map<String, Object> mcpProvidedParameters,
            String group,
            String version) {
        String cacheKey = interfaceName + ":" + (group == null ? "" : group) + ":" + (version == null ? "" : version);
        GenericService genericService = serviceCache.get(cacheKey);
        if (genericService == null) {
            ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
            reference.setApplication(this.applicationConfig);
            reference.setInterface(interfaceName);
            reference.setGeneric("true"); // Defaults to 'bean' or 'true' for POJO generalization.
            reference.setScope("local");
            if (group != null && !group.isEmpty()) {
                reference.setGroup(group);
            }
            if (version != null && !version.isEmpty()) {
                reference.setVersion(version);
            }

            try {
                genericService = reference.get();
                if (genericService != null) {
                    serviceCache.put(cacheKey, genericService);
                } else {
                    String errorMessage = "Failed to obtain GenericService instance for " + interfaceName
                            + (group != null ? " group " + group : "") + (version != null ? " version " + version : "");
                    logger.error(COMMON_UNEXPECTED_EXCEPTION, "", "", errorMessage);
                    throw new IllegalStateException(errorMessage);
                }
            } catch (Exception e) {
                String errorMessage = "Error obtaining GenericService for " + interfaceName + ": " + e.getMessage();
                logger.error(COMMON_UNEXPECTED_EXCEPTION, "", "", errorMessage, e);
                throw new RuntimeException(errorMessage, e);
            }
        }

        String[] invokeParameterTypes = new String[parameterJavaTypes.length];
        for (int i = 0; i < parameterJavaTypes.length; i++) {
            invokeParameterTypes[i] = parameterJavaTypes[i].getName();
        }

        Object[] invokeArgs = new Object[orderedJavaParameterNames.size()];
        for (int i = 0; i < orderedJavaParameterNames.size(); i++) {
            String paramName = orderedJavaParameterNames.get(i);
            if (mcpProvidedParameters.containsKey(paramName)) {
                invokeArgs[i] = mcpProvidedParameters.get(paramName);
            } else {
                invokeArgs[i] = null;
                logger.warn(
                        COMMON_UNEXPECTED_EXCEPTION,
                        "",
                        "",
                        "Parameter '" + paramName + "' not found in MCP provided parameters for method '" + methodName
                                + "' of interface '" + interfaceName + "'. Will use null.");
            }
        }

        try {
            return genericService.$invoke(methodName, invokeParameterTypes, invokeArgs);
        } catch (Exception e) {
            String errorMessage = "GenericService $invoke failed for method '" + methodName + "' on interface '"
                    + interfaceName + "': " + e.getMessage();
            logger.error(COMMON_UNEXPECTED_EXCEPTION, "", "", errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }
}
