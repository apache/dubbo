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
import org.apache.dubbo.metadata.MetadataServiceExporter;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.service.EchoService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.COMPOSITE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.metadata.MetadataServiceExporter.getExtension;
import static org.apache.dubbo.metadata.report.support.Constants.SYNC_REPORT_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RemoteMetadataServiceExporter} Test-Cases
 *
 * @since 2.7.8
 */
public class RemoteMetadataServiceExporterTest {

    private static final URL METADATA_REPORT_URL = URL.valueOf("file://")
            .addParameter(APPLICATION_KEY, "test")
            .addParameter(SYNC_REPORT_KEY, "true");

    private static final Class<EchoService> INTERFACE_CLASS = EchoService.class;

    private static final String INTERFACE_NAME = INTERFACE_CLASS.getName();

    private static final String APP_NAME = "test-service";

    private static final URL BASE_URL = URL
            .valueOf("dubbo://127.0.0.1:20880")
            .setPath(INTERFACE_NAME)
            .addParameter(APPLICATION_KEY, APP_NAME)
            .addParameter(SIDE_KEY, "provider");

    private final MetadataServiceExporter exporter = getExtension(REMOTE_METADATA_STORAGE_TYPE);

    private WritableMetadataService writableMetadataService;

    @BeforeEach
    public void init() {
        ApplicationModel.getConfigManager().setApplication(new ApplicationConfig(APP_NAME));
        MetadataReportInstance.init(METADATA_REPORT_URL);
        writableMetadataService = WritableMetadataService.getDefaultExtension();
        writableMetadataService.exportURL(BASE_URL);
    }

    @AfterEach
    public void reset() {
        ApplicationModel.reset();
    }

    @Test
    public void testType() {
        assertEquals(RemoteMetadataServiceExporter.class, exporter.getClass());
    }

    @Test
    public void testSupports() {
        assertTrue(exporter.supports(REMOTE_METADATA_STORAGE_TYPE));
        assertTrue(exporter.supports(COMPOSITE_METADATA_STORAGE_TYPE));
        assertFalse(exporter.supports(DEFAULT_METADATA_STORAGE_TYPE));
    }

    @Test
    public void testExportAndUnexport() {
        assertFalse(exporter.isExported());
        assertEquals(exporter, exporter.export());
        assertTrue(exporter.isExported());

        assertEquals(asList(BASE_URL), exporter.getExportedURLs());

        assertEquals(exporter, exporter.unexport());
        assertFalse(exporter.isExported());
    }
}
