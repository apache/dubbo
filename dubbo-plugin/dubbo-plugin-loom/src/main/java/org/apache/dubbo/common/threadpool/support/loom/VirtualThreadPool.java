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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.ThreadPool;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_THREAD_NAME;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_VIRTUAL_CORE;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;

/**
 * Creates a thread pool that uses virtual threads (Project Loom).
 *
 * <p>Two operating modes are supported:
 *
 * <ul>
 *   <li><b>Unpooled mode (default)</b>: When {@code threads.virtual.core} is not set (or {@code
 *       <= 0}), a new virtual thread is created for every submitted task via {@link
 *       Executors#newThreadPerTaskExecutor(java.util.concurrent.ThreadFactory)}. This is the
 *       simplest and most scalable mode.
 *
 *   <li><b>Pooled mode</b>: When {@code threads.virtual.core} is set to a positive value, a
 *       {@link ThreadPoolExecutor} backed by virtual threads is used with {@code corePoolSize =
 *       threads.virtual.core} and an unbounded maximum. This mode is beneficial when downstream
 *       libraries (e.g. FastJSON, Aerospike Java client) store large byte-buffers in
 *       {@link ThreadLocal} caches: by keeping a pool of warm virtual threads alive, those cached
 *       buffers can be reused across consecutive tasks instead of being re-allocated on every
 *       call.
 *
 *       <p><b>Note on keepAliveTime</b>: non-core threads are kept alive for
 *       {@value #KEEP_ALIVE_SECONDS} seconds after becoming idle. This window must be long enough
 *       for the {@link ThreadLocal} reuse benefit to materialise. A value of {@code 0} would cause
 *       threads to terminate immediately and eliminate any reuse benefit.
 * </ul>
 *
 * @see Executors#newVirtualThreadPerTaskExecutor()
 */
public class VirtualThreadPool implements ThreadPool {

    /**
     * Number of seconds that excess (non-core) virtual threads are kept alive while idle.
     * Must be greater than zero to allow {@link ThreadLocal} state to be reused across tasks.
     */
    static final long KEEP_ALIVE_SECONDS = 60L;

    @Override
    public Executor getExecutor(URL url) {
        String name =
                url.getParameter(THREAD_NAME_KEY, (String) url.getAttribute(THREAD_NAME_KEY, DEFAULT_THREAD_NAME));
        int threads = url.getParameter(THREADS_VIRTUAL_CORE, 0);
        if (threads > 0) {
            /*
             * Pooled virtual-thread executor.
             *
             * corePoolSize   = threads   (warm pool of reusable virtual threads)
             * maximumPoolSize = MAX_VALUE  (auto-expand under burst load, just like unpooled mode)
             * keepAliveTime  = 60s        (non-core threads stay alive long enough to reuse
             *                              ThreadLocal-cached buffers before being reclaimed)
             * workQueue      = SynchronousQueue (no internal buffering; tasks are handed off
             *                              directly to a thread, matching the behaviour of
             *                              newThreadPerTaskExecutor under light load)
             */
            return new ThreadPoolExecutor(
                    threads,
                    Integer.MAX_VALUE,
                    KEEP_ALIVE_SECONDS,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    Thread.ofVirtual().name(name, 1).factory());
        } else {
            return Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual().name(name, 1).factory());
        }
    }
}
