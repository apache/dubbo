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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.resource.XdsResourceType;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.stub.StreamObserver;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_PARSING_XDS;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_REQUEST_XDS;

public class AdsObserver {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AdsObserver.class);
    private final ApplicationModel applicationModel;
    private final URL url;
    private final Node node;
    private volatile XdsChannel xdsChannel;

    private final Map<XdsResourceType<?>, ConcurrentMap<String, XdsRawResourceListener>> rawResourceListeners =
            new ConcurrentHashMap<>();

    protected StreamObserver<DiscoveryRequest> requestObserver;

    private final CompletableFuture<String> future = new CompletableFuture<>();

    private final Map<String, XdsResourceType<?>> subscribedResourceTypeUrls = new HashMap<>();

    public AdsObserver(URL url, Node node) {
        this.url = url;
        this.node = node;
        this.xdsChannel = new XdsChannel(url);
        this.applicationModel = url.getOrDefaultApplicationModel();
    }

    public boolean hasSubscribed(XdsResourceType<?> type) {
        return subscribedResourceTypeUrls.containsKey(type.typeUrl());
    }

    public void saveSubscribedType(XdsResourceType<?> type) {
        subscribedResourceTypeUrls.put(type.typeUrl(), type);
    }

    @SuppressWarnings("unchecked")
    public <T extends ResourceUpdate> XdsRawResourceProtocol<T> addListener(
            String resourceName, XdsResourceType<T> clusterResourceType) {
        ConcurrentMap<String, XdsRawResourceListener> resourceListeners =
                rawResourceListeners.computeIfAbsent(clusterResourceType, k -> new ConcurrentHashMap<>());
        return (XdsRawResourceProtocol<T>) resourceListeners.computeIfAbsent(
                resourceName,
                k -> new XdsRawResourceProtocol<>(this, NodeBuilder.build(), clusterResourceType, applicationModel));
    }

    public void adjustResourceSubscription(XdsResourceType<?> resourceType) {
        this.request(buildDiscoveryRequest(resourceType, getResourcesToObserve(resourceType)));
    }

    public Set<String> getResourcesToObserve(XdsResourceType<?> resourceType) {
        Map<String, XdsRawResourceListener> listenerMap =
                rawResourceListeners.getOrDefault(resourceType, new ConcurrentHashMap<>());
        Set<String> resourceNames = new HashSet<>();
        for (Map.Entry<String, XdsRawResourceListener> entry : listenerMap.entrySet()) {
            resourceNames.add(entry.getKey());
        }
        return resourceNames;
    }

    private <T extends ResourceUpdate> void process(
            XdsResourceType<T> resourceTypeInstance, DiscoveryResponse response) {
        ValidatedResourceUpdate<T> validatedResourceUpdate =
                resourceTypeInstance.parse(XdsResourceType.xdsResourceTypeArgs, response.getResourcesList());
        if (!validatedResourceUpdate.getErrors().isEmpty()) {
            logger.error(
                    REGISTRY_ERROR_PARSING_XDS,
                    validatedResourceUpdate.getErrors().toArray());
        }
        ConcurrentMap<String, T> parsedResources = validatedResourceUpdate.getParsedResources().entrySet().stream()
                .collect(Collectors.toConcurrentMap(
                        Entry::getKey, e -> e.getValue().getResourceUpdate()));

        Map<String, XdsRawResourceListener> resourceListenerMap =
                rawResourceListeners.getOrDefault(resourceTypeInstance, new ConcurrentHashMap<>());
        for (Map.Entry<String, XdsRawResourceListener> entry : resourceListenerMap.entrySet()) {
            String resourceName = entry.getKey();
            XdsRawResourceListener rawResourceListener = entry.getValue();
            if (parsedResources.containsKey(resourceName)) {
                rawResourceListener.onResourceUpdate(parsedResources.get(resourceName));
            }
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
        try {
            // TODO：This is to make the child thread receive the information.
            //  Maybe Using CountDownLatch would be better
            String name = Thread.currentThread().getName();
            if ("main".equals(name)) {
                future.get(600, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
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
            logger.info("Receive message from server");
            if (future != null) {
                future.complete(null);
            }

            XdsResourceType<?> resourceType = fromTypeUrl(discoveryResponse.getTypeUrl());

            adsObserver.process(resourceType, discoveryResponse);

            adsObserver.requestObserver.onNext(buildAck(resourceType, discoveryResponse));
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
        ScheduledExecutorService scheduledFuture = applicationModel
                .getFrameworkModel()
                .getBeanFactory()
                .getBean(FrameworkExecutorRepository.class)
                .getSharedScheduledExecutor();
        scheduledFuture.schedule(this::recover, 3, TimeUnit.SECONDS);
    }

    private void recover() {
        try {
            xdsChannel = new XdsChannel(url);
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
