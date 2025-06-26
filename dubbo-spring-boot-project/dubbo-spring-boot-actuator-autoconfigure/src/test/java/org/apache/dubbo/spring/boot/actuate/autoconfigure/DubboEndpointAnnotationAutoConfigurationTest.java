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
package org.apache.dubbo.spring.boot.actuate.autoconfigure;

import org.apache.dubbo.common.BaseServiceMetadata;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboConfigsMetadataEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboPropertiesMetadataEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboQosEndpoints;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboReferencesMetadataEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboServicesMetadataEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboShutdownEndpoint;

import java.util.Map;
import java.util.SortedMap;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DubboEndpointAnnotationAutoConfiguration} Test
 *
 * @since 2.7.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = {
            DubboEndpointAnnotationAutoConfigurationTest.class,
            DubboEndpointAnnotationAutoConfigurationTest.ConsumerConfiguration.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "dubbo.service.version = 1.0.0",
            "dubbo.application.id = my-application",
            "dubbo.application.name = dubbo-demo-application",
            "dubbo.module.id = my-module",
            "dubbo.module.name = dubbo-demo-module",
            "dubbo.registry.id = my-registry",
            "dubbo.registry.address = N/A",
            "dubbo.protocol.id=my-protocol",
            "dubbo.protocol.name=dubbo",
            "dubbo.protocol.port=20880",
            "dubbo.provider.id=my-provider",
            "dubbo.provider.host=127.0.0.1",
            "dubbo.scan.basePackages = org.apache.dubbo.spring.boot.actuate.autoconfigure",
            "management.endpoint.dubbo.enabled = true",
            "management.endpoint.dubboshutdown.enabled = true",
            "management.endpoint.dubboconfigs.enabled = true",
            "management.endpoint.dubboservices.enabled = true",
            "management.endpoint.dubboreferences.enabled = true",
            "management.endpoint.dubboproperties.enabled = true",
            "management.endpoints.web.exposure.include = *",
        })
@EnableAutoConfiguration
@Disabled
class DubboEndpointAnnotationAutoConfigurationTest {

    @Autowired
    private DubboQosEndpoints dubboQosEndpoints;

    @Autowired
    private DubboConfigsMetadataEndpoint dubboConfigsMetadataEndpoint;

    @Autowired
    private DubboPropertiesMetadataEndpoint dubboPropertiesEndpoint;

    @Autowired
    private DubboReferencesMetadataEndpoint dubboReferencesMetadataEndpoint;

    @Autowired
    private DubboServicesMetadataEndpoint dubboServicesMetadataEndpoint;

    @Autowired
    private DubboShutdownEndpoint dubboShutdownEndpoint;

    private RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ObjectMapper objectMapper;

    @Value("http://127.0.0.1:${local.management.port}${management.endpoints.web.base-path:/actuator}")
    private String actuatorBaseURL;

    @BeforeEach
    void init() {
        DubboBootstrap.reset();
    }

    @AfterEach
    void destroy() {
        DubboBootstrap.reset();
    }

    @Test
    void testShutdown() throws Exception {

        Map<String, Object> value = dubboShutdownEndpoint.shutdown();

        Map<String, Object> shutdownCounts = (Map<String, Object>) value.get("shutdown.count");

        assertEquals(0, shutdownCounts.get("registries"));
        assertEquals(1, shutdownCounts.get("protocols"));
        assertEquals(1, shutdownCounts.get("services"));
        assertEquals(0, shutdownCounts.get("references"));
    }

    @Test
    void testConfigs() {

        Map<String, Map<String, Map<String, Object>>> configsMap = dubboConfigsMetadataEndpoint.configs();

        Map<String, Map<String, Object>> beansMetadata = configsMap.get("ApplicationConfig");
        assertEquals(
                "dubbo-demo-application", beansMetadata.get("my-application").get("name"));

        beansMetadata = configsMap.get("ConsumerConfig");
        assertTrue(beansMetadata.isEmpty());

        beansMetadata = configsMap.get("MethodConfig");
        assertTrue(beansMetadata.isEmpty());

        beansMetadata = configsMap.get("ModuleConfig");
        assertEquals("dubbo-demo-module", beansMetadata.get("my-module").get("name"));

        beansMetadata = configsMap.get("MonitorConfig");
        assertTrue(beansMetadata.isEmpty());

        beansMetadata = configsMap.get("ProtocolConfig");
        assertEquals("dubbo", beansMetadata.get("my-protocol").get("name"));

        beansMetadata = configsMap.get("ProviderConfig");
        assertEquals("127.0.0.1", beansMetadata.get("my-provider").get("host"));

        beansMetadata = configsMap.get("ReferenceConfig");
        assertTrue(beansMetadata.isEmpty());

        beansMetadata = configsMap.get("RegistryConfig");
        assertEquals("N/A", beansMetadata.get("my-registry").get("address"));

        beansMetadata = configsMap.get("ServiceConfig");
        assertFalse(beansMetadata.isEmpty());
    }

