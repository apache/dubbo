/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.common.utils;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class LRUCacheTest {
    @Test
    public void testCache() throws Exception {
        LRUCache<String, Integer> cache = new LRUCache<String, Integer>(3);
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        assertThat(cache.get("one"), equalTo(1));
        assertThat(cache.get("two"), equalTo(2));
        assertThat(cache.get("three"), equalTo(3));
        assertThat(cache.size(), equalTo(3));
        cache.put("four", 4);
        assertThat(cache.size(), equalTo(3));
        assertFalse(cache.containsKey("one"));
        assertTrue(cache.containsKey("two"));
        assertTrue(cache.containsKey("three"));
        assertTrue(cache.containsKey("four"));
        cache.remove("four");
        assertThat(cache.size(), equalTo(2));
        cache.put("five", 5);
        assertFalse(cache.containsKey("four"));
        assertTrue(cache.containsKey("five"));
        assertTrue(cache.containsKey("two"));
        assertTrue(cache.containsKey("three"));
        assertThat(cache.size(), equalTo(3));
    }

    @Test
    public void testCapacity() throws Exception {
        LRUCache<String, Integer> cache = new LRUCache<String, Integer>();
        assertThat(cache.getMaxCapacity(), equalTo(1000));
        cache.setMaxCapacity(10);
        assertThat(cache.getMaxCapacity(), equalTo(10));
    }
}
