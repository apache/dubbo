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
package org.apache.dubbo.config.spring6.utils;

import org.apache.dubbo.common.compiler.support.ClassUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

public class AotUtils {

    private AotUtils() {}

    public static void registerSerializationForService(Class<?> serviceType, RuntimeHints hints) {
        Set<Class<?>> serializationTypeCache = new LinkedHashSet<>();
        Arrays.stream(serviceType.getMethods()).forEach((method) -> {
            Arrays.stream(method.getParameterTypes())
                    .forEach(
                            (parameterType) -> registerSerializationType(parameterType, hints, serializationTypeCache));

            registerSerializationType(method.getReturnType(), hints, serializationTypeCache);
        });
    }

    private static void registerSerializationType(
            Class<?> registerType, RuntimeHints hints, Set<Class<?>> serializationTypeCache) {
        if (isPrimitive(registerType)) {
            hints.serialization().registerType(TypeReference.of(ClassUtils.getBoxedClass(registerType)));
            serializationTypeCache.add(registerType);
        } else {
            if (Serializable.class.isAssignableFrom(registerType)) {
                hints.serialization().registerType(TypeReference.of(registerType));
                serializationTypeCache.add(registerType);

                Arrays.stream(registerType.getDeclaredFields()).forEach((field -> {
                    if (!serializationTypeCache.contains(field.getType())) {
                        registerSerializationType(field.getType(), hints, serializationTypeCache);
                        serializationTypeCache.add(field.getType());
                    }
                }));

                if (registerType.getSuperclass() != null) {
                    registerSerializationType(registerType.getSuperclass(), hints, serializationTypeCache);
                }
            }
        }
    }

    private static boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive()
                || cls == Boolean.class
                || cls == Byte.class
                || cls == Character.class
                || cls == Short.class
                || cls == Integer.class
                || cls == Long.class
                || cls == Float.class
                || cls == Double.class
                || cls == String.class
                || cls == Date.class
                || cls == Class.class;
    }
}
