package org.apache.dubbo.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Deprecated method invocation counter, which is used by annotation processor.
 * <p>
 * If an IDE says it is unused, just ignore it.
 */
public final class DeprecatedMethodInvocationCounter {
    private DeprecatedMethodInvocationCounter() {
        throw new UnsupportedOperationException("No instance of DeprecatedMethodInvocationCounter for you! ");
    }

    private static final ConcurrentHashMap<String, LongAdder> COUNTERS = new ConcurrentHashMap<>();

    public static void incrementInvocationCount(String methodDefinition) {
        COUNTERS.putIfAbsent(methodDefinition, new LongAdder());
        LongAdder adder = COUNTERS.get(methodDefinition);

        adder.increment();
    }

    public static boolean hasThisMethodInvoked(String methodDefinition) {
        return COUNTERS.containsKey(methodDefinition);
    }

    public static Map<String, Integer> getInvocationRecord() {
        // Perform a deep-copy to avoid concurrent issues.
        HashMap<String, Integer> copyOfCounters = new HashMap<>();

        for (Map.Entry<String, LongAdder> entry : COUNTERS.entrySet()) {
            copyOfCounters.put(entry.getKey(), entry.getValue().intValue());
        }

        return Collections.unmodifiableMap(copyOfCounters);
    }
}
