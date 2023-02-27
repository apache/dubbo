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

import org.apache.dubbo.rpc.cluster.Merger;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

class ResultMergerTest {
    private MergerFactory mergerFactory;
    
    @BeforeEach
    public void setup() {
        mergerFactory = new MergerFactory();
        mergerFactory.setScopeModel(ApplicationModel.defaultModel());
    }

    /**
     * MergerFactory test
     */
    @Test
    void testMergerFactoryIllegalArgumentException() {
        try {
            mergerFactory.getMerger(null);
            Assertions.fail("expected IllegalArgumentException for null argument");
        } catch (IllegalArgumentException exception) {
            Assertions.assertEquals("returnType is null", exception.getMessage());
        }
    }

    /**
     * ArrayMerger test
     */
    @Test
    void testArrayMergerIllegalArgumentException() {
        String[] stringArray = {"1", "2", "3"};
        Integer[] integerArray = {3, 4, 5};
        try {
            Object result = ArrayMerger.INSTANCE.merge(stringArray, null, integerArray);
            Assertions.fail("expected IllegalArgumentException for different arguments' types");
        } catch (IllegalArgumentException exception) {
            Assertions.assertEquals("Arguments' types are different", exception.getMessage());
        }
    }

    /**
     * ArrayMerger test
     */
    @Test
    void testArrayMerger() {
        String[] stringArray1 = {"1", "2", "3"};
        String[] stringArray2 = {"4", "5", "6"};
        String[] stringArray3 = {};

        Object result = ArrayMerger.INSTANCE.merge(stringArray1, stringArray2, stringArray3, null);
        Assertions.assertTrue(result.getClass().isArray());
        Assertions.assertEquals(6, Array.getLength(result));
        Assertions.assertTrue(String.class.isInstance(Array.get(result, 0)));
        for (int i = 0; i < 6; i++) {
            Assertions.assertEquals(String.valueOf(i + 1), Array.get(result, i));
        }

        Integer[] intArray1 = {1, 2, 3};
        Integer[] intArray2 = {4, 5, 6};
        Integer[] intArray3 = {7};
        // trigger ArrayMerger
        result = mergerFactory.getMerger(Integer[].class).merge(intArray1, intArray2, intArray3, null);
        Assertions.assertTrue(result.getClass().isArray());
        Assertions.assertEquals(7, Array.getLength(result));
        Assertions.assertSame(Integer.class, result.getClass().getComponentType());
        for (int i = 0; i < 7; i++) {
            Assertions.assertEquals(i + 1, Array.get(result, i));
        }

        result = ArrayMerger.INSTANCE.merge(null);
        Assertions.assertEquals(0, Array.getLength(result));

        result = ArrayMerger.INSTANCE.merge(null, null);
        Assertions.assertEquals(0, Array.getLength(result));

        result = ArrayMerger.INSTANCE.merge(null, new Object[0]);
        Assertions.assertEquals(0, Array.getLength(result));
    }

    /**
     * BooleanArrayMerger test
     */
    @Test
    void testBooleanArrayMerger() {
        boolean[] arrayOne = {true, false};
        boolean[] arrayTwo = {false};
        boolean[] result = mergerFactory.getMerger(boolean[].class).merge(arrayOne, arrayTwo, null);
        Assertions.assertEquals(3, result.length);
        boolean[] mergedResult = {true, false, false};
        for (int i = 0; i < mergedResult.length; i++) {
            Assertions.assertEquals(mergedResult[i], result[i]);
        }

        result = mergerFactory.getMerger(boolean[].class).merge(null);
        Assertions.assertEquals(0, result.length);

        result = mergerFactory.getMerger(boolean[].class).merge(null, null);
        Assertions.assertEquals(0, result.length);
    }

    /**
     * ByteArrayMerger test
     */
    @Test
    void testByteArrayMerger() {
        byte[] arrayOne = {1, 2};
        byte[] arrayTwo = {1, 32};
        byte[] result = mergerFactory.getMerger(byte[].class).merge(arrayOne, arrayTwo, null);
        Assertions.assertEquals(4, result.length);
        byte[] mergedResult = {1, 2, 1, 32};
        for (int i = 0; i < mergedResult.length; i++) {
            Assertions.assertEquals(mergedResult[i], result[i]);
        }

        result = mergerFactory.getMerger(byte[].class).merge(null);
        Assertions.assertEquals(0, result.length);

        result = mergerFactory.getMerger(byte[].class).merge(null, null);
        Assertions.assertEquals(0, result.length);
    }

    /**
     * CharArrayMerger test
     */
    @Test
    void testCharArrayMerger() {
        char[] arrayOne = "hello".toCharArray();
        char[] arrayTwo = "world".toCharArray();
        char[] result = mergerFactory.getMerger(char[].class).merge(arrayOne, arrayTwo, null);
        Assertions.assertEquals(10, result.length);
        char[] mergedResult = "helloworld".toCharArray();
        for (int i = 0; i < mergedResult.length; i++) {
            Assertions.assertEquals(mergedResult[i], result[i]);
        }

        result = mergerFactory.getMerger(char[].class).merge(null);
        Assertions.assertEquals(0, result.length);

        result = mergerFactory.getMerger(char[].class).merge(null, null);
        Assertions.assertEquals(0, result.length);
    }

