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
    public void iteratorTest() {
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
        set.add("A");
        set.add("B");

        Iterator<String> it = set.iterator();
        int count = 0;

        while (it.hasNext()) {
            it.next();
            count += 1;
        }
        assertEquals(2, count);
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
        assertTrue(set.isEmpty()); // since I have not add anything it is true
        set.add("pen");
        assertFalse(set.isEmpty()); // It has sth in , so it is false if I say isEmpty
    }

    @Test
    public void containTest() {
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
        set.add("Banana");
        // will find banana because it is there
        assertTrue(set.contains("Banana"));
        // will ignore Orange because we don't have it
        assertFalse(set.contains("Orange"));
    }

    @Test
    public void addTest() {
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
        assertTrue(set.add("Apple"));
        // If i add apple again , will return false since it is there
        assertFalse(set.add("Apple"));
        // size will retain 1
        assertEquals(1, set.size());
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
    public void clearTest() {
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
        set.add("A");
        set.add("B");

        set.clear();

        // now it is empty
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
    }
}
