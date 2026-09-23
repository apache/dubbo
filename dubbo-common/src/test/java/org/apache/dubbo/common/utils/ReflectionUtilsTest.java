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

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReflectionUtilsTest {

    static class TestService implements Comparable<String> {
        private String secret = "hidden_treasure";

        private String sayHello(String name) {
            return "Hello, " + name;
        }

        @Override
        public int compareTo(String o) {
            return 0;
        }
    }

    @Test
    void testGetField() {
        TestService service = new TestService();
        // Even though 'secret' is private, ReflectionUtils should find it
        Object value = ReflectionUtils.getField(service, "secret");
        assertEquals("hidden_treasure", value);
    }

    @Test
    void testInvoke() {
        TestService service = new TestService();
        // Running a private method 'sayHello' with one parameter
        Object result = ReflectionUtils.invoke(service, "sayHello", "Dubbo");
        assertEquals("Hello, Dubbo", result);
    }

    @Test
    void testGetClassGenerics() {
        //  checks if the utility can see that TestService uses <String>
        List<Class<?>> generics = ReflectionUtils.getClassGenerics(TestService.class, Comparable.class);

        assertNotNull(generics);
        assertFalse(generics.isEmpty());
        assertEquals(String.class, generics.get(0));
    }

    @Test
    void testGetFieldThrowsException() {
        TestService service = new TestService();
        // Testing that it fails correctly when the field name is wrong
        assertThrows(ReflectionUtils.ReflectionException.class, () -> {
            ReflectionUtils.getField(service, "nonExistentField");
        });
    }
}
