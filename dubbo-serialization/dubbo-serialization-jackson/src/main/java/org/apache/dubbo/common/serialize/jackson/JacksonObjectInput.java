package org.apache.dubbo.common.serialize.jackson;

import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.jackson.utils.JacksonUtils;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;

/**
 * Jackson object input implementation
 */
public class JacksonObjectInput implements ObjectInput {

    private final BufferedReader reader;

    public JacksonObjectInput(InputStream in) {
        this(new InputStreamReader(in));
    }

    public JacksonObjectInput(Reader reader) {
        this.reader = new BufferedReader(reader);
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
        return readLine().getBytes();
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        return JacksonUtils.readValue(readLine(), Object.class);
    }

    @Override
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        return read(cls);
    }

    @Override
    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        return readObject(cls);
    }

    private String readLine() throws IOException {
        String line = reader.readLine();
        if (line == null || line.trim().length() == 0) {
            throw new EOFException();
        }
        return line;
    }

    protected <T> T read(Class<T> cls) throws IOException {
        return JacksonUtils.readValue(readLine(), cls);
    }
}
