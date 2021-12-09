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

import org.apache.dubbo.common.threadlocal.InternalThreadLocal;

public class Profiler {
    public static final String PROFILER_KEY = "DUBBO_INVOKE_PROFILER";

    private final static InternalThreadLocal<ProfilerEntry> bizProfiler = new InternalThreadLocal<>();

    public static ProfilerEntry start(String message) {
        return new ProfilerEntry(message);
    }

    public static ProfilerEntry enter(ProfilerEntry entry, String message) {
        ProfilerEntry subEntry = new ProfilerEntry(message, entry, entry.getFirst());
        entry.getSub().add(subEntry);
        return subEntry;
    }

    public static ProfilerEntry release(ProfilerEntry entry) {
        entry.setEndTime(System.currentTimeMillis());
        ProfilerEntry parent = entry.getParent();
        if (parent != null) {
            return parent;
        } else {
            return entry;
        }
    }

    public static void setToBizProfiler(ProfilerEntry entry) {
        bizProfiler.set(entry);
    }

    public static ProfilerEntry getBizProfiler() {
        return bizProfiler.get();
    }

    public static void removeBizProfiler() {
        bizProfiler.remove();
    }

    public static String buildDetail(ProfilerEntry entry) {
        ProfilerEntry firstEntry = entry.getFirst();
        long totalUsageTime = firstEntry.getEndTime() - firstEntry.getStartTime();
        return "Start time: " + firstEntry.getStartTime() + "\n" + buildDetail(firstEntry, firstEntry.getStartTime(), totalUsageTime, 0);
    }

    public static String buildDetail(ProfilerEntry entry, long startTime, long totalUsageTime, int depth) {
        StringBuilder stringBuilder = new StringBuilder();
        int percent = (int) (((entry.getEndTime() - entry.getStartTime()) * 100) / totalUsageTime);
        for (int i = 0; i < depth; i++) {
            stringBuilder.append("  ");
        }
        stringBuilder.append("+-[ Offset: ")
            .append(entry.getStartTime() - startTime)
            .append("ms; Usage: ")
            .append(entry.getEndTime() - entry.getStartTime())
            .append("ms, ")
            .append(percent)
            .append("% ] ")
            .append(entry.getMessage())
            .append("\n");
        for (ProfilerEntry sub : entry.getSub()) {
            stringBuilder.append(buildDetail(sub, startTime, totalUsageTime, depth + 1));
        }
        return stringBuilder.toString();
    }
}
