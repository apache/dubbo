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
package org.apache.dubbo.xds.security;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.CidrRange;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.rbac.v3.Permission;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.rbac.v3.Policy;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.rbac.v3.Principal;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.rbac.v3.RBAC;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.HeaderMatcher;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.PathMatcher;
import io.netty.util.NetUtil;

/** Compiles the ALLOW/DENY contract; unsupported restrictions fail resource validation. */
public final class RbacPolicy {
    private final List<RuleSet> denied = new ArrayList<>();
    private final List<RuleSet> allowed = new ArrayList<>();

    public RbacPolicy(List<RBAC> filters) {
        Compiler compiler = new Compiler();
        for (RBAC filter : filters) {
            SecurityFields.only(filter, "action", "policies");
            List<Predicate<RbacRequest>> policies = new ArrayList<>();
            for (Policy policy : filter.getPoliciesMap().values()) {
                SecurityFields.only(policy, "permissions", "principals");
                if (policy.getPermissionsCount() == 0 || policy.getPrincipalsCount() == 0) {
                    throw new IllegalArgumentException("RBAC policy requires permissions and principals");
                }
                List<Predicate<RbacRequest>> permissions = new ArrayList<>();
                List<Predicate<RbacRequest>> principals = new ArrayList<>();
                for (Permission permission : policy.getPermissionsList()) {
                    permissions.add(compiler.permission(permission, 0));
                }
                for (Principal principal : policy.getPrincipalsList()) {
                    principals.add(compiler.principal(principal, 0));
                }
                policies.add(request -> any(permissions, request) && any(principals, request));
            }
            RuleSet rules = new RuleSet(policies);
            switch (filter.getAction()) {
                case ALLOW:
                    allowed.add(rules);
                    break;
                case DENY:
                    denied.add(rules);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported RBAC action");
            }
        }
    }

    public boolean permits(RbacRequest request) {
        for (RuleSet rules : denied) {
            if (any(rules.policies, request)) {
                return false;
            }
        }
        for (RuleSet rules : allowed) {
            if (!any(rules.policies, request)) {
                return false;
            }
        }
        return true;
    }

    private static boolean any(List<Predicate<RbacRequest>> predicates, RbacRequest request) {
        for (Predicate<RbacRequest> predicate : predicates) {
            if (predicate.test(request)) {
                return true;
            }
        }
        return false;
    }

    private static Predicate<RbacRequest> combine(List<Predicate<RbacRequest>> predicates, boolean all) {
        if (predicates.isEmpty()) {
            throw new IllegalArgumentException("Empty RBAC matcher set");
        }
        return request -> {
            for (Predicate<RbacRequest> predicate : predicates) {
                if (predicate.test(request) != all) {
                    return !all;
                }
            }
            return all;
        };
    }

    private static Predicate<RbacRequest> path(PathMatcher matcher) {
        SecurityFields.only(matcher, "path");
        if (!matcher.hasPath()) {
            throw new IllegalArgumentException("Missing RBAC path matcher");
        }
        Predicate<String> value = XdsMatchers.string(matcher.getPath());
        return request -> value.test(request.path());
    }

    static Predicate<RbacRequest> header(HeaderMatcher matcher) {
        Predicate<Map<String, String>> compiled = XdsHeaderMatcher.compile(matcher);
        return request -> compiled.test(request.headers);
    }

    private static Predicate<InetSocketAddress> cidr(CidrRange range) {
        SecurityFields.only(range, "address_prefix", "prefix_len");
        byte[] network = NetUtil.createByteArrayFromIpAddressString(range.getAddressPrefix());
        if (network == null) {
            throw new IllegalArgumentException("Invalid numeric RBAC CIDR address");
        }
        int bits = range.getPrefixLen().getValue();
        if (bits < 0 || bits > network.length * 8) {
            throw new IllegalArgumentException("Invalid RBAC CIDR prefix");
        }
        return address -> {
            if (address == null || address.getAddress() == null) {
                return false;
            }
            byte[] peer = address.getAddress().getAddress();
            if (peer.length != network.length) {
                return false;
            }
            int bytes = bits / 8;
            for (int i = 0; i < bytes; i++) {
                if (network[i] != peer[i]) {
                    return false;
                }
            }
            int remaining = bits % 8;
            int mask = 0xff << (8 - remaining);
            return remaining == 0 || (network[bytes] & mask) == (peer[bytes] & mask);
        };
    }

    private static final class Compiler {
        int nodes;

