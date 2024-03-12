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
package org.apache.dubbo.xds.protocol.impl;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.xds.AdsObserver;
import org.apache.dubbo.xds.protocol.AbstractProtocol;
import org.apache.dubbo.xds.resource.XdsRoute;
import org.apache.dubbo.xds.resource.XdsRouteAction;
import org.apache.dubbo.xds.resource.XdsRouteConfiguration;
import org.apache.dubbo.xds.resource.XdsRouteMatch;
import org.apache.dubbo.xds.resource.XdsVirtualHost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_RESPONSE_XDS;

public class RdsProtocol extends AbstractProtocol<String> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RdsProtocol.class);

    protected Consumer<List<XdsRouteConfiguration>> updateCallback;

    public RdsProtocol(
            AdsObserver adsObserver,
            Node node,
            int checkInterval,
            Consumer<List<XdsRouteConfiguration>> updateCallback) {
        super(adsObserver, node, checkInterval);
        this.updateCallback = updateCallback;
    }

    @Override
    public String getTypeUrl() {
        return "type.googleapis.com/envoy.config.route.v3.RouteConfiguration";
    }

    @Override
    protected Map<String, String> decodeDiscoveryResponse(DiscoveryResponse response) {
        List<XdsRouteConfiguration> xdsRouteConfigurations = parse(response);
        System.out.println(xdsRouteConfigurations);
        updateCallback.accept(xdsRouteConfigurations);
        // if (getTypeUrl().equals(response.getTypeUrl())) {
        //     return response.getResourcesList().stream()
        //             .map(RdsProtocol::unpackRouteConfiguration)
        //             .filter(Objects::nonNull)
        //             .collect(Collectors.toConcurrentMap(RouteConfiguration::getName,
        // this::decodeResourceToListener));
        // }
        return new HashMap<>();
    }

    public List<XdsRouteConfiguration> parse(DiscoveryResponse response) {

        if (!getTypeUrl().equals(response.getTypeUrl())) {
            return null;
        }

        return response.getResourcesList().stream()
                .map(RdsProtocol::unpackRouteConfiguration)
                .filter(Objects::nonNull)
                .map(this::parseRouteConfiguration)
                .collect(Collectors.toList());
    }

    public XdsRouteConfiguration parseRouteConfiguration(RouteConfiguration routeConfiguration) {
        XdsRouteConfiguration xdsRouteConfiguration = new XdsRouteConfiguration();
        xdsRouteConfiguration.setName(routeConfiguration.getName());

        List<XdsVirtualHost> xdsVirtualHosts = routeConfiguration.getVirtualHostsList().stream()
                .map(this::parseVirtualHost)
                .collect(Collectors.toList());

        Map<String, XdsVirtualHost> xdsVirtualHostMap = new HashMap<>();

        xdsVirtualHosts.forEach(xdsVirtualHost -> {
            String domain = xdsVirtualHost.getDomains().get(0).split("\\.")[0];
            xdsVirtualHostMap.put(domain, xdsVirtualHost);
            // for (String domain : xdsVirtualHost.getDomains()) {
            //     xdsVirtualHostMap.put(domain, xdsVirtualHost);
            // }
        });

        xdsRouteConfiguration.setVirtualHosts(xdsVirtualHostMap);
        return xdsRouteConfiguration;
    }

    public XdsVirtualHost parseVirtualHost(VirtualHost virtualHost) {
        XdsVirtualHost xdsVirtualHost = new XdsVirtualHost();

        List<String> domains = virtualHost.getDomainsList();

        List<XdsRoute> xdsRoutes =
                virtualHost.getRoutesList().stream().map(this::parseRoute).collect(Collectors.toList());

        xdsVirtualHost.setName(virtualHost.getName());
        xdsVirtualHost.setRoutes(xdsRoutes);
        xdsVirtualHost.setDomains(domains);
        return xdsVirtualHost;
    }

    public XdsRoute parseRoute(Route route) {
        XdsRoute xdsRoute = new XdsRoute();

        XdsRouteMatch xdsRouteMatch = parseRouteMatch(route.getMatch());
        XdsRouteAction xdsRouteAction = parseRouteAction(route.getRoute());

        xdsRoute.setRouteMatch(xdsRouteMatch);
        xdsRoute.setRouteAction(xdsRouteAction);
        return xdsRoute;
    }

    public XdsRouteMatch parseRouteMatch(RouteMatch routeMatch) {
        XdsRouteMatch xdsRouteMatch = new XdsRouteMatch();
        String prefix = routeMatch.getPrefix();
        String path = routeMatch.getPath();

        xdsRouteMatch.setPrefix(prefix);
        xdsRouteMatch.setPath(path);
        return xdsRouteMatch;
    }

    public XdsRouteAction parseRouteAction(RouteAction routeAction) {
        XdsRouteAction xdsRouteAction = new XdsRouteAction();

        String cluster = routeAction.getCluster();

        if (cluster.equals("")) {
            System.out.println("parse weight clusters");
        }

        xdsRouteAction.setCluster(cluster);

        return xdsRouteAction;
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
