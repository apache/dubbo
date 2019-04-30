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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private URL registryUrl;


    public MultipleRegistry(URL url) {
        this.registryUrl = url;
        // This urls contain parameter and it donot inherit from the parameter of url in MultipleRegistry
        List<String> serviceRegistryURLs = url.getParameter(REGISTRY_FOR_SERVICE, new ArrayList<String>());
        List<String> referenceRegistryURLs = url.getParameter(REGISTRY_FOR_REFERENCE, new ArrayList<String>());


        boolean defaultRegistry = url.getParameter(Constants.DEFAULT_KEY, true);
        if (defaultRegistry && serviceRegistryURLs.isEmpty() && referenceRegistryURLs.isEmpty()) {
            throw new IllegalArgumentException("Illegal registry url. You need to configure parameter " +
                    REGISTRY_FOR_SERVICE + " or " + REGISTRY_FOR_REFERENCE);
        }
        Set<String> allURLs = new HashSet<String>(serviceRegistryURLs);
        allURLs.addAll(referenceRegistryURLs);
        Map<String, Registry> tmpMap = new HashMap<String, Registry>(4);
        for (String tmpUrl : allURLs) {
            tmpMap.put(tmpUrl, registryFactory.getRegistry(URL.valueOf(tmpUrl)));
        }
        for (String serviceRegistyURL : serviceRegistryURLs) {
            serviceRegistries.put(serviceRegistyURL, tmpMap.get(serviceRegistyURL));
        }
        for (String referenceReigstyURL : referenceRegistryURLs) {
            referenceRegistries.put(referenceReigstyURL, tmpMap.get(referenceReigstyURL));
        }
    }


    @Override
    public URL getUrl() {
        return registryUrl;
    }

    @Override
    public boolean isAvailable() {
        return false;
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

    protected List<String> refreshServiceRegistry(List<String> serviceRegistryURLs) {
        for (String serviceRegistryURL : serviceRegistryURLs) {
            Registry registry = serviceRegistries.get(serviceRegistryURL);
            if(registry == null){
                Registry newRegistry = registryFactory.getRegistry(URL.valueOf(serviceRegistryURL));
                //估计得需要保存下registriedURL
//                newRegistry.register();
//                serviceRegistries.put(serviceRegistryURL, );
            }
        }
        return serviceRegistryURLs;
    }

    protected List<String> refreshReferenceRegistry(List<String> referenceRegistryURLs) {
        return referenceRegistryURLs;
    }

}
