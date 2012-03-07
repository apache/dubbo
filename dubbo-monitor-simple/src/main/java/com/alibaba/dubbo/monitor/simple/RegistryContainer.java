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
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.ConfigUtils;
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
public class RegistryContainer implements Container {

    private static final Logger logger = LoggerFactory.getLogger(RegistryContainer.class);

    public static final String REGISTRY_ADDRESS = "dubbo.registry.address";

    private final RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();

    private final Set<String> applications = new ConcurrentHashSet<String>();

    private final Map<String, Set<String>> providerServiceApplications = new ConcurrentHashMap<String, Set<String>>();

    private final Map<String, Set<String>> providerApplicationServices = new ConcurrentHashMap<String, Set<String>>();

    private final Map<String, Set<String>> consumerServiceApplications = new ConcurrentHashMap<String, Set<String>>();

    private final Map<String, Set<String>> consumerApplicationServices = new ConcurrentHashMap<String, Set<String>>();

    private final Set<String> services = new ConcurrentHashSet<String>();

    private final Map<String, List<URL>> serviceProviders = new ConcurrentHashMap<String, List<URL>>();

    private final Map<String, List<URL>> serviceConsumers = new ConcurrentHashMap<String, List<URL>>();

    private final Map<String, List<URL>> serviceRoutes = new ConcurrentHashMap<String, List<URL>>();

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

    public Set<String> getApplications() {
        return Collections.unmodifiableSet(applications);
    }
    
    public Set<String> getDependencies(String application, boolean reverse) {
        if (reverse) {
            Set<String> dependencies = new HashSet<String>();
            Set<String> services = providerApplicationServices.get(application);
            if (services != null && services.size() > 0) {
                for (String service : services) {
                    Set<String> applications = consumerServiceApplications.get(service);
                    if (applications != null && applications.size() > 0) {
                        dependencies.addAll(applications);
                    }
                }
            }
            return dependencies;
        } else {
            Set<String> dependencies = new HashSet<String>();
            Set<String> services = consumerApplicationServices.get(application);
            if (services != null && services.size() > 0) {
                for (String service : services) {
                    Set<String> applications = providerServiceApplications.get(service);
                    if (applications != null && applications.size() > 0) {
                        dependencies.addAll(applications);
                    }
                }
            }
            return dependencies;
        }
    }

    public Set<String> getServices() {
        return Collections.unmodifiableSet(services);
    }

    public Map<String, List<URL>> getServiceProviders() {
        return Collections.unmodifiableMap(serviceProviders);
    }

    public List<URL> getProvidersByService(String service) {
        List<URL> urls = serviceProviders.get(service);
        return urls == null ? null : Collections.unmodifiableList(urls);
    }

    public List<URL> getProvidersByHost(String host) {
        List<URL> urls = new ArrayList<URL>();
        if (host != null && host.length() > 0) {
            for (List<URL> providers : serviceProviders.values()) {
                for (URL url : providers) {
                    if (host.equals(url.getHost())) {
                        urls.add(url);
                    }
                }
            }
        }
        return urls;
    }

    public List<URL> getProvidersByApplication(String application) {
        List<URL> urls = new ArrayList<URL>();
        if (application != null && application.length() > 0) {
            for (List<URL> providers : serviceProviders.values()) {
                for (URL url : providers) {
                    if (application.equals(url.getParameter(Constants.APPLICATION_KEY))) {
                        urls.add(url);
                    }
                }
            }
        }
        return urls;
    }

    public Set<String> getHosts() {
        Set<String> addresses = new HashSet<String>();
        for (List<URL> providers : serviceProviders.values()) {
            for (URL url : providers) {
                addresses.add(url.getHost());
            }
        }
        for (List<URL> providers : serviceConsumers.values()) {
            for (URL url : providers) {
                addresses.add(url.getHost());
            }
        }
        return addresses;
    }

    public Map<String, List<URL>> getServiceConsumers() {
        return Collections.unmodifiableMap(serviceConsumers);
    }

