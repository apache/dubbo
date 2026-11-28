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
package org.apache.dubbo.xds.traffic;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;
import org.apache.dubbo.rpc.protocol.tri.TripleInvoker;
import org.apache.dubbo.rpc.protocol.tri.UnaryRetryState;
import org.apache.dubbo.xds.client.XdsResourceGraph;
import org.apache.dubbo.xds.client.XdsResourceGraph.ClusterLease;
import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;
import org.apache.dubbo.xds.registry.DiscoveryTarget;
import org.apache.dubbo.xds.registry.XdsEndpoint;
import org.apache.dubbo.xds.security.NativeTlsAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.netty.handler.codec.http2.DefaultHttp2Headers;

/** Service selection -> virtual host -> route -> weighted cluster; endpoint choice stays in LoadBalance. */
public final class XdsRouter extends AbstractRouter {
    private final long referenceId = ThreadLocalRandom.current().nextLong();

    public XdsRouter(URL url) {
        super(url);
        setForce(true);
        setPriority(-10000);
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        Map<DiscoveryTarget, List<Invoker<T>>> targets = new LinkedHashMap<>();
        for (Invoker<T> invoker : invokers) {
            XdsEndpoint endpoint = NativeTlsAdapter.endpoint(invoker.getUrl());
            if (endpoint == null || !"tri".equals(endpoint.getTarget().getProtocol())) {
                throw new RpcException("Native xDS routing received a non-Triple endpoint");
            }
            targets.computeIfAbsent(endpoint.getTarget(), key -> new ArrayList<>())
                    .add(invoker);
        }
        if (targets.isEmpty()) {
            return Collections.emptyList();
        }
        if (invocation.get(UnaryRetryState.ATTRIBUTE) != null
                || invocation.get(XdsFaultExecutor.RESUME_ATTRIBUTE) != null) {
            XdsRouteSelection original = (XdsRouteSelection) invocation.get(XdsRouteSelection.ATTRIBUTE);
            List<Invoker<T>> sameTarget = targets.get(original.getSnapshot().getTarget());
            if (sameTarget == null) {
                return Collections.emptyList();
            }
            Snapshot current = original.getCurrentSnapshot();
            if (current == null) {
                return Collections.emptyList();
            }
            XdsRouteSelection retained = original.updated(current);
            invocation.put(XdsRouteSelection.ATTRIBUTE, retained);
            return candidates(sameTarget, retained);
        }
        // Multiple explicit Services are independent targets, chosen before their individual xDS policies.
        List<List<Invoker<T>>> services = new ArrayList<>(targets.values());
        List<Invoker<T>> service = services.get(ThreadLocalRandom.current().nextInt(services.size()));
        URL address = service.get(0).getUrl();
        Snapshot snapshot = NativeTlsAdapter.endpoint(address).getSnapshot();
        if (snapshot == null
                || snapshot.getListener() == null
                || snapshot.getRoutes().isEmpty()) {
            throw new RpcException("Native xDS outbound listener or routes are unavailable");
        }
        DefaultHttp2Headers wire = TripleInvoker.createRoutingHeaders(address, invocation);
        Map<String, String> headers = new LinkedHashMap<>();
        Map<String, String> faultHeaders = new LinkedHashMap<>();
        wire.forEach(entry -> {
            String key = entry.getKey().toString().toLowerCase(Locale.ROOT);
            String value = entry.getValue().toString();
            headers.merge(key, value, (first, next) -> first + "," + next);
            faultHeaders.putIfAbsent(key, value);
        });
        XdsRouteTable.Match[] route = new XdsRouteTable.Match[1];
        // Selection and ownership are atomic with respect to RDS updates and resource release.
        ClusterLease lease = snapshot.retainCluster(current -> {
            route[0] = XdsRouteTable.select(current, headers);
            if (route[0] == null) {
                throw new RpcException("Native xDS has no matching route for the request");
            }
            return route[0].selectCluster();
        });
        XdsRouteSelection selected = new XdsRouteSelection(
                lease.getSelectedSnapshot(),
                route[0],
                lease.getCluster(),
                headers,
                lease,
                faultHeaders,
                route[0].requestHash(
                        headers,
                        invocation.get(XdsHashPolicy.REFERENCE_ID_ATTRIBUTE) == null
                                ? referenceId
                                : (Long) invocation.get(XdsHashPolicy.REFERENCE_ID_ATTRIBUTE)));
        invocation.put(XdsRouteSelection.ATTRIBUTE, selected);
        List<Invoker<T>> candidates = candidates(service, selected);
        return candidates;
    }

    static <T> List<Invoker<T>> candidates(List<Invoker<T>> invokers, XdsRouteSelection selected) {
        Snapshot snapshot = selected.getSnapshot();
        Cluster cluster = snapshot.getClusters().get(selected.getCluster());
        ClusterLoadAssignment assignment =
                cluster == null ? null : snapshot.getEndpoints().get(XdsResourceGraph.edsName(cluster));
        if (assignment == null) {
            return Collections.emptyList();
        }
        List<Invoker<T>> result = new ArrayList<>();
        for (Invoker<T> invoker : invokers) {
            XdsEndpoint endpoint = NativeTlsAdapter.endpoint(invoker.getUrl());
            if (endpoint == null
                    || !snapshot.getTarget().equals(endpoint.getTarget())
                    || !cluster.equals(endpoint.getCluster())) {
                continue;
            }
            // Directory notification can lag ADS; never use an address removed from this call's selected snapshot.
            for (LocalityLbEndpoints locality : assignment.getEndpointsList()) {
                if (locality.getPriority() == endpoint.getLocality().getPriority()
                        && locality.getLocality().equals(endpoint.getLocality().getLocality())
                        && locality.getLoadBalancingWeight()
                                .equals(endpoint.getLocality().getLoadBalancingWeight())
                        && locality.getLbEndpointsList().contains(endpoint.getEndpoint())) {
                    result.add(invoker);
                    break;
                }
            }
        }
        return result;
    }
}
