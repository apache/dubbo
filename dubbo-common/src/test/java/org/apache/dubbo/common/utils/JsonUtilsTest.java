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
package org.apache.dubbo.common.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.dubbo.common.json.impl.FastJson2Impl;
import org.apache.dubbo.common.json.impl.FastJsonImpl;
import org.apache.dubbo.common.json.impl.GsonImpl;
import org.apache.dubbo.common.json.impl.JacksonImpl;
import org.apache.dubbo.common.utils.json.TestEnum;
import org.apache.dubbo.common.utils.json.TestObjectA;
import org.apache.dubbo.common.utils.json.TestObjectB;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JsonUtilsTest {

    @Test
    void testGetJson1() {
        Assertions.assertNotNull(JsonUtils.getJson());
        Assertions.assertEquals(JsonUtils.getJson(), JsonUtils.getJson());

        Map<String, String> map = new HashMap<>();
        map.put("a", "a");
        Assertions.assertEquals("{\"a\":\"a\"}", JsonUtils.getJson().toJson(map));
        Assertions.assertEquals(map, JsonUtils.getJson().toJavaObject("{\"a\":\"a\"}", Map.class));
        Assertions.assertEquals(Collections.singletonList(map), JsonUtils.getJson().toJavaList("[{\"a\":\"a\"}]", Map.class));

        // prefer use fastjson2
        JsonUtils.setJson(null);
        System.setProperty("dubbo.json-framework.prefer", "fastjson2");
        Assertions.assertEquals("{\"a\":\"a\"}", JsonUtils.getJson().toJson(map));
        Assertions.assertEquals(map, JsonUtils.getJson().toJavaObject("{\"a\":\"a\"}", Map.class));
        Assertions.assertEquals(Collections.singletonList(map), JsonUtils.getJson().toJavaList("[{\"a\":\"a\"}]", Map.class));
        System.clearProperty("dubbo.json-framework.prefer");

        // prefer use fastjson
        JsonUtils.setJson(null);
        System.setProperty("dubbo.json-framework.prefer", "fastjson");
        Assertions.assertEquals("{\"a\":\"a\"}", JsonUtils.getJson().toJson(map));
        Assertions.assertEquals(map, JsonUtils.getJson().toJavaObject("{\"a\":\"a\"}", Map.class));
        Assertions.assertEquals(Collections.singletonList(map), JsonUtils.getJson().toJavaList("[{\"a\":\"a\"}]", Map.class));
        System.clearProperty("dubbo.json-framework.prefer");

        // prefer use gson
        JsonUtils.setJson(null);
        System.setProperty("dubbo.json-framework.prefer", "gson");
        Assertions.assertEquals("{\"a\":\"a\"}", JsonUtils.getJson().toJson(map));
        Assertions.assertEquals(map, JsonUtils.getJson().toJavaObject("{\"a\":\"a\"}", Map.class));
        Assertions.assertEquals(Collections.singletonList(map), JsonUtils.getJson().toJavaList("[{\"a\":\"a\"}]", Map.class));
        System.clearProperty("dubbo.json-framework.prefer");

        // prefer use jackson
        JsonUtils.setJson(null);
        System.setProperty("dubbo.json-framework.prefer", "jackson");
        Assertions.assertEquals("{\"a\":\"a\"}", JsonUtils.getJson().toJson(map));
        Assertions.assertEquals(map, JsonUtils.getJson().toJavaObject("{\"a\":\"a\"}", Map.class));
        Assertions.assertEquals(Collections.singletonList(map), JsonUtils.getJson().toJavaList("[{\"a\":\"a\"}]", Map.class));
        System.clearProperty("dubbo.json-framework.prefer");

        JsonUtils.setJson(null);
    }

    @Test
    public void consistentTest() {
        List<Object> objs = new LinkedList<>();

        {
            objs.add(null);
        }

        {
            Map<String, String> map = new HashMap<>();
            map.put("a", "a");
            objs.add(map);
        }

        {
            TestObjectA a = new TestObjectA();
            objs.add(a);
        }

        {
            TestObjectA a = new TestObjectA();
            a.setTestEnum(TestEnum.TYPE_A);
            objs.add(a);
        }

        {
            TestObjectB b = new TestObjectB();
            objs.add(b);
        }

        {
            TestObjectB b = new TestObjectB();
            b.setInnerA(new TestObjectB.Inner());
            b.setInnerB(new TestObjectB.Inner());
            objs.add(b);
        }

        {
            TestObjectB b = new TestObjectB();
            TestObjectB.Inner inner1 = new TestObjectB.Inner();
            TestObjectB.Inner inner2 = new TestObjectB.Inner();
            inner1.setName("Test");
            inner2.setName("Test");
            b.setInnerA(inner1);
            b.setInnerB(inner2);
            objs.add(b);
        }

        {
            TestObjectB b = new TestObjectB();
            TestObjectB.Inner inner1 = new TestObjectB.Inner();
            inner1.setName("Test");
            b.setInnerA(inner1);
            b.setInnerB(inner1);
            objs.add(b);
        }

        for (Object obj : objs) {

            // prefer use fastjson2
            JsonUtils.setJson(null);
            System.setProperty("dubbo.json-framework.prefer", "fastjson2");
            Assertions.assertInstanceOf(FastJson2Impl.class, JsonUtils.getJson());
            String fromFastjson2 = JsonUtils.getJson().toJson(obj);
            System.clearProperty("dubbo.json-framework.prefer");

            // prefer use fastjson
            JsonUtils.setJson(null);
            System.setProperty("dubbo.json-framework.prefer", "fastjson");
            Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());
            String fromFastjson1 = JsonUtils.getJson().toJson(obj);
            System.clearProperty("dubbo.json-framework.prefer");

            // prefer use gson
            JsonUtils.setJson(null);
            System.setProperty("dubbo.json-framework.prefer", "gson");
            Assertions.assertInstanceOf(GsonImpl.class, JsonUtils.getJson());
            String fromGson = JsonUtils.getJson().toJson(obj);
            System.clearProperty("dubbo.json-framework.prefer");

            // prefer use jackson
            JsonUtils.setJson(null);
            System.setProperty("dubbo.json-framework.prefer", "jackson");
            Assertions.assertInstanceOf(JacksonImpl.class, JsonUtils.getJson());
            String fromJackson = JsonUtils.getJson().toJson(obj);
            System.clearProperty("dubbo.json-framework.prefer");

            JsonUtils.setJson(null);

            Assertions.assertEquals(fromFastjson1, fromFastjson2);
            Assertions.assertEquals(fromFastjson1, fromGson);
            Assertions.assertEquals(fromFastjson2, fromGson);
            Assertions.assertEquals(fromFastjson1, fromJackson);
            Assertions.assertEquals(fromFastjson2, fromJackson);
        }
    }

    @Test
    void testGetJson2() {
        ClassLoader originClassLoader = Thread.currentThread().getContextClassLoader();
        AtomicReference<List<String>> removedPackages = new AtomicReference<>(Collections.emptyList());
        ClassLoader newClassLoader = new ClassLoader(originClassLoader) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                for (String removedPackage : removedPackages.get()) {
                    if (name.startsWith(removedPackage)) {
                        throw new ClassNotFoundException("Test");
                    }
                }
                return super.loadClass(name);
            }
        };
        Thread.currentThread().setContextClassLoader(newClassLoader);

        // default use fastjson2
        JsonUtils.setJson(null);
        removedPackages.set(Collections.emptyList());
        Assertions.assertInstanceOf(FastJson2Impl.class, JsonUtils.getJson());

        // prefer use fastjson2
        JsonUtils.setJson(null);
        removedPackages.set(Collections.emptyList());
        System.setProperty("dubbo.json-framework.prefer", "fastjson2");
        Assertions.assertInstanceOf(FastJson2Impl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");

        // prefer use fastjson
        JsonUtils.setJson(null);
        removedPackages.set(Collections.emptyList());
        System.setProperty("dubbo.json-framework.prefer", "fastjson");
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");

        // prefer use gson
        JsonUtils.setJson(null);
        removedPackages.set(Collections.emptyList());
        System.setProperty("dubbo.json-framework.prefer", "gson");
        Assertions.assertInstanceOf(GsonImpl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");

        // prefer use not found
        JsonUtils.setJson(null);
        removedPackages.set(Collections.emptyList());
        System.setProperty("dubbo.json-framework.prefer", "notfound");
        Assertions.assertInstanceOf(FastJson2Impl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");

        JsonUtils.setJson(null);
        // TCCL not found fastjson2
        removedPackages.set(Collections.singletonList("com.alibaba.fastjson2"));
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());

        JsonUtils.setJson(null);
        // TCCL not found fastjson
        removedPackages.set(Arrays.asList("com.alibaba.fastjson2", "com.alibaba.fastjson"));
        Assertions.assertInstanceOf(GsonImpl.class, JsonUtils.getJson());

        JsonUtils.setJson(null);
        // TCCL not found gson
        removedPackages.set(Arrays.asList("com.alibaba.fastjson2", "com.google.gson"));
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());

        JsonUtils.setJson(null);
        // TCCL not found fastjson2, prefer use fastjson
        removedPackages.set(Collections.singletonList("com.alibaba.fastjson2"));
        System.setProperty("dubbo.json-framework.prefer", "fastjson");
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");

        JsonUtils.setJson(null);
        // TCCL not found fastjson, prefer use fastjson
        removedPackages.set(Arrays.asList("com.alibaba.fastjson2", "com.alibaba.fastjson"));
        System.setProperty("dubbo.json-framework.prefer", "fastjson");
        Assertions.assertInstanceOf(GsonImpl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");

        JsonUtils.setJson(null);
        // TCCL not found gson, prefer use gson
        removedPackages.set(Arrays.asList("com.alibaba.fastjson2", "com.google.gson"));
        System.setProperty("dubbo.json-framework.prefer", "gson");
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");

        JsonUtils.setJson(null);
        // TCCL not found fastjson, gson, prefer use jackson
        removedPackages.set(Arrays.asList("com.alibaba.fastjson2", "com.alibaba.fastjson", "com.google.gson"));
        Assertions.assertInstanceOf(JacksonImpl.class, JsonUtils.getJson());

        JsonUtils.setJson(null);
        // TCCL not found fastjson, gson, jackson
        removedPackages.set(Arrays.asList("com.alibaba.fastjson2", "com.alibaba.fastjson", "com.google.gson", "com.fasterxml.jackson.databind"));
        Assertions.assertThrows(IllegalStateException.class, JsonUtils::getJson);

        Thread.currentThread().setContextClassLoader(originClassLoader);
        JsonUtils.setJson(null);
    }
}
