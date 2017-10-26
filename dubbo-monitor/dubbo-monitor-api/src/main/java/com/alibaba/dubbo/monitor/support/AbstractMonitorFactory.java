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
import com.alibaba.dubbo.monitor.Monitor;
import com.alibaba.dubbo.monitor.MonitorFactory;
import com.alibaba.dubbo.monitor.MonitorService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
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

    private int count = 0;

    public static Collection<Monitor> getMonitors() {
        return Collections.unmodifiableCollection(MONITORS.values());
    }

    /**
     * TODO 待ListenableFuture模式优化
     *
     * @param url
     * @return
     */
    public Monitor getMonitor(URL url) {
        url = url.setPath(MonitorService.class.getName()).addParameter(Constants.INTERFACE_KEY, MonitorService.class.getName());
        String key = url.toServiceStringWithoutResolving();
        LOCK.lock();
        try {
            Monitor monitor = MONITORS.get(key);
            if (monitor != null) {
                return monitor;
            }

            Future<Monitor> future = MONITOR_CREATORS.get(key);
            if (future != null) {
                if (future.isDone()) {
                    try {
                        monitor = future.get();
                        MONITORS.put(key, monitor);
                        MONITOR_CREATORS.remove(key);
                    } catch (Throwable t) {
                    }
                }
                return monitor;
            }

            // 数量：key=注册中心地址，数量一般很少
            if (count < 10) {
                final URL monitorUrl = url;
                FutureTask<Monitor> task = new FutureTask<Monitor>(new MonitorCreator(monitorUrl));
                Thread thread = new Thread(task);
                thread.setName("DubboMointorCreator-thread-" + ++count);
                thread.setDaemon(true);
                thread.start();
                try {
                    monitor = task.get(10, TimeUnit.MILLISECONDS);
                } catch (Throwable t) {
                    MONITOR_CREATORS.put(key, task);
                }
                if (monitor != null) {
                    MONITORS.put(key, monitor);
                }
            } else {
                monitor = this.createMonitor(url);
            }

            return monitor;
        } finally {
            // 释放锁
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
            Monitor monitor = AbstractMonitorFactory.this.createMonitor(url);
            return monitor;
        }
    }

}