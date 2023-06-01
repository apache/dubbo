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
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;


public class AotUtils {

    private AotUtils() {

    }

    public static void registerSerializationForService(Class<?> serviceType, RuntimeHints hints) {
        Arrays.stream(serviceType.getMethods()).forEach((method) -> {
            Arrays.stream(method.getParameterTypes()).forEach((parameterType) -> registerSerializationType(parameterType, hints));

            registerSerializationType(method.getReturnType(), hints);
        });
    }

    private static void registerSerializationType(Class<?> registerType, RuntimeHints hints) {
        if (isPrimitive(registerType)) {
            hints.serialization().registerType(TypeReference.of(ClassUtils.getBoxedClass(registerType)));
        } else {
            if (Serializable.class.isAssignableFrom(registerType)) {
                hints.serialization().registerType(TypeReference.of(registerType));

                Arrays.stream(registerType.getDeclaredFields()).forEach((field -> registerSerializationType(field.getType(), hints)));

                registerSerializationType(registerType.getSuperclass(), hints);
            }
        }

    }

    private static boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive() || cls == Boolean.class || cls == Byte.class
                || cls == Character.class || cls == Short.class || cls == Integer.class
                || cls == Long.class || cls == Float.class || cls == Double.class
                || cls == String.class || cls == Date.class || cls == Class.class;
    }
}
