///*
// * Copyright 2022 The gRPC Authors
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.apache.dubbo.xds.resource.grpc;
//
//import org.apache.dubbo.xds.resource.grpc.XdsClient.ResourceUpdate;
//import org.apache.dubbo.xds.resource.grpc.XdsClientImpl.ResourceInvalidException;
//import org.apache.dubbo.xds.resource.grpc.XdsEndpointResource.EdsUpdate;
//
//import com.google.common.annotations.VisibleForTesting;
//import com.google.common.base.MoreObjects;
//import com.google.common.collect.ImmutableList;
//import com.google.protobuf.Message;
//import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
//import io.envoyproxy.envoy.type.v3.FractionalPercent;
//import io.grpc.EquivalentAddressGroup;
//
//import javax.annotation.Nullable;
//
//import java.net.InetSocketAddress;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Set;
//
//import static com.google.common.base.Preconditions.checkNotNull;
//
//class XdsEndpointResource extends XdsResourceType<EdsUpdate> {
//  static final String ADS_TYPE_URL_EDS =
//      "type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment";
//
//  private static final XdsEndpointResource instance = new XdsEndpointResource();
//
//  public static XdsEndpointResource getInstance() {
//    return instance;
//  }
//
//  @Override
//  @Nullable
//  String extractResourceName(Message unpackedResource) {
//    if (!(unpackedResource instanceof ClusterLoadAssignment)) {
//      return null;
//    }
//    return ((ClusterLoadAssignment) unpackedResource).getClusterName();
//  }
//
//  @Override
//  String typeName() {
//    return "EDS";
//  }
//
//  @Override
//  String typeUrl() {
//    return ADS_TYPE_URL_EDS;
//  }
//
//  @Override
//  boolean isFullStateOfTheWorld() {
//    return false;
//  }
//
//  @Override
//  Class<ClusterLoadAssignment> unpackedClassName() {
//    return ClusterLoadAssignment.class;
//  }
//
//  @Override
//  EdsUpdate doParse(Args args, Message unpackedMessage)
//      throws ResourceInvalidException {
//    if (!(unpackedMessage instanceof ClusterLoadAssignment)) {
//      throw new ResourceInvalidException("Invalid message type: " + unpackedMessage.getClass());
//    }
//    return processClusterLoadAssignment((ClusterLoadAssignment) unpackedMessage);
//  }
//
//  private static EdsUpdate processClusterLoadAssignment(ClusterLoadAssignment assignment)
//      throws ResourceInvalidException {
//    Map<Integer, Set<Locality>> priorities = new HashMap<>();
//    Map<Locality, LocalityLbEndpoints> localityLbEndpointsMap = new LinkedHashMap<>();
//    List<Endpoints.DropOverload> dropOverloads = new ArrayList<>();
//    int maxPriority = -1;
//    for (io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints localityLbEndpointsProto
//        : assignment.getEndpointsList()) {
//      StructOrError<LocalityLbEndpoints> structOrError =
//          parseLocalityLbEndpoints(localityLbEndpointsProto);
//      if (structOrError == null) {
//        continue;
//      }
//      if (structOrError.getErrorDetail() != null) {
//        throw new ResourceInvalidException(structOrError.getErrorDetail());
//      }
//
//      LocalityLbEndpoints localityLbEndpoints = structOrError.getStruct();
//      int priority = localityLbEndpoints.priority();
//      maxPriority = Math.max(maxPriority, priority);
//      // Note endpoints with health status other than HEALTHY and UNKNOWN are still
//      // handed over to watching parties. It is watching parties' responsibility to
//      // filter out unhealthy endpoints. See EnvoyProtoData.LbEndpoint#isHealthy().
//      Locality locality =  parseLocality(localityLbEndpointsProto.getLocality());
//      localityLbEndpointsMap.put(locality, localityLbEndpoints);
//      if (!priorities.containsKey(priority)) {
//        priorities.put(priority, new HashSet<>());
//      }
//      if (!priorities.get(priority).add(locality)) {
//        throw new ResourceInvalidException("ClusterLoadAssignment has duplicate locality:"
//            + locality + " for priority:" + priority);
//      }
//    }
//    if (priorities.size() != maxPriority + 1) {
//      throw new ResourceInvalidException("ClusterLoadAssignment has sparse priorities");
//    }
//
//    for (ClusterLoadAssignment.Policy.DropOverload dropOverloadProto
//        : assignment.getPolicy().getDropOverloadsList()) {
//      dropOverloads.add(parseDropOverload(dropOverloadProto));
//    }
//    return new EdsUpdate(assignment.getClusterName(), localityLbEndpointsMap, dropOverloads);
//  }
//
//  private static Locality parseLocality(io.envoyproxy.envoy.config.core.v3.Locality proto) {
//    return Locality.create(proto.getRegion(), proto.getZone(), proto.getSubZone());
//  }
//
//  private static DropOverload parseDropOverload(
//      ClusterLoadAssignment.Policy.DropOverload proto) {
//    return DropOverload.create(proto.getCategory(), getRatePerMillion(proto.getDropPercentage()));
//  }
//
//  private static int getRatePerMillion(FractionalPercent percent) {
//    int numerator = percent.getNumerator();
//    FractionalPercent.DenominatorType type = percent.getDenominator();
//    switch (type) {
//      case TEN_THOUSAND:
//        numerator *= 100;
//        break;
//      case HUNDRED:
//        numerator *= 10_000;
//        break;
//      case MILLION:
//        break;
//      case UNRECOGNIZED:
//      default:
//        throw new IllegalArgumentException("Unknown denominator type of " + percent);
//    }
//
//    if (numerator > 1_000_000 || numerator < 0) {
//      numerator = 1_000_000;
//    }
//    return numerator;
//  }
//
//
//  @VisibleForTesting
//  @Nullable
//  static StructOrError<LocalityLbEndpoints> parseLocalityLbEndpoints(
//      io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints proto) {
//    // Filter out localities without or with 0 weight.
//    if (!proto.hasLoadBalancingWeight() || proto.getLoadBalancingWeight().getValue() < 1) {
//      return null;
//    }
//    if (proto.getPriority() < 0) {
//      return StructOrError.fromError("negative priority");
//    }
//    List<Endpoints.LbEndpoint> endpoints = new ArrayList<>(proto.getLbEndpointsCount());
//    for (io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint endpoint : proto.getLbEndpointsList()) {
//      // The endpoint field of each lb_endpoints must be set.
//      // Inside of it: the address field must be set.
//      if (!endpoint.hasEndpoint() || !endpoint.getEndpoint().hasAddress()) {
//        return StructOrError.fromError("LbEndpoint with no endpoint/address");
//      }
//      io.envoyproxy.envoy.config.core.v3.SocketAddress socketAddress =
//          endpoint.getEndpoint().getAddress().getSocketAddress();
//      InetSocketAddress addr =
//          new InetSocketAddress(socketAddress.getAddress(), socketAddress.getPortValue());
//      boolean isHealthy =
//          endpoint.getHealthStatus() == io.envoyproxy.envoy.config.core.v3.HealthStatus.HEALTHY
//              || endpoint.getHealthStatus()
//              == io.envoyproxy.envoy.config.core.v3.HealthStatus.UNKNOWN;
//      endpoints.add(Endpoints.LbEndpoint.create(
//          new EquivalentAddressGroup(ImmutableList.<java.net.SocketAddress>of(addr)),
//          endpoint.getLoadBalancingWeight().getValue(), isHealthy));
//    }
//    return StructOrError.fromStruct(Endpoints.LocalityLbEndpoints.create(
//        endpoints, proto.getLoadBalancingWeight().getValue(), proto.getPriority()));
//  }
//
//  static final class EdsUpdate implements ResourceUpdate {
//    final String clusterName;
//    final Map<Locality, LocalityLbEndpoints> localityLbEndpointsMap;
//    final List<DropOverload> dropPolicies;
//
//    EdsUpdate(String clusterName, Map<Locality, LocalityLbEndpoints> localityLbEndpoints,
//              List<DropOverload> dropPolicies) {
//      this.clusterName = checkNotNull(clusterName, "clusterName");
//      this.localityLbEndpointsMap = Collections.unmodifiableMap(
//          new LinkedHashMap<>(checkNotNull(localityLbEndpoints, "localityLbEndpoints")));
//      this.dropPolicies = Collections.unmodifiableList(
//          new ArrayList<>(checkNotNull(dropPolicies, "dropPolicies")));
//    }
//
//    @Override
//    public boolean equals(Object o) {
//      if (this == o) {
//        return true;
//      }
//      if (o == null || getClass() != o.getClass()) {
//        return false;
//      }
//      EdsUpdate that = (EdsUpdate) o;
//      return Objects.equals(clusterName, that.clusterName)
//          && Objects.equals(localityLbEndpointsMap, that.localityLbEndpointsMap)
//          && Objects.equals(dropPolicies, that.dropPolicies);
//    }
//
//    @Override
//    public int hashCode() {
//      return Objects.hash(clusterName, localityLbEndpointsMap, dropPolicies);
//    }
//
//    @Override
//    public String toString() {
//      return
//          MoreObjects
//              .toStringHelper(this)
//              .add("clusterName", clusterName)
//              .add("localityLbEndpointsMap", localityLbEndpointsMap)
//              .add("dropPolicies", dropPolicies)
//              .toString();
//    }
//  }
//}
