package org.apache.dubbo.servicedata.metadata.builder;

import org.apache.dubbo.servicedata.metadata.TypeDescriptor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Map;

/**
 * @author cvictory ON 2018/9/18
 */
public class MapTypeBuilder implements TypeBuilder{

    @Override
    public boolean accept(Type type, Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        if (Map.class.isAssignableFrom(clazz)) {
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
        if (actualTypeArgs == null || actualTypeArgs.length != 2) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Map type [{0}] with unexpected amount of arguments [{1}]." + actualTypeArgs, new Object[] {
                            type, actualTypeArgs }));
        }

        for (Type actualType : actualTypeArgs) {
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
        }

        return new TypeDescriptor(type.toString());
    }
}
