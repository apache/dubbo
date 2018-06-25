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
     * ArrayMerger test
     *
     * @throws Exception
     */
    @Test
    public void testArrayMerger() throws Exception {
        String[] stringArray1 = {"1", "2", "3"};
        String[] stringArray2 = {"4", "5", "6"};
        String[] stringArray3 = {};

        Object result = ArrayMerger.INSTANCE.merge(stringArray1, stringArray2, stringArray3);
        Assert.assertTrue(result.getClass().isArray());
        Assert.assertEquals(6, Array.getLength(result));
        Assert.assertTrue(String.class.isInstance(Array.get(result, 0)));
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(String.valueOf(i + 1), Array.get(result, i));
        }

        int[] intArray1 = {1, 2, 3};
        int[] intArray2 = {4, 5, 6};
        int[] intArray3 = {7};
        result = MergerFactory.getMerger(int[].class).merge(intArray1, intArray2, intArray3);
        Assert.assertTrue(result.getClass().isArray());
        Assert.assertEquals(7, Array.getLength(result));
        Assert.assertTrue(int.class == result.getClass().getComponentType());
        for (int i = 0; i < 7; i++) {
            Assert.assertEquals(i + 1, Array.get(result, i));
        }

    }

    /**
     * BooleanArrayMerger test
     *
     * @throws Exception
     */
    @Test
    public void testBooleanArrayMerger() throws Exception {
        boolean[] arrayOne = {true, false};
        boolean[] arrayTwo = {false};
        boolean[] result = MergerFactory.getMerger(boolean[].class).merge(arrayOne, arrayTwo);
        Assert.assertEquals(3, result.length);
        boolean[] mergedResult = {true, false, false};
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertEquals(mergedResult[i], result[i]);
        }
    }

    /**
     * ByteArrayMerger test
     *
     * @throws Exception
     */
    @Test
    public void testByteArrayMerger() throws Exception {
        byte[] arrayOne = {1, 2};
        byte[] arrayTwo = {1, 32};
        byte[] result = MergerFactory.getMerger(byte[].class).merge(arrayOne, arrayTwo);
        Assert.assertEquals(4, result.length);
        byte[] mergedResult = {1, 2, 1, 32};
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertEquals(mergedResult[i], result[i]);
        }
    }

    /**
     * CharArrayMerger test
     *
     * @throws Exception
     */
    @Test
    public void testCharArrayMerger() throws Exception {
        char[] arrayOne = "hello".toCharArray();
        char[] arrayTwo = "world".toCharArray();
        char[] result = MergerFactory.getMerger(char[].class).merge(arrayOne, arrayTwo);
        Assert.assertEquals(10, result.length);
        char[] mergedResult = "helloworld".toCharArray();
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertEquals(mergedResult[i], result[i]);
        }
    }

    /**
     * DoubleArrayMerger test
     *
     * @throws Exception
     */
    @Test
    public void testDoubleArrayMerger() throws Exception {
        double[] arrayOne = {1.2d, 3.5d};
        double[] arrayTwo = {2d, 34d};
        double[] result = MergerFactory.getMerger(double[].class).merge(arrayOne, arrayTwo);
        Assert.assertEquals(4, result.length);
        double[] mergedResult = {1.2d, 3.5d, 2d, 34d};
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertTrue(mergedResult[i] == result[i]);
        }
    }

    /**
     * FloatArrayMerger test
     *
     * @throws Exception
     */
    @Test
    public void testFloatArrayMerger() throws Exception {
        float[] arrayOne = {1.2f, 3.5f};
        float[] arrayTwo = {2f, 34f};
        float[] result = MergerFactory.getMerger(float[].class).merge(arrayOne, arrayTwo);
        Assert.assertEquals(4, result.length);
        double[] mergedResult = {1.2f, 3.5f, 2f, 34f};
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertTrue(mergedResult[i] == result[i]);
        }
    }

    /**
     * IntArrayMerger test
     *
     * @throws Exception
     */
    @Test
    public void testIntArrayMerger() throws Exception {
        int[] arrayOne = {1, 2};
        int[] arrayTwo = {2, 34};
        int[] result = MergerFactory.getMerger(int[].class).merge(arrayOne, arrayTwo);
        Assert.assertEquals(4, result.length);
        double[] mergedResult = {1, 2, 2, 34};
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertTrue(mergedResult[i] == result[i]);
        }
    }

    /**
     * ListMerger test
     *
     * @throws Exception
     */
    @Test
    public void testListMerger() throws Exception {
        List<Object> list1 = new ArrayList<Object>(){{
            add(null);
            add("1");
            add("2"); 
        }};
        List<Object> list2 = new ArrayList<Object>(){{
            add("3");
            add("4");
        }};

        List result = MergerFactory.getMerger(List.class).merge(list1, list2);
        Assert.assertEquals(5, result.size());
        ArrayList<String> expected = new ArrayList<String>() {{
            add(null);
            add("1");
            add("2");
            add("3");
            add("4");
        }};
        Assert.assertEquals(expected, result);
    }

    /**
     * LongArrayMerger test
     *
     * @throws Exception
     */
    @Test
    public void testMapArrayMerger() throws Exception {
        Map<Object, Object> mapOne = new HashMap() {{
            put("11", 222);
            put("223", 11);
        }};
        Map<Object, Object> mapTwo = new HashMap() {{
            put("3333", 3232);
            put("444", 2323);
        }};
        Map<Object, Object> result = MergerFactory.getMerger(Map.class).merge(mapOne, mapTwo);
        Assert.assertEquals(4, result.size());
        Map<Object, Object> mergedResult = new HashMap() {{
            put("11", 222);
            put("223", 11);
            put("3333", 3232);
            put("444", 2323);
        }};
        Assert.assertEquals(mergedResult, result);
    }

    /**
     * LongArrayMerger test
     *
     * @throws Exception
     */
    @Test
    public void testLongArrayMerger() throws Exception {
        long[] arrayOne = {1l, 2l};
        long[] arrayTwo = {2l, 34l};
        long[] result = MergerFactory.getMerger(long[].class).merge(arrayOne, arrayTwo);
        Assert.assertEquals(4, result.length);
        double[] mergedResult = {1l, 2l, 2l, 34l};
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertTrue(mergedResult[i] == result[i]);
        }
    }

    /**
     * SetMerger test
     *
     * @throws Exception
     */
    @Test
    public void testSetMerger() throws Exception {
        Set<Object> set1 = new HashSet<Object>();
        set1.add(null);
        set1.add("1");
        set1.add("2");
        Set<Object> set2 = new HashSet<Object>();
        set2.add("2");
        set2.add("3");

        Set result = MergerFactory.getMerger(Set.class).merge(set1, set2);

        Assert.assertEquals(4, result.size());
        Assert.assertEquals(new HashSet<String>() {
            {
                add(null);
                add("1");
                add("2");
                add("3");
            }
        }, result);
    }

    /**
     * ShortArrayMerger test
     *
     * @throws Exception
     */
    @Test
    public void testShortArrayMerger() throws Exception {
        short[] arrayOne = {1, 2};
        short[] arrayTwo = {2, 34};
        short[] result = MergerFactory.getMerger(short[].class).merge(arrayOne, arrayTwo);
        Assert.assertEquals(4, result.length);
        double[] mergedResult = {1, 2, 2, 34};
        for (int i = 0; i < mergedResult.length; i++) {
            Assert.assertTrue(mergedResult[i] == result[i]);
        }
    }
}
