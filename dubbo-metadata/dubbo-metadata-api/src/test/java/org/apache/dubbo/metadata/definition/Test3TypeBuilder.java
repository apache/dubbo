package org.apache.dubbo.metadata.definition;

import org.apache.dubbo.metadata.definition.builder.TypeBuilder;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * test for sort
 */
public class Test3TypeBuilder implements TypeBuilder {
    // it is smaller than the implements of TypeBuilder
    public int getPriority(){
        return 10;
    }

    @Override
    public boolean accept(Type type, Class<?> clazz) {
        return false;
    }

    @Override
    public TypeDefinition build(Type type, Class<?> clazz, Map<Class<?>, TypeDefinition> typeCache) {
        return null;
    }
}
