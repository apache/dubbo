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
import java.lang.reflect.ParameterizedType;
import java.util.Map;

public class MethodUtils {

    public static boolean isSetter(Method method) {
        return method.getName().startsWith("set")
                && !"set".equals(method.getName())
                && Modifier.isPublic(method.getModifiers())
                && method.getParameterCount() == 1
                && ClassUtils.isPrimitive(method.getParameterTypes()[0]);
    }

    /**
     * test if method is a setter if Map<String, String>
     */
    public static boolean isStringMapSetter(Method method) {
        return method.getName().startsWith("set")
                && !"set".equals(method.getName())
                && Modifier.isPublic(method.getModifiers())
                && method.getParameterCount() == 1
                && Map.class == method.getParameterTypes()[0]
                && Map.class != method.getGenericParameterTypes()[0]
                && ((ParameterizedType)method.getGenericParameterTypes()[0]).getActualTypeArguments()[0] == String.class
                && ((ParameterizedType)method.getGenericParameterTypes()[0]).getActualTypeArguments()[1] == String.class;
    }


    public static Method findGetterBySetter(Class<?> clazz, Method setterMethod) {
        String propertyName = setterMethod.getName().substring("set".length());
        try {
            return clazz.getMethod("get"+propertyName);
        } catch (NoSuchMethodException e) {
            if (setterMethod.getParameterTypes()[0] == boolean.class || setterMethod.getParameterTypes()[0] == Boolean.class) {
                try {
                    return clazz.getMethod("is"+propertyName);
                } catch (NoSuchMethodException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    public static boolean isGetter(Method method) {
        String name = method.getName();
        return (name.startsWith("get") || name.startsWith("is"))
                && !"get".equals(name) && !"is".equals(name)
                && !"getClass".equals(name) && !"getObject".equals(name)
                && Modifier.isPublic(method.getModifiers())
                && method.getParameterTypes().length == 0
                && ClassUtils.isPrimitive(method.getReturnType());
    }

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
}
