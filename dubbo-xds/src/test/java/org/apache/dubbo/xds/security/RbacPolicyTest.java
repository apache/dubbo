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

import org.apache.dubbo.remoting.http12.HttpHeaders;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SSLSession;

import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Collections;

import com.google.protobuf.UInt32Value;
import com.google.protobuf.UnknownFieldSet;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.CidrRange;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.rbac.v3.Permission;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.rbac.v3.Policy;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.rbac.v3.Principal;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.rbac.v3.RBAC;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.HeaderMatcher;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.PathMatcher;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.RegexMatcher;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.StringMatcher;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.xds.security.InboundSecurityTest.PRINCIPAL;
import static org.apache.dubbo.xds.security.InboundSecurityTest.allow;
import static org.apache.dubbo.xds.security.InboundSecurityTest.deny;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RbacPolicyTest {
    @Test
    void authenticatedPrincipalComesFromCertificateAndDenyAlwaysWins() throws Exception {
        HttpHeaders spoofed =
                HttpHeaders.create().set("x-forwarded-client-cert", PRINCIPAL).set("authorization", PRINCIPAL);
        RbacPolicy allowed = new RbacPolicy(Collections.singletonList(allow()));
        assertFalse(allowed.permits(request(spoofed, null)));
        SSLSession session = mock(SSLSession.class);
        XdsSecurityTest.Authority ca = new XdsSecurityTest.Authority();
        when(session.getPeerCertificates()).thenReturn(new Certificate[] {ca.identity(PRINCIPAL).certificate});
        RbacRequest authenticated = request(spoofed, session);
        assertTrue(allowed.permits(authenticated));
        assertFalse(new RbacPolicy(Arrays.asList(allow(), deny())).permits(authenticated));
        assertFalse(new RbacPolicy(Collections.singletonList(
                        RBAC.newBuilder().setAction(RBAC.Action.ALLOW).build()))
                .permits(authenticated));
        assertTrue(new RbacPolicy(Collections.emptyList()).permits(request(spoofed, null)));
    }

    @Test
    void matchesWirePathHeadersSocketAddressAndPortTogether() {
        Permission permission = Permission.newBuilder()
                .setAndRules(Permission.Set.newBuilder()
                        .addRules(Permission.newBuilder()
                                .setUrlPath(PathMatcher.newBuilder()
                                        .setPath(StringMatcher.newBuilder().setExact("/service/method"))))
                        .addRules(Permission.newBuilder().setDestinationPort(17070))
                        .addRules(Permission.newBuilder()
                                .setHeader(HeaderMatcher.newBuilder()
                                        .setName("X-Tenant")
                                        .setSafeRegexMatch(
                                                RegexMatcher.newBuilder().setRegex("tenant-[0-9]+")))))
                .build();
        Principal principal = Principal.newBuilder()
                .setDirectRemoteIp(
                        CidrRange.newBuilder().setAddressPrefix("10.1.2.0").setPrefixLen(UInt32Value.of(24)))
                .build();
        RbacPolicy policy = new RbacPolicy(Collections.singletonList(RBAC.newBuilder()
                .setAction(RBAC.Action.ALLOW)
                .putPolicies(
                        "wire",
                        Policy.newBuilder()
                                .addPermissions(permission)
                                .addPrincipals(principal)
                                .build())
                .build()));
        HttpHeaders headers = HttpHeaders.create()
                .set(":path", "/service/method?query=1#fragment")
                .set("x-tenant", "tenant-42");
        assertTrue(policy.permits(request(headers, null)));
        assertFalse(policy.permits(
                request(HttpHeaders.create().set(":path", "/other/method").set("x-tenant", "tenant-42"), null)));
        assertFalse(policy.permits(new RbacRequest(
                headers, new InetSocketAddress("127.0.0.1", 17070), new InetSocketAddress("10.1.3.2", 10000), null)));
        assertFalse(policy.permits(request(
                HttpHeaders.create().set(":path", "/service/method").set("x-tenant", "prefix-tenant-42"), null)));
        assertEquals(
                "/service/method?query=1#fragment",
                request(headers, null).headers.get(":path"));
    }

    @Test
    void missingHeaderDoesNotSatisfyInvertedValueUnlessExplicitlyAllowed() {
        HeaderMatcher inverted = HeaderMatcher.newBuilder()
                .setName("missing")
                .setExactMatch("forbidden")
                .setInvertMatch(true)
                .build();
        RbacRequest absent = request(HttpHeaders.create(), null);
        assertFalse(RbacPolicy.header(inverted).test(absent));
        assertTrue(RbacPolicy.header(
                        inverted.toBuilder().setTreatMissingHeaderAsEmpty(true).build())
                .test(absent));
        assertTrue(RbacPolicy.header(HeaderMatcher.newBuilder()
                        .setName("missing")
                        .setPresentMatch(false)
                        .build())
                .test(absent));
    }

    @Test
    void unsupportedRestrictionsAreRejectedAndUnavailableSniDoesNotErasePrincipal() throws Exception {
        UnknownFieldSet unknown = UnknownFieldSet.newBuilder()
                .addField(10000, UnknownFieldSet.Field.newBuilder().addVarint(1).build())
                .build();
        RBAC invalid = allow().toBuilder()
                .putPolicies(
                        "unsupported",
                        Policy.newBuilder()
                                .addPermissions(
                                        Permission.newBuilder().setAny(true).setUnknownFields(unknown))
                                .addPrincipals(Principal.newBuilder().setAny(true))
                                .build())
                .build();
        assertThrows(IllegalArgumentException.class, () -> new RbacPolicy(Collections.singletonList(invalid)));
        ExtendedSSLSession session = mock(ExtendedSSLSession.class);
        XdsSecurityTest.Authority ca = new XdsSecurityTest.Authority();
        when(session.getPeerCertificates()).thenReturn(new Certificate[] {ca.identity(PRINCIPAL).certificate});
        when(session.getRequestedServerNames()).thenThrow(new UnsupportedOperationException());
        assertTrue(new RbacPolicy(Collections.singletonList(allow())).permits(request(HttpHeaders.create(), session)));
    }

    private static RbacRequest request(HttpHeaders headers, SSLSession session) {
        return new RbacRequest(
                headers, new InetSocketAddress("127.0.0.1", 17070), new InetSocketAddress("10.1.2.3", 10000), session);
    }
}
