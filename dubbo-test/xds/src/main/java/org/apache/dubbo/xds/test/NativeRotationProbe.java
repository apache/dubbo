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
package org.apache.dubbo.xds.test;

import org.apache.dubbo.remoting.api.connection.ConnectionManager;
import org.apache.dubbo.remoting.api.connection.MultiplexProtocolConnectionManager;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.CertificateProviderInfo;
import org.apache.dubbo.xds.client.XdsRuntimePool;
import org.apache.dubbo.xds.registry.XdsEndpoint;

import java.io.ByteArrayInputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/** Public certificate observations and explicitly shortened bootstrap polling in owned test Pods only. */
final class NativeRotationProbe {
    private NativeRotationProbe() {}

    static void prepare(Path runtime) throws Exception {
        prepare(runtime, false);
    }

    static void prepareRoot(Path runtime) throws Exception {
        prepare(runtime, true);
    }

    private static void prepare(Path runtime, boolean testRoots) throws Exception {
        String source = System.getenv("GRPC_XDS_BOOTSTRAP");
        if (source == null || source.isEmpty()) throw new IllegalStateException("Missing agent bootstrap file");
        Path original = Paths.get(source);
        Path destination = runtime.resolve("rotation-bootstrap.json");
        if (original.toAbsolutePath()
                .normalize()
                .equals(destination.toAbsolutePath().normalize()))
            throw new IllegalArgumentException("Never overwrite the agent bootstrap");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (!Files.isRegularFile(original) && System.nanoTime() < deadline) Thread.sleep(100);
        if (Files.size(original) > 1048576) throw new IllegalArgumentException("Bootstrap file too large");
        JSONObject bootstrap = JSON.parseObject(new String(Files.readAllBytes(original), StandardCharsets.UTF_8));
        JSONObject providers = bootstrap.getJSONObject("certificate_providers");
        if (providers == null || providers.isEmpty())
            throw new IllegalArgumentException("Missing certificate providers");
        if (testRoots && providers.size() != 1)
            throw new IllegalArgumentException("Root rollover fixture requires one certificate provider");
        Map<String, Object> adjustments = new LinkedHashMap<>();
        for (String name : providers.keySet()) {
            JSONObject provider = providers.getJSONObject(name);
            if (!"file_watcher".equals(provider.getString("plugin_name")))
                throw new IllegalArgumentException("Rotation fixture requires file_watcher");
            JSONObject config = provider.getJSONObject("config");
            if (config == null) throw new IllegalArgumentException("Missing file_watcher config");
            Map<String, Object> interval = new LinkedHashMap<>();
            interval.put("original_refresh_interval", config.getString("refresh_interval"));
            interval.put("test_refresh_interval", "1s");
            adjustments.put(name, interval);
            config.put("refresh_interval", "1s");
            if (testRoots) {
                Path material = runtime.resolve("root-rotation");
                Map<String, String> files = new LinkedHashMap<>();
                files.put("certificate_file", "cert.pem");
                files.put("private_key_file", "key.pem");
                files.put("ca_certificate_file", "root.pem");
                for (Map.Entry<String, String> file : files.entrySet()) {
                    Path input = material.resolve(file.getValue());
                    if (!Files.isRegularFile(input) || Files.size(input) == 0 || Files.size(input) > 1048576)
                        throw new IllegalArgumentException("Missing or invalid root rollover fixture input");
                    config.put(file.getKey(), input.toString());
                }
            }
        }
        Files.write(destination, JSON.toJSONBytes(bootstrap));
        Files.setPosixFilePermissions(destination, PosixFilePermissions.fromString("rw-------"));
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("status", "PREPARED");
        report.put("certificate_providers", adjustments);
        report.put(
                "scope",
                testRoots
                        ? "Test bootstrap copy; original Node and ADS retained, Pod-local test identities and roots"
                        : "Test bootstrap copy; original Node, ADS and certificate paths retained");
        Files.write(runtime.resolve("rotation-tuning.json"), JSON.toJSONBytes(report));
    }

