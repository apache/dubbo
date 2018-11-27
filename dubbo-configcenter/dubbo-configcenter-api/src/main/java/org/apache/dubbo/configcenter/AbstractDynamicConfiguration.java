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
package org.apache.dubbo.configcenter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.AbstractConfiguration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Dynamic configuration template class. The concrete implementation needs to provide implementation for three methods.
 *
 * @see AbstractDynamicConfiguration#getTargetConfig(String, String, long)
 * @see AbstractDynamicConfiguration#addConfigurationListener(TargetListener, ConfigurationListener)
 * @see AbstractDynamicConfiguration#createTargetListener(String)
 */
public abstract class AbstractDynamicConfiguration<TargetListener> extends AbstractConfiguration
        implements DynamicConfiguration {
    protected static final String DEFAULT_GROUP = "dubbo";

    protected URL url;

    // One key can register multiple target listeners, but one target listener only maps to one configuration listener
    protected ConcurrentMap<String, TargetListener> targetListeners =
            new ConcurrentHashMap<>();

    public AbstractDynamicConfiguration() {
    }

    @Override
    public void initWith(URL url) {
        this.url = url;
    }

    @Override
    public void addListener(String key, ConfigurationListener listener) {
        TargetListener targetListener = targetListeners.computeIfAbsent(key, this::createTargetListener);
        addConfigurationListener(key, targetListener, listener);
    }

    @Override
    public String getConfig(String key) {
        return getConfig(key, null, null);
    }

    @Override
    public String getConfig(String key, String group) {
        return getConfig(key, group, null);
    }

    @Override
    public String getConfig(String key, ConfigurationListener listener) {
        return getConfig(key, null, listener);
    }

    @Override
    public String getConfig(String key, String group, ConfigurationListener listener) {
        return getConfig(key, group, listener, 0L);
    }

    @Override
    public String getConfig(String key, String group, ConfigurationListener listener, long timeout) {
        try {
            if (listener != null) {
                this.addListener(key, listener);
            }
            return getTargetConfig(key, group, timeout);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void removeListener(String key) {

    }

    protected abstract void recover();

    /**
     * Fetch dynamic configuration from backend config storage. If timeout exceeds, exception should be thrown.
     *
     * @param key     property key
     * @param group   group
     * @param timeout timeout
     * @return target config value
     */
    protected abstract String getTargetConfig(String key, String group, long timeout);

    /**
     * Register a native listener to the backend config storage so that Dubbo has chance to get notified when the
     * value changes.
     * @param key
     * @param targetListener Implementation dependent listener, such as, zookeeper watcher, Apollo listener, ...
     * @param configurationListener Listener in Dubbo that will handle notification.
     */
    protected abstract void addConfigurationListener(String key, TargetListener targetListener, ConfigurationListener configurationListener);

    /**
     * Create a native listener for the backend config storage, eventually ConfigurationListener will get notified once
     * the value changes.
     *
     * @param key      property key the native listener will listen on
     * @return native listener for the backend config storage
     */
    protected abstract TargetListener createTargetListener(String key);

}
