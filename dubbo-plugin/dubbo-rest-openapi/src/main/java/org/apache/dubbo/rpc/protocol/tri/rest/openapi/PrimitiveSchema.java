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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi;

import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema.Type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Primitive Schema
 * Format: <a href="https://spec.openapis.org/registry/format/">Formats Registry</a>
 */
public enum PrimitiveSchema {
    STRING(String.class, Type.STRING),
    BOOLEAN(Boolean.class, Type.BOOLEAN),
    BYTE(Byte.class, Type.STRING, "byte"),
    BINARY(Byte.class, Type.STRING, "binary"),
    URI(java.net.URI.class, Type.STRING, "uri"),
    URL(java.net.URL.class, Type.STRING, "url"),
    EMAIL(String.class, Type.STRING, "email"),
    PASSWORD(String.class, Type.STRING, "password"),
    UUID(java.util.UUID.class, Type.STRING, "uuid"),
    INT(Integer.class, Type.INTEGER, "int32"),
    LONG(Long.class, Type.INTEGER, "int64"),
    FLOAT(Float.class, Type.NUMBER, "float"),
    DOUBLE(Double.class, Type.NUMBER, "double"),
    INTEGER(java.math.BigInteger.class, Type.INTEGER),
    DECIMAL(java.math.BigDecimal.class, Type.NUMBER, "number"),
    NUMBER(Number.class, Type.NUMBER),
    IP_V4(java.net.Inet4Address.class, Type.STRING, "ipv4"),
    IP_V6(java.net.Inet6Address.class, Type.STRING, "ipv6"),
    DATE_TIME(java.util.Date.class, Type.STRING, "date-time"),
    DATE(java.time.LocalDate.class, Type.STRING, "date"),
    TIME(java.time.LocalTime.class, Type.STRING, "time"),
    DURATION(java.time.Duration.class, Type.STRING, "duration"),
    FILE(java.io.File.class, Type.STRING, "binary"),
    OBJECT(Object.class, Type.OBJECT),
    ARRAY(Object[].class, Type.ARRAY);

    private static final Map<Object, PrimitiveSchema> TYPE_MAPPING = new ConcurrentHashMap<>();

    static {
        for (PrimitiveSchema schema : values()) {
            TYPE_MAPPING.putIfAbsent(schema.keyClass, schema);
        }
        TYPE_MAPPING.put(boolean.class, BOOLEAN);
        TYPE_MAPPING.put(byte.class, BYTE);
        TYPE_MAPPING.put(char.class, STRING);
        TYPE_MAPPING.put(Character.class, STRING);
        TYPE_MAPPING.put(short.class, INT);
        TYPE_MAPPING.put(Short.class, INT);
        TYPE_MAPPING.put(int.class, INT);
        TYPE_MAPPING.put(long.class, LONG);
        TYPE_MAPPING.put(float.class, FLOAT);
        TYPE_MAPPING.put(double.class, DOUBLE);
        TYPE_MAPPING.put(byte[].class, BYTE);

        TYPE_MAPPING.put(java.util.Calendar.class, DATE_TIME);
        TYPE_MAPPING.put(java.sql.Date.class, DATE_TIME);
        TYPE_MAPPING.put(java.time.Instant.class, DATE_TIME);
        TYPE_MAPPING.put(java.time.LocalDateTime.class, DATE_TIME);
        TYPE_MAPPING.put(java.time.ZonedDateTime.class, DATE_TIME);
        TYPE_MAPPING.put(java.time.OffsetDateTime.class, DATE_TIME);
        TYPE_MAPPING.put(java.time.OffsetTime.class, TIME);
        TYPE_MAPPING.put(java.time.Period.class, DURATION);
        TYPE_MAPPING.put("javax.xml.datatype.XMLGregorianCalendar", DATE_TIME);
        TYPE_MAPPING.put("org.joda.time.LocalDateTime", DATE_TIME);
        TYPE_MAPPING.put("org.joda.time.ReadableDateTime", DATE_TIME);
        TYPE_MAPPING.put("org.joda.time.DateTime", DATE_TIME);
        TYPE_MAPPING.put("org.joda.time.LocalTime", TIME);

        TYPE_MAPPING.put(CharSequence.class, STRING);
        TYPE_MAPPING.put(StringBuffer.class, STRING);
        TYPE_MAPPING.put(StringBuilder.class, STRING);
        TYPE_MAPPING.put(java.nio.charset.Charset.class, STRING);
        TYPE_MAPPING.put(java.time.ZoneId.class, STRING);
        TYPE_MAPPING.put(java.util.Currency.class, STRING);
        TYPE_MAPPING.put(java.util.Locale.class, STRING);
        TYPE_MAPPING.put(java.util.TimeZone.class, STRING);
        TYPE_MAPPING.put(java.util.regex.Pattern.class, STRING);

        TYPE_MAPPING.put(java.io.InputStream.class, BYTE);
        TYPE_MAPPING.put(java.net.InetAddress.class, IP_V4);

        TYPE_MAPPING.put("object", OBJECT);
    }

    private final Class<?> keyClass;
    private final Type type;
    private final String format;

    PrimitiveSchema(Class<?> keyClass, Type type, String format) {
        this.keyClass = keyClass;
        this.type = type;
        this.format = format;
    }

    PrimitiveSchema(Class<?> keyClass, Type type) {
        this.keyClass = keyClass;
        this.type = type;
        format = null;
    }

    public Schema newSchema() {
        return new Schema().setType(type).setFormat(format);
    }

    public static Schema newSchemaOf(Class<?> type) {
        PrimitiveSchema schema = TYPE_MAPPING.get(type);
        if (schema == null) {
            schema = TYPE_MAPPING.get(type.getName());
        }
        return schema == null ? null : schema.newSchema();
    }

    public static boolean isPrimitive(Class<?> type) {
        return TYPE_MAPPING.containsKey(type);
    }

    public static void addTypeMapping(Object key, PrimitiveSchema schema) {
        TYPE_MAPPING.put(key, schema);
    }
}
