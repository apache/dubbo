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
import org.apache.dubbo.metadata.swagger.model.media.Content;
import org.apache.dubbo.metadata.swagger.model.media.MediaType;
import org.apache.dubbo.metadata.swagger.model.media.Schema;
import org.apache.dubbo.metadata.swagger.model.parameters.ParameterInfo;
import org.apache.dubbo.metadata.swagger.model.parameters.RequestBody;
import org.apache.dubbo.metadata.swagger.model.parameters.RequestBodyInfo;

import java.util.Locale;
import java.util.Optional;

public class RequestBodyService {

    /**
     * The Parameter builder.
     */
    private final ParameterService parameterBuilder;

    public RequestBodyService(ParameterService parameterBuilder) {
        super();
        this.parameterBuilder = parameterBuilder;
    }

    public void calculateRequestBodyInfo(
            Components components,
            MethodAttributes methodAttributes,
            ParameterInfo parameterInfo,
            RequestBodyInfo requestBodyInfo) {
        RequestBody requestBody = requestBodyInfo.getRequestBody();

        String paramName = null;
        paramName = StringUtils.defaultIfEmpty(paramName, parameterInfo.getpName());
        parameterInfo.setpName(paramName);

        requestBody = this.buildRequestBody(requestBody, components, methodAttributes, parameterInfo, requestBodyInfo);
        requestBodyInfo.setRequestBody(requestBody);
    }

    private RequestBody buildRequestBody(
            RequestBody requestBody,
            Components components,
            MethodAttributes methodAttributes,
            ParameterInfo parameterInfo,
            RequestBodyInfo requestBodyInfo) {
        if (requestBody == null) {
            requestBody = new RequestBody();
            requestBodyInfo.setRequestBody(requestBody);
        }

        if (requestBody.getContent() == null) {
            Schema<?> schema = parameterBuilder.calculateSchema(components, parameterInfo, requestBodyInfo);
            this.buildContent(requestBody, methodAttributes, schema);
        } else {
            Schema<?> schema = parameterBuilder.calculateSchema(components, parameterInfo, requestBodyInfo);
            this.mergeContent(requestBody, methodAttributes, schema);
        }

        return requestBody;
    }

    private void mergeContent(RequestBody requestBody, MethodAttributes methodAttributes, Schema<?> schema) {
        Content content = requestBody.getContent();
        buildContent(requestBody, methodAttributes, schema, content);
    }

    private void buildContent(
            RequestBody requestBody, MethodAttributes methodAttributes, Schema<?> schema, Content content) {
        for (String value : methodAttributes.getMethodConsumes()) {
            MediaType mediaTypeObject = new MediaType();
            mediaTypeObject.setSchema(schema);
            content.addMediaType(value, mediaTypeObject);
        }
        requestBody.setContent(content);
    }

    private void buildContent(RequestBody requestBody, MethodAttributes methodAttributes, Schema<?> schema) {
        Content content = new Content();
        buildContent(requestBody, methodAttributes, schema, content);
    }

    public Optional<RequestBody> buildRequestBodyFromDoc(
            MethodAttributes methodAttributes, Components components, Locale locale) {
        return Optional.empty();
    }
}
