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

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentHashSetTest {

    @Test
    public void addTest() {
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
        // Action: Add "Apple". Expectation: Should return true (new item)
        assertTrue(set.add("Apple"));
        // Action: Add "Apple" again. Expectation: Should return false (already exists)
        assertFalse(set.add("Apple"));
        assertEquals(1, set.size());
    }

    @Test
    public void containTest() {
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
        set.add("Banana");
        // Check if it finds what's there and ignores what isn't
        assertTrue(set.contains("Banana"));
        assertFalse(set.contains("Orange"));
    }

    @Test
    public void sizeTest() {
        ConcurrentHashSet<Integer> set = new ConcurrentHashSet<>();
        set.add(1);
        set.add(2);
        assertEquals(2, set.size());
    }

    @Test
    public void isEmptyTest() {
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
        assertTrue(set.isEmpty()); // Should be true at start
        set.add("Item");
        assertFalse(set.isEmpty()); // Should be false now,since item is in
    }

    @Test
    public void removeTest() {
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
        set.add("apple");
        // True it removes because there is apple in there
        assertTrue(set.remove("apple"));
        // Now the set is empty
        assertEquals(0, set.size());
    }

    @Test
    public void iteratorTest() {
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
        set.add("A");
        set.add("B");

        Iterator<String> it = set.iterator();
        int count = 0;
        while (it.hasNext()) {
            String value = it.next();
            // This checks every item the iterator finds is in set
            assertTrue(set.contains(value));
            count += 1;
        }
        assertEquals(2, count);
    }

    @Test
    public void clearTest() {
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
        set.add("A");
        set.add("B");
        set.clear();
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
    }
}
