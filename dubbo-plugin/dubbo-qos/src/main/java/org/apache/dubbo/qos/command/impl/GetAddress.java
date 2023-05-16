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
package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.Cmd;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.api.PermissionLevel;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.migration.MigrationInvoker;
import org.apache.dubbo.registry.client.migration.model.MigrationStep;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.directory.AbstractDirectory;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Cmd(name = "getAddress",
    summary = "Get service available address ",
    example = {"getAddress com.example.DemoService", "getAddress group/com.example.DemoService"},
    requiredPermissionLevel = PermissionLevel.PRIVATE)
public class GetAddress implements BaseCommand {
    public final FrameworkServiceRepository serviceRepository;

    public GetAddress(FrameworkModel frameworkModel) {
        this.serviceRepository = frameworkModel.getServiceRepository();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(CommandContext commandContext, String[] args) {
        if (args == null || args.length != 1) {
            return "Invalid parameters, please input like getAddress com.example.DemoService";
        }

        String serviceName = args[0];

        StringBuilder plainOutput = new StringBuilder();
        Map<String, Object> jsonOutput = new HashMap<>();

        for (ConsumerModel consumerModel : serviceRepository.allConsumerModels()) {
            if (serviceName.equals(consumerModel.getServiceKey())) {
                appendConsumer(plainOutput, jsonOutput, consumerModel);
            }
        }

        if (commandContext.isHttp()) {
            return JsonUtils.toJson(jsonOutput);
        } else {
            return plainOutput.toString();
        }
    }

    private static void appendConsumer(StringBuilder plainOutput, Map<String, Object> jsonOutput, ConsumerModel consumerModel) {
        plainOutput.append("ConsumerModel: ")
            .append(consumerModel.getServiceKey())
            .append("@")
            .append(Integer.toHexString(System.identityHashCode(consumerModel)))
            .append("\n\n");
        Map<String, Object> consumerMap = new HashMap<>();
        jsonOutput.put(consumerModel.getServiceKey() + "@" + Integer.toHexString(System.identityHashCode(consumerModel)), consumerMap);

        Object object = consumerModel.getServiceMetadata().getAttribute(CommonConstants.CURRENT_CLUSTER_INVOKER_KEY);
        Map<Registry, MigrationInvoker<?>> invokerMap;
        if (object instanceof Map) {
            invokerMap = (Map<Registry, MigrationInvoker<?>>) object;
            for (Map.Entry<Registry, MigrationInvoker<?>> entry : invokerMap.entrySet()) {
                appendInvokers(plainOutput, consumerMap, entry);
            }
        }
    }

    private static void appendInvokers(StringBuilder plainOutput, Map<String, Object> consumerMap, Map.Entry<Registry, MigrationInvoker<?>> entry) {
        URL registryUrl = entry.getKey().getUrl();

        plainOutput.append("Registry: ")
            .append(registryUrl)
            .append("\n");
        Map<String, Object> registryMap = new HashMap<>();
        consumerMap.put(registryUrl.toString(), registryMap);

        MigrationInvoker<?> migrationInvoker = entry.getValue();

        MigrationStep migrationStep = migrationInvoker.getMigrationStep();
        plainOutput.append("MigrationStep: ")
            .append(migrationStep)
            .append("\n\n");
        registryMap.put("MigrationStep", migrationStep);

        Map<String, Object> invokersMap = new HashMap<>();
        registryMap.put("Invokers", invokersMap);

        URL originConsumerUrl = RpcContext.getServiceContext().getConsumerUrl();
        RpcContext.getServiceContext().setConsumerUrl(migrationInvoker.getConsumerUrl());

        appendInterfaceLevel(plainOutput, migrationInvoker, invokersMap);
        appendAppLevel(plainOutput, migrationInvoker, invokersMap);

        RpcContext.getServiceContext().setConsumerUrl(originConsumerUrl);
    }

    private static void appendAppLevel(StringBuilder plainOutput, MigrationInvoker<?> migrationInvoker, Map<String, Object> invokersMap) {
        Map<String, Object> appMap = new HashMap<>();
        invokersMap.put("Application-Level", appMap);
        Optional.ofNullable(migrationInvoker.getServiceDiscoveryInvoker())
            .ifPresent(i -> plainOutput.append("Application-Level: \n"));
        Optional.ofNullable(migrationInvoker.getServiceDiscoveryInvoker())
            .map(ClusterInvoker::getDirectory)
            .map(Directory::getAllInvokers)
            .ifPresent(invokers -> {
                List<String> invokerUrls = new LinkedList<>();
                plainOutput.append("All Invokers: \n");
                for (org.apache.dubbo.rpc.Invoker<?> invoker : invokers) {
                    invokerUrls.add(invoker.getUrl().toFullString());
                    plainOutput.append(invoker.getUrl().toFullString()).append("\n");
                }
                plainOutput.append("\n");
                appMap.put("All", invokerUrls);
            });
        Optional.ofNullable(migrationInvoker.getServiceDiscoveryInvoker())
            .map(ClusterInvoker::getDirectory)
            .map(s -> (AbstractDirectory<?>) s)
            .map(AbstractDirectory::getValidInvokers)
            .ifPresent(invokers -> {
                List<String> invokerUrls = new LinkedList<>();
                plainOutput.append("Valid Invokers: \n");
                for (org.apache.dubbo.rpc.Invoker<?> invoker : invokers) {
                    invokerUrls.add(invoker.getUrl().toFullString());
                    plainOutput.append(invoker.getUrl().toFullString()).append("\n");
                }
                plainOutput.append("\n");
                appMap.put("Valid", invokerUrls);
            });
        Optional.ofNullable(migrationInvoker.getServiceDiscoveryInvoker())
            .map(ClusterInvoker::getDirectory)
            .map(s -> (AbstractDirectory<?>) s)
            .map(AbstractDirectory::getDisabledInvokers)
            .ifPresent(invokers -> {
                List<String> invokerUrls = new LinkedList<>();
                plainOutput.append("Disabled Invokers: \n");
                for (org.apache.dubbo.rpc.Invoker<?> invoker : invokers) {
                    invokerUrls.add(invoker.getUrl().toFullString());
                    plainOutput.append(invoker.getUrl().toFullString()).append("\n");
                }
                plainOutput.append("\n");
                appMap.put("Disabled", invokerUrls);
            });
    }

    private static void appendInterfaceLevel(StringBuilder plainOutput, MigrationInvoker<?> migrationInvoker, Map<String, Object> invokersMap) {
        Map<String, Object> interfaceMap = new HashMap<>();
        invokersMap.put("Interface-Level", interfaceMap);
        Optional.ofNullable(migrationInvoker.getInvoker())
            .ifPresent(i -> plainOutput.append("Interface-Level: \n"));
        Optional.ofNullable(migrationInvoker.getInvoker())
            .map(ClusterInvoker::getDirectory)
            .map(Directory::getAllInvokers)
            .ifPresent(invokers -> {
                List<String> invokerUrls = new LinkedList<>();
                plainOutput.append("All Invokers: \n");
                for (org.apache.dubbo.rpc.Invoker<?> invoker : invokers) {
                    invokerUrls.add(invoker.getUrl().toFullString());
                    plainOutput.append(invoker.getUrl().toFullString()).append("\n");
                }
                plainOutput.append("\n");
                interfaceMap.put("All", invokerUrls);
            });
        Optional.ofNullable(migrationInvoker.getInvoker())
            .map(ClusterInvoker::getDirectory)
            .map(s -> (AbstractDirectory<?>) s)
            .map(AbstractDirectory::getValidInvokers)
            .ifPresent(invokers -> {
                List<String> invokerUrls = new LinkedList<>();
                plainOutput.append("Valid Invokers: \n");
                for (org.apache.dubbo.rpc.Invoker<?> invoker : invokers) {
                    invokerUrls.add(invoker.getUrl().toFullString());
                    plainOutput.append(invoker.getUrl().toFullString()).append("\n");
                }
                plainOutput.append("\n");
                interfaceMap.put("Valid", invokerUrls);
            });
        Optional.ofNullable(migrationInvoker.getInvoker())
            .map(ClusterInvoker::getDirectory)
            .map(s -> (AbstractDirectory<?>) s)
            .map(AbstractDirectory::getDisabledInvokers)
            .ifPresent(invokers -> {
                List<String> invokerUrls = new LinkedList<>();
                plainOutput.append("Disabled Invokers: \n");
                for (org.apache.dubbo.rpc.Invoker<?> invoker : invokers) {
                    invokerUrls.add(invoker.getUrl().toFullString());
                    plainOutput.append(invoker.getUrl().toFullString()).append("\n");
                }
                plainOutput.append("\n");
                interfaceMap.put("Disabled", invokerUrls);
            });
    }
}
