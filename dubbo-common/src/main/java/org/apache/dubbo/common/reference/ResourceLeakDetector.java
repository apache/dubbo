/*
 * Copyright 2013 The Netty Project
 * Copyright 2018 Aleksandr Dubinsky
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.apache.dubbo.common.reference;

import java.lang.ref.ReferenceQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Detects leaks of instances of classes {@link ReferenceCountedObject} and {@link CloseableObject}.
 * Set the {@link Level leak detection level} using the Java system property {@code -DleakDetection.level}.
 * <p>For unit testing, you can use {@link #setInheritableThreadLocalInstance(ResourceLeakDetector)}.
 * Be careful of conflicts such as one test disabling leak detection for the thread
 * and forgetting to restore it.
 */
public final class ResourceLeakDetector {

    static final String PROP_LEVEL = "leakDetection.level";
    static final String PROP_SAMPLING_INTERVAL = "leakDetection.samplingInterval";
    static final String PROP_TRACE_COUNT = "leakDetection.traceCount";

    static final Level DEFAULT_LEVEL = Level.FULL;
    static final int DEFAULT_LIGHT_SAMPLING_INTERVAL = 128;
    static final int DEFAULT_TRACE_COUNT = 4;

    /**
     * The default resource leak detector used by all threads unless
     * {@link #setInheritableThreadLocalInstance(ResourceLeakDetector) overriden}.
     */
    public static final ResourceLeakDetector DEFAULT_INSTANCE = newResourceLeakDetector();
    private static final InheritableThreadLocal<ResourceLeakDetector> LOCAL_INSTANCE
        = new InheritableThreadLocal<>();

    /**
     * A disabled resource leak detector.
     * For use with {@link #setInheritableThreadLocalInstance(ResourceLeakDetector)}.
     */
    public static final ResourceLeakDetector DISABLED_INSTANCE
        = newResourceLeakDetector(0, 0);


    /**
     * Represents the level of resource leak detection.
     * Set the leak detection level using the Java system property {@code -DleakDetection.level}.
     * Valid values are {@link #DISABLED DISABLED}, {@link #LIGHT LIGHT}, {@link #FULL FULL}, and {@link #DEBUG DEBUG}.
     * The default level is {@code FULL}.
     */
    public static enum Level {
        /**
         * Disables resource leak detection.
         */
        DISABLED,
        /**
         * Enables sampling leak detection.
         * Every Nth allocated resource is registered with the leak detector.
         * This level has little impact on performance even in applications that allocate many objects.
         * You can control the sampling frequency with {@code -DleakDetection.samplingInterval}.
         * The default value is {@code 128}.
         */
        LIGHT,
        /**
         * Enables leak detection for all objects.
         * Every allocated resource is registered with the leak detector.
         */
        FULL,
        /**
         * Enables leak detection with tracing of allocation and use.
         * Every allocated resource is registered with the leak detector.
         * Additionally, the stack trace of its allocation is recorded.
         * You may call {@link ReferenceCountedObject#trace() trace} while using using the object
         * in order to collect additional stack traces.
         * To minimize memory usage, not every stack trace is stored.
         * You can control the number of stack traces that are stored in memory with {@code -DleakDetection.traceCount}.
         * The default value is {@code 4}.
         * The value {@code 1} will only record the allocation stack trace.
         * A value of {@code 2} or greater will record both the stack trace of allocation and of the last call to {@code trace}.
         * A value of {@code 2} or greater causes a possibly significant performance impact,
         * because every call to {@code trace} will allocate a {@code Throwable}.
         * A value of {@code 2} or greater is not a hard limit on the number of stored stack traces.
         * Instead, additional stack traces will be randomly chosen to be stored according to a back-off strategy,
         * in order to aid debugging.
         * Additionally, you can enable sampling with {@code -DleakDetection.samplingInterval}.
         * The default value is {@code 1} (ie, every object is tracked).
         */
        DEBUG;

        static Level valueOfEx(String levelStr) {
            levelStr = levelStr.trim();
            for (Level l : values())
                if (levelStr.equalsIgnoreCase(l.name())
                    || levelStr.equals(Integer.toString(l.ordinal())))
                    return l;

            throw new IllegalArgumentException("Invalid " + PROP_LEVEL + "=" + levelStr
                + ". Acceptable values are DISABLED, LIGHT, FULL, DEBUG, or number 0-3.");
        }
    }

