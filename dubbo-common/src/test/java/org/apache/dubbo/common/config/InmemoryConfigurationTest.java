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
package org.apache.dubbo.common.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * The type Inmemory configuration test.
 */
class InmemoryConfigurationTest {

    private InmemoryConfiguration memConfig;
    private static final String MOCK_KEY = "mockKey";
    private static final String MOCK_VALUE = "mockValue";
    private static final String MOCK_ONE_KEY = "one";
    private static final String MOCK_TWO_KEY = "two";
    private static final String MOCK_THREE_KEY = "three";

    /**
     * Init.
     */
    @BeforeEach
    public void init() {

        memConfig = new InmemoryConfiguration();
    }

    /**
     * Test get mem property.
     */
    @Test
    public void testGetMemProperty() {
        Assertions.assertNull(memConfig.getInternalProperty(MOCK_KEY));
        Assertions.assertFalse(memConfig.containsKey(MOCK_KEY));
        Assertions.assertNull(memConfig.getString(MOCK_KEY));
        Assertions.assertNull(memConfig.getProperty(MOCK_KEY));
        memConfig.addProperty(MOCK_KEY, MOCK_VALUE);
        Assertions.assertTrue(memConfig.containsKey(MOCK_KEY));
        Assertions.assertEquals(MOCK_VALUE, memConfig.getInternalProperty(MOCK_KEY));
        Assertions.assertEquals(MOCK_VALUE, memConfig.getString(MOCK_KEY, MOCK_VALUE));
        Assertions.assertEquals(MOCK_VALUE, memConfig.getProperty(MOCK_KEY, MOCK_VALUE));
    }

    /**
     * Test get properties.
     */
    @Test
    public void testGetProperties() {
        Assertions.assertNull(memConfig.getInternalProperty(MOCK_ONE_KEY));
        Assertions.assertNull(memConfig.getInternalProperty(MOCK_TWO_KEY));
        Map<String, String> proMap = new HashMap<>();
        proMap.put(MOCK_ONE_KEY, MOCK_VALUE);
        proMap.put(MOCK_TWO_KEY, MOCK_VALUE);
        memConfig.addProperties(proMap);
        Assertions.assertNotNull(memConfig.getInternalProperty(MOCK_ONE_KEY));
        Assertions.assertNotNull(memConfig.getInternalProperty(MOCK_TWO_KEY));
        Map<String, String> anotherProMap = new HashMap<>();
        anotherProMap.put(MOCK_THREE_KEY, MOCK_VALUE);
        memConfig.setProperties(anotherProMap);
        Assertions.assertNotNull(memConfig.getInternalProperty(MOCK_THREE_KEY));
        Assertions.assertNull(memConfig.getInternalProperty(MOCK_ONE_KEY));
        Assertions.assertNull(memConfig.getInternalProperty(MOCK_TWO_KEY));

    }

    @Test
    public void testGetInt() {
        memConfig.addProperty("a", "1");
        Assertions.assertEquals(1, memConfig.getInt("a"));
        Assertions.assertEquals(Integer.valueOf(1), memConfig.getInteger("a", 2));
        Assertions.assertEquals(2, memConfig.getInt("b", 2));
    }

    @Test
    public void getBoolean() {
        memConfig.addProperty("a", Boolean.TRUE.toString());
        Assertions.assertTrue(memConfig.getBoolean("a"));
        Assertions.assertFalse(memConfig.getBoolean("b", false));
        Assertions.assertTrue(memConfig.getBoolean("b", Boolean.TRUE));
    }

    /**
     * Clean.
     */
    @AfterEach
    public void clean() {

    }

}