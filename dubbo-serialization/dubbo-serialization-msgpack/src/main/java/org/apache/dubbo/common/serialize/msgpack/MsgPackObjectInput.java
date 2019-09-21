package org.apache.dubbo.common.serialize.msgpack;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.lang.reflect.Type;

import org.apache.dubbo.common.serialize.ObjectInput;
import org.msgpack.jackson.dataformat.MessagePackFactory;

/**
 * @author goodjava@163.com
 */
public class MsgPackObjectInput implements ObjectInput {

    private final InputStream in;

    private final ObjectMapper objectMapper;

    public MsgPackObjectInput(InputStream in) {
        this.in = in;
        objectMapper = new ObjectMapper(new MessagePackFactory());
        objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
    }

    @Override
    public boolean readBool() throws IOException {
        return read(boolean.class);
    }

    @Override
    public byte readByte() throws IOException {
        return read(byte.class);
    }

    @Override
    public short readShort() throws IOException {
        return read(short.class);
    }

    @Override
    public int readInt() throws IOException {
        return read(int.class);
    }

    @Override
    public long readLong() throws IOException {
        return read(long.class);
    }

    @Override
    public float readFloat() throws IOException {
        return read(float.class);
    }

    @Override
    public double readDouble() throws IOException {
        return read(double.class);
    }

    @Override
    public String readUTF() throws IOException {
        return read(String.class);
    }

    @Override
    public byte[] readBytes() throws IOException {
        return read(byte[].class);
    }

    @Override
    public Object readObject() throws IOException {
        return objectMapper.readValue(this.in, Object.class);
    }

    @Override
    public <T> T readObject(Class<T> cls) throws IOException {
        return read(cls);
    }

    @Override
    public <T> T readObject(Class<T> cls, Type type) throws IOException {
        return readObject(cls);
    }


    private <T> T read(Class<T> cls) throws IOException {
        return objectMapper.readValue(this.in, cls);
    }
}
