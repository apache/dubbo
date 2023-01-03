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
package org.apache.dubbo.registry.xds.util.protocol;


import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.registry.xds.util.XdsChannel;
import org.apache.dubbo.rpc.model.ApplicationModel;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_REQUEST_XDS;

public abstract class AbstractProtocol<T, S extends DeltaResource<T>> implements XdsProtocol<T> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AbstractProtocol.class);

    protected XdsChannel xdsChannel;

    protected final Node node;

    private final int checkInterval;

    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    protected final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

    protected Consumer<Map<String, T>> futureConsumer;
    protected final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    protected Set<String> observeResourcesName;

    protected static final String emptyResourceName = "emptyResourcesName";
    private final ReentrantLock resourceLock = new ReentrantLock();

    protected Map<Set<String>, List<Consumer<Map<String, T>>>> consumerObserveMap = new ConcurrentHashMap<>();
    private final ApplicationModel applicationModel;

    public AbstractProtocol(XdsChannel xdsChannel, Node node, int checkInterval, ApplicationModel applicationModel) {
        this.xdsChannel = xdsChannel;
        this.node = node;
        this.checkInterval = checkInterval;
        this.applicationModel = applicationModel;
    }

    protected Map<String, T> resourcesMap = new ConcurrentHashMap<>();

    protected StreamObserver<DiscoveryRequest> requestObserver;

    /**
     * Abstract method to obtain Type-URL from sub-class
     *
     * @return Type-URL of xDS
     */
    public abstract String getTypeUrl();

    protected boolean isCacheExistResource(Set<String> resourceNames) {
        for (String resourceName : resourceNames) {
            if ("".equals(resourceName)) {
                continue;
            }
            if (!resourcesMap.containsKey(resourceName)) {
                return false;
            }
        }
        return true;
    }

    public T getCacheResource(String resourceName) {
        if (resourceName == null || resourceName.length() == 0) {
            return null;
        }
        return resourcesMap.get(resourceName);
    }


    @Override
    public Map<String, T> getResource(Set<String> resourceNames) {
        resourceNames = resourceNames == null ? Collections.emptySet() : resourceNames;

        if (!resourceNames.isEmpty() && isCacheExistResource(resourceNames)) {
            return getResourceFromCache(resourceNames);
        } else {
            return getResourceFromRemote(resourceNames);
        }
    }

    private Map<String, T> getResourceFromCache(Set<String> resourceNames) {
        return resourceNames.stream()
            .collect(Collectors.toMap(k -> k, this::getCacheResource));
    }

    public Map<String, T> getResourceFromRemote(Set<String> resourceNames) {
        try {
            resourceLock.lock();
            CompletableFuture<Map<String, T>> future = new CompletableFuture<>();
            if (requestObserver == null) {
                requestObserver = xdsChannel.createDeltaDiscoveryRequest(new ResponseObserver());
            }
            observeResourcesName = resourceNames;
            Set<String> consumerObserveResourceNames = new HashSet<>();
            if (resourceNames.isEmpty()) {
                consumerObserveResourceNames.add(emptyResourceName);
            } else {
                consumerObserveResourceNames = resourceNames;
            }

            this.futureConsumer = future::complete;
            try {
                writeLock.lock();
                consumerObserveMap.computeIfAbsent(consumerObserveResourceNames, key -> new ArrayList<>())
                    .add(futureConsumer);
            } finally {
                writeLock.unlock();
            }

            Set<String> resourceNamesToObserve = new HashSet<>(resourceNames);
            resourceNamesToObserve.addAll(resourcesMap.keySet());
            requestObserver.onNext(buildDiscoveryRequest(resourceNamesToObserve));
            logger.info("Send xDS Observe request to remote. Resource count: " + resourceNamesToObserve.size() + ". Resource Type: " + getTypeUrl());

            try {
                Map<String, T> result = future.get();

                try {
                    writeLock.lock();
                    consumerObserveMap.get(consumerObserveResourceNames).removeIf(o -> o.equals(futureConsumer));
                } finally {
                    writeLock.unlock();
                }

                return result;
            } catch (InterruptedException e) {
                logger.error("InterruptedException occur when request control panel. error={}", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Error occur when request control panel. error=. ", e);
            }
        } finally {
            resourceLock.unlock();
        }
        return Collections.emptyMap();
    }

    public void observeResource(Set<String> resourceNames, Consumer<Map<String, T>> consumer, boolean isReConnect) {
        resourceNames = resourceNames == null ? Collections.emptySet() : resourceNames;
        // call once for full data
        if (!isReConnect) {
            try {
                writeLock.lock();
                Set<String> consumerObserveResourceNames = new HashSet<>();
                if (resourceNames.isEmpty()) {
                    consumerObserveResourceNames.add(emptyResourceName);
                } else {
                    consumerObserveResourceNames = resourceNames;
                }
                consumerObserveMap.compute(consumerObserveResourceNames, (k, v) -> {
                    if (v == null) {
                        v = new ArrayList<>();
                    }
                    // support multi-consumer
                    v.add(consumer);
                    return v;
                });
                consumer.accept(getResource(resourceNames));
            } finally {
                writeLock.unlock();
            }
        }
        try {
            writeLock.lock();
            this.observeResourcesName = consumerObserveMap.keySet()
                .stream().flatMap(Set::stream).collect(Collectors.toSet());
        } finally {
            writeLock.unlock();
        }
    }

    protected DiscoveryRequest buildDiscoveryRequest(Set<String> resourceNames) {
        return DiscoveryRequest.newBuilder()
            .setNode(node)
            .setTypeUrl(getTypeUrl())
            .addAllResourceNames(resourceNames)
            .build();
    }

    protected DiscoveryRequest buildDiscoveryRequest(Set<String> resourceNames, DiscoveryResponse response) {
        // for ACK
        return DiscoveryRequest.newBuilder()
            .setNode(node)
            .setTypeUrl(response.getTypeUrl())
            .setVersionInfo(response.getVersionInfo())
            .setResponseNonce(response.getNonce())
            .build();
    }

    protected abstract Map<String, T> decodeDiscoveryResponse(DiscoveryResponse response);

    public class ResponseObserver implements StreamObserver<DiscoveryResponse> {

        public ResponseObserver() {

        }

        @Override
        public void onNext(DiscoveryResponse value) {
            Map<String, T> newResult = decodeDiscoveryResponse(value);
            Map<String, T> oldResource = resourcesMap;
            discoveryResponseListener(oldResource, newResult);
            resourcesMap = newResult;
            requestObserver.onNext(buildDiscoveryRequest(Collections.emptySet(), value));
        }

        public void discoveryResponseListener(Map<String, T> oldResult, Map<String, T> newResult) {
            Set<String> changedResourceNames = new HashSet<>();
            oldResult.forEach((key, origin) -> {
                if (!Objects.equals(origin, newResult.get(key))) {
                    changedResourceNames.add(key);
                }
            });
            newResult.forEach((key, origin) -> {
                if (!Objects.equals(origin, oldResult.get(key))) {
                    changedResourceNames.add(key);
                }
            });
            if (changedResourceNames.isEmpty()) {
                return;
            }

            logger.info("Receive resource update notification from xds server. Change resource count: " + changedResourceNames.stream() + ". Type: " + getTypeUrl());

            // call once for full data
            try {
                readLock.lock();
                for (Map.Entry<Set<String>, List<Consumer<Map<String, T>>>> entry : consumerObserveMap.entrySet()) {
                    if (entry.getKey().stream().noneMatch(changedResourceNames::contains)) {
                        // none update
                        continue;
                    }

                    Map<String, T> dsResultMap = entry.getKey()
                        .stream()
                        .collect(Collectors.toMap(k -> k, newResult::get));

                    entry.getValue().forEach(o -> o.accept(dsResultMap));
                }
            } finally {
                readLock.unlock();
            }
        }

        @Override
        public void onError(Throwable t) {
            logger.error(REGISTRY_ERROR_REQUEST_XDS, "", "", "xDS Client received error message! detail:", t);
            if (consumerObserveMap.size() != 0) {
                triggerReConnectTask();
            }
        }

        @Override
        public void onCompleted() {
            logger.info("xDS Client completed");
        }
    }

    private void triggerReConnectTask() {
        AtomicBoolean isConnectFail = new AtomicBoolean(false);
        ScheduledExecutorService scheduledFuture = applicationModel.getFrameworkModel().getBeanFactory()
            .getBean(FrameworkExecutorRepository.class).getSharedScheduledExecutor();
        scheduledFuture.scheduleAtFixedRate(() -> {
            xdsChannel = new XdsChannel(xdsChannel.getUrl());
            if (xdsChannel.getChannel() != null) {
                for (Map.Entry<Set<String>, List<Consumer<Map<String, T>>>> entry : consumerObserveMap.entrySet()) {
                    if (entry.getKey().equals(observeResourcesName)) {
                        for (Consumer<Map<String, T>> consumer : entry.getValue()) {
                            observeResource(observeResourcesName, consumer, true);
                        }
                    }
                }
                if (isConnectFail.get()) {
                    scheduledFuture.shutdown();
                }
            } else {
                isConnectFail.set(true);
            }
        }, checkInterval, checkInterval, TimeUnit.SECONDS);
    }
}
