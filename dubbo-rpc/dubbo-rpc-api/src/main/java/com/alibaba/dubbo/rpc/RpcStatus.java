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
package com.alibaba.dubbo.rpc;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * URL statistics. (API, Cached, ThreadSafe)
 *
 * RPC 状态。可以计入如下维度统计：
 *
 * 1. 基于服务 URL ，{@link #SERVICE_STATISTICS}
 * 2. 基于服务 URL + 方法，{@link #METHOD_STATISTICS}
 *
 * @see com.alibaba.dubbo.rpc.filter.ActiveLimitFilter
 * @see com.alibaba.dubbo.rpc.filter.ExecuteLimitFilter
 * @see .com.alibaba.dubbo.rpc.cluster.loadbalance.LeastActiveLoadBalance
 */
public class RpcStatus {

    /**
     * 基于服务 URL 为维度的 RpcStatus 集合
     *
     * key：URL
     */
    private static final ConcurrentMap<String, RpcStatus> SERVICE_STATISTICS = new ConcurrentHashMap<String, RpcStatus>();
    /**
     * 基于服务 URL + 方法维度的 RpcStatus 集合
     *
     * key1：URL
     * key2：方法名
     */
    private static final ConcurrentMap<String, ConcurrentMap<String, RpcStatus>> METHOD_STATISTICS = new ConcurrentHashMap<String, ConcurrentMap<String, RpcStatus>>();

    // 目前没有用到
    private final ConcurrentMap<String, Object> values = new ConcurrentHashMap<String, Object>();
    /**
     * 调用中的次数
     */
    private final AtomicInteger active = new AtomicInteger();
    /**
     * 总调用次数
     */
    private final AtomicLong total = new AtomicLong();
    /**
     * 总调用失败次数
     */
    private final AtomicInteger failed = new AtomicInteger();
    /**
     * 总调用时长，单位：毫秒
     */
    private final AtomicLong totalElapsed = new AtomicLong();
    /**
     * 总调用失败时长，单位：毫秒
     */
    private final AtomicLong failedElapsed = new AtomicLong();
    /**
     * 最大调用时长，单位：毫秒
     */
    private final AtomicLong maxElapsed = new AtomicLong();
    /**
     * 最大调用失败时长，单位：毫秒
     */
    private final AtomicLong failedMaxElapsed = new AtomicLong();
    /**
     * 最大调用成功时长，单位：毫秒
     */
    private final AtomicLong succeededMaxElapsed = new AtomicLong();

    /**
     * Semaphore used to control concurrency limit set by `executes`
     *
     * 服务执行信号量，在 {@link com.alibaba.dubbo.rpc.filter.ExecuteLimitFilter} 中使用
     */
    private volatile Semaphore executesLimit;
    /**
     * 服务执行信号量大小
     */
    private volatile int executesPermits;

    private RpcStatus() {
    }

    /**
     * 获得 RpcStatus 对象
     *
     * @param url URL
     * @return status
     */
    public static RpcStatus getStatus(URL url) {
        String uri = url.toIdentityString();
        // 获得
        RpcStatus status = SERVICE_STATISTICS.get(uri);
        // 不存在，则进行创建
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
     * 获得 RpcStatus 对象
     *
     * @param url URL
     * @param methodName 方法
     * @return status
     */
    public static RpcStatus getStatus(URL url, String methodName) {
        String uri = url.toIdentityString();
        // 获得方法集合
        ConcurrentMap<String, RpcStatus> map = METHOD_STATISTICS.get(uri);
        // 不存在，创建方法集合
        if (map == null) {
            METHOD_STATISTICS.putIfAbsent(uri, new ConcurrentHashMap<String, RpcStatus>());
            map = METHOD_STATISTICS.get(uri);
        }

        // 获得 RpcStatus 对象
        RpcStatus status = map.get(methodName);
        // 不存在，创建 RpcStatus 对象
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
     * 服务调用开始的计数
     *
     * @param url URL 对象
     * @param methodName 方法名
     */
    public static void beginCount(URL url, String methodName) {
        // `SERVICE_STATISTICS` 的计数
        beginCount(getStatus(url));
        // `METHOD_STATISTICS` 的计数
        beginCount(getStatus(url, methodName));
    }

    private static void beginCount(RpcStatus status) {
        // 调用中的次数
        status.active.incrementAndGet();
    }

    /**
     * 服务调用结束的计数
     *
     * @param url URL 对象
     * @param elapsed 时长，毫秒
     * @param succeeded 是否成功
     */
    public static void endCount(URL url, String methodName, long elapsed, boolean succeeded) {
        // `SERVICE_STATISTICS` 的计数
        endCount(getStatus(url), elapsed, succeeded);
        // `METHOD_STATISTICS` 的计数
        endCount(getStatus(url, methodName), elapsed, succeeded);
    }

    private static void endCount(RpcStatus status, long elapsed, boolean succeeded) {
        // 次数计数
        status.active.decrementAndGet();
        status.total.incrementAndGet();
        status.totalElapsed.addAndGet(elapsed);
        // 时长计数
        if (status.maxElapsed.get() < elapsed) {
            status.maxElapsed.set(elapsed);
        }
        if (succeeded) {
            if (status.succeededMaxElapsed.get() < elapsed) {
                status.succeededMaxElapsed.set(elapsed);
            }
        } else {
            status.failed.incrementAndGet(); // 失败次数
            status.failedElapsed.addAndGet(elapsed);
            if (status.failedMaxElapsed.get() < elapsed) {
                status.failedMaxElapsed.set(elapsed);
            }
        }
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
        return active.get();
    }

    /**
     * get total.
     *
     * @return total
     */
    public long getTotal() {
        return total.longValue();
    }

    /**
     * get total elapsed.
     *
     * @return total elapsed
     */
    public long getTotalElapsed() {
        return totalElapsed.get();
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
        return maxElapsed.get();
    }

    /**
     * get failed.
     *
     * @return failed
     */
    public int getFailed() {
        return failed.get();
    }

    /**
     * get failed elapsed.
     *
     * @return failed elapsed
     */
    public long getFailedElapsed() {
        return failedElapsed.get();
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
        return failedMaxElapsed.get();
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
        return succeededMaxElapsed.get();
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

    /**
     * Get the semaphore for thread number. Semaphore's permits is decided by {@link Constants#EXECUTES_KEY}
     *
     * @param maxThreadNum value of {@link Constants#EXECUTES_KEY}
     * @return thread number semaphore
     */
    public Semaphore getSemaphore(int maxThreadNum) {
        if(maxThreadNum <= 0) {
            return null;
        }
        // 若信号量不存在，或者信号量大小改变，创建新的信号量
        if (executesLimit == null || executesPermits != maxThreadNum) {
            synchronized (this) {
                if (executesLimit == null || executesPermits != maxThreadNum) {
                    executesLimit = new Semaphore(maxThreadNum);
                    executesPermits = maxThreadNum;
                }
            }
        }
        // 返回信号量
        return executesLimit;
    }

}