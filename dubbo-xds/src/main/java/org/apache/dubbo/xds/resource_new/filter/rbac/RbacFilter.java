/*
 * Copyright 2021 The gRPC Authors
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

package org.apache.dubbo.xds.resource_new.filter.rbac;

import org.apache.dubbo.xds.resource_new.common.ConfigOrError;
import org.apache.dubbo.xds.resource_new.filter.Filter;
import org.apache.dubbo.xds.resource_new.filter.ServerFilter;
import org.apache.dubbo.xds.resource_new.matcher.CidrMatcher;
import org.apache.dubbo.xds.resource_new.matcher.MatcherParser;
import org.apache.dubbo.xds.resource_new.matcher.StringMatcher;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.envoyproxy.envoy.config.core.v3.CidrRange;
import io.envoyproxy.envoy.config.rbac.v3.Permission;
import io.envoyproxy.envoy.config.rbac.v3.Policy;
import io.envoyproxy.envoy.config.rbac.v3.Principal;
import io.envoyproxy.envoy.extensions.filters.http.rbac.v3.RBAC;
import io.envoyproxy.envoy.extensions.filters.http.rbac.v3.RBACPerRoute;
import io.envoyproxy.envoy.type.v3.Int32Range;

/**
 * RBAC Http filter implementation.
 */
public final class RbacFilter implements Filter, ServerFilter {
    private static final Logger logger = Logger.getLogger(RbacFilter.class.getName());

    public static final RbacFilter INSTANCE = new RbacFilter();

    static final String TYPE_URL = "type.googleapis.com/envoy.extensions.filters.http.rbac.v3.RBAC";

    private static final String TYPE_URL_OVERRIDE_CONFIG =
            "type.googleapis.com/envoy.extensions.filters.http.rbac.v3" + ".RBACPerRoute";

    RbacFilter() {}

    @Override
    public String[] typeUrls() {
        return new String[] {TYPE_URL, TYPE_URL_OVERRIDE_CONFIG};
    }

    @Override
    public ConfigOrError<RbacConfig> parseFilterConfig(Message rawProtoMessage) {
        RBAC rbacProto;
        if (!(rawProtoMessage instanceof Any)) {
            return ConfigOrError.fromError("Invalid config type: " + rawProtoMessage.getClass());
        }
        Any anyMessage = (Any) rawProtoMessage;
        try {
            rbacProto = anyMessage.unpack(RBAC.class);
        } catch (InvalidProtocolBufferException e) {
            return ConfigOrError.fromError("Invalid proto: " + e);
        }
        return parseRbacConfig(rbacProto);
    }

    static ConfigOrError<RbacConfig> parseRbacConfig(RBAC rbac) {
        if (!rbac.hasRules()) {
            return ConfigOrError.fromConfig(RbacConfig.create(null));
        }
        io.envoyproxy.envoy.config.rbac.v3.RBAC rbacConfig = rbac.getRules();
        Action authAction;
        switch (rbacConfig.getAction()) {
            case ALLOW:
                authAction = Action.ALLOW;
                break;
            case DENY:
                authAction = Action.DENY;
                break;
            case LOG:
                return ConfigOrError.fromConfig(RbacConfig.create(null));
            case UNRECOGNIZED:
            default:
                return ConfigOrError.fromError("Unknown rbacConfig action type: " + rbacConfig.getAction());
        }
        List<PolicyMatcher> policyMatchers = new ArrayList<>();
        List<Entry<String, Policy>> sortedPolicyEntries = rbacConfig.getPoliciesMap()
                .entrySet()
                .stream()
                .sorted((a, b) -> a.getKey()
                        .compareTo(b.getKey()))
                .collect(Collectors.toList());
        for (Entry<String, Policy> entry : sortedPolicyEntries) {
            try {
                Policy policy = entry.getValue();
                if (policy.hasCondition() || policy.hasCheckedCondition()) {
                    return ConfigOrError.fromError(
                            "Policy.condition and Policy.checked_condition must not set: " + entry.getKey());
                }
                policyMatchers.add(PolicyMatcher.create(entry.getKey(),
                        parsePermissionList(policy.getPermissionsList()),
                        parsePrincipalList(policy.getPrincipalsList())));
            } catch (Exception e) {
                return ConfigOrError.fromError("Encountered error parsing policy: " + e);
            }
        }
        return ConfigOrError.fromConfig(RbacConfig.create(AuthConfig.create(policyMatchers, authAction)));
    }

    @Override
    public ConfigOrError<RbacConfig> parseFilterConfigOverride(Message rawProtoMessage) {
        RBACPerRoute rbacPerRoute;
        if (!(rawProtoMessage instanceof Any)) {
            return ConfigOrError.fromError("Invalid config type: " + rawProtoMessage.getClass());
        }
        Any anyMessage = (Any) rawProtoMessage;
        try {
            rbacPerRoute = anyMessage.unpack(RBACPerRoute.class);
        } catch (InvalidProtocolBufferException e) {
            return ConfigOrError.fromError("Invalid proto: " + e);
        }
        if (rbacPerRoute.hasRbac()) {
            return parseRbacConfig(rbacPerRoute.getRbac());
        } else {
            return ConfigOrError.fromConfig(RbacConfig.create(null));
        }
    }

    private static OrMatcher parsePermissionList(List<Permission> permissions) {
        List<Matcher> anyMatch = new ArrayList<>();
        for (Permission permission : permissions) {
            anyMatch.add(parsePermission(permission));
        }
        return OrMatcher.create(anyMatch);
    }

