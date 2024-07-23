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
package org.apache.dubbo.rpc.protocol.tri.rest.util;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

public final class TypeUtils {

    private static final Set<Class<?>> SIMPLE_TYPES = new ConcurrentHashSet<>();

    static {
        Collections.addAll(
                SIMPLE_TYPES,
                Void.class,
                void.class,
                String.class,
                URI.class,
                URL.class,
                UUID.class,
                Locale.class,
                Currency.class,
                Pattern.class,
                Class.class);
    }

    private TypeUtils() {}

    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl == null ? RestUtils.class.getClassLoader() : cl;
    }

    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        return getDefaultClassLoader().loadClass(className);
    }

    public static boolean isPresent(String className) {
        try {
            loadClass(className);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isSimpleProperty(Class<?> type) {
        return type == null || isSimpleValueType(type) || type.isArray() && isSimpleValueType(type.getComponentType());
    }

    private static boolean isSimpleValueType(Class<?> type) {
        if (type.isPrimitive() || ClassUtils.isPrimitiveWrapper(type)) {
            return true;
        }
        if (SIMPLE_TYPES.contains(type)) {
            return true;
        }
        if (Enum.class.isAssignableFrom(type)
                || CharSequence.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || Date.class.isAssignableFrom(type)
                || Temporal.class.isAssignableFrom(type)
                || ZoneId.class.isAssignableFrom(type)
                || TimeZone.class.isAssignableFrom(type)
                || File.class.isAssignableFrom(type)
                || Path.class.isAssignableFrom(type)
                || Charset.class.isAssignableFrom(type)
                || InetAddress.class.isAssignableFrom(type)) {
            SIMPLE_TYPES.add(type);
            return true;
        }
        return false;
    }

    public static Class<?> getMapValueType(Class<?> targetClass) {
        for (Type gi : targetClass.getGenericInterfaces()) {
            if (gi instanceof ParameterizedType) {
                ParameterizedType type = (ParameterizedType) gi;
                if (type.getRawType() == Map.class) {
                    return getActualType(type.getActualTypeArguments()[1]);
                }
            }
        }
        return null;
    }

    public static Class<?> getSuperGenericType(Class<?> clazz, int index) {
        Class<?> result = getNestedActualType(clazz.getGenericSuperclass(), index);
        return result == null ? getNestedActualType(ArrayUtils.first(clazz.getGenericInterfaces()), index) : result;
    }

    public static Class<?> getSuperGenericType(Class<?> clazz) {
        return getSuperGenericType(clazz, 0);
    }

    public static Class<?>[] getNestedActualTypes(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
            int len = typeArgs.length;
            Class<?>[] nestedTypes = new Class<?>[len];
            for (int i = 0; i < len; i++) {
                nestedTypes[i] = getActualType(typeArgs[i]);
            }
            return nestedTypes;
        }
        return null;
    }

    public static Class<?> getNestedActualType(Type type, int index) {
        if (type instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
            if (index < typeArgs.length) {
                return getActualType(typeArgs[index]);
            }
        }
        return null;
    }

    public static Class<?> getActualType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return getActualType(((ParameterizedType) type).getRawType());
        }
        if (type instanceof TypeVariable) {
            return getActualType(((TypeVariable<?>) type).getBounds()[0]);
        }
        if (type instanceof WildcardType) {
            return getActualType(((WildcardType) type).getUpperBounds()[0]);
        }
        if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getActualType(componentType), 0).getClass();
        }
        return null;
    }

    public static Type getNestedGenericType(Type type, int index) {
        if (type instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
            if (index < typeArgs.length) {
                return getActualGenericType(typeArgs[index]);
            }
        }
        return null;
    }

    public static Type getActualGenericType(Type type) {
        if (type instanceof TypeVariable) {
            return ((TypeVariable<?>) type).getBounds()[0];
        }
        if (type instanceof WildcardType) {
            return ((WildcardType) type).getUpperBounds()[0];
        }
        return type;
    }

    public static Object nullDefault(Class<?> targetClass) {
        if (targetClass == long.class) {
            return 0L;
        }
        if (targetClass == int.class) {
            return 0;
        }
        if (targetClass == boolean.class) {
            return Boolean.FALSE;
        }
        if (targetClass == double.class) {
            return 0D;
        }
        if (targetClass == float.class) {
            return 0F;
        }
        if (targetClass == byte.class) {
            return (byte) 0;
        }
        if (targetClass == short.class) {
            return (short) 0;
        }
        if (targetClass == char.class) {
            return (char) 0;
        }
        if (targetClass == Optional.class) {
            return Optional.empty();
        }
        return null;
    }

    public static Object longToObject(long value, Class<?> targetClass) {
        if (targetClass == Long.class) {
            return value;
        }
        if (targetClass == Integer.class) {
            return (int) value;
        }
        if (targetClass == Short.class) {
            return (short) value;
        }
        if (targetClass == Character.class) {
            return (char) value;
        }
        if (targetClass == Byte.class) {
            return (byte) value;
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public static Collection createCollection(Class targetClass) {
        if (targetClass.isInterface()) {
            if (targetClass == List.class || targetClass == Collection.class) {
                return new ArrayList<>();
            }
            if (targetClass == Set.class) {
                return new HashSet<>();
            }
            if (targetClass == SortedSet.class) {
                return new LinkedHashSet<>();
            }
            if (targetClass == Queue.class || targetClass == Deque.class) {
                return new LinkedList<>();
            }
        } else if (Collection.class.isAssignableFrom(targetClass)) {
            if (targetClass == ArrayList.class) {
                return new ArrayList<>();
            }
            if (targetClass == LinkedList.class) {
                return new LinkedList();
            }
            if (targetClass == HashSet.class) {
                return new HashSet<>();
            }
            if (targetClass == LinkedHashSet.class) {
                return new LinkedHashSet<>();
            }
            if (!Modifier.isAbstract(targetClass.getModifiers())) {
                try {
                    Constructor sizeCt = null;
                    for (Constructor ct : targetClass.getConstructors()) {
                        switch (ct.getParameterCount()) {
                            case 0:
                                return (Collection) ct.newInstance();
                            case 1:
                                if (ct.getParameterTypes()[0] == int.class) {
                                    sizeCt = ct;
                                }
                                break;
                            default:
                        }
                    }
                    if (sizeCt != null) {
                        return (Collection) sizeCt.newInstance(16);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        throw new IllegalArgumentException("Unsupported collection type: " + targetClass.getName());
    }

    @SuppressWarnings("rawtypes")
    public static Map createMap(Class targetClass) {
        if (targetClass.isInterface()) {
            if (targetClass == Map.class) {
                return new HashMap<>();
            }
            if (targetClass == ConcurrentMap.class) {
                return new ConcurrentHashMap<>();
            }
            if (SortedMap.class.isAssignableFrom(targetClass)) {
                return new TreeMap<>();
            }
        } else if (Map.class.isAssignableFrom(targetClass)) {
            if (targetClass == HashMap.class) {
                return new HashMap<>();
            }
            if (targetClass == LinkedHashMap.class) {
                return new LinkedHashMap<>();
            }
            if (targetClass == TreeMap.class) {
                return new TreeMap<>();
            }
            if (targetClass == ConcurrentHashMap.class) {
                return new ConcurrentHashMap<>();
            }
            if (!Modifier.isAbstract(targetClass.getModifiers())) {
                try {
                    Constructor sizeCt = null;
                    for (Constructor ct : targetClass.getConstructors()) {
                        if (Modifier.isPublic(ct.getModifiers())) {
                            switch (ct.getParameterCount()) {
                                case 0:
                                    return (Map) ct.newInstance();
                                case 1:
                                    if (ct.getParameterTypes()[0] == int.class) {
                                        sizeCt = ct;
                                    }
                                    break;
                                default:
                            }
                        }
                    }
                    if (sizeCt != null) {
                        return (Map) sizeCt.newInstance(16);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        throw new IllegalArgumentException("Unsupported map type: " + targetClass.getName());
    }

    public static String buildSig(Method method) {
        StringBuilder sb = new StringBuilder(8);
        for (Class<?> type : method.getParameterTypes()) {
            String name = type.getName();
            sb.append(name.charAt(name.lastIndexOf('.') + 1));
        }
        return sb.toString();
    }
}
