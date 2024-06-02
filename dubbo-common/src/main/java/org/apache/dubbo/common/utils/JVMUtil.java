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

import org.apache.dubbo.common.constants.CommonConstants;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

import static java.lang.Thread.State.BLOCKED;
import static java.lang.Thread.State.TIMED_WAITING;
import static java.lang.Thread.State.WAITING;

public class JVMUtil {

    public static ThreadInfo[] dumpAllThreads() {
        return ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
    }

    public static void jstack(OutputStream stream) throws IOException {
        jstack(stream, dumpAllThreads());
    }

    public static void jstack(OutputStream stream, ThreadInfo[] allThreads) throws IOException {
        for (ThreadInfo threadInfo : allThreads) {
            getThreadDumpString(stream, threadInfo);
        }
    }

    private static void getThreadDumpString(final OutputStream stream, ThreadInfo threadInfo) throws IOException {
        // print basic info
        stream.write(String.format(
                        "\"%s\" Id=%d %s",
                        threadInfo.getThreadName(), threadInfo.getThreadId(), threadInfo.getThreadState())
                .getBytes());
        if (threadInfo.getLockName() != null) {
            stream.write(String.format(" on %s", threadInfo.getLockName()).getBytes());
        }
        if (threadInfo.getLockOwnerName() != null) {
            stream.write(
                    String.format(" owned by \"%s\" Id=%d", threadInfo.getLockOwnerName(), threadInfo.getLockOwnerId())
                            .getBytes());
        }
        if (threadInfo.isSuspended()) {
            stream.write(" (suspended)".getBytes());
        }
        if (threadInfo.isInNative()) {
            stream.write(" (in native)".getBytes());
        }
        stream.write("\n".getBytes());

        // calculate stack print depth
        StackTraceElement[] stackTrace = threadInfo.getStackTrace();
        // default is 32, means only print up to 32 lines
        int printStackDepth = Math.min(32, stackTrace.length);
        String jstackMaxLineStr = System.getProperty(CommonConstants.DUBBO_JSTACK_MAXLINE);
        if (StringUtils.isNotEmpty(jstackMaxLineStr)) {
            try {
                int specifiedDepth = Integer.parseInt(jstackMaxLineStr);
                if (specifiedDepth < 0) {
                    // if set to a negative number, print all lines instead
                    specifiedDepth = stackTrace.length;
                }
                printStackDepth = Math.min(stackTrace.length, specifiedDepth);
            } catch (Exception ignore) {
            }
        }

        // print stack trace info and monitor info
        MonitorInfo[] lockedMonitors = threadInfo.getLockedMonitors();
        for (int i = 0; i < printStackDepth; i++) {
            StackTraceElement ste = stackTrace[i];
            stream.write(String.format("\tat %s\n", ste).getBytes());
            if (i == 0 && threadInfo.getLockInfo() != null) {
                Thread.State ts = threadInfo.getThreadState();
                if (BLOCKED.equals(ts)) {
                    stream.write(String.format("\t-  blocked on %s\n", threadInfo.getLockInfo())
                            .getBytes());
                } else if (WAITING.equals(ts) || TIMED_WAITING.equals(ts)) {
                    stream.write(String.format("\t-  waiting on %s\n", threadInfo.getLockInfo())
                            .getBytes());
                }
            }

            for (MonitorInfo mi : lockedMonitors) {
                if (mi.getLockedStackDepth() == i) {
                    stream.write(String.format("\t-  locked %s\n", mi).getBytes());
                }
            }
        }
        if (printStackDepth < stackTrace.length) {
            // current stack is deeper than the number of lines printed
            stream.write("\t...\n".getBytes());
        }

        // print lock info
        LockInfo[] locks = threadInfo.getLockedSynchronizers();
        if (locks.length > 0) {
            stream.write(String.format("\n\tNumber of locked synchronizers = %d\n", locks.length)
                    .getBytes());
            for (LockInfo li : locks) {
                stream.write(String.format("\t- %s\n", li).getBytes());
            }
        }
        stream.write("\n".getBytes());
    }
}
