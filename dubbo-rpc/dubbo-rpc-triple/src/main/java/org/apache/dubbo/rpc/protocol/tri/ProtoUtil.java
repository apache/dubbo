package org.apache.dubbo.rpc.protocol.tri;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.ByteBufInputStream;

/**
 * guohaoice@gmail.com
 */
public class ProtoUtil {

    private static ConcurrentHashMap<Class<?>, Message> instCache = new ConcurrentHashMap<>();
    /**
     * parse proto from netty {@link ByteBuf}
     *
     * @param input           netty ByteBuf
     * @param defaultInstance default instance for proto
     * @return proto message
     */
    @SuppressWarnings("all")
    //public static <T extends MessageLite> T parseFrom(ByteBuf input, int len, T defaultInstance) throws InvalidProtocolBufferException {
    //    final ByteBuf byteBuf = input.readSlice(len);
    //    T message = (T) defaultInstance.getParserForType().parseFrom(new ByteBufInputStream(byteBuf));
    //    return message;
    //}

    public static Message defaultInst(Class<?> clz) {
        Message defaultInst = instCache.get(clz);
        if (defaultInst != null) {
            return defaultInst;
        }
        try {
            defaultInst = (Message) clz.getMethod("getDefaultInstance").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Create default protobuf instance failed ", e);
        }
        instCache.put(clz, defaultInst);
        return defaultInst;
    }

    @SuppressWarnings("all")
    public static <T> Parser<T> getParser(Class<T> clz) {
        Message defaultInst = defaultInst(clz);
        return (Parser<T>) defaultInst.getParserForType();
    }
}
