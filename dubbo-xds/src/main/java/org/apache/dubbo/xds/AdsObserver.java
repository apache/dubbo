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
package org.apache.dubbo.xds;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.resource.XdsResourceType;
import org.apache.dubbo.xds.resource.update.LdsUpdate;
import org.apache.dubbo.xds.resource.update.ResourceUpdate;
import org.apache.dubbo.xds.resource.update.ValidatedResourceUpdate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.stub.StreamObserver;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_PARSING_XDS;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_REQUEST_XDS;

public class AdsObserver {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AdsObserver.class);
    private final Node node;
    private volatile XdsChannel xdsChannel;
    private final Map<XdsResourceType<?>, ConcurrentMap<String, XdsRawResourceProtocol<?>>> rawResourceListeners =
            new ConcurrentHashMap<>();
    protected StreamObserver<DiscoveryRequest> requestObserver;
    private final CompletableFuture<String> future = new CompletableFuture<>();
    private final Map<String, XdsResourceType<?>> subscribedResourceTypeUrls = new HashMap<>();

    public AdsObserver() {
        this.node = NodeBuilder.build();
        this.xdsChannel = new XdsChannel();
    }

    public boolean hasSubscribed(XdsResourceType<?> type) {
        return subscribedResourceTypeUrls.containsKey(type.typeUrl());
    }

    public void saveSubscribedType(XdsResourceType<?> type) {
        subscribedResourceTypeUrls.put(type.typeUrl(), type);
    }

    @SuppressWarnings("unchecked")
    public <T extends ResourceUpdate> void addListener(
            String resourceName, XdsResourceType<T> resourceType, XdsResourceListener<T> resourceListener) {
        ConcurrentMap<String, XdsRawResourceProtocol<?>> resourceListeners =
                rawResourceListeners.computeIfAbsent(resourceType, k -> new ConcurrentHashMap<>());
        XdsRawResourceProtocol<T> xdsProtocol = (XdsRawResourceProtocol<T>) resourceListeners.computeIfAbsent(
                resourceName, k -> new XdsRawResourceProtocol<>(this, node, resourceType));
        xdsProtocol.subscribeResource(resourceName, resourceType, resourceListener);
    }

    public void adjustResourceSubscription(XdsResourceType<?> resourceType) {
        this.request(buildDiscoveryRequest(resourceType, getResourcesToObserve(resourceType)));
    }

    public Set<String> getResourcesToObserve(XdsResourceType<?> resourceType) {
        Map<String, XdsRawResourceProtocol<?>> listenerMap =
                rawResourceListeners.getOrDefault(resourceType, new ConcurrentHashMap<>());
        Set<String> resourceNames = new HashSet<>();
        for (Map.Entry<String, XdsRawResourceProtocol<?>> entry : listenerMap.entrySet()) {
            resourceNames.add(entry.getKey());
        }
        return resourceNames;
    }

    public <T extends ResourceUpdate> ValidatedResourceUpdate<T> process(
            XdsResourceType<T> resourceTypeInstance, DiscoveryResponse response) {
        ValidatedResourceUpdate<T> validatedResourceUpdate =
                resourceTypeInstance.parse(XdsResourceType.xdsResourceTypeArgs, response.getResourcesList());
        if (!validatedResourceUpdate.getErrors().isEmpty()) {
            logger.error(
                    REGISTRY_ERROR_PARSING_XDS,
                    "Parse errors for {}: {}",
                    resourceTypeInstance.typeName(),
                    validatedResourceUpdate.getErrors());
        }

        ConcurrentMap<String, T> parsedResources = validatedResourceUpdate.getParsedResources().entrySet().stream()
                .collect(Collectors.toConcurrentMap(
                        Entry::getKey, e -> e.getValue().getResourceUpdate()));

        Map<String, XdsRawResourceProtocol<?>> resourceListenerMap =
                rawResourceListeners.getOrDefault(resourceTypeInstance, new ConcurrentHashMap<>());
        for (Map.Entry<String, XdsRawResourceProtocol<?>> entry : resourceListenerMap.entrySet()) {
            String resourceName = entry.getKey();
            XdsRawResourceProtocol<T> rawResourceListener = (XdsRawResourceProtocol<T>) entry.getValue();

            T resourceUpdate = null;

            if (parsedResources.containsKey(resourceName)) {
                resourceUpdate = parsedResources.get(resourceName);
            } else if (resourceTypeInstance.typeName().equals("LDS") && resourceName.contains(":")) {
                int port = parsePort(resourceName);
                for (T update : parsedResources.values()) {
                    if (update instanceof LdsUpdate) {
                        LdsUpdate ldsUpdate = (LdsUpdate) update;
                        if (ldsUpdate.isContainPort(port)) {
                            resourceUpdate = update;
                            break;
                        }
                    }
                }
            }

            if (resourceUpdate != null) {
                rawResourceListener.onResourceUpdate(resourceUpdate);
            } else {
                logger.info("[XDS] No parsed resource found for {}", resourceName);
            }
        }

        return validatedResourceUpdate;
    }

    private int parsePort(String resourceName) {
        try {
            return Integer.parseInt(resourceName.substring(resourceName.lastIndexOf(':') + 1));
        } catch (Exception e) {
            logger.warn("[XDS] Failed to parse port from resource name: {}", resourceName);
            return -1;
        }
    }

    protected DiscoveryRequest buildDiscoveryRequest(XdsResourceType<?> resourceType, Set<String> resourceNames) {
        return DiscoveryRequest.newBuilder()
                .setNode(node)
                .setTypeUrl(resourceType.typeUrl())
                .addAllResourceNames(resourceNames)
                .build();
    }

    public void request(DiscoveryRequest discoveryRequest) {
        if (requestObserver == null) {
            requestObserver = xdsChannel.createDeltaDiscoveryRequest(new ResponseObserver(this, future));
        }
        requestObserver.onNext(discoveryRequest);
    }

    private static class ResponseObserver implements StreamObserver<DiscoveryResponse> {

        private final AdsObserver adsObserver;

        private final CompletableFuture<?> future;

        public ResponseObserver(AdsObserver adsObserver, CompletableFuture<?> future) {
            this.adsObserver = adsObserver;
            this.future = future;
        }

        @Override
        public void onNext(DiscoveryResponse discoveryResponse) {
            try {
                if (future != null) {
                    future.complete(null);
                }

                XdsResourceType<?> resourceType = fromTypeUrl(discoveryResponse.getTypeUrl());
                if (resourceType == null) {
                    return;
                }

                ValidatedResourceUpdate<?> validatedResourceUpdate =
                        adsObserver.process(resourceType, discoveryResponse);

                adsObserver.requestObserver.onNext(buildAck(resourceType, discoveryResponse));
            } catch (Throwable t) {
                logger.error(
                        REGISTRY_ERROR_REQUEST_XDS,
                        "",
                        "",
                        "Error processing xDS response - TypeUrl: " + discoveryResponse.getTypeUrl() + ", Error: "
                                + t.getMessage(),
                        t);
            }
        }

        protected DiscoveryRequest buildAck(XdsResourceType<?> resourceType, DiscoveryResponse response) {
            // for ACK
            return DiscoveryRequest.newBuilder()
                    .setNode(adsObserver.node)
                    .setTypeUrl(response.getTypeUrl())
                    .setVersionInfo(response.getVersionInfo())
                    .setResponseNonce(response.getNonce())
                    .addAllResourceNames(adsObserver.getResourcesToObserve(resourceType))
                    .build();
        }

        @Override
        public void onError(Throwable throwable) {
            logger.error(REGISTRY_ERROR_REQUEST_XDS, "", "", "xDS Client received error message! detail:", throwable);
            adsObserver.triggerReConnectTask();
        }

        @Override
        public void onCompleted() {
            logger.info("xDS Client completed");
            adsObserver.triggerReConnectTask();
        }

        XdsResourceType<?> fromTypeUrl(String typeUrl) {
            return adsObserver.subscribedResourceTypeUrls.get(typeUrl);
        }
    }

    private void triggerReConnectTask() {
        ScheduledExecutorService scheduledFuture = FrameworkModel.defaultModel()
                .getBeanFactory()
                .getBean(FrameworkExecutorRepository.class)
                .getSharedScheduledExecutor();
        scheduledFuture.schedule(this::recover, 3, TimeUnit.SECONDS);
    }

    private void recover() {
        try {
            xdsChannel = new XdsChannel();
            if (xdsChannel.getChannel() != null) {
                // Child thread not need to wait other child thread.
                requestObserver = xdsChannel.createDeltaDiscoveryRequest(new ResponseObserver(this, null));
                // FIXME, make sure recover all resource subscriptions.
                //                observedResources.values().forEach(requestObserver::onNext);
                return;
            } else {
                logger.error(
                        REGISTRY_ERROR_REQUEST_XDS,
                        "",
                        "",
                        "Recover failed for xDS connection. Will retry. Create channel failed.");
            }
        } catch (Exception e) {
            logger.error(REGISTRY_ERROR_REQUEST_XDS, "", "", "Recover failed for xDS connection. Will retry.", e);
        }
        triggerReConnectTask();
    }

    public void destroy() {
        this.xdsChannel.destroy();
    }
}
