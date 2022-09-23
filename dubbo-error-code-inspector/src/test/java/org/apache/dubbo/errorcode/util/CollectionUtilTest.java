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

package org.apache.dubbo.errorcode.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests of CollectionUtil.
 */
class CollectionUtilTest {

    private static final Map<String, String> SINGLE_ELEMENT_MAP = Collections.singletonMap("a", "a");

    private static final Map<String, String> DUAL_ELEMENT_MAP;

    private static final Map<String, String> THREE_ELEMENTS_MAP;

    static {

        Map<String, String> dualElementMap = new HashMap<>();
        dualElementMap.put("a", "a");
        dualElementMap.put("b", "b");

        DUAL_ELEMENT_MAP = Collections.unmodifiableMap(dualElementMap);

        Map<String, String> threeElementsMap = new HashMap<>();
        threeElementsMap.put("a", "a");
        threeElementsMap.put("b", "b");
        threeElementsMap.put("c", "c");

        THREE_ELEMENTS_MAP = Collections.unmodifiableMap(threeElementsMap);
    }

    @Test
    void testSingleElement() {
        Assertions.assertEquals(SINGLE_ELEMENT_MAP, CollectionUtil.mapOf("a", "a"));
    }

    @Test
    void testDualElements() {
        Assertions.assertEquals(DUAL_ELEMENT_MAP, CollectionUtil.mapOf("a", "a", "b", "b"));
    }

    @Test
    void testThreeElements() {
        Assertions.assertEquals(THREE_ELEMENTS_MAP, CollectionUtil.mapOf("a", "a", "b", "b", "c", "c"));
    }
}
