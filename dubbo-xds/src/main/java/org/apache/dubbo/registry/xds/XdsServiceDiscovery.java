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

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ReflectionBasedServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.xds.util.PilotExchanger;
import org.apache.dubbo.registry.xds.util.protocol.message.Endpoint;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_INITIALIZE_XDS;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_PARSING_XDS;

public class XdsServiceDiscovery extends ReflectionBasedServiceDiscovery {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(XdsServiceDiscovery.class);

    private PilotExchanger exchanger;

    public XdsServiceDiscovery(ApplicationModel applicationModel, URL registryURL) {
        super(applicationModel, registryURL);
    }

    @Override
    public void doInitialize(URL registryURL) {
        try {
            exchanger = PilotExchanger.initialize(registryURL);
        } catch (Throwable t) {
            logger.error(REGISTRY_ERROR_INITIALIZE_XDS, "", "", t.getMessage(), t);
        }
    }

    @Override
    public void doDestroy() {
        try {
            exchanger.destroy();
        } catch (Throwable t) {
            logger.error(REGISTRY_ERROR_INITIALIZE_XDS, "", "", t.getMessage(), t);
        }
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
        listener.getServiceNames().forEach(serviceName -> exchanger.observeEndpoints(serviceName,
            (endpoints -> notifyListener(serviceName, listener, changedToInstances(serviceName, endpoints)))));
    }

    private List<ServiceInstance> changedToInstances(String serviceName, Collection<Endpoint> endpoints) {
        List<ServiceInstance> instances = new LinkedList<>();
        endpoints.forEach(endpoint -> {
            try {
                DefaultServiceInstance serviceInstance = new DefaultServiceInstance(serviceName, endpoint.getAddress(), endpoint.getPortValue(), ScopeModelUtil.getApplicationModel(getUrl().getScopeModel()));
                // fill metadata by SelfHostMetaServiceDiscovery, will be fetched by RPC request
                serviceInstance.putExtendParam("clusterName", endpoint.getClusterName());
                fillServiceInstance(serviceInstance);
                instances.add(serviceInstance);
            } catch (Throwable t) {
                logger.error(REGISTRY_ERROR_PARSING_XDS, "", "", "Error occurred when parsing endpoints. Endpoints List:" + endpoints, t);
            }
        });
        instances.sort(Comparator.comparingInt(ServiceInstance::hashCode));
        return instances;
    }
}
