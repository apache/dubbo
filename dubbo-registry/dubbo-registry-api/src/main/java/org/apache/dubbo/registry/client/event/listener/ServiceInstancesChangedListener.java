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
package org.apache.dubbo.registry.client.event.listener;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.event.ConditionalEventListener;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataInfo.ServiceInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.metadata.MetadataUtils;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.registry.client.metadata.store.RemoteMetadataServiceImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getExportedServicesRevision;

/**
 * The Service Discovery Changed {@link EventListener Event Listener}
 *
 * @see ServiceInstancesChangedEvent
 * @since 2.7.5
 */
public abstract class ServiceInstancesChangedListener implements ConditionalEventListener<ServiceInstancesChangedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInstancesChangedListener.class);

    private final String serviceName;
    private final ServiceDiscovery serviceDiscovery;
    private URL url;

    private List<ServiceInstance> instances;

    private Map<String, List<ServiceInstance>> revisionToInstances;

    private Map<String, MetadataInfo> revisionToMetadata;

    private Map<String, Set<String>> serviceToRevisions;

    private Map<String, List<ServiceInstance>> serviceToInstances;

    protected ServiceInstancesChangedListener(String serviceName, ServiceDiscovery serviceDiscovery, URL url) {
        this.serviceName = serviceName;
        this.serviceDiscovery = serviceDiscovery;
        this.url = url;
    }

    /**
     * On {@link ServiceInstancesChangedEvent the service instances change event}
     *
     * @param event {@link ServiceInstancesChangedEvent}
     */
    public void onEvent(ServiceInstancesChangedEvent event) {
        instances = event.getServiceInstances();

        Map<String, List<ServiceInstance>> localRevisionToInstances = new HashMap<>();
        Map<String, MetadataInfo> localRevisionToMetadata = new HashMap<>();
        Map<String, Set<String>> localServiceToRevisions = new HashMap<>();
        for (ServiceInstance instance : instances) {
            String revision = getExportedServicesRevision(instance);
            Collection<ServiceInstance> rInstances = localRevisionToInstances.computeIfAbsent(revision, r -> new ArrayList<>());
            rInstances.add(instance);

            MetadataInfo metadata = null;
            if (revisionToMetadata != null && ((metadata = revisionToMetadata.get(revision)) != null)) {
                localRevisionToMetadata.put(revision, metadata);
            } else {
                metadata = getMetadataInfo(instance);
                if (metadata != null) {
                    localRevisionToMetadata.put(revision, getMetadataInfo(instance));
                }
            }

            if (metadata != null) {
                parse(revision, metadata, localServiceToRevisions);
                ((DefaultServiceInstance) instance).setServiceMetadata(metadata);
            } else {
                logger.error("Failed to load service metadata for instance " + instance);
                Set<String> set = localServiceToRevisions.computeIfAbsent(url.getServiceKey(), k -> new HashSet<>());
                set.add(revision);
            }
        }

        this.revisionToInstances = localRevisionToInstances;
        this.revisionToMetadata = localRevisionToMetadata;
        this.serviceToRevisions = localServiceToRevisions;

        Map<String, List<ServiceInstance>> localServiceToInstances = new HashMap<>();
        for (String serviceKey : localServiceToRevisions.keySet()) {
            if (CollectionUtils.equals(localRevisionToInstances.keySet(), localServiceToRevisions.get(serviceKey))) {
                localServiceToInstances.put(serviceKey, instances);
            }
        }

        this.serviceToInstances = localServiceToInstances;
    }

    public List<ServiceInstance> getInstances(String serviceKey) {
        if (serviceToInstances.containsKey(serviceKey)) {
            return serviceToInstances.get(serviceKey);
        }

        Set<String> revisions = serviceToRevisions.get(serviceKey);
        List<ServiceInstance> allInstances = new LinkedList<>();
        for (String r : revisions) {
            allInstances.addAll(revisionToInstances.get(r));
        }
        return allInstances;
    }

    private Map<String, Set<String>> parse(String revision, MetadataInfo metadata, Map<String, Set<String>> localServiceToRevisions) {
        Map<String, ServiceInfo> serviceInfos = metadata.getServices();
        for (Map.Entry<String, ServiceInfo> entry : serviceInfos.entrySet()) {
            String serviceKey = entry.getValue().getServiceKey();
            Set<String> set = localServiceToRevisions.computeIfAbsent(serviceKey, k -> new HashSet<>());
            set.add(revision);
        }

        return localServiceToRevisions;
    }

    private MetadataInfo getMetadataInfo(ServiceInstance instance) {
        String metadataType = ServiceInstanceMetadataUtils.getMetadataStorageType(instance);

        MetadataInfo metadataInfo;
        try {
            if (REMOTE_METADATA_STORAGE_TYPE.equals(metadataType)) {
                RemoteMetadataServiceImpl remoteMetadataService = MetadataUtils.getRemoteMetadataService();
                metadataInfo = remoteMetadataService.getMetadata(instance);
            } else {
                MetadataService metadataServiceProxy = MetadataUtils.getMetadataServiceProxy(instance);
                metadataInfo = metadataServiceProxy.getMetadataInfo();
            }
        } catch (Exception e) {
            // TODO, load metadata backup
            metadataInfo = null;
        }
        return metadataInfo;
    }

    protected abstract void notifyAddresses();

    /**
     * Get the correlative service name
     *
     * @return the correlative service name
     */
    public final String getServiceName() {
        return serviceName;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }

    /**
     * @param event {@link ServiceInstancesChangedEvent event}
     * @return If service name matches, return <code>true</code>, or <code>false</code>
     */
    public final boolean accept(ServiceInstancesChangedEvent event) {
        return Objects.equals(serviceName, event.getServiceName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceInstancesChangedListener)) return false;
        ServiceInstancesChangedListener that = (ServiceInstancesChangedListener) o;
        return Objects.equals(getServiceName(), that.getServiceName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), getServiceName());
    }
}
