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

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GetConfigTest {
    @Test
    void testAll(){
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel1 = frameworkModel.newApplication();

        applicationModel1.getApplicationConfigManager().setApplication(new ApplicationConfig("app1"));
        applicationModel1.getApplicationConfigManager().addProtocol(new ProtocolConfig("dubbo", 12345));
        applicationModel1.getApplicationConfigManager().addRegistry(new RegistryConfig("zookeeper://127.0.0.1:2181"));
        applicationModel1.getApplicationConfigManager().addMetadataReport(new MetadataReportConfig("zookeeper://127.0.0.1:2181"));
        ConfigCenterConfig configCenterConfig = new ConfigCenterConfig();
        configCenterConfig.setAddress("zookeeper://127.0.0.1:2181");
        applicationModel1.getApplicationConfigManager().addConfigCenter(configCenterConfig);
        applicationModel1.getApplicationConfigManager().setMetrics(new MetricsConfig());
        applicationModel1.getApplicationConfigManager().setMonitor(new MonitorConfig());
        applicationModel1.getApplicationConfigManager().setSsl(new SslConfig());

        ModuleModel moduleModel = applicationModel1.newModule();
        moduleModel.getConfigManager().setModule(new ModuleConfig());
        moduleModel.getConfigManager().addConsumer(new ConsumerConfig());
        moduleModel.getConfigManager().addProvider(new ProviderConfig());
        ReferenceConfig<Object> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(MetadataService.class);
        moduleModel.getConfigManager().addReference(referenceConfig);
        ServiceConfig<Object> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(MetadataService.class);
        moduleModel.getConfigManager().addService(serviceConfig);

        CommandContext commandContext = new CommandContext("getConfig");
        commandContext.setHttp(true);

        Assertions.assertNotNull(new GetConfig(frameworkModel).execute(commandContext, null));
    }

    @Test
    void testFilter1(){
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel1 = frameworkModel.newApplication();

        applicationModel1.getApplicationConfigManager().setApplication(new ApplicationConfig("app1"));
        applicationModel1.getApplicationConfigManager().addProtocol(new ProtocolConfig("dubbo", 12345));
        applicationModel1.getApplicationConfigManager().addRegistry(new RegistryConfig("zookeeper://127.0.0.1:2181"));
        applicationModel1.getApplicationConfigManager().addMetadataReport(new MetadataReportConfig("zookeeper://127.0.0.1:2181"));
        ConfigCenterConfig configCenterConfig = new ConfigCenterConfig();
        configCenterConfig.setAddress("zookeeper://127.0.0.1:2181");
        applicationModel1.getApplicationConfigManager().addConfigCenter(configCenterConfig);
        applicationModel1.getApplicationConfigManager().setMetrics(new MetricsConfig());
        applicationModel1.getApplicationConfigManager().setMonitor(new MonitorConfig());
        applicationModel1.getApplicationConfigManager().setSsl(new SslConfig());

        ModuleModel moduleModel = applicationModel1.newModule();
        moduleModel.getConfigManager().setModule(new ModuleConfig());
        moduleModel.getConfigManager().addConsumer(new ConsumerConfig());
        moduleModel.getConfigManager().addProvider(new ProviderConfig());
        ReferenceConfig<Object> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(MetadataService.class);
        moduleModel.getConfigManager().addReference(referenceConfig);
        ServiceConfig<Object> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(MetadataService.class);
        moduleModel.getConfigManager().addService(serviceConfig);

        CommandContext commandContext = new CommandContext("getConfig");
        commandContext.setHttp(true);

        Assertions.assertNotNull(new GetConfig(frameworkModel).execute(commandContext, new String[]{"ApplicationConfig"}));
    }

    @Test
    void testFilter2(){
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel1 = frameworkModel.newApplication();

        applicationModel1.getApplicationConfigManager().setApplication(new ApplicationConfig("app1"));
        applicationModel1.getApplicationConfigManager().addProtocol(new ProtocolConfig("dubbo", 12345));
        applicationModel1.getApplicationConfigManager().addRegistry(new RegistryConfig("zookeeper://127.0.0.1:2181"));
        applicationModel1.getApplicationConfigManager().addMetadataReport(new MetadataReportConfig("zookeeper://127.0.0.1:2181"));
        ConfigCenterConfig configCenterConfig = new ConfigCenterConfig();
        configCenterConfig.setAddress("zookeeper://127.0.0.1:2181");
        applicationModel1.getApplicationConfigManager().addConfigCenter(configCenterConfig);
        applicationModel1.getApplicationConfigManager().setMetrics(new MetricsConfig());
        applicationModel1.getApplicationConfigManager().setMonitor(new MonitorConfig());
        applicationModel1.getApplicationConfigManager().setSsl(new SslConfig());

        ModuleModel moduleModel = applicationModel1.newModule();
        moduleModel.getConfigManager().setModule(new ModuleConfig());
        moduleModel.getConfigManager().addConsumer(new ConsumerConfig());
        moduleModel.getConfigManager().addProvider(new ProviderConfig());
        ReferenceConfig<Object> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(MetadataService.class);
        moduleModel.getConfigManager().addReference(referenceConfig);
        ServiceConfig<Object> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(MetadataService.class);
        moduleModel.getConfigManager().addService(serviceConfig);

        CommandContext commandContext = new CommandContext("getConfig");
        commandContext.setHttp(true);

        Assertions.assertNotNull(new GetConfig(frameworkModel).execute(commandContext, new String[]{"ProtocolConfig", "dubbo"}));
    }
}
