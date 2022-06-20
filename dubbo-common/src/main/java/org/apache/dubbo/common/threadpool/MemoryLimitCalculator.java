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

import org.apache.dubbo.common.resource.GlobalResourcesRepository;
import org.apache.dubbo.common.utils.NamedThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link java.lang.Runtime#freeMemory()} technology is used to calculate the
 * memory limit by using the percentage of the current maximum available memory,
 * which can be used with {@link MemoryLimiter}.
 *
 * @see MemoryLimiter
 * @see <a href="https://github.com/apache/incubator-shenyu/blob/master/shenyu-common/src/main/java/org/apache/shenyu/common/concurrent/MemoryLimitCalculator.java">MemoryLimitCalculator</a>
 */
public class MemoryLimitCalculator {

    private static volatile long maxAvailable;

    private static final AtomicBoolean refreshStarted = new AtomicBoolean(false);

    private static void refresh() {
        maxAvailable = Runtime.getRuntime().freeMemory();
    }

    private static void checkAndScheduleRefresh() {
        if (!refreshStarted.get()) {
            // immediately refresh when first call to prevent maxAvailable from being 0
            // to ensure that being refreshed before refreshStarted being set as true
            // notice: refresh may be called for more than once because there is no lock
            refresh();
            if (refreshStarted.compareAndSet(false, true)) {
                ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-Memory-Calculator"));
                // check every 50 ms to improve performance
                scheduledExecutorService.scheduleWithFixedDelay(MemoryLimitCalculator::refresh, 50, 50, TimeUnit.MILLISECONDS);
                GlobalResourcesRepository.registerGlobalDisposable(() -> {
                    refreshStarted.set(false);
                    scheduledExecutorService.shutdown();
                });
            }
        }
    }

    /**
     * Get the maximum available memory of the current JVM.
     *
     * @return maximum available memory
     */
    public static long maxAvailable() {
        checkAndScheduleRefresh();
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
        checkAndScheduleRefresh();
        return (long) (maxAvailable() * percentage);
    }

    /**
     * By default, it takes 80% of the maximum available memory of the current JVM.
     *
     * @return available memory
     */
    public static long defaultLimit() {
        checkAndScheduleRefresh();
        return (long) (maxAvailable() * 0.8);
    }
}
