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

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.dubbo.common.json.impl.FastJsonImpl;
import org.apache.dubbo.common.json.impl.GsonImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;

import static org.mockito.Answers.CALLS_REAL_METHODS;


class JsonUtilsTest {
    private static Gson gson = new Gson();
    private static MockedStatic<JSON> fastjsonMock;
    private static AtomicReference<Gson> gsonReference = new AtomicReference<>();
    private static MockedConstruction<Gson> gsonMock;
    private static AtomicReference<Consumer<Gson>> gsonInit = new AtomicReference<>();

    @BeforeAll
    static void setup() {
        fastjsonMock = Mockito.mockStatic(JSON.class, CALLS_REAL_METHODS);
        gsonMock = Mockito.mockConstruction(Gson.class,
            (mock, context) -> {
                gsonReference.set(mock);
                Mockito.when(mock.toJson((Object) Mockito.any())).thenAnswer(invocation -> gson.toJson((Object) invocation.getArgument(0)));
                Mockito.when(mock.fromJson(Mockito.anyString(), (Type) Mockito.any())).thenAnswer(invocation -> gson.fromJson((String) invocation.getArgument(0), (Type) invocation.getArgument(1)));
                Consumer<Gson> gsonConsumer = gsonInit.get();
                if (gsonConsumer != null) {
                    gsonConsumer.accept(mock);
                }
            });
    }

    @AfterAll
    static void teardown() {
        fastjsonMock.close();
        gsonMock.close();
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

        JsonUtils.setJson(null);
    }

    @Test
    void testGetJson2() {
        // default use fastjson
        JsonUtils.setJson(null);
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());

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
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");

        JsonUtils.setJson(null);
        // TCCL not found fastjson
        fastjsonMock.when(() -> JSON.toJSONString(Mockito.any(), Mockito.any())).thenThrow(new RuntimeException());
        Assertions.assertInstanceOf(GsonImpl.class, JsonUtils.getJson());
        fastjsonMock.reset();

        JsonUtils.setJson(null);
        // TCCL not found gson
        gsonInit.set(mock -> Mockito.reset(mock));
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());
        gsonInit.set(null);

        JsonUtils.setJson(null);
        // TCCL not found fastjson, prefer use fastjson
        fastjsonMock.when(() -> JSON.toJSONString(Mockito.any(), Mockito.any())).thenThrow(new RuntimeException());
        System.setProperty("dubbo.json-framework.prefer", "fastjson");
        Assertions.assertInstanceOf(GsonImpl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");
        fastjsonMock.reset();

        JsonUtils.setJson(null);
        // TCCL not found gson, prefer use gson
        gsonInit.set(mock -> Mockito.reset(mock));
        System.setProperty("dubbo.json-framework.prefer", "gson");
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");
        gsonInit.set(null);

        JsonUtils.setJson(null);
        // TCCL not found fastjson, gson
        fastjsonMock.when(() -> JSON.toJSONString((Object) Mockito.any(), Mockito.any())).thenThrow(new RuntimeException());
        gsonInit.set(mock -> Mockito.reset(mock));
        Assertions.assertThrows(IllegalStateException.class, JsonUtils::getJson);
        gsonInit.set(null);
        fastjsonMock.reset();

        JsonUtils.setJson(null);
    }
}
