package org.apache.dubbo.common;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.apache.dubbo.common.model.AnimalEnum;
import org.apache.dubbo.common.serialize.protobuf.Utils;
import org.apache.dubbo.common.serialize.protobuf.Wrapper;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class ProtostuffTest {
    @Test
    public void test_enum() throws IOException, ClassNotFoundException {
        Object obj = AnimalEnum.dog;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // 序列化
        DataOutputStream dos = new DataOutputStream(byteArrayOutputStream);

        LinkedBuffer buffer = LinkedBuffer.allocate();

        byte[] bytes;
        byte[] classNameBytes;

        Schema schema = RuntimeSchema.getSchema(obj.getClass());
        bytes = ProtobufIOUtil.toByteArray(obj, schema, buffer);
        classNameBytes = obj.getClass().getName().getBytes();

        int classNameBytesLength = classNameBytes.length;
        dos.writeInt(classNameBytesLength);
        int length = bytes.length;
        dos.writeInt(length);
        dos.write(classNameBytes);
        dos.write(bytes);
        dos.flush();

        // 反序列化
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        DataInputStream dis = new DataInputStream(byteArrayInputStream);

        int classNameLength = dis.readInt();
        int bytesLength = dis.readInt();

        if (classNameLength < 0 || bytesLength < 0) {
            throw new IOException();
        }

        byte[] classNameBytes2 = new byte[classNameLength];
        dis.readFully(classNameBytes, 0, classNameLength);

        byte[] bytes2 = new byte[bytesLength];
        dis.readFully(bytes2, 0, bytesLength);

        String className2 = new String(classNameBytes);
        Class clazz2 = Class.forName(className2);

        assertEquals(clazz2, obj.getClass());

        Object result;
        Schema schema2 = RuntimeSchema.getSchema(AnimalEnum.class);
        result = schema2.newMessage();
        ProtobufIOUtil.mergeFrom(bytes, result, schema2);

        System.out.println(AnimalEnum.dog.equals(result));
        System.out.println(AnimalEnum.dog == result);

        Class classX = obj.getClass();
        Class classY = result.getClass();

        System.out.println(classX == classY);

        assertEquals(AnimalEnum.dog, (AnimalEnum) result);
    }
}
