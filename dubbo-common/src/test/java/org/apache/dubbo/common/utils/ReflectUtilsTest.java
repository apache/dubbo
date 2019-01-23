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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ReflectUtilsTest {
    @Test
    public void testIsPrimitives() throws Exception {
        assertTrue(ReflectUtils.isPrimitives(boolean[].class));
        assertTrue(ReflectUtils.isPrimitives(byte.class));
        assertFalse(ReflectUtils.isPrimitive(Map[].class));
    }

    @Test
    public void testIsPrimitive() throws Exception {
        assertTrue(ReflectUtils.isPrimitive(boolean.class));
        assertTrue(ReflectUtils.isPrimitive(String.class));
        assertTrue(ReflectUtils.isPrimitive(Boolean.class));
        assertTrue(ReflectUtils.isPrimitive(Character.class));
        assertTrue(ReflectUtils.isPrimitive(Number.class));
        assertTrue(ReflectUtils.isPrimitive(Date.class));
        assertFalse(ReflectUtils.isPrimitive(Map.class));
    }

    @Test
    public void testGetBoxedClass() throws Exception {
        assertThat(ReflectUtils.getBoxedClass(int.class), sameInstance(Integer.class));
        assertThat(ReflectUtils.getBoxedClass(boolean.class), sameInstance(Boolean.class));
        assertThat(ReflectUtils.getBoxedClass(long.class), sameInstance(Long.class));
        assertThat(ReflectUtils.getBoxedClass(float.class), sameInstance(Float.class));
        assertThat(ReflectUtils.getBoxedClass(double.class), sameInstance(Double.class));
        assertThat(ReflectUtils.getBoxedClass(char.class), sameInstance(Character.class));
        assertThat(ReflectUtils.getBoxedClass(byte.class), sameInstance(Byte.class));
        assertThat(ReflectUtils.getBoxedClass(short.class), sameInstance(Short.class));
    }

    @Test
    public void testIsCompatible() throws Exception {
        assertTrue(ReflectUtils.isCompatible(short.class, (short) 1));
        assertTrue(ReflectUtils.isCompatible(int.class, 1));
        assertTrue(ReflectUtils.isCompatible(double.class, 1.2));
        assertTrue(ReflectUtils.isCompatible(Object.class, 1.2));
        assertTrue(ReflectUtils.isCompatible(List.class, new ArrayList<String>()));
    }

    @Test
    public void testIsCompatibleWithArray() throws Exception {
        assertFalse(ReflectUtils.isCompatible(new Class[]{short.class, int.class}, new Object[]{(short) 1}));
        assertFalse(ReflectUtils.isCompatible(new Class[]{double.class}, new Object[]{"hello"}));
        assertTrue(ReflectUtils.isCompatible(new Class[]{double.class}, new Object[]{1.2}));
    }

    @Test
    public void testGetCodeBase() throws Exception {
        assertNull(ReflectUtils.getCodeBase(null));
        assertNull(ReflectUtils.getCodeBase(String.class));
        assertNotNull(ReflectUtils.getCodeBase(ReflectUtils.class));
    }

    @Test
    public void testGetName() throws Exception {
        // getName
        assertEquals("boolean", ReflectUtils.getName(boolean.class));
        assertEquals("int[][][]", ReflectUtils.getName(int[][][].class));
        assertEquals("java.lang.Object[][]", ReflectUtils.getName(Object[][].class));

        // getDesc
        assertEquals("Z", ReflectUtils.getDesc(boolean.class));
        assertEquals("[[[I", ReflectUtils.getDesc(int[][][].class));
        assertEquals("[[Ljava/lang/Object;", ReflectUtils.getDesc(Object[][].class));

        // name2desc
        assertEquals("Z", ReflectUtils.name2desc(ReflectUtils.getName(boolean.class)));
        assertEquals("[[[I", ReflectUtils.name2desc(ReflectUtils.getName(int[][][].class)));
        assertEquals("[[Ljava/lang/Object;", ReflectUtils.name2desc(ReflectUtils.getName(Object[][].class)));

        // desc2name
        assertEquals("short[]", ReflectUtils.desc2name(ReflectUtils.getDesc(short[].class)));
        assertEquals("boolean[]", ReflectUtils.desc2name(ReflectUtils.getDesc(boolean[].class)));
        assertEquals("byte[]", ReflectUtils.desc2name(ReflectUtils.getDesc(byte[].class)));
        assertEquals("char[]", ReflectUtils.desc2name(ReflectUtils.getDesc(char[].class)));
        assertEquals("double[]", ReflectUtils.desc2name(ReflectUtils.getDesc(double[].class)));
        assertEquals("float[]", ReflectUtils.desc2name(ReflectUtils.getDesc(float[].class)));
        assertEquals("int[]", ReflectUtils.desc2name(ReflectUtils.getDesc(int[].class)));
        assertEquals("long[]", ReflectUtils.desc2name(ReflectUtils.getDesc(long[].class)));
        assertEquals("int", ReflectUtils.desc2name(ReflectUtils.getDesc(int.class)));
        assertEquals("void", ReflectUtils.desc2name(ReflectUtils.getDesc(void.class)));
        assertEquals("java.lang.Object[][]", ReflectUtils.desc2name(ReflectUtils.getDesc(Object[][].class)));
    }

    @Test
    public void testGetGenericClass() throws Exception {
        assertThat(ReflectUtils.getGenericClass(Foo1.class), sameInstance(String.class));
    }

    @Test
    public void testGetGenericClassWithIndex() throws Exception {
        assertThat(ReflectUtils.getGenericClass(Foo1.class, 0), sameInstance(String.class));
        assertThat(ReflectUtils.getGenericClass(Foo1.class, 1), sameInstance(Integer.class));
        assertThat(ReflectUtils.getGenericClass(Foo2.class, 0), sameInstance(List.class));
        assertThat(ReflectUtils.getGenericClass(Foo2.class, 1), sameInstance(int.class));
        assertThat(ReflectUtils.getGenericClass(Foo3.class, 0), sameInstance(Foo1.class));
        assertThat(ReflectUtils.getGenericClass(Foo3.class, 1), sameInstance(Foo2.class));
    }

    @Test
    public void testGetMethodName() throws Exception {
        assertThat(ReflectUtils.getName(Foo2.class.getDeclaredMethod("hello", int[].class)),
                equalTo("java.util.List hello(int[])"));
    }

    @Test
    public void testGetSignature() throws Exception {
        Method m = Foo2.class.getDeclaredMethod("hello", int[].class);
        assertThat(ReflectUtils.getSignature("greeting", m.getParameterTypes()), equalTo("greeting([I)"));
    }

    @Test
    public void testGetConstructorName() throws Exception {
        Constructor c = Foo2.class.getConstructors()[0];
        assertThat(ReflectUtils.getName(c), equalTo("(java.util.List,int[])"));
    }

    @Test
    public void testName2Class() throws Exception {
        assertEquals(boolean.class, ReflectUtils.name2class("boolean"));
        assertEquals(boolean[].class, ReflectUtils.name2class("boolean[]"));
        assertEquals(int[][].class, ReflectUtils.name2class(ReflectUtils.getName(int[][].class)));
        assertEquals(ReflectUtilsTest[].class, ReflectUtils.name2class(ReflectUtils.getName(ReflectUtilsTest[].class)));
    }

    @Test
    public void testGetDescMethod() throws Exception {
        assertThat(ReflectUtils.getDesc(Foo2.class.getDeclaredMethod("hello", int[].class)),
                equalTo("hello([I)Ljava/util/List;"));
    }

    @Test
    public void testGetDescConstructor() throws Exception {
        assertThat(ReflectUtils.getDesc(Foo2.class.getConstructors()[0]), equalTo("(Ljava/util/List;[I)V"));
    }

    @Test
    public void testGetDescWithoutMethodName() throws Exception {
        assertThat(ReflectUtils.getDescWithoutMethodName(Foo2.class.getDeclaredMethod("hello", int[].class)),
                equalTo("([I)Ljava/util/List;"));
    }

    @Test
    public void testFindMethodByMethodName1() throws Exception {
        assertNotNull(ReflectUtils.findMethodByMethodName(Foo.class, "hello"));
    }

    @Test
    public void testFindMethodByMethodName2() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ReflectUtils.findMethodByMethodName(Foo2.class, "hello");
        });

    }

    @Test
    public void testFindConstructor() throws Exception {
        Constructor constructor = ReflectUtils.findConstructor(Foo3.class, Foo2.class);
        assertNotNull(constructor);
    }

    @Test
    public void testIsInstance() throws Exception {
        assertTrue(ReflectUtils.isInstance(new Foo1(), Foo.class.getName()));
    }

    @Test
    public void testIsBeanPropertyReadMethod() throws Exception {
        Method method = EmptyClass.class.getMethod("getProperty");
        assertTrue(ReflectUtils.isBeanPropertyReadMethod(method));
        method = EmptyClass.class.getMethod("getProperties");
        assertFalse(ReflectUtils.isBeanPropertyReadMethod(method));
        method = EmptyClass.class.getMethod("isProperty");
        assertFalse(ReflectUtils.isBeanPropertyReadMethod(method));
        method = EmptyClass.class.getMethod("getPropertyIndex", int.class);
        assertFalse(ReflectUtils.isBeanPropertyReadMethod(method));
    }

    @Test
    public void testGetPropertyNameFromBeanReadMethod() throws Exception {
        Method method = EmptyClass.class.getMethod("getProperty");
        assertEquals(ReflectUtils.getPropertyNameFromBeanReadMethod(method), "property");
        method = EmptyClass.class.getMethod("isSet");
        assertEquals(ReflectUtils.getPropertyNameFromBeanReadMethod(method), "set");
    }

    @Test
    public void testIsBeanPropertyWriteMethod() throws Exception {
        Method method = EmptyClass.class.getMethod("setProperty", EmptyProperty.class);
        assertTrue(ReflectUtils.isBeanPropertyWriteMethod(method));
        method = EmptyClass.class.getMethod("setSet", boolean.class);
        assertTrue(ReflectUtils.isBeanPropertyWriteMethod(method));
    }

    @Test
    public void testGetPropertyNameFromBeanWriteMethod() throws Exception {
        Method method = EmptyClass.class.getMethod("setProperty", EmptyProperty.class);
        assertEquals(ReflectUtils.getPropertyNameFromBeanWriteMethod(method), "property");
    }

    @Test
    public void testIsPublicInstanceField() throws Exception {
        Field field = EmptyClass.class.getDeclaredField("set");
        assertTrue(ReflectUtils.isPublicInstanceField(field));
        field = EmptyClass.class.getDeclaredField("property");
        assertFalse(ReflectUtils.isPublicInstanceField(field));
    }

    @Test
    public void testGetBeanPropertyFields() throws Exception {
        Map<String, Field> map = ReflectUtils.getBeanPropertyFields(EmptyClass.class);
        assertThat(map.size(), is(2));
        assertThat(map, hasKey("set"));
        assertThat(map, hasKey("property"));
        for (Field f : map.values()) {
            if (!f.isAccessible()) {
                fail();
            }
        }
    }

    @Test
    public void testGetBeanPropertyReadMethods() throws Exception {
        Map<String, Method> map = ReflectUtils.getBeanPropertyReadMethods(EmptyClass.class);
        assertThat(map.size(), is(2));
        assertThat(map, hasKey("set"));
        assertThat(map, hasKey("property"));
        for (Method m : map.values()) {
            if (!m.isAccessible()) {
                fail();
            }
        }
    }

    @Test
    public void testDesc2Class() throws Exception {
        assertEquals(void.class, ReflectUtils.desc2class("V"));
        assertEquals(boolean.class, ReflectUtils.desc2class("Z"));
        assertEquals(boolean[].class, ReflectUtils.desc2class("[Z"));
        assertEquals(byte.class, ReflectUtils.desc2class("B"));
        assertEquals(char.class, ReflectUtils.desc2class("C"));
        assertEquals(double.class, ReflectUtils.desc2class("D"));
        assertEquals(float.class, ReflectUtils.desc2class("F"));
        assertEquals(int.class, ReflectUtils.desc2class("I"));
        assertEquals(long.class, ReflectUtils.desc2class("J"));
        assertEquals(short.class, ReflectUtils.desc2class("S"));
        assertEquals(String.class, ReflectUtils.desc2class("Ljava.lang.String;"));
        assertEquals(int[][].class, ReflectUtils.desc2class(ReflectUtils.getDesc(int[][].class)));
        assertEquals(ReflectUtilsTest[].class, ReflectUtils.desc2class(ReflectUtils.getDesc(ReflectUtilsTest[].class)));

        String desc;
        Class<?>[] cs;

        cs = new Class<?>[]{int.class, getClass(), String.class, int[][].class, boolean[].class};
        desc = ReflectUtils.getDesc(cs);
        assertSame(cs, ReflectUtils.desc2classArray(desc));

        cs = new Class<?>[]{};
        desc = ReflectUtils.getDesc(cs);
        assertSame(cs, ReflectUtils.desc2classArray(desc));

        cs = new Class<?>[]{void.class, String[].class, int[][].class, ReflectUtilsTest[][].class};
        desc = ReflectUtils.getDesc(cs);
        assertSame(cs, ReflectUtils.desc2classArray(desc));
    }

    protected void assertSame(Class<?>[] cs1, Class<?>[] cs2) throws Exception {
        assertEquals(cs1.length, cs2.length);
        for (int i = 0; i < cs1.length; i++)
            assertEquals(cs1[i], cs2[i]);
    }

    @Test
    public void testFindMethodByMethodSignature() throws Exception {
        Method m = ReflectUtils.findMethodByMethodSignature(TestedClass.class, "method1", null);

        assertEquals("method1", m.getName());
        Class<?>[] parameterTypes = m.getParameterTypes();
        assertEquals(1, parameterTypes.length);
        assertEquals(int.class, parameterTypes[0]);
    }

    @Test
    public void testFindMethodByMethodSignature_override() throws Exception {
        {
            Method m = ReflectUtils.findMethodByMethodSignature(TestedClass.class,
                    "overrideMethod", new String[]{"int"});

            assertEquals("overrideMethod", m.getName());
            Class<?>[] parameterTypes = m.getParameterTypes();
            assertEquals(1, parameterTypes.length);
            assertEquals(int.class, parameterTypes[0]);
        }
        {
            Method m = ReflectUtils.findMethodByMethodSignature(TestedClass.class,
                    "overrideMethod", new String[]{"java.lang.Integer"});

            assertEquals("overrideMethod", m.getName());
            Class<?>[] parameterTypes = m.getParameterTypes();
            assertEquals(1, parameterTypes.length);
            assertEquals(Integer.class, parameterTypes[0]);
        }
    }

    @Test
    public void testFindMethodByMethodSignatureOverrideMoreThan1() throws Exception {
        try {
            ReflectUtils.findMethodByMethodSignature(TestedClass.class, "overrideMethod", null);
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Not unique method for method name("));
        }
    }

    @Test
    public void testFindMethodByMethodSignatureNotFound() throws Exception {
        try {
            ReflectUtils.findMethodByMethodSignature(TestedClass.class, "notExsited", null);
            fail();
        } catch (NoSuchMethodException expected) {
            assertThat(expected.getMessage(), containsString("No such method "));
            assertThat(expected.getMessage(), containsString("in class"));
        }
    }

    @Test
    public void testGetEmptyObject() throws Exception {
        assertTrue(ReflectUtils.getEmptyObject(Collection.class) instanceof Collection);
        assertTrue(ReflectUtils.getEmptyObject(List.class) instanceof List);
        assertTrue(ReflectUtils.getEmptyObject(Set.class) instanceof Set);
        assertTrue(ReflectUtils.getEmptyObject(Map.class) instanceof Map);
        assertTrue(ReflectUtils.getEmptyObject(Object[].class) instanceof Object[]);
        assertEquals(ReflectUtils.getEmptyObject(String.class), "");
        assertEquals(ReflectUtils.getEmptyObject(short.class), Short.valueOf((short) 0));
        assertEquals(ReflectUtils.getEmptyObject(byte.class), Byte.valueOf((byte) 0));
        assertEquals(ReflectUtils.getEmptyObject(int.class), Integer.valueOf(0));
        assertEquals(ReflectUtils.getEmptyObject(long.class), Long.valueOf(0));
        assertEquals(ReflectUtils.getEmptyObject(float.class), Float.valueOf(0));
        assertEquals(ReflectUtils.getEmptyObject(double.class), Double.valueOf(0));
        assertEquals(ReflectUtils.getEmptyObject(char.class), Character.valueOf('\0'));
        assertEquals(ReflectUtils.getEmptyObject(boolean.class), Boolean.FALSE);
        EmptyClass object = (EmptyClass) ReflectUtils.getEmptyObject(EmptyClass.class);
        assertNotNull(object);
        assertNotNull(object.getProperty());
    }

    @Test
    public void testForName1() throws Exception {
        assertThat(ReflectUtils.forName(ReflectUtils.class.getName()), sameInstance(ReflectUtils.class));
    }

    @Test
    public void testForName2() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ReflectUtils.forName("a.c.d.e.F");
        });
    }

    public static class EmptyClass {
        private EmptyProperty property;
        public boolean set;
        public static String s;
        private transient int i;

        public EmptyProperty getProperty() {
            return property;
        }

        public EmptyProperty getPropertyIndex(int i) {
            return property;
        }

        public static EmptyProperty getProperties() {
            return null;
        }

        public void isProperty() {

        }

        public boolean isSet() {
            return set;
        }

        public void setProperty(EmptyProperty property) {
            this.property = property;
        }

        public void setSet(boolean set) {
            this.set = set;
        }
    }

    public static class EmptyProperty {
    }

    static class TestedClass {
        public void method1(int x) {
        }

        public void overrideMethod(int x) {
        }

        public void overrideMethod(Integer x) {
        }

        public void overrideMethod(String s) {
        }

        public void overrideMethod(String s1, String s2) {
        }
    }


    interface Foo<A, B> {
        A hello(B b);
    }

    static class Foo1 implements Foo<String, Integer> {
        @Override
        public String hello(Integer integer) {
            return null;
        }
    }

    static class Foo2 implements Foo<List<String>, int[]> {
        public Foo2(List<String> list, int[] ints) {
        }

        @Override
        public List<String> hello(int[] ints) {
            return null;
        }
    }

    static class Foo3 implements Foo<Foo1, Foo2> {
        public Foo3(Foo foo) {
        }

        @Override
        public Foo1 hello(Foo2 foo2) {
            return null;
        }
    }


}