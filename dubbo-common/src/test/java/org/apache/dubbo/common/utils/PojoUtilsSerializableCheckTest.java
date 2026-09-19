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

import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for Issue #16270:
 * Generic invocation without a {@code "class"} key in the argument Map must not bypass the
 * Serializable check performed in {@link PojoUtils#realize1}.
 *
 * <p>Before the fix, Case A (no {@code "class"} key) silently succeeded even when the target
 * POJO did not implement {@link java.io.Serializable}. After the fix both cases must be
 * treated consistently: a non-Serializable POJO is rejected regardless of whether the caller
 * included a {@code "class"} entry.
 */
class PojoUtilsSerializableCheckTest {

    @BeforeEach
    void setUp() {
        FrameworkModel.destroyAll();
    }

    @AfterEach
    void tearDown() {
        FrameworkModel.destroyAll();
    }

    // -----------------------------------------------------------------------
    // A concrete POJO that intentionally does NOT implement Serializable,
    // matching the reproducer in Issue #16270.
    // -----------------------------------------------------------------------
    public static class NonSerializableDto {
        private String label;
        private Integer sequence;

        public NonSerializableDto() {}

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Integer getSequence() {
            return sequence;
        }

        public void setSequence(Integer sequence) {
            this.sequence = sequence;
        }
    }

    // -----------------------------------------------------------------------
    // Happy-path: Serializable POJO should still work in both map styles.
    // -----------------------------------------------------------------------

    /**
     * A Map without a {@code "class"} key should succeed when the target type implements
     * {@link java.io.Serializable}.
     */
    @Test
    void realize_withoutClassKey_serializablePojo_shouldSucceed() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "Alice");
        map.put("age", 30);

        // org.apache.dubbo.common.model.Person implements Serializable
        org.apache.dubbo.common.model.Person result = (org.apache.dubbo.common.model.Person)
                PojoUtils.realize(map, org.apache.dubbo.common.model.Person.class);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Alice", result.getName());
        Assertions.assertEquals(30, result.getAge());
    }

    /**
     * A Map with a {@code "class"} key pointing to a Serializable POJO should also succeed.
     */
    @Test
    void realize_withClassKey_serializablePojo_shouldSucceed() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("class", org.apache.dubbo.common.model.Person.class.getName());
        map.put("name", "Bob");
        map.put("age", 25);

        org.apache.dubbo.common.model.Person result = (org.apache.dubbo.common.model.Person)
                PojoUtils.realize(map, org.apache.dubbo.common.model.Person.class);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Bob", result.getName());
    }

    // -----------------------------------------------------------------------
    // Error-path: Non-Serializable POJO must be rejected in both map styles.
    // -----------------------------------------------------------------------

    /**
     * Case B from Issue #16270: a Map <em>with</em> a {@code "class"} key referring to a
     * non-Serializable type must throw {@link IllegalArgumentException}.
     * (This already worked before the fix — kept here as a contract anchor.)
     */
    @Test
    void realize_withClassKey_nonSerializablePojo_shouldThrow() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("class", NonSerializableDto.class.getName());
        map.put("label", "with-class-key");
        map.put("sequence", 200);

        Assertions.assertThrows(IllegalArgumentException.class, () -> PojoUtils.realize(map, NonSerializableDto.class));
    }

    /**
     * Case A from Issue #16270: a Map <em>without</em> a {@code "class"} key where the target
     * type is resolved from the method signature must also throw {@link IllegalArgumentException}.
     * This is the regression case introduced by the fix.
     */
    @Test
    void realize_withoutClassKey_nonSerializablePojo_shouldThrow() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("label", "no-class-key");
        map.put("sequence", 100);

        // Before the fix this silently succeeded. After the fix it must throw.
        Assertions.assertThrows(IllegalArgumentException.class, () -> PojoUtils.realize(map, NonSerializableDto.class));
    }

    // -----------------------------------------------------------------------
    // Edge-path: interface / Object targets must not be blocked.
    // -----------------------------------------------------------------------

    /**
     * When the declared type is {@link Object}, the new check must not fire (the map is
     * returned as-is or wrapped, not instantiated as a POJO).
     */
    @Test
    void realize_withoutClassKey_objectType_shouldReturnMapAsIs() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", "value");

        // Should not throw — Object target takes the Map branch, no POJO instantiation.
        Object result = PojoUtils.realize(map, Object.class);
        Assertions.assertNotNull(result);
    }

    /**
     * When the declared type is a Map, the new check must not fire.
     */
    @Test
    void realize_withoutClassKey_mapType_shouldReturnMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("k", "v");

        @SuppressWarnings("unchecked")
        Map<Object, Object> result = (Map<Object, Object>) PojoUtils.realize(map, Map.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("v", result.get("k"));
    }
}
