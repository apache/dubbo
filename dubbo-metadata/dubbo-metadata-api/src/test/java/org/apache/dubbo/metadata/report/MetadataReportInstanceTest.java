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
package org.apache.dubbo.metadata.report;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class MetadataReportInstanceTest {
    private MetadataReportInstance metadataReportInstance;
    private MetadataReportConfig metadataReportConfig;
    private ConfigManager configManager;

    private String registryId = "9103";

    @BeforeEach
    public void setUp() {
        configManager = mock(ConfigManager.class);
        ApplicationModel applicationModel = spy(ApplicationModel.defaultModel());
        metadataReportInstance = new MetadataReportInstance(applicationModel);


        URL url = URL.valueOf("metadata://127.0.0.1:20880/TestService?version=1.0.0&metadata=JTest");
        metadataReportConfig = mock(MetadataReportConfig.class);
        when(metadataReportConfig.getApplicationModel()).thenReturn(applicationModel);
        when(metadataReportConfig.toUrl()).thenReturn(url);
        when(metadataReportConfig.getScopeModel()).thenReturn(applicationModel);
        when(metadataReportConfig.getRegistry()).thenReturn(registryId);

        when(configManager.getMetadataConfigs()).thenReturn(Collections.emptyList());
        when(applicationModel.getApplicationConfigManager()).thenReturn(configManager);
        when(applicationModel.getCurrentConfig()).thenReturn(new ApplicationConfig("test"));

    }

    @Test
    public void test() {
        Assertions.assertNull(metadataReportInstance.getMetadataReport(registryId),
            "the metadata report was not initialized.");
        assertThat(metadataReportInstance.getMetadataReports(true), Matchers.anEmptyMap());

        metadataReportInstance.init(Arrays.asList(metadataReportConfig));
        MetadataReport metadataReport = metadataReportInstance.getMetadataReport(registryId);
        Assertions.assertNotNull(metadataReport);

        MetadataReport metadataReport2 = metadataReportInstance.getMetadataReport(registryId + "NOT_EXIST");
        Assertions.assertEquals(metadataReport, metadataReport2);

        Map<String, MetadataReport> metadataReports = metadataReportInstance.getMetadataReports(true);
        Assertions.assertEquals(metadataReports.size(), 1);
        Assertions.assertEquals(metadataReports.get(registryId), metadataReport);
    }

}
