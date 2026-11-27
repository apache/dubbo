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
package org.apache.dubbo.xds.bootstrap;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Node;

/** Reads the gRPC bootstrap contract injected by Istio's grpc-agent template. */
public final class Bootstrapper {
    public static final String FILE_ENV = "GRPC_XDS_BOOTSTRAP";
    public static final String CONFIG_ENV = "GRPC_XDS_BOOTSTRAP_CONFIG";
    private static final int MAX_BYTES = 1024 * 1024;
    private static final DefaultBootstrap DEFAULT = new DefaultBootstrap();

    private Bootstrapper() {}

    public static BootstrapInfo load() throws IOException {
        return load(System.getenv(), null, null, 30_000);
    }

    /** The process default is immutable after its first successful load, matching the gRPC bootstrap contract. */
    public static BootstrapInfo loadDefault(long waitMillis) throws IOException {
        return DEFAULT.load(System.getenv(), waitMillis);
    }

    static final class DefaultBootstrap {
        private BootstrapInfo snapshot;

        synchronized BootstrapInfo load(Map<String, String> environment, long waitMillis) throws IOException {
            if (waitMillis < 0) throw new IllegalArgumentException("Bootstrap wait must be non-negative");
            if (snapshot == null) snapshot = Bootstrapper.load(environment, null, null, waitMillis);
            return snapshot;
        }
    }

    /** Explicit sources precede standard environment sources; an invalid source never falls through. */
    public static BootstrapInfo load(
            Map<String, String> environment, String explicitFile, String explicitConfig, long waitMillis)
            throws IOException {
        if (waitMillis < 0) {
            throw new IllegalArgumentException("Bootstrap wait must be non-negative");
        }
        if (present(explicitFile) && present(explicitConfig)) {
            throw new IllegalArgumentException("Specify either a bootstrap file or inline configuration");
        }
        String file = present(explicitFile) ? explicitFile : null;
        String config = present(explicitConfig) ? explicitConfig : null;
        if (file == null && config == null) {
            file = environment.get(FILE_ENV);
            if (!present(file)) {
                file = null;
                config = environment.get(CONFIG_ENV);
            }
        }
        if (file != null) {
            Path path = Paths.get(file);
            long start = System.nanoTime();
            for (; ; ) {
                try {
                    if (Files.size(path) > MAX_BYTES) {
                        throw new IOException("xDS bootstrap exceeds the size limit");
                    }
                    return parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
                } catch (NoSuchFileException e) {
                    if (Duration.ofNanos(System.nanoTime() - start).toMillis() >= waitMillis) {
                        throw new IOException("xDS bootstrap file is not available: " + path, e);
                    }
                    try {
                        Thread.sleep(Math.min(100, waitMillis));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted waiting for the xDS bootstrap", interrupted);
                    }
                }
            }
        }
        if (!present(config)) {
            throw new IOException("Missing xDS bootstrap: set " + FILE_ENV + " or " + CONFIG_ENV);
        }
        return parse(config);
    }

