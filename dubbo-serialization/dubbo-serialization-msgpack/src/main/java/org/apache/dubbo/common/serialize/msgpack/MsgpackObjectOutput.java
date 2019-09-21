package org.apache.dubbo.common.serialize.msgpack;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;


/**
 * @author goodjava@163.com
 */
public class MsgpackObjectOutput implements ObjectOutput {

    private final OutputStream out;
    private final ObjectMapper objectMapper;

    public MsgpackObjectOutput(OutputStream out) {
        this.out = out;
        this.objectMapper = new ObjectMapper(new MessagePackFactory());
    }


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
        writeObject(v);
    }

    @Override
    public void writeBytes(byte[] b) throws IOException {
        writeObject(b);
    }

    @Override
    public void writeBytes(byte[] b, int off, int len) throws IOException {
        byte[] data = Arrays.copyOfRange(b, off, len);
        objectMapper.writeValue(this.out, data);
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        objectMapper.writeValue(this.out, obj);
    }

    @Override
    public void flushBuffer() throws IOException {
        this.out.flush();
    }

}
