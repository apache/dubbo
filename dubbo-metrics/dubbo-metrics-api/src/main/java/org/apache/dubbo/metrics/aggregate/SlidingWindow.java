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

import org.apache.dubbo.common.utils.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SlidingWindow adopts sliding window algorithm for statistics.
 * <p>
 * A window contains {@code paneCount} panes,
 * {@code intervalInMs} = {@code paneCount} * {@code paneIntervalInMs}
 *
 * @param <T> Value type for window statistics.
 */
public abstract class SlidingWindow<T> {

    /**
     * The number of panes the sliding window contains.
     */
    protected int paneCount;

    /**
     * Total time interval of the sliding window in milliseconds.
     */
    protected long intervalInMs;

    /**
     * Time interval of a pane in milliseconds.
     */
    protected long paneIntervalInMs;

    /**
     * The panes reference, supports atomic operations.
     */
    protected final AtomicReferenceArray<Pane<T>> referenceArray;

    /**
     * The lock is used only when current pane is deprecated.
     */
    private final ReentrantLock updateLock = new ReentrantLock();

    protected SlidingWindow(int paneCount, long intervalInMs) {
        Assert.assertTrue(paneCount > 0, "pane count is invalid: " + paneCount);
        Assert.assertTrue(intervalInMs > 0, "total time interval of the sliding window should be positive");
        Assert.assertTrue(intervalInMs % paneCount == 0, "total time interval needs to be evenly divided");

        this.paneCount = paneCount;
        this.intervalInMs = intervalInMs;
        this.paneIntervalInMs = intervalInMs / paneCount;
        this.referenceArray = new AtomicReferenceArray<>(paneCount);
    }

    /**
     * Get the pane at the current timestamp.
     *
     * @return the pane at current timestamp.
     */
    public Pane<T> currentPane() {
        return currentPane(System.currentTimeMillis());
    }

    /**
     * Get the pane at the specified timestamp in milliseconds.
     *
     * @param timeMillis a timestamp in milliseconds.
     * @return the pane at the specified timestamp if the time is valid; null if time is invalid.
     */
    public Pane<T> currentPane(long timeMillis) {
        if (timeMillis < 0) {
            return null;
        }

        int paneIdx = calculatePaneIdx(timeMillis);
        long paneStartInMs = calculatePaneStart(timeMillis);

        while (true) {
            Pane<T> oldPane = referenceArray.get(paneIdx);

            // Create a pane instance when the pane does not exist.
            if (oldPane == null) {
                Pane<T> pane = new Pane<>(paneIntervalInMs, paneStartInMs, newEmptyValue(timeMillis));
                if (referenceArray.compareAndSet(paneIdx, null, pane)) {
                    return pane;
                } else {
                    // Contention failed, the thread will yield its time slice to wait for pane available.
                    Thread.yield();
                }
            }
            //
            else if (paneStartInMs == oldPane.getStartInMs()) {
                return oldPane;
            }
            // The pane has deprecated. To avoid the overhead of creating a new instance, reset the original pane directly.
            else if (paneStartInMs > oldPane.getStartInMs()) {
                if (updateLock.tryLock()) {
                    try {
                        return resetPaneTo(oldPane, paneStartInMs);
                    } finally {
                        updateLock.unlock();
                    }
                } else {
                    // Contention failed, the thread will yield its time slice to wait for pane available.
                    Thread.yield();
                }
            }
            // The specified timestamp has passed.
            else if (paneStartInMs < oldPane.getStartInMs()) {
                return new Pane<>(paneIntervalInMs, paneStartInMs, newEmptyValue(timeMillis));
            }
        }
    }

    /**
     * Get statistic value from pane at the specified timestamp.
     *
     * @param timeMillis the specified timestamp in milliseconds.
     * @return the statistic value if pane at the specified timestamp is up-to-date; otherwise null.
     */
    public T getPaneValue(long timeMillis) {
        if (timeMillis < 0) {
            return null;
        }

        int paneIdx = calculatePaneIdx(timeMillis);

        Pane<T> pane = referenceArray.get(paneIdx);

        if (pane == null || !pane.isTimeInWindow(timeMillis)) {
            return null;
        }

        return pane.getValue();
    }

