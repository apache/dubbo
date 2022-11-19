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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_REQUEST_XDS;

public abstract class AbstractProtocol<T, S extends DeltaResource<T>> implements XdsProtocol<T> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AbstractProtocol.class);

    protected XdsChannel xdsChannel;

    protected final Node node;

    /**
     * Store Request Parameter ( resourceNames )
     * K - requestId, V - resourceNames
     */
    protected final Map<Long, Set<String>> requestParam = new ConcurrentHashMap<>();

    /**
     * Store ADS Request Observer ( StreamObserver in Streaming Request )
     * K - requestId, V - StreamObserver
     */
    private final LinkedHashSet<StreamObserver<DiscoveryRequest>> requestObserverSet = new LinkedHashSet<>();

    /**
     * Store Delta-ADS Request Observer ( StreamObserver in Streaming Request )
     * K - requestId, V - StreamObserver
     */
    private final Map<Long, ScheduledFuture<?>> observeScheduledMap = new ConcurrentHashMap<>();

    /**
     * Store CompletableFuture for Request ( used to fetch async result in ResponseObserver )
     * K - requestId, V - CompletableFuture
     */
    private final Map<Long, CompletableFuture<T>> streamResult = new ConcurrentHashMap<>();

    private final int pollingTimeout;

//    protected final static AtomicLong requestId = new AtomicLong(0);

    public AbstractProtocol(XdsChannel xdsChannel, Node node, int pollingPoolSize, int pollingTimeout) {
        this.xdsChannel = xdsChannel;
        this.node = node;
        this.pollingTimeout = pollingTimeout;
    }

    /**
     * Abstract method to obtain Type-URL from sub-class
     *
     * @return Type-URL of xDS
     */
    public abstract String getTypeUrl();

    public abstract Set<Set<String>> getResouceNamesList();
    public abstract void addResouceNames(Set<String> resourceNaems);
    public abstract boolean isExistResource(Set<String> resourceNames);
    @Override
    public T getResource(Set<String> resourceNames) {
//        long request = requestId.getAndIncrement();
        resourceNames = resourceNames == null ? Collections.emptySet() : resourceNames;
        if (!resourceNames.isEmpty() && !isExistResource(resourceNames)) {
            addResouceNames(resourceNames);
        }

        // Store Request Parameter, which will be used for ACK
//        requestParam.put(request, resourceNames);

        // create observer
        StreamObserver<DiscoveryRequest> requestObserver = xdsChannel.createDeltaDiscoveryRequest(new ResponseObserver(null, null));

        // use future to get async result
        CompletableFuture<T> future = new CompletableFuture<>();
        requestObserverSet.add(requestObserver);
//        requestObserverMap.put(request, requestObserver);
//        streamResult.put(request, future);

        // send request to control panel
        requestObserver.onNext(buildDiscoveryRequest(resourceNames));

        try {
            // get result
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(REGISTRY_ERROR_REQUEST_XDS, "", "", "Error occur when request control panel.");
            return null;
        } finally {
            // close observer
            //requestObserver.onCompleted();

            // remove temp
//            streamResult.remove(request);
//            requestObserverMap.remove(request);
//            requestParam.remove(request);
        }
    }

    @Override
    public void observeResource(Set<String> resourceNames, Consumer<T> consumer) {
//        long request = requestId.getAndIncrement();
        resourceNames = resourceNames == null ? Collections.emptySet() : resourceNames;
        if (!resourceNames.isEmpty() &&  !isExistResource(resourceNames)) {
            addResouceNames(resourceNames);
        }
        // Store Request Parameter, which will be used for ACK
//        requestParam.put(request, resourceNames);

        // call once for full data
        consumer.accept(getResource(resourceNames));

        // channel reused
        StreamObserver<DiscoveryRequest> requestObserver = xdsChannel.createDeltaDiscoveryRequest(new ResponseObserver(null, consumer));
//        requestObserverMap.put(request, requestObserver);
        try {
            int requestObserverSetSize = requestObserverSet.size();
            // origin request, may changed by updateObserve
            Set<String> names = requestParam.get(requestObserverSetSize >= 2 ?
                requestObserverSet.stream().skip(requestObserverSetSize - 2).findFirst().get(): null
            );

            // use future to get async result, future complete on StreamObserver onNext
            CompletableFuture<T> future = new CompletableFuture<>();
//            streamResult.put(request, future);

            // observer reused
            StreamObserver<DiscoveryRequest> observer = requestObserverSet.iterator().next();

            if (observer == null) {
                observer = xdsChannel.createDeltaDiscoveryRequest(new ResponseObserver(future, consumer));
//                requestObserverMap.put(request, observer);
            }

            // send request to control panel
            observer.onNext(buildDiscoveryRequest(names));
        } catch (Throwable t) {
            logger.error("Error when requesting observe data. Type: " + getTypeUrl(), t);
        }
//        return request;
    }

    @Override
    public void updateObserve(long request, Set<String> resourceNames) {
        // send difference in resourceNames
        requestParam.put(request, resourceNames);
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

    private class ResponseObserver implements StreamObserver<DiscoveryResponse> {
//        private final long requestId;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ResponseObserver that = (ResponseObserver) o;
            return Objects.equals(consumer, that.consumer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(consumer);
        }

        private Consumer<T> consumer;

        private CompletableFuture<T> future;
        public ResponseObserver(CompletableFuture<T> future, Consumer<T> consumer) {
//            this.requestId = requestId;
            this.future = future;
            this.consumer = consumer;
        }

        @Override
        public void onNext(DiscoveryResponse value) {
            logger.info("receive notification from xds server, type: " + getTypeUrl());
            T result = decodeDiscoveryResponse(value);
            StreamObserver<DiscoveryRequest> observer = requestObserverSet.iterator().next();
            if (observer == null) {
                return;
            }
            if (consumer != null) {
                observer.onNext(buildDiscoveryRequest(Collections.emptySet(), value));
                // get result
                consumer.accept(result);
//                streamResult.remove(this.requestId);
            } else {
                returnResult(result);
            }
        }

        @Override
        public void onError(Throwable t) {
            logger.error(REGISTRY_ERROR_REQUEST_XDS, "", "", "xDS Client received error message! detail:", t);
            clear();
            if (consumer != null) {
                consumer.accept(null);
//                streamResult.remove(this.requestId);
            } else {
                returnResult(null);
            }
            triggerReConnectTask(this.consumer);
        }

        private void returnResult(T result) {
//            CompletableFuture<T> future = streamResult.get(requestId);
            if (future == null) {
                return;
            }
            future.complete(result);
        }

        @Override
        public void onCompleted() {
            logger.info("xDS Client completed");
            clear();
        }

        private void clear() {
//            requestObserverMap.remove(requestId);
        }
    }

    private void triggerReConnectTask(Consumer<T> consumer) {
        AtomicBoolean isConnectFail = new AtomicBoolean(false);
        ScheduledExecutorService scheduledFuture = ApplicationModel.defaultModel().getFrameworkModel().getBeanFactory()
            .getBean(FrameworkExecutorRepository.class).getSharedScheduledExecutor();
        scheduledFuture.schedule(() -> {
            xdsChannel = new XdsChannel(xdsChannel.getUrl());
            if (xdsChannel.getChannel() != null) {
                observeResource(null, consumer);
                if (isConnectFail.get()) {
                    scheduledFuture.shutdown();
                }
            } else {
                isConnectFail.set(true);
            }
        }, pollingTimeout, TimeUnit.SECONDS);
    }

}
