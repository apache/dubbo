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
package org.apache.dubbo.mcp;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaTypeTest {

    @Test
    void testBooleanTypes() {
        assertEquals("boolean", JsonSchemaType.BOOLEAN.getJsonSchemaType());
        assertEquals(boolean.class, JsonSchemaType.BOOLEAN.getJavaType());
        assertNull(JsonSchemaType.BOOLEAN.getJsonSchemaFormat());

        assertEquals("boolean", JsonSchemaType.BOOLEAN_OBJ.getJsonSchemaType());
        assertEquals(Boolean.class, JsonSchemaType.BOOLEAN_OBJ.getJavaType());
    }

    @Test
    void testIntegerTypes() {
        assertEquals("integer", JsonSchemaType.INT.getJsonSchemaType());
        assertEquals(int.class, JsonSchemaType.INT.getJavaType());
        assertNull(JsonSchemaType.INT.getJsonSchemaFormat());

        assertEquals("integer", JsonSchemaType.LONG.getJsonSchemaType());
        assertEquals(long.class, JsonSchemaType.LONG.getJavaType());
        assertEquals("int64", JsonSchemaType.LONG.getJsonSchemaFormat());

        assertEquals("integer", JsonSchemaType.BIG_INTEGER.getJsonSchemaType());
        assertEquals(BigInteger.class, JsonSchemaType.BIG_INTEGER.getJavaType());
        assertEquals("int64", JsonSchemaType.BIG_INTEGER.getJsonSchemaFormat());
    }

    @Test
    void testNumberTypes() {
        assertEquals("number", JsonSchemaType.FLOAT.getJsonSchemaType());
        assertEquals(float.class, JsonSchemaType.FLOAT.getJavaType());
        assertEquals("float", JsonSchemaType.FLOAT.getJsonSchemaFormat());

        assertEquals("number", JsonSchemaType.DOUBLE.getJsonSchemaType());
        assertEquals(double.class, JsonSchemaType.DOUBLE.getJavaType());
        assertEquals("double", JsonSchemaType.DOUBLE.getJsonSchemaFormat());

        assertEquals("number", JsonSchemaType.BIG_DECIMAL.getJsonSchemaType());
        assertEquals(BigDecimal.class, JsonSchemaType.BIG_DECIMAL.getJavaType());
        assertNull(JsonSchemaType.BIG_DECIMAL.getJsonSchemaFormat());
    }

    @Test
    void testStringTypes() {
        assertEquals("string", JsonSchemaType.STRING.getJsonSchemaType());
        assertEquals(String.class, JsonSchemaType.STRING.getJavaType());
        assertNull(JsonSchemaType.STRING.getJsonSchemaFormat());

        assertEquals("string", JsonSchemaType.CHAR.getJsonSchemaType());
        assertEquals(char.class, JsonSchemaType.CHAR.getJavaType());

        assertEquals("string", JsonSchemaType.BYTE_ARRAY.getJsonSchemaType());
        assertEquals(byte[].class, JsonSchemaType.BYTE_ARRAY.getJavaType());
        assertEquals("byte", JsonSchemaType.BYTE_ARRAY.getJsonSchemaFormat());
    }

    @Test
    void testGenericSchemaTypes() {
        assertEquals("object", JsonSchemaType.OBJECT_SCHEMA.getJsonSchemaType());
        assertNull(JsonSchemaType.OBJECT_SCHEMA.getJavaType());

        assertEquals("array", JsonSchemaType.ARRAY_SCHEMA.getJsonSchemaType());
        assertNull(JsonSchemaType.ARRAY_SCHEMA.getJavaType());

        assertEquals("string", JsonSchemaType.STRING_SCHEMA.getJsonSchemaType());
        assertNull(JsonSchemaType.STRING_SCHEMA.getJavaType());
    }

    @Test
    void testDateTimeFormats() {
        assertEquals("date", JsonSchemaType.DATE_FORMAT.getJsonSchemaFormat());
        assertNull(JsonSchemaType.DATE_FORMAT.getJsonSchemaType());

        assertEquals("time", JsonSchemaType.TIME_FORMAT.getJsonSchemaFormat());
        assertNull(JsonSchemaType.TIME_FORMAT.getJsonSchemaType());

        assertEquals("date-time", JsonSchemaType.DATE_TIME_FORMAT.getJsonSchemaFormat());
        assertNull(JsonSchemaType.DATE_TIME_FORMAT.getJsonSchemaType());
    }

    @Test
    void testFromJavaType() {
        assertEquals(JsonSchemaType.STRING, JsonSchemaType.fromJavaType(String.class));
        assertEquals(JsonSchemaType.INT, JsonSchemaType.fromJavaType(int.class));
        assertEquals(JsonSchemaType.INTEGER_OBJ, JsonSchemaType.fromJavaType(Integer.class));
        assertEquals(JsonSchemaType.BOOLEAN, JsonSchemaType.fromJavaType(boolean.class));
        assertEquals(JsonSchemaType.DOUBLE, JsonSchemaType.fromJavaType(double.class));
        assertEquals(JsonSchemaType.BIG_DECIMAL, JsonSchemaType.fromJavaType(BigDecimal.class));

        assertNull(JsonSchemaType.fromJavaType(Object.class));
    }
}