    /**
     * Sets the ResourceLeakDetector used by this and all child threads.
     *
     * @param instance an instance created with {@link #newResourceLeakDetector(int, int)},
     *                 {@link #DISABLED_INSTANCE}, or {@link #DEFAULT_INSTANCE}.
     */
    public static void setInheritableThreadLocalInstance(ResourceLeakDetector instance) {
        LOCAL_INSTANCE.set(instance);
    }

    static ResourceLeakDetector getInstance() {
        ResourceLeakDetector localInstance = LOCAL_INSTANCE.get();
        if (localInstance != null)
            return localInstance;
        else
            return DEFAULT_INSTANCE;
    }

    private static ResourceLeakDetector
    newResourceLeakDetector() {

        String levelStr = System.getProperty(PROP_LEVEL, DEFAULT_LEVEL.name());
        Level level = Level.valueOfEx(levelStr);

        int samplingInterval, traceCount;
        switch (level) {
            case DISABLED:
                samplingInterval = -1;
                traceCount = -1;
                break;

            case LIGHT:
                samplingInterval = Integer.getInteger(PROP_SAMPLING_INTERVAL, DEFAULT_LIGHT_SAMPLING_INTERVAL);
                traceCount = 0;
                break;

            case FULL:
                samplingInterval = 1;
                traceCount = 0;
                break;

            case DEBUG:
                samplingInterval = Integer.getInteger(PROP_SAMPLING_INTERVAL, 1);
                traceCount = Integer.getInteger(PROP_TRACE_COUNT, DEFAULT_TRACE_COUNT);
                break;

            default:
                throw new AssertionError();
        }

        if (log.isDebugEnabled()) {
            log.debug("-D{}: {}", PROP_LEVEL, level.name().toLowerCase());
            log.debug("-D{}: {}", PROP_TRACE_COUNT, traceCount);
        }

        return new ResourceLeakDetector(level, samplingInterval, traceCount);
    }

    /**
     * Creates a new ResourceLeakDetector for use with
     * {@link #setInheritableThreadLocalInstance }.
     * If {@code samplingInterval} is 0, leak detection is disabled.
     * If {@code traceCount} is 1 or greater, leak detection is performed
     * as in {@link Level#DEBUG DEBUG} mode. Otherwise, leak detection is performed
     * as in {@link Level#FULL FULL} or {@link Level#LIGHT LIGHT} modes.
     *
     * @param samplingInterval Every Nth allocated resource is registered with the leak detector.
     *                         Set to 1 to register every resource. Set to 0 to disable leak detection.
     * @param traceCount       Number of stack traces to record.
     *                         Set to 0 to disable stack trace recording (for best performance).
     *                         Set to 1 to record the allocation stack trace.
     *                         Set to 2 or greater and call {@code trace} to record additional stack trace
     *                         when the object is used.
     * @return a new {@code ResourceLeakDetector} instance
     * @see Level#DEBUG
     * @see ReferenceCountedObject#trace()
     */
    public static ResourceLeakDetector
    newResourceLeakDetector(int samplingInterval, int traceCount) {

        Level level;
        if (samplingInterval <= 0)
            level = Level.DISABLED;
        else if (traceCount > 0)
            level = Level.DEBUG;
        else
            level = Level.FULL;

        return new ResourceLeakDetector(level, samplingInterval, traceCount);
    }

    private ResourceLeakDetector(Level level, int samplingInterval, int traceCount) {

        this.level = level;
        this.samplingInterval = samplingInterval;
        this.traceCount = traceCount;

        suppressedStackTraceEntries.add("net.almson.object.ResourceReference.<init>");
        suppressedStackTraceEntries.add("net.almson.object.ResourceReference.trace");
        suppressedStackTraceEntries.add("net.almson.object.ResourceLeakDetector.tryRegister");
        suppressedStackTraceEntries.add("net.almson.object.ReferenceCountedObject.<init>");
        suppressedStackTraceEntries.add("net.almson.object.ReferenceCountedObject.trace");
        suppressedStackTraceEntries.add("net.almson.object.CloseableObject.<init>");
        suppressedStackTraceEntries.add("net.almson.object.CloseableObject.trace");
    }

