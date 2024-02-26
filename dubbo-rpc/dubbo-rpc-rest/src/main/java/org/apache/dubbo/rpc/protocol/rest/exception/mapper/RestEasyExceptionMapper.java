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
package org.apache.dubbo.rpc.protocol.rest.exception.mapper;

import org.apache.dubbo.rpc.protocol.rest.util.ReflectUtils;

import javax.ws.rs.core.Response;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * for rest easy  exception mapper extension
 */
public class RestEasyExceptionMapper extends ExceptionMapper {

    private final Map<Class<?>, javax.ws.rs.ext.ExceptionMapper> exceptionMappers = new ConcurrentHashMap<>();

    protected List<Method> getExceptionHandlerMethods(Class<?> exceptionHandler) {
        if (!javax.ws.rs.ext.ExceptionMapper.class.isAssignableFrom(exceptionHandler)) {
            return super.getExceptionHandlerMethods(exceptionHandler);
        }
        // resolve Java_Zulu_jdk/17.0.6-10/x64 param is not throwable
        List<Method> methods = ReflectUtils.getMethodByNameList(exceptionHandler, "toResponse");
        return methods;
    }

    protected Map getExceptionHandlerMap(Object handler) {
        if (handler instanceof ExceptionHandler) {
            return super.getExceptionHandlerMap(handler);
        }
        return exceptionMappers;
    }

    public ExceptionHandlerResult exceptionToResult(Object throwable) {
        Object exceptionMapper = getExceptionHandler(throwable.getClass());
        if (exceptionMapper == null || exceptionMapper instanceof ExceptionHandler) {
            return super.exceptionToResult(throwable);
        }

        Response response = ((javax.ws.rs.ext.ExceptionMapper) exceptionMapper).toResponse((Throwable) throwable);

        return ExceptionHandlerResult.build().setStatus(response.getStatus()).setEntity(response.getEntity());
    }
}
