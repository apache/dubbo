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
import org.apache.dubbo.registry.xds.util.protocol.delta.DeltaEndpoint;
import org.apache.dubbo.registry.xds.util.protocol.message.Endpoint;
import org.apache.dubbo.registry.xds.util.protocol.message.EndpointResult;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.envoyproxy.envoy.config.core.v3.HealthStatus;
import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import org.apache.dubbo.registry.xds.util.protocol.message.RouteResult;

import java.util.HashMap;
import java.util.Set;
import java.util.Objects;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_RESPONSE_XDS;

public class EdsProtocol extends AbstractProtocol<EndpointResult, DeltaEndpoint> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(EdsProtocol.class);

    private StreamObserver<DiscoveryRequest> requestObserver;

    private HashMap<String, Object> resourcesMap = new HashMap<>();


    public EdsProtocol(XdsChannel xdsChannel, Node node, int pollingTimeout) {
        super(xdsChannel, node, pollingTimeout);
    }

    @Override
    public String getTypeUrl() {
        return "type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment";
    }

    @Override
    public boolean isExistResource(Set<String> resourceNames) {
        for (String resourceName : resourceNames) {
            if (!resourcesMap.containsKey(resourceName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public EndpointResult getCacheResource(Set<String> resourceNames) {
        Set<Endpoint> resourceSet = new HashSet<>();
        if (!resourceNames.isEmpty() && isExistResource(resourceNames)) {
            for (String resourceName : resourceNames) {
                resourceSet.addAll((Set<Endpoint>) resourcesMap.get(resourceName));
            }
        } else {
            CompletableFuture<EndpointResult> future = new CompletableFuture<>();
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
        return new EndpointResult(resourceSet);
    }

    @Override
    public StreamObserver<DiscoveryRequest> getStreamObserver() {
        return requestObserver;
    }

    @Override
    protected EndpointResult decodeDiscoveryResponse(DiscoveryResponse response) {
        if (getTypeUrl().equals(response.getTypeUrl())) {
            Set<Endpoint> set = response.getResourcesList().stream()
                .map(EdsProtocol::unpackClusterLoadAssignment)
                .filter(Objects::nonNull)
                .flatMap((e) -> decodeResourceToEndpoint(e).stream())
                .collect(Collectors.toSet());
            return new EndpointResult(set);
        }
        return new EndpointResult();
    }

    private Set<Endpoint> decodeResourceToEndpoint(ClusterLoadAssignment resource) {
        Set<Endpoint> endpoints = resource.getEndpointsList().stream()
            .flatMap((e) -> e.getLbEndpointsList().stream())
            .map(EdsProtocol::decodeLbEndpointToEndpoint)
            .collect(Collectors.toSet());
        resourcesMap.put(resource.getClusterName(), endpoints);
        return endpoints;
    }

    private static Endpoint decodeLbEndpointToEndpoint(LbEndpoint lbEndpoint) {
        Endpoint endpoint = new Endpoint();
        SocketAddress address = lbEndpoint.getEndpoint().getAddress().getSocketAddress();
        endpoint.setAddress(address.getAddress());
        endpoint.setPortValue(address.getPortValue());
        boolean healthy = HealthStatus.HEALTHY.equals(lbEndpoint.getHealthStatus()) ||
            HealthStatus.UNKNOWN.equals(lbEndpoint.getHealthStatus());
        endpoint.setHealthy(healthy);
        endpoint.setWeight(lbEndpoint.getLoadBalancingWeight().getValue());
        return endpoint;
    }

    private static ClusterLoadAssignment unpackClusterLoadAssignment(Any any) {
        try {
            return any.unpack(ClusterLoadAssignment.class);
        } catch (InvalidProtocolBufferException e) {
            logger.error(REGISTRY_ERROR_RESPONSE_XDS, "", "", "Error occur when decode xDS response.", e);
            return null;
        }
    }
}
