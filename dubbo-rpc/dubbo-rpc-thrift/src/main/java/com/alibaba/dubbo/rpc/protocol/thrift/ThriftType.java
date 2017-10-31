package com.alibaba.dubbo.rpc.protocol.thrift;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public enum ThriftType {

    BOOL, BYTE, I16, I32, I64, DOUBLE, STRING;

    private static final Map<Class<?>, ThriftType> types =
            new HashMap<Class<?>, ThriftType>();

    static {
        put(boolean.class, BOOL);
        put(Boolean.class, BOOL);
        put(byte.class, BYTE);
        put(Byte.class, BYTE);
        put(short.class, I16);
    }

    public static ThriftType get(Class<?> key) {
        if (key != null) {
            return types.get(key);
        }
        throw new NullPointerException("key == null");
    }

    private static void put(Class<?> key, ThriftType value) {
        types.put(key, value);
    }

}
