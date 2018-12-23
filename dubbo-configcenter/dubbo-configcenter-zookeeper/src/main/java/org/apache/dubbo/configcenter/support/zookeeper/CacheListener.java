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
package org.apache.dubbo.configcenter.support.zookeeper;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.configcenter.ConfigChangeEvent;
import org.apache.dubbo.configcenter.ConfigChangeType;
import org.apache.dubbo.configcenter.ConfigurationListener;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;

/**
 *
 */
public class CacheListener implements TreeCacheListener {
    private Map<String, Set<ConfigurationListener>> keyListeners = new ConcurrentHashMap<>();
    private CountDownLatch initializedLatch;
    private String rootPath;

    public CacheListener(String rootPath, CountDownLatch initializedLatch) {
        this.rootPath = rootPath;
        this.initializedLatch = initializedLatch;
    }

    @Override
    public void childEvent(CuratorFramework aClient, TreeCacheEvent event) throws Exception {

        TreeCacheEvent.Type type = event.getType();
        ChildData data = event.getData();
        if (type == TreeCacheEvent.Type.INITIALIZED) {
            initializedLatch.countDown();
            return;
        }

        // TODO, ignore other event types
        if (data == null) {
            return;
        }

        // TODO We limit the notification of config changes to a specific path level, for example
        //  /dubbo/config/service/configurators, other config changes not in this level will not get notified,
        //  say /dubbo/config/dubbo.properties
        if (data.getPath().split("/").length == 5) {
            byte[] value = data.getData();
            String key = pathToKey(data.getPath());
            ConfigChangeType changeType;
            switch (type) {
                case NODE_ADDED:
                    changeType = ConfigChangeType.ADDED;
                    break;
                case NODE_REMOVED:
                    changeType = ConfigChangeType.DELETED;
                    break;
                case NODE_UPDATED:
                    changeType = ConfigChangeType.MODIFIED;
                    break;
                default:
                    return;
            }

            ConfigChangeEvent configChangeEvent = new ConfigChangeEvent(key, new String(value, StandardCharsets.UTF_8), changeType);
            Set<ConfigurationListener> listeners = keyListeners.get(key);
            if (CollectionUtils.isNotEmpty(listeners)) {
                listeners.forEach(listener -> listener.process(configChangeEvent));
            }
        }
    }

    public void addListener(String key, ConfigurationListener configurationListener) {
        Set<ConfigurationListener> listeners = this.keyListeners.computeIfAbsent(key, k -> new CopyOnWriteArraySet<>());
        listeners.add(configurationListener);
    }

    public void removeListener(String key, ConfigurationListener configurationListener) {
        Set<ConfigurationListener> listeners = this.keyListeners.get(key);
        if (listeners != null) {
            listeners.remove(configurationListener);
        }
    }

    /**
     * This is used to convert a configuration nodePath into a key
     * TODO doc
     *
     * @param path
     * @return key (nodePath less the config root path)
     */
    private String pathToKey(String path) {
        if (StringUtils.isEmpty(path)) {
            return path;
        }
        return path.replace(rootPath + "/", "").replaceAll("/", ".");
    }
}
