/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.common.utils;

import org.junit.Test;
import org.mockito.Mockito;

import static org.apache.dubbo.common.utils.ClassHelper.forName;
import static org.apache.dubbo.common.utils.ClassHelper.getCallerClassLoader;
import static org.apache.dubbo.common.utils.ClassHelper.getClassLoader;
import static org.apache.dubbo.common.utils.ClassHelper.resolvePrimitiveClassName;
import static org.apache.dubbo.common.utils.ClassHelper.toShortString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

public class ClassHelperTest {
    @Test
    public void testForNameWithThreadContextClassLoader() throws Exception {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader classLoader = Mockito.mock(ClassLoader.class);
            Thread.currentThread().setContextClassLoader(classLoader);
            ClassHelper.forNameWithThreadContextClassLoader("a.b.c.D");
            verify(classLoader).loadClass("a.b.c.D");
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Test
    public void tetForNameWithCallerClassLoader() throws Exception {
        Class c = ClassHelper.forNameWithCallerClassLoader(ClassHelper.class.getName(), ClassHelperTest.class);
        assertThat(c == ClassHelper.class, is(true));
    }

    @Test
    public void testGetCallerClassLoader() throws Exception {
        assertThat(getCallerClassLoader(ClassHelperTest.class), sameInstance(ClassHelperTest.class.getClassLoader()));
    }

    @Test
    public void testGetClassLoader1() throws Exception {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            assertThat(getClassLoader(ClassHelperTest.class), sameInstance(oldClassLoader));
            Thread.currentThread().setContextClassLoader(null);
            assertThat(getClassLoader(ClassHelperTest.class), sameInstance(ClassHelperTest.class.getClassLoader()));
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Test
    public void testGetClassLoader2() throws Exception {
        assertThat(getClassLoader(), sameInstance(ClassHelper.class.getClassLoader()));
    }

    @Test
    public void testForName1() throws Exception {
        assertThat(forName(ClassHelperTest.class.getName()) == ClassHelperTest.class, is(true));
    }

    @Test
    public void testForName2() throws Exception {
        assertThat(forName("byte") == byte.class, is(true));
        assertThat(forName("java.lang.String[]") == String[].class, is(true));
        assertThat(forName("[Ljava.lang.String;") == String[].class, is(true));
    }

    @Test
    public void testForName3() throws Exception {
        ClassLoader classLoader = Mockito.mock(ClassLoader.class);
        forName("a.b.c.D", classLoader);
        verify(classLoader).loadClass("a.b.c.D");
    }

    @Test
    public void testResolvePrimitiveClassName() throws Exception {
        assertThat(resolvePrimitiveClassName("boolean") == boolean.class, is(true));
        assertThat(resolvePrimitiveClassName("byte") == byte.class, is(true));
        assertThat(resolvePrimitiveClassName("char") == char.class, is(true));
        assertThat(resolvePrimitiveClassName("double") == double.class, is(true));
        assertThat(resolvePrimitiveClassName("float") == float.class, is(true));
        assertThat(resolvePrimitiveClassName("int") == int.class, is(true));
        assertThat(resolvePrimitiveClassName("long") == long.class, is(true));
        assertThat(resolvePrimitiveClassName("short") == short.class, is(true));
        assertThat(resolvePrimitiveClassName("[Z") == boolean[].class, is(true));
        assertThat(resolvePrimitiveClassName("[B") == byte[].class, is(true));
        assertThat(resolvePrimitiveClassName("[C") == char[].class, is(true));
        assertThat(resolvePrimitiveClassName("[D") == double[].class, is(true));
        assertThat(resolvePrimitiveClassName("[F") == float[].class, is(true));
        assertThat(resolvePrimitiveClassName("[I") == int[].class, is(true));
        assertThat(resolvePrimitiveClassName("[J") == long[].class, is(true));
        assertThat(resolvePrimitiveClassName("[S") == short[].class, is(true));
    }

    @Test
    public void testToShortString() throws Exception {
        assertThat(toShortString(null), equalTo("null"));
        assertThat(toShortString(new ClassHelperTest()), startsWith("ClassHelperTest@"));
    }

    @Test
    public void testConvertPrimitive() {
        // Character
        Object resultCharacterEmpty = ClassHelper.convertPrimitive(Character.class, "");
        assertNull(resultCharacterEmpty);

        Object resultCharacterBlank = ClassHelper.convertPrimitive(Character.class, " ");
        assertEquals(' ', resultCharacterBlank);

        Object resultCharacterNormal = ClassHelper.convertPrimitive(Character.class, "abc");
        assertEquals('a', resultCharacterNormal);

        Object resultCharacterNull = ClassHelper.convertPrimitive(Character.class, null);
        assertNull(resultCharacterNull);

        // Boolean
        Object resultBoolEmpty = ClassHelper.convertPrimitive(Boolean.class, "");
        assertNotNull(resultBoolEmpty);
        assertTrue((resultBoolEmpty instanceof Boolean) && !((Boolean) resultBoolEmpty).booleanValue());

        Object resultBoolBlank = ClassHelper.convertPrimitive(Boolean.class, "  ");
        assertNotNull(resultBoolBlank);
        assertTrue((resultBoolBlank instanceof Boolean) && !((Boolean) resultBoolBlank).booleanValue());

        Object resultBoolNull = ClassHelper.convertPrimitive(Boolean.class, null);
        assertNull(resultBoolNull);

        Object resultBoolFalse = ClassHelper.convertPrimitive(Boolean.class, "false");
        assertTrue((resultBoolFalse instanceof Boolean) && !((Boolean) resultBoolFalse).booleanValue());

        Object resultBoolTrue = ClassHelper.convertPrimitive(Boolean.class, "true");
        assertTrue((resultBoolTrue instanceof Boolean) && ((Boolean) resultBoolTrue).booleanValue());

        // Byte
        Object resultByteEmpty = ClassHelper.convertPrimitive(Byte.class, "");
        assertNull(resultByteEmpty);

        Object resultByteBlank = ClassHelper.convertPrimitive(Byte.class, " ");
        assertNull(resultByteBlank);

        Object resultByteNull = ClassHelper.convertPrimitive(Byte.class, null);
        assertNull(resultByteNull);

        Object resultByteNormal = ClassHelper.convertPrimitive(Byte.class, "5");
        assertTrue((resultByteNormal instanceof Byte) && ((Byte) resultByteNormal).byteValue() == 5 );

        // Short
        Object resultShortEmpty = ClassHelper.convertPrimitive(Short.class, "");
        assertNull(resultShortEmpty);

        Object resultShortBlank = ClassHelper.convertPrimitive(Short.class, " ");
        assertNull(resultShortBlank);

        Object resultShortNull = ClassHelper.convertPrimitive(Short.class, null);
        assertNull(resultShortNull);

        Object resultShortNormal = ClassHelper.convertPrimitive(Short.class, "6");
        assertTrue((resultShortNormal instanceof Short) && ((Short) resultShortNormal).shortValue() == 6);

        // Integer
        Object resultIntEmpty = ClassHelper.convertPrimitive(Integer.class, "");
        assertNull(resultIntEmpty);

        Object resultIntBlank = ClassHelper.convertPrimitive(Integer.class, " ");
        assertNull(resultIntBlank);

        Object resultIntNull = ClassHelper.convertPrimitive(Integer.class, null);
        assertNull(resultIntNull);

        Object resultIntNormal = ClassHelper.convertPrimitive(Integer.class, "7");
        assertTrue((resultIntNormal instanceof Integer) && ((Integer) resultIntNormal).intValue() == 7);

        // Long
        Object resultLongEmpty = ClassHelper.convertPrimitive(Long.class, "");
        assertNull(resultLongEmpty);

        Object resultLongBlank = ClassHelper.convertPrimitive(Long.class, " ");
        assertNull(resultLongBlank);

        Object resultLongNull = ClassHelper.convertPrimitive(Long.class, null);
        assertNull(resultLongNull);

        Object resultLongNormal = ClassHelper.convertPrimitive(Long.class, "8");
        assertTrue((resultLongNormal instanceof Long) && ((Long) resultLongNormal).longValue() == 8);

        // Float
        Object resultFloatEmpty = ClassHelper.convertPrimitive(Float.class, "");
        assertNull(resultFloatEmpty);

        Object resultFloatBlank = ClassHelper.convertPrimitive(Float.class, " ");
        assertNull(resultFloatBlank);

        Object resultFloatNull = ClassHelper.convertPrimitive(Float.class, null);
        assertNull(resultFloatNull);

        Object resultFloatNormal = ClassHelper.convertPrimitive(Float.class, "9.9");
        assertTrue(resultFloatNormal != null && resultFloatNormal instanceof Float);

        // Double
        Object resultDoubleEmpty = ClassHelper.convertPrimitive(Double.class, "");
        assertNull(resultDoubleEmpty);

        Object resultDoubleBlank = ClassHelper.convertPrimitive(Double.class, " ");
        assertNull(resultDoubleBlank);

        Object resultDoubleNull = ClassHelper.convertPrimitive(Double.class, null);
        assertNull(resultDoubleNull);

        Object resultDoubleNormal = ClassHelper.convertPrimitive(Double.class, "10.11");
        assertTrue(resultDoubleNormal != null && resultDoubleNormal instanceof Double);
    }
}
