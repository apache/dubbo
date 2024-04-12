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
package org.apache.dubbo.xds.security.authz.rule.source;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;
import org.apache.dubbo.xds.security.authz.rule.matcher.CustomMatcher;
import org.apache.dubbo.xds.security.authz.rule.matcher.IpMatcher;
import org.apache.dubbo.xds.security.authz.rule.matcher.KeyMatcher;
import org.apache.dubbo.xds.security.authz.rule.matcher.Matcher;
import org.apache.dubbo.xds.security.authz.rule.matcher.Matchers;
import org.apache.dubbo.xds.security.authz.rule.matcher.StringMatcher;
import org.apache.dubbo.xds.security.authz.rule.tree.CompositeRuleNode;
import org.apache.dubbo.xds.security.authz.rule.tree.LeafRuleNode;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleNode;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleNode.Relation;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleRoot;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleRoot.Action;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.protobuf.InvalidProtocolBufferException;
import io.envoyproxy.envoy.config.rbac.v3.Permission;
import io.envoyproxy.envoy.config.rbac.v3.Policy;
import io.envoyproxy.envoy.config.rbac.v3.Principal;
import io.envoyproxy.envoy.config.rbac.v3.Principal.IdentifierCase;
import io.envoyproxy.envoy.config.rbac.v3.RBAC;
import io.envoyproxy.envoy.extensions.filters.http.jwt_authn.v3.JwtAuthentication;
import io.envoyproxy.envoy.extensions.filters.http.jwt_authn.v3.JwtProvider;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter;
import io.envoyproxy.envoy.type.matcher.v3.MetadataMatcher.PathSegment;

import static org.apache.dubbo.xds.listener.ListenerConstants.LDS_JWT_FILTER;
import static org.apache.dubbo.xds.listener.ListenerConstants.LDS_RBAC_FILTER;
import static org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty.AUTHENTICATED;
import static org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty.DIRECT_REMOTE_IP;
import static org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty.HEADER;
import static org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty.REMOTE_IP;
import static org.apache.dubbo.xds.security.authz.rule.tree.RuleNode.Relation.AND;
import static org.apache.dubbo.xds.security.authz.rule.tree.RuleNode.Relation.OR;

