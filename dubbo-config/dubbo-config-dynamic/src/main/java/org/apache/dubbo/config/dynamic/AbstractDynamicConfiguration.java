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
package org.apache.dubbo.config.dynamic;

import org.apache.dubbo.common.URL;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public abstract class AbstractDynamicConfiguration<TargetConfigListener> implements DynamicConfiguration {
    protected URL url;
    /**
     * One key can register multiple target listeners, but one target listener only maps to one configuration listener
     */
    private ConcurrentMap<String, ConcurrentMap<ConfigurationListener, TargetConfigListener>> listenerToTargetListenerMap = new ConcurrentHashMap<>();

    public AbstractDynamicConfiguration() {
    }

    @Override
    public void addListener(String key, ConfigurationListener listener) {
        ConcurrentMap<ConfigurationListener, TargetConfigListener> listeners = listenerToTargetListenerMap.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        TargetConfigListener targetListener = listeners.computeIfAbsent(listener, k -> createTargetConfigListener(key, listener));
        addTargetListener(key, targetListener);
    }

    @Override
    public String getConfig(String key, String group) {
        return getConfig(key, group, null);
    }

    @Override
    public String getConfig(String key, String group, ConfigurationListener listener) {
        return getConfig(key, group, 0l, listener);
    }

    @Override
    public String getConfig(String key, String group, long timeout, ConfigurationListener listener) {
        try {
            if (listener != null) {
                this.addListener(key, listener);
            }
            return getInternalProperty(key, group, timeout, listener);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    protected abstract String getInternalProperty(String key, String group, long timeout, ConfigurationListener listener);

    protected abstract void addTargetListener(String key, TargetConfigListener listener);

    protected abstract TargetConfigListener createTargetConfigListener(String key, ConfigurationListener listener);

}
