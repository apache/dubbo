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
package org.apache.dubbo.convert;

import org.apache.dubbo.common.convert.Converter;
import org.apache.dubbo.common.convert.StringToIntegerConverter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link StringToIntegerConverter} Test
 *
 * @since 2.7.6
 */
public class StringToIntegerConverterTest {

    private StringToIntegerConverter converter;

    @BeforeEach
    public void init() {
        converter = (StringToIntegerConverter) getExtensionLoader(Converter.class).getExtension("string-to-integer");
    }

    @Test
    public void testAccept() {
        assertTrue(converter.accept(String.class, Integer.class));
    }

    @Test
    public void testConvert() {
        assertEquals(Integer.valueOf("1"), converter.convert("1"));
        assertNull(converter.convert(null));
        assertThrows(NumberFormatException.class, () -> {
            converter.convert("ttt");
        });
    }
}
