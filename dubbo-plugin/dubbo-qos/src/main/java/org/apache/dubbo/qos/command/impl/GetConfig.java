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

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfigBase;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.TracingConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.context.ModuleConfigManager;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.api.Cmd;
import org.apache.dubbo.qos.api.PermissionLevel;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Cmd(name = "getConfig",
    summary = "Get current running config.",
    example = {"getConfig ReferenceConfig com.example.DemoService", "getConfig ApplicationConfig"},
    requiredPermissionLevel = PermissionLevel.PRIVATE)
public class GetConfig implements BaseCommand {
    private final FrameworkModel frameworkModel;

    public GetConfig(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        boolean http = commandContext.isHttp();
        StringBuilder plainOutput = new StringBuilder();
        Map<String, Object> frameworkMap = new HashMap<>();

        appendFrameworkConfig(args, plainOutput, frameworkMap);

        if (http) {
            return JsonUtils.toJson(frameworkMap);
        } else {
            return plainOutput.toString();
        }
    }

    private void appendFrameworkConfig(String[] args, StringBuilder plainOutput, Map<String, Object> frameworkMap) {
        for (ApplicationModel applicationModel : frameworkModel.getApplicationModels()) {
            Map<String, Object> applicationMap = new HashMap<>();
            frameworkMap.put(applicationModel.getDesc(), applicationMap);
            plainOutput.append("ApplicationModel: ")
                .append(applicationModel.getDesc())
                .append("\n");

            ConfigManager configManager = applicationModel.getApplicationConfigManager();

            appendApplicationConfigs(args, plainOutput, applicationModel, applicationMap, configManager);
        }
    }

    private static void appendApplicationConfigs(String[] args, StringBuilder plainOutput, ApplicationModel applicationModel, Map<String, Object> applicationMap, ConfigManager configManager) {
        Optional<ApplicationConfig> applicationConfig = configManager.getApplication();
        applicationConfig.ifPresent(config -> appendConfig("ApplicationConfig", config.getName(), config, plainOutput, applicationMap, args));

        for (ProtocolConfig protocol : configManager.getProtocols()) {
            appendConfigs("ProtocolConfig", protocol.getName(), protocol, plainOutput, applicationMap, args);
        }

        for (RegistryConfig registry : configManager.getRegistries()) {
            appendConfigs("RegistryConfig", registry.getId(), registry, plainOutput, applicationMap, args);
        }

        for (MetadataReportConfig metadataConfig : configManager.getMetadataConfigs()) {
            appendConfigs("MetadataReportConfig", metadataConfig.getId(), metadataConfig, plainOutput, applicationMap, args);
        }

        for (ConfigCenterConfig configCenter : configManager.getConfigCenters()) {
            appendConfigs("ConfigCenterConfig", configCenter.getId(), configCenter, plainOutput, applicationMap, args);
        }

        Optional<MetricsConfig> metricsConfig = configManager.getMetrics();
        metricsConfig.ifPresent(config -> appendConfig("MetricsConfig", config.getId(), config, plainOutput, applicationMap, args));

        Optional<TracingConfig> tracingConfig = configManager.getTracing();
        tracingConfig.ifPresent(config -> appendConfig("TracingConfig", config.getId(), config, plainOutput, applicationMap, args));

        Optional<MonitorConfig> monitorConfig = configManager.getMonitor();
        monitorConfig.ifPresent(config -> appendConfig("MonitorConfig", config.getId(), config, plainOutput, applicationMap, args));

        Optional<SslConfig> sslConfig = configManager.getSsl();
        sslConfig.ifPresent(config -> appendConfig("SslConfig", config.getId(), config, plainOutput, applicationMap, args));

        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            Map<String, Object> moduleMap = new HashMap<>();
            applicationMap.put(moduleModel.getDesc(), moduleMap);
            plainOutput.append("ModuleModel: ")
                .append(moduleModel.getDesc())
                .append("\n");

            ModuleConfigManager moduleConfigManager = moduleModel.getConfigManager();

            appendModuleConfigs(args, plainOutput, moduleMap, moduleConfigManager);
        }
    }

    private static void appendModuleConfigs(String[] args, StringBuilder plainOutput, Map<String, Object> moduleMap, ModuleConfigManager moduleConfigManager) {
        for (ProviderConfig provider : moduleConfigManager.getProviders()) {
            appendConfigs("ProviderConfig", provider.getId(), provider, plainOutput, moduleMap, args);
        }

        for (ConsumerConfig consumer : moduleConfigManager.getConsumers()) {
            appendConfigs("ConsumerConfig", consumer.getId(), consumer, plainOutput, moduleMap, args);
        }

        Optional<ModuleConfig> moduleConfig = moduleConfigManager.getModule();
        moduleConfig.ifPresent(config -> appendConfig("ModuleConfig", config.getId(), config, plainOutput, moduleMap, args));

        for (ServiceConfigBase<?> service : moduleConfigManager.getServices()) {
            appendConfigs("ServiceConfig", service.getUniqueServiceName(), service, plainOutput, moduleMap, args);
        }

        for (ReferenceConfigBase<?> reference : moduleConfigManager.getReferences()) {
            appendConfigs("ReferenceConfig", reference.getUniqueServiceName(), reference, plainOutput, moduleMap, args);
        }
    }

    @SuppressWarnings("unchecked")
    private static void appendConfigs(String type, String id, Object config, StringBuilder plainOutput, Map<String, Object> map, String[] args) {
        if (!isMatch(type, id, args)) {
            return;
        }

        plainOutput.append(type).append(": ")
            .append(id)
            .append("\n")
            .append(config)
            .append("\n\n");

        Map<String, Object> typeMap = (Map<String, Object>) map.computeIfAbsent(type, k -> new HashMap<String, Object>());
        typeMap.put(id, config);
    }

    private static void appendConfig(String type, String id, Object config, StringBuilder plainOutput, Map<String, Object> map, String[] args) {
        if (!isMatch(type, id, args)) {
            return;
        }

        plainOutput.append(type).append(": ")
            .append(id)
            .append("\n")
            .append(config)
            .append("\n\n");

        map.put(type, config);
    }

    private static boolean isMatch(String type, String id, String[] args) {
        if (args == null) {
            return true;
        }
        switch (args.length) {
            case 1:
                if (!Objects.equals(args[0], type)) {
                    return false;
                }
                break;
            case 2:
                if (!Objects.equals(args[0], type) || !Objects.equals(args[1], id)) {
                    return false;
                }
                break;
            default:
        }
        return true;
    }
}
