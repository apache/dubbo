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
package org.apache.dubbo.rpc.protocol.tri.h12;

import org.apache.dubbo.common.logger.Level;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.ExceptionHandler;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.RequestMetadata;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;
import org.apache.dubbo.rpc.protocol.tri.h12.grpc.GrpcHeaderNames;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class CompositeExceptionHandler implements ExceptionHandler<Throwable, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeExceptionHandler.class);

    private final List<ExceptionHandler> exceptionHandlers;
    private final Map<Class, List<ExceptionHandler>> cache = CollectionUtils.newConcurrentHashMap();

    public CompositeExceptionHandler(FrameworkModel frameworkModel) {
        exceptionHandlers = frameworkModel.getActivateExtensions(ExceptionHandler.class);
    }

    @Override
    public Level resolveLogLevel(Throwable throwable) {
        List<ExceptionHandler> exceptionHandlers = getSuitableExceptionHandlers(throwable.getClass());
        for (int i = 0, size = exceptionHandlers.size(); i < size; i++) {
            Level level = exceptionHandlers.get(i).resolveLogLevel(throwable);
            if (level != null) {
                return level;
            }
        }

        return ExceptionUtils.resolveLogLevel(throwable);
    }

    @Override
    public boolean resolveGrpcStatus(
            Throwable throwable, HttpHeaders headers, RequestMetadata metadata, MethodDescriptor descriptor) {
        throwable = ExceptionUtils.unwrap(throwable);

        List<ExceptionHandler> exceptionHandlers = getSuitableExceptionHandlers(throwable.getClass());
        for (int i = 0, size = exceptionHandlers.size(); i < size; i++) {
            if (exceptionHandlers.get(i).resolveGrpcStatus(throwable, headers, metadata, descriptor)) {
                return true;
            }
        }

        TriRpcStatus status = TriRpcStatus.getStatus(throwable);
        headers.set(GrpcHeaderNames.GRPC_STATUS.getName(), String.valueOf(status.code.code));
        headers.set(
                GrpcHeaderNames.GRPC_MESSAGE.getName(),
                TripleProtocol.VERBOSE_ENABLED
                        ? ExceptionUtils.buildVerboseMessage(throwable)
                        : throwable.getMessage());

        return true;
    }

    @Override
    public Object handle(Throwable throwable, RequestMetadata metadata, MethodDescriptor descriptor) {
        throwable = ExceptionUtils.unwrap(throwable);

        Object result;
        List<ExceptionHandler> exceptionHandlers = getSuitableExceptionHandlers(throwable.getClass());
        for (int i = 0, size = exceptionHandlers.size(); i < size; i++) {
            result = exceptionHandlers.get(i).handle(throwable, metadata, descriptor);
            if (result != null) {
                return result;
            }
        }

        int statusCode = -1, grpcStatusCode = -1;
        if (throwable instanceof HttpStatusException) {
            statusCode = ((HttpStatusException) throwable).getStatusCode();
        } else {
            grpcStatusCode = TriRpcStatus.grpcCodeToHttpStatus(TriRpcStatus.getStatus(throwable).code);
        }

        if (TripleProtocol.VERBOSE_ENABLED) {
            if (statusCode == -1) {
                statusCode = grpcStatusCode < 0 ? HttpStatus.INTERNAL_SERVER_ERROR.getCode() : grpcStatusCode;
            }

            LOGGER.info("Http request process error: status={}", statusCode, throwable);
        }

        return grpcStatusCode < 0 ? null : new HttpStatusException(grpcStatusCode, throwable.getMessage(), throwable);
    }

    @Override
    public Object handleGrpc(Throwable throwable, RequestMetadata metadata, MethodDescriptor descriptor) {
        throwable = ExceptionUtils.unwrap(throwable);

        Object result;
        List<ExceptionHandler> exceptionHandlers = getSuitableExceptionHandlers(throwable.getClass());
        for (int i = 0, size = exceptionHandlers.size(); i < size; i++) {
            result = exceptionHandlers.get(i).handleGrpc(throwable, metadata, descriptor);
            if (result != null) {
                return result;
            }
        }

        if (descriptor != null) {
            Method method = descriptor.getMethod();
            if (method != null) {
                for (Class<?> exceptionClass : method.getExceptionTypes()) {
                    if (exceptionClass.isInstance(throwable)) {
                        return HttpResult.of(throwable);
                    }
                }
            }
        }

        if (TripleProtocol.VERBOSE_ENABLED) {
            LOGGER.info("Grpc http request process error", throwable);
        }

        return null;
    }

    private List<ExceptionHandler> getSuitableExceptionHandlers(Class type) {
        return cache.computeIfAbsent(type, k -> {
            List<ExceptionHandler> result = new ArrayList<>();
            for (ExceptionHandler handler : exceptionHandlers) {
                Class<?> supportType = TypeUtils.getSuperGenericType(handler.getClass());
                if (supportType != null && supportType.isAssignableFrom(type)) {
                    result.add(handler);
                }
            }
            if (result.isEmpty()) {
                return Collections.emptyList();
            }
            LOGGER.info("Found suitable ExceptionHandler for [{}], handlers: {}", type, result);
            return result;
        });
    }
}
