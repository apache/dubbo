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

import org.apache.dubbo.common.json.impl.FastJson2Impl;
import org.apache.dubbo.common.json.impl.FastJsonImpl;
import org.apache.dubbo.common.json.impl.GsonImpl;
import org.apache.dubbo.common.json.impl.JacksonImpl;
import org.apache.dubbo.common.utils.json.TestEnum;
import org.apache.dubbo.common.utils.json.TestObjectA;
import org.apache.dubbo.common.utils.json.TestObjectB;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

class JsonUtilsTest {
    private AtomicBoolean allowFastjson2 = new AtomicBoolean(true);
    private AtomicBoolean allowFastjson = new AtomicBoolean(true);
    private AtomicBoolean allowGson = new AtomicBoolean(true);
    private AtomicBoolean allowJackson = new AtomicBoolean(true);
    private MockedConstruction<FastJson2Impl> fastjson2Mock;
    private MockedConstruction<FastJsonImpl> fastjsonMock;
    private MockedConstruction<GsonImpl> gsonMock;
    private MockedConstruction<JacksonImpl> jacksonMock;

    @AfterEach
    void teardown() {
        if (fastjsonMock != null) {
            fastjsonMock.close();
        }
        if (fastjson2Mock != null) {
            fastjson2Mock.close();
        }
        if (gsonMock != null) {
            gsonMock.close();
        }
        if (jacksonMock != null) {
            jacksonMock.close();
        }
    }

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
    void consistentTest() {
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
        fastjson2Mock = Mockito.mockConstruction(FastJson2Impl.class,
            (mock, context) -> Mockito.when(mock.isSupport()).thenAnswer(invocation -> allowFastjson2.get()));
        fastjsonMock = Mockito.mockConstruction(FastJsonImpl.class,
            (mock, context) -> Mockito.when(mock.isSupport()).thenAnswer(invocation -> allowFastjson.get()));
        gsonMock = Mockito.mockConstruction(GsonImpl.class,
            (mock, context) -> Mockito.when(mock.isSupport()).thenAnswer(invocation -> allowGson.get()));
        jacksonMock = Mockito.mockConstruction(JacksonImpl.class,
            (mock, context) -> Mockito.when(mock.isSupport()).thenAnswer(invocation -> allowJackson.get()));

        // default use fastjson2
        JsonUtils.setJson(null);
        Assertions.assertInstanceOf(FastJson2Impl.class, JsonUtils.getJson());

        // prefer use fastjson2
        JsonUtils.setJson(null);
        System.setProperty("dubbo.json-framework.prefer", "fastjson2");
        Assertions.assertInstanceOf(FastJson2Impl.class, JsonUtils.getJson());

        // prefer use fastjson
        JsonUtils.setJson(null);
        System.setProperty("dubbo.json-framework.prefer", "fastjson");
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");

        // prefer use gson
        JsonUtils.setJson(null);
        System.setProperty("dubbo.json-framework.prefer", "gson");
        Assertions.assertInstanceOf(GsonImpl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");

        // prefer use not found
        JsonUtils.setJson(null);
        System.setProperty("dubbo.json-framework.prefer", "notfound");
        Assertions.assertInstanceOf(FastJson2Impl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");

        JsonUtils.setJson(null);
        // TCCL not found fastjson2
        allowFastjson2.set(false);
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());
        allowFastjson2.set(true);

        JsonUtils.setJson(null);
        // TCCL not found fastjson2, fastjson
        allowFastjson2.set(false);
        allowFastjson.set(false);
        Assertions.assertInstanceOf(GsonImpl.class, JsonUtils.getJson());
        allowFastjson.set(true);
        allowFastjson2.set(true);

        JsonUtils.setJson(null);
        // TCCL not found fastjson2, fastjson, gson
        allowFastjson2.set(false);
        allowFastjson.set(false);
        allowGson.set(false);
        Assertions.assertInstanceOf(JacksonImpl.class, JsonUtils.getJson());
        allowGson.set(true);
        allowFastjson.set(true);
        allowFastjson2.set(true);

        JsonUtils.setJson(null);
        // TCCL not found fastjson2, prefer use fastjson2
        allowFastjson2.set(false);
        System.setProperty("dubbo.json-framework.prefer", "fastjson2");
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");
        allowFastjson2.set(true);

        JsonUtils.setJson(null);
        // TCCL not found fastjson, prefer use fastjson
        allowFastjson.set(false);
        System.setProperty("dubbo.json-framework.prefer", "fastjson");
        Assertions.assertInstanceOf(FastJson2Impl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");
        allowFastjson.set(true);

        JsonUtils.setJson(null);
        // TCCL not found gson, prefer use gson
        allowGson.set(false);
        System.setProperty("dubbo.json-framework.prefer", "gson");
        Assertions.assertInstanceOf(FastJson2Impl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");
        allowGson.set(true);

        JsonUtils.setJson(null);
        // TCCL not found jackson, prefer use jackson
        allowJackson.set(false);
        System.setProperty("dubbo.json-framework.prefer", "jackson");
        Assertions.assertInstanceOf(FastJson2Impl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");
        allowJackson.set(true);

        JsonUtils.setJson(null);
        // TCCL not found fastjson, gson
        allowFastjson2.set(false);
        allowFastjson.set(false);
        allowGson.set(false);
        allowJackson.set(false);
        Assertions.assertThrows(IllegalStateException.class, JsonUtils::getJson);
        allowGson.set(true);
        allowFastjson.set(true);
        allowFastjson2.set(true);
        allowJackson.set(true);

        JsonUtils.setJson(null);
    }
}
