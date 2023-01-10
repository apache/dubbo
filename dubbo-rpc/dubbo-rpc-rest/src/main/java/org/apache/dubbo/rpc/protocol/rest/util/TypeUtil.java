package org.apache.dubbo.rpc.protocol.rest.util;

public class TypeUtil {

    public static boolean isNumber(Class clazz) {
        return Number.class.isAssignableFrom(clazz);
    }

    public static boolean isPrimitive(Class clazz) {
        return clazz.isPrimitive();
    }

    public static boolean isString(Class clazz) {
        return clazz == String.class;
    }

    public static boolean isNumberType(Class clazz) {
        return clazz.isPrimitive() || isNumber(clazz);
    }


}
