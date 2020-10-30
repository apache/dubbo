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

import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.utils.FieldUtils.findField;
import static org.apache.dubbo.common.utils.FieldUtils.getDeclaredField;
import static org.apache.dubbo.common.utils.FieldUtils.getFieldValue;
import static org.apache.dubbo.common.utils.FieldUtils.setFieldValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link FieldUtils} Test-cases
 *
 * @since 2.7.6
 */
public class FieldUtilsTest {

    @Test
    public void testGetDeclaredField() {
        assertEquals("a", getDeclaredField(A.class, "a").getName());
        assertEquals("b", getDeclaredField(B.class, "b").getName());
        assertEquals("c", getDeclaredField(C.class, "c").getName());
        assertNull(getDeclaredField(B.class, "a"));
        assertNull(getDeclaredField(C.class, "a"));
    }

    @Test
    public void testFindField() {
        assertEquals("a", findField(A.class, "a").getName());
        assertEquals("a", findField(new A(), "a").getName());
        assertEquals("a", findField(B.class, "a").getName());
        assertEquals("b", findField(B.class, "b").getName());
        assertEquals("a", findField(C.class, "a").getName());
        assertEquals("b", findField(C.class, "b").getName());
        assertEquals("c", findField(C.class, "c").getName());
    }

    @Test
    public void testGetFieldValue() {
        assertEquals("a", getFieldValue(new A(), "a"));
        assertEquals("a", getFieldValue(new B(), "a"));
        assertEquals("b", getFieldValue(new B(), "b"));
        assertEquals("a", getFieldValue(new C(), "a"));
        assertEquals("b", getFieldValue(new C(), "b"));
        assertEquals("c", getFieldValue(new C(), "c"));
    }

    @Test
    public void setSetFieldValue() {
        A a = new A();
        assertEquals("a", setFieldValue(a, "a", "x"));
        assertEquals("x", getFieldValue(a, "a"));
    }
}

class A {

    private String a = "a";

}

class B extends A {

    private String b = "b";

}

class C extends B {

    private String c = "c";
}
