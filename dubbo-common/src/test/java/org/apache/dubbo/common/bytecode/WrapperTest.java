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
package org.apache.dubbo.common.bytecode;

import org.apache.dubbo.common.utils.ClassUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

class WrapperTest {
    @Test
    void testMain() throws Exception {
        Wrapper w = Wrapper.getWrapper(I1.class);
        String[] ns = w.getDeclaredMethodNames();
        assertEquals(ns.length, 5);
        ns = w.getMethodNames();
        assertEquals(ns.length, 6);

        Object obj = new Impl1();
        assertEquals(w.getPropertyValue(obj, "name"), "you name");

        w.setPropertyValue(obj, "name", "changed");
        assertEquals(w.getPropertyValue(obj, "name"), "changed");

        w.invokeMethod(obj, "hello", new Class<?>[]{String.class}, new Object[]{"qianlei"});

        w.setPropertyValues(obj, new String[]{"name", "float"}, new Object[]{"mrh", 1.0f});
        Object[] propertyValues = w.getPropertyValues(obj, new String[]{"name", "float"});
        Assertions.assertEquals(propertyValues.length, 2);
        Assertions.assertEquals(propertyValues[0], "mrh");
        Assertions.assertEquals(propertyValues[1], 1.0f);
    }

    // bug: DUBBO-132
    @Test
    void test_unwantedArgument() throws Exception {
        Wrapper w = Wrapper.getWrapper(I1.class);
        Object obj = new Impl1();
        try {
            w.invokeMethod(obj, "hello", new Class<?>[]{String.class, String.class},
                    new Object[]{"qianlei", "badboy"});
            fail();
        } catch (NoSuchMethodException expected) {
        }
    }

    //bug: DUBBO-425
    @Test
    void test_makeEmptyClass() throws Exception {
        Wrapper.getWrapper(EmptyServiceImpl.class);
    }

    @Test
    void testHasMethod() throws Exception {
        Wrapper w = Wrapper.getWrapper(I1.class);
        Assertions.assertTrue(w.hasMethod("setName"));
        Assertions.assertTrue(w.hasMethod("hello"));
        Assertions.assertTrue(w.hasMethod("showInt"));
        Assertions.assertTrue(w.hasMethod("getFloat"));
        Assertions.assertTrue(w.hasMethod("setFloat"));
        Assertions.assertFalse(w.hasMethod("setFloatXXX"));
    }

    @Test
    void testWrapperObject() throws Exception {
        Wrapper w = Wrapper.getWrapper(Object.class);
        Assertions.assertEquals(4, w.getMethodNames().length);
        Assertions.assertEquals(4, w.getDeclaredMethodNames().length);
        Assertions.assertEquals(0, w.getPropertyNames().length);
        Assertions.assertNull(w.getPropertyType(null));
        Assertions.assertFalse(w.hasProperty(null));
    }

    @Test
    void testGetPropertyValue() throws Exception {
        Assertions.assertThrows(NoSuchPropertyException.class, () -> {
            Wrapper w = Wrapper.getWrapper(Object.class);
            w.getPropertyValue(null, null);
        });
    }

    @Test
    void testSetPropertyValue() throws Exception {
        Assertions.assertThrows(NoSuchPropertyException.class, () -> {
            Wrapper w = Wrapper.getWrapper(Object.class);
            w.setPropertyValue(null, null, null);
        });
    }

