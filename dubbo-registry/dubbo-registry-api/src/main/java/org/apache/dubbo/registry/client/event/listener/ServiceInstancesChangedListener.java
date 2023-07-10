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

import org.apache.dubbo.common.ProtocolServiceKey;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataInfo.ServiceInfo;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.RetryServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceNotificationCustomizer;
import org.apache.dubbo.rpc.model.ApplicationModel;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_REFRESH_ADDRESS;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_ENABLE_EMPTY_PROTECTION;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.ENABLE_EMPTY_PROTECTION_KEY;
import static org.apache.dubbo.metadata.RevisionResolver.EMPTY_REVISION;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getExportedServicesRevision;

;

/**
 * TODO, refactor to move revision-metadata mapping to ServiceDiscovery. Instances should have already been mapped with metadata when reached here.
 * <p>
 * The operations of ServiceInstancesChangedListener should be synchronized.
 */
public class ServiceInstancesChangedListener {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ServiceInstancesChangedListener.class);

    protected final Set<String> serviceNames;
    protected final ServiceDiscovery serviceDiscovery;
    protected Map<String, Set<NotifyListenerWithKey>> listeners;

    protected AtomicBoolean destroyed = new AtomicBoolean(false);

    protected Map<String, List<ServiceInstance>> allInstances;
    protected Map<String, List<ProtocolServiceKeyWithUrls>> serviceUrls;

    private volatile long lastRefreshTime;
    private final Semaphore retryPermission;
    private volatile ScheduledFuture<?> retryFuture;
    private final ScheduledExecutorService scheduler;
    private volatile boolean hasEmptyMetadata;
    private final Set<ServiceInstanceNotificationCustomizer> serviceInstanceNotificationCustomizers;
    private final ApplicationModel applicationModel;


    public ServiceInstancesChangedListener(Set<String> serviceNames, ServiceDiscovery serviceDiscovery) {
        this.serviceNames = serviceNames;
        this.serviceDiscovery = serviceDiscovery;
        this.listeners = new ConcurrentHashMap<>();
        this.allInstances = new HashMap<>();
        this.serviceUrls = new HashMap<>();
        retryPermission = new Semaphore(1);
        ApplicationModel applicationModel = ScopeModelUtil.getApplicationModel(serviceDiscovery == null || serviceDiscovery.getUrl() == null ? null : serviceDiscovery.getUrl().getScopeModel());
        this.scheduler = applicationModel.getBeanFactory().getBean(FrameworkExecutorRepository.class).getMetadataRetryExecutor();
        this.serviceInstanceNotificationCustomizers = applicationModel.getExtensionLoader(ServiceInstanceNotificationCustomizer.class).getSupportedExtensionInstances();
        this.applicationModel = applicationModel;
    }

    /**
     * On {@link ServiceInstancesChangedEvent the service instances change event}
     *
     * @param event {@link ServiceInstancesChangedEvent}
     */
    public void onEvent(ServiceInstancesChangedEvent event) {
        if (destroyed.get() || !accept(event) || isRetryAndExpired(event)) {
            return;
        }
        doOnEvent(event);
    }

    /**
     * @param event
     */
    private synchronized void doOnEvent(ServiceInstancesChangedEvent event) {
        if (destroyed.get() || !accept(event) || isRetryAndExpired(event)) {
            return;
        }

        refreshInstance(event);

        if (logger.isDebugEnabled()) {
            logger.debug(event.getServiceInstances().toString());
        }

        Map<String, List<ServiceInstance>> revisionToInstances = new HashMap<>();
        Map<ServiceInfo, Set<String>> localServiceToRevisions = new HashMap<>();

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

            MetadataInfo metadata = subInstances.stream()
                .map(ServiceInstance::getServiceMetadata)
                .filter(Objects::nonNull)
                .filter(m -> revision.equals(m.getRevision()))
                .findFirst()
                .orElseGet(() -> serviceDiscovery.getRemoteMetadata(revision, subInstances));

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
                try {
                    retryFuture = scheduler.schedule(new AddressRefreshRetryTask(retryPermission, event.getServiceName()), 10_000L, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    logger.error(INTERNAL_ERROR, "unknown error in registry module", "", "Error submitting async retry task.");
                }
                logger.warn(INTERNAL_ERROR, "unknown error in registry module", "", "Address refresh try task submitted");
            }

            // return if all metadata is empty, this notification will not take effect.
            if (emptyNum == revisionToInstances.size()) {
                // 1-17 - Address refresh failed.
                logger.error(REGISTRY_FAILED_REFRESH_ADDRESS, "metadata Server failure", "",
                    "Address refresh failed because of Metadata Server failure, wait for retry or new address refresh event.");

                return;
            }
        }
        hasEmptyMetadata = false;

        Map<String, Map<Integer, Map<Set<String>, Object>>> protocolRevisionsToUrls = new HashMap<>();
        Map<String, List<ProtocolServiceKeyWithUrls>> newServiceUrls = new HashMap<>();
        for (Map.Entry<ServiceInfo, Set<String>> entry : localServiceToRevisions.entrySet()) {
            ServiceInfo serviceInfo = entry.getKey();
            Set<String> revisions = entry.getValue();

            Map<Integer, Map<Set<String>, Object>> portToRevisions = protocolRevisionsToUrls.computeIfAbsent(serviceInfo.getProtocol(), k -> new HashMap<>());
            Map<Set<String>, Object> revisionsToUrls = portToRevisions.computeIfAbsent(serviceInfo.getPort(), k -> new HashMap<>());
            Object urls = revisionsToUrls.get(revisions);
            if (urls == null) {
                urls = getServiceUrlsCache(revisionToInstances, revisions, serviceInfo.getProtocol(), serviceInfo.getPort());
                revisionsToUrls.put(revisions, urls);
            }

            List<ProtocolServiceKeyWithUrls> list = newServiceUrls.computeIfAbsent(serviceInfo.getPath(), k -> new LinkedList<>());
            list.add(new ProtocolServiceKeyWithUrls(serviceInfo.getProtocolServiceKey(), (List<URL>) urls));
        }

        this.serviceUrls = newServiceUrls;
        this.notifyAddressChanged();
    }

    public synchronized void addListenerAndNotify(URL url, NotifyListener listener) {
        if (destroyed.get()) {
            return;
        }

        Set<NotifyListenerWithKey> notifyListeners = this.listeners.computeIfAbsent(url.getServiceKey(), _k -> new ConcurrentHashSet<>());
        String protocol = listener.getConsumerUrl().getParameter(PROTOCOL_KEY, url.getProtocol());
        ProtocolServiceKey protocolServiceKey = new ProtocolServiceKey(url.getServiceInterface(), url.getVersion(), url.getGroup(),
            !CommonConstants.CONSUMER.equals(protocol) ? protocol : null);
        NotifyListenerWithKey listenerWithKey = new NotifyListenerWithKey(protocolServiceKey, listener);
        notifyListeners.add(listenerWithKey);

        // Aggregate address and notify on subscription.
        List<URL> urls = getAddresses(protocolServiceKey, listener.getConsumerUrl());

        if (CollectionUtils.isNotEmpty(urls)) {
            logger.info(String.format("Notify serviceKey: %s, listener: %s with %s urls on subscription", protocolServiceKey, listener, urls.size()));
            listener.notify(urls);
        }
    }

    public synchronized void removeListener(String serviceKey, NotifyListener notifyListener) {
        if (destroyed.get()) {
            return;
        }

        // synchronized method, no need to use DCL
        Set<NotifyListenerWithKey> notifyListeners = this.listeners.get(serviceKey);
        if (notifyListeners != null) {
            notifyListeners.removeIf(listener -> listener.getNotifyListener().equals(notifyListener));

            // ServiceKey has no listener, remove set
            if (notifyListeners.size() == 0) {
                this.listeners.remove(serviceKey);
            }
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
            logger.warn(INTERNAL_ERROR, "unknown error in registry module", "", "Received address refresh retry event, " + retryEvent.getFailureRecordTime());
            if (retryEvent.getFailureRecordTime() < lastRefreshTime && !hasEmptyMetadata) {
                logger.warn(INTERNAL_ERROR, "unknown error in registry module", "", "Ignore retry event, event time: " + retryEvent.getFailureRecordTime() + ", last refresh time: " + lastRefreshTime);
                return true;
            }
            logger.warn(INTERNAL_ERROR, "unknown error in registry module", "", "Retrying address notification...");
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
        for (ServiceInstanceNotificationCustomizer serviceInstanceNotificationCustomizer : serviceInstanceNotificationCustomizers) {
            serviceInstanceNotificationCustomizer.customize(appInstances);
        }
        allInstances.put(appName, appInstances);
        lastRefreshTime = System.currentTimeMillis();
    }

    /**
     * Calculate the number of revisions that failed to find metadata info.
     *
     * @param revisionToInstances instance list classified by revisions
     * @return the number of revisions that failed at fetching MetadataInfo
     */
    protected int hasEmptyMetadata(Map<String, List<ServiceInstance>> revisionToInstances) {
        if (revisionToInstances == null) {
            return 0;
        }

        StringBuilder builder = new StringBuilder();
        int emptyMetadataNum = 0;
        for (Map.Entry<String, List<ServiceInstance>> entry : revisionToInstances.entrySet()) {
            DefaultServiceInstance serviceInstance = (DefaultServiceInstance) entry.getValue().get(0);
            if (serviceInstance == null || serviceInstance.getServiceMetadata() == MetadataInfo.EMPTY) {
                emptyMetadataNum++;
            }

            builder.append(entry.getKey());
            builder.append(' ');
        }

        if (emptyMetadataNum > 0) {
            builder.insert(0, emptyMetadataNum + "/" + revisionToInstances.size() + " revisions failed to get metadata from remote: ");
            logger.error(INTERNAL_ERROR, "unknown error in registry module", "", builder.toString());
        } else {
            builder.insert(0, revisionToInstances.size() + " unique working revisions: ");
            logger.info(builder.toString());
        }
        return emptyMetadataNum;
    }

    protected Map<ServiceInfo, Set<String>> parseMetadata(String revision, MetadataInfo metadata, Map<ServiceInfo, Set<String>> localServiceToRevisions) {
        Map<String, ServiceInfo> serviceInfos = metadata.getServices();
        for (Map.Entry<String, ServiceInfo> entry : serviceInfos.entrySet()) {
            Set<String> set = localServiceToRevisions.computeIfAbsent(entry.getValue(), _k -> new TreeSet<>());
            set.add(revision);
        }

        return localServiceToRevisions;
    }

    protected Object getServiceUrlsCache(Map<String, List<ServiceInstance>> revisionToInstances, Set<String> revisions, String protocol, int port) {
        List<URL> urls = new ArrayList<>();
        for (String r : revisions) {
            for (ServiceInstance i : revisionToInstances.get(r)) {
                if (port > 0) {
                    if (i.getPort() == port) {
                        urls.add(i.toURL(protocol).setScopeModel(i.getApplicationModel()));
                    } else {
                        urls.add(((DefaultServiceInstance) i).copyFrom(port).toURL(protocol).setScopeModel(i.getApplicationModel()));
                    }
                    continue;
                }
                // different protocols may have ports specified in meta
                if (ServiceInstanceMetadataUtils.hasEndpoints(i)) {
                    DefaultServiceInstance.Endpoint endpoint = ServiceInstanceMetadataUtils.getEndpoint(i, protocol);
                    if (endpoint != null && endpoint.getPort() != i.getPort()) {
                        urls.add(((DefaultServiceInstance) i).copyFrom(endpoint).toURL(endpoint.getProtocol()));
                        continue;
                    }
                }
                urls.add(i.toURL(protocol).setScopeModel(i.getApplicationModel()));
            }
        }
        return urls;
    }

    protected List<URL> getAddresses(ProtocolServiceKey protocolServiceKey, URL consumerURL) {
        List<ProtocolServiceKeyWithUrls> protocolServiceKeyWithUrlsList = serviceUrls.get(protocolServiceKey.getInterfaceName());
        List<URL> urls = new ArrayList<>();
        if (protocolServiceKeyWithUrlsList != null) {
            for (ProtocolServiceKeyWithUrls protocolServiceKeyWithUrls : protocolServiceKeyWithUrlsList) {
                if (ProtocolServiceKey.Matcher.isMatch(protocolServiceKey, protocolServiceKeyWithUrls.getProtocolServiceKey())) {
                    urls.addAll(protocolServiceKeyWithUrls.getUrls());
                }
            }
        }
        if (serviceUrls.containsKey(CommonConstants.ANY_VALUE)) {
            for (ProtocolServiceKeyWithUrls protocolServiceKeyWithUrls : serviceUrls.get(CommonConstants.ANY_VALUE)) {
                urls.addAll(protocolServiceKeyWithUrls.getUrls());
            }
        }
        return urls;
    }

    /**
     * race condition is protected by onEvent/doOnEvent
     */
    protected void notifyAddressChanged() {

        MetricsEventBus.post(RegistryEvent.toNotifyEvent(applicationModel),
            () -> {
                Map<String, Integer> lastNumMap = new HashMap<>();
                // 1 different services
                listeners.forEach((serviceKey, listenerSet) -> {
                    // 2 multiple subscription listener of the same service
                    for (NotifyListenerWithKey listenerWithKey : listenerSet) {
                        NotifyListener notifyListener = listenerWithKey.getNotifyListener();

                        List<URL> urls = toUrlsWithEmpty(getAddresses(listenerWithKey.getProtocolServiceKey(), notifyListener.getConsumerUrl()));
                        logger.info("Notify service " + listenerWithKey.getProtocolServiceKey() + " with urls " + urls.size());
                        notifyListener.notify(urls);
                        lastNumMap.put(serviceKey, urls.size());
                    }
                });
                return lastNumMap;
            }
        );

    }

    protected List<URL> toUrlsWithEmpty(List<URL> urls) {
        boolean emptyProtectionEnabled = serviceDiscovery.getUrl().getParameter(ENABLE_EMPTY_PROTECTION_KEY, DEFAULT_ENABLE_EMPTY_PROTECTION);
        if (!emptyProtectionEnabled && urls == null) {
            urls = new ArrayList<>();
        } else if (emptyProtectionEnabled && urls == null) {
            urls = Collections.emptyList();
        }

        if (CollectionUtils.isEmpty(urls) && !emptyProtectionEnabled) {
            // notice that the service of this.url may not be the same as notify listener.
            URL empty = URLBuilder.from(serviceDiscovery.getUrl()).setProtocol(EMPTY_PROTOCOL).build();
            urls.add(empty);
        }
        return urls;
    }

    /**
     * Since this listener is shared among interfaces, destroy this listener only when all interface listener are unsubscribed
     */
    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            logger.info("Destroying instance listener of  " + this.getServiceNames());
            serviceDiscovery.removeServiceInstancesChangedListener(this);
            synchronized (this) {
                allInstances.clear();
                serviceUrls.clear();
                listeners.clear();
                if (retryFuture != null && !retryFuture.isDone()) {
                    retryFuture.cancel(true);
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
        return Objects.equals(getServiceNames(), that.getServiceNames()) && Objects.equals(listeners, that.listeners);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), getServiceNames(), listeners);
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

    public static class NotifyListenerWithKey {
        private final ProtocolServiceKey protocolServiceKey;
        private final NotifyListener notifyListener;

        public NotifyListenerWithKey(ProtocolServiceKey protocolServiceKey, NotifyListener notifyListener) {
            this.protocolServiceKey = protocolServiceKey;
            this.notifyListener = notifyListener;
        }

        public ProtocolServiceKey getProtocolServiceKey() {
            return protocolServiceKey;
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
            return Objects.equals(protocolServiceKey, that.protocolServiceKey) && Objects.equals(notifyListener, that.notifyListener);
        }

        @Override
        public int hashCode() {
            return Objects.hash(protocolServiceKey, notifyListener);
        }
    }

    public static class ProtocolServiceKeyWithUrls {
        private final ProtocolServiceKey protocolServiceKey;
        private final List<URL> urls;

        public ProtocolServiceKeyWithUrls(ProtocolServiceKey protocolServiceKey, List<URL> urls) {
            this.protocolServiceKey = protocolServiceKey;
            this.urls = urls;
        }

        public ProtocolServiceKey getProtocolServiceKey() {
            return protocolServiceKey;
        }

        public List<URL> getUrls() {
            return urls;
        }
    }
}
