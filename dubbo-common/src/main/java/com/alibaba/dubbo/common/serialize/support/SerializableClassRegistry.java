package com.alibaba.dubbo.common.serialize.support;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author lishen
 */
public abstract class SerializableClassRegistry {

    private static final Set<Class> registrations = new LinkedHashSet<Class>();

    /**
     * only supposed to be called at startup time
     */
    public static void registerClass(Class clazz) {
        registrations.add(clazz);
    }

    public static Set<Class> getRegisteredClasses() {
        return registrations;
    }
}
