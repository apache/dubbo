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

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.common.timer.Timer;
import org.apache.dubbo.common.timer.TimerTask;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.DubboUncaughtExceptionHandler;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class VirtualTimer implements Timer {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(VirtualTimer.class);
    private final ExecutorService workerExecutor;
    private final Set<Timeout> timeouts;
    private final AtomicBoolean shutdown;

    public VirtualTimer(String namePrefix) {
        this.workerExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                .name(namePrefix, 1)
                .uncaughtExceptionHandler(DubboUncaughtExceptionHandler.getInstance())
                .factory());
        this.timeouts = new ConcurrentHashSet<>();
        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        VirtualTimeout timeout = new VirtualTimeout(this, task);
        timeouts.add(timeout);
        try {
            Future<?> future = workerExecutor.submit(() -> {
                try {
                    LockSupport.parkNanos(unit.toNanos(delay));
                    if (timeout.isCancelled()) {
                        return;
                    }
                    try {
                        task.run(timeout);
                    } catch (Exception e) {
                        DubboUncaughtExceptionHandler.getInstance().uncaughtException(Thread.currentThread(), e);
                    }
                } finally {
                    removeTimeout(timeout);
                }
            });
            timeout.setTaskFuture(future);
            return timeout;
        } catch (Throwable t) {
            removeTimeout(timeout);
            throw t;
        }
    }

    protected void removeTimeout(VirtualTimeout timeout) {
        timeouts.remove(timeout);
    }

    @Override
    public Set<Timeout> stop() {
        if (shutdown.compareAndSet(false, true)) {
            Set<Timeout> origin = new ConcurrentHashSet<>();
            origin.addAll(timeouts);
            for (Timeout timeout : origin) {
                try {
                    timeout.cancel();
                } catch (Throwable t) {
                    logger.warn(
                            LoggerCodeConstants.INTERNAL_ERROR,
                            "",
                            "",
                            "An exception was caught while stopping " + "the timer",
                            t);
                }
            }
            workerExecutor.shutdownNow();
            return origin;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public boolean isStop() {
        return shutdown.get();
    }

    protected Set<Timeout> getTimeouts() {
        return timeouts;
    }
}
