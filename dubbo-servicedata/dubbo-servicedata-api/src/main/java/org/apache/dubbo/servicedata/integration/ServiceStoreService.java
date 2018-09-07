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
package org.apache.dubbo.servicedata.integration;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.servicedata.ServiceStore;
import org.apache.dubbo.servicedata.ServiceStoreFactory;
import org.apache.dubbo.servicedata.support.AbstractServiceStoreFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public class ServiceStoreService {

    private ServiceStoreFactory serviceStoreFactory = ExtensionLoader.getExtensionLoader(ServiceStoreFactory.class).getAdaptiveExtension();
    private static final Set<URL> providerURLs = new HashSet<URL>();
    private static final Set<URL> consumerURLs = new HashSet<URL>();
    private ServiceStore serviceStore;

    private URL serviceStoreUrl;

    public ServiceStoreService(URL serviceStoreURL) {
        if (Constants.SERVICE_STORE_KEY.equals(serviceStoreURL.getProtocol())) {
            String protocol = serviceStoreURL.getParameter(Constants.SERVICE_STORE_KEY, Constants.DEFAULT_DIRECTORY);
            serviceStoreURL = serviceStoreURL.setProtocol(protocol).removeParameter(Constants.SERVICE_STORE_KEY);
        }
        this.serviceStoreUrl = serviceStoreURL;
        serviceStore = serviceStoreFactory.getServiceStore(this.serviceStoreUrl);
    }

    public int getDefaultPort() {
        return 9099;
    }

    public void publishProvider(URL providerUrl) throws RpcException {
        providerURLs.add(providerUrl);
        serviceStore.put(providerUrl);
    }

    public void publishConsumer(URL consumerURL) throws RpcException {
        consumerURLs.add(consumerURL);
        serviceStore.put(consumerURL);
    }

    public static void destroyAll() {
        for (ServiceStore serviceStore : AbstractServiceStoreFactory.getServiceStores()) {
            for (URL provideUrl : providerURLs) {
                serviceStore.remove(provideUrl);
            }
            for (URL consumerUrl : consumerURLs) {
                serviceStore.remove(consumerUrl);
            }
        }
    }

    private URL getSubscribedOverrideUrl(URL registedProviderUrl) {
        return registedProviderUrl.setProtocol(Constants.PROVIDER_PROTOCOL)
                .addParameters(Constants.CATEGORY_KEY, Constants.CONFIGURATORS_CATEGORY,
                        Constants.CHECK_KEY, String.valueOf(false));
    }

    private URL getServiceStoreTargetUrl(final URL url) {
        String export = url.getParameterAndDecoded(Constants.EXPORT_KEY);
        if (export == null || export.length() == 0) {
            throw new IllegalArgumentException("The registry export url is null! registry: " + url);
        }

        URL providerUrl = URL.valueOf(export);
        return providerUrl;
    }


    //Filter the parameters that do not need to be output in url(Starting with .)
    private static String[] getFilteredKeys(URL url) {
        Map<String, String> params = url.getParameters();
        if (params != null && !params.isEmpty()) {
            List<String> filteredKeys = new ArrayList<String>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry != null && entry.getKey() != null && entry.getKey().startsWith(Constants.HIDE_KEY_PREFIX)) {
                    filteredKeys.add(entry.getKey());
                }
            }
            return filteredKeys.toArray(new String[filteredKeys.size()]);
        } else {
            return new String[]{};
        }
    }

    public ServiceStoreFactory getServiceStoreFactory() {
        return serviceStoreFactory;
    }

    public void setServiceStoreFactory(ServiceStoreFactory serviceStoreFactory) {
        this.serviceStoreFactory = serviceStoreFactory;
    }

}
