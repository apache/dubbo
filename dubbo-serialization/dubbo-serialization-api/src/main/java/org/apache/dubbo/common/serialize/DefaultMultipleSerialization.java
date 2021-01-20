package org.apache.dubbo.common.serialize;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DefaultMultipleSerialization implements MultipleSerialization {

    @Override
    public void serialize(URL url, String serializeType, String clz, Object obj, OutputStream os) throws IOException {
        serializeType = convertHessian(serializeType);
        final Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(serializeType);
        final ObjectOutput serialize = serialization.serialize(null, os);
        serialize.writeObject(obj);
        serialize.flushBuffer();
    }

    @Override
    public Object deserialize(URL url, String serializeType, String clz, InputStream os) throws IOException, ClassNotFoundException {
        serializeType = convertHessian(serializeType);
        final Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(serializeType);
        final Class<?> aClass = ClassUtils.forName(clz);
        final ObjectInput in = serialization.deserialize(null, os);
        return in.readObject(aClass);
    }

    private String convertHessian(String ser) {
        if (ser.equals("hessian4")) {
            return "hessian2";
        }
        return ser;
    }
}
