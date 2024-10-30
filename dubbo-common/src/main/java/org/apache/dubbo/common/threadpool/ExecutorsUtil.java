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

import org.apache.dubbo.common.timer.Timer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorsUtil {
    private static ExecutorAdaptor getExecutorAdaptor() {
        return new DefaultExecutorAdaptor();
    }

    public static ExecutorService newSingleThreadExecutorService(String namePrefix) {
        return getExecutorAdaptor().newSingleThreadExecutorService(namePrefix);
    }

    public static ExecutorService newExecutorService(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            String namePrefix) {
        return getExecutorAdaptor()
                .newExecutorService(
                        corePoolSize,
                        maximumPoolSize,
                        keepAliveTime,
                        unit,
                        workQueue,
                        namePrefix,
                        new ThreadPoolExecutor.AbortPolicy());
    }

    public static ExecutorService newExecutorService(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            String namePrefix,
            RejectedExecutionHandler handler) {
        return getExecutorAdaptor()
                .newExecutorService(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, namePrefix, handler);
    }

    public static ScheduledExecutorService newScheduledExecutorService(int corePoolSize, String namePrefix) {
        return getExecutorAdaptor().newScheduledExecutorService(corePoolSize, namePrefix);
    }

    public static Timer newTimer(String namePrefix, long tickDuration, TimeUnit unit) {
        return newTimer(namePrefix, tickDuration, unit, 512);
    }

    public static Timer newTimer(String namePrefix, long tickDuration, TimeUnit unit, int ticksPerWheel) {
        return newTimer(namePrefix, tickDuration, unit, ticksPerWheel, -1);
    }

    public static Timer newTimer(
            String namePrefix, long tickDuration, TimeUnit unit, int ticksPerWheel, long maxPendingTimeouts) {
        return getExecutorAdaptor().newTimer(namePrefix, tickDuration, unit, ticksPerWheel, maxPendingTimeouts);
    }
}
