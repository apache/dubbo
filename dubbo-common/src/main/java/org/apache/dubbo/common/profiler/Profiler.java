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

import java.util.LinkedList;
import java.util.List;

public class Profiler {
    public static final String PROFILER_KEY = "DUBBO_INVOKE_PROFILER";
    public static final int MAX_ENTRY_SIZE = 1000;

    private static final InternalThreadLocal<ProfilerEntry> bizProfiler = new InternalThreadLocal<>();

    public static ProfilerEntry start(String message) {
        return new ProfilerEntry(message);
    }

    public static ProfilerEntry enter(ProfilerEntry entry, String message) {
        ProfilerEntry subEntry = new ProfilerEntry(message, entry, entry.getFirst());
        if (subEntry.getRequestCount().incrementAndGet() < MAX_ENTRY_SIZE) {
            entry.getSub().add(subEntry);
        } // ignore if sub entry size is exceed
        return subEntry;
    }

    public static ProfilerEntry release(ProfilerEntry entry) {
        entry.setEndTime(System.nanoTime());
        ProfilerEntry parent = entry.getParent();
        if (parent != null) {
            return parent;
        } else {
            return entry;
        }
    }

    public static ProfilerEntry setToBizProfiler(ProfilerEntry entry) {
        try {
            return bizProfiler.get();
        } finally {
            bizProfiler.set(entry);
        }
    }

    public static ProfilerEntry getBizProfiler() {
        return bizProfiler.get();
    }

    public static void removeBizProfiler() {
        bizProfiler.remove();
    }

    public static String buildDetail(ProfilerEntry entry) {
        long totalUsageTime = entry.getEndTime() - entry.getStartTime();
        return "Start time: " + entry.getStartTime() + "\n" +
            String.join("\n", buildDetail(entry, entry.getStartTime(), totalUsageTime, 0));
    }

    public static List<String> buildDetail(ProfilerEntry entry, long startTime, long totalUsageTime, int depth) {
        StringBuilder stringBuilder = new StringBuilder();
        long usage = entry.getEndTime() - entry.getStartTime();
        int percent = (int) (((usage) * 100) / totalUsageTime);

        long offset = entry.getStartTime() - startTime;
        List<String> lines = new LinkedList<>();
        stringBuilder.append("+-[ Offset: ")
            .append(offset / 1000_000).append('.').append(String.format("%06d", offset % 1000_000))
            .append("ms; Usage: ")
            .append(usage / 1000_000).append('.').append(String.format("%06d", usage % 1000_000))
            .append("ms, ")
            .append(percent)
            .append("% ] ")
            .append(entry.getMessage());
        lines.add(stringBuilder.toString());
        List<ProfilerEntry> entrySub = entry.getSub();
        for (int i = 0, entrySubSize = entrySub.size(); i < entrySubSize; i++) {
            ProfilerEntry sub = entrySub.get(i);
            List<String> subLines = buildDetail(sub, startTime, totalUsageTime, depth + 1);
            if (i < entrySubSize - 1) {
                lines.add("  " + subLines.get(0));
                for (int j = 1, subLinesSize = subLines.size(); j < subLinesSize; j++) {
                    String subLine = subLines.get(j);
                    lines.add("  |" + subLine);
                }
            } else {
                lines.add("  " + subLines.get(0));
                for (int j = 1, subLinesSize = subLines.size(); j < subLinesSize; j++) {
                    String subLine = subLines.get(j);
                    lines.add("   " + subLine);
                }
            }
        }
        return lines;
    }
}
