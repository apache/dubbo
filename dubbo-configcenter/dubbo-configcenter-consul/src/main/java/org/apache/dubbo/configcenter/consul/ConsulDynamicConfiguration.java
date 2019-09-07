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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.configcenter.ConfigChangeEvent;
import org.apache.dubbo.configcenter.ConfigChangeType;
import org.apache.dubbo.configcenter.ConfigurationListener;
import org.apache.dubbo.configcenter.DynamicConfiguration;

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
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.configcenter.ConfigChangeType.ADDED;
import static org.apache.dubbo.configcenter.Constants.CONFIG_NAMESPACE_KEY;

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
        ConsulKVWatcher watcher = watchers.putIfAbsent(normalizedKey, new ConsulKVWatcher(normalizedKey));
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
    public String getRule(String key, String group, long timeout) throws IllegalStateException {
        return (String) getInternalProperty(convertKey(group, key));
    }

    @Override
    public String getProperties(String key, String group, long timeout) throws IllegalStateException {
        if (StringUtils.isEmpty(group)) {
            group = DEFAULT_GROUP;
        }
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

    private String convertKey(String group, String key) {
        return rootPath + PATH_SEPARATOR + group + PATH_SEPARATOR + key;
    }

    private int buildWatchTimeout(URL url) {
        return url.getParameter(WATCH_TIMEOUT, DEFAULT_WATCH_TIMEOUT) / 1000;
    }

    private class ConsulKVWatcher implements Runnable {
        private String key;
        private Set<ConfigurationListener> listeners;
        private boolean running = true;
        private boolean existing = false;

        public ConsulKVWatcher(String key) {
            this.key = key;
            this.listeners = new HashSet<>();
        }

        @Override
        public void run() {
            while (running) {
                Long lastIndex = consulIndexes.computeIfAbsent(key, k -> -1L);
                Response<GetValue> response = getValue(key);
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
                ConfigChangeEvent event = null;
                if (getValue != null) {
                    String value = getValue.getDecodedValue();
                    if (existing) {
                        logger.info("notify change for key: " + key + ", the changed value is: " + value);
                        event = new ConfigChangeEvent(key, value);
                    } else {
                        logger.info("notify change for key: " + key + ", the added value is: " + value);
                        event = new ConfigChangeEvent(key, value, ADDED);
                    }
                } else {
                    if (existing) {
                        logger.info("notify change for key: " + key + ", the value is deleted");
                        event = new ConfigChangeEvent(key, null, ConfigChangeType.DELETED);
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
