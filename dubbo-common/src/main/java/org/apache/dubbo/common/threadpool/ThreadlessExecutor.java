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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * The most important difference between this Executor and other normal Executor is that this one doesn't manage
 * any thread.
 * <p>
 * Tasks submitted to this executor through {@link #execute(Runnable)} will not get scheduled to a specific thread, though normal executors always do the schedule.
 * Those tasks are stored in a blocking queue and will only be executed when a thread calls {@link #waitAndDrain()}, the thread executing the task
 * is exactly the same as the one calling waitAndDrain.
 */
public class ThreadlessExecutor extends AbstractExecutorService {
    private static final Logger logger = LoggerFactory.getLogger(ThreadlessExecutor.class.getName());

    private static final Object SHUTDOWN = new Object();

    private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();

    /**
     * Wait thread. It must be visible to other threads and does not need to be thread-safe
     */
    private volatile Object waiter;

    /**
     * Waits until there is a task, executes the task and all queued tasks (if there're any). The task is either a normal
     * response or a timeout response.
     */
    public void waitAndDrain() throws InterruptedException {
        throwIfInterrupted();
        Runnable runnable = queue.poll();
        if (runnable == null) {
            waiter = Thread.currentThread();
            try {
                while ((runnable = queue.poll()) == null) {
                    LockSupport.park(this);
                    throwIfInterrupted();
                }
            } finally {
                waiter = null;
            }
        }
        do {
            runnable.run();
        } while ((runnable = queue.poll()) != null);
    }

    private static void throwIfInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    /**
     * If the calling thread is still waiting for a callback task, add the task into the blocking queue to wait for schedule.
     * Otherwise, submit to shared callback executor directly.
     *
     * @param runnable
     */
    @Override
    public void execute(Runnable runnable) {
        RunnableWrapper run = new RunnableWrapper(runnable);
        queue.add(run);
        if (waiter != SHUTDOWN) {
            LockSupport.unpark((Thread) waiter);
        } else if (queue.remove(run)) {
            throw new RejectedExecutionException();
        }
    }

    /**
     * The following methods are still not supported
     */

    @Override
    public void shutdown() {
        shutdownNow();
    }

    @Override
    public List<Runnable> shutdownNow() {
        waiter = SHUTDOWN;
        Runnable runnable;
        while ((runnable = queue.poll()) != null) {
            runnable.run();
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return waiter == SHUTDOWN;
    }

    @Override
    public boolean isTerminated() {
        return isShutdown();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return false;
    }

    private static class RunnableWrapper implements Runnable {
        private final Runnable runnable;

        public RunnableWrapper(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            try {
                runnable.run();
            } catch (Throwable t) {
                logger.info(t);
            }
        }
    }
}
