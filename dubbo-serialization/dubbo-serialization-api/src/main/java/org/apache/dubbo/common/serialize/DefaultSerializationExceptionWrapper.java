package org.apache.dubbo.common.serialize;

import com.sun.xml.internal.ws.encoding.soap.SerializationException;
import org.apache.dubbo.common.URL;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DefaultSerializationExceptionWrapper implements Serialization {

    private final Serialization serialization;

    public DefaultSerializationExceptionWrapper(Serialization serialization) {
        if (serialization == null) {
            throw new IllegalArgumentException("serialization == null");
        }
        this.serialization = serialization;
    }

    @Override
    public byte getContentTypeId() {
        return serialization.getContentTypeId();
    }

    @Override
    public String getContentType() {
        return serialization.getContentType();
    }

    @Override
    public ObjectOutput serialize(URL url, OutputStream output) throws IOException {
        ObjectOutput objectOutput = serialization.serialize(url, output);
        ObjectInputInvocationHandler handler = new ObjectInputInvocationHandler(objectOutput);
        return (ObjectOutput) Proxy.newProxyInstance(objectOutput.getClass().getClassLoader(), objectOutput.getClass().getInterfaces(), handler);
    }

    @Override
    public ObjectInput deserialize(URL url, InputStream input) throws IOException {
        ObjectInput objectOutput = serialization.deserialize(url, input);
        ObjectInputInvocationHandler handler = new ObjectInputInvocationHandler(objectOutput);
        return (ObjectInput) Proxy.newProxyInstance(objectOutput.getClass().getClassLoader(), objectOutput.getClass().getInterfaces(), handler);
    }

    static class ObjectInputInvocationHandler implements InvocationHandler {

        private final Object target;

        public ObjectInputInvocationHandler(Object target) {
            this.target = target;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
            Object result;
            try {
                result = method.invoke(target, args);
            } catch (Exception e) {
                Throwable t = e.getCause();
                if (!(t instanceof IOException)) {
                    t = new IOException(new SerializationException(e));
                }
                throw t;
            }
            return result;
        }

    }
}
