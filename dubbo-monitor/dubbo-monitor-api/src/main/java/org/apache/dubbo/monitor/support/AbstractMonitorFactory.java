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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.concurrent.CompletableFutureTask;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.monitor.Monitor;
import org.apache.dubbo.monitor.MonitorFactory;
import org.apache.dubbo.monitor.MonitorService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * AbstractMonitorFactory. (SPI, Singleton, ThreadSafe)
 */
public abstract class AbstractMonitorFactory implements MonitorFactory {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMonitorFactory.class);

    /**
     * lock for getting monitor center
     */
    private static final ReentrantLock LOCK = new ReentrantLock();

    /**
     * monitor centers Map<RegistryAddress, Registry>
     */
    private static final Map<String, Monitor> MONITORS = new ConcurrentHashMap<>();

    private static final Map<String, CompletableFutureTask<Monitor>> FUTURES = new ConcurrentHashMap<>();

    public static Collection<Monitor> getMonitors() {
        return Collections.unmodifiableCollection(MONITORS.values());
    }

    @Override
    public Monitor getMonitor(URL url) {
        url = url.setPath(MonitorService.class.getName()).addParameter(Constants.INTERFACE_KEY,
            MonitorService.class.getName());
        String key = url.toServiceStringWithoutResolving();
        Monitor monitor = MONITORS.get(key);
        CompletableFutureTask<Monitor> completableFutureTask = FUTURES.get(key);
        if (monitor != null || completableFutureTask != null) {
            return monitor;
        }

        LOCK.lock();
        try {
            monitor = MONITORS.get(key);
            completableFutureTask = FUTURES.get(key);
            if (monitor != null || completableFutureTask != null) {
                return monitor;
            }

            final URL monitorUrl = url;

            final CompletableFutureTask<Monitor> futureTask = CompletableFutureTask.create(
                new Supplier<Monitor>() {
                    @Override
                    public Monitor get() {
                        try {
                            Monitor monitor = new MonitorCreator(monitorUrl).call();
                            MONITORS.put(key, monitor);
                            return monitor;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                });

            Thread connectThread = new Thread();
            connectThread.setName("AbstractMonitorFactory getMonitor");
            connectThread.setDaemon(true);

            futureTask.start(connectThread);

            FUTURES.put(key, futureTask);

            return null;
        } finally {
            // unlock
            LOCK.unlock();
        }
    }

    protected abstract Monitor createMonitor(URL url);

    class MonitorCreator implements Callable<Monitor> {

        private URL url;

        public MonitorCreator(URL url) {
            this.url = url;
        }

        @Override
        public Monitor call() throws Exception {
            return AbstractMonitorFactory.this.createMonitor(url);
        }
    }

    class MonitorListener implements Runnable {

        private String key;

        public MonitorListener(String key) {
            this.key = key;
        }

        @Override
        public void run() {
            CompletableFutureTask<Monitor> listenableFuture = AbstractMonitorFactory.FUTURES.get(key);
            AbstractMonitorFactory.MONITORS.put(key, listenableFuture.get(1000, TimeUnit.MILLISECONDS));
            AbstractMonitorFactory.FUTURES.remove(key);
        }
    }
}
