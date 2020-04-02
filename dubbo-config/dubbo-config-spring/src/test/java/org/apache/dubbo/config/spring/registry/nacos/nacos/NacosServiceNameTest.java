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
package org.apache.dubbo.config.spring.registry.nacos.nacos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.nacos.NacosServiceName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_CATEGORY;
import static org.apache.dubbo.registry.nacos.NacosServiceName.DEFAULT_PARAM_VALUE;
import static org.apache.dubbo.registry.nacos.NacosServiceName.WILDCARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * {@link NacosServiceName} Test
 *
 * @since 2.7.3
 */
class NacosServiceNameTest {

    public static final String NAME_SEPARATOR = ":";

    private static final String category = DEFAULT_CATEGORY;

    private static final String serviceInterface = "org.apache.dubbo.registry.nacos.NacosServiceName";

    private static final String version = "1.0.0";

    private static final String group = "default";

    private NacosServiceName name;


    @BeforeEach
    public void init() {
        URL mockUrl = mock(URL.class);
        when(mockUrl.getParameter(INTERFACE_KEY)).thenReturn(serviceInterface);
        when(mockUrl.getParameter(CATEGORY_KEY)).thenReturn(category);
        when(mockUrl.getParameter(VERSION_KEY, DEFAULT_PARAM_VALUE)).thenReturn(version);
        when(mockUrl.getParameter(GROUP_KEY, DEFAULT_PARAM_VALUE)).thenReturn(group);
        name = spy(new NacosServiceName(mockUrl));

    }

    @Test
    public void testGetter() {
        assertEquals(category, name.getCategory());
        assertEquals(serviceInterface, name.getServiceInterface());
        assertEquals(version, name.getVersion());
        assertEquals(group, name.getGroup());
        assertEquals(new StringBuilder(category)
                .append(NAME_SEPARATOR).append(serviceInterface)
                .append(NAME_SEPARATOR).append(version)
                .append(NAME_SEPARATOR).append(group)
                .toString(), name.getValue());
    }

    @ParameterizedTest(name = " testIsConcrete with value {0} ")
    @ValueSource(strings = {WILDCARD, "1.0.0,2.0.0"})
    public void testIsConcrete(String v) {
        Assertions.assertTrue(name.isConcrete());

        name.setGroup(v);
        Assertions.assertFalse(name.isConcrete());
        name.setGroup(group);

        name.setVersion(v);
        Assertions.assertFalse(name.isConcrete());
        name.setVersion(version);

        name.setServiceInterface(v);
        Assertions.assertFalse(name.isConcrete());
        name.setServiceInterface(serviceInterface);

        Assertions.assertTrue(name.isConcrete());
    }

    @Test
    public void testEquals() {
        NacosServiceName nameByValue = new NacosServiceName(name.getValue());

        Assertions.assertTrue(name.isCompatible(nameByValue));
        // becase name has been spied , so first param MUST `nameByValue`
        assertEquals(nameByValue, name);
    }

    @Test
    public void testIsCompatible() {

        NacosServiceName concrete = new NacosServiceName();

        Assertions.assertFalse(name.isCompatible(concrete));

        // set category
        concrete.setCategory(category);
        Assertions.assertFalse(name.isCompatible(concrete));

        concrete.setServiceInterface(serviceInterface);
        Assertions.assertFalse(name.isCompatible(concrete));

        concrete.setVersion(version);
        Assertions.assertFalse(name.isCompatible(concrete));

        concrete.setGroup(group);
        Assertions.assertTrue(name.isCompatible(concrete));

        // wildcard cases
        name.setGroup(WILDCARD);
        Assertions.assertTrue(name.isCompatible(concrete));

        init();
        name.setVersion(WILDCARD);
        Assertions.assertTrue(name.isCompatible(concrete));

        // range cases
        init();
        name.setGroup(group + ",2.0.0");
        Assertions.assertTrue(name.isCompatible(concrete));

        init();
        name.setVersion(version + ",2.0.0");
        Assertions.assertTrue(name.isCompatible(concrete));
    }
}
