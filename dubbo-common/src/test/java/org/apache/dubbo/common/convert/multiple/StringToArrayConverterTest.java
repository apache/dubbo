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
package org.apache.dubbo.common.convert.multiple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Objects.deepEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link StringToArrayConverter} Test
 *
 * @since 2.7.6
 */
public class StringToArrayConverterTest {

    private StringToArrayConverter converter;

    @BeforeEach
    public void init() {
        converter = new StringToArrayConverter();
    }

    @Test
    public void testAccept() {
        assertTrue(converter.accept(String.class, char[].class));
        assertTrue(converter.accept(null, char[].class));
        assertFalse(converter.accept(null, String.class));
        assertFalse(converter.accept(null, String.class));
        assertFalse(converter.accept(null, null));
    }

    @Test
    public void testConvert() {
        assertTrue(deepEquals(new Integer[]{123}, converter.convert("123", Integer[].class, Integer.class)));
        assertTrue(deepEquals(new Integer[]{1, 2, 3}, converter.convert("1,2,3", Integer[].class, null)));
        assertNull(converter.convert("", Integer[].class, null));
        assertNull(converter.convert(null, Integer[].class, null));
    }

    @Test
    public void testGetSourceType() {
        assertEquals(String.class, converter.getSourceType());
    }

    @Test
    public void testGetPriority() {
        assertEquals(Integer.MAX_VALUE, converter.getPriority());
    }
}
