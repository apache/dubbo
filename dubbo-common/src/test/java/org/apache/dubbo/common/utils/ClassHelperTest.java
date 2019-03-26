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
import static org.junit.Assert.assertThat;
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
}
