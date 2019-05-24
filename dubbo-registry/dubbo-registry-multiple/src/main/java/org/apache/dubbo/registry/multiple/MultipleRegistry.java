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


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.support.AbstractRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;

/**
 * MultipleRegistry
 */
public class MultipleRegistry extends AbstractRegistry {

    public static final String REGISTRY_FOR_SERVICE = "service-registry";
    public static final String REGISTRY_FOR_REFERENCE = "reference-registry";

    protected RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
    private final Map<String, Registry> serviceRegistries = new ConcurrentHashMap<>(4);
    private final Map<String, Registry> referenceRegistries = new ConcurrentHashMap<String, Registry>(4);
    private final Map<NotifyListener, MultipleNotifyListenerWrapper> multipleNotifyListenerMap = new ConcurrentHashMap<NotifyListener, MultipleNotifyListenerWrapper>(32);
    protected List<String> origServiceRegistryURLs;
    protected List<String> origReferenceRegistryURLs;
    protected List<String> effectServiceRegistryURLs;
    protected List<String> effectReferenceRegistryURLs;
    private URL registryUrl;
    private String applicationName;


    public MultipleRegistry(URL url) {
        super(url);
        this.registryUrl = url;
        this.applicationName = url.getParameter(CommonConstants.APPLICATION_KEY);
        init();
        checkApplicationName(this.applicationName);
        // This urls contain parameter and it donot inherit from the parameter of url in MultipleRegistry
        origServiceRegistryURLs = url.getParameter(REGISTRY_FOR_SERVICE, new ArrayList<String>());
        origReferenceRegistryURLs = url.getParameter(REGISTRY_FOR_REFERENCE, new ArrayList<String>());
        effectServiceRegistryURLs = this.filterServiceRegistry(origServiceRegistryURLs);
        effectReferenceRegistryURLs = this.filterReferenceRegistry(origReferenceRegistryURLs);

        boolean defaultRegistry = url.getParameter(CommonConstants.DEFAULT_KEY, true);
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
        super.register(url);
        for (Registry registry : serviceRegistries.values()) {
            registry.register(url);
        }
    }

    @Override
    public void unregister(URL url) {
        super.unregister(url);
        for (Registry registry : serviceRegistries.values()) {
            registry.unregister(url);
        }
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        MultipleNotifyListenerWrapper multipleNotifyListenerWrapper = new MultipleNotifyListenerWrapper(listener);
        multipleNotifyListenerMap.put(listener, multipleNotifyListenerWrapper);
        for (Registry registry : referenceRegistries.values()) {
            SingleNotifyListener singleNotifyListener = new SingleNotifyListener(multipleNotifyListenerWrapper, registry);
            multipleNotifyListenerWrapper.putRegistryMap(registry.getUrl(), singleNotifyListener);
            registry.subscribe(url, singleNotifyListener);
        }
        super.subscribe(url, multipleNotifyListenerWrapper);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        MultipleNotifyListenerWrapper notifyListener = multipleNotifyListenerMap.remove(listener);
        for (Registry registry : referenceRegistries.values()) {
            SingleNotifyListener singleNotifyListener = notifyListener.registryMap.get(registry.getUrl());
            registry.unsubscribe(url, singleNotifyListener);
        }

        if (notifyListener != null) {
            super.unsubscribe(url, notifyListener);
            notifyListener.destroy();
        }
    }

    @Override
    public List<URL> lookup(URL url) {
        List<URL> urls = new ArrayList<URL>();
        for (Registry registry : referenceRegistries.values()) {
            List<URL> tmpUrls = registry.lookup(url);
            if (!CollectionUtils.isEmpty(tmpUrls)) {
                urls.addAll(tmpUrls);
            }
        }
        return urls;
    }

    protected void init() {
    }

    protected List<String> filterServiceRegistry(List<String> serviceRegistryURLs) {
        return serviceRegistryURLs;
    }

    protected List<String> filterReferenceRegistry(List<String> referenceRegistryURLs) {
        return referenceRegistryURLs;
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

    public List<String> getEffectServiceRegistryURLs() {
        return effectServiceRegistryURLs;
    }

    public List<String> getEffectReferenceRegistryURLs() {
        return effectReferenceRegistryURLs;
    }

    static protected class MultipleNotifyListenerWrapper implements NotifyListener {

        Map<URL, SingleNotifyListener> registryMap = new ConcurrentHashMap<URL, SingleNotifyListener>(4);
        NotifyListener sourceNotifyListener;

        public MultipleNotifyListenerWrapper(NotifyListener sourceNotifyListener) {
            this.sourceNotifyListener = sourceNotifyListener;
        }

        public void putRegistryMap(URL registryURL, SingleNotifyListener singleNotifyListener) {
            this.registryMap.put(registryURL, singleNotifyListener);
        }

        public void destroy() {
            for (SingleNotifyListener singleNotifyListener : registryMap.values()) {
                if (singleNotifyListener != null) {
                    singleNotifyListener.destroy();
                }
            }
            registryMap.clear();
            sourceNotifyListener = null;
        }

        public synchronized void notifySourceListener() {
            List<URL> notifyURLs = new ArrayList<URL>();
            URL emptyURL = null;
            for (SingleNotifyListener singleNotifyListener : registryMap.values()) {
                List<URL> tmpUrls = singleNotifyListener.getUrlList();
                if (CollectionUtils.isEmpty(tmpUrls)) {
                    continue;
                }
                // empty protocol
                if (tmpUrls.size() == 1
                        && tmpUrls.get(0) != null
                        && EMPTY_PROTOCOL.equals(tmpUrls.get(0).getProtocol())) {
                    // if only one empty
                    if (emptyURL == null) {
                        emptyURL = tmpUrls.get(0);
                    }
                    continue;
                }
                notifyURLs.addAll(tmpUrls);
            }
            // if no notify URL, add empty protocol URL
            if (emptyURL != null && notifyURLs.isEmpty()) {
                notifyURLs.add(emptyURL);
            }
            this.notify(notifyURLs);
        }

        @Override
        public void notify(List<URL> urls) {
            sourceNotifyListener.notify(urls);
        }

        public Map<URL, SingleNotifyListener> getRegistryMap() {
            return registryMap;
        }
    }

    static protected class SingleNotifyListener implements NotifyListener {

        MultipleNotifyListenerWrapper multipleNotifyListenerWrapper;
        Registry registry;
        volatile List<URL> urlList;

        public SingleNotifyListener(MultipleNotifyListenerWrapper multipleNotifyListenerWrapper, Registry registry) {
            this.registry = registry;
            this.multipleNotifyListenerWrapper = multipleNotifyListenerWrapper;
        }

        @Override
        public synchronized void notify(List<URL> urls) {
            this.urlList = urls;
            if (multipleNotifyListenerWrapper != null) {
                this.multipleNotifyListenerWrapper.notifySourceListener();
            }
        }

        public void destroy() {
            this.multipleNotifyListenerWrapper = null;
            this.registry = null;
        }

        public List<URL> getUrlList() {
            return urlList;
        }
    }
}
