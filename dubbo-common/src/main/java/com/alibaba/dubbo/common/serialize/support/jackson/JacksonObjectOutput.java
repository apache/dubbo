package com.alibaba.dubbo.common.serialize.support.jackson;

import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;

/**
 * Created by wuyu on 2017/2/9.
 */
public class JacksonObjectOutput implements ObjectOutput {

    private final PrintWriter writer;

    public JacksonObjectOutput(OutputStream out) {
        this(new OutputStreamWriter(out));
    }

    public JacksonObjectOutput(Writer writer) {
        this.writer = new PrintWriter(writer);
    }

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void writeBool(boolean v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeByte(byte v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeShort(short v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeUTF(String v) throws IOException {
        writer.write(v);
        writer.println();
    }

    @Override
    public void writeBytes(byte[] v) throws IOException {
        writer.write(new String(v));
        writer.println();
    }

    @Override
    public void writeBytes(byte[] v, int off, int len) throws IOException {
        writer.write(new String(v, off, len));
        writer.println();
    }

    @Override
    public void flushBuffer() throws IOException {
        writer.flush();
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        if (obj == null) {
            return ;
        } else if (obj instanceof String) {
            writer.write(obj.toString());
        } else {
            writer.write(objectMapper.writeValueAsString(obj));
        }
        writer.println();
        writer.flush();
    }
}
