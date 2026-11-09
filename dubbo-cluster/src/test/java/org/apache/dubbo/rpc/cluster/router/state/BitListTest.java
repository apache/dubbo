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
package org.apache.dubbo.rpc.cluster.router.state;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;

class BitListTest {
    @Test
    void test() {
        List<String> list = Arrays.asList("A", "B", "C");
        BitList<String> bitList = new BitList<>(list);
        Assertions.assertEquals(bitList.getOriginList(), list);
        Assertions.assertEquals(3, bitList.size());

        Assertions.assertEquals("A", bitList.get(0));
        Assertions.assertEquals("B", bitList.get(1));
        Assertions.assertEquals("C", bitList.get(2));

        Assertions.assertTrue(bitList.contains("A"));
        Assertions.assertTrue(bitList.contains("B"));
        Assertions.assertTrue(bitList.contains("C"));

        for (String str : bitList) {
            Assertions.assertTrue(list.contains(str));
        }

        Assertions.assertEquals(0, bitList.indexOf("A"));
        Assertions.assertEquals(1, bitList.indexOf("B"));
        Assertions.assertEquals(2, bitList.indexOf("C"));

        Object[] objects = bitList.toArray();
        for (Object obj : objects) {
            Assertions.assertTrue(list.contains(obj));
        }

        Object[] newObjects = new Object[1];
        Object[] copiedList = bitList.toArray(newObjects);
        Assertions.assertEquals(3, copiedList.length);
        Assertions.assertArrayEquals(copiedList, list.toArray());

        newObjects = new Object[10];
        copiedList = bitList.toArray(newObjects);
        Assertions.assertEquals(10, copiedList.length);
        Assertions.assertEquals("A", copiedList[0]);
        Assertions.assertEquals("B", copiedList[1]);
        Assertions.assertEquals("C", copiedList[2]);

        bitList.remove(0);
        Assertions.assertEquals("B", bitList.get(0));
        bitList.addIndex(0);
        Assertions.assertEquals("A", bitList.get(0));

        bitList.removeAll(list);
        Assertions.assertEquals(0, bitList.size());
        bitList.clear();
    }

    @Test
    void testIntersect() {
        List<String> aList = Arrays.asList("A", "B", "C");
        List<String> bList = Arrays.asList("A", "B");
        List<String> totalList = Arrays.asList("A", "B", "C");

        BitList<String> aBitList = new BitList<>(aList);
        BitList<String> bBitList = new BitList<>(bList);

        BitList<String> intersectBitList = aBitList.and(bBitList);
        Assertions.assertEquals(2, intersectBitList.size());
        Assertions.assertEquals(totalList.get(0), intersectBitList.get(0));
        Assertions.assertEquals(totalList.get(1), intersectBitList.get(1));

        aBitList.add("D");
        intersectBitList = aBitList.and(bBitList);
        Assertions.assertEquals(2, intersectBitList.size());
        Assertions.assertEquals(totalList.get(0), intersectBitList.get(0));
        Assertions.assertEquals(totalList.get(1), intersectBitList.get(1));
        Assertions.assertFalse(intersectBitList.contains("D"));
    }

    @Test
    void testIntersectTailList() {
        List<String> list = Arrays.asList("A", "B");
        BitList<String> bitList = new BitList<>(list);
        bitList.add("C");
        bitList.add("D");

        BitList<String> target = new BitList<>(list);
        target.remove("B");
        target.add("D");

        BitList<String> intersectBitList = bitList.and(target);
        Assertions.assertEquals(2, intersectBitList.size());
        Assertions.assertEquals("A", intersectBitList.get(0));
        Assertions.assertEquals("D", intersectBitList.get(1));
        Assertions.assertFalse(intersectBitList.contains("B"));
        Assertions.assertFalse(intersectBitList.contains("C"));
    }

    @Test
    void testIsEmpty() {
        List<String> list = Arrays.asList("A", "B", "C");
        BitList<String> bitList = new BitList<>(list);
        bitList.add("D");

        bitList.removeAll(list);
        Assertions.assertEquals(1, bitList.size());

        bitList.remove("D");
        Assertions.assertTrue(bitList.isEmpty());
    }

