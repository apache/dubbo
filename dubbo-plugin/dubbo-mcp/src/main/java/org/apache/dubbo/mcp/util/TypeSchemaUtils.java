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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TypeSchemaUtils {

    public static TypeSchemaInfo resolveTypeSchema(Class<?> javaType, Type genericType, String description) {
        TypeSchemaInfo.Builder builder =
                TypeSchemaInfo.builder().description(description).javaType(javaType.getName());

        if (isPrimitiveType(javaType)) {
            return builder.type(getPrimitiveJsonSchemaType(javaType))
                    .format(getFormatForType(javaType))
                    .build();
        }

        if (javaType.isArray()) {
            TypeSchemaInfo itemSchema =
                    resolveTypeSchema(javaType.getComponentType(), javaType.getComponentType(), "Array item");
            return builder.type(JsonSchemaType.ARRAY_SCHEMA.getJsonSchemaType())
                    .items(itemSchema)
                    .build();
        }

        if (genericType instanceof ParameterizedType) {
            return resolveParameterizedType((ParameterizedType) genericType, javaType, description, builder);
        }

        if (java.util.Collection.class.isAssignableFrom(javaType)) {
            return builder.type(JsonSchemaType.ARRAY_SCHEMA.getJsonSchemaType())
                    .items(TypeSchemaInfo.builder()
                            .type(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType())
                            .description("Collection item")
                            .build())
                    .build();
        }

        if (java.util.Map.class.isAssignableFrom(javaType)) {
            return builder.type(JsonSchemaType.OBJECT_SCHEMA.getJsonSchemaType())
                    .additionalProperties(TypeSchemaInfo.builder()
                            .type(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType())
                            .description("Map value")
                            .build())
                    .build();
        }

        if (javaType.isEnum()) {
            Object[] enumConstants = javaType.getEnumConstants();
            List<String> enumValues = new ArrayList<>();
            if (enumConstants != null) {
                for (Object enumConstant : enumConstants) {
                    enumValues.add(enumConstant.toString());
                }
            }
            return builder.type(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType())
                    .enumValues(enumValues)
                    .build();
        }

        if (isDateTimeType(javaType)) {
            return builder.type(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType())
                    .format(getDateTimeFormat(javaType))
                    .build();
        }

        return builder.type(JsonSchemaType.OBJECT_SCHEMA.getJsonSchemaType())
                .description(
                        description != null
                                ? description + " (POJO type: " + javaType.getSimpleName() + ")"
                                : "Complex object of type " + javaType.getSimpleName())
                .build();
    }

    private static TypeSchemaInfo resolveParameterizedType(
            ParameterizedType paramType, Class<?> rawType, String description, TypeSchemaInfo.Builder builder) {

        Type[] typeArgs = paramType.getActualTypeArguments();

        if (java.util.Collection.class.isAssignableFrom(rawType)) {
            TypeSchemaInfo itemSchema;
            if (typeArgs.length > 0) {
                Class<?> itemType = getClassFromType(typeArgs[0]);
                itemSchema = resolveTypeSchema(itemType, typeArgs[0], "Collection item");
            } else {
                itemSchema = TypeSchemaInfo.builder()
                        .type(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType())
                        .description("Collection item")
                        .build();
            }
            return builder.type(JsonSchemaType.ARRAY_SCHEMA.getJsonSchemaType())
                    .items(itemSchema)
                    .build();
        }

        if (java.util.Map.class.isAssignableFrom(rawType)) {
            TypeSchemaInfo valueSchema;
            if (typeArgs.length > 1) {
                Class<?> valueType = getClassFromType(typeArgs[1]);
                valueSchema = resolveTypeSchema(valueType, typeArgs[1], "Map value");
            } else {
                valueSchema = TypeSchemaInfo.builder()
                        .type(JsonSchemaType.STRING_SCHEMA.getJsonSchemaType())
                        .description("Map value")
                        .build();
            }
            return builder.type(JsonSchemaType.OBJECT_SCHEMA.getJsonSchemaType())
                    .additionalProperties(valueSchema)
                    .build();
        }

        return builder.type(JsonSchemaType.OBJECT_SCHEMA.getJsonSchemaType())
                .description(
                        description != null
                                ? description + " (Generic type: " + rawType.getSimpleName() + ")"
                                : "Complex generic object of type " + rawType.getSimpleName())
                .build();
    }

    public static Map<String, Object> toSchemaMap(TypeSchemaInfo schemaInfo) {
        Map<String, Object> schemaMap = new HashMap<>();

        schemaMap.put(McpConstant.SCHEMA_PROPERTY_TYPE, schemaInfo.getType());

        if (schemaInfo.getFormat() != null) {
            schemaMap.put(McpConstant.SCHEMA_PROPERTY_FORMAT, schemaInfo.getFormat());
        }

        if (schemaInfo.getDescription() != null) {
            schemaMap.put(McpConstant.SCHEMA_PROPERTY_DESCRIPTION, schemaInfo.getDescription());
        }

        if (schemaInfo.getEnumValues() != null && !schemaInfo.getEnumValues().isEmpty()) {
            schemaMap.put(McpConstant.SCHEMA_PROPERTY_ENUM, schemaInfo.getEnumValues());
        }

        if (schemaInfo.getItems() != null) {
            schemaMap.put(McpConstant.SCHEMA_PROPERTY_ITEMS, toSchemaMap(schemaInfo.getItems()));
        }

        if (schemaInfo.getAdditionalProperties() != null) {
            schemaMap.put(
                    McpConstant.SCHEMA_PROPERTY_ADDITIONAL_PROPERTIES,
                    toSchemaMap(schemaInfo.getAdditionalProperties()));
        }

        return schemaMap;
    }

    public static TypeSchemaInfo resolveNestedType(Type type, String description) {
        if (type instanceof Class) {
            return resolveTypeSchema((Class<?>) type, type, description);
        }

        if (type instanceof ParameterizedType paramType) {
            Class<?> rawType = (Class<?>) paramType.getRawType();
            return resolveTypeSchema(rawType, type, description);
        }

        if (type instanceof TypeVariable) {
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            if (bounds.length > 0) {
                return resolveNestedType(bounds[0], description);
            }
        }

        if (type instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            if (upperBounds.length > 0) {
                return resolveNestedType(upperBounds[0], description);
            }
        }

        if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            TypeSchemaInfo itemSchema = resolveNestedType(componentType, "Array item");
            return TypeSchemaInfo.builder()
                    .type(JsonSchemaType.ARRAY_SCHEMA.getJsonSchemaType())
                    .items(itemSchema)
                    .description(description)
                    .build();
        }

        // Fallback to object type
        return TypeSchemaInfo.builder()
                .type(JsonSchemaType.OBJECT_SCHEMA.getJsonSchemaType())
                .description(description != null ? description : "Unknown type")
                .build();
    }

    public static boolean isPrimitiveType(Class<?> type) {
        return JsonSchemaType.fromJavaType(type) != null;
    }

    public static String getPrimitiveJsonSchemaType(Class<?> javaType) {
        JsonSchemaType mapping = JsonSchemaType.fromJavaType(javaType);
        return mapping != null ? mapping.getJsonSchemaType() : JsonSchemaType.STRING_SCHEMA.getJsonSchemaType();
    }

    public static String getFormatForType(Class<?> javaType) {
        JsonSchemaType mapping = JsonSchemaType.fromJavaType(javaType);
        return mapping != null ? mapping.getJsonSchemaFormat() : null;
    }

    public static boolean isDateTimeType(Class<?> type) {
        return java.util.Date.class.isAssignableFrom(type)
                || java.time.temporal.Temporal.class.isAssignableFrom(type)
                || java.util.Calendar.class.isAssignableFrom(type);
    }

    public static String getDateTimeFormat(Class<?> type) {
        if (java.time.LocalDate.class.isAssignableFrom(type)) {
            return JsonSchemaType.DATE_FORMAT.getJsonSchemaFormat();
        }
        if (java.time.LocalTime.class.isAssignableFrom(type) || java.time.OffsetTime.class.isAssignableFrom(type)) {
            return JsonSchemaType.TIME_FORMAT.getJsonSchemaFormat();
        }
        return JsonSchemaType.DATE_TIME_FORMAT.getJsonSchemaFormat();
    }

    public static Class<?> getClassFromType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            Class<?> componentClass = getClassFromType(componentType);
            return java.lang.reflect.Array.newInstance(componentClass, 0).getClass();
        }
        if (type instanceof TypeVariable) {
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            if (bounds.length > 0) {
                return getClassFromType(bounds[0]);
            }
        }
        if (type instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            if (upperBounds.length > 0) {
                return getClassFromType(upperBounds[0]);
            }
        }
        return Object.class;
    }

    public static boolean isPrimitiveOrWrapper(Class<?> type) {
        return isPrimitiveType(type);
    }

    public static class TypeSchemaInfo {
        private final String type;
        private final String format;
        private final String description;
        private final String javaType;
        private final List<String> enumValues;
        private final TypeSchemaInfo items;
        private final TypeSchemaInfo additionalProperties;

        private TypeSchemaInfo(Builder builder) {
            this.type = builder.type;
            this.format = builder.format;
            this.description = builder.description;
            this.javaType = builder.javaType;
            this.enumValues = builder.enumValues;
            this.items = builder.items;
            this.additionalProperties = builder.additionalProperties;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getType() {
            return type;
        }

        public String getFormat() {
            return format;
        }

        public String getDescription() {
            return description;
        }

        public String getJavaType() {
            return javaType;
        }

        public List<String> getEnumValues() {
            return enumValues;
        }

        public TypeSchemaInfo getItems() {
            return items;
        }

        public TypeSchemaInfo getAdditionalProperties() {
            return additionalProperties;
        }

        public static class Builder {
            private String type;
            private String format;
            private String description;
            private String javaType;
            private List<String> enumValues;
            private TypeSchemaInfo items;
            private TypeSchemaInfo additionalProperties;

            public Builder type(String type) {
                this.type = type;
                return this;
            }

            public Builder format(String format) {
                this.format = format;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder javaType(String javaType) {
                this.javaType = javaType;
                return this;
            }

            public Builder enumValues(List<String> enumValues) {
                this.enumValues = enumValues;
                return this;
            }

            public Builder items(TypeSchemaInfo items) {
                this.items = items;
                return this;
            }

            public Builder additionalProperties(TypeSchemaInfo additionalProperties) {
                this.additionalProperties = additionalProperties;
                return this;
            }

            public TypeSchemaInfo build() {
                return new TypeSchemaInfo(this);
            }
        }
    }
}
