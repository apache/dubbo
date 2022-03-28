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
package org.apache.dubbo.common.profiler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ProfilerEntry {
    private final List<ProfilerEntry> sub = new ArrayList<>(4);
    private final String message;
    private final ProfilerEntry parent;
    private final ProfilerEntry first;
    private final long startTime;
    private final AtomicInteger requestCount;
    private long endTime;

    public ProfilerEntry(String message) {
        this.message = message;
        this.parent = null;
        this.first = this;
        this.startTime = System.nanoTime();
        this.requestCount = new AtomicInteger(1);
    }

    public ProfilerEntry(String message, ProfilerEntry parentEntry, ProfilerEntry firstEntry) {
        this.message = message;
        this.parent = parentEntry;
        this.first = firstEntry;
        this.startTime = System.nanoTime();
        this.requestCount = parentEntry.getRequestCount();
    }

    public List<ProfilerEntry> getSub() {
        return sub;
    }

    public String getMessage() {
        return message;
    }

    public ProfilerEntry getParent() {
        return parent;
    }

    public ProfilerEntry getFirst() {
        return first;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public AtomicInteger getRequestCount() {
        return requestCount;
    }
}
