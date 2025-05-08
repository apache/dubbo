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

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

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
import java.util.Arrays;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public final class TypeUtils {

    private static final Set<Class<?>> SIMPLE_TYPES = new ConcurrentHashSet<>();
    private static final List<String> SYSTEM_PREFIXES = new CopyOnWriteArrayList<>();

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

        Collections.addAll(SYSTEM_PREFIXES, "java.", "javax.", "sun.", "com.sun.", "com.google.protobuf.");
    }

    private TypeUtils() {}

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

    public static void addSimpleTypes(Class<?>... types) {
        SIMPLE_TYPES.addAll(Arrays.asList(types));
    }

    public static List<String> getSystemPrefixes() {
        return SYSTEM_PREFIXES;
    }

    public static void addSystemPrefixes(String... prefixes) {
        for (String prefix : prefixes) {
            if (StringUtils.isNotEmpty(prefix)) {
                SYSTEM_PREFIXES.add(prefix);
            }
        }
    }

    public static boolean isSystemType(Class<?> type) {
        String name = type.getName();
        List<String> systemPrefixes = getSystemPrefixes();
        for (int i = systemPrefixes.size() - 1; i >= 0; i--) {
            String prefix = systemPrefixes.get(i);
            if (prefix.charAt(0) == '!') {
                if (name.regionMatches(0, prefix, 1, prefix.length() - 1)) {
                    return false;
                }
            } else if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWrapperType(Class<?> type) {
        return type == Optional.class || type == CompletableFuture.class || type == StreamObserver.class;
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
        if (method.getParameterCount() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder(8);
        for (Class<?> type : method.getParameterTypes()) {
            String name = type.getName();
            sb.append(name.charAt(name.lastIndexOf('.') + 1));
        }
        return sb.toString();
    }

    public static String toTypeString(Type type) {
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            return clazz.isArray() ? clazz.getComponentType().getName() + "[]" : clazz.getName();
        }
        StringBuilder result = new StringBuilder(32);
        buildGenericTypeString(type, result);
        return result.toString();
    }

    private static void buildGenericTypeString(Type type, StringBuilder sb) {
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                buildGenericTypeString(clazz.getComponentType(), sb);
                sb.append("[]");
            } else {
                sb.append(clazz.getName());
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pzType = (ParameterizedType) type;
            Type[] typeArgs = pzType.getActualTypeArguments();
            buildGenericTypeString(pzType.getRawType(), sb);
            sb.append('<');
            for (int i = 0, len = typeArgs.length; i < len; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                buildGenericTypeString(typeArgs[i], sb);
            }
            sb.append('>');
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            Type[] upperBounds = wildcardType.getUpperBounds();
            Type[] lowerBounds = wildcardType.getLowerBounds();
            if (lowerBounds.length > 0) {
                sb.append("? super ");
                buildGenericTypeString(lowerBounds[0], sb);
            } else if (upperBounds.length > 0 && upperBounds[0] != Object.class) {
                sb.append("? extends ");
                buildGenericTypeString(upperBounds[0], sb);
            } else {
                sb.append('?');
            }
        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            buildGenericTypeString(genericArrayType.getGenericComponentType(), sb);
            sb.append("[]");
        } else if (type instanceof TypeVariable) {
            TypeVariable<?> typeVariable = (TypeVariable<?>) type;
            sb.append(typeVariable.getName());
            Type[] bounds = typeVariable.getBounds();
            int len = bounds.length;
            if (len > 0 && !(len == 1 && bounds[0] == Object.class)) {
                sb.append(" extends ");
                for (int i = 0; i < len; i++) {
                    if (i > 0) {
                        sb.append(" & ");
                    }
                    buildGenericTypeString(bounds[i], sb);
                }
            }
        } else {
            sb.append(type.toString());
        }
    }

    public static Object getMethodDescriptor(MethodMeta methodMeta) {
        StringBuilder sb = new StringBuilder(64);
        sb.append(toTypeString(methodMeta.getGenericReturnType()))
                .append(' ')
                .append(methodMeta.getMethod().getName())
                .append('(');
        ParameterMeta[] parameters = methodMeta.getParameters();
        for (int i = 0, len = parameters.length; i < len; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            ParameterMeta paramMeta = parameters[i];
            String name = paramMeta.getName();
            sb.append(toTypeString(paramMeta.getGenericType())).append(' ');
            if (name == null) {
                sb.append("arg").append(i + 1);
            } else {
                sb.append(name);
            }
        }
        sb.append(')');
        return sb.toString();
    }
}
