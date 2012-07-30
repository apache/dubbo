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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.RegistryService;

/**
 * AbstractRegistryService
 * 
 * @author william.liangf
 */
public abstract class AbstractRegistryService implements RegistryService {

    // 日志输出
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // 已注册的服务
    // Map<serviceName, Map<url, queryString>>
    private final ConcurrentMap<String, List<URL>> registered = new ConcurrentHashMap<String, List<URL>>();

    // 已订阅的服务
    // Map<serviceName, queryString>
    private final ConcurrentMap<String, Map<String, String>> subscribed = new ConcurrentHashMap<String, Map<String, String>>();

    // 已通知的服务
    // Map<serviceName, Map<url, queryString>>
    private final ConcurrentMap<String, List<URL>> notified = new ConcurrentHashMap<String, List<URL>>();
    private final ConcurrentMap<String, List<URL>> overrided = new ConcurrentHashMap<String, List<URL>>();
    private final ConcurrentMap<String, List<URL>> routed = new ConcurrentHashMap<String, List<URL>>();
    
    // 已订阅服务的监听器列表
    // Map<serviceName, List<notificationListener>>
    private final ConcurrentMap<String, List<NotifyListener>> notifyListeners = new ConcurrentHashMap<String, List<NotifyListener>>();
    
    public void register(URL url, NotifyListener listener) {
        if (logger.isInfoEnabled()) {
            logger.info("Register service: " + url.getServiceKey() + ",url:" + url);
        }
        register(url.getServiceKey(), url, listener);
    }

    public void unregister(URL url, NotifyListener listener) {
        if (logger.isInfoEnabled()) {
            logger.info("Unregister service: " + url.getServiceKey() + ",url:" + url);
        }
        unregister(url.getServiceKey(), url, listener);
    }

    public void subscribe(URL url, NotifyListener listener) {
        if (logger.isInfoEnabled()) {
            logger.info("Subscribe service: " + url.getServiceKey() + ",url:" + url);
        }
        subscribe(url.getServiceKey(), url, listener);
    }
    
    public void unsubscribe(URL url, NotifyListener listener) {
        if (logger.isInfoEnabled()) {
            logger.info("Unsubscribe service: " + url.getServiceKey() + ",url:" + url);
        }
        unsubscribe(url.getServiceKey(), url, listener);
    }

    public List<URL> lookup(URL url) {
        return getRegistered(url.getServiceKey());
    }

    public void register(String service, URL url, NotifyListener listener) {
        if (service == null) {
            throw new IllegalArgumentException("service == null");
        }
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        List<URL> urls = registered.get(service);
        if (urls == null) {
            registered.putIfAbsent(service, new CopyOnWriteArrayList<URL>());
            urls = registered.get(service);
        }
        if (! urls.contains(url)) {
            urls.add(url);
        }
        addListener(service, listener);
    }
    
    public void unregister(String service, URL url, NotifyListener listener) {
        if (service == null) {
            throw new IllegalArgumentException("service == null");
        }
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        List<URL> urls = registered.get(service);
        if (urls != null) {
            URL deleteURL = null;
            for (URL u : urls) {
                if (u.toIdentityString().equals(url.toIdentityString())) {
                    deleteURL = u;
                    break;
                }
            }
            if (deleteURL != null) {
                urls.remove(deleteURL);
            }
        }
        removeListener(service, listener);
    }
    
    public void subscribe(String service, URL url, NotifyListener listener) {
        if (service == null) {
            throw new IllegalArgumentException("service == null");
        }
        if (url == null) {
            throw new IllegalArgumentException("parameters == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener == null");
        }
        subscribed.put(service, url.getParameters()); 
        addListener(service, listener);
    }

    public void unsubscribe(String service, URL url, NotifyListener listener) {
        if (service == null) {
            throw new IllegalArgumentException("service == null");
        }
        if (url == null) {
            throw new IllegalArgumentException("parameters == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener == null");
        }
        subscribed.remove(service);
        removeListener(service, listener);
    }
    
