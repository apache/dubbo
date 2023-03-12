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

package org.apache.dubbo.metrics.aggregate;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaneTest {

    @Test
    void testIntervalInMs() {
        Pane<?> pane = mock(Pane.class);
        when(pane.getIntervalInMs()).thenReturn(100L);
        assertEquals(100L, pane.getIntervalInMs());
    }

    @Test
    void testStartInMs() {
        Pane<?> pane = mock(Pane.class);
        long startTime = System.currentTimeMillis();
        when(pane.getStartInMs()).thenReturn(startTime);
        assertEquals(startTime, pane.getStartInMs());
    }

    @Test
    void testEndInMs() {
        long startTime = System.currentTimeMillis();
        Pane<?> pane = new Pane<>(10, startTime, new Object());
        assertEquals(startTime + 10, pane.getEndInMs());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testValue() {
        Pane<LongAdder> pane = mock(Pane.class);
        LongAdder count = new LongAdder();
        when(pane.getValue()).thenReturn(count);
        assertEquals(count, pane.getValue());
        when(pane.getValue()).thenReturn(null);
        assertNotEquals(count, pane.getValue());
    }

    @Test
    void testIsTimeInWindow() {
        Pane<?> pane = new Pane<>(10, System.currentTimeMillis(), new Object());
        assertTrue(pane.isTimeInWindow(System.currentTimeMillis()));
        assertFalse(pane.isTimeInWindow(System.currentTimeMillis() + 10));
    }
}
