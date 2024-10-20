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
package org.apache.dubbo.metadata.swagger;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.swagger.model.Components;
import org.apache.dubbo.metadata.swagger.model.Operation;
import org.apache.dubbo.metadata.swagger.model.media.ComposedSchema;
import org.apache.dubbo.metadata.swagger.model.media.Content;
import org.apache.dubbo.metadata.swagger.model.media.MediaType;
import org.apache.dubbo.metadata.swagger.model.media.Schema;
import org.apache.dubbo.metadata.swagger.model.responses.ApiResponse;
import org.apache.dubbo.metadata.swagger.model.responses.ApiResponses;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static org.apache.dubbo.metadata.swagger.SwaggerConstants.COMPONENTS_REF;
import static org.apache.dubbo.metadata.swagger.SwaggerConstants.DEFAULT_DESCRIPTION;

public class ResponseService {

    private Map<String, ApiResponse> genericMapResponse = new LinkedHashMap<>();

    public ApiResponses build(
            Components components, HandlerMeta handlerMeta, Operation operation, MethodAttributes methodAttributes) {
        final ApiResponses apiResponses = this.getApiResponses(operation);
        // for each one build ApiResponse and add it to existing responses
        for (Entry<String, ApiResponse> entry : genericMapResponse.entrySet()) {
            apiResponses.addApiResponse(entry.getKey(), entry.getValue());
        }

        // Fill api Responses
        this.computeResponse(components, handlerMeta.getMethod(), apiResponses, methodAttributes, false);
        return apiResponses;
    }

    private ApiResponses computeResponse(
            Components components,
            MethodMeta method,
            ApiResponses apiResponses,
            MethodAttributes methodAttributes,
            boolean isGeneric) {
        // Parsing documentation, if present

        if (!CollectionUtils.isEmptyMap(apiResponses) && (apiResponses.size() != genericMapResponse.size())) {
            // API Responses at operation and @ApiResponse annotation
            for (Map.Entry<String, ApiResponse> entry : apiResponses.entrySet()) {
                String httpCode = entry.getKey();
                ApiResponse apiResponse = entry.getValue();
                buildApiResponses(components, method, apiResponses, methodAttributes, httpCode, apiResponse, isGeneric);
            }
        } else {
            // Use response parameters with no description filled - No documentation
            // available
            String httpCode = HttpStatus.OK.toString();
            ApiResponse apiResponse =
                    genericMapResponse.containsKey(httpCode) ? genericMapResponse.get(httpCode) : new ApiResponse();
            if (httpCode != null)
                buildApiResponses(components, method, apiResponses, methodAttributes, httpCode, apiResponse, isGeneric);
        }
        return apiResponses;
    }

    private void buildApiResponses(
            Components components,
            MethodMeta method,
            ApiResponses apiResponsesOp,
            MethodAttributes methodAttributes,
            String httpCode,
            ApiResponse apiResponse,
            boolean isGeneric) {
        // No documentation
        if (StringUtils.isBlank(apiResponse.get$ref())) {
            if (apiResponse.getContent() == null) {
                Content content = this.buildContent(components, method, methodAttributes.getMethodProduces());
                apiResponse.setContent(content);
            } else if (CollectionUtils.isEmptyMap(apiResponse.getContent())) {
                apiResponse.setContent(null);
            }
            if (StringUtils.isBlank(apiResponse.getDescription())) {
                apiResponse.setDescription(DEFAULT_DESCRIPTION);
            }
        }
        if (apiResponse.getContent() != null && ((isGeneric || methodAttributes.isMethodOverloaded()))) {
            // Merge with existing schema
            Content existingContent = apiResponse.getContent();
            Schema<?> schemaN = this.calculateSchema(components, method.getGenericReturnType());
            if (schemaN != null && ArrayUtils.isNotEmpty(methodAttributes.getMethodProduces())) {
                Arrays.stream(methodAttributes.getMethodProduces())
                        .forEach(mediaTypeStr -> this.mergeSchema(existingContent, schemaN, mediaTypeStr));
            }
        }
        apiResponsesOp.addApiResponse(httpCode, apiResponse);
    }

