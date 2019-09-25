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

import org.apache.dubbo.common.annotations.GenericFeature;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * PojoUtils. Travel object deeply, and convert complex type to simple type.
 * <p/>
 * Simple type below will be remained:
 * <ul>
 * <li> Primitive Type, also include <b>String</b>, <b>Number</b>(Integer, Long), <b>Date</b>
 * <li> Array of Primitive Type
 * <li> Collection, eg: List, Map, Set etc.
 * </ul>
 * <p/>
 * Other type will be covert to a map which contains the attributes and value pair of object.
 */
public class PojoUtils {

    private static final Logger logger = LoggerFactory.getLogger(PojoUtils.class);
    private static final ConcurrentMap<String, Method> NAME_METHODS_CACHE = new ConcurrentHashMap<String, Method>();
    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, Field>> CLASS_FIELD_CACHE = new ConcurrentHashMap<Class<?>, ConcurrentMap<String, Field>>();

    public static Object[] generalize(Object[] objs) {
        Object[] dests = new Object[objs.length];
        for (int i = 0; i < objs.length; i++) {
            dests[i] = generalize(objs[i]);
        }
        return dests;
    }

    public static Object[] realize(Object[] objs, Class<?>[] types) {
        if (objs.length != types.length) {
            throw new IllegalArgumentException("args.length != types.length");
        }

        Object[] dests = new Object[objs.length];
        for (int i = 0; i < objs.length; i++) {
            dests[i] = realize(objs[i], types[i]);
        }

        return dests;
    }

    public static Object[] realize(Object[] objs, Class<?>[] types, Type[] gtypes) {
        if (objs.length != types.length || objs.length != gtypes.length) {
            throw new IllegalArgumentException("args.length != types.length");
        }
        Object[] dests = new Object[objs.length];
        for (int i = 0; i < objs.length; i++) {
            dests[i] = realize(objs[i], types[i], gtypes[i]);
        }
        return dests;
    }

    public static Object generalize(Object pojo) {
        return generalize(pojo, new IdentityHashMap<Object, Object>());
    }

