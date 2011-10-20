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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.util.Date;

import org.junit.Test;

public class CompatibleTypeUtilsTest {

    @Test
    public void testIsIntegerLikeType() {
        Class<?>[] types = { byte.class, Byte.class, short.class, Short.class, int.class,
                Integer.class, long.class, Long.class, };

        for (Class<?> c : types) {
            assertTrue(CompatibleTypeUtils.isIntegerLikeType(c));
        }

        assertFalse(CompatibleTypeUtils.isIntegerLikeType(double.class));
        assertFalse(CompatibleTypeUtils.isIntegerLikeType(Date.class));
    }

    @Test
    public void testIsFloatLikeType() {
        Class<?>[] types = { float.class, Float.class, double.class, Double.class };

        for (Class<?> c : types) {
            assertTrue(CompatibleTypeUtils.isFloatLikeType(c));
        }

        assertFalse(CompatibleTypeUtils.isFloatLikeType(int.class));
        assertFalse(CompatibleTypeUtils.isFloatLikeType(Date.class));
    }

    @Test
    public void testConvertIntegerLikeType() {
        {
            Object v = CompatibleTypeUtils.convertIntegerLikeType(3, byte.class);
            assertEquals((byte) 3, ((Byte) v).byteValue());
        }
        {
            Object v = CompatibleTypeUtils.convertIntegerLikeType(3L, Short.class);
            assertEquals((short) 3, ((Short) v).shortValue());
        }
        {
            Object v = CompatibleTypeUtils.convertIntegerLikeType((short) 3, int.class);
            assertEquals(3, ((Integer) v).intValue());
        }
        {
            Object v = CompatibleTypeUtils.convertIntegerLikeType((short) 3, long.class);
            assertEquals(Long.class, v.getClass());
            assertEquals(3L, ((Long) v).longValue());
        }
        {
            Object v = CompatibleTypeUtils.convertIntegerLikeType((short) 3, Long.class);
            assertEquals(Long.class, v.getClass());
            assertEquals(3L, ((Long) v).longValue());
        }
    }

    @Test
    public void testConvertFloatLikeType() {
        {
            Object v = CompatibleTypeUtils.convertFloatLikeType(3D, float.class);
            assertEquals(Float.class, v.getClass());
            assertEquals(3F, ((Float) v).floatValue(), 0);
        }
        {
            Object v = CompatibleTypeUtils.convertFloatLikeType(3F, double.class);
            assertEquals(Double.class, v.getClass());
            assertEquals(3D, ((Double) v).doubleValue(), 0);
        }
        {
            Object v = CompatibleTypeUtils.convertFloatLikeType(3F, Double.class);
            assertEquals(Double.class, v.getClass());
            assertEquals(3D, ((Double) v).doubleValue(), 0);
        }
    }

    @Test
    public void testIsCharType() {
        Class<?>[] types = { char.class, Character.class, };

        for (Class<?> c : types) {
            assertTrue(CompatibleTypeUtils.isCharType(c));
        }

        assertFalse(CompatibleTypeUtils.isCharType(double.class));
        assertFalse(CompatibleTypeUtils.isCharType(Date.class));
    }

    @Test
    public void testCovert2Char() {
        Character c = CompatibleTypeUtils.covert2Char("a");
        assertEquals(Character.valueOf('a'), c);
    }

    @Test
    public void testCovert2Char_exception_moreChar() {
        try {
            CompatibleTypeUtils.covert2Char("ab");
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("the String MUST only 1 char"));
        }
    }

    @Test
    public void testNeedCompatibleTypeConvert() {
        assertTrue(CompatibleTypeUtils.needCompatibleTypeConvert("abc", char.class));
        assertTrue(CompatibleTypeUtils.needCompatibleTypeConvert(3L, int.class));
        assertTrue(CompatibleTypeUtils.needCompatibleTypeConvert(3F, double.class));

        // 类型参数为null时，要返回False
        assertFalse(CompatibleTypeUtils.needCompatibleTypeConvert("a", null));
        
        assertFalse(CompatibleTypeUtils.needCompatibleTypeConvert(null, String.class));
        assertFalse(CompatibleTypeUtils.needCompatibleTypeConvert(null, int.class));
        assertFalse(CompatibleTypeUtils.needCompatibleTypeConvert(null, Date.class));

        assertFalse(CompatibleTypeUtils.needCompatibleTypeConvert("abc", String.class));
        assertTrue(CompatibleTypeUtils.needCompatibleTypeConvert("abc", Date.class));
        assertFalse(CompatibleTypeUtils.needCompatibleTypeConvert(3D, Double.class));
    }

    @Test
    public void testCompatibleTypeConvert() {
        {
            Object v = CompatibleTypeUtils.compatibleTypeConvert("a", char.class);
            assertEquals(Character.valueOf('a'), (Character) v);
        }
        {
            Object v = CompatibleTypeUtils.compatibleTypeConvert(3L, int.class);
            assertEquals(Integer.valueOf(3), (Integer) v);
        }
        {
            Object v = CompatibleTypeUtils.compatibleTypeConvert(3F, Double.class);
            assertEquals(Double.valueOf(3), (Double) v, 0);
        }
        {
            Object input = new Object();
            Object v = CompatibleTypeUtils.compatibleTypeConvert(input, Date.class);
            assertSame(input, v);
        }
    }
}