    @Test
    void testServices() {

        Map<String, Map<String, Object>> services = dubboServicesMetadataEndpoint.services();

        assertEquals(1, services.size());

        Map<String, Object> demoServiceMeta = services.get(
                "ServiceBean:org.apache.dubbo.spring.boot.actuate.autoconfigure.DubboEndpointAnnotationAutoConfigurationTest$DemoService:1.0.0:");

        assertEquals("1.0.0", demoServiceMeta.get("version"));
    }

    @Test
    void testReferences() {

        Map<String, Map<String, Object>> references = dubboReferencesMetadataEndpoint.references();

        assertFalse(references.isEmpty());
        String injectedField =
                "private " + DemoService.class.getName() + " " + ConsumerConfiguration.class.getName() + ".demoService";
        Map<String, Object> referenceMap = references.get(injectedField);
        assertNotNull(referenceMap);
        assertEquals(DemoService.class, referenceMap.get("interfaceClass"));
        assertEquals(
                BaseServiceMetadata.buildServiceKey(
                        DemoService.class.getName(),
                        ConsumerConfiguration.DEMO_GROUP,
                        ConsumerConfiguration.DEMO_VERSION),
                referenceMap.get("uniqueServiceName"));
    }

    @Test
    void testProperties() {

        SortedMap<String, Object> properties = dubboPropertiesEndpoint.properties();

        assertEquals("my-application", properties.get("dubbo.application.id"));
        assertEquals("dubbo-demo-application", properties.get("dubbo.application.name"));
        assertEquals("my-module", properties.get("dubbo.module.id"));
        assertEquals("dubbo-demo-module", properties.get("dubbo.module.name"));
        assertEquals("my-registry", properties.get("dubbo.registry.id"));
        assertEquals("N/A", properties.get("dubbo.registry.address"));
        assertEquals("my-protocol", properties.get("dubbo.protocol.id"));
        assertEquals("dubbo", properties.get("dubbo.protocol.name"));
        assertEquals("20880", properties.get("dubbo.protocol.port"));
        assertEquals("my-provider", properties.get("dubbo.provider.id"));
        assertEquals("127.0.0.1", properties.get("dubbo.provider.host"));
        assertEquals("org.apache.dubbo.spring.boot.actuate.autoconfigure", properties.get("dubbo.scan.basePackages"));
    }

    @Test
    void testHttpEndpoints() throws JsonProcessingException {
        //        testHttpEndpoint("/dubbo", dubboQosEndpoints::invoke);
        testHttpEndpoint("/dubbo/configs", dubboConfigsMetadataEndpoint::configs);
        testHttpEndpoint("/dubbo/services", dubboServicesMetadataEndpoint::services);
        testHttpEndpoint("/dubbo/references", dubboReferencesMetadataEndpoint::references);
        testHttpEndpoint("/dubbo/properties", dubboPropertiesEndpoint::properties);
    }

    private void testHttpEndpoint(String actuatorURI, Supplier<Map> resultsSupplier) throws JsonProcessingException {
        String actuatorURL = actuatorBaseURL + actuatorURI;
        String response = restTemplate.getForObject(actuatorURL, String.class);
        assertEquals(objectMapper.writeValueAsString(resultsSupplier.get()), response);
    }

    interface DemoService {
        String sayHello(String name);
    }

    @DubboService(
            version = "${dubbo.service.version}",
            application = "${dubbo.application.id}",
            protocol = "${dubbo.protocol.id}",
            registry = "${dubbo.registry.id}")
    static class DefaultDemoService implements DemoService {

        public String sayHello(String name) {
            return "Hello, " + name + " (from Spring Boot)";
        }
    }

    @Configuration
    static class ConsumerConfiguration {
        public static final String DEMO_GROUP = "demo";
        public static final String DEMO_VERSION = "1.0.0";

        @DubboReference(group = DEMO_GROUP, version = DEMO_VERSION)
        private DemoService demoService;
    }
}
