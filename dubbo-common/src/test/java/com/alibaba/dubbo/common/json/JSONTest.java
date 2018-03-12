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
package com.alibaba.dubbo.common.json;

import junit.framework.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@Deprecated
public class JSONTest {
    static byte[] DEFAULT_BYTES = {3, 12, 14, 41, 12, 2, 3, 12, 4, 67, 23};
    static int DEFAULT_$$ = 152;

    @Test
    public void testException() throws Exception {
        MyException e = new MyException("001", "AAAAAAAA");

        StringWriter writer = new StringWriter();
        JSON.json(e, writer);
        String json = writer.getBuffer().toString();
        System.out.println(json);
        // Assert.assertEquals("{\"code\":\"001\",\"message\":\"AAAAAAAA\"}", json);

        StringReader reader = new StringReader(json);
        MyException result = JSON.parse(reader, MyException.class);
        Assert.assertEquals("001", result.getCode());
        Assert.assertEquals("AAAAAAAA", result.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMap() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("aaa", "bbb");

        StringWriter writer = new StringWriter();
        JSON.json(map, writer);
        String json = writer.getBuffer().toString();
        Assert.assertEquals("{\"aaa\":\"bbb\"}", json);

        StringReader reader = new StringReader(json);
        Map<String, String> result = JSON.parse(reader, Map.class);
        Assert.assertEquals("bbb", result.get("aaa"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMapArray() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("aaa", "bbb");

        StringWriter writer = new StringWriter();
        JSON.json(new Object[]{map}, writer); // args
        String json = writer.getBuffer().toString();
        Assert.assertEquals("[{\"aaa\":\"bbb\"}]", json);

        StringReader reader = new StringReader(json);
        Object[] result = JSON.parse(reader, new Class<?>[]{Map.class});
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("bbb", ((Map<String, String>) result[0]).get("aaa"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLinkedMap() throws Exception {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("aaa", "bbb");

        StringWriter writer = new StringWriter();
        JSON.json(map, writer);
        String json = writer.getBuffer().toString();
        Assert.assertEquals("{\"aaa\":\"bbb\"}", json);

        StringReader reader = new StringReader(json);
        LinkedHashMap<String, String> result = JSON.parse(reader, LinkedHashMap.class);
        Assert.assertEquals("bbb", result.get("aaa"));
    }

    @Test
    public void testObject2Json() throws Exception {
        Bean bean = new Bean();
        bean.array = new int[]{1, 3, 4};
        bean.setName("ql");

        String json = JSON.json(bean);
        bean = JSON.parse(json, Bean.class);
        assertEquals(bean.getName(), "ql");
        assertEquals(bean.getDisplayName(), "钱磊");
        assertEquals(bean.bytes.length, DEFAULT_BYTES.length);
        assertEquals(bean.$$, DEFAULT_$$);

        assertEquals("{\"name\":\"ql\",\"array\":[1,3,4]}", JSON.json(bean, new String[]{"name", "array"}));
    }

    @Test
    public void testParse2JSONObject() throws Exception {
        JSONObject jo = (JSONObject) JSON.parse("{name:'qianlei',array:[1,2,3,4,98.123],b1:TRUE,$1:NULL,$2:FALSE,__3:NULL}");
        assertEquals(jo.getString("name"), "qianlei");
        assertEquals(jo.getArray("array").length(), 5);
        assertEquals(jo.get("$2"), Boolean.FALSE);
        assertEquals(jo.get("__3"), null);

        for (int i = 0; i < 10000; i++)
            JSON.parse("{\"name\":\"qianlei\",\"array\":[1,2,3,4,98.123],\"displayName\":\"钱磊\"}");

        long now = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++)
            JSON.parse("{\"name\":\"qianlei\",\"array\":[1,2,3,4,98.123],\"displayName\":\"钱磊\"}");
        System.out.println("parse to JSONObject 10000 times in: " + (System.currentTimeMillis() - now));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testParse2Class() throws Exception {
        int[] o1 = {1, 2, 3, 4, 5}, o2 = JSON.parse("[1.2,2,3,4,5]", int[].class);
        assertEquals(o2.length, 5);
        for (int i = 0; i < 5; i++)
            assertEquals(o1[i], o2[i]);

        List l1 = (List) JSON.parse("[1.2,2,3,4,5]", List.class);
        assertEquals(l1.size(), 5);
        for (int i = 0; i < 5; i++)
            assertEquals(o1[i], ((Number) l1.get(i)).intValue());

        Bean bean = JSON.parse("{name:'qianlei',array:[1,2,3,4,98.123],displayName:'钱磊',$$:214726,$b:TRUE}", Bean.class);
        assertEquals(bean.getName(), "qianlei");
        assertEquals(bean.getDisplayName(), "钱磊");
        assertEquals(bean.array.length, 5);
        assertEquals(bean.$$, 214726);
        assertEquals(bean.$b, true);

        for (int i = 0; i < 10000; i++)
            JSON.parse("{name:'qianlei',array:[1,2,3,4,98.123],displayName:'钱磊'}", Bean1.class);

        long now = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++)
            JSON.parse("{name:'qianlei',array:[1,2,3,4,98.123],displayName:'钱磊'}", Bean1.class);
        System.out.println("parse to Class 10000 times in: " + (System.currentTimeMillis() - now));
    }

    @Test
    public void testParse2Arguments() throws Exception {
        Object[] test = JSON.parse("[1.2, 2, {name:'qianlei',array:[1,2,3,4,98.123]} ]", new Class<?>[]{int.class, int.class, Bean.class});
        assertEquals(test[1], 2);
        assertEquals(test[2].getClass(), Bean.class);
        test = JSON.parse("[1.2, 2]", new Class<?>[]{int.class, int.class});
        assertEquals(test[0], 1);
    }

    public static class Bean1 {
        public int[] array;
        private String name, displayName;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Bean {
        public int[] array;
        public boolean $b;
        public int $$ = DEFAULT_$$;
        public byte[] bytes = DEFAULT_BYTES;
        private String name, displayName = "钱磊";

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}