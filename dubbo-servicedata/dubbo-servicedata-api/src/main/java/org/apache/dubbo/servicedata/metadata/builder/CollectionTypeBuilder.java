package org.apache.dubbo.servicedata.metadata.builder;

import org.apache.dubbo.servicedata.metadata.TypeDescriptor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;

/**
 *  2018/9/18
 */
public class CollectionTypeBuilder implements TypeBuilder{
    @Override
    public boolean accept(Type type, Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        if (Collection.class.isAssignableFrom(clazz)) {
            return true;
        }

        return false;
    }

    @Override
    public TypeDescriptor build(Type type, Class<?> clazz, Map<Class<?>, TypeDescriptor> typeCache) {
        if (!(type instanceof ParameterizedType)) {
            // 没有泛型信息，就直接返回class name
            return new TypeDescriptor(clazz.getName());
        }

        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] actualTypeArgs = parameterizedType.getActualTypeArguments();
        if (actualTypeArgs == null || actualTypeArgs.length != 1) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "[Jaket] Collection type [{0}] with unexpected amount of arguments [{1}]." + actualTypeArgs,
                    new Object[] { type, actualTypeArgs }));
        }

        Type actualType = actualTypeArgs[0];
        if (actualType instanceof ParameterizedType) {
            // Nested collection or map.
            Class<?> rawType = (Class<?>) ((ParameterizedType) actualType).getRawType();
            TypeDescriptorBuilder.build(actualType, rawType, typeCache);
        } else if (actualType instanceof Class<?>) {
            Class<?> actualClass = (Class<?>) actualType;
            if (actualClass.isArray() || actualClass.isEnum()) {
                TypeDescriptorBuilder.build(null, actualClass, typeCache);
            } else {
                DefaultTypeBuilder.build(actualClass, typeCache);
            }
        }

        return new TypeDescriptor(type.toString());
    }
}
