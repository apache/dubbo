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

    protected final Map<Long, Set<String>> requestTemp = new ConcurrentHashMap<>();

    private final Map<Long, StreamObserver<DiscoveryRequest>> requestObserverMap = new ConcurrentHashMap<>();

    private final Map<Long, StreamObserver<DeltaDiscoveryRequest>> deltaRequestObserverMap = new ConcurrentHashMap<>();

    private final Map<Long, CompletableFuture<T>> streamResult = new ConcurrentHashMap<>();

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
        requestTemp.put(request, resourceNames);
        StreamObserver<DiscoveryRequest> requestObserver = xdsChannel.createDeltaDiscoveryRequest(new ResponseObserver(request));
        CompletableFuture<T> future = new CompletableFuture<>();
        requestObserverMap.put(request, requestObserver);
        streamResult.put(request, future);
        requestObserver.onNext(buildDiscoveryRequest(resourceNames));
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error occur when request control panel.");
            return null;
        } finally {
            requestObserver.onCompleted();
            streamResult.remove(request);
            requestObserverMap.remove(request);
            requestTemp.remove(request);
        }
    }

    @Override
    public long observeResource(Set<String> resourceNames, Consumer<T> consumer) {
        long request = requestId.getAndIncrement();
        requestTemp.put(request, resourceNames);
        consumer.accept(getResource(resourceNames));
        consumers.put(request, consumer);
        deltaRequestObserverMap.compute(request, (k, v) -> {
            if (v == null) {
                v = xdsChannel.observeDeltaDiscoveryRequest(new DeltaResponseObserver(request));
            }
            v.onNext(buildDeltaDiscoveryRequest(resourceNames));
            return v;
        });
        return request;
    }

    @Override
    public void updateObserve(long request, Set<String> resourceNames) {
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
        Set<String> previous = requestTemp.get(request);
        Set<String> unsubscribe = new HashSet<String>(previous) {{
            removeAll(resourceNames);
        }};
        requestTemp.put(request, resourceNames);
        return DeltaDiscoveryRequest.newBuilder()
                .setNode(node)
                .setTypeUrl(getTypeUrl())
                .addAllResourceNamesUnsubscribe(unsubscribe)
                .addAllResourceNamesSubscribe(resourceNames)
                .build();
    }

    private DeltaDiscoveryRequest buildDeltaDiscoveryRequest(Set<String> resourceNames, DeltaDiscoveryResponse response) {
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
            requestObserverMap.get(requestId).onNext(buildDiscoveryRequest(requestTemp.get(requestId), value));
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
            deltaRequestObserverMap.get(requestId).onNext(buildDeltaDiscoveryRequest(requestTemp.get(requestId), value));
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
