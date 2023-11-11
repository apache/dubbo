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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.LongAdder;

public class TimeWindowAggregator {

    private final SnapshotSlidingWindow slidingWindow;

    public TimeWindowAggregator(int bucketNum, int timeWindowSeconds) {
        this.slidingWindow = new SnapshotSlidingWindow(bucketNum, TimeUnit.SECONDS.toMillis(timeWindowSeconds));
    }

    public SnapshotSlidingWindow getSlidingWindow() {
        return slidingWindow;
    }

    public void add(double value) {
        SnapshotObservation sample = this.slidingWindow.currentPane().getValue();
        sample.add(value);
    }

    public SampleAggregatedEntry get() {
        SampleAggregatedEntry aggregatedEntry = new SampleAggregatedEntry();

        double total = 0L;
        long count = 0;
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;

        List<SnapshotObservation> windows = this.slidingWindow.values();

        for (SnapshotObservation window : windows) {
            total += window.getTotal();
            count += window.getCount();

            max = Math.max(max, window.getMax());
            min = Math.min(min, window.getMin());
        }

        if (count > 0) {
            double avg = total / count;
            aggregatedEntry.setAvg(Math.round(avg * 100.0) / 100.0);
        } else {
            aggregatedEntry.setAvg(0);
        }

        aggregatedEntry.setMax(max == Double.MIN_VALUE ? 0 : max);
        aggregatedEntry.setMin(min == Double.MAX_VALUE ? 0 : min);
        aggregatedEntry.setTotal(total);
        aggregatedEntry.setCount(count);

        return aggregatedEntry;
    }

    public static class SnapshotSlidingWindow extends SlidingWindow<SnapshotObservation> {

        public SnapshotSlidingWindow(int sampleCount, long intervalInMs) {
            super(sampleCount, intervalInMs);
        }

        @Override
        public SnapshotObservation newEmptyValue(long timeMillis) {
            return new SnapshotObservation();
        }

        @Override
        protected Pane<SnapshotObservation> resetPaneTo(final Pane<SnapshotObservation> pane, long startTime) {
            pane.setStartInMs(startTime);
            pane.getValue().reset();
            return pane;
        }
    }

    public static class SnapshotObservation {

        private final AtomicReference<Double> min = new AtomicReference<>(Double.MAX_VALUE);
        private final AtomicReference<Double> max = new AtomicReference<>(0d);
        private final DoubleAccumulator total = new DoubleAccumulator((x, y) -> x + y, 0);
        private final LongAdder count = new LongAdder();

        public void add(double sample) {
            total.accumulate(sample);
            count.increment();
            updateMin(sample);
            updateMax(sample);
        }

        private void updateMin(double sample) {
            Double curMin;
            do {
                curMin = min.get();
            } while (sample < curMin && !min.compareAndSet(curMin, sample));
        }

        private void updateMax(double sample) {
            Double curMax;
            do {
                curMax = max.get();
                if (sample <= curMax) {
                    return;
                }

            } while (!max.compareAndSet(curMax, sample));
        }

        public void reset() {
            min.set(Double.MAX_VALUE);
            max.set(0d);
            count.reset();
            total.reset();
        }

        public double getMin() {
            return min.get();
        }

        public double getMax() {
            return max.get();
        }

        public Double getTotal() {
            return total.get();
        }

        public long getCount() {
            return count.sum();
        }
    }
}