    @SuppressWarnings("unchecked")
    private static Object generalize(Object pojo, Map<Object, Object> history) {
        if (pojo == null) {
            return null;
        }

        if (pojo instanceof Enum<?>) {
            return ((Enum<?>) pojo).name();
        }
        if (pojo.getClass().isArray() && Enum.class.isAssignableFrom(pojo.getClass().getComponentType())) {
            int len = Array.getLength(pojo);
            String[] values = new String[len];
            for (int i = 0; i < len; i++) {
                values[i] = ((Enum<?>) Array.get(pojo, i)).name();
            }
            return values;
        }

        if (ReflectUtils.isPrimitives(pojo.getClass())) {
            return pojo;
        }

        if (pojo instanceof Class) {
            return ((Class) pojo).getName();
        }

        Object o = history.get(pojo);
        if (o != null) {
            return o;
        }
        history.put(pojo, pojo);

        if (pojo.getClass().isArray()) {
            int len = Array.getLength(pojo);
            Object[] dest = new Object[len];
            history.put(pojo, dest);
            for (int i = 0; i < len; i++) {
                Object obj = Array.get(pojo, i);
                dest[i] = generalize(obj, history);
            }
            return dest;
        }
        if (pojo instanceof Collection<?>) {
            Collection<Object> src = (Collection<Object>) pojo;
            int len = src.size();
            Collection<Object> dest = (pojo instanceof List<?>) ? new ArrayList<Object>(len) : new HashSet<Object>(len);
            history.put(pojo, dest);
            for (Object obj : src) {
                dest.add(generalize(obj, history));
            }
            return dest;
        }
        if (pojo instanceof Map<?, ?>) {
            Map<Object, Object> src = (Map<Object, Object>) pojo;
            Map<Object, Object> dest = createMap(src);
            history.put(pojo, dest);
            for (Map.Entry<Object, Object> obj : src.entrySet()) {
                dest.put(generalize(obj.getKey(), history), generalize(obj.getValue(), history));
            }
            return dest;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        history.put(pojo, map);
        map.put("class", pojo.getClass().getName());
        Set<String> annoFieldSet = new HashSet<>();
        List<Field> allFieldList = getClassAllFields(pojo.getClass());
        for (Field field : allFieldList) {
            GenericFeature genericFeature = field.getAnnotation(GenericFeature.class);
            if (ReflectUtils.isSpecialInstanceField(field) &&  genericFeature != null) {
                String fieldAliasName = genericFeature.alias();
                if (genericFeature.ignore()) {
                    annoFieldSet.add(field.getName());
                    annoFieldSet.add(fieldAliasName);
                    continue;
                }
                try {

                    if (history.containsKey(pojo)) {
                        Object pojoGeneralizedValue = history.get(pojo);
                        if (pojoGeneralizedValue instanceof Map
                                && ((Map) pojoGeneralizedValue).containsKey(fieldAliasName)) {
                            continue;
                        }
                    }
                    Object rawValue = forceGetFieldValue(field,pojo);
                    if(isNullDateAsZero(field.getType(),genericFeature,rawValue)){
                        annoFieldSet.add(field.getName());
                        annoFieldSet.add(fieldAliasName);
                        map.put(fieldAliasName, isNullDateAsZero(field.getType(),genericFeature.dateFormatter()));
                        continue;
                    }
                    if(genericFeature.nullNotGeneralize() && rawValue == null) {
                        annoFieldSet.add(field.getName());
                        annoFieldSet.add(fieldAliasName);
                        continue;
                    }
                    Object fieldValue = toValueType(rawValue,field.getType(),genericFeature);
                    annoFieldSet.add(field.getName());
                    annoFieldSet.add(fieldAliasName);
                    map.put(fieldAliasName, generalize(fieldValue, history));
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
        Set<String> methodNameSet = new HashSet<>();
        for (Method method : pojo.getClass().getMethods()) {
            if (ReflectUtils.isBeanPropertyReadMethod(method)) {
                try {
                    GenericFeature genericFeature = method.getAnnotation(GenericFeature.class);
                    String methodAlias = genericFeature==null?null:genericFeature.alias();
                    String propertyName = ReflectUtils.getPropertyNameFromBeanReadMethod(method);
                    String uniqName = methodAlias == null?propertyName : methodAlias;
                    if(!annoFieldSet.contains(propertyName) && (methodAlias == null || !annoFieldSet.contains(methodAlias))) {
                        if(genericFeature != null && genericFeature.ignore()) {
                            methodNameSet.add(propertyName);
                            continue;
                        }
                        Object rawMethodValue = method.invoke(pojo);
                        if (isNullDateAsZero(method.getReturnType(), genericFeature, rawMethodValue)) {
                            methodNameSet.add(propertyName);
                            map.put(uniqName, isNullDateAsZero(method.getReturnType(), genericFeature.dateFormatter()));
                            continue;
                        }
                        if(genericFeature != null && genericFeature.nullNotGeneralize() && rawMethodValue == null) {
                            methodNameSet.add(propertyName);
                            continue;
                        }
                        Object mVal = toValueType(rawMethodValue,method.getReturnType(),genericFeature);
                        map.put(uniqName, generalize(mVal, history));
                        methodNameSet.add(propertyName);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
        // public field
        for (Field field : pojo.getClass().getFields()) {
            if (ReflectUtils.isPublicInstanceField(field)) {
                if(methodNameSet.contains(field.getName()) || annoFieldSet.contains(field.getName())) {
                    continue;
                }
                try {
                    Object fieldValue = field.get(pojo);
                    if (history.containsKey(pojo)) {
                        Object pojoGeneralizedValue = history.get(pojo);
                        if (pojoGeneralizedValue instanceof Map
                                && ((Map) pojoGeneralizedValue).containsKey(field.getName())) {
                            continue;
                        }
                    }
                    if (fieldValue != null) {
                        map.put(field.getName(), generalize(fieldValue, history));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
        return map;
    }
    private static boolean isNullDateAsZero(Class type,GenericFeature genericFeature, Object value) {
        if (value != null || genericFeature == null || !genericFeature.nullDateAsZeroGeneralize()
                || genericFeature.dateFormatter().length() == 0) {
            return false;
        }
        if (type.equals(Date.class)) {
            return true;
        }
        if (type.equals(LocalDateTime.class)) {
            return true;
        }
        if (type.equals(LocalDate.class)) {
            return true;
        }
        return false;
    }
    private static String isNullDateAsZero(Class type, String formatter) {
        String dateZero = null;
        if (type.equals(Date.class)) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(1971, 0, 2, 3, 4, 5);
            SimpleDateFormat dateFormat = new SimpleDateFormat(formatter);
            dateZero = dateFormat.format(calendar.getTime());
        }
        if (type.equals(LocalDateTime.class)) {
            LocalDateTime localDateTime = LocalDateTime.of(1971, 1, 2, 3, 4, 5);
            dateZero = localDateTime.format(DateTimeFormatter.ofPattern(formatter));
        }
        if (type.equals(LocalDate.class)) {
            LocalDate localDate = LocalDate.of(1971, 1, 2);
            dateZero = localDate.format(DateTimeFormatter.ofPattern(formatter));
        }
        if (dateZero == null) {
            throw new RuntimeException(type.getName() + " is not support");
        }
        dateZero = dateZero.replace("1971", "0000")
                .replace("01", "00")
                .replace("02", "00")
                .replace("03", "00")
                .replace("04", "00")
                .replace("05", "00");
        return dateZero;
    }
    private static Object toValueType(Object val, Class type, GenericFeature genericFeature) {
        if(genericFeature == null){
            return val;
        }

        if((type == Boolean.class || type == boolean.class) && genericFeature.numberAsString()) {
            return val == null?"":val.toString();
        }

        if((type == Byte.class || type == byte.class) && genericFeature.numberAsString()) {
            return val == null?"":val.toString();
        }

        if((type == Short.class || type == short.class) && genericFeature.numberAsString()) {
            return val == null?"":val.toString();
        }
        if((type == Integer.class || type == int.class) && genericFeature.numberAsString()) {
            return val == null?"":val.toString();
        }
        if((type == Long.class || type == long.class) && genericFeature.numberAsString()) {
            return val == null?"":val.toString();
        }

        if((type == Float.class || type == float.class) && genericFeature.numberAsString()) {
            return val == null?"":val.toString();
        }

        if((type == Double.class || type == double.class) && genericFeature.numberAsString()) {
            return val == null?"":val.toString();
        }

        if(type == BigDecimal.class && genericFeature.numberAsString()) {
            return val == null?"":((BigDecimal)val).toPlainString();
        }

        if(type == BigInteger.class && genericFeature.numberAsString()) {
            return val == null?"":val.toString();
        }



        if(type == LocalDateTime.class  && genericFeature.dateFormatter().length() > 0) {
            LocalDateTime dateTime = (LocalDateTime)val;
            return dateTime==null?"":dateTime.format(DateTimeFormatter.ofPattern(genericFeature.dateFormatter()));
        }

        if(type == LocalDate.class  && genericFeature.dateFormatter().length() > 0) {
            LocalDate date = (LocalDate)val;
            return date==null?"":date.format(DateTimeFormatter.ofPattern(genericFeature.dateFormatter()));
        }
        if(type == Date.class  && genericFeature.dateFormatter().length() > 0) {
            Date date = (Date) val;
            SimpleDateFormat format = new SimpleDateFormat(genericFeature.dateFormatter());
            return date==null?"":format.format(date);
        }
        return val;
    }

    private static Object fromValueType(Object val, Class type, GenericFeature genericFeature) {
        if(genericFeature == null || val.getClass() != String.class){
            return val;
        }
        String strValue = (String)val;
        boolean isEmpty = strValue == null || strValue.length() == 0;

        if (type == boolean.class && genericFeature.numberAsString()) {
            return isEmpty ? false : Boolean.parseBoolean(strValue);
        }

        if (type == Boolean.class && genericFeature.numberAsString()) {
            return isEmpty ? null : Boolean.parseBoolean(strValue);
        }

        if(type == byte.class && genericFeature.numberAsString()) {
            return isEmpty?0:Byte.parseByte(strValue);
        }

        if(type == Byte.class && genericFeature.numberAsString()) {
            return isEmpty?null:Byte.parseByte(strValue);
        }

        if(type == short.class && genericFeature.numberAsString()) {
            return isEmpty?0:Short.parseShort(strValue);
        }

        if(type == Short.class && genericFeature.numberAsString()) {
            return isEmpty?null:Short.parseShort(strValue);
        }


        if(type == int.class && genericFeature.numberAsString()) {
            return isEmpty?0:Integer.parseInt(strValue);
        }

        if(type == Integer.class && genericFeature.numberAsString()) {
            return isEmpty?null:Integer.parseInt(strValue);
        }

        if(type == long.class && genericFeature.numberAsString()) {
            return isEmpty?0L:Long.parseLong(strValue);
        }
        if(type == Long.class && genericFeature.numberAsString()) {
            return isEmpty?null:Long.parseLong(strValue);
        }

        if(type == float.class && genericFeature.numberAsString()) {
            return isEmpty?0.0F:Float.parseFloat(strValue);
        }

        if(type == Float.class && genericFeature.numberAsString()) {
            return isEmpty?null:Float.parseFloat(strValue);
        }

        if(type == double.class && genericFeature.numberAsString()) {
            return isEmpty?0.0D:Double.parseDouble(strValue);
        }

        if(type == Double.class && genericFeature.numberAsString()) {
            return isEmpty?null:Double.parseDouble(strValue);
        }

        if(type == BigInteger.class && genericFeature.numberAsString()) {
            return isEmpty?null: new BigInteger(strValue);
        }

        if(type == BigDecimal.class && genericFeature.numberAsString()) {
            return isEmpty?null: new BigDecimal(strValue);
        }



        if(type == LocalDateTime.class  && genericFeature.dateFormatter().length() > 0) {
            return isEmpty?null:LocalDateTime.parse(strValue,DateTimeFormatter.ofPattern(genericFeature.dateFormatter()));
        }

        if(type == LocalDate.class  && genericFeature.dateFormatter().length() > 0) {
            return isEmpty?null:LocalDate.parse(strValue,DateTimeFormatter.ofPattern(genericFeature.dateFormatter()));
        }
        if(type == Date.class  && genericFeature.dateFormatter().length() > 0) {
            SimpleDateFormat format = new SimpleDateFormat(genericFeature.dateFormatter());
            try {
                return isEmpty?null:format.parse(strValue);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return val;
    }

    private static Object forceGetFieldValue(Field field,Object pojo) throws IllegalAccessException {
        boolean isPublic = Modifier.isPublic(field.getModifiers());
        if(!isPublic) {
            field.setAccessible(true);
        }
        try{
            return   field.get(pojo);
        } finally {
            if(!isPublic) {
                field.setAccessible(false);
            }
        }
    }

    public static Object realize(Object pojo, Class<?> type) {
        return realize0(pojo, type, null, new IdentityHashMap<Object, Object>());
    }

    public static Object realize(Object pojo, Class<?> type, Type genericType) {
        return realize0(pojo, type, genericType, new IdentityHashMap<Object, Object>());
    }

    private static class PojoInvocationHandler implements InvocationHandler {

        private Map<Object, Object> map;

        public PojoInvocationHandler(Map<Object, Object> map) {
            this.map = map;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(map, args);
            }
            String methodName = method.getName();
            Object value = null;
            if (methodName.length() > 3 && methodName.startsWith("get")) {
                value = map.get(methodName.substring(3, 4).toLowerCase() + methodName.substring(4));
            } else if (methodName.length() > 2 && methodName.startsWith("is")) {
                value = map.get(methodName.substring(2, 3).toLowerCase() + methodName.substring(3));
            } else {
                value = map.get(methodName.substring(0, 1).toLowerCase() + methodName.substring(1));
            }
            if (value instanceof Map<?, ?> && !Map.class.isAssignableFrom(method.getReturnType())) {
                value = realize0((Map<String, Object>) value, method.getReturnType(), null, new IdentityHashMap<Object, Object>());
            }
            return value;
        }
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> createCollection(Class<?> type, int len) {
        if (type.isAssignableFrom(ArrayList.class)) {
            return new ArrayList<Object>(len);
        }
        if (type.isAssignableFrom(HashSet.class)) {
            return new HashSet<Object>(len);
        }
        if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
            try {
                return (Collection<Object>) type.newInstance();
            } catch (Exception e) {
                // ignore
            }
        }
        return new ArrayList<Object>();
    }

    private static Map createMap(Map src) {
        Class<? extends Map> cl = src.getClass();
        Map result = null;
        if (HashMap.class == cl) {
            result = new HashMap();
        } else if (Hashtable.class == cl) {
            result = new Hashtable();
        } else if (IdentityHashMap.class == cl) {
            result = new IdentityHashMap();
        } else if (LinkedHashMap.class == cl) {
            result = new LinkedHashMap();
        } else if (Properties.class == cl) {
            result = new Properties();
        } else if (TreeMap.class == cl) {
            result = new TreeMap();
        } else if (WeakHashMap.class == cl) {
            return new WeakHashMap();
        } else if (ConcurrentHashMap.class == cl) {
            result = new ConcurrentHashMap();
        } else if (ConcurrentSkipListMap.class == cl) {
            result = new ConcurrentSkipListMap();
        } else {
            try {
                result = cl.newInstance();
            } catch (Exception e) { /* ignore */ }

            if (result == null) {
                try {
                    Constructor<?> constructor = cl.getConstructor(Map.class);
                    result = (Map) constructor.newInstance(Collections.EMPTY_MAP);
                } catch (Exception e) { /* ignore */ }
            }
        }

        if (result == null) {
            result = new HashMap<Object, Object>();
        }

        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object realize0(Object pojo, Class<?> type, Type genericType, final Map<Object, Object> history) {
        if (pojo == null) {
            return null;
        }

        if (type != null && type.isEnum() && pojo.getClass() == String.class) {
            return Enum.valueOf((Class<Enum>) type, (String) pojo);
        }

        if (ReflectUtils.isPrimitives(pojo.getClass())
                && !(type != null && type.isArray()
                && type.getComponentType().isEnum()
                && pojo.getClass() == String[].class)) {
            return CompatibleTypeUtils.compatibleTypeConvert(pojo, type);
        }

        Object o = history.get(pojo);

        if (o != null) {
            return o;
        }

        history.put(pojo, pojo);

        if (pojo.getClass().isArray()) {
            if (Collection.class.isAssignableFrom(type)) {
                Class<?> ctype = pojo.getClass().getComponentType();
                int len = Array.getLength(pojo);
                Collection dest = createCollection(type, len);
                history.put(pojo, dest);
                for (int i = 0; i < len; i++) {
                    Object obj = Array.get(pojo, i);
                    Object value = realize0(obj, ctype, null, history);
                    dest.add(value);
                }
                return dest;
            } else {
                Class<?> ctype = (type != null && type.isArray() ? type.getComponentType() : pojo.getClass().getComponentType());
                int len = Array.getLength(pojo);
                Object dest = Array.newInstance(ctype, len);
                history.put(pojo, dest);
                for (int i = 0; i < len; i++) {
                    Object obj = Array.get(pojo, i);
                    Object value = realize0(obj, ctype, null, history);
                    Array.set(dest, i, value);
                }
                return dest;
            }
        }

        if (pojo instanceof Collection<?>) {
            if (type.isArray()) {
                Class<?> ctype = type.getComponentType();
                Collection<Object> src = (Collection<Object>) pojo;
                int len = src.size();
                Object dest = Array.newInstance(ctype, len);
                history.put(pojo, dest);
                int i = 0;
                for (Object obj : src) {
                    Object value = realize0(obj, ctype, null, history);
                    Array.set(dest, i, value);
                    i++;
                }
                return dest;
            } else {
                Collection<Object> src = (Collection<Object>) pojo;
                int len = src.size();
                Collection<Object> dest = createCollection(type, len);
                history.put(pojo, dest);
                for (Object obj : src) {
                    Type keyType = getGenericClassByIndex(genericType, 0);
                    Class<?> keyClazz = obj == null ? null : obj.getClass();
                    if (keyType instanceof Class) {
                        keyClazz = (Class<?>) keyType;
                    }
                    Object value = realize0(obj, keyClazz, keyType, history);
                    dest.add(value);
                }
                return dest;
            }
        }

        if (pojo instanceof Map<?, ?> && type != null) {
            Object className = ((Map<Object, Object>) pojo).get("class");
            if (className instanceof String) {
                try {
                    type = ClassUtils.forName((String) className);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }

            // special logic for enum
            if (type.isEnum()) {
                Object name = ((Map<Object, Object>) pojo).get("name");
                if (name != null) {
                    return Enum.valueOf((Class<Enum>) type, name.toString());
                }
            }
            Map<Object, Object> map;
            // when return type is not the subclass of return type from the signature and not an interface
            if (!type.isInterface() && !type.isAssignableFrom(pojo.getClass())) {
                try {
                    map = (Map<Object, Object>) type.newInstance();
                    Map<Object, Object> mapPojo = (Map<Object, Object>) pojo;
                    map.putAll(mapPojo);
                    map.remove("class");
                } catch (Exception e) {
                    //ignore error
                    map = (Map<Object, Object>) pojo;
                }
            } else {
                map = (Map<Object, Object>) pojo;
            }

            if (Map.class.isAssignableFrom(type) || type == Object.class) {
                final Map<Object, Object> result = createMap(map);
                history.put(pojo, result);
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    Type keyType = getGenericClassByIndex(genericType, 0);
                    Type valueType = getGenericClassByIndex(genericType, 1);
                    Class<?> keyClazz;
                    if (keyType instanceof Class) {
                        keyClazz = (Class<?>) keyType;
                    } else if (keyType instanceof ParameterizedType) {
                        keyClazz = (Class<?>) ((ParameterizedType) keyType).getRawType();
                    } else {
                        keyClazz = entry.getKey() == null ? null : entry.getKey().getClass();
                    }
                    Class<?> valueClazz;
                    if (valueType instanceof Class) {
                        valueClazz = (Class<?>) valueType;
                    } else if (valueType instanceof ParameterizedType) {
                        valueClazz = (Class<?>) ((ParameterizedType) valueType).getRawType();
                    } else {
                        valueClazz = entry.getValue() == null ? null : entry.getValue().getClass();
                    }

                    Object key = keyClazz == null ? entry.getKey() : realize0(entry.getKey(), keyClazz, keyType, history);
                    Object value = valueClazz == null ? entry.getValue() : realize0(entry.getValue(), valueClazz, valueType, history);
                    result.put(key, value);
                }
                return result;
            } else if (type.isInterface()) {
                Object dest = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{type}, new PojoInvocationHandler(map));
                history.put(pojo, dest);
                return dest;
            } else {
                Object dest = newInstance(type);
                history.put(pojo, dest);
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    Object key = entry.getKey();
                    if (key instanceof String) {
                        String name = (String) key;
                        Object value = entry.getValue();
                        if (value != null) {
                            Method method = getSetterMethod(dest.getClass(), name, value.getClass());
                            Field field = getField(dest.getClass(), name);
                            if(realizeGenericFeatures(method,field,value,dest,name,history)) {
                                continue;
                            }
                            if (method != null) {
                                if (!method.isAccessible()) {
                                    method.setAccessible(true);
                                }
                                Type ptype = method.getGenericParameterTypes()[0];
                                value = realize0(value, method.getParameterTypes()[0], ptype, history);

                                try {
                                    method.invoke(dest, fromValueType(value, method.getParameterTypes()[0], null));
                                } catch (Exception e) {
                                    String exceptionDescription = "Failed to set pojo " + dest.getClass().getSimpleName() + " property " + name
                                            + " value " + value + "(" + value.getClass() + "), cause: " + e.getMessage();
                                    logger.error(exceptionDescription, e);
                                    throw new RuntimeException(exceptionDescription, e);
                                }
                            } else if (field != null) {
                                value = realize0(value, field.getType(), field.getGenericType(), history);
                                try {
                                    forceSetFieldValue(field, dest, fromValueType(value, field.getType(), null));
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException("Failed to set field " + name + " of pojo " + dest.getClass().getName() + " : " + e.getMessage(), e);
                                }
                            }
                        }
                    }
                }
                if (dest instanceof Throwable) {
                    Object message = map.get("message");
                    if (message instanceof String) {
                        try {
                            Field field = Throwable.class.getDeclaredField("detailMessage");
                            if (!field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            field.set(dest, message);
                        } catch (Exception e) {
                        }
                    }
                }
                return dest;
            }
        }
        return pojo;
    }

    private static boolean realizeGenericFeatures(Method method,Field field,Object value,Object dest ,String name,final Map<Object, Object> history ) {
        Object rawValue = value;
        if(field != null) {
            GenericFeature genericFeature = field.getAnnotation(GenericFeature.class);
            if (genericFeature != null && genericFeature.ignore()) {
                return true;
            }

            if(genericFeature != null) {
                if(genericFeature.nullNotRealize() && value == null) {
                    return true;
                }
                if(value instanceof String) {
                    value = fromValueType(value, field.getType(), genericFeature);
                }
                if(rawValue == value) {
                    value = realize0(value, field.getType(), field.getGenericType(), history);
                }

                try {
                    forceSetFieldValue(field, dest, value);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to set field " + name + " of pojo " + dest.getClass().getName() + " : " + e.getMessage(), e);
                }
                return true;
            }
        }
        if(method == null) {
            return false;
        }
        GenericFeature genericFeature = method.getAnnotation(GenericFeature.class);
        if(genericFeature == null) {
            return false;
        }
        if (genericFeature.ignore()) {
            return true;
        }

        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        if(genericFeature.nullNotRealize() && value == null) {
            return true;
        }
        if (value instanceof String) {
            value = fromValueType(value, method.getParameterTypes()[0], genericFeature);
        }
        if(rawValue == value) {
            Type ptype = method.getGenericParameterTypes()[0];
            value = realize0(value, method.getParameterTypes()[0], ptype, history);
        }

        try {
            method.invoke(dest, value);
        } catch (Exception e) {
            String exceptionDescription = "Failed to set pojo " + dest.getClass().getSimpleName() + " property " + name
                    + " value " + value + "(" + value.getClass() + "), cause: " + e.getMessage();
            logger.error(exceptionDescription, e);
            throw new RuntimeException(exceptionDescription, e);
        }
        return true;

    }

    private static void forceSetFieldValue(Field field,Object dest,Object value) throws IllegalAccessException {
        boolean isNotPublic = Modifier.isPublic(field.getModifiers());
        if(!isNotPublic) {
            field.setAccessible(true);
        }
        try{
            field.set(dest,value);
        } finally {
            if(!isNotPublic) {
                field.setAccessible(false);
            }
        }
    }

    /**
     * Get parameterized type
     *
     * @param genericType generic type
     * @param index       index of the target parameterized type
     * @return Return Person.class for List<Person>, return Person.class for Map<String, Person> when index=0
     */
    private static Type getGenericClassByIndex(Type genericType, int index) {
        Type clazz = null;
        // find parameterized type
        if (genericType instanceof ParameterizedType) {
            ParameterizedType t = (ParameterizedType) genericType;
            Type[] types = t.getActualTypeArguments();
            clazz = types[index];
        }
        return clazz;
    }

    private static Object newInstance(Class<?> cls) {
        try {
            return cls.newInstance();
        } catch (Throwable t) {
            try {
                Constructor<?>[] constructors = cls.getDeclaredConstructors();
                /**
                 * From Javadoc java.lang.Class#getDeclaredConstructors
                 * This method returns an array of Constructor objects reflecting all the constructors
                 * declared by the class represented by this Class object.
                 * This method returns an array of length 0,
                 * if this Class object represents an interface, a primitive type, an array class, or void.
                 */
                if (constructors.length == 0) {
                    throw new RuntimeException("Illegal constructor: " + cls.getName());
                }
                Constructor<?> constructor = constructors[0];
                if (constructor.getParameterTypes().length > 0) {
                    for (Constructor<?> c : constructors) {
                        if (c.getParameterTypes().length < constructor.getParameterTypes().length) {
                            constructor = c;
                            if (constructor.getParameterTypes().length == 0) {
                                break;
                            }
                        }
                    }
                }
                constructor.setAccessible(true);
                Object[] parameters = Arrays.stream(constructor.getParameterTypes()).map(PojoUtils::getDefaultValue).toArray();
                return constructor.newInstance(parameters);
            } catch (InstantiationException e) {
                throw new RuntimeException(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e.getMessage(), e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    /**
     * return init value
     * @param parameterType
     * @return
     */
    private static Object getDefaultValue(Class<?> parameterType) {
        if (parameterType.getName().equals("char")) {
            return Character.MIN_VALUE;
        }
        if (parameterType.getName().equals("bool")) {
            return false;
        }
        return parameterType.isPrimitive() ? 0 : null;
    }

    private static Method getSetterMethod(Class<?> cls, String property, Class<?> valueCls) {
        String name = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
        Method method = NAME_METHODS_CACHE.get(cls.getName() + "." + name + "(" + valueCls.getName() + ")");
        if (method == null) {
                for (Method m : cls.getMethods()) {

                    GenericFeature genericFeature = m.getAnnotation(GenericFeature.class);
                    if(genericFeature != null && genericFeature.alias().equals(name)) {
                        method = m;
                        break;
                    }
                if (ReflectUtils.isBeanPropertyWriteMethod(m) && m.getName().equals(name)) {
                    method = m;
                }
            }
            if (method != null) {
                NAME_METHODS_CACHE.put(cls.getName() + "." + name + "(" + valueCls.getName() + ")", method);
            }
        }
        return method;
    }

    private static Field getField(Class<?> cls, String fieldName) {
        Field result = null;
        if (CLASS_FIELD_CACHE.containsKey(cls) && CLASS_FIELD_CACHE.get(cls).containsKey(fieldName)) {
            return CLASS_FIELD_CACHE.get(cls).get(fieldName);
        }
        List<Field> fieldList = getClassAllFields(cls);
        for (Field field : fieldList) {
            GenericFeature genericFeature = field.getAnnotation(GenericFeature.class);
            if(genericFeature != null && genericFeature.alias().equals(fieldName)) {
                    result = field;
                    break;
                }

            if (fieldName.equals(field.getName())) {
                    result = field;
                    break;
            }
        }
        if (result != null) {
            ConcurrentMap<String, Field> fields = CLASS_FIELD_CACHE.get(cls);
            if (fields == null) {
                fields = new ConcurrentHashMap<String, Field>();
                CLASS_FIELD_CACHE.putIfAbsent(cls, fields);
            }
            fields = CLASS_FIELD_CACHE.get(cls);
            fields.putIfAbsent(fieldName, result);
        }
        return result;
    }

    private static List<Field> getClassAllFields(Class cls) {
        List<Field> allFields = new ArrayList<Field>();
        Class<?> currentClass = cls;
        while (currentClass != null) {
            final Field[] declaredFields = currentClass.getDeclaredFields();
            for (Field f : declaredFields) {
                if(Modifier.isStatic(f.getModifiers()) || Modifier.isNative(f.getModifiers())) {
                    continue;
                }
                allFields.add(f);
            }
            currentClass = currentClass.getSuperclass();
        }
        return allFields;
    }

    public static boolean isPojo(Class<?> cls) {
        return !ReflectUtils.isPrimitives(cls)
                && !Collection.class.isAssignableFrom(cls)
                && !Map.class.isAssignableFrom(cls);
    }

}
