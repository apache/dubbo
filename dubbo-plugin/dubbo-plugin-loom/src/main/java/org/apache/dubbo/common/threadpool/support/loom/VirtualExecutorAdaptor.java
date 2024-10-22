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
package org.apache.dubbo.common.threadpool.support.loom;

import org.apache.dubbo.common.threadpool.ExecutorAdaptor;
import org.apache.dubbo.common.timer.Timer;
import org.apache.dubbo.common.utils.DubboUncaughtExceptionHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VirtualExecutorAdaptor implements ExecutorAdaptor {
    @Override
    public ExecutorService newSingleThreadExecutorService(String namePrefix) {
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                .name(namePrefix, 1)
                .uncaughtExceptionHandler(DubboUncaughtExceptionHandler.getInstance())
                .factory());
    }

    @Override
    public ExecutorService newExecutorService(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            String namePrefix,
            RejectedExecutionHandler handler) {
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                .name(namePrefix, 1)
                .uncaughtExceptionHandler(DubboUncaughtExceptionHandler.getInstance())
                .factory());
    }

    @Override
    public ScheduledExecutorService newScheduledExecutorService(int corePoolSize, String namePrefix) {
        return new ScheduledVirtualExecutorService(namePrefix);
    }

    @Override
    public Timer newTimer(
            String namePrefix, long tickDuration, TimeUnit unit, int ticksPerWheel, long maxPendingTimeouts) {
        return new VirtualTimer(namePrefix);
    }
}
