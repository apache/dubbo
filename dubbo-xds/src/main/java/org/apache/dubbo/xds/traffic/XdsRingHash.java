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
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider.State;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.xds.registry.XdsEndpoint;
import org.apache.dubbo.xds.security.NativeTlsAdapter;
import org.apache.dubbo.xds.security.SecurityFields;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster.RingHashLbConfig;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.HealthStatus;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;

/** A42: one weighted ring across all localities in a selected priority. */
public final class XdsRingHash {
    private static final long MAX_SIZE = 8 * 1024 * 1024L;
    private static final long DEFAULT_CAP = 4096;
    private final Map<String, Long> weights;
    private final int minimum;
    private final int maximum;
    private final List<Point> points;

    private XdsRingHash(Map<String, Long> weights, int minimum, int maximum) {
        this.weights = weights;
        this.minimum = minimum;
        this.maximum = maximum;
        double total = total(weights);
        long smallest = Collections.min(weights.values());
        double normalized = smallest / total;
        double scale = Math.min(Math.ceil(normalized * minimum) / normalized, maximum);
        List<Point> ring = new ArrayList<>((int) Math.ceil(scale));
        double target = 0;
        for (Map.Entry<String, Long> entry : weights.entrySet()) {
            target += scale * (entry.getValue() / total);
            int ordinal = 0;
            while (ring.size() < target && ring.size() < maximum) {
                ring.add(new Point(XdsHash64.string(entry.getKey() + "_" + ordinal++), entry.getKey()));
            }
        }
        ring.sort((a, b) -> Long.compareUnsigned(a.hash, b.hash));
        points = Collections.unmodifiableList(ring);
    }

    public static void validate(Cluster cluster) {
        RingHashLbConfig config = cluster.getRingHashLbConfig();
        SecurityFields.only(config, "minimum_ring_size", "maximum_ring_size", "hash_function");
        if (config.getHashFunction() != RingHashLbConfig.HashFunction.XX_HASH) {
            throw new IllegalArgumentException("Native xDS ring hash requires XX_HASH");
        }
        long min = config.hasMinimumRingSize() ? config.getMinimumRingSize().getValue() : 1024;
        long max = config.hasMaximumRingSize() ? config.getMaximumRingSize().getValue() : MAX_SIZE;
        if (min < 1 || max < 1 || min > max || max > MAX_SIZE) {
            throw new IllegalArgumentException("Invalid xDS ring size: require 1 <= min <= max <= 8388608");
        }
    }

    public static void validateEndpoints(ClusterLoadAssignment assignment) {
        Map<Integer, Map<String, Long>> priorities = new TreeMap<>(Integer::compareUnsigned);
        for (LocalityLbEndpoints locality : assignment.getEndpointsList()) {
            long localityWeight = weight(locality);
            if (localityWeight == 0) continue;
            for (LbEndpoint endpoint : locality.getLbEndpointsList()) {
                long endpointWeight = weight(endpoint);
                if (endpointWeight == 0 || !healthy(endpoint)) continue;
                Map<String, Long> weights =
                        priorities.computeIfAbsent(locality.getPriority(), ignored -> new TreeMap<>());
                String host =
                        endpoint.getEndpoint().getAddress().getSocketAddress().getAddress();
                int port =
                        endpoint.getEndpoint().getAddress().getSocketAddress().getPortValue();
                if (weights.putIfAbsent(address(host, port), multiply(localityWeight, endpointWeight)) != null) {
                    throw new IllegalArgumentException(
                            "Ring hash has duplicate EDS socket addresses within a priority");
                }
            }
        }
        priorities.values().forEach(XdsRingHash::total);
    }

    static <T> XdsRingHash update(XdsRingHash previous, List<Invoker<T>> invokers, Cluster cluster, URL url) {
        long cap = url.getParameter("xds.ring-size-cap", DEFAULT_CAP);
        if (cap < 1 || cap > MAX_SIZE) throw new IllegalArgumentException("Invalid native xDS ring-size-cap");
        RingHashLbConfig config = cluster.getRingHashLbConfig();
        int minimum = (int) Math.min(
                cap, config.hasMinimumRingSize() ? config.getMinimumRingSize().getValue() : 1024);
        int maximum = (int) Math.min(
                cap, config.hasMaximumRingSize() ? config.getMaximumRingSize().getValue() : MAX_SIZE);
        Map<String, Long> weights = new TreeMap<>();
        for (Invoker<T> invoker : invokers) {
            XdsEndpoint endpoint = NativeTlsAdapter.endpoint(invoker.getUrl());
            String key = address(endpoint.getHost(), endpoint.getPort());
            long value = multiply(weight(endpoint.getLocality()), weight(endpoint.getEndpoint()));
            if (weights.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("Ring hash has duplicate logical socket addresses");
            }
        }
        if (previous != null
                && minimum == previous.minimum
                && maximum == previous.maximum
                && weights.equals(previous.weights)) {
            return previous;
        }
        return new XdsRingHash(weights, minimum, maximum);
    }

    <T> Invoker<T> select(List<Invoker<T>> invokers, long hash) {
        Map<String, Invoker<T>> live = new TreeMap<>();
        for (Invoker<T> invoker : invokers) {
            XdsEndpoint endpoint = NativeTlsAdapter.endpoint(invoker.getUrl());
            live.put(address(endpoint.getHost(), endpoint.getPort()), invoker);
        }
        int low = 0;
        int high = points.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (Long.compareUnsigned(points.get(middle).hash, hash) < 0) low = middle + 1;
            else high = middle;
        }
        for (int i = 0; i < points.size(); i++) {
            Invoker<T> invoker = live.get(points.get((low + i) % points.size()).address);
            State state = XdsPriorityState.state(invoker);
            if (state == State.CONNECTING || state == State.IDLE) return null;
            if (state == State.READY && invoker.isAvailable()) return invoker;
        }
        throw TriRpcStatus.UNAVAILABLE
                .withDescription("Native xDS ring has no usable endpoint")
                .asException();
    }

    int size() {
        return points.size();
    }

    private static String address(String host, int port) {
        return "/" + (host.indexOf(':') >= 0 && !host.startsWith("[") ? "[" + host + "]" : host) + ":" + port;
    }

    private static long total(Map<String, Long> weights) {
        long total = 0;
        for (long weight : weights.values()) {
            total = Math.addExact(total, weight);
            if (total > 0xffffffffL) throw new IllegalArgumentException("Ring hash weight sum exceeds uint32");
        }
        return total;
    }

    private static long multiply(long locality, long endpoint) {
        if (locality != 0 && endpoint > 0xffffffffL / locality) {
            throw new IllegalArgumentException("Ring hash combined locality/endpoint weight exceeds uint32");
        }
        return locality * endpoint;
    }

    private static boolean healthy(LbEndpoint endpoint) {
        return endpoint.getHealthStatus() == HealthStatus.UNKNOWN || endpoint.getHealthStatus() == HealthStatus.HEALTHY;
    }

    private static long weight(LocalityLbEndpoints locality) {
        return locality.hasLoadBalancingWeight()
                ? Integer.toUnsignedLong(locality.getLoadBalancingWeight().getValue())
                : 1;
    }

    private static long weight(LbEndpoint endpoint) {
        return endpoint.hasLoadBalancingWeight()
                ? Integer.toUnsignedLong(endpoint.getLoadBalancingWeight().getValue())
                : 1;
    }

    private static final class Point {
        final long hash;
        final String address;

        Point(long hash, String address) {
            this.hash = hash;
            this.address = address;
        }
    }
}