public class LdsRuleFactory implements RuleFactory<HttpFilter> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(LdsRuleFactory.class);

    public static final String LDS_REQUEST_AUTH_PRINCIPAL = "request.auth.principal";

    public static final String LDS_REQUEST_AUTH_AUDIENCE = "request.auth.audiences";

    public static final String LDS_REQUEST_AUTH_PRESENTER = "request.auth.presenter";

    public static final String LDS_REQUEST_AUTH_CLAIMS = "request.auth.claims";

    public LdsRuleFactory(ApplicationModel applicationModel) {}

    @Override
    public List<RuleRoot> getRules(List<HttpFilter> ruleSource) {
        ArrayList<RuleRoot> roots = new ArrayList<>(resolveJWT(ruleSource).values());
        roots.addAll(resolveRbac(ruleSource).values());
        return roots;
    }

    public Map<String, RuleRoot> resolveRbac(List<HttpFilter> httpFilters) {
        Map<String, RuleRoot> roots = new HashMap<>();
        Map<RBAC.Action, RBAC> rbacMap = new HashMap<>();
        for (HttpFilter httpFilter : httpFilters) {
            if (!httpFilter.getName().equals(LDS_RBAC_FILTER)) {
                continue;
            }
            try {
                io.envoyproxy.envoy.extensions.filters.http.rbac.v3.RBAC rbac = httpFilter
                        .getTypedConfig()
                        .unpack(io.envoyproxy.envoy.extensions.filters.http.rbac.v3.RBAC.class);
                if (rbac != null) {
                    /*There are multiple duplicates, and we only choose one of them */
                    if (!rbacMap.containsKey(rbac.getRules().getAction())) {
                        rbacMap.put(rbac.getRules().getAction(), rbac.getRules());
                    }
                }
            } catch (InvalidProtocolBufferException e) {
                logger.warn("", "", "", "Parsing RbacRule error", e);
            }
        }

        for (Entry<RBAC.Action, RBAC> rbacEntry : rbacMap.entrySet()) {
            RBAC.Action action = rbacEntry.getKey();
            RBAC rbac = rbacEntry.getValue();

            RuleRoot ruleNode =
                    new RuleRoot(AND, action.equals(RBAC.Action.ALLOW) ? Action.ALLOW : Action.DENY, "rules");

            // policies:  "service-admin"、"product-viewer"
            for (Entry<String, Policy> entry : rbac.getPoliciesMap().entrySet()) {

                // rule下的单个policy,包含一个principals Node和 permissions Node，两Node之间AND关系
                CompositeRuleNode policyNode = new CompositeRuleNode(entry.getKey(), AND);

                // 每个policy下可以多个principal，之间OR关系
                CompositeRuleNode principalNode = new CompositeRuleNode("principals", Relation.OR);

                List<Principal> principals = entry.getValue().getPrincipalsList();

                for (Principal principal : principals) {
                    // 解析单个Principal到node
                    RuleNode principalAnd = resolvePrincipal(principal);
                    if (principalAnd != null) {
                        principalNode.addChild(principalAnd);
                    }
                }

                if (!principals.isEmpty()) {
                    policyNode.addChild(principalNode);
                }

                CompositeRuleNode permissionNode = new CompositeRuleNode("permissions", Relation.OR);
                List<Permission> permissions = entry.getValue().getPermissionsList();
                for (Permission permission : permissions) {
                    RuleNode permissionRule = resolvePermission(permission);
                    if (permissionRule != null) {
                        permissionNode.addChild(permissionRule);
                    }
                }

                if (!permissions.isEmpty()) {
                    policyNode.addChild(permissionNode);
                }

                ruleNode.addChild(policyNode);
                roots.put(ruleNode.getNodeName(), ruleNode);
            }
        }
        return roots;
    }

    private RuleNode resolvePrincipal(Principal principal) {

        switch (principal.getIdentifierCase()) {
            case AND_IDS:
                CompositeRuleNode andNode = new CompositeRuleNode("and_ids", Relation.AND);
                for (Principal subPrincipal : principal.getAndIds().getIdsList()) {
                    andNode.addChild(resolvePrincipal(subPrincipal));
                }
                return andNode;

            case OR_IDS:
                CompositeRuleNode orNode = new CompositeRuleNode("or_ids", Relation.OR);
                for (Principal subPrincipal : principal.getOrIds().getIdsList()) {
                    orNode.addChild(resolvePrincipal(subPrincipal));
                }
                return orNode;

            case NOT_ID:
                CompositeRuleNode notNode = new CompositeRuleNode("not_id", Relation.NOT);
                notNode.addChild(resolvePrincipal(principal.getNotId()));
                return notNode;

            default:
                return handleLeafPrincipal(principal);
        }
    }

    private LeafRuleNode handleLeafPrincipal(Principal orIdentity) {
        IdentifierCase principalCase = orIdentity.getIdentifierCase();

        LeafRuleNode valueNode = null;

        switch (principalCase) {
            case AUTHENTICATED:
                StringMatcher matcher =
                        Matchers.stringMatcher(orIdentity.getAuthenticated().getPrincipalName(), AUTHENTICATED);
                if (matcher != null) {
                    valueNode = new LeafRuleNode(Collections.singletonList(matcher), AUTHENTICATED.name());
                }
                break;

            case HEADER:
                String headerName = orIdentity.getHeader().getName();
                KeyMatcher keyMatcher =
                        Matchers.keyMatcher(headerName, Matchers.stringMatcher(orIdentity.getHeader(), HEADER));
                valueNode = new LeafRuleNode(Collections.singletonList(keyMatcher), HEADER.name());
                break;

            case REMOTE_IP:
                IpMatcher ipMatcher = Matchers.ipMatcher(orIdentity.getRemoteIp(), REMOTE_IP);
                valueNode = new LeafRuleNode(Collections.singletonList(ipMatcher), REMOTE_IP.name());
                break;

            case DIRECT_REMOTE_IP:
                IpMatcher directIpMatcher = Matchers.ipMatcher(orIdentity.getDirectRemoteIp(), DIRECT_REMOTE_IP);
                valueNode = new LeafRuleNode(Collections.singletonList(directIpMatcher), DIRECT_REMOTE_IP.name());
                break;

            case METADATA:
                List<PathSegment> segments = orIdentity.getMetadata().getPathList();
                String key = segments.get(0).getKey();

                switch (key) {
                    case LDS_REQUEST_AUTH_PRINCIPAL:
                        StringMatcher jwtPrincipalMatcher = Matchers.stringMatcher(
                                orIdentity.getMetadata().getValue().getStringMatch(),
                                RequestAuthProperty.JWT_PRINCIPALS);
                        if (jwtPrincipalMatcher != null) {
                            valueNode = new LeafRuleNode(
                                    Collections.singletonList(jwtPrincipalMatcher), LDS_REQUEST_AUTH_PRINCIPAL);
                        }
                        break;
                    case LDS_REQUEST_AUTH_AUDIENCE:
                        StringMatcher jwtAudienceMatcher = Matchers.stringMatcher(
                                orIdentity.getMetadata().getValue().getStringMatch(),
                                RequestAuthProperty.JWT_AUDIENCES);
                        if (jwtAudienceMatcher != null) {
                            valueNode = new LeafRuleNode(
                                    Collections.singletonList(jwtAudienceMatcher), LDS_REQUEST_AUTH_AUDIENCE);
                        }
                        break;
                    case LDS_REQUEST_AUTH_PRESENTER:
                        StringMatcher jwtPresenterMatcher = Matchers.stringMatcher(
                                orIdentity.getMetadata().getValue().getStringMatch(),
                                RequestAuthProperty.JWT_PRESENTERS);
                        if (jwtPresenterMatcher != null) {
                            valueNode = new LeafRuleNode(
                                    Collections.singletonList(jwtPresenterMatcher), LDS_REQUEST_AUTH_PRESENTER);
                        }
                        break;
                    case LDS_REQUEST_AUTH_CLAIMS:
                        if (segments.size() >= 2) {
                            String claimKey = segments.get(1).getKey();
                            KeyMatcher jwtClaimsMatcher = Matchers.keyMatcher(
                                    claimKey,
                                    Matchers.stringMatcher(
                                            orIdentity
                                                    .getMetadata()
                                                    .getValue()
                                                    .getListMatch()
                                                    .getOneOf()
                                                    .getStringMatch(),
                                            RequestAuthProperty.JWT_CLAIMS));
                            valueNode = new LeafRuleNode(
                                    Collections.singletonList(jwtClaimsMatcher), LDS_REQUEST_AUTH_CLAIMS);
                        }
                        break;
                    default:
                        logger.warn("Unsupported metadata type=" + key);
                        break;
                }
                break;

            default:
                logger.warn("Unsupported principalCase =" + principalCase);
                break;
        }
        return valueNode;
    }

    private RuleNode resolvePermission(Permission permission) {

        switch (permission.getRuleCase()) {
            case AND_RULES:
                CompositeRuleNode andNode = new CompositeRuleNode("and_rules", Relation.AND);
                for (Permission subPermission : permission.getAndRules().getRulesList()) {
                    andNode.addChild(resolvePermission(subPermission));
                }
                return andNode;

            case OR_RULES:
                CompositeRuleNode orNode = new CompositeRuleNode("or_rules", Relation.OR);
                for (Permission subPermission : permission.getOrRules().getRulesList()) {
                    orNode.addChild(resolvePermission(subPermission));
                }
                return orNode;

            case NOT_RULE:
                CompositeRuleNode notNode = new CompositeRuleNode("not_rules", Relation.NOT);
                notNode.addChild(resolvePermission(permission.getNotRule()));
                return notNode;

            default:
                return handleLeafPermission(permission);
        }
    }

    protected static final String LDS_HEADER_NAME_AUTHORITY = ":authority";

    protected static final String LDS_HEADER_NAME_METHOD = ":method";

    private RuleNode handleLeafPermission(Permission permission) {
        Permission.RuleCase ruleCase = permission.getRuleCase();

        LeafRuleNode leafRuleNode = null;

        switch (ruleCase) {
            case DESTINATION_PORT: {
                int port = permission.getDestinationPort();
                if (port != 0) {
                    StringMatcher matcher = Matchers.stringMatcher(
                            String.valueOf(permission.getDestinationPort()), RequestAuthProperty.DESTINATION_PORT);
                    leafRuleNode = new LeafRuleNode(
                            Collections.singletonList(matcher), RequestAuthProperty.DESTINATION_PORT.name());
                }
                break;
            }
            case REQUESTED_SERVER_NAME: {
                StringMatcher matcher = Matchers.stringMatcher(
                        permission.getRequestedServerName(), RequestAuthProperty.REQUESTED_SERVER_NAME);
                leafRuleNode = new LeafRuleNode(
                        Collections.singletonList(matcher), RequestAuthProperty.DESTINATION_PORT.name());
                break;
            }
            case DESTINATION_IP: {
                IpMatcher matcher =
                        Matchers.ipMatcher(permission.getDestinationIp(), RequestAuthProperty.DESTINATION_IP);
                leafRuleNode =
                        new LeafRuleNode(Collections.singletonList(matcher), RequestAuthProperty.DESTINATION_IP.name());
                break;
            }
            case URL_PATH: {
                StringMatcher matcher =
                        Matchers.stringMatcher(permission.getUrlPath().getPath(), RequestAuthProperty.URL_PATH);
                leafRuleNode =
                        new LeafRuleNode(Collections.singletonList(matcher), RequestAuthProperty.URL_PATH.name());
                break;
            }
            case HEADER: {
                String headerName = permission.getHeader().getName();

                KeyMatcher matcher = null;

                if (LDS_HEADER_NAME_AUTHORITY.equals(headerName)) {
                    matcher = Matchers.keyMatcher(
                            headerName, Matchers.stringMatcher(permission.getHeader(), RequestAuthProperty.HOSTS));
                    leafRuleNode =
                            new LeafRuleNode(Collections.singletonList(matcher), RequestAuthProperty.HOSTS.name());
                } else if (LDS_HEADER_NAME_METHOD.equals(headerName)) {
                    matcher = Matchers.keyMatcher(
                            headerName, Matchers.stringMatcher(permission.getHeader(), RequestAuthProperty.METHODS));
                    leafRuleNode =
                            new LeafRuleNode(Collections.singletonList(matcher), RequestAuthProperty.METHODS.name());
                }

                if (matcher == null) {
                    logger.warn("", "", "", "Unsupported headerName=" + headerName);
                }

                break;
            }
            default:
                logger.warn("", "", "", "Unsupported ruleCase=" + ruleCase);
                break;
        }
        return leafRuleNode;
    }

    public Map<String, RuleRoot> resolveJWT(List<HttpFilter> httpFilters) {
        Map<String, RuleRoot> jwtRules = new HashMap<>();

        JwtAuthentication jwtAuthentication = null;

        for (HttpFilter httpFilter : httpFilters) {
            if (!httpFilter.getName().equals(LDS_JWT_FILTER)) {
                continue;
            }
            try {
                jwtAuthentication = httpFilter.getTypedConfig().unpack(JwtAuthentication.class);
                if (null != jwtAuthentication) {
                    break;
                }
            } catch (InvalidProtocolBufferException e) {
                logger.warn("", "", "", "Parsing JwtRule error", e);
            }
        }
        if (null == jwtAuthentication) {
            return jwtRules;
        }

        RuleRoot ruleRoot = new RuleRoot(OR, Action.ALLOW, "providers");

        Map<String, JwtProvider> jwtProviders = jwtAuthentication.getProvidersMap();
        for (Entry<String, JwtProvider> entry : jwtProviders.entrySet()) {

            CompositeRuleNode compositeRuleNode = new CompositeRuleNode(entry.getKey(), AND);
            JwtProvider provider = entry.getValue();

            String issuer = provider.getIssuer();
            compositeRuleNode.addChild(new LeafRuleNode(
                    Matchers.stringMatcher(issuer, RequestAuthProperty.JWT_ISSUER),
                    RequestAuthProperty.JWT_ISSUER.name()));
            HashSet<String> audiencesList = new HashSet<>(provider.getAudiencesList());

            if (!audiencesList.isEmpty()) {
                Matcher<List<String>> matcher =
                        new CustomMatcher<>(RequestAuthProperty.JWT_AUDIENCES, actualAudiences -> {
                            ArrayList<String> copy = new ArrayList<>(audiencesList);
                            copy.removeAll(actualAudiences);
                            // At least one request audiences can match given audiences
                            return copy.size() != audiencesList.size();
                        });
                compositeRuleNode.addChild(new LeafRuleNode(matcher, RequestAuthProperty.JWT_AUDIENCES.name()));
            }

            String localJwks = provider.getLocalJwks().getInlineString();
            Matcher<DecodedJWT> jwkMatcher = buildJwksMatcher(localJwks);
            compositeRuleNode.addChild(new LeafRuleNode(jwkMatcher, RequestAuthProperty.JWKS.name()));

            ruleRoot.addChild(compositeRuleNode);
        }

        return jwtRules;
    }

    public Matcher<DecodedJWT> buildJwksMatcher(String localJwks) {
        JSONObject jwks = JSON.parseObject(localJwks);

        JSONArray keys = jwks.getJSONArray("keys");

        return new CustomMatcher<>(RequestAuthProperty.JWKS, requestJwt -> {
            Date expiresAt = requestJwt.getExpiresAt();
            if (expiresAt == null || expiresAt.getTime() <= System.currentTimeMillis()) {
                logger.warn(
                        "",
                        "",
                        "",
                        "Failed to verify JWT: JWT.expiresAt=[" + expiresAt + "] and current time is "
                                + System.currentTimeMillis());
                return false;
            }

            String kid = requestJwt.getKeyId();
            String alg = requestJwt.getAlgorithm();
            RSAPublicKey publicKey = null;

            for (int i = 0; i < keys.size(); i++) {
                JSONObject keyNode = keys.getJSONObject(i);
                if (keyNode.getString("kid").equals(kid)) {
                    try {
                        publicKey = buildPublicKey(keyNode.getString("n"), keyNode.getString("e"));
                    } catch (Exception e) {
                        logger.warn("", "", "", "Failed to verify JWT by JWKS: build JWT public key failed.");
                        return false;
                    }
                    break;
                }
            }

            if (publicKey == null) {
                throw new IllegalStateException("Public key not found in JWKS");
            }
            Algorithm algorithm = determineAlgorithm(alg, publicKey);
            JWTVerifier verifier = JWT.require(algorithm).build();

            // Verify the token
            verifier.verify(requestJwt);
            return true;
        });
    }

    private static RSAPublicKey buildPublicKey(String modulusBase64, String exponentBase64)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] modulusBytes = Base64.getUrlDecoder().decode(modulusBase64);
        byte[] exponentBytes = Base64.getUrlDecoder().decode(exponentBase64);
        BigInteger modulus = new BigInteger(1, modulusBytes);
        BigInteger exponent = new BigInteger(1, exponentBytes);

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) factory.generatePublic(spec);
    }

    private static Algorithm determineAlgorithm(String alg, RSAPublicKey publicKey) throws IllegalArgumentException {
        switch (alg) {
            case "RS256":
                return Algorithm.RSA256(publicKey, null);
            case "RS384":
                return Algorithm.RSA384(publicKey, null);
            case "RS512":
                return Algorithm.RSA512(publicKey, null);
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + alg);
        }
    }
}