    /**
     * DoubleArrayMerger test
     */
    @Test
    void testDoubleArrayMerger() {
        double[] arrayOne = {1.2d, 3.5d};
        double[] arrayTwo = {2d, 34d};
        double[] result = mergerFactory.getMerger(double[].class).merge(arrayOne, arrayTwo, null);
        Assertions.assertEquals(4, result.length);
        double[] mergedResult = {1.2d, 3.5d, 2d, 34d};
        for (int i = 0; i < mergedResult.length; i++) {
            Assertions.assertEquals(mergedResult[i], result[i], 0.0);
        }

        result = mergerFactory.getMerger(double[].class).merge(null);
        Assertions.assertEquals(0, result.length);

        result = mergerFactory.getMerger(double[].class).merge(null, null);
        Assertions.assertEquals(0, result.length);
    }

    /**
     * FloatArrayMerger test
     */
    @Test
    void testFloatArrayMerger() {
        float[] arrayOne = {1.2f, 3.5f};
        float[] arrayTwo = {2f, 34f};
        float[] result = mergerFactory.getMerger(float[].class).merge(arrayOne, arrayTwo, null);
        Assertions.assertEquals(4, result.length);
        double[] mergedResult = {1.2f, 3.5f, 2f, 34f};
        for (int i = 0; i < mergedResult.length; i++) {
            Assertions.assertEquals(mergedResult[i], result[i], 0.0);
        }

        result = mergerFactory.getMerger(float[].class).merge(null);
        Assertions.assertEquals(0, result.length);

        result = mergerFactory.getMerger(float[].class).merge(null, null);
        Assertions.assertEquals(0, result.length);
    }

    /**
     * IntArrayMerger test
     */
    @Test
    void testIntArrayMerger() {
        int[] arrayOne = {1, 2};
        int[] arrayTwo = {2, 34};
        int[] result = mergerFactory.getMerger(int[].class).merge(arrayOne, arrayTwo, null);
        Assertions.assertEquals(4, result.length);
        double[] mergedResult = {1, 2, 2, 34};
        for (int i = 0; i < mergedResult.length; i++) {
            Assertions.assertEquals(mergedResult[i], result[i], 0.0);
        }

        result = mergerFactory.getMerger(int[].class).merge(null);
        Assertions.assertEquals(0, result.length);

        result = mergerFactory.getMerger(int[].class).merge(null, null);
        Assertions.assertEquals(0, result.length);
    }

    /**
     * ListMerger test
     */
    @Test
    void testListMerger() {
        List<Object> list1 = new ArrayList<Object>() {{
            add(null);
            add("1");
            add("2");
        }};
        List<Object> list2 = new ArrayList<Object>() {{
            add("3");
            add("4");
        }};

        List result = mergerFactory.getMerger(List.class).merge(list1, list2, null);
        Assertions.assertEquals(5, result.size());
        ArrayList<String> expected = new ArrayList<String>() {{
            add(null);
            add("1");
            add("2");
            add("3");
            add("4");
        }};
        Assertions.assertEquals(expected, result);

        result = mergerFactory.getMerger(List.class).merge(null);
        Assertions.assertEquals(0, result.size());

        result = mergerFactory.getMerger(List.class).merge(null, null);
        Assertions.assertEquals(0, result.size());
    }

    /**
     * LongArrayMerger test
     */
    @Test
    void testMapArrayMerger() {
        Map<Object, Object> mapOne = new HashMap<Object, Object>() {{
            put("11", 222);
            put("223", 11);
        }};
        Map<Object, Object> mapTwo = new HashMap<Object, Object>() {{
            put("3333", 3232);
            put("444", 2323);
        }};
        Map<Object, Object> result = mergerFactory.getMerger(Map.class).merge(mapOne, mapTwo, null);
        Assertions.assertEquals(4, result.size());
        Map<String, Integer> mergedResult = new HashMap<String, Integer>() {{
            put("11", 222);
            put("223", 11);
            put("3333", 3232);
            put("444", 2323);
        }};
        Assertions.assertEquals(mergedResult, result);

        result = mergerFactory.getMerger(Map.class).merge(null);
        Assertions.assertEquals(0, result.size());

        result = mergerFactory.getMerger(Map.class).merge(null, null);
        Assertions.assertEquals(0, result.size());
    }

