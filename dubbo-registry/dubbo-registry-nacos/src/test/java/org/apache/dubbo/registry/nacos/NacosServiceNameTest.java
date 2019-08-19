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
package org.apache.dubbo.registry.nacos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_CATEGORY;
import static org.apache.dubbo.registry.nacos.NacosServiceName.WILDCARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link NacosServiceName} Test
 *
 * @since 2.7.3
 */
public class NacosServiceNameTest {

    private static final String category = DEFAULT_CATEGORY;

    private static final String serviceInterface = "org.apache.dubbo.registry.nacos.NacosServiceName";

    private static final String version = "1.0.0";

    private static final String group = "default";

    private final NacosServiceName name = new NacosServiceName();

    @BeforeEach
    public void init() {
        name.setCategory(category);
        name.setServiceInterface(serviceInterface);
        name.setVersion(version);
        name.setGroup(group);
    }

    @Test
    public void testGetter() {
        assertEquals(category, name.getCategory());
        assertEquals(serviceInterface, name.getServiceInterface());
        assertEquals(version, name.getVersion());
        assertEquals(group, name.getGroup());
        assertEquals("providers:org.apache.dubbo.registry.nacos.NacosServiceName:1.0.0:default", name.getValue());
    }

    @Test
    public void testToString() {
        assertEquals("providers:org.apache.dubbo.registry.nacos.NacosServiceName:1.0.0:default", name.toString());
    }

    @Test
    public void testIsConcrete() {

        assertTrue(name.isConcrete());

        name.setGroup(WILDCARD);
        assertFalse(name.isConcrete());

        init();
        name.setVersion(WILDCARD);
        assertFalse(name.isConcrete());

        init();
        name.setGroup(null);
        name.setVersion(null);
        assertTrue(name.isConcrete());

    }

    @Test
    public void testIsCompatible() {

        NacosServiceName concrete = new NacosServiceName();

        assertFalse(name.isCompatible(concrete));

        // set category
        concrete.setCategory(category);
        assertFalse(name.isCompatible(concrete));

        concrete.setServiceInterface(serviceInterface);
        assertFalse(name.isCompatible(concrete));

        concrete.setVersion(version);
        assertFalse(name.isCompatible(concrete));

        concrete.setGroup(group);
        assertTrue(name.isCompatible(concrete));

        // wildcard cases
        name.setGroup(WILDCARD);
        assertTrue(name.isCompatible(concrete));

        init();
        name.setVersion(WILDCARD);
        assertTrue(name.isCompatible(concrete));

        // range cases
        init();
        name.setGroup(group + ",2.0.0");
        assertTrue(name.isCompatible(concrete));

        init();
        name.setVersion(version + ",2.0.0");
        assertTrue(name.isCompatible(concrete));
    }
}
