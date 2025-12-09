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

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ServiceListener;
import org.apache.dubbo.mcp.tool.DubboServiceToolRegistry;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class McpServiceExportListener implements ServiceListener {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(McpServiceExportListener.class);

    private final Map<String, RegisteredServiceInfo> registeredServiceTools = new ConcurrentHashMap<>();

    private static class RegisteredServiceInfo {
        final int toolCount;
        final String interfaceName;
        final ProviderModel providerModel;

        RegisteredServiceInfo(int toolCount, String interfaceName, ProviderModel providerModel) {
            this.toolCount = toolCount;
            this.interfaceName = interfaceName;
            this.providerModel = providerModel;
        }
    }

    @Override
    public void exported(ServiceConfig sc) {
        try {
            if (sc.getRef() == null) {
                return;
            }

            String serviceKey = sc.getUniqueServiceName();
            ProviderModel providerModel =
                    sc.getScopeModel().getServiceRepository().lookupExportedService(serviceKey);

            if (providerModel == null) {
                logger.warn(
                        LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                        "",
                        "",
                        "ProviderModel not found for service: " + sc.getInterface() + " with key: " + serviceKey);
                return;
            }

            DubboServiceToolRegistry toolRegistry = getToolRegistry(sc);
            if (toolRegistry == null) {
                return;
            }

            int registeredCount = toolRegistry.registerService(providerModel);

            if (registeredCount > 0) {
                registeredServiceTools.put(
                        serviceKey,
                        new RegisteredServiceInfo(
                                registeredCount, providerModel.getServiceModel().getInterfaceName(), providerModel));
                logger.info(
                        "Dynamically registered {} MCP tools for exported service: {}",
                        registeredCount,
                        providerModel.getServiceModel().getInterfaceName());
            }
        } catch (Exception e) {
            logger.error(
                    LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                    "",
                    "",
                    "Failed to register MCP tools for exported service: " + sc.getInterface(),
                    e);
        }
    }

    @Override
    public void unexported(ServiceConfig sc) {
        try {
            if (sc.getRef() == null) {
                return;
            }

            String serviceKey = sc.getUniqueServiceName();
            RegisteredServiceInfo serviceInfo = registeredServiceTools.remove(serviceKey);

            if (serviceInfo != null && serviceInfo.toolCount > 0) {
                DubboServiceToolRegistry toolRegistry = getToolRegistry(sc);
                if (toolRegistry == null) {
                    return;
                }

                toolRegistry.unregisterService(serviceInfo.providerModel);
                logger.info(
                        "Dynamically unregistered {} MCP tools for unexported service: {}",
                        serviceInfo.toolCount,
                        serviceInfo.interfaceName);
            }

        } catch (Exception e) {
            logger.error(
                    LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                    "",
                    "",
                    "Failed to unregister MCP tools for unexported service: " + sc.getInterface(),
                    e);
        }
    }

    private DubboServiceToolRegistry getToolRegistry(ServiceConfig sc) {
        try {
            ApplicationModel applicationModel = sc.getScopeModel().getApplicationModel();
            return applicationModel.getBeanFactory().getBean(DubboServiceToolRegistry.class);
        } catch (Exception e) {
            logger.warn(
                    LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                    "",
                    "",
                    "Failed to get DubboServiceToolRegistry from application context",
                    e);
            return null;
        }
    }
}
