/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.serialize.dubbo;

import com.alibaba.dubbo.common.io.Bytes;
import com.alibaba.dubbo.common.io.UnsafeByteArrayOutputStream;
import com.alibaba.dubbo.common.serialize.support.dubbo.Builder;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class BuilderTest {
    public static void main(String[] args) {
        System.out.println(Modifier.isPublic(String.class.getModifiers()));
    }

    @Test
    public void testPrimaryTypeBuilder() throws Exception {
        System.out.println((new byte[2]).hashCode());
        Builder<String> builder = Builder.register(String.class);
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();
        String v = "123";
        builder.writeTo(v, os);
        byte[] b = os.toByteArray();
        System.out.println(b.length + ":" + Bytes.bytes2hex(b));
        v = builder.parseFrom(b);
        builder.writeTo(v, os);
        b = os.toByteArray();
        System.out.println(b.length + ":" + Bytes.bytes2hex(b));
    }

    @Test
    public void testEnumBuilder() throws Exception {
        Builder<Type> builder = Builder.register(Type.class);
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();
        Type v = Type.High;
        builder.writeTo(v, os);
        byte[] b = os.toByteArray();
        System.out.println(b.length + ":" + Bytes.bytes2hex(b));
        v = builder.parseFrom(b);
    }

    @Test
    public void testThrowableBuilder() throws Exception {
        Builder<Throwable> builder = Builder.register(Throwable.class);
        Throwable th = new Throwable();
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();
        builder.writeTo(th, os);
        byte[] b = os.toByteArray();
        System.out.println(b.length + ":" + Bytes.bytes2hex(b));

        th = builder.parseFrom(b);
    }

    @Test
    public void testArrayClassBuilder() throws Exception {
        UnsafeByteArrayOutputStream os;

        byte[] b;

        Builder<Object[]> osb = Builder.register(Object[].class);
        os = new UnsafeByteArrayOutputStream();
        osb.writeTo(new Object[]{new String[0]}, os);
        b = os.toByteArray();

        Builder<long[]> lsb = Builder.register(long[].class);
        os = new UnsafeByteArrayOutputStream();
        lsb.writeTo(new long[]{1, 121232, -3, 4, -5, 61321432413l}, os);
        lsb.writeTo(new long[]{1, 121232, -3, 4, -5, 61321432413l}, os);
        lsb.writeTo(new long[]{1, 2, 3, 12131314, 123132313135l, -6}, os);
        b = os.toByteArray();
        long[] ls = lsb.parseFrom(b);
        assertEquals(ls.length, 6);

        Builder<byte[]> bsb = Builder.register(byte[].class);
        os = new UnsafeByteArrayOutputStream();
        bsb.writeTo("i am a string.".getBytes(), os);
        b = os.toByteArray();

        Builder<int[][]> iisb = Builder.register(int[][].class);
        os = new UnsafeByteArrayOutputStream();
        iisb.writeTo(new int[][]{{1, 2, 3, 4}, {5, 6, 7, 8}, {9, 10}, {122, 123, 444}}, os);
        b = os.toByteArray();
        int[][] iis = iisb.parseFrom(b);
        assertEquals(iis.length, 4);

        Builder<int[][][]> iiisb = Builder.register(int[][][].class);
        os = new UnsafeByteArrayOutputStream();
        iiisb.writeTo(new int[][][]{
                {{1, 2, 3, 4}},
                {{5, 6, 7, 8}},
                {{122, 123, 444}}
        }, os);
        b = os.toByteArray();
        int[][][] iii = iiisb.parseFrom(b);
        assertEquals(iii.length, 3);
    }

    @Test
    public void testObjectBuilder() throws Exception {
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();
        Builder<Bean> BeanBuilder = Builder.register(Bean.class);

        Bean bean = new Bean();
        bean.name = "ql";
        bean.type = Type.High;
        bean.types = new Type[]{Type.High, Type.High};
        BeanBuilder.writeTo(bean, os);

        byte[] b = os.toByteArray();
        System.out.println(b.length + ":" + Bytes.bytes2hex(b));

        bean = BeanBuilder.parseFrom(b);
        assertNull(bean.time);
        assertEquals(bean.i, 123123);
        assertEquals(bean.ni, -12344);
        assertEquals(bean.d, 12.345);
        assertEquals(bean.nd, -12.345);
        assertEquals(bean.l, 1281447759383l);
        assertEquals(bean.nl, -13445l);
        assertEquals(bean.vl, 100l);
        assertEquals(bean.type, Type.High);
        assertEquals(bean.types.length, 2);
        assertEquals(bean.types[0], Type.High);
        assertEquals(bean.types[1], Type.High);
        assertEquals(bean.list.size(), 3);
        assertEquals(bean.list.get(0), 1);
        assertEquals(bean.list.get(1), 2);
        assertEquals(bean.list.get(2), 1308147);
    }

    @Test
    public void testInterfaceBuilder() throws Exception {
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();
        Builder<TestDO> builder = Builder.register(TestDO.class);
        TestDO d = new TestDOImpl();
        builder.writeTo(d, os);

        byte[] b = os.toByteArray();

        d = builder.parseFrom(b);
        assertTrue(TestDO.class.isAssignableFrom(d.getClass()));
        assertEquals("name", d.getName());
        assertEquals(28, d.getArg());
        assertEquals(Type.High, d.getType());
    }

    @Test
    public void testGenericBuilder() throws Exception {
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();
        Builder<Object> ob = Builder.register(Object.class);

        Object o = new Object();
        ob.writeTo(o, os);
        byte[] b = os.toByteArray();

        os = new UnsafeByteArrayOutputStream();
        Bean bean = new Bean();
        bean.name = "ql";
        bean.type = Type.High;
        bean.types = new Type[]{Type.High, Type.High};
        ob.writeTo(bean, os);

        b = os.toByteArray();
        bean = (Bean) ob.parseFrom(b);
        assertEquals(bean.i, 123123);
        assertEquals(bean.ni, -12344);
        assertEquals(bean.d, 12.345);
        assertEquals(bean.nd, -12.345);
        assertEquals(bean.l, 1281447759383l);
        assertEquals(bean.nl, -13445l);
        assertEquals(bean.vl, 100l);
        assertEquals(bean.type, Type.High);
        assertEquals(bean.types.length, 2);
        assertEquals(bean.types[0], Type.High);
        assertEquals(bean.types[1], Type.High);
        assertEquals(bean.list.size(), 3);
        assertEquals(bean.list.get(0), 1);
        assertEquals(bean.list.get(1), 2);
        assertEquals(bean.list.get(2), 1308147);
    }

    @Test
    public void testObjectArrayBuilder() throws Exception {
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();
        Builder<Object[]> builder = Builder.register(Object[].class);

        Object[] obj = new Object[5];
        obj[0] = "1234";
        obj[1] = new Double(109.23);
        obj[2] = "3455";
        obj[3] = null;
        obj[4] = Boolean.TRUE;

        builder.writeTo(obj, os);
        byte[] b = os.toByteArray();
        System.out.println("Object array:" + b.length + ":" + Bytes.bytes2hex(b));

        Assert.assertArrayEquals(obj, builder.parseFrom(b));
    }

    // FIXME MyList的从ArrayList中继承来的属性size会在decode时设置好，再Add时就不对了！！
    @Ignore
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testBuilder_MyList() throws Exception {
        Builder<MyList> b1 = Builder.register(MyList.class);
        MyList list = new MyList();
        list.add(new boolean[]{true, false});
        list.add(new int[]{1, 2, 3, 4, 5});
        list.add("String");
        list.add(4);
        list.code = 4321;

        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();
        b1.writeTo(list, os);
        byte[] b = os.toByteArray();
        System.out.println(b.length + ":" + Bytes.bytes2hex(b));
        MyList result = b1.parseFrom(b);

        assertEquals(4, result.size());
        assertEquals(result.code, 4321);
        assertEquals(result.id, "feedback");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testBuilder_MyMap() throws Exception {
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();
        Builder<MyMap> b2 = Builder.register(MyMap.class);
        MyMap map = new MyMap();
        map.put("name", "qianlei");
        map.put("displayName", "钱磊");
        map.code = 4321;
        b2.writeTo(map, os);
        byte[] b = os.toByteArray();
        System.out.println(b.length + ":" + Bytes.bytes2hex(b));

        map = b2.parseFrom(b);

        assertEquals(map.size(), 2);
        assertEquals(map.code, 4321);
        assertEquals(map.id, "feedback");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSerializableBean() throws Exception {
        System.out.println("testSerializableBean");
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();

        SerializableBean sb = new SerializableBean();
        Builder<SerializableBean> sbb = Builder.register(SerializableBean.class);
        sbb.writeTo(sb, os);

        byte[] b = os.toByteArray();
        System.out.println(b.length + ":" + Bytes.bytes2hex(b));
        assertEquals(sbb.parseFrom(os.toByteArray()), sb);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOthers() throws Exception {
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 1024 * 32 + 32; i++)
            buf.append('A');
        Builder<String> sb = Builder.register(String.class);
        sb.writeTo(buf.toString(), os);
        assertEquals(sb.parseFrom(os.toByteArray()), buf.toString());

        os = new UnsafeByteArrayOutputStream();
        Builder<HashMap> builder = Builder.register(HashMap.class);
        Map services = new HashMap();
        HashMap map = new HashMap();
        services.put("test.service", "http://127.0.0.1:9010/test.service");
        map.put("name", "qianlei");
        map.put("password", "123455");
        map.put("services", services);

        builder.writeTo(map, os);
        byte[] b = os.toByteArray();
        System.out.println(b.length + ":" + Bytes.bytes2hex(b));
        map = builder.parseFrom(b);
        assertTrue(map.size() > 0);
        assertEquals("http://127.0.0.1:9010/test.service", ((Map) map.get("services")).get("test.service"));

        services = new ConcurrentHashMap();
        services.put("test.service", "http://127.0.0.1:9010/test.service");
        map.put("services", services);

        os = new UnsafeByteArrayOutputStream();
        builder.writeTo(map, os);
        b = os.toByteArray();
        System.out.println(b.length + ":" + Bytes.bytes2hex(b));
        map = builder.parseFrom(b);
        assertTrue(map.size() > 0);
        assertEquals("http://127.0.0.1:9010/test.service", ((Map) map.get("services")).get("test.service"));

        Node node1 = new Node();
        Node node0 = new Node();
        node0.value = "0";
        node0.next = node1;
        node1.value = "1";
        node1.prev = node0;
        // write.
        Builder<Node> nodebuilder = Builder.register(Node.class);
        os = new UnsafeByteArrayOutputStream();
        nodebuilder.writeTo(node0, os);
        b = os.toByteArray();
        System.out.println("Node:" + b.length + ":" + Bytes.bytes2hex(b));
        // parse
        node0 = nodebuilder.parseFrom(b);
        assertEquals(node0, node0.prev);
        assertEquals(node0, node0.next.prev);
        assertEquals(node0.value, "0");
    }

    @Test
    public void testWithFC() throws Exception {
        Builder<SimpleDO> builder = Builder.register(SimpleDO.class);
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream();

        SimpleDO sd = new SimpleDO();
        sd.a = 1;
        sd.b = 2;
        sd.c = 3;
        sd.str1 = "12345";
        sd.str2 = "54321";
        builder.writeTo(sd, os);
        byte[] b = os.toByteArray();
        System.out.println(b.length + ":" + Bytes.bytes2hex(b));

        sd = builder.parseFrom(b);
        assertEquals(sd.a, 1);
        assertEquals(sd.b, 2);
        assertEquals(sd.c, 3);
        assertEquals(sd.str1, "124");
        System.out.println(sd.str2);
    }

    public enum Type {
        Lower, Normal, High;
    }

    static interface TestDO {
        String getName();

        void setName(String name);

        Type getType();

        void setType(Type t);

        int getArg();

        void setArg(int arg);
    }

    static class TestDOImpl implements TestDO, Serializable {
        private static final long serialVersionUID = 1L;

        public String getName() {
            return "name";
        }

        public void setName(String name) {
        }

        public Type getType() {
            return Type.High;
        }

        public void setType(Type t) {
        }

        public int getArg() {
            return 28;
        }

        public void setArg(int arg) {
        }
    }

    static class Bean implements Serializable {
        private static final long serialVersionUID = 1L;
        public int vi = 0;
        public long vl = 100l;

        boolean b = true;
        boolean[] bs = {false, true};

        String s1 = "1234567890";
        String s2 = "1234567890一二三四五六七八九零";

        private int i = 123123, ni = -12344, is[] = {1, 2, 3, 4, -1, -2, -3, -4};
        private short s = 12, ns = -76;
        private double d = 12.345, nd = -12.345;
        private long l = 1281447759383l, nl = -13445l;

        private Boolean B = Boolean.FALSE;
        private Integer I = -1234;
        private Double D = new Double(1.23);
        private String name = "qianlei";
        private Type type = Type.Lower, type1 = Type.Normal;
        private Type[] types = {Type.Lower, Type.Lower};

        private Time time = null;
        private ArrayList list = new ArrayList();

        {
            list.add(1);
            list.add(2);
            list.add(1308147);
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }
    }

    static class MyList<T> extends ArrayList<T> {
        private int code = 12345;
        private String id = "feedback";
    }

    static class MyMap<K, V> extends HashMap<K, V> {
        private int code = 12345;
        private String id = "feedback";
    }

    static class Node implements Serializable {
        private static final long serialVersionUID = 1L;
        Node prev = this;
        Node next = this;
        String value = "value";

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Node other = (Node) obj;
            if (value == null) {
                if (other.value != null) return false;
            } else if (!value.equals(other.value)) return false;
            return true;
        }
    }

    static class SerializableBean implements Serializable {
        private static final long serialVersionUID = -8949681707161463700L;

        public int a = 0;
        public long b = 100l;
        boolean c = true;
        String s1 = "1234567890";
        String s2 = "1234567890一二三四五六七八九零";

        public int hashCode() {
            return s1.hashCode() ^ s2.hashCode();
        }

        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj == this) return true;
            if (obj instanceof SerializableBean) {
                SerializableBean sb = (SerializableBean) obj;
                return this.a == sb.a && this.b == sb.b && this.c == sb.c && this.s1.equals(sb.s1) && this.s2.equals(sb.s2);
            }

            return false;
        }
    }
}