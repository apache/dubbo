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
package org.apache.dubbo.xds.resource;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.xds.resource.common.CidrRange;
import org.apache.dubbo.xds.resource.common.ConfigOrError;
import org.apache.dubbo.xds.resource.exception.ResourceInvalidException;
import org.apache.dubbo.xds.resource.filter.Filter;
import org.apache.dubbo.xds.resource.filter.FilterConfig;
import org.apache.dubbo.xds.resource.filter.FilterRegistry;
import org.apache.dubbo.xds.resource.filter.NamedFilterConfig;
import org.apache.dubbo.xds.resource.filter.router.RouterFilter;
import org.apache.dubbo.xds.resource.listener.FilterChain;
import org.apache.dubbo.xds.resource.listener.FilterChainMatch;
import org.apache.dubbo.xds.resource.listener.security.ConnectionSourceType;
import org.apache.dubbo.xds.resource.listener.security.TlsContextManager;
import org.apache.dubbo.xds.resource.route.VirtualHost;
import org.apache.dubbo.xds.resource.update.LdsUpdate;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.udpa.udpa.type.v1.TypedStruct;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;
import com.google.re2j.Pattern;
import io.envoyproxy.envoy.config.core.v3.HttpProtocolOptions;
import io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.envoyproxy.envoy.config.core.v3.TrafficDirection;
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.Rds;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.DownstreamTlsContext;

import static org.apache.dubbo.xds.resource.XdsClusterResource.validateCommonTlsContext;

