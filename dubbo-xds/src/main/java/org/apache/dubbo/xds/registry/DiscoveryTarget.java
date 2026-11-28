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

import java.util.Locale;
import java.util.Objects;

/** A service port is part of the discovery identity; it is not necessarily an endpoint's workload port. */
public final class DiscoveryTarget {
    private final String hostname;
    private final int servicePort;
    private final String protocol;

    public DiscoveryTarget(String hostname, int servicePort, String protocol) {
        String normalized = Objects.requireNonNull(hostname, "service hostname").toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()
                || normalized.length() > 253
                || !normalized.matches("[a-z0-9][a-z0-9.-]*[a-z0-9]|[a-z0-9]")) {
            throw new IllegalArgumentException("Invalid service hostname");
        }
        for (String label : normalized.split("\\.", -1)) {
            if (label.length() > 63 || !label.matches("[a-z0-9]([a-z0-9-]*[a-z0-9])?")) {
                throw new IllegalArgumentException("Invalid service hostname label");
            }
        }
        if (servicePort < 1 || servicePort > 65535) {
            throw new IllegalArgumentException("An explicit service port is required");
        }
        if (!"tri".equals(protocol) && !"dubbo".equals(protocol)) {
            throw new IllegalArgumentException("xDS supports tri and dubbo discovery targets");
        }
        this.hostname = normalized;
        this.servicePort = servicePort;
        this.protocol = protocol;
    }

    public static DiscoveryTarget kubernetes(
            String service, String namespace, String domain, int port, String protocol) {
        String fqdn;
        String[] labels = service.split("\\.");
        if (labels.length == 1) {
            if (namespace == null || namespace.isEmpty()) {
                throw new IllegalArgumentException("A namespace is required for a short Kubernetes Service name");
            }
            fqdn = service + "." + namespace + ".svc." + domain;
        } else if (labels.length == 2) {
            fqdn = service + ".svc." + domain;
        } else if (labels.length == 3 && "svc".equals(labels[2])) {
            fqdn = service + "." + domain;
        } else {
            fqdn = service;
        }
        return new DiscoveryTarget(fqdn, port, protocol);
    }

    public static DiscoveryTarget parse(String key) {
        int scheme = key.indexOf("://");
        int port = key.lastIndexOf(':');
        if (scheme < 0 || port <= scheme + 3) {
            throw new IllegalArgumentException("Invalid discovery target");
        }
        return new DiscoveryTarget(
                key.substring(scheme + 3, port), Integer.parseInt(key.substring(port + 1)), key.substring(0, scheme));
    }

    public String getHostname() {
        return hostname;
    }

    public int getServicePort() {
        return servicePort;
    }

    public String getProtocol() {
        return protocol;
    }

    public String authority() {
        return hostname + ":" + servicePort;
    }

    public String defaultCluster() {
        return "outbound|" + servicePort + "||" + hostname;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DiscoveryTarget)) {
            return false;
        }
        DiscoveryTarget target = (DiscoveryTarget) other;
        return servicePort == target.servicePort
                && hostname.equals(target.hostname)
                && protocol.equals(target.protocol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, servicePort, protocol);
    }

    @Override
    public String toString() {
        return protocol + "://" + authority();
    }
}
