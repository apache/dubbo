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
package org.apache.dubbo.xds.auth;

// import envoy.config.rbac.v3.Permission;
// import envoy.config.rbac.v3.Policy;
// import envoy.config.rbac.v3.Principal;
// import envoy.config.rbac.v3.RBAC;
// import envoy.type.matcher.v3.HttpMatcher;
// import envoy.type.matcher.v3.StringMatcher;
// import envoy.type.v3.CidrRange;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.xds.security.authz.AuthorizationRequestContext;
import org.apache.dubbo.xds.security.authz.rule.GeneralRequestCredential;
import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;
import org.apache.dubbo.xds.security.authz.rule.source.LdsRuleFactory;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleRoot;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.google.protobuf.Any;
import com.google.protobuf.UInt32Value;
import io.envoyproxy.envoy.config.core.v3.CidrRange;
import io.envoyproxy.envoy.config.rbac.v3.Permission;
import io.envoyproxy.envoy.config.rbac.v3.Permission.Set;
import io.envoyproxy.envoy.config.rbac.v3.Policy;
import io.envoyproxy.envoy.config.rbac.v3.Principal;
import io.envoyproxy.envoy.config.rbac.v3.Principal.Authenticated;
import io.envoyproxy.envoy.config.rbac.v3.RBAC;
import io.envoyproxy.envoy.config.rbac.v3.RBAC.Action;
import io.envoyproxy.envoy.config.route.v3.HeaderMatcher;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter;
import io.envoyproxy.envoy.type.matcher.v3.RegexMatcher;
import io.envoyproxy.envoy.type.matcher.v3.StringMatcher;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class LdsRuleTest {

    @Test
    public void testMatcher() {

        RBAC sampleConfig1 = io.envoyproxy
                .envoy
                .extensions
                .filters
                .http
                .rbac
                .v3
                .RBAC
                .newBuilder()
                .getRulesBuilder()
                .setAction(Action.ALLOW)
                .putPolicies(
                        "policy-1",
                        Policy.newBuilder()
                                .addPermissions(Policy.newBuilder()
                                        .addPermissionsBuilder()
                                        .setOrRules(Set.newBuilder()
                                                .addRules(Permission.newBuilder()
                                                        .setHeader(HeaderMatcher.newBuilder()
                                                                .setName("method")
                                                                .setExactMatch("GET"))))
                                        .build())
                                .addPrincipals(Principal.newBuilder()
                                        .setAuthenticated(
                                                Authenticated.newBuilder()
                                                        .setPrincipalName(
                                                                StringMatcher.newBuilder()
                                                                        .setSuffix(
                                                                                "CN=example.com,OU=IT,O=Example Corp,L=San Francisco,ST=California,C=US")))
                                        .build())
                                .addPrincipals(Principal.newBuilder()
                                        .setRemoteIp(CidrRange.newBuilder().setAddressPrefix("11.22.33"))
                                        .build())
                                .build())
                .buildPartial();
        RBAC sampleConfig2 = RBAC.newBuilder()
                .setAction(Action.ALLOW)
                .putPolicies(
                        "complex-policy-2",
                        Policy.newBuilder()
                                .addPermissions(Permission.newBuilder()
                                        .setAndRules(Permission.Set.newBuilder()
                                                .addRules(Permission.newBuilder()
                                                        .setOrRules(Permission.Set.newBuilder()
                                                                .addRules(Permission.newBuilder()
                                                                        .setHeader(HeaderMatcher.newBuilder()
                                                                                .setName("path")
                                                                                .setExactMatch("/api")))
                                                                .addRules(Permission.newBuilder()
                                                                        .setHeader(HeaderMatcher.newBuilder()
                                                                                .setName("user-agent")
                                                                                .setSafeRegexMatch(
                                                                                        RegexMatcher.newBuilder()
                                                                                                .setRegex(".*Android.*")
                                                                                                .build()))
                                                                        .build())))
                                                .addRules(Permission.newBuilder()
                                                        .setOrRules(Permission.Set.newBuilder()
                                                                .addRules(Permission.newBuilder()
                                                                        .setDestinationPort(443))
                                                                .addRules(Permission.newBuilder()
                                                                        .setDestinationIp(CidrRange.newBuilder()
                                                                                .setAddressPrefix("10.1.2")
                                                                                .setPrefixLen(UInt32Value.of(24))))
                                                                .build()))
                                                .build())
                                        .build())
                                .addPrincipals(Principal.newBuilder()
                                        .setAndIds(Principal.Set.newBuilder()
                                                .addIds(Principal.newBuilder()
                                                        .setOrIds(Principal.Set.newBuilder()
                                                                .addIds(
                                                                        Principal.newBuilder()
                                                                                .setAuthenticated(
                                                                                        Principal.Authenticated
                                                                                                .newBuilder()
                                                                                                .setPrincipalName(
                                                                                                        StringMatcher
                                                                                                                .newBuilder()
                                                                                                                .setExact(
                                                                                                                        "user@example.com"))))
                                                                .addIds(
                                                                        Principal.newBuilder()
                                                                                .setAuthenticated(
                                                                                        Principal.Authenticated
                                                                                                .newBuilder()
                                                                                                .setPrincipalName(
                                                                                                        StringMatcher
                                                                                                                .newBuilder()
                                                                                                                .setPrefix(
                                                                                                                        "admin"))))
                                                                .build()))
                                                .build())
                                        .build())
                                .build())
                .build();

        io.envoyproxy.envoy.extensions.filters.http.rbac.v3.RBAC rbacConfig = io.envoyproxy
                .envoy
                .extensions
                .filters
                .http
                .rbac
                .v3
                .RBAC
                .newBuilder()
                .setRules(sampleConfig1)
                .build();

        io.envoyproxy.envoy.extensions.filters.http.rbac.v3.RBAC rbacConfig2 = io.envoyproxy
                .envoy
                .extensions
                .filters
                .http
                .rbac
                .v3
                .RBAC
                .newBuilder()
                .setRules(sampleConfig2)
                .build();

        HttpFilter rbacFilter = HttpFilter.newBuilder()
                .setName("envoy.filters.http.rbac")
                .setTypedConfig(Any.pack(rbacConfig))
                .build();
        HttpFilter rbacFilter2 = HttpFilter.newBuilder()
                .setName("envoy.filters.http.rbac")
                .setTypedConfig(Any.pack(rbacConfig2))
                .build();

        LdsRuleFactory ldsRuleFactory = new LdsRuleFactory(null);
        List<RuleRoot> rules =
                ldsRuleFactory.getRules(URL.valueOf("test://test"), Arrays.asList(rbacFilter, rbacFilter2));

        // rule1: ALLOW [ method=GET AND FROM *CN=example.com,OU=IT,O=Example Corp,L=San Francisco,ST=California,C=US
        // AND sourceIP = 11.22.33*]
        // rule2: ALLOW [(path=/api OR user-agent=Android) AND (destinationPort=443 OR destinationIP=10.1.2*) AND
        // (Principal = user@example.com OR admin*) ]
        GeneralRequestCredential credential = new GeneralRequestCredential(Collections.emptyMap());
        credential.addByType(RequestAuthProperty.HTTP_METHOD, "GET");
        credential.addByType(
                RequestAuthProperty.AUTHENTICATED,
                "admin,CN=example.com,OU=IT,O=Example Corp,L=San Francisco,ST=California,C=US");
        credential.addByType(RequestAuthProperty.URL_PATH, "/api");
        HashMap<String, String> header = new HashMap<>();
        header.put("path", "/api");
        header.put("method", "GET");
        credential.addByType(RequestAuthProperty.HEADER, header);
        credential.addByType(RequestAuthProperty.REMOTE_IP, "33.44.55.66");
        credential.addByType(RequestAuthProperty.DESTINATION_IP, "11.22.33.44");
        credential.addByType(RequestAuthProperty.DESTINATION_PORT, "443");
        AuthorizationRequestContext context = new AuthorizationRequestContext(null, credential);

        context.startTrace();
        boolean res = rules.get(0).evaluate(context);
        res &= rules.get(1).evaluate(context);
        Assertions.assertTrue(res);
        System.out.println(context.getTraceInfo());
    }
}
