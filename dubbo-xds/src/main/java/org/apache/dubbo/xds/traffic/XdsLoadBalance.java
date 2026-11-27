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
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.xds.registry.XdsEndpoint;
import org.apache.dubbo.xds.security.NativeTlsAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;

/** Locality selection is separate from endpoint policy and route cluster weights. */
public final class XdsLoadBalance implements LoadBalance, AutoCloseable {
    private final Map<String, RoundRobin> cursors = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> active = new ConcurrentHashMap<>();
    private final XdsPriorityState priorities = new XdsPriorityState();
    private final Map<String, XdsRingHash> rings = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public static void validate(Cluster cluster) {
        if ((cluster.getLbPolicy() != Cluster.LbPolicy.ROUND_ROBIN
                        && cluster.getLbPolicy() != Cluster.LbPolicy.LEAST_REQUEST
                        && cluster.getLbPolicy() != Cluster.LbPolicy.RING_HASH)
                || cluster.hasLoadBalancingPolicy()) {
            throw new UnsupportedPolicyException(cluster.getLbPolicy(), cluster.hasLoadBalancingPolicy());
        }
        if (cluster.getLbPolicy() == Cluster.LbPolicy.LEAST_REQUEST) choiceCount(cluster);
        if (cluster.getLbPolicy() == Cluster.LbPolicy.RING_HASH) XdsRingHash.validate(cluster);
    }

