/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.dubbo.common.timer;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import org.jctools.queues.MpscChunkedArrayQueue;
import org.jctools.queues.MpscUnboundedArrayQueue;
import org.jctools.queues.atomic.MpscChunkedAtomicArrayQueue;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;
import org.jctools.util.Pow2;
import org.jctools.util.UnsafeAccess;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.invoke.MethodType.methodType;
import static org.apache.dubbo.common.constants.CommonConstants.SystemProperty.SYSTEM_OS_NAME;
import static org.apache.dubbo.common.constants.CommonConstants.OS_WIN_PREFIX;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_ERROR_RUN_THREAD_TASK;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_ERROR_TOO_MANY_INSTANCES;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION;
import static org.apache.dubbo.common.utils.ClassUtils.simpleClassName;

/**
 * A {@link Timer} optimized for approximated I/O timeout scheduling.
 *
 * <h3>Tick Duration</h3>
 *
 * As described with 'approximated', this timer does not execute the scheduled
 * {@link TimerTask} on time.  {@link HashedWheelTimer}, on every tick, will
 * check if there are any {@link TimerTask}s behind the schedule and execute
 * them.
 * <p>
 * You can increase or decrease the accuracy of the execution timing by
 * specifying smaller or larger tick duration in the constructor.  In most
 * network applications, I/O timeout does not need to be accurate.  Therefore,
 * the default tick duration is 100 milliseconds and you will not need to try
 * different configurations in most cases.
 *
 * <h3>Ticks per Wheel (Wheel Size)</h3>
 *
 * {@link HashedWheelTimer} maintains a data structure called 'wheel'.
 * To put simply, a wheel is a hash table of {@link TimerTask}s whose hash
 * function is 'dead line of the task'.  The default number of ticks per wheel
 * (i.e. the size of the wheel) is 512.  You could specify a larger value
 * if you are going to schedule a lot of timeouts.
 *
 * <h3>Do not create many instances.</h3>
 *
 * {@link HashedWheelTimer} creates a new thread whenever it is instantiated and
 * started.  Therefore, you should make sure to create only one instance and
 * share it across your application.  One of the common mistakes, that makes
 * your application unresponsive, is to create a new instance for every connection.
 *
 * <h3>Implementation Details</h3>
 *
 * {@link HashedWheelTimer} is based on
 * <a href="https://cseweb.ucsd.edu/users/varghese/">George Varghese</a> and
 * Tony Lauck's paper,
 * <a href="https://cseweb.ucsd.edu/users/varghese/PAPERS/twheel.ps.Z">'Hashed
 * and Hierarchical Timing Wheels: data structures to efficiently implement a
 * timer facility'</a>.  More comprehensive slides are located
 * <a href="https://www.cse.wustl.edu/~cdgill/courses/cs6874/TimingWheels.ppt">here</a>.
 */
public class HashedWheelTimer implements Timer {

