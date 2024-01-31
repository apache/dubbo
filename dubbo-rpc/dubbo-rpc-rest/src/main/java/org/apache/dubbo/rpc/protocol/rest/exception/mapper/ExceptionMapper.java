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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.protocol.rest.util.ReflectUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ExceptionMapper {
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private final Map<Class<?>, ExceptionHandler> exceptionHandlerMap = new ConcurrentHashMap<>();

    private final Map allExceptionHandlers = new ConcurrentHashMap<>();

    public ExceptionHandlerResult exceptionToResult(Object throwable) {
        ExceptionHandler exceptionHandler = (ExceptionHandler) getExceptionHandler(throwable.getClass());

        Object result = exceptionHandler.result((Throwable) throwable);

        return ExceptionHandlerResult.build().setEntity(result).setStatus(exceptionHandler.status());
    }

    public Object getExceptionHandler(Class causeClass) {

        return getExceptionHandler(allExceptionHandlers, causeClass);
    }

    public Object getExceptionHandler(Map exceptionHandlerMap, Class causeClass) {
        Object exceptionHandler = null;
        while (causeClass != null) {
            exceptionHandler = exceptionHandlerMap.get(causeClass);
            if (exceptionHandler != null) {
                break;
            }
            // When the exception handling class cannot be obtained, it should recursively search the base class
            causeClass = causeClass.getSuperclass();
        }
        return exceptionHandler;
    }

    public boolean hasExceptionMapper(Object throwable) {
        if (throwable == null) {
            return false;
        }
        return allExceptionHandlers.containsKey(throwable.getClass());
    }

    public void registerMapper(Class<?> exceptionHandler) {

        try {
            List<Method> methods = getExceptionHandlerMethods(exceptionHandler);

            if (methods == null || methods.isEmpty()) {
                return;
            }

            Set<Class<?>> exceptions = new HashSet<>();

            for (Method method : methods) {
                Class<?> parameterType = method.getParameterTypes()[0];

                // param type isAssignableFrom throwable
                if (!Throwable.class.isAssignableFrom(parameterType)) {
                    continue;
                }

                exceptions.add(parameterType);
            }

            ArrayList<Class<?>> classes = new ArrayList<>(exceptions);

            // if size==1 so ,exception handler for Throwable
            if (classes.size() != 1) {
                // else remove throwable
                exceptions.remove(Throwable.class);
            }

            List<Constructor<?>> constructors = ReflectUtils.getConstructList(exceptionHandler);

            if (constructors.isEmpty()) {
                throw new RuntimeException(
                        "dubbo rest exception mapper register mapper need exception handler exist no  construct declare, current class is: "
                                + exceptionHandler);
            }

            // if exceptionHandler is inner class , no arg construct don`t appear , so  newInstance don`t use
            // noArgConstruct
            Object handler = constructors
                    .get(0)
                    .newInstance(new Object[constructors.get(0).getParameterCount()]);

            putExtensionToMap(exceptions, handler);

        } catch (Exception e) {
            throw new RuntimeException("dubbo rest protocol exception mapper register error ", e);
        }
    }

    protected void putExtensionToMap(Set<Class<?>> exceptions, Object handler) {

        Map exceptionHandlerMaps = getExceptionHandlerMap(handler);

        for (Class<?> exception : exceptions) {
            // put to instance map
            exceptionHandlerMaps.put(exception, handler);
            // put to all map
            allExceptionHandlers.put(exception, handler);
        }
    }

    protected Map getExceptionHandlerMap(Object handler) {
        return exceptionHandlerMap;
    }

    protected List<Method> getExceptionHandlerMethods(Class<?> exceptionHandler) {
        if (!ExceptionHandler.class.isAssignableFrom(exceptionHandler)) {
            return null;
        }
        // resolve Java_Zulu_jdk/17.0.6-10/x64 param is not throwable
        List<Method> methods = ReflectUtils.getMethodByNameList(exceptionHandler, "result");
        return methods;
    }

    public void registerMapper(String exceptionMapper) {
        try {
            registerMapper(ReflectUtils.findClass(exceptionMapper));
        } catch (Exception e) {
            logger.warn(
                    "",
                    e.getMessage(),
                    "",
                    "dubbo rest protocol exception mapper register error ,and current exception mapper is  :"
                            + exceptionMapper);
        }
    }

    public void unRegisterMapper(Class<?> exception) {
        exceptionHandlerMap.remove(exception);
    }

    public static boolean isSupport(Class<?> exceptionHandler) {
        try {
            return ExceptionHandler.class.isAssignableFrom(exceptionHandler)
                    || ReflectUtils.findClassTryException("javax.ws.rs.ext.ExceptionMapper")
                            .isAssignableFrom(exceptionHandler);
        } catch (Exception e) {
            return false;
        }
    }
}
