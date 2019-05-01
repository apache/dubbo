package org.apache.dubbo.serialize.hessian;

import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.Serializer;
import com.caucho.hessian.io.SerializerFactory;

public class Hessian2SerializerFactory extends SerializerFactory {
    public static final SerializerFactory INSTANCE = new Hessian2SerializerFactory();

    private Hessian2SerializerFactory() {
        super();
    }

    @Override
    protected Serializer loadSerializer(Class<?> cl) throws HessianProtocolException {
        Serializer serializer = Java8SerializerFactory.INSTANCE.getSerializer(cl);
        return serializer != null ? serializer : super.loadSerializer(cl);
    }

    @Override
    protected Deserializer loadDeserializer(Class cl) throws HessianProtocolException {
        Deserializer deserializer = Java8SerializerFactory.INSTANCE.getDeserializer(cl);
        return deserializer != null ? deserializer : super.loadDeserializer(cl);
    }
}
