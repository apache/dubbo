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
package org.apache.dubbo.rpc.cluster.merger;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResultMergerTest {

    /**
     * MergerFactory test
     */
    @Test
    public void testMergerFactoryIllegalArgumentException() {
        try {
            MergerFactory.getMerger(null);
            Assert.fail("expected IllegalArgumentException for null argument");
        } catch (IllegalArgumentException exception) {
            Assert.assertEquals("returnType is null", exception.getMessage());
        }
    }

    /**
     * ArrayMerger test
     */
    @Test
    public void testArrayMergerIllegalArgumentException() {
        String[] stringArray = {"1", "2", "3"};
        Integer[] integerArray = {3, 4, 5};
        try {
            Object result = ArrayMerger.INSTANCE.merge(stringArray, null, integerArray);
            Assert.fail("expected IllegalArgumentException for different arguments' types");
        } catch (IllegalArgumentException exception) {
            Assert.assertEquals("Arguments' types are different", exception.getMessage());
        }
    }

    /**
     * ArrayMerger test
     */
    @Test
    public void testArrayMerger() {
        String[] stringArray1 = {"1", "2", "3"};
        String[] stringArray2 = {"4", "5", "6"};
        String[] stringArray3 = {};

        Object result = ArrayMerger.INSTANCE.merge(stringArray1, stringArray2, stringArray3, null);
        Assert.assertTrue(result.getClass().isArray());
        Assert.assertEquals(6, Array.getLength(result));
        Assert.assertTrue(String.class.isInstance(Array.get(result, 0)));
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(String.valueOf(i + 1), Array.get(result, i));
        }

        Integer[] intArray1 = {1, 2, 3};
        Integer[] intArray2 = {4, 5, 6};
        Integer[] intArray3 = {7};
        // trigger ArrayMerger
        result = MergerFactory.getMerger(Integer[].class).merge(intArray1, intArray2, intArray3, null);
        Assert.assertTrue(result.getClass().isArray());
        Assert.assertEquals(7, Array.getLength(result));
        Assert.assertTrue(Integer.class == result.getClass().getComponentType());
        for (int i = 0; i < 7; i++) {
            Assert.assertEquals(i + 1, Array.get(result, i));
        }

        result = ArrayMerger.INSTANCE.merge(null);
        Assert.assertEquals(0, Array.getLength(result));

        result = ArrayMerger.INSTANCE.merge(null, null);
        Assert.assertEquals(0, Array.getLength(result));

        result = ArrayMerger.INSTANCE.merge(null, new Object[0]);
        Assert.assertEquals(0, Array.getLength(result));
    }

    /**
     * BooleanArrayMerger test
     */
    @Test
    public void testBooleanArrayMerger() {
        boolean[] arrayOne = {true, false};
        boolean[] arrayTwo = {false};
        boolean[] result = MergerFactory.getMerger(boolean[].class).merge(arrayOne, arrayTwo, null);
        Assert.assertEquals(3, result.length);
        boolean[] mergedResult = {true, false, false};
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertEquals(mergedResult[i], result[i]);
        }

        result = MergerFactory.getMerger(boolean[].class).merge(null);
        Assert.assertEquals(0, result.length);

        result = MergerFactory.getMerger(boolean[].class).merge(null, null);
        Assert.assertEquals(0, result.length);
    }

    /**
     * ByteArrayMerger test
     */
    @Test
    public void testByteArrayMerger() {
        byte[] arrayOne = {1, 2};
        byte[] arrayTwo = {1, 32};
        byte[] result = MergerFactory.getMerger(byte[].class).merge(arrayOne, arrayTwo, null);
        Assert.assertEquals(4, result.length);
        byte[] mergedResult = {1, 2, 1, 32};
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertEquals(mergedResult[i], result[i]);
        }

        result = MergerFactory.getMerger(byte[].class).merge(null);
        Assert.assertEquals(0, result.length);

        result = MergerFactory.getMerger(byte[].class).merge(null, null);
        Assert.assertEquals(0, result.length);
    }

    /**
     * CharArrayMerger test
     */
    @Test
    public void testCharArrayMerger() {
        char[] arrayOne = "hello".toCharArray();
        char[] arrayTwo = "world".toCharArray();
        char[] result = MergerFactory.getMerger(char[].class).merge(arrayOne, arrayTwo, null);
        Assert.assertEquals(10, result.length);
        char[] mergedResult = "helloworld".toCharArray();
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertEquals(mergedResult[i], result[i]);
        }

        result = MergerFactory.getMerger(char[].class).merge(null);
        Assert.assertEquals(0, result.length);

        result = MergerFactory.getMerger(char[].class).merge(null, null);
        Assert.assertEquals(0, result.length);
    }

    /**
     * DoubleArrayMerger test
     */
    @Test
    public void testDoubleArrayMerger() {
        double[] arrayOne = {1.2d, 3.5d};
        double[] arrayTwo = {2d, 34d};
        double[] result = MergerFactory.getMerger(double[].class).merge(arrayOne, arrayTwo, null);
        Assert.assertEquals(4, result.length);
        double[] mergedResult = {1.2d, 3.5d, 2d, 34d};
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertTrue(mergedResult[i] == result[i]);
        }

        result = MergerFactory.getMerger(double[].class).merge(null);
        Assert.assertEquals(0, result.length);

        result = MergerFactory.getMerger(double[].class).merge(null, null);
        Assert.assertEquals(0, result.length);
    }

    /**
     * FloatArrayMerger test
     */
    @Test
    public void testFloatArrayMerger() {
        float[] arrayOne = {1.2f, 3.5f};
        float[] arrayTwo = {2f, 34f};
        float[] result = MergerFactory.getMerger(float[].class).merge(arrayOne, arrayTwo, null);
        Assert.assertEquals(4, result.length);
        double[] mergedResult = {1.2f, 3.5f, 2f, 34f};
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertTrue(mergedResult[i] == result[i]);
        }

        result = MergerFactory.getMerger(float[].class).merge(null);
        Assert.assertEquals(0, result.length);

        result = MergerFactory.getMerger(float[].class).merge(null, null);
        Assert.assertEquals(0, result.length);
    }

    /**
     * IntArrayMerger test
     */
    @Test
    public void testIntArrayMerger() {
        int[] arrayOne = {1, 2};
        int[] arrayTwo = {2, 34};
        int[] result = MergerFactory.getMerger(int[].class).merge(arrayOne, arrayTwo, null);
        Assert.assertEquals(4, result.length);
        double[] mergedResult = {1, 2, 2, 34};
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertTrue(mergedResult[i] == result[i]);
        }

        result = MergerFactory.getMerger(int[].class).merge(null);
        Assert.assertEquals(0, result.length);

        result = MergerFactory.getMerger(int[].class).merge(null, null);
        Assert.assertEquals(0, result.length);
    }

    /**
     * ListMerger test
     */
    @Test
    public void testListMerger() {
        List<Object> list1 = new ArrayList<Object>() {{
            add(null);
            add("1");
            add("2");
        }};
        List<Object> list2 = new ArrayList<Object>() {{
            add("3");
            add("4");
        }};

        List result = MergerFactory.getMerger(List.class).merge(list1, list2, null);
        Assert.assertEquals(5, result.size());
        ArrayList<String> expected = new ArrayList<String>() {{
            add(null);
            add("1");
            add("2");
            add("3");
            add("4");
        }};
        Assert.assertEquals(expected, result);

        result = MergerFactory.getMerger(List.class).merge(null);
        Assert.assertEquals(0, result.size());

        result = MergerFactory.getMerger(List.class).merge(null, null);
        Assert.assertEquals(0, result.size());
    }

    /**
     * LongArrayMerger test
     */
    @Test
    public void testMapArrayMerger() {
        Map<Object, Object> mapOne = new HashMap<Object, Object>() {{
            put("11", 222);
            put("223", 11);
        }};
        Map<Object, Object> mapTwo = new HashMap<Object, Object>() {{
            put("3333", 3232);
            put("444", 2323);
        }};
        Map<Object, Object> result = MergerFactory.getMerger(Map.class).merge(mapOne, mapTwo, null);
        Assert.assertEquals(4, result.size());
        Map<String, Integer> mergedResult = new HashMap<String, Integer>() {{
            put("11", 222);
            put("223", 11);
            put("3333", 3232);
            put("444", 2323);
        }};
        Assert.assertEquals(mergedResult, result);

        result = MergerFactory.getMerger(Map.class).merge(null);
        Assert.assertEquals(0, result.size());

        result = MergerFactory.getMerger(Map.class).merge(null, null);
        Assert.assertEquals(0, result.size());
    }

    /**
     * LongArrayMerger test
     */
    @Test
    public void testLongArrayMerger() {
        long[] arrayOne = {1l, 2l};
        long[] arrayTwo = {2l, 34l};
        long[] result = MergerFactory.getMerger(long[].class).merge(arrayOne, arrayTwo, null);
        Assert.assertEquals(4, result.length);
        double[] mergedResult = {1l, 2l, 2l, 34l};
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertTrue(mergedResult[i] == result[i]);
        }

        result = MergerFactory.getMerger(long[].class).merge(null);
        Assert.assertEquals(0, result.length);

        result = MergerFactory.getMerger(long[].class).merge(null, null);
        Assert.assertEquals(0, result.length);
    }

    /**
     * SetMerger test
     */
    @Test
    public void testSetMerger() {
        Set<Object> set1 = new HashSet<Object>() {{
            add(null);
            add("1");
            add("2");
        }};

        Set<Object> set2 = new HashSet<Object>() {{
            add("2");
            add("3");
        }};

        Set result = MergerFactory.getMerger(Set.class).merge(set1, set2, null);

        Assert.assertEquals(4, result.size());
        Assert.assertEquals(new HashSet<String>() {
            {
                add(null);
                add("1");
                add("2");
                add("3");
            }
        }, result);

        result = MergerFactory.getMerger(Set.class).merge(null);
        Assert.assertEquals(0, result.size());

        result = MergerFactory.getMerger(Set.class).merge(null, null);
        Assert.assertEquals(0, result.size());
    }

    /**
     * ShortArrayMerger test
     */
    @Test
    public void testShortArrayMerger() {
        short[] arrayOne = {1, 2};
        short[] arrayTwo = {2, 34};
        short[] result = MergerFactory.getMerger(short[].class).merge(arrayOne, arrayTwo, null);
        Assert.assertEquals(4, result.length);
        double[] mergedResult = {1, 2, 2, 34};
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertTrue(mergedResult[i] == result[i]);
        }

        result = MergerFactory.getMerger(short[].class).merge(null);
        Assert.assertEquals(0, result.length);

        result = MergerFactory.getMerger(short[].class).merge(null, null);
        Assert.assertEquals(0, result.length);
    }
}
