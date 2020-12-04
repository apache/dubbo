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
import io.envoyproxy.envoy.config.core.v3.HealthStatus;
import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryResponse;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.envoyproxy.envoy.service.discovery.v3.Resource;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class EdsProtocol{

    private static final Logger logger = LoggerFactory.getLogger(LdsProtocol.class);

    private final static String TYPE_URL = "type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment";

    public static DiscoveryRequest buildDiscoveryRequest(Node node, Set<String> resourceNames) {
        return DiscoveryRequest.newBuilder()
                .setNode(node)
                .setTypeUrl(TYPE_URL)
                .addAllResourceNames(resourceNames)
                .build();
    }

    public static DiscoveryRequest buildDiscoveryRequest(Node node, Set<String> resourceNames, DiscoveryResponse response) {
        return DiscoveryRequest.newBuilder()
                .setNode(node)
                .setTypeUrl(TYPE_URL)
                .addAllResourceNames(resourceNames)
                .setVersionInfo(response.getVersionInfo())
                .setResponseNonce(response.getNonce())
                .build();
    }

    @SuppressWarnings("unchecked")
    public static Set<Endpoint> decodeDiscoveryResponse(DiscoveryResponse response) {
        if (TYPE_URL.equals(response.getTypeUrl())) {
            Set<Endpoint> set = response.getResourcesList().stream()
                    .map(EdsProtocol::unpackClusterLoadAssignment)
                    .filter(Objects::nonNull)
                    .flatMap((e) -> decodeResourceToEndpoint(e).stream())
                    .collect(Collectors.toSet());
            return Collections.unmodifiableSet(set);
        }
        return Collections.EMPTY_SET;
    }

    public static DeltaEndpoint decodeDeltaDiscoveryResponse(DeltaDiscoveryResponse response, DeltaEndpoint previous) {
        DeltaEndpoint deltaEndpoint = previous;
        if (deltaEndpoint == null) {
            deltaEndpoint = new DeltaEndpoint();
        }
        if (TYPE_URL.equals(response.getTypeUrl())) {
            deltaEndpoint.removeResource(response.getRemovedResourcesList());
            for (Resource resource : response.getResourcesList()) {
                ClusterLoadAssignment unpackedResource = unpackClusterLoadAssignment(resource.getResource());
                if (unpackedResource == null) {
                    continue;
                }
                deltaEndpoint.addResource(resource.getName(), decodeResourceToEndpoint(unpackedResource));
            }
        }
        return previous;
    }

    private static Set<Endpoint> decodeResourceToEndpoint(ClusterLoadAssignment resource) {
        return resource.getEndpointsList().stream()
                .flatMap((e) -> e.getLbEndpointsList().stream())
                .map(EdsProtocol::decodeLbEndpointToEndpoint)
                .collect(Collectors.toSet());
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
            logger.error("Error occur when decode xDS response.", e);
            return null;
        }
    }

}
