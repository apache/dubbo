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

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.common.url.component.URLAddress;
import org.apache.dubbo.xds.resource.grpc.resource.endpoint.DropOverload;
import org.apache.dubbo.xds.resource.grpc.resource.endpoint.LbEndpoint;
import org.apache.dubbo.xds.resource.grpc.resource.endpoint.Locality;
import org.apache.dubbo.xds.resource.grpc.resource.endpoint.LocalityLbEndpoints;
import org.apache.dubbo.xds.resource.grpc.resource.exception.ResourceInvalidException;
import org.apache.dubbo.xds.resource.grpc.resource.update.EdsUpdate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.Message;
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.type.v3.FractionalPercent;

class XdsEndpointResource extends XdsResourceType<EdsUpdate> {
    static final String ADS_TYPE_URL_EDS = "type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment";

    private static final XdsEndpointResource instance = new XdsEndpointResource();

    public static XdsEndpointResource getInstance() {
        return instance;
    }

    @Override
    @Nullable
    String extractResourceName(Message unpackedResource) {
        if (!(unpackedResource instanceof ClusterLoadAssignment)) {
            return null;
        }
        return ((ClusterLoadAssignment) unpackedResource).getClusterName();
    }

    @Override
    String typeName() {
        return "EDS";
    }

    @Override
    String typeUrl() {
        return ADS_TYPE_URL_EDS;
    }

    @Override
    boolean isFullStateOfTheWorld() {
        return false;
    }

    @Override
    Class<ClusterLoadAssignment> unpackedClassName() {
        return ClusterLoadAssignment.class;
    }

    @Override
    EdsUpdate doParse(Args args, Message unpackedMessage) throws ResourceInvalidException {
        if (!(unpackedMessage instanceof ClusterLoadAssignment)) {
            throw new ResourceInvalidException("Invalid message type: " + unpackedMessage.getClass());
        }
        return processClusterLoadAssignment((ClusterLoadAssignment) unpackedMessage);
    }

    private static EdsUpdate processClusterLoadAssignment(ClusterLoadAssignment assignment) throws ResourceInvalidException {
        Map<Integer, Set<Locality>> priorities = new HashMap<>();
        Map<Locality, LocalityLbEndpoints> localityLbEndpointsMap = new LinkedHashMap<>();
        List<DropOverload> dropOverloads = new ArrayList<>();
        int maxPriority = -1;
        for (io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints localityLbEndpointsProto :
                assignment.getEndpointsList()) {
            StructOrError<LocalityLbEndpoints> structOrError = parseLocalityLbEndpoints(localityLbEndpointsProto);
            if (structOrError == null) {
                continue;
            }
            if (structOrError.getErrorDetail() != null) {
                throw new ResourceInvalidException(structOrError.getErrorDetail());
            }

            LocalityLbEndpoints localityLbEndpoints = structOrError.getStruct();
            int priority = localityLbEndpoints.priority();
            maxPriority = Math.max(maxPriority, priority);
            // Note endpoints with health status other than HEALTHY and UNKNOWN are still
            // handed over to watching parties. It is watching parties' responsibility to
            // filter out unhealthy endpoints. See EnvoyProtoData.LbEndpoint#isHealthy().
            Locality locality = parseLocality(localityLbEndpointsProto.getLocality());
            localityLbEndpointsMap.put(locality, localityLbEndpoints);
            if (!priorities.containsKey(priority)) {
                priorities.put(priority, new HashSet<>());
            }
            if (!priorities.get(priority)
                    .add(locality)) {
                throw new ResourceInvalidException(
                        "ClusterLoadAssignment has duplicate locality:" + locality + " for priority:" + priority);
            }
        }
        if (priorities.size() != maxPriority + 1) {
            throw new ResourceInvalidException("ClusterLoadAssignment has sparse priorities");
        }

        for (ClusterLoadAssignment.Policy.DropOverload dropOverloadProto : assignment.getPolicy()
                .getDropOverloadsList()) {
            dropOverloads.add(parseDropOverload(dropOverloadProto));
        }
        return new EdsUpdate(assignment.getClusterName(), localityLbEndpointsMap, dropOverloads);
    }

    private static Locality parseLocality(io.envoyproxy.envoy.config.core.v3.Locality proto) {
        return new Locality(proto.getRegion(), proto.getZone(), proto.getSubZone());
    }

    private static DropOverload parseDropOverload(
            ClusterLoadAssignment.Policy.DropOverload proto) {
        return new DropOverload(proto.getCategory(), getRatePerMillion(proto.getDropPercentage()));
    }

    private static int getRatePerMillion(FractionalPercent percent) {
        int numerator = percent.getNumerator();
        FractionalPercent.DenominatorType type = percent.getDenominator();
        switch (type) {
            case TEN_THOUSAND:
                numerator *= 100;
                break;
            case HUNDRED:
                numerator *= 10_000;
                break;
            case MILLION:
                break;
            case UNRECOGNIZED:
            default:
                throw new IllegalArgumentException("Unknown denominator type of " + percent);
        }

        if (numerator > 1_000_000 || numerator < 0) {
            numerator = 1_000_000;
        }
        return numerator;
    }

    @Nullable
    static StructOrError<LocalityLbEndpoints> parseLocalityLbEndpoints(
            io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints proto) {
        // Filter out localities without or with 0 weight.
        if (!proto.hasLoadBalancingWeight() || proto.getLoadBalancingWeight()
                .getValue() < 1) {
            return null;
        }
        if (proto.getPriority() < 0) {
            return StructOrError.fromError("negative priority");
        }
        List<LbEndpoint> endpoints = new ArrayList<>(proto.getLbEndpointsCount());
        for (io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint endpoint : proto.getLbEndpointsList()) {
            // The endpoint field of each lb_endpoints must be set.
            // Inside of it: the address field must be set.
            if (!endpoint.hasEndpoint() || !endpoint.getEndpoint()
                    .hasAddress()) {
                return StructOrError.fromError("LbEndpoint with no endpoint/address");
            }
            io.envoyproxy.envoy.config.core.v3.SocketAddress socketAddress = endpoint.getEndpoint()
                    .getAddress()
                    .getSocketAddress();
            URLAddress addr = new URLAddress(socketAddress.getAddress(), socketAddress.getPortValue());
            boolean isHealthy = endpoint.getHealthStatus() == io.envoyproxy.envoy.config.core.v3.HealthStatus.HEALTHY
                    || endpoint.getHealthStatus() == io.envoyproxy.envoy.config.core.v3.HealthStatus.UNKNOWN;
            endpoints.add(new LbEndpoint(Collections.singletonList(addr), endpoint.getLoadBalancingWeight()
                    .getValue(), isHealthy));
        }
        return StructOrError.fromStruct(new LocalityLbEndpoints(endpoints, proto.getLoadBalancingWeight()
                .getValue(), proto.getPriority()));
    }
}
