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
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;
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
import java.util.HashSet;
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

import static java.util.Collections.emptySet;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_CHAR_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.ENABLE_EMPTY_PROTECTION_KEY;
import static org.apache.dubbo.metadata.RevisionResolver.EMPTY_REVISION;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getExportedServicesRevision;

/**
 * TODO, refactor to move revision-metadata mapping to ServiceDiscovery. Instances should have already been mapped with metadata when reached here.
 * <p>
 * The operations of ServiceInstancesChangedListener should be synchronized.
 */
public class ServiceInstancesChangedListener {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInstancesChangedListener.class);

    protected final Set<String> serviceNames;
    protected final ServiceDiscovery serviceDiscovery;
    protected URL url;
    protected Map<String, Set<NotifyListenerWithKey>> listeners;

    protected AtomicBoolean destroyed = new AtomicBoolean(false);

    protected Map<String, List<ServiceInstance>> allInstances;
    protected Map<String, Object> serviceUrls;

    private volatile long lastRefreshTime;
    private final Semaphore retryPermission;
    private volatile ScheduledFuture<?> retryFuture;
    private final ScheduledExecutorService scheduler;
    private volatile boolean hasEmptyMetadata;

    // protocols subscribe by default, specify the protocol that should be subscribed through 'consumer.protocol'.
    private static final String[] SUPPORTED_PROTOCOLS = new String[]{"dubbo", "tri", "rest"};
    public static final String CONSUMER_PROTOCOL_SUFFIX = ":consumer";

    public ServiceInstancesChangedListener(Set<String> serviceNames, ServiceDiscovery serviceDiscovery) {
        this.serviceNames = serviceNames;
        this.serviceDiscovery = serviceDiscovery;
        this.listeners = new ConcurrentHashMap<>();
        this.allInstances = new HashMap<>();
        this.serviceUrls = new HashMap<>();
        retryPermission = new Semaphore(1);
        this.scheduler = ScopeModelUtil.getApplicationModel(serviceDiscovery == null || serviceDiscovery.getUrl() == null ? null : serviceDiscovery.getUrl().getScopeModel())
            .getBeanFactory().getBean(FrameworkExecutorRepository.class).getMetadataRetryExecutor();
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
            MetadataInfo metadata = serviceDiscovery.getRemoteMetadata(revision, subInstances);
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
                logger.warn("Address refresh try task submitted");
            }
            // return if all metadata is empty, this notification will not take effect.
            if (emptyNum == revisionToInstances.size()) {
                logger.error("Address refresh failed because of Metadata Server failure, wait for retry or new address refresh event.");
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
        if (destroyed.get()) {
            return;
        }

        Set<String> protocolServiceKeys = getProtocolServiceKeyList(serviceKey, listener);
        for (String protocolServiceKey : protocolServiceKeys) {
            // Add to global listeners
            if (!this.listeners.containsKey(serviceKey)) {
                // synchronized method, no need to use DCL
                this.listeners.put(serviceKey, new ConcurrentHashSet<>());
            }
            Set<NotifyListenerWithKey> notifyListeners = this.listeners.get(serviceKey);
            notifyListeners.add(new NotifyListenerWithKey(protocolServiceKey, listener));
        }

        List<URL> urls;
        if (protocolServiceKeys.size() > 1) {
            urls = new ArrayList<>();
            for (NotifyListenerWithKey notifyListenerWithKey : this.listeners.get(serviceKey)) {
                String protocolKey = notifyListenerWithKey.getProtocolServiceKey();
                List<URL> urlsOfProtocol = getAddresses(protocolKey, listener.getConsumerUrl());
                if (CollectionUtils.isNotEmpty(urlsOfProtocol)) {
                    urls.addAll(urlsOfProtocol);
                }
            }
        } else {
            String protocolKey = this.listeners.get(serviceKey).iterator().next().getProtocolServiceKey();
            urls = getAddresses(protocolKey, listener.getConsumerUrl());
        }

        if (CollectionUtils.isNotEmpty(urls)) {
            listener.notify(urls);
        }
    }

    public synchronized void removeListener(String serviceKey, NotifyListener notifyListener) {
        if (destroyed.get()) {
            return;
        }

        for (String protocolServiceKey : getProtocolServiceKeyList(serviceKey, notifyListener)) {
            // synchronized method, no need to use DCL
            Set<NotifyListenerWithKey> notifyListeners = this.listeners.get(serviceKey);
            if (notifyListeners != null) {
                NotifyListenerWithKey listenerWithKey = new NotifyListenerWithKey(protocolServiceKey, notifyListener);
                // Remove from global listeners
                notifyListeners.remove(listenerWithKey);

                // ServiceKey has no listener, remove set
                if (notifyListeners.size() == 0) {
                    this.listeners.remove(serviceKey);
                }
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
            builder.append(" ");
        }

        if (emptyMetadataNum > 0) {
            builder.insert(0, emptyMetadataNum + "/" + revisionToInstances.size() + " revisions failed to get metadata from remote: ");
            logger.error(builder.toString());
        } else {
            builder.insert(0, revisionToInstances.size() + " unique working revisions: ");
            logger.info(builder.toString());
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

    protected Object getServiceUrlsCache(Map<String, List<ServiceInstance>> revisionToInstances, Set<String> revisions, String protocol) {
        List<URL> urls = new ArrayList<>();
        for (String r : revisions) {
            for (ServiceInstance i : revisionToInstances.get(r)) {
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

    protected List<URL> getAddresses(String serviceProtocolKey, URL consumerURL) {
        return (List<URL>) serviceUrls.get(serviceProtocolKey);
    }

    /**
     * race condition is protected by onEvent/doOnEvent
     */
    protected void notifyAddressChanged() {
        listeners.forEach((serviceKey, listenerSet) -> {
            if (listenerSet != null) {
                if (listenerSet.size() == 1) {
                    NotifyListenerWithKey listenerWithKey = listenerSet.iterator().next();
                    String protocolServiceKey = listenerWithKey.getProtocolServiceKey();
                    NotifyListener notifyListener = listenerWithKey.getNotifyListener();
                    //FIXME, group wildcard match
                    List<URL> urls = toUrlsWithEmpty(getAddresses(protocolServiceKey, notifyListener.getConsumerUrl()));
                    logger.info("Notify service " + serviceKey + " with urls " + urls.size());
                    notifyListener.notify(urls);
                } else {
                    List<URL> urls = new ArrayList<>();
                    NotifyListener notifyListener = null;
                    for (NotifyListenerWithKey listenerWithKey : listenerSet) {
                        String protocolServiceKey = listenerWithKey.getProtocolServiceKey();
                        notifyListener = listenerWithKey.getNotifyListener();
                        List<URL> tmpUrls = getAddresses(protocolServiceKey, notifyListener.getConsumerUrl());
                        if (CollectionUtils.isNotEmpty(tmpUrls)) {
                            urls.addAll(tmpUrls);
                        }
                    }
                    if (notifyListener != null) {
                        logger.info("Notify service " + serviceKey + " with urls " + urls.size());
                        urls = toUrlsWithEmpty(urls);
                        notifyListener.notify(urls);
                    }
                }
            }
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
        return Objects.equals(getServiceNames(), that.getServiceNames());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), getServiceNames());
    }

    /**
     * Calculate the protocol list that the consumer cares about.
     *
     * @param serviceKey possible input serviceKey includes
     *                   1. {group}/{interface}:{version}:consumer
     *                   2. {group}/{interface}:{version}:{user specified protocols}
     * @param listener   listener also contains the user specified protocols
     * @return protocol list with the format {group}/{interface}:{version}:{protocol}
     */
    protected Set<String> getProtocolServiceKeyList(String serviceKey, NotifyListener listener) {
        if (StringUtils.isEmpty(serviceKey)) {
            return emptySet();
        }

        Set<String> result = new HashSet<>();
        String protocol = listener.getConsumerUrl().getParameter(PROTOCOL_KEY);
        if (serviceKey.endsWith(CONSUMER_PROTOCOL_SUFFIX)) {
            serviceKey = serviceKey.substring(0, serviceKey.indexOf(CONSUMER_PROTOCOL_SUFFIX));
        }

        if (StringUtils.isNotEmpty(protocol)) {
            int protocolIndex = serviceKey.indexOf(":" + protocol);
            if (protocol.contains(",") && protocolIndex != -1) {
                serviceKey = serviceKey.substring(0, protocolIndex);
                String[] specifiedProtocols = protocol.split(",");
                for (String specifiedProtocol : specifiedProtocols) {
                    result.add(serviceKey + GROUP_CHAR_SEPARATOR + specifiedProtocol);
                }
            } else {
                result.add(serviceKey);
            }
        } else {
            for (String supportedProtocol : SUPPORTED_PROTOCOLS) {
                result.add(serviceKey + GROUP_CHAR_SEPARATOR + supportedProtocol);
            }
        }

        return result;
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
        private final String protocolServiceKey;
        private final NotifyListener notifyListener;

        public NotifyListenerWithKey(String protocolServiceKey, NotifyListener notifyListener) {
            this.protocolServiceKey = protocolServiceKey;
            this.notifyListener = notifyListener;
        }

        public String getProtocolServiceKey() {
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
}
