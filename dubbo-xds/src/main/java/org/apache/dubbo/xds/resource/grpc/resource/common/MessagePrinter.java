/*
 * Copyright 2020 The gRPC Authors
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

package org.apache.dubbo.xds.resource.grpc.resource.common;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.envoyproxy.envoy.extensions.clusters.aggregate.v3.ClusterConfig;
import io.envoyproxy.envoy.extensions.filters.http.fault.v3.HTTPFault;
import io.envoyproxy.envoy.extensions.filters.http.rbac.v3.RBAC;
import io.envoyproxy.envoy.extensions.filters.http.rbac.v3.RBACPerRoute;
import io.envoyproxy.envoy.extensions.filters.http.router.v3.Router;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.DownstreamTlsContext;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.UpstreamTlsContext;

/**
 * Converts protobuf message to human readable String format. Useful for protobuf messages containing
 * {@link com.google.protobuf.Any} fields.
 */
public final class MessagePrinter {

    private MessagePrinter() {}

    // The initialization-on-demand holder idiom.
    private static class LazyHolder {
        static final JsonFormat.Printer printer = newPrinter();

        private static JsonFormat.Printer newPrinter() {
            TypeRegistry.Builder registry = TypeRegistry.newBuilder()
                    .add(Listener.getDescriptor())
                    .add(io.envoyproxy.envoy.api.v2.Listener.getDescriptor())
                    .add(HttpConnectionManager.getDescriptor())
                    .add(io.envoyproxy.envoy.config.filter.network.http_connection_manager.v2.HttpConnectionManager.getDescriptor())
                    .add(HTTPFault.getDescriptor())
                    .add(io.envoyproxy.envoy.config.filter.http.fault.v2.HTTPFault.getDescriptor())
                    .add(RBAC.getDescriptor())
                    .add(RBACPerRoute.getDescriptor())
                    .add(Router.getDescriptor())
                    .add(io.envoyproxy.envoy.config.filter.http.router.v2.Router.getDescriptor())
                    // UpstreamTlsContext and DownstreamTlsContext in v3 are not transitively imported
                    // by top-level resource types.
                    .add(UpstreamTlsContext.getDescriptor())
                    .add(DownstreamTlsContext.getDescriptor())
                    .add(RouteConfiguration.getDescriptor())
                    .add(io.envoyproxy.envoy.api.v2.RouteConfiguration.getDescriptor())
                    .add(Cluster.getDescriptor())
                    .add(io.envoyproxy.envoy.api.v2.Cluster.getDescriptor())
                    .add(ClusterConfig.getDescriptor())
                    .add(io.envoyproxy.envoy.config.cluster.aggregate.v2alpha.ClusterConfig.getDescriptor())
                    .add(ClusterLoadAssignment.getDescriptor())
                    .add(io.envoyproxy.envoy.api.v2.ClusterLoadAssignment.getDescriptor());
            try {
                @SuppressWarnings("unchecked") Class<? extends Message> routeLookupClusterSpecifierClass = (Class<?
                        extends Message>) Class.forName("io.grpc.lookup.v1.RouteLookupClusterSpecifier");
                Descriptor descriptor = (Descriptor) routeLookupClusterSpecifierClass.getDeclaredMethod("getDescriptor")
                        .invoke(null);
                registry.add(descriptor);
            } catch (Exception e) {
                // Ignore. In most cases RouteLookup is not required.
            }
            return JsonFormat.printer()
                    .usingTypeRegistry(registry.build());
        }
    }

    public static String print(MessageOrBuilder message) {
        String res;
        try {
            res = LazyHolder.printer.print(message);
        } catch (InvalidProtocolBufferException e) {
            res = message + " (failed to pretty-print: " + e + ")";
        }
        return res;
    }
}
