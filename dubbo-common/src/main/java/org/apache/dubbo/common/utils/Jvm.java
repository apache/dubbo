package org.apache.dubbo.common.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 */
public enum Jvm {
    ;
    private static final int JVM_JAVA_MAJOR_VERSION;
    private static final boolean IS_JAVA_8_PLUS;

    static {
        JVM_JAVA_MAJOR_VERSION = getMajorVersion0();
        IS_JAVA_8_PLUS = getMajorVersion0() > 7;
    }

    public static boolean isJava8Plus() {
        return IS_JAVA_8_PLUS;
    }

    private static int getMajorVersion0() {
        try {
            final Method method = Runtime.class.getDeclaredMethod("version");
            if (method != null) {
                final Object version = method.invoke(Runtime.getRuntime());
                final Class<?> clz = Class.forName("java.lang.Runtime$Version");
                return (Integer) clz.getDeclaredMethod("major").invoke(version);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            // ignore and fall back to pre-jdk9
        }
        return Integer.parseInt(Runtime.class.getPackage().getSpecificationVersion().split("\\.")[1]);
    }
}
