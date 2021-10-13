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
package org.apache.dubbo.metadata.definition.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * 2015/1/27.
 */
public final class ClassUtils {

    /**
     * Get the code source file or class path of the Class passed in.
     *
     * @param clazz
     * @return Jar file name or class path.
     */
    public static String getCodeSource(Class<?> clazz) {
        ProtectionDomain protectionDomain = clazz.getProtectionDomain();
        if (protectionDomain == null || protectionDomain.getCodeSource() == null) {
            return null;
        }

        CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
        URL location = codeSource.getLocation();
        if (location == null) {
            return null;
        }

        String path = location.toExternalForm();

        if (path.endsWith(".jar") && path.contains("/")) {
            return path.substring(path.lastIndexOf('/') + 1);
        }
        return path;
    }

    /**
     * Get all non-static fields of the Class passed in or its super classes.
     * <p>
     *
     * @param clazz Class to parse.
     * @return field list
     */
    public static List<Field> getNonStaticFields(final Class<?> clazz) {
        List<Field> result = new ArrayList<>();
        Class<?> target = clazz;
        while (target != null) {
            if (JaketConfigurationUtils.isExcludedType(target)) {
                break;
            }

            Field[] fields = target.getDeclaredFields();
            for (Field field : fields) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    continue;
                }

                result.add(field);
            }
            target = target.getSuperclass();
        }

        return result;
    }

    /**
     * Get all public, non-static methods of the Class passed in.
     * <p>
     *
     * @param clazz Class to parse.
     * @return methods list
     */
    public static List<Method> getPublicNonStaticMethods(final Class<?> clazz) {
        List<Method> result = new ArrayList<Method>();

        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            int mod = method.getModifiers();
            if (Modifier.isPublic(mod) && !Modifier.isStatic(mod)) {
                result.add(method);
            }
        }
        return result;
    }

    public static String getCanonicalNameForParameterizedType(ParameterizedType parameterizedType) {
        StringBuilder sb = new StringBuilder();
        Type ownerType = parameterizedType.getOwnerType();
        Class<?> rawType = (Class) parameterizedType.getRawType();
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

        if (ownerType != null) {
            if (ownerType instanceof Class) {
                sb.append(((Class) ownerType).getName());
            } else {
                sb.append(ownerType);
            }

            sb.append('.');

            if (ownerType instanceof ParameterizedType) {
                // Find simple name of nested type by removing the
                // shared prefix with owner.
                sb.append(rawType.getName().replace(((Class) ((ParameterizedType) ownerType).getRawType()).getName() + "$",
                        ""));
            } else {
                sb.append(rawType.getSimpleName());
            }
        } else {
            sb.append(rawType.getCanonicalName());
        }

        if (actualTypeArguments != null &&
                actualTypeArguments.length > 0) {
            sb.append('<');
            boolean first = true;
            for (Type t : actualTypeArguments) {
                if (!first) {
                    sb.append(", ");
                }
                if (t instanceof Class) {
                    Class c = (Class) t;
                    sb.append(c.getCanonicalName());
                } else if (t instanceof ParameterizedType) {
                    sb.append(getCanonicalNameForParameterizedType((ParameterizedType) t));
                } else {
                    sb.append(t.toString());
                }
                first = false;
            }
            sb.append('>');
        }

        return sb.toString();
    }

    private ClassUtils() {
    }
}
