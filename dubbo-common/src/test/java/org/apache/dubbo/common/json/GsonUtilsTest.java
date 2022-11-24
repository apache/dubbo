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
package org.apache.dubbo.common.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class GsonUtilsTest {
    @Test
    void test1() {
        Object user = GsonUtils.fromJson("{'name':'Tom','age':24}", User.class);
        Assertions.assertInstanceOf(User.class, user);
        Assertions.assertEquals("Tom", ((User) user).getName());
        Assertions.assertEquals(24, ((User) user).getAge());

        try {
            GsonUtils.fromJson("{'name':'Tom','age':}", User.class);
            Assertions.fail();
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Generic serialization [gson] Json syntax exception thrown when parsing (message:{'name':'Tom','age':} type:class org.apache.dubbo.common.json.GsonUtilsTest$User) error:com.google.gson.stream.MalformedJsonException: Expected value at line 1 column 21 path $.age", ex.getMessage());
        }
    }

    @Test
    void test2() {
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

        // TCCL not found gson
        removedPackages.set(Collections.singletonList("com.google.gson"));
        GsonUtils.setSupportGson(null);
        try {
            GsonUtils.fromJson("{'name':'Tom','age':24}", User.class);
            Assertions.fail();
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Gson is not supported. Please import Gson in JVM env.", ex.getMessage());
        }

        Thread.currentThread().setContextClassLoader(originClassLoader);
        GsonUtils.setSupportGson(null);
    }

    private static class User {
        String name;
        int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}