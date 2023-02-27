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
package org.apache.dubbo.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceAddressURL;
import org.apache.dubbo.common.url.component.URLAddress;
import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.support.CacheableFailbackRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 *
 */
public class MockCacheableRegistryImpl extends CacheableFailbackRegistry {

    private final List<String> children = new ArrayList<>();
    NotifyListener listener;

    public MockCacheableRegistryImpl(URL url) {
        super(url);
    }

    @Override
    public int getDelay() {
        return 0;
    }

    @Override
    protected boolean isMatch(URL subscribeUrl, URL providerUrl) {
        return UrlUtils.isMatch(subscribeUrl, providerUrl);
    }

    @Override
    public void doRegister(URL url) {

    }

    @Override
    public void doUnregister(URL url) {

    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        List<URL> res = toUrlsWithoutEmpty(url, children);
        Semaphore semaphore = getSemaphore();
        while (semaphore.availablePermits() != 1) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        listener.notify(res);
        this.listener = listener;
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        super.doUnsubscribe(url, listener);
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    public void addChildren(URL url) {
        children.add(URL.encode(url.toFullString()));
    }

    public void removeChildren(URL url) {
        children.remove(URL.encode(url.toFullString()));
        if (listener != null) {
            listener.notify(toUrlsWithEmpty(getUrl(), "providers", children));
        }
    }

    public List<String> getChildren() {
        return children;
    }

    public void clearChildren() {
        children.clear();
        if (listener != null) {
            listener.notify(toUrlsWithEmpty(getUrl(), "providers", children));
        }
    }

    public Map<URL, Map<String, ServiceAddressURL>> getStringUrls() {
        return stringUrls;
    }

    public Map<String, URLAddress> getStringAddress() {
        return stringAddress;
    }

    public Map<String, URLParam> getStringParam() {
        return stringParam;
    }
}
