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
package org.apache.dubbo.common.config.configcenter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The abstract implementation of {@link DynamicConfiguration}
 *
 * @since 2.7.5
 */
public abstract class AbstractDynamicConfiguration implements DynamicConfiguration {

    public static final String PARAM_NAME_PREFIX = "dubbo.config-center.";

    public static final String THREAD_POOL_PREFIX_PARAM_NAME = PARAM_NAME_PREFIX + "thread-pool.prefix";

    public static final String DEFAULT_THREAD_POOL_PREFIX = PARAM_NAME_PREFIX + "workers";

    public static final String THREAD_POOL_SIZE_PARAM_NAME = PARAM_NAME_PREFIX + "thread-pool.size";

    /**
     * The keep alive time in milliseconds for threads in {@link ThreadPoolExecutor}
     */
    public static final String THREAD_POOL_KEEP_ALIVE_TIME_PARAM_NAME = PARAM_NAME_PREFIX + "thread-pool.keep-alive-time";

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;

    /**
     * Default keep alive time in milliseconds for threads in {@link ThreadPoolExecutor} is 1 minute( 60 * 1000 ms)
     */
    public static final long DEFAULT_THREAD_POOL_KEEP_ALIVE_TIME = TimeUnit.MINUTES.toMillis(1);

    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The thread pool for workers who executes the tasks
     */
    private final ThreadPoolExecutor workersThreadPool;

    public AbstractDynamicConfiguration() {
        this(DEFAULT_THREAD_POOL_PREFIX, DEFAULT_THREAD_POOL_SIZE, DEFAULT_THREAD_POOL_KEEP_ALIVE_TIME);
    }

    public AbstractDynamicConfiguration(URL url) {
        this(getThreadPoolPrefixName(url), getThreadPoolSize(url), getThreadPoolKeepAliveTime(url));
    }

    public AbstractDynamicConfiguration(String threadPoolPrefixName,
                                        int threadPoolSize,
                                        long keepAliveTime) {
        this.workersThreadPool = initWorkersThreadPool(threadPoolPrefixName, threadPoolSize, keepAliveTime);
    }

    @Override
    public void addListener(String key, String group, ConfigurationListener listener) {

    }

    @Override
    public void removeListener(String key, String group, ConfigurationListener listener) {

    }

    @Override
    public final String getConfig(String key, String group, long timeout) throws IllegalStateException {
        return execute(() -> doGetConfig(key, group), timeout);
    }

    @Override
    public Object getInternalProperty(String key) {
        return null;
    }

    @Override
    public final void close() throws Exception {
        try {
            doClose();
        } finally {
            doFinally();
        }
    }

    /**
     * Get the content of configuration in the specified key and group
     *
     * @param key   the key
     * @param group the group
     * @return if found, return the content of configuration
     * @throws Exception If met with some problems
     */
    protected abstract String doGetConfig(String key, String group) throws Exception;

    /**
     * Close the resources if necessary
     *
     * @throws Exception If met with some problems
     */
    protected abstract void doClose() throws Exception;

    /**
     * Executes the {@link Runnable} with the specified timeout
     *
     * @param task    the {@link Runnable task}
     * @param timeout timeout in milliseconds
     */
    protected final void execute(Runnable task, long timeout) {
        execute(() -> {
            task.run();
            return null;
        }, timeout);
    }

    /**
     * Executes the {@link Callable} with the specified timeout
     *
     * @param task    the {@link Callable task}
     * @param timeout timeout in milliseconds
     * @param <V>     the type of computing result
     * @return the computing result
     */
    protected final <V> V execute(Callable<V> task, long timeout) {
        V value = null;
        try {

            if (timeout < 1) { // less or equal 0
                value = task.call();
            } else {
                Future<V> future = workersThreadPool.submit(task);
                value = future.get(timeout, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
        return value;
    }

    protected ThreadPoolExecutor getWorkersThreadPool() {
        return workersThreadPool;
    }

    private void doFinally() {
        shutdownWorkersThreadPool();
    }

    private void shutdownWorkersThreadPool() {
        if (!workersThreadPool.isShutdown()) {
            workersThreadPool.shutdown();
        }
    }

    protected ThreadPoolExecutor initWorkersThreadPool(String threadPoolPrefixName,
                                                       int threadPoolSize,
                                                       long keepAliveTime) {
        return new ThreadPoolExecutor(threadPoolSize, threadPoolSize, keepAliveTime,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory(threadPoolPrefixName));
    }

    protected static String getThreadPoolPrefixName(URL url) {
        return getParameter(url, THREAD_POOL_PREFIX_PARAM_NAME, DEFAULT_THREAD_POOL_PREFIX);
    }

    protected static int getThreadPoolSize(URL url) {
        return getParameter(url, THREAD_POOL_SIZE_PARAM_NAME, DEFAULT_THREAD_POOL_SIZE);
    }

    protected static long getThreadPoolKeepAliveTime(URL url) {
        return getParameter(url, THREAD_POOL_KEEP_ALIVE_TIME_PARAM_NAME, DEFAULT_THREAD_POOL_KEEP_ALIVE_TIME);
    }

    protected static String getParameter(URL url, String name, String defaultValue) {
        if (url != null) {
            return url.getParameter(name, defaultValue);
        }
        return defaultValue;
    }

    protected static int getParameter(URL url, String name, int defaultValue) {
        if (url != null) {
            return url.getParameter(name, defaultValue);
        }
        return defaultValue;
    }

    protected static long getParameter(URL url, String name, long defaultValue) {
        if (url != null) {
            return url.getParameter(name, defaultValue);
        }
        return defaultValue;
    }
}
