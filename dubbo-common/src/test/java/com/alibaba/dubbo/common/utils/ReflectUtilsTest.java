/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.utils;

import junit.framework.TestCase;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class ReflectUtilsTest extends TestCase {
    public void testIsCompatible() throws Exception {
        assertEquals(ReflectUtils.isCompatible(short.class, (short) 1), true);
        assertEquals(ReflectUtils.isCompatible(int.class, 1), true);
        assertEquals(ReflectUtils.isCompatible(double.class, 1.2), true);
        assertEquals(ReflectUtils.isCompatible(Object.class, 1.2), true);
        assertEquals(ReflectUtils.isCompatible(List.class, new ArrayList<String>()), true);
    }

    public void testNameDesc() throws Exception {
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
        assertEquals("int", ReflectUtils.desc2name(ReflectUtils.getDesc(int.class)));
        assertEquals("java.lang.Object[][]", ReflectUtils.desc2name(ReflectUtils.getDesc(Object[][].class)));
    }

    public void testName2Class() throws Exception {
        assertEquals(boolean.class, ReflectUtils.name2class("boolean"));
        assertEquals(boolean[].class, ReflectUtils.name2class("boolean[]"));
        assertEquals(int[][].class, ReflectUtils.name2class(ReflectUtils.getName(int[][].class)));
        assertEquals(ReflectUtilsTest[].class, ReflectUtils.name2class(ReflectUtils.getName(ReflectUtilsTest[].class)));
    }

    public void testDesc2Class() throws Exception {
        assertEquals(boolean.class, ReflectUtils.desc2class("Z"));
        assertEquals(boolean[].class, ReflectUtils.desc2class("[Z"));
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
    public void test_findMethodByMethodSignature() throws Exception {
        Method m = ReflectUtils.findMethodByMethodSignature(TestedClass.class, "method1", null);

        assertEquals("method1", m.getName());
        Class<?>[] parameterTypes = m.getParameterTypes();
        assertEquals(1, parameterTypes.length);
        assertEquals(int.class, parameterTypes[0]);
    }

    @Test
    public void test_findMethodByMethodSignature_override() throws Exception {
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
    public void test_findMethodByMethodSignature_override_Morethan1() throws Exception {
        try {
            ReflectUtils.findMethodByMethodSignature(TestedClass.class, "overrideMethod", null);
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString(
                    "Not unique method for method name("));
        }
    }

    @Test
    public void test_findMethodByMethodSignature_notFound() throws Exception {
        try {
            ReflectUtils.findMethodByMethodSignature(TestedClass.class, "notExsited", null);
            fail();
        } catch (NoSuchMethodException expected) {
            assertThat(expected.getMessage(), containsString("No such method "));
            assertThat(expected.getMessage(), containsString("in class"));
        }
    }

    @Test
    public void test_getEmptyObject() throws Exception {
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

    public static class EmptyClass {

        private EmptyProperty property;

        public EmptyProperty getProperty() {
            return property;
        }

        public void setProperty(EmptyProperty property) {
            this.property = property;
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

}