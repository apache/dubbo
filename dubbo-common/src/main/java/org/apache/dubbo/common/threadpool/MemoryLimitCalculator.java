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

package org.apache.dubbo.common.threadpool;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * {@link javax.management.MXBean} technology is used to calculate the memory
 * limit by using the percentage of the current maximum available memory,
 * which can be used with {@link MemoryLimiter}.
 *
 * @see MemoryLimiter
 * @see <a href="https://github.com/apache/incubator-shenyu/blob/master/shenyu-common/src/main/java/org/apache/shenyu/common/concurrent/MemoryLimitCalculator.java">MemoryLimitCalculator</a>
 */
public class MemoryLimitCalculator {

    private static final MemoryMXBean MX_BEAN = ManagementFactory.getMemoryMXBean();

    private static volatile long maxAvailable;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    static {
        // immediately refresh when this class is loaded to prevent maxAvailable from being 0
        refresh();
        // check every 50 ms to improve performance
        SCHEDULER.scheduleWithFixedDelay(MemoryLimitCalculator::refresh, 50, 50, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(SCHEDULER::shutdown));
    }

    private static void refresh() {
        final MemoryUsage usage = MX_BEAN.getHeapMemoryUsage();
        maxAvailable = usage.getCommitted();
    }

    /**
     * Get the maximum available memory of the current JVM.
     *
     * @return maximum available memory
     */
    public static long maxAvailable() {
        return maxAvailable;
    }

    /**
     * Take the current JVM's maximum available memory
     * as a percentage of the result as the limit.
     *
     * @param percentage percentage
     * @return available memory
     */
    public static long calculate(final float percentage) {
        if (percentage <= 0 || percentage > 1) {
            throw new IllegalArgumentException();
        }
        return (long) (maxAvailable() * percentage);
    }

    /**
     * By default, it takes 80% of the maximum available memory of the current JVM.
     *
     * @return available memory
     */
    public static long defaultLimit() {
        return (long) (maxAvailable() * 0.8);
    }
}