    /** Contains only a protocol enum and a typed-policy flag, so it is safe to report in NACK diagnostics. */
    public static final class UnsupportedPolicyException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        private UnsupportedPolicyException(Cluster.LbPolicy policy, boolean typed) {
            super("Native xDS LB policy is not enabled: " + policy + (typed ? " (typed policy)" : ""));
        }
    }

    static int choiceCount(Cluster cluster) {
        long choices = cluster.getLeastRequestLbConfig().hasChoiceCount()
                ? Integer.toUnsignedLong(
                        cluster.getLeastRequestLbConfig().getChoiceCount().getValue())
                : 2;
        if (choices < 2) throw new IllegalArgumentException("Native xDS least-request choice_count must be at least 2");
        return (int) Math.min(choices, 10);
    }

    // Security/traffic updates may rebuild Invokers while the same logical endpoint is still busy.
    private static String key(XdsEndpoint endpoint) {
        return endpoint.getTarget() + "/" + endpoint.getCluster().getName() + "/"
                + endpoint.getLocality().getPriority() + "/"
                + endpoint.getLocality().getLocality() + "/"
                + endpoint.getHost() + ":" + endpoint.getPort();
    }

    public long active(XdsEndpoint endpoint) {
        AtomicLong count = active.get(key(endpoint));
        return count == null ? 0 : count.get();
    }

    /** Track every dispatched attempt, including RR calls during an online policy switch. */
    public Permit acquire(Invoker<?> invoker) {
        if (closed)
            throw TriRpcStatus.CANCELLED
                    .withDescription("Native xDS LB has been closed")
                    .asException();
        String key = key(NativeTlsAdapter.endpoint(invoker.getUrl()));
        AtomicLong count = active.compute(key, (unused, current) -> {
            if (current == null) current = new AtomicLong();
            current.incrementAndGet();
            return current;
        });
        Permit permit = new Permit(key, count);
        if (closed) {
            permit.close();
            throw TriRpcStatus.CANCELLED
                    .withDescription("Native xDS LB has been closed")
                    .asException();
        }
        return permit;
    }

    public final class Permit implements AutoCloseable {
        private final String key;
        private final AtomicLong count;
        private final AtomicBoolean released = new AtomicBoolean();

        private Permit(String key, AtomicLong count) {
            this.key = key;
            this.count = count;
        }

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                active.compute(
                        key, (unused, current) -> count.decrementAndGet() == 0 && current == count ? null : current);
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        cursors.clear();
        rings.clear();
        priorities.close();
        // Outstanding permits retain their counters until actual completion/cancellation.
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        Invoker<T> selected = selectReady(invokers, url, invocation);
        if (selected == null)
            throw TriRpcStatus.UNAVAILABLE
                    .withDescription("Native xDS priority is connecting")
                    .asException();
        return selected;
    }

    /** Returns null when priority policy must buffer; caller owns budget and cancellation. */
    public <T> Invoker<T> selectReady(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        if (invokers.isEmpty()) {
            throw TriRpcStatus.UNAVAILABLE
                    .withDescription("Native xDS cluster has no endpoints")
                    .asException();
        }
        XdsRouteSelection selected = (XdsRouteSelection) invocation.get(XdsRouteSelection.ATTRIBUTE);
        if (selected == null) {
            throw new RpcException("Native xDS endpoint selection has no route context");
        }
        Cluster cluster = selected.getSnapshot().getClusters().get(selected.getCluster());
        validate(cluster);
        List<Invoker<T>> eligible = new ArrayList<>();
        for (Invoker<T> invoker : invokers) {
            if (eligible(invoker, cluster)) eligible.add(invoker);
        }
        String scope = selected.getSnapshot().getTarget() + "/" + selected.getCluster();
        boolean ring = cluster.getLbPolicy() == Cluster.LbPolicy.RING_HASH;
        List<Invoker<T>> ready = priorities.ready(scope, eligible, ring);
        if (ready == null) return null;
        if (ring && !ready.isEmpty()) {
            XdsRingHash hash =
                    rings.compute(scope, (key, previous) -> XdsRingHash.update(previous, ready, cluster, url));
            return hash.select(ready, selected.getRequestHash());
        }
        Map<String, List<Invoker<T>>> localities = new LinkedHashMap<>();
        Map<String, Long> weights = new LinkedHashMap<>();
        for (Invoker<T> invoker : ready) {
            XdsEndpoint endpoint = NativeTlsAdapter.endpoint(invoker.getUrl());
            String key = endpoint.getTarget() + "/" + endpoint.getCluster().getName() + "/"
                    + endpoint.getLocality().getLocality();
            long weight = endpoint.getLocality().hasLoadBalancingWeight()
                    ? Integer.toUnsignedLong(
                            endpoint.getLocality().getLoadBalancingWeight().getValue())
                    : 1;
            if (weight == 0) {
                continue;
            }
            localities.computeIfAbsent(key, unused -> new ArrayList<>()).add(invoker);
            weights.put(key, weight);
        }
        long total = 0;
        for (long weight : weights.values()) {
            total = Math.addExact(total, weight);
        }
        if (total == 0) {
            throw TriRpcStatus.UNAVAILABLE
                    .withDescription("Native xDS cluster has no ready locality")
                    .asException();
        }
        long pick = ThreadLocalRandom.current().nextLong(total);
        for (Map.Entry<String, Long> weight : weights.entrySet()) {
            pick -= weight.getValue();
            if (pick < 0) {
                if (cluster.getLbPolicy() == Cluster.LbPolicy.LEAST_REQUEST) {
                    return leastRequest(localities.get(weight.getKey()), choiceCount(cluster));
                }
                return cursors.computeIfAbsent(weight.getKey(), key -> new RoundRobin())
                        .select(localities.get(weight.getKey()));
            }
        }
        throw new RpcException("Native xDS locality selection failed");
    }

    private static boolean eligible(Invoker<?> invoker, Cluster cluster) {
        XdsEndpoint endpoint = NativeTlsAdapter.endpoint(invoker.getUrl());
        if (endpoint.getLocality().hasLoadBalancingWeight()
                && endpoint.getLocality().getLoadBalancingWeight().getValue() == 0) {
            return false;
        }
        switch (endpoint.getEndpoint().getHealthStatus()) {
            case UNKNOWN:
            case HEALTHY:
                break;
            default:
                return false;
        }
        // A48 ignores endpoint weights; weighted RR must exclude zero before priority selection.
        return cluster.getLbPolicy() == Cluster.LbPolicy.LEAST_REQUEST
                || !endpoint.getEndpoint().hasLoadBalancingWeight()
                || endpoint.getEndpoint().getLoadBalancingWeight().getValue() != 0;
    }

    private <T> Invoker<T> leastRequest(List<Invoker<T>> invokers, int choices) {
        // A48 ignores endpoint weights, including duplicate logical addresses.
        Map<String, Invoker<T>> unique = new LinkedHashMap<>();
        for (Invoker<T> invoker : invokers)
            unique.putIfAbsent(key(NativeTlsAdapter.endpoint(invoker.getUrl())), invoker);
        List<Invoker<T>> candidates = new ArrayList<>(unique.values());
        Invoker<T> selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        long minimum = active(NativeTlsAdapter.endpoint(selected.getUrl()));
        for (int i = 1; i < choices; i++) {
            Invoker<T> sampled = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            long count = active(NativeTlsAdapter.endpoint(sampled.getUrl()));
            if (count < minimum) {
                selected = sampled;
                minimum = count;
            }
        }
        return selected;
    }

    private static final class RoundRobin {
        private final Map<String, Long> currents = new LinkedHashMap<>();

        synchronized <T> Invoker<T> select(List<Invoker<T>> invokers) {
            Set<String> live = new HashSet<>();
            long total = 0;
            long best = Long.MIN_VALUE;
            Invoker<T> selected = null;
            String selectedKey = null;
            for (Invoker<T> invoker : invokers) {
                XdsEndpoint endpoint = NativeTlsAdapter.endpoint(invoker.getUrl());
                String key = endpoint.identity();
                live.add(key);
                long weight = endpoint.getEndpoint().hasLoadBalancingWeight()
                        ? Integer.toUnsignedLong(
                                endpoint.getEndpoint().getLoadBalancingWeight().getValue())
                        : 1;
                if (weight == 0) {
                    continue;
                }
                total = Math.addExact(total, weight);
                long current = currents.getOrDefault(key, 0L) + weight;
                currents.put(key, current);
                if (current > best) {
                    best = current;
                    selected = invoker;
                    selectedKey = key;
                }
            }
            currents.keySet().retainAll(live);
            if (selected == null) {
                throw TriRpcStatus.UNAVAILABLE
                        .withDescription("Native xDS locality has no weighted endpoints")
                        .asException();
            }
            currents.put(selectedKey, currents.get(selectedKey) - total);
            return selected;
        }
    }
}
