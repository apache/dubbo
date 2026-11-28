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

import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BootstrapperTest {
    @TempDir
    Path directory;

    @Test
    void filePrecedesInlineAndInvalidExplicitSourceDoesNotFallThrough() throws Exception {
        Map<String, String> environment = new HashMap<>();
        Path file = directory.resolve("bootstrap.json");
        Files.write(file, json("file-node").getBytes(StandardCharsets.UTF_8));
        environment.put(Bootstrapper.FILE_ENV, file.toString());
        environment.put(Bootstrapper.CONFIG_ENV, json("inline-node"));
        assertEquals(
                "file-node",
                Bootstrapper.load(environment, null, null, 0).getNode().getId());
        assertEquals(
                "explicit-node",
                Bootstrapper.load(environment, null, json("explicit-node"), 0)
                        .getNode()
                        .getId());
        Files.write(file, "{broken".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> Bootstrapper.load(environment, null, null, 0));
        assertThrows(
                IOException.class,
                () -> Bootstrapper.load(
                        environment, directory.resolve("missing").toString(), null, 0));
    }

    @Test
    void preservesNodeMetadataAndSelectsFirstSupportedCredential() throws Exception {
        BootstrapInfo info = Bootstrapper.parse(json("sidecar-node"));
        assertEquals(
                "grpc",
                info.getNode().getMetadata().getFieldsOrThrow("GENERATOR").getStringValue());
        assertEquals("insecure", info.getServers().get(0).getCredential());
        assertEquals("service:7070", info.clientListenerName("service:7070"));
        assertEquals("xds.istio.io/grpc/lds/inbound/0.0.0.0:17070", info.serverListenerName("0.0.0.0:17070"));
        assertEquals(Duration.ofMillis(1500), Bootstrapper.parseDuration("1.5s"));
        assertEquals(
                info.getFingerprint(),
                Bootstrapper.parse("\n" + json("sidecar-node") + "\n").getFingerprint());
    }

    @Test
    void rejectsUnsupportedCredentialsAndUnusableCertificateConfig() {
        assertThrows(IOException.class, () -> Bootstrapper.parse(json("node").replace("insecure", "unknown")));
        assertThrows(
                IOException.class, () -> Bootstrapper.parse(json("node").replace("private_key_file", "missing_key")));
        assertThrows(IOException.class, () -> Bootstrapper.parse(json("node").replace("900s", "0s")));
        assertThrows(
                IOException.class, () -> Bootstrapper.parse(json("node").replace("/inbound/%s", "/inbound/%s/%s")));
        assertTrue(Bootstrapper.parseDuration("900s").toMillis() > 0);
    }

    @Test
    void defaultFileSnapshotSurvivesAgentMetadataRewriteAndFileRemoval() throws Exception {
        Path file = directory.resolve("agent-bootstrap.json");
        Map<String, String> environment = new HashMap<>();
        environment.put(Bootstrapper.FILE_ENV, file.toString());
        Bootstrapper.DefaultBootstrap process = new Bootstrapper.DefaultBootstrap();
        Files.write(file, json("workload").getBytes(StandardCharsets.UTF_8));
        BootstrapInfo initial = process.load(environment, 0);
        String rewritten = json("workload")
                .replace(
                        "\"GENERATOR\":\"grpc\"",
                        "\"GENERATOR\":\"grpc\",\"ANNOTATIONS\":{\"cloud-node\":\"allocated\"}");
        Files.write(file, rewritten.getBytes(StandardCharsets.UTF_8));
        assertSame(initial, process.load(environment, 0));
        BootstrapInfo explicit = Bootstrapper.load(environment, file.toString(), null, 0);
        assertNotEquals(initial.getFingerprint(), explicit.getFingerprint());
        assertEquals("workload", explicit.getNode().getId());
        Files.delete(file);
        assertSame(initial, process.load(environment, 0));
        assertThrows(IOException.class, () -> Bootstrapper.load(environment, file.toString(), null, 0));
        assertThrows(IllegalArgumentException.class, () -> process.load(environment, -1));
    }

    @Test
    void failedDefaultInitializationCanRetryAndDoesNotCacheAnInvalidBootstrap() throws Exception {
        Path file = directory.resolve("not-ready.json");
        Map<String, String> environment = new HashMap<>();
        environment.put(Bootstrapper.FILE_ENV, file.toString());
        Bootstrapper.DefaultBootstrap process = new Bootstrapper.DefaultBootstrap();
        assertThrows(IOException.class, () -> process.load(environment, 0));
        Files.write(file, "{invalid".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> process.load(environment, 0));
        Files.write(file, json("ready").getBytes(StandardCharsets.UTF_8));
        assertEquals("ready", process.load(environment, 0).getNode().getId());
    }

    @Test
    void inlineDefaultRetainsItsInitialSnapshotWhileExplicitSourcesStayIndependent() throws Exception {
        Map<String, String> environment = new HashMap<>();
        environment.put(Bootstrapper.CONFIG_ENV, json("initial"));
        Bootstrapper.DefaultBootstrap process = new Bootstrapper.DefaultBootstrap();
        BootstrapInfo initial = process.load(environment, 0);
        environment.put(Bootstrapper.CONFIG_ENV, json("changed"));
        assertSame(initial, process.load(environment, 0));
        assertEquals(
                "changed",
                Bootstrapper.load(environment, null, null, 0).getNode().getId());
        assertEquals(
                "explicit",
                Bootstrapper.load(environment, null, json("explicit"), 0)
                        .getNode()
                        .getId());
    }

    private static String json(String node) {
        return "{\"xds_servers\":[{\"server_uri\":\"unix:///etc/istio/proxy/XDS\","
                + "\"channel_creds\":[{\"type\":\"unsupported\"},{\"type\":\"insecure\"}],\"server_features\":[\"xds_v3\"]}],"
                + "\"node\":{\"id\":\"" + node + "\",\"metadata\":{\"GENERATOR\":\"grpc\",\"NAMESPACE\":\"test\"}},"
                + "\"certificate_providers\":{\"default\":{\"plugin_name\":\"file_watcher\",\"config\":{"
                + "\"certificate_file\":\"/cert-chain.pem\",\"private_key_file\":\"/key.pem\",\"ca_certificate_file\":\"/root-cert.pem\","
                + "\"refresh_interval\":\"900s\"}}},\"server_listener_resource_name_template\":\"xds.istio.io/grpc/lds/inbound/%s\"}";
    }
}
