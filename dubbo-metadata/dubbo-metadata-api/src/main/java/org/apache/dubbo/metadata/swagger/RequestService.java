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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.swagger.model.Components;
import org.apache.dubbo.metadata.swagger.model.OpenAPI;
import org.apache.dubbo.metadata.swagger.model.Operation;
import org.apache.dubbo.metadata.swagger.model.OperationService;
import org.apache.dubbo.metadata.swagger.model.PathItem.HttpMethod;
import org.apache.dubbo.metadata.swagger.model.media.Schema;
import org.apache.dubbo.metadata.swagger.model.media.StringSchema;
import org.apache.dubbo.metadata.swagger.model.parameters.Parameter;
import org.apache.dubbo.metadata.swagger.model.parameters.ParameterId;
import org.apache.dubbo.metadata.swagger.model.parameters.ParameterInfo;
import org.apache.dubbo.metadata.swagger.model.parameters.RequestBody;
import org.apache.dubbo.metadata.swagger.model.parameters.RequestBodyInfo;
import org.apache.dubbo.metadata.swagger.utils.PrimitiveType;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import static org.apache.dubbo.metadata.swagger.SwaggerConstants.COOKIE;
import static org.apache.dubbo.metadata.swagger.SwaggerConstants.DEFAULT_NONE;
import static org.apache.dubbo.metadata.swagger.SwaggerConstants.HEADER;
import static org.apache.dubbo.metadata.swagger.SwaggerConstants.MULTIPART_FORM_DATA_VALUE;
import static org.apache.dubbo.metadata.swagger.SwaggerConstants.PATH;
import static org.apache.dubbo.metadata.swagger.SwaggerConstants.QUERY;

public class RequestService {

    private final ParameterService parameterBuilder;

    private final RequestBodyService requestBodyService;

    private final OperationService operationService;

    public RequestService(
            ParameterService parameterBuilder,
            RequestBodyService requestBodyService,
            OperationService operationService) {
        super();
        this.parameterBuilder = parameterBuilder;
        this.requestBodyService = requestBodyService;
        this.operationService = operationService;
    }

    public static Collection<Parameter> getHeaders(
            MethodAttributes methodAttributes, LinkedHashMap<ParameterId, Parameter> map) {
        for (Map.Entry<String, String> entry : methodAttributes.getHeaders().entrySet()) {
            StringSchema schema = new StringSchema();
            if (StringUtils.isNotEmpty(entry.getValue())) schema.addEnumItem(entry.getValue());
            Parameter parameter =
                    new Parameter().in(HEADER).name(entry.getKey()).schema(schema);
            ParameterId parameterId = new ParameterId(parameter);
            if (map.containsKey(parameterId)) {
                parameter = map.get(parameterId);
                List existingEnum = null;
                if (parameter.getSchema() != null
                        && !CollectionUtils.isEmpty(parameter.getSchema().getEnum()))
                    existingEnum = parameter.getSchema().getEnum();
                if (StringUtils.isNotEmpty(entry.getValue())
                        && (existingEnum == null || !existingEnum.contains(entry.getValue())))
                    parameter.getSchema().addEnumItemObject(entry.getValue());
                parameter.setSchema(parameter.getSchema());
            }
            map.put(parameterId, parameter);
        }
        return map.values();
    }