    public static BootstrapInfo parse(String json) throws IOException {
        Objects.requireNonNull(json, "bootstrap JSON");
        if (json.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IOException("xDS bootstrap exceeds the size limit");
        }
        Struct.Builder rootBuilder = Struct.newBuilder();
        JsonFormat.parser().merge(json, rootBuilder);
        Struct root = rootBuilder.build();
        try {
            List<ServerInfo> servers = new ArrayList<>();
            Value configuredServers = required(root, "xds_servers", Value.KindCase.LIST_VALUE);
            for (Value item : configuredServers.getListValue().getValuesList()) {
                Struct server = object(item, "xds_servers entry");
                String uri = string(server, "server_uri");
                Value credentials = required(server, "channel_creds", Value.KindCase.LIST_VALUE);
                String selected = null;
                for (Value credential : credentials.getListValue().getValuesList()) {
                    Struct candidate = object(credential, "channel credential");
                    String type = string(candidate, "type");
                    if ("insecure".equals(type) || "tls".equals(type)) {
                        if (candidate.containsFields("config")
                                && object(candidate.getFieldsOrThrow("config"), "credential config")
                                                .getFieldsCount()
                                        != 0) {
                            throw new IllegalArgumentException(
                                    "Unsupported configuration for channel credential " + type);
                        }
                        selected = type;
                        break;
                    }
                }
                if (selected == null) {
                    throw new IllegalArgumentException("No supported xDS channel credential");
                }
                List<String> features = new ArrayList<>();
                if (server.containsFields("server_features")) {
                    for (Value feature : required(server, "server_features", Value.KindCase.LIST_VALUE)
                            .getListValue()
                            .getValuesList()) {
                        if (feature.getKindCase() != Value.KindCase.STRING_VALUE) {
                            throw new IllegalArgumentException("Server features must be strings");
                        }
                        features.add(feature.getStringValue());
                    }
                }
                servers.add(new ServerInfo(uri, selected, features));
            }
            if (servers.isEmpty()) {
                throw new IllegalArgumentException("No xDS servers configured");
            }
            Node.Builder node = Node.newBuilder();
            Struct nodeJson = object(required(root, "node", Value.KindCase.STRUCT_VALUE), "node");
            JsonFormat.parser()
                    .ignoringUnknownFields()
                    .merge(JsonFormat.printer().print(nodeJson), node);
            if (node.getId().isEmpty()) {
                throw new IllegalArgumentException("The workload node ID must be specified");
            }
            if (node.getUserAgentName().isEmpty()) {
                node.setUserAgentName("Apache Dubbo");
            }
            Map<String, CertificateProviderInfo> providers = new LinkedHashMap<>();
            if (root.containsFields("certificate_providers")) {
                Struct configuredProviders =
                        object(root.getFieldsOrThrow("certificate_providers"), "certificate_providers");
                for (Map.Entry<String, Value> entry :
                        configuredProviders.getFieldsMap().entrySet()) {
                    Struct provider = object(entry.getValue(), "certificate provider");
                    String plugin = string(provider, "plugin_name");
                    Struct config =
                            object(required(provider, "config", Value.KindCase.STRUCT_VALUE), "provider config");
                    if ("file_watcher".equals(plugin)) {
                        boolean cert = present(optionalString(config, "certificate_file", null));
                        boolean key = present(optionalString(config, "private_key_file", null));
                        boolean rootCert = present(optionalString(config, "ca_certificate_file", null));
                        if (cert != key || (!cert && !rootCert)) {
                            throw new IllegalArgumentException(
                                    "file_watcher requires a certificate/key pair or a root certificate");
                        }
                        parseDuration(optionalString(config, "refresh_interval", "600s"));
                    }
                    providers.put(entry.getKey(), new CertificateProviderInfo(plugin, config));
                }
            }
            String serverTemplate = optionalString(root, "server_listener_resource_name_template", null);
            String clientTemplate = optionalString(root, "client_default_listener_resource_name_template", "%s");
            validateTemplate(clientTemplate);
            if (serverTemplate != null) {
                validateTemplate(serverTemplate);
            }
            // Federation requires separate authority routing; do not silently resolve it against the default server.
            if (root.containsFields("authorities")
                    && object(root.getFieldsOrThrow("authorities"), "authorities")
                                    .getFieldsCount()
                            > 0) {
                throw new IllegalArgumentException("xDS federation authorities are not yet supported");
            }
            String canonical = JsonFormat.printer()
                    .sortingMapKeys()
                    .omittingInsignificantWhitespace()
                    .print(root);
            return new BootstrapInfo(
                    servers, node.build(), providers, serverTemplate, clientTemplate, fingerprint(canonical));
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid xDS bootstrap: " + e.getMessage(), e);
        }
    }

