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
package org.apache.dubbo.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_DELAY_EXECUTE_TIMES;

public abstract class RegistryNotifier {

    private static final Logger logger = LoggerFactory.getLogger(RegistryNotifier.class);
    private volatile long lastExecuteTime;
    private volatile long lastEventTime;

    private Object rawAddresses;
    private long delayTime;

    // should delay notify or not
    private final AtomicBoolean shouldDelay = new AtomicBoolean(false);
    // for the first 10 notification will be notified immediately
    private final AtomicInteger executeTime = new AtomicInteger(0);

    private ScheduledExecutorService scheduler;

    public RegistryNotifier(URL registryUrl, long delayTime) {
        this(registryUrl, delayTime, null);
    }

    public RegistryNotifier(URL registryUrl, long delayTime, ScheduledExecutorService scheduler) {
        this.delayTime = delayTime;
        if (scheduler == null) {
            this.scheduler = registryUrl.getOrDefaultApplicationModel().getExtensionLoader(ExecutorRepository.class)
                    .getDefaultExtension().getRegistryNotificationExecutor();
        } else {
            this.scheduler = scheduler;
        }
    }

    public synchronized void notify(Object rawAddresses) {
        this.rawAddresses = rawAddresses;
        long notifyTime = System.currentTimeMillis();
        this.lastEventTime = notifyTime;

        long delta = (System.currentTimeMillis() - lastExecuteTime) - delayTime;

        // more than 10 calls && next execute time is in the future
        boolean delay = shouldDelay.get() && delta < 0;
        if (delay) {
            scheduler.schedule(new NotificationTask(this, notifyTime), -delta, TimeUnit.MILLISECONDS);
        } else {
            // check if more than 10 calls
            if (!shouldDelay.get() && executeTime.incrementAndGet() > DEFAULT_DELAY_EXECUTE_TIMES) {
                shouldDelay.set(true);
            }
            scheduler.submit(new NotificationTask(this, notifyTime));
        }
    }

    public long getDelayTime() {
        return delayTime;
    }

    protected abstract void doNotify(Object rawAddresses);

    public static class NotificationTask implements Runnable {
        private final RegistryNotifier listener;
        private final long time;

        public NotificationTask(RegistryNotifier listener, long time) {
            this.listener = listener;
            this.time = time;
        }

        @Override
        public void run() {
            try {
                if (this.time == listener.lastEventTime) {
                    listener.doNotify(listener.rawAddresses);
                    listener.lastExecuteTime = System.currentTimeMillis();
                    synchronized (listener) {
                        if (this.time == listener.lastEventTime) {
                            listener.rawAddresses = null;
                        }
                    }
                }
            } catch (Throwable t) {
                logger.error("Error occurred when notify directory. ", t);
            }
        }
    }

}
