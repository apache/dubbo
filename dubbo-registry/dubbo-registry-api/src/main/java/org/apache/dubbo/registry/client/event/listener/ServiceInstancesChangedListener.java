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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.event.ConditionalEventListener;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataInfo.ServiceInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.RegistryClusterIdentifier;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.RetryServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.metadata.MetadataUtils;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.registry.client.metadata.store.RemoteMetadataServiceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_CLUSTER_KEY;
import static org.apache.dubbo.metadata.RevisionResolver.EMPTY_REVISION;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getExportedServicesRevision;

/**
 * The Service Discovery Changed {@link EventListener Event Listener}
 *
 * @see ServiceInstancesChangedEvent
 * @since 2.7.5
 */
public class ServiceInstancesChangedListener implements ConditionalEventListener<ServiceInstancesChangedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInstancesChangedListener.class);

    private final Set<String> serviceNames;
    private final ServiceDiscovery serviceDiscovery;
    private URL url;
    private Map<String, NotifyListener> listeners;

    private Map<String, List<ServiceInstance>> allInstances;

    private Map<String, List<URL>> serviceUrls;

    private Map<String, MetadataInfo> revisionToMetadata;

    private volatile long lastRefreshTime;
    private volatile long lastFailureTime;
    private volatile AtomicInteger failureCounter = new AtomicInteger(0);
    private Semaphore retryPermission;

    private ScheduledExecutorService scheduler;

    public ServiceInstancesChangedListener(Set<String> serviceNames, ServiceDiscovery serviceDiscovery) {
        this.serviceNames = serviceNames;
        this.serviceDiscovery = serviceDiscovery;
        this.listeners = new HashMap<>();
        this.allInstances = new HashMap<>();
        this.serviceUrls = new HashMap<>();
        this.revisionToMetadata = new HashMap<>();
        retryPermission = new Semaphore(1);
        this.scheduler = ExtensionLoader.getExtensionLoader(ExecutorRepository.class).getDefaultExtension().getMetadataRetryExecutor();
    }

    /**
     * On {@link ServiceInstancesChangedEvent the service instances change event}
     *
     * @param event {@link ServiceInstancesChangedEvent}
     */
    public synchronized void onEvent(ServiceInstancesChangedEvent event) {
        if (this.isRetryAndExpired(event)) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(event.getServiceInstances().toString());
        }

        Map<String, List<ServiceInstance>> revisionToInstances = new HashMap<>();
        Map<ServiceInfo, Set<String>> localServiceToRevisions = new HashMap<>();
        Map<String, Map<Set<String>, List<URL>>> protocolRevisionsToUrls = new HashMap<>();
        Map<String, List<URL>> newServiceUrls = new HashMap<>();//TODO
        Map<String, MetadataInfo> newRevisionToMetadata = new HashMap<>();

        for (Map.Entry<String, List<ServiceInstance>> entry : allInstances.entrySet()) {
            List<ServiceInstance> instances = entry.getValue();
            for (ServiceInstance instance : instances) {
                String revision = getExportedServicesRevision(instance);
                if (EMPTY_REVISION.equals(revision)) {
                    logger.info("Find instance without valid service metadata: " + instance.getAddress());
                    continue;
                }
                List<ServiceInstance> subInstances = revisionToInstances.computeIfAbsent(revision, r -> new LinkedList<>());
                subInstances.add(instance);

                MetadataInfo metadata = getRemoteMetadata(instance, revision, localServiceToRevisions, subInstances);

                // it means fetching Meta Server failed if metadata is null
                ((DefaultServiceInstance) instance).setServiceMetadata(metadata);
                newRevisionToMetadata.putIfAbsent(revision, metadata);
            }
        }

        logger.info(newRevisionToMetadata.size() + " unique revisions. ");

        if (hasEmptyMetadata(newRevisionToMetadata)) {// retry every 10 seconds
            if (retryPermission.tryAcquire()) {
                scheduler.schedule(new AddressRefreshRetryTask(retryPermission), 10000, TimeUnit.MILLISECONDS);
                logger.warn("Address refresh try task submitted.");
            }
            logger.warn("Address refresh failed because of Metadata Server failure, wait for retry or new address refresh event.");
            this.revisionToMetadata = newRevisionToMetadata;
            return;
        }

        this.revisionToMetadata = newRevisionToMetadata;

        localServiceToRevisions.forEach((serviceInfo, revisions) -> {
            String protocol = serviceInfo.getProtocol();
            Map<Set<String>, List<URL>> revisionsToUrls = protocolRevisionsToUrls.computeIfAbsent(protocol, k -> {
                return new HashMap<>();
            });
            List<URL> urls = revisionsToUrls.get(revisions);
            if (urls != null) {
                newServiceUrls.put(serviceInfo.getMatchKey(), urls);
            } else {
                urls = new ArrayList<>();
                for (String r : revisions) {
                    for (ServiceInstance i : revisionToInstances.get(r)) {
                        // different protocols may have ports specified in meta
                        if (ServiceInstanceMetadataUtils.hasEndpoints(i)) {
                            DefaultServiceInstance.Endpoint endpoint = ServiceInstanceMetadataUtils.getEndpoint(i, protocol);
                            if (endpoint != null && !endpoint.getPort().equals(i.getPort())) {
                                urls.add(((DefaultServiceInstance)i).copy(endpoint).toURL());
                                break;
                            }
                        }
                        urls.add(i.toURL());
                    }
                }
                revisionsToUrls.put(revisions, urls);
                newServiceUrls.put(serviceInfo.getMatchKey(), urls);
            }
        });
        this.serviceUrls = newServiceUrls;

        this.notifyAddressChanged();
    }

    public synchronized void addListenerAndNotify(String serviceKey, NotifyListener listener) {
        this.listeners.put(serviceKey, listener);
        List<URL> urls = this.serviceUrls.get(serviceKey);
        if (CollectionUtils.isNotEmpty(urls)) {
            listener.notify(urls);
        }
    }

    public void removeListener(String serviceKey) {
        listeners.remove(serviceKey);
        if (listeners.isEmpty()) {
            serviceDiscovery.removeServiceInstancesChangedListener(this);
        }
    }

    public List<URL> getUrls(String serviceKey) {
        return toUrlsWithEmpty(serviceUrls.get(serviceKey));
    }

    /**
     * Get the correlative service name
     *
     * @return the correlative service name
     */
    public final Set<String> getServiceNames() {
        return serviceNames;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }

    public Map<String, List<ServiceInstance>> getAllInstances() {
        return allInstances;
    }

    public List<ServiceInstance> getInstancesOfApp(String appName) {
        return allInstances.get(appName);
    }

    public Map<String, MetadataInfo> getRevisionToMetadata() {
        return revisionToMetadata;
    }

    public MetadataInfo getMetadata(String revision) {
        return revisionToMetadata.get(revision);
    }

    /**
     * @param event {@link ServiceInstancesChangedEvent event}
     * @return If service name matches, return <code>true</code>, or <code>false</code>
     */
    public final boolean accept(ServiceInstancesChangedEvent event) {
        return serviceNames.contains(event.getServiceName());
    }

    private boolean isRetryAndExpired(ServiceInstancesChangedEvent event) {
        String appName = event.getServiceName();
        List<ServiceInstance> appInstances = event.getServiceInstances();

        if (event instanceof RetryServiceInstancesChangedEvent) {
            RetryServiceInstancesChangedEvent retryEvent = (RetryServiceInstancesChangedEvent) event;
            logger.warn("Received address refresh retry event, " + retryEvent.getFailureRecordTime());
            if (retryEvent.getFailureRecordTime() < lastRefreshTime) {
                logger.warn("Ignore retry event, event time: " + retryEvent.getFailureRecordTime() + ", last refresh time: " + lastRefreshTime);
                return true;
            }
            logger.warn("Retrying address notification...");
        } else {
            logger.info("Received instance notification, serviceName: " + appName + ", instances: " + appInstances.size());
            allInstances.put(appName, appInstances);
            lastRefreshTime = System.currentTimeMillis();
        }
        return false;
    }

    private boolean hasEmptyMetadata(Map<String, MetadataInfo> revisionToMetadata) {
        if (revisionToMetadata == null) {
            return false;
        }
        boolean result = false;
        for (Map.Entry<String, MetadataInfo> entry : revisionToMetadata.entrySet()) {
            if (entry.getValue() == null) {
                result = true;
                break;
            }
        }
        return result;
    }

    private MetadataInfo getRemoteMetadata(ServiceInstance instance, String revision, Map<ServiceInfo, Set<String>> localServiceToRevisions, List<ServiceInstance> subInstances) {
        MetadataInfo metadata = revisionToMetadata.get(revision);
        if (metadata == null) {
            if (failureCounter.get() < 3 || (System.currentTimeMillis() - lastFailureTime > 10000)) {
                metadata = getMetadataInfo(instance);
                if (metadata != null) {
                    logger.info("MetadataInfo for instance " + instance.getAddress() + "?revision=" + revision + " is " + metadata);
                    failureCounter.set(0);
                    revisionToMetadata.putIfAbsent(revision, metadata);
                    parseMetadata(revision, metadata, localServiceToRevisions);
                } else {
                    logger.error("Failed to get MetadataInfo for instance " + instance.getAddress() + "?revision=" + revision
                            + ", wait for retry.");
                    lastFailureTime = System.currentTimeMillis();
                    failureCounter.incrementAndGet();
                }
            }
        } else if (subInstances.size() == 1) {
            // "subInstances.size() >= 2" means metadata of this revision has been parsed, ignore
            parseMetadata(revision, metadata, localServiceToRevisions);
        }
        return metadata;
    }

    private Map<ServiceInfo, Set<String>> parseMetadata(String revision, MetadataInfo metadata, Map<ServiceInfo, Set<String>> localServiceToRevisions) {
        Map<String, ServiceInfo> serviceInfos = metadata.getServices();
        for (Map.Entry<String, ServiceInfo> entry : serviceInfos.entrySet()) {
            Set<String> set = localServiceToRevisions.computeIfAbsent(entry.getValue(), k -> new TreeSet<>());
            set.add(revision);
        }

        return localServiceToRevisions;
    }

    private MetadataInfo getMetadataInfo(ServiceInstance instance) {
        String metadataType = ServiceInstanceMetadataUtils.getMetadataStorageType(instance);
        // FIXME, check "REGISTRY_CLUSTER_KEY" must be set by every registry implementation.
        instance.getExtendParams().putIfAbsent(REGISTRY_CLUSTER_KEY, RegistryClusterIdentifier.getExtension(url).consumerKey(url));
        MetadataInfo metadataInfo;
        try {
            if (logger.isDebugEnabled()) {
                logger.info("Instance " + instance.getAddress() + " is using metadata type " + metadataType);
            }
            if (REMOTE_METADATA_STORAGE_TYPE.equals(metadataType)) {
                RemoteMetadataServiceImpl remoteMetadataService = MetadataUtils.getRemoteMetadataService();
                metadataInfo = remoteMetadataService.getMetadata(instance);
            } else {
                MetadataService metadataServiceProxy = MetadataUtils.getMetadataServiceProxy(instance, serviceDiscovery);
                metadataInfo = metadataServiceProxy.getMetadataInfo(ServiceInstanceMetadataUtils.getExportedServicesRevision(instance));
            }
            if (logger.isDebugEnabled()) {
                logger.info("Metadata " + metadataInfo.toString());
            }
        } catch (Exception e) {
            logger.error("Failed to load service metadata, meta type is " + metadataType, e);
            metadataInfo = null;
        }
        return metadataInfo;
    }

    private void notifyAddressChanged() {
        listeners.forEach((key, notifyListener) -> {
            //FIXME, group wildcard match
            List<URL> urls = toUrlsWithEmpty(serviceUrls.get(key));
            logger.info("Notify service " + key + " with urls " + urls.size());
            notifyListener.notify(urls);
        });
    }

    private List<URL> toUrlsWithEmpty(List<URL> urls) {
        if (urls == null) {
            urls = Collections.emptyList();
        }
        return urls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceInstancesChangedListener)) return false;
        ServiceInstancesChangedListener that = (ServiceInstancesChangedListener) o;
        return Objects.equals(getServiceNames(), that.getServiceNames());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), getServiceNames());
    }

    private class AddressRefreshRetryTask implements Runnable {
        private final RetryServiceInstancesChangedEvent retryEvent;
        private final Semaphore retryPermission;

        public AddressRefreshRetryTask(Semaphore semaphore) {
            this.retryEvent = new RetryServiceInstancesChangedEvent();
            this.retryPermission = semaphore;
        }

        @Override
        public void run() {
            retryPermission.release();
            ServiceInstancesChangedListener.this.onEvent(retryEvent);
        }
    }
}
