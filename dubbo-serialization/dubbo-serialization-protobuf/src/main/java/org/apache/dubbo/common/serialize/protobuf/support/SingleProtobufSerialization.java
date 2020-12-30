package org.apache.dubbo.common.serialize.protobuf.support;

import org.apache.dubbo.common.serialize.Serialization2;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SingleProtobufSerialization implements Serialization2 {
    private final ConcurrentMap<Class<?>, ProtobufUtils.SingleMessageMarshaller<?>> marshallers = new ConcurrentHashMap<>();

    @Override
    public Object deserialize(InputStream in, Class<?> clz) throws IOException {
        try {
            return getMarshaller(clz).parse(in);
        } catch (InvalidProtocolBufferException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int serialize(Object obj, OutputStream os) throws IOException {
        final MessageLite msg = (MessageLite) obj;
        msg.writeTo(os);
        return msg.getSerializedSize();
    }

    private ProtobufUtils.SingleMessageMarshaller<?> getMarshaller(Class<?> clz) {
        return marshallers.computeIfAbsent(clz, k -> new ProtobufUtils.SingleMessageMarshaller(k));
    }

    private ProtobufUtils.SingleMessageMarshaller<?> getMarshaller(Object obj) {
        return getMarshaller(obj.getClass());
    }

}
