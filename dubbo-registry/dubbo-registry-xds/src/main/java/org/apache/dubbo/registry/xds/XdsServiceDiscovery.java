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
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.xds.util.PilotExchanger;
import org.apache.dubbo.registry.xds.util.protocol.message.Endpoint;

import java.util.List;
import java.util.Set;

public class XdsServiceDiscovery extends AbstractServiceDiscovery {
    private PilotExchanger exchanger;
    private URL registryURL;

    @Override
    public void initialize(URL registryURL) throws Exception {
        exchanger = PilotExchanger.initialize(registryURL);
    }

    @Override
    public void register(ServiceInstance serviceInstance) throws RuntimeException {

    }

    @Override
    public void update(ServiceInstance serviceInstance) throws RuntimeException {

    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {

    }

    @Override
    public Set<String> getServices() {
        return exchanger.getServices();
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        Set<Endpoint> endpoints = exchanger.getEndpoints(serviceName);
        return null;
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {
        listener.getServiceNames().forEach(serviceName -> {
            exchanger.observeEndpoints(serviceName, (endpoints -> {
                listener.accept(new ServiceInstancesChangedEvent(serviceName, null));
            }));
        });
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }
}
