/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc;

import com.alibaba.dubbo.common.URL;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * URL statistics. (API, Cached, ThreadSafe)
 *
 * @author william.liangf
 * @see com.alibaba.dubbo.rpc.filter.ActiveLimitFilter
 * @see com.alibaba.dubbo.rpc.filter.ExecuteLimitFilter
 * // * @see com.alibaba.dubbo.rpc.cluster.loadbalance.LeastActiveLoadBalance
 */
public class RpcStatus {

    private static final ConcurrentMap<String, RpcStatus> SERVICE_STATISTICS = new ConcurrentHashMap<String, RpcStatus>();

    private static final ConcurrentMap<String, ConcurrentMap<String, RpcStatus>> METHOD_STATISTICS = new ConcurrentHashMap<String, ConcurrentMap<String, RpcStatus>>();

    /**
     * @param url
     * @return status
     */
    public static RpcStatus getStatus(URL url) {
        String uri = url.toIdentityString();
        RpcStatus status = SERVICE_STATISTICS.get(uri);
        if (status == null) {
            SERVICE_STATISTICS.putIfAbsent(uri, new RpcStatus());
            status = SERVICE_STATISTICS.get(uri);
        }
        return status;
    }

    /**
     * @param url
     */
    public static void removeStatus(URL url) {
        String uri = url.toIdentityString();
        SERVICE_STATISTICS.remove(uri);
    }

    /**
     * @param url
     * @param methodName
     * @return status
     */
    public static RpcStatus getStatus(URL url, String methodName) {
        String uri = url.toIdentityString();
        ConcurrentMap<String, RpcStatus> map = METHOD_STATISTICS.get(uri);
        if (map == null) {
            METHOD_STATISTICS.putIfAbsent(uri, new ConcurrentHashMap<String, RpcStatus>());
            map = METHOD_STATISTICS.get(uri);
        }
        RpcStatus status = map.get(methodName);
        if (status == null) {
            map.putIfAbsent(methodName, new RpcStatus());
            status = map.get(methodName);
        }
        return status;
    }

    /**
     * @param url
     */
    public static void removeStatus(URL url, String methodName) {
        String uri = url.toIdentityString();
        ConcurrentMap<String, RpcStatus> map = METHOD_STATISTICS.get(uri);
        if (map != null) {
            map.remove(methodName);
        }
    }

    /**
     * @param url
     */
    public static void beginCount(URL url, String methodName) {
        beginCount(getStatus(url));
        beginCount(getStatus(url, methodName));
    }

    private static void beginCount(RpcStatus status) {
        activeUpdater.incrementAndGet(status);
    }

    /**
     * @param url
     * @param elapsed
     * @param succeeded
     */
    public static void endCount(URL url, String methodName, long elapsed, boolean succeeded) {
        endCount(getStatus(url), elapsed, succeeded);
        endCount(getStatus(url, methodName), elapsed, succeeded);
    }

    private static void endCount(RpcStatus status, long elapsed, boolean succeeded) {
        activeUpdater.decrementAndGet(status);
        totalUpdater.incrementAndGet(status);
        totalElapsedUpdater.addAndGet(status, elapsed);
        if (status.getMaxElapsed() < elapsed) {
            maxElapsedUpdater.set(status, elapsed);
        }
        if (succeeded) {
            if (status.getSucceededElapsed() < elapsed) {
                succeededMaxElapsedUpdater.set(status, elapsed);
            }
        } else {
            failedUpdater.incrementAndGet(status);
            failedElapsedUpdater.addAndGet(status, elapsed);
            if (status.getFailedMaxElapsed() < elapsed) {
                failedMaxElapsedUpdater.set(status, elapsed);
            }
        }
    }

    private final ConcurrentMap<String, Object> values = new ConcurrentHashMap<String, Object>();

    @SuppressWarnings({"unused", "FieldMayBeFinal", "RedundantFieldInitialization"})
    private volatile int active = 0;
    private static final AtomicIntegerFieldUpdater<RpcStatus> activeUpdater =
            AtomicIntegerFieldUpdater.newUpdater(RpcStatus.class, "active");

    @SuppressWarnings({"unused", "FieldMayBeFinal", "RedundantFieldInitialization"})
    private volatile long total = 0;
    private static final AtomicLongFieldUpdater<RpcStatus> totalUpdater =
            AtomicLongFieldUpdater.newUpdater(RpcStatus.class, "total");

    @SuppressWarnings({"unused", "FieldMayBeFinal", "RedundantFieldInitialization"})
    private volatile int failed = 0;
    private static final AtomicIntegerFieldUpdater<RpcStatus> failedUpdater =
            AtomicIntegerFieldUpdater.newUpdater(RpcStatus.class, "failed");

