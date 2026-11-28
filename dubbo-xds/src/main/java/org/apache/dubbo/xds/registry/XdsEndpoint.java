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

import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;
import org.apache.dubbo.xds.security.NativeCertificate;
import org.apache.dubbo.xds.security.TlsPolicy;
import org.apache.dubbo.xds.security.XdsSecurityRuntime;

import java.util.Objects;
import java.util.function.Supplier;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;

/** Keeps the cluster/security/locality identity attached to the address throughout Dubbo adaptation. */
public final class XdsEndpoint {
    public static final String ATTRIBUTE = "dubbo.xds.endpoint";
    private final Snapshot snapshot;
    private final Supplier<Snapshot> routingSnapshot;
    private final Cluster cluster;
    private final LocalityLbEndpoints locality;
    private final LbEndpoint endpoint;
    private final XdsSecurityRuntime security;
    private final long securityEpoch;
    private volatile NativeCertificate certificate;

    public XdsEndpoint(Snapshot snapshot, Cluster cluster, LocalityLbEndpoints locality, LbEndpoint endpoint) {
        this(snapshot, cluster, locality, endpoint, null);
    }

    public XdsEndpoint(
            Snapshot snapshot,
            Cluster cluster,
            LocalityLbEndpoints locality,
            LbEndpoint endpoint,
            XdsSecurityRuntime security) {
        this(snapshot, cluster, locality, endpoint, security, () -> snapshot);
    }

    public XdsEndpoint(
            Snapshot snapshot,
            Cluster cluster,
            LocalityLbEndpoints locality,
            LbEndpoint endpoint,
            XdsSecurityRuntime security,
            Supplier<Snapshot> routingSnapshot) {
        this.snapshot = Objects.requireNonNull(snapshot);
        this.routingSnapshot = Objects.requireNonNull(routingSnapshot);
        this.cluster = Objects.requireNonNull(cluster);
        this.locality = Objects.requireNonNull(locality);
        this.endpoint = Objects.requireNonNull(endpoint);
        this.security = security;
        securityEpoch = security == null ? 0 : security.getEpoch();
    }

    public XdsSecurityRuntime getSecurity() {
        return security;
    }

    public synchronized NativeCertificate getCertificate() {
        if (security == null) {
            throw new IllegalStateException("Endpoint has no native xDS security runtime");
        }
        if (certificate == null) {
            TlsPolicy policy = TlsPolicy.client(cluster, security.getBootstrap());
            if (policy.isTls() && !"tri".equals(getTarget().getProtocol())) {
                throw new IllegalStateException("Native xDS TLS requires Triple; binary Dubbo supports discovery only");
            }
            certificate = security.certificate(policy);
        }
        certificate.checkValidity();
        return certificate;
    }

    public DiscoveryTarget getTarget() {
        return snapshot.getTarget();
    }

    public Snapshot getSnapshot() {
        return routingSnapshot.get();
    }

    public Cluster getCluster() {
        return cluster;
    }

    public LocalityLbEndpoints getLocality() {
        return locality;
    }

    public LbEndpoint getEndpoint() {
        return endpoint;
    }

    public String getHost() {
        return endpoint.getEndpoint().getAddress().getSocketAddress().getAddress();
    }

    public int getPort() {
        return endpoint.getEndpoint().getAddress().getSocketAddress().getPortValue();
    }

    public String identity() {
        return getTarget() + "/" + cluster.getName() + "/" + locality.getPriority() + "/" + locality.getLocality() + "/"
                + getHost() + ":" + getPort() + "/security-" + securityEpoch;
    }

    /** Traffic-only CDS changes do not change the point-to-point metadata identity or its credentials. */
    public boolean hasSameMetadataContext(XdsEndpoint other) {
        return other != null
                && identity().equals(other.identity())
                && cluster.getType() == other.cluster.getType()
                && cluster.getEdsClusterConfig()
                        .getServiceName()
                        .equals(other.cluster.getEdsClusterConfig().getServiceName())
                && cluster.getTransportSocket().equals(other.cluster.getTransportSocket())
                && cluster.getTransportSocketMatchesList().equals(other.cluster.getTransportSocketMatchesList());
    }

    @Override
    public boolean equals(Object value) {
        if (!(value instanceof XdsEndpoint)) {
            return false;
        }
        XdsEndpoint other = (XdsEndpoint) value;
        return getTarget().equals(other.getTarget())
                && securityEpoch == other.securityEpoch
                && cluster.equals(other.cluster)
                && endpoint.equals(other.endpoint)
                && locality.getPriority() == other.locality.getPriority()
                && locality.getLocality().equals(other.locality.getLocality())
                && locality.getLoadBalancingWeight().equals(other.locality.getLoadBalancingWeight());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getTarget(),
                cluster,
                endpoint,
                securityEpoch,
                locality.getPriority(),
                locality.getLocality(),
                locality.getLoadBalancingWeight());
    }
}
