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

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.xds.util.XdsChannel;
import org.apache.dubbo.registry.xds.util.protocol.AbstractProtocol;
import org.apache.dubbo.registry.xds.util.protocol.delta.DeltaRoute;
import org.apache.dubbo.registry.xds.util.protocol.message.RouteResult;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_RESPONSE_XDS;

public class RdsProtocol extends AbstractProtocol<RouteResult, DeltaRoute> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RdsProtocol.class);

    private Map<String, RouteResult> routeDecodeResult = new HashMap<>();
    public RdsProtocol(XdsChannel xdsChannel, Node node, int pollingTimeout, ApplicationModel applicationModel) {
        super(xdsChannel, node, pollingTimeout, applicationModel);
    }

    @Override
    public String getTypeUrl() {
        return "type.googleapis.com/envoy.config.route.v3.RouteConfiguration";
    }

    @Override
    protected Map<String, RouteResult> getDsResult(Map<String, Object> resourcesMap) {
        Map<String, RouteResult> results = new HashMap<>();
        for (Map.Entry<String, Object> entry : resourcesMap.entrySet()) {
            if ("".equals(entry.getValue())) {
                continue;
            }
            RouteResult routeResult = new RouteResult();
            routeResult.getDomainMap().putAll((Map<String, Set<String>>) entry.getValue());
            results.put(entry.getKey(), routeResult);
        }
        return results;
    }

    @Override
    protected Map<String, RouteResult> decodeDiscoveryResponse(DiscoveryResponse response) {
        if (getTypeUrl().equals(response.getTypeUrl())) {
            Map<String, Set<String>> map = response.getResourcesList().stream()
                .map(RdsProtocol::unpackRouteConfiguration)
                .filter(Objects::nonNull)
                .map(this::decodeResourceToListener)
                .reduce((a, b) -> {
                    a.putAll(b);
                    return a;
                }).orElse(new HashMap<>());
            return routeDecodeResult;
        }
        return new HashMap<>();
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
                routeDecodeResult.put(resource.getName(), new RouteResult(map));
            });
        Map<String, Map<Set<String>, RouteResult>> stringMapMap = new HashMap<>();
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
