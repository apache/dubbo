package org.apache.dubbo.compatible.serialization;

import com.alibaba.dubbo.common.serialize.ObjectOutput;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class MyObjectOutput implements ObjectOutput {

    private final BufferedWriter writer;

    public MyObjectOutput(OutputStream outputStream) {
        writer = new BufferedWriter(new OutputStreamWriter(outputStream));
    }

    @Override
    public void writeObject(Object obj) throws IOException {

    }

    @Override
    public void writeBool(boolean v) throws IOException {

    }

    @Override
    public void writeByte(byte v) throws IOException {

    }

    @Override
    public void writeShort(short v) throws IOException {

    }

    @Override
    public void writeInt(int v) throws IOException {

    }

    @Override
    public void writeLong(long v) throws IOException {

    }

    @Override
    public void writeFloat(float v) throws IOException {

    }

    @Override
    public void writeDouble(double v) throws IOException {

    }

    @Override
    public void writeUTF(String v) throws IOException {
        writer.write(v);
        writer.write('\n');
    }

    @Override
    public void writeBytes(byte[] v) throws IOException {

    }

    @Override
    public void writeBytes(byte[] v, int off, int len) throws IOException {

    }

    @Override
    public void flushBuffer() throws IOException {
        writer.flush();
    }
}
