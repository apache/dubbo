package org.apache.dubbo.common.serialize.jackson;

import java.io.*;

import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.jackson.utils.JacksonUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 *
 * Jackson object output implementation
 * 
 * @author Johnson.Jia
 */
public class JacksonObjectOutput implements ObjectOutput {

    private final PrintWriter writer;

    public JacksonObjectOutput(OutputStream out) {
        this(new OutputStreamWriter(out));
    }

    public JacksonObjectOutput(Writer writer) {
        this.writer = new PrintWriter(writer);
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
        writer.println(new String(b));
    }

    @Override
    public void writeBytes(byte[] b, int off, int len) throws IOException {
        writer.println(new String(b, off, len));
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        char[] json = JacksonUtils.writeValueAsString(obj).toCharArray();
        writer.write(json, 0, json.length);
        writer.println();
        writer.flush();
        json = null;
    }

    @Override
    public void flushBuffer() throws IOException {
        writer.flush();
    }

    @Override
    public void writeThrowable(Object obj) throws IOException {
        writeObjectClass(obj);
    }

    @Override
    public void writeEvent(Object data) throws IOException {
        writeObjectClass(data);
    }

    private void writeObjectClass(Object data) throws JsonProcessingException {
        String value =
            "{\"@c\":\"" + data.getClass().getName() + "\"," + JacksonUtils.writeValueAsString(data).substring(1);
        char[] json = value.toCharArray();
        writer.write(json, 0, json.length);
        writer.println();
        writer.flush();
        json = null;
        value = null;
    }

}
