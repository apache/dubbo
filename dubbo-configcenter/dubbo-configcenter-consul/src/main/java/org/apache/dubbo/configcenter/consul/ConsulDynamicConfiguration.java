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
import org.apache.dubbo.common.config.configcenter.TreePathDynamicConfiguration;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;

import com.google.common.base.Charsets;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.model.kv.Value;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;

/**
 * config center implementation for consul
 */
public class ConsulDynamicConfiguration extends TreePathDynamicConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ConsulDynamicConfiguration.class);

    private static final int DEFAULT_PORT = 8500;
    private static final int DEFAULT_WATCH_TIMEOUT = 60 * 1000;
    private static final String WATCH_TIMEOUT = "consul-watch-timeout";

    private Consul client;

    private KeyValueClient kvClient;

    private ConcurrentMap<String, ConsulListener> watchers = new ConcurrentHashMap<>();

    public ConsulDynamicConfiguration(URL url) {
        super(url);
        String host = url.getHost();
        int port = url.getPort() != 0 ? url.getPort() : DEFAULT_PORT;
        client = Consul.builder().withHostAndPort(HostAndPort.fromParts(host, port)).build();
        this.kvClient = client.keyValueClient();
    }

    @Override
    public String getInternalProperty(String key) {
        logger.info("getting config from: " + key);
        return kvClient.getValueAsString(key, Charsets.UTF_8).orElse(null);
    }

    @Override
    protected boolean doPublishConfig(String pathKey, String content) throws Exception {
        return kvClient.putValue(pathKey, content);
    }

    @Override
    protected String doGetConfig(String pathKey) throws Exception {
        return getInternalProperty(pathKey);
    }

    @Override
    protected boolean doRemoveConfig(String pathKey) throws Exception {
        kvClient.deleteKey(pathKey);
        return true;
    }

    @Override
    protected Collection<String> doGetConfigKeys(String groupPath) {
        List<String> keys = kvClient.getKeys(groupPath);
        List<String> configKeys = new LinkedList<>();
        if (CollectionUtils.isNotEmpty(keys)) {
            keys.stream()
                    .filter(k -> !k.equals(groupPath))
                    .map(k -> k.substring(k.lastIndexOf(PATH_SEPARATOR) + 1))
                    .forEach(configKeys::add);
        }
        return configKeys;
    }

    @Override
    protected void doAddListener(String pathKey, ConfigurationListener listener) {
        logger.info("register listener " + listener.getClass() + " for config with key: " + pathKey);
        ConsulListener watcher = watchers.computeIfAbsent(pathKey, k -> new ConsulListener(pathKey));
        watcher.addListener(listener);
    }

    @Override
    protected void doRemoveListener(String pathKey, ConfigurationListener listener) {
        logger.info("unregister listener " + listener.getClass() + " for config with key: " + pathKey);
        ConsulListener watcher = watchers.get(pathKey);
        if (watcher != null) {
            watcher.removeListener(listener);
        }
    }

    @Override
    protected void doClose() throws Exception {
        client.destroy();
    }

    private class ConsulListener implements KVCache.Listener<String, Value> {

        private KVCache kvCache;
        private Set<ConfigurationListener> listeners = new LinkedHashSet<>();
        private String normalizedKey;

        public ConsulListener(String normalizedKey) {
            this.normalizedKey = normalizedKey;
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
                    ConfigChangedEvent event = new ConfigChangedEvent(normalizedKey, getGroup(), v, ConfigChangeType.MODIFIED);
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
