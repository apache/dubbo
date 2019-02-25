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
package com.alibaba.dubbo.registry.nacos;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.support.Registration;
import com.alibaba.dubbo.registry.support.ServiceInstanceRegistry;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Nacos {@link Registry}
 *
 * @since 2.6.6
 */
public class NacosRegistry extends ServiceInstanceRegistry<Instance> {

    /**
     * The pagination size of query for Nacos service names(only for Dubbo-OPS)
     */
    private static final int PAGINATION_SIZE = Integer.getInteger("nacos.service.names.pagination.size", 100);

    private final NamingService namingService;

    private final ConcurrentMap<String, EventListener> nacosListeners;

    public NacosRegistry(URL url, NamingService namingService) {
        super(url);
        this.namingService = namingService;
        this.nacosListeners = new ConcurrentHashMap<String, EventListener>();
    }


    @Override
    protected Instance toServiceInstance(Registration registration) {
        Instance instance = new Instance();
        instance.setServiceName(registration.getServiceName());
        instance.setIp(registration.getIp());
        instance.setPort(registration.getPort());
        instance.setMetadata(registration.getMetadata());
        return instance;
    }

    @Override
    protected Registration toRegistration(final Instance serviceInstance) {
        return new Registration() {

            @Override
            public String getServiceName() {
                return serviceInstance.getServiceName();
            }

            @Override
            public String getIp() {
                return serviceInstance.getIp();
            }

            @Override
            public int getPort() {
                return serviceInstance.getPort();
            }

            @Override
            public Map<String, String> getMetadata() {
                return serviceInstance.getMetadata();
            }
        };
    }

    @Override
    protected void register(final String serviceName, final Instance serviceInstance, URL url) {
        execute(new NamingServiceCallback() {
            @Override
            public void callback(NamingService namingService) throws NacosException {
                namingService.registerInstance(serviceName, serviceInstance);
            }
        });
    }

    @Override
    protected void deregister(final String serviceName, final Instance serviceInstance, URL url) {
        execute(new NamingServiceCallback() {
            @Override
            public void callback(NamingService namingService) throws NacosException {
                namingService.deregisterInstance(serviceName, serviceInstance.getIp(), serviceInstance.getPort());
            }
        });
    }

    @Override
    protected Collection<Instance> findServiceInstances(final String serviceName) {
        final Collection<Instance> instances = new LinkedList<Instance>();
        execute(new NamingServiceCallback() {
            @Override
            public void callback(NamingService namingService) throws NacosException {
                instances.addAll(namingService.getAllInstances(serviceName));
            }
        });
        return instances;
    }

    @Override
    protected boolean filterHealthyRegistration(Instance serviceInstance) {
        return serviceInstance.isEnabled();
    }

    @Override
    protected Set<String> findAllServiceNames() {
        final Set<String> serviceNames = new LinkedHashSet<String>();

        execute(new NamingServiceCallback() {
            @Override
            public void callback(NamingService namingService) throws NacosException {

                int pageIndex = 1;
                ListView<String> listView = namingService.getServicesOfServer(pageIndex, PAGINATION_SIZE);
                // First page data
                List<String> firstPageData = listView.getData();
                // Append first page into list
                serviceNames.addAll(firstPageData);
                // the total count
                int count = listView.getCount();
                // the number of pages
                int pageNumbers = count / PAGINATION_SIZE;
                int remainder = count % PAGINATION_SIZE;
                // remain
                if (remainder > 0) {
                    pageNumbers += 1;
                }
                // If more than 1 page
                while (pageIndex < pageNumbers) {
                    listView = namingService.getServicesOfServer(++pageIndex, PAGINATION_SIZE);
                    serviceNames.addAll(listView.getData());
                }

            }
        });

        return serviceNames;
    }

    @Override
    public boolean isAvailable() {
        return "UP".equals(namingService.getServerStatus());
    }

    private void execute(NamingServiceCallback callback) {
        try {
            callback.callback(namingService);
        } catch (NacosException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getErrMsg(), e);
            }
        }
    }

    /**
     * {@link NamingService} Callback
     */
    interface NamingServiceCallback {

        /**
         * Callback
         *
         * @param namingService {@link NamingService}
         * @throws NacosException
         */
        void callback(NamingService namingService) throws NacosException;

    }
}
