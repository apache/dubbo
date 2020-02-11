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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Miscellaneous method utility methods.
 * Mainly for internal use within the framework.
 *
 * @author LiZhenNet
 * @since 2.7.2
 */
public class MethodUtils {

    /**
     * Return {@code true} if the provided method is a set method.
     * Otherwise, return {@code false}.
     *
     * @param method the method to check
     * @return whether the given method is setter method
     */
    public static boolean isSetter(Method method) {
        return method.getName().startsWith("set")
                && !"set".equals(method.getName())
                && Modifier.isPublic(method.getModifiers())
                && method.getParameterCount() == 1
                && ClassUtils.isPrimitive(method.getParameterTypes()[0]);
    }

    /**
     * Return {@code true} if the provided method is a get method.
     * Otherwise, return {@code false}.
     *
     * @param method the method to check
     * @return whether the given method is getter method
     */
    public static boolean isGetter(Method method) {
        String name = method.getName();
        return (name.startsWith("get") || name.startsWith("is"))
                && !"get".equals(name) && !"is".equals(name)
                && !"getClass".equals(name) && !"getObject".equals(name)
                && Modifier.isPublic(method.getModifiers())
                && method.getParameterTypes().length == 0
                && ClassUtils.isPrimitive(method.getReturnType());
    }

    /**
     * Return {@code true} If this method is a meta method.
     * Otherwise, return {@code false}.
     *
     * @param method the method to check
     * @return whether the given method is meta method
     */
    public static boolean isMetaMethod(Method method) {
        String name = method.getName();
        if (!(name.startsWith("get") || name.startsWith("is"))) {
            return false;
        }
        if ("get".equals(name)) {
            return false;
        }
        if ("getClass".equals(name)) {
            return false;
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }
        if (method.getParameterTypes().length != 0) {
            return false;
        }
        if (!ClassUtils.isPrimitive(method.getReturnType())) {
            return false;
        }
        return true;
    }

    /**
     * Check if the method is a deprecated method. The standard is whether the {@link java.lang.Deprecated} annotation is declared on the class.
     * Return {@code true} if this annotation is present.
     * Otherwise, return {@code false}.
     *
     * @param method the method to check
     * @return whether the given method is deprecated method
     */
    public static boolean isDeprecated(Method method) {
        return method.getAnnotation(Deprecated.class) != null;
    }
}
