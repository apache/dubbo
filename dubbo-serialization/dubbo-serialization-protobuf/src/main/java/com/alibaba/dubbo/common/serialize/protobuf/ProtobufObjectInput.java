package com.alibaba.dubbo.common.serialize.protobuf;

import com.alibaba.dubbo.common.serialize.ObjectInput;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ProtobufObjectInput implements ObjectInput {

    private InputStream in;
    private DataInputStream inputStream;

    public ProtobufObjectInput(InputStream in){
        this.in = in;
        this.inputStream = new DataInputStream(in);
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        int classNameLength = inputStream.readInt();
        int length = inputStream.readInt();

        byte[] classNameBytes = new byte[classNameLength];
        inputStream.read(classNameBytes,0,classNameLength);

        byte[] bytes = new byte[length];
        inputStream.read(bytes,0,length);

        String className = new String(classNameBytes);
        Class clazz = Class.forName(className);

        if (clazz == Map.class || clazz == HashMap.class){
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream inputStream = new ObjectInputStream(bis);
            Object o = inputStream.readObject();
            inputStream.close();
            return o;
        }else {
            Objenesis objenesis = new ObjenesisStd(true);
            Object obj = objenesis.newInstance(clazz);

            Schema schema = RuntimeSchema.getSchema(clazz);
            ProtobufIOUtil.mergeFrom(bytes,obj,schema);

            return obj;
        }
    }

    @Override
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        if (cls == Map.class){
            int classNameLength = inputStream.readInt();
            int length = inputStream.readInt();

            byte[] classNameBytes = new byte[classNameLength];
            inputStream.read(classNameBytes,0,classNameLength);

            byte[] bytes = new byte[length];
            inputStream.read(bytes,0,length);

            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream inputStream = new ObjectInputStream(bis);
            Object o = inputStream.readObject();
            inputStream.close();
            return (T) o;
        }else {
            int classNameLength = inputStream.readInt();
            int length = inputStream.readInt();

            byte[] classNameBytes = new byte[classNameLength];
            inputStream.read(classNameBytes,0,classNameLength);

            byte[] bytes = new byte[length];
            inputStream.read(bytes,0,length);

            Objenesis objenesis = new ObjenesisStd(true);
            T obj = objenesis.newInstance(cls);

            Schema schema = RuntimeSchema.getSchema(cls);
            ProtobufIOUtil.mergeFrom(bytes,obj,schema);

            return obj;
        }
    }

    @Override
    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        return (T) readObject();
    }

    @Override
    public boolean readBool() throws IOException {
        return false;
    }

    @Override
    public byte readByte() throws IOException {
        return inputStream.readByte();
    }

    @Override
    public short readShort() throws IOException {
        return 0;
    }

    @Override
    public int readInt() throws IOException {
        return 0;
    }

    @Override
    public long readLong() throws IOException {
        return 0;
    }

    @Override
    public float readFloat() throws IOException {
        return 0;
    }

    @Override
    public double readDouble() throws IOException {
        return 0;
    }

    @Override
    public String readUTF() throws IOException {
        int length = inputStream.readInt();
        byte[] bytes = new byte[length];
        inputStream.read(bytes,0,length);
        String s = new String(bytes);
        return s;
    }

    @Override
    public byte[] readBytes() throws IOException {
        return new byte[0];
    }
}
