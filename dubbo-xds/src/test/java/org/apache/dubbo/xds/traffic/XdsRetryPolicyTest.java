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

import org.apache.dubbo.rpc.TriRpcStatus;

import java.util.Collections;

import com.google.protobuf.Duration;
import com.google.protobuf.UInt32Value;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Decorator;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RetryPolicy;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsRetryPolicyTest {
    @Test
    void mapsGrpcConditionsAndBoundsAttemptsAndJitter() {
        XdsRetryPolicy policy = XdsRetryPolicy.parse(RetryPolicy.newBuilder()
                .setRetryOn("connect-failure, cancelled,unavailable,5xx,permission-denied")
                .setNumRetries(UInt32Value.of(-1))
                .build());
        assertEquals(5, policy.getMaxAttempts());
        assertTrue(policy.permits(TriRpcStatus.Code.CANCELLED));
        assertTrue(policy.permits(TriRpcStatus.Code.UNAVAILABLE));
        assertFalse(policy.permits(TriRpcStatus.Code.PERMISSION_DENIED));
        assertEquals(25000000, policy.getInitialBackoff());
        assertEquals(50000000, policy.nextBackoff(policy.getInitialBackoff()));
        assertEquals(250000000, policy.nextBackoff(Long.MAX_VALUE));
        for (int i = 0; i < 50; i++) {
            long delay = policy.jitter(policy.getInitialBackoff());
            assertTrue(delay >= 0 && delay < 25000000);
        }
        assertNull(XdsRetryPolicy.parse(
                RetryPolicy.newBuilder().setRetryOn("5xx,connect-failure").build()));
        assertThrows(
                IllegalArgumentException.class,
                () -> XdsRetryPolicy.parse(RetryPolicy.newBuilder()
                        .setNumRetries(UInt32Value.of(0))
                        .build()));
    }

    @Test
    void validatesBackoffEvenWhenNoConditionsAreSupported() {
        RetryPolicy.Builder policy = RetryPolicy.newBuilder()
                .setRetryOn("unavailable")
                .setRetryBackOff(RetryPolicy.RetryBackOff.newBuilder());
        assertThrows(IllegalArgumentException.class, () -> XdsRetryPolicy.parse(policy.build()));
        policy.getRetryBackOffBuilder().setBaseInterval(Duration.getDefaultInstance());
        assertThrows(IllegalArgumentException.class, () -> XdsRetryPolicy.parse(policy.build()));
        policy.getRetryBackOffBuilder().setBaseInterval(Duration.newBuilder().setNanos(100));
        assertEquals(1000000, XdsRetryPolicy.parse(policy.build()).getInitialBackoff());
        policy.getRetryBackOffBuilder().setMaxInterval(Duration.newBuilder().setNanos(99));
        assertThrows(IllegalArgumentException.class, () -> XdsRetryPolicy.parse(policy.build()));
        policy.getRetryBackOffBuilder()
                .setBaseInterval(Duration.newBuilder().setSeconds(315576000000L))
                .clearMaxInterval();
        assertEquals(Long.MAX_VALUE, XdsRetryPolicy.parse(policy.build()).nextBackoff(Long.MAX_VALUE));
    }

    @Test
    void routeOverridesHostIncludingDisabledPolicyAndAcceptsIstioProxyFields() {
        RetryPolicy hostPolicy =
                RetryPolicy.newBuilder().setRetryOn("unavailable").build();
        Route.Builder route = Route.newBuilder()
                .setDecorator(Decorator.newBuilder().setOperation("echo:7070/*"))
                .setMatch(RouteMatch.newBuilder().setPrefix("/"));
        RouteAction.Builder action =
                RouteAction.newBuilder().setCluster("cluster").setMaxGrpcTimeout(Duration.getDefaultInstance());
        VirtualHost.Builder host = VirtualHost.newBuilder().addDomains("*").setRetryPolicy(hostPolicy);
        assertTrue(table(host, route.setRoute(action)).getRetryPolicy().permits(TriRpcStatus.Code.UNAVAILABLE));
        action.setRetryPolicy(RetryPolicy.newBuilder()
                .setRetryOn("resource-exhausted")
                .setPerTryTimeout(Duration.newBuilder().setNanos(1))
                .setHostSelectionRetryMaxAttempts(5));
        XdsRetryPolicy overridden = table(host, route.setRoute(action)).getRetryPolicy();
        assertTrue(overridden.permits(TriRpcStatus.Code.RESOURCE_EXHAUSTED));
        assertFalse(overridden.permits(TriRpcStatus.Code.UNAVAILABLE));
        action.setRetryPolicy(RetryPolicy.newBuilder().setRetryOn("5xx"));
        assertNull(table(host, route.setRoute(action)).getRetryPolicy());
    }

    private static XdsRouteTable.Match table(VirtualHost.Builder host, Route.Builder route) {
        host.clearRoutes().addRoutes(route);
        return new XdsRouteTable(
                        RouteConfiguration.newBuilder().addVirtualHosts(host).build())
                .match(Collections.singletonMap(":path", "/method"));
    }
}
