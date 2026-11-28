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
import org.apache.dubbo.xds.traffic.XdsFaultPolicy.Decision;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.TextFormat;
import com.google.protobuf.UInt32Value;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.FilterConfig;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.common.fault.v3.FaultDelay;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.fault.v3.FaultAbort;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.fault.v3.HTTPFault;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.router.v3.Router;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.v3.FractionalPercent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsFaultPolicyTest {
    @AfterEach
    void permitsAreReleased() {
        assertEquals(0, XdsFaultPolicy.activeFaults());
    }

    @Test
    void eachDenominatorHasTheSameBoundaryAndUnknownDenominatorsAreRejected() {
        for (FractionalPercent.DenominatorType denominator : Arrays.asList(
                FractionalPercent.DenominatorType.HUNDRED,
                FractionalPercent.DenominatorType.TEN_THOUSAND,
                FractionalPercent.DenominatorType.MILLION)) {
            int numerator = denominator == FractionalPercent.DenominatorType.HUNDRED
                    ? 10
                    : denominator == FractionalPercent.DenominatorType.TEN_THOUSAND ? 1000 : 100_000;
            HTTPFault config = abort(14).toBuilder()
                    .setAbort(abort(14).getAbort().toBuilder()
                            .setPercentage(FractionalPercent.newBuilder()
                                    .setDenominator(denominator)
                                    .setNumerator(numerator)))
                    .build();
            XdsFaultPolicy policy = XdsFaultPolicy.parse(config);
            try (Decision below = policy.decide(Collections.emptyMap(), ignored -> 99_999);
                    Decision boundary = policy.decide(Collections.emptyMap(), ignored -> 100_000)) {
                assertTrue(below.isInjected());
                assertFalse(boundary.isInjected());
            }
        }
        HTTPFault unknown = abort(14).toBuilder()
                .setAbort(abort(14).getAbort().toBuilder()
                        .setPercentage(FractionalPercent.newBuilder()
                                .setDenominatorValue(99)
                                .setNumerator(10)))
                .build();
        assertThrows(IllegalArgumentException.class, () -> XdsFaultPolicy.parse(unknown));
    }

    @Test
    void independentDelayAndAbortSamplesUseFractionBoundariesAndUnsignedClamping() {
        HTTPFault config = HTTPFault.newBuilder()
                .setDelay(FaultDelay.newBuilder()
                        .setFixedDelay(Duration.newBuilder().setNanos(20_000_000))
                        .setPercentage(percent(10)))
                .setAbort(FaultAbort.newBuilder().setGrpcStatus(14).setPercentage(percent(10)))
                .build();
        XdsFaultPolicy policy = XdsFaultPolicy.parse(config);
        AtomicInteger sample = new AtomicInteger();
        try (Decision selected =
                policy.decide(Collections.emptyMap(), ignored -> sample.getAndIncrement() == 0 ? 99_999 : 100_000)) {
            assertTrue(selected.isInjected());
            assertEquals(20_000_000, selected.getDelayNanos());
            assertNull(selected.getAbort());
            assertEquals(2, sample.get());
        }
        HTTPFault clamped = config.toBuilder()
                .setAbort(config.getAbort().toBuilder().setPercentage(percent(-1)))
                .clearDelay()
                .build();
        try (Decision selected = XdsFaultPolicy.parse(clamped).decide(Collections.emptyMap(), ignored -> 999_999)) {
            assertEquals(TriRpcStatus.Code.UNAVAILABLE, selected.getAbort().code);
        }
        try (Decision none = policy.decide(Collections.emptyMap(), ignored -> 100_000)) {
            assertFalse(none.isInjected());
        }
    }

    @Test
    void headersOnlyApplyToExplicitHeaderPoliciesAndPercentagesCannotRaiseTheCap() {
        HTTPFault header = HTTPFault.newBuilder()
                .setDelay(FaultDelay.newBuilder()
                        .setHeaderDelay(FaultDelay.HeaderDelay.getDefaultInstance())
                        .setPercentage(percent(10)))
                .setAbort(FaultAbort.newBuilder()
                        .setHeaderAbort(FaultAbort.HeaderAbort.getDefaultInstance())
                        .setPercentage(percent(10)))
                .build();
        XdsFaultPolicy policy = XdsFaultPolicy.parse(header);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("x-envoy-fault-delay-request", "53");
        metadata.put("x-envoy-fault-delay-request-percentage", "999");
        metadata.put("x-envoy-fault-abort-grpc-request", "3");
        metadata.put("x-envoy-fault-abort-request-percentage", "5");
        try (Decision selected = policy.decide(metadata, ignored -> 49_999)) {
            assertEquals(53_000_000, selected.getDelayNanos());
            assertEquals(TriRpcStatus.Code.INVALID_ARGUMENT, selected.getAbort().code);
        }
        try (Decision selected = policy.decide(metadata, ignored -> 50_000)) {
            assertEquals(53_000_000, selected.getDelayNanos());
            assertNull(selected.getAbort());
        }
        try (Decision none = policy.decide(metadata, ignored -> 100_000)) {
            assertFalse(none.isInjected(), "Header percentage must not exceed its configured cap");
        }
        HTTPFault fixed = abort(14).toBuilder()
                .setDelay(FaultDelay.newBuilder()
                        .setFixedDelay(Duration.newBuilder().setNanos(7))
                        .setPercentage(percent(100)))
                .build();
        metadata.put("x-envoy-fault-abort-request-percentage", "0");
        try (Decision selected = XdsFaultPolicy.parse(fixed).decide(metadata, ignored -> 0)) {
            assertEquals(7, selected.getDelayNanos());
            assertEquals(TriRpcStatus.Code.UNAVAILABLE, selected.getAbort().code);
        }
    }

    @Test
    void invalidHeadersDisableOnlyTheirFeatureAndHttpStatusOverridesGrpcAsInGrpcJava() {
        HTTPFault config = HTTPFault.newBuilder()
                .setDelay(FaultDelay.newBuilder()
                        .setHeaderDelay(FaultDelay.HeaderDelay.getDefaultInstance())
                        .setPercentage(percent(100)))
                .setAbort(FaultAbort.newBuilder()
                        .setHeaderAbort(FaultAbort.HeaderAbort.getDefaultInstance())
                        .setPercentage(percent(100)))
                .build();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("x-envoy-fault-delay-request", "-1");
        metadata.put("x-envoy-fault-abort-grpc-request", "3");
        metadata.put("x-envoy-fault-abort-request", "503");
        try (Decision selected = XdsFaultPolicy.parse(config).decide(metadata, ignored -> 0)) {
            assertEquals(0, selected.getDelayNanos());
            assertEquals(TriRpcStatus.Code.UNAVAILABLE, selected.getAbort().code);
        }
        metadata.put("x-envoy-fault-delay-request", "9");
        metadata.put("x-envoy-fault-abort-request-percentage", "invalid");
        try (Decision selected = XdsFaultPolicy.parse(config).decide(metadata, ignored -> 0)) {
            assertEquals(9_000_000, selected.getDelayNanos());
            assertNull(selected.getAbort());
        }
    }

    @Test
    void grpcOkIsARealAbortAndHttpOkOrInvalidGrpcCodesMapToUnknown() {
        for (int code : Arrays.asList(0, 999, -1)) {
            try (Decision selected = XdsFaultPolicy.parse(abort(code)).decide(Collections.emptyMap(), ignored -> 0)) {
                assertEquals(code == 0 ? TriRpcStatus.Code.OK : TriRpcStatus.Code.UNKNOWN, selected.getAbort().code);
            }
        }
        Map<Integer, TriRpcStatus.Code> mappings = new HashMap<>();
        mappings.put(200, TriRpcStatus.Code.UNKNOWN);
        mappings.put(400, TriRpcStatus.Code.INTERNAL);
        mappings.put(401, TriRpcStatus.Code.UNAUTHENTICATED);
        mappings.put(403, TriRpcStatus.Code.PERMISSION_DENIED);
        mappings.put(404, TriRpcStatus.Code.UNIMPLEMENTED);
        mappings.put(429, TriRpcStatus.Code.UNAVAILABLE);
        mappings.put(503, TriRpcStatus.Code.UNAVAILABLE);
        mappings.forEach((code, expected) -> {
            HTTPFault config = HTTPFault.newBuilder()
                    .setAbort(FaultAbort.newBuilder().setHttpStatus(code).setPercentage(percent(100)))
                    .build();
            try (Decision selected = XdsFaultPolicy.parse(config).decide(Collections.emptyMap(), ignored -> 0)) {
                assertEquals(expected, selected.getAbort().code);
            }
        });
    }

    @Test
    void maximumIsGlobalAcrossPoliciesAndConcurrentAdmissionReleasesExactlyOnce() throws Exception {
        HTTPFault config =
                abort(14).toBuilder().setMaxActiveFaults(UInt32Value.of(3)).build();
        XdsFaultPolicy first = XdsFaultPolicy.parse(config);
        XdsFaultPolicy second = XdsFaultPolicy.parse(config);
        List<Decision> held = new ArrayList<>();
        ExecutorService workers = Executors.newFixedThreadPool(8);
        try {
            List<Future<Decision>> jobs = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                XdsFaultPolicy policy = i % 2 == 0 ? first : second;
                jobs.add(workers.submit(() -> policy.decide(Collections.emptyMap(), ignored -> 0)));
            }
            for (Future<Decision> job : jobs) held.add(job.get(1, TimeUnit.SECONDS));
            assertEquals(3, held.stream().filter(Decision::isInjected).count());
            assertEquals(3, XdsFaultPolicy.activeFaults());
            held.forEach(decision -> {
                decision.close();
                decision.close();
            });
            assertEquals(0, XdsFaultPolicy.activeFaults());
            try (Decision zero = XdsFaultPolicy.parse(config.toBuilder()
                            .setMaxActiveFaults(UInt32Value.of(0))
                            .build())
                    .decide(Collections.emptyMap(), ignored -> 0)) {
                assertFalse(zero.isInjected());
            }
            try (Decision unsigned = XdsFaultPolicy.parse(config.toBuilder()
                            .setMaxActiveFaults(UInt32Value.of(-1))
                            .build())
                    .decide(Collections.emptyMap(), ignored -> 0)) {
                assertTrue(unsigned.isInjected());
            }
        } finally {
            held.forEach(Decision::close);
            workers.shutdownNow();
        }
    }

    @Test
    void filterInstanceOrderAndMostSpecificOverridesArePreserved() {
        HttpConnectionManager hcm = HttpConnectionManager.newBuilder()
                .addHttpFilters(filter("fault-1", Any.pack(abort(1))))
                .addHttpFilters(filter("fault-2", Any.pack(abort(2))))
                .addHttpFilters(filter("router", Any.pack(Router.getDefaultInstance())))
                .build();
        XdsFaultFilters filters = new XdsFaultFilters(hcm);
        Map<String, XdsFaultPolicy> host = override("fault-1", abort(3));
        Map<String, XdsFaultPolicy> route = override("fault-1", abort(4));
        Map<String, XdsFaultPolicy> cluster = override("fault-1", abort(5));
        cluster = new HashMap<>(cluster);
        cluster.putAll(override("unconfigured", abort(6)));
        assertEquals(
                Arrays.asList("fault-1", "fault-2"),
                new ArrayList<>(filters.effective(host, route, cluster).keySet()));
        assertCode(5, filters.effective(host, route, cluster).get("fault-1"));
        assertCode(4, filters.effective(host, route, Collections.emptyMap()).get("fault-1"));
        assertCode(
                3,
                filters.effective(host, Collections.emptyMap(), Collections.emptyMap())
                        .get("fault-1"));
        assertCode(
                1,
                filters.effective(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap())
                        .get("fault-1"));
        assertCode(2, filters.effective(host, route, cluster).get("fault-2"));
    }

    @Test
    void optionalUnknownTypesAreIgnoredButMalformedKnownAndMandatoryFiltersAreRejected() {
        Any unknown = Any.newBuilder()
                .setTypeUrl("type.googleapis.com/example.Unknown")
                .build();
        HttpFilter router = filter("router", Any.pack(Router.getDefaultInstance()));
        HttpConnectionManager optional = HttpConnectionManager.newBuilder()
                .addHttpFilters(filter("optional", unknown).toBuilder().setIsOptional(true))
                .addHttpFilters(router)
                .build();
        assertTrue(new XdsFaultFilters(optional).getPolicies().isEmpty());
        assertThrows(
                IllegalArgumentException.class,
                () -> new XdsFaultFilters(optional.toBuilder()
                        .setHttpFilters(
                                0, optional.getHttpFilters(0).toBuilder().setIsOptional(false))
                        .build()));
        assertThrows(
                IllegalArgumentException.class, () -> new XdsFaultFilters(HttpConnectionManager.getDefaultInstance()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new XdsFaultFilters(HttpConnectionManager.newBuilder()
                        .addHttpFilters(router)
                        .addHttpFilters(filter("fault", Any.pack(abort(1))))
                        .build()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new XdsFaultFilters(HttpConnectionManager.newBuilder()
                        .addHttpFilters(router)
                        .addHttpFilters(router)
                        .build()));
        Any wrapped = Any.pack(
                FilterConfig.newBuilder().setIsOptional(true).setConfig(unknown).build());
        assertTrue(XdsFaultFilters.overrides(Collections.singletonMap("optional", wrapped))
                .isEmpty());
        assertThrows(
                IllegalArgumentException.class,
                () -> XdsFaultFilters.overrides(Collections.singletonMap("mandatory", unknown)));
        HTTPFault invalid =
                HTTPFault.newBuilder().setAbort(FaultAbort.getDefaultInstance()).build();
        Any invalidKnown = Any.pack(FilterConfig.newBuilder()
                .setIsOptional(true)
                .setConfig(Any.pack(invalid))
                .build());
        assertThrows(
                IllegalArgumentException.class,
                () -> XdsFaultFilters.overrides(Collections.singletonMap("fault", invalidKnown)));
        assertThrows(
                IllegalArgumentException.class,
                () -> XdsFaultPolicy.parse(HTTPFault.newBuilder()
                        .setDelay(FaultDelay.newBuilder()
                                .setFixedDelay(Duration.newBuilder().setSeconds(-1)))
                        .build()));
        assertThrows(
                IllegalArgumentException.class,
                () -> XdsFaultPolicy.parse(
                        abort(14).toBuilder().setUpstreamCluster("select").build()));
    }

    @Test
    void realIstioRouteFaultParsesWithoutLosingItsPercentagesOrStatus() throws Exception {
        RouteConfiguration.Builder routes = RouteConfiguration.newBuilder();
        try (InputStreamReader reader = new InputStreamReader(
                getClass().getResourceAsStream("/istio-1.31.1/extension-policy/rds.textproto"),
                StandardCharsets.UTF_8)) {
            TextFormat.merge(reader, routes);
        }
        Map<String, XdsFaultPolicy> overrides =
                XdsFaultFilters.overrides(routes.getVirtualHosts(0).getRoutes(0).getTypedPerFilterConfigMap());
        assertEquals(Collections.singleton("envoy.filters.http.fault"), overrides.keySet());
        try (Decision selected =
                overrides.values().iterator().next().decide(Collections.emptyMap(), ignored -> 99_999)) {
            assertEquals(20_000_000, selected.getDelayNanos());
            assertEquals(TriRpcStatus.Code.UNAVAILABLE, selected.getAbort().code);
        }
    }

    private static void assertCode(int code, XdsFaultPolicy policy) {
        try (Decision selected = policy.decide(Collections.emptyMap(), ignored -> 0)) {
            assertEquals(TriRpcStatus.fromCode(code).code, selected.getAbort().code);
        }
    }

    private static Map<String, XdsFaultPolicy> override(String name, HTTPFault policy) {
        return XdsFaultFilters.overrides(Collections.singletonMap(name, Any.pack(policy)));
    }

    private static HttpFilter filter(String name, Any config) {
        return HttpFilter.newBuilder().setName(name).setTypedConfig(config).build();
    }

    private static HTTPFault abort(int grpc) {
        return HTTPFault.newBuilder()
                .setAbort(FaultAbort.newBuilder().setGrpcStatus(grpc).setPercentage(percent(100)))
                .build();
    }

    private static FractionalPercent percent(int numerator) {
        return FractionalPercent.newBuilder().setNumerator(numerator).build();
    }
}
