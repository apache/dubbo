package com.alibaba.dubbo.common.serialize.dubbo;

import com.alibaba.dubbo.common.io.UnsafeByteArrayOutputStream;
import com.alibaba.dubbo.common.serialize.support.dubbo.GenericObjectOutput;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class YunaiBuilderTest {

    public static class Student implements Serializable {

        public String username;
        public String password;

        public Info info1;
        public Info info2;

        public Student student;

        public final int a = 3;
    }

    public static class Info implements Serializable {

        public String key;

    }

    @Test
    public void testRef() throws IOException {

        Info info = new Info();
        info.key = "热爱吃饭";
        Student student = new Student();
        student.username = "wangwenbin";
        student.password = "world";
        student.info1 = info;
        student.info2 = info;

        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();
        GenericObjectOutput objectOutput = new GenericObjectOutput(os);
        objectOutput.writeObject(student);
//        objectOutput.writeObject(student);
    }

    @Test
    public void testRef2() throws IOException {
        Info info = new Info();
        info.key = "热爱吃饭";
        Student student = new Student();

        Student[] students = new Student[]{student, student};
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();
        GenericObjectOutput objectOutput = new GenericObjectOutput(os);
        objectOutput.writeObject(students);
    }

    @Test
    public void testHashMap() throws IOException {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("123", 666);

        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();
        GenericObjectOutput objectOutput = new GenericObjectOutput(os);
        objectOutput.writeObject(params);
    }

    @Test
    public void test01() throws IOException {

        int[][] ass = new int[2][3];
        ass[0] = new int[]{1, 2, 3};
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();
        GenericObjectOutput objectOutput = new GenericObjectOutput(os);
        objectOutput.writeObject(ass);
    }

}
