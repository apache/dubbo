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
package org.apache.dubbo.mcp.tool;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DubboMcpGenericCallerTest {

    @Mock
    private ApplicationModel applicationModel;

    @Mock
    private ApplicationConfig applicationConfig;

    private DubboMcpGenericCaller genericCaller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConstructor_WithValidApplicationModel() {
        when(applicationModel.getCurrentConfig()).thenReturn(applicationConfig);

        assertDoesNotThrow(() -> {
            genericCaller = new DubboMcpGenericCaller(applicationModel);
        });
    }

    @Test
    void testConstructor_WithNullApplicationModel() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DubboMcpGenericCaller(null);
        });
    }

    @Test
    void testConstructor_WithNullApplicationConfig() {
        when(applicationModel.getCurrentConfig()).thenReturn(null);
        when(applicationModel.getApplicationName()).thenReturn("TestApp");

        assertThrows(IllegalStateException.class, () -> {
            new DubboMcpGenericCaller(applicationModel);
        });
    }

    @Test
    void testConstructor_WithNullApplicationName() {
        when(applicationModel.getCurrentConfig()).thenReturn(null);
        when(applicationModel.getApplicationName()).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> {
            new DubboMcpGenericCaller(applicationModel);
        });
    }

    @Test
    void testExecute_WithValidParameters() {
        when(applicationModel.getCurrentConfig()).thenReturn(applicationConfig);
        genericCaller = new DubboMcpGenericCaller(applicationModel);

        String interfaceName = "TestInterface";
        String methodName = "testMethod";
        List<String> orderedJavaParameterNames = createParameterNames();
        Class<?>[] parameterJavaTypes = createParameterTypes();
        Map<String, Object> mcpProvidedParameters = createMcpParameters();
        String group = "testGroup";
        String version = "1.0.0";

        assertThrows(RuntimeException.class, () -> {
            genericCaller.execute(
                    interfaceName,
                    methodName,
                    orderedJavaParameterNames,
                    parameterJavaTypes,
                    mcpProvidedParameters,
                    group,
                    version);
        });
    }

    @Test
    void testExecute_WithNullGroup() {
        when(applicationModel.getCurrentConfig()).thenReturn(applicationConfig);
        genericCaller = new DubboMcpGenericCaller(applicationModel);

        String interfaceName = "TestInterface";
        String methodName = "testMethod";
        List<String> orderedJavaParameterNames = createParameterNames();
        Class<?>[] parameterJavaTypes = createParameterTypes();
        Map<String, Object> mcpProvidedParameters = createMcpParameters();
        String version = "1.0.0";

        assertThrows(RuntimeException.class, () -> {
            genericCaller.execute(
                    interfaceName,
                    methodName,
                    orderedJavaParameterNames,
                    parameterJavaTypes,
                    mcpProvidedParameters,
                    null,
                    version);
        });
    }

    @Test
    void testExecute_WithNullVersion() {
        when(applicationModel.getCurrentConfig()).thenReturn(applicationConfig);
        genericCaller = new DubboMcpGenericCaller(applicationModel);

        String interfaceName = "TestInterface";
        String methodName = "testMethod";
        List<String> orderedJavaParameterNames = createParameterNames();
        Class<?>[] parameterJavaTypes = createParameterTypes();
        Map<String, Object> mcpProvidedParameters = createMcpParameters();
        String group = "testGroup";

        assertThrows(RuntimeException.class, () -> {
            genericCaller.execute(
                    interfaceName,
                    methodName,
                    orderedJavaParameterNames,
                    parameterJavaTypes,
                    mcpProvidedParameters,
                    group,
                    null);
        });
    }

    @Test
    void testExecute_WithMissingParameters() {
        when(applicationModel.getCurrentConfig()).thenReturn(applicationConfig);
        genericCaller = new DubboMcpGenericCaller(applicationModel);

        String interfaceName = "TestInterface";
        String methodName = "testMethod";
        List<String> orderedJavaParameterNames = createParameterNames();
        Class<?>[] parameterJavaTypes = createParameterTypes();
        Map<String, Object> mcpProvidedParameters = new HashMap<>();
        String group = "testGroup";
        String version = "1.0.0";

        assertThrows(RuntimeException.class, () -> {
            genericCaller.execute(
                    interfaceName,
                    methodName,
                    orderedJavaParameterNames,
                    parameterJavaTypes,
                    mcpProvidedParameters,
                    group,
                    version);
        });
    }

    @Test
    void testExecute_WithEmptyStrings() {
        when(applicationModel.getCurrentConfig()).thenReturn(applicationConfig);
        genericCaller = new DubboMcpGenericCaller(applicationModel);

        String interfaceName = "TestInterface";
        String methodName = "testMethod";
        List<String> orderedJavaParameterNames = createParameterNames();
        Class<?>[] parameterJavaTypes = createParameterTypes();
        Map<String, Object> mcpProvidedParameters = createMcpParameters();

        assertThrows(RuntimeException.class, () -> {
            genericCaller.execute(
                    interfaceName,
                    methodName,
                    orderedJavaParameterNames,
                    parameterJavaTypes,
                    mcpProvidedParameters,
                    "",
                    "");
        });
    }

    private List<String> createParameterNames() {
        List<String> names = new ArrayList<>();
        names.add("param1");
        names.add("param2");
        return names;
    }

    private Class<?>[] createParameterTypes() {
        return new Class<?>[] {String.class, Integer.class};
    }

    private Map<String, Object> createMcpParameters() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param1", "value1");
        parameters.put("param2", 42);
        return parameters;
    }
}
