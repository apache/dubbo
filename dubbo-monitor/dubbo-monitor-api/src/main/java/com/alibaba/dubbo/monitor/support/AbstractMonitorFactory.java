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
package com.alibaba.dubbo.monitor.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.monitor.Monitor;
import com.alibaba.dubbo.monitor.MonitorFactory;
import com.alibaba.dubbo.monitor.MonitorService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AbstractMonitorFactroy. (SPI, Singleton, ThreadSafe)
 *
 * @author william.liangf
 */
public abstract class AbstractMonitorFactory implements MonitorFactory {
    private static final Logger logger = LoggerFactory.getLogger(AbstractMonitorFactory.class);

    // 注册中心获取过程锁
    private static final ReentrantLock LOCK = new ReentrantLock();

    // 注册中心集合 Map<RegistryAddress, Registry>
    private static final Map<String, Monitor> MONITORS = new ConcurrentHashMap<String, Monitor>();

    private static final Map<String, Future<Monitor>> MONITOR_CREATORS = new ConcurrentHashMap<String, Future<Monitor>>();

    private static final ExecutorService monitorScanner = Executors.newFixedThreadPool(1, new NamedThreadFactory("DubboMonitorScanner", true));

    static {
        monitorScanner.submit(new MonitorScanTask());
    }

    public static Collection<Monitor> getMonitors() {
        return Collections.unmodifiableCollection(MONITORS.values());
    }

    public Monitor getMonitor(URL url) {
        url = url.setPath(MonitorService.class.getName()).addParameter(Constants.INTERFACE_KEY, MonitorService.class.getName());
        String key = url.toServiceStringWithoutResolving();
        LOCK.lock();
        try {
            Monitor monitor = MONITORS.get(key);
            if (monitor != null || MONITOR_CREATORS.get(key) != null) {
                return monitor;
            }

            final URL monitorUrl = url;
            FutureTask<Monitor> task = new FutureTask<Monitor>(new MonitorCreator(monitorUrl));
            Thread thread = new Thread(task);
            thread.setName("DubboMointorCreator-thread-1");
            thread.setDaemon(true);
            thread.start();
            try {
                System.out.println("main: " + System.currentTimeMillis());
                monitor = task.get(10, TimeUnit.MILLISECONDS);
            } catch (Throwable t) {
                MONITOR_CREATORS.put(key, task);
            }
            if (monitor != null) {
                MONITORS.put(key, monitor);
            }

            return monitor;
        } finally {
            // 释放锁
            LOCK.unlock();
        }
    }

    protected abstract Monitor createMonitor(URL url);

    static class MonitorScanTask implements Runnable {
        @Override
        public void run() {
            while (true) {
                for (Map.Entry<String, Future<Monitor>> entry : MONITOR_CREATORS.entrySet()) {
                    System.out.println(MONITOR_CREATORS.size());
                    String key = entry.getKey();
                    Future<Monitor> future = MONITOR_CREATORS.get(key);
                    if (future != null) {
                        try {
                            Monitor monitor = future.get(10, TimeUnit.MILLISECONDS);
                            System.out.println(monitor);
                            MONITORS.put(key, monitor);
                            MONITOR_CREATORS.remove(key);
                        } catch (Throwable t) {
                            logger.info(t);
                        }
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    class MonitorCreator implements Callable<Monitor> {

        private URL url;

        public MonitorCreator(URL url) {
            this.url = url;
        }

        @Override
        public Monitor call() throws Exception {
            Monitor monitor = AbstractMonitorFactory.this.createMonitor(url);
            System.out.println("thread: " + System.currentTimeMillis());
            return monitor;
        }
    }

}