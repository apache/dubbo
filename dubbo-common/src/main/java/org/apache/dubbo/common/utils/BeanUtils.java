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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.dubbo.common.utils.ClassUtils.isAssignableFrom;

public class BeanUtils {

    private static final List<Class<?>> CAN_BE_STRING = Arrays.asList(Byte.class, Short.class, Integer.class,
            Long.class, Float.class, Double.class, Boolean.class, Character.class);

    /**
     * convert map to a specific class instance
     *
     * @param map map wait for convert
     * @param cls the specified class
     * @param <T> the type of {@code cls}
     * @return class instance declare in param {@code cls}
     * @throws IllegalAccessException if the instance creation is failed
     * @throws InstantiationException if the instance creation is failed
     */
    public static <T> T mapToBean(Map<String, Object> map, Class<T> cls) throws IllegalAccessException, InstantiationException {
        T instance = cls.newInstance();
        Map<String, Field> beanPropertyFields = ReflectUtils.getBeanPropertyFields(cls);
        for (Map.Entry<String, Field> entry : beanPropertyFields.entrySet()) {
            String name = entry.getKey();
            Field field = entry.getValue();
            Object mapObject = map.get(name);
            if (mapObject == null) {
                continue;
            }

            Type type = field.getGenericType();
            Object fieldObject = getFieldObject(mapObject, type);
            field.set(instance, fieldObject);
        }

        return instance;
    }

    private static Object getFieldObject(Object mapObject, Type fieldType) throws InstantiationException, IllegalAccessException {
        if (fieldType instanceof Class<?>) {
            return convertClassType(mapObject, (Class<?>) fieldType);
        } else if (fieldType instanceof ParameterizedType) {
            return convertParameterizedType(mapObject, (ParameterizedType) fieldType);
        } else if (fieldType instanceof GenericArrayType || fieldType instanceof TypeVariable<?> || fieldType instanceof WildcardType) {
            // ignore these type currently
            return null;
        } else {
            throw new IllegalArgumentException("Unrecognized Type: " + fieldType.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private static Object convertClassType(Object mapObject, Class<?> type) throws InstantiationException, IllegalAccessException {
        if (type.isPrimitive() || isAssignableFrom(type, mapObject.getClass())) {
            return mapObject;
        } else if (Objects.equals(type, String.class) && CAN_BE_STRING.contains(mapObject.getClass())) {
            // auto convert specified type to string
            return mapObject.toString();
        } else if (mapObject instanceof Map) {
            return mapToBean((Map<String, Object>) mapObject, type);
        } else {
            // type didn't match and mapObject is not another Map struct.
            // we just ignore this situation.
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Object convertParameterizedType(Object mapObject, ParameterizedType type) throws IllegalAccessException, InstantiationException {
        Type rawType = type.getRawType();
        if (!isAssignableFrom((Class<?>) rawType, mapObject.getClass())) {
            return null;
        }

        Type[] actualTypeArguments = type.getActualTypeArguments();
        if (isAssignableFrom(Map.class, (Class<?>) rawType)) {
            Map<Object, Object> map = (Map<Object, Object>) mapObject.getClass().newInstance();
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) mapObject).entrySet()) {
                Object key = getFieldObject(entry.getKey(), actualTypeArguments[0]);
                Object value = getFieldObject(entry.getValue(), actualTypeArguments[1]);
                map.put(key, value);
            }

            return map;
        } else if (isAssignableFrom(Collection.class, (Class<?>) rawType)) {
            Collection<Object> collection = (Collection<Object>) mapObject.getClass().newInstance();
            for (Object m : (Iterable<?>) mapObject) {
                Object ele = getFieldObject(m, actualTypeArguments[0]);
                collection.add(ele);
            }

            return collection;
        } else {
            // ignore other type currently
            return null;
        }
    }
}
