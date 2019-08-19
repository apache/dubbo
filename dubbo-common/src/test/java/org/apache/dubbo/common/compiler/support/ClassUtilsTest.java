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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassUtilsTest {

    @Test
    public void testNewInstance() {
        HelloServiceImpl0 instance = (HelloServiceImpl0) ClassUtils.newInstance(HelloServiceImpl0.class.getName());
        Assertions.assertEquals("Hello world!", instance.sayHello());
    }

    @Test
    public void testNewInstance0() {
        Assertions.assertThrows(IllegalStateException.class, () -> ClassUtils.newInstance(PrivateHelloServiceImpl.class.getName()));
    }

    @Test
    public void testNewInstance1() {
        Assertions.assertThrows(IllegalStateException.class, () -> ClassUtils.newInstance("org.apache.dubbo.common.compiler.support.internal.HelloServiceInternalImpl"));
    }

    @Test
    public void testNewInstance2() {
        Assertions.assertThrows(IllegalStateException.class, () -> ClassUtils.newInstance("org.apache.dubbo.common.compiler.support.internal.NotExistsImpl"));
    }

    @Test
    public void testForName() {
        ClassUtils.forName(new String[]{"org.apache.dubbo.common.compiler.support"}, "HelloServiceImpl0");
    }

    @Test
    public void testForName1() {
        Assertions.assertThrows(IllegalStateException.class, () -> ClassUtils.forName(new String[]{"org.apache.dubbo.common.compiler.support"}, "HelloServiceImplXX"));
    }

    @Test
    public void testForName2() {
        ClassUtils.forName("boolean");
        ClassUtils.forName("byte");
        ClassUtils.forName("char");
        ClassUtils.forName("short");
        ClassUtils.forName("int");
        ClassUtils.forName("long");
        ClassUtils.forName("float");
        ClassUtils.forName("double");
        ClassUtils.forName("boolean[]");
        ClassUtils.forName("byte[]");
        ClassUtils.forName("char[]");
        ClassUtils.forName("short[]");
        ClassUtils.forName("int[]");
        ClassUtils.forName("long[]");
        ClassUtils.forName("float[]");
        ClassUtils.forName("double[]");
    }

    @Test
    public void testGetBoxedClass() {
        Assertions.assertEquals(Boolean.class, ClassUtils.getBoxedClass(boolean.class));
        Assertions.assertEquals(Character.class, ClassUtils.getBoxedClass(char.class));
        Assertions.assertEquals(Byte.class, ClassUtils.getBoxedClass(byte.class));
        Assertions.assertEquals(Short.class, ClassUtils.getBoxedClass(short.class));
        Assertions.assertEquals(Integer.class, ClassUtils.getBoxedClass(int.class));
        Assertions.assertEquals(Long.class, ClassUtils.getBoxedClass(long.class));
        Assertions.assertEquals(Float.class, ClassUtils.getBoxedClass(float.class));
        Assertions.assertEquals(Double.class, ClassUtils.getBoxedClass(double.class));
        Assertions.assertEquals(ClassUtilsTest.class, ClassUtils.getBoxedClass(ClassUtilsTest.class));
    }

    @Test
    public void testBoxedAndUnboxed() {
        Assertions.assertEquals(Boolean.valueOf(true), ClassUtils.boxed(true));
        Assertions.assertEquals(Character.valueOf('0'), ClassUtils.boxed('0'));
        Assertions.assertEquals(Byte.valueOf((byte) 0), ClassUtils.boxed((byte) 0));
        Assertions.assertEquals(Short.valueOf((short) 0), ClassUtils.boxed((short) 0));
        Assertions.assertEquals(Integer.valueOf((int) 0), ClassUtils.boxed((int) 0));
        Assertions.assertEquals(Long.valueOf((long) 0), ClassUtils.boxed((long) 0));
        Assertions.assertEquals(Float.valueOf((float) 0), ClassUtils.boxed((float) 0));
        Assertions.assertEquals(Double.valueOf((double) 0), ClassUtils.boxed((double) 0));

        Assertions.assertTrue(ClassUtils.unboxed(Boolean.valueOf(true)));
        Assertions.assertEquals('0', ClassUtils.unboxed(Character.valueOf('0')));
        Assertions.assertEquals((byte) 0, ClassUtils.unboxed(Byte.valueOf((byte) 0)));
        Assertions.assertEquals((short) 0, ClassUtils.unboxed(Short.valueOf((short) 0)));
        Assertions.assertEquals(0, ClassUtils.unboxed(Integer.valueOf((int) 0)));
        Assertions.assertEquals((long) 0, ClassUtils.unboxed(Long.valueOf((long) 0)));
//        Assertions.assertEquals((float) 0, ClassUtils.unboxed(Float.valueOf((float) 0)), ((float) 0));
//        Assertions.assertEquals((double) 0, ClassUtils.unboxed(Double.valueOf((double) 0)), ((double) 0));
    }

    @Test
    public void testGetSize() {
        Assertions.assertEquals(0, ClassUtils.getSize(null));
        List<Integer> list = new ArrayList<>();
        list.add(1);
        Assertions.assertEquals(1, ClassUtils.getSize(list));
        Map map = new HashMap();
        map.put(1, 1);
        Assertions.assertEquals(1, ClassUtils.getSize(map));
        int[] array = new int[1];
        Assertions.assertEquals(1, ClassUtils.getSize(array));
        Assertions.assertEquals(-1, ClassUtils.getSize(new Object()));
    }

    @Test
    public void testToUri() {
        Assertions.assertThrows(RuntimeException.class, () -> ClassUtils.toURI("#xx_abc#hello"));
    }

    @Test
    public void testGetGenericClass() {
        Assertions.assertTrue(TypeVariable.class.isAssignableFrom(ClassUtils.getGenericClass(GenericClass.class)));
        Assertions.assertTrue(String.class.isAssignableFrom(ClassUtils.getGenericClass(GenericClass0.class)));
        Assertions.assertTrue(Collection.class.isAssignableFrom(ClassUtils.getGenericClass(GenericClass1.class)));
        Assertions.assertTrue(TypeVariable.class.isAssignableFrom(ClassUtils.getGenericClass(GenericClass2.class)));
        Assertions.assertTrue(GenericArrayType.class.isAssignableFrom(ClassUtils.getGenericClass(GenericClass3.class)));
    }

    @Test
    public void testGetSizeMethod() {
        Assertions.assertEquals("getLength()", ClassUtils.getSizeMethod(GenericClass3.class));
    }
    
    @Test
    public void testGetSimpleClassName() {
        Assertions.assertNull(ClassUtils.getSimpleClassName(null));
        Assertions.assertEquals("Map", ClassUtils.getSimpleClassName(Map.class.getName()));
        Assertions.assertEquals("Map", ClassUtils.getSimpleClassName(Map.class.getSimpleName()));
    }

    private interface GenericInterface<T> {
    }

    private class GenericClass<T> implements GenericInterface<T> {
    }

    private class GenericClass0 implements GenericInterface<String> {
    }

    private class GenericClass1 implements GenericInterface<Collection<String>> {
    }

    private class GenericClass2<T> implements GenericInterface<T[]> {
    }

    private class GenericClass3<T> implements GenericInterface<T[][]> {
        public int getLength() {
            return -1;
        }
    }

    private class PrivateHelloServiceImpl implements HelloService {
        private PrivateHelloServiceImpl() {
        }

        @Override
        public String sayHello() {
            return "Hello world!";
        }
    }

}
