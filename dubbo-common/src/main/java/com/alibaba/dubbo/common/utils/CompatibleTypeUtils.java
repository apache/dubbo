/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author ding.lid
 */
public class CompatibleTypeUtils {
    private CompatibleTypeUtils() {
    }

    private static final List<Class<?>> INETGER_LIKE_TYPES;
    private static final List<Class<?>> FLOAT_LIKE_TYPES;
    static {
        List<Class<?>> list = new ArrayList<Class<?>>();
        list.add(byte.class);
        list.add(Byte.class);
        list.add(short.class);
        list.add(Short.class);
        list.add(int.class);
        list.add(Integer.class);
        list.add(long.class);
        list.add(Long.class);

        INETGER_LIKE_TYPES = Collections.unmodifiableList(list);

        list = new ArrayList<Class<?>>();
        list.add(float.class);
        list.add(Float.class);
        list.add(double.class);
        list.add(Double.class);

        FLOAT_LIKE_TYPES = Collections.unmodifiableList(list);
    };

    public static boolean isIntegerLikeType(Class<?> clazz) {
        return INETGER_LIKE_TYPES.contains(clazz);
    }

    public static boolean isFloatLikeType(Class<?> clazz) {
        return FLOAT_LIKE_TYPES.contains(clazz);
    }

    public static Object convertIntegerLikeType(Object value, Class<?> clazz) {
        if(value == null || clazz == null || value.getClass().equals(clazz)) return value;
        
        Class<?> valueClass = value.getClass();
        if (!INETGER_LIKE_TYPES.contains(valueClass)) {
            String msg = String.format("Input value(type: %s) is not a integer like type!", valueClass);
            throw new IllegalArgumentException(msg);
        }
        if (!INETGER_LIKE_TYPES.contains(clazz)) {
            String msg = String.format("Destination type(%s) is not a integer like type!", clazz);
            throw new IllegalArgumentException(msg);
        }

        Number n = (Number) value;
        if (clazz.equals(INETGER_LIKE_TYPES.get(0))
                || clazz.equals(INETGER_LIKE_TYPES.get(1))) {
            if(n.longValue() > Byte.MAX_VALUE || n.longValue() < Byte.MIN_VALUE ) {
                throw new IllegalStateException("Overflow/Underflow when convert value to compatible type byte!");
            }
            return n.byteValue();
        } else if (clazz.equals(INETGER_LIKE_TYPES.get(2))
                || clazz.equals(INETGER_LIKE_TYPES.get(3))) {
            if(n.longValue() > Short.MAX_VALUE || n.longValue() < Short.MIN_VALUE ) {
                throw new IllegalStateException("Overflow/Underflow when convert value to compatible type short!");
            }
            return n.shortValue();
        } else if (clazz.equals(INETGER_LIKE_TYPES.get(4))
                || clazz.equals(INETGER_LIKE_TYPES.get(5))) {
            if(n.longValue() > Integer.MAX_VALUE || n.longValue() < Integer.MIN_VALUE ) {
                throw new IllegalStateException("Overflow/Underflow when convert value to compatible type int!");
            }
            return n.intValue();
        }
        return n.longValue();
    }

    public static Object convertFloatLikeType(Object value, Class<?> clazz) {
        if(value ==null || clazz == null || value.getClass().equals(clazz)) return value;
        
        Class<?> valueClass = value.getClass();
        if (!FLOAT_LIKE_TYPES.contains(valueClass)) {
            String msg = String.format("Input value(type: %s) is not a float like type!", valueClass);
            throw new IllegalArgumentException(msg);
        }
        if (!FLOAT_LIKE_TYPES.contains(clazz)) {
            String msg = String.format("Destination type(%s) is not a float like type!", clazz);
            throw new IllegalArgumentException(msg);
        }

        Number n = (Number) value;
        if (clazz.equals(FLOAT_LIKE_TYPES.get(0))
                || clazz.equals(FLOAT_LIKE_TYPES.get(1))) {
            return n.floatValue();
        }

        return n.doubleValue();
    }
    
    public static boolean needCompatibleTypeConvert(Object value, Class<?> destinationType) {
        return value != null && destinationType != null && ! value.getClass().equals(destinationType);
    }
    
    public static boolean isCharType(Class<?> clazz) {
        return char.class.equals(clazz) || Character.class.equals(clazz);
    }
    
    public static Character covert2Char(String value) {
        String s = (String) value;        
        if(s.length() != 1) {
            String msg = String.format("CAN NOT convert String(%s) to char!" +
                    " when convert String to char, the String MUST only 1 char.", s);
            throw new IllegalArgumentException(msg);
        }
        return s.charAt(0);
    }
    
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 兼容类型转换。null值是OK的。如果不需要转换，则返回原来的值。
     * 进行的兼容类型转换如下：（基本类对应的Wrapper类型不再列出。）
     * <ul>
     * <li> String -> char, enum, Date
     * <li> byte, short, int, long -> byte, short, int, long
     * <li> float, double -> float, double
     * </ul>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object compatibleTypeConvert(Object value, Class<?> destinationType) {
        if(! needCompatibleTypeConvert(value, destinationType)) {
        	return value;
        }
        if (isCharType(destinationType) && value instanceof String) {
            return CompatibleTypeUtils.covert2Char((String) value);
        } else if(destinationType.isEnum() && value instanceof String) {
            return Enum.valueOf((Class<Enum>)destinationType, (String) value);
        } else if(destinationType == Date.class && value instanceof String) {
            try {
				return new SimpleDateFormat(DATE_FORMAT).parse((String) value);
			} catch (ParseException e) {
				throw new IllegalStateException("Failed to parse date " + value + " by format " + DATE_FORMAT + ", cause: " + e.getMessage(), e);
			}
        } else if(CompatibleTypeUtils.isIntegerLikeType(destinationType)) {
            return CompatibleTypeUtils.convertIntegerLikeType(value, destinationType);
        } else if(CompatibleTypeUtils.isFloatLikeType(destinationType)) {
            return CompatibleTypeUtils.convertFloatLikeType(value, destinationType);
        }
        return value;
    }
}