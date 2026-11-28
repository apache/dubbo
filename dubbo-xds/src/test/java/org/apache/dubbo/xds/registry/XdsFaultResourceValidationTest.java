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
package org.apache.dubbo.xds.registry;

import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.registry.XdsRetryTest.Fixture;
import org.apache.dubbo.xds.registry.XdsRetryTest.Request;
import org.apache.dubbo.xds.security.NativeTlsAdapter;
import org.apache.dubbo.xds.traffic.XdsFaultFilters;
import org.apache.dubbo.xds.traffic.XdsFaultPolicy;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import com.google.protobuf.Any;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.AggregatedConfigSource;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.ApiVersion;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.ConfigSource;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Listener;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.fault.v3.FaultAbort;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.fault.v3.HTTPFault;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.Rds;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.v3.FractionalPercent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsFaultResourceValidationTest {
    @Test
    void separateRdsAcceptsFaultOverridesAndNacksMalformedUpdatesKeepingTheActivePolicy() throws Exception {
        try (Fixture f = new Fixture()) {
            XdsEndpoint endpoint = XdsFaultExecutionTest.endpoint(f);
            Listener original = endpoint.getSnapshot().getListener();
            RouteConfiguration baseline = f.ads.routeOverride;
            RouteConfiguration valid = baseline.toBuilder()
                    .setVirtualHosts(
                            0,
                            baseline.getVirtualHosts(0).toBuilder()
                                    .setRoutes(
                                            0,
                                            baseline.getVirtualHosts(0).getRoutes(0).toBuilder()
                                                    .putTypedPerFilterConfig(
                                                            "fault", Any.pack(XdsFaultExecutionTest.abort(14)))))
                    .build();
            f.ads.routeResources.put(valid.getName(), valid);
            HttpConnectionManager hcm =
                    original.getApiListener().getApiListener().unpack(HttpConnectionManager.class);
            HttpConnectionManager separate = hcm.toBuilder()
                    .clearRouteConfig()
                    .setRds(Rds.newBuilder()
                            .setRouteConfigName(valid.getName())
                            .setConfigSource(ConfigSource.newBuilder()
                                    .setAds(AggregatedConfigSource.getDefaultInstance())
                                    .setResourceApiVersion(ApiVersion.V3)))
                    .clearHttpFilters()
                    .addHttpFilters(HttpFilter.newBuilder()
                            .setName("fault")
                            .setTypedConfig(Any.pack(HTTPFault.getDefaultInstance())))
                    .addAllHttpFilters(hcm.getHttpFiltersList())
                    .build();
            f.ads.listenerOverrides.put(
                    original.getName(),
                    original.toBuilder()
                            .setApiListener(
                                    original.getApiListener().toBuilder().setApiListener(Any.pack(separate)))
                            .build());
            f.ads.send(XdsResourceType.LDS.getTypeUrl());
            await(() -> valid.equals(endpoint.getSnapshot().getRoutes().get(valid.getName())));
            XdsFaultExecutionTest.status(TriRpcStatus.Code.UNAVAILABLE, f.remote.call(new Request("active")));
            HTTPFault malformed = HTTPFault.newBuilder()
                    .setAbort(FaultAbort.newBuilder()
                            .setPercentage(FractionalPercent.newBuilder().setNumerator(100)))
                    .build();
            RouteConfiguration invalid = valid.toBuilder()
                    .setVirtualHosts(
                            0,
                            valid.getVirtualHosts(0).toBuilder()
                                    .setRoutes(
                                            0,
                                            valid.getVirtualHosts(0).getRoutes(0).toBuilder()
                                                    .putTypedPerFilterConfig("fault", Any.pack(malformed))))
                    .build();
            int rejected = f.ads.rejected.get();
            f.ads.routeResources.put(invalid.getName(), invalid);
            f.ads.send(XdsResourceType.RDS.getTypeUrl());
            await(() -> f.ads.rejected.get() > rejected
                    && f.ads.lastRejection != null
                    && f.ads.lastRejection.contains(XdsResourceType.RDS.getTypeUrl()));
            assertEquals(valid, endpoint.getSnapshot().getRoutes().get(valid.getName()));
            XdsFaultExecutionTest.status(TriRpcStatus.Code.UNAVAILABLE, f.remote.call(new Request("last-good")));
            assertEquals(0, f.blue.calls.get());
            f.ads.routeResources.put(baseline.getName(), baseline);
            f.ads.send(XdsResourceType.RDS.getTypeUrl());
            await(() -> baseline.equals(endpoint.getSnapshot().getRoutes().get(baseline.getName())));
            assertEquals(
                    "blue:recovered", f.remote.call(new Request("recovered")).get(1, TimeUnit.SECONDS));
            await(() -> XdsFaultPolicy.activeFaults() == 0);
        }
    }

    @Test
    void malformedGlobalFaultAndUnknownMandatoryFiltersNackWithoutDiscardingLastGoodRpcPolicy() throws Exception {
        try (Fixture f = new Fixture()) {
            XdsEndpoint endpoint = NativeTlsAdapter.endpoint(
                    f.directory.getAllInvokers().get(0).getUrl());
            Listener original = endpoint.getSnapshot().getListener();
            HttpConnectionManager baseline =
                    original.getApiListener().getApiListener().unpack(HttpConnectionManager.class);
            HTTPFault active = HTTPFault.newBuilder()
                    .setAbort(FaultAbort.newBuilder()
                            .setPercentage(FractionalPercent.newBuilder().setNumerator(100)))
                    .build();
            HttpConnectionManager configured = baseline.toBuilder()
                    .clearHttpFilters()
                    .addHttpFilters(HttpFilter.newBuilder().setName("fault").setTypedConfig(Any.pack(active)))
                    .addAllHttpFilters(baseline.getHttpFiltersList())
                    .build();
            reject(f, original, configured, "Fault abort requires an error type");
            assertEquals(
                    "blue:after-fault-nack",
                    f.remote.call(new Request("after-fault-nack")).get(1, TimeUnit.SECONDS));
            HttpConnectionManager mandatory = baseline.toBuilder()
                    .clearHttpFilters()
                    .addHttpFilters(HttpFilter.newBuilder()
                            .setName("authentication")
                            .setTypedConfig(Any.newBuilder().setTypeUrl("type.googleapis.com/example.Authentication")))
                    .addAllHttpFilters(baseline.getHttpFiltersList())
                    .build();
            reject(f, original, mandatory, "Unsupported mandatory outbound HTTP filter");
            assertEquals(
                    "blue:after-filter-nack",
                    f.remote.call(new Request("after-filter-nack")).get(1, TimeUnit.SECONDS));
            assertEquals(2, f.blue.calls.get());
            assertEquals(0, f.green.calls.get());
        }
    }

    private static void reject(Fixture f, Listener original, HttpConnectionManager update, String message)
            throws Exception {
        IllegalArgumentException validation =
                assertThrows(IllegalArgumentException.class, () -> new XdsFaultFilters(update));
        assertTrue(validation.getMessage().contains(message));
        int previous = f.ads.rejected.get();
        f.ads.lastRejection = null;
        f.ads.listenerOverrides.put(
                original.getName(),
                original.toBuilder()
                        .setApiListener(original.getApiListener().toBuilder().setApiListener(Any.pack(update)))
                        .build());
        f.ads.send(XdsResourceType.LDS.getTypeUrl());
        // The ADS error intentionally exposes only type/category, never raw configuration or credentials.
        await(() -> f.ads.rejected.get() > previous
                && f.ads.lastRejection != null
                && f.ads.lastRejection.contains(XdsResourceType.LDS.getTypeUrl()));
        assertEquals(
                original,
                NativeTlsAdapter.endpoint(f.directory.getAllInvokers().get(0).getUrl())
                        .getSnapshot()
                        .getListener());
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(10);
        assertTrue(condition.getAsBoolean());
    }
}
