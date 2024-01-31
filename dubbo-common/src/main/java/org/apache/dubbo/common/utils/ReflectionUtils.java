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
package org.apache.dubbo.common.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A utility class that provides methods for accessing and manipulating private fields and methods of an object.
 * This is useful for white-box testing, where the internal workings of a class need to be tested directly.
 * <p>
 * Note: Usage of this class should be limited to testing purposes only, as it violates the encapsulation principle.
 */
public class ReflectionUtils {

    private ReflectionUtils() {}

    /**
     * Retrieves the value of the specified field from the given object.
     *
     * @param source    The object from which to retrieve the field value.
     * @param fieldName The name of the field to retrieve.
     * @return The value of the specified field in the given object.
     * @throws RuntimeException If the specified field does not exist.
     */
    public static Object getField(Object source, String fieldName) {
        try {
            Field f = source.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(source);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    /**
     * Invokes the specified method on the given object with the provided parameters.
     *
     * @param source     The object on which to invoke the method.
     * @param methodName The name of the method to invoke.
     * @param params     The parameters to pass to the method.
     * @return The result of invoking the specified method on the given object.
     */
    public static Object invoke(Object source, String methodName, Object... params) {
        try {
            Class<?>[] classes = Arrays.stream(params)
                    .map(param -> param != null ? param.getClass() : null)
                    .toArray(Class<?>[]::new);

            for (Method method : source.getClass().getDeclaredMethods()) {
                if (method.getName().equals(methodName) && matchParameters(method.getParameterTypes(), classes)) {
                    method.setAccessible(true);
                    return method.invoke(source, params);
                }
            }
            throw new NoSuchMethodException("No method found with the specified name and parameter types");
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    private static boolean matchParameters(Class<?>[] methodParamTypes, Class<?>[] givenParamTypes) {
        if (methodParamTypes.length != givenParamTypes.length) {
            return false;
        }

        for (int i = 0; i < methodParamTypes.length; i++) {
            if (givenParamTypes[i] == null) {
                if (methodParamTypes[i].isPrimitive()) {
                    return false;
                }
            } else if (!methodParamTypes[i].isAssignableFrom(givenParamTypes[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a list of distinct {@link Class} objects representing the generics of the given class that implement the
     * given interface.
     *
     * @param clazz          the class to retrieve the generics for
     * @param interfaceClass the interface to retrieve the generics for
     * @return a list of distinct {@link Class} objects representing the generics of the given class that implement the
     * given interface
     */
    public static List<Class<?>> getClassGenerics(Class<?> clazz, Class<?> interfaceClass) {
        List<Class<?>> generics = new ArrayList<>();
        Type[] genericInterfaces = clazz.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                Type rawType = parameterizedType.getRawType();
                if (rawType instanceof Class && interfaceClass.isAssignableFrom((Class<?>) rawType)) {
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    for (Type actualTypeArgument : actualTypeArguments) {
                        if (actualTypeArgument instanceof Class) {
                            generics.add((Class<?>) actualTypeArgument);
                        }
                    }
                }
            }
        }
        Type genericSuperclass = clazz.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            for (Type actualTypeArgument : actualTypeArguments) {
                if (actualTypeArgument instanceof Class) {
                    generics.add((Class<?>) actualTypeArgument);
                }
            }
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            generics.addAll(getClassGenerics(superclass, interfaceClass));
        }
        return generics.stream().distinct().collect(Collectors.toList());
    }

    public static class ReflectionException extends RuntimeException {
        public ReflectionException(Throwable cause) {
            super(cause);
        }
    }

    public static boolean match(Class<?> clazz, Class<?> interfaceClass, Object event) {
        List<Class<?>> eventTypes = ReflectionUtils.getClassGenerics(clazz, interfaceClass);
        return eventTypes.stream().allMatch(eventType -> eventType.isInstance(event));
    }
}
