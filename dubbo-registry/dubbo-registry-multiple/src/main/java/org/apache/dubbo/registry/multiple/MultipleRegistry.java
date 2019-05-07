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
package org.apache.dubbo.registry.multiple;


import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.support.AbstractRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * MultipleRegistry
 */
public class MultipleRegistry implements Registry {

    private static final Logger logger = LoggerFactory.getLogger(MultipleRegistry.class);
    private static final String REGISTRY_FOR_SERVICE = "for-service";
    private static final String REGISTRY_FOR_REFERENCE = "for-reference";

    private RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
    private Map<String, Registry> serviceRegistries = new HashMap<String, Registry>(4);
    private Map<String, Registry> referenceRegistries = new HashMap<String, Registry>(4);
    List<String> origServiceRegistryURLs;
    List<String> origReferenceRegistryURLs;
    List<String> effectServiceRegistryURLs;
    List<String> effectReferenceRegistryURLs;
    private URL registryUrl;
    private String applicationName;


    public MultipleRegistry(URL url) {
        this.registryUrl = url;
        this.applicationName = url.getParameter(Constants.APPLICATION_KEY);
        checkApplicationName(this.applicationName);
        // This urls contain parameter and it donot inherit from the parameter of url in MultipleRegistry
        origServiceRegistryURLs = url.getParameter(REGISTRY_FOR_SERVICE, new ArrayList<String>());
        origReferenceRegistryURLs = url.getParameter(REGISTRY_FOR_REFERENCE, new ArrayList<String>());
        effectServiceRegistryURLs = this.filterServiceRegistry(origServiceRegistryURLs);
        effectReferenceRegistryURLs = this.filterReferenceRegistry(origReferenceRegistryURLs);

        boolean defaultRegistry = url.getParameter(Constants.DEFAULT_KEY, true);
        if (defaultRegistry && effectServiceRegistryURLs.isEmpty() && effectReferenceRegistryURLs.isEmpty()) {
            throw new IllegalArgumentException("Illegal registry url. You need to configure parameter " +
                    REGISTRY_FOR_SERVICE + " or " + REGISTRY_FOR_REFERENCE);
        }
        Set<String> allURLs = new HashSet<String>(effectServiceRegistryURLs);
        allURLs.addAll(effectReferenceRegistryURLs);
        Map<String, Registry> tmpMap = new HashMap<String, Registry>(4);
        for (String tmpUrl : allURLs) {
            tmpMap.put(tmpUrl, registryFactory.getRegistry(URL.valueOf(tmpUrl)));
        }
        for (String serviceRegistyURL : effectServiceRegistryURLs) {
            serviceRegistries.put(serviceRegistyURL, tmpMap.get(serviceRegistyURL));
        }
        for (String referenceReigstyURL : effectReferenceRegistryURLs) {
            referenceRegistries.put(referenceReigstyURL, tmpMap.get(referenceReigstyURL));
        }
    }


    @Override
    public URL getUrl() {
        return registryUrl;
    }

    @Override
    public boolean isAvailable() {
        boolean available = serviceRegistries.isEmpty() ? true : false;
        for (Registry serviceRegistry : serviceRegistries.values()) {
            if (serviceRegistry.isAvailable()) {
                available = true;
            }
        }
        if (!available) {
            return false;
        }

        available = referenceRegistries.isEmpty() ? true : false;
        for (Registry referenceRegistry : referenceRegistries.values()) {
            if (referenceRegistry.isAvailable()) {
                available = true;
            }
        }
        if (!available) {
            return false;
        }
        return true;
    }

    @Override
    public void destroy() {
        Set<Registry> registries = new HashSet<Registry>(serviceRegistries.values());
        registries.addAll(referenceRegistries.values());
        for (Registry registry : registries) {
            registry.destroy();
        }
    }

    @Override
    public void register(URL url) {
        for (Registry registry : serviceRegistries.values()) {
            registry.register(url);
        }
    }

    @Override
    public void unregister(URL url) {
        for (Registry registry : serviceRegistries.values()) {
            registry.unregister(url);
        }
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        for (Registry registry : referenceRegistries.values()) {
            registry.subscribe(url, listener);
        }
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        for (Registry registry : referenceRegistries.values()) {
            registry.unsubscribe(url, listener);
        }
    }

    @Override
    public List<URL> lookup(URL url) {
        List<URL> urls = new ArrayList<URL>();
        for (Registry registry : referenceRegistries.values()) {
            List<URL> tmpUrls = registry.lookup(url);
            if (tmpUrls != null && !tmpUrls.isEmpty()) {
                urls.addAll(tmpUrls);
            }
        }
        return urls;
    }

    protected List<String> filterServiceRegistry(List<String> serviceRegistryURLs) {
        return serviceRegistryURLs;
    }

    protected List<String> filterReferenceRegistry(List<String> referenceRegistryURLs) {
        return referenceRegistryURLs;
    }