    /**
     * LongArrayMerger test
     */
    @Test
    void testLongArrayMerger() {
        long[] arrayOne = {1L, 2L};
        long[] arrayTwo = {2L, 34L};
        long[] result = mergerFactory.getMerger(long[].class).merge(arrayOne, arrayTwo, null);
        Assertions.assertEquals(4, result.length);
        double[] mergedResult = {1L, 2L, 2L, 34L};
        for (int i = 0; i < mergedResult.length; i++) {
            Assertions.assertEquals(mergedResult[i], result[i], 0.0);
        }

        result = mergerFactory.getMerger(long[].class).merge(null);
        Assertions.assertEquals(0, result.length);

        result = mergerFactory.getMerger(long[].class).merge(null, null);
        Assertions.assertEquals(0, result.length);
    }

    /**
     * SetMerger test
     */
    @Test
    void testSetMerger() {
        Set<Object> set1 = new HashSet<Object>() {{
            add(null);
            add("1");
            add("2");
        }};

        Set<Object> set2 = new HashSet<Object>() {{
            add("2");
            add("3");
        }};

        Set result = mergerFactory.getMerger(Set.class).merge(set1, set2, null);

        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals(new HashSet<String>() {
            {
                add(null);
                add("1");
                add("2");
                add("3");
            }
        }, result);

        result = mergerFactory.getMerger(Set.class).merge(null);
        Assertions.assertEquals(0, result.size());

        result = mergerFactory.getMerger(Set.class).merge(null, null);
        Assertions.assertEquals(0, result.size());
    }

    /**
     * ShortArrayMerger test
     */
    @Test
    void testShortArrayMerger() {
        short[] arrayOne = {1, 2};
        short[] arrayTwo = {2, 34};
        short[] result = mergerFactory.getMerger(short[].class).merge(arrayOne, arrayTwo, null);
        Assertions.assertEquals(4, result.length);
        double[] mergedResult = {1, 2, 2, 34};
        for (int i = 0; i < mergedResult.length; i++) {
            Assertions.assertEquals(mergedResult[i], result[i], 0.0);
        }

        result = mergerFactory.getMerger(short[].class).merge(null);
        Assertions.assertEquals(0, result.length);

        result = mergerFactory.getMerger(short[].class).merge(null, null);
        Assertions.assertEquals(0, result.length);
    }

    /**
     * IntSumMerger test
     */
    @Test
    void testIntSumMerger() {
        Integer[] intArr = IntStream.rangeClosed(1, 100).boxed().toArray(Integer[]::new);
        Merger<Integer> merger = ApplicationModel.defaultModel().getExtension(Merger.class, "intsum");
        Assertions.assertEquals(5050, merger.merge(intArr));

        intArr = new Integer[]{};
        Assertions.assertEquals(0, merger.merge(intArr));
    }

    /**
     * DoubleSumMerger test
     */
    @Test
    void testDoubleSumMerger() {
        Double[] doubleArr = DoubleStream.iterate(1, v -> ++v).limit(100).boxed().toArray(Double[]::new);
        Merger<Double> merger = ApplicationModel.defaultModel().getExtension(Merger.class, "doublesum");
        Assertions.assertEquals(5050, merger.merge(doubleArr));

        doubleArr = new Double[]{};
        Assertions.assertEquals(0, merger.merge(doubleArr));
    }

    /**
     * FloatSumMerger test
     */
    @Test
    void testFloatSumMerger() {
        Float[] floatArr = Stream.iterate(1.0F, v -> ++v).limit(100).toArray(Float[]::new);
        Merger<Float> merger = ApplicationModel.defaultModel().getExtension(Merger.class, "floatsum");
        Assertions.assertEquals(5050, merger.merge(floatArr));

        floatArr = new Float[]{};
        Assertions.assertEquals(0, merger.merge(floatArr));
    }

    /**
     * LongSumMerger test
     */
    @Test
    void testLongSumMerger() {
        Long[] longArr = LongStream.rangeClosed(1, 100).boxed().toArray(Long[]::new);
        Merger<Long> merger = ApplicationModel.defaultModel().getExtension(Merger.class, "longsum");
        Assertions.assertEquals(5050, merger.merge(longArr));

        longArr = new Long[]{};
        Assertions.assertEquals(0, merger.merge(longArr));
    }

    /**
     * IntFindAnyMerger test
     */
    @Test
    void testIntFindAnyMerger() {
        Integer[] intArr = {1, 2, 3, 4};
        Merger<Integer> merger = ApplicationModel.defaultModel().getExtension(Merger.class, "intany");
        Assertions.assertNotNull(merger.merge(intArr));

        intArr = new Integer[]{};
        Assertions.assertNull(merger.merge(intArr));
    }

    /**
     * IntFindFirstMerger test
     */
    @Test
    void testIntFindFirstMerger() {
        Integer[] intArr = {1, 2, 3, 4};
        Merger<Integer> merger = ApplicationModel.defaultModel().getExtension(Merger.class, "intfirst");
        Assertions.assertEquals(1, merger.merge(intArr));

        intArr = new Integer[]{};
        Assertions.assertNull(merger.merge(intArr));
    }
}
