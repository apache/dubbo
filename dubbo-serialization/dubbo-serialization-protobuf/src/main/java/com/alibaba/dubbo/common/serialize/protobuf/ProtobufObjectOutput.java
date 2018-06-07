package com.alibaba.dubbo.common.serialize.protobuf;


import com.alibaba.dubbo.common.serialize.ObjectOutput;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.io.*;
import java.util.Map;

public class ProtobufObjectOutput implements ObjectOutput {

    private OutputStream out;
    private DataOutputStream outputStream;

    public ProtobufObjectOutput(OutputStream out){
        this.out = out;
        this.outputStream = new DataOutputStream(out);
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        // 写入class信息和object的数据
        // int,            int,          bytes,     bytes
        //class信息的长度    obj数据的长度   class信息    obj数据
        if (obj instanceof Map){
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                ObjectOutputStream out = new ObjectOutputStream(bos);
                out.writeObject(obj);
                out.flush();
                byte[] bytes = bos.toByteArray();

                String className = obj.getClass().getName();

                outputStream.writeInt(className.getBytes().length);
                outputStream.writeInt(bytes.length);
                outputStream.write(className.getBytes());
                outputStream.write(bytes);
            } finally {
                try {
                    bos.close();
                } catch (IOException ex) {
                    // ignore close exception
                }
            }
        }else {
            LinkedBuffer buffer = LinkedBuffer.allocate();
            Schema schema = RuntimeSchema.getSchema(obj.getClass());

            byte[] bytes = ProtobufIOUtil.toByteArray(obj,schema,buffer);

            String className = obj.getClass().getName();

            outputStream.writeInt(className.getBytes().length);
            outputStream.writeInt(bytes.length);
            outputStream.write(className.getBytes());
            outputStream.write(bytes);
        }
    }

    @Override
    public void writeBool(boolean v) throws IOException {

    }

    @Override
    public void writeByte(byte v) throws IOException {
        outputStream.writeByte(v);
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
        byte[] bytes = v.getBytes();
        outputStream.writeInt(bytes.length);
        outputStream.write(bytes);
    }

    @Override
    public void writeBytes(byte[] v) throws IOException {

    }

    @Override
    public void writeBytes(byte[] v, int off, int len) throws IOException {

    }

    @Override
    public void flushBuffer() throws IOException {

    }
}
