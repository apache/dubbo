/*
 * Copyright 2022 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds.resource.grpc.resource;


import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.DownstreamTlsContext;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData.CidrRange;
import org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData.ConnectionSourceType;
import org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData.FilterChain;
import org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData.FilterChainMatch;
import org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData.TlsContextManager;
import org.apache.dubbo.xds.resource.grpc.resource.exception.ResourceInvalidException;
import org.apache.dubbo.xds.resource.grpc.resource.filter.ClientInterceptorBuilder;
import org.apache.dubbo.xds.resource.grpc.resource.common.ConfigOrError;
import org.apache.dubbo.xds.resource.grpc.resource.filter.Filter;
import org.apache.dubbo.xds.resource.grpc.resource.filter.FilterConfig;
import org.apache.dubbo.xds.resource.grpc.resource.filter.FilterRegistry;
import org.apache.dubbo.xds.resource.grpc.resource.filter.NamedFilterConfig;
import org.apache.dubbo.xds.resource.grpc.resource.filter.ServerInterceptorBuilder;
import org.apache.dubbo.xds.resource.grpc.resource.update.LdsUpdate;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.udpa.udpa.type.v1.TypedStruct;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;
import io.envoyproxy.envoy.config.core.v3.HttpProtocolOptions;
import io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.envoyproxy.envoy.config.core.v3.TrafficDirection;
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.Rds;

import static org.apache.dubbo.xds.resource.grpc.resource.XdsClusterResource.validateCommonTlsContext;

public class XdsListenerResource extends XdsResourceType<LdsUpdate> {
  static final String ADS_TYPE_URL_LDS =
      "type.googleapis.com/envoy.config.listener.v3.Listener";
  static final String TYPE_URL_HTTP_CONNECTION_MANAGER =
      "type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3"
          + ".HttpConnectionManager";
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
  String typeName() {
    return "LDS";
  }

  @Override
  Class<Listener> unpackedClassName() {
    return Listener.class;
  }

  @Override
  String typeUrl() {
    return ADS_TYPE_URL_LDS;
  }

  @Override
  boolean isFullStateOfTheWorld() {
    return true;
  }

  @Override
  LdsUpdate doParse(Args args, Message unpackedMessage)
      throws ResourceInvalidException {
    if (!(unpackedMessage instanceof Listener)) {
      throw new ResourceInvalidException("Invalid message type: " + unpackedMessage.getClass());
    }
    Listener listener = (Listener) unpackedMessage;

    if (listener.hasApiListener()) {
      return processClientSideListener(
          listener, args);
    } else {
      return processServerSideListener(
          listener, args);
    }
  }

  private LdsUpdate processClientSideListener(Listener listener, Args args)
      throws ResourceInvalidException {
    // Unpack HttpConnectionManager from the Listener.
    HttpConnectionManager hcm;
    try {
      hcm = unpackCompatibleType(
          listener.getApiListener().getApiListener(), HttpConnectionManager.class,
          TYPE_URL_HTTP_CONNECTION_MANAGER, null);
    } catch (InvalidProtocolBufferException e) {
      throw new ResourceInvalidException(
          "Could not parse HttpConnectionManager config from ApiListener", e);
    }
    return LdsUpdate.forApiListener(parseHttpConnectionManager(
        hcm, args.filterRegistry, true /* isForClient */));
  }

  private LdsUpdate processServerSideListener(Listener proto, Args args)
      throws ResourceInvalidException {
    Set<String> certProviderInstances = null;
    if (args.bootstrapInfo != null && args.bootstrapInfo.certProviders() != null) {
      certProviderInstances = args.bootstrapInfo.certProviders().keySet();
    }
    return LdsUpdate.forTcpListener(parseServerSideListener(proto, args.tlsContextManager,
        args.filterRegistry, certProviderInstances));
  }

  static org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData.Listener parseServerSideListener(
          Listener proto, TlsContextManager tlsContextManager,
          FilterRegistry filterRegistry, Set<String> certProviderInstances)
      throws ResourceInvalidException {
    if (!proto.getTrafficDirection().equals(TrafficDirection.INBOUND)
        && !proto.getTrafficDirection().equals(TrafficDirection.UNSPECIFIED)) {
      throw new ResourceInvalidException(
          "Listener " + proto.getName() + " with invalid traffic direction: "
              + proto.getTrafficDirection());
    }
    if (!proto.getListenerFiltersList().isEmpty()) {
      throw new ResourceInvalidException(
          "Listener " + proto.getName() + " cannot have listener_filters");
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
      filterChains.add(
          parseFilterChain(fc, tlsContextManager, filterRegistry, uniqueSet,
              certProviderInstances));
    }
    FilterChain defaultFilterChain = null;
    if (proto.hasDefaultFilterChain()) {
      defaultFilterChain = parseFilterChain(
          proto.getDefaultFilterChain(), tlsContextManager, filterRegistry,
          null, certProviderInstances);
    }

    return org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData.Listener.create(
        proto.getName(), address, filterChains, defaultFilterChain);
  }

  static FilterChain parseFilterChain(
          io.envoyproxy.envoy.config.listener.v3.FilterChain proto,
          TlsContextManager tlsContextManager, FilterRegistry filterRegistry,
          Set<FilterChainMatch> uniqueSet, Set<String> certProviderInstances)
      throws ResourceInvalidException {
    if (proto.getFiltersCount() != 1) {
      throw new ResourceInvalidException("FilterChain " + proto.getName()
          + " should contain exact one HttpConnectionManager filter");
    }
    io.envoyproxy.envoy.config.listener.v3.Filter filter = proto.getFiltersList().get(0);
    if (!filter.hasTypedConfig()) {
      throw new ResourceInvalidException(
          "FilterChain " + proto.getName() + " contains filter " + filter.getName()
              + " without typed_config");
    }
    Any any = filter.getTypedConfig();
    // HttpConnectionManager is the only supported network filter at the moment.
    if (!any.getTypeUrl().equals(TYPE_URL_HTTP_CONNECTION_MANAGER)) {
      throw new ResourceInvalidException(
          "FilterChain " + proto.getName() + " contains filter " + filter.getName()
              + " with unsupported typed_config type " + any.getTypeUrl());
    }
    HttpConnectionManager hcmProto;
    try {
      hcmProto = any.unpack(HttpConnectionManager.class);
    } catch (InvalidProtocolBufferException e) {
      throw new ResourceInvalidException("FilterChain " + proto.getName() + " with filter "
          + filter.getName() + " failed to unpack message", e);
    }
      org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData.HttpConnectionManager httpConnectionManager = parseHttpConnectionManager(
        hcmProto, filterRegistry, false /* isForClient */);

      org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData.DownstreamTlsContext downstreamTlsContext = null;
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
        throw new ResourceInvalidException("FilterChain " + proto.getName()
            + " failed to unpack message", e);
      }
      downstreamTlsContext =
              org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData.DownstreamTlsContext.fromEnvoyProtoDownstreamTlsContext(
              validateDownstreamTlsContext(downstreamTlsContextProto, certProviderInstances));
    }

    FilterChainMatch filterChainMatch = parseFilterChainMatch(proto.getFilterChainMatch());
    checkForUniqueness(uniqueSet, filterChainMatch);
    return FilterChain.create(
        proto.getName(),
        filterChainMatch,
        httpConnectionManager,
        downstreamTlsContext,
        tlsContextManager
    );
  }

  static DownstreamTlsContext validateDownstreamTlsContext(
      DownstreamTlsContext downstreamTlsContext, Set<String> certProviderInstances)
      throws ResourceInvalidException {
    if (downstreamTlsContext.hasCommonTlsContext()) {
      validateCommonTlsContext(downstreamTlsContext.getCommonTlsContext(), certProviderInstances,
          true);
    } else {
      throw new ResourceInvalidException(
          "common-tls-context is required in downstream-tls-context");
    }
    if (downstreamTlsContext.hasRequireSni()) {
      throw new ResourceInvalidException(
          "downstream-tls-context with require-sni is not supported");
    }
    DownstreamTlsContext.OcspStaplePolicy ocspStaplePolicy = downstreamTlsContext
        .getOcspStaplePolicy();
    if (ocspStaplePolicy != DownstreamTlsContext.OcspStaplePolicy.UNRECOGNIZED
        && ocspStaplePolicy != DownstreamTlsContext.OcspStaplePolicy.LENIENT_STAPLING) {
      throw new ResourceInvalidException(
          "downstream-tls-context with ocsp_staple_policy value " + ocspStaplePolicy.name()
              + " is not supported");
    }
    return downstreamTlsContext;
  }

  private static void checkForUniqueness(Set<FilterChainMatch> uniqueSet,
      FilterChainMatch filterChainMatch) throws ResourceInvalidException {
    if (uniqueSet != null) {
      List<FilterChainMatch> crossProduct = getCrossProduct(filterChainMatch);
      for (FilterChainMatch cur : crossProduct) {
        if (!uniqueSet.add(cur)) {
          throw new ResourceInvalidException("FilterChainMatch must be unique. "
              + "Found duplicate: " + cur);
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
    if (filterChainMatch.prefixRanges().isEmpty()) {
      expandedList.add(filterChainMatch);
    } else {
      for (CidrRange cidrRange : filterChainMatch.prefixRanges()) {
        expandedList.add(FilterChainMatch.create(filterChainMatch.destinationPort(),
            Collections.singletonList(cidrRange),
            filterChainMatch.applicationProtocols(),
            filterChainMatch.sourcePrefixRanges(),
            filterChainMatch.connectionSourceType(),
            filterChainMatch.sourcePorts(),
            filterChainMatch.serverNames(),
            filterChainMatch.transportProtocol()));
      }
    }
    return expandedList;
  }

  private static List<FilterChainMatch> expandOnApplicationProtocols(
      Collection<FilterChainMatch> set) {
    ArrayList<FilterChainMatch> expandedList = new ArrayList<>();
    for (FilterChainMatch filterChainMatch : set) {
      if (filterChainMatch.applicationProtocols().isEmpty()) {
        expandedList.add(filterChainMatch);
      } else {
        for (String applicationProtocol : filterChainMatch.applicationProtocols()) {
          expandedList.add(FilterChainMatch.create(filterChainMatch.destinationPort(),
              filterChainMatch.prefixRanges(),
              Collections.singletonList(applicationProtocol),
              filterChainMatch.sourcePrefixRanges(),
              filterChainMatch.connectionSourceType(),
              filterChainMatch.sourcePorts(),
              filterChainMatch.serverNames(),
              filterChainMatch.transportProtocol()));
        }
      }
    }
    return expandedList;
  }

  private static List<FilterChainMatch> expandOnSourcePrefixRange(
      Collection<FilterChainMatch> set) {
    ArrayList<FilterChainMatch> expandedList = new ArrayList<>();
    for (FilterChainMatch filterChainMatch : set) {
      if (filterChainMatch.sourcePrefixRanges().isEmpty()) {
        expandedList.add(filterChainMatch);
      } else {
        for (CidrRange cidrRange : filterChainMatch.sourcePrefixRanges()) {
          expandedList.add(FilterChainMatch.create(filterChainMatch.destinationPort(),
              filterChainMatch.prefixRanges(),
              filterChainMatch.applicationProtocols(),
              Collections.singletonList(cidrRange),
              filterChainMatch.connectionSourceType(),
              filterChainMatch.sourcePorts(),
              filterChainMatch.serverNames(),
              filterChainMatch.transportProtocol()));
        }
      }
    }
    return expandedList;
  }

  private static List<FilterChainMatch> expandOnSourcePorts(Collection<FilterChainMatch> set) {
    ArrayList<FilterChainMatch> expandedList = new ArrayList<>();
    for (FilterChainMatch filterChainMatch : set) {
      if (filterChainMatch.sourcePorts().isEmpty()) {
        expandedList.add(filterChainMatch);
      } else {
        for (Integer sourcePort : filterChainMatch.sourcePorts()) {
          expandedList.add(FilterChainMatch.create(filterChainMatch.destinationPort(),
              filterChainMatch.prefixRanges(),
              filterChainMatch.applicationProtocols(),
              filterChainMatch.sourcePrefixRanges(),
              filterChainMatch.connectionSourceType(),
              Collections.singletonList(sourcePort),
              filterChainMatch.serverNames(),
              filterChainMatch.transportProtocol()));
        }
      }
    }
    return expandedList;
  }

  private static List<FilterChainMatch> expandOnServerNames(Collection<FilterChainMatch> set) {
    ArrayList<FilterChainMatch> expandedList = new ArrayList<>();
    for (FilterChainMatch filterChainMatch : set) {
      if (filterChainMatch.serverNames().isEmpty()) {
        expandedList.add(filterChainMatch);
      } else {
        for (String serverName : filterChainMatch.serverNames()) {
          expandedList.add(FilterChainMatch.create(filterChainMatch.destinationPort(),
              filterChainMatch.prefixRanges(),
              filterChainMatch.applicationProtocols(),
              filterChainMatch.sourcePrefixRanges(),
              filterChainMatch.connectionSourceType(),
              filterChainMatch.sourcePorts(),
              Collections.singletonList(serverName),
              filterChainMatch.transportProtocol()));
        }
      }
    }
    return expandedList;
  }

  private static FilterChainMatch parseFilterChainMatch(
      io.envoyproxy.envoy.config.listener.v3.FilterChainMatch proto)
      throws ResourceInvalidException {
      List<CidrRange> prefixRanges = new ArrayList<>();
    List<CidrRange> sourcePrefixRanges = new ArrayList<>();
    try {
      for (io.envoyproxy.envoy.config.core.v3.CidrRange range : proto.getPrefixRangesList()) {
        prefixRanges.add(
            CidrRange.create(range.getAddressPrefix(), range.getPrefixLen().getValue()));
      }
      for (io.envoyproxy.envoy.config.core.v3.CidrRange range
          : proto.getSourcePrefixRangesList()) {
        sourcePrefixRanges.add(
            CidrRange.create(range.getAddressPrefix(), range.getPrefixLen().getValue()));
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

  static org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData.HttpConnectionManager  parseHttpConnectionManager(
      HttpConnectionManager proto, FilterRegistry filterRegistry,
      boolean isForClient) throws ResourceInvalidException {
    if (proto.getXffNumTrustedHops() != 0) {
      throw new ResourceInvalidException(
          "HttpConnectionManager with xff_num_trusted_hops unsupported");
    }
    if (!proto.getOriginalIpDetectionExtensionsList().isEmpty()) {
      throw new ResourceInvalidException("HttpConnectionManager with "
          + "original_ip_detection_extensions unsupported");
    }
    // Obtain max_stream_duration from Http Protocol Options.
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
      io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter
          httpFilter = proto.getHttpFiltersList().get(i);
      String filterName = httpFilter.getName();
      if (!names.add(filterName)) {
        throw new ResourceInvalidException(
            "HttpConnectionManager contains duplicate HttpFilter: " + filterName);
      }
      StructOrError<FilterConfig> filterConfig =
          parseHttpFilter(httpFilter, filterRegistry, isForClient);
      if ((i == proto.getHttpFiltersCount() - 1)
          && (filterConfig == null || !isTerminalFilter(filterConfig.getStruct()))) {
        throw new ResourceInvalidException("The last HttpFilter must be a terminal filter: "
            + filterName);
      }
      if (filterConfig == null) {
        continue;
      }
      if (filterConfig.getErrorDetail() != null) {
        throw new ResourceInvalidException(
            "HttpConnectionManager contains invalid HttpFilter: "
                + filterConfig.getErrorDetail());
      }
      if ((i < proto.getHttpFiltersCount() - 1) && isTerminalFilter(filterConfig.getStruct())) {
        throw new ResourceInvalidException("A terminal HttpFilter must be the last filter: "
            + filterName);
      }
      filterConfigs.add(new NamedFilterConfig(filterName, filterConfig.getStruct()));
    }

    // Parse inlined RouteConfiguration or RDS.
    if (proto.hasRouteConfig()) {
      List<VirtualHost> virtualHosts = extractVirtualHosts(
          proto.getRouteConfig(), filterRegistry);
      return org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData.HttpConnectionManager.forVirtualHosts(
          maxStreamDuration, virtualHosts, filterConfigs);
    }
    if (proto.hasRds()) {
      Rds rds = proto.getRds();
      if (!rds.hasConfigSource()) {
        throw new ResourceInvalidException(
            "HttpConnectionManager contains invalid RDS: missing config_source");
      }
      if (!rds.getConfigSource().hasAds() && !rds.getConfigSource().hasSelf()) {
        throw new ResourceInvalidException(
            "HttpConnectionManager contains invalid RDS: must specify ADS or self ConfigSource");
      }
      return org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData.HttpConnectionManager.forRdsName(
          maxStreamDuration, rds.getRouteConfigName(), filterConfigs);
    }
    throw new ResourceInvalidException(
        "HttpConnectionManager neither has inlined route_config nor RDS");
  }
    static List<VirtualHost> extractVirtualHosts(RouteConfiguration routeConfig, FilterRegistry filterRegistry)
            throws ResourceInvalidException {
      return null;
    }


  // hard-coded: currently router config is the only terminal filter.
  private static boolean isTerminalFilter(FilterConfig filterConfig) {
    return RouterFilter.ROUTER_CONFIG.equals(filterConfig);
  }

  @Nullable // Returns null if the filter is optional but not supported.
  static StructOrError<FilterConfig> parseHttpFilter(
      io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter
          httpFilter, FilterRegistry filterRegistry, boolean isForClient) {
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
      return StructOrError.fromError(
          "HttpFilter [" + filterName + "] contains invalid proto: " + e);
    }
    Filter filter = filterRegistry.get(typeUrl);
    if ((isForClient && !(filter instanceof ClientInterceptorBuilder))
        || (!isForClient && !(filter instanceof ServerInterceptorBuilder))) {
      if (isOptional) {
        return null;
      } else {
        return StructOrError.fromError(
            "HttpFilter [" + filterName + "](" + typeUrl + ") is required but unsupported for "
                + (isForClient ? "client" : "server"));
      }
    }
    ConfigOrError<? extends FilterConfig> filterConfig = filter.parseFilterConfig(rawConfig);
    if (filterConfig.errorDetail != null) {
      return StructOrError.fromError(
          "Invalid filter config for HttpFilter [" + filterName + "]: " + filterConfig.errorDetail);
    }
    return StructOrError.fromStruct(filterConfig.config);
  }

}
