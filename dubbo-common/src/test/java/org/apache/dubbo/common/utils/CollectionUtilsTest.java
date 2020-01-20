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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;
import static org.apache.dubbo.common.utils.CollectionUtils.isNotEmpty;
import static org.apache.dubbo.common.utils.CollectionUtils.ofSet;
import static org.apache.dubbo.common.utils.CollectionUtils.toMap;
import static org.apache.dubbo.common.utils.CollectionUtils.toStringMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CollectionUtilsTest {
    @Test
    public void testSort() throws Exception {
        List<Integer> list = new ArrayList<Integer>();
        list.add(100);
        list.add(10);
        list.add(20);

        List<Integer> expected = new ArrayList<Integer>();
        expected.add(10);
        expected.add(20);
        expected.add(100);

        assertEquals(expected, CollectionUtils.sort(list));
    }

    @Test
    public void testSortNull() throws Exception {
        assertNull(CollectionUtils.sort(null));

        assertTrue(CollectionUtils.sort(new ArrayList<Integer>()).isEmpty());
    }

    @Test
    public void testSortSimpleName() throws Exception {
        List<String> list = new ArrayList<String>();
        list.add("aaa.z");
        list.add("b");
        list.add(null);
        list.add("zzz.a");
        list.add("c");
        list.add(null);

        List<String> sorted = CollectionUtils.sortSimpleName(list);
        assertNull(sorted.get(0));
        assertNull(sorted.get(1));
    }

    @Test
    public void testSortSimpleNameNull() throws Exception {
        assertNull(CollectionUtils.sortSimpleName(null));

        assertTrue(CollectionUtils.sortSimpleName(new ArrayList<String>()).isEmpty());
    }

    @Test
    public void testSplitAll() throws Exception {
        assertNull(CollectionUtils.splitAll(null, null));
        assertNull(CollectionUtils.splitAll(null, "-"));

        assertTrue(CollectionUtils.splitAll(new HashMap<String, List<String>>(), "-").isEmpty());

        Map<String, List<String>> input = new HashMap<String, List<String>>();
        input.put("key1", Arrays.asList("1:a", "2:b", "3:c"));
        input.put("key2", Arrays.asList("1:a", "2:b"));
        input.put("key3", null);
        input.put("key4", new ArrayList<String>());

        Map<String, Map<String, String>> expected = new HashMap<String, Map<String, String>>();
        expected.put("key1", CollectionUtils.toStringMap("1", "a", "2", "b", "3", "c"));
        expected.put("key2", CollectionUtils.toStringMap("1", "a", "2", "b"));
        expected.put("key3", null);
        expected.put("key4", new HashMap<String, String>());

        assertEquals(expected, CollectionUtils.splitAll(input, ":"));
    }

    @Test
    public void testJoinAll() throws Exception {
        assertNull(CollectionUtils.joinAll(null, null));
        assertNull(CollectionUtils.joinAll(null, "-"));

        Map<String, List<String>> expected = new HashMap<String, List<String>>();
        expected.put("key1", Arrays.asList("1:a", "2:b", "3:c"));
        expected.put("key2", Arrays.asList("1:a", "2:b"));
        expected.put("key3", null);
        expected.put("key4", new ArrayList<String>());

        Map<String, Map<String, String>> input = new HashMap<String, Map<String, String>>();
        input.put("key1", CollectionUtils.toStringMap("1", "a", "2", "b", "3", "c"));
        input.put("key2", CollectionUtils.toStringMap("1", "a", "2", "b"));
        input.put("key3", null);
        input.put("key4", new HashMap<String, String>());

        Map<String, List<String>> output = CollectionUtils.joinAll(input, ":");
        for (Map.Entry<String, List<String>> entry : output.entrySet()) {
            if (entry.getValue() == null)
                continue;
            Collections.sort(entry.getValue());
        }

        assertEquals(expected, output);
    }

    @Test
    public void testJoinList() throws Exception {
        List<String> list = Arrays.asList();
        assertEquals("", CollectionUtils.join(list, "/"));

        list = Arrays.asList("x");
        assertEquals("x", CollectionUtils.join(list, "-"));

        list = Arrays.asList("a", "b");
        assertEquals("a/b", CollectionUtils.join(list, "/"));
    }

    @Test
    public void testMapEquals() throws Exception {
        assertTrue(CollectionUtils.mapEquals(null, null));
        assertFalse(CollectionUtils.mapEquals(null, new HashMap<String, String>()));
        assertFalse(CollectionUtils.mapEquals(new HashMap<String, String>(), null));

        assertTrue(CollectionUtils.mapEquals(CollectionUtils.toStringMap("1", "a", "2", "b"), CollectionUtils.toStringMap("1", "a", "2", "b")));
        assertFalse(CollectionUtils.mapEquals(CollectionUtils.toStringMap("1", "a"), CollectionUtils.toStringMap("1", "a", "2", "b")));
    }

    @Test
    public void testStringMap1() throws Exception {
        assertThat(toStringMap("key", "value"), equalTo(Collections.singletonMap("key", "value")));
    }

    @Test
    public void testStringMap2() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> toStringMap("key", "value", "odd"));
    }

    @Test
    public void testToMap1() throws Exception {
        assertTrue(CollectionUtils.toMap().isEmpty());

        Map<String, Integer> expected = new HashMap<String, Integer>();
        expected.put("a", 1);
        expected.put("b", 2);
        expected.put("c", 3);

        assertEquals(expected, CollectionUtils.toMap("a", 1, "b", 2, "c", 3));
    }

    @Test
    public void testToMap2() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> toMap("a", "b", "c"));
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertThat(isEmpty(null), is(true));
        assertThat(isEmpty(new HashSet()), is(true));
        assertThat(isEmpty(emptyList()), is(true));
    }

    @Test
    public void testIsNotEmpty() throws Exception {
        assertThat(isNotEmpty(singleton("a")), is(true));
    }

    @Test
    public void testOfSet() {
        Set<String> set = ofSet();
        assertEquals(emptySet(), set);

        set = ofSet(((String[]) null));
        assertEquals(emptySet(), set);

        set = ofSet("A", "B", "C");
        Set<String> expectedSet = new LinkedHashSet<>();
        expectedSet.add("A");
        expectedSet.add("B");
        expectedSet.add("C");
        assertEquals(expectedSet, set);
    }
}