    /**
     * Create a new statistic value for pane.
     *
     * @param timeMillis the specified timestamp in milliseconds.
     * @return new empty statistic value.
     */
    public abstract T newEmptyValue(long timeMillis);

    /**
     * Reset given pane to the specified start time and reset the value.
     *
     * @param pane      the given pane.
     * @param startInMs the start timestamp of the pane in milliseconds.
     * @return reset pane.
     */
    protected abstract Pane<T> resetPaneTo(final Pane<T> pane, long startInMs);

    /**
     * Calculate the pane index corresponding to the specified timestamp.
     *
     * @param timeMillis the specified timestamp.
     * @return the pane index corresponding to the specified timestamp.
     */
    private int calculatePaneIdx(long timeMillis) {
        return (int) ((timeMillis / paneIntervalInMs) % paneCount);
    }

    /**
     * Calculate the pane start corresponding to the specified timestamp.
     *
     * @param timeMillis the specified timestamp.
     * @return the pane start corresponding to the specified timestamp.
     */
    protected long calculatePaneStart(long timeMillis) {
        return timeMillis - timeMillis % paneIntervalInMs;
    }

    /**
     * Checks if the specified pane is deprecated at the current timestamp.
     *
     * @param pane the specified pane.
     * @return true if the pane is deprecated; otherwise false.
     */
    public boolean isPaneDeprecated(final Pane<T> pane) {
        return isPaneDeprecated(System.currentTimeMillis(), pane);
    }

    /**
     * Checks if the specified pane is deprecated at the specified timestamp.
     *
     * @param timeMillis the specified time.
     * @param pane       the specified pane.
     * @return true if the pane is deprecated; otherwise false.
     */
    public boolean isPaneDeprecated(long timeMillis, final Pane<T> pane) {
        // the pane is '[)'
        return (timeMillis - pane.getStartInMs()) > intervalInMs;
    }

    /**
     * Get valid pane list for entire sliding window at the current time.
     * The list will only contain "valid" panes.
     *
     * @return valid pane list for entire sliding window.
     */
    public List<Pane<T>> list() {
        return list(System.currentTimeMillis());
    }

    /**
     * Get valid pane list for entire sliding window at the specified time.
     * The list will only contain "valid" panes.
     *
     * @param timeMillis the specified time.
     * @return valid pane list for entire sliding window.
     */
    public List<Pane<T>> list(long timeMillis) {
        if (timeMillis < 0) {
            return new ArrayList<>();
        }

        List<Pane<T>> result = new ArrayList<>(paneCount);

        for (int idx = 0; idx < paneCount; idx++) {
            Pane<T> pane = referenceArray.get(idx);
            if (pane == null || isPaneDeprecated(timeMillis, pane)) {
                continue;
            }
            result.add(pane);
        }

        return result;
    }

    /**
     * Get aggregated value list for entire sliding window at the current time.
     * The list will only contain value from "valid" panes.
     *
     * @return aggregated value list for entire sliding window.
     */
    public List<T> values() {
        return values(System.currentTimeMillis());
    }

    /**
     * Get aggregated value list for entire sliding window at the specified time.
     * The list will only contain value from "valid" panes.
     *
     * @return aggregated value list for entire sliding window.
     */
    public List<T> values(long timeMillis) {
        if (timeMillis < 0) {
            return new ArrayList<>();
        }

        List<T> result = new ArrayList<>(paneCount);

        for (int idx = 0; idx < paneCount; idx++) {
            Pane<T> pane = referenceArray.get(idx);
            if (pane == null || isPaneDeprecated(timeMillis, pane)) {
                continue;
            }
            result.add(pane.getValue());
        }
        return result;
    }

    /**
     * Get total interval of the sliding window in milliseconds.
     *
     * @return the total interval in milliseconds.
     */
    public long getIntervalInMs() {
        return intervalInMs;
    }

    /**
     * Get pane interval of the sliding window in milliseconds.
     *
     * @return the interval of a pane in milliseconds.
     */
    public long getPaneIntervalInMs() {
        return paneIntervalInMs;
    }
}
