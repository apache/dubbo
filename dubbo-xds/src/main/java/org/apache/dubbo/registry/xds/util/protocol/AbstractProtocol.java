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
import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.stub.StreamObserver;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_REQUEST_XDS;

public abstract class AbstractProtocol<T, S extends DeltaResource<T>> implements XdsProtocol<T> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AbstractProtocol.class);

    protected XdsChannel xdsChannel;

    protected final Node node;

    private final int checkInterval;

    private T dsResult;
    private final Lock lock = new ReentrantLock();

    private Set<String> observeResourcesName;

//    private CompletableFuture<T> future;
    private final Map<Set<String>, List<Consumer<T>>> consumerObserveMap = new ConcurrentHashMap<>();

    private ApplicationModel applicationModel;

    public AbstractProtocol(XdsChannel xdsChannel, Node node, int checkInterval, ApplicationModel applicationModel) {
        this.xdsChannel = xdsChannel;
        this.node = node;
        this.checkInterval = checkInterval;
        this.applicationModel = applicationModel;
    }

    protected Map<String, Object> resourcesMap = new ConcurrentHashMap<>();

    private StreamObserver<DiscoveryRequest> requestObserver;

    /**
     * Abstract method to obtain Type-URL from sub-class
     *
     * @return Type-URL of xDS
     */
    public abstract String getTypeUrl();

    public abstract void updateResourceCollection(T resourceCollection, Set<String> resourceNames);

    public abstract T getDsResult();

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

    public T getCacheResource(Set<String> resourceNames) {
        this.dsResult = getDsResult();

        if (!resourceNames.isEmpty() && isExistResource(resourceNames)) {
            updateResourceCollection(dsResult, resourceNames);
        } else {
            CompletableFuture<T> future = new CompletableFuture<>();
            if (requestObserver == null) {
//                future = new CompletableFuture<>();
                requestObserver = xdsChannel.createDeltaDiscoveryRequest(new ResponseObserver());
            }
            consumerObserveMap.computeIfAbsent(resourceNames, (key) ->
                new ArrayList<>()
            ).add(new Consumer<T>() {
                @Override
                public void accept(T t) {
                    future.complete(t);
                }
            });


            resourceNames.addAll(resourcesMap.keySet());
            requestObserver.onNext(buildDiscoveryRequest(resourceNames));
            try {
                return future.get();
            } catch (InterruptedException e) {
                logger.error("InterruptedException occur when request control panel. error={}", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Error occur when request control panel. error=. ", e);
            }
        }
        return dsResult;
    }


    @Override
    public T getResource(Set<String> resourceNames) {
        resourceNames = resourceNames == null ? Collections.emptySet() : resourceNames;
        return getCacheResource(resourceNames);
    }

    @Override
    public void observeResource(Set<String> resourceNames, Consumer<T> consumer) {
        resourceNames = resourceNames == null ? Collections.emptySet() : resourceNames;
        // call once for full data
        try {
            lock.lock();
            consumerObserveMap.compute(resourceNames, (k, v) -> {
                if (v == null) {
                    v = new ArrayList<>();
                }
                // support multi-consumer
                v.add(consumer);
                return v;
            });
            for (Consumer<T> cacheConsumer : consumerObserveMap.get(resourceNames)) {
                cacheConsumer.accept(getResource(resourceNames));
            }
        } finally {
            lock.unlock();
        }
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

    protected abstract T decodeDiscoveryResponse(DiscoveryResponse response);

    protected class ResponseObserver implements StreamObserver<DiscoveryResponse> {

//        private CompletableFuture<T> future;
        public ResponseObserver() {
//            this.future = future;
        }

        @Override
        public void onNext(DiscoveryResponse value) {
            logger.info("receive notification from xds server, type: " + getTypeUrl());
            T result = decodeDiscoveryResponse(value);
            discoveryResponseListener(result);
            requestObserver.onNext(buildDiscoveryRequest(Collections.emptySet(), value));
//            returnResult(result);
        }

        private void discoveryResponseListener(T result) {
            // call once for full data
            if (observeResourcesName != null) {
                try {
                    lock.lock();
                    consumerObserveMap.get(observeResourcesName).forEach(o -> o.accept(result));
                } finally {
                    lock.unlock();
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            logger.error(REGISTRY_ERROR_REQUEST_XDS, "", "", "xDS Client received error message! detail:", t);
            if (consumerObserveMap.size() != 0) {
                triggerReConnectTask();
            }
        }

//        private void returnResult(T result) {
//            if (future == null) {
//                return;
//            }
//            future.complete(result);
//        }

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
                for (Map.Entry<Set<String>, List<Consumer<T>>> entry : consumerObserveMap.entrySet()) {
                    if (entry.getKey().equals(observeResourcesName)) {
                        for (Consumer<T> consumer : entry.getValue()) {
                            observeResource(observeResourcesName, consumer);
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
