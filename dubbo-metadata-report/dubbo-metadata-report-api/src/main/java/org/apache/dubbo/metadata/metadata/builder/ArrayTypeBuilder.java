package org.apache.dubbo.metadata.metadata.builder;

import org.apache.dubbo.metadata.metadata.TypeDescriptor;

import java.lang.reflect.Type;
import java.util.Map;

/**
 *  2018/9/18
 */
public class ArrayTypeBuilder implements TypeBuilder {

    @Override
    public boolean accept(Type type, Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        if (clazz.isArray()) {
            return true;
        }

        return false;
    }

    @Override
    public TypeDescriptor build(Type type, Class<?> clazz, Map<Class<?>, TypeDescriptor> typeCache) {
        // Process the component type of an array.
        Class<?> componentType = clazz.getComponentType();
        TypeDescriptorBuilder.build(componentType, componentType, typeCache);

        final String canonicalName = clazz.getCanonicalName();
        TypeDescriptor td = new TypeDescriptor(canonicalName);
        return td;
    }
}
