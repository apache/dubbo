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
package org.apache.dubbo.config.utils;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ConfigValidationUtilsTest {

    @Test
    void testValidateMetadataConfig() {
        MetadataReportConfig config = new MetadataReportConfig();
        config.setAddress("protocol://ip:host");
        try {
            ConfigValidationUtils.validateMetadataConfig(config);
        } catch (Exception e) {
            Assertions.fail("valid config expected.");
        }

        config.setAddress("ip:host");
        config.setProtocol("protocol");
        try {
            ConfigValidationUtils.validateMetadataConfig(config);
        } catch (Exception e) {
            Assertions.fail("valid config expected.");
        }

        config.setAddress("ip:host");
        config.setProtocol(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ConfigValidationUtils.validateMetadataConfig(config);
        });
    }

    @Test
    void testValidateApplicationConfig() throws Exception {
        try (MockedStatic<ConfigValidationUtils> mockedStatic = Mockito.mockStatic(ConfigValidationUtils.class); ) {
            mockedStatic
                    .when(() -> ConfigValidationUtils.validateApplicationConfig(any()))
                    .thenCallRealMethod();
            ApplicationConfig config = new ApplicationConfig();
            Assertions.assertThrows(IllegalStateException.class, () -> {
                ConfigValidationUtils.validateApplicationConfig(config);
            });

            config.setName("testName");
            config.setOwner("testOwner");
            config.setOrganization("testOrg");
            config.setArchitecture("testArchitecture");
            config.setEnvironment("test");
            Map<String, String> map = new HashMap();
            map.put("k1", "v1");
            map.put("k2", "v2");
            config.setParameters(map);
            ConfigValidationUtils.validateApplicationConfig(config);
            mockedStatic.verify(
                    () -> {
                        ConfigValidationUtils.checkName(any(), any());
                    },
                    times(4));
            mockedStatic.verify(
                    () -> {
                        ConfigValidationUtils.checkMultiName(any(), any());
                    },
                    times(1));
            mockedStatic.verify(
                    () -> {
                        ConfigValidationUtils.checkParameterName(any());
                    },
                    times(1));
        }
    }

    @Test
    void testCheckQosInApplicationConfig() throws Exception {
        ConfigValidationUtils mock = Mockito.mock(ConfigValidationUtils.class);
        ErrorTypeAwareLogger loggerMock = Mockito.mock(ErrorTypeAwareLogger.class);
        injectField(mock.getClass().getDeclaredField("logger"), loggerMock);
        ApplicationConfig config = new ApplicationConfig();
        config.setName("testName");
        config.setQosEnable(false);
        mock.validateApplicationConfig(config);
        verify(loggerMock, never()).warn(any(), any(Throwable.class));

        config.setQosEnable(true);
        mock.validateApplicationConfig(config);
        verify(loggerMock).info(anyString());
    }

    private void injectField(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        field.set(null, newValue);
    }

    public static class InterfaceConfig extends AbstractInterfaceConfig {}
}
