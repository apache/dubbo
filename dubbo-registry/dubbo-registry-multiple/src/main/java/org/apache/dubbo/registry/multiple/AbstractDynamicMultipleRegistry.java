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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONSUMERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONSUMER_PROTOCOL;

/**
 * AbstractDynamicMultipleRegistry
 */
public class AbstractDynamicMultipleRegistry extends MultipleRegistry {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDynamicMultipleRegistry.class);

    public AbstractDynamicMultipleRegistry(URL url) {
        super(url);
    }

    protected synchronized void refreshServiceRegistry(List<String> serviceRegistryURLs) {
        this.effectServiceRegistryURLs = serviceRegistryURLs;

        // If new registry is empty or registry running is empty , it will not be freshed.
        if (serviceRegistryURLs == null || serviceRegistryURLs.isEmpty() || getServiceRegistries().isEmpty()) {
            return;
        }
        // fetch registriedURLs
        Set<URL> registerOrSubscriberData = getRegisteredProviderURLs();
        if (CollectionUtils.isEmpty(registerOrSubscriberData)) {
            logger.info("Cannot fetch registered URL.");
            return;
        }

        Map<String, Registry> newRegistryMap = new HashMap<String, Registry>(4);
        // get new registry for service
        for (String serviceRegistryURL : serviceRegistryURLs) {
            Registry registry = getServiceRegistries().get(serviceRegistryURL);
            if (registry == null) {
                registry = registryFactory.getRegistry(URL.valueOf(serviceRegistryURL));
                newRegistryMap.put(serviceRegistryURL, registry);

                logger.info("Add new registry for service:" + serviceRegistryURL);
                // registry all
                for (URL url : registerOrSubscriberData) {
                    registry.register(url);
                }
            }
        }

        // get removed registry and keep the registry that is the same as new Configuration.
        List<Registry> removedRegistries = new ArrayList<>();
        for (Map.Entry<String, Registry> origRegistryEntry : getServiceRegistries().entrySet()) {
            if (serviceRegistryURLs.contains(origRegistryEntry.getKey())) {
                newRegistryMap.put(origRegistryEntry.getKey(), origRegistryEntry.getValue());
            } else {
                removedRegistries.add(origRegistryEntry.getValue());
            }
        }
        // unregister/unsubscribe by remove registry
        for (Registry removedRegistry : removedRegistries) {
            logger.info("Remove original registry for service:" + removedRegistry.getUrl());
            for (URL url : registerOrSubscriberData) {
                removedRegistry.unregister(url);
            }
        }
        // set serviceRegistry
        this.getServiceRegistries().clear();
        this.getServiceRegistries().putAll(newRegistryMap);
    }

    protected synchronized void refreshReferenceRegistry(List<String> referenceRegistryURLs) {
        this.effectReferenceRegistryURLs = referenceRegistryURLs;
        // If new registry is empty or registry running is empty , it will not be freshed.
        if (referenceRegistryURLs == null || referenceRegistryURLs.isEmpty() || getReferenceRegistries().isEmpty()) {
            return;
        }
        // fetch register or subscriber
        Map<URL, Set<NotifyListener>> registerOrSubscriberData = getSubscribedURLMap();
        Set<URL> consumerURLs = getRegisteredConsumerURLs();
        if (registerOrSubscriberData == null) {
            logger.info("Cannot fetch registered URL.");
            return;
        }

        Map<String, Registry> newRegistryMap = new HashMap<String, Registry>(4);
        // get new registry for reference
        for (String serviceRegistryURL : referenceRegistryURLs) {
            Registry registry = getReferenceRegistries().get(serviceRegistryURL);
            if (registry == null) {
                registry = registryFactory.getRegistry(URL.valueOf(serviceRegistryURL));
                newRegistryMap.put(serviceRegistryURL, registry);

                logger.info("Add new registry for reference:" + serviceRegistryURL);
                // registry all
                for (Map.Entry<URL, Set<NotifyListener>> urlNotifyListenerMap : registerOrSubscriberData.entrySet()) {
                    for (NotifyListener notifyListener : urlNotifyListenerMap.getValue()) {
                        NotifyListener singleNotifyListener = fetchNotifyListenerAndAddMultipleSubscribed(notifyListener, registry);
                        registry.subscribe(urlNotifyListenerMap.getKey(), singleNotifyListener);
                    }
                    // register consumer for ops
                    for (URL url : consumerURLs) {
                        registry.register(url);
                    }
                }
            }
        }

        // get removed registry and keep the registry that is the same as new Configuration.
        List<Registry> removedRegistries = new ArrayList<>();
        for (Map.Entry<String, Registry> origRegistryEntry : getReferenceRegistries().entrySet()) {
            if (referenceRegistryURLs.contains(origRegistryEntry.getKey())) {
                newRegistryMap.put(origRegistryEntry.getKey(), origRegistryEntry.getValue());
            } else {
                removedRegistries.add(origRegistryEntry.getValue());
            }
        }
        // unregister/unsubscribe by remove registry
        for (Registry removedRegistry : removedRegistries) {
            logger.info("Remove original registry for reference:" + removedRegistry.getUrl());

            // unregistry registried consumer data
            for (URL url : consumerURLs) {
                removedRegistry.unregister(url);
            }
            // unsubscriber
            for (Map.Entry<URL, Set<NotifyListener>> urlNotifyListenerMap : registerOrSubscriberData.entrySet()) {
                for (NotifyListener notifyListener : urlNotifyListenerMap.getValue()) {
                    doRemoveMultipleSubscribed(notifyListener, removedRegistry);
                    removedRegistry.unsubscribe(urlNotifyListenerMap.getKey(), notifyListener);
                }
            }
        }
        // set referenceRegistry
        this.getReferenceRegistries().clear();
        this.getReferenceRegistries().putAll(newRegistryMap);

        // After subscribe and unsubscribe, we should notify all listener.
        for (Map.Entry<URL, Set<NotifyListener>> urlNotifyListenerMap : registerOrSubscriberData.entrySet()) {
            for (NotifyListener notifyListener : urlNotifyListenerMap.getValue()) {
                if (notifyListener instanceof MultipleNotifyListenerWrapper) {
                    ((MultipleNotifyListenerWrapper) notifyListener).notifySourceListener();
                }
            }
        }
    }

    private Set<URL> getRegisteredProviderURLs() {
        Set<URL> urlSet = new HashSet<URL>();
        for (URL url : super.getRegistered()) {
            if (url == null) {
                continue;
            }
            if (CONSUMER_PROTOCOL.equals(url.getProtocol()) || CONSUMERS_CATEGORY.equals(url.getParameter(CATEGORY_KEY))) {
                continue;
            }
            urlSet.add(url);
        }
        return urlSet;
    }

    private Set<URL> getRegisteredConsumerURLs() {
        Set<URL> urlSet = new HashSet<URL>();
        for (URL url : super.getRegistered()) {
            if (url == null) {
                continue;
            }
            if (CONSUMER_PROTOCOL.equals(url.getProtocol()) || CONSUMERS_CATEGORY.equals(url.getParameter(CATEGORY_KEY))) {
                urlSet.add(url);
            }
        }
        return urlSet;
    }


    private Map<URL, Set<NotifyListener>> getSubscribedURLMap() {
        // registry all
        return super.getSubscribed();
    }

    private void doRemoveMultipleSubscribed(NotifyListener notifyListener, Registry removedRegistry) {
        if (notifyListener instanceof MultipleNotifyListenerWrapper) {
            ((MultipleNotifyListenerWrapper) notifyListener).registryMap.remove(removedRegistry.getUrl());
        }
    }

    private NotifyListener fetchNotifyListenerAndAddMultipleSubscribed(NotifyListener notifyListener, Registry newRegistry) {
        if (notifyListener instanceof MultipleNotifyListenerWrapper) {
            MultipleNotifyListenerWrapper multipleNotifyListenerWrapper = (MultipleNotifyListenerWrapper) notifyListener;
            SingleNotifyListener singleNotifyListener = new SingleNotifyListener(multipleNotifyListenerWrapper, newRegistry);
            ((MultipleNotifyListenerWrapper) notifyListener).registryMap.put(newRegistry.getUrl(), singleNotifyListener);
            return singleNotifyListener;
        }
        throw new IllegalArgumentException("fetch notify listener but not return any MultipleNotifyListenerWrapper." + notifyListener);
    }


}
