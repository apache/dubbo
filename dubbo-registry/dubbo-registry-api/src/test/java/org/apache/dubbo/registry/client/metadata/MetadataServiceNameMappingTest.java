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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataServiceNameMappingTest {

    private MetadataServiceNameMapping mapping;
    private URL url;
    private ConfigManager configManager;
    private MetadataReport metadataReport;

    @BeforeEach
    public void setUp() {
        mapping = new MetadataServiceNameMapping();
        url = URL.valueOf("dubbo://127.0.0.1:20880/TestService?version=1.0.0");
        configManager = mock(ConfigManager.class);
        metadataReport = mock(MetadataReport.class);
    }

    @Test
    public void testMap() {
        try (MockedStatic<ApplicationModel> mockedApplicationModel = Mockito.mockStatic(ApplicationModel.class)) {
            when(configManager.getMetadataConfigs()).thenReturn(Collections.emptyList());
            mockedApplicationModel.when(ApplicationModel::getConfigManager).thenReturn(configManager);

            // metadata report config not found
            boolean result = mapping.map(url);
            assertFalse(result);

            try (MockedStatic<MetadataReportInstance> mockedMetadataHolder = Mockito.mockStatic(MetadataReportInstance.class)) {
                when(configManager.getMetadataConfigs()).thenReturn(Arrays.asList(new MetadataReportConfig()));
                mockedMetadataHolder.when(() -> ApplicationModel.defaultModel().getBeanFactory().getBean(MetadataReportInstance.class).getMetadataReport(any())).thenReturn(metadataReport);
                when(metadataReport.registerServiceAppMapping(any(), any(), any())).thenReturn(true);

                // metadata report directly
                result = mapping.map(url);
                assertTrue(result);

                // metadata report using cas and retry, succeeded after retried 5 times
                when(metadataReport.registerServiceAppMapping(any(), any(), any())).thenReturn(false);
                when(metadataReport.getConfigItem(any(), any())).thenReturn(new ConfigItem());
                when(metadataReport.registerServiceAppMapping(any(), any(), any(), any())).thenAnswer(new Answer<Boolean>() {
                    private int counter = 0;
                    @Override
                    public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                        if (++counter == 5) {
                            return true;
                        }
                        return false;
                    }
                });
                assertTrue(mapping.map(url));

                // metadata report using cas and retry, failed after 6 times retry
                when(metadataReport.registerServiceAppMapping(any(), any(), any(), any())).thenReturn(false);
                Exception exceptionExpected = null;
                try {
                    mapping.map(url);
                } catch (RuntimeException e) {
                    exceptionExpected = e;
                }
                if (exceptionExpected == null) {
                    fail();
                }
            }
        }
    }

    /**
     * This test currently doesn't make any sense
     */
    @Test
    public void testGet() {
        Set<String> set = new HashSet<>();
        set.add("app1");
        try (MockedStatic<MetadataReportInstance> mockedMetadataHolder = Mockito.mockStatic(MetadataReportInstance.class)) {
            mockedMetadataHolder.when(() -> ApplicationModel.defaultModel().getBeanFactory().getBean(MetadataReportInstance.class).getMetadataReport(any())).thenReturn(metadataReport);
            when(metadataReport.getServiceAppMapping(any(), any())).thenReturn(set);

            Set<String> result = mapping.get(url);
            assertEquals(set, result);
        }
    }

    /**
     * Same situation as testGet, so left empty.
     */
    @Test
    public void testGetAndListen() {
        // TODO
    }
}
