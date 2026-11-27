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
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.registry.XdsServiceDiscoveryTest.Ads;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetadataEntryConfigTest {
    private final DiscoveryTarget tri = new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "tri");
    private final DiscoveryTarget binary = new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "dubbo");

    @Test
    void mergesTargetFieldsAndDefaultsWithoutCollapsingProtocolOrPort() {
        URL registry = URL.valueOf("xds://localhost?xds.metadata.timeout=5000")
                .addParameter(
                        MetadataEntryConfig.TARGETS,
                        "{\"tri://echo.test.svc.cluster.local:7070\":{\"application\":\"provider\",\"service-port\":\"7071\"},"
                                + "\"dubbo://echo.test.svc.cluster.local:7070\":{\"application\":\"binary-provider\"}}");
        URL reference = URL.valueOf("tri://localhost?xds.metadata.timeout=2000")
                .addParameter(
                        MetadataEntryConfig.TARGETS,
                        "{\"tri://echo.test.svc.cluster.local:7070\":{\"group\":\"metadata-group\"}}");
        MetadataEntryConfig config = new MetadataEntryConfig(reference, registry);
        MetadataEntryOptions options = new MetadataEntryOptions(config, tri);
        assertEquals("provider", options.application);
        assertEquals("metadata-group", options.group);
        assertEquals(7071, options.target.getServicePort());
        assertEquals(2000, options.timeoutMillis);
        MetadataEntryOptions binaryOptions = new MetadataEntryOptions(config, binary);
        assertEquals("binary-provider", binaryOptions.group);
        assertEquals(7070, binaryOptions.target.getServicePort());
        assertEquals("dubbo", binaryOptions.protocol);
        MetadataEntryOptions otherPort =
                new MetadataEntryOptions(config, new DiscoveryTarget(tri.getHostname(), 8080, "tri"));
        assertEquals("echo", otherPort.application);
        assertEquals(8080, otherPort.target.getServicePort());
    }

    @Test
    void targetProtocolAndNamespaceApplyToIndependentMetadataService() {
        URL reference = URL.valueOf("dubbo://localhost?provider-namespace=source")
                .addParameter(
                        MetadataEntryConfig.TARGETS,
                        "{\"dubbo://echo.target.svc.cluster.local:7070\":{\"protocol\":\"tri\",\"service-port\":\"8081\",\"service\":\"metadata\"}}");
        MetadataEntryOptions options = new MetadataEntryOptions(
                reference,
                URL.valueOf("xds://localhost"),
                new DiscoveryTarget("echo.target.svc.cluster.local", 7070, "dubbo"));
        assertEquals(new DiscoveryTarget("metadata.target.svc.cluster.local", 8081, "tri"), options.target);
    }

    @Test
    void aConflictingLaterTargetDoesNotLeavePartialBindings() throws Exception {
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig("config-test"));
        try (Ads ads = new Ads()) {
            XdsServiceDiscovery discovery = new XdsServiceDiscovery(application, ads.registry(application));
            try {
                URL reference = URL.valueOf(
                                "tri://localhost?provided-by=tri://echo.test.svc.cluster.local:7070&protocol=tri")
                        .setScopeModel(application);
                discovery.bindReference(reference);
                assertThrows(
                        IllegalArgumentException.class,
                        () -> discovery.bindReference(reference
                                .addParameter(
                                        "provided-by",
                                        "tri://other.test.svc.cluster.local:8080,tri://echo.test.svc.cluster.local:7070")
                                .addParameter("xds.metadata.application", "conflicting")));
                assertEquals(Collections.singleton(tri.toString()), discovery.getServices());
            } finally {
                discovery.destroy();
            }
        } finally {
            framework.destroy();
        }
    }

    @Test
    void malformedUnusedEntriesFailBeforeAnyDiscoveryBinding() {
        String[] invalid = {
            "not-json",
            "[]",
            "{\"tri://echo:7070\":{}}",
            "{\"tri://echo.test:7070\":true}",
            "{\"tri://echo.test:7070\":{\"unknown\":\"x\"}}",
            "{\"tri://echo.test:7070\":{\"service-port\":7071}}",
            "{\"tri://echo.test:7070\":{\"service-port\":\"0\"}}",
            "{\"tri://echo.test:7070\":{\"timeout\":\"-1\"}}",
            "{\"tri://echo.test:7070\":{\"group\":\" \"}}",
            "{\"tri://echo.test:7070\":{\"protocol\":\"dubbo\"}}",
            "{\"tri://echo.test:7070\":{},\"tri://ECHO.TEST.:7070\":{}}"
        };
        for (String json : invalid) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new MetadataEntryConfig(
                            URL.valueOf("tri://localhost").addParameter(MetadataEntryConfig.TARGETS, json),
                            URL.valueOf("xds://localhost")),
                    json);
        }
    }
}
