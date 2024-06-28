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
import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.common.timer.Timer;
import org.apache.dubbo.common.timer.TimerTask;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.DubboUncaughtExceptionHandler;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

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
        ExecutorService workerExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                .name(namePrefix, 1)
                .uncaughtExceptionHandler(DubboUncaughtExceptionHandler.getInstance())
                .factory());

        return new ScheduledExecutorService() {
            @Override
            public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
                AtomicLong time = new AtomicLong(System.currentTimeMillis() + unit.toMillis(delay));
                AtomicBoolean cancelled = new AtomicBoolean();
                Future<?> future = workerExecutor.submit(() -> {
                    LockSupport.parkNanos(unit.toNanos(delay));
                    if (cancelled.get()) {
                        return;
                    }
                    command.run();
                });

                return wrapFuture(time, cancelled, future);
            }

            @Override
            public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
                AtomicLong time = new AtomicLong(System.currentTimeMillis() + unit.toMillis(delay));
                AtomicBoolean cancelled = new AtomicBoolean();
                Future<V> future = workerExecutor.submit(() -> {
                    LockSupport.parkNanos(unit.toNanos(delay));
                    if (cancelled.get()) {
                        return null;
                    }
                    return callable.call();
                });

                return wrapFuture(time, cancelled, future);
            }

            @Override
            public ScheduledFuture<?> scheduleAtFixedRate(
                    Runnable command, long initialDelay, long period, TimeUnit unit) {
                AtomicLong time = new AtomicLong(System.currentTimeMillis() + unit.toMillis(initialDelay));
                AtomicBoolean cancelled = new AtomicBoolean();
                Future<?> future = workerExecutor.submit(() -> {
                    LockSupport.parkNanos(unit.toNanos(initialDelay));
                    if (cancelled.get()) {
                        return;
                    }
                    while (!cancelled.get()) {
                        workerExecutor.submit(command);
                        time.addAndGet(unit.toMillis(period));
                        LockSupport.parkNanos(unit.toNanos(period));
                    }
                });

                return wrapFuture(time, cancelled, future);
            }

            @Override
            public ScheduledFuture<?> scheduleWithFixedDelay(
                    Runnable command, long initialDelay, long delay, TimeUnit unit) {
                AtomicLong time = new AtomicLong(System.currentTimeMillis() + unit.toMillis(initialDelay));
                AtomicBoolean cancelled = new AtomicBoolean();
                Future<?> future = workerExecutor.submit(() -> {
                    LockSupport.parkNanos(unit.toNanos(initialDelay));
                    if (cancelled.get()) {
                        return;
                    }
                    while (!cancelled.get()) {
                        command.run();
                        time.addAndGet(unit.toMillis(delay));
                        LockSupport.parkNanos(unit.toNanos(delay));
                    }
                });

                return wrapFuture(time, cancelled, future);
            }

            @Override
            public void shutdown() {
                workerExecutor.shutdown();
            }

            @Override
            public List<Runnable> shutdownNow() {
                return workerExecutor.shutdownNow();
            }

            @Override
            public boolean isShutdown() {
                return workerExecutor.isShutdown();
            }

            @Override
            public boolean isTerminated() {
                return workerExecutor.isTerminated();
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return workerExecutor.awaitTermination(timeout, unit);
            }

            @Override
            public <T> Future<T> submit(Callable<T> task) {
                return workerExecutor.submit(task);
            }

            @Override
            public <T> Future<T> submit(Runnable task, T result) {
                return workerExecutor.submit(task, result);
            }

            @Override
            public Future<?> submit(Runnable task) {
                return workerExecutor.submit(task);
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
                return workerExecutor.invokeAll(tasks);
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                    throws InterruptedException {
                return workerExecutor.invokeAll(tasks, timeout, unit);
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                    throws InterruptedException, ExecutionException {
                return workerExecutor.invokeAny(tasks);
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                return workerExecutor.invokeAny(tasks, timeout, unit);
            }

            @Override
            public void execute(Runnable command) {
                workerExecutor.execute(command);
            }
        };
    }

    private static <V> ScheduledFuture<V> wrapFuture(AtomicLong time, AtomicBoolean cancelled, Future<V> future) {
        return new ScheduledFuture<>() {
            @Override
            public long getDelay(TimeUnit unit) {
                return unit.convert(time.get() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            }

            @Override
            public int compareTo(Delayed o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                cancelled.set(true);
                return future.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return future.isCancelled() || cancelled.get();
            }

            @Override
            public boolean isDone() {
                return future.isDone();
            }

            @Override
            public V get() throws InterruptedException, ExecutionException {
                return future.get();
            }

            @Override
            public V get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                return future.get(timeout, unit);
            }

            @Override
            public V resultNow() {
                return future.resultNow();
            }

            @Override
            public Throwable exceptionNow() {
                return future.exceptionNow();
            }

            @Override
            public State state() {
                return future.state();
            }
        };
    }

    @Override
    public Timer newTimer(
            String namePrefix, long tickDuration, TimeUnit unit, int ticksPerWheel, long maxPendingTimeouts) {
        ExecutorService workerExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                .name(namePrefix, 1)
                .uncaughtExceptionHandler(DubboUncaughtExceptionHandler.getInstance())
                .factory());
        Set<Timeout> timeouts = new ConcurrentHashSet<>();
        AtomicBoolean shutdown = new AtomicBoolean(false);
        return new Timer() {
            @Override
            public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
                Timer timer = this;
                AtomicBoolean cancelled = new AtomicBoolean(false);
                AtomicReference<Thread> lock = new AtomicReference<>();
                Timeout timeout = new Timeout() {
                    @Override
                    public Timer timer() {
                        return timer;
                    }

                    @Override
                    public TimerTask task() {
                        return task;
                    }

                    @Override
                    public boolean isExpired() {
                        return false;
                    }

                    @Override
                    public boolean isCancelled() {
                        return cancelled.get();
                    }

                    @Override
                    public boolean cancel() {
                        cancelled.set(true);
                        if (lock.get() != null) {
                            LockSupport.unpark(lock.get());
                        }
                        return true;
                    }
                };
                timeouts.add(timeout);
                workerExecutor.submit(() -> {
                    try {
                        lock.set(Thread.currentThread());
                        LockSupport.parkNanos(unit.toNanos(delay));
                        if (cancelled.get()) {
                            return;
                        }
                        try {
                            task.run(timeout);
                        } catch (Exception e) {
                            DubboUncaughtExceptionHandler.getInstance().uncaughtException(Thread.currentThread(), e);
                        }
                    } finally {
                        timeouts.remove(timeout);
                    }
                });
                return timeout;
            }

            @Override
            public Set<Timeout> stop() {
                shutdown.set(true);
                Set<Timeout> origin = new ConcurrentHashSet<>();
                origin.addAll(timeouts);
                for (Timeout timeout : origin) {
                    timeout.cancel();
                }
                workerExecutor.shutdown();
                return origin;
            }

            @Override
            public boolean isStop() {
                return shutdown.get();
            }
        };
    }
}
