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
package com.alibaba.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 2019-04-16
 */
@Deprecated
public abstract class AbstractRegistry implements Registry {

    private CompatibleAbstractRegistry abstractRegistry;

    public AbstractRegistry(com.alibaba.dubbo.common.URL url) {
        abstractRegistry = new CompatibleAbstractRegistry(url.getOriginalURL());
    }

    @Override
    public com.alibaba.dubbo.common.URL getUrl() {
        return new com.alibaba.dubbo.common.URL(abstractRegistry.getUrl());
    }

    protected void setUrl(com.alibaba.dubbo.common.URL url) {
        abstractRegistry.setUrl(url.getOriginalURL());
    }

    public Set<com.alibaba.dubbo.common.URL> getRegistered() {
        return abstractRegistry.getRegistered().stream().map(url -> new com.alibaba.dubbo.common.URL(url)).collect(Collectors.toSet());
    }

    public Map<com.alibaba.dubbo.common.URL, Set<com.alibaba.dubbo.registry.NotifyListener>> getSubscribed() {
        return abstractRegistry.getSubscribed().entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> new com.alibaba.dubbo.common.URL(entry.getKey()),
                        entry -> convertToNotifyListeners(entry.getValue())));
    }

    public Map<com.alibaba.dubbo.common.URL, Map<String, List<com.alibaba.dubbo.common.URL>>> getNotified() {
        return abstractRegistry.getNotified().entrySet().stream()
                .collect(Collectors.toMap(entry -> new com.alibaba.dubbo.common.URL(entry.getKey()),
                        entry -> {
                            return entry.getValue().entrySet()
                                    .stream()
                                    .collect(Collectors.toMap(e -> e.getKey(), e -> {
                                        return e.getValue().stream().map(url -> new com.alibaba.dubbo.common.URL(url)).collect(Collectors.toList());
                                    }));
                        }));
    }


    public List<com.alibaba.dubbo.common.URL> getCacheUrls(com.alibaba.dubbo.common.URL url) {
        return abstractRegistry.lookup(url.getOriginalURL()).stream().map(tmpUrl -> new com.alibaba.dubbo.common.URL(tmpUrl)).collect(Collectors.toList());
    }

    public List<com.alibaba.dubbo.common.URL> lookup(com.alibaba.dubbo.common.URL url) {
        return abstractRegistry.lookup(url.getOriginalURL()).stream().map(tmpUrl -> new com.alibaba.dubbo.common.URL(tmpUrl)).collect(Collectors.toList());
    }

    protected void notify(com.alibaba.dubbo.common.URL url, com.alibaba.dubbo.registry.NotifyListener listener, List<com.alibaba.dubbo.common.URL> urls) {
        abstractRegistry.notify(url.getOriginalURL(), new com.alibaba.dubbo.registry.NotifyListener.ReverseCompatibleNotifyListener(listener), urls.stream().map(tmpUrl -> tmpUrl.getOriginalURL()).collect(Collectors.toList()));
    }

    public void register(com.alibaba.dubbo.common.URL url) {
        abstractRegistry.register(url.getOriginalURL());
    }

    public void unregister(com.alibaba.dubbo.common.URL url) {
        abstractRegistry.unregister(url.getOriginalURL());
    }

    public void subscribe(com.alibaba.dubbo.common.URL url, com.alibaba.dubbo.registry.NotifyListener listener) {
        abstractRegistry.subscribe(url.getOriginalURL(), new com.alibaba.dubbo.registry.NotifyListener.ReverseCompatibleNotifyListener(listener));
    }

    public void unsubscribe(com.alibaba.dubbo.common.URL url, com.alibaba.dubbo.registry.NotifyListener listener) {
        abstractRegistry.unsubscribe(url.getOriginalURL(), new com.alibaba.dubbo.registry.NotifyListener.ReverseCompatibleNotifyListener(listener));
    }


    @Override
    public void register(URL url) {
        this.register(new com.alibaba.dubbo.common.URL(url));
    }

    @Override
    public void unregister(URL url) {
        this.unregister(new com.alibaba.dubbo.common.URL(url));
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        this.subscribe(new com.alibaba.dubbo.common.URL(url), new com.alibaba.dubbo.registry.NotifyListener.CompatibleNotifyListener(listener));
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        this.unsubscribe(new com.alibaba.dubbo.common.URL(url), new com.alibaba.dubbo.registry.NotifyListener.CompatibleNotifyListener(listener));
    }

    final Set<com.alibaba.dubbo.registry.NotifyListener> convertToNotifyListeners(Set<NotifyListener> notifyListeners) {
        return notifyListeners.stream().map(listener -> new com.alibaba.dubbo.registry.NotifyListener.CompatibleNotifyListener(listener)).collect(Collectors.toSet());
    }


    static class CompatibleAbstractRegistry extends org.apache.dubbo.registry.support.AbstractRegistry {
        public CompatibleAbstractRegistry(URL url) {
            super(url);
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        public void notify(URL url, NotifyListener listener, List<URL> urls) {
            super.notify(url, listener, urls);
        }

        public void setUrl(URL url) {
            super.setUrl(url);
        }
    }
}
