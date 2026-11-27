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

import org.apache.dubbo.xds.security.SecurityFields;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.FilterConfig;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.fault.v3.HTTPFault;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.router.v3.Router;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter;

/** A39 named filter validation and most-specific configuration selection. Activation requires call execution. */
public final class XdsFaultFilters {
    private final Map<String, XdsFaultPolicy> policies;

    private XdsFaultFilters() {
        policies = Collections.emptyMap();
    }

    public static XdsFaultFilters empty() {
        return new XdsFaultFilters();
    }

    public XdsFaultFilters(HttpConnectionManager hcm) {
        Map<String, XdsFaultPolicy> parsed = new LinkedHashMap<>();
        Set<String> names = new HashSet<>();
        boolean terminal = false;
        for (HttpFilter filter : hcm.getHttpFiltersList()) {
            if (filter.getName().isEmpty() || !names.add(filter.getName())) {
                throw new IllegalArgumentException("Empty or duplicate outbound HTTP filter name");
            }
            Any config = filter.getTypedConfig();
            if (!config.is(HTTPFault.class) && !config.is(Router.class)) {
                if (filter.getIsOptional()) continue;
                throw new IllegalArgumentException("Unsupported mandatory outbound HTTP filter: " + filter.getName());
            }
            SecurityFields.only(filter, "name", "typed_config", "is_optional");
            if (terminal) throw new IllegalArgumentException("Outbound router filter must be terminal");
            if (config.is(HTTPFault.class)) {
                parsed.put(filter.getName(), XdsFaultPolicy.parse(config));
            } else {
                try {
                    SecurityFields.only(config.unpack(Router.class));
                } catch (InvalidProtocolBufferException error) {
                    throw new IllegalArgumentException("Invalid outbound router filter", error);
                }
                terminal = true;
            }
        }
        if (!terminal) throw new IllegalArgumentException("Outbound HTTP filters require a terminal router");
        policies = Collections.unmodifiableMap(parsed);
    }

    public Map<String, XdsFaultPolicy> getPolicies() {
        return policies;
    }

    public static Map<String, XdsFaultPolicy> overrides(Map<String, Any> configs) {
        Map<String, XdsFaultPolicy> result = new LinkedHashMap<>();
        configs.forEach((name, raw) -> {
            if (name.isEmpty()) throw new IllegalArgumentException("Empty HTTP filter override name");
            Any config = raw;
            boolean optional = false;
            if (config.is(FilterConfig.class)) {
                try {
                    FilterConfig wrapper = config.unpack(FilterConfig.class);
                    SecurityFields.only(wrapper, "config", "is_optional");
                    config = wrapper.getConfig();
                    optional = wrapper.getIsOptional();
                } catch (InvalidProtocolBufferException error) {
                    throw new IllegalArgumentException("Invalid HTTP filter override", error);
                }
            }
            if (config.is(HTTPFault.class)) {
                result.put(name, XdsFaultPolicy.parse(config));
            } else if (config.is(Router.class) || !optional) {
                throw new IllegalArgumentException("Unsupported mandatory HTTP filter override: " + name);
            }
        });
        return Collections.unmodifiableMap(result);
    }

    public Map<String, XdsFaultPolicy> effective(
            Map<String, XdsFaultPolicy> virtualHost,
            Map<String, XdsFaultPolicy> route,
            Map<String, XdsFaultPolicy> weightedCluster) {
        Map<String, XdsFaultPolicy> selected = new LinkedHashMap<>();
        policies.forEach((name, base) -> selected.put(
                name,
                weightedCluster.containsKey(name)
                        ? weightedCluster.get(name)
                        : route.containsKey(name) ? route.get(name) : virtualHost.getOrDefault(name, base)));
        return Collections.unmodifiableMap(selected);
    }
}
