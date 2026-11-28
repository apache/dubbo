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

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Duration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsTimeoutsTest {
    @Test
    void headerMaximumOverridesRouteLimitIncludingExplicitZero() {
        RouteAction.Builder action = RouteAction.newBuilder().setCluster("cluster");
        assertEquals(TimeUnit.SECONDS.toNanos(3), limit(action, TimeUnit.SECONDS.toNanos(3)));
        action.setMaxStreamDuration(RouteAction.MaxStreamDuration.newBuilder()
                .setMaxStreamDuration(Duration.newBuilder().setSeconds(2)));
        assertEquals(TimeUnit.SECONDS.toNanos(2), limit(action, TimeUnit.SECONDS.toNanos(3)));
        action.getMaxStreamDurationBuilder().setGrpcTimeoutHeaderMax(Duration.getDefaultInstance());
        assertEquals(0, limit(action, TimeUnit.SECONDS.toNanos(3)));
        action.getMaxStreamDurationBuilder()
                .setGrpcTimeoutHeaderMax(Duration.newBuilder().setSeconds(1));
        assertEquals(TimeUnit.SECONDS.toNanos(1), limit(action, TimeUnit.SECONDS.toNanos(3)));
    }

    @Test
    void proxyResponseTimerAndOffsetDoNotShortenTheClientDeadline() {
        RouteAction.Builder action = RouteAction.newBuilder()
                .setCluster("cluster")
                .setTimeout(Duration.newBuilder().setNanos(1))
                .setMaxStreamDuration(RouteAction.MaxStreamDuration.newBuilder()
                        .setGrpcTimeoutHeaderOffset(Duration.newBuilder().setSeconds(9)));
        assertEquals(TimeUnit.SECONDS.toNanos(3), limit(action, TimeUnit.SECONDS.toNanos(3)));
    }

    @Test
    void invalidDurationRejectedAndVeryLongValidDurationSaturatesSafely() {
        assertThrows(
                IllegalArgumentException.class,
                () -> XdsTimeouts.nanos(Duration.newBuilder().setSeconds(-1).build()));
        assertThrows(
                IllegalArgumentException.class,
                () -> XdsTimeouts.nanos(
                        Duration.newBuilder().setNanos(1000000000).build()));
        assertThrows(
                IllegalArgumentException.class,
                () -> XdsTimeouts.nanos(
                        Duration.newBuilder().setSeconds(315576000001L).build()));
        assertEquals(
                Long.MAX_VALUE,
                XdsTimeouts.nanos(
                        Duration.newBuilder().setSeconds(315576000000L).build()));
    }

    private static long limit(RouteAction.Builder action, long fallback) {
        XdsRouteTable table = new XdsRouteTable(RouteConfiguration.newBuilder()
                .addVirtualHosts(VirtualHost.newBuilder()
                        .addDomains("*")
                        .addRoutes(Route.newBuilder()
                                .setMatch(RouteMatch.newBuilder().setPrefix("/"))
                                .setRoute(action)))
                .build());
        return table.match(Collections.singletonMap(":path", "/service/hello")).getTimeoutNanos(fallback);
    }
}
