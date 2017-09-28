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
package com.alibaba.dubbo.config.spring;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.RegistryService;
import com.alibaba.dubbo.rpc.RpcContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SimpleRegistryService
 *
 * @author william.liangf
 */
public class SimpleRegistryService extends AbstractRegistryService {

    private final static Logger logger = LoggerFactory.getLogger(SimpleRegistryService.class);
    private final ConcurrentMap<String, ConcurrentMap<String, URL>> remoteRegistered = new ConcurrentHashMap<String, ConcurrentMap<String, URL>>();
    private final ConcurrentMap<String, ConcurrentMap<String, NotifyListener>> remoteListeners = new ConcurrentHashMap<String, ConcurrentMap<String, NotifyListener>>();
    private List<String> registries;

    @Override
    public void register(String service, URL url) {
        super.register(service, url);
        String client = RpcContext.getContext().getRemoteAddressString();
        Map<String, URL> urls = remoteRegistered.get(client);
        if (urls == null) {
            remoteRegistered.putIfAbsent(client, new ConcurrentHashMap<String, URL>());
            urls = remoteRegistered.get(client);
        }
        urls.put(service, url);
        notify(service, getRegistered().get(service));
    }

    @Override
    public void unregister(String service, URL url) {
        super.unregister(service, url);
        String client = RpcContext.getContext().getRemoteAddressString();
        Map<String, URL> urls = remoteRegistered.get(client);
        if (urls != null && urls.size() > 0) {
            urls.remove(service);
        }
        notify(service, getRegistered().get(service));
    }

    @Override
    public void subscribe(String service, URL url, NotifyListener listener) {
        String client = RpcContext.getContext().getRemoteAddressString();
        if (logger.isInfoEnabled()) {
            logger.info("[subscribe] service: " + service + ",client:" + client);
        }
        List<URL> urls = getRegistered().get(service);
        if ((RegistryService.class.getName() + ":0.0.0").equals(service)
                && (urls == null || urls.size() == 0)) {
            register(service, new URL("dubbo",
                    NetUtils.getLocalHost(),
                    RpcContext.getContext().getLocalPort(),
                    com.alibaba.dubbo.registry.RegistryService.class.getName(),
                    url.getParameters()));
            List<String> rs = registries;
            if (rs != null && rs.size() > 0) {
                for (String registry : rs) {
                    register(service, UrlUtils.parseURL(registry, url.getParameters()));
                }
            }
        }
        super.subscribe(service, url, listener);

        Map<String, NotifyListener> listeners = remoteListeners.get(client);
        if (listeners == null) {
            remoteListeners.putIfAbsent(client, new ConcurrentHashMap<String, NotifyListener>());
            listeners = remoteListeners.get(client);
        }
        listeners.put(service, listener);
        urls = getRegistered().get(service);
        if (urls != null && urls.size() > 0) {
            listener.notify(urls);
        }


    }

    @Override
    public void unsubscribe(String service, URL url, NotifyListener listener) {
        super.unsubscribe(service, url, listener);
        String client = RpcContext.getContext().getRemoteAddressString();
        Map<String, NotifyListener> listeners = remoteListeners.get(client);
        if (listeners != null && listeners.size() > 0) {
            listeners.remove(service);
        }
        List<URL> urls = getRegistered().get(service);
        if (urls != null && urls.size() > 0) {
            listener.notify(urls);
        }
    }

    public void disconnect() {
        String client = RpcContext.getContext().getRemoteAddressString();
        if (logger.isInfoEnabled()) {
            logger.info("Disconnected " + client);
        }
        ConcurrentMap<String, URL> urls = remoteRegistered.get(client);
        if (urls != null && urls.size() > 0) {
            for (Map.Entry<String, URL> entry : urls.entrySet()) {
                super.unregister(entry.getKey(), entry.getValue());
            }
        }
        Map<String, NotifyListener> listeners = remoteListeners.get(client);
        if (listeners != null && listeners.size() > 0) {
            for (Map.Entry<String, NotifyListener> entry : listeners.entrySet()) {
                String service = entry.getKey();
                super.unsubscribe(service, new URL("subscribe",
                        RpcContext.getContext().getRemoteHost(),
                        RpcContext.getContext().getRemotePort(),
                        com.alibaba.dubbo.registry.RegistryService.class.getName(), getSubscribed(service)), entry.getValue());
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