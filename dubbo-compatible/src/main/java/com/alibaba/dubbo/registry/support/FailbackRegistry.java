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

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 2019-04-17
 */
@Deprecated
public abstract class FailbackRegistry implements org.apache.dubbo.registry.Registry, Registry {

    private CompatibleFailbackRegistry failbackRegistry;

    public FailbackRegistry(URL url) {
        failbackRegistry = new CompatibleFailbackRegistry(url.getOriginalURL(), this);
    }

    public void removeFailedRegisteredTask(URL url) {
        failbackRegistry.removeFailedRegisteredTask(url.getOriginalURL());
    }

    public void removeFailedUnregisteredTask(URL url) {
        failbackRegistry.removeFailedUnregisteredTask(url.getOriginalURL());
    }

    public void removeFailedSubscribedTask(URL url, NotifyListener listener) {
        failbackRegistry.removeFailedSubscribedTask(url.getOriginalURL(), new NotifyListener.ReverseCompatibleNotifyListener(listener));
    }

    public void removeFailedUnsubscribedTask(URL url, NotifyListener listener) {
        failbackRegistry.removeFailedUnsubscribedTask(url.getOriginalURL(), new NotifyListener.ReverseCompatibleNotifyListener(listener));
    }

    public void removeFailedNotifiedTask(URL url, NotifyListener listener) {
        failbackRegistry.removeFailedNotifiedTask(url.getOriginalURL(), new NotifyListener.ReverseCompatibleNotifyListener(listener));
    }

    @Override
    public void register(URL url) {
        failbackRegistry.register(url.getOriginalURL());
    }

    @Override
    public void unregister(URL url) {
        failbackRegistry.unregister(url.getOriginalURL());
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        failbackRegistry.subscribe(url.getOriginalURL(), new com.alibaba.dubbo.registry.NotifyListener.ReverseCompatibleNotifyListener(listener));
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        failbackRegistry.unsubscribe(url.getOriginalURL(), new com.alibaba.dubbo.registry.NotifyListener.ReverseCompatibleNotifyListener(listener));
    }

    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        List<org.apache.dubbo.common.URL> urlResult = urls.stream().map(URL::getOriginalURL).collect(Collectors.toList());
        failbackRegistry.notify(url.getOriginalURL(), new com.alibaba.dubbo.registry.NotifyListener.ReverseCompatibleNotifyListener(listener), urlResult);
    }

    protected void doNotify(URL url, NotifyListener listener, List<URL> urls) {
        List<org.apache.dubbo.common.URL> urlResult = urls.stream().map(URL::getOriginalURL).collect(Collectors.toList());
        failbackRegistry.doNotify(url.getOriginalURL(), new com.alibaba.dubbo.registry.NotifyListener.ReverseCompatibleNotifyListener(listener), urlResult);
    }

    protected void recover() throws Exception {
        failbackRegistry.recover();
    }

    @Override
    public List<URL> lookup(URL url) {
        return failbackRegistry.lookup(url.getOriginalURL()).stream().map(e -> new URL(e)).collect(Collectors.toList());
    }

    @Override
    public URL getUrl() {
        return new URL(failbackRegistry.getUrl());
    }

    @Override
    public void destroy() {
        failbackRegistry.destroy();
    }

    // ==== Template method ====

    public abstract void doRegister(URL url);

    public abstract void doUnregister(URL url);

    public abstract void doSubscribe(URL url, NotifyListener listener);

    public abstract void doUnsubscribe(URL url, NotifyListener listener);

    @Override
    public void register(org.apache.dubbo.common.URL url) {
        this.register(new URL(url));
    }

    @Override
    public void unregister(org.apache.dubbo.common.URL url) {
        this.unregister(new URL(url));
    }

    @Override
    public void subscribe(org.apache.dubbo.common.URL url, org.apache.dubbo.registry.NotifyListener listener) {
        this.subscribe(new URL(url), new NotifyListener.CompatibleNotifyListener(listener));
    }

    @Override
    public void unsubscribe(org.apache.dubbo.common.URL url, org.apache.dubbo.registry.NotifyListener listener) {
        this.unsubscribe(new URL(url), new NotifyListener.CompatibleNotifyListener(listener));
    }

    @Override
    public List<org.apache.dubbo.common.URL> lookup(org.apache.dubbo.common.URL url) {
        return failbackRegistry.lookup(url);
    }


    static class CompatibleFailbackRegistry extends org.apache.dubbo.registry.support.FailbackRegistry {

        private FailbackRegistry compatibleFailbackRegistry;

        public CompatibleFailbackRegistry(org.apache.dubbo.common.URL url, FailbackRegistry compatibleFailbackRegistry) {
            super(url);
            this.compatibleFailbackRegistry = compatibleFailbackRegistry;
        }

        @Override
        public void doRegister(org.apache.dubbo.common.URL url) {
            this.compatibleFailbackRegistry.doRegister(new URL(url));
        }

        @Override
        public void doUnregister(org.apache.dubbo.common.URL url) {
            this.compatibleFailbackRegistry.doUnregister(new URL(url));
        }

        @Override
        public void doSubscribe(org.apache.dubbo.common.URL url, org.apache.dubbo.registry.NotifyListener listener) {
            this.compatibleFailbackRegistry.doSubscribe(new URL(url), new NotifyListener.CompatibleNotifyListener(listener));
        }

        @Override
        public void doUnsubscribe(org.apache.dubbo.common.URL url, org.apache.dubbo.registry.NotifyListener listener) {
            this.compatibleFailbackRegistry.doUnsubscribe(new URL(url), new NotifyListener.CompatibleNotifyListener(listener));
        }

        @Override
        public void notify(org.apache.dubbo.common.URL url, org.apache.dubbo.registry.NotifyListener listener, List<org.apache.dubbo.common.URL> urls) {
            super.notify(url, listener, urls);
        }

        @Override
        public void doNotify(org.apache.dubbo.common.URL url, org.apache.dubbo.registry.NotifyListener listener, List<org.apache.dubbo.common.URL> urls) {
            super.doNotify(url, listener, urls);
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void recover() throws Exception {
            super.recover();
        }
    }

}
