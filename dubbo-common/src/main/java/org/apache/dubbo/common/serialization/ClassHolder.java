package org.apache.dubbo.common.serialization;

import org.apache.dubbo.common.utils.ConcurrentHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClassHolder {
    private final Map<String, Set<Class<?>>> classCache = new ConcurrentHashMap<>();

    public void storeClass(Class<?> clazz) {
        classCache.computeIfAbsent(clazz.getName(), k -> new ConcurrentHashSet<>()).add(clazz);
    }

    public Class<?> loadClass(String className, ClassLoader classLoader) {
        Set<Class<?>> classList = classCache.get(className);
        if (classList == null) {
            return null;
        }
        for (Class<?> clazz : classList) {
            if (classLoader.equals(clazz.getClassLoader())) {
                return clazz;
            }
        }
        return null;
    }
}