    public static Duration parseDuration(String value) {
        if (!value.matches("[0-9]+(\\.[0-9]{1,9})?s")) {
            throw new IllegalArgumentException("Invalid bootstrap duration");
        }
        try {
            long nanos = new BigDecimal(value.substring(0, value.length() - 1))
                    .multiply(BigDecimal.valueOf(1_000_000_000L))
                    .longValueExact();
            if (nanos <= 0) {
                throw new IllegalArgumentException("Bootstrap duration must be positive");
            }
            return Duration.ofNanos(nanos);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Bootstrap duration exceeds supported precision/range", e);
        }
    }

    public static String optionalString(Struct object, String field, String fallback) {
        return object.containsFields(field) ? string(object, field) : fallback;
    }

    private static String string(Struct object, String field) {
        String value = required(object, field, Value.KindCase.STRING_VALUE).getStringValue();
        if (!present(value)) {
            throw new IllegalArgumentException("Empty bootstrap field: " + field);
        }
        return value;
    }

    private static Struct object(Value value, String field) {
        if (value.getKindCase() != Value.KindCase.STRUCT_VALUE) {
            throw new IllegalArgumentException("Expected an object for " + field);
        }
        return value.getStructValue();
    }

    private static Value required(Struct object, String field, Value.KindCase kind) {
        Value value = object.getFieldsMap().get(field);
        if (value == null || value.getKindCase() != kind) {
            throw new IllegalArgumentException("Missing or invalid bootstrap field: " + field);
        }
        return value;
    }

    private static void validateTemplate(String template) {
        int index = template.indexOf("%s");
        if (index < 0
                || template.indexOf("%s", index + 2) >= 0
                || template.replace("%s", "").contains("%")) {
            throw new IllegalArgumentException("Listener template must contain exactly one %s placeholder");
        }
    }

    private static boolean present(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : digest) {
                result.append(String.format("%02x", b & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static final class BootstrapInfo {
        private final List<ServerInfo> servers;
        private final Node node;
        private final Map<String, CertificateProviderInfo> providers;
        private final String serverTemplate;
        private final String clientTemplate;
        private final String fingerprint;

        private BootstrapInfo(
                List<ServerInfo> servers,
                Node node,
                Map<String, CertificateProviderInfo> providers,
                String serverTemplate,
                String clientTemplate,
                String fingerprint) {
            this.servers = Collections.unmodifiableList(new ArrayList<>(servers));
            this.node = node;
            this.providers = Collections.unmodifiableMap(new LinkedHashMap<>(providers));
            this.serverTemplate = serverTemplate;
            this.clientTemplate = clientTemplate;
            this.fingerprint = fingerprint;
        }

        public List<ServerInfo> getServers() {
            return servers;
        }

        public Node getNode() {
            return node;
        }

        public Map<String, CertificateProviderInfo> getCertificateProviders() {
            return providers;
        }

        public String getFingerprint() {
            return fingerprint;
        }

        public String clientListenerName(String target) {
            return clientTemplate.replace("%s", target);
        }

        public String serverListenerName(String bindAddress) {
            if (serverTemplate == null) {
                throw new IllegalStateException("Bootstrap has no server listener template");
            }
            return serverTemplate.replace("%s", bindAddress);
        }
    }

    public static final class ServerInfo {
        private final String uri;
        private final String credential;
        private final List<String> features;

        private ServerInfo(String uri, String credential, List<String> features) {
            this.uri = uri;
            this.credential = credential;
            this.features = Collections.unmodifiableList(new ArrayList<>(features));
        }

        public String getUri() {
            return uri;
        }

        public String getCredential() {
            return credential;
        }

        public boolean ignoresResourceDeletion() {
            return features.contains("ignore_resource_deletion");
        }
    }

    public static final class CertificateProviderInfo {
        private final String plugin;
        private final Struct config;

        private CertificateProviderInfo(String plugin, Struct config) {
            this.plugin = plugin;
            this.config = config;
        }

        public String getPlugin() {
            return plugin;
        }

        public Struct getConfig() {
            return config;
        }
    }
}
