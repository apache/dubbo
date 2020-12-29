package org.apache.dubbo.rpc.protocol.tri;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Marshaller {

    public static Marshaller marshaller = new Marshaller();

    public Object unmarshaller(Class<?> requestClass, ByteBuf in) {

        final Parser<?> parser = ProtoUtil.getParser(requestClass);
        Object result = null;
        try {
            result = parser.parseFrom(new ByteBufInputStream(in));
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return result;
    }

    public ByteBuf marshaller(ByteBufAllocator alloc, Object object) {
        InputStream stream = new ByteArrayInputStream(((MessageLite) object).toByteArray());
        try {
            int len = stream.available();
            final ByteBuf out = alloc.buffer(len + 5);
            out.writeByte(0);
            out.writeInt(len);
            out.writeBytes(stream, len);
            return out;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
