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
package org.apache.dubbo.config.event.listener;

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.bootstrap.EchoService;
import org.apache.dubbo.config.bootstrap.EchoServiceImpl;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.event.ServiceConfigExportedEvent;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;
import org.apache.dubbo.rpc.model.ApplicationModel;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link PublishingServiceDefinitionListener} Test-Cases
 *
 * @since 2.7.8
 */
public class PublishingServiceDefinitionListenerTest {

    private WritableMetadataService writableMetadataService;

    @BeforeEach
    public void init() {
        DubboBootstrap.reset();
        String metadataType = DEFAULT_METADATA_STORAGE_TYPE;
        ConfigManager configManager = ApplicationModel.getConfigManager();
        ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-demo-provider");
        applicationConfig.setMetadataType(metadataType);
        configManager.setApplication(applicationConfig);
        this.writableMetadataService = WritableMetadataService.getDefaultExtension();
    }

    @AfterEach
    public void reset() {
        DubboBootstrap.reset();
    }

    /**
     * Test {@link ServiceConfigExportedEvent} arising
     */
    @Test
    public void testOnServiceConfigExportedEvent() {
        ServiceConfig<EchoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(EchoService.class);
        serviceConfig.setRef(new EchoServiceImpl());
        serviceConfig.setRegistry(new RegistryConfig("N/A"));
        serviceConfig.setProtocol(new ProtocolConfig("dubbo", NetUtils.getAvailablePort(20880 + new Random().nextInt(10000))));
        serviceConfig.export();

        String serviceDefinition = writableMetadataService.getServiceDefinition(EchoService.class.getName());

        ServiceDefinition serviceDefinitionBuild = ServiceDefinitionBuilder.build(serviceConfig.getInterfaceClass());

        assertEquals(serviceDefinition, JSON.toJSONString(serviceDefinitionBuild));
        serviceConfig.unexport();
    }
}
