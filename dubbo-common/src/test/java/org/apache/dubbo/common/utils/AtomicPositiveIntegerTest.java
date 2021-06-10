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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class AtomicPositiveIntegerTest {
    private AtomicPositiveInteger i1 = new AtomicPositiveInteger();

    private AtomicPositiveInteger i2 = new AtomicPositiveInteger(127);

    private AtomicPositiveInteger i3 = new AtomicPositiveInteger(Integer.MAX_VALUE);

    @Test
    public void testGet() throws Exception {
        assertEquals(0, i1.get());
        assertEquals(127, i2.get());
        assertEquals(Integer.MAX_VALUE, i3.get());
    }

    @Test
    public void testSet() throws Exception {
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
    public void testGetAndIncrement() throws Exception {
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
    public void testGetAndDecrement() throws Exception {
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
    public void testIncrementAndGet() throws Exception {
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
    public void testDecrementAndGet() throws Exception {
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
    public void testGetAndSet() throws Exception {
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
    public void testGetAndAnd() throws Exception {
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
    public void testAddAndGet() throws Exception {
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
    public void testCompareAndSet1() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            i1.compareAndSet(i1.get(), -1);
        });
    }

    @Test
    public void testCompareAndSet2() {
        assertThat(i1.compareAndSet(i1.get(), 2), is(true));
        assertThat(i1.get(), is(2));
    }

    @Test
    public void testWeakCompareAndSet1() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            i1.weakCompareAndSet(i1.get(), -1);
        });
    }

    @Test
    public void testWeakCompareAndSet2() {
        assertThat(i1.weakCompareAndSet(i1.get(), 2), is(true));
        assertThat(i1.get(), is(2));
    }

    @Test
    public void testValues() throws Exception {
        Integer i = i1.get();
        assertThat(i1.byteValue(), equalTo(i.byteValue()));
        assertThat(i1.shortValue(), equalTo(i.shortValue()));
        assertThat(i1.intValue(), equalTo(i.intValue()));
        assertThat(i1.longValue(), equalTo(i.longValue()));
        assertThat(i1.floatValue(), equalTo(i.floatValue()));
        assertThat(i1.doubleValue(), equalTo(i.doubleValue()));
        assertThat(i1.toString(), equalTo(i.toString()));
    }

    @Test
    public void testEquals() {
        assertEquals(new AtomicPositiveInteger(), new AtomicPositiveInteger());
        assertEquals(new AtomicPositiveInteger(1), new AtomicPositiveInteger(1));
    }
}
