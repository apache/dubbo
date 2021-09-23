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
package org.apache.dubbo.registry.xds;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.SelfHostMetaServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.xds.util.PilotExchanger;
import org.apache.dubbo.registry.xds.util.protocol.message.Endpoint;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class XdsServiceDiscovery extends SelfHostMetaServiceDiscovery {
    private PilotExchanger exchanger;
    private static final Logger logger = LoggerFactory.getLogger(XdsServiceDiscovery.class);

    @Override
    public void doInitialize(URL registryURL) throws Exception {
        try {
            exchanger = PilotExchanger.initialize(registryURL);
        } catch (Throwable t) {
            logger.error(t);
        }
    }

    @Override
    public void doDestroy() throws Exception {
        exchanger.destroy();
    }

    @Override
    public Set<String> getServices() {
        return exchanger.getServices();
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        Set<Endpoint> endpoints = exchanger.getEndpoints(serviceName);
        return changedToInstances(serviceName, endpoints);
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {
        listener.getServiceNames().forEach(serviceName -> {
            exchanger.observeEndpoints(serviceName, (endpoints -> {
                notifyListener(serviceName, listener, changedToInstances(serviceName, endpoints));
            }));
        });
    }

    private List<ServiceInstance> changedToInstances(String serviceName, Collection<Endpoint> endpoints) {
        List<ServiceInstance> instances = new LinkedList<>();
        endpoints.forEach(endpoint -> {
            try {
                DefaultServiceInstance serviceInstance = new DefaultServiceInstance(serviceName, endpoint.getAddress(), endpoint.getPortValue(), ScopeModelUtil.getApplicationModel(getUrl().getScopeModel()));
                // fill metadata by SelfHostMetaServiceDiscovery, will be fetched by RPC request
                fillServiceInstance(serviceInstance);
                instances.add(serviceInstance);
            } catch (Throwable t) {
                logger.error("Error occurred when parsing endpoints. Endpoints List:" + endpoints,t);
            }
        });
        instances.sort(Comparator.comparingInt(ServiceInstance::hashCode));
        return instances;
    }
}
