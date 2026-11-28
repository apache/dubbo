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

import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;
import org.apache.dubbo.xds.security.SecurityFields;
import org.apache.dubbo.xds.security.XdsHeaderMatcher;
import org.apache.dubbo.xds.security.XdsMatchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.HeaderMatcher;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.WeightedCluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.StringMatcher;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.v3.FractionalPercent;

/** Immutable route matching and cluster selection, independently of endpoint load balancing. */
public final class XdsRouteTable {
    private final List<Host> hosts = new ArrayList<>();
    private final boolean ignorePort;

    public XdsRouteTable(RouteConfiguration config) {
        SecurityFields.only(config, "name", "virtual_hosts", "ignore_port_in_host_matching", "validate_clusters");
        ignorePort = config.getIgnorePortInHostMatching();
        Set<String> domains = new HashSet<>();
        for (VirtualHost host : config.getVirtualHostsList()) {
            // Envoy's attempt-count header is not part of the gRPC xDS retry contract.
            SecurityFields.only(
                    host,
                    "name",
                    "domains",
                    "routes",
                    "retry_policy",
                    "include_request_attempt_count",
                    "typed_per_filter_config");
            if (host.getDomainsCount() == 0) {
                throw new IllegalArgumentException("Virtual host has no domains");
            }
            for (String domain : host.getDomainsList()) {
                String normalized = domain.toLowerCase(Locale.ROOT);
                if (!validDomain(normalized) || !domains.add(normalized)) {
                    throw new IllegalArgumentException("Invalid or duplicate virtual-host domain");
                }
            }
            hosts.add(new Host(host));
        }
    }

    public Match match(Map<String, String> headers) {
        String authority = headers.getOrDefault(":authority", "").toLowerCase(Locale.ROOT);
        if (ignorePort) {
            int port = authority.lastIndexOf(':');
            if (port > authority.lastIndexOf(']')) {
                authority = authority.substring(0, port);
            }
        }
        Host selected = null;
        long best = -1;
        for (Host host : hosts) {
            for (String domain : host.domains) {
                long rank = domainRank(domain, authority);
                if (rank > best) {
                    best = rank;
                    selected = host;
                }
            }
        }
        if (selected == null) {
            return null;
        }
        for (Match route : selected.routes) {
            if (route.matches(headers)) {
                return route;
            }
        }
        return null;
    }

