/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.serialize;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import com.alibaba.com.caucho.hessian.io.Hessian2Output;
import com.alibaba.dubbo.common.io.Bytes;
import com.alibaba.dubbo.common.serialize.support.dubbo.Builder;
import com.alibaba.dubbo.common.serialize.support.java.CompactedObjectInputStream;
import com.alibaba.dubbo.common.serialize.support.java.CompactedObjectOutputStream;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class SerializationCompareTest {
    @Test
    public void test_CompareSerializeLength() throws Exception {
        long[] data = new long[]{-1l, 2l, 3l, 4l, 5l};
        ByteArrayOutputStream os;

        os = new ByteArrayOutputStream();
        ObjectOutputStream jos = new ObjectOutputStream(os);
        jos.writeObject(data);
        System.out.println("java:" + Bytes.bytes2hex(os.toByteArray()) + ":" + os.size());

        os = new ByteArrayOutputStream();
        CompactedObjectOutputStream oos = new CompactedObjectOutputStream(os);
        oos.writeObject(data);
        System.out.println("compacted java:" + Bytes.bytes2hex(os.toByteArray()) + ":" + os.size());

        os = new ByteArrayOutputStream();
        Hessian2Output h2o = new Hessian2Output(os);
        h2o.writeObject(data);
        h2o.flushBuffer();
        System.out.println("hessian:" + Bytes.bytes2hex(os.toByteArray()) + ":" + os.size());

        os = new ByteArrayOutputStream();
        Builder<long[]> lb = Builder.register(long[].class);
        lb.writeTo(data, os);
        System.out.println("DataOutput:" + Bytes.bytes2hex(os.toByteArray()) + ":" + os.size());
    }

    @Test
    public void testBuilderPerm() throws Exception {
        Builder<Bean> bb = Builder.register(Bean.class);
        Bean bean = new Bean();
        int len = 0;
        long now = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bb.writeTo(bean, os);
            os.close();
            if (i == 0)
                len = os.toByteArray().length;

            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            Bean b = bb.parseFrom(is);
            assertEquals(b.getClass(), Bean.class);
        }
        System.out.println("Builder write and parse 500 times in " + (System.currentTimeMillis() - now) + "ms, size " + len);
    }

    @Test
    public void testH2oPerm() throws Exception {
        Bean bean = new Bean();
        int len = 0;
        long now = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Hessian2Output out = new Hessian2Output(os);
            out.writeObject(bean);
            out.flushBuffer();
            os.close();
            if (i == 0)
                len = os.toByteArray().length;
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            Hessian2Input in = new Hessian2Input(is);
            assertEquals(in.readObject().getClass(), Bean.class);
        }
        System.out.println("Hessian2 write and parse 500 times in " + (System.currentTimeMillis() - now) + "ms, size " + len);
    }

    @Test
    public void testJavaOutputPerm() throws Exception {
        Bean bean = new Bean();
        int len = 0;
        long now = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(os);
            out.writeObject(bean);
            os.close();
            if (i == 0)
                len = os.toByteArray().length;
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            ObjectInputStream in = new ObjectInputStream(is);
            assertEquals(in.readObject().getClass(), Bean.class);
        }
        System.out.println("java write and parse 500 times in " + (System.currentTimeMillis() - now) + "ms, size " + len);
    }

    @Test
    public void testCompactedJavaOutputPerm() throws Exception {
        Bean bean = new Bean();
        int len = 0;
        long now = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            CompactedObjectOutputStream out = new CompactedObjectOutputStream(os);
            out.writeObject(bean);
            os.close();
            if (i == 0)
                len = os.toByteArray().length;
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            CompactedObjectInputStream in = new CompactedObjectInputStream(is);
            assertEquals(in.readObject().getClass(), Bean.class);
        }
        System.out.println("compacted java write and parse 500 times in " + (System.currentTimeMillis() - now) + "ms, size " + len);
    }

    public static enum EnumTest {READ, WRITE, CREATE, UNREGISTER}

    ;

    static class MyList<T> extends ArrayList<T> {
        private static final long serialVersionUID = 1L;

        private int code = 12345;
        private String id = "feedback";
    }

    static class MyMap<K, V> extends HashMap<K, V> {
        private static final long serialVersionUID = 1L;

        private int code = 12345;
        private String id = "feedback";
    }

    public static class Bean implements Serializable {
        private static final long serialVersionUID = 7737610585231102146L;

        public EnumTest ve = EnumTest.CREATE;

        public int vi = 0;
        public long vl = 100l;

        boolean b = true;
        boolean[] bs = {false, true};

        String s1 = "1234567890";
        String s2 = "1234567890一二三四五六七八九零";

        int i = 123123, ni = -12344, is[] = {1, 2, 3, 4, -1, -2, -3, -4};
        short s = 12, ns = -76;
        double d = 12.345, nd = -12.345;
        long l = 1281447759383l, nl = -13445l;
        private ArrayList<Object> mylist = new ArrayList<Object>();
        private HashMap<Object, Object> mymap = new HashMap<Object, Object>();

        {
            mylist.add(1);
            mylist.add("qianlei");
            mylist.add("qianlei");
            mylist.add("qianlei");
            mylist.add("qianlei");
        }

        {
            mymap.put(1, 2);
            mymap.put(2, "1234");
            mymap.put("2345", 12938.122);
            mymap.put("2345", -1);
            mymap.put("2345", -1.20);
        }

        public ArrayList<Object> getMylist() {
            return mylist;
        }

        public void setMylist(ArrayList<Object> list) {
            mylist = list;
        }

        public HashMap<Object, Object> getMymap() {
            return mymap;
        }

        public void setMymap(HashMap<Object, Object> map) {
            mymap = map;
        }
    }
}