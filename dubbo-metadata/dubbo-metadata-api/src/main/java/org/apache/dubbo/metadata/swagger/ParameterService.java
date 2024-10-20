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

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.swagger.model.Components;
import org.apache.dubbo.metadata.swagger.model.media.ArraySchema;
import org.apache.dubbo.metadata.swagger.model.media.FileSchema;
import org.apache.dubbo.metadata.swagger.model.media.ObjectSchema;
import org.apache.dubbo.metadata.swagger.model.media.Schema;
import org.apache.dubbo.metadata.swagger.model.parameters.Parameter;
import org.apache.dubbo.metadata.swagger.model.parameters.ParameterInfo;
import org.apache.dubbo.metadata.swagger.model.parameters.RequestBodyInfo;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

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
import java.util.stream.Collectors;

import static org.apache.dubbo.metadata.swagger.SwaggerConstants.COMPONENTS_REF;

public class ParameterService {

    public static Parameter mergeParameter(List<Parameter> operationParameters, Parameter parameter) {
        Parameter result = parameter;
        if (parameter != null && parameter.getName() != null) {
            final String name = parameter.getName();
            final String in = parameter.getIn();

            Parameter paramDoc = operationParameters.stream()
                    .filter(p -> name.equals(p.getName())
                            && (StringUtils.isEmpty(in) || StringUtils.isEmpty(p.getIn()) || in.equals(p.getIn())))
                    .findAny()
                    .orElse(null);
            if (paramDoc != null) {
                mergeParameter(parameter, paramDoc);
                result = paramDoc;
            } else {
                operationParameters.add(result);
            }
        }
        return result;
    }

    public static void mergeParameter(Parameter parameter, Parameter paramDoc) {
        if (StringUtils.isBlank(paramDoc.getDescription())) {
            paramDoc.setDescription(parameter.getDescription());
        }

        if (StringUtils.isBlank(paramDoc.getIn())) {
            paramDoc.setIn(parameter.getIn());
        }

        if (paramDoc.getExample() == null) {
            paramDoc.setExample(parameter.getExample());
        }

        if (paramDoc.getDeprecated() == null) {
            paramDoc.setDeprecated(parameter.getDeprecated());
        }

        if (paramDoc.getRequired() == null) {
            paramDoc.setRequired(parameter.getRequired());
        }

        if (paramDoc.getAllowEmptyValue() == null) {
            paramDoc.setAllowEmptyValue(parameter.getAllowEmptyValue());
        }

        if (paramDoc.getAllowReserved() == null) {
            paramDoc.setAllowReserved(parameter.getAllowReserved());
        }

        if (StringUtils.isBlank(paramDoc.get$ref())) {
            paramDoc.set$ref(paramDoc.get$ref());
        }

        if (paramDoc.getSchema() == null && paramDoc.getContent() == null) {
            paramDoc.setSchema(parameter.getSchema());
        }

        if (paramDoc.getExtensions() == null) {
            paramDoc.setExtensions(parameter.getExtensions());
        }

        if (paramDoc.getStyle() == null) {
            paramDoc.setStyle(parameter.getStyle());
        }

        if (paramDoc.getExplode() == null) {
            paramDoc.setExplode(parameter.getExplode());
        }
    }

    public Schema<?> calculateSchema(
            Components components, ParameterInfo parameterInfo, RequestBodyInfo requestBodyInfo) {
        Schema<?> schemaN;
        String paramName = parameterInfo.getpName();
        ParameterMeta parameter = parameterInfo.getMethodParameter();

        if (parameterInfo.getParameterModel() == null
                || parameterInfo.getParameterModel().getSchema() == null) {
            Type type = parameter.getGenericType();
            schemaN = this.extractSchema(components, type);
        } else {
            schemaN = parameterInfo.getParameterModel().getSchema();
        }

        if (requestBodyInfo != null) {
            schemaN = this.calculateRequestBodySchema(components, parameterInfo, requestBodyInfo, schemaN, paramName);
        }

        return schemaN;
    }

    private Schema calculateRequestBodySchema(
            Components components,
            ParameterInfo parameterInfo,
            RequestBodyInfo requestBodyInfo,
            Schema schemaN,
            String paramName) {
        if (schemaN != null
                && org.apache.commons.lang3.StringUtils.isEmpty(schemaN.getDescription())
                && parameterInfo.getParameterModel() != null) {
            String description = parameterInfo.getParameterModel().getDescription();
            if (schemaN.get$ref() != null && schemaN.get$ref().contains(COMPONENTS_REF)) {
                String key = schemaN.get$ref().substring(21);
                Schema existingSchema = components.getSchemas().get(key);
                if (!org.apache.commons.lang3.StringUtils.isEmpty(description))
                    existingSchema.setDescription(description);
            } else schemaN.setDescription(description);
        }

        if (requestBodyInfo.getMergedSchema() != null) {
            requestBodyInfo.getMergedSchema().addProperty(paramName, schemaN);
            schemaN = requestBodyInfo.getMergedSchema();
        } else if (schemaN instanceof FileSchema
                || schemaN instanceof ArraySchema && ((ArraySchema) schemaN).getItems() instanceof FileSchema) {
            schemaN = new ObjectSchema().addProperty(paramName, schemaN);
            requestBodyInfo.setMergedSchema(schemaN);
        } else requestBodyInfo.addProperties(paramName, schemaN);

        if (requestBodyInfo.getMergedSchema() != null && parameterInfo.isRequired())
            requestBodyInfo.getMergedSchema().addRequiredItem(parameterInfo.getpName());

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
}