    public Operation build(
            HandlerMeta handlerMeta,
            HttpMethod httpMethod,
            Operation operation,
            MethodAttributes methodAttributes,
            OpenAPI openAPI) {
        String operationId = operationService.getOperationId(
                handlerMeta.getMethod().getMethod().getName(), operation.getOperationId(), openAPI);
        operation.setOperationId(operationId);

        ParameterMeta[] parameters = handlerMeta.getParameters();

        RequestBodyInfo requestBodyInfo = new RequestBodyInfo();
        List<Parameter> operationParameters =
                (operation.getParameters() != null) ? operation.getParameters() : new ArrayList<>();
        Components components = openAPI.getComponents();

        for (ParameterMeta parameterMeta : parameters) {
            Parameter parameter;
            final String pName = parameterMeta.getName();
            ParameterInfo parameterInfo = new ParameterInfo(pName, parameterMeta, parameterBuilder);

            parameter = this.buildParams(parameterInfo, components, httpMethod, methodAttributes, openAPI.getOpenapi());
            // Merge with the operation parameters
            ParameterService.mergeParameter(operationParameters, parameter);

            if (!HttpMethod.GET.equals(httpMethod)) {
                if (operation.getRequestBody() != null) requestBodyInfo.setRequestBody(operation.getRequestBody());
                requestBodyService.calculateRequestBodyInfo(
                        components, methodAttributes, parameterInfo, requestBodyInfo);
            }
        }

        LinkedHashMap<ParameterId, Parameter> map = this.getParameterLinkedHashMap(operationParameters);
        RequestBody requestBody = requestBodyInfo.getRequestBody();

        // support form-data
        if (requestBody != null
                && requestBody.getContent() != null
                && requestBody.getContent().containsKey(MULTIPART_FORM_DATA_VALUE)) {
            Iterator<Entry<ParameterId, Parameter>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Entry<ParameterId, Parameter> entry = it.next();
                Parameter parameter = entry.getValue();
                if (!PATH.equals(parameter.getIn())
                        && !HEADER.equals(parameter.getIn())
                        && !COOKIE.equals(parameter.getIn())) {
                    Schema<?> itemSchema = new Schema<>();
                    itemSchema.setName(entry.getKey().getpName());
                    requestBodyInfo.addProperties(entry.getKey().getpName(), itemSchema);
                    it.remove();
                }
            }
        }
        return operation;
    }

    private LinkedHashMap<ParameterId, Parameter> getParameterLinkedHashMap(List<Parameter> operationParameters) {
        LinkedHashMap<ParameterId, Parameter> map = operationParameters.stream()
                .collect(Collectors.toMap(
                        ParameterId::new,
                        parameter -> parameter,
                        (u, v) -> {
                            throw new IllegalStateException(String.format("Duplicate key %s", u));
                        },
                        LinkedHashMap::new));

        map.forEach((parameterId, parameter) -> {
            if (StringUtils.isBlank(parameter.getIn()) && StringUtils.isBlank(parameter.get$ref()))
                parameter.setIn(QUERY);
        });
        return map;
    }

    private Parameter buildParams(
            ParameterInfo parameterInfo,
            Components components,
            HttpMethod httpMethod,
            MethodAttributes methodAttributes,
            String openapi) {
        if (parameterInfo.getParamType() != null) {
            if (!DEFAULT_NONE.equals(parameterInfo.getDefaultValue())) {
                parameterInfo.setRequired(false);
            } else {
                parameterInfo.setDefaultValue(null);
            }
            return this.buildParam(parameterInfo, components);
        }
        return null;
    }

    private Parameter buildParam(ParameterInfo parameterInfo, Components components) {
        Parameter parameter = parameterInfo.getParameterModel();
        String name = parameterInfo.getpName();

        if (parameter == null) {
            parameter = new Parameter();
            parameterInfo.setParameterModel(parameter);
        }

        if (StringUtils.isBlank(parameter.getName())) {
            parameter.setName(name);
        }

        if (StringUtils.isBlank(parameter.getIn())) {
            parameter.setIn(parameterInfo.getParamType());
        }

        if (parameter.getRequired() == null) {
            parameter.setRequired(parameterInfo.isRequired());
        }

        if (parameter.getSchema() == null && parameter.getContent() == null) {
            Schema<?> schema = parameterBuilder.calculateSchema(components, parameterInfo, null);
            if (parameterInfo.getDefaultValue() != null && schema != null) {
                Object defaultValue = parameterInfo.getDefaultValue();
                // Cast default value
                PrimitiveType primitiveType = PrimitiveType.fromTypeAndFormat(schema.getType(), schema.getFormat());
                if (primitiveType != null) {
                    Schema<?> primitiveSchema = primitiveType.createProperty();
                    primitiveSchema.setDefault(parameterInfo.getDefaultValue());
                    defaultValue = primitiveSchema.getDefault();
                }
                schema.setDefault(defaultValue);
            }
            parameter.setSchema(schema);
        }
        return parameter;
    }

    public RequestBodyService getRequestBodyBuilder() {
        return requestBodyService;
    }
}
