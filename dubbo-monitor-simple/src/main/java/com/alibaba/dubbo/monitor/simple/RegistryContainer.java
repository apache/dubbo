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
package com.alibaba.dubbo.monitor.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.container.Container;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;

/**
 * RegistryContainer
 * 
 * @author william.liangf
 */
@Extension("registry")
public class RegistryContainer implements Container {

    private static final Logger logger = LoggerFactory.getLogger(RegistryContainer.class);

    public static final String REGISTRY_URL = "registry.url";

    private final RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();

    private final Set<String> services = new ConcurrentHashSet<String>();
    
    private final Map<String, List<URL>> providers = new ConcurrentHashMap<String, List<URL>>();

    private final Map<String, List<URL>> consumers = new ConcurrentHashMap<String, List<URL>>();

    private final Map<String, List<URL>> routes = new ConcurrentHashMap<String, List<URL>>();
    
    private Registry registry;
    
    private static RegistryContainer INSTANCE = null;
    
    public RegistryContainer() {
        INSTANCE = this;
    }
    
    public static RegistryContainer getInstance() {
        if (INSTANCE == null) {
            ExtensionLoader.getExtensionLoader(Container.class).getExtension("registry");
        }
        return INSTANCE;
    }

    public Registry getRegistry() {
        return registry;
    }

    public Set<String> getServices() {
        return Collections.unmodifiableSet(services);
    }

    public Map<String, List<URL>> getProviders() {
        return Collections.unmodifiableMap(providers);
    }

    public List<URL> getProviders(String service) {
        List<URL> urls = providers.get(service);
        return urls == null ? null : Collections.unmodifiableList(urls);
    }

    public Map<String, List<URL>> getConsumers() {
        return Collections.unmodifiableMap(consumers);
    }

    public List<URL> getConsumers(String service) {
        List<URL> urls = consumers.get(service);
        return urls == null ? null : Collections.unmodifiableList(urls);
    }

    public Map<String, List<URL>> getRoutes() {
        return Collections.unmodifiableMap(routes);
    }

    public List<URL> getRoutes(String service) {
        List<URL> urls = routes.get(service);
        return urls == null ? null : Collections.unmodifiableList(urls);
    }

    public void start() {
        String url = System.getProperty(REGISTRY_URL);
        if (url == null || url.length() == 0) {
            throw new IllegalArgumentException("Please set java start argument: -D" + REGISTRY_URL + "=zookeeper://127.0.0.1:2181");
        }
        URL registryUrl = URL.valueOf(url);
        registry = registryFactory.getRegistry(registryUrl);
        URL subscribeUrl = new URL(Constants.SUBSCRIBE_PROTOCOL, NetUtils.getLocalHost(), 0)
                        .addParameters(Constants.ADMIN_KEY, String.valueOf(true),
                                Constants.INTERFACE_KEY, Constants.ANY_VALUE, 
                                Constants.GROUP_KEY, Constants.ANY_VALUE, 
                                Constants.VERSION_KEY, Constants.ANY_VALUE);
        registry.subscribe(subscribeUrl, new NotifyListener() {
            public void notify(List<URL> urls) {
                if (urls == null || urls.size() == 0) {
                    return;
                }
                Set<String> notifiedServices = new HashSet<String>();
                Map<String, List<URL>> proivderMap = new HashMap<String, List<URL>>();
                Map<String, List<URL>> consumerMap = new HashMap<String, List<URL>>();
                Map<String, List<URL>> routeMap = new HashMap<String, List<URL>>();
                for (URL url : urls) {
                    String service = url.getServiceName();
                    notifiedServices.add(service);
                    services.add(service);
                    if (Constants.ROUTE_PROTOCOL.equals(url.getProtocol())) {
                        List<URL> list = routeMap.get(service);
                        if (list == null) {
                            list = new ArrayList<URL>();
                            routeMap.put(service, list);
                        }
                        list.add(url);
                    } else if (Constants.SUBSCRIBE_PROTOCOL.equals(url.getProtocol())) {
                        List<URL> list = consumerMap.get(service);
                        if (list == null) {
                            list = new ArrayList<URL>();
                            consumerMap.put(service, list);
                        }
                        list.add(url);
                    } else if (! Constants.EMPTY_PROTOCOL.equals(url.getProtocol())) {
                        List<URL> list = proivderMap.get(service);
                        if (list == null) {
                            list = new ArrayList<URL>();
                            proivderMap.put(service, list);
                        }
                        list.add(url);
                    }
                }
                if (proivderMap != null && proivderMap.size() > 0) {
                    providers.putAll(proivderMap);
                }
                if (consumerMap != null && consumerMap.size() > 0) {
                    consumers.putAll(consumerMap);
                }
                if (routeMap != null && routeMap.size() > 0) {
                    routes.putAll(routeMap);
                }
                for (String service : notifiedServices) {
                    if (! proivderMap.containsKey(service)) {
                        providers.remove(service);
                    }
                    if (! consumerMap.containsKey(service)) {
                        consumers.remove(service);
                    }
                    if (! routeMap.containsKey(service)) {
                        routes.remove(service);
                    }
                }
            }
        });
    }

    public void stop() {
        try {
            registry.destroy();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

}
