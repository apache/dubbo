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
package com.alibaba.dubbo.common.utils;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

public class AtomicPositiveIntegerTest {
    AtomicPositiveInteger i1 = new AtomicPositiveInteger();

    AtomicPositiveInteger i2 = new AtomicPositiveInteger(127);

    AtomicPositiveInteger i3 = new AtomicPositiveInteger(Integer.MAX_VALUE);

    @Test
    public void test_get() throws Exception {
        assertEquals(0, i1.get());
        assertEquals(127, i2.get());
        assertEquals(Integer.MAX_VALUE, i3.get());
    }

    @Test
    public void test_set() throws Exception {
        i1.set(100);
        assertEquals(100, i1.get());

        try {
            i1.set(-1);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                    allOf(containsString("new value"), containsString("< 0")));
        }
    }

    @Test
    public void test_getAndIncrement() throws Exception {
        int get = i1.getAndIncrement();
        assertEquals(0, get);
        assertEquals(1, i1.get());

        get = i2.getAndIncrement();
        assertEquals(127, get);
        assertEquals(128, i2.get());

        get = i3.getAndIncrement();
        assertEquals(Integer.MAX_VALUE, get);
        assertEquals(0, i3.get());
    }

    @Test
    public void test_getAndDecrement() throws Exception {
        int get = i1.getAndDecrement();
        assertEquals(0, get);
        assertEquals(Integer.MAX_VALUE, i1.get());

        get = i2.getAndDecrement();
        assertEquals(127, get);
        assertEquals(126, i2.get());

        get = i3.getAndDecrement();
        assertEquals(Integer.MAX_VALUE, get);
        assertEquals(Integer.MAX_VALUE - 1, i3.get());
    }

    @Test
    public void test_incrementAndGet() throws Exception {
        int get = i1.incrementAndGet();
        assertEquals(1, get);
        assertEquals(1, i1.get());

        get = i2.incrementAndGet();
        assertEquals(128, get);
        assertEquals(128, i2.get());

        get = i3.incrementAndGet();
        assertEquals(0, get);
        assertEquals(0, i3.get());
    }

    @Test
    public void test_decrementAndGet() throws Exception {
        int get = i1.decrementAndGet();
        assertEquals(Integer.MAX_VALUE, get);
        assertEquals(Integer.MAX_VALUE, i1.get());

        get = i2.decrementAndGet();
        assertEquals(126, get);
        assertEquals(126, i2.get());

        get = i3.decrementAndGet();
        assertEquals(Integer.MAX_VALUE - 1, get);
        assertEquals(Integer.MAX_VALUE - 1, i3.get());
    }

    @Test
    public void test_getAndSet() throws Exception {
        int get = i1.getAndSet(100);
        assertEquals(0, get);
        assertEquals(100, i1.get());

        try {
            i1.getAndSet(-1);
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                    allOf(containsString("new value"), containsString("< 0")));
        }
    }

    @Test
    public void test_getAndAnd() throws Exception {
        int get = i1.getAndAdd(3);
        assertEquals(0, get);
        assertEquals(3, i1.get());

        get = i2.getAndAdd(3);
        assertEquals(127, get);
        assertEquals(127 + 3, i2.get());

        get = i3.getAndAdd(3);
        assertEquals(Integer.MAX_VALUE, get);
        assertEquals(2, i3.get());
    }


    @Test
    public void test_addAndGet() throws Exception {
        int get = i1.addAndGet(3);
        assertEquals(3, get);
        assertEquals(3, i1.get());

        get = i2.addAndGet(3);
        assertEquals(127 + 3, get);
        assertEquals(127 + 3, i2.get());

        get = i3.addAndGet(3);
        assertEquals(2, get);
        assertEquals(2, i3.get());
    }

    @Test
    public void test_equals() {
        assertEquals(new AtomicPositiveInteger(), new AtomicPositiveInteger());
        assertEquals(new AtomicPositiveInteger(1), new AtomicPositiveInteger(1));
    }
}