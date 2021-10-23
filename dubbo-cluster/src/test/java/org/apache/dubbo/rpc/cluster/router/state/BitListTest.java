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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class BitListTest {
    @Test
    public void test() {
        List<String> list = Arrays.asList("A", "B", "C");
        BitList<String> bitList = new BitList<>(list);
        Assertions.assertEquals(bitList.getUnmodifiableList(), list);
        Assertions.assertEquals(3, bitList.size());

        Assertions.assertEquals("A", bitList.get(0));
        Assertions.assertEquals("B", bitList.get(1));
        Assertions.assertEquals("C", bitList.get(2));

        Assertions.assertTrue(bitList.contains("A"));
        Assertions.assertTrue(bitList.contains("B"));
        Assertions.assertTrue(bitList.contains("C"));

        Iterator<String> iterator = bitList.iterator();
        while (iterator.hasNext()) {
            String str = iterator.next();
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
        Assertions.assertEquals(copiedList.length, 3);
        Assertions.assertArrayEquals(copiedList, list.toArray());

        newObjects = new Object[10];
        copiedList = bitList.toArray(newObjects);
        Assertions.assertEquals(copiedList.length, 10);
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
    public void testIntersect() {
        List<String> aList = Arrays.asList("A", "B", "C");
        List<String> bList = Arrays.asList("A", "B");
        List<String> totalList = Arrays.asList("A", "B", "C");

        BitList<String> aBitList = new BitList<>(aList);
        BitList<String> bBitList = new BitList<>(bList);

        BitList<String> intersectBitList = aBitList.intersect(bBitList, totalList);
        Assertions.assertEquals(intersectBitList.size(), 2);
        Assertions.assertEquals(intersectBitList.get(0), totalList.get(0));
        Assertions.assertEquals(intersectBitList.get(1), totalList.get(1));
    }

    @Test
    public void testIsEmpty() {
        List<String> list = Arrays.asList("A", "B", "C");
        BitList<String> bitList = new BitList<>(list);

        bitList.removeAll(list);
        Assertions.assertTrue(bitList.isEmpty());
    }

    @Test
    public void testAdd() {
        List<String> list = Arrays.asList("A", "B", "C");
        BitList<String> bitList = new BitList<>(list);

        bitList.remove("A");

        Assertions.assertEquals(2, bitList.size());

        bitList.addAll(Collections.singletonList("A"));
        Assertions.assertEquals(3, bitList.size());

        Assertions.assertThrows(UnsupportedOperationException.class, ()->{
            bitList.addAll(Collections.singletonList("D"));
        });

        bitList.clear();

        Assertions.assertThrows(UnsupportedOperationException.class, ()->{
            bitList.addAll(Collections.singletonList("A"));
        });
    }

    @Test
    public void testRemove() {
        List<String> list = Arrays.asList("A", "B", "C");
        BitList<String> bitList = new BitList<>(list);

        Assertions.assertTrue(bitList.remove("A"));
        Assertions.assertFalse(bitList.remove("A"));

        Assertions.assertTrue(bitList.removeAll(Collections.singletonList("B")));
        Assertions.assertFalse(bitList.removeAll(Collections.singletonList("B")));

        Assertions.assertFalse(bitList.removeAll(Collections.singletonList("D")));
    }

    @Test
    public void testRetain() {
        List<String> list = Arrays.asList("A", "B", "C");
        BitList<String> bitList = new BitList<>(list);

        List<String> list1 = Arrays.asList("B", "C");
        Assertions.assertTrue(bitList.retainAll(list1));

        Assertions.assertTrue(bitList.containsAll(list1));
        Assertions.assertFalse(bitList.containsAll(list));
        Assertions.assertFalse(bitList.contains("A"));
    }

    @Test
    public void testIndex() {
        List<String> list = Arrays.asList("A", "B", "A");
        BitList<String> bitList = new BitList<>(list);

        Assertions.assertEquals(0, bitList.indexOf("A"));
        Assertions.assertEquals(2, bitList.lastIndexOf("A"));

        Assertions.assertEquals(-1, bitList.indexOf("D"));
        Assertions.assertEquals(-1, bitList.lastIndexOf("D"));
    }

    @Test
    public void testSubList() {
        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        BitList<String> bitList = new BitList<>(list);

        List<String> subList1 = bitList.subList(0, 5);
        Assertions.assertEquals(Arrays.asList("A", "B", "C", "D", "E"), subList1);

        List<String> subList2 = bitList.subList(1, 5);
        Assertions.assertEquals(Arrays.asList("B", "C", "D", "E"), subList2);

        List<String> subList3 = bitList.subList(0, 4);
        Assertions.assertEquals(Arrays.asList("A", "B", "C", "D"), subList3);

        List<String> subList4 = bitList.subList(1, 4);
        Assertions.assertEquals(Arrays.asList("B", "C", "D"), subList4);

        List<String> subList5 = bitList.subList(2, 3);
        Assertions.assertEquals(Collections.singletonList("C"), subList5);
    }

    @Test
    public void testListIterator() {
        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        BitList<String> bitList = new BitList<>(list);

        ListIterator<String> listIterator = bitList.listIterator(2);

        int index = 2;
        while (listIterator.hasNext()) {
            Assertions.assertEquals(index, listIterator.nextIndex());
            Assertions.assertEquals(list.get(index++), listIterator.next());
        }

        index -= 2;
        while (listIterator.hasPrevious()) {
            Assertions.assertEquals(index, listIterator.previousIndex());
            Assertions.assertEquals(list.get(index--), listIterator.previous());
        }
    }

    @Test
    public void testClone() {
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
}
