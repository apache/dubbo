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

import org.apache.dubbo.metadata.OperationService;
import org.apache.dubbo.metadata.swagger.model.Components;
import org.apache.dubbo.metadata.swagger.model.OpenAPI;
import org.apache.dubbo.metadata.swagger.model.Operation;
import org.apache.dubbo.metadata.swagger.model.ParameterId;
import org.apache.dubbo.metadata.swagger.model.ParameterInfo;
import org.apache.dubbo.metadata.swagger.model.ParameterService;
import org.apache.dubbo.metadata.swagger.model.PathItem.HttpMethod;
import org.apache.dubbo.metadata.swagger.model.RequestBodyInfo;
import org.apache.dubbo.metadata.swagger.model.annotations.ParameterIn;
import org.apache.dubbo.metadata.swagger.model.media.Schema;
import org.apache.dubbo.metadata.swagger.model.parameters.Parameter;
import org.apache.dubbo.metadata.swagger.model.parameters.RequestBody;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class RequestService {
    /**
     * The Operation builder.
     */
    private final OperationService operationService;

    /**
     * The Parameter builder.
     */
    private final ParameterService parameterBuilder;

    /**
     * The Request body builder.
     */
    private final RequestBodyService requestBodyService;

    protected RequestService(
            ParameterService parameterBuilder,
            RequestBodyService requestBodyService,
            OperationService operationService) {
        super();
        this.parameterBuilder = parameterBuilder;
        this.requestBodyService = requestBodyService;
        this.operationService = operationService;
    }

    protected RequestService() {
        super();
        this.parameterBuilder = new ParameterService();
        this.requestBodyService = new RequestBodyService();
        this.operationService = new OperationService();
    }

    public static final String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";
    String DEFAULT_NONE = "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n";

    public Operation build(
            HandlerMeta handlerMeta,
            HttpMethod httpMethod,
            Operation operation,
            MethodAttributes methodAttributes,
            OpenAPI openAPI) {

        // Documentation
        String operationId = operationService.getOperationId(
                handlerMeta.getMethod().getMethod().getName(), operation.getOperationId(), openAPI);

        // requests
        ParameterMeta[] parameters = handlerMeta.getParameters();

        RequestBodyInfo requestBodyInfo = new RequestBodyInfo();
        List<Parameter> operationParameters =
                (operation.getParameters() != null) ? operation.getParameters() : new ArrayList<>();
        Components components = openAPI.getComponents();

        for (ParameterMeta parameterMeta : parameters) {
            Parameter parameter;
            final String pName = parameterMeta.getName();
            ParameterInfo parameterInfo = new ParameterInfo(pName, parameterMeta, parameterBuilder);

            parameter = buildParams(parameterInfo, components, httpMethod, methodAttributes, openAPI.getOpenapi());
            // Merge with the operation parameters
            parameter = ParameterService.mergeParameter(operationParameters, parameter);

            if (!HttpMethod.GET.equals(httpMethod)) {
                if (operation.getRequestBody() != null) requestBodyInfo.setRequestBody(operation.getRequestBody());
                requestBodyService.calculateRequestBodyInfo(
                        components, methodAttributes, parameterInfo, requestBodyInfo);
            }
        }

        LinkedHashMap<ParameterId, Parameter> map = getParameterLinkedHashMap(operationParameters);
        RequestBody requestBody = requestBodyInfo.getRequestBody();

        // support form-data
        if (requestBody != null
                && requestBody.getContent() != null
                && requestBody.getContent().containsKey(MULTIPART_FORM_DATA_VALUE)) {
            Iterator<Entry<ParameterId, Parameter>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Entry<ParameterId, Parameter> entry = it.next();
                Parameter parameter = entry.getValue();
                if (!ParameterIn.PATH.toString().equals(parameter.getIn())
                        && !ParameterIn.HEADER.toString().equals(parameter.getIn())
                        && !ParameterIn.COOKIE.toString().equals(parameter.getIn())) {
                    Schema<?> itemSchema = new Schema<>();
                    itemSchema.setName(entry.getKey().getpName());
                    requestBodyInfo.addProperties(entry.getKey().getpName(), itemSchema);
                    it.remove();
                }
            }
        }
        return operation;
    }

    private Parameter buildParams(
            ParameterInfo parameterInfo,
            Components components,
            HttpMethod httpMethod,
            MethodAttributes methodAttributes,
            String openApiVersion) {
        ParameterMeta parameter = parameterInfo.getMethodParameter();
        if (parameterInfo.getParamType() != null) {
            if (!DEFAULT_NONE.equals(parameterInfo.getDefaultValue())) parameterInfo.setRequired(false);
            else parameterInfo.setDefaultValue(null);
            return this.buildParam(parameterInfo, components);
        }
        return null;
    }

    private Parameter buildParam(ParameterInfo parameterInfo, Components components) {
        return null;
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
                parameter.setIn(ParameterIn.QUERY.toString());
        });
        return map;
    }
}
