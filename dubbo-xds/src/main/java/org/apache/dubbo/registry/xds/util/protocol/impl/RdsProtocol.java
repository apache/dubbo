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
package org.apache.dubbo.registry.xds.util.protocol.impl;

import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.grpc.stub.StreamObserver;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.xds.util.XdsChannel;
import org.apache.dubbo.registry.xds.util.protocol.AbstractProtocol;
import org.apache.dubbo.registry.xds.util.protocol.delta.DeltaRoute;
import org.apache.dubbo.registry.xds.util.protocol.message.RouteResult;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_RESPONSE_XDS;

public class RdsProtocol extends AbstractProtocol<RouteResult, DeltaRoute> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RdsProtocol.class);

    private StreamObserver<DiscoveryRequest> requestObserver;

    private HashMap<String, Object> resourcesMap = new HashMap<>();

    public RdsProtocol(XdsChannel xdsChannel, Node node, int pollingTimeout) {
        super(xdsChannel, node, pollingTimeout);
    }

    @Override
    public String getTypeUrl() {
        return "type.googleapis.com/envoy.config.route.v3.RouteConfiguration";
    }

    @Override
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

    @Override
    public RouteResult getCacheResource(Set<String> resourceNames) {
        Map<String, Set<String>> resultMap = new HashMap<>();
        if (!resourceNames.isEmpty() && isExistResource(resourceNames)) {
            for (String resourceName : resourceNames) {
                if ("".equals(resourceName)) {
                    continue;
                }
                resultMap.putAll((Map<String, Set<String>>) resourcesMap.get(resourceName));
            }
        } else {
            CompletableFuture<RouteResult> future = new CompletableFuture<>();
            if (requestObserver == null) {
                requestObserver = xdsChannel.createDeltaDiscoveryRequest(new ResponseObserver(future));
            }
            resourceNames.addAll(resourcesMap.keySet());
            requestObserver.onNext(buildDiscoveryRequest(resourceNames));
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error occur when request control panel.");
            }
        }
        return new RouteResult(resultMap);
    }

    @Override
    public StreamObserver<DiscoveryRequest> getStreamObserver() {
        return requestObserver;
    }

    @Override
    protected RouteResult decodeDiscoveryResponse(DiscoveryResponse response) {
        if (getTypeUrl().equals(response.getTypeUrl())) {
            Map<String, Set<String>> map = response.getResourcesList().stream()
                .map(RdsProtocol::unpackRouteConfiguration)
                .filter(Objects::nonNull)
                .map(this::decodeResourceToListener)
                .reduce((a, b) -> {
                    a.putAll(b);
                    return a;
                }).orElse(new HashMap<>());
            return new RouteResult(map);
        }
        return new RouteResult();
    }

    private Map<String, Set<String>> decodeResourceToListener(RouteConfiguration resource) {
        Map<String, Set<String>> map = new HashMap<>();
        resource.getVirtualHostsList()
            .forEach(virtualHost -> {
                Set<String> cluster = virtualHost.getRoutesList().stream()
                    .map(Route::getRoute)
                    .map(RouteAction::getCluster)
                    .collect(Collectors.toSet());
                for (String domain : virtualHost.getDomainsList()) {
                    map.put(domain, cluster);
                }
                resourcesMap.put(resource.getName(), map);
            });
        return map;
    }

    private static RouteConfiguration unpackRouteConfiguration(Any any) {
        try {
            return any.unpack(RouteConfiguration.class);
        } catch (InvalidProtocolBufferException e) {
            logger.error(REGISTRY_ERROR_RESPONSE_XDS, "", "", "Error occur when decode xDS response.", e);
            return null;
        }
    }


}
