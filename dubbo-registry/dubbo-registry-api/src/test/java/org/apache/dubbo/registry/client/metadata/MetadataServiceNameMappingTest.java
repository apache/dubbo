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
package org.apache.dubbo.registry.client.metadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class MetadataServiceNameMappingTest {

    private MetadataServiceNameMapping mapping;
    private URL url;
    private ConfigManager configManager;
    private MetadataReport metadataReport;
    private ApplicationModel applicationModel;
    private Map<String, MetadataReport> metadataReportList = new HashMap<>();

    @BeforeEach
    public void setUp() {
        applicationModel = ApplicationModel.defaultModel();
        configManager = mock(ConfigManager.class);
        metadataReport = mock(MetadataReport.class);
        metadataReportList.put("default", metadataReport);
        mapping = new MetadataServiceNameMapping(applicationModel);
        mapping.setApplicationModel(applicationModel);
        url = URL.valueOf("dubbo://127.0.0.1:20880/TestService?version=1.0.0");
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    void testMap() {
        ApplicationModel mockedApplicationModel = spy(applicationModel);

        when(configManager.getMetadataConfigs()).thenReturn(Collections.emptyList());
        Mockito.when(mockedApplicationModel.getApplicationConfigManager()).thenReturn(configManager);
        Mockito.when(mockedApplicationModel.getCurrentConfig()).thenReturn(new ApplicationConfig("test"));

        // metadata report config not found
        mapping.setApplicationModel(mockedApplicationModel);
        boolean result = mapping.map(url);
        assertFalse(result);

        when(configManager.getMetadataConfigs()).thenReturn(Arrays.asList(new MetadataReportConfig()));
        MetadataReportInstance reportInstance = mock(MetadataReportInstance.class);
        Mockito.when(reportInstance.getMetadataReports(true)).thenReturn(metadataReportList);
        mapping.metadataReportInstance = reportInstance;

        when(metadataReport.registerServiceAppMapping(any(), any(), any())).thenReturn(true);

        // metadata report directly
        result = mapping.map(url);
        assertTrue(result);

        // metadata report using cas and retry, succeeded after retried 10 times
        when(metadataReport.registerServiceAppMapping(any(), any(), any())).thenReturn(false);
        when(metadataReport.getConfigItem(any(), any())).thenReturn(new ConfigItem());
        when(metadataReport.registerServiceAppMapping(any(), any(), any(), any())).thenAnswer(new Answer<Boolean>() {
            private int counter = 0;

            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) {
                if (++counter == 10) {
                    return true;
                }
                return false;
            }
        });
        assertTrue(mapping.map(url));

        // metadata report using cas and retry, failed after 11 times retry
        when(metadataReport.registerServiceAppMapping(any(), any(), any(), any())).thenReturn(false);
        Exception exceptionExpected = null;
        assertFalse(mapping.map(url));
    }

    /**
     * This test currently doesn't make any sense
     */
    @Test
    void testGet() {
        Set<String> set = new HashSet<>();
        set.add("app1");

        MetadataReportInstance reportInstance = mock(MetadataReportInstance.class);
        Mockito.when(reportInstance.getMetadataReport(any())).thenReturn(metadataReport);
        when(metadataReport.getServiceAppMapping(any(), any())).thenReturn(set);

        mapping.metadataReportInstance = reportInstance;
        Set<String> result = mapping.get(url);
        assertEquals(set, result);
    }

    /**
     * Same situation as testGet, so left empty.
     */
    @Test
    void testGetAndListen() {
        // TODO
    }
}
