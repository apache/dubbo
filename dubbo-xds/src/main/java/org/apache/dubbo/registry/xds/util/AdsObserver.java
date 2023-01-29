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
package org.apache.dubbo.registry.xds.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.registry.xds.util.protocol.AbstractProtocol;
import org.apache.dubbo.registry.xds.util.protocol.DeltaResource;
import org.apache.dubbo.rpc.model.ApplicationModel;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.stub.StreamObserver;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_REQUEST_XDS;

public class AdsObserver {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AdsObserver.class);
    private final ApplicationModel applicationModel;
    private final URL url;
    private final Node node;
    private volatile XdsChannel xdsChannel;

    private final Map<String, XdsListener> listeners = new ConcurrentHashMap<>();

    protected StreamObserver<DiscoveryRequest> requestObserver;

    private final Map<String, DiscoveryRequest> observedResources = new ConcurrentHashMap<>();

    public AdsObserver(URL url, Node node) {
        this.url = url;
        this.node = node;
        this.xdsChannel = new XdsChannel(url);
        this.applicationModel = url.getOrDefaultApplicationModel();
    }

    public <T, S extends DeltaResource<T>> void addListener(AbstractProtocol<T, S> protocol) {
        listeners.put(protocol.getTypeUrl(), protocol);
    }

    public void request(DiscoveryRequest discoveryRequest) {
        if (requestObserver == null) {
            requestObserver = xdsChannel.createDeltaDiscoveryRequest(new ResponseObserver(this));
        }
        requestObserver.onNext(discoveryRequest);
        observedResources.put(discoveryRequest.getTypeUrl(), discoveryRequest);
    }

    private static class ResponseObserver implements StreamObserver<DiscoveryResponse> {
        private AdsObserver adsObserver;

        public ResponseObserver(AdsObserver adsObserver) {
            this.adsObserver = adsObserver;
        }

        @Override
        public void onNext(DiscoveryResponse discoveryResponse) {
            XdsListener xdsListener = adsObserver.listeners.get(discoveryResponse.getTypeUrl());
            xdsListener.process(discoveryResponse);
            adsObserver.requestObserver.onNext(buildAck(discoveryResponse));
        }

        protected DiscoveryRequest buildAck(DiscoveryResponse response) {
            // for ACK
            return DiscoveryRequest.newBuilder()
                .setNode(adsObserver.node)
                .setTypeUrl(response.getTypeUrl())
                .setVersionInfo(response.getVersionInfo())
                .setResponseNonce(response.getNonce())
                .addAllResourceNames(adsObserver.observedResources.get(response.getTypeUrl()).getResourceNamesList())
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
    }

    private void triggerReConnectTask() {
        ScheduledExecutorService scheduledFuture = applicationModel.getFrameworkModel().getBeanFactory()
            .getBean(FrameworkExecutorRepository.class).getSharedScheduledExecutor();
        scheduledFuture.schedule(this::recover, 3, TimeUnit.SECONDS);
    }

    private void recover() {
        try {
            xdsChannel = new XdsChannel(url);
            if (xdsChannel.getChannel() != null) {
                requestObserver = xdsChannel.createDeltaDiscoveryRequest(new ResponseObserver(this));
                observedResources.values().forEach(requestObserver::onNext);
                return;
            } else {
                logger.error(REGISTRY_ERROR_REQUEST_XDS, "", "", "Recover failed for xDS connection. Will retry. Create channel failed.");
            }
        } catch (Exception e) {
            logger.error(REGISTRY_ERROR_REQUEST_XDS, "", "", "Recover failed for xDS connection. Will retry.", e);
        }
        triggerReConnectTask();
    }
}
