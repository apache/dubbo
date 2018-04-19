package com.alibaba.com.caucho.hessian.io.base;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import com.alibaba.com.caucho.hessian.io.Hessian2Output;
import com.alibaba.com.caucho.hessian.io.HessianInput;
import com.alibaba.com.caucho.hessian.io.HessianOutput;
import com.alibaba.com.caucho.hessian.io.SerializerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * hession base serialize utils
 *
 */
public class SerializeTestBase {
    protected SerializerFactory factory = new SerializerFactory();

    /**
     * hession serialize util
     *
     * @param data
     * @param <T>
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    protected <T> T baseHessionSerialize(T data) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        HessianOutput out = new HessianOutput(bout);

        out.writeObject(data);
        out.flush();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        HessianInput input = new HessianInput(bin);
        input.setSerializerFactory(factory);
        return (T) input.readObject();
    }

    /**
     * hession2 serialize util
     *
     * @param data
     * @param <T>
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    protected <T> T baseHession2Serialize(T data) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bout);

        out.writeObject(data);
        out.flush();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        Hessian2Input input = new Hessian2Input(bin);
        input.setSerializerFactory(factory);
        return (T) input.readObject();
    }

    @SuppressWarnings("unchecked")
    protected <T> T getFieldValue(Object bean, String fieldName) throws Exception {
        Field field = bean.getClass().getDeclaredField(fieldName);
        if (!field.isAccessible()) field.setAccessible(true);
        return (T) field.get(bean);
    }
}
