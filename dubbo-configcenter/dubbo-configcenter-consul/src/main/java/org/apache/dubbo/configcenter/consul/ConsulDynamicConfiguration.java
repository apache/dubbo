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

package org.apache.dubbo.configcenter.consul;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.apache.dubbo.common.config.configcenter.ConfigChangeType.ADDED;
import static org.apache.dubbo.common.config.configcenter.Constants.CONFIG_NAMESPACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;

/**
 * config center implementation for consul
 */
public class ConsulDynamicConfiguration implements DynamicConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ConsulDynamicConfiguration.class);

    private static final int DEFAULT_PORT = 8500;
    private static final int DEFAULT_WATCH_TIMEOUT = 60 * 1000;
    private static final String WATCH_TIMEOUT = "consul-watch-timeout";

    private URL url;
    private String rootPath;
    private ConsulClient client;
    private int watchTimeout = -1;
    private ConcurrentMap<String, ConsulKVWatcher> watchers = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Long> consulIndexes = new ConcurrentHashMap<>();
    private ExecutorService watcherService = newCachedThreadPool(
            new NamedThreadFactory("dubbo-consul-configuration-watcher", true));

    public ConsulDynamicConfiguration(URL url) {
        this.url = url;
        this.rootPath = PATH_SEPARATOR + url.getParameter(CONFIG_NAMESPACE_KEY, DEFAULT_GROUP) + PATH_SEPARATOR + "config";
        this.watchTimeout = buildWatchTimeout(url);
        String host = url.getHost();
        int port = url.getPort() != 0 ? url.getPort() : DEFAULT_PORT;
        client = new ConsulClient(host, port);
    }

    @Override
    public void addListener(String key, String group, ConfigurationListener listener) {
        logger.info("register listener " + listener.getClass() + " for config with key: " + key + ", group: " + group);
        String normalizedKey = convertKey(group, key);
        ConsulKVWatcher watcher = watchers.putIfAbsent(normalizedKey, new ConsulKVWatcher(key, group));
        if (watcher == null) {
            watcher = watchers.get(normalizedKey);
            watcherService.submit(watcher);
        }
        watcher.addListener(listener);
    }

    @Override
    public void removeListener(String key, String group, ConfigurationListener listener) {
        logger.info("unregister listener " + listener.getClass() + " for config with key: " + key + ", group: " + group);
        ConsulKVWatcher watcher = watchers.get(convertKey(group, key));
        if (watcher != null) {
            watcher.removeListener(listener);
        }
    }

    @Override
    public String getConfig(String key, String group, long timeout) throws IllegalStateException {
        return (String) getInternalProperty(convertKey(group, key));
    }

    @Override
    public Object getInternalProperty(String key) {
        logger.info("get config from: " + key);
        Response<GetValue> response = getValue(key);
        if (response != null) {
            GetValue value = response.getValue();
            consulIndexes.put(key, response.getConsulIndex());
            return value != null ? value.getDecodedValue() : null;
        }
        return null;
    }

    private Response<GetValue> getValue(String key) {
        try {
            Long currentIndex = consulIndexes.computeIfAbsent(key, k -> -1L);
            return client.getKVValue(key, new QueryParams(watchTimeout, currentIndex));
        } catch (Throwable t) {
            logger.warn("fail to get value for key: " + key);
        }
        return null;
    }

    private String buildPath(String group) {
        String actualGroup = StringUtils.isEmpty(group) ? DEFAULT_GROUP : group;
        return rootPath + PATH_SEPARATOR + actualGroup;
    }

    private String convertKey(String group, String key) {
        return buildPath(group) + PATH_SEPARATOR + key;
    }

    private int buildWatchTimeout(URL url) {
        return url.getParameter(WATCH_TIMEOUT, DEFAULT_WATCH_TIMEOUT) / 1000;
    }

    private class ConsulKVWatcher implements Runnable {
        private final String key;
        private final String group;
        private final String normalizedKey;
        private Set<ConfigurationListener> listeners;
        private boolean running = true;
        private boolean existing = false;

        public ConsulKVWatcher(String key, String group) {
            this.key = key;
            this.group = group;
            this.normalizedKey = convertKey(group, key);
            this.listeners = new HashSet<>();
        }

        @Override
        public void run() {
            while (running) {
                Long lastIndex = consulIndexes.computeIfAbsent(normalizedKey, k -> -1L);
                Response<GetValue> response = getValue(normalizedKey);
                if (response == null) {
                    try {
                        Thread.sleep(watchTimeout);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    continue;
                }

                GetValue getValue = response.getValue();
                Long currentIndex = response.getConsulIndex();
                if (currentIndex == null || currentIndex <= lastIndex) {
                    continue;
                }

                consulIndexes.put(key, currentIndex);
                ConfigChangedEvent event = null;
                if (getValue != null) {
                    String value = getValue.getDecodedValue();
                    if (existing) {
                        logger.info("notify change for key: " + normalizedKey + ", the changed value is: " + value);
                        event = new ConfigChangedEvent(key, group, value);
                    } else {
                        logger.info("notify change for key: " + normalizedKey + ", the added value is: " + value);
                        event = new ConfigChangedEvent(key, group, value, ADDED);
                    }
                } else {
                    if (existing) {
                        logger.info("notify change for key: " + normalizedKey + ", the value is deleted");
                        event = new ConfigChangedEvent(key, group, null, ConfigChangeType.DELETED);
                    }
                }

                existing = getValue != null;
                if (event != null) {
                    for (ConfigurationListener listener : listeners) {
                        listener.process(event);
                    }
                }
            }
        }

        private void addListener(ConfigurationListener listener) {
            this.listeners.add(listener);
        }

        private void removeListener(ConfigurationListener listener) {
            this.listeners.remove(listener);
        }

        private void stop() {
            running = false;
        }
    }
}
