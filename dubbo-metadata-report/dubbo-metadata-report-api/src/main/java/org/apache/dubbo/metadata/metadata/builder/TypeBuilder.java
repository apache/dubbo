package org.apache.dubbo.metadata.metadata.builder;

import org.apache.dubbo.metadata.metadata.TypeDescriptor;

import java.lang.reflect.Type;
import java.util.Map;

/**
 *  2018/9/18
 */
public interface TypeBuilder {

    /**
     * Whether the build accept the type or class passed in.
     */
    boolean accept(Type type, Class<?> clazz);

    /**
     * Build type definition with the type or class.
     */
    TypeDescriptor build(Type type, Class<?> clazz, Map<Class<?>, TypeDescriptor> typeCache);

}
