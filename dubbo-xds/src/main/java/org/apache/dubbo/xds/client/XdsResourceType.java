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
package org.apache.dubbo.xds.client;

import org.apache.dubbo.xds.traffic.XdsLoadReports;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Locality;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Listener;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.WeightedCluster.ClusterWeight;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.Resource;

/** v3 resource types. Only LDS/CDS responses represent a complete state of the subscribed names. */
public final class XdsResourceType<T extends Message> {
    public static final XdsResourceType<Listener> LDS =
            new XdsResourceType<>("envoy.config.listener.v3.Listener", Listener.class, Listener::getName, true);
    public static final XdsResourceType<RouteConfiguration> RDS = new XdsResourceType<>(
            "envoy.config.route.v3.RouteConfiguration", RouteConfiguration.class, RouteConfiguration::getName, false);
    public static final XdsResourceType<Cluster> CDS =
            new XdsResourceType<>("envoy.config.cluster.v3.Cluster", Cluster.class, Cluster::getName, true);
    public static final XdsResourceType<ClusterLoadAssignment> EDS = new XdsResourceType<>(
            "envoy.config.endpoint.v3.ClusterLoadAssignment",
            ClusterLoadAssignment.class,
            ClusterLoadAssignment::getClusterName,
            false);

    private final String typeUrl;
    private final Class<T> messageClass;
    private final Function<T, String> name;
    private final boolean fullState;

    private XdsResourceType(String name, Class<T> messageClass, Function<T, String> resourceName, boolean fullState) {
        this.typeUrl = "type.googleapis.com/" + name;
        this.messageClass = messageClass;
        this.name = resourceName;
        this.fullState = fullState;
    }

    public String getTypeUrl() {
        return typeUrl;
    }

    public String name(T resource) {
        return name.apply(resource);
    }

    public boolean isFullState() {
        return fullState;
    }

    public T parse(Any raw) throws IOException {
        String wrapperName = null;
        if (raw.is(Resource.class)) {
            Resource wrapper = raw.unpack(Resource.class);
            if (wrapper.hasTtl() || wrapper.getAliasesCount() != 0 || !wrapper.hasResource()) {
                throw new IOException("Resource TTL, aliases and heartbeat wrappers are not supported");
            }
            wrapperName = wrapper.getName();
            raw = wrapper.getResource();
        }
        if (!raw.is(messageClass)) {
            throw new IOException("Unexpected resource payload for " + typeUrl);
        }
        T resource = raw.unpack(messageClass);
        if (name(resource).isEmpty() || (wrapperName != null && !wrapperName.equals(name(resource)))) {
            throw new IOException("Missing or inconsistent resource name");
        }
        validateLayout(resource);
        return resource;
    }

    private static void validateLayout(Message resource) throws IOException {
        if (resource instanceof Listener) {
            Listener listener = (Listener) resource;
            if (listener.hasApiListener()) {
                Any api = listener.getApiListener().getApiListener();
                if (!api.is(HttpConnectionManager.class)) {
                    throw new IOException("Unsupported outbound API listener");
                }
                HttpConnectionManager hcm = api.unpack(HttpConnectionManager.class);
                if (hcm.hasRouteConfig()) {
                    validateRoutes(hcm.getRouteConfig());
                } else if (!hcm.hasRds()
                        || hcm.getRds().getRouteConfigName().isEmpty()
                        || !hcm.getRds().getConfigSource().hasAds()) {
                    throw new IOException("Outbound listener must use inline routes or ADS RDS");
                }
            } else if (!listener.hasAddress() || listener.getFilterChainsCount() == 0) {
                throw new IOException("Inbound listener is missing its address or filter chains");
            }
        } else if (resource instanceof RouteConfiguration) {
            validateRoutes((RouteConfiguration) resource);
        } else if (resource instanceof Cluster) {
            Cluster cluster = (Cluster) resource;
            XdsLoadReports.validate(cluster);
            if (cluster.getType() != Cluster.DiscoveryType.EDS
                    || !cluster.hasEdsClusterConfig()
                    || !cluster.getEdsClusterConfig().getEdsConfig().hasAds()) {
                throw new IOException("Only ADS EDS clusters are currently supported");
            }
        } else if (resource instanceof ClusterLoadAssignment) {
            ClusterLoadAssignment assignment = (ClusterLoadAssignment) resource;
            Map<Long, Set<Locality>> priorities = new HashMap<>();
            long maxPriority = -1;
            for (LocalityLbEndpoints locality : assignment.getEndpointsList()) {
                long priority = Integer.toUnsignedLong(locality.getPriority());
                maxPriority = Math.max(maxPriority, priority);
                if (!priorities
                        .computeIfAbsent(priority, unused -> new HashSet<>())
                        .add(locality.getLocality())) {
                    throw new IOException("EDS has a duplicate locality within a priority");
                }
                for (LbEndpoint endpoint : locality.getLbEndpointsList()) {
                    if (!endpoint.getEndpoint().getAddress().hasSocketAddress()) {
                        throw new IOException("An EDS endpoint must have a socket address");
                    }
                    SocketAddress address = endpoint.getEndpoint().getAddress().getSocketAddress();
                    if (address.getAddress().isEmpty()
                            || address.getPortValue() < 1
                            || address.getPortValue() > 65535) {
                        throw new IOException("Invalid EDS socket address or port");
                    }
                }
            }
            // Validate without iterating an untrusted uint32 priority range.
            if (priorities.size() != maxPriority + 1) {
                throw new IOException("EDS has sparse priorities");
            }
        }
    }

    private static void validateRoutes(RouteConfiguration routes) throws IOException {
        for (VirtualHost host : routes.getVirtualHostsList()) {
            if (host.getDomainsCount() == 0) {
                throw new IOException("Virtual host has no domains");
            }
            for (Route route : host.getRoutesList()) {
                if (!route.hasRoute()) {
                    throw new IOException("Unsupported outbound route action");
                }
                switch (route.getRoute().getClusterSpecifierCase()) {
                    case CLUSTER:
                        if (route.getRoute().getCluster().isEmpty()) {
                            throw new IOException("Empty route cluster");
                        }
                        break;
                    case WEIGHTED_CLUSTERS:
                        long total = 0;
                        for (ClusterWeight cluster :
                                route.getRoute().getWeightedClusters().getClustersList()) {
                            if (cluster.getName().isEmpty() || !cluster.hasWeight()) {
                                throw new IOException("Invalid weighted route cluster");
                            }
                            total += Integer.toUnsignedLong(cluster.getWeight().getValue());
                        }
                        if (total == 0) {
                            throw new IOException("Weighted route has no positive weights");
                        }
                        break;
                    default:
                        throw new IOException("Unsupported outbound route cluster specifier");
                }
            }
        }
    }
}
