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
package org.apache.dubbo.common.utils;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypeUtilsTest {

    static class StringList extends java.util.ArrayList<String> {}

    static interface StringIntegerMap extends Map<String, Integer> {}

    @Test
    void testIsParameterizedType() {
        // String is not parameterized
        assertFalse(TypeUtils.isParameterizedType(String.class));

        // Get the generic superclass of our StringList (which is ArrayList<String>)
        Type genericType = StringList.class.getGenericSuperclass();
        assertTrue(TypeUtils.isParameterizedType(genericType));
    }

    @Test
    void testGetRawClass() {
        Type genericType = StringList.class.getGenericSuperclass(); // ArrayList<String>
        assertEquals(java.util.ArrayList.class, TypeUtils.getRawClass(genericType));

        // Test with a normal class
        assertEquals(String.class, TypeUtils.getRawClass(String.class));

        // Test with null
        assertNull(TypeUtils.getRawClass(null));
    }

    @Test
    void testFindActualTypeArguments() {
        // Check StringList (should find String)
        List<Class<?>> args = TypeUtils.findActualTypeArguments(StringList.class, List.class);
        assertEquals(1, args.size());
        assertEquals(String.class, args.get(0));
    }

    @Test
    void testGetClassName() {
        assertEquals(String.class.getName(), TypeUtils.getClassName(String.class));
    }
}