    private final Level level;
    private final int samplingInterval;
    private final int traceCount;
    private final Set<String> suppressedStackTraceEntries = ConcurrentHashMap.newKeySet();

    private final ResourceReference refListHead = new ResourceReference();
    private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
    private final Set<String> loggedLeaks = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new {@link ResourceReference} which is expected to be
     * {@link ResourceReference#unregister() unregistered} when the referenced resource is released.
     *
     * @return the {@link ResourceLeak} or {@code null}
     */
    ResourceReference
    tryRegister(Object obj) {

        if (level == Level.DISABLED)
            return null;

        if (samplingInterval == 1 || ThreadLocalRandom.current().nextInt(samplingInterval) == 0) {
            pollAndLogLeaks();
            return new ResourceReference(obj, referenceQueue, refListHead, traceCount, suppressedStackTraceEntries);
        } else
            return null;
    }

    /**
     * Check if any tracked objects have been collected by the GC without having been destroyed.
     * Any detected leaks will be logged.
     */
    public void
    pollAndLogLeaks() {

        for (ResourceReference ref = (ResourceReference) referenceQueue.poll()
             ; ref != null
            ; ref = (ResourceReference) referenceQueue.poll()) {
            ref.unregister();
            logLeak(ref);
        }
    }

    /**
     * Throws AssertionError if there's been any leaks.
     * The current thread should have already been synchronized with any other threads that were responsible for
     * releasing tracked objects. Any tracked object which has not been destroyed (whether or not it is reachable
     * or has been collected by the GC) will be considered a leak.
     */
    public void
    assertAllResourcesDestroyed() {

        // asserting this is undersirable if this method is used in production code
//            assert level.ordinal() >= Level.FULL.ordinal() : "Enable a leak detection level of FULL or DEBUG";

        // All unlogged leaks, whether they've already been queued by the GC or not, will be here.
        synchronized (refListHead) {
            for (ResourceReference ref : refListHead) {
                ref.unregister();
                logLeak(ref);
            }
        }

        if (!loggedLeaks.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String loggedLeak : loggedLeaks)
                sb.append(loggedLeak).append("\n");
            loggedLeaks.clear();
            throw new AssertionError(sb);
        }
    }

    // Instead of having this as a protected method expecting the whole class to be overriden,
    // we should have a consumer lambda, and allow setting a different lambda.
    // and make everything else static
    private void
    logLeak(ResourceReference ref) {

        String leakWarning = getLeakWarning(ref);
        if (loggedLeaks.add(leakWarning)) // Only log unique leak warnings
            log.error(leakWarning);
    }

    private String
    getLeakWarning(ResourceReference ref) {

        if (level == Level.DEBUG) {
            String traces = ref.getTracesString();
            return "RESOURCE LEAK DETECTED: Object of type " + ref.getReferentClassName() + " was not destroyed prior to becoming unreachable and garbage collected."
                + traces
                + (traceCount == 0 ? System.lineSeparator() + "\tStack traces are not being stored. To store allocation stack traces specify the JVM option -D" + PROP_TRACE_COUNT + "=1 or greater." : "")
                + (traceCount == 1 ? System.lineSeparator() + "\tOnly the allocation stack trace was stored. To store additional stack traces specify the JVM option -D" + PROP_TRACE_COUNT + "=2 or greater." : "")
                + (traceCount >= 2 ? System.lineSeparator() + "\tTo trace the lifetime of the object more thoroughly, make more frequent calls to trace()." : "");
        } else
            return "RESOURCE LEAK DETECTED: Object of type " + ref.getReferentClassName() + " was not destroyed prior to becoming unreachable and garbage collected. "
                + "The log level is " + level + ", which does not record stack traces. "
//                        + "To enable debugging, specify the JVM option '-D"+PROP_LEVEL+"="+Level.DEBUG.name().toLowerCase()+"' or call ResourceLeakDetector.setLevel().";
                + "To enable debugging, specify the JVM option -D" + PROP_LEVEL + "=" + Level.DEBUG;
    }
}
