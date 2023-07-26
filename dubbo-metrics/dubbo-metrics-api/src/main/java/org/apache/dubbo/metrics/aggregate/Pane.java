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

/**
 * The pane represents a window over a period of time.
 *
 * @param <T> The type of value the pane statistics.
 */
public class Pane<T> {

    /**
     * Time interval of the pane in milliseconds.
     */
    private final long intervalInMs;

    /**
     * Start timestamp of the pane in milliseconds.
     */
    private volatile long startInMs;

    /**
     * End timestamp of the pane in milliseconds.
     * <p>
     * endInMs = startInMs + intervalInMs
     */
    private volatile long endInMs;

    /**
     * Pane statistics value.
     */
    private T value;

    /**
     * @param intervalInMs interval of the pane in milliseconds.
     * @param startInMs    start timestamp of the pane in milliseconds.
     * @param value        the pane value.
     */
    public Pane(long intervalInMs, long startInMs, T value) {
        this.intervalInMs = intervalInMs;
        this.startInMs = startInMs;
        this.endInMs = this.startInMs + this.intervalInMs;
        this.value = value;
    }

    /**
     * Get the interval of the pane in milliseconds.
     *
     * @return the interval of the pane in milliseconds.
     */
    public long getIntervalInMs() {
        return this.intervalInMs;
    }

    /**
     * Get start timestamp of the pane in milliseconds.
     *
     * @return the start timestamp of the pane in milliseconds.
     */
    public long getStartInMs() {
        return this.startInMs;
    }

    /**
     * Get end timestamp of the pane in milliseconds.
     *
     * @return the end timestamp of the pane in milliseconds.
     */
    public long getEndInMs() {
        return this.endInMs;
    }

    /**
     * Get the pane statistics value.
     *
     * @return the pane statistics value.
     */
    public T getValue() {
        return this.value;
    }

    /**
     * Set the new start timestamp to the pane, for reset the instance.
     *
     * @param newStartInMs the new start timestamp.
     */
    public void setStartInMs(long newStartInMs) {
        this.startInMs = newStartInMs;
        this.endInMs = this.startInMs + this.intervalInMs;
    }

    /**
     * Set new value to the pane, for reset the instance.
     *
     * @param newData the new value.
     */
    public void setValue(T newData) {
        this.value = newData;
    }

    /**
     * Check whether given timestamp is in current pane.
     *
     * @param timeMillis timestamp in milliseconds.
     * @return true if the given time is in current pane, otherwise false
     */
    public boolean isTimeInWindow(long timeMillis) {
        // [)
        return startInMs <= timeMillis && timeMillis < endInMs;
    }
}
