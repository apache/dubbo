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
package org.apache.dubbo.common.config.configcenter.wrapper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.function.ThrowableConsumer;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.dubbo.common.constants.CommonConstants.RETRY_PERIOD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RETRY_TIMES_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_SERVER_DISCONNECTED;

public class FailbackDynamicConfiguration implements DynamicConfiguration {
    private final DynamicConfiguration wrapper;
    private final Map<ListenerIdentification, ThrowableConsumer<DynamicConfiguration>> retryTask =
            new ConcurrentHashMap<>();
    private ScheduledExecutorService retryExecutor;
    private volatile ScheduledFuture<?> retryScheduledFuture = null;
    private final NamedThreadFactory namedThreadFactory;
    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(FailbackDynamicConfiguration.class);
    private final AtomicLong retryCounter = new AtomicLong(0);
    private final int retryLimit;
    private final long retryPeriod;

    public FailbackDynamicConfiguration(DynamicConfiguration wrapper, URL url) {
        this.wrapper = wrapper;
        this.namedThreadFactory = new NamedThreadFactory("DynamicConfigurationListenerRetry", true);
        this.retryLimit = url.getParameter(RETRY_TIMES_KEY, 0);
        this.retryPeriod = url.getParameter(RETRY_PERIOD_KEY, 3000);
    }

    private void start() {
        if (retryScheduledFuture == null) {
            synchronized (retryCounter) {
                if (retryScheduledFuture == null) {
                    retryExecutor = Executors.newScheduledThreadPool(0, namedThreadFactory);
                    retryScheduledFuture =
                            retryExecutor.scheduleWithFixedDelay(this::retry, 1000, retryPeriod, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private void retry() {
        if (!wrapper.isAvailable()) {
            return;
        }
        if (retryTask.isEmpty() || (retryLimit > 0 && retryCounter.incrementAndGet() > retryLimit)) {
            cancel();
            return;
        }

        for (Entry<ListenerIdentification, ThrowableConsumer<DynamicConfiguration>> entry : retryTask.entrySet()) {
            ListenerIdentification identification = entry.getKey();
            ThrowableConsumer<DynamicConfiguration> consumer = entry.getValue();
            retryTask.remove(identification);
            try {
                consumer.accept(wrapper);
            } catch (Throwable e) {
                retryTask.put(identification, consumer);
                logger.error(
                        CONFIG_SERVER_DISCONNECTED,
                        e.getMessage(),
                        "",
                        "Retry add listener " + identification + "fail.",
                        e);
            }
        }
    }

    private void cancel() {
        if (retryScheduledFuture != null) {
            retryScheduledFuture.cancel(false);
            retryScheduledFuture = null;
        }
        retryExecutor.shutdown();
    }

    @Override
    public Object getInternalProperty(String key) {
        return wrapper.isAvailable() ? wrapper.getInternalProperty(key) : null;
    }

    @Override
    public void addListener(String key, String group, ConfigurationListener listener) {
        if (wrapper.isAvailable()) {
            wrapper.addListener(key, group, listener);
        } else {
            ThrowableConsumer<DynamicConfiguration> consumer =
                    dynamicConfiguration -> dynamicConfiguration.addListener(key, group, listener);
            retryTask.put(new ListenerIdentification(key, group, listener), consumer);
            start();
        }
    }

    @Override
    public void removeListener(String key, String group, ConfigurationListener listener) {
        if (wrapper.isAvailable()) {
            wrapper.removeListener(key, group, listener);
        } else {
            ListenerIdentification identification = new ListenerIdentification(key, group, listener);
            ThrowableConsumer<DynamicConfiguration> consumer = retryTask.remove(identification);
            if (consumer == null) {
                consumer = dynamicConfiguration -> dynamicConfiguration.removeListener(key, group, listener);
                retryTask.put(identification, consumer);
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (retryScheduledFuture != null) {
            retryScheduledFuture.cancel(false);
        }
        retryExecutor.shutdown();
        wrapper.close();
    }

    @Override
    public String getConfig(String key, String group, long timeout) throws IllegalStateException {
        return wrapper.isAvailable() ? wrapper.getConfig(key, group, timeout) : null;
    }

    static class ListenerIdentification {
        private final String key;
        private final String group;
        private final ConfigurationListener listener;

        ListenerIdentification(String key, String group, ConfigurationListener listener) {
            this.key = key;
            this.group = group;
            this.listener = listener;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, group, listener);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            ListenerIdentification listenerIdentification = (ListenerIdentification) obj;
            return Objects.equals(key, listenerIdentification.key)
                    && Objects.equals(group, listenerIdentification.group)
                    && Objects.equals(listener, listenerIdentification.listener);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (key != null) {
                sb.append(key).append(":");
            }
            if (group != null) {
                sb.append(group).append(":");
            }
            if (listener != null) {
                sb.append(listener);
            }
            return sb.toString();
        }
    }
}