    public List<URL> getConsumersByService(String service) {
        List<URL> urls = serviceConsumers.get(service);
        return urls == null ? null : Collections.unmodifiableList(urls);
    }

    public List<URL> getConsumersByHost(String host) {
        List<URL> urls = new ArrayList<URL>();
        if (host != null && host.length() > 0) {
            for (List<URL> consumers : serviceConsumers.values()) {
                for (URL url : consumers) {
                    if (host.equals(url.getHost())) {
                        urls.add(url);
                    }
                }
            }
        }
        return Collections.unmodifiableList(urls);
    }

    public List<URL> getConsumersByApplication(String application) {
        List<URL> urls = new ArrayList<URL>();
        if (application != null && application.length() > 0) {
            for (List<URL> consumers : serviceConsumers.values()) {
                for (URL url : consumers) {
                    if (application.equals(url.getParameter(Constants.APPLICATION_KEY))) {
                        urls.add(url);
                    }
                }
            }
        }
        return urls;
    }

    public Map<String, List<URL>> getServiceRoutes() {
        return Collections.unmodifiableMap(serviceRoutes);
    }

    public List<URL> getRoutesByService(String service) {
        List<URL> urls = serviceRoutes.get(service);
        return urls == null ? null : Collections.unmodifiableList(urls);
    }

    public void start() {
        String url = ConfigUtils.getProperty(REGISTRY_ADDRESS);
        if (url == null || url.length() == 0) {
            throw new IllegalArgumentException("Please set java start argument: -D" + REGISTRY_ADDRESS + "=zookeeper://127.0.0.1:2181");
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
                    String application = url.getParameter(Constants.APPLICATION_KEY);
                    if (application != null && application.length() > 0) {
                        applications.add(application);
                    }
                    String service = url.getServiceInterface();
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

                        if (application != null && application.length() > 0) {
                            Set<String> serviceApplications = consumerServiceApplications.get(service);
                            if (serviceApplications == null) {
                                consumerServiceApplications.put(service, new ConcurrentHashSet<String>());
                                serviceApplications = consumerServiceApplications.get(service);
                            }
                            serviceApplications.add(application);
    
                            Set<String> applicationServices = consumerApplicationServices.get(application);
                            if (applicationServices == null) {
                                consumerApplicationServices.put(application, new ConcurrentHashSet<String>());
                                applicationServices = consumerApplicationServices.get(application);
                            }
                            applicationServices.add(service);
                        }
                    } else if (! Constants.EMPTY_PROTOCOL.equals(url.getProtocol())) {
                        List<URL> list = proivderMap.get(service);
                        if (list == null) {
                            list = new ArrayList<URL>();
                            proivderMap.put(service, list);
                        }
                        list.add(url);

                        if (application != null && application.length() > 0) {
                            Set<String> serviceApplications = providerServiceApplications.get(service);
                            if (serviceApplications == null) {
                                providerServiceApplications.put(service, new ConcurrentHashSet<String>());
                                serviceApplications = providerServiceApplications.get(service);
                            }
                            serviceApplications.add(application);
    
                            Set<String> applicationServices = providerApplicationServices.get(application);
                            if (applicationServices == null) {
                                providerApplicationServices.put(application, new ConcurrentHashSet<String>());
                                applicationServices = providerApplicationServices.get(application);
                            }
                            applicationServices.add(service);
                        }
                    }
                }
                if (proivderMap != null && proivderMap.size() > 0) {
                    serviceProviders.putAll(proivderMap);
                }
                if (consumerMap != null && consumerMap.size() > 0) {
                    serviceConsumers.putAll(consumerMap);
                }
                if (routeMap != null && routeMap.size() > 0) {
                    serviceRoutes.putAll(routeMap);
                }
                for (String service : notifiedServices) {
                    if (! proivderMap.containsKey(service)) {
                        serviceProviders.remove(service);
                    }
                    if (! consumerMap.containsKey(service)) {
                        serviceConsumers.remove(service);
                    }
                    if (! routeMap.containsKey(service)) {
                        serviceRoutes.remove(service);
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