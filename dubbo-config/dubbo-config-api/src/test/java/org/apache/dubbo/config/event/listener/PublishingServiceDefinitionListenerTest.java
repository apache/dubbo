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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.EchoService;
import org.apache.dubbo.config.bootstrap.EchoServiceImpl;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.event.ServiceConfigExportedEvent;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.rpc.model.ApplicationModel;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder.buildFullDefinition;
import static org.apache.dubbo.remoting.Constants.BIND_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;
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
        String metadataType = DEFAULT_METADATA_STORAGE_TYPE;
        ConfigManager configManager = ApplicationModel.getConfigManager();
        ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-demo-provider");
        applicationConfig.setMetadataType(metadataType);
        configManager.setApplication(applicationConfig);
        this.writableMetadataService = WritableMetadataService.getExtension(metadataType);
    }

    @AfterEach
    public void reset() {
        ApplicationModel.reset();
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
        serviceConfig.export();

        String serviceDefinition = writableMetadataService.getServiceDefinition(EchoService.class.getName());

        List<URL> exportedUrls = serviceConfig.getExportedUrls();

        FullServiceDefinition fullServiceDefinition = buildFullDefinition(
                serviceConfig.getInterfaceClass(),
                exportedUrls.get(0)
                        .removeParameters(PID_KEY, TIMESTAMP_KEY, BIND_IP_KEY, BIND_PORT_KEY, TIMESTAMP_KEY)
                        .getParameters()
        );

        assertEquals(serviceDefinition, new Gson().toJson(fullServiceDefinition));
    }
}
