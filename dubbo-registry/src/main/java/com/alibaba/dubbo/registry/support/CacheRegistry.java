/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.registry.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;

/**
 * CacheRegistry
 * 
 * @author william.liangf
 */
public abstract class CacheRegistry extends FailbackRegistry {

    private final ConcurrentMap<String, Set<String>> notified = new ConcurrentHashMap<String, Set<String>>();

    public CacheRegistry(URL url) {
        super(url);
    }

    protected void registered(URL url) {
        for (Map.Entry<String, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            String key = entry.getKey();
            URL subscribe = URL.valueOf(key);
            if (UrlUtils.isMatch(subscribe, url)) {
                Set<String> urls = notified.get(key);
                if (urls == null) {
                    notified.putIfAbsent(key, new ConcurrentHashSet<String>());
                    urls = notified.get(key);
                }
                urls.add(url.toFullString());
                List<URL> list = toList(urls);
                if (list != null && list.size() > 0) {
                    for (NotifyListener listener : entry.getValue()) {
                        notify(subscribe, listener, list);
                        synchronized (listener) {
                            listener.notify();
                        }
                    }
                }
            }
        }
    }

    protected void unregistered(URL url) {
        for (Map.Entry<String, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            String key = entry.getKey();
            URL subscribe = URL.valueOf(key);
            if (UrlUtils.isMatch(subscribe, url)) {
                Set<String> urls = notified.get(key);
                if (urls != null) {
                    urls.remove(url.toFullString());
                }
                List<URL> list = toList(urls);
                if (list != null && list.size() > 0) {
                    for (NotifyListener listener : entry.getValue()) {
                        notify(subscribe, listener, list);
                    }
                }
            }
        }
    }

    public List<URL> lookup(URL url) {
        List<URL> urls= new ArrayList<URL>();
        for (String r: getRegistered()) {
            URL u = URL.valueOf(r);
            if (UrlUtils.isMatch(url, u)) {
                urls.add(u);
            }
        }
        return urls;
    }

    protected void subscribed(URL url, NotifyListener listener) {
        List<URL> urls = lookup(url);
        if (urls != null && urls.size() > 0) {
            notify(url, listener, urls);
        }
    }

    private List<URL> toList(Set<String> urls) {
        List<URL> list = new ArrayList<URL>();
        if (urls != null && urls.size() > 0) {
            for (String url : urls) {
                list.add(URL.valueOf(url));
            }
        }
        return list;
    }

    public void register(URL url) {
        super.register(url);
        registered(url);
    }

    public void unregister(URL url) {
        super.unregister(url);
        unregistered(url);
    }

    public void subscribe(URL url, NotifyListener listener) {
        super.subscribe(url, listener);
        subscribed(url, listener);
    }

    public void unsubscribe(URL url, NotifyListener listener) {
        super.unsubscribe(url, listener);
        notified.remove(url.toFullString());
    }

    public Map<String, Set<String>> getNotified() {
        return notified;
    }

}