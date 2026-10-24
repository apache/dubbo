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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToStringUtilsTest {

    @Test
    void testPrintToString() {
        // Testing the first method that tries to use JSON
        String result = ToStringUtils.printToString("Dubbo");
        // It should wrap it in quotes because of JsonUtils
        assertTrue(result.contains("Dubbo"));

        assertEquals("null", ToStringUtils.printToString(null));
    }

    @Test
    void testToStringWithSimpleTypes() {
        // Simple types should just return their string value
        assertEquals("123", ToStringUtils.toString(123));
        assertEquals("true", ToStringUtils.toString(true));
        assertEquals("null", ToStringUtils.toString(null));
    }

    @Test
    void testToStringWithArray() {
        // Testing the loop that builds [item1, item2]
        Object[] array = new Object[] {"A", "B", 1};
        assertEquals("[A, B, 1]", ToStringUtils.toString(array));
    }

    @Test
    void testToStringWithList() {
        // Testing the List block
        List<String> list = Arrays.asList("Apple", "Banana");
        assertEquals("[Apple, Banana]", ToStringUtils.toString(list));
    }

    @Test
    void testToStringWithMap() {
        // Testing the Map block with the key=value format
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 1);
        assertEquals("{key=1}", ToStringUtils.toString(map));
    }

    @Test
    void testComplexRecursion() {
        // A List inside a Map
        Map<String, Object> complexMap = new HashMap<>();
        complexMap.put("list", Arrays.asList(1, 2));

        String result = ToStringUtils.toString(complexMap);
        assertEquals("{list=[1, 2]}", result);
    }
}