    /**
     * may be in spi?
     */
    public static final String NAME = "hashed";

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(HashedWheelTimer.class);

    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();
    private static final AtomicBoolean WARNED_TOO_MANY_INSTANCES = new AtomicBoolean();
    private static final int INSTANCE_COUNT_LIMIT = 64;
    private static final long MILLISECOND_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    private static final AtomicIntegerFieldUpdater<HashedWheelTimer> WORKER_STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimer.class, "workerState");

    private final Worker worker = new Worker();
    private final Thread workerThread;

    public static final int WORKER_STATE_INIT = 0;
    public static final int WORKER_STATE_STARTED = 1;
    public static final int WORKER_STATE_SHUTDOWN = 2;

    /**
     * 0 - init, 1 - started, 2 - shut down
     */
    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private volatile int workerState;

    private final long tickDuration;
    private final HashedWheelBucket[] wheel;
    private final int mask;
    private final CountDownLatch startTimeInitialized = new CountDownLatch(1);
    private final Queue<HashedWheelTimeout> timeouts = newMpscQueue();
    private final Queue<HashedWheelTimeout> cancelledTimeouts = newMpscQueue();
    private final AtomicLong pendingTimeouts = new AtomicLong(0);
    private final long maxPendingTimeouts;
    private final Executor taskExecutor;

    private volatile long startTime;

    /**
     * Creates a new timer with the default thread factory
     * ({@link Executors#defaultThreadFactory()}), default tick duration, and
     * default number of ticks per wheel.
     */
    public HashedWheelTimer() {
        this(Executors.defaultThreadFactory());
    }

    /**
     * Creates a new timer with the default thread factory
     * ({@link Executors#defaultThreadFactory()}) and default number of ticks
     * per wheel.
     *
     * @param tickDuration the duration between tick
     * @param unit         the time unit of the {@code tickDuration}
     * @throws NullPointerException     if {@code unit} is {@code null}
     * @throws IllegalArgumentException if {@code tickDuration} is &lt;= 0
     */
    public HashedWheelTimer(long tickDuration, TimeUnit unit) {
        this(Executors.defaultThreadFactory(), tickDuration, unit);
    }

    /**
     * Creates a new timer with the default thread factory
     * ({@link Executors#defaultThreadFactory()}).
     *
     * @param tickDuration  the duration between tick
     * @param unit          the time unit of the {@code tickDuration}
     * @param ticksPerWheel the size of the wheel
     * @throws NullPointerException     if {@code unit} is {@code null}
     * @throws IllegalArgumentException if either of {@code tickDuration} and {@code ticksPerWheel} is &lt;= 0
     */
    public HashedWheelTimer(long tickDuration, TimeUnit unit, int ticksPerWheel) {
        this(Executors.defaultThreadFactory(), tickDuration, unit, ticksPerWheel);
    }

    /**
     * Creates a new timer with the default tick duration and default number of
     * ticks per wheel.
     *
     * @param threadFactory a {@link ThreadFactory} that creates a
     *                      background {@link Thread} which is dedicated to
     *                      {@link TimerTask} execution.
     * @throws NullPointerException if {@code threadFactory} is {@code null}
     */
    public HashedWheelTimer(ThreadFactory threadFactory) {
        this(threadFactory, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new timer with the default number of ticks per wheel.
     *
     * @param threadFactory a {@link ThreadFactory} that creates a
     *                      background {@link Thread} which is dedicated to
     *                      {@link TimerTask} execution.
     * @param tickDuration  the duration between tick
     * @param unit          the time unit of the {@code tickDuration}
     * @throws NullPointerException     if either of {@code threadFactory} and {@code unit} is {@code null}
     * @throws IllegalArgumentException if {@code tickDuration} is &lt;= 0
     */
    public HashedWheelTimer(
            ThreadFactory threadFactory, long tickDuration, TimeUnit unit) {
        this(threadFactory, tickDuration, unit, 512);
    }

    /**
     * Creates a new timer.
     *
     * @param threadFactory a {@link ThreadFactory} that creates a
     *                      background {@link Thread} which is dedicated to
     *                      {@link TimerTask} execution.
     * @param tickDuration  the duration between tick
     * @param unit          the time unit of the {@code tickDuration}
     * @param ticksPerWheel the size of the wheel
     * @throws NullPointerException     if either of {@code threadFactory} and {@code unit} is {@code null}
     * @throws IllegalArgumentException if either of {@code tickDuration} and {@code ticksPerWheel} is &lt;= 0
     */
    public HashedWheelTimer(
            ThreadFactory threadFactory,
            long tickDuration, TimeUnit unit, int ticksPerWheel) {
        this(threadFactory, tickDuration, unit, ticksPerWheel, -1);
    }

    /**
     * Creates a new timer.
     *
     * @param threadFactory        a {@link ThreadFactory} that creates a
     *                             background {@link Thread} which is dedicated to
     *                             {@link TimerTask} execution.
     * @param tickDuration         the duration between tick
     * @param unit                 the time unit of the {@code tickDuration}
     * @param ticksPerWheel        the size of the wheel
     * @param  maxPendingTimeouts  The maximum number of pending timeouts after which call to
     *                             {@code newTimeout} will result in
     *                             {@link java.util.concurrent.RejectedExecutionException}
     *                             being thrown. No maximum pending timeouts limit is assumed if
     *                             this value is 0 or negative.
     * @throws NullPointerException     if either of {@code threadFactory} and {@code unit} is {@code null}
     * @throws IllegalArgumentException if either of {@code tickDuration} and {@code ticksPerWheel} is &lt;= 0
     */
    public HashedWheelTimer(
            ThreadFactory threadFactory,
            long tickDuration, TimeUnit unit, int ticksPerWheel,
            long maxPendingTimeouts) {
        this(threadFactory, tickDuration, unit, ticksPerWheel,
                maxPendingTimeouts, ImmediateExecutor.INSTANCE);
    }

    /**
     * Creates a new timer.
     *
     * @param threadFactory        a {@link ThreadFactory} that creates a
     *                             background {@link Thread} which is dedicated to
     *                             {@link TimerTask} execution.
     * @param tickDuration         the duration between tick
     * @param unit                 the time unit of the {@code tickDuration}
     * @param ticksPerWheel        the size of the wheel
     * @param maxPendingTimeouts   The maximum number of pending timeouts after which call to
     *                             {@code newTimeout} will result in
     *                             {@link java.util.concurrent.RejectedExecutionException}
     *                             being thrown. No maximum pending timeouts limit is assumed if
     *                             this value is 0 or negative.
     * @param taskExecutor         The {@link Executor} that is used to execute the submitted {@link TimerTask}s.
     *                             The caller is responsible to shutdown the {@link Executor} once it is not needed
     *                             anymore.
     * @throws NullPointerException     if either of {@code threadFactory} and {@code unit} is {@code null}
     * @throws IllegalArgumentException if either of {@code tickDuration} and {@code ticksPerWheel} is &lt;= 0
     */
    public HashedWheelTimer(
            ThreadFactory threadFactory,
            long tickDuration, TimeUnit unit, int ticksPerWheel,
            long maxPendingTimeouts, Executor taskExecutor) {

        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (tickDuration <= 0) {
            throw new IllegalArgumentException("tickDuration must be greater than 0: " + tickDuration);
        }
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException("ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }
        if (taskExecutor == null) {
            throw new NullPointerException("taskExecutor");
        }
        this.taskExecutor = taskExecutor;

        // Normalize ticksPerWheel to power of two and initialize the wheel.
        wheel = createWheel(ticksPerWheel);
        mask = wheel.length - 1;

        // Convert tickDuration to nanos.
        long duration = unit.toNanos(tickDuration);

        // Prevent overflow.
        if (duration >= Long.MAX_VALUE / wheel.length) {
            throw new IllegalArgumentException(String.format(
                    "tickDuration: %d (expected: 0 < tickDuration in nanos < %d",
                    tickDuration, Long.MAX_VALUE / wheel.length));
        }

        if (duration < MILLISECOND_NANOS) {
            logger.warn(COMMON_UNEXPECTED_EXCEPTION, "", "", "Configured tickDuration " + tickDuration + " smaller than " + MILLISECOND_NANOS + ", using 1ms.");
            this.tickDuration = MILLISECOND_NANOS;
        } else {
            this.tickDuration = duration;
        }

        workerThread = threadFactory.newThread(worker);

        this.maxPendingTimeouts = maxPendingTimeouts;

        if (INSTANCE_COUNTER.incrementAndGet() > INSTANCE_COUNT_LIMIT &&
                WARNED_TOO_MANY_INSTANCES.compareAndSet(false, true)) {
            reportTooManyInstances();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            // This object is going to be GCed and it is assumed the ship has sailed to do a proper shutdown. If
            // we have not yet shutdown then we want to make sure we decrement the active instance count.
            if (WORKER_STATE_UPDATER.getAndSet(this, WORKER_STATE_SHUTDOWN) != WORKER_STATE_SHUTDOWN) {
                INSTANCE_COUNTER.decrementAndGet();
            }
        }
    }

    private static HashedWheelBucket[] createWheel(int ticksPerWheel) {
        ticksPerWheel = MathUtil.findNextPositivePowerOfTwo(ticksPerWheel);

        HashedWheelBucket[] wheel = new HashedWheelBucket[ticksPerWheel];
        for (int i = 0; i < wheel.length; i ++) {
            wheel[i] = new HashedWheelBucket();
        }
        return wheel;
    }

    /**
     * Starts the background thread explicitly.  The background thread will
     * start automatically on demand even if you did not call this method.
     *
     * @throws IllegalStateException if this timer has been
     *                               {@linkplain #stop() stopped} already
     */
    public void start() {
        int state = WORKER_STATE_UPDATER.get(this);
        switch (state) {
            case WORKER_STATE_INIT:
                if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
                    workerThread.start();
                }
                break;
            case WORKER_STATE_STARTED:
                break;
            case WORKER_STATE_SHUTDOWN:
                throw new IllegalStateException("cannot be started once stopped");
            default:
                throw new Error("Invalid WorkerState: " + state);
        }

        // Wait until the startTime is initialized by the worker.
        while (startTime == 0) {
            try {
                startTimeInitialized.await();
            } catch (InterruptedException ignore) {
                // Ignore - it will be ready very soon.
            }
        }
    }

    @Override
    public Set<Timeout> stop() {
        if (Thread.currentThread() == workerThread) {
            throw new IllegalStateException(
                    HashedWheelTimer.class.getSimpleName() +
                            ".stop() cannot be called from " +
                            TimerTask.class.getSimpleName());
        }

        if (!WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_STARTED, WORKER_STATE_SHUTDOWN)) {
            // workerState can be 0 or 2 at this moment - let it always be 2.
            if (WORKER_STATE_UPDATER.getAndSet(this, WORKER_STATE_SHUTDOWN) != WORKER_STATE_SHUTDOWN) {
                INSTANCE_COUNTER.decrementAndGet();
            }

            return Collections.emptySet();
        }

        try {
            boolean interrupted = false;
            while (workerThread.isAlive()) {
                workerThread.interrupt();
                try {
                    workerThread.join(100);
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }

            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        } finally {
            INSTANCE_COUNTER.decrementAndGet();
        }
        Set<Timeout> unprocessed = worker.unprocessedTimeouts();
        Set<Timeout> cancelled = new HashSet<Timeout>(unprocessed.size());
        for (Timeout timeout : unprocessed) {
            if (timeout.cancel()) {
                cancelled.add(timeout);
            }
        }
        return cancelled;
    }

    @Override
    public boolean isStop() {
        return WORKER_STATE_SHUTDOWN == WORKER_STATE_UPDATER.get(this);
    }

    @Override
    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }

        long pendingTimeoutsCount = pendingTimeouts.incrementAndGet();

        if (maxPendingTimeouts > 0 && pendingTimeoutsCount > maxPendingTimeouts) {
            pendingTimeouts.decrementAndGet();
            throw new RejectedExecutionException("Number of pending timeouts ("
                    + pendingTimeoutsCount + ") is greater than or equal to maximum allowed pending "
                    + "timeouts (" + maxPendingTimeouts + ")");
        }

        start();

        // Add the timeout to the timeout queue which will be processed on the next tick.
        // During processing all the queued HashedWheelTimeouts will be added to the correct HashedWheelBucket.
        long deadline = System.nanoTime() + unit.toNanos(delay) - startTime;

        // Guard against overflow.
        if (delay > 0 && deadline < 0) {
            deadline = Long.MAX_VALUE;
        }
        HashedWheelTimeout timeout = new HashedWheelTimeout(this, task, deadline);
        timeouts.add(timeout);
        return timeout;
    }

    /**
     * Returns the number of pending timeouts of this {@link Timer}.
     */
    public long pendingTimeouts() {
        return pendingTimeouts.get();
    }

    private static void reportTooManyInstances() {
        if (logger.isErrorEnabled()) {
            String resourceType = simpleClassName(HashedWheelTimer.class);
            logger.error(COMMON_ERROR_TOO_MANY_INSTANCES, "", "", "You are creating too many " + resourceType + " instances. " +
                    resourceType + " is a shared resource that must be reused across the JVM, " +
                    "so that only a few instances are created.");
        }
    }

    private final class Worker implements Runnable {
        private final Set<Timeout> unprocessedTimeouts = new HashSet<Timeout>();

        private long tick;

        @Override
        public void run() {
            // Initialize the startTime.
            startTime = System.nanoTime();
            if (startTime == 0) {
                // We use 0 as an indicator for the uninitialized value here, so make sure it's not 0 when initialized.
                startTime = 1;
            }

            // Notify the other threads waiting for the initialization at start().
            startTimeInitialized.countDown();

            do {
                final long deadline = waitForNextTick();
                if (deadline > 0) {
                    int idx = (int) (tick & mask);
                    processCancelledTasks();
                    HashedWheelBucket bucket =
                            wheel[idx];
                    transferTimeoutsToBuckets();
                    bucket.expireTimeouts(deadline);
                    tick++;
                }
            } while (WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == WORKER_STATE_STARTED);

            // Fill the unprocessedTimeouts so we can return them from stop() method.
            for (HashedWheelBucket bucket: wheel) {
                bucket.clearTimeouts(unprocessedTimeouts);
            }
            for (;;) {
                HashedWheelTimeout timeout = timeouts.poll();
                if (timeout == null) {
                    break;
                }
                if (!timeout.isCancelled()) {
                    unprocessedTimeouts.add(timeout);
                }
            }
            processCancelledTasks();
        }

        private void transferTimeoutsToBuckets() {
            // transfer only max. 100000 timeouts per tick to prevent a thread to stale the workerThread when it just
            // adds new timeouts in a loop.
            for (int i = 0; i < 100000; i++) {
                HashedWheelTimeout timeout = timeouts.poll();
                if (timeout == null) {
                    // all processed
                    break;
                }
                if (timeout.state() == HashedWheelTimeout.ST_CANCELLED) {
                    // Was cancelled in the meantime.
                    continue;
                }

                long calculated = timeout.deadline / tickDuration;
                timeout.remainingRounds = (calculated - tick) / wheel.length;

                // Ensure we don't schedule for past.
                final long ticks = Math.max(calculated, tick);
                int stopIndex = (int) (ticks & mask);

                HashedWheelBucket bucket = wheel[stopIndex];
                bucket.addTimeout(timeout);
            }
        }

        private void processCancelledTasks() {
            for (; ; ) {
                HashedWheelTimeout timeout = cancelledTimeouts.poll();
                if (timeout == null) {
                    // all processed
                    break;
                }
                try {
                    timeout.removeAfterCancellation();
                } catch (Throwable t) {
                    if (logger.isWarnEnabled()) {
                        logger.warn(COMMON_ERROR_RUN_THREAD_TASK, "", "", "An exception was thrown while process a cancellation task", t);
                    }
                }
            }
        }

        /**
         * calculate goal nanoTime from startTime and current tick number,
         * then wait until that goal has been reached.
         *
         * @return Long.MIN_VALUE if received a shutdown request,
         * current time otherwise (with Long.MIN_VALUE changed by +1)
         */
        private long waitForNextTick() {
            long deadline = tickDuration * (tick + 1);

            for (; ; ) {
                final long currentTime = System.nanoTime() - startTime;
                long sleepTimeMs = (deadline - currentTime + 999999) / 1000000;

                if (sleepTimeMs <= 0) {
                    if (currentTime == Long.MIN_VALUE) {
                        return -Long.MAX_VALUE;
                    } else {
                        return currentTime;
                    }
                }

                // Check if we run on windows, as if thats the case we will need
                // to round the sleepTime as workaround for a bug that only affect
                // the JVM if it runs on windows.
                //
                // See https://github.com/netty/netty/issues/356
                if (isWindows()) {
                    sleepTimeMs = sleepTimeMs / 10 * 10;
                    if (sleepTimeMs == 0) {
                        sleepTimeMs = 1;
                    }
                }

                try {
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException ignored) {
                    if (WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == WORKER_STATE_SHUTDOWN) {
                        return Long.MIN_VALUE;
                    }
                }
            }
        }

        public Set<Timeout> unprocessedTimeouts() {
            return Collections.unmodifiableSet(unprocessedTimeouts);
        }
    }

    private static final class ImmediateExecutor implements Executor {
        public static final ImmediateExecutor INSTANCE = new ImmediateExecutor();

        private ImmediateExecutor() {
            // use static instance
        }

        @Override
        public void execute(Runnable command) {
            if (command == null) {
                throw new NullPointerException("command");
            }
            command.run();
        }
    }

    private static final class MathUtil {

        private MathUtil() {
        }

        /**
         * Fast method of finding the next power of 2 greater than or equal to the supplied value.
         *
         * <p>If the value is {@code <= 0} then 1 will be returned.
         * This method is not suitable for {@link Integer#MIN_VALUE} or numbers greater than 2^30.
         *
         * @param value from which to search for next power of 2
         * @return The next power of 2 or the value itself if it is a power of 2
         */
        public static int findNextPositivePowerOfTwo(final int value) {
            if (value <= Integer.MIN_VALUE) {
                throw new IllegalArgumentException(
                        "ticksPerWheel must be greater than " + Integer.MIN_VALUE + ": " + value);
            }
            if (value >= 0x40000000) {
                throw new IllegalArgumentException(
                        "ticksPerWheel must be less than or equal to 0x40000000: " + value);
            }
            return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
        }

        /**
         * Fast method of finding the next power of 2 greater than or equal to the supplied value.
         * <p>This method will do runtime bounds checking and call {@link #findNextPositivePowerOfTwo(int)} if within a
         * valid range.
         * @param value from which to search for next power of 2
         * @return The next power of 2 or the value itself if it is a power of 2.
         * <p>Special cases for return values are as follows:
         * <ul>
         *     <li>{@code <= 0} -> 1</li>
         *     <li>{@code >= 2^30} -> 2^30</li>
         * </ul>
         */
        public static int safeFindNextPositivePowerOfTwo(final int value) {
            return value <= 0 ? 1 : value >= 0x40000000 ? 0x40000000 : findNextPositivePowerOfTwo(value);
        }

        /**
         * Determine if the requested {@code index} and {@code length} will fit within {@code capacity}.
         * @param index The starting index.
         * @param length The length which will be utilized (starting from {@code index}).
         * @param capacity The capacity that {@code index + length} is allowed to be within.
         * @return {@code false} if the requested {@code index} and {@code length} will fit within {@code capacity}.
         * {@code true} if this would result in an index out of bounds exception.
         */
        public static boolean isOutOfBounds(int index, int length, int capacity) {
            return (index | length | capacity | index + length) < 0 || index + length > capacity;
        }

        /**
         * @deprecated not used anymore. User Integer.compare() instead. For removal.
         * Compares two {@code int} values.
         *
         * @param  x the first {@code int} to compare
         * @param  y the second {@code int} to compare
         * @return the value {@code 0} if {@code x == y};
         *         {@code -1} if {@code x < y}; and
         *         {@code 1} if {@code x > y}
         */
        @Deprecated
        public static int compare(final int x, final int y) {
            // do not subtract for comparison, it could overflow
            return Integer.compare(x, y);
        }

        /**
         * @deprecated not used anymore. User Long.compare() instead. For removal.
         * Compare two {@code long} values.
         * @param x the first {@code long} to compare.
         * @param y the second {@code long} to compare.
         * @return
         * <ul>
         * <li>0 if {@code x == y}</li>
         * <li>{@code > 0} if {@code x > y}</li>
         * <li>{@code < 0} if {@code x < y}</li>
         * </ul>
         */
        @Deprecated
        public static int compare(long x, long y) {
            return Long.compare(x, y);
        }

    }

    private static final class HashedWheelTimeout implements Timeout, Runnable {

        private static final int ST_INIT = 0;
        private static final int ST_CANCELLED = 1;
        private static final int ST_EXPIRED = 2;
        private static final AtomicIntegerFieldUpdater<HashedWheelTimeout> STATE_UPDATER =
                AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimeout.class, "state");

        private final HashedWheelTimer timer;
        private final TimerTask task;
        private final long deadline;

        @SuppressWarnings({"unused", "FieldMayBeFinal", "RedundantFieldInitialization" })
        private volatile int state = ST_INIT;

        // remainingRounds will be calculated and set by Worker.transferTimeoutsToBuckets() before the
        // HashedWheelTimeout will be added to the correct HashedWheelBucket.
        long remainingRounds;

        // This will be used to chain timeouts in HashedWheelTimerBucket via a double-linked-list.
        // As only the workerThread will act on it there is no need for synchronization / volatile.
        HashedWheelTimeout next;
        HashedWheelTimeout prev;

        // The bucket to which the timeout was added
        HashedWheelBucket bucket;

        HashedWheelTimeout(HashedWheelTimer timer, TimerTask task, long deadline) {
            this.timer = timer;
            this.task = task;
            this.deadline = deadline;
        }

        @Override
        public Timer timer() {
            return timer;
        }

        @Override
        public TimerTask task() {
            return task;
        }

        @Override
        public boolean cancel() {
            // only update the state it will be removed from HashedWheelBucket on next tick.
            if (!compareAndSetState(ST_INIT, ST_CANCELLED)) {
                return false;
            }
            // If a task should be canceled we put this to another queue which will be processed on each tick.
            // So this means that we will have a GC latency of max. 1 tick duration which is good enough. This way
            // we can make again use of our MpscLinkedQueue and so minimize the locking / overhead as much as possible.
            timer.cancelledTimeouts.add(this);
            return true;
        }

        private void remove() {
            HashedWheelBucket bucket = this.bucket;
            if (bucket != null) {
                bucket.remove(this);
            }
            timer.pendingTimeouts.decrementAndGet();
        }
        void removeAfterCancellation() {
            remove();
        }

        public boolean compareAndSetState(int expected, int state) {
            return STATE_UPDATER.compareAndSet(this, expected, state);
        }

        public int state() {
            return state;
        }

        @Override
        public boolean isCancelled() {
            return state() == ST_CANCELLED;
        }

        @Override
        public boolean isExpired() {
            return state() == ST_EXPIRED;
        }

        public void expire() {
            if (!compareAndSetState(ST_INIT, ST_EXPIRED)) {
                return;
            }

            try {
                remove();
                timer.taskExecutor.execute(this);
            } catch (Throwable t) {
                if (logger.isWarnEnabled()) {
                    logger.warn(COMMON_ERROR_RUN_THREAD_TASK, "", "", "An exception was thrown while submit " + TimerTask.class.getSimpleName()
                            + " for execution.", t);
                }
            }
        }

        @Override
        public void run() {
            try {
                task.run(this);
            } catch (Throwable t) {
                if (logger.isWarnEnabled()) {
                    logger.warn(COMMON_ERROR_RUN_THREAD_TASK, "", "", "An exception was thrown by " + TimerTask.class.getSimpleName() + '.', t);
                }
            }
        }

        @Override
        public String toString() {
            final long currentTime = System.nanoTime();
            long remaining = deadline - currentTime + timer.startTime;

            StringBuilder buf = new StringBuilder(192)
                    .append(simpleClassName(this.getClass()))
                    .append('(')
                    .append("deadline: ");
            if (remaining > 0) {
                buf.append(remaining)
                        .append(" ns later");
            } else if (remaining < 0) {
                buf.append(-remaining)
                        .append(" ns ago");
            } else {
                buf.append("now");
            }

            if (isCancelled()) {
                buf.append(", cancelled");
            }

            return buf.append(", task: ")
                    .append(task())
                    .append(')')
                    .toString();
        }
    }

    /**
     * Bucket that stores HashedWheelTimeouts. These are stored in a linked-list like datastructure to allow easy
     * removal of HashedWheelTimeouts in the middle. Also the HashedWheelTimeout act as nodes themself and so no
     * extra object creation is needed.
     */
    private static final class HashedWheelBucket {

        /**
         * Used for the linked-list data structure
         */
        private HashedWheelTimeout head;
        private HashedWheelTimeout tail;

        /**
         * Add {@link HashedWheelTimeout} to this bucket.
         */
        public void addTimeout(HashedWheelTimeout timeout) {
            assert timeout.bucket == null;
            timeout.bucket = this;
            if (head == null) {
                head = tail = timeout;
            } else {
                tail.next = timeout;
                timeout.prev = tail;
                tail = timeout;
            }
        }

        /**
         * Expire all {@link HashedWheelTimeout}s for the given {@code deadline}.
         */
        public void expireTimeouts(long deadline) {
            HashedWheelTimeout timeout = head;

            // process all timeouts
            while (timeout != null) {
                HashedWheelTimeout next = timeout.next;
                if (timeout.remainingRounds <= 0) {
                    if (timeout.deadline <= deadline) {
                        timeout.expire();
                    } else {
                        // The timeout was placed into a wrong slot. This should never happen.
                        throw new IllegalStateException(String.format(
                                "timeout.deadline (%d) > deadline (%d)", timeout.deadline, deadline));
                    }
                } else if (!timeout.isCancelled()) {
                    timeout.remainingRounds --;
                }
                timeout = next;
            }
        }

        public HashedWheelTimeout remove(HashedWheelTimeout timeout) {
            HashedWheelTimeout prev = timeout.prev;
            HashedWheelTimeout next = timeout.next;

            // remove timeout that was either processed or cancelled by updating the linked-list
            if (prev != null) {
                prev.next = next;
            }
            if (next != null) {
                next.prev = prev;
            }

            if (timeout == head) {
                head = next;
            }
            if (timeout == tail) {
                tail = prev;
            }
            // null out prev, next and bucket to allow for GC.
            timeout.prev = null;
            timeout.next = null;
            timeout.bucket = null;
            return next;
        }

        /**
         * Clear this bucket and return all not expired / cancelled {@link Timeout}s.
         */
        public void clearTimeouts(Set<Timeout> set) {
            for (;;) {
                HashedWheelTimeout timeout = pollTimeout();
                if (timeout == null) {
                    return;
                }
                if (timeout.isExpired() || timeout.isCancelled()) {
                    continue;
                }
                set.add(timeout);
            }
        }

        private HashedWheelTimeout pollTimeout() {
            HashedWheelTimeout head = this.head;
            if (head == null) {
                return null;
            }
            HashedWheelTimeout next = head.next;
            if (next == null) {
                tail = this.head =  null;
            } else {
                this.head = next;
                next.prev = null;
            }

            // null out prev and next to allow for GC.
            head.next = null;
            head.prev = null;
            head.bucket = null;
            return head;
        }
    }

    private static final boolean IS_OS_WINDOWS = System.getProperty(
            SYSTEM_OS_NAME, "").toLowerCase(Locale.US).contains(OS_WIN_PREFIX);

    private boolean isWindows() {
        return IS_OS_WINDOWS;
    }

    private static final int MAX_ALLOWED_MPSC_CAPACITY = Pow2.MAX_POW2;

    private static final int MPSC_CHUNK_SIZE =  1024;

    private static final int MIN_MAX_MPSC_CAPACITY =  MPSC_CHUNK_SIZE * 2;

    private static <T> Queue<T> newMpscQueue() {
        return USE_MPSC_CHUNKED_ARRAY_QUEUE ? new MpscUnboundedArrayQueue<T>(MPSC_CHUNK_SIZE)
                : new MpscUnboundedAtomicArrayQueue<T>(MPSC_CHUNK_SIZE);
    }

    private static final boolean USE_MPSC_CHUNKED_ARRAY_QUEUE;

    static {
        Object unsafe = null;
        if (hasUnsafe()) {
            // jctools goes through its own process of initializing unsafe; of
            // course, this requires permissions which might not be granted to calling code, so we
            // must mark this block as privileged too
            unsafe = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    // force JCTools to initialize unsafe
                    return UnsafeAccess.UNSAFE;
                }
            });
        }

        if (unsafe == null) {
            logger.debug("org.jctools-core.MpscChunkedArrayQueue: unavailable");
            USE_MPSC_CHUNKED_ARRAY_QUEUE = false;
        } else {
            logger.debug("org.jctools-core.MpscChunkedArrayQueue: available");
            USE_MPSC_CHUNKED_ARRAY_QUEUE = true;
        }
    }

    private static final Throwable UNSAFE_UNAVAILABILITY_CAUSE = unsafeUnavailabilityCause0();

    private static final boolean IS_IVKVM_DOT_NET = isIkvmDotNet0();

    private static boolean isIkvmDotNet0() {
        String vmName = System.getProperty("java.vm.name", "").toUpperCase(Locale.US);
        return vmName.equals("IKVM.NET");
    }

    static boolean isIkvmDotNet() {
        return IS_IVKVM_DOT_NET;
    }
    private static Throwable unsafeUnavailabilityCause0() {
        if (PlatformDependent0.isAndroid()) {
            logger.debug("sun.misc.Unsafe: unavailable (Android)");
            return new UnsupportedOperationException("sun.misc.Unsafe: unavailable (Android)");
        }

        if (isIkvmDotNet()) {
            logger.debug("sun.misc.Unsafe: unavailable (IKVM.NET)");
            return new UnsupportedOperationException("sun.misc.Unsafe: unavailable (IKVM.NET)");
        }

        Throwable cause = PlatformDependent0.getUnsafeUnavailabilityCause();
        if (cause != null) {
            return cause;
        }

        try {
            boolean hasUnsafe = PlatformDependent0.hasUnsafe();
            logger.debug("sun.misc.Unsafe: {}", hasUnsafe ? "available" : "unavailable");
            return null;
        } catch (Throwable t) {
            logger.trace("Could not determine if Unsafe is available", t);
            // Probably failed to initialize PlatformDependent0.
            return new UnsupportedOperationException("Could not determine if Unsafe is available", t);
        }
    }

    private static boolean hasUnsafe() {
        return UNSAFE_UNAVAILABILITY_CAUSE == null;
    }

    private <T> Queue<T> newChunkedMpscQueue(final int chunkSize, final int capacity) {
        return USE_MPSC_CHUNKED_ARRAY_QUEUE ? new MpscChunkedArrayQueue<T>(chunkSize, capacity)
                : new MpscChunkedAtomicArrayQueue<T>(chunkSize, capacity);
    }


    private static final class PlatformDependent0 {
        private static final long ADDRESS_FIELD_OFFSET;
        private static final long BYTE_ARRAY_BASE_OFFSET;
        private static final long INT_ARRAY_BASE_OFFSET;
        private static final long INT_ARRAY_INDEX_SCALE;
        private static final long LONG_ARRAY_BASE_OFFSET;
        private static final long LONG_ARRAY_INDEX_SCALE;
        private static final MethodHandle DIRECT_BUFFER_CONSTRUCTOR;
        private static final MethodHandle ALLOCATE_ARRAY_METHOD;
        private static final MethodHandle ALIGN_SLICE;
        private static final MethodHandle OFFSET_SLICE;
        private static final boolean IS_ANDROID = isAndroid0();
        private static final int JAVA_VERSION = javaVersion0();
        private static final Throwable EXPLICIT_NO_UNSAFE_CAUSE = explicitNoUnsafeCause0();

        private static final Throwable UNSAFE_UNAVAILABILITY_CAUSE;

        // See https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/
        // ImageInfo.java
        private static final boolean RUNNING_IN_NATIVE_IMAGE = System.getProperty("org.graalvm.nativeimage.imagecode") != null;

        private static final boolean IS_EXPLICIT_TRY_REFLECTION_SET_ACCESSIBLE = explicitTryReflectionSetAccessible0();

        // Package-private for testing.
        static final MethodHandle IS_VIRTUAL_THREAD_METHOD_HANDLE = getIsVirtualThreadMethodHandle();

        static final Unsafe UNSAFE;

        // constants borrowed from murmur3
        static final int HASH_CODE_ASCII_SEED = 0xc2b2ae35;
        static final int HASH_CODE_C1 = 0xcc9e2d51;
        static final int HASH_CODE_C2 = 0x1b873593;

        /**
         * Limits the number of bytes to copy per {@link Unsafe#copyMemory(long, long, long)} to allow safepoint polling
         * during a large copy.
         */
        private static final long UNSAFE_COPY_THRESHOLD = 1024L * 1024L;

        private static final boolean UNALIGNED;

        private static final long BITS_MAX_DIRECT_MEMORY;

        static {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            final ByteBuffer direct;
            Field addressField = null;
            MethodHandle allocateArrayMethod = null;
            Throwable unsafeUnavailabilityCause;
            Unsafe unsafe;
            if ((unsafeUnavailabilityCause = EXPLICIT_NO_UNSAFE_CAUSE) != null) {
                direct = null;
                addressField = null;
                unsafe = null;
            } else {
                direct = ByteBuffer.allocateDirect(1);

                // attempt to access field Unsafe#theUnsafe
                final Object maybeUnsafe = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        try {
                            final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                            // We always want to try using Unsafe as the access still works on java9 as well and
                            // we need it for out native-transports and many optimizations.
                            Throwable cause = ReflectionUtil.trySetAccessible(unsafeField, false);
                            if (cause != null) {
                                return cause;
                            }
                            // the unsafe instance
                            return unsafeField.get(null);
                        } catch (NoSuchFieldException | IllegalAccessException | SecurityException e) {
                            return e;
                        } catch (NoClassDefFoundError e) {
                            // Also catch NoClassDefFoundError in case someone uses for example OSGI and it made
                            // Unsafe unloadable.
                            return e;
                        }
                    }
                });

                // the conditional check here can not be replaced with checking that maybeUnsafe
                // is an instanceof Unsafe and reversing the if and else blocks; this is because an
                // instanceof check against Unsafe will trigger a class load and we might not have
                // the runtime permission accessClassInPackage.sun.misc
                if (maybeUnsafe instanceof Throwable) {
                    unsafe = null;
                    unsafeUnavailabilityCause = (Throwable) maybeUnsafe;
                    if (logger.isTraceEnabled()) {
                        logger.debug("sun.misc.Unsafe.theUnsafe: unavailable", unsafeUnavailabilityCause);
                    } else {
                        logger.debug("sun.misc.Unsafe.theUnsafe: unavailable: {}", unsafeUnavailabilityCause.getMessage());
                    }
                } else {
                    unsafe = (Unsafe) maybeUnsafe;
                    logger.debug("sun.misc.Unsafe.theUnsafe: available");
                }

                // ensure the unsafe supports all necessary methods to work around the mistake in the latest OpenJDK,
                // or that they haven't been removed by JEP 471.
                // https://github.com/netty/netty/issues/1061
                // https://www.mail-archive.com/jdk6-dev@openjdk.java.net/msg00698.html
                // https://openjdk.org/jeps/471
                if (unsafe != null) {
                    final Unsafe finalUnsafe = unsafe;
                    final Object maybeException = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {
                            try {
                                // Other methods like storeFence() and invokeCleaner() are tested for elsewhere.
                                Class<? extends Unsafe> cls = finalUnsafe.getClass();
                                cls.getDeclaredMethod(
                                        "copyMemory", Object.class, long.class, Object.class, long.class, long.class);
                                if (javaVersion() > 23) {
                                    cls.getDeclaredMethod("objectFieldOffset", Field.class);
                                    cls.getDeclaredMethod("staticFieldOffset", Field.class);
                                    cls.getDeclaredMethod("staticFieldBase", Field.class);
                                    cls.getDeclaredMethod("arrayBaseOffset", Class.class);
                                    cls.getDeclaredMethod("arrayIndexScale", Class.class);
                                    cls.getDeclaredMethod("allocateMemory", long.class);
                                    cls.getDeclaredMethod("reallocateMemory", long.class, long.class);
                                    cls.getDeclaredMethod("freeMemory", long.class);
                                    cls.getDeclaredMethod("setMemory", long.class, long.class, byte.class);
                                    cls.getDeclaredMethod("setMemory", Object.class, long.class, long.class, byte.class);
                                    cls.getDeclaredMethod("getBoolean", Object.class, long.class);
                                    cls.getDeclaredMethod("getByte", long.class);
                                    cls.getDeclaredMethod("getByte", Object.class, long.class);
                                    cls.getDeclaredMethod("getInt", long.class);
                                    cls.getDeclaredMethod("getInt", Object.class, long.class);
                                    cls.getDeclaredMethod("getLong", long.class);
                                    cls.getDeclaredMethod("getLong", Object.class, long.class);
                                    cls.getDeclaredMethod("putByte", long.class, byte.class);
                                    cls.getDeclaredMethod("putByte", Object.class, long.class, byte.class);
                                    cls.getDeclaredMethod("putInt", long.class, int.class);
                                    cls.getDeclaredMethod("putInt", Object.class, long.class, int.class);
                                    cls.getDeclaredMethod("putLong", long.class, long.class);
                                    cls.getDeclaredMethod("putLong", Object.class, long.class, long.class);
                                    cls.getDeclaredMethod("addressSize");
                                }
                                if (javaVersion() >= 23) {
                                    // The following tests the methods are usable.
                                    // Will throw UnsupportedOperationException if unsafe memory access is denied:
                                    long address = finalUnsafe.allocateMemory(8);
                                    finalUnsafe.putLong(address, 42);
                                    finalUnsafe.freeMemory(address);
                                }
                                return null;
                            } catch (UnsupportedOperationException | SecurityException | NoSuchMethodException e) {
                                return e;
                            }
                        }
                    });

                    if (maybeException == null) {
                        logger.debug("sun.misc.Unsafe base methods: all available");
                    } else {
                        // Unsafe.copyMemory(Object, long, Object, long, long) unavailable.
                        unsafe = null;
                        unsafeUnavailabilityCause = (Throwable) maybeException;
                        if (logger.isTraceEnabled()) {
                            logger.debug("sun.misc.Unsafe method unavailable:", unsafeUnavailabilityCause);
                        } else {
                            logger.debug("sun.misc.Unsafe method unavailable: {}", unsafeUnavailabilityCause.getMessage());
                        }
                    }
                }

                if (unsafe != null) {
                    final Unsafe finalUnsafe = unsafe;

                    // attempt to access field Buffer#address
                    final Object maybeAddressField = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {
                            try {
                                final Field field = Buffer.class.getDeclaredField("address");
                                // Use Unsafe to read value of the address field. This way it will not fail on JDK9+ which
                                // will forbid changing the access level via reflection.
                                final long offset = finalUnsafe.objectFieldOffset(field);
                                final long address = finalUnsafe.getLong(direct, offset);

                                // if direct really is a direct buffer, address will be non-zero
                                if (address == 0) {
                                    return null;
                                }
                                return field;
                            } catch (NoSuchFieldException | SecurityException e) {
                                return e;
                            }
                        }
                    });

                    if (maybeAddressField instanceof Field) {
                        addressField = (Field) maybeAddressField;
                        logger.debug("java.nio.Buffer.address: available");
                    } else {
                        unsafeUnavailabilityCause = (Throwable) maybeAddressField;
                        if (logger.isTraceEnabled()) {
                            logger.debug("java.nio.Buffer.address: unavailable", (Throwable) maybeAddressField);
                        } else {
                            logger.debug("java.nio.Buffer.address: unavailable: {}",
                                    ((Throwable) maybeAddressField).getMessage());
                        }

                        // If we cannot access the address of a direct buffer, there's no point of using unsafe.
                        // Let's just pretend unsafe is unavailable for overall simplicity.
                        unsafe = null;
                    }
                }

                if (unsafe != null) {
                    // There are assumptions made where ever BYTE_ARRAY_BASE_OFFSET is used (equals, hashCodeAscii, and
                    // primitive accessors) that arrayIndexScale == 1, and results are undefined if this is not the case.
                    long byteArrayIndexScale = unsafe.arrayIndexScale(byte[].class);
                    if (byteArrayIndexScale != 1) {
                        logger.debug("unsafe.arrayIndexScale is {} (expected: 1). Not using unsafe.", byteArrayIndexScale);
                        unsafeUnavailabilityCause = new UnsupportedOperationException("Unexpected unsafe.arrayIndexScale");
                        unsafe = null;
                    }
                }
            }
            UNSAFE_UNAVAILABILITY_CAUSE = unsafeUnavailabilityCause;
            UNSAFE = unsafe;

            if (unsafe == null) {
                ADDRESS_FIELD_OFFSET = -1;
                BYTE_ARRAY_BASE_OFFSET = -1;
                LONG_ARRAY_BASE_OFFSET = -1;
                LONG_ARRAY_INDEX_SCALE = -1;
                INT_ARRAY_BASE_OFFSET = -1;
                INT_ARRAY_INDEX_SCALE = -1;
                UNALIGNED = false;
                BITS_MAX_DIRECT_MEMORY = -1;
                DIRECT_BUFFER_CONSTRUCTOR = null;
                ALLOCATE_ARRAY_METHOD = null;
            } else {
                MethodHandle directBufferConstructor;
                long address = -1;
                try {
                    final Object maybeDirectBufferConstructor =
                            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                                @Override
                                public Object run() {
                                    try {
                                        Class<? extends ByteBuffer> directClass = direct.getClass();
                                        final Constructor<?> constructor = javaVersion() >= 21 ?
                                                directClass.getDeclaredConstructor(long.class, long.class) :
                                                directClass.getDeclaredConstructor(long.class, int.class);
                                        Throwable cause = ReflectionUtil.trySetAccessible(constructor, true);
                                        if (cause != null) {
                                            return cause;
                                        }
                                        return lookup.unreflectConstructor(constructor)
                                                .asType(methodType(ByteBuffer.class, long.class, int.class));
                                    } catch (Throwable e) {
                                        return e;
                                    }
                                }
                            });

                    if (maybeDirectBufferConstructor instanceof MethodHandle) {
                        address = UNSAFE.allocateMemory(1);
                        // try to use the constructor now
                        try {
                            MethodHandle constructor = (MethodHandle) maybeDirectBufferConstructor;
                            ByteBuffer ignore = (ByteBuffer) constructor.invokeExact(address, 1);
                            directBufferConstructor = constructor;
                            logger.debug("direct buffer constructor: available");
                        } catch (Throwable e) {
                            directBufferConstructor = null;
                        }
                    } else {
                        if (logger.isTraceEnabled()) {
                            logger.debug("direct buffer constructor: unavailable",
                                    (Throwable) maybeDirectBufferConstructor);
                        } else {
                            logger.debug("direct buffer constructor: unavailable: {}",
                                    ((Throwable) maybeDirectBufferConstructor).getMessage());
                        }
                        directBufferConstructor = null;
                    }
                } finally {
                    if (address != -1) {
                        UNSAFE.freeMemory(address);
                    }
                }
                DIRECT_BUFFER_CONSTRUCTOR = directBufferConstructor;
                ADDRESS_FIELD_OFFSET = objectFieldOffset(addressField);
                BYTE_ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
                INT_ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(int[].class);
                INT_ARRAY_INDEX_SCALE = UNSAFE.arrayIndexScale(int[].class);
                LONG_ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(long[].class);
                LONG_ARRAY_INDEX_SCALE = UNSAFE.arrayIndexScale(long[].class);
                final boolean unaligned;
                // using a known type to avoid loading new classes
                final AtomicLong maybeMaxMemory = new AtomicLong(-1);
                Object maybeUnaligned = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        try {
                            Class<?> bitsClass =
                                    Class.forName("java.nio.Bits", false, getSystemClassLoader());
                            int version = javaVersion();
                            if (version >= 9) {
                                // Java9/10 use all lowercase and later versions all uppercase.
                                String fieldName = version >= 11? "MAX_MEMORY" : "maxMemory";
                                // On Java9 and later we try to directly access the field as we can do this without
                                // adjust the accessible levels.
                                try {
                                    Field maxMemoryField = bitsClass.getDeclaredField(fieldName);
                                    if (maxMemoryField.getType() == long.class) {
                                        long offset = UNSAFE.staticFieldOffset(maxMemoryField);
                                        Object object = UNSAFE.staticFieldBase(maxMemoryField);
                                        maybeMaxMemory.lazySet(UNSAFE.getLong(object, offset));
                                    }
                                } catch (Throwable ignore) {
                                    // ignore if can't access
                                }
                                fieldName = version >= 11? "UNALIGNED" : "unaligned";
                                try {
                                    Field unalignedField = bitsClass.getDeclaredField(fieldName);
                                    if (unalignedField.getType() == boolean.class) {
                                        long offset = UNSAFE.staticFieldOffset(unalignedField);
                                        Object object = UNSAFE.staticFieldBase(unalignedField);
                                        return UNSAFE.getBoolean(object, offset);
                                    }
                                    // There is something unexpected stored in the field,
                                    // let us fall-back and try to use a reflective method call as last resort.
                                } catch (NoSuchFieldException ignore) {
                                    // We did not find the field we expected, move on.
                                }
                            }
                            Method unalignedMethod = bitsClass.getDeclaredMethod("unaligned");
                            Throwable cause = ReflectionUtil.trySetAccessible(unalignedMethod, true);
                            if (cause != null) {
                                return cause;
                            }
                            return unalignedMethod.invoke(null);
                        } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                                 InvocationTargetException | ClassNotFoundException e) {
                            return e;
                        }
                    }
                });

                if (maybeUnaligned instanceof Boolean) {
                    unaligned = (Boolean) maybeUnaligned;
                    logger.debug("java.nio.Bits.unaligned: available, {}", unaligned);
                } else {
                    String arch = System.getProperty("os.arch", "");
                    //noinspection DynamicRegexReplaceableByCompiledPattern
                    unaligned = arch.matches("^(i[3-6]86|x86(_64)?|x64|amd64)$");
                    Throwable t = (Throwable) maybeUnaligned;
                    if (logger.isTraceEnabled()) {
                        logger.debug("java.nio.Bits.unaligned: unavailable, {}", unaligned, t);
                    } else {
                        logger.debug("java.nio.Bits.unaligned: unavailable, {}, {}", unaligned, t.getMessage());
                    }
                }

                UNALIGNED = unaligned;
                BITS_MAX_DIRECT_MEMORY = maybeMaxMemory.get() >= 0? maybeMaxMemory.get() : -1;

                if (javaVersion() >= 9) {
                    Object maybeException = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {
                            try {
                                // Java9 has jdk.internal.misc.Unsafe and not all methods are propagated to
                                // sun.misc.Unsafe
                                Class<?> cls = getClassLoader(PlatformDependent0.class)
                                        .loadClass("jdk.internal.misc.Unsafe");
                                return lookup.findStatic(cls, "getUnsafe", methodType(cls)).invoke();
                            } catch (Throwable e) {
                                return e;
                            }
                        }
                    });
                    if (!(maybeException instanceof Throwable)) {
                        final Object finalInternalUnsafe = maybeException;
                        maybeException = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                            @Override
                            public Object run() {
                                try {
                                    Class<?> finalInternalUnsafeClass = finalInternalUnsafe.getClass();
                                    return lookup.findVirtual(
                                            finalInternalUnsafeClass,
                                            "allocateUninitializedArray",
                                            methodType(Object.class, Class.class, int.class));
                                } catch (Throwable e) {
                                    return e;
                                }
                            }
                        });

                        if (maybeException instanceof MethodHandle) {
                            try {
                                MethodHandle m = (MethodHandle) maybeException;
                                m = m.bindTo(finalInternalUnsafe);
                                byte[] bytes = (byte[]) (Object) m.invokeExact(byte.class, 8);
                                assert bytes.length == 8;
                                allocateArrayMethod = m;
                            } catch (Throwable e) {
                                maybeException = e;
                            }
                        }
                    }

                    if (maybeException instanceof Throwable) {
                        if (logger.isTraceEnabled()) {
                            logger.debug("jdk.internal.misc.Unsafe.allocateUninitializedArray(int): unavailable",
                                    (Throwable) maybeException);
                        } else {
                            logger.debug("jdk.internal.misc.Unsafe.allocateUninitializedArray(int): unavailable: {}",
                                    ((Throwable) maybeException).getMessage());
                        }
                    } else {
                        logger.debug("jdk.internal.misc.Unsafe.allocateUninitializedArray(int): available");
                    }
                } else {
                    logger.debug("jdk.internal.misc.Unsafe.allocateUninitializedArray(int): unavailable prior to Java9");
                }
                ALLOCATE_ARRAY_METHOD = allocateArrayMethod;
            }

            if (javaVersion() > 9) {
                ALIGN_SLICE = (MethodHandle) AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        try {
                            return MethodHandles.publicLookup().findVirtual(
                                    ByteBuffer.class, "alignedSlice", methodType(ByteBuffer.class, int.class));
                        } catch (Throwable e) {
                            return null;
                        }
                    }
                });
            } else {
                ALIGN_SLICE = null;
            }

            if (javaVersion() >= 13) {
                OFFSET_SLICE = (MethodHandle) AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        try {
                            return MethodHandles.publicLookup().findVirtual(
                                    ByteBuffer.class, "slice", methodType(ByteBuffer.class, int.class, int.class));
                        } catch (Throwable e) {
                            return null;
                        }
                    }
                });
            } else {
                OFFSET_SLICE = null;
            }

            logger.debug("java.nio.DirectByteBuffer.<init>(long, {int,long}): {}",
                    DIRECT_BUFFER_CONSTRUCTOR != null ? "available" : "unavailable");
        }

        private static MethodHandle getIsVirtualThreadMethodHandle() {
            try {
                MethodHandle methodHandle = MethodHandles.publicLookup().findVirtual(Thread.class, "isVirtual",
                        methodType(boolean.class));
                // Call once to make sure the invocation works.
                boolean isVirtual = (boolean) methodHandle.invokeExact(Thread.currentThread());
                return methodHandle;
            } catch (Throwable e) {
                if (logger.isTraceEnabled()) {
                    logger.debug("Thread.isVirtual() is not available: ", e);
                } else {
                    logger.debug("Thread.isVirtual() is not available: ", e.getMessage());
                }
                return null;
            }
        }

        /**
         * @param thread The thread to be checked.
         * @return {@code true} if this {@link Thread} is a virtual thread, {@code false} otherwise.
         */
        static boolean isVirtualThread(Thread thread) {
            if (thread == null || IS_VIRTUAL_THREAD_METHOD_HANDLE == null) {
                return false;
            }
            try {
                return (boolean) IS_VIRTUAL_THREAD_METHOD_HANDLE.invokeExact(thread);
            } catch (Throwable t) {
                // Should not happen.
                if (t instanceof Error) {
                    throw (Error) t;
                }
                throw new Error(t);
            }
        }

        static boolean isNativeImage() {
            return RUNNING_IN_NATIVE_IMAGE;
        }

        static boolean isExplicitNoUnsafe() {
            return EXPLICIT_NO_UNSAFE_CAUSE != null;
        }

        private static Throwable explicitNoUnsafeCause0() {
            boolean explicitProperty = System.getProperty("io.netty.noUnsafe") != null;
            boolean noUnsafe = Boolean.parseBoolean(System.getProperty("io.netty.noUnsafe", "false"));
            logger.debug("-Dio.netty.noUnsafe: {}", noUnsafe);

            // See JDK 23 JEP 471 https://openjdk.org/jeps/471 and sun.misc.Unsafe.beforeMemoryAccess() on JDK 23+.
            // And JDK 24 JEP 498 https://openjdk.org/jeps/498, that enable warnings by default.
            // Due to JDK bugs, we only actually disable Unsafe by default on Java 25+, where we have memory segment APIs
            // available, and working.
            String reason = "io.netty.noUnsafe";
            String unspecified = "<unspecified>";
            String unsafeMemoryAccess = System.getProperty("sun.misc.unsafe.memory.access", unspecified);
            if (!explicitProperty && unspecified.equals(unsafeMemoryAccess) && javaVersion() >= 25) {
                reason = "io.netty.noUnsafe=true by default on Java 25+";
                noUnsafe = true;
            } else if (!("allow".equals(unsafeMemoryAccess) || unspecified.equals(unsafeMemoryAccess))) {
                reason = "--sun-misc-unsafe-memory-access=" + unsafeMemoryAccess;
                noUnsafe = true;
            }

            if (noUnsafe) {
                String msg = "sun.misc.Unsafe: unavailable (" + reason + ')';
                logger.debug(msg);
                return new UnsupportedOperationException(msg);
            }

            // Legacy properties
            String unsafePropName;
            if (System.getProperty("io.netty.tryUnsafe") != null) {
                unsafePropName = "io.netty.tryUnsafe";
            } else {
                unsafePropName = "org.jboss.netty.tryUnsafe";
            }

            if (!Boolean.parseBoolean(System.getProperty(unsafePropName, "true"))) {
                String msg = "sun.misc.Unsafe: unavailable (" + unsafePropName + ')';
                logger.debug(msg);
                return new UnsupportedOperationException(msg);
            }

            return null;
        }

        static boolean isUnaligned() {
            return UNALIGNED;
        }

        /**
         * Any value >= 0 should be considered as a valid max direct memory value.
         */
        static long bitsMaxDirectMemory() {
            return BITS_MAX_DIRECT_MEMORY;
        }

        static boolean hasUnsafe() {
            return UNSAFE != null;
        }

        static Throwable getUnsafeUnavailabilityCause() {
            return UNSAFE_UNAVAILABILITY_CAUSE;
        }

        static boolean unalignedAccess() {
            return UNALIGNED;
        }

        static void throwException(Throwable cause) {
            throwException0(cause);
        }

        @SuppressWarnings("unchecked")
        private static <E extends Throwable> void throwException0(Throwable t) throws E {
            throw (E) t;
        }

        static boolean hasDirectBufferNoCleanerConstructor() {
            return DIRECT_BUFFER_CONSTRUCTOR != null;
        }

        static ByteBuffer reallocateDirectNoCleaner(ByteBuffer buffer, int capacity) {
            return newDirectBuffer(UNSAFE.reallocateMemory(directBufferAddress(buffer), capacity), capacity);
        }

        static ByteBuffer allocateDirectNoCleaner(int capacity) {
            // Calling malloc with capacity of 0 may return a null ptr or a memory address that can be used.
            // Just use 1 to make it safe to use in all cases:
            // See: https://pubs.opengroup.org/onlinepubs/009695399/functions/malloc.html
            return newDirectBuffer(UNSAFE.allocateMemory(Math.max(1, capacity)), capacity);
        }

        static boolean hasAlignSliceMethod() {
            return ALIGN_SLICE != null;
        }

        static ByteBuffer alignSlice(ByteBuffer buffer, int alignment) {
            try {
                return (ByteBuffer) ALIGN_SLICE.invokeExact(buffer, alignment);
            } catch (Throwable e) {
                rethrowIfPossible(e);
                throw new LinkageError("ByteBuffer.alignedSlice not available", e);
            }
        }

        static boolean hasOffsetSliceMethod() {
            return OFFSET_SLICE != null;
        }

        static ByteBuffer offsetSlice(ByteBuffer buffer, int index, int length) {
            try {
                return (ByteBuffer) OFFSET_SLICE.invokeExact(buffer, index, length);
            } catch (Throwable e) {
                rethrowIfPossible(e);
                throw new LinkageError("ByteBuffer.slice(int, int) not available", e);
            }
        }

        static boolean hasAllocateArrayMethod() {
            return ALLOCATE_ARRAY_METHOD != null;
        }

        static byte[] allocateUninitializedArray(int size) {
            try {
                return (byte[]) (Object) ALLOCATE_ARRAY_METHOD.invokeExact(byte.class, size);
            } catch (Throwable e) {
                rethrowIfPossible(e);
                throw new LinkageError("Unsafe.allocateUninitializedArray not available", e);
            }
        }

        static ByteBuffer newDirectBuffer(long address, int capacity) {
            if (capacity < 0) {
                throw new IllegalArgumentException("capacity : " + capacity + " (expected: >= 0)");
            }

            try {
                return (ByteBuffer) DIRECT_BUFFER_CONSTRUCTOR.invokeExact(address, capacity);
            } catch (Throwable cause) {
                rethrowIfPossible(cause);
                throw new LinkageError("DirectByteBuffer constructor not available", cause);
            }
        }

        private static void rethrowIfPossible(Throwable cause) {
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
        }

        static long directBufferAddress(ByteBuffer buffer) {
            return getLong(buffer, ADDRESS_FIELD_OFFSET);
        }

        static long byteArrayBaseOffset() {
            return BYTE_ARRAY_BASE_OFFSET;
        }

        static Object getObject(Object object, long fieldOffset) {
            return UNSAFE.getObject(object, fieldOffset);
        }

        static int getInt(Object object, long fieldOffset) {
            return UNSAFE.getInt(object, fieldOffset);
        }

        static int getIntVolatile(Object object, long fieldOffset) {
            return UNSAFE.getIntVolatile(object, fieldOffset);
        }

        static void putOrderedInt(Object object, long fieldOffset, int value) {
            UNSAFE.putOrderedInt(object, fieldOffset, value);
        }

        static int getAndAddInt(Object object, long fieldOffset, int value) {
            return UNSAFE.getAndAddInt(object, fieldOffset, value);
        }

        static boolean compareAndSwapInt(Object object, long fieldOffset, int expected, int value) {
            return UNSAFE.compareAndSwapInt(object, fieldOffset, expected, value);
        }

        static void safeConstructPutInt(Object object, long fieldOffset, int value) {
            UNSAFE.putInt(object, fieldOffset, value);
            UNSAFE.storeFence();
        }

        private static long getLong(Object object, long fieldOffset) {
            return UNSAFE.getLong(object, fieldOffset);
        }

        static long objectFieldOffset(Field field) {
            return UNSAFE.objectFieldOffset(field);
        }

        static byte getByte(long address) {
            return UNSAFE.getByte(address);
        }

        static short getShort(long address) {
            return UNSAFE.getShort(address);
        }

        static int getInt(long address) {
            return UNSAFE.getInt(address);
        }

        static long getLong(long address) {
            return UNSAFE.getLong(address);
        }

        static byte getByte(byte[] data, int index) {
            return UNSAFE.getByte(data, BYTE_ARRAY_BASE_OFFSET + index);
        }

        static byte getByte(byte[] data, long index) {
            return UNSAFE.getByte(data, BYTE_ARRAY_BASE_OFFSET + index);
        }

        static short getShort(byte[] data, int index) {
            return UNSAFE.getShort(data, BYTE_ARRAY_BASE_OFFSET + index);
        }

        static int getInt(byte[] data, int index) {
            return UNSAFE.getInt(data, BYTE_ARRAY_BASE_OFFSET + index);
        }

        static int getInt(int[] data, long index) {
            return UNSAFE.getInt(data, INT_ARRAY_BASE_OFFSET + INT_ARRAY_INDEX_SCALE * index);
        }

        static long getLong(byte[] data, int index) {
            return UNSAFE.getLong(data, BYTE_ARRAY_BASE_OFFSET + index);
        }

        static long getLong(long[] data, long index) {
            return UNSAFE.getLong(data, LONG_ARRAY_BASE_OFFSET + LONG_ARRAY_INDEX_SCALE * index);
        }

        static void putByte(long address, byte value) {
            UNSAFE.putByte(address, value);
        }

        static void putShort(long address, short value) {
            UNSAFE.putShort(address, value);
        }

        static void putShortOrdered(long address, short newValue) {
            UNSAFE.storeFence();
            UNSAFE.putShort(null, address, newValue);
        }

        static void putInt(long address, int value) {
            UNSAFE.putInt(address, value);
        }

        static void putLong(long address, long value) {
            UNSAFE.putLong(address, value);
        }

        static void putByte(byte[] data, int index, byte value) {
            UNSAFE.putByte(data, BYTE_ARRAY_BASE_OFFSET + index, value);
        }

        static void putByte(Object data, long offset, byte value) {
            UNSAFE.putByte(data, offset, value);
        }

        static void putShort(byte[] data, int index, short value) {
            UNSAFE.putShort(data, BYTE_ARRAY_BASE_OFFSET + index, value);
        }

        static void putInt(byte[] data, int index, int value) {
            UNSAFE.putInt(data, BYTE_ARRAY_BASE_OFFSET + index, value);
        }

        static void putLong(byte[] data, int index, long value) {
            UNSAFE.putLong(data, BYTE_ARRAY_BASE_OFFSET + index, value);
        }

        static void putObject(Object o, long offset, Object x) {
            UNSAFE.putObject(o, offset, x);
        }

        static void copyMemory(long srcAddr, long dstAddr, long length) {
            // Manual safe-point polling is only needed prior Java9:
            // See https://bugs.openjdk.java.net/browse/JDK-8149596
            if (javaVersion() <= 8) {
                copyMemoryWithSafePointPolling(srcAddr, dstAddr, length);
            } else {
                UNSAFE.copyMemory(srcAddr, dstAddr, length);
            }
        }

        private static void copyMemoryWithSafePointPolling(long srcAddr, long dstAddr, long length) {
            while (length > 0) {
                long size = Math.min(length, UNSAFE_COPY_THRESHOLD);
                UNSAFE.copyMemory(srcAddr, dstAddr, size);
                length -= size;
                srcAddr += size;
                dstAddr += size;
            }
        }

        static void copyMemory(Object src, long srcOffset, Object dst, long dstOffset, long length) {
            // Manual safe-point polling is only needed prior Java9:
            // See https://bugs.openjdk.java.net/browse/JDK-8149596
            if (javaVersion() <= 8) {
                copyMemoryWithSafePointPolling(src, srcOffset, dst, dstOffset, length);
            } else {
                UNSAFE.copyMemory(src, srcOffset, dst, dstOffset, length);
            }
        }

        private static void copyMemoryWithSafePointPolling(
                Object src, long srcOffset, Object dst, long dstOffset, long length) {
            while (length > 0) {
                long size = Math.min(length, UNSAFE_COPY_THRESHOLD);
                UNSAFE.copyMemory(src, srcOffset, dst, dstOffset, size);
                length -= size;
                srcOffset += size;
                dstOffset += size;
            }
        }

        static void setMemory(long address, long bytes, byte value) {
            UNSAFE.setMemory(address, bytes, value);
        }

        static void setMemory(Object o, long offset, long bytes, byte value) {
            UNSAFE.setMemory(o, offset, bytes, value);
        }

        static boolean equals(byte[] bytes1, int startPos1, byte[] bytes2, int startPos2, int length) {
            int remainingBytes = length & 7;
            final long baseOffset1 = BYTE_ARRAY_BASE_OFFSET + startPos1;
            final long diff = startPos2 - startPos1;
            if (length >= 8) {
                final long end = baseOffset1 + remainingBytes;
                for (long i = baseOffset1 - 8 + length; i >= end; i -= 8) {
                    if (UNSAFE.getLong(bytes1, i) != UNSAFE.getLong(bytes2, i + diff)) {
                        return false;
                    }
                }
            }
            if (remainingBytes >= 4) {
                remainingBytes -= 4;
                long pos = baseOffset1 + remainingBytes;
                if (UNSAFE.getInt(bytes1, pos) != UNSAFE.getInt(bytes2, pos + diff)) {
                    return false;
                }
            }
            final long baseOffset2 = baseOffset1 + diff;
            if (remainingBytes >= 2) {
                return UNSAFE.getChar(bytes1, baseOffset1) == UNSAFE.getChar(bytes2, baseOffset2) &&
                        (remainingBytes == 2 ||
                                UNSAFE.getByte(bytes1, baseOffset1 + 2) == UNSAFE.getByte(bytes2, baseOffset2 + 2));
            }
            return remainingBytes == 0 ||
                    UNSAFE.getByte(bytes1, baseOffset1) == UNSAFE.getByte(bytes2, baseOffset2);
        }

        static int equalsConstantTime(byte[] bytes1, int startPos1, byte[] bytes2, int startPos2, int length) {
            long result = 0;
            long remainingBytes = length & 7;
            final long baseOffset1 = BYTE_ARRAY_BASE_OFFSET + startPos1;
            final long end = baseOffset1 + remainingBytes;
            final long diff = startPos2 - startPos1;
            for (long i = baseOffset1 - 8 + length; i >= end; i -= 8) {
                result |= UNSAFE.getLong(bytes1, i) ^ UNSAFE.getLong(bytes2, i + diff);
            }
            if (remainingBytes >= 4) {
                result |= UNSAFE.getInt(bytes1, baseOffset1) ^ UNSAFE.getInt(bytes2, baseOffset1 + diff);
                remainingBytes -= 4;
            }
            if (remainingBytes >= 2) {
                long pos = end - remainingBytes;
                result |= UNSAFE.getChar(bytes1, pos) ^ UNSAFE.getChar(bytes2, pos + diff);
                remainingBytes -= 2;
            }
            if (remainingBytes == 1) {
                long pos = end - 1;
                result |= UNSAFE.getByte(bytes1, pos) ^ UNSAFE.getByte(bytes2, pos + diff);
            }
            return equalsConstantTime(result, 0);
        }

        static int equalsConstantTime(int x, int y) {
            int z = ~(x ^ y);
            z &= z >> 16;
            z &= z >> 8;
            z &= z >> 4;
            z &= z >> 2;
            z &= z >> 1;
            return z & 1;
        }

        static int equalsConstantTime(long x, long y) {
            long z = ~(x ^ y);
            z &= z >> 32;
            z &= z >> 16;
            z &= z >> 8;
            z &= z >> 4;
            z &= z >> 2;
            z &= z >> 1;
            return (int) (z & 1);
        }

        static boolean isZero(byte[] bytes, int startPos, int length) {
            if (length <= 0) {
                return true;
            }
            final long baseOffset = BYTE_ARRAY_BASE_OFFSET + startPos;
            int remainingBytes = length & 7;
            final long end = baseOffset + remainingBytes;
            for (long i = baseOffset - 8 + length; i >= end; i -= 8) {
                if (UNSAFE.getLong(bytes, i) != 0) {
                    return false;
                }
            }

            if (remainingBytes >= 4) {
                remainingBytes -= 4;
                if (UNSAFE.getInt(bytes, baseOffset + remainingBytes) != 0) {
                    return false;
                }
            }
            if (remainingBytes >= 2) {
                return UNSAFE.getChar(bytes, baseOffset) == 0 &&
                        (remainingBytes == 2 || bytes[startPos + 2] == 0);
            }
            return bytes[startPos] == 0;
        }

        static int hashCodeAscii(byte[] bytes, int startPos, int length) {
            int hash = HASH_CODE_ASCII_SEED;
            long baseOffset = BYTE_ARRAY_BASE_OFFSET + startPos;
            final int remainingBytes = length & 7;
            final long end = baseOffset + remainingBytes;
            for (long i = baseOffset - 8 + length; i >= end; i -= 8) {
                hash = hashCodeAsciiCompute(UNSAFE.getLong(bytes, i), hash);
            }
            if (remainingBytes == 0) {
                return hash;
            }
            int hcConst = HASH_CODE_C1;
            if (remainingBytes != 2 & remainingBytes != 4 & remainingBytes != 6) { // 1, 3, 5, 7
                hash = hash * HASH_CODE_C1 + hashCodeAsciiSanitize(UNSAFE.getByte(bytes, baseOffset));
                hcConst = HASH_CODE_C2;
                baseOffset++;
            }
            if (remainingBytes != 1 & remainingBytes != 4 & remainingBytes != 5) { // 2, 3, 6, 7
                hash = hash * hcConst + hashCodeAsciiSanitize(UNSAFE.getShort(bytes, baseOffset));
                hcConst = hcConst == HASH_CODE_C1 ? HASH_CODE_C2 : HASH_CODE_C1;
                baseOffset += 2;
            }
            if (remainingBytes >= 4) { // 4, 5, 6, 7
                return hash * hcConst + hashCodeAsciiSanitize(UNSAFE.getInt(bytes, baseOffset));
            }
            return hash;
        }

        static int hashCodeAsciiCompute(long value, int hash) {
            // masking with 0x1f reduces the number of overall bits that impact the hash code but makes the hash
            // code the same regardless of character case (upper case or lower case hash is the same).
            return hash * HASH_CODE_C1 +
                    // Low order int
                    hashCodeAsciiSanitize((int) value) * HASH_CODE_C2 +
                    // High order int
                    (int) ((value & 0x1f1f1f1f00000000L) >>> 32);
        }

        static int hashCodeAsciiSanitize(int value) {
            return value & 0x1f1f1f1f;
        }

        static int hashCodeAsciiSanitize(short value) {
            return value & 0x1f1f;
        }

        static int hashCodeAsciiSanitize(byte value) {
            return value & 0x1f;
        }

        static ClassLoader getClassLoader(final Class<?> clazz) {
            if (System.getSecurityManager() == null) {
                return clazz.getClassLoader();
            } else {
                return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        return clazz.getClassLoader();
                    }
                });
            }
        }

        static ClassLoader getContextClassLoader() {
            if (System.getSecurityManager() == null) {
                return Thread.currentThread().getContextClassLoader();
            } else {
                return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                });
            }
        }

        static ClassLoader getSystemClassLoader() {
            if (System.getSecurityManager() == null) {
                return ClassLoader.getSystemClassLoader();
            } else {
                return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        return ClassLoader.getSystemClassLoader();
                    }
                });
            }
        }

        static int addressSize() {
            return UNSAFE.addressSize();
        }

        static long allocateMemory(long size) {
            return UNSAFE.allocateMemory(size);
        }

        static void freeMemory(long address) {
            UNSAFE.freeMemory(address);
        }

        static long reallocateMemory(long address, long newSize) {
            return UNSAFE.reallocateMemory(address, newSize);
        }

        static boolean isAndroid() {
            return IS_ANDROID;
        }

        private static boolean isAndroid0() {
            // Idea: Sometimes java binaries include Android classes on the classpath, even if it isn't actually Android.
            // Rather than check if certain classes are present, just check the VM, which is tied to the JDK.

            // Optional improvement: check if `android.os.Build.VERSION` is >= 24. On later versions of Android, the
            // OpenJDK is used, which means `Unsafe` will actually work as expected.

            // Android sets this property to Dalvik, regardless of whether it actually is.
            String vmName = System.getProperty("java.vm.name");
            boolean isAndroid = "Dalvik".equals(vmName);
            if (isAndroid) {
                logger.debug("Platform: Android");
            }
            return isAndroid;
        }

        private static boolean explicitTryReflectionSetAccessible0() {
            // we disable reflective access
            return Boolean.parseBoolean(
                    System.getProperty("io.netty.tryReflectionSetAccessible",
                        String.valueOf(javaVersion() < 9 || RUNNING_IN_NATIVE_IMAGE)));
        }

        static boolean isExplicitTryReflectionSetAccessible() {
            return IS_EXPLICIT_TRY_REFLECTION_SET_ACCESSIBLE;
        }

        static int javaVersion() {
            return JAVA_VERSION;
        }

        private static int javaVersion0() {
            final int majorVersion;

            if (isAndroid()) {
                majorVersion = 6;
            } else {
                majorVersion = majorVersionFromJavaSpecificationVersion();
            }

            logger.debug("Java version: {}", majorVersion);

            return majorVersion;
        }

        // Package-private for testing only
        static int majorVersionFromJavaSpecificationVersion() {
            String javaVersion = System.getProperty("java.version");
            if (javaVersion == null) {
                javaVersion = System.getProperty("java.specification.version", "1.6");
            }
            return majorVersion(javaVersion);
        }

        // Package-private for testing only
        static int majorVersion(final String javaSpecVersion) {
            final String[] components = javaSpecVersion.split("\\.");
            final int[] version = new int[components.length];
            for (int i = 0; i < components.length && i < 2; i++) {
                version[i] = Integer.parseInt(components[i]);
            }

            if (version[0] == 1) {
                assert version[1] >= 6;
                return version[1];
            } else {
                return version[0];
            }
        }

        private PlatformDependent0() {
        }
    }

    private static final class ReflectionUtil {

        private ReflectionUtil() { }

        /**
         * Try to call {@link AccessibleObject#setAccessible(boolean)} but will catch any {@link SecurityException} and
         * {@link java.lang.reflect.InaccessibleObjectException} and return it.
         * The caller must check if it returns {@code null} and if not handle the returned exception.
         */
        public static Throwable trySetAccessible(AccessibleObject object, boolean checkAccessible) {
            if (checkAccessible && !PlatformDependent0.isExplicitTryReflectionSetAccessible()) {
                return new UnsupportedOperationException("Reflective setAccessible(true) disabled");
            }
            try {
                object.setAccessible(true);
                return null;
            } catch (SecurityException e) {
                return e;
            } catch (RuntimeException e) {
                return handleInaccessibleObjectException(e);
            }
        }

        private static RuntimeException handleInaccessibleObjectException(RuntimeException e) {
            // JDK 9 can throw an inaccessible object exception here; since Netty compiles
            // against JDK 7 and this exception was only added in JDK 9, we have to weakly
            // check the type
            if ("java.lang.reflect.InaccessibleObjectException".equals(e.getClass().getName())) {
                return e;
            }
            throw e;
        }

        private static Class<?> fail(Class<?> type, String typeParamName) {
            throw new IllegalStateException(
                    "cannot determine the type of the type parameter '" + typeParamName + "': " + type);
        }

        /**
         * Resolve a type parameter of a class that is a subclass of the given parametrized superclass.
         * @param object The object to resolve the type parameter for
         * @param parametrizedSuperclass The parametrized superclass
         * @param typeParamName The name of the type parameter to resolve
         * @return The resolved type parameter
         * @throws IllegalStateException if the type parameter could not be resolved
         * */
        public static Class<?> resolveTypeParameter(final Object object,
                                                    Class<?> parametrizedSuperclass,
                                                    String typeParamName) {
            final Class<?> thisClass = object.getClass();
            Class<?> currentClass = thisClass;
            for (;;) {
                if (currentClass.getSuperclass() == parametrizedSuperclass) {
                    int typeParamIndex = -1;
                    TypeVariable<?>[] typeParams = currentClass.getSuperclass().getTypeParameters();
                    for (int i = 0; i < typeParams.length; i ++) {
                        if (typeParamName.equals(typeParams[i].getName())) {
                            typeParamIndex = i;
                            break;
                        }
                    }

                    if (typeParamIndex < 0) {
                        throw new IllegalStateException(
                                "unknown type parameter '" + typeParamName + "': " + parametrizedSuperclass);
                    }

                    Type genericSuperType = currentClass.getGenericSuperclass();
                    if (!(genericSuperType instanceof ParameterizedType)) {
                        return Object.class;
                    }

                    Type[] actualTypeParams = ((ParameterizedType) genericSuperType).getActualTypeArguments();

                    Type actualTypeParam = actualTypeParams[typeParamIndex];
                    if (actualTypeParam instanceof ParameterizedType) {
                        actualTypeParam = ((ParameterizedType) actualTypeParam).getRawType();
                    }
                    if (actualTypeParam instanceof Class) {
                        return (Class<?>) actualTypeParam;
                    }
                    if (actualTypeParam instanceof GenericArrayType) {
                        Type componentType = ((GenericArrayType) actualTypeParam).getGenericComponentType();
                        if (componentType instanceof ParameterizedType) {
                            componentType = ((ParameterizedType) componentType).getRawType();
                        }
                        if (componentType instanceof Class) {
                            return Array.newInstance((Class<?>) componentType, 0).getClass();
                        }
                    }
                    if (actualTypeParam instanceof TypeVariable) {
                        // Resolved type parameter points to another type parameter.
                        TypeVariable<?> v = (TypeVariable<?>) actualTypeParam;
                        if (!(v.getGenericDeclaration() instanceof Class)) {
                            return Object.class;
                        }

                        currentClass = thisClass;
                        parametrizedSuperclass = (Class<?>) v.getGenericDeclaration();
                        typeParamName = v.getName();
                        if (parametrizedSuperclass.isAssignableFrom(thisClass)) {
                            continue;
                        }
                        return Object.class;
                    }

                    return fail(thisClass, typeParamName);
                }
                currentClass = currentClass.getSuperclass();
                if (currentClass == null) {
                    return fail(thisClass, typeParamName);
                }
            }
        }
    }
}
