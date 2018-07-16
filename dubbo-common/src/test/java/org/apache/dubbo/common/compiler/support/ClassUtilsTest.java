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

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
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
        Assert.assertEquals("Hello world!", instance.sayHello());
    }

    @Test(expected = IllegalStateException.class)
    public void testNewInstance0() {
        ClassUtils.newInstance(PrivateHelloServiceImpl.class.getName());
    }

    @Test(expected = IllegalStateException.class)
    public void testNewInstance1() {
        ClassUtils.newInstance("org.apache.dubbo.common.compiler.support.internal.HelloServiceInternalImpl");
    }

    @Test(expected = IllegalStateException.class)
    public void testNewInstance2() {
        ClassUtils.newInstance("org.apache.dubbo.common.compiler.support.internal.NotExistsImpl");
    }

    @Test
    public void testForName() {
        ClassUtils.forName(new String[]{"org.apache.dubbo.common.compiler.support"}, "HelloServiceImpl0");
    }

    @Test(expected = IllegalStateException.class)
    public void testForName1() {
        ClassUtils.forName(new String[]{"org.apache.dubbo.common.compiler.support"}, "HelloServiceImplXX");
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
        Assert.assertEquals(Boolean.class, ClassUtils.getBoxedClass(boolean.class));
        Assert.assertEquals(Character.class, ClassUtils.getBoxedClass(char.class));
        Assert.assertEquals(Byte.class, ClassUtils.getBoxedClass(byte.class));
        Assert.assertEquals(Short.class, ClassUtils.getBoxedClass(short.class));
        Assert.assertEquals(Integer.class, ClassUtils.getBoxedClass(int.class));
        Assert.assertEquals(Long.class, ClassUtils.getBoxedClass(long.class));
        Assert.assertEquals(Float.class, ClassUtils.getBoxedClass(float.class));
        Assert.assertEquals(Double.class, ClassUtils.getBoxedClass(double.class));
        Assert.assertEquals(ClassUtilsTest.class, ClassUtils.getBoxedClass(ClassUtilsTest.class));
    }

    @Test
    public void testBoxedAndUnboxed() {
        Assert.assertEquals(Boolean.valueOf(true), ClassUtils.boxed(true));
        Assert.assertEquals(Character.valueOf('0'), ClassUtils.boxed('0'));
        Assert.assertEquals(Byte.valueOf((byte) 0), ClassUtils.boxed((byte) 0));
        Assert.assertEquals(Short.valueOf((short) 0), ClassUtils.boxed((short) 0));
        Assert.assertEquals(Integer.valueOf((int) 0), ClassUtils.boxed((int) 0));
        Assert.assertEquals(Long.valueOf((long) 0), ClassUtils.boxed((long) 0));
        Assert.assertEquals(Float.valueOf((float) 0), ClassUtils.boxed((float) 0));
        Assert.assertEquals(Double.valueOf((double) 0), ClassUtils.boxed((double) 0));

        Assert.assertEquals(true, ClassUtils.unboxed(Boolean.valueOf(true)));
        Assert.assertEquals('0', ClassUtils.unboxed(Character.valueOf('0')));
        Assert.assertEquals((byte) 0, ClassUtils.unboxed(Byte.valueOf((byte) 0)));
        Assert.assertEquals((short) 0, ClassUtils.unboxed(Short.valueOf((short) 0)));
        Assert.assertEquals(0, ClassUtils.unboxed(Integer.valueOf((int) 0)));
        Assert.assertEquals((long) 0, ClassUtils.unboxed(Long.valueOf((long) 0)));
        Assert.assertEquals((float) 0, ClassUtils.unboxed(Float.valueOf((float) 0)), ((float)0));
        Assert.assertEquals((double) 0, ClassUtils.unboxed(Double.valueOf((double) 0)), ((double)0));
    }

    @Test
    public void testGetSize(){
        Assert.assertEquals(0, ClassUtils.getSize(null));
        List<Integer> list = new ArrayList<>();list.add(1);
        Assert.assertEquals(1, ClassUtils.getSize(list));
        Map map = new HashMap(); map.put(1, 1);
        Assert.assertEquals(1, ClassUtils.getSize(map));
        int[] array = new int[1];
        Assert.assertEquals(1, ClassUtils.getSize(array));
        Assert.assertEquals(-1, ClassUtils.getSize(new Object()));
    }

    @Test(expected = RuntimeException.class)
    public void testToUri(){
        ClassUtils.toURI("#xx_abc#hello");
    }

    @Test
    public void testGetGenericClass(){
        Assert.assertTrue(TypeVariable.class.isAssignableFrom(ClassUtils.getGenericClass(GenericClass.class)));
        Assert.assertTrue(String.class.isAssignableFrom(ClassUtils.getGenericClass(GenericClass0.class)));
        Assert.assertTrue(Collection.class.isAssignableFrom(ClassUtils.getGenericClass(GenericClass1.class)));
        Assert.assertTrue(TypeVariable.class.isAssignableFrom(ClassUtils.getGenericClass(GenericClass2.class)));
        Assert.assertTrue(GenericArrayType.class.isAssignableFrom(ClassUtils.getGenericClass(GenericClass3.class)));
    }

    @Test
    public void testGetSizeMethod(){
        Assert.assertEquals("getLength()", ClassUtils.getSizeMethod(GenericClass3.class));
    }

    private interface GenericInterface<T>{
    }

    private class GenericClass<T> implements GenericInterface<T>{
    }

    private class GenericClass0 implements GenericInterface<String>{
    }

    private class GenericClass1 implements GenericInterface<Collection<String>>{
    }

    private class GenericClass2<T> implements GenericInterface<T[]>{
    }

    private class GenericClass3<T> implements GenericInterface<T[][]>{
        public int getLength(){
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
