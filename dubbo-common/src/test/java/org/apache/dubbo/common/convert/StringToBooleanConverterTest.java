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
package org.apache.dubbo.common.convert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link StringToBooleanConverter} Test
 *
 * @since 2.7.6
 */
class StringToBooleanConverterTest {

    private StringToBooleanConverter converter;

    @BeforeEach
    public void init() {
        converter = (StringToBooleanConverter) getExtensionLoader(Converter.class).getExtension("string-to-boolean");
    }

    @Test
    void testAccept() {
        assertTrue(converter.accept(String.class, Boolean.class));
    }

    @Test
    void testConvert() {
        assertTrue(converter.convert("true"));
        assertTrue(converter.convert("true"));
        assertTrue(converter.convert("True"));
        assertFalse(converter.convert("a"));
        assertNull(converter.convert(""));
        assertNull(converter.convert(null));
    }
}