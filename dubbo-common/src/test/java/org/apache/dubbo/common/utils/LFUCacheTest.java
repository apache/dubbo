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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class LFUCacheTest {

    @Test
    void testCacheEviction() {
        LFUCache<String, Integer> cache = new LFUCache<>(8, 0.8f);
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        assertThat(cache.get("one"), equalTo(1));
        assertThat(cache.get("two"), equalTo(2));
        assertThat(cache.get("three"), equalTo(3));
        assertThat(cache.getSize(), equalTo(3));
        cache.put("four", 4);
        assertThat(cache.getSize(), equalTo(4));
        cache.put("five", 5);
        cache.put("six", 6);
        assertThat(cache.getSize(), equalTo(6));
        cache.put("seven", 7);
        cache.put("eight", 8);
        cache.put("nine", 9);
        assertThat(cache.getSize(), equalTo(2));
    }

    @Test
    void testCacheRemove() {
        LFUCache<String, Integer> cache = new LFUCache<>(8, 0.8f);
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        assertThat(cache.get("one"), equalTo(1));
        assertThat(cache.get("two"), equalTo(2));
        assertThat(cache.get("three"), equalTo(3));
        assertThat(cache.getSize(), equalTo(3));
        cache.put("four", 4);
        assertThat(cache.getSize(), equalTo(4));
        cache.remove("four");
        assertThat(cache.getSize(), equalTo(3));
        cache.put("five", 5);
        assertThat(cache.getSize(), equalTo(4));
        cache.put("six", 6);
        assertThat(cache.getSize(), equalTo(5));
    }

    @Test
    void testDefaultCapacity() {
        LFUCache<String, Integer> cache = new LFUCache<>();
        assertThat(cache.getCapacity(), equalTo(1000));
    }

    @Test
    void testErrorConstructArguments() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LFUCache<>(0, 0.8f));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LFUCache<>(-1, 0.8f));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LFUCache<>(100, 0.0f));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LFUCache<>(100, -0.1f));
    }
}
