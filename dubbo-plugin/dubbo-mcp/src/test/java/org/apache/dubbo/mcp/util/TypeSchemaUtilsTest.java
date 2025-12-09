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
package org.apache.dubbo.mcp.util;

import org.apache.dubbo.mcp.JsonSchemaType;
import org.apache.dubbo.mcp.McpConstant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypeSchemaUtilsTest {

    enum TestEnum {
        VALUE1,
        VALUE2,
        VALUE3
    }

    @Test
    void testResolvePrimitiveTypes() {
        TypeSchemaUtils.TypeSchemaInfo intSchema = TypeSchemaUtils.resolveTypeSchema(int.class, int.class, "test int");
        assertEquals(JsonSchemaType.INTEGER_SCHEMA.getJsonSchemaType(), intSchema.getType());
        assertNull(intSchema.getFormat());
        assertEquals("test int", intSchema.getDescription());

        TypeSchemaUtils.TypeSchemaInfo longSchema =
                TypeSchemaUtils.resolveTypeSchema(long.class, long.class, "test long");
        assertEquals(JsonSchemaType.INTEGER_SCHEMA.getJsonSchemaType(), longSchema.getType());
        assertEquals("int64", longSchema.getFormat());

        TypeSchemaUtils.TypeSchemaInfo doubleSchema =
                TypeSchemaUtils.resolveTypeSchema(double.class, double.class, "test double");
        assertEquals(JsonSchemaType.NUMBER_SCHEMA.getJsonSchemaType(), doubleSchema.getType());
        assertEquals("double", doubleSchema.getFormat());

        TypeSchemaUtils.TypeSchemaInfo stringSchema =
                TypeSchemaUtils.resolveTypeSchema(String.class, String.class, "test string");
        assertEquals(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType(), stringSchema.getType());
        assertNull(stringSchema.getFormat());

        TypeSchemaUtils.TypeSchemaInfo booleanSchema =
                TypeSchemaUtils.resolveTypeSchema(boolean.class, boolean.class, "test boolean");
        assertEquals(JsonSchemaType.BOOLEAN_SCHEMA.getJsonSchemaType(), booleanSchema.getType());
    }

    @Test
    void testResolveArrayTypes() {
        TypeSchemaUtils.TypeSchemaInfo arraySchema =
                TypeSchemaUtils.resolveTypeSchema(int[].class, int[].class, "test array");
        assertEquals(JsonSchemaType.ARRAY_SCHEMA.getJsonSchemaType(), arraySchema.getType());
        assertNotNull(arraySchema.getItems());
        assertEquals(
                JsonSchemaType.INTEGER_SCHEMA.getJsonSchemaType(),
                arraySchema.getItems().getType());
    }

    @Test
    void testResolveEnumTypes() {
        TypeSchemaUtils.TypeSchemaInfo enumSchema =
                TypeSchemaUtils.resolveTypeSchema(TestEnum.class, TestEnum.class, "test enum");
        assertEquals(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType(), enumSchema.getType());
        assertNotNull(enumSchema.getEnumValues());
        assertEquals(3, enumSchema.getEnumValues().size());
        assertTrue(enumSchema.getEnumValues().contains("VALUE1"));
        assertTrue(enumSchema.getEnumValues().contains("VALUE2"));
        assertTrue(enumSchema.getEnumValues().contains("VALUE3"));
    }

    @Test
    void testResolveDateTimeTypes() {
        TypeSchemaUtils.TypeSchemaInfo dateSchema =
                TypeSchemaUtils.resolveTypeSchema(Date.class, Date.class, "test date");
        assertEquals(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType(), dateSchema.getType());
        assertEquals(JsonSchemaType.DATE_TIME_FORMAT.getJsonSchemaFormat(), dateSchema.getFormat());

        TypeSchemaUtils.TypeSchemaInfo localDateSchema =
                TypeSchemaUtils.resolveTypeSchema(LocalDate.class, LocalDate.class, "test local date");
        assertEquals(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType(), localDateSchema.getType());
        assertEquals(JsonSchemaType.DATE_FORMAT.getJsonSchemaFormat(), localDateSchema.getFormat());

        TypeSchemaUtils.TypeSchemaInfo localTimeSchema =
                TypeSchemaUtils.resolveTypeSchema(LocalTime.class, LocalTime.class, "test local time");
        assertEquals(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType(), localTimeSchema.getType());
        assertEquals(JsonSchemaType.TIME_FORMAT.getJsonSchemaFormat(), localTimeSchema.getFormat());

        TypeSchemaUtils.TypeSchemaInfo localDateTimeSchema =
                TypeSchemaUtils.resolveTypeSchema(LocalDateTime.class, LocalDateTime.class, "test local datetime");
        assertEquals(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType(), localDateTimeSchema.getType());
        assertEquals(JsonSchemaType.DATE_TIME_FORMAT.getJsonSchemaFormat(), localDateTimeSchema.getFormat());
    }

    @Test
    void testResolveCollectionTypes() {
        TypeSchemaUtils.TypeSchemaInfo listSchema =
                TypeSchemaUtils.resolveTypeSchema(List.class, List.class, "test list");
        assertEquals(JsonSchemaType.ARRAY_SCHEMA.getJsonSchemaType(), listSchema.getType());
        assertNotNull(listSchema.getItems());
        assertEquals(
                JsonSchemaType.STRING_SCHEMA.getJsonSchemaType(),
                listSchema.getItems().getType());

        TypeSchemaUtils.TypeSchemaInfo mapSchema = TypeSchemaUtils.resolveTypeSchema(Map.class, Map.class, "test map");
        assertEquals(JsonSchemaType.OBJECT_SCHEMA.getJsonSchemaType(), mapSchema.getType());
        assertNotNull(mapSchema.getAdditionalProperties());
        assertEquals(
                JsonSchemaType.STRING_SCHEMA.getJsonSchemaType(),
                mapSchema.getAdditionalProperties().getType());
    }

    @Test
    void testResolveComplexObjectTypes() {
        TypeSchemaUtils.TypeSchemaInfo objectSchema =
                TypeSchemaUtils.resolveTypeSchema(Object.class, Object.class, "test object");
        assertEquals(JsonSchemaType.OBJECT_SCHEMA.getJsonSchemaType(), objectSchema.getType());
        // The description includes the provided description plus type information
        assertEquals("test object (POJO type: Object)", objectSchema.getDescription());
    }

    @Test
    void testToSchemaMap() {
        TypeSchemaUtils.TypeSchemaInfo schemaInfo = TypeSchemaUtils.TypeSchemaInfo.builder()
                .type(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType())
                .format("email")
                .description("Email address")
                .build();

        Map<String, Object> schemaMap = TypeSchemaUtils.toSchemaMap(schemaInfo);

        assertEquals(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType(), schemaMap.get(McpConstant.SCHEMA_PROPERTY_TYPE));
        assertEquals("email", schemaMap.get(McpConstant.SCHEMA_PROPERTY_FORMAT));
        assertEquals("Email address", schemaMap.get(McpConstant.SCHEMA_PROPERTY_DESCRIPTION));
    }

    @Test
    void testToSchemaMapWithEnumValues() {
        List<String> enumValues = new ArrayList<>();
        enumValues.add("OPTION1");
        enumValues.add("OPTION2");

        TypeSchemaUtils.TypeSchemaInfo schemaInfo = TypeSchemaUtils.TypeSchemaInfo.builder()
                .type(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType())
                .enumValues(enumValues)
                .description("Enum field")
                .build();

        Map<String, Object> schemaMap = TypeSchemaUtils.toSchemaMap(schemaInfo);

        assertEquals(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType(), schemaMap.get(McpConstant.SCHEMA_PROPERTY_TYPE));
        assertEquals(enumValues, schemaMap.get(McpConstant.SCHEMA_PROPERTY_ENUM));
        assertEquals("Enum field", schemaMap.get(McpConstant.SCHEMA_PROPERTY_DESCRIPTION));
    }

    @Test
    void testToSchemaMapWithArrayItems() {
        TypeSchemaUtils.TypeSchemaInfo itemSchema = TypeSchemaUtils.TypeSchemaInfo.builder()
                .type(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType())
                .description("Array item")
                .build();

        TypeSchemaUtils.TypeSchemaInfo arraySchema = TypeSchemaUtils.TypeSchemaInfo.builder()
                .type(JsonSchemaType.ARRAY_SCHEMA.getJsonSchemaType())
                .items(itemSchema)
                .description("Array field")
                .build();

        Map<String, Object> schemaMap = TypeSchemaUtils.toSchemaMap(arraySchema);

        assertEquals(JsonSchemaType.ARRAY_SCHEMA.getJsonSchemaType(), schemaMap.get(McpConstant.SCHEMA_PROPERTY_TYPE));
        assertEquals("Array field", schemaMap.get(McpConstant.SCHEMA_PROPERTY_DESCRIPTION));

        @SuppressWarnings("unchecked")
        Map<String, Object> itemsMap = (Map<String, Object>) schemaMap.get(McpConstant.SCHEMA_PROPERTY_ITEMS);
        assertNotNull(itemsMap);
        assertEquals(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType(), itemsMap.get(McpConstant.SCHEMA_PROPERTY_TYPE));
    }

    @Test
    void testUtilityMethods() {
        assertTrue(TypeSchemaUtils.isPrimitiveType(int.class));
        assertTrue(TypeSchemaUtils.isPrimitiveType(String.class));
        assertFalse(TypeSchemaUtils.isPrimitiveType(Object.class));

        assertEquals(
                JsonSchemaType.INTEGER_SCHEMA.getJsonSchemaType(),
                TypeSchemaUtils.getPrimitiveJsonSchemaType(int.class));
        assertEquals(
                JsonSchemaType.STRING_SCHEMA.getJsonSchemaType(),
                TypeSchemaUtils.getPrimitiveJsonSchemaType(String.class));

        assertEquals("int64", TypeSchemaUtils.getFormatForType(long.class));
        assertNull(TypeSchemaUtils.getFormatForType(int.class));

        assertTrue(TypeSchemaUtils.isDateTimeType(Date.class));
        assertTrue(TypeSchemaUtils.isDateTimeType(LocalDate.class));
        assertFalse(TypeSchemaUtils.isDateTimeType(String.class));

        assertEquals(
                JsonSchemaType.DATE_FORMAT.getJsonSchemaFormat(), TypeSchemaUtils.getDateTimeFormat(LocalDate.class));
        assertEquals(
                JsonSchemaType.TIME_FORMAT.getJsonSchemaFormat(), TypeSchemaUtils.getDateTimeFormat(LocalTime.class));
        assertEquals(
                JsonSchemaType.DATE_TIME_FORMAT.getJsonSchemaFormat(),
                TypeSchemaUtils.getDateTimeFormat(LocalDateTime.class));

        assertEquals(String.class, TypeSchemaUtils.getClassFromType(String.class));
        assertEquals(Object.class, TypeSchemaUtils.getClassFromType(Object.class));

        assertTrue(TypeSchemaUtils.isPrimitiveOrWrapper(int.class));
        assertTrue(TypeSchemaUtils.isPrimitiveOrWrapper(Integer.class));
        assertFalse(TypeSchemaUtils.isPrimitiveOrWrapper(Object.class));
    }
}