    //consumer 与 provider的 listener可以一起存储,都是根据服务名称共享
    private void addListener(final String service, final NotifyListener listener){
        if (listener == null) {
            return;
        }
        List<NotifyListener> listeners = notifyListeners.get(service);
        if (listeners == null) {
            notifyListeners.putIfAbsent(service, new CopyOnWriteArrayList<NotifyListener>());
            listeners = notifyListeners.get(service);
        }
        if (listeners != null && !listeners.contains(listener)){
            listeners.add(listener);
        }
    }
    
    private void removeListener(final String service, final NotifyListener listener){
        if (listener == null) {
            return;
        }
        List<NotifyListener> listeners = notifyListeners.get(service);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }
    
    void addNotified(String service, List<URL> urls) {
        List<URL> providers = null;
        List<URL> overrides = null;
        List<URL> routes = null;
        for (URL url : urls) {
            if (Constants.OVERRIDE_PROTOCOL.equals(url.getProtocol())) {
                if (overrides == null) {
                    overrides = new ArrayList<URL>();
                }
                overrides.add(url);
            } else if (Constants.ROUTE_PROTOCOL.equals(url.getProtocol())) {
                if (routes == null) {
                    routes = new ArrayList<URL>();
                }
                routes.add(url);
            } else {
                if (providers == null) {
                    providers = new ArrayList<URL>();
                }
                providers.add(url);
            }
        }
        if (providers != null) {
            notified.put(service, providers);
        }
        if (overrides != null) {
            overrided.put(service, overrides);
        }
        if (routes != null) {
            routed.put(service, routes);
        }
    }
    
    private void doNotify(String service, List<URL> urls) {
        addNotified(service, urls);
        List<NotifyListener> listeners = notifyListeners.get(service);
        if (listeners != null) {
            for (NotifyListener listener : listeners) {
                try {
                    notify(service, urls, listener);
                } catch (Throwable t) {
                    logger.error("Failed to notify registry event, service: " + service + ", urls: " +  urls + ", cause: " + t.getMessage(), t);
                }
            }
        }
    }
    
    protected void notify(String service, List<URL> urls, NotifyListener listener) {
        listener.notify(urls);
    }
    
    protected final void forbid(String service) {
        doNotify(service, new ArrayList<URL>(0));
    }

    protected final void notify(String service, List<URL> urls) {
        if (service == null || service.length() == 0
                || urls == null || urls.size() == 0) {
            return;
        }
        doNotify(service, urls);
    }
    
    public Map<String, List<URL>> getRegistered() {
        return Collections.unmodifiableMap(registered);
    }
    
    public List<URL> getRegistered(String service) {
        return Collections.unmodifiableList(registered.get(service));
    }
    
    public Map<String, Map<String, String>> getSubscribed() {
        return Collections.unmodifiableMap(subscribed);
    }
    
    public Map<String, String> getSubscribed(String service) {
        return subscribed.get(service);
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, List<URL>> getNotified() {
        Map<String, List<URL>> map = new HashMap<String, List<URL>>();
        for (Map<String, List<URL>> cache : new Map[] {notified, overrided, routed}) {
            for (Map.Entry<String, List<URL>> entry : cache.entrySet()) {
                String service = entry.getKey();
                List<URL> urls = map.get(service);
                if (urls == null) {
                    urls = new ArrayList<URL>();
                    map.put(service, urls);
                }
                urls.addAll(entry.getValue());
            }
        }
        
        return Collections.unmodifiableMap(map);
    }
    
    public List<URL> getNotified(String service) {
        List<URL> urls = new ArrayList<URL>();
        List<URL> providers = notified.get(service);
        if (providers != null) {
            urls.addAll(providers);
        }
        List<URL> overrides = overrided.get(service);
        if (overrides != null) {
            urls.addAll(overrides);
        }
        List<URL> routes = routed.get(service);
        if (routes != null) {
            urls.addAll(routes);
        }
        return urls.size() == 0 ? null : Collections.unmodifiableList(urls);
    }
    
    public Map<String, List<NotifyListener>> getListeners() {
        return Collections.unmodifiableMap(notifyListeners);
    }

}