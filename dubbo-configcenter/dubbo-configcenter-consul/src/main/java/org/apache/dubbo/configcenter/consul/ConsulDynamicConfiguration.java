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
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;

import com.google.common.base.Charsets;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.model.kv.Value;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.config.configcenter.Constants.CONFIG_NAMESPACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.common.utils.StringUtils.EMPTY_STRING;

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
    private Consul client;
    private KeyValueClient kvClient;
    private ConcurrentMap<String, ConsulListener> watchers = new ConcurrentHashMap<>();

    public ConsulDynamicConfiguration(URL url) {
        this.url = url;
        this.rootPath = PATH_SEPARATOR + url.getParameter(CONFIG_NAMESPACE_KEY, DEFAULT_GROUP) + PATH_SEPARATOR + "config";
        String host = url.getHost();
        int port = url.getPort() != 0 ? url.getPort() : DEFAULT_PORT;
        client = Consul.builder().withHostAndPort(HostAndPort.fromParts(host, port)).build();
        this.kvClient = client.keyValueClient();
    }

    @Override
    public void addListener(String key, String group, ConfigurationListener listener) {
        logger.info("register listener " + listener.getClass() + " for config with key: " + key + ", group: " + group);
        String normalizedKey = convertKey(group, key);
        ConsulListener watcher = watchers.computeIfAbsent(normalizedKey, k -> new ConsulListener(key, group));
        watcher.addListener(listener);
    }

    @Override
    public void removeListener(String key, String group, ConfigurationListener listener) {
        logger.info("unregister listener " + listener.getClass() + " for config with key: " + key + ", group: " + group);
        ConsulListener watcher = watchers.get(convertKey(group, key));
        if (watcher != null) {
            watcher.removeListener(listener);
        }
    }

    @Override
    public String getConfig(String key, String group, long timeout) throws IllegalStateException {
        return (String) getInternalProperty(convertKey(group, key));
    }

    @Override
    public SortedSet<String> getConfigKeys(String group) throws UnsupportedOperationException {
        SortedSet<String> configKeys = new TreeSet<>();
        String normalizedKey = convertKey(group, EMPTY_STRING);
        List<String> keys = kvClient.getKeys(normalizedKey);
        if (CollectionUtils.isNotEmpty(keys)) {
            keys.stream()
                    .filter(k -> !k.equals(normalizedKey))
                    .map(k -> k.substring(k.lastIndexOf(PATH_SEPARATOR) + 1))
                    .forEach(configKeys::add);
        }
        return configKeys;
//        SortedSet<String> configKeys = new TreeSet<>();
//        String normalizedKey = convertKey(group, key);
//        kvClient.getValueAsString(normalizedKey).ifPresent(v -> {
//            Collections.addAll(configKeys, v.split(","));
//        });
//        return configKeys;
    }

    /**
     * @param key     the key to represent a configuration
     * @param group   the group where the key belongs to
     * @param content the content of configuration
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public boolean publishConfig(String key, String group, String content) throws UnsupportedOperationException {
//        String normalizedKey = convertKey(group, key);
//        Value value = kvClient.getValue(normalizedKey).orElseThrow(() -> new IllegalArgumentException(normalizedKey + " does not exit."));
//        Optional<String> old = value.getValueAsString();
//        if (old.isPresent()) {
//            content = old.get() + "," + content;
//        }
//
//        while (!kvClient.putValue(key, content, value.getModifyIndex())) {
//            value = kvClient.getValue(normalizedKey).orElseThrow(() -> new IllegalArgumentException(normalizedKey + " does not exit."));
//            old = value.getValueAsString();
//            if (old.isPresent()) {
//                content = old.get() + "," + content;
//            }
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        return true;
        String normalizedKey = convertKey(group, key);
        return kvClient.putValue(normalizedKey + PATH_SEPARATOR + content);
    }

    @Override
    public Object getInternalProperty(String key) {
        logger.info("getting config from: " + key);
        return kvClient.getValueAsString(key, Charsets.UTF_8).orElseThrow(() -> new IllegalArgumentException(key + " does not exit."));
    }

    @Override
    public void close() throws Exception {
        client.destroy();
    }

    private String buildPath(String group) {
        String actualGroup = StringUtils.isEmpty(group) ? DEFAULT_GROUP : group;
        return rootPath + PATH_SEPARATOR + actualGroup;
    }

    private String convertKey(String group, String key) {
        return buildPath(group) + PATH_SEPARATOR + key;
    }

    private class ConsulListener implements KVCache.Listener<String, Value> {

        private KVCache kvCache;
        private Set<ConfigurationListener> listeners = new LinkedHashSet<>();
        private String key;
        private String group;
        private String normalizedKey;

        public ConsulListener(String key, String group) {
            this.key = key;
            this.group = group;
            this.normalizedKey = convertKey(group, key);
            initKVCache();
        }

        private void initKVCache() {
            this.kvCache = KVCache.newCache(kvClient, normalizedKey);
            kvCache.addListener(this);
            kvCache.start();
        }

        @Override
        public void notify(Map<String, Value> newValues) {
            // Cache notifies all paths with "foo" the root path
            // If you want to watch only "foo" value, you must filter other paths
            Optional<Value> newValue = newValues.values().stream()
                    .filter(value -> value.getKey().equals(normalizedKey))
                    .findAny();

            newValue.ifPresent(value -> {
                // Values are encoded in key/value store, decode it if needed
                Optional<String> decodedValue = newValue.get().getValueAsString();
                decodedValue.ifPresent(v -> listeners.forEach(l -> {
                    ConfigChangedEvent event = new ConfigChangedEvent(key, group, v, ConfigChangeType.MODIFIED);
                    l.process(event);
                }));
            });
        }

        private void addListener(ConfigurationListener listener) {
            this.listeners.add(listener);
        }

        private void removeListener(ConfigurationListener listener) {
            this.listeners.remove(listener);
        }
    }
}