    @Test
    void testAdd() {
        List<String> list = Arrays.asList("A", "B", "C");
        BitList<String> bitList = new BitList<>(list);

        bitList.remove("A");

        Assertions.assertEquals(2, bitList.size());

        bitList.addAll(Collections.singletonList("A"));
        Assertions.assertEquals(3, bitList.size());

        bitList.addAll(Collections.singletonList("D"));
        Assertions.assertEquals(4, bitList.size());
        Assertions.assertEquals("D", bitList.get(3));
        Assertions.assertTrue(bitList.hasMoreElementInTailList());
        Assertions.assertEquals(Collections.singletonList("D"), bitList.getTailList());

        bitList.clear();

        bitList.addAll(Collections.singletonList("A"));
        Assertions.assertEquals(1, bitList.size());
        Assertions.assertEquals("A", bitList.get(0));
    }

    @Test
    void testAddAll() {
        List<String> list = Arrays.asList("A", "B", "C");
        BitList<String> bitList1 = new BitList<>(list);
        BitList<String> bitList2 = new BitList<>(list);

        bitList1.removeAll(list);
        Assertions.assertEquals(0, bitList1.size());

        bitList1.addAll(bitList2);

        Assertions.assertEquals(3, bitList1.size());
        Assertions.assertFalse(bitList1.hasMoreElementInTailList());

        bitList1.addAll(bitList2);

        Assertions.assertEquals(3, bitList1.size());
    }

