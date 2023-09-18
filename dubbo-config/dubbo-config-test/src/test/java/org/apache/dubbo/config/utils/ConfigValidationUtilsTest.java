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
import org.apache.dubbo.config.exception.ConfigValidationException;
import org.apache.dubbo.config.validator.ApplicationConfigValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_CLASS_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


class ConfigValidationUtilsTest {

    @Test
    void testValidateApplicationConfig() throws Exception {
        try (MockedStatic<ApplicationConfigValidator> mockedStatic = Mockito.mockStatic(ApplicationConfigValidator.class);
             MockedStatic<ConfigValidationUtils> configValidationUtilsMockedStatic =  Mockito.mockStatic(ConfigValidationUtils.class);) {
            mockedStatic.when(() -> ApplicationConfigValidator.validateApplicationConfig(any())).thenCallRealMethod();
            ApplicationConfig config = new ApplicationConfig();
            Assertions.assertThrows(ConfigValidationException.class, () -> {
                ApplicationConfigValidator.validateApplicationConfig(config);
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
            ApplicationConfigValidator.validateApplicationConfig(config);

            configValidationUtilsMockedStatic.when(()-> ConfigValidationUtils.checkName(anyString(),anyString())).thenCallRealMethod();
            configValidationUtilsMockedStatic.when(()-> ConfigValidationUtils.checkMultiName(anyString(),anyString())).thenCallRealMethod();
            configValidationUtilsMockedStatic.when(()-> ConfigValidationUtils.checkParameterName(any())).thenCallRealMethod();
            configValidationUtilsMockedStatic.when(()-> ConfigValidationUtils.checkProperty(anyString(),anyString(),anyInt(),any(Pattern.class))).thenCallRealMethod();
            configValidationUtilsMockedStatic.verify(() -> {
                ConfigValidationUtils.checkName(any(), any());
            }, times(4));
            configValidationUtilsMockedStatic.verify(() -> {
                ConfigValidationUtils.checkMultiName(any(), any());
            }, times(1));
            configValidationUtilsMockedStatic.verify(() -> {
                ConfigValidationUtils.checkParameterName(any());
            }, times(1));
        }
    }

    @Test
    void testCheckQosInApplicationConfig() throws Exception {
        ApplicationConfigValidator mock = Mockito.mock(ApplicationConfigValidator.class);
        ErrorTypeAwareLogger loggerMock = Mockito.mock(ErrorTypeAwareLogger.class);
        injectField(mock.getClass().getDeclaredField("logger"), loggerMock);
        ApplicationConfig config = new ApplicationConfig();
        config.setName("testName");
        config.setQosEnable(false);
        ApplicationConfigValidator.validateApplicationConfig(config);
        verify(loggerMock, never()).warn(any(), any(Throwable.class));

        config.setQosEnable(true);
        mock.validateApplicationConfig(config);
        verify(loggerMock).warn(eq(COMMON_CLASS_NOT_FOUND), eq(""), eq(""), eq("No QosProtocolWrapper class was found. Please check the dependency of dubbo-qos whether was imported correctly."), any());
    }

    private void injectField(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        field.set(null, newValue);
    }

    public static class InterfaceConfig extends AbstractInterfaceConfig {

    }

}