    protected synchronized void refreshServiceRegistry(List<String> serviceRegistryURLs) {
        doRefreshRegistry(serviceRegistryURLs, serviceRegistries, () -> this.getRegisteredURLs(),
                (registry, registeredURLs) -> {
                    for (URL url : (Set<URL>) registeredURLs) {
                        registry.register(url);
                    }
                },
                (registry, registeredURLs) -> {
                    for (URL url : (Set<URL>) registeredURLs) {
                        registry.unregister(url);
                    }
                },
                newRegistryMap -> this.serviceRegistries = newRegistryMap

        );
    }

    protected synchronized void refreshReferenceRegistry(List<String> referenceRegistryURLs) {
        doRefreshRegistry(referenceRegistryURLs, referenceRegistries, () -> this.getSubscribedURLMap(),
                (registry, registeredURLs) -> {
                    for (Map.Entry<URL, Set<NotifyListener>> urlNotifyListenerMap : ((Map<URL, Set<NotifyListener>>) registeredURLs).entrySet()) {
                        for (NotifyListener notifyListener : urlNotifyListenerMap.getValue()) {
                            registry.subscribe(urlNotifyListenerMap.getKey(), notifyListener);
                        }
                    }
                },
                (registry, registeredURLs) -> {
                    for (Map.Entry<URL, Set<NotifyListener>> urlNotifyListenerMap : ((Map<URL, Set<NotifyListener>>) registeredURLs).entrySet()) {
                        for (NotifyListener notifyListener : urlNotifyListenerMap.getValue()) {
                            registry.unsubscribe(urlNotifyListenerMap.getKey(), notifyListener);
                        }
                    }
                },
                newRegistryMap -> this.referenceRegistries = newRegistryMap

        );
    }

    /**
     * @param newRegistryURLs
     * @param oldRegistryMap
     * @param getURLSupplier    if result is empty, please return null
     * @param joinConsumer
     * @param leftConsumer
     * @param setResultConsumer
     */
    private synchronized void doRefreshRegistry(List<String> newRegistryURLs, Map<String, Registry> oldRegistryMap,
                                                Supplier<Object> getURLSupplier,
                                                BiConsumer<Registry, Object> joinConsumer, BiConsumer<Registry, Object> leftConsumer,
                                                Consumer<Map<String, Registry>> setResultConsumer
    ) {
        // If new registry is empty or registry running is empty , it will not be freshed.
        if (newRegistryURLs == null || newRegistryURLs.isEmpty() || oldRegistryMap.isEmpty()) {
            return;
        }
        // fetch register or subscriber
        Object registeredURLs = this.getRegisteredURLs();
        if (registeredURLs == null) {
            logger.info("Cannot fetch registered URL.");
            return;
        }

        Map<String, Registry> newRegistryMap = new HashMap<String, Registry>(4);
        for (String serviceRegistryURL : newRegistryURLs) {
            Registry registry = oldRegistryMap.get(serviceRegistryURL);
            if (registry == null) {
                registry = registryFactory.getRegistry(URL.valueOf(serviceRegistryURL));
                newRegistryMap.put(serviceRegistryURL, registry);
                // registry all
                joinConsumer.accept(registry, registeredURLs);
            }
        }

        // get removed registry and keep the registry that is the same as new Configuration.
        List<Registry> removedRegistries = new ArrayList<>();
        for (Map.Entry<String, Registry> origRegistryEntry : oldRegistryMap.entrySet()) {
            if (newRegistryURLs.contains(origRegistryEntry.getKey())) {
                newRegistryMap.put(origRegistryEntry.getKey(), origRegistryEntry.getValue());
            } else {
                removedRegistries.add(origRegistryEntry.getValue());
            }
        }
        // unregister by remove registry
        for (Registry removedRegistry : removedRegistries) {
            leftConsumer.accept(removedRegistry, registeredURLs);
        }
        setResultConsumer.accept(newRegistryMap);
    }

    private Set<URL> getRegisteredURLs() {
        // registry all
        Iterator<Registry> iterator = serviceRegistries.values().iterator();
        while (iterator.hasNext()) {
            Registry tmpRegistry = iterator.next();
            if (tmpRegistry instanceof AbstractRegistry) {
                AbstractRegistry tmpAbstractRegistry = (AbstractRegistry) tmpRegistry;
                return tmpAbstractRegistry.getRegistered();
            }
        }
        return Collections.emptySet();
    }

    private Map<URL, Set<NotifyListener>> getSubscribedURLMap() {
        // registry all
        Iterator<Registry> iterator = referenceRegistries.values().iterator();
        while (iterator.hasNext()) {
            Registry tmpRegistry = iterator.next();
            if (tmpRegistry instanceof AbstractRegistry) {
                AbstractRegistry tmpAbstractRegistry = (AbstractRegistry) tmpRegistry;
                return tmpAbstractRegistry.getSubscribed();
            }
        }
        return Collections.EMPTY_MAP;
    }

    protected void checkApplicationName(String applicationName) {
    }

    protected String getApplicationName() {
        return applicationName;
    }

    public Map<String, Registry> getServiceRegistries() {
        return serviceRegistries;
    }

    public Map<String, Registry> getReferenceRegistries() {
        return referenceRegistries;
    }

    public List<String> getOrigServiceRegistryURLs() {
        return origServiceRegistryURLs;
    }

    public List<String> getOrigReferenceRegistryURLs() {
        return origReferenceRegistryURLs;
    }
}
