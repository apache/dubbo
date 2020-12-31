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

import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.zookeeper.DataListener;
import org.apache.dubbo.remoting.zookeeper.EventType;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;

import static org.apache.dubbo.common.constants.CommonConstants.DOT_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;

/**
 *
 */

public class CacheListener implements DataListener {
    private static final int MIN_PATH_DEPTH = 5;

    private Map<String, Set<ConfigurationListener>> keyListeners = new ConcurrentHashMap<>();
    private CountDownLatch initializedLatch;
    private String rootPath;

    public CacheListener(String rootPath, CountDownLatch initializedLatch) {
        this.rootPath = rootPath;
        this.initializedLatch = initializedLatch;
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
        String groupKey = path.replace(rootPath + PATH_SEPARATOR, "").replaceAll(PATH_SEPARATOR, DOT_SEPARATOR);
        return groupKey.substring(groupKey.indexOf(DOT_SEPARATOR) + 1);
    }

    private String getGroup(String path) {
        if (!StringUtils.isEmpty(path)) {
            int beginIndex = path.indexOf(rootPath + PATH_SEPARATOR);
            if (beginIndex > -1) {
                int endIndex = path.indexOf(PATH_SEPARATOR, beginIndex);
                if (endIndex > beginIndex) {
                    return path.substring(beginIndex, endIndex);
                }
            }
        }
        return path;
    }


    @Override
    public void dataChanged(String path, Object value, EventType eventType) {
        if (eventType == null) {
            return;
        }

        if (eventType == EventType.INITIALIZED) {
            initializedLatch.countDown();
            return;
        }

        if (path == null || (value == null && eventType != EventType.NodeDeleted)) {
            return;
        }

        // TODO We only care the changes happened on a specific path level, for example
        //  /dubbo/config/dubbo/configurators, other config changes not in this level will be ignored,
        if (path.split("/").length >= MIN_PATH_DEPTH) {
            String key = pathToKey(path);
            ConfigChangeType changeType;
            switch (eventType) {
                case NodeCreated:
                    changeType = ConfigChangeType.ADDED;
                    break;
                case NodeDeleted:
                    changeType = ConfigChangeType.DELETED;
                    break;
                case NodeDataChanged:
                    changeType = ConfigChangeType.MODIFIED;
                    break;
                default:
                    return;
            }

            ConfigChangedEvent configChangeEvent = new ConfigChangedEvent(key, getGroup(path), (String) value, changeType);
            Set<ConfigurationListener> listeners = keyListeners.get(path);
            if (CollectionUtils.isNotEmpty(listeners)) {
                listeners.forEach(listener -> listener.process(configChangeEvent));
            }
        }
    }
}