    private void mergeSchema(Content existingContent, Schema<?> schemaN, String mediaTypeStr) {
        if (existingContent.containsKey(mediaTypeStr)) {
            MediaType mediaType = existingContent.get(mediaTypeStr);
            if (!schemaN.equals(mediaType.getSchema())) {
                // Merge the two schemas for the same mediaType
                Schema firstSchema = mediaType.getSchema();
                ComposedSchema schemaObject;
                if (firstSchema instanceof ComposedSchema) {
                    schemaObject = (ComposedSchema) firstSchema;
                    List<Schema> listOneOf = schemaObject.getOneOf();
                    if (!CollectionUtils.isEmpty(listOneOf) && !listOneOf.contains(schemaN))
                        schemaObject.addOneOfItem(schemaN);
                } else {
                    schemaObject = new ComposedSchema();
                    schemaObject.addOneOfItem(schemaN);
                    schemaObject.addOneOfItem(firstSchema);
                }
                mediaType.setSchema(schemaObject);
                existingContent.addMediaType(mediaTypeStr, mediaType);
            }
        } else {
            // Add the new schema for a different mediaType
            existingContent.addMediaType(mediaTypeStr, new MediaType().schema(schemaN));
        }
    }

    public Schema<?> calculateSchema(Components components, Type returnType) {
        if (isVoid(returnType)) {
            return null;
        }

        Schema<?> schemaN = null;

        if (returnType instanceof ParameterizedType) {
            schemaN = calculateSchemaFromParameterizedType(components, (ParameterizedType) returnType);
        } else {
            schemaN = extractSchema(components, returnType);
        }

        return schemaN;
    }

