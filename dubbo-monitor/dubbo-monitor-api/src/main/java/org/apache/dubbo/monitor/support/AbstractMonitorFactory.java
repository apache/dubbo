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
package org.apache.dubbo.monitor.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.monitor.Monitor;
import org.apache.dubbo.monitor.MonitorFactory;
import org.apache.dubbo.monitor.MonitorService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_MONITOR_EXCEPTION;

/**
 * AbstractMonitorFactory. (SPI, Singleton, ThreadSafe)
 */
public abstract class AbstractMonitorFactory implements MonitorFactory {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AbstractMonitorFactory.class);

    /**
     * The lock for getting monitor center
     */
    private static final ReentrantLock LOCK = new ReentrantLock();

    /**
     * The monitor centers Map<RegistryAddress, Registry>
     */
    private static final Map<String, Monitor> MONITORS = new ConcurrentHashMap<String, Monitor>();

    private static final Map<String, Future<Monitor>> FUTURES = new ConcurrentHashMap<String, Future<Monitor>>();

    /**
     * The monitor create executor
     */
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(0, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new NamedThreadFactory("DubboMonitorCreator", true));

    public static Collection<Monitor> getMonitors() {
        return Collections.unmodifiableCollection(MONITORS.values());
    }

    @Override
    public Monitor getMonitor(URL url) {
        url = url.setPath(MonitorService.class.getName()).addParameter(INTERFACE_KEY, MonitorService.class.getName());
        String key = url.toServiceStringWithoutResolving();
        Monitor monitor = MONITORS.get(key);
        Future<Monitor> future = FUTURES.get(key);
        if (monitor != null || future != null) {
            return monitor;
        }

        LOCK.lock();
        try {
            monitor = MONITORS.get(key);
            future = FUTURES.get(key);
            if (monitor != null || future != null) {
                return monitor;
            }

            final URL monitorUrl = url;
            future = EXECUTOR.submit(() -> {
                try {
                    Monitor m = createMonitor(monitorUrl);
                    MONITORS.put(key, m);
                    FUTURES.remove(key);
                    return m;
                } catch (Throwable e) {
                    logger.warn(COMMON_MONITOR_EXCEPTION, "", "", "Create monitor failed, monitor data will not be collected until you fix this problem. monitorUrl: " + monitorUrl, e);
                    return null;
                }
            });
            FUTURES.put(key, future);
            return null;
        } finally {
            // unlock
            LOCK.unlock();
        }
    }

    protected abstract Monitor createMonitor(URL url);

}
