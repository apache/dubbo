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
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryResponse;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.envoyproxy.envoy.service.discovery.v3.Resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class RdsProtocol {

    private static final Logger logger = LoggerFactory.getLogger(LdsProtocol.class);

    private final static String TYPE_URL = "type.googleapis.com/envoy.config.route.v3.RouteConfiguration";

    public DiscoveryRequest buildDiscoveryRequest(Node node, Set<String> resourceNames) {
        return DiscoveryRequest.newBuilder()
                .setNode(node)
                .setTypeUrl(TYPE_URL)
                .addAllResourceNames(resourceNames)
                .build();
    }

    public DiscoveryRequest buildDiscoveryRequest(Node node, Set<String> resourceNames, DiscoveryResponse response) {
        return DiscoveryRequest.newBuilder()
                .setNode(node)
                .setTypeUrl(TYPE_URL)
                .addAllResourceNames(resourceNames)
                .setVersionInfo(response.getVersionInfo())
                .setResponseNonce(response.getNonce())
                .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Set<String>> decodeDiscoveryResponse(DiscoveryResponse response) {
        if (TYPE_URL.equals(response.getTypeUrl())) {
            Map<String, Set<String>> map = response.getResourcesList().stream()
                    .map(RdsProtocol::unpackRouteConfiguration)
                    .filter(Objects::nonNull)
                    .map(RdsProtocol::decodeResourceToListener)
                    .reduce((a,b)->{
                        a.putAll(b);
                        return a;
                    }).orElse(new HashMap<>());
            return Collections.unmodifiableMap(map);
        }
        return Collections.EMPTY_MAP;
    }

    public static DeltaRoute decodeDeltaDiscoveryResponse(DeltaDiscoveryResponse response, DeltaRoute previous) {
        DeltaRoute deltaRoute = previous;
        if (deltaRoute == null) {
            deltaRoute = new DeltaRoute();
        }
        if (TYPE_URL.equals(response.getTypeUrl())) {
            deltaRoute.removeResource(response.getRemovedResourcesList());
            for (Resource resource : response.getResourcesList()) {
                RouteConfiguration unpackedResource = unpackRouteConfiguration(resource.getResource());
                if (unpackedResource == null) {
                    continue;
                }
                deltaRoute.addResource(resource.getName(), decodeResourceToListener(unpackedResource));
            }
        }
        return deltaRoute;
    }

    private static Map<String, Set<String>> decodeResourceToListener(RouteConfiguration resource) {
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
                });
        return map;
    }

    private static RouteConfiguration unpackRouteConfiguration(Any any) {
        try {
            return any.unpack(RouteConfiguration.class);
        } catch (InvalidProtocolBufferException e) {
            logger.error("Error occur when decode xDS response.", e);
            return null;
        }
    }
}
