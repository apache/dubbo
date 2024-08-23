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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.swagger.model.Components;
import org.apache.dubbo.metadata.swagger.model.Operation;
import org.apache.dubbo.metadata.swagger.model.media.Content;
import org.apache.dubbo.metadata.swagger.model.responses.ApiResponse;
import org.apache.dubbo.metadata.swagger.model.responses.ApiResponses;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
;

public class ResponseService {

    /**
     * The Reentrant lock.
     */
    private final Lock reentrantLock = new ReentrantLock();

    /**
     * The constant DEFAULT_DESCRIPTION.
     */
    public static final String DEFAULT_DESCRIPTION = "default response";

    public ApiResponses build(
            Components components, HandlerMeta handlerMeta, Operation operation, MethodAttributes methodAttributes) {
        ApiResponses apiResponses = new ApiResponses();
        // Fill api Responses
        buildApiResponses(components, handlerMeta.getMethod(), apiResponses, methodAttributes);
        return apiResponses;
    }

    private void buildApiResponses(
            Components components,
            MethodMeta methodMeta,
            ApiResponses apiResponses,
            MethodAttributes methodAttributes) {
        Map<String, ApiResponse> genericMapResponse = methodAttributes.getGenericMapResponse();
        if (!CollectionUtils.isEmptyMap(apiResponses)) {
            // API Responses at operation
            for (Map.Entry<String, ApiResponse> entry : apiResponses.entrySet()) {
                String httpCode = entry.getKey();
                ApiResponse apiResponse = entry.getValue();
                buildApiResponses(components, methodMeta, apiResponses, methodAttributes, httpCode, apiResponse);
            }
        } else {
            String httpCode = evaluateResponseStatus(
                    methodMeta, Objects.requireNonNull(methodMeta.getMethod()).getClass());
            if (Objects.nonNull(httpCode)) {
                buildApiResponses(components, methodMeta, apiResponses, methodAttributes, httpCode, new ApiResponse());
            }
        }
    }

    private String evaluateResponseStatus(MethodMeta methodMeta, Class<? extends Method> aClass) {
        return "";
    }

    private void buildApiResponses(
            Components components,
            MethodMeta methodMeta,
            ApiResponses apiResponsesOp,
            MethodAttributes methodAttributes,
            String httpCode,
            ApiResponse apiResponse) {
        // No documentation
        if (StringUtils.isBlank(apiResponse.get$ref())) {
            if (apiResponse.getContent() == null) {
                Content content = buildContent(components, methodMeta, methodAttributes.getMethodProduces());
                apiResponse.setContent(content);
            } else {
                apiResponse.setContent(null);
            }
            if (StringUtils.isBlank(apiResponse.getDescription())) {
                setDescription(httpCode, apiResponse);
            }
        }

        //        if (apiResponse.getContent() != null) {
        //            // Merge with existing schema
        //            Content existingContent = apiResponse.getContent();
        //            Type type = ReturnTypeParser.getType(methodMeta);
        //            Schema<?> schema = calculateSchema(components, type);
        //
        //            if (schema != null && ArrayUtils.isNotEmpty(methodAttributes.getMethodProduces())) {
        //                Arrays.stream(methodAttributes.getMethodProduces())
        //                        .forEach(mediaTypeStr -> mergeSchema(existingContent, schema, mediaTypeStr));
        //            }
        //        }

        apiResponsesOp.addApiResponse(httpCode, apiResponse);
    }

    private void setDescription(String httpCode, ApiResponse apiResponse) {
        return;
    }

    private Content buildContent(Components components, MethodMeta methodMeta, String[] methodProduces) {
        return null;
    }
}
