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
package org.apache.dubbo.xds.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Instances arrive with individually fetched metadata; no revision lookup or port inference is performed. */
public final class XdsServiceInstancesChangedListener extends ServiceInstancesChangedListener {
    public XdsServiceInstancesChangedListener(Set<String> serviceNames, ServiceDiscovery discovery) {
        super(serviceNames, discovery);
    }

    @Override
    public synchronized void onEvent(ServiceInstancesChangedEvent event) {
        if (destroyed.get() || !serviceNames.contains(event.getServiceName())) {
            return;
        }
        allInstances.put(event.getServiceName(), event.getServiceInstances());
        Map<String, List<ProtocolServiceKeyWithUrls>> urls = new HashMap<>();
        for (List<ServiceInstance> instances : allInstances.values()) {
            for (ServiceInstance instance : instances) {
                if (!(instance instanceof XdsServiceInstance) || !instance.isEnabled() || !instance.isHealthy()) {
                    continue;
                }
                XdsServiceInstance xds = (XdsServiceInstance) instance;
                for (MetadataInfo.ServiceInfo service :
                        xds.getServiceMetadata().getServices().values()) {
                    if (!service.getProtocol()
                                    .equals(xds.getEndpoint().getTarget().getProtocol())
                            || service.getPort() != instance.getPort()) {
                        continue;
                    }
                    urls.computeIfAbsent(service.getName(), key -> new ArrayList<>())
                            .add(new ProtocolServiceKeyWithUrls(
                                    service.getProtocolServiceKey(),
                                    Collections.singletonList(instance.toURL(service.getProtocol()))));
                }
            }
        }
        serviceUrls = urls;
        notifyAddressChanged();
    }

    @Override
    protected List<URL> toUrlsWithEmpty(List<URL> urls) {
        // An authoritative removal must evict stale Invokers even if generic registry empty protection is enabled.
        return urls == null || urls.isEmpty()
                ? Collections.singletonList(serviceDiscovery.getUrl().setProtocol("empty"))
                : urls;
    }
}
