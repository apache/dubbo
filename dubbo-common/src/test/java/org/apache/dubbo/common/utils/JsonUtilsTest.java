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

import org.apache.dubbo.common.json.impl.FastJsonImpl;
import org.apache.dubbo.common.json.impl.GsonImpl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class JsonUtilsTest {
    @Test
    public void testGetJson1() {
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
    public void testGetJson2() {
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

        // default use fastjson
        JsonUtils.setJson(null);
        removedPackages.set(Collections.emptyList());
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());

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
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");

        JsonUtils.setJson(null);
        // TCCL not found fastjson
        removedPackages.set(Collections.singletonList("com.alibaba.fastjson"));
        Assertions.assertInstanceOf(GsonImpl.class, JsonUtils.getJson());

        JsonUtils.setJson(null);
        // TCCL not found gson
        removedPackages.set(Collections.singletonList("com.google.gson"));
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());

        JsonUtils.setJson(null);
        // TCCL not found fastjson, prefer use fastjson
        removedPackages.set(Collections.singletonList("com.alibaba.fastjson"));
        System.setProperty("dubbo.json-framework.prefer", "fastjson");
        Assertions.assertInstanceOf(GsonImpl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");

        JsonUtils.setJson(null);
        // TCCL not found gson, prefer use gson
        removedPackages.set(Collections.singletonList("com.google.gson"));
        System.setProperty("dubbo.json-framework.prefer", "gson");
        Assertions.assertInstanceOf(FastJsonImpl.class, JsonUtils.getJson());
        System.clearProperty("dubbo.json-framework.prefer");

        JsonUtils.setJson(null);
        // TCCL not found fastjson, gson
        removedPackages.set(Arrays.asList("com.alibaba.fastjson", "com.google.gson"));
        Assertions.assertThrows(IllegalStateException.class, JsonUtils::getJson);

        Thread.currentThread().setContextClassLoader(originClassLoader);
        JsonUtils.setJson(null);
    }
}
