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

import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * one path has one zookeeperDataListener
 */
public class CacheListener {

    private final ConcurrentMap<String, ZookeeperDataListener> pathKeyListeners = new ConcurrentHashMap<>();

    public CacheListener() {
    }

    public ZookeeperDataListener addListener(String pathKey, ConfigurationListener configurationListener, String key, String group, ApplicationModel applicationModel) {
        ZookeeperDataListener zookeeperDataListener = ConcurrentHashMapUtils.computeIfAbsent(pathKeyListeners, pathKey,
            _pathKey -> new ZookeeperDataListener(_pathKey, key, group, applicationModel));
        zookeeperDataListener.addListener(configurationListener);
        return zookeeperDataListener;
    }

    public ZookeeperDataListener removeListener(String pathKey, ConfigurationListener configurationListener) {
        ZookeeperDataListener zookeeperDataListener = pathKeyListeners.get(pathKey);
        if (zookeeperDataListener != null) {
            zookeeperDataListener.removeListener(configurationListener);
            if (CollectionUtils.isEmpty(zookeeperDataListener.getListeners())) {
                pathKeyListeners.remove(pathKey);
            }
        }
        return zookeeperDataListener;
    }

    public ZookeeperDataListener getCachedListener(String pathKey) {
        return pathKeyListeners.get(pathKey);
    }

    public Map<String, ZookeeperDataListener> getPathKeyListeners() {
        return pathKeyListeners;
    }

    public void clear() {
        pathKeyListeners.clear();
    }
}

