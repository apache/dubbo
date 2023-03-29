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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExceptionMapper {

    // TODO static or instance ? think about influence  between  difference url exception
    private final Map<Class<?>, ExceptionHandler> exceptionHandlerMap = new ConcurrentHashMap<>();

    public Object exceptionToResult(Object throwable) {
        if (!hasExceptionMapper(throwable)) {
            return throwable;
        }

        return exceptionHandlerMap.get(throwable.getClass()).result((Throwable) throwable);
    }

    public boolean hasExceptionMapper(Object throwable) {
        if (throwable == null) {
            return false;
        }
        return exceptionHandlerMap.containsKey(throwable.getClass());
    }


    public void registerMapper(Class<?> exceptionHandler) {

        try {
            Method result = ReflectUtils.getMethodByName(exceptionHandler, "result");
            Class<?> exceptionClass = result.getParameterTypes()[0];

            Constructor<?> constructor = getConstructor(exceptionHandler);
            // if exceptionHandler is inner class , no arg construct don`t appear , so  newInstance don`t use noArgConstruct
            Object handler = constructor.newInstance(new Object[constructor.getParameterCount()]);
            exceptionHandlerMap.put(exceptionClass, (ExceptionHandler) handler);
        } catch (Exception e) {
            throw new RuntimeException("dubbo rest protocol exception mapper register error ", e);
        }


    }

    private static Constructor<?> getConstructor(Class<?> exceptionHandler) {
        Constructor<?>[] constructor = exceptionHandler.getConstructors();

        if (constructor.length == 0) {
            throw new RuntimeException("dubbo rest exception mapper register mapper need exception handler exist no arg construct, please make  class public if class is inner class, current class is: " + exceptionHandler);
        }
        return constructor[0];
    }

    public void registerMapper(String exceptionMapper) {
        try {
            registerMapper(ReflectUtils.findClass(exceptionMapper));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("dubbo rest protocol exception mapper register error ", e);
        }

    }


    public void unRegisterMapper(Class<?> exception) {
        exceptionHandlerMap.remove(exception);
    }
}
