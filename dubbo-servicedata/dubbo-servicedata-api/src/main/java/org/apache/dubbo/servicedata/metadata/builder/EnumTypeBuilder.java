package org.apache.dubbo.servicedata.metadata.builder;

import org.apache.dubbo.servicedata.metadata.TypeDescriptor;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * @author cvictory ON 2018/9/18
 */
public class EnumTypeBuilder implements TypeBuilder{

    @Override
    public boolean accept(Type type, Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        if (clazz.isEnum()) {
            return true;
        }

        return false;
    }

    @Override
    public TypeDescriptor build(Type type, Class<?> clazz, Map<Class<?>, TypeDescriptor> typeCache) {
        TypeDescriptor td = new TypeDescriptor(clazz.getCanonicalName());

        try {
            // set values
//            Method methodValues = clazz.getDeclaredMethod("values", new Class<?>[0]);
//            Object[] values = (Object[]) methodValues.invoke(clazz, new Object[0]);
//            int length = values.length;
//            for (int i = 0; i < length; i++) {
//                Object value = values[i];
//                td.getEnums().add(value.toString());
//            }
        } catch (Throwable t) {
        }

        typeCache.put(clazz, td);
        return td;
    }
}
