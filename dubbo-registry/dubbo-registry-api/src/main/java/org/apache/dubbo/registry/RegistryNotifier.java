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

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class RegistryNotifier {
    private long lastExecuteTime;
    private long lastEventTime;

    private Object rawAddresses;
    private Registry registry;

    private ScheduledExecutorService scheduler = ExtensionLoader.getExtensionLoader(ExecutorRepository.class)
            .getDefaultExtension().getRegistryNotificationExecutor();

    public Registry getRegistry() {
        return registry;
    }

    public RegistryNotifier(Registry registry) {
        this.registry = registry;
    }

    public void notify(Object rawAddresses) {
        this.rawAddresses = rawAddresses;
        long notifyTime = System.currentTimeMillis();
        this.lastEventTime = notifyTime;
        scheduler.submit(new NotificationTask(this, notifyTime));
    }

    protected abstract void doNotify(Object rawAddresses);

    public class NotificationTask implements Runnable {
        private RegistryNotifier listener;
        private long time;

        public NotificationTask(RegistryNotifier listener, long time) {
            this.listener = listener;
            this.time = time;
        }

        @Override
        public void run() {
            int delayTime = getRegistry().getDelay();
            if (this.time < listener.lastEventTime) {
                return;
            } else if (delayTime <= 0) {
                listener.doNotify(listener.rawAddresses);
                listener.lastExecuteTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - listener.lastExecuteTime < delayTime) {
                scheduler.schedule(new NotificationTask(this.listener, this.time), System.currentTimeMillis() - listener.lastExecuteTime, TimeUnit.MILLISECONDS);
            } else {
                listener.doNotify(listener.rawAddresses);
                listener.lastExecuteTime = System.currentTimeMillis();
            }
        }
    }

}
