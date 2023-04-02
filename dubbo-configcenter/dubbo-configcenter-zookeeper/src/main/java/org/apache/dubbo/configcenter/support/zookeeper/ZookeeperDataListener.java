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
import org.apache.dubbo.metrics.collector.ConfigCenterMetricsCollector;
import org.apache.dubbo.remoting.zookeeper.DataListener;
import org.apache.dubbo.remoting.zookeeper.EventType;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * one path has multi configurationListeners
 */
public class ZookeeperDataListener implements DataListener {

    private String path;
    private String key;
    private String group;
    private Set<ConfigurationListener> listeners;
    private ApplicationModel applicationModel;

    public ZookeeperDataListener(String path, String key, String group, ApplicationModel applicationModel) {
        this.path = path;
        this.key = key;
        this.group = group;
        this.listeners = new CopyOnWriteArraySet<>();
        this.applicationModel = applicationModel;
    }

    public void addListener(ConfigurationListener configurationListener) {
        listeners.add(configurationListener);
    }

    public void removeListener(ConfigurationListener configurationListener) {
        listeners.remove(configurationListener);
    }

    public Set<ConfigurationListener> getListeners() {
        return listeners;
    }

    @Override
    public void dataChanged(String path, Object value, EventType eventType) {
        if (!this.path.equals(path)) {
            return;
        }
        ConfigChangeType changeType;
        if (EventType.NodeCreated.equals(eventType)) {
            changeType = ConfigChangeType.ADDED;
        } else if (value == null) {
            changeType = ConfigChangeType.DELETED;
        } else {
            changeType = ConfigChangeType.MODIFIED;
        }
        ConfigChangedEvent configChangeEvent = new ConfigChangedEvent(key, group, (String) value, changeType);
        if (CollectionUtils.isNotEmpty(listeners)) {
            listeners.forEach(listener -> listener.process(configChangeEvent));
        }

        ConfigCenterMetricsCollector collector =
            applicationModel.getBeanFactory().getBean(ConfigCenterMetricsCollector.class);
        collector.increaseUpdated("zookeeper", applicationModel.getApplicationName(), configChangeEvent);
    }

}
