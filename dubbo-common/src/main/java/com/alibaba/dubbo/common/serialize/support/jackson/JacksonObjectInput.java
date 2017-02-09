package com.alibaba.dubbo.common.serialize.support.jackson;

import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.*;
import java.lang.reflect.Type;

/**
 * Created by wuyu on 2017/2/9.
 */
public class JacksonObjectInput implements ObjectInput {

    private final BufferedReader reader;

    private ObjectMapper objectMapper = new ObjectMapper();

    public JacksonObjectInput(InputStream in) {
        this(new InputStreamReader(in));
    }

    public JacksonObjectInput(Reader reader) {
        this.reader = new BufferedReader(reader);
    }

    @Override
    public boolean readBool() throws IOException {
        return objectMapper.convertValue(readLine(), boolean.class);
    }

    @Override
    public byte readByte() throws IOException {
        return objectMapper.convertValue(readLine(),byte.class);
    }

    @Override
    public short readShort() throws IOException {
        return objectMapper.convertValue(readLine(),short.class);
    }

    @Override
    public int readInt() throws IOException {
        return objectMapper.convertValue(readLine(),int.class);
    }

    @Override
    public long readLong() throws IOException {
        return objectMapper.convertValue(readLine(),long.class);
    }

    @Override
    public float readFloat() throws IOException {
        return objectMapper.convertValue(readLine(),float.class);
    }

    @Override
    public double readDouble() throws IOException {
        return objectMapper.convertValue(readLine(),double.class);
    }

    @Override
    public String readUTF() throws IOException {
        return readLine();
    }

    @Override
    public byte[] readBytes() throws IOException {
        return readLine().getBytes();
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        return objectMapper.readTree(readLine());
    }

    @Override
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        return objectMapper.readValue(readLine(),cls);
    }

    @Override
    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        JavaType javaType = TypeFactory.defaultInstance().constructType(type);
        return objectMapper.readValue(readLine(),javaType);
    }

    private String readLine() throws IOException, EOFException {
        String line = reader.readLine();
        if (line == null || line.trim().length() == 0) throw new EOFException();
        return line;
    }
}
