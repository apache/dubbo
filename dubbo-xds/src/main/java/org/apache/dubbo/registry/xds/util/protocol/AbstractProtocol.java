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


import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.stub.StreamObserver;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.registry.xds.util.XdsChannel;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

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

    protected final String ldsResourcesName = "ldsResourcesName";
    private ReentrantLock resourceLock = new ReentrantLock();

    protected Map<Set<String>, List<Consumer<Map<String, T>>>> consumerObserveMap = new ConcurrentHashMap<>();
    private ApplicationModel applicationModel;

    public AbstractProtocol(XdsChannel xdsChannel, Node node, int checkInterval, ApplicationModel applicationModel) {
        this.xdsChannel = xdsChannel;
        this.node = node;
        this.checkInterval = checkInterval;
        this.applicationModel = applicationModel;
    }

    protected Map<String, Object> resourcesMap = new ConcurrentHashMap<>();

    protected StreamObserver<DiscoveryRequest> requestObserver;

    /**
     * Abstract method to obtain Type-URL from sub-class
     *
     * @return Type-URL of xDS
     */
    public abstract String getTypeUrl();

    public boolean isExistResource(Set<String> resourceNames) {
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

    public Object getCacheResource(String resourceName) {
        if (resourceName == null || resourceName.length() == 0) {
            return "";
        }
        return resourcesMap.get(resourceName);
    }


    @Override
    public Map<String, T> getResource(Set<String> resourceNames) {
        resourceNames = resourceNames == null ? Collections.emptySet() : resourceNames;
        if (!resourceNames.isEmpty() && isExistResource(resourceNames)) {
            Map<String, Object> resourcesMap = new HashMap<>();
            for (String resourceName : resourceNames) {
                resourcesMap.put(resourceName, getCacheResource(resourceName));
            }
            return getDsResult(resourcesMap);
        } else {
            // 针对资源粒度的锁
            try {
                resourceLock.lock();
                // 同一时刻只允许 resourceNames 加锁， 可以改成resources 粒度加锁
                CompletableFuture<Map<String, T>> future = new CompletableFuture<>();
                if (requestObserver == null) {
                    requestObserver = xdsChannel.createDeltaDiscoveryRequest(new ResponseObserver());
                }
                observeResourcesName = resourceNames;
                Set<String> consumerObserveResourceNames = new HashSet<>();
                if (resourceNames.isEmpty()) {
                    consumerObserveResourceNames.add(ldsResourcesName);
                } else {
                    consumerObserveResourceNames = resourceNames;
                }

                this.futureConsumer = future::complete;
                try {
                    writeLock.lock();
                    consumerObserveMap.computeIfAbsent(consumerObserveResourceNames, (key) ->
                        new ArrayList<>()
                    ).add(futureConsumer);
                } finally {
                    writeLock.unlock();
                }

                resourceNames.addAll(resourcesMap.keySet());
                requestObserver.onNext(buildDiscoveryRequest(resourceNames));
                try {
                    Map<String, T> result = future.get();

                    // remove掉 futureConsumer
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
        }
        return null;
    }

    protected abstract Map<String, T> getDsResult(Map<String, Object> resourcesMap);

    public void observeResource(Set<String> resourceNames, Consumer<Map<String, T>> consumer, boolean isReConnect) {
        resourceNames = resourceNames == null ? Collections.emptySet() : resourceNames;
        // call once for full data
        if (!isReConnect) {
            try {
                writeLock.lock();
                Set<String> consumerObserveResourceNames = new HashSet<>();
                if (resourceNames.isEmpty()) {
                    consumerObserveResourceNames.add(ldsResourcesName);
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
            } finally {
                writeLock.unlock();
            }
        }
        Map<Set<String>, List<Consumer<Map<String, T>>>> consumerObserveList = new HashMap<>();
        // 遍历监听
        try {
            writeLock.lock();
            for (Map.Entry<Set<String>, List<Consumer<Map<String, T>>>> entry : consumerObserveMap.entrySet()) {
                Set<String> newKey = JsonUtils.getJson().toJavaObject(JsonUtils.getJson().toJson(entry.getKey()), Set.class);
                for (Consumer<Map<String, T>> observeConsumer : entry.getValue()) {
                    Consumer<Map<String, T>> newObserveConsumer = observeConsumer::accept;
                    consumerObserveList.computeIfAbsent(newKey, (k) -> new ArrayList<>()).add(newObserveConsumer);
                }
            }
        } finally {
            writeLock.unlock();
        }
        consumerObserveList.forEach((resourcesName, consumerList) -> consumerList.forEach(o -> {
            o.accept(getResource(resourcesName));
        }));
        this.observeResourcesName = resourceNames;
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
            logger.info("receive notification from xds server, type: " + getTypeUrl());
            Map<String, T> result = decodeDiscoveryResponse(value); // 应该是个map， 并替换缓存池里的map
            discoveryResponseListener(result);
            requestObserver.onNext(buildDiscoveryRequest(Collections.emptySet(), value));
        }

        public void discoveryResponseListener(Map<String, T> result) {
            // call once for full data
            try {
                readLock.lock();
                for (Map.Entry<Set<String>, List<Consumer<Map<String, T>>>> entry : consumerObserveMap.entrySet()) {
                    Set<String> key = entry.getKey();
                    Map<String, T> dsResultMap = new HashMap<>();
                    for (String resourcesName : key) {
                        for (Map.Entry<String, T> resultEntry : result.entrySet()) {
                            if (resourcesName.equals(resultEntry.getKey())) {
                                dsResultMap.put(resultEntry.getKey(), resultEntry.getValue());
                            }
                        }
                    }
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