    private Schema<?> extractSchema(Components components, Type type) {
        if (type == null) {
            return null;
        }

        if (type == String.class) {
            Schema<String> schema = new Schema<>();
            schema.setType("string");
            return schema;
        } else if (type == Integer.class || type == int.class) {
            Schema<Integer> schema = new Schema<>();
            schema.setType("integer");
            schema.setFormat("int32");
            return schema;
        } else if (type == Long.class || type == long.class) {
            Schema<Long> schema = new Schema<>();
            schema.setType("integer");
            schema.setFormat("int64");
            return schema;
        } else if (type == Short.class || type == short.class) {
            Schema<Integer> schema = new Schema<>();
            schema.setType("integer");
            schema.setFormat("int32");
            return schema;
        } else if (type == Byte.class || type == byte.class) {
            Schema<Integer> schema = new Schema<>();
            schema.setType("integer");
            schema.setFormat("int32");
            return schema;
        } else if (type == Boolean.class || type == boolean.class) {
            Schema<Boolean> schema = new Schema<>();
            schema.setType("boolean");
            return schema;
        } else if (type == Double.class || type == double.class) {
            Schema<Double> schema = new Schema<>();
            schema.setType("number");
            schema.setFormat("double");
            return schema;
        } else if (type == Float.class || type == float.class) {
            Schema<Float> schema = new Schema<>();
            schema.setType("number");
            schema.setFormat("float");
            return schema;
        } else if (type == BigDecimal.class) {
            Schema<BigDecimal> schema = new Schema<>();
            schema.setType("number");
            schema.setFormat("double");
            return schema;
        } else if (type == BigInteger.class) {
            Schema<BigInteger> schema = new Schema<>();
            schema.setType("integer");
            schema.setFormat("int64");
            return schema;
        } else if (type == Date.class) {
            Schema<Date> schema = new Schema<>();
            schema.setType("string");
            schema.setFormat("date-time");
            return schema;
        } else if (type == LocalDate.class) {
            Schema<String> schema = new Schema<>();
            schema.setType("string");
            schema.setFormat("date");
            return schema;
        } else if (type == LocalDateTime.class) {
            Schema<String> schema = new Schema<>();
            schema.setType("string");
            schema.setFormat("date-time");
            return schema;
        } else if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            String schemaName = clazz.getSimpleName();

            if (components.getSchemas() != null && components.getSchemas().containsKey(schemaName)) {
                Schema<?> refSchema = new Schema<>();
                refSchema.$ref(COMPONENTS_REF + schemaName);
                return refSchema;
            }

            if (clazz.isEnum()) {
                Schema<String> schema = new Schema<>();
                schema.setType("string");
                List<String> enumValues = Arrays.stream(clazz.getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.toList());
                schema.setEnum(enumValues);

                components.addSchemas(schemaName, schema);

                Schema<?> refSchema = new Schema<>();
                refSchema.$ref(COMPONENTS_REF + schemaName);
                return refSchema;
            } else {
                Schema<Object> schema = new Schema<>();
                schema.setType("object");
                Map<String, Schema> properties = extractPropertiesSchema(components, clazz);
                schema.setProperties(properties);

                components.addSchemas(schemaName, schema);

                Schema<?> refSchema = new Schema<>();
                refSchema.$ref(COMPONENTS_REF + schemaName);
                return refSchema;
            }
        } else if (type instanceof ParameterizedType) {
            return calculateSchemaFromParameterizedType(components, (ParameterizedType) type);
        } else {
            Schema<String> schema = new Schema<>();
            schema.setType("string");
            return schema;
        }
    }

    private Schema<?> calculateSchemaFromParameterizedType(Components components, ParameterizedType parameterizedType) {
        Type rawType = parameterizedType.getRawType();
        Type[] typeArguments = parameterizedType.getActualTypeArguments();

        if (rawType == List.class || rawType == Collection.class) {
            // 处理列表类型
            Schema<Object> schema = new Schema<>();
            schema.setType("array");
            Schema<?> itemsSchema = extractSchema(components, typeArguments[0]);
            schema.setItems(itemsSchema);
            return schema;
        } else if (rawType == Map.class) {
            // 处理映射类型
            Schema<Object> schema = new Schema<>();
            schema.setType("object");
            // 对于 Map 类型，我们可以设置 additionalProperties
            Schema<?> valueSchema = extractSchema(components, typeArguments[1]);
            schema.setAdditionalProperties(valueSchema);
            return schema;
        } else {
            // 其他参数化类型，按对象处理
            Schema<Object> schema = new Schema<>();
            schema.setType("object");
            // 如果需要，可以进一步处理泛型类型
            return schema;
        }
    }

    private Map<String, Schema> extractPropertiesSchema(Components components, Class<?> clazz) {
        Map<String, Schema> properties = new LinkedHashMap<>();

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            String fieldName = field.getName();
            Type fieldType = field.getGenericType();
            Schema<?> fieldSchema = extractSchema(components, fieldType);
            properties.put(fieldName, fieldSchema);
        }

        return properties;
    }

    private ApiResponses getApiResponses(Operation operation) {
        ApiResponses apiResponses = operation.getResponses();
        if (apiResponses == null) apiResponses = new ApiResponses();
        return apiResponses;
    }

    private boolean isVoid(Type returnType) {
        if (returnType == Void.TYPE) {
            return true;
        }
        if (returnType instanceof Class<?>) {
            return Void.class.equals(returnType);
        }
        return false;
    }

    private Content buildContent(Components components, MethodMeta method, String[] methodProduces) {
        Content content = new Content();
        Type returnType = method.getReturnType();
        if (isVoid(returnType)) {
            // if void, no content
            content = null;
        } else if (ArrayUtils.isNotEmpty(methodProduces)) {
            Schema<?> schemaN = this.calculateSchema(components, returnType);
            if (schemaN != null) {
                MediaType mediaType = new MediaType();
                mediaType.setSchema(schemaN);
                // Fill the content
                setContent(methodProduces, content, mediaType);
            }
        }
        return content;
    }

    private void setContent(String[] methodProduces, Content content, MediaType mediaType) {
        Arrays.stream(methodProduces).forEach(mediaTypeStr -> content.addMediaType(mediaTypeStr, mediaType));
    }
}
