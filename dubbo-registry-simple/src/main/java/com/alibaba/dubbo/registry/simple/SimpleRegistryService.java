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
package com.alibaba.dubbo.registry.simple;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.RegistryService;
import com.alibaba.dubbo.registry.support.CacheRegistry;
import com.alibaba.dubbo.rpc.RpcContext;

/**
 * DubboRegistryService
 * 
 * @author william.liangf
 */
public class SimpleRegistryService extends CacheRegistry {

    private final ConcurrentMap<String, Set<String>> remoteRegistered = new ConcurrentHashMap<String, Set<String>>();

    private final ConcurrentMap<String, ConcurrentMap<String, Set<NotifyListener>>> remoteSubscribed = new ConcurrentHashMap<String, ConcurrentMap<String, Set<NotifyListener>>>();
    
    private final static Logger logger = LoggerFactory.getLogger(SimpleRegistryService.class);

    private List<String> registries;
    
    public SimpleRegistryService() {
        this(0);
    }
    
    public SimpleRegistryService(int port) {
        super(new URL("dubbo", NetUtils.getLocalHost(), port, RegistryService.class.getName()));
    }
    
    public boolean isAvailable() {
        return true;
    }
    
    public void register(URL url) {
        String client = RpcContext.getContext().getRemoteAddressString();
        Set<String> urls = remoteRegistered.get(client);
        if (urls == null) {
            remoteRegistered.putIfAbsent(client, new ConcurrentHashSet<String>());
            urls = remoteRegistered.get(client);
        }
        urls.add(url.toFullString());
        super.register(url);
    }

    public void doRegister(URL url) {
        registered(url);
    }
    
    public void unregister(URL url) {
        String client = RpcContext.getContext().getRemoteAddressString();
        Set<String> urls = remoteRegistered.get(client);
        if (urls != null && urls.size() > 0) {
            urls.remove(url.toFullString());
        }
        super.unregister(url);
    }

    public void doUnregister(URL url) {
        unregistered(url);
    }
    
    public void subscribe(URL url, NotifyListener listener) {
        if (! Constants.ANY_VALUE.equals(url.getServiceName())
                && url.getParameter(Constants.REGISTER_KEY, true)) {
            register(url);
        }
        String client = RpcContext.getContext().getRemoteAddressString();
        List<URL> urls = lookup(url);
        if ((RegistryService.class.getName()).equals(url.getServiceName())
                && (urls == null || urls.size() == 0)) {
            register(new URL("dubbo", 
                    NetUtils.getLocalHost(), 
                    RpcContext.getContext().getLocalPort(), 
                    com.alibaba.dubbo.registry.RegistryService.class.getName(), 
                    url.getParameters()));
            List<String> rs = registries;
            if (rs != null && rs.size() > 0) {
                for (String registry : rs) {
                    register(UrlUtils.parseURL(registry, url.getParameters()));
                }
            }
        }
        ConcurrentMap<String, Set<NotifyListener>> clientListeners = remoteSubscribed.get(client);
        if (clientListeners == null) {
            remoteSubscribed.putIfAbsent(client, new ConcurrentHashMap<String, Set<NotifyListener>>());
            clientListeners = remoteSubscribed.get(client);
        }
        String key = url.toFullString();
        Set<NotifyListener> listeners = clientListeners.get(key);
        if (listeners == null) {
            clientListeners.putIfAbsent(key, new ConcurrentHashSet<NotifyListener>());
            listeners = clientListeners.get(key);
        }
        listeners.add(listener);
        super.subscribe(url, listener);
    }

    public void doSubscribe(URL url, NotifyListener listener) {
        subscribed(url, listener);
    }
    
    public void unsubscribe(URL url, NotifyListener listener) {
        if (! Constants.ANY_VALUE.equals(url.getServiceName())
                && url.getParameter(Constants.REGISTER_KEY, true)) {
            unregister(url);
        }
        String client = RpcContext.getContext().getRemoteAddressString();
        Map<String, Set<NotifyListener>> clientListeners = remoteSubscribed.get(client);
        if (clientListeners != null && clientListeners.size() > 0) {
            String key = url.toFullString();
            Set<NotifyListener> listeners = clientListeners.get(key);
            if (listeners != null && listeners.size() > 0) {
                listeners.remove(listener);
            }
        }
        super.unregister(url);
    }
    
    public void doUnsubscribe(URL url, NotifyListener listener) {
    }
    
    public void disconnect() {
        String client = RpcContext.getContext().getRemoteAddressString();
        if (logger.isInfoEnabled()) {
            logger.info("Disconnected " + client);
        }
        Set<String> urls = remoteRegistered.get(client);
        if (urls != null && urls.size() > 0) {
            for (String url : urls) {
                unregister(URL.valueOf(url));
            }
        }
        Map<String, Set<NotifyListener>> listeners = remoteSubscribed.get(client);
        if (listeners != null && listeners.size() > 0) {
            for (Map.Entry<String, Set<NotifyListener>> entry : listeners.entrySet()) {
                String url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    unsubscribe(URL.valueOf(url), listener);
                }
            }
        }
    }

    public List<String> getRegistries() {
        return registries;
    }

    public void setRegistries(List<String> registries) {
        this.registries = registries;
    }

}
