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
package org.apache.dubbo.metadata.store.nacos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.nacos.NacosAppNameUtils;

import java.util.Properties;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.apache.dubbo.common.nacos.NacosAppNameUtils.NACOS_SET_PROJECT_NAME_KEY;
import static org.mockito.ArgumentMatchers.any;

class NacosMetadataReportAppNameTest {

    private final String backup = System.getProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);

    @AfterEach
    void tearDown() {
        if (backup == null) {
            System.clearProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);
        } else {
            System.setProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY, backup);
        }
    }

    @Test
    void shouldSetProjectNameWhenEnabled() throws NacosException {
        try (MockedStatic<NacosFactory> nacosFactory = Mockito.mockStatic(NacosFactory.class)) {
            ConfigService mockConfig = Mockito.mock(ConfigService.class);
            Mockito.when(mockConfig.getServerStatus()).thenReturn("UP");
            Mockito.when(mockConfig.getConfig(any(), any(), any(Long.class))).thenReturn("");
            nacosFactory
                    .when(() -> NacosFactory.createConfigService((Properties) any()))
                    .thenReturn(mockConfig);

            System.clearProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);
            URL url = URL.valueOf("nacos://127.0.0.1:8848")
                    .addParameter(NACOS_SET_PROJECT_NAME_KEY, "true")
                    .addParameter("application", "metadata-app");

            new NacosMetadataReport(url);

            Assertions.assertEquals(
                    url.getApplication(), System.getProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY));
        }
    }

    @Test
    void shouldNotSetProjectNameWhenDisabled() throws NacosException {
        try (MockedStatic<NacosFactory> nacosFactory = Mockito.mockStatic(NacosFactory.class)) {
            ConfigService mockConfig = Mockito.mock(ConfigService.class);
            Mockito.when(mockConfig.getServerStatus()).thenReturn("UP");
            Mockito.when(mockConfig.getConfig(any(), any(), any(Long.class))).thenReturn("");
            nacosFactory
                    .when(() -> NacosFactory.createConfigService((Properties) any()))
                    .thenReturn(mockConfig);

            System.clearProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);
            URL url = URL.valueOf("nacos://127.0.0.1:8848");

            new NacosMetadataReport(url);

            Assertions.assertNull(System.getProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY));
        }
    }
}