    @SuppressWarnings({"unused", "FieldMayBeFinal", "RedundantFieldInitialization"})
    private volatile long totalElapsed = 0;
    private static final AtomicLongFieldUpdater<RpcStatus> totalElapsedUpdater =
            AtomicLongFieldUpdater.newUpdater(RpcStatus.class, "totalElapsed");

    @SuppressWarnings({"unused", "FieldMayBeFinal", "RedundantFieldInitialization"})
    private volatile long failedElapsed = 0;
    private static final AtomicLongFieldUpdater<RpcStatus> failedElapsedUpdater =
            AtomicLongFieldUpdater.newUpdater(RpcStatus.class, "failedElapsed");

    @SuppressWarnings({"unused", "FieldMayBeFinal", "RedundantFieldInitialization"})
    private volatile long maxElapsed = 0;
    private static final AtomicLongFieldUpdater<RpcStatus> maxElapsedUpdater =
            AtomicLongFieldUpdater.newUpdater(RpcStatus.class, "maxElapsed");

    @SuppressWarnings({"unused", "FieldMayBeFinal", "RedundantFieldInitialization"})
    private volatile long failedMaxElapsed = 0;
    private static final AtomicLongFieldUpdater<RpcStatus> failedMaxElapsedUpdater =
            AtomicLongFieldUpdater.newUpdater(RpcStatus.class, "failedMaxElapsed");

    @SuppressWarnings({"unused", "FieldMayBeFinal", "RedundantFieldInitialization"})
    private volatile long succeededMaxElapsed = 0;
    private static final AtomicLongFieldUpdater<RpcStatus> succeededMaxElapsedUpdater =
            AtomicLongFieldUpdater.newUpdater(RpcStatus.class, "succeededMaxElapsed");


    private RpcStatus() {
    }

    /**
     * set value.
     *
     * @param key
     * @param value
     */
    public void set(String key, Object value) {
        values.put(key, value);
    }

    /**
     * get value.
     *
     * @param key
     * @return value
     */
    public Object get(String key) {
        return values.get(key);
    }

    /**
     * get active.
     *
     * @return active
     */
    public int getActive() {
        return active;
    }

    /**
     * get total.
     *
     * @return total
     */
    public long getTotal() {
        return total;
    }

    /**
     * get total elapsed.
     *
     * @return total elapsed
     */
    public long getTotalElapsed() {
        return totalElapsed;
    }

    /**
     * get average elapsed.
     *
     * @return average elapsed
     */
    public long getAverageElapsed() {
        long total = getTotal();
        if (total == 0) {
            return 0;
        }
        return getTotalElapsed() / total;
    }

    /**
     * get max elapsed.
     *
     * @return max elapsed
     */
    public long getMaxElapsed() {
        return maxElapsed;
    }

    /**
     * get failed.
     *
     * @return failed
     */
    public int getFailed() {
        return failed;
    }

    /**
     * get failed elapsed.
     *
     * @return failed elapsed
     */
    public long getFailedElapsed() {
        return failedElapsed;
    }

    /**
     * get failed average elapsed.
     *
     * @return failed average elapsed
     */
    public long getFailedAverageElapsed() {
        long failed = getFailed();
        if (failed == 0) {
            return 0;
        }
        return getFailedElapsed() / failed;
    }

    /**
     * get failed max elapsed.
     *
     * @return failed max elapsed
     */
    public long getFailedMaxElapsed() {
        return failedMaxElapsed;
    }

    /**
     * get succeeded.
     *
     * @return succeeded
     */
    public long getSucceeded() {
        return getTotal() - getFailed();
    }

    /**
     * get succeeded elapsed.
     *
     * @return succeeded elapsed
     */
    public long getSucceededElapsed() {
        return getTotalElapsed() - getFailedElapsed();
    }

    /**
     * get succeeded average elapsed.
     *
     * @return succeeded average elapsed
     */
    public long getSucceededAverageElapsed() {
        long succeeded = getSucceeded();
        if (succeeded == 0) {
            return 0;
        }
        return getSucceededElapsed() / succeeded;
    }

    /**
     * get succeeded max elapsed.
     *
     * @return succeeded max elapsed.
     */
    public long getSucceededMaxElapsed() {
        return succeededMaxElapsed;
    }

    /**
     * Calculate average TPS (Transaction per second).
     *
     * @return tps
     */
    public long getAverageTps() {
        if (getTotalElapsed() >= 1000L) {
            return getTotal() / (getTotalElapsed() / 1000L);
        }
        return getTotal();
    }

}