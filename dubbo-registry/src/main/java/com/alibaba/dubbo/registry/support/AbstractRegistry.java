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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;

/**
 * 嵌入式注册中心实现，不开端口，只是map进行存储查询.不需要显示声明
 * 
 * @author chao.liuc
 * @author william.liangf
 */
public abstract class AbstractRegistry implements Registry {

    // 日志输出
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final URL registryUrl;

    private final Set<String> registered = new ConcurrentHashSet<String>();

    private final ConcurrentMap<String, Set<NotifyListener>> subscribed = new ConcurrentHashMap<String, Set<NotifyListener>>();

    public AbstractRegistry(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("registry url == null");
        }
        this.registryUrl = url;
    }

    public Set<String> getRegistered() {
        return registered;
    }

    public Map<String, Set<NotifyListener>> getSubscribed() {
        return subscribed;
    }

    public URL getUrl() {
        return registryUrl;
    }

    public void register(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("register url == null");
        }
        if (logger.isInfoEnabled()){
            logger.info("Register: " + url);
        }
        registered.add(url.toFullString());
    }
    
    public void unregister(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("unregister url == null");
        }
        if (logger.isInfoEnabled()){
            logger.info("Unregister: " + url);
        }
        registered.remove(url.toFullString());
    }
    
    public void subscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("subscribe url == null");
        }
        if (logger.isInfoEnabled()){
            logger.info("Subscribe: " + url);
        }
        if (listener == null) {
            throw new IllegalArgumentException("subscribe listener == null");
        }
        String key = url.toFullString();
        Set<NotifyListener> listeners = subscribed.get(key);
        if (listeners == null) {
            subscribed.putIfAbsent(key, new ConcurrentHashSet<NotifyListener>());
            listeners = subscribed.get(key);
        }
        listeners.add(listener);
    }
    
    public void unsubscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("unsubscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("unsubscribe listener == null");
        }
        if (logger.isInfoEnabled()){
            logger.info("Unsubscribe: " + url);
        }
        String key = url.toFullString();
        Set<NotifyListener> listeners = subscribed.get(key);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }
    
    public void destroy() {
        if (logger.isInfoEnabled()){
            logger.info("Destroy registry: " + getUrl());
        }
        for (String url : new HashSet<String>(registered)) {
            try {
                unregister(URL.valueOf(url));
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
        }
    }
    
    public String toString() {
        return getUrl().toString();
    }

}