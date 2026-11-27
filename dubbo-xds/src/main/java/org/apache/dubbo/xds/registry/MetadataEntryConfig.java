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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;

/** Immutable per-destination metadata configuration; never keyed by application alone. */
final class MetadataEntryConfig {
    static final String TARGETS = "xds.metadata.targets";
    private static final Set<String> FIELDS = Collections.unmodifiableSet(new LinkedHashSet<>(
            Arrays.asList("application", "group", "service", "service-port", "protocol", "timeout")));
    private final Map<String, String> defaults = new LinkedHashMap<>();
    private final Map<DiscoveryTarget, Map<String, String>> targets = new LinkedHashMap<>();
    final String namespace;
    final String domain;

    MetadataEntryConfig(URL reference, URL registry) {
        for (String field : FIELDS) {
            String value = MetadataEntryOptions.parameter(reference, registry, "xds.metadata." + field, null);
            if (value != null) defaults.put(field, value);
        }
        namespace = MetadataEntryOptions.parameter(reference, registry, "provider-namespace", null);
        domain = MetadataEntryOptions.parameter(reference, registry, "xds.cluster-domain", "cluster.local");
        merge(registry.getParameter(TARGETS));
        merge(reference.getParameter(TARGETS));
        if (targets.size() > 128) throw new IllegalArgumentException("Too many xDS metadata targets");
        // Validate unused entries too: a typo must not silently select the default metadata service.
        targets.keySet().forEach(target -> new MetadataEntryOptions(this, target));
    }

    private void merge(String json) {
        if (json == null) return;
        if (json.length() > 65536 || json.getBytes(StandardCharsets.UTF_8).length > 65536)
            throw new IllegalArgumentException("xDS metadata targets exceed 64 KiB");
        Struct.Builder root = Struct.newBuilder();
        try {
            JsonFormat.parser().merge(json, root);
        } catch (Exception error) {
            throw new IllegalArgumentException("Invalid xds.metadata.targets JSON", error);
        }
        if (root.getFieldsCount() > 128) throw new IllegalArgumentException("Too many xDS metadata targets");
        Set<DiscoveryTarget> seen = new LinkedHashSet<>();
        for (Map.Entry<String, Value> entry : root.getFieldsMap().entrySet()) {
            DiscoveryTarget target = DiscoveryTarget.parse(entry.getKey());
            if (target.getHostname().indexOf('.') < 0 || !seen.add(target)) {
                throw new IllegalArgumentException("Metadata target keys require distinct canonical Service FQDNs");
            }
            if (entry.getValue().getKindCase() != Value.KindCase.STRUCT_VALUE) {
                throw new IllegalArgumentException("Each metadata target must contain an object");
            }
            Map<String, String> fields = targets.computeIfAbsent(target, ignored -> new LinkedHashMap<>());
            for (Map.Entry<String, Value> field :
                    entry.getValue().getStructValue().getFieldsMap().entrySet()) {
                if (!FIELDS.contains(field.getKey()) || field.getValue().getKindCase() != Value.KindCase.STRING_VALUE) {
                    throw new IllegalArgumentException("Metadata target fields must be known string parameters");
                }
                fields.put(field.getKey(), field.getValue().getStringValue());
            }
        }
    }

    String parameter(DiscoveryTarget target, String field, String fallback) {
        return targets.getOrDefault(target, Collections.emptyMap())
                .getOrDefault(field, defaults.getOrDefault(field, fallback));
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MetadataEntryConfig)) return false;
        MetadataEntryConfig value = (MetadataEntryConfig) other;
        return defaults.equals(value.defaults)
                && targets.equals(value.targets)
                && Objects.equals(namespace, value.namespace)
                && domain.equals(value.domain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaults, targets, namespace, domain);
    }
}
