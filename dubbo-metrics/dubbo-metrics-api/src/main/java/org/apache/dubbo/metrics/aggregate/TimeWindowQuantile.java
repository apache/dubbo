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

import com.tdunning.math.stats.TDigest;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper around TDigest.
 */
public class TimeWindowQuantile {

    private final double compression;

    private final DigestSlidingWindow slidingWindow;

    public TimeWindowQuantile(double compression, int bucketNum, int timeWindowSeconds) {
        this.compression = compression;
        this.slidingWindow = new DigestSlidingWindow(compression, bucketNum, TimeUnit.SECONDS.toMillis(timeWindowSeconds));
    }

    public double quantile(double q) {
        TDigest mergeDigest = TDigest.createDigest(compression);
        List<TDigest> validWindows = this.slidingWindow.values();
        for (TDigest window : validWindows) {
            mergeDigest.add(window);
        }
        // This may return Double.NaN, and it's correct behavior.
        // see: https://github.com/prometheus/client_golang/issues/85
        return mergeDigest.quantile(q);
    }

    public void add(double value) {
        this.slidingWindow.currentPane().getValue().add(value);
    }

    /**
     * Sliding window of type TDigest.
     */
    private static class DigestSlidingWindow extends SlidingWindow<TDigest> {

        private final double compression;

        public DigestSlidingWindow(double compression, int sampleCount, long intervalInMs) {
            super(sampleCount, intervalInMs);
            this.compression = compression;
        }

        @Override
        public TDigest newEmptyValue(long timeMillis) {
            return TDigest.createDigest(compression);
        }

        @Override
        protected Pane<TDigest> resetPaneTo(final Pane<TDigest> pane, long startTime) {
            pane.setStartInMs(startTime);
            pane.setValue(TDigest.createDigest(compression));
            return pane;
        }
    }
}
