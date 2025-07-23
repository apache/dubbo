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
package org.apache.dubbo.rpc.protocol.tri.rest.argument;

import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.DateUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpCookie;
import org.apache.dubbo.remoting.http12.HttpJsonUtils;
import org.apache.dubbo.remoting.http12.HttpRequest.FileUpload;
import org.apache.dubbo.remoting.http12.HttpUtils;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.remoting.http12.message.codec.CodecUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.RestParameterException;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.dubbo.common.utils.StringUtils.tokenizeToList;
import static org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils.getActualGenericType;
import static org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils.getActualType;
import static org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils.nullDefault;

@SuppressWarnings({"unchecked", "rawtypes"})
public class GeneralTypeConverter implements TypeConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralTypeConverter.class);

    private final CompositeArgumentConverter converter;
    private final CodecUtils codecUtils;
    private final HttpJsonUtils httpJsonUtils;

    public GeneralTypeConverter(FrameworkModel frameworkModel) {
        converter = frameworkModel.getOrRegisterBean(CompositeArgumentConverter.class);
        codecUtils = frameworkModel.getOrRegisterBean(CodecUtils.class);
        httpJsonUtils = frameworkModel.getOrRegisterBean(HttpJsonUtils.class);
    }

    @Override
    public <T> T convert(Object source, Class<T> targetClass) {
        try {
            return targetClass == null ? (T) source : (T) doConvert(source, targetClass);
        } catch (Exception e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    @Override
    public <T> T convert(Object source, Type targetType) {
        try {
            return targetType == null ? (T) source : (T) doConvert(source, targetType);
        } catch (Exception e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private <T> Object doConvert(Object source, Class<T> targetClass) throws Exception {
        if (source == null) {
            return nullDefault(targetClass);
        }

        if (targetClass.isInstance(source)) {
            return source;
        }

        if (targetClass == Optional.class) {
            return Optional.of(source);
        }

        Class sourceClass = source.getClass();
        if (sourceClass == Optional.class) {
            source = ((Optional<?>) source).orElse(null);
            if (source == null) {
                return nullDefault(targetClass);
            }

            if (targetClass.isInstance(source)) {
                return source;
            }
        }

        Object target = customConvert(source, targetClass);
        if (target != null) {
            return target;
        }

        if (source instanceof CharSequence) {
            String str = source.toString();

            if (targetClass == String.class) {
                return str;
            }

            if (str.isEmpty() || "null".equals(str) || "NULL".equals(str)) {
                return emptyDefault(targetClass);
            }

            switch (targetClass.getName()) {
                case "java.lang.Double":
                case "double":
                    return Double.valueOf(str);
                case "java.lang.Float":
                case "float":
                    return Float.valueOf(str);
                case "java.lang.Long":
                case "long":
                    return isHexNumber(str) ? Long.decode(str) : Long.valueOf(str);
                case "java.lang.Integer":
                case "int":
                    return isHexNumber(str) ? Integer.decode(str) : Integer.valueOf(str);
                case "java.lang.Short":
                case "short":
                    return isHexNumber(str) ? Short.decode(str) : Short.valueOf(str);
                case "java.lang.Character":
                case "char":
                    if (str.length() == 1) {
                        return str.charAt(0);
                    }
                    throw new RestParameterException("Can not convert String(" + str + ") to char, must only 1 char");
                case "java.lang.Byte":
                case "byte":
                    return isHexNumber(str) ? Byte.decode(str) : Byte.valueOf(str);
                case "java.lang.Boolean":
                    return toBoolean(str);
                case "boolean":
                    return toBoolean(str) == Boolean.TRUE;
                case "java.math.BigInteger":
                    return new BigInteger(str);
                case "java.math.BigDecimal":
                    return new BigDecimal(str);
                case "java.lang.Number":
                    return str.indexOf('.') == -1 ? doConvert(str, Long.class) : doConvert(str, Double.class);
                case "java.util.Date":
                    return DateUtils.parse(str);
                case "java.util.Calendar":
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(DateUtils.parse(str).getTime());
                    return cal;
                case "java.sql.Timestamp":
                    return new Timestamp(DateUtils.parse(str).getTime());
                case "java.time.Instant":
                    return DateUtils.parse(str).toInstant();
                case "java.time.ZonedDateTime":
                    return toZonedDateTime(str);
                case "java.time.LocalDate":
                    return toZonedDateTime(str).toLocalDate();
                case "java.time.LocalTime":
                    return toZonedDateTime(str).toLocalTime();
                case "java.time.LocalDateTime":
                    return toZonedDateTime(str).toLocalDateTime();
                case "java.time.ZoneId":
                    return TimeZone.getTimeZone(str).toZoneId();
                case "java.util.TimeZone":
                    return TimeZone.getTimeZone(str);
                case "java.io.File":
                    return new File(str);
                case "java.nio.file.Path":
                    return Paths.get(str);
                case "java.nio.charset.Charset":
                    return Charset.forName(str);
                case "java.net.InetAddress":
                    return InetAddress.getByName(str);
                case "java.net.URI":
                    return new URI(str);
                case "java.net.URL":
                    return new URL(str);
                case "java.util.UUID":
                    return UUID.fromString(str);
                case "java.util.Locale":
                    String[] parts = StringUtils.tokenize(str, '-', '_');
                    switch (parts.length) {
                        case 2:
                            return new Locale(parts[0], parts[1]);
                        case 3:
                            return new Locale(parts[0], parts[1], parts[2]);
                        default:
                            return new Locale(parts[0]);
                    }
                case "java.util.Currency":
                    return Currency.getInstance(str);
                case "java.util.regex.Pattern":
                    return Pattern.compile(str);
                case "java.lang.Class":
                    return ClassUtils.loadClass(str);
                case "[B":
                    return str.getBytes(UTF_8);
                case "[C":
                    return str.toCharArray();
                case "java.util.OptionalInt":
                    return OptionalInt.of(isHexNumber(str) ? Integer.decode(str) : Integer.parseInt(str));
                case "java.util.OptionalLong":
                    return OptionalLong.of(isHexNumber(str) ? Long.decode(str) : Long.parseLong(str));
                case "java.util.OptionalDouble":
                    return OptionalDouble.of(Double.parseDouble(str));
                case "java.util.Properties":
                    Properties properties = new Properties();
                    properties.load(new StringReader(str));
                    return properties;
                default:
            }

            if (targetClass.isEnum()) {
                try {
                    return Enum.valueOf((Class<Enum>) targetClass, str);
                } catch (Exception ignored) {
                }
            }

            target = jsonToObject(str, targetClass);
            if (target != null) {
                return target;
            }

            if (targetClass.isArray()) {
                List<String> list = tokenizeToList(str);
                int n = list.size();
                Class itemType = targetClass.getComponentType();
                if (itemType == String.class) {
                    return list.toArray(StringUtils.EMPTY_STRING_ARRAY);
                }
                Object arr = Array.newInstance(itemType, n);
                for (int i = 0; i < n; i++) {
                    Array.set(arr, i, doConvert(list.get(i), itemType));
                }
                return arr;
            } else if (Collection.class.isAssignableFrom(targetClass)) {
                target = convertCollection(tokenizeToList(str), targetClass);
                if (target != null) {
                    return target;
                }
            } else if (Map.class.isAssignableFrom(targetClass)) {
                target = convertMap(tokenizeToMap(str), targetClass);
                if (target != null) {
                    return target;
                }
            }
        } else if (source instanceof Number) {
            Number num = (Number) source;

            switch (targetClass.getName()) {
                case "java.lang.String":
                    return source.toString();
                case "java.lang.Double":
                case "double":
                    return num.doubleValue();
                case "java.lang.Float":
                case "float":
                    return num.floatValue();
                case "java.lang.Long":
                case "long":
                    return num.longValue();
                case "java.lang.Integer":
                case "int":
                    return num.intValue();
                case "java.lang.Short":
                case "short":
                    return num.shortValue();
                case "java.lang.Character":
                case "char":
                    return (char) num.intValue();
                case "java.lang.Byte":
                case "byte":
                    return num.byteValue();
                case "java.lang.Boolean":
                case "boolean":
                    return toBoolean(num);
                case "java.math.BigInteger":
                    return BigInteger.valueOf(num.longValue());
                case "java.math.BigDecimal":
                    return BigDecimal.valueOf(num.doubleValue());
                case "java.util.Date":
                    return new Date(num.longValue());
                case "java.util.Calendar":
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(num.longValue());
                    return cal;
                case "java.sql.Timestamp":
                    return new Timestamp(num.longValue());
                case "java.time.Instant":
                    return Instant.ofEpochMilli(num.longValue());
                case "java.time.ZonedDateTime":
                    return toZonedDateTime(num);
                case "java.time.LocalDate":
                    return toZonedDateTime(num).toLocalDate();
                case "java.time.LocalTime":
                    return toZonedDateTime(num).toLocalTime();
                case "java.time.LocalDateTime":
                    return toZonedDateTime(num).toLocalDateTime();
                case "java.util.TimeZone":
                    return toTimeZone(num.intValue());
                case "[B":
                    return toBytes(num);
                case "[C":
                    return new char[] {(char) num.intValue()};
                case "java.util.OptionalInt":
                    return OptionalInt.of(num.intValue());
                case "java.util.OptionalLong":
                    return OptionalLong.of(num.longValue());
                case "java.util.OptionalDouble":
                    return OptionalDouble.of(num.doubleValue());
                default:
            }

            if (targetClass.isEnum()) {
                for (T e : targetClass.getEnumConstants()) {
                    if (((Enum) e).ordinal() == num.intValue()) {
                        return e;
                    }
                }
            }
        } else if (source instanceof Date) {
            Date date = (Date) source;
            switch (targetClass.getName()) {
                case "java.lang.String":
                    return DateUtils.format(date);
                case "java.lang.Long":
                case "long":
                    return date.getTime();
                case "java.lang.Integer":
                case "int":
                    return (int) (date.getTime() / 1000);
                case "java.util.Calendar":
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(date.getTime());
                    return cal;
                case "java.time.Instant":
                    return date.toInstant();
                case "java.time.ZonedDateTime":
                    return toZonedDateTime(date.getTime());
                case "java.time.LocalDate":
                    return toZonedDateTime(date.getTime()).toLocalDate();
                case "java.time.LocalTime":
                    return toZonedDateTime(date.getTime()).toLocalTime();
                case "java.time.LocalDateTime":
                    return toZonedDateTime(date.getTime()).toLocalDateTime();
                default:
            }
        } else if (source instanceof TemporalAccessor) {
            return doConvert(DateUtils.toDate((TemporalAccessor) source), targetClass);
        } else if (source instanceof Enum) {
            Enum en = (Enum) source;
            if (targetClass == String.class) {
                return en.toString();
            }
            if (targetClass == int.class || targetClass == Integer.class) {
                return en.ordinal();
            }
            if (Number.class.isAssignableFrom(targetClass)) {
                return doConvert(en.ordinal(), targetClass);
            }
            if (targetClass.isEnum()) {
                return Enum.valueOf((Class<Enum>) targetClass, en.name());
            }
        } else if (source instanceof byte[]) {
            byte[] bytes = (byte[]) source;

            if (bytes.length == 0) {
                return emptyDefault(targetClass);
            }

            switch (targetClass.getName()) {
                case "java.lang.String":
                    return new String(bytes, UTF_8);
                case "java.lang.Double":
                case "double":
                    return ByteBuffer.wrap(bytes).getDouble();
                case "java.lang.Float":
                case "float":
                    return ByteBuffer.wrap(bytes).getFloat();
                case "java.lang.Long":
                case "long":
                    return ByteBuffer.wrap(bytes).getLong();
                case "java.lang.Integer":
                case "int":
                    return ByteBuffer.wrap(bytes).getInt();
                case "java.lang.Short":
                case "short":
                    return ByteBuffer.wrap(bytes).getShort();
                case "java.lang.Character":
                case "char":
                    return ByteBuffer.wrap(bytes).getChar();
                case "java.lang.Byte":
                case "byte":
                    return bytes[0];
                case "java.lang.Boolean":
                case "boolean":
                    return bytes[0] == (byte) 0 ? Boolean.FALSE : Boolean.TRUE;
                case "java.math.BigInteger":
                    return new BigInteger(bytes);
                case "java.util.Properties":
                    Properties properties = new Properties();
                    properties.load(new ByteArrayInputStream(bytes));
                    return properties;
                default:
            }

            target = jsonToObject(new String(bytes, StandardCharsets.ISO_8859_1), targetClass);
            if (target != null) {
                return target;
            }
        }

        if (targetClass.isArray()) {
            if (targetClass == byte[].class) {
                if (source instanceof InputStream) {
                    try (InputStream is = (InputStream) source) {
                        return StreamUtils.readBytes(is);
                    }
                }
                if (source instanceof FileUpload) {
                    try (InputStream is = ((FileUpload) source).inputStream()) {
                        return StreamUtils.readBytes(is);
                    }
                }
                if (source instanceof Character) {
                    char c = (Character) source;
                    return new byte[] {(byte) (c >> 8), (byte) c};
                }
                if (source instanceof Boolean) {
                    boolean b = (Boolean) source;
                    return new byte[] {b ? (byte) 1 : (byte) 0};
                }
            }

            Class itemType = targetClass.getComponentType();

            if (source instanceof Collection) {
                Collection c = (Collection) source;
                int i = 0;
                Object arr = Array.newInstance(itemType, c.size());
                for (Object item : c) {
                    Array.set(arr, i++, item == null ? null : doConvert(item, itemType));
                }
                return arr;
            }

            if (source instanceof Iterable) {
                List list = new ArrayList();
                for (Object item : (Iterable) source) {
                    list.add(item == null ? null : doConvert(item, itemType));
                }
                return list.toArray((Object[]) Array.newInstance(itemType, 0));
            }

            if (sourceClass.isArray()) {
                int len = Array.getLength(source);
                Object arr = Array.newInstance(itemType, len);
                for (int i = 0; i < len; i++) {
                    Object item = Array.get(source, i);
                    Array.set(arr, i, item == null ? null : doConvert(item, itemType));
                }
                return arr;
            }

            Object arr = Array.newInstance(itemType, 1);
            Array.set(arr, 0, doConvert(source, itemType));
            return arr;
        }

        if (Collection.class.isAssignableFrom(targetClass)) {
            target = convertCollection(toCollection(source), targetClass);
            if (target != null) {
                return target;
            }
        }

        if (Map.class.isAssignableFrom(targetClass) && source instanceof Map) {
            target = convertMap((Map) source, targetClass);
            if (target != null) {
                return target;
            }
        }

        if (sourceClass.isArray()) {
            if (Array.getLength(source) == 0) {
                return nullDefault(targetClass);
            }
            return doConvert(Array.get(source, 0), targetClass);
        }

        if (source instanceof List) {
            List list = (List) source;
            if (list.isEmpty()) {
                return nullDefault(targetClass);
            }
            return doConvert(list.get(0), targetClass);
        }

        if (source instanceof Iterable) {
            Iterator it = ((Iterable) source).iterator();
            if (!it.hasNext()) {
                return nullDefault(targetClass);
            }
            return doConvert(it.next(), targetClass);
        }

        if (targetClass == String.class) {
            if (sourceClass == HttpCookie.class) {
                return ((HttpCookie) source).value();
            }
            if (source instanceof InputStream) {
                try (InputStream is = (InputStream) source) {
                    return StreamUtils.toString(is);
                }
            }
            if (source instanceof FileUpload) {
                FileUpload fu = (FileUpload) source;
                try (InputStream is = fu.inputStream()) {
                    String contentType = fu.contentType();
                    if (contentType != null) {
                        int index = contentType.lastIndexOf(HttpUtils.CHARSET_PREFIX);
                        if (index > 0) {
                            return StreamUtils.toString(
                                    is,
                                    Charset.forName(
                                            contentType.substring(index + 8).trim()));
                        }
                    }
                    return StreamUtils.toString(is);
                }
            }
            return source.toString();
        }

        if (!Modifier.isAbstract(targetClass.getModifiers())) {
            try {
                for (Constructor ct : targetClass.getConstructors()) {
                    if (ct.getParameterCount() == 1) {
                        if (ct.getParameterTypes()[0].isAssignableFrom(sourceClass)) {
                            return ct.newInstance(source);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        if (sourceClass == String.class) {
            try {
                Method valueOf = targetClass.getMethod("valueOf", String.class);
                //noinspection JavaReflectionInvocation
                return valueOf.invoke(null, source);
            } catch (Throwable ignored) {
            }
            return null;
        }

        try {
            return httpJsonUtils.convertObject(source, targetClass);
        } catch (Throwable t) {
            String msg = "JSON convert value '{}' from type [{}] to type [{}] failed";
            LOGGER.debug(msg, source, sourceClass, targetClass, t);
        }

        return null;
    }

    private Object doConvert(Object source, Type targetType) throws Exception {
        if (targetType instanceof Class) {
            return doConvert(source, (Class) targetType);
        }

        if (source == null) {
            return nullDefault(getActualType(targetType));
        }

        if (source.getClass() == Optional.class) {
            source = ((Optional<?>) source).orElse(null);
            if (source == null) {
                return nullDefault(getActualType(targetType));
            }
        }

        if (source instanceof CharSequence) {
            String str = source.toString();

            if (str.isEmpty() || "null".equals(str) || "NULL".equals(str)) {
                return emptyDefault(getActualType(targetType));
            }

            Object target = jsonToObject(str, targetType);
            if (target != null) {
                return target;
            }
        }

        if (targetType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) targetType;
            Type rawType = type.getRawType();
            if (rawType instanceof Class) {
                Class targetClass = (Class) rawType;
                Type[] argTypes = type.getActualTypeArguments();

                if (Collection.class.isAssignableFrom(targetClass)) {
                    Type itemType = getActualGenericType(argTypes[0]);
                    if (itemType instanceof Class && targetClass.isInstance(source)) {
                        boolean same = true;
                        Class<?> itemClass = (Class<?>) itemType;
                        for (Object item : (Collection) source) {
                            if (item != null && !itemClass.isInstance(item)) {
                                same = false;
                                break;
                            }
                        }
                        if (same) {
                            return source;
                        }
                    }

                    Collection items = toCollection(source);
                    Collection targetItems = createCollection(targetClass, items.size());
                    for (Object item : items) {
                        targetItems.add(doConvert(item, itemType));
                    }
                    return targetItems;
                }

                if (Map.class.isAssignableFrom(targetClass)) {
                    Type keyType = argTypes[0];
                    Type valueType = argTypes[1];

                    if (keyType instanceof Class && valueType instanceof Class && targetClass.isInstance(source)) {
                        boolean same = true;
                        Class<?> keyClass = (Class<?>) keyType;
                        Class<?> valueClass = (Class<?>) valueType;
                        for (Map.Entry entry : ((Map<Object, Object>) source).entrySet()) {
                            Object key = entry.getKey();
                            if (key != null && !keyClass.isInstance(key)) {
                                same = false;
                                break;
                            }
                            Object value = entry.getValue();
                            if (value != null && !valueClass.isInstance(value)) {
                                same = false;
                                break;
                            }
                        }
                        if (same) {
                            return source;
                        }
                    }

                    Class<?> mapValueClass = TypeUtils.getMapValueType(targetClass);
                    boolean multiValue = mapValueClass != null && Collection.class.isAssignableFrom(mapValueClass);

                    if (source instanceof CharSequence) {
                        source = tokenizeToMap(source.toString());
                    }

                    if (source instanceof Map) {
                        Map<?, ?> map = (Map) source;
                        Map targetMap = createMap(targetClass, map.size());
                        for (Map.Entry entry : map.entrySet()) {
                            Object key = doConvert(entry.getKey(), keyType);
                            if (multiValue) {
                                Collection items = toCollection(entry.getValue());
                                Collection targetItems = createCollection(mapValueClass, items.size());
                                for (Object item : items) {
                                    targetItems.add(doConvert(item, valueType));
                                }
                                targetMap.put(key, targetItems);
                            } else {
                                targetMap.put(key, doConvert(entry.getValue(), valueType));
                            }
                        }
                        return targetMap;
                    }
                }

                if (targetClass == Optional.class) {
                    return Optional.ofNullable(doConvert(source, argTypes[0]));
                }
            }
        } else if (targetType instanceof TypeVariable) {
            return doConvert(source, ((TypeVariable<?>) targetType).getBounds()[0]);
        } else if (targetType instanceof WildcardType) {
            return doConvert(source, ((WildcardType) targetType).getUpperBounds()[0]);
        } else if (targetType instanceof GenericArrayType) {
            Type itemType = ((GenericArrayType) targetType).getGenericComponentType();
            Class<?> itemClass = getActualType(itemType);
            Collection items = toCollection(source);
            Object target = Array.newInstance(itemClass, items.size());
            int i = 0;
            for (Object item : items) {
                Array.set(target, i++, doConvert(item, itemType));
            }
            return target;
        }

        try {
            return httpJsonUtils.convertObject(source, targetType);
        } catch (Throwable t) {
            String msg = "JSON convert value '{}' from type [{}] to type [{}] failed";
            LOGGER.debug(msg, source, source.getClass(), targetType, t);
        }

        return null;
    }

    protected Object customConvert(Object source, Class<?> targetClass) {
        return converter.convert(source, targetClass);
    }

    protected Collection customCreateCollection(Class targetClass, int size) {
        return (Collection) converter.convert(size, targetClass);
    }

    protected Map customCreateMap(Class targetClass, int size) {
        return (Map) converter.convert(size, targetClass);
    }

    private Collection createCollection(Class targetClass, int size) {
        if (targetClass.isInterface()) {
            if (targetClass == List.class || targetClass == Collection.class) {
                return new ArrayList<>(size);
            }
            if (targetClass == Set.class) {
                return CollectionUtils.newHashSet(size);
            }
            if (targetClass == SortedSet.class) {
                return CollectionUtils.newLinkedHashSet(size);
            }
            if (targetClass == Queue.class || targetClass == Deque.class) {
                return new LinkedList<>();
            }
        } else if (Collection.class.isAssignableFrom(targetClass)) {
            if (targetClass == ArrayList.class) {
                return new ArrayList<>(size);
            }
            if (targetClass == LinkedList.class) {
                return new LinkedList();
            }
            if (targetClass == HashSet.class) {
                return CollectionUtils.newHashSet(size);
            }
            if (targetClass == LinkedHashSet.class) {
                return CollectionUtils.newLinkedHashSet(size);
            }
            if (!Modifier.isAbstract(targetClass.getModifiers())) {
                try {
                    Constructor defCt = null;
                    for (Constructor ct : targetClass.getConstructors()) {
                        switch (ct.getParameterCount()) {
                            case 0:
                                defCt = ct;
                                break;
                            case 1:
                                Class paramType = ct.getParameterTypes()[0];
                                if (paramType == int.class) {
                                    return (Collection) ct.newInstance(size);
                                }
                                break;
                            default:
                        }
                    }
                    if (defCt != null) {
                        return (Collection) defCt.newInstance();
                    }
                } catch (Exception ignored) {
                }
            }
        }
        Collection collection = customCreateCollection(targetClass, size);
        if (collection != null) {
            return collection;
        }
        if (targetClass.isAssignableFrom(ArrayList.class)) {
            return new ArrayList<>(size);
        }
        if (targetClass.isAssignableFrom(LinkedHashSet.class)) {
            return CollectionUtils.newLinkedHashSet(size);
        }
        throw new IllegalArgumentException("Unsupported collection type: " + targetClass.getName());
    }

    private Collection convertCollection(Collection source, Class targetClass) {
        if (targetClass.isInstance(source)) {
            return source;
        }
        if (targetClass.isInterface()) {
            if (targetClass == List.class || targetClass == Collection.class) {
                return new ArrayList<>(source);
            }
            if (targetClass == Set.class) {
                return new HashSet<>(source);
            }
            if (targetClass == SortedSet.class) {
                return new LinkedHashSet(source);
            }
            if (targetClass == Queue.class || targetClass == Deque.class) {
                return new LinkedList<>(source);
            }
        } else {
            if (targetClass == ArrayList.class) {
                return new ArrayList<>(source);
            }
            if (targetClass == LinkedList.class) {
                return new LinkedList(source);
            }
            if (targetClass == HashSet.class) {
                return new HashSet(source);
            }
            if (targetClass == LinkedHashSet.class) {
                return new LinkedHashSet(source);
            }
            if (Modifier.isAbstract(targetClass.getModifiers())) {
                Collection collection = customCreateCollection(targetClass, source.size());
                if (collection != null) {
                    collection.addAll(source);
                    return collection;
                }
                if (targetClass.isAssignableFrom(ArrayList.class)) {
                    return new ArrayList<>(source);
                }
                if (targetClass.isAssignableFrom(LinkedHashSet.class)) {
                    return new LinkedHashSet(source);
                }
                return null;
            }
            try {
                Constructor defCt = null;
                for (Constructor ct : targetClass.getConstructors()) {
                    if (Modifier.isPublic(ct.getModifiers())) {
                        switch (ct.getParameterCount()) {
                            case 0:
                                defCt = ct;
                                break;
                            case 1:
                                Class paramType = ct.getParameterTypes()[0];
                                if (paramType == Collection.class) {
                                    return (Collection) ct.newInstance(source);
                                } else if (paramType == List.class) {
                                    return (Collection) ct.newInstance(toList(source));
                                }
                                break;
                            default:
                        }
                    }
                }
                if (defCt != null) {
                    Collection c = (Collection) defCt.newInstance();
                    c.addAll(source);
                    return c;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Map createMap(Class targetClass, int size) {
        if (targetClass.isInterface()) {
            if (targetClass == Map.class) {
                return CollectionUtils.newHashMap(size);
            }
            if (targetClass == ConcurrentMap.class) {
                return CollectionUtils.newConcurrentHashMap(size);
            }
            if (SortedMap.class.isAssignableFrom(targetClass)) {
                return new TreeMap<>();
            }
        } else if (Map.class.isAssignableFrom(targetClass)) {
            if (targetClass == HashMap.class) {
                return CollectionUtils.newHashMap(size);
            }
            if (targetClass == LinkedHashMap.class) {
                return CollectionUtils.newLinkedHashMap(size);
            }
            if (targetClass == TreeMap.class) {
                return new TreeMap<>();
            }
            if (targetClass == ConcurrentHashMap.class) {
                return CollectionUtils.newConcurrentHashMap(size);
            }
            if (!Modifier.isAbstract(targetClass.getModifiers())) {
                try {
                    Constructor defCt = null;
                    for (Constructor ct : targetClass.getConstructors()) {
                        if (Modifier.isPublic(ct.getModifiers())) {
                            switch (ct.getParameterCount()) {
                                case 0:
                                    defCt = ct;
                                    break;
                                case 1:
                                    if (ct.getParameterTypes()[0] == int.class) {
                                        return (Map) ct.newInstance(CollectionUtils.capacity(size));
                                    }
                                    break;
                                default:
                            }
                        }
                    }
                    if (defCt != null) {
                        return (Map) defCt.newInstance();
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        Map map = customCreateMap(targetClass, size);
        if (map != null) {
            return map;
        }
        if (targetClass.isAssignableFrom(LinkedHashMap.class)) {
            return CollectionUtils.newLinkedHashMap(size);
        }
        throw new IllegalArgumentException("Unsupported map type: " + targetClass.getName());
    }

    private Map convertMap(Map source, Class targetClass) {
        if (targetClass.isInstance(source)) {
            return source;
        }
        if (targetClass.isInterface()) {
            if (targetClass == Map.class) {
                return new HashMap<>(source);
            }
            if (targetClass == ConcurrentMap.class) {
                return new ConcurrentHashMap<>(source);
            }
            if (SortedMap.class.isAssignableFrom(targetClass)) {
                return new TreeMap<>(source);
            }
        } else {
            if (targetClass == HashMap.class) {
                return new HashMap(source);
            }
            if (targetClass == LinkedHashMap.class) {
                return new LinkedHashMap(source);
            }
            if (targetClass == TreeMap.class) {
                return new TreeMap(source);
            }
            if (targetClass == ConcurrentHashMap.class) {
                return new ConcurrentHashMap(source);
            }
            if (Modifier.isAbstract(targetClass.getModifiers())) {
                Map map = customCreateMap(targetClass, source.size());
                if (map != null) {
                    map.putAll(source);
                    return map;
                }
                if (targetClass.isAssignableFrom(LinkedHashMap.class)) {
                    return new LinkedHashMap(source);
                }
                return null;
            }
            try {
                Constructor defCt = null;
                for (Constructor ct : targetClass.getConstructors()) {
                    switch (ct.getParameterCount()) {
                        case 0:
                            defCt = ct;
                            break;
                        case 1:
                            Class paramType = ct.getParameterTypes()[0];
                            if (paramType == Map.class) {
                                return (Map) ct.newInstance(source);
                            }
                            break;
                        default:
                    }
                }
                if (defCt != null) {
                    Map map = (Map) defCt.newInstance();
                    map.putAll(source);
                    return map;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Object emptyDefault(Class targetClass) {
        if (targetClass == null) {
            return null;
        }
        if (targetClass.isPrimitive()) {
            return nullDefault(targetClass);
        }
        if (targetClass == Optional.class) {
            return Optional.empty();
        }
        if (List.class.isAssignableFrom(targetClass)) {
            return targetClass == List.class ? Collections.EMPTY_LIST : createCollection(targetClass, 0);
        }
        if (Set.class.isAssignableFrom(targetClass)) {
            return targetClass == Set.class ? Collections.EMPTY_SET : createCollection(targetClass, 0);
        }
        if (Map.class.isAssignableFrom(targetClass)) {
            return targetClass == Map.class ? Collections.EMPTY_MAP : createMap(targetClass, 0);
        }
        if (targetClass.isArray()) {
            return Array.newInstance(targetClass.getComponentType(), 0);
        }
        return null;
    }

    private static Map<String, String> tokenizeToMap(String str) {
        if (StringUtils.isEmpty(str)) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String item : tokenizeToList(str, ';')) {
            int index = item.indexOf('=');
            if (index == -1) {
                result.put(item, null);
            } else {
                result.put(
                        item.substring(0, index).trim(),
                        RequestUtils.decodeURL(item.substring(index + 1).trim()));
            }
        }
        return result;
    }

    private static boolean isMaybeJSONObjectOrArray(String str) {
        if (str == null) {
            return false;
        }
        int i = 0, n = str.length();
        if (n < 3) {
            return false;
        }
        char expected = 0;
        for (; i < n; i++) {
            char c = str.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (c == '{') {
                expected = '}';
                break;
            }
            if (c == '[') {
                expected = ']';
                break;
            }
            return false;
        }
        for (int j = n - 1; j > i; j--) {
            char c = str.charAt(j);
            if (Character.isWhitespace(c)) {
                continue;
            }
            return c == expected;
        }
        return false;
    }

    private Object jsonToObject(String value, Type targetType) {
        if (isMaybeJSONObjectOrArray(value)) {
            try {
                return targetType instanceof Class
                        ? codecUtils
                                .determineHttpMessageDecoder(MediaType.APPLICATION_JSON.getName())
                                .decode(new ByteArrayInputStream(value.getBytes(UTF_8)), (Class<?>) targetType)
                        : httpJsonUtils.toJavaObject(value, targetType);
            } catch (Throwable t) {
                LOGGER.debug("Failed to parse value '{}' from json string '{}'", targetType, value, t);
            }
        }
        return null;
    }

    private static boolean isHexNumber(String value) {
        if (value.length() < 3) {
            return false;
        }
        int index = value.indexOf('-') == 0 ? 1 : 0;
        char c0 = value.charAt(index);
        if (c0 == '0') {
            char c1 = value.charAt(index + 1);
            return c1 == 'x' || c1 == 'X';
        }
        return c0 == '#';
    }

    private static Boolean toBoolean(Number n) {
        Class<?> type = n.getClass();
        if (type == Double.class) {
            return n.doubleValue() != 0.0D;
        }
        if (type == Float.class) {
            return n.floatValue() != 0.0F;
        }
        if (type == BigDecimal.class) {
            return ((BigDecimal) n).compareTo(BigDecimal.ZERO) != 0;
        }
        return n.intValue() != 0;
    }

    private static Boolean toBoolean(String str) {
        if (str == null) {
            return null;
        }
        switch (str.length()) {
            case 1:
                char c = str.charAt(0);
                if (c == 'y' || c == 'Y' || c == 't' || c == 'T' || c == '1') {
                    return Boolean.TRUE;
                }
                if (c == 'n' || c == 'N' || c == 'f' || c == 'F' || c == '0') {
                    return Boolean.FALSE;
                }
                break;
            case 2:
                if ("on".equalsIgnoreCase(str)) {
                    return Boolean.TRUE;
                }
                if ("no".equalsIgnoreCase(str)) {
                    return Boolean.FALSE;
                }
                break;
            case 3:
                if ("yes".equalsIgnoreCase(str)) {
                    return Boolean.TRUE;
                }
                if ("off".equalsIgnoreCase(str)) {
                    return Boolean.FALSE;
                }
                break;
            case 4:
                if ("true".equalsIgnoreCase(str)) {
                    return Boolean.TRUE;
                }
                break;
            case 5:
                if ("false".equalsIgnoreCase(str)) {
                    return Boolean.FALSE;
                }
                break;
            default:
        }
        return null;
    }

    private static byte[] toBytes(Number n) {
        ByteBuffer buffer;
        if (n instanceof Long || n instanceof AtomicLong) {
            buffer = ByteBuffer.allocate(8);
            buffer.putLong(n.longValue());
        } else if (n instanceof Integer || n instanceof AtomicInteger) {
            buffer = ByteBuffer.allocate(4);
            buffer.putInt(n.intValue());
        } else if (n instanceof Double) {
            buffer = ByteBuffer.allocate(8);
            buffer.putDouble(n.doubleValue());
        } else if (n instanceof Float) {
            buffer = ByteBuffer.allocate(4);
            buffer.putFloat(n.floatValue());
        } else if (n instanceof Short) {
            buffer = ByteBuffer.allocate(2);
            buffer.putShort(n.shortValue());
        } else if (n instanceof Byte) {
            return new byte[] {n.byteValue()};
        } else if (n instanceof BigInteger) {
            return ((BigInteger) n).toByteArray();
        } else {
            return null;
        }
        return buffer.array();
    }

    private static ZonedDateTime toZonedDateTime(String str) {
        return DateUtils.parse(str).toInstant().atZone(ZoneId.systemDefault());
    }

    private static ZonedDateTime toZonedDateTime(Number num) {
        return Instant.ofEpochMilli(num.longValue()).atZone(ZoneId.systemDefault());
    }

    private static TimeZone toTimeZone(int offset) {
        if (offset < -12 || offset > 12) {
            throw new RestParameterException("Invalid timeZone offset " + offset);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("GMT");
        if (offset >= 0) {
            sb.append('+');
            if (offset < 10) {
                sb.append('0');
            }
        } else {
            sb.append('-');
            if (offset > -10) {
                sb.append('0');
            }
        }
        sb.append(offset).append(":00");
        return TimeZone.getTimeZone(sb.toString());
    }

    private static List toList(Iterable source) {
        List list = new ArrayList(32);
        for (Object item : source) {
            list.add(item);
        }
        return list;
    }

    private static List toList(Collection source) {
        if (source instanceof List) {
            return (List) source;
        }
        List list = new ArrayList(source.size());
        list.addAll(source);
        return list;
    }

    private static List arrayToList(Object source) {
        int len = Array.getLength(source);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            array[i] = Array.get(source, i);
        }
        return Arrays.asList(array);
    }

    private static Collection toCollection(Object source) {
        if (source instanceof Collection) {
            return (Collection) source;
        }
        if (source.getClass().isArray()) {
            return arrayToList(source);
        }
        if (source instanceof Iterable) {
            return toList((Iterable) source);
        }
        if (source instanceof CharSequence) {
            return tokenizeToList(source.toString());
        }
        return Collections.singletonList(source);
    }
}
