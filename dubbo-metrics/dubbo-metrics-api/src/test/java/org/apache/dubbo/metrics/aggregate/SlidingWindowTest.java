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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlidingWindowTest {

    static final int paneCount = 10;

    static final long intervalInMs = 2000;

    TestSlidingWindow window;

    @BeforeEach
    void setup() {
        window = new TestSlidingWindow(paneCount, intervalInMs);
    }

    @Test
    void testCurrentPane() {
        assertNull(window.currentPane(/* invalid time*/-1L));
        long timeInMs = System.currentTimeMillis();
        Pane<LongAdder> currentPane = window.currentPane(timeInMs);
        assertNotNull(currentPane);
        // reuse test
        assertEquals(currentPane,
            window.currentPane(1 + timeInMs + window.getPaneIntervalInMs() * paneCount));
    }

    @Test
    void testGetPaneData() {
        assertNull(window.getPaneValue(/* invalid time*/-1L));
        window.currentPane();
        assertNotNull(window.getPaneValue(System.currentTimeMillis()));
        assertNull(window.getPaneValue(System.currentTimeMillis() + window.getPaneIntervalInMs()));
    }

    @Test
    void testNewEmptyValue() {
        assertEquals(0L, window.newEmptyValue(System.currentTimeMillis()).sum());
    }

    @Test
    void testResetPaneTo() {
        Pane<LongAdder> currentPane = window.currentPane();
        currentPane.getValue().add(2);
        currentPane.getValue().add(1);
        assertEquals(3, currentPane.getValue().sum());
        window.resetPaneTo(currentPane, System.currentTimeMillis());
        assertEquals(0, currentPane.getValue().sum());
        currentPane.getValue().add(1);
        assertEquals(1, currentPane.getValue().sum());
    }

    @Test
    void testCalculatePaneStart() {
        long time = System.currentTimeMillis();
        assertTrue(window.calculatePaneStart(time) <= time);
        assertTrue(time < window.calculatePaneStart(time) + window.getPaneIntervalInMs());
    }

    @Test
    void testIsPaneDeprecated() {
        Pane<LongAdder> currentPane = window.currentPane();
        currentPane.setStartInMs(1000000L);
        assertTrue(window.isPaneDeprecated(currentPane));
    }

    @Test
    void testList() {
        window.currentPane();
        assertTrue(0 < window.list().size());
    }

    @Test
    void testValues() {
        window.currentPane().getValue().add(10);
        long sum = 0;
        for (LongAdder value : window.values()) {
            sum += value.sum();
        }
        assertEquals(10, sum);
    }

    @Test
    void testGetIntervalInMs() {
        assertEquals(intervalInMs, window.getIntervalInMs());
    }

    @Test
    void testGetPaneIntervalInMs() {
        assertEquals(intervalInMs / paneCount, window.getPaneIntervalInMs());
    }

    private static class TestSlidingWindow extends SlidingWindow<LongAdder> {

        public TestSlidingWindow(int paneCount, long intervalInMs) {
            super(paneCount, intervalInMs);
        }

        @Override
        public LongAdder newEmptyValue(long timeMillis) {
            return new LongAdder();
        }

        @Override
        protected Pane<LongAdder> resetPaneTo(Pane<LongAdder> pane, long startInMs) {
            pane.setStartInMs(startInMs);
            pane.getValue().reset();
            return pane;
        }
    }
}