    @Test
    void testGet() {
        List<String> list = Arrays.asList("A", "B", "C");
        BitList<String> bitList = new BitList<>(list);

        Assertions.assertEquals("A", bitList.get(0));
        Assertions.assertEquals("B", bitList.get(1));
        Assertions.assertEquals("C", bitList.get(2));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> bitList.get(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> bitList.get(3));

        bitList.add("D");
        Assertions.assertEquals("D", bitList.get(3));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> bitList.get(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> bitList.get(4));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> bitList.get(5));
    }

    @Test
    void testRemove() {
        List<String> list = Arrays.asList("A", "B", "C");
        BitList<String> bitList = new BitList<>(list);

        Assertions.assertTrue(bitList.remove("A"));
        Assertions.assertFalse(bitList.remove("A"));

        Assertions.assertTrue(bitList.removeAll(Collections.singletonList("B")));
        Assertions.assertFalse(bitList.removeAll(Collections.singletonList("B")));

        Assertions.assertFalse(bitList.removeAll(Collections.singletonList("D")));

        bitList.add("D");
        Assertions.assertTrue(bitList.removeAll(Collections.singletonList("D")));
        Assertions.assertFalse(bitList.hasMoreElementInTailList());

        bitList.add("A");
        bitList.add("E");
        bitList.add("F");

        Assertions.assertEquals(4, bitList.size());

        Assertions.assertFalse(bitList.removeAll(Collections.singletonList("D")));
        Assertions.assertTrue(bitList.removeAll(Collections.singletonList("A")));
        Assertions.assertTrue(bitList.removeAll(Collections.singletonList("C")));
        Assertions.assertTrue(bitList.removeAll(Collections.singletonList("E")));
        Assertions.assertTrue(bitList.removeAll(Collections.singletonList("F")));

        Assertions.assertTrue(bitList.isEmpty());
    }

    @Test
    void testRemoveIndex() {
        List<String> list = Arrays.asList("A", "B", "C");
        BitList<String> bitList = new BitList<>(list);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> bitList.remove(3));
        Assertions.assertNotNull(bitList.remove(1));
        Assertions.assertNotNull(bitList.remove(0));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> bitList.remove(1));

        bitList.add("D");
        Assertions.assertNotNull(bitList.remove(1));

        bitList.add("A");
        bitList.add("E");
        bitList.add("F");
        // A C E F
        Assertions.assertEquals(4, bitList.size());

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> bitList.remove(4));
        Assertions.assertEquals("F", bitList.remove(3));
        Assertions.assertEquals("E", bitList.remove(2));
        Assertions.assertEquals("C", bitList.remove(1));
        Assertions.assertEquals("A", bitList.remove(0));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> bitList.remove(0));

        Assertions.assertTrue(bitList.isEmpty());
    }

    @Test
    void testRetain() {
        List<String> list = Arrays.asList("A", "B", "C");
        BitList<String> bitList = new BitList<>(list);

        List<String> list1 = Arrays.asList("B", "C");
        Assertions.assertTrue(bitList.retainAll(list1));

        Assertions.assertTrue(bitList.containsAll(list1));
        Assertions.assertFalse(bitList.containsAll(list));
        Assertions.assertFalse(bitList.contains("A"));

        bitList = new BitList<>(list);
        bitList.add("D");
        bitList.add("E");
        List<String> list2 = Arrays.asList("B", "C", "D");
        Assertions.assertTrue(bitList.retainAll(list2));

        Assertions.assertTrue(bitList.containsAll(list2));
        Assertions.assertFalse(bitList.containsAll(list));
        Assertions.assertFalse(bitList.contains("A"));
        Assertions.assertFalse(bitList.contains("E"));
    }

    @Test
    void testIndex() {
        List<String> list = Arrays.asList("A", "B", "A");
        BitList<String> bitList = new BitList<>(list);

        Assertions.assertEquals(0, bitList.indexOf("A"));
        Assertions.assertEquals(2, bitList.lastIndexOf("A"));

        Assertions.assertEquals(-1, bitList.indexOf("D"));
        Assertions.assertEquals(-1, bitList.lastIndexOf("D"));

        bitList.add("D");
        bitList.add("E");

        Assertions.assertEquals(0, bitList.indexOf("A"));
        Assertions.assertEquals(2, bitList.lastIndexOf("A"));
        Assertions.assertEquals(3, bitList.indexOf("D"));
        Assertions.assertEquals(3, bitList.lastIndexOf("D"));

        Assertions.assertEquals(-1, bitList.indexOf("F"));
    }

    @Test
    void testSubList() {
        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        BitList<String> bitList = new BitList<>(list);
        bitList.addAll(Arrays.asList("F", "G", "H", "I"));

        BitList<String> subList1 = bitList.subList(0, 5);
        Assertions.assertEquals(Arrays.asList("A", "B", "C", "D", "E"), subList1);

        BitList<String> subList2 = bitList.subList(1, 5);
        Assertions.assertEquals(Arrays.asList("B", "C", "D", "E"), subList2);

        BitList<String> subList3 = bitList.subList(0, 4);
        Assertions.assertEquals(Arrays.asList("A", "B", "C", "D"), subList3);

        BitList<String> subList4 = bitList.subList(1, 4);
        Assertions.assertEquals(Arrays.asList("B", "C", "D"), subList4);

        BitList<String> subList5 = bitList.subList(2, 3);
        Assertions.assertEquals(Collections.singletonList("C"), subList5);
        Assertions.assertFalse(subList5.hasMoreElementInTailList());

        BitList<String> subList6 = bitList.subList(0, 9);
        Assertions.assertEquals(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I"), subList6);
        Assertions.assertEquals(Arrays.asList("F", "G", "H", "I"), subList6.getTailList());

        BitList<String> subList7 = bitList.subList(1, 8);
        Assertions.assertEquals(Arrays.asList("B", "C", "D", "E", "F", "G", "H"), subList7);
        Assertions.assertEquals(Arrays.asList("F", "G", "H"), subList7.getTailList());

        BitList<String> subList8 = bitList.subList(4, 8);
        Assertions.assertEquals(Arrays.asList("E", "F", "G", "H"), subList8);
        Assertions.assertEquals(Arrays.asList("F", "G", "H"), subList8.getTailList());

        BitList<String> subList9 = bitList.subList(5, 8);
        Assertions.assertEquals(Arrays.asList("F", "G", "H"), subList9);
        Assertions.assertEquals(Arrays.asList("F", "G", "H"), subList9.getTailList());

        BitList<String> subList10 = bitList.subList(6, 8);
        Assertions.assertEquals(Arrays.asList("G", "H"), subList10);
        Assertions.assertEquals(Arrays.asList("G", "H"), subList10.getTailList());

        BitList<String> subList11 = bitList.subList(6, 7);
        Assertions.assertEquals(Collections.singletonList("G"), subList11);
        Assertions.assertEquals(Collections.singletonList("G"), subList11.getTailList());
    }

    @Test
    void testListIterator1() {
        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        BitList<String> bitList = new BitList<>(list);

        ListIterator<String> listIterator = bitList.listIterator(2);
        ListIterator<String> expectedIterator = list.listIterator(2);

        while (listIterator.hasNext()) {
            Assertions.assertEquals(expectedIterator.nextIndex(), listIterator.nextIndex());
            Assertions.assertEquals(expectedIterator.next(), listIterator.next());
        }

        while (listIterator.hasPrevious()) {
            Assertions.assertEquals(expectedIterator.previousIndex(), listIterator.previousIndex());
            Assertions.assertEquals(expectedIterator.previous(), listIterator.previous());
        }
    }

    @Test
    @ValueSource(
            ints = {
                2,
            })
    void testListIterator2() {
        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        BitList<String> bitList = new BitList<>(list);
        bitList.addAll(Arrays.asList("F", "G", "H", "I"));
        List<String> expectedResult = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I");

        ListIterator<String> listIterator = bitList.listIterator(2);
        ListIterator<String> expectedIterator = expectedResult.listIterator(2);

        while (listIterator.hasNext()) {
            Assertions.assertEquals(expectedIterator.nextIndex(), listIterator.nextIndex());
            Assertions.assertEquals(expectedIterator.next(), listIterator.next());
        }

        while (listIterator.hasPrevious()) {
            Assertions.assertEquals(expectedIterator.previousIndex(), listIterator.previousIndex());
            Assertions.assertEquals(expectedIterator.previous(), listIterator.previous());
        }
    }

    @Test
    void testListIterator3() {
        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        BitList<String> bitList = new BitList<>(list);
        bitList.addAll(Arrays.asList("F", "G", "H", "I"));
        List<String> expectedResult = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I");

        ListIterator<String> listIterator = bitList.listIterator(7);
        ListIterator<String> expectedIterator = expectedResult.listIterator(7);

        while (listIterator.hasNext()) {
            Assertions.assertEquals(expectedIterator.nextIndex(), listIterator.nextIndex());
            Assertions.assertEquals(expectedIterator.next(), listIterator.next());
        }

        while (listIterator.hasPrevious()) {
            Assertions.assertEquals(expectedIterator.previousIndex(), listIterator.previousIndex());
            Assertions.assertEquals(expectedIterator.previous(), listIterator.previous());
        }
    }

    @Test
    void testListIterator4() {
        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        BitList<String> bitList = new BitList<>(list);
        bitList.addAll(Arrays.asList("F", "G", "H", "I"));
        List<String> expectedResult = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I");

        ListIterator<String> listIterator = bitList.listIterator(8);
        ListIterator<String> expectedIterator = expectedResult.listIterator(8);

        while (listIterator.hasNext()) {
            Assertions.assertEquals(expectedIterator.nextIndex(), listIterator.nextIndex());
            Assertions.assertEquals(expectedIterator.next(), listIterator.next());
        }

        while (listIterator.hasPrevious()) {
            Assertions.assertEquals(expectedIterator.previousIndex(), listIterator.previousIndex());
            Assertions.assertEquals(expectedIterator.previous(), listIterator.previous());
        }
    }

    @Test
    void testListIterator5() {
        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        BitList<String> bitList = new BitList<>(list);
        bitList.addAll(Arrays.asList("F", "G", "H", "I"));
        List<String> expectedResult = new LinkedList<>(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I"));

        ListIterator<String> listIterator = bitList.listIterator(2);
        ListIterator<String> expectedIterator = expectedResult.listIterator(2);

        while (listIterator.hasNext()) {
            Assertions.assertEquals(expectedIterator.nextIndex(), listIterator.nextIndex());
            Assertions.assertEquals(expectedIterator.next(), listIterator.next());
            listIterator.remove();
            expectedIterator.remove();
        }

        Assertions.assertEquals(expectedResult, bitList);

        while (listIterator.hasPrevious()) {
            Assertions.assertEquals(expectedIterator.previousIndex(), listIterator.previousIndex());
            Assertions.assertEquals(expectedIterator.previous(), listIterator.previous());
            listIterator.remove();
            expectedIterator.remove();
        }

        Assertions.assertEquals(expectedResult, bitList);
    }

    @Test
    void testListIterator6() {
        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        BitList<String> bitList = new BitList<>(list);
        bitList.addAll(Arrays.asList("F", "G", "H", "I"));
        List<String> expectedResult = new LinkedList<>(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I"));

        ListIterator<String> listIterator = bitList.listIterator(2);
        ListIterator<String> expectedIterator = expectedResult.listIterator(2);

        while (listIterator.hasPrevious()) {
            Assertions.assertEquals(expectedIterator.previousIndex(), listIterator.previousIndex());
            Assertions.assertEquals(expectedIterator.previous(), listIterator.previous());
            listIterator.remove();
            expectedIterator.remove();
        }

        Assertions.assertEquals(expectedResult, bitList);

        while (listIterator.hasNext()) {
            Assertions.assertEquals(expectedIterator.nextIndex(), listIterator.nextIndex());
            Assertions.assertEquals(expectedIterator.next(), listIterator.next());
            listIterator.remove();
            expectedIterator.remove();
        }

        Assertions.assertEquals(expectedResult, bitList);
    }

    @Test
    void testListIterator7() {
        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        BitList<String> bitList = new BitList<>(list);
        bitList.addAll(Arrays.asList("F", "G", "H", "I"));
        List<String> expectedResult = new LinkedList<>(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I"));

        ListIterator<String> listIterator = bitList.listIterator(7);
        ListIterator<String> expectedIterator = expectedResult.listIterator(7);

        while (listIterator.hasNext()) {
            Assertions.assertEquals(expectedIterator.nextIndex(), listIterator.nextIndex());
            Assertions.assertEquals(expectedIterator.next(), listIterator.next());
            listIterator.remove();
            expectedIterator.remove();
        }

        Assertions.assertEquals(expectedResult, bitList);

        while (listIterator.hasPrevious()) {
            Assertions.assertEquals(expectedIterator.previousIndex(), listIterator.previousIndex());
            Assertions.assertEquals(expectedIterator.previous(), listIterator.previous());
            listIterator.remove();
            expectedIterator.remove();
        }

        Assertions.assertEquals(expectedResult, bitList);
    }

    @Test
    void testListIterator8() {
        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        BitList<String> bitList = new BitList<>(list);
        bitList.addAll(Arrays.asList("F", "G", "H", "I"));
        List<String> expectedResult = new LinkedList<>(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I"));

        ListIterator<String> listIterator = bitList.listIterator(7);
        ListIterator<String> expectedIterator = expectedResult.listIterator(7);

        while (listIterator.hasPrevious()) {
            Assertions.assertEquals(expectedIterator.previousIndex(), listIterator.previousIndex());
            Assertions.assertEquals(expectedIterator.previous(), listIterator.previous());
            listIterator.remove();
            expectedIterator.remove();
        }

        Assertions.assertEquals(expectedResult, bitList);

        while (listIterator.hasNext()) {
            Assertions.assertEquals(expectedIterator.nextIndex(), listIterator.nextIndex());
            Assertions.assertEquals(expectedIterator.next(), listIterator.next());
            listIterator.remove();
            expectedIterator.remove();
        }

        Assertions.assertEquals(expectedResult, bitList);
    }

    @Test
    void testClone1() {
        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        BitList<String> bitList = new BitList<>(list);

        BitList<String> clone1 = bitList.clone();
        Assertions.assertNotSame(bitList, clone1);
        Assertions.assertEquals(bitList, clone1);

        HashSet<Object> set = new HashSet<>();
        set.add(bitList);
        set.add(clone1);
        Assertions.assertEquals(1, set.size());

        set.add(new LinkedList<>());
        Assertions.assertEquals(2, set.size());

        set.add(new LinkedList<>(Arrays.asList("A", "B", "C", "D", "E")));
        Assertions.assertEquals(2, set.size());
    }

    @Test
    void testClone2() {
        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        BitList<String> bitList = new BitList<>(list);
        bitList.addAll(Arrays.asList("F", "G"));

        BitList<String> clone1 = bitList.clone();
        Assertions.assertNotSame(bitList, clone1);
        Assertions.assertEquals(bitList, clone1);

        HashSet<Object> set = new HashSet<>();
        set.add(bitList);
        set.add(clone1);
        Assertions.assertEquals(1, set.size());

        set.add(new LinkedList<>());
        Assertions.assertEquals(2, set.size());

        set.add(new LinkedList<>(Arrays.asList("A", "B", "C", "D", "E", "F", "G")));
        Assertions.assertEquals(2, set.size());
    }

    @Test
    void testConcurrent() throws InterruptedException {
        for (int i = 0; i < 100000; i++) {
            BitList<String> bitList = new BitList<>(Collections.singletonList("test"));
            bitList.remove("test");

            CountDownLatch countDownLatch = new CountDownLatch(1);
            CountDownLatch countDownLatch2 = new CountDownLatch(2);

            Thread thread1 = new Thread(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                bitList.add("test");
                countDownLatch2.countDown();
            });

            AtomicReference<BitList<String>> ref = new AtomicReference<>();
            Thread thread2 = new Thread(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ref.set(bitList.clone());
                countDownLatch2.countDown();
            });

            thread1.start();
            thread2.start();

            countDownLatch.countDown();
            countDownLatch2.await();

            Assertions.assertDoesNotThrow(() -> ref.get().iterator().hasNext());
        }
    }

    @Test
    void testSubListAfterRemove() {
        // B and D have been removed (e.g. filtered out by a router), so the list is [A, C, E] + [F, G]
        BitList<String> bitList = new BitList<>(Arrays.asList("A", "B", "C", "D", "E"));
        bitList.remove("B");
        bitList.remove("D");
        bitList.add("F");
        bitList.add("G");
        List<String> expected = Arrays.asList("A", "C", "E", "F", "G");
        Assertions.assertEquals(expected, bitList);

        for (int from = 0; from <= expected.size(); from++) {
            for (int to = from; to <= expected.size(); to++) {
                BitList<String> subList = bitList.subList(from, to);
                Assertions.assertEquals(expected.subList(from, to), subList, "subList(" + from + ", " + to + ")");
                Assertions.assertEquals(to - from, subList.size());
            }
        }

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> bitList.subList(-1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> bitList.subList(0, 6));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> bitList.subList(3, 2));
    }

    @Test
    void testCopyConstructorFromBitList() {
        BitList<String> bitList = new BitList<>(Arrays.asList("A", "B", "C", "D", "E"));
        bitList.remove("A");
        bitList.remove("C");
        bitList.add("F");

        BitList<String> copy = new BitList<>(bitList);
        Assertions.assertEquals(Arrays.asList("B", "D", "E", "F"), copy);
        Assertions.assertEquals(bitList, copy);

        // the copy is independent of the source
        copy.add("G");
        copy.remove("F");
        copy.remove("B");
        Assertions.assertEquals(Arrays.asList("B", "D", "E", "F"), bitList);
        Assertions.assertEquals(Arrays.asList("D", "E", "G"), copy);

        BitList<String> emptyCopy = new BitList<>(bitList, true);
        Assertions.assertTrue(emptyCopy.isEmpty());
        Assertions.assertSame(bitList.getOriginList(), emptyCopy.getOriginList());
    }

    @Test
    void testListIteratorRemove() {
        BitList<String> bitList = new BitList<>(Arrays.asList("A", "B", "C", "D"));
        bitList.remove("B");
        bitList.add("E");
        List<String> expected = new LinkedList<>(Arrays.asList("A", "C", "D", "E"));
        Assertions.assertEquals(expected, bitList);

        ListIterator<String> iterator = bitList.listIterator();
        ListIterator<String> expectedIterator = expected.listIterator();

        Assertions.assertEquals(expectedIterator.next(), iterator.next());
        iterator.remove();
        expectedIterator.remove();
        // a second remove() without next()/previous() must not remove another element
        Assertions.assertThrows(IllegalStateException.class, iterator::remove);
        Assertions.assertEquals(expected, bitList);
        // the removed element must not be returned by previous()
        Assertions.assertEquals(expectedIterator.hasPrevious(), iterator.hasPrevious());
        Assertions.assertEquals(expectedIterator.nextIndex(), iterator.nextIndex());
        Assertions.assertEquals(expectedIterator.previousIndex(), iterator.previousIndex());

        Assertions.assertEquals(expectedIterator.next(), iterator.next());
        Assertions.assertEquals(expectedIterator.next(), iterator.next());
        Assertions.assertEquals(expectedIterator.previous(), iterator.previous());
        iterator.remove();
        expectedIterator.remove();
        Assertions.assertEquals(expected, bitList);
        Assertions.assertEquals(expectedIterator.nextIndex(), iterator.nextIndex());
        Assertions.assertEquals(expectedIterator.previous(), iterator.previous());

        while (expectedIterator.hasNext()) {
            Assertions.assertTrue(iterator.hasNext());
            Assertions.assertEquals(expectedIterator.nextIndex(), iterator.nextIndex());
            Assertions.assertEquals(expectedIterator.next(), iterator.next());
        }
        Assertions.assertFalse(iterator.hasNext());
        // at the end of the list nextIndex() is the list size
        Assertions.assertEquals(expected.size(), iterator.nextIndex());
        Assertions.assertEquals(expected.size() - 1, iterator.previousIndex());
    }

    @Test
    void testListIteratorMatchesArrayList() {
        Random random = new Random(0);
        for (int round = 0; round < 2000; round++) {
            List<String> origin = new ArrayList<>();
            int originSize = random.nextInt(6);
            for (int i = 0; i < originSize; i++) {
                origin.add("o" + i);
            }
            BitList<String> bitList = new BitList<>(origin);
            for (int i = 0; i < originSize; i++) {
                if (random.nextBoolean()) {
                    bitList.remove("o" + i);
                }
            }
            int tailSize = random.nextInt(3);
            for (int i = 0; i < tailSize; i++) {
                bitList.add("t" + i);
            }
            List<String> expected = new ArrayList<>(bitList);

            int start = random.nextInt(expected.size() + 1);
            ListIterator<String> iterator = bitList.listIterator(start);
            ListIterator<String> expectedIterator = expected.listIterator(start);
            for (int step = 0; step < 10; step++) {
                String context = "round " + round + " step " + step;
                switch (random.nextInt(3)) {
                    case 0:
                        Assertions.assertEquals(expectedIterator.hasNext(), iterator.hasNext(), context);
                        if (expectedIterator.hasNext()) {
                            Assertions.assertEquals(expectedIterator.next(), iterator.next(), context);
                        }
                        break;
                    case 1:
                        Assertions.assertEquals(expectedIterator.hasPrevious(), iterator.hasPrevious(), context);
                        if (expectedIterator.hasPrevious()) {
                            Assertions.assertEquals(expectedIterator.previous(), iterator.previous(), context);
                        }
                        break;
                    default:
                        try {
                            expectedIterator.remove();
                        } catch (IllegalStateException e) {
                            Assertions.assertThrows(IllegalStateException.class, iterator::remove, context);
                            break;
                        }
                        iterator.remove();
                }
                Assertions.assertEquals(expectedIterator.nextIndex(), iterator.nextIndex(), context);
                Assertions.assertEquals(expectedIterator.previousIndex(), iterator.previousIndex(), context);
                Assertions.assertEquals(expected, bitList, context);
            }
        }
    }
}