    static Map<String, Object> snapshot(ApplicationModel application) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "OBSERVED");
        result.put("process_start_ms", ManagementFactory.getRuntimeMXBean().getStartTime());
        result.put("observed_at_ms", System.currentTimeMillis());
        Map<String, Object> certificates = new LinkedHashMap<>();
        try (XdsRuntimePool.Lease lease = XdsRuntimePool.acquire(NativeXdsApplication.diagnosticBootstrap())) {
            result.put("security_epoch", lease.getSecurity().getEpoch());
            result.put("connection", lease.getClient().getConnectionState());
            for (Map.Entry<String, CertificateProviderInfo> entry :
                    lease.getBootstrap().getCertificateProviders().entrySet()) {
                CertificateProviderInfo provider = entry.getValue();
                if (!provider.getConfig().containsFields("certificate_file")) continue;
                Path path = Paths.get(provider.getConfig()
                        .getFieldsOrThrow("certificate_file")
                        .getStringValue());
                Map<String, Object> certificate = new LinkedHashMap<>();
                try {
                    if (Files.size(path) > 1048576) throw new IllegalArgumentException("Certificate chain too large");
                    X509Certificate leaf = (X509Certificate) CertificateFactory.getInstance("X.509")
                            .generateCertificate(new ByteArrayInputStream(Files.readAllBytes(path)));
                    leaf.checkValidity();
                    certificate.put("ready", true);
                    certificate.put("leaf_sha256", NativeLifecycleProbe.fingerprint(leaf));
                    certificate.put("not_before_ms", leaf.getNotBefore().getTime());
                    certificate.put("not_after_ms", leaf.getNotAfter().getTime());
                    List<String> identities = new ArrayList<>();
                    if (leaf.getSubjectAlternativeNames() != null) {
                        for (List<?> alternative : leaf.getSubjectAlternativeNames()) {
                            if (Integer.valueOf(6).equals(alternative.get(0)))
                                identities.add(String.valueOf(alternative.get(1)));
                        }
                    }
                    certificate.put("uri_sans", identities);
                    if (provider.getConfig().containsFields("ca_certificate_file")) {
                        Path roots = Paths.get(provider.getConfig()
                                .getFieldsOrThrow("ca_certificate_file")
                                .getStringValue());
                        if (Files.size(roots) > 1048576) throw new IllegalArgumentException("Root bundle too large");
                        List<String> authorities = new ArrayList<>();
                        for (Certificate authority : CertificateFactory.getInstance("X.509")
                                .generateCertificates(new ByteArrayInputStream(Files.readAllBytes(roots)))) {
                            authorities.add(NativeLifecycleProbe.fingerprint((X509Certificate) authority));
                        }
                        if (authorities.isEmpty()) throw new IllegalArgumentException("Root bundle is empty");
                        certificate.put("root_authorities_sha256", authorities);
                    }
                } catch (Exception error) {
                    certificate.put("ready", false);
                    certificate.put("error_type", error.getClass().getSimpleName());
                }
                certificates.put(entry.getKey(), certificate);
            }
        }
        result.put("certificates", certificates);
        List<Map<String, Object>> connections = new ArrayList<>();
        if (application != null) {
            ConnectionManager manager = application
                    .getFrameworkModel()
                    .getExtensionLoader(ConnectionManager.class)
                    .getExtension(MultiplexProtocolConnectionManager.NAME);
            manager.forEachConnection(client -> {
                XdsEndpoint endpoint = (XdsEndpoint) client.getUrl().getAttribute(XdsEndpoint.ATTRIBUTE);
                if (endpoint == null || !"tri".equals(endpoint.getTarget().getProtocol())) return;
                Map<String, Object> connection = new LinkedHashMap<>();
                connection.put("cluster", endpoint.getCluster().getName());
                connection.put("connectivity", client.getConnectivityState());
                try {
                    connection.put("tls", NativeLifecycleProbe.tls(client.getChannel(true)));
                } catch (Exception error) {
                    connection.put("error_type", error.getClass().getSimpleName());
                }
                connections.add(connection);
            });
        }
        result.put("connections", connections);
        return result;
    }

    static void watchProvider(Path runtime) {
        Thread observer = new Thread(
                () -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Map<String, Object> result = snapshot(null);
                            Path temporary = runtime.resolve("rotation-observation.tmp");
                            Files.write(temporary, JSON.toJSONBytes(result));
                            Files.move(
                                    temporary,
                                    runtime.resolve("rotation-observation.json"),
                                    StandardCopyOption.REPLACE_EXISTING);
                            Thread.sleep(1000);
                        } catch (InterruptedException stopped) {
                            Thread.currentThread().interrupt();
                        } catch (Exception error) {
                            // No bootstrap or credential payload is emitted when a read races with agent rotation.
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException stopped) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                },
                "native-test-public-certificates");
        observer.setDaemon(true);
        observer.start();
    }
}
