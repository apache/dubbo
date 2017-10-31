package com.alibaba.com.caucho.hessian.io;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.EnumSet;

/**
 * @author bw on 24/10/2017.
 */
public class EnumSetSerializer extends AbstractSerializer {
    private static EnumSetSerializer SERIALIZER = new EnumSetSerializer();

    public static EnumSetSerializer getInstance() {
        return SERIALIZER;
    }

    @Override
    public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {
        if (obj == null) {
            out.writeNull();
        } else {
            try {
                Field field = EnumSet.class.getDeclaredField("elementType");
                field.setAccessible(true);
                Class type = (Class) field.get(obj);
                EnumSet enumSet = (EnumSet) obj;
                Object[] objects = enumSet.toArray();
                out.writeObject(new EnumSetHandler(type, objects));
            } catch (Throwable t) {
                throw new IOException(t);
            }
        }
    }
}