        void check(int depth) {
            if (depth > 64 || ++nodes > 16384) {
                throw new IllegalArgumentException("RBAC matcher complexity limit");
            }
        }

        Predicate<RbacRequest> permission(Permission permission, int depth) {
            check(depth);
            SecurityFields.only(permission, permission.getRuleCase().name().toLowerCase(Locale.ROOT));
            switch (permission.getRuleCase()) {
                case ANY:
                    return request -> permission.getAny();
                case AND_RULES:
                case OR_RULES:
                    boolean all = permission.getRuleCase() == Permission.RuleCase.AND_RULES;
                    Permission.Set set = all ? permission.getAndRules() : permission.getOrRules();
                    SecurityFields.only(set, "rules");
                    List<Predicate<RbacRequest>> rules = new ArrayList<>();
                    for (Permission rule : set.getRulesList()) {
                        rules.add(permission(rule, depth + 1));
                    }
                    return combine(rules, all);
                case NOT_RULE:
                    return permission(permission.getNotRule(), depth + 1).negate();
                case HEADER:
                    return header(permission.getHeader());
                case URL_PATH:
                    return path(permission.getUrlPath());
                case DESTINATION_IP:
                    Predicate<InetSocketAddress> ip = cidr(permission.getDestinationIp());
                    return request -> ip.test(request.local);
                case DESTINATION_PORT:
                    int port = permission.getDestinationPort();
                    if (port < 1 || port > 65535) {
                        throw new IllegalArgumentException("Invalid RBAC port");
                    }
                    return request -> request.local != null && request.local.getPort() == port;
                case DESTINATION_PORT_RANGE:
                    int start = permission.getDestinationPortRange().getStart();
                    int end = permission.getDestinationPortRange().getEnd();
                    if (start < 0 || start >= end || end > 65536) {
                        throw new IllegalArgumentException("Invalid RBAC port range");
                    }
                    return request ->
                            request.local != null && request.local.getPort() >= start && request.local.getPort() < end;
                case REQUESTED_SERVER_NAME:
                    Predicate<String> sni = XdsMatchers.string(permission.getRequestedServerName());
                    return request -> sni.test(request.serverName);
                default:
                    throw new IllegalArgumentException("Unsupported RBAC permission: " + permission.getRuleCase());
            }
        }

        Predicate<RbacRequest> principal(Principal principal, int depth) {
            check(depth);
            SecurityFields.only(principal, principal.getIdentifierCase().name().toLowerCase(Locale.ROOT));
            switch (principal.getIdentifierCase()) {
                case ANY:
                    return request -> principal.getAny();
                case AND_IDS:
                case OR_IDS:
                    boolean all = principal.getIdentifierCase() == Principal.IdentifierCase.AND_IDS;
                    Principal.Set set = all ? principal.getAndIds() : principal.getOrIds();
                    SecurityFields.only(set, "ids");
                    List<Predicate<RbacRequest>> ids = new ArrayList<>();
                    for (Principal id : set.getIdsList()) {
                        ids.add(principal(id, depth + 1));
                    }
                    return combine(ids, all);
                case NOT_ID:
                    return principal(principal.getNotId(), depth + 1).negate();
                case AUTHENTICATED:
                    SecurityFields.only(principal.getAuthenticated(), "principal_name");
                    Predicate<String> name = principal.getAuthenticated().hasPrincipalName()
                            ? XdsMatchers.string(principal.getAuthenticated().getPrincipalName())
                            : value -> true;
                    return request -> request.principals.stream().anyMatch(name);
                case SOURCE_IP:
                case DIRECT_REMOTE_IP:
                case REMOTE_IP:
                    CidrRange range = principal.getIdentifierCase() == Principal.IdentifierCase.SOURCE_IP
                            ? principal.getSourceIp()
                            : principal.getIdentifierCase() == Principal.IdentifierCase.DIRECT_REMOTE_IP
                                    ? principal.getDirectRemoteIp()
                                    : principal.getRemoteIp();
                    Predicate<InetSocketAddress> ip = cidr(range);
                    return request -> ip.test(request.remote);
                case HEADER:
                    return header(principal.getHeader());
                case URL_PATH:
                    return path(principal.getUrlPath());
                default:
                    throw new IllegalArgumentException("Unsupported RBAC principal: " + principal.getIdentifierCase());
            }
        }
    }

    private static final class RuleSet {
        final List<Predicate<RbacRequest>> policies;

        RuleSet(List<Predicate<RbacRequest>> policies) {
            this.policies = policies;
        }
    }
}
