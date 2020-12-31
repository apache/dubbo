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
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The most important difference between this Executor and other normal Executor is that this one doesn't manage
 * any thread.
 *
 * Tasks submitted to this executor through {@link #execute(Runnable)} will not get scheduled to a specific thread, though normal executors always do the schedule.
 * Those tasks are stored in a blocking queue and will only be executed when a thread calls {@link #waitAndDrain()}, the thread executing the task
 * is exactly the same as the one calling waitAndDrain.
 */
public class ThreadlessExecutor extends AbstractExecutorService {
    private static final Logger logger = LoggerFactory.getLogger(ThreadlessExecutor.class.getName());

    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

    private ExecutorService sharedExecutor;

    private CompletableFuture<?> waitingFuture;

    private boolean finished = false;

    private volatile boolean waiting = true;

    private final Object lock = new Object();

    public ThreadlessExecutor(ExecutorService sharedExecutor) {
        this.sharedExecutor = sharedExecutor;
    }

    public CompletableFuture<?> getWaitingFuture() {
        return waitingFuture;
    }

    public void setWaitingFuture(CompletableFuture<?> waitingFuture) {
        this.waitingFuture = waitingFuture;
    }

    public boolean isWaiting() {
        return waiting;
    }

    /**
     * Waits until there is a task, executes the task and all queued tasks (if there're any). The task is either a normal
     * response or a timeout response.
     */
    public void waitAndDrain() throws InterruptedException {
        /**
         * Usually, {@link #waitAndDrain()} will only get called once. It blocks for the response for the first time,
         * once the response (the task) reached and being executed waitAndDrain will return, the whole request process
         * then finishes. Subsequent calls on {@link #waitAndDrain()} (if there're any) should return immediately.
         *
         * There's no need to worry that {@link #finished} is not thread-safe. Checking and updating of
         * 'finished' only appear in waitAndDrain, since waitAndDrain is binding to one RPC call (one thread), the call
         * of it is totally sequential.
         */
        if (finished) {
            return;
        }

        Runnable runnable = queue.take();

        synchronized (lock) {
            waiting = false;
            runnable.run();
        }

        runnable = queue.poll();
        while (runnable != null) {
            try {
                runnable.run();
            } catch (Throwable t) {
                logger.info(t);

            }
            runnable = queue.poll();
        }
        // mark the status of ThreadlessExecutor as finished.
        finished = true;
    }

    public long waitAndDrain(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        /*long startInMs = System.currentTimeMillis();
        Runnable runnable = queue.poll(timeout, unit);
        if (runnable == null) {
            throw new TimeoutException();
        }
        runnable.run();
        long elapsedInMs = System.currentTimeMillis() - startInMs;
        long timeLeft = timeout - elapsedInMs;
        if (timeLeft < 0) {
            throw new TimeoutException();
        }
        return timeLeft;*/
        throw new UnsupportedOperationException();
    }

    /**
     * If the calling thread is still waiting for a callback task, add the task into the blocking queue to wait for schedule.
     * Otherwise, submit to shared callback executor directly.
     *
     * @param runnable
     */
    @Override
    public void execute(Runnable runnable) {
        synchronized (lock) {
            if (!waiting) {
                sharedExecutor.execute(runnable);
            } else {
                queue.add(runnable);
            }
        }
    }

    /**
     * tells the thread blocking on {@link #waitAndDrain()} to return, despite of the current status, to avoid endless waiting.
     */
    public void notifyReturn(Throwable t) {
        // an empty runnable task.
        execute(() -> {
            waitingFuture.completeExceptionally(t);
        });
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
        notifyReturn(new IllegalStateException("Consumer is shutting down and this call is going to be stopped without " +
                "receiving any result, usually this is called by a slow provider instance or bad service implementation."));
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }
}
