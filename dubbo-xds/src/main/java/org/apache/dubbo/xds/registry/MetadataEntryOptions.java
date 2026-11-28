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

import org.apache.dubbo.common.URL;

import java.util.Objects;

/** An explicit metadata application is independent of the Kubernetes Service name. */
final class MetadataEntryOptions {
    final String application;
    final String group;
    final String protocol;
    final int timeoutMillis;
    final DiscoveryTarget target;

    MetadataEntryOptions(URL reference, URL registry, DiscoveryTarget target) {
        this(new MetadataEntryConfig(reference, registry), target);
    }

    MetadataEntryOptions(MetadataEntryConfig config, DiscoveryTarget target) {
        application =
                config.parameter(target, "application", target.getHostname().split("\\.")[0]);
        group = config.parameter(target, "group", application);
        protocol = config.parameter(target, "protocol", target.getProtocol());
        timeoutMillis = Integer.parseInt(config.parameter(target, "timeout", "3000"));
        String port = config.parameter(target, "service-port", null);
        if (!protocol.equals(target.getProtocol()) && port == null) {
            throw new IllegalArgumentException(
                    "A different metadata protocol requires explicit xds.metadata.service-port");
        }
        String[] labels = target.getHostname().split("\\.");
        String namespace = labels.length > 2 && "svc".equals(labels[2]) ? labels[1] : config.namespace;
        this.target = DiscoveryTarget.kubernetes(
                config.parameter(target, "service", target.getHostname()),
                namespace,
                config.domain,
                port == null ? target.getServicePort() : Integer.parseInt(port),
                protocol);
        if (application.trim().isEmpty() || group.trim().isEmpty() || timeoutMillis < 1) {
            throw new IllegalArgumentException("Metadata requires an application, group and a positive timeout");
        }
    }

    static String parameter(URL reference, URL registry, String key, String fallback) {
        return reference.getParameter(key, registry.getParameter(key, fallback));
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MetadataEntryOptions)) {
            return false;
        }
        MetadataEntryOptions value = (MetadataEntryOptions) other;
        return application.equals(value.application)
                && group.equals(value.group)
                && protocol.equals(value.protocol)
                && target.equals(value.target)
                && timeoutMillis == value.timeoutMillis;
    }

    @Override
    public int hashCode() {
        return Objects.hash(application, group, protocol, target, timeoutMillis);
    }
}
