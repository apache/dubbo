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
package org.apache.dubbo.common.compiler.support;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

public class JdkCompilerTest extends JavaCodeTest {

    @Test
    public void test_compileJavaClass() throws Exception {
        JdkCompiler compiler = new JdkCompiler();
        Class<?> clazz = compiler.compile(getSimpleCode(), JdkCompiler.class.getClassLoader());
        Object instance = clazz.newInstance();
        Method sayHello = instance.getClass().getMethod("sayHello");
        Assertions.assertEquals("Hello world!", sayHello.invoke(instance));
    }

    @Test
    public void test_compileJavaClass0() throws Exception {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            JdkCompiler compiler = new JdkCompiler();
            Class<?> clazz = compiler.compile(getSimpleCodeWithoutPackage(), JdkCompiler.class.getClassLoader());
            Object instance = clazz.newInstance();
            Method sayHello = instance.getClass().getMethod("sayHello");
            Assertions.assertEquals("Hello world!", sayHello.invoke(instance));
        });
    }

    @Test
    public void test_compileJavaClass1() throws Exception {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            JdkCompiler compiler = new JdkCompiler();
            Class<?> clazz = compiler.compile(getSimpleCodeWithSyntax(), JdkCompiler.class.getClassLoader());
            Object instance = clazz.newInstance();
            Method sayHello = instance.getClass().getMethod("sayHello");
            Assertions.assertEquals("Hello world!", sayHello.invoke(instance));
        });
    }

    @Test
    public void test_compileJavaClass_java8() throws Exception {
        JdkCompiler compiler = new JdkCompiler("1.8");
        Class<?> clazz = compiler.compile(getSimpleCode(), JdkCompiler.class.getClassLoader());
        Object instance = clazz.newInstance();
        Method sayHello = instance.getClass().getMethod("sayHello");
        Assertions.assertEquals("Hello world!", sayHello.invoke(instance));
    }

    @Test
    public void test_compileJavaClass0_java8() throws Exception {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            JdkCompiler compiler = new JdkCompiler("1.8");
            Class<?> clazz = compiler.compile(getSimpleCodeWithoutPackage(), JdkCompiler.class.getClassLoader());
            Object instance = clazz.newInstance();
            Method sayHello = instance.getClass().getMethod("sayHello");
            Assertions.assertEquals("Hello world!", sayHello.invoke(instance));
        });
    }

    @Test
    public void test_compileJavaClass1_java8() throws Exception {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            JdkCompiler compiler = new JdkCompiler("1.8");
            Class<?> clazz = compiler.compile(getSimpleCodeWithSyntax(), JdkCompiler.class.getClassLoader());
            Object instance = clazz.newInstance();
            Method sayHello = instance.getClass().getMethod("sayHello");
            Assertions.assertEquals("Hello world!", sayHello.invoke(instance));
        });
    }

    @Test
    public void testGetCompileClassWithError() {
        Exception exception = null;
        try {
            JdkCompiler compiler = new JdkCompiler();
            compiler.compile(getSimpleCodeWithError(), JdkCompiler.class.getClassLoader());
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertThat("can not acquire detail error message from exception.", exception.getMessage(), StringContains.containsString("public String sayHello( {"));
    }
}
