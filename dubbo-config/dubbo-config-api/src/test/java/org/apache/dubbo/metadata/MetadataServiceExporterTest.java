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
import org.apache.dubbo.config.metadata.ConfigurableMetadataServiceExporter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.apache.dubbo.common.constants.CommonConstants.COMPOSITE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MetadataServiceExporter} Test-Cases
 *
 * @since 2.7.8
 */
public class MetadataServiceExporterTest {

    @BeforeAll
    public static void init() {
        ApplicationModel.reset();
        ApplicationModel.getConfigManager().setApplication(new ApplicationConfig("Test"));
        ApplicationModel.getConfigManager().addRegistry(new RegistryConfig("test://1.2.3.4"));
        ApplicationModel.getConfigManager().addProtocol(new ProtocolConfig("injvm"));
    }

    @AfterAll
    public static void destroy() {
        ApplicationModel.getConfigManager().setApplication(null);
        ApplicationModel.reset();
    }

    @Test
    public void test() {
        MetadataService metadataService = Mockito.mock(MetadataService.class);
        MetadataServiceExporter exporter = new ConfigurableMetadataServiceExporter(metadataService);

        exporter.export();
        assertTrue(exporter.isExported());
        exporter.unexport();

        assertTrue(exporter.supports(DEFAULT_METADATA_STORAGE_TYPE));
        assertTrue(exporter.supports(REMOTE_METADATA_STORAGE_TYPE));
        assertTrue(exporter.supports(COMPOSITE_METADATA_STORAGE_TYPE));
    }
}
