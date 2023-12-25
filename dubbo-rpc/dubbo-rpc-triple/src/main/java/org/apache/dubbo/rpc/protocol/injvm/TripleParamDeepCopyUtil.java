package org.apache.dubbo.rpc.protocol.injvm;

import com.google.protobuf.Message;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.protocol.tri.TripleInvoker;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import static org.apache.dubbo.common.constants.CommonConstants.PROTOBUF_MESSAGE_CLASS_NAME;


public class TripleParamDeepCopyUtil implements ParamDeepCopyUtil{

    @Override
    public <T> T copy(URL url, Object src, Class<T> targetClass, Type type) {

        if(src instanceof Message) {
            try {
                OutputStream outputStream = new ByteArrayOutputStream();
                int compressed = 0;
                outputStream.write(compressed);
                int serializedSize = ((Message) src).getSerializedSize();


                Compressor compressor = TripleInvoker.getCompressorFromEnv();

            }catch (Exception e) {
                return null;
            }
        }else {

        }
    }

    private static void write(HttpMessageCodec codec, OutputStream outputStream, Object data) {
        int serializedSize = ((Message) data).getSerializedSize();
        // write length
        writeLength(outputStream, serializedSize);
        codec.encode(outputStream, data);
    }

    private static void writeLength(OutputStream outputStream, int length) {
        try {
            outputStream.write(((length >> 24) & 0xFF));
            outputStream.write(((length >> 16) & 0xFF));
            outputStream.write(((length >> 8) & 0xFF));
            outputStream.write((length & 0xFF));
        } catch (IOException e) {

        }
    }

    private static boolean isProtobuf(Object data) {
        if (data == null) {
            return false;
        }
        return isProtoClass(data.getClass());
    }

    private static boolean isProtoClass(Class<?> clazz) {
        while (clazz != Object.class && clazz != null) {
            Class<?>[] interfaces = clazz.getInterfaces();
            if (interfaces.length > 0) {
                for (Class<?> clazzInterface : interfaces) {
                    if (PROTOBUF_MESSAGE_CLASS_NAME.equalsIgnoreCase(clazzInterface.getName())) {
                        return true;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }


}