    private static Matcher parsePermission(Permission permission) {
        switch (permission.getRuleCase()) {
            case AND_RULES:
                List<Matcher> andMatch = new ArrayList<>();
                for (Permission p : permission.getAndRules()
                        .getRulesList()) {
                    andMatch.add(parsePermission(p));
                }
                return AndMatcher.create(andMatch);
            case OR_RULES:
                return parsePermissionList(permission.getOrRules()
                        .getRulesList());
            case ANY:
                return AlwaysTrueMatcher.INSTANCE;
            case HEADER:
                return parseHeaderMatcher(permission.getHeader());
            case URL_PATH:
                return parsePathMatcher(permission.getUrlPath());
            case DESTINATION_IP:
                return createDestinationIpMatcher(permission.getDestinationIp());
            case DESTINATION_PORT:
                return createDestinationPortMatcher(permission.getDestinationPort());
            case DESTINATION_PORT_RANGE:
                return parseDestinationPortRangeMatcher(permission.getDestinationPortRange());
            case NOT_RULE:
                return InvertMatcher.create(parsePermission(permission.getNotRule()));
            case METADATA: // hard coded, never match.
                return InvertMatcher.create(AlwaysTrueMatcher.INSTANCE);
            case REQUESTED_SERVER_NAME:
                return parseRequestedServerNameMatcher(permission.getRequestedServerName());
            case RULE_NOT_SET:
            default:
                throw new IllegalArgumentException("Unknown permission rule case: " + permission.getRuleCase());
        }
    }

    private static OrMatcher parsePrincipalList(List<Principal> principals) {
        List<Matcher> anyMatch = new ArrayList<>();
        for (Principal principal : principals) {
            anyMatch.add(parsePrincipal(principal));
        }
        return OrMatcher.create(anyMatch);
    }

    private static Matcher parsePrincipal(Principal principal) {
        switch (principal.getIdentifierCase()) {
            case OR_IDS:
                return parsePrincipalList(principal.getOrIds()
                        .getIdsList());
            case AND_IDS:
                List<Matcher> nextMatchers = new ArrayList<>();
                for (Principal next : principal.getAndIds()
                        .getIdsList()) {
                    nextMatchers.add(parsePrincipal(next));
                }
                return AndMatcher.create(nextMatchers);
            case ANY:
                return AlwaysTrueMatcher.INSTANCE;
            case AUTHENTICATED:
                return parseAuthenticatedMatcher(principal.getAuthenticated());
            case DIRECT_REMOTE_IP:
                return createSourceIpMatcher(principal.getDirectRemoteIp());
            case REMOTE_IP:
                return createSourceIpMatcher(principal.getRemoteIp());
            case SOURCE_IP:
                return createSourceIpMatcher(principal.getSourceIp());
            case HEADER:
                return parseHeaderMatcher(principal.getHeader());
            case NOT_ID:
                return InvertMatcher.create(parsePrincipal(principal.getNotId()));
            case URL_PATH:
                return parsePathMatcher(principal.getUrlPath());
            case METADATA: // hard coded, never match.
                return InvertMatcher.create(AlwaysTrueMatcher.INSTANCE);
            case IDENTIFIER_NOT_SET:
            default:
                throw new IllegalArgumentException(
                        "Unknown principal identifier case: " + principal.getIdentifierCase());
        }
    }

    private static PathMatcher parsePathMatcher(
            io.envoyproxy.envoy.type.matcher.v3.PathMatcher proto) {
        switch (proto.getRuleCase()) {
            case PATH:
                return PathMatcher.create(MatcherParser.parseStringMatcher(proto.getPath()));
            case RULE_NOT_SET:
            default:
                throw new IllegalArgumentException("Unknown path matcher rule type: " + proto.getRuleCase());
        }
    }

    private static RequestedServerNameMatcher parseRequestedServerNameMatcher(
            io.envoyproxy.envoy.type.matcher.v3.StringMatcher proto) {
        return RequestedServerNameMatcher.create(MatcherParser.parseStringMatcher(proto));
    }

    private static AuthHeaderMatcher parseHeaderMatcher(
            io.envoyproxy.envoy.config.route.v3.HeaderMatcher proto) {
        if (proto.getName()
                .startsWith("grpc-")) {
            throw new IllegalArgumentException(
                    "Invalid header matcher config: [grpc-] prefixed " + "header name is not allowed.");
        }
        if (":scheme".equals(proto.getName())) {
            throw new IllegalArgumentException(
                    "Invalid header matcher config: header name [:scheme] " + "is not allowed.");
        }
        return AuthHeaderMatcher.create(MatcherParser.parseHeaderMatcher(proto));
    }

    private static AuthenticatedMatcher parseAuthenticatedMatcher(
            Principal.Authenticated proto) {
        StringMatcher matcher = MatcherParser.parseStringMatcher(proto.getPrincipalName());
        return AuthenticatedMatcher.create(matcher);
    }

    private static DestinationPortMatcher createDestinationPortMatcher(int port) {
        return DestinationPortMatcher.create(port);
    }

    private static DestinationPortRangeMatcher parseDestinationPortRangeMatcher(Int32Range range) {
        return DestinationPortRangeMatcher.create(range.getStart(), range.getEnd());
    }

    private static DestinationIpMatcher createDestinationIpMatcher(CidrRange cidrRange) {
        return DestinationIpMatcher.create(CidrMatcher.create(resolve(cidrRange), cidrRange.getPrefixLen()
                .getValue()));
    }

    private static SourceIpMatcher createSourceIpMatcher(CidrRange cidrRange) {
        return SourceIpMatcher.create(CidrMatcher.create(resolve(cidrRange), cidrRange.getPrefixLen()
                .getValue()));
    }

    private static InetAddress resolve(CidrRange cidrRange) {
        try {
            return InetAddress.getByName(cidrRange.getAddressPrefix());
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("IP address can not be found: " + ex);
        }
    }
}

