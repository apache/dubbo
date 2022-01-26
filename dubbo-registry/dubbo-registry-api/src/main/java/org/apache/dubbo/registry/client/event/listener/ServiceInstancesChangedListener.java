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
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataInfo.ServiceInfo;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.RetryServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.ENABLE_EMPTY_PROTECTION_KEY;
import static org.apache.dubbo.metadata.RevisionResolver.EMPTY_REVISION;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getExportedServicesRevision;

/**
 * TODO, refactor to move revision-metadata mapping to ServiceDiscovery. Instances should already being mapped with metadata when reached here.
 */
public class ServiceInstancesChangedListener {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInstancesChangedListener.class);

    protected final Set<String> serviceNames;
    protected final ServiceDiscovery serviceDiscovery;
    protected URL url;
    protected Map<String, Set<NotifyListener>> listeners;
    protected ConcurrentLinkedQueue<NotifyListenerWithKey> listenerQueue;

    protected AtomicBoolean destroyed = new AtomicBoolean(false);

    protected Map<String, List<ServiceInstance>> allInstances;
    protected Map<String, Object> serviceUrls;

    private volatile long lastRefreshTime;
    private final Semaphore retryPermission;
    private volatile ScheduledFuture<?> retryFuture;
    private final ScheduledExecutorService scheduler;
    private volatile boolean hasEmptyMetadata;

    public ServiceInstancesChangedListener(Set<String> serviceNames, ServiceDiscovery serviceDiscovery) {
        this.serviceNames = serviceNames;
        this.serviceDiscovery = serviceDiscovery;
        this.listeners = new ConcurrentHashMap<>();
        this.listenerQueue = new ConcurrentLinkedQueue<>();
        this.allInstances = new HashMap<>();
        this.serviceUrls = new HashMap<>();
        retryPermission = new Semaphore(1);
        this.scheduler = ScopeModelUtil.getApplicationModel(serviceDiscovery == null || serviceDiscovery.getUrl() == null ? null : serviceDiscovery.getUrl().getScopeModel())
            .getExtensionLoader(ExecutorRepository.class).getDefaultExtension().getMetadataRetryExecutor();
    }

    /**
     * On {@link ServiceInstancesChangedEvent the service instances change event}
     *
     * @param event {@link ServiceInstancesChangedEvent}
     */
    public synchronized void onEvent(ServiceInstancesChangedEvent event) {
        if (destroyed.get() || !accept(event) || isRetryAndExpired(event)) {
            return;
        }

        refreshInstance(event);

        if (logger.isDebugEnabled()) {
            logger.debug(event.getServiceInstances().toString());
        }

        Map<String, List<ServiceInstance>> revisionToInstances = new HashMap<>();
        Map<String, Map<String, Set<String>>> localServiceToRevisions = new HashMap<>();

        // grouping all instances of this app(service name) by revision
        for (Map.Entry<String, List<ServiceInstance>> entry : allInstances.entrySet()) {
            List<ServiceInstance> instances = entry.getValue();
            for (ServiceInstance instance : instances) {
                String revision = getExportedServicesRevision(instance);
                if (revision == null || EMPTY_REVISION.equals(revision)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Find instance without valid service metadata: " + instance.getAddress());
                    }
                    continue;
                }
                List<ServiceInstance> subInstances = revisionToInstances.computeIfAbsent(revision, r -> new LinkedList<>());
                subInstances.add(instance);
            }
        }

        // get MetadataInfo with revision
        for (Map.Entry<String, List<ServiceInstance>> entry : revisionToInstances.entrySet()) {
            String revision = entry.getKey();
            List<ServiceInstance> subInstances = entry.getValue();
            ServiceInstance instance = selectInstance(subInstances);
            MetadataInfo metadata = serviceDiscovery.getRemoteMetadata(revision, instance);
            parseMetadata(revision, metadata, localServiceToRevisions);
            // update metadata into each instance, in case new instance created.
            for (ServiceInstance tmpInstance : subInstances) {
                MetadataInfo originMetadata = tmpInstance.getServiceMetadata();
                if (originMetadata == null || !Objects.equals(originMetadata.getRevision(), metadata.getRevision())) {
                    tmpInstance.setServiceMetadata(metadata);
                }
            }
        }

        int emptyNum = hasEmptyMetadata(revisionToInstances);
        if (emptyNum != 0) {// retry every 10 seconds
            hasEmptyMetadata = true;
            if (retryPermission.tryAcquire()) {
                if (retryFuture != null && !retryFuture.isDone()) {
                    // cancel last retryFuture because only one retryFuture will be canceled at destroy().
                    retryFuture.cancel(true);
                }
                retryFuture = scheduler.schedule(new AddressRefreshRetryTask(retryPermission, event.getServiceName()), 10_000L, TimeUnit.MILLISECONDS);
                logger.warn("Address refresh try task submitted.");
            }
            logger.error("Address refresh failed because of Metadata Server failure, wait for retry or new address refresh event.");
            if (emptyNum == revisionToInstances.size()) {// return if all metadata is empty
                return;
            }
        }
        hasEmptyMetadata = false;

        Map<String, Map<Set<String>, Object>> protocolRevisionsToUrls = new HashMap<>();
        Map<String, Object> newServiceUrls = new HashMap<>();
        for (Map.Entry<String, Map<String, Set<String>>> entry : localServiceToRevisions.entrySet()) {
            String protocol = entry.getKey();
            entry.getValue().forEach((protocolServiceKey, revisions) -> {
                Map<Set<String>, Object> revisionsToUrls = protocolRevisionsToUrls.computeIfAbsent(protocol, k -> new HashMap<>());
                Object urls = revisionsToUrls.get(revisions);
                if (urls == null) {
                    urls = getServiceUrlsCache(revisionToInstances, revisions, protocol);
                    revisionsToUrls.put(revisions, urls);
                }

                newServiceUrls.put(protocolServiceKey, urls);
            });
        }

        this.serviceUrls = newServiceUrls;
        this.notifyAddressChanged();
    }

    public synchronized void addListenerAndNotify(String serviceKey, NotifyListener listener) {
        // Add to global listeners
        if (!this.listeners.containsKey(serviceKey)) {
            // synchronized method, no need to use DCL
            this.listeners.put(serviceKey, new ConcurrentHashSet<>());
        }
        Set<NotifyListener> notifyListeners = this.listeners.get(serviceKey);

        if (notifyListeners.add(listener)) {
            // Add to notify queue
            NotifyListenerWithKey listenerWithKey = new NotifyListenerWithKey(serviceKey, listener);
            listenerQueue.offer(listenerWithKey);
        }

        List<URL> urls = getAddresses(serviceKey, listener.getConsumerUrl());
        if (CollectionUtils.isNotEmpty(urls)) {
            listener.notify(urls);
        }
    }

    public synchronized void removeListener(String serviceKey, NotifyListener notifyListener) {
        // synchronized method, no need to use DCL
        Set<NotifyListener> notifyListeners = this.listeners.get(serviceKey);
        if (notifyListeners != null) {
            if (notifyListeners.contains(notifyListener)) {
                // Remove from global listeners
                notifyListeners.remove(notifyListener);

                // Remove from notify queue
                NotifyListenerWithKey listenerWithKey = new NotifyListenerWithKey(serviceKey, notifyListener);
                listenerQueue.remove(listenerWithKey);
            }

            // ServiceKey has no listener, remove set
            if (notifyListeners.size() == 0) {
                this.listeners.remove(serviceKey);
            }
        }

        logger.info("Interface listener of interface " + serviceKey + " removed.");
        if (listeners.isEmpty()) {
            logger.info("No interface listeners exist, will stop instance listener for " + this.getServiceNames());
            serviceDiscovery.removeServiceInstancesChangedListener(this);
        }
    }

    public boolean hasListeners() {
        return CollectionUtils.isNotEmptyMap(listeners);
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

    /**
     * @param event {@link ServiceInstancesChangedEvent event}
     * @return If service name matches, return <code>true</code>, or <code>false</code>
     */
    private boolean accept(ServiceInstancesChangedEvent event) {
        return serviceNames.contains(event.getServiceName());
    }

    protected boolean isRetryAndExpired(ServiceInstancesChangedEvent event) {
        if (event instanceof RetryServiceInstancesChangedEvent) {
            RetryServiceInstancesChangedEvent retryEvent = (RetryServiceInstancesChangedEvent) event;
            logger.warn("Received address refresh retry event, " + retryEvent.getFailureRecordTime());
            if (retryEvent.getFailureRecordTime() < lastRefreshTime && !hasEmptyMetadata) {
                logger.warn("Ignore retry event, event time: " + retryEvent.getFailureRecordTime() + ", last refresh time: " + lastRefreshTime);
                return true;
            }
            logger.warn("Retrying address notification...");
        }
        return false;
    }

    private void refreshInstance(ServiceInstancesChangedEvent event) {
        if (event instanceof RetryServiceInstancesChangedEvent) {
            return;
        }
        String appName = event.getServiceName();
        List<ServiceInstance> appInstances = event.getServiceInstances();
        logger.info("Received instance notification, serviceName: " + appName + ", instances: " + appInstances.size());
        allInstances.put(appName, appInstances);
        lastRefreshTime = System.currentTimeMillis();
    }

    protected int hasEmptyMetadata(Map<String, List<ServiceInstance>> revisionToInstances) {
        if (revisionToInstances == null) {
            return 0;
        }
        int emptyMetadataNum = 0;
        for (Map.Entry<String, List<ServiceInstance>> entry : revisionToInstances.entrySet()) {
            DefaultServiceInstance serviceInstance = (DefaultServiceInstance) entry.getValue().get(0);
            if (serviceInstance == null || serviceInstance.getServiceMetadata() == MetadataInfo.EMPTY) {
                emptyMetadataNum++;
            }
        }
        return emptyMetadataNum;
    }

    protected Map<String, Map<String, Set<String>>> parseMetadata(String revision, MetadataInfo metadata, Map<String, Map<String, Set<String>>> localServiceToRevisions) {
        Map<String, ServiceInfo> serviceInfos = metadata.getServices();
        for (Map.Entry<String, ServiceInfo> entry : serviceInfos.entrySet()) {
            String protocol = entry.getValue().getProtocol();
            String protocolServiceKey = entry.getValue().getMatchKey();
            Map<String, Set<String>> map = localServiceToRevisions.computeIfAbsent(protocol, _p -> new HashMap<>());
            Set<String> set = map.computeIfAbsent(protocolServiceKey, _k -> new TreeSet<>());
            set.add(revision);
        }

        return localServiceToRevisions;
    }

    private ServiceInstance selectInstance(List<ServiceInstance> instances) {
        if (instances.size() == 1) {
            return instances.get(0);
        }
        return instances.get(ThreadLocalRandom.current().nextInt(0, instances.size()));
    }

    protected Object getServiceUrlsCache(Map<String, List<ServiceInstance>> revisionToInstances, Set<String> revisions, String protocol) {
        List<URL> urls;
        urls = new ArrayList<>();
        for (String r : revisions) {
            for (ServiceInstance i : revisionToInstances.get(r)) {
                // different protocols may have ports specified in meta
                if (ServiceInstanceMetadataUtils.hasEndpoints(i)) {
                    DefaultServiceInstance.Endpoint endpoint = ServiceInstanceMetadataUtils.getEndpoint(i, protocol);
                    if (endpoint != null && endpoint.getPort() != i.getPort()) {
                        urls.add(((DefaultServiceInstance) i).copyFrom(endpoint).toURL());
                        continue;
                    }
                }
                urls.add(i.toURL().setScopeModel(i.getApplicationModel()));
            }
        }
        return urls;
    }

    protected List<URL> getAddresses(String serviceProtocolKey, URL consumerURL) {
        return (List<URL>) serviceUrls.get(serviceProtocolKey);
    }

    protected void notifyAddressChanged() {
        listenerQueue.forEach(listenerWithKey -> {
            String key = listenerWithKey.getServiceKey();
            NotifyListener notifyListener = listenerWithKey.getNotifyListener();
            //FIXME, group wildcard match
            List<URL> urls = toUrlsWithEmpty(getAddresses(key, notifyListener.getConsumerUrl()));
            logger.info("Notify service " + key + " with urls " + urls.size());
            notifyListener.notify(urls);
        });
    }

    protected List<URL> toUrlsWithEmpty(List<URL> urls) {
        if (urls == null) {
            urls = Collections.emptyList();
        }
        boolean emptyProtectionEnabled = serviceDiscovery.getUrl().getParameter(ENABLE_EMPTY_PROTECTION_KEY, true);
        if (CollectionUtils.isEmpty(urls) && !emptyProtectionEnabled) {
            // notice that the service of this.url may not be the same as notify listener.
            URL empty = URLBuilder.from(this.url)
                .setProtocol(EMPTY_PROTOCOL)
                .build();
            urls.add(empty);
        }
        return urls;
    }

    /**
     * Since this listener is shared among interfaces, destroy this listener only when all interface listener are unsubscribed
     */
    public synchronized void destroy() {
        if (!destroyed.get()) {
            if (CollectionUtils.isEmptyMap(listeners)) {
                if (destroyed.compareAndSet(false, true)) {
                    allInstances.clear();
                    serviceUrls.clear();
                    if (retryFuture != null && !retryFuture.isDone()) {
                        retryFuture.cancel(true);
                    }
                }
            }
        }
    }

    public boolean isDestroyed() {
        return destroyed.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceInstancesChangedListener)) {
            return false;
        }
        ServiceInstancesChangedListener that = (ServiceInstancesChangedListener) o;
        return Objects.equals(getServiceNames(), that.getServiceNames());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), getServiceNames());
    }

    // for test purpose
    public List<ServiceInstance> getInstancesOfApp(String appName) {
        return allInstances.get(appName);
    }

    protected class AddressRefreshRetryTask implements Runnable {
        private final RetryServiceInstancesChangedEvent retryEvent;
        private final Semaphore retryPermission;

        public AddressRefreshRetryTask(Semaphore semaphore, String serviceName) {
            this.retryEvent = new RetryServiceInstancesChangedEvent(serviceName);
            this.retryPermission = semaphore;
        }

        @Override
        public void run() {
            retryPermission.release();
            ServiceInstancesChangedListener.this.onEvent(retryEvent);
        }
    }

    protected static class NotifyListenerWithKey {
        private String serviceKey;
        private NotifyListener notifyListener;

        public NotifyListenerWithKey(String serviceKey, NotifyListener notifyListener) {
            this.serviceKey = serviceKey;
            this.notifyListener = notifyListener;
        }

        public String getServiceKey() {
            return serviceKey;
        }

        public NotifyListener getNotifyListener() {
            return notifyListener;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NotifyListenerWithKey that = (NotifyListenerWithKey) o;
            return Objects.equals(serviceKey, that.serviceKey) && Objects.equals(notifyListener, that.notifyListener);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serviceKey, notifyListener);
        }
    }
}
