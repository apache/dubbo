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

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CompatibleTypeUtilsTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testCompatibleTypeConvert() throws Exception {
        Object result;

        {
            Object input = new Object();
            result = CompatibleTypeUtils.compatibleTypeConvert(input, Date.class);
            assertSame(input, result);

            result = CompatibleTypeUtils.compatibleTypeConvert(input, null);
            assertSame(input, result);

            result = CompatibleTypeUtils.compatibleTypeConvert(null, Date.class);
            assertNull(result);
        }

        {
            result = CompatibleTypeUtils.compatibleTypeConvert("a", char.class);
            assertEquals(Character.valueOf('a'), (Character) result);

            result = CompatibleTypeUtils.compatibleTypeConvert("A", MyEnum.class);
            assertEquals(MyEnum.A, (MyEnum) result);

            result = CompatibleTypeUtils.compatibleTypeConvert("3", BigInteger.class);
            assertEquals(new BigInteger("3"), (BigInteger) result);

            result = CompatibleTypeUtils.compatibleTypeConvert("3", BigDecimal.class);
            assertEquals(new BigDecimal("3"), (BigDecimal) result);

            result = CompatibleTypeUtils.compatibleTypeConvert("2011-12-11 12:24:12", Date.class);
            assertEquals(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2011-12-11 12:24:12"), (Date) result);
        }

        {
            result = CompatibleTypeUtils.compatibleTypeConvert(3, byte.class);
            assertEquals(Byte.valueOf((byte) 3), (Byte) result);

            result = CompatibleTypeUtils.compatibleTypeConvert((byte) 3, int.class);
            assertEquals(Integer.valueOf(3), (Integer) result);

            result = CompatibleTypeUtils.compatibleTypeConvert(3, short.class);
            assertEquals(Short.valueOf((short) 3), (Short) result);

            result = CompatibleTypeUtils.compatibleTypeConvert((short) 3, int.class);
            assertEquals(Integer.valueOf(3), (Integer) result);

            result = CompatibleTypeUtils.compatibleTypeConvert(3, int.class);
            assertEquals(Integer.valueOf(3), (Integer) result);

            result = CompatibleTypeUtils.compatibleTypeConvert(3, long.class);
            assertEquals(Long.valueOf(3), (Long) result);

            result = CompatibleTypeUtils.compatibleTypeConvert(3L, int.class);
            assertEquals(Integer.valueOf(3), (Integer) result);

            result = CompatibleTypeUtils.compatibleTypeConvert(3L, BigInteger.class);
            assertEquals(BigInteger.valueOf(3L), (BigInteger) result);

            result = CompatibleTypeUtils.compatibleTypeConvert(BigInteger.valueOf(3L), int.class);
            assertEquals(Integer.valueOf(3), (Integer) result);
        }

        {
            result = CompatibleTypeUtils.compatibleTypeConvert(3D, float.class);
            assertEquals(Float.valueOf(3), (Float) result);

            result = CompatibleTypeUtils.compatibleTypeConvert(3F, double.class);
            assertEquals(Double.valueOf(3), (Double) result);

            result = CompatibleTypeUtils.compatibleTypeConvert(3D, double.class);
            assertEquals(Double.valueOf(3), (Double) result);

            result = CompatibleTypeUtils.compatibleTypeConvert(3D, BigDecimal.class);
            assertEquals(BigDecimal.valueOf(3D), (BigDecimal) result);

            result = CompatibleTypeUtils.compatibleTypeConvert(BigDecimal.valueOf(3D), double.class);
            assertEquals(Double.valueOf(3), (Double) result);
        }

        {
            List<String> list = new ArrayList<String>();
            list.add("a");
            list.add("b");

            Set<String> set = new HashSet<String>();
            set.add("a");
            set.add("b");

            String[] array = new String[]{"a", "b"};

            result = CompatibleTypeUtils.compatibleTypeConvert(array, List.class);
            assertEquals(ArrayList.class, result.getClass());
            assertEquals(2, ((List<String>) result).size());
            assertTrue(((List<String>) result).contains("a"));
            assertTrue(((List<String>) result).contains("b"));

            result = CompatibleTypeUtils.compatibleTypeConvert(set, List.class);
            assertEquals(ArrayList.class, result.getClass());
            assertEquals(2, ((List<String>) result).size());
            assertTrue(((List<String>) result).contains("a"));
            assertTrue(((List<String>) result).contains("b"));

            result = CompatibleTypeUtils.compatibleTypeConvert(array, CopyOnWriteArrayList.class);
            assertEquals(CopyOnWriteArrayList.class, result.getClass());
            assertEquals(2, ((List<String>) result).size());
            assertTrue(((List<String>) result).contains("a"));
            assertTrue(((List<String>) result).contains("b"));

            result = CompatibleTypeUtils.compatibleTypeConvert(set, CopyOnWriteArrayList.class);
            assertEquals(CopyOnWriteArrayList.class, result.getClass());
            assertEquals(2, ((List<String>) result).size());
            assertTrue(((List<String>) result).contains("a"));
            assertTrue(((List<String>) result).contains("b"));

            result = CompatibleTypeUtils.compatibleTypeConvert(set, String[].class);
            assertEquals(String[].class, result.getClass());
            assertEquals(2, ((String[]) result).length);
            assertTrue(((String[]) result)[0].equals("a") || ((String[]) result)[0].equals("b"));
            assertTrue(((String[]) result)[1].equals("a") || ((String[]) result)[1].equals("b"));

            result = CompatibleTypeUtils.compatibleTypeConvert(array, Set.class);
            assertEquals(HashSet.class, result.getClass());
            assertEquals(2, ((Set<String>) result).size());
            assertTrue(((Set<String>) result).contains("a"));
            assertTrue(((Set<String>) result).contains("b"));

            result = CompatibleTypeUtils.compatibleTypeConvert(list, Set.class);
            assertEquals(HashSet.class, result.getClass());
            assertEquals(2, ((Set<String>) result).size());
            assertTrue(((Set<String>) result).contains("a"));
            assertTrue(((Set<String>) result).contains("b"));

            result = CompatibleTypeUtils.compatibleTypeConvert(array, ConcurrentHashSet.class);
            assertEquals(ConcurrentHashSet.class, result.getClass());
            assertEquals(2, ((Set<String>) result).size());
            assertTrue(((Set<String>) result).contains("a"));
            assertTrue(((Set<String>) result).contains("b"));

            result = CompatibleTypeUtils.compatibleTypeConvert(list, ConcurrentHashSet.class);
            assertEquals(ConcurrentHashSet.class, result.getClass());
            assertEquals(2, ((Set<String>) result).size());
            assertTrue(((Set<String>) result).contains("a"));
            assertTrue(((Set<String>) result).contains("b"));

            result = CompatibleTypeUtils.compatibleTypeConvert(list, String[].class);
            assertEquals(String[].class, result.getClass());
            assertEquals(2, ((String[]) result).length);
            assertTrue(((String[]) result)[0].equals("a"));
            assertTrue(((String[]) result)[1].equals("b"));

        }

    }
}