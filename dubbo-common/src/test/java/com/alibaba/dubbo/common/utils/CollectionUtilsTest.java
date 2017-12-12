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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author ding.lid
 */
public class CollectionUtilsTest {
    @Test
    public void test_sort() throws Exception {
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
    public void test_sort_null() throws Exception {
        assertNull(CollectionUtils.sort(null));

        assertTrue(CollectionUtils.sort(new ArrayList<Integer>()).isEmpty());
    }

    @Test
    public void test_sortSimpleName() throws Exception {
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
    public void test_sortSimpleName_null() throws Exception {
        assertNull(CollectionUtils.sortSimpleName(null));

        assertTrue(CollectionUtils.sortSimpleName(new ArrayList<String>()).isEmpty());
    }

    @Test
    public void test_splitAll() throws Exception {
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
    public void test_joinAll() throws Exception {
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
    public void test_joinList() throws Exception {
        List<String> list = Arrays.asList();
        assertEquals("", CollectionUtils.join(list, "/"));

        list = Arrays.asList("x");
        assertEquals("x", CollectionUtils.join(list, "-"));

        list = Arrays.asList("a", "b");
        assertEquals("a/b", CollectionUtils.join(list, "/"));
    }

    @Test
    public void test_mapEquals() throws Exception {
        assertTrue(CollectionUtils.mapEquals(null, null));
        assertFalse(CollectionUtils.mapEquals(null, new HashMap<String, String>()));
        assertFalse(CollectionUtils.mapEquals(new HashMap<String, String>(), null));

        assertTrue(CollectionUtils.mapEquals(CollectionUtils.toStringMap("1", "a", "2", "b"), CollectionUtils.toStringMap("1", "a", "2", "b")));
        assertFalse(CollectionUtils.mapEquals(CollectionUtils.toStringMap("1", "a"), CollectionUtils.toStringMap("1", "a", "2", "b")));
    }

    @Test
    public void test_toMap() throws Exception {
        assertTrue(CollectionUtils.toMap().isEmpty());


        Map<String, Integer> expected = new HashMap<String, Integer>();
        expected.put("a", 1);
        expected.put("b", 2);
        expected.put("c", 3);

        assertEquals(expected, CollectionUtils.toMap("a", 1, "b", 2, "c", 3));
    }
}