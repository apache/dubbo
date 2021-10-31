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
package org.apache.dubbo.metadata;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.metadata.ConfigurableMetadataServiceExporter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.apache.dubbo.common.constants.CommonConstants.COMPOSITE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MetadataServiceExporter} Test-Cases
 *
 * @since 2.7.8
 */
public class MetadataServiceExporterTest {

    @BeforeEach
    public void init() {
        DubboBootstrap.reset();

        ApplicationConfig applicationConfig = new ApplicationConfig("Test");
        applicationConfig.setRegisterConsumer(true);
        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(applicationConfig);
        ApplicationModel.defaultModel().getApplicationConfigManager().addRegistry(new RegistryConfig("multicast://224.5.6.7:1234"));
        ApplicationModel.defaultModel().getApplicationConfigManager().addProtocol(new ProtocolConfig("injvm"));
    }

    @Test
    public void test() {
        MetadataService metadataService = Mockito.mock(MetadataService.class);
        ConfigurableMetadataServiceExporter exporter = new ConfigurableMetadataServiceExporter();
        exporter.setMetadataService(metadataService);
        exporter.setApplicationModel(ApplicationModel.defaultModel());

        exporter.export();
        assertTrue(exporter.isExported());
        exporter.unexport();

        assertTrue(exporter.supports(DEFAULT_METADATA_STORAGE_TYPE));
        assertTrue(exporter.supports(REMOTE_METADATA_STORAGE_TYPE));
        assertTrue(exporter.supports(COMPOSITE_METADATA_STORAGE_TYPE));
    }

    @Test
    public void test2() throws Exception {

        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ConfigurableMetadataServiceExporter exporter = (ConfigurableMetadataServiceExporter) applicationModel.getExtensionLoader(MetadataServiceExporter.class).getDefaultExtension();
        MetadataService metadataService = Mockito.mock(MetadataService.class);
        exporter.setMetadataService(metadataService);

        applicationModel.getDeployer().start().get();
        assertTrue(exporter.isExported());
        assertTrue(exporter.supports(DEFAULT_METADATA_STORAGE_TYPE));
        assertTrue(exporter.supports(REMOTE_METADATA_STORAGE_TYPE));
        assertTrue(exporter.supports(COMPOSITE_METADATA_STORAGE_TYPE));

        applicationModel.getDeployer().stop();
        assertFalse(exporter.isExported());
    }

}