    @Test
    void testWrapPrimitive() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Wrapper.getWrapper(Byte.TYPE);
        });
    }
    
    @Test
    void testInvokeWrapperObject() throws Exception {
        Wrapper w = Wrapper.getWrapper(Object.class);
        Object instance = new Object();
        Assertions.assertEquals(instance.getClass(), w.invokeMethod(instance, "getClass", null, null));
        Assertions.assertEquals(instance.hashCode(), (int) w.invokeMethod(instance, "hashCode", null, null));
        Assertions.assertEquals(instance.toString(), w.invokeMethod(instance, "toString", null, null));
        Assertions.assertTrue((boolean) w.invokeMethod(instance, "equals", new Class[]{instance.getClass()}, new Object[]{instance}));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> w.invokeMethod(instance, "equals", new Class[]{instance.getClass()}, new Object[]{instance, instance}));
    }

    @Test
    void testNoSuchMethod() throws Exception {
        Assertions.assertThrows(NoSuchMethodException.class, () -> {
            Wrapper w = Wrapper.getWrapper(Object.class);
            w.invokeMethod(new Object(), "__XX__", null, null);
        });
    }

    @Test
    void testOverloadMethod() throws Exception {
        Wrapper w = Wrapper.getWrapper(I2.class);
        assertEquals(2, w.getMethodNames().length);

        Impl2 impl = new Impl2();

        w.invokeMethod(impl, "setFloat", new Class[]{float.class}, new Object[]{1F});
        assertEquals(1F, impl.getFloat1());
        assertNull(impl.getFloat2());

        w.invokeMethod(impl, "setFloat", new Class[]{Float.class}, new Object[]{2f});
        assertEquals(1F, impl.getFloat1());
        assertEquals(2F, impl.getFloat2());

        w.invokeMethod(impl, "setFloat", new Class[]{Float.class}, new Object[]{null});
        assertEquals(1F, impl.getFloat1());
        assertNull(impl.getFloat2());
    }

    @Test
    void test_getDeclaredMethodNames_ContainExtendsParentMethods() throws Exception {
        assertArrayEquals(new String[]{"hello",}, Wrapper.getWrapper(Parent1.class).getMethodNames());
        assertArrayEquals(new String[]{"hello",}, ClassUtils.getMethodNames(Parent1.class));

        assertArrayEquals(new String[]{}, Wrapper.getWrapper(Son.class).getDeclaredMethodNames());
        assertArrayEquals(new String[]{}, ClassUtils.getDeclaredMethodNames(Son.class));
    }

    @Test
    void test_getMethodNames_ContainExtendsParentMethods() throws Exception {
        assertArrayEquals(new String[]{"hello", "world"}, Wrapper.getWrapper(Son.class).getMethodNames());
        assertArrayEquals(new String[]{"hello", "world"}, ClassUtils.getMethodNames(Son.class));
    }

    @Test
    void testWrapImplClass(){
        Wrapper w = Wrapper.getWrapper(Impl0.class);

        String[] propertyNames = w.getPropertyNames();
        Assertions.assertArrayEquals(propertyNames, new String[]{"a", "b", "c"});
        // fields that do not contain the static|final|transient modifier
        Assertions.assertFalse(w.hasProperty("f"));
        Assertions.assertFalse(w.hasProperty("l"));
        Assertions.assertFalse(w.hasProperty("ch"));

        // only has public methods, do not contain the private or comes from object methods
        Assertions.assertTrue(w.hasMethod("publicMethod"));
        Assertions.assertFalse(w.hasMethod("privateMethod"));
        Assertions.assertFalse(w.hasMethod("hashcode"));
    }

    public interface I0 {
        String getName();
    }

    public interface I1 extends I0 {
        void setName(String name);

        void hello(String name);

        int showInt(int v);

        float getFloat();

        void setFloat(float f);
    }

    public interface I2 {
        void setFloat(float f);

        void setFloat(Float f);
    }

    public interface EmptyService {
    }

    public interface Parent1 {
        void hello();
    }


    public interface Parent2 {
        void world();
    }

    public interface Son extends Parent1, Parent2 {

    }

    public static class Impl0 {
        public float a, b, c;
        public transient boolean f;
        public static long l = 1;
        public final char ch = 'c';

        private void privateMethod() {

        }

        public void publicMethod() {

        }
    }

    public static class Impl1 implements I1 {
        private String name = "you name";

        private float fv = 0;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void hello(String name) {
            System.out.println("hello " + name);
        }

        public int showInt(int v) {
            return v;
        }

        public float getFloat() {
            return fv;
        }

        public void setFloat(float f) {
            fv = f;
        }
    }

    public static class Impl2 implements I2 {
        private float float1;
        private Float float2;

        @Override
        public void setFloat(float f) {
            this.float1 = f;
        }

        @Override
        public void setFloat(Float f) {
            this.float2 = f;
        }

        public float getFloat1() {
            return float1;
        }

        public Float getFloat2() {
            return float2;
        }
    }

    public static class EmptyServiceImpl implements EmptyService {
    }
}