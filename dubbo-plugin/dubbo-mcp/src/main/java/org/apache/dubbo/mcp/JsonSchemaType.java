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
import java.util.HashMap;
import java.util.Map;

public enum JsonSchemaType {
    BOOLEAN(boolean.class, "boolean", null),
    BOOLEAN_OBJ(Boolean.class, "boolean", null),

    BYTE(byte.class, "integer", null),
    BYTE_OBJ(Byte.class, "integer", null),
    SHORT(short.class, "integer", null),
    SHORT_OBJ(Short.class, "integer", null),
    INT(int.class, "integer", null),
    INTEGER_OBJ(Integer.class, "integer", null),
    LONG(long.class, "integer", "int64"),
    LONG_OBJ(Long.class, "integer", "int64"),
    BIG_INTEGER(BigInteger.class, "integer", "int64"),

    FLOAT(float.class, "number", "float"),
    FLOAT_OBJ(Float.class, "number", "float"),
    DOUBLE(double.class, "number", "double"),
    DOUBLE_OBJ(Double.class, "number", "double"),
    BIG_DECIMAL(BigDecimal.class, "number", null), // BigDecimal often without specific format

    CHAR(char.class, "string", null),
    CHARACTER_OBJ(Character.class, "string", null),
    STRING(String.class, "string", null),
    CHAR_SEQUENCE(CharSequence.class, "string", null),
    STRING_BUFFER(StringBuffer.class, "string", null),
    STRING_BUILDER(StringBuilder.class, "string", null),
    BYTE_ARRAY(byte[].class, "string", "byte"),

    // Generic JSON Schema Types
    OBJECT_SCHEMA(null, "object", null),
    ARRAY_SCHEMA(null, "array", null),
    STRING_SCHEMA(null, "string", null),
    NUMBER_SCHEMA(null, "number", null),
    INTEGER_SCHEMA(null, "integer", null),
    BOOLEAN_SCHEMA(null, "boolean", null),

    // Date/Time Formats
    DATE_FORMAT(null, null, "date"),
    TIME_FORMAT(null, null, "time"),
    DATE_TIME_FORMAT(null, null, "date-time");

    private final Class<?> javaType;
    private final String jsonSchemaType;
    private final String jsonSchemaFormat;

    private static final Map<Class<?>, JsonSchemaType> javaTypeLookup = new HashMap<>();

    static {
        for (JsonSchemaType mapping : values()) {
            javaTypeLookup.put(mapping.javaType, mapping);
        }
    }

    JsonSchemaType(Class<?> javaType, String jsonSchemaType, String jsonSchemaFormat) {
        this.javaType = javaType;
        this.jsonSchemaType = jsonSchemaType;
        this.jsonSchemaFormat = jsonSchemaFormat;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public String getJsonSchemaType() {
        return jsonSchemaType;
    }

    public String getJsonSchemaFormat() {
        return jsonSchemaFormat;
    }

    public static JsonSchemaType fromJavaType(Class<?> type) {
        return javaTypeLookup.get(type);
    }
}
