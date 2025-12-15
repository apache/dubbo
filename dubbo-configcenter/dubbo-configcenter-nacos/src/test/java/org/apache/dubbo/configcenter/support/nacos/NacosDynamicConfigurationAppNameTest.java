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
package org.apache.dubbo.configcenter.support.nacos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.nacos.NacosAppNameUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Properties;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.apache.dubbo.common.nacos.NacosAppNameUtils.NACOS_SET_PROJECT_NAME_KEY;
import static org.mockito.ArgumentMatchers.any;

class NacosDynamicConfigurationAppNameTest {

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
    void shouldSetProjectNameWhenEnabled() throws Exception {
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();
        try (MockedStatic<NacosFactory> nacosFactory = Mockito.mockStatic(NacosFactory.class)) {
            ConfigService mockConfig = Mockito.mock(ConfigService.class);
            Mockito.when(mockConfig.getServerStatus()).thenReturn("UP");
            Mockito.when(mockConfig.getConfig(any(), any(), any(Long.class))).thenReturn("");
            nacosFactory
                    .when(() -> NacosFactory.createConfigService((Properties) any()))
                    .thenReturn(mockConfig);

            System.clearProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("configcenter-app"));

            URL url = URL.valueOf("nacos://127.0.0.1:8848").addParameter(NACOS_SET_PROJECT_NAME_KEY, "true");

            new NacosDynamicConfiguration(url, applicationModel);

            Assertions.assertEquals(
                    "configcenter-app", System.getProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY));
        } finally {
            applicationModel.destroy();
        }
    }

    @Test
    void shouldNotSetProjectNameWhenDisabled() throws Exception {
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();
        try (MockedStatic<NacosFactory> nacosFactory = Mockito.mockStatic(NacosFactory.class)) {
            ConfigService mockConfig = Mockito.mock(ConfigService.class);
            Mockito.when(mockConfig.getServerStatus()).thenReturn("UP");
            Mockito.when(mockConfig.getConfig(any(), any(), any(Long.class))).thenReturn("");
            nacosFactory
                    .when(() -> NacosFactory.createConfigService((Properties) any()))
                    .thenReturn(mockConfig);

            System.clearProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("configcenter-app"));

            URL url = URL.valueOf("nacos://127.0.0.1:8848");

            new NacosDynamicConfiguration(url, applicationModel);

            Assertions.assertNull(System.getProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY));
        } finally {
            applicationModel.destroy();
        }
    }
}
