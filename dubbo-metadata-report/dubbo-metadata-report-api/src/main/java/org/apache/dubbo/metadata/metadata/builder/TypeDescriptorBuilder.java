package org.apache.dubbo.metadata.metadata.builder;

import org.apache.dubbo.metadata.metadata.TypeDescriptor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  2018/9/18
 */
public class TypeDescriptorBuilder {

    private static final List<TypeBuilder> builders = new ArrayList<TypeBuilder>();
    private Map<Class<?>, TypeDescriptor> typeCache = new HashMap<Class<?>, TypeDescriptor>();


    static {
        builders.add(new ArrayTypeBuilder());
        builders.add(new CollectionTypeBuilder());
        builders.add(new MapTypeBuilder());
        builders.add(new EnumTypeBuilder());
    }


    public static TypeDescriptor build(Type type, Class<?> clazz, Map<Class<?>, TypeDescriptor> typeCache) {
        TypeBuilder builder = getGenericTypeBuilder(type, clazz);
        TypeDescriptor td = null;
        if (builder != null) {
            td = builder.build(type, clazz, typeCache);
        } else {
            td = DefaultTypeBuilder.build(clazz, typeCache);
        }
        return td;
    }

    static TypeBuilder getGenericTypeBuilder(Type type, Class<?> clazz) {
        for (TypeBuilder builder : builders) {
            if (builder.accept(type, clazz)) {
                return builder;
            }
        }
        return null;
    }

    public TypeDescriptor build(Type type, Class<?> clazz) {
        return build(type, clazz, typeCache);
    }

    public Map<String, TypeDescriptor> getTypeDescriptorMap() {
        if (typeCache == null || typeCache.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        Map<String, TypeDescriptor> typeDescriptorMap = new HashMap<>();
        for (Map.Entry<Class<?>, TypeDescriptor> entry : typeCache.entrySet()) {
            typeDescriptorMap.put(entry.getKey().getName(), entry.getValue());
        }
        return typeDescriptorMap;
    }
}
