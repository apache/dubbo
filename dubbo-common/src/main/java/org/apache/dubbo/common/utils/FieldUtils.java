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

import static org.apache.dubbo.common.utils.ClassUtils.getAllInheritedTypes;

/**
 * The utilities class for Java Reflection {@link Field}
 *
 * @since 2.7.6
 */
public interface FieldUtils {

    /**
     * Like the {@link Class#getDeclaredField(String)} method without throwing any {@link Exception}
     *
     * @param declaredClass the declared class
     * @param fieldName     the name of {@link Field}
     * @return if can't be found, return <code>null</code>
     */
    static Field getDeclaredField(Class<?> declaredClass, String fieldName) {
        Field field = null;
        try {
            field = declaredClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ignored) {
            field = null;
        }
        return field;
    }

    /**
     * Find the {@link Field} by the name in the specified class and its inherited types
     *
     * @param declaredClass the declared class
     * @param fieldName     the name of {@link Field}
     * @return if can't be found, return <code>null</code>
     */
    static Field findField(Class<?> declaredClass, String fieldName) {
        Field field = getDeclaredField(declaredClass, fieldName);
        if (field != null) {
            return field;
        }
        for (Class superType : getAllInheritedTypes(declaredClass)) {
            field = getDeclaredField(superType, fieldName);
            if (field != null) {
                break;
            }
        }
        return field;
    }

    /**
     * Find the {@link Field} by the name in the specified class and its inherited types
     *
     * @param object    the object whose field should be modified
     * @param fieldName the name of {@link Field}
     * @return if can't be found, return <code>null</code>
     */
    static Field findField(Object object, String fieldName) {
        return findField(object.getClass(), fieldName);
    }

    /**
     * Get the value of the specified {@link Field}
     *
     * @param object    the object whose field should be modified
     * @param fieldName the name of {@link Field}
     * @return the value of  the specified {@link Field}
     */
    static Object getFieldValue(Object object, String fieldName) {
        return getFieldValue(object, findField(object, fieldName));
    }

    /**
     * Get the value of the specified {@link Field}
     *
     * @param object the object whose field should be modified
     * @param field  {@link Field}
     * @return the value of  the specified {@link Field}
     */
    static <T> T getFieldValue(Object object, Field field) {
        Object value = null;
        try {
            ReflectUtils.makeAccessible(field);
            value = field.get(object);
        } catch (IllegalAccessException ignored) {
        } finally {
            ReflectUtils.makeAccessible(field);
        }
        return (T) value;
    }

    /**
     * Set the value for the specified {@link Field}
     *
     * @param object    the object whose field should be modified
     * @param fieldName the name of {@link Field}
     * @param value     the value of field to be set
     * @return the previous value of the specified {@link Field}
     */
    static <T> T setFieldValue(Object object, String fieldName, T value) {
        return setFieldValue(object, findField(object, fieldName), value);
    }

    /**
     * Set the value for the specified {@link Field}
     *
     * @param object the object whose field should be modified
     * @param field  {@link Field}
     * @param value  the value of field to be set
     * @return the previous value of the specified {@link Field}
     */
    static <T> T setFieldValue(Object object, Field field, T value) {
        Object previousValue = null;
        try {
            ReflectUtils.makeAccessible(field);
            previousValue = field.get(object);
            field.set(object, value);
        } catch (IllegalAccessException ignored) {
        } finally {
            ReflectUtils.makeAccessible(field);
        }
        return (T) previousValue;
    }

}
