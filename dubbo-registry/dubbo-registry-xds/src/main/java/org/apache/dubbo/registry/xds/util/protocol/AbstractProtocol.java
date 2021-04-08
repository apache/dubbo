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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.xds.util.XdsChannel;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryResponse;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.stub.StreamObserver;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public abstract class AbstractProtocol<T, S extends DeltaResource<T>> implements XdsProtocol<T>{

    private static final Logger logger = LoggerFactory.getLogger(AbstractProtocol.class);

    protected final XdsChannel xdsChannel;

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
    private final Map<Long, StreamObserver<DiscoveryRequest>> requestObserverMap = new ConcurrentHashMap<>();

    /**
     * Store Delta-ADS Request Observer ( StreamObserver in Streaming Request )
     * K - requestId, V - StreamObserver
     */
    private final Map<Long, StreamObserver<DeltaDiscoveryRequest>> deltaRequestObserverMap = new ConcurrentHashMap<>();

    /**
     * Store CompletableFuture for Request ( used to fetch async result in ResponseObserver )
     * K - requestId, V - CompletableFuture
     */
    private final Map<Long, CompletableFuture<T>> streamResult = new ConcurrentHashMap<>();

    /**
     * Store consumers for Observers ( will consume message produced by Delta-ADS )
     * K - requestId, V - Consumer
     */
    private final Map<Long, Consumer<T>> consumers = new ConcurrentHashMap<>();

    protected final AtomicLong requestId = new AtomicLong(0);

    public AbstractProtocol(XdsChannel xdsChannel, Node node) {
        this.xdsChannel = xdsChannel;
        this.node = node;
    }

    /**
     * Abstract method to obtain Type-URL from sub-class
     *
     * @return Type-URL of xDS
     */
    public abstract String getTypeUrl();

    @Override
    public T getResource(Set<String> resourceNames) {
        long request = requestId.getAndIncrement();
        resourceNames = resourceNames == null ? Collections.emptySet() : resourceNames;

        // Store Request Parameter, which will be used for ACK
        requestParam.put(request, resourceNames);

        // create observer
        StreamObserver<DiscoveryRequest> requestObserver = xdsChannel.createDeltaDiscoveryRequest(new ResponseObserver(request));

        // use future to get async result
        CompletableFuture<T> future = new CompletableFuture<>();
        requestObserverMap.put(request, requestObserver);
        streamResult.put(request, future);

        // send request to control panel
        requestObserver.onNext(buildDiscoveryRequest(resourceNames));

        try {
            // get result
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error occur when request control panel.");
            return null;
        } finally {
            // close observer
            requestObserver.onCompleted();

            // remove temp
            streamResult.remove(request);
            requestObserverMap.remove(request);
            requestParam.remove(request);
        }
    }

    @Override
    public long observeResource(Set<String> resourceNames, Consumer<T> consumer) {
        long request = requestId.getAndIncrement();

        // Store Request Parameter, which will be used for ACK
        requestParam.put(request, resourceNames);

        // call once for full data
        consumer.accept(getResource(resourceNames));

        consumers.put(request, consumer);
        deltaRequestObserverMap.compute(request, (k, v) -> {
            // create Delta-ADS observer
            v= xdsChannel.observeDeltaDiscoveryRequest(new DeltaResponseObserver(request));

            // send observe request
            v.onNext(buildDeltaDiscoveryRequest(resourceNames));
            return v;
        });
        return request;
    }

    @Override
    public void updateObserve(long request, Set<String> resourceNames) {
        // send difference in resourceNames
        deltaRequestObserverMap.get(request).onNext(buildDeltaDiscoveryRequest(request, resourceNames));
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
                .setTypeUrl(getTypeUrl())
                .addAllResourceNames(resourceNames)
                .setVersionInfo(response.getVersionInfo())
                .setResponseNonce(response.getNonce())
                .build();
    }

    protected DeltaDiscoveryRequest buildDeltaDiscoveryRequest(Set<String> resourceNames) {
        return DeltaDiscoveryRequest.newBuilder()
                .setNode(node)
                .setTypeUrl(getTypeUrl())
                .addAllResourceNamesSubscribe(resourceNames)
                .build();
    }

    protected DeltaDiscoveryRequest buildDeltaDiscoveryRequest(long request, Set<String> resourceNames) {
        // compare with previous
        Set<String> previous = requestParam.get(request);
        Set<String> unsubscribe = new HashSet<String>(previous) {{
            removeAll(resourceNames);
        }};
        requestParam.put(request, resourceNames);
        return DeltaDiscoveryRequest.newBuilder()
                .setNode(node)
                .setTypeUrl(getTypeUrl())
                .addAllResourceNamesUnsubscribe(unsubscribe)
                .addAllResourceNamesSubscribe(resourceNames)
                .build();
    }

    private DeltaDiscoveryRequest buildDeltaDiscoveryRequest(Set<String> resourceNames, DeltaDiscoveryResponse response) {
        // for ACK
        return DeltaDiscoveryRequest.newBuilder()
                .setNode(node)
                .setTypeUrl(getTypeUrl())
                .addAllResourceNamesSubscribe(resourceNames)
                .setResponseNonce(response.getNonce())
                .build();
    }

    protected abstract T decodeDiscoveryResponse(DiscoveryResponse response);

    protected abstract S decodeDeltaDiscoveryResponse(DeltaDiscoveryResponse response, S previous);

    private class ResponseObserver implements StreamObserver<DiscoveryResponse> {
        private final long requestId;

        public ResponseObserver(long requestId) {
            this.requestId = requestId;
        }

        @Override
        public void onNext(DiscoveryResponse value) {
            T result = decodeDiscoveryResponse(value);
            requestObserverMap.get(requestId).onNext(buildDiscoveryRequest(requestParam.get(requestId), value));
            streamResult.get(requestId).complete(result);
        }

        @Override
        public void onError(Throwable t) {
            logger.error("xDS Client received error message! detail:", t);
        }

        @Override
        public void onCompleted() {
            // ignore
        }
    }

    private class DeltaResponseObserver implements StreamObserver<DeltaDiscoveryResponse> {
        private S delta = null;
        private final long requestId;

        public DeltaResponseObserver(long requestId) {
            this.requestId = requestId;
        }

        @Override
        public void onNext(DeltaDiscoveryResponse value) {
            delta = decodeDeltaDiscoveryResponse(value, delta);
            T routes = delta.getResource();
            consumers.get(requestId).accept(routes);
            deltaRequestObserverMap.get(requestId).onNext(buildDeltaDiscoveryRequest(requestParam.get(requestId), value));
        }

        @Override
        public void onError(Throwable t) {
            logger.error("xDS Client received error message! detail:", t);
        }

        @Override
        public void onCompleted() {
            // ignore
        }
    }

}
