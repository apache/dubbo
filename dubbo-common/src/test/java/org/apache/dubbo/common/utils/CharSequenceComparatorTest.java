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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CharSequenceComparatorTest {

    @Test
    public void testCompare() {
        CharSequenceComparator comparator = CharSequenceComparator.INSTANCE;

        // 1: Identify Equal tests
        assertEquals(0, comparator.compare("dubbo", "dubbo"));

        // 2. Alphabet test
        assertTrue(comparator.compare("apple", "banana") < 0);
        assertTrue(comparator.compare("banana", "apple") > 0);

        // 3. Mixed types (String vs StringBuilder)
        StringBuilder sb = new StringBuilder("abc");
        String s = "abc";
        assertEquals(0, comparator.compare(s, sb));

        // case Sensitivity
        assertTrue(comparator.compare("Apple", "apple") < 0);

        // Length test
        assertTrue(comparator.compare("dubbo", "dubbo3") < 0);
    }
}