public class XdsListenerResource extends XdsResourceType<LdsUpdate> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(XdsListenerResource.class);
    private static final String ADS_TYPE_URL_LDS = "type.googleapis.com/envoy.config.listener.v3.Listener";
    private static final String TYPE_URL_HTTP_CONNECTION_MANAGER =
            "type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager";
    private static final String TRANSPORT_SOCKET_NAME_TLS = "envoy.transport_sockets.tls";
    private static final XdsListenerResource instance = new XdsListenerResource();

    public static XdsListenerResource getInstance() {
        return instance;
    }

    @Override
    @Nullable
    String extractResourceName(Message unpackedResource) {
        if (!(unpackedResource instanceof Listener)) {
            return null;
        }
        return ((Listener) unpackedResource).getName();
    }

    @Override
    public String typeName() {
        return "LDS";
    }

    @Override
    Class<Listener> unpackedClassName() {
        return Listener.class;
    }

    @Override
    public String typeUrl() {
        return ADS_TYPE_URL_LDS;
    }

    @Override
    boolean isFullStateOfTheWorld() {
        return true;
    }

    @Override
    LdsUpdate doParse(Args args, Message unpackedMessage) throws ResourceInvalidException {
        if (!(unpackedMessage instanceof Listener)) {
            throw new ResourceInvalidException("Invalid message type " + unpackedMessage.getClass());
        }
        Listener listener = (Listener) unpackedMessage;

        int listenerPort = -1;
        if (listener.hasAddress() && listener.getAddress().hasSocketAddress()) {
            listenerPort = listener.getAddress().getSocketAddress().getPortValue();
        }

        LdsUpdate ldsUpdate = null;

        for (io.envoyproxy.envoy.config.listener.v3.FilterChain filterChain : listener.getFilterChainsList()) {
            for (io.envoyproxy.envoy.config.listener.v3.Filter filter : filterChain.getFiltersList()) {
                if (filter.getName().equals("envoy.filters.network.http_connection_manager")) {
                    try {
                        HttpConnectionManager hcm = unpackCompatibleType(
                                filter.getTypedConfig(),
                                HttpConnectionManager.class,
                                TYPE_URL_HTTP_CONNECTION_MANAGER,
                                null);

                        if (hcm != null) {
                            org.apache.dubbo.xds.resource.listener.HttpConnectionManager parsedHcm =
                                    parseHttpConnectionManager(hcm, args.filterRegistry, true);
                            ldsUpdate = new LdsUpdate(parsedHcm, null);
                            logger.info(
                                    "Found HttpConnectionManager in filter_chains for listener: " + listener.getName());
                            break;
                        }
                    } catch (InvalidProtocolBufferException e) {
                        logger.warn("Failed to parse HttpConnectionManager from filter", e);
                    }
                }
            }
            if (ldsUpdate != null) {
                break;
            }
        }

        // default filter_chain
        if (ldsUpdate == null && listener.hasDefaultFilterChain()) {
            logger.info("Checking default_filter_chain for listener: " + listener.getName());
            io.envoyproxy.envoy.config.listener.v3.FilterChain defaultFilterChain = listener.getDefaultFilterChain();

            for (io.envoyproxy.envoy.config.listener.v3.Filter filter : defaultFilterChain.getFiltersList()) {
                if (filter.getName().equals("envoy.filters.network.http_connection_manager")) {
                    try {
                        HttpConnectionManager hcm = unpackCompatibleType(
                                filter.getTypedConfig(),
                                HttpConnectionManager.class,
                                TYPE_URL_HTTP_CONNECTION_MANAGER,
                                null);

                        if (hcm != null) {
                            org.apache.dubbo.xds.resource.listener.HttpConnectionManager parsedHcm =
                                    parseHttpConnectionManager(hcm, args.filterRegistry, true);
                            ldsUpdate = new LdsUpdate(parsedHcm, null);
                            logger.info("Found HttpConnectionManager in default_filter_chain for listener: "
                                    + listener.getName());
                            break;
                        }
                    } catch (InvalidProtocolBufferException e) {
                        logger.warn("Failed to parse HttpConnectionManager from default filter chain", e);
                    }
                }
            }
        }

        if (ldsUpdate == null && listener.hasApiListener()) {
            logger.info("Checking api_listener for listener: " + listener.getName());
            try {
                HttpConnectionManager hcm = unpackCompatibleType(
                        listener.getApiListener().getApiListener(),
                        HttpConnectionManager.class,
                        TYPE_URL_HTTP_CONNECTION_MANAGER,
                        null);

                if (hcm != null) {
                    org.apache.dubbo.xds.resource.listener.HttpConnectionManager parsedHcm =
                            parseHttpConnectionManager(hcm, args.filterRegistry, true);
                    ldsUpdate = new LdsUpdate(parsedHcm, null);
                    logger.info("Found HttpConnectionManager in api_listener for listener: " + listener.getName());
                }
            } catch (InvalidProtocolBufferException e) {
                throw new ResourceInvalidException("Could not parse HttpConnectionManager from ApiListener", e);
            }
        }

        if (ldsUpdate == null) {
            throw new ResourceInvalidException("No valid configuration found in listener " + listener.getName());
        }

        ldsUpdate.setRawListener(listener);
        if (listenerPort > 0) {
            ldsUpdate.setPort(listenerPort);
        }

        return ldsUpdate;
    }

    private LdsUpdate processClientSideListener(Listener listener, Args args) throws ResourceInvalidException {
        // Unpack HttpConnectionManager from the Listener.
        HttpConnectionManager hcm;
        try {
            hcm = unpackCompatibleType(
                    listener.getApiListener().getApiListener(),
                    HttpConnectionManager.class,
                    TYPE_URL_HTTP_CONNECTION_MANAGER,
                    null);
        } catch (InvalidProtocolBufferException e) {
            throw new ResourceInvalidException("Could not parse HttpConnectionManager config from ApiListener", e);
        }
        return LdsUpdate.forApiListener(parseHttpConnectionManager(hcm, args.filterRegistry, true /* isForClient */));
    }

    private LdsUpdate processServerSideListener(Listener proto, Args args) throws ResourceInvalidException {
        Set<String> certProviderInstances = null;
        if (args.bootstrapInfo != null && args.bootstrapInfo.getCertProviders() != null) {
            certProviderInstances = args.bootstrapInfo.getCertProviders().keySet();
        }
        return LdsUpdate.forTcpListener(
                parseServerSideListener(proto, args.tlsContextManager, args.filterRegistry, certProviderInstances));
    }

    static org.apache.dubbo.xds.resource.listener.Listener parseServerSideListener(
            Listener proto,
            TlsContextManager tlsContextManager,
            FilterRegistry filterRegistry,
            Set<String> certProviderInstances)
            throws ResourceInvalidException {
        if (!proto.getTrafficDirection().equals(TrafficDirection.INBOUND)
                && !proto.getTrafficDirection().equals(TrafficDirection.UNSPECIFIED)) {
            throw new ResourceInvalidException(
                    "Listener " + proto.getName() + " with invalid traffic direction: " + proto.getTrafficDirection());
        }
        if (!proto.getListenerFiltersList().isEmpty()) {
            throw new ResourceInvalidException("Listener " + proto.getName() + " cannot have listener_filters");
        }
        if (proto.hasUseOriginalDst()) {
            throw new ResourceInvalidException(
                    "Listener " + proto.getName() + " cannot have use_original_dst set to true");
        }

        String address = null;
        if (proto.getAddress().hasSocketAddress()) {
            SocketAddress socketAddress = proto.getAddress().getSocketAddress();
            address = socketAddress.getAddress();
            switch (socketAddress.getPortSpecifierCase()) {
                case NAMED_PORT:
                    address = address + ":" + socketAddress.getNamedPort();
                    break;
                case PORT_VALUE:
                    address = address + ":" + socketAddress.getPortValue();
                    break;
                default:
                    // noop
            }
        }

        List<FilterChain> filterChains = new ArrayList<>();
        Set<FilterChainMatch> uniqueSet = new HashSet<>();
        for (io.envoyproxy.envoy.config.listener.v3.FilterChain fc : proto.getFilterChainsList()) {
            filterChains.add(parseFilterChain(fc, tlsContextManager, filterRegistry, uniqueSet, certProviderInstances));
        }
        FilterChain defaultFilterChain = null;
        if (proto.hasDefaultFilterChain()) {
            defaultFilterChain = parseFilterChain(
                    proto.getDefaultFilterChain(), tlsContextManager, filterRegistry, null, certProviderInstances);
        }

        return org.apache.dubbo.xds.resource.listener.Listener.create(
                proto.getName(), address, filterChains, defaultFilterChain);
    }

    static FilterChain parseFilterChain(
            io.envoyproxy.envoy.config.listener.v3.FilterChain proto,
            TlsContextManager tlsContextManager,
            FilterRegistry filterRegistry,
            Set<FilterChainMatch> uniqueSet,
            Set<String> certProviderInstances)
            throws ResourceInvalidException {
        org.apache.dubbo.xds.resource.listener.HttpConnectionManager httpConnectionManager = null;

        for (io.envoyproxy.envoy.config.listener.v3.Filter filter : proto.getFiltersList()) {
            if (!filter.hasTypedConfig()) {
                continue;
            }

            Any any = filter.getTypedConfig();
            if (any.getTypeUrl().equals(TYPE_URL_HTTP_CONNECTION_MANAGER)) {
                try {
                    HttpConnectionManager hcmProto = any.unpack(HttpConnectionManager.class);
                    httpConnectionManager =
                            parseHttpConnectionManager(hcmProto, filterRegistry, false /* isForClient */);
                    break;
                } catch (InvalidProtocolBufferException e) {
                    logger.warn("Failed to unpack HttpConnectionManager from filter " + filter.getName()
                            + " in FilterChain " + proto.getName() + ": " + e.getMessage());
                }
            }
        }

        if (httpConnectionManager == null) {
            httpConnectionManager = org.apache.dubbo.xds.resource.listener.HttpConnectionManager.create(
                    0, null, Collections.emptyList(), Collections.emptyList());
        }

        org.apache.dubbo.xds.resource.listener.security.DownstreamTlsContext downstreamTlsContext = null;
        if (proto.hasTransportSocket()) {
            if (!TRANSPORT_SOCKET_NAME_TLS.equals(proto.getTransportSocket().getName())) {
                throw new ResourceInvalidException("transport-socket with name "
                        + proto.getTransportSocket().getName() + " not supported.");
            }
            DownstreamTlsContext downstreamTlsContextProto;
            try {
                downstreamTlsContextProto =
                        proto.getTransportSocket().getTypedConfig().unpack(DownstreamTlsContext.class);
            } catch (InvalidProtocolBufferException e) {
                throw new ResourceInvalidException("FilterChain " + proto.getName() + " failed to unpack message", e);
            }
            downstreamTlsContext =
                    org.apache.dubbo.xds.resource.listener.security.DownstreamTlsContext
                            .fromEnvoyProtoDownstreamTlsContext(
                                    validateDownstreamTlsContext(downstreamTlsContextProto, certProviderInstances));
        }

        FilterChainMatch filterChainMatch = parseFilterChainMatch(proto.getFilterChainMatch());
        checkForUniqueness(uniqueSet, filterChainMatch);
        return FilterChain.create(
                proto.getName(), filterChainMatch, httpConnectionManager, downstreamTlsContext, tlsContextManager);
    }

    static DownstreamTlsContext validateDownstreamTlsContext(
            DownstreamTlsContext downstreamTlsContext, Set<String> certProviderInstances)
            throws ResourceInvalidException {
        if (downstreamTlsContext.hasCommonTlsContext()) {
            validateCommonTlsContext(downstreamTlsContext.getCommonTlsContext(), certProviderInstances, true);
        } else {
            throw new ResourceInvalidException("common-tls-context is required in downstream-tls-context");
        }
        if (downstreamTlsContext.hasRequireSni()) {
            throw new ResourceInvalidException("downstream-tls-context with require-sni is not supported");
        }
        DownstreamTlsContext.OcspStaplePolicy ocspStaplePolicy = downstreamTlsContext.getOcspStaplePolicy();
        if (ocspStaplePolicy != DownstreamTlsContext.OcspStaplePolicy.UNRECOGNIZED
                && ocspStaplePolicy != DownstreamTlsContext.OcspStaplePolicy.LENIENT_STAPLING) {
            throw new ResourceInvalidException("downstream-tls-context with ocsp_staple_policy value "
                    + ocspStaplePolicy.name() + " is not supported");
        }
        return downstreamTlsContext;
    }

    private static void checkForUniqueness(Set<FilterChainMatch> uniqueSet, FilterChainMatch filterChainMatch)
            throws ResourceInvalidException {
        if (uniqueSet != null) {
            List<FilterChainMatch> crossProduct = getCrossProduct(filterChainMatch);
            for (FilterChainMatch cur : crossProduct) {
                if (!uniqueSet.add(cur)) {
                    throw new ResourceInvalidException("FilterChainMatch must be unique. " + "Found duplicate: " + cur);
                }
            }
        }
    }

    private static List<FilterChainMatch> getCrossProduct(FilterChainMatch filterChainMatch) {
        // repeating fields to process:
        // prefixRanges, applicationProtocols, sourcePrefixRanges, sourcePorts, serverNames
        List<FilterChainMatch> expandedList = expandOnPrefixRange(filterChainMatch);
        expandedList = expandOnApplicationProtocols(expandedList);
        expandedList = expandOnSourcePrefixRange(expandedList);
        expandedList = expandOnSourcePorts(expandedList);
        return expandOnServerNames(expandedList);
    }

    private static List<FilterChainMatch> expandOnPrefixRange(FilterChainMatch filterChainMatch) {
        ArrayList<FilterChainMatch> expandedList = new ArrayList<>();
        if (filterChainMatch.getPrefixRanges().isEmpty()) {
            expandedList.add(filterChainMatch);
        } else {
            for (CidrRange cidrRange : filterChainMatch.getPrefixRanges()) {
                expandedList.add(FilterChainMatch.create(
                        filterChainMatch.getDestinationPort(),
                        Collections.singletonList(cidrRange),
                        filterChainMatch.getApplicationProtocols(),
                        filterChainMatch.getSourcePrefixRanges(),
                        filterChainMatch.getConnectionSourceType(),
                        filterChainMatch.getSourcePorts(),
                        filterChainMatch.getServerNames(),
                        filterChainMatch.getTransportProtocol()));
            }
        }
        return expandedList;
    }

    private static List<FilterChainMatch> expandOnApplicationProtocols(Collection<FilterChainMatch> set) {
        ArrayList<FilterChainMatch> expandedList = new ArrayList<>();
        for (FilterChainMatch filterChainMatch : set) {
            if (filterChainMatch.getApplicationProtocols().isEmpty()) {
                expandedList.add(filterChainMatch);
            } else {
                for (String applicationProtocol : filterChainMatch.getApplicationProtocols()) {
                    expandedList.add(FilterChainMatch.create(
                            filterChainMatch.getDestinationPort(),
                            filterChainMatch.getPrefixRanges(),
                            Collections.singletonList(applicationProtocol),
                            filterChainMatch.getSourcePrefixRanges(),
                            filterChainMatch.getConnectionSourceType(),
                            filterChainMatch.getSourcePorts(),
                            filterChainMatch.getServerNames(),
                            filterChainMatch.getTransportProtocol()));
                }
            }
        }
        return expandedList;
    }

    private static List<FilterChainMatch> expandOnSourcePrefixRange(Collection<FilterChainMatch> set) {
        ArrayList<FilterChainMatch> expandedList = new ArrayList<>();
        for (FilterChainMatch filterChainMatch : set) {
            if (filterChainMatch.getSourcePrefixRanges().isEmpty()) {
                expandedList.add(filterChainMatch);
            } else {
                for (CidrRange cidrRange : filterChainMatch.getSourcePrefixRanges()) {
                    expandedList.add(FilterChainMatch.create(
                            filterChainMatch.getDestinationPort(),
                            filterChainMatch.getPrefixRanges(),
                            filterChainMatch.getApplicationProtocols(),
                            Collections.singletonList(cidrRange),
                            filterChainMatch.getConnectionSourceType(),
                            filterChainMatch.getSourcePorts(),
                            filterChainMatch.getServerNames(),
                            filterChainMatch.getTransportProtocol()));
                }
            }
        }
        return expandedList;
    }

    private static List<FilterChainMatch> expandOnSourcePorts(Collection<FilterChainMatch> set) {
        ArrayList<FilterChainMatch> expandedList = new ArrayList<>();
        for (FilterChainMatch filterChainMatch : set) {
            if (filterChainMatch.getSourcePorts().isEmpty()) {
                expandedList.add(filterChainMatch);
            } else {
                for (Integer sourcePort : filterChainMatch.getSourcePorts()) {
                    expandedList.add(FilterChainMatch.create(
                            filterChainMatch.getDestinationPort(),
                            filterChainMatch.getPrefixRanges(),
                            filterChainMatch.getApplicationProtocols(),
                            filterChainMatch.getSourcePrefixRanges(),
                            filterChainMatch.getConnectionSourceType(),
                            Collections.singletonList(sourcePort),
                            filterChainMatch.getServerNames(),
                            filterChainMatch.getTransportProtocol()));
                }
            }
        }
        return expandedList;
    }

    private static List<FilterChainMatch> expandOnServerNames(Collection<FilterChainMatch> set) {
        ArrayList<FilterChainMatch> expandedList = new ArrayList<>();
        for (FilterChainMatch filterChainMatch : set) {
            if (filterChainMatch.getServerNames().isEmpty()) {
                expandedList.add(filterChainMatch);
            } else {
                for (String serverName : filterChainMatch.getServerNames()) {
                    expandedList.add(FilterChainMatch.create(
                            filterChainMatch.getDestinationPort(),
                            filterChainMatch.getPrefixRanges(),
                            filterChainMatch.getApplicationProtocols(),
                            filterChainMatch.getSourcePrefixRanges(),
                            filterChainMatch.getConnectionSourceType(),
                            filterChainMatch.getSourcePorts(),
                            Collections.singletonList(serverName),
                            filterChainMatch.getTransportProtocol()));
                }
            }
        }
        return expandedList;
    }

    private static FilterChainMatch parseFilterChainMatch(io.envoyproxy.envoy.config.listener.v3.FilterChainMatch proto)
            throws ResourceInvalidException {
        List<CidrRange> prefixRanges = new ArrayList<>();
        List<CidrRange> sourcePrefixRanges = new ArrayList<>();
        try {
            for (io.envoyproxy.envoy.config.core.v3.CidrRange range : proto.getPrefixRangesList()) {
                prefixRanges.add(CidrRange.create(
                        range.getAddressPrefix(), range.getPrefixLen().getValue()));
            }
            for (io.envoyproxy.envoy.config.core.v3.CidrRange range : proto.getSourcePrefixRangesList()) {
                sourcePrefixRanges.add(CidrRange.create(
                        range.getAddressPrefix(), range.getPrefixLen().getValue()));
            }
        } catch (UnknownHostException e) {
            throw new ResourceInvalidException("Failed to create CidrRange", e);
        }
        ConnectionSourceType sourceType;
        switch (proto.getSourceType()) {
            case ANY:
                sourceType = ConnectionSourceType.ANY;
                break;
            case EXTERNAL:
                sourceType = ConnectionSourceType.EXTERNAL;
                break;
            case SAME_IP_OR_LOOPBACK:
                sourceType = ConnectionSourceType.SAME_IP_OR_LOOPBACK;
                break;
            default:
                throw new ResourceInvalidException("Unknown source-type: " + proto.getSourceType());
        }
        return FilterChainMatch.create(
                proto.getDestinationPort().getValue(),
                prefixRanges,
                new ArrayList<>(proto.getApplicationProtocolsList()),
                sourcePrefixRanges,
                sourceType,
                new ArrayList<>(proto.getSourcePortsList()),
                new ArrayList<>(proto.getServerNamesList()),
                proto.getTransportProtocol());
    }

    static org.apache.dubbo.xds.resource.listener.HttpConnectionManager parseHttpConnectionManager(
            HttpConnectionManager proto, FilterRegistry filterRegistry, boolean isForClient)
            throws ResourceInvalidException {
        if (proto.getXffNumTrustedHops() != 0) {
            throw new ResourceInvalidException("HttpConnectionManager with xff_num_trusted_hops unsupported");
        }
        if (!proto.getOriginalIpDetectionExtensionsList().isEmpty()) {
            throw new ResourceInvalidException(
                    "HttpConnectionManager with " + "original_ip_detection_extensions unsupported");
        }

        long maxStreamDuration = 0;
        if (proto.hasCommonHttpProtocolOptions()) {
            HttpProtocolOptions options = proto.getCommonHttpProtocolOptions();
            if (options.hasMaxStreamDuration()) {
                maxStreamDuration = Durations.toNanos(options.getMaxStreamDuration());
            }
        }

        // Parse http filters.
        if (proto.getHttpFiltersList().isEmpty()) {
            throw new ResourceInvalidException("Missing HttpFilter in HttpConnectionManager.");
        }
        List<NamedFilterConfig> filterConfigs = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (int i = 0; i < proto.getHttpFiltersCount(); i++) {
            io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter httpFilter =
                    proto.getHttpFiltersList().get(i);
            String filterName = httpFilter.getName();
            if (!names.add(filterName)) {
                throw new ResourceInvalidException(
                        "HttpConnectionManager contains duplicate HttpFilter: " + filterName);
            }
            StructOrError<FilterConfig> filterConfig = parseHttpFilter(httpFilter, filterRegistry, isForClient);
            if ((i == proto.getHttpFiltersCount() - 1)
                    && (filterConfig == null || !isTerminalFilter(filterConfig.getStruct()))) {
                throw new ResourceInvalidException("The last HttpFilter must be a terminal filter: " + filterName);
            }
            if (filterConfig == null) {
                continue;
            }
            if (filterConfig.getErrorDetail() != null) {
                throw new ResourceInvalidException(
                        "HttpConnectionManager contains invalid HttpFilter: " + filterConfig.getErrorDetail());
            }
            if ((i < proto.getHttpFiltersCount() - 1) && isTerminalFilter(filterConfig.getStruct())) {
                throw new ResourceInvalidException("A terminal HttpFilter must be the last filter: " + filterName);
            }
            filterConfigs.add(new NamedFilterConfig(filterName, filterConfig.getStruct()));
        }

        // Parse inlined RouteConfiguration or RDS.
        if (proto.hasRouteConfig()) {
            List<VirtualHost> virtualHosts = extractVirtualHosts(proto.getRouteConfig(), filterRegistry);
            return org.apache.dubbo.xds.resource.listener.HttpConnectionManager.forVirtualHosts(
                    maxStreamDuration, virtualHosts, filterConfigs);
        }
        if (proto.hasRds()) {
            Rds rds = proto.getRds();
            if (!rds.hasConfigSource()) {
                throw new ResourceInvalidException("HttpConnectionManager contains invalid RDS: missing config_source");
            }
            if (!rds.getConfigSource().hasAds() && !rds.getConfigSource().hasSelf()) {
                throw new ResourceInvalidException(
                        "HttpConnectionManager contains invalid RDS: must specify ADS or " + "self ConfigSource");
            }
            return org.apache.dubbo.xds.resource.listener.HttpConnectionManager.forRdsName(
                    maxStreamDuration, rds.getRouteConfigName(), filterConfigs);
        }
        throw new ResourceInvalidException("HttpConnectionManager neither has inlined route_config nor RDS");
    }

    static List<VirtualHost> extractVirtualHosts(RouteConfiguration routeConfig, FilterRegistry filterRegistry)
            throws ResourceInvalidException {
        List<VirtualHost> virtualHosts = new ArrayList<>();
        for (io.envoyproxy.envoy.config.route.v3.VirtualHost virtualHostProto : routeConfig.getVirtualHostsList()) {
            List<org.apache.dubbo.xds.resource.route.Route> routes = new ArrayList<>();
            for (io.envoyproxy.envoy.config.route.v3.Route routeProto : virtualHostProto.getRoutesList()) {
                org.apache.dubbo.xds.resource.route.RouteMatch routeMatch = parseRouteMatch(routeProto.getMatch());
                if (routeMatch == null) {
                    continue;
                }
                org.apache.dubbo.xds.resource.route.RouteAction routeAction = parseRouteAction(routeProto.getRoute());
                if (routeAction != null) {
                    routes.add(org.apache.dubbo.xds.resource.route.Route.create(
                            routeMatch,
                            routeAction,
                            parseTypedPerFilterConfig(routeProto.getTypedPerFilterConfigMap(), filterRegistry)));
                }
            }
            virtualHosts.add(org.apache.dubbo.xds.resource.route.VirtualHost.create(
                    virtualHostProto.getName(),
                    new ArrayList<>(virtualHostProto.getDomainsList()),
                    routes,
                    parseTypedPerFilterConfig(virtualHostProto.getTypedPerFilterConfigMap(), filterRegistry)));
        }
        return virtualHosts;
    }

    private static Map<String, FilterConfig> parseTypedPerFilterConfig(
            Map<String, Any> typedPerFilterConfigMap, FilterRegistry filterRegistry) {
        Map<String, FilterConfig> configs = new HashMap<>();
        for (Map.Entry<String, Any> entry : typedPerFilterConfigMap.entrySet()) {
            String filterName = entry.getKey();
            Any any = entry.getValue();
            String typeUrl = any.getTypeUrl();
            boolean isOptional = false;

            // 处理FilterConfig包装
            if (typeUrl.equals("type.googleapis.com/envoy.config.route.v3.FilterConfig")) {
                try {
                    io.envoyproxy.envoy.config.route.v3.FilterConfig filterConfig =
                            any.unpack(io.envoyproxy.envoy.config.route.v3.FilterConfig.class);
                    isOptional = filterConfig.getIsOptional();
                    any = filterConfig.getConfig();
                    typeUrl = any.getTypeUrl();
                } catch (InvalidProtocolBufferException e) {
                    logger.warn(
                            "Failed to unpack FilterConfig for filter: " + filterName + ", error: " + e.getMessage());
                    continue;
                }
            }

            try {
                // 解析TypedStruct
                Message rawConfig = any;
                if (typeUrl.equals("type.googleapis.com/udpa.type.v1.TypedStruct")) {
                    try {
                        TypedStruct typedStruct = any.unpack(TypedStruct.class);
                        typeUrl = typedStruct.getTypeUrl();
                        rawConfig = typedStruct.getValue();
                    } catch (InvalidProtocolBufferException e) {
                        logger.warn("Failed to unpack TypedStruct for filter: " + filterName + ", error: "
                                + e.getMessage());
                        continue;
                    }
                } else if (typeUrl.equals("type.googleapis.com/xds.type.v3.TypedStruct")) {
                    try {
                        com.github.xds.type.v3.TypedStruct newTypedStruct =
                                any.unpack(com.github.xds.type.v3.TypedStruct.class);
                        typeUrl = newTypedStruct.getTypeUrl();
                        rawConfig = newTypedStruct.getValue();
                    } catch (InvalidProtocolBufferException e) {
                        logger.warn("Failed to unpack new TypedStruct for filter: " + filterName + ", error: "
                                + e.getMessage());
                        continue;
                    }
                }

                // 在解析TypedStruct后重新获取filter
                Filter filter = filterRegistry.get(typeUrl);
                if (filter == null) {
                    // Treat unknown filters as optional in proxyless mode, just log and skip.
                    logger.warn(
                            "HttpFilter [" + filterName + "](" + typeUrl + ") is not supported in Dubbo, skipping.");
                    continue;
                }

                ConfigOrError<? extends FilterConfig> filterConfig = filter.parseFilterConfig(rawConfig);
                if (filterConfig.errorDetail == null) {
                    configs.put(filterName, filterConfig.config);
                } else {
                    logger.warn("Failed to parse filter config for filter: " + filterName + ", error: "
                            + filterConfig.errorDetail);
                }
            } catch (Exception e) {
                logger.warn("Failed to parse filter config for filter: " + filterName + ", error: " + e.getMessage());
            }
        }
        return configs;
    }

    private static org.apache.dubbo.xds.resource.route.RouteMatch parseRouteMatch(
            io.envoyproxy.envoy.config.route.v3.RouteMatch routeMatchProto) {
        // 暂时不支持查询参数匹配
        if (routeMatchProto.getQueryParametersCount() != 0) {
            return null;
        }

        org.apache.dubbo.xds.resource.matcher.PathMatcher pathMatcher;
        boolean caseSensitive = routeMatchProto.getCaseSensitive().getValue();

        // 解析路径匹配器
        switch (routeMatchProto.getPathSpecifierCase()) {
            case PATH:
                pathMatcher = org.apache.dubbo.xds.resource.matcher.PathMatcher.fromPath(
                        routeMatchProto.getPath(), caseSensitive);
                break;
            case PREFIX:
                pathMatcher = org.apache.dubbo.xds.resource.matcher.PathMatcher.fromPrefix(
                        routeMatchProto.getPrefix(), caseSensitive);
                break;
            case SAFE_REGEX:
                try {
                    Pattern safeRegEx = com.google.re2j.Pattern.compile(
                            routeMatchProto.getSafeRegex().getRegex());
                    pathMatcher = org.apache.dubbo.xds.resource.matcher.PathMatcher.fromRegEx(safeRegEx);
                } catch (Exception e) {
                    // 如果正则表达式无效，返回null
                    logger.warn("Invalid regex pattern: " + e.getMessage());
                    return null;
                }
                break;
            case PATHSPECIFIER_NOT_SET:
            default:
                // 未知的路径匹配类型，返回null
                return null;
        }

        // 解析头部匹配器
        List<org.apache.dubbo.xds.resource.matcher.HeaderMatcher> headerMatchers = new ArrayList<>();
        for (io.envoyproxy.envoy.config.route.v3.HeaderMatcher hmProto : routeMatchProto.getHeadersList()) {
            try {
                org.apache.dubbo.xds.resource.matcher.HeaderMatcher headerMatcher =
                        org.apache.dubbo.xds.resource.matcher.MatcherParser.parseHeaderMatcher(hmProto);
                headerMatchers.add(headerMatcher);
            } catch (Exception e) {
                // 如果头部匹配器解析失败，记录警告但继续处理
                logger.warn("Failed to parse header matcher: " + e.getMessage());
            }
        }

        // 解析分数匹配器（如果存在）
        org.apache.dubbo.xds.resource.matcher.FractionMatcher fractionMatcher = null;
        if (routeMatchProto.hasRuntimeFraction()) {
            io.envoyproxy.envoy.config.core.v3.RuntimeFractionalPercent runtimeFraction =
                    routeMatchProto.getRuntimeFraction();
            if (runtimeFraction.hasDefaultValue()) {
                io.envoyproxy.envoy.type.v3.FractionalPercent percent = runtimeFraction.getDefaultValue();
                int numerator = percent.getNumerator();
                int denominator;
                switch (percent.getDenominator()) {
                    case HUNDRED:
                        denominator = 100;
                        break;
                    case TEN_THOUSAND:
                        denominator = 10_000;
                        break;
                    case MILLION:
                        denominator = 1_000_000;
                        break;
                    case UNRECOGNIZED:
                    default:
                        // 不支持的分数类型，返回null
                        return null;
                }
                fractionMatcher = org.apache.dubbo.xds.resource.matcher.FractionMatcher.create(numerator, denominator);
            }
        }

        // 创建RouteMatch对象
        return new org.apache.dubbo.xds.resource.route.RouteMatch(pathMatcher, headerMatchers, fractionMatcher);
    }

    private static org.apache.dubbo.xds.resource.route.RouteAction parseRouteAction(
            io.envoyproxy.envoy.config.route.v3.RouteAction routeActionProto) {
        // 解析超时设置
        Long timeoutNano = null;

        // 1. 首先检查标准的timeout字段（来自VirtualService的timeout配置）
        if (routeActionProto.hasTimeout()) {
            timeoutNano = com.google.protobuf.util.Durations.toNanos(routeActionProto.getTimeout());
        }

        // 2. 如果没有timeout，再检查MaxStreamDuration（gRPC特定的超时设置）
        if (timeoutNano == null && routeActionProto.hasMaxStreamDuration()) {
            io.envoyproxy.envoy.config.route.v3.RouteAction.MaxStreamDuration maxStreamDuration =
                    routeActionProto.getMaxStreamDuration();
            if (maxStreamDuration.hasGrpcTimeoutHeaderMax()) {
                timeoutNano = com.google.protobuf.util.Durations.toNanos(maxStreamDuration.getGrpcTimeoutHeaderMax());
            } else if (maxStreamDuration.hasMaxStreamDuration()) {
                timeoutNano = com.google.protobuf.util.Durations.toNanos(maxStreamDuration.getMaxStreamDuration());
            }
        }

        // 解析哈希策略
        List<org.apache.dubbo.xds.resource.route.HashPolicy> hashPolicies = new ArrayList<>();
        for (io.envoyproxy.envoy.config.route.v3.RouteAction.HashPolicy config : routeActionProto.getHashPolicyList()) {
            try {
                org.apache.dubbo.xds.resource.route.HashPolicy policy = null;
                boolean terminal = config.getTerminal();
                switch (config.getPolicySpecifierCase()) {
                    case HEADER:
                        io.envoyproxy.envoy.config.route.v3.RouteAction.HashPolicy.Header headerCfg =
                                config.getHeader();
                        Pattern regEx = null;
                        String regExSubstitute = null;
                        if (headerCfg.hasRegexRewrite()
                                && headerCfg.getRegexRewrite().hasPattern()
                                && headerCfg.getRegexRewrite().getPattern().hasGoogleRe2()) {
                            try {
                                regEx = Pattern.compile(
                                        headerCfg.getRegexRewrite().getPattern().getRegex());
                                regExSubstitute = headerCfg.getRegexRewrite().getSubstitution();
                            } catch (Exception e) {
                                logger.warn("Failed to compile regex pattern: " + e.getMessage());
                                return null;
                            }
                        }
                        policy = org.apache.dubbo.xds.resource.route.HashPolicy.forHeader(
                                terminal, headerCfg.getHeaderName(), regEx, regExSubstitute);
                        break;
                    case FILTER_STATE:
                        if (config.getFilterState().getKey().equals("io.grpc.channel_id")) {
                            policy = org.apache.dubbo.xds.resource.route.HashPolicy.forChannelId(terminal);
                        }
                        break;
                    default:
                        // 忽略其他类型的哈希策略
                        break;
                }
                if (policy != null) {
                    hashPolicies.add(policy);
                }
            } catch (Exception e) {
                // 如果哈希策略解析失败，记录警告但继续处理
                logger.warn("Failed to parse hash policy: " + e.getMessage());
            }
        }

        // 解析重试策略
        org.apache.dubbo.xds.resource.route.RetryPolicy retryPolicy = null;
        if (routeActionProto.hasRetryPolicy()) {
            // 简单实现，实际应该解析完整的重试策略
            // 暂时跳过重试策略解析，避免构造函数参数不匹配的问题
            retryPolicy = null;
        }

        // 根据集群类型创建不同的RouteAction
        switch (routeActionProto.getClusterSpecifierCase()) {
            case CLUSTER:
                return org.apache.dubbo.xds.resource.route.RouteAction.forCluster(
                        routeActionProto.getCluster(), hashPolicies, timeoutNano, retryPolicy);
            case CLUSTER_HEADER:
                // 暂时忽略CLUSTER_HEADER类型
                return null;
            case WEIGHTED_CLUSTERS:
                List<io.envoyproxy.envoy.config.route.v3.WeightedCluster.ClusterWeight> clusterWeights =
                        routeActionProto.getWeightedClusters().getClustersList();
                if (clusterWeights.isEmpty()) {
                    logger.warn("No cluster found in weighted cluster list");
                    return null;
                }
                List<org.apache.dubbo.xds.resource.route.ClusterWeight> weightedClusters = new ArrayList<>();
                long clusterWeightSum = 0;
                for (io.envoyproxy.envoy.config.route.v3.WeightedCluster.ClusterWeight clusterWeight : clusterWeights) {
                    int weight = clusterWeight.getWeight().getValue();
                    clusterWeightSum += weight;
                    weightedClusters.add(new org.apache.dubbo.xds.resource.route.ClusterWeight(
                            clusterWeight.getName(), weight, new HashMap<>()));
                }
                if (clusterWeightSum <= 0) {
                    logger.warn("Sum of cluster weights should be above 0");
                    return null;
                }
                if (clusterWeightSum > 0xFFFFFFFFL) {
                    logger.warn("Sum of cluster weights should be less than the maximum unsigned integer");
                    return null;
                }
                return org.apache.dubbo.xds.resource.route.RouteAction.forWeightedClusters(
                        weightedClusters, hashPolicies, timeoutNano, retryPolicy);
            case CLUSTER_SPECIFIER_PLUGIN:
                // 暂时忽略CLUSTER_SPECIFIER_PLUGIN类型
                return null;
            case CLUSTERSPECIFIER_NOT_SET:
            default:
                return null;
        }
    }

    // hard-coded: currently router config is the only terminal filter.
    private static boolean isTerminalFilter(FilterConfig filterConfig) {
        return RouterFilter.ROUTER_CONFIG.equals(filterConfig);
    }

    @Nullable // Returns null if the filter is optional but not supported.
    static StructOrError<FilterConfig> parseHttpFilter(
            io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter httpFilter,
            FilterRegistry filterRegistry,
            boolean isForClient) {
        String filterName = httpFilter.getName();
        boolean isOptional = httpFilter.getIsOptional();
        if (!httpFilter.hasTypedConfig()) {
            if (isOptional) {
                return null;
            } else {
                return StructOrError.fromError(
                        "HttpFilter [" + filterName + "] is not optional and has no typed config");
            }
        }
        Message rawConfig = httpFilter.getTypedConfig();
        String typeUrl = httpFilter.getTypedConfig().getTypeUrl();

        try {
            if (typeUrl.equals(TYPE_URL_TYPED_STRUCT_UDPA)) {
                TypedStruct typedStruct = httpFilter.getTypedConfig().unpack(TypedStruct.class);
                typeUrl = typedStruct.getTypeUrl();
                rawConfig = typedStruct.getValue();
            } else if (typeUrl.equals(TYPE_URL_TYPED_STRUCT)) {
                com.github.xds.type.v3.TypedStruct newTypedStruct =
                        httpFilter.getTypedConfig().unpack(com.github.xds.type.v3.TypedStruct.class);
                typeUrl = newTypedStruct.getTypeUrl();
                rawConfig = newTypedStruct.getValue();
            }
        } catch (InvalidProtocolBufferException e) {
            return StructOrError.fromError("HttpFilter [" + filterName + "] contains invalid proto: " + e);
        }
        Filter filter = filterRegistry.get(typeUrl);
        if (filter == null) {
            logger.warn("HttpFilter [" + filterName + "](" + typeUrl + ") is not supported in Dubbo, skipping.");
            return null;
        }

        // if ((isForClient && !(filter instanceof ClientFilter)) || (!isForClient && !(filter instanceof
        // ServerFilter))) {
        //    if (isOptional) {
        //        return null;
        //    } else {
        //        return StructOrError.fromError("HttpFilter [" + filterName + "](" + typeUrl
        //                + ") is required but unsupported for " + (isForClient ? "client" : "server"));
        //    }
        // }

        // if ((isForClient && !(filter instanceof Filter.ClientInterceptorBuilder))
        //        || (!isForClient && !(filter instanceof Filter.ServerInterceptorBuilder))) {
        //    if (isOptional) {
        //        return null;
        //    } else {
        //        return StructOrError.fromError(
        //                "HttpFilter [" + filterName + "](" + typeUrl + ") is required but unsupported for "
        //                        + (isForClient ? "client" : "server"));
        //    }
        // }
        ConfigOrError<? extends FilterConfig> filterConfig = filter.parseFilterConfig(rawConfig);
        if (filterConfig.errorDetail != null) {
            return StructOrError.fromError(
                    "Invalid filter config for HttpFilter [" + filterName + "]: " + filterConfig.errorDetail);
        }
        return StructOrError.fromStruct(filterConfig.config);
    }
}
