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
package org.apache.dubbo.config.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.store.InMemoryWritableMetadataService;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ConfigurableMetadataServiceExporter} Test
 *
 * @since 2.7.4
 */
public class ConfigurableMetadataServiceExporterTest {

    @BeforeAll
    public static void init() {
        ConfigManager configManager = ConfigManager.getInstance();
        configManager.clear();
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("test");
        configManager.setApplication(applicationConfig);

        // Add ProtocolConfig
        configManager.addProtocol(protocolConfig());
        // Add RegistryConfig
        configManager.addRegistry(registryConfig());
    }

    private static ProtocolConfig protocolConfig() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName("mockprotocol");
        protocolConfig.setPort(20880);
        return protocolConfig;
    }

    private static RegistryConfig registryConfig() {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("mockregistry://127.0.0.1");
        return registryConfig;
    }

    @Test
    public void testExportAndUnexport() {
        ConfigurableMetadataServiceExporter exporter = new ConfigurableMetadataServiceExporter(new InMemoryWritableMetadataService());
        List<URL> urls = exporter.export().getExportedURLs();

        assertEquals(1, urls.size());

        URL url = urls.get(0);

        assertEquals("test", url.getParameter(APPLICATION_KEY));
        assertEquals(MetadataService.class.getName(), url.getServiceInterface());
        assertEquals("test", url.getParameter(GROUP_KEY));
        assertEquals(MetadataService.VERSION, url.getParameter(VERSION_KEY));
        assertEquals("mockprotocol", url.getProtocol());

        exporter.unexport();
    }

}