    public static Match select(Snapshot snapshot, Map<String, String> headers) {
        for (XdsRouteTable table : snapshot.getRouteTables().values()) {
            Match match = table.match(headers);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private static boolean validDomain(String domain) {
        int star = domain.indexOf('*');
        return !domain.isEmpty()
                && (star < 0 || (domain.indexOf('*', star + 1) < 0 && (star == 0 || star == domain.length() - 1)));
    }

    private static long domainRank(String domain, String authority) {
        if (domain.equals(authority)) {
            return (3L << 32) + domain.length();
        }
        if ("*".equals(domain)) {
            return 0;
        }
        if (domain.startsWith("*")
                && authority.length() >= domain.length()
                && authority.endsWith(domain.substring(1))) {
            return (2L << 32) + domain.length();
        }
        if (domain.endsWith("*")
                && authority.length() >= domain.length()
                && authority.startsWith(domain.substring(0, domain.length() - 1))) {
            return (1L << 32) + domain.length();
        }
        return -1;
    }

    private static final class Host {
        final List<String> domains = new ArrayList<>();
        final List<Match> routes = new ArrayList<>();

        Host(VirtualHost host) {
            host.getDomainsList().forEach(domain -> domains.add(domain.toLowerCase(Locale.ROOT)));
            XdsRetryPolicy inherited = host.hasRetryPolicy() ? XdsRetryPolicy.parse(host.getRetryPolicy()) : null;
            Map<String, XdsFaultPolicy> overrides = XdsFaultFilters.overrides(host.getTypedPerFilterConfigMap());
            host.getRoutesList().forEach(route -> routes.add(new Match(route, inherited, overrides)));
        }
    }

    public static final class Match {
        private final Route route;
        private final Predicate<String> path;
        private final List<Predicate<Map<String, String>>> headers = new ArrayList<>();
        private final Map<String, Long> clusters = new LinkedHashMap<>();
        private final int denominator;
        private final long numerator;
        private final Long timeoutNanos;
        private final XdsRetryPolicy retryPolicy;
        private final XdsHashPolicy hashPolicy;
        private final Map<String, XdsFaultPolicy> hostFaults;
        private final Map<String, XdsFaultPolicy> routeFaults;
        private final Map<String, Map<String, XdsFaultPolicy>> clusterFaults = new LinkedHashMap<>();

        Match(Route route, XdsRetryPolicy inherited, Map<String, XdsFaultPolicy> hostFaults) {
            // Istio attaches provenance metadata; it does not affect request selection.
            SecurityFields.only(route, "name", "match", "route", "decorator", "metadata", "typed_per_filter_config");
            this.route = route;
            this.hostFaults = hostFaults;
            this.routeFaults = XdsFaultFilters.overrides(route.getTypedPerFilterConfigMap());
            RouteMatch match = route.getMatch();
            SecurityFields.only(match, "prefix", "path", "safe_regex", "case_sensitive", "headers", "runtime_fraction");
            boolean ignoreCase =
                    match.hasCaseSensitive() && !match.getCaseSensitive().getValue();
            StringMatcher.Builder matcher = StringMatcher.newBuilder().setIgnoreCase(ignoreCase);
            switch (match.getPathSpecifierCase()) {
                case PREFIX:
                    matcher.setPrefix(match.getPrefix());
                    break;
                case PATH:
                    matcher.setExact(match.getPath());
                    break;
                case SAFE_REGEX:
                    matcher.setSafeRegex(match.getSafeRegex()).setIgnoreCase(false);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported route path matcher");
            }
            path = XdsMatchers.string(matcher.build());
            for (HeaderMatcher header : match.getHeadersList()) {
                headers.add(XdsHeaderMatcher.compile(header));
            }
            int fractionDenominator = 100;
            long fractionNumerator = 100;
            if (match.hasRuntimeFraction()) {
                SecurityFields.only(match.getRuntimeFraction(), "default_value", "runtime_key");
                FractionalPercent fraction = match.getRuntimeFraction().getDefaultValue();
                SecurityFields.only(fraction, "numerator", "denominator");
                switch (fraction.getDenominator()) {
                    case HUNDRED:
                        fractionDenominator = 100;
                        break;
                    case TEN_THOUSAND:
                        fractionDenominator = 10000;
                        break;
                    case MILLION:
                        fractionDenominator = 1000000;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported route fraction denominator");
                }
                fractionNumerator = Math.min(Integer.toUnsignedLong(fraction.getNumerator()), fractionDenominator);
            }
            denominator = fractionDenominator;
            numerator = fractionNumerator;
            if (!route.hasRoute()) {
                throw new IllegalArgumentException("Unsupported route action");
            }
            RouteAction action = route.getRoute();
            // Additional call policies are enabled with their execution support, never silently ignored.
            SecurityFields.only(
                    action,
                    "cluster",
                    "weighted_clusters",
                    "timeout",
                    "max_stream_duration",
                    "max_grpc_timeout",
                    "retry_policy",
                    "hash_policy",
                    "hedge_policy");
            hashPolicy = new XdsHashPolicy(action.getHashPolicyList());
            retryPolicy = action.hasRetryPolicy() ? XdsRetryPolicy.parse(action.getRetryPolicy()) : inherited;
            if (action.hasMaxGrpcTimeout()) {
                XdsTimeouts.nanos(action.getMaxGrpcTimeout()); // Deprecated proxy field, ignored by gRPC xDS.
            }
            timeoutNanos = XdsTimeouts.route(action);
            switch (action.getClusterSpecifierCase()) {
                case CLUSTER:
                    addCluster(action.getCluster(), 1);
                    break;
                case WEIGHTED_CLUSTERS:
                    WeightedCluster weighted = action.getWeightedClusters();
                    SecurityFields.only(weighted, "clusters", "total_weight", "runtime_key_prefix");
                    long total = 0;
                    for (WeightedCluster.ClusterWeight weight : weighted.getClustersList()) {
                        SecurityFields.only(weight, "name", "weight", "typed_per_filter_config");
                        clusterFaults.put(
                                weight.getName(), XdsFaultFilters.overrides(weight.getTypedPerFilterConfigMap()));
                        if (!weight.hasWeight()) {
                            throw new IllegalArgumentException("Cluster weight is missing");
                        }
                        long value = Integer.toUnsignedLong(weight.getWeight().getValue());
                        addCluster(weight.getName(), value);
                        total = Math.addExact(total, value);
                    }
                    if (total == 0
                            || total > 0xffffffffL
                            || (weighted.hasTotalWeight()
                                    && Integer.toUnsignedLong(
                                                    weighted.getTotalWeight().getValue())
                                            != total)) {
                        throw new IllegalArgumentException("Invalid total cluster weight");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported route cluster selection");
            }
        }

        private void addCluster(String cluster, long weight) {
            if (cluster.isEmpty() || clusters.putIfAbsent(cluster, weight) != null) {
                throw new IllegalArgumentException("Empty or duplicate route cluster");
            }
        }

        public boolean matches(Map<String, String> values) {
            if (!path.test(values.get(":path"))) {
                return false;
            }
            for (Predicate<Map<String, String>> header : headers) {
                if (!header.test(values)) {
                    return false;
                }
            }
            return numerator >= denominator || ThreadLocalRandom.current().nextInt(denominator) < numerator;
        }

        public String selectCluster() {
            long total = 0;
            for (long weight : clusters.values()) {
                total += weight;
            }
            long selected = ThreadLocalRandom.current().nextLong(total);
            for (Map.Entry<String, Long> weight : clusters.entrySet()) {
                selected -= weight.getValue();
                if (selected < 0) {
                    return weight.getKey();
                }
            }
            throw new IllegalStateException("No weighted cluster");
        }

        public long getTimeoutNanos(long listenerDefaultNanos) {
            return timeoutNanos == null ? listenerDefaultNanos : timeoutNanos;
        }

        public XdsRetryPolicy getRetryPolicy() {
            return retryPolicy;
        }

        long requestHash(Map<String, String> headers, long referenceId) {
            return hashPolicy.hash(headers, referenceId);
        }

        public Route getRoute() {
            return route;
        }

        public Map<String, Long> getClusters() {
            return Collections.unmodifiableMap(clusters);
        }

        public Collection<XdsFaultPolicy> getFaultPolicies(Snapshot snapshot, String cluster) {
            Collection<XdsFaultPolicy> selected = snapshot.getFaultFilters()
                    .effective(hostFaults, routeFaults, clusterFaults.getOrDefault(cluster, Collections.emptyMap()))
                    .values();
            List<XdsFaultPolicy> active = new ArrayList<>();
            selected.forEach(policy -> {
                if (policy.hasActions()) active.add(policy);
            });
            return Collections.unmodifiableList(active);
        }
    }
}
