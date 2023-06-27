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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.lang.reflect.Method;

class JavassistCompilerTest extends JavaCodeTest {
    @Test
    void testCompileJavaClass() throws Exception {
        JavassistCompiler compiler = new JavassistCompiler();
        Class<?> clazz = compiler.compile(JavaCodeTest.class, getSimpleCode(), JavassistCompiler.class.getClassLoader());

        // Because javassist compiles using the caller class loader, we should't use HelloService directly
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Method sayHello = instance.getClass().getMethod("sayHello");
        Assertions.assertEquals("Hello world!", sayHello.invoke(instance));
    }

    /**
     * javassist compile will find HelloService in classpath
     */
    @Test
    @DisabledForJreRange(min = JRE.JAVA_16)
    public void testCompileJavaClass0() throws Exception {
        boolean ignoreWithoutPackage = shouldIgnoreWithoutPackage();
        JavassistCompiler compiler = new JavassistCompiler();

        if (ignoreWithoutPackage) {
            Assertions.assertThrows(RuntimeException.class, () -> compiler.compile(null, getSimpleCodeWithoutPackage(), JavassistCompiler.class.getClassLoader()));
        } else {
            Class<?> clazz = compiler.compile(null, getSimpleCodeWithoutPackage(), JavassistCompiler.class.getClassLoader());
            Object instance = clazz.getDeclaredConstructor().newInstance();
            Method sayHello = instance.getClass().getMethod("sayHello");
            Assertions.assertEquals("Hello world!", sayHello.invoke(instance));
        }
    }

    @Test
    void testCompileJavaClass1() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            JavassistCompiler compiler = new JavassistCompiler();
            Class<?> clazz = compiler.compile(JavaCodeTest.class, getSimpleCodeWithSyntax0(), JavassistCompiler.class.getClassLoader());
            Object instance = clazz.getDeclaredConstructor().newInstance();
            Method sayHello = instance.getClass().getMethod("sayHello");
            Assertions.assertEquals("Hello world!", sayHello.invoke(instance));
        });
    }

    @Test
    void testCompileJavaClassWithImport() throws Exception {
        JavassistCompiler compiler = new JavassistCompiler();
        Class<?> clazz = compiler.compile(JavaCodeTest.class, getSimpleCodeWithImports(), JavassistCompiler.class.getClassLoader());
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Method sayHello = instance.getClass().getMethod("sayHello");
        Assertions.assertEquals("Hello world!", sayHello.invoke(instance));
    }

    @Test
    void testCompileJavaClassWithExtends() throws Exception {
        JavassistCompiler compiler = new JavassistCompiler();
        Class<?> clazz = compiler.compile(JavaCodeTest.class, getSimpleCodeWithWithExtends(), JavassistCompiler.class.getClassLoader());
        Object instance = clazz.getDeclaredConstructor().newInstance();
        Method sayHello = instance.getClass().getMethod("sayHello");
        Assertions.assertEquals("Hello world3!", sayHello.invoke(instance));
    }
}
