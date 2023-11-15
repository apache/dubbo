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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.lang.Thread.State.BLOCKED;
import static java.lang.Thread.State.TIMED_WAITING;
import static java.lang.Thread.State.WAITING;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_JSTACK_MAXLINE;
import static org.apache.dubbo.common.constants.CommonConstants.OS_NAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.OS_WIN_PREFIX;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_UNEXPECTED_CREATE_DUMP;
import static org.apache.dubbo.common.utils.JVMUtil.jstack;

class JVMUtilTest {

    protected static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(JVMUtilTest.class);

    @Test
    void testPrintStackTraceWithSpecifiedDepth() {
        test(10);
    }

    @Test
    void testPrintStackTraceWithUnlimitedDepth() {
        test(-1);
    }

    private void test(Integer depth) {
        // read the old property, then set new property
        String oldProperty = System.getProperty(DUBBO_JSTACK_MAXLINE);
        System.setProperty(DUBBO_JSTACK_MAXLINE, depth.toString());

        SimpleDateFormat sdf;
        String os = System.getProperty(OS_NAME_KEY).toLowerCase();
        // window system don't support ":" in file name
        if (os.contains(OS_WIN_PREFIX)) {
            sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss .SSS");
        } else {
            sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss .SSS");
        }
        String filename = String.format("Dubbo_JStack.log.%s", sdf.format(new Date()));

        try (FileOutputStream jStackStream = new FileOutputStream(new File("/tmp", filename))) {
            // output new stack trace
            jstack(jStackStream);
        } catch (Exception e) {
            logger.error(COMMON_UNEXPECTED_CREATE_DUMP, "", "", "dump jStack error", e);
        }

        // compare the depth between new trace and old trace
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(String.format("/tmp/%s", filename)));
            String newOutput = new String(encoded, StandardCharsets.UTF_8);
            int newStackDepth = calculateStackDepth(newOutput);

            int oldStackDepth = calculateStackDepth(OldJVMUtil.jstack());
            /**
             * if set to a non-negative integer(0 or greater),
             * the depth between specified depth and output should be equal.
             * if set to a negative integer,
             * the old stack depth should be 0 while the new depth should be greater than 0.
             */
            if (depth != -1) {
                Assertions.assertTrue(oldStackDepth == newStackDepth && depth == newStackDepth);
            } else {
                Assertions.assertTrue(oldStackDepth == 0 && newStackDepth > 0);
            }
        } catch (IOException e) {
            // only catch IOException because it blocks testing
            logger.error(String.format("Encountered Error when reading logs from %s", filename), e);
        } finally {
            // recover the old property
            if (oldProperty == null) {
                System.clearProperty(DUBBO_JSTACK_MAXLINE);
            } else {
                System.setProperty(DUBBO_JSTACK_MAXLINE, oldProperty);
            }
        }
    }

    private static int calculateStackDepth(String stackTrace) {
        String[] lines = stackTrace.split("\\r?\\n");
        for (int i = 1; i < lines.length; ++i) {
            if (lines[i].endsWith("...") || lines[i].isEmpty()) {
                // calculate specified depth using main trace
                return i - 1;
            }
        }
        return lines.length - 1;
    }

    private static class OldJVMUtil {
        public static String jstack() {
            ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
            StringBuilder sb = new StringBuilder();
            for (ThreadInfo threadInfo : threadMxBean.dumpAllThreads(true, true)) {
                sb.append(getThreadDumpString(threadInfo));
            }
            return sb.toString();
        }

        private static String getThreadDumpString(ThreadInfo threadInfo) {
            StringBuilder sb = new StringBuilder("\"" + threadInfo.getThreadName() + "\"" + " Id="
                    + threadInfo.getThreadId() + " " + threadInfo.getThreadState());
            if (threadInfo.getLockName() != null) {
                sb.append(" on " + threadInfo.getLockName());
            }
            if (threadInfo.getLockOwnerName() != null) {
                sb.append(" owned by \"" + threadInfo.getLockOwnerName() + "\" Id=" + threadInfo.getLockOwnerId());
            }
            if (threadInfo.isSuspended()) {
                sb.append(" (suspended)");
            }
            if (threadInfo.isInNative()) {
                sb.append(" (in native)");
            }
            sb.append('\n');
            int i = 0;
            // default is 32, means only print up to 32 lines
            int jstackMaxLine = 32;
            String jstackMaxLineStr = System.getProperty(CommonConstants.DUBBO_JSTACK_MAXLINE);
            if (StringUtils.isNotEmpty(jstackMaxLineStr)) {
                try {
                    jstackMaxLine = Integer.parseInt(jstackMaxLineStr);
                } catch (Exception ignore) {
                }
            }
            StackTraceElement[] stackTrace = threadInfo.getStackTrace();
            MonitorInfo[] lockedMonitors = threadInfo.getLockedMonitors();
            for (; i < stackTrace.length && i < jstackMaxLine; i++) {
                StackTraceElement ste = stackTrace[i];
                sb.append("\tat ").append(ste.toString());
                sb.append('\n');
                if (i == 0 && threadInfo.getLockInfo() != null) {
                    Thread.State ts = threadInfo.getThreadState();
                    if (BLOCKED.equals(ts)) {
                        sb.append("\t-  blocked on ").append(threadInfo.getLockInfo());
                        sb.append('\n');
                    } else if (WAITING.equals(ts) || TIMED_WAITING.equals(ts)) {
                        sb.append("\t-  waiting on ").append(threadInfo.getLockInfo());
                        sb.append('\n');
                    }
                }

                for (MonitorInfo mi : lockedMonitors) {
                    if (mi.getLockedStackDepth() == i) {
                        sb.append("\t-  locked ").append(mi);
                        sb.append('\n');
                    }
                }
            }
            if (i < stackTrace.length) {
                sb.append("\t...");
                sb.append('\n');
            }

            LockInfo[] locks = threadInfo.getLockedSynchronizers();
            if (locks.length > 0) {
                sb.append("\n\tNumber of locked synchronizers = " + locks.length);
                sb.append('\n');
                for (LockInfo li : locks) {
                    sb.append("\t- " + li);
                    sb.append('\n');
                }
            }
            sb.append('\n');
            return sb.toString();
        }
    }
}
