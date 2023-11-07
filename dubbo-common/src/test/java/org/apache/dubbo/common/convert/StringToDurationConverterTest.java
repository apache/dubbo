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

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link StringToDurationConverter} Test
 *
 * @since 3.2.3
 */
class StringToDurationConverterTest {

    private StringToDurationConverter converter;

    @BeforeEach
    public void init() {
        converter =
                (StringToDurationConverter) getExtensionLoader(Converter.class).getExtension("string-to-duration");
    }

    @Test
    void testAccept() {
        assertTrue(converter.accept(String.class, Duration.class));
    }

    @Test
    void testConvert() {
        assertEquals(Duration.ofMillis(1000), converter.convert("1000ms"));
        assertEquals(Duration.ofSeconds(1), converter.convert("1s"));
        assertEquals(Duration.ofMinutes(1), converter.convert("1m"));
        assertEquals(Duration.ofHours(1), converter.convert("1h"));
        assertEquals(Duration.ofDays(1), converter.convert("1d"));

        assertNull(converter.convert(null));
        assertThrows(IllegalArgumentException.class, () -> {
            converter.convert("ttt");
        });
    }
}
