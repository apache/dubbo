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
package org.apache.dubbo.rpc.protocol.tri.h12.http2;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.ErrorResponse;
import org.apache.dubbo.remoting.http12.ExceptionHandler;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.exception.HttpResultPayloadException;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public final class CompositeExceptionHandler implements ExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeExceptionHandler.class);

    private final List<ExceptionHandler> exceptionHandlers;
    private final Map<Class, List<ExceptionHandler>> cache = CollectionUtils.newConcurrentHashMap();

    public CompositeExceptionHandler(FrameworkModel frameworkModel) {
        exceptionHandlers = frameworkModel.getActivateExtensions(ExceptionHandler.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public HttpResult handle(Throwable error) {
        error = ExceptionUtils.unwrap(error);

        if (error instanceof HttpResultPayloadException) {
            return ((HttpResultPayloadException) error).getResult();
        }

        List<ExceptionHandler> exceptionHandlers = getSuitableExceptionHandlers(error.getClass());
        for (int i = 0, len = exceptionHandlers.size(); i < len; i++) {
            ExceptionHandler handler = exceptionHandlers.get(i);
            HttpResult result = handler.handle(error);
            if (result != null) {
                return result;
            }
        }

        int httpStatusCode = TriRpcStatus.triCodeToDubboCode(TriRpcStatus.getStatus(error).code);

        if (TripleProtocol.VERBOSE_ENABLED) {
            if (httpStatusCode < 1) {
                if (error instanceof HttpStatusException) {
                    httpStatusCode = ((HttpStatusException) error).getStatusCode();
                } else {
                    httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR.getCode();
                }
            }

            LOGGER.info("An error occurred while processing the http request: status={}", httpStatusCode, error);

            ErrorResponse response = new ErrorResponse();
            response.setStatus(String.valueOf(httpStatusCode));
            response.setMessage(ExceptionUtils.buildVerboseMessage(error));
            return HttpResult.of(httpStatusCode, response);
        }

        if (httpStatusCode > 0) {
            return HttpResult.of(httpStatusCode, error);
        }

        return null;
    }

    private List<ExceptionHandler> getSuitableExceptionHandlers(Class type) {
        return cache.computeIfAbsent(type, k -> {
            List<ExceptionHandler> result = new ArrayList<>();
            for (ExceptionHandler handler : exceptionHandlers) {
                Class<?> supportType = TypeUtils.getSuperGenericType(handler.getClass(), 0);
                if (supportType != null) {
                    if (supportType.isAssignableFrom(type)) {
                        result.add(handler);
                    }
                }
            }
            return result.isEmpty() ? Collections.emptyList() : result;
        });
    }
}
