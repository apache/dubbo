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

import org.apache.dubbo.common.beanutil.JavaBeanAccessor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Unit test of class InmemoryConfiguration, and interface Configuration.
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
    void testGetMemProperty() {
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
    void testGetProperties() {
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
    void testGetInt() {
        memConfig.addProperty("a", "1");
        Assertions.assertEquals(1, memConfig.getInt("a"));
        Assertions.assertEquals(Integer.valueOf(1), memConfig.getInteger("a", 2));
        Assertions.assertEquals(2, memConfig.getInt("b", 2));
    }

    @Test
    void getBoolean() {
        memConfig.addProperty("a", Boolean.TRUE.toString());
        Assertions.assertTrue(memConfig.getBoolean("a"));
        Assertions.assertFalse(memConfig.getBoolean("b", false));
        Assertions.assertTrue(memConfig.getBoolean("b", Boolean.TRUE));
    }

    @Test
    void testIllegalType() {
        memConfig.addProperty("it", "aaa");

        Assertions.assertThrows(IllegalStateException.class, () -> memConfig.getInteger("it", 1));
        Assertions.assertThrows(IllegalStateException.class, () -> memConfig.getInt("it", 1));
        Assertions.assertThrows(IllegalStateException.class, () -> memConfig.getInt("it"));
    }

    @Test
    void testDoesNotExist() {
        Assertions.assertThrows(NoSuchElementException.class, () -> memConfig.getInt("ne"));
        Assertions.assertThrows(NoSuchElementException.class, () -> memConfig.getBoolean("ne"));
    }

    @Test
    void testConversions() {
        memConfig.addProperty("long", "2147483648");
        memConfig.addProperty("byte", "127");
        memConfig.addProperty("short", "32767");
        memConfig.addProperty("float", "3.14");
        memConfig.addProperty("double", "3.14159265358979323846264338327950");
        memConfig.addProperty("enum", "FIELD");

        Object longObject = memConfig.convert(Long.class, "long", 1L);
        Object byteObject = memConfig.convert(Byte.class, "byte", (byte) 1);
        Object shortObject = memConfig.convert(Short.class, "short", (short) 1);
        Object floatObject = memConfig.convert(Float.class, "float", 3.14F);
        Object doubleObject = memConfig.convert(Double.class, "double", 3.14159265358979323846264338327950);
        JavaBeanAccessor javaBeanAccessor = memConfig.convert(JavaBeanAccessor.class, "enum", JavaBeanAccessor.ALL);

        Assertions.assertEquals(Long.class, longObject.getClass());
        Assertions.assertEquals(2147483648L, longObject);

        Assertions.assertEquals(Byte.class, byteObject.getClass());
        Assertions.assertEquals((byte) 127, byteObject);

        Assertions.assertEquals(Short.class, shortObject.getClass());
        Assertions.assertEquals((short) 32767, shortObject);

        Assertions.assertEquals(Float.class, floatObject.getClass());
        Assertions.assertEquals(3.14F, floatObject);

        Assertions.assertEquals(Double.class, doubleObject.getClass());
        Assertions.assertEquals(3.14159265358979323846264338327950, doubleObject);

        Assertions.assertEquals(JavaBeanAccessor.class, javaBeanAccessor.getClass());
        Assertions.assertEquals(JavaBeanAccessor.FIELD, javaBeanAccessor);
    }

    /**
     * Clean.
     */
    @AfterEach
    public void clean() {

    }

}