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
import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import java.io.IOException;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Any;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Address;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.SocketAddress.PortSpecifierCase;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.AdditionalAddress;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Filter;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.FilterChain;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Listener;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.rbac.v3.RBAC;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.router.v3.Router;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter;

/** Supported grpcgen inbound layout, validated in full before it becomes callable. */
public final class InboundPolicy {
    private final int port;
    private final TlsPolicy tls;
    private final RbacPolicy rbac;
    private volatile Credentials cachedCredentials;

    private InboundPolicy(int port, TlsPolicy tls, RbacPolicy rbac) {
        this.port = port;
        this.tls = tls;
        this.rbac = rbac;
    }

    public static InboundPolicy parse(Listener listener, BootstrapInfo bootstrap) {
        try {
            SecurityFields.only(listener, "name", "address", "filter_chains", "additional_addresses");
            if (!listener.getAddress().hasSocketAddress() || listener.getFilterChainsCount() != 1) {
                throw new IllegalArgumentException("Native inbound LDS requires one socket-address filter chain");
            }
            int port = socketPort(listener.getAddress());
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Invalid inbound LDS port");
            }
            String host = listener.getAddress().getSocketAddress().getAddress();
            String address = (host.contains(":") ? "[" + host + "]" : host) + ":" + port;
            if (!listener.getName().equals(bootstrap.serverListenerName(address))) {
                throw new IllegalArgumentException("Inbound listener name does not match its socket address");
            }
            for (AdditionalAddress additional : listener.getAdditionalAddressesList()) {
                SecurityFields.only(additional, "address");
                if (socketPort(additional.getAddress()) != port) {
                    throw new IllegalArgumentException("Inbound additional address has a different port");
                }
            }
            FilterChain chain = listener.getFilterChains(0);
            SecurityFields.only(chain, "name", "filters", "transport_socket", "filter_chain_match");
            SecurityFields.only(chain.getFilterChainMatch());
            if (chain.hasFilterChainMatch()
                    && !chain.getFilterChainMatch().getAllFields().isEmpty()) {
                throw new IllegalArgumentException("Inbound filter-chain matching is unsupported");
            }
            if (chain.getFiltersCount() != 1) {
                throw new IllegalArgumentException("Inbound listener requires one HCM");
            }
            Filter filter = chain.getFilters(0);
            SecurityFields.only(filter, "name", "typed_config");
            HttpConnectionManager hcm = filter.getTypedConfig().unpack(HttpConnectionManager.class);
            SecurityFields.only(hcm, "stat_prefix", "route_config", "http_filters");
            routes(hcm.getRouteConfig());
            List<io.grpc.xds.shaded.io.envoyproxy.envoy.config.rbac.v3.RBAC> rules = new ArrayList<>();
            boolean router = false;
            for (int i = 0; i < hcm.getHttpFiltersCount(); i++) {
                HttpFilter http = hcm.getHttpFilters(i);
                SecurityFields.only(http, "name", "typed_config", "is_optional");
                Any config = http.getTypedConfig();
                if (config.is(RBAC.class)) {
                    RBAC rbac = config.unpack(RBAC.class);
                    SecurityFields.only(
                            rbac,
                            "rules",
                            "rules_stat_prefix",
                            "shadow_rules",
                            "shadow_rules_stat_prefix",
                            "track_per_rule_stats");
                    if (rbac.hasRules()) {
                        rules.add(rbac.getRules());
                    }
                } else if (config.is(Router.class)) {
                    SecurityFields.only(config.unpack(Router.class));
                    if (router || i != hcm.getHttpFiltersCount() - 1) {
                        throw new IllegalArgumentException("Inbound router must be unique and last");
                    }
                    router = true;
                } else if (!http.getIsOptional()) {
                    throw new IllegalArgumentException("Unsupported mandatory inbound HTTP filter: " + http.getName());
                }
            }
            if (!router) {
                throw new IllegalArgumentException("Inbound listener has no terminal router");
            }
            return new InboundPolicy(port, TlsPolicy.server(chain, bootstrap), new RbacPolicy(rules));
        } catch (IOException error) {
            throw new IllegalArgumentException("Invalid inbound security resource", error);
        }
    }

    private static int socketPort(Address address) {
        SecurityFields.only(address, "socket_address");
        if (!address.hasSocketAddress()) {
            throw new IllegalArgumentException("Inbound listener requires a socket address");
        }
        io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.SocketAddress socket = address.getSocketAddress();
        SecurityFields.only(socket, "address", "port_value", "protocol");
        if (socket.getProtocolValue() != 0
                || socket.getAddress().isEmpty()
                || socket.getPortSpecifierCase() != PortSpecifierCase.PORT_VALUE) {
            throw new IllegalArgumentException("Inbound listener requires an explicit TCP address and port");
        }
        return socket.getPortValue();
    }

    private static void routes(RouteConfiguration routes) {
        SecurityFields.only(routes, "name", "virtual_hosts");
        if (routes.getVirtualHostsCount() != 1) {
            throw new IllegalArgumentException("Unsupported inbound virtual hosts");
        }
        VirtualHost host = routes.getVirtualHosts(0);
        SecurityFields.only(host, "name", "domains", "routes");
        if (host.getDomainsCount() != 1 || !"*".equals(host.getDomains(0)) || host.getRoutesCount() != 1) {
            throw new IllegalArgumentException("Inbound grpcgen requires a default route");
        }
        Route route = host.getRoutes(0);
        SecurityFields.only(route, "name", "match", "non_forwarding_action");
        SecurityFields.only(route.getMatch(), "prefix", "case_sensitive");
        SecurityFields.only(route.getNonForwardingAction());
        if (!route.hasNonForwardingAction() || !"/".equals(route.getMatch().getPrefix())) {
            throw new IllegalArgumentException("Unsupported inbound action or route match");
        }
    }

    public int getPort() {
        return port;
    }

    public TlsPolicy getTls() {
        return tls;
    }

    NativeCertificate credentials(XdsSecurityRuntime runtime) {
        Credentials cached = cachedCredentials;
        if (cached == null || cached.epoch != runtime.getEpoch() || cached.runtime != runtime) {
            synchronized (this) {
                cached = cachedCredentials;
                if (cached == null || cached.epoch != runtime.getEpoch() || cached.runtime != runtime) {
                    cached = new Credentials(runtime, runtime.getEpoch(), runtime.certificate(tls));
                    cachedCredentials = cached;
                }
            }
        }
        cached.certificate.checkValidity();
        return cached.certificate;
    }

    public boolean permits(
            XdsSecurityRuntime runtime,
            HttpHeaders headers,
            SocketAddress local,
            SocketAddress remote,
            SSLSession session) {
        if (tls.isTls()) {
            if (session == null) {
                return false;
            }
            NativeCertificate credentials = credentials(runtime);
            try {
                Certificate[] peer = session.getPeerCertificates();
                X509Certificate[] chain = new X509Certificate[peer.length];
                for (int i = 0; i < peer.length; i++) {
                    if (!(peer[i] instanceof X509Certificate)) {
                        return false;
                    }
                    chain[i] = (X509Certificate) peer[i];
                }
                if (chain.length == 0) {
                    return !tls.isClientCertificateRequired()
                            && rbac.permits(new RbacRequest(headers, local, remote, null));
                }
                // Check new RPCs against current roots/SANs, even on a connection accepted under an older policy.
                X509TrustManager trust =
                        (X509TrustManager) credentials.getTrustManagerFactory().getTrustManagers()[0];
                trust.checkClientTrusted(chain, chain[0].getPublicKey().getAlgorithm());
            } catch (SSLPeerUnverifiedException noPeer) {
                if (tls.isClientCertificateRequired()) {
                    return false;
                }
            } catch (Exception invalid) {
                return false;
            }
        } else if (session != null) {
            return false;
        }
        return rbac.permits(new RbacRequest(headers, local, remote, session));
    }

    private static final class Credentials {
        final XdsSecurityRuntime runtime;
        final long epoch;
        final NativeCertificate certificate;

        Credentials(XdsSecurityRuntime runtime, long epoch, NativeCertificate certificate) {
            this.runtime = runtime;
            this.epoch = epoch;
            this.certificate = certificate;
        }
    }
}
