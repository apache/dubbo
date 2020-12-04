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

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.config.listener.v3.Filter;
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.Rds;
import io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryResponse;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.envoyproxy.envoy.service.discovery.v3.Resource;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LdsProtocol {

    private static final Logger logger = LoggerFactory.getLogger(LdsProtocol.class);

    private final static String TYPE_URL = "type.googleapis.com/envoy.config.listener.v3.Listener";

    private StreamObserver<DiscoveryRequest> responseObserver;

    private StreamObserver<DeltaDiscoveryRequest> deltaResponseObserver;

    private final AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceStub ads;

    private final Node node;

    private final CompletableFuture<org.apache.dubbo.registry.xds.util.protocol.Listener> streamResult = new CompletableFuture<>();

    private final LinkedBlockingQueue<Consumer<org.apache.dubbo.registry.xds.util.protocol.Listener>> consumers = new LinkedBlockingQueue<>();

    private final AtomicBoolean completed = new AtomicBoolean(true);

    private final AtomicBoolean deltaCompleted = new AtomicBoolean(true);

    public LdsProtocol(Channel channel, Node node) {
        this.ads = AggregatedDiscoveryServiceGrpc.newStub(channel);
        this.node = node;
    }

    private void initObserver() {
        if (completed.compareAndSet(true, false)) {
            this.responseObserver = ads.streamAggregatedResources(new ResponseObserver());
        }
        if (deltaCompleted.compareAndSet(true, false)) {
            this.deltaResponseObserver = ads.deltaAggregatedResources(new DeltaResponseObserver());
            this.deltaResponseObserver.onNext(buildDeltaDiscoveryRequest());
        }
    }

    public org.apache.dubbo.registry.xds.util.protocol.Listener getListeners() {
        initObserver();
        responseObserver.onNext(buildDiscoveryRequest());
        try {
            return streamResult.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error occur when request control panel.");
            return new org.apache.dubbo.registry.xds.util.protocol.Listener();
        }
    }

    public void observeListeners(Consumer<org.apache.dubbo.registry.xds.util.protocol.Listener> consumer) {
        initObserver();
        consumer.accept(getListeners());
        consumers.offer(consumer);
    }

    private DiscoveryRequest buildDiscoveryRequest() {
        return DiscoveryRequest.newBuilder()
                .setNode(node)
                .setTypeUrl(TYPE_URL)
                .build();
    }

    private DiscoveryRequest buildDiscoveryRequest(DiscoveryResponse response) {
        return DiscoveryRequest.newBuilder()
                .setNode(node)
                .setTypeUrl(TYPE_URL)
                .setVersionInfo(response.getVersionInfo())
                .setResponseNonce(response.getNonce())
                .build();
    }

    private DeltaDiscoveryRequest buildDeltaDiscoveryRequest() {
        return DeltaDiscoveryRequest.newBuilder()
                .setNode(node)
                .setTypeUrl(TYPE_URL)
                .build();
    }

    private DeltaDiscoveryRequest buildDeltaDiscoveryRequest(DeltaDiscoveryResponse response) {
        return DeltaDiscoveryRequest.newBuilder()
                .setNode(node)
                .setTypeUrl(TYPE_URL)
                .setResponseNonce(response.getNonce())
                .build();
    }

    private org.apache.dubbo.registry.xds.util.protocol.Listener decodeDiscoveryResponse(DiscoveryResponse response) {
        if (TYPE_URL.equals(response.getTypeUrl())) {
            Set<String> set = response.getResourcesList().stream()
                    .map(LdsProtocol::unpackListener)
                    .filter(Objects::nonNull)
                    .flatMap((e) -> decodeResourceToListener(e).stream())
                    .collect(Collectors.toSet());
            return new org.apache.dubbo.registry.xds.util.protocol.Listener(set);
        }
        return new org.apache.dubbo.registry.xds.util.protocol.Listener();
    }

    private DeltaListener decodeDeltaDiscoveryResponse(DeltaDiscoveryResponse response, DeltaListener previous) {
        DeltaListener deltaListener = previous;
        if (deltaListener == null) {
            deltaListener = new DeltaListener();
        }
        if (TYPE_URL.equals(response.getTypeUrl())) {
            deltaListener.removeResource(response.getRemovedResourcesList());
            for (Resource resource : response.getResourcesList()) {
                Listener unpackedResource = unpackListener(resource.getResource());
                if (unpackedResource == null) {
                    continue;
                }
                deltaListener.addResource(resource.getName(), decodeResourceToListener(unpackedResource));
            }
        }
        return deltaListener;
    }

    private Set<String> decodeResourceToListener(Listener resource) {
        return resource.getFilterChainsList().stream()
                .flatMap((e) -> e.getFiltersList().stream())
                .map(Filter::getTypedConfig)
                .map(LdsProtocol::unpackHttpConnectionManager)
                .filter(Objects::nonNull)
                .map(HttpConnectionManager::getRds)
                .map(Rds::getRouteConfigName)
                .collect(Collectors.toSet());
    }

    private static Listener unpackListener(Any any) {
        try {
            return any.unpack(Listener.class);
        } catch (InvalidProtocolBufferException e) {
            logger.error("Error occur when decode xDS response.", e);
            return null;
        }
    }

    private static HttpConnectionManager unpackHttpConnectionManager(Any any) {
        try {
            return any.unpack(HttpConnectionManager.class);
        } catch (InvalidProtocolBufferException e) {
            logger.error("Error occur when decode xDS response.", e);
            return null;
        }
    }

    private class ResponseObserver implements StreamObserver<DiscoveryResponse> {
        @Override
        public void onNext(DiscoveryResponse value) {
            streamResult.complete(decodeDiscoveryResponse(value));
            responseObserver.onNext(buildDiscoveryRequest(value));
        }

        @Override
        public void onError(Throwable t) {
            logger.error("xDS Client received error message! detail:", t);
        }

        @Override
        public void onCompleted() {
            completed.set(true);
        }
    }

    private class DeltaResponseObserver implements StreamObserver<DeltaDiscoveryResponse> {
        private DeltaListener deltaListener = null;

        @Override
        public void onNext(DeltaDiscoveryResponse value) {
            deltaListener = decodeDeltaDiscoveryResponse(value, deltaListener);
            org.apache.dubbo.registry.xds.util.protocol.Listener listeners = deltaListener.getListeners();
            consumers.forEach(e -> e.accept(listeners));
            deltaResponseObserver.onNext(buildDeltaDiscoveryRequest(value));
        }

        @Override
        public void onError(Throwable t) {
            logger.error("xDS Client received error message! detail:", t);
        }

        @Override
        public void onCompleted() {
            deltaCompleted.set(true);
        }
    }
}
