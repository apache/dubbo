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
package org.apache.dubbo.registry.dns;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.InstanceMetadataChangedListener;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.RevisionResolver;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.client.metadata.MetadataUtils;
import org.apache.dubbo.registry.dns.util.DNSClientConst;
import org.apache.dubbo.registry.dns.util.DNSResolver;
import org.apache.dubbo.registry.dns.util.ResolveResult;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;

import com.alibaba.fastjson.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DNSServiceDiscovery implements ServiceDiscovery {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private URL registryURL;

    /**
     * Echo check if consumer is still work
     * echo task may take a lot of time when consumer offline, create a new ScheduledThreadPool
     */
    private final ScheduledExecutorService echoCheckExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Dubbo-DNS-EchoCheck"));

    // =================================== Provider side =================================== //

    private ServiceInstance serviceInstance;

    /**
     * Local {@link ServiceInstance} Metadata's revision
     */
    private String lastMetadataRevision;

    // =================================== Consumer side =================================== //

    /**
     * DNS properties
     */

    private String addressPrefix;
    private String addressSuffix;
    private long pollingCycle;
    private DNSResolver dnsResolver;

    /**
     * Local Cache of {@link ServiceInstance} Metadata
     * <p>
     * Key - {@link ServiceInstance} ID ( usually ip + port )
     * Value - Json processed metadata string
     */
    private final ConcurrentHashMap<String, String> metadataMap = new ConcurrentHashMap<>();

    /**
     * Local Cache of {@link ServiceInstance}
     * <p>
     * Key - Service Name
     * Value - List {@link ServiceInstance}
     */
    private final ConcurrentHashMap<String, List<ServiceInstance>> cachedServiceInstances = new ConcurrentHashMap<>();

    /**
     * Local Cache of Service's {@link ServiceInstance} list revision,
     * used to check if {@link ServiceInstance} list has been updated
     * <p>
     * Key - ServiceName
     * Value - a revision calculate from {@link List} of {@link ServiceInstance}
     */
    private final ConcurrentHashMap<String, String> serviceInstanceRevisionMap = new ConcurrentHashMap<>();

    /**
     * Polling task ScheduledFuture, used to stop task when destroy
     */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> pollingExecutorMap = new ConcurrentHashMap<>();

    /**
     * Polling check provider ExecutorService
     */
    private ScheduledExecutorService pollingExecutorService;

    @Override
    public void initialize(URL registryURL) throws Exception {
        this.registryURL = registryURL;
        this.addressPrefix = registryURL.getParameter(DNSClientConst.ADDRESS_PREFIX, "");
        this.addressSuffix = registryURL.getParameter(DNSClientConst.ADDRESS_SUFFIX, "");
        this.pollingCycle = registryURL.getParameter(DNSClientConst.POLLING_CYCLE, 5000);
        long echoPollingCycle = registryURL.getParameter(DNSClientConst.ECHO_POLLING_CYCLE, 60000);
        int scheduledThreadPoolSize = registryURL.getParameter(DNSClientConst.SCHEDULED_THREAD_POOL_SIZE, 1);

        String nameserver = registryURL.getHost();
        int port = registryURL.getPort();
        int maxQueriesPerResolve = registryURL.getParameter(DNSClientConst.MAX_QUERIES_PER_RESOLVE, 10);
        this.dnsResolver = new DNSResolver(nameserver, port, maxQueriesPerResolve);

        // polling task may take a lot of time, create a new ScheduledThreadPool
        pollingExecutorService = Executors.newScheduledThreadPool(scheduledThreadPoolSize, new NamedThreadFactory("Dubbo-DNS-EchoCheck"));

        // Echo check: test if consumer is offline, remove MetadataChangeListener,
        // reduce the probability of failure when metadata update
        echoCheckExecutor.scheduleAtFixedRate(() -> {
            WritableMetadataService metadataService = WritableMetadataService.getDefaultExtension();
            Map<String, InstanceMetadataChangedListener> listenerMap = metadataService.getInstanceMetadataChangedListenerMap();
            Iterator<Map.Entry<String, InstanceMetadataChangedListener>> iterator = listenerMap.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, InstanceMetadataChangedListener> entry = iterator.next();
                try {
                    entry.getValue().echo(CommonConstants.DUBBO);
                } catch (RpcException e) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Send echo message to consumer error. Possible cause: consumer is offline.");
                    }
                    iterator.remove();
                }
            }
        }, echoPollingCycle, echoPollingCycle, TimeUnit.MILLISECONDS);

    }

    @Override
    public void destroy() throws Exception {
        metadataMap.clear();
        serviceInstanceRevisionMap.clear();
        pollingExecutorMap.forEach((serviceName, scheduledFuture) -> scheduledFuture.cancel(true));
        pollingExecutorMap.clear();
        echoCheckExecutor.shutdown();
        pollingExecutorService.shutdown();
        dnsResolver.destroy();
    }

    @Override
    public void register(ServiceInstance serviceInstance) throws RuntimeException {
        this.serviceInstance = serviceInstance;

        updateMetadata(serviceInstance);
    }

    @Override
    public void update(ServiceInstance serviceInstance) throws RuntimeException {
        this.serviceInstance = serviceInstance;

        updateMetadata(serviceInstance);
    }

    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {
        this.serviceInstance = null;

        // notify empty message to consumer
        WritableMetadataService metadataService = WritableMetadataService.getDefaultExtension();
        metadataService.exportInstanceMetadata("");
        metadataService.getInstanceMetadataChangedListenerMap().forEach((consumerId, listener) -> listener.onEvent(""));
        metadataService.getInstanceMetadataChangedListenerMap().clear();
    }

    @Override
    public Set<String> getServices() {
        // it is impossible for dns to discover service names
        return Collections.singleton("Unsupported Method");
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {

        String serviceAddress = addressPrefix + serviceName + addressSuffix;

        ResolveResult resolveResult = dnsResolver.resolve(serviceAddress);

        return toServiceInstance(serviceName, resolveResult);
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {
        listener.getServiceNames().forEach(serviceName -> {
            ScheduledFuture<?> scheduledFuture = pollingExecutorService.scheduleAtFixedRate(() -> {
                        List<ServiceInstance> instances = getInstances(serviceName);
                        instances.sort(Comparator.comparingInt(ServiceInstance::hashCode));

                        String serviceInstanceRevision = RevisionResolver.calRevision(JSONObject.toJSONString(instances));
                        boolean changed = !serviceInstanceRevision.equalsIgnoreCase(
                                serviceInstanceRevisionMap.put(serviceName, serviceInstanceRevision));

                        if (logger.isDebugEnabled()) {
                            logger.debug("Poll DNS data. Service Instance changed: " + changed + " Service Name: " + serviceName);
                        }

                        if (changed) {
                            List<ServiceInstance> oldServiceInstances = cachedServiceInstances.getOrDefault(serviceName, new LinkedList<>());

                            // remove expired invoker
                            Set<ServiceInstance> allServiceInstances = new HashSet<>(oldServiceInstances.size() + instances.size());
                            allServiceInstances.addAll(oldServiceInstances);
                            allServiceInstances.addAll(instances);

                            allServiceInstances.removeAll(oldServiceInstances);

                            allServiceInstances.forEach(removedServiceInstance -> {
                                MetadataUtils.destroyMetadataServiceProxy(removedServiceInstance, this);
                            });

                            cachedServiceInstances.put(serviceName, instances);
                            listener.onEvent(new ServiceInstancesChangedEvent(serviceName, instances));
                        }

                    },
                    pollingCycle, pollingCycle, TimeUnit.MILLISECONDS);

            pollingExecutorMap.put(serviceName, scheduledFuture);
        });
    }

    @Override
    public ServiceInstance getLocalInstance() {
        return serviceInstance;
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }

    /**
     * UT used only
     */
    @Deprecated
    public void setDnsResolver(DNSResolver dnsResolver) {
        this.dnsResolver = dnsResolver;
    }

    /**
     * UT used only
     */
    @Deprecated
    public ConcurrentHashMap<String, List<ServiceInstance>> getCachedServiceInstances() {
        return cachedServiceInstances;
    }

    @SuppressWarnings("unchecked")
    private List<ServiceInstance> toServiceInstance(String serviceName, ResolveResult resolveResult) {

        int port;

        if (resolveResult.getPort().size() > 0) {
            // use first as default
            port = resolveResult.getPort().get(0);
        } else {
            // not support SRV record
            port = 20880;
        }

        List<ServiceInstance> instanceList = new LinkedList<>();

        for (String host : resolveResult.getHostnameList()) {
            DefaultServiceInstance serviceInstance = new DefaultServiceInstance(serviceName, host, port);
            String hostId = serviceInstance.getId();
            if (metadataMap.containsKey(hostId)) {
                // Use cached metadata.
                // Metadata will be updated by provider callback

                String metadataString = metadataMap.get(hostId);
                serviceInstance.setMetadata(JSONObject.parseObject(metadataString, Map.class));
            } else {
                // refer from MetadataUtils, this proxy is different from the one used to refer exportedURL
                MetadataService metadataService = MetadataUtils.getMetadataServiceProxy(serviceInstance, this);

                String consumerId = ApplicationModel.getName() + NetUtils.getLocalHost();
                String metadata = metadataService.getAndListenInstanceMetadata(
                        consumerId, metadataString -> {
                            logger.info("Receive callback: " + metadataString + serviceInstance);
                            if (StringUtils.isEmpty(metadataString)) {
                                // provider is shutdown
                                metadataMap.remove(hostId);
                            } else {
                                metadataMap.put(hostId, metadataString);
                            }
                        });
                metadataMap.put(hostId, metadata);
                serviceInstance.setMetadata(JSONObject.parseObject(metadata, Map.class));
            }
            instanceList.add(serviceInstance);
        }

        return instanceList;
    }

    private void updateMetadata(ServiceInstance serviceInstance) {
        WritableMetadataService metadataService = WritableMetadataService.getDefaultExtension();
        String metadataString = JSONObject.toJSONString(serviceInstance.getMetadata());
        String metadataRevision = RevisionResolver.calRevision(metadataString);

        // check if metadata updated
        if (!metadataRevision.equalsIgnoreCase(lastMetadataRevision)) {
            logger.info("Update Service Instance Metadata of DNS registry. Newer metadata: " + metadataString);
            if (logger.isDebugEnabled()) {
                logger.debug("Update Service Instance Metadata of DNS registry. Newer metadata: " + metadataString);
            }

            lastMetadataRevision = metadataRevision;

            // save newest metadata to local
            metadataService.exportInstanceMetadata(metadataString);

            // notify to consumer
            Map<String, InstanceMetadataChangedListener> listenerMap = metadataService.getInstanceMetadataChangedListenerMap();
            Iterator<Map.Entry<String, InstanceMetadataChangedListener>> iterator = listenerMap.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, InstanceMetadataChangedListener> entry = iterator.next();
                try {
                    entry.getValue().onEvent(metadataString);
                } catch (RpcException e) {
                    logger.warn("Notify to consumer error. Possible cause: consumer is offline.");
                    // remove listener if consumer is offline
                    iterator.remove();
                }
            }
        }
    }
}
