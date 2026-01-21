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

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MethodComparatorTest {

    // The test methods: we use interface since
    // It is the fastest way compared to class
    // which will need the curly braces
    interface TestInterface {
        void a();

        void b();

        void c(int i);

        void c(int i, int j);

        void d(Integer i);

        void d(String s);
    }

    @Test
    void testCompare() throws Exception {
        MethodComparator comparator = MethodComparator.INSTANCE;

        // Grab the Methods usind the Reflection
        Method a = TestInterface.class.getMethod("a");
        Method b = TestInterface.class.getMethod("b");
        Method c1 = TestInterface.class.getMethod("c", int.class);
        Method c2 = TestInterface.class.getMethod("c", int.class, int.class);
        Method dInt = TestInterface.class.getMethod("d", Integer.class);
        Method dStr = TestInterface.class.getMethod("d", String.class);

        // 1: Test Equals(The "Safety")
        assertEquals(0, comparator.compare(a, a));

        // 2:Names (a comes b4 b)
        assertTrue(comparator.compare(a, b) < 0);
        assertTrue(comparator.compare(b, a) > 0);

        // 3: Parameter count example for c1 and c2 method names are similar
        // so we check number of parameters
        assertTrue(comparator.compare(c1, c2) < 0);

        // 4: Parameter types eg Integer vs String
        // this is when they have same number of parameters
        assertTrue(comparator.compare(dInt, dStr) < 0);
    }
}
