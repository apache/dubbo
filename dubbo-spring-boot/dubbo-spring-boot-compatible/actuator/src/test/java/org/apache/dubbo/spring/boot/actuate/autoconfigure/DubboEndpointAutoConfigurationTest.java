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

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.metadata.DubboConfigsMetadata;
import org.apache.dubbo.spring.boot.actuate.endpoint.metadata.DubboPropertiesMetadata;
import org.apache.dubbo.spring.boot.actuate.endpoint.metadata.DubboReferencesMetadata;
import org.apache.dubbo.spring.boot.actuate.endpoint.metadata.DubboServicesMetadata;
import org.apache.dubbo.spring.boot.actuate.endpoint.metadata.DubboShutdownMetadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.SortedMap;
import java.util.function.Supplier;

/**
 * {@link DubboEndpointAutoConfiguration} Test
 *
 * @since 2.7.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                DubboEndpointAutoConfiguration.class,
                DubboEndpointAutoConfigurationTest.class
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
                "dubbo.scan.basePackages=org.apache.dubbo.spring.boot.actuate.autoconfigure",
                "endpoints.enabled = true",
                "management.security.enabled = false",
                "management.contextPath = /actuator",
                "endpoints.dubbo.enabled = true",
                "endpoints.dubbo.sensitive = false",
                "endpoints.dubboshutdown.enabled = true",
                "endpoints.dubboconfigs.enabled = true",
                "endpoints.dubboservices.enabled = true",
                "endpoints.dubboreferences.enabled = true",
                "endpoints.dubboproperties.enabled = true",
        })
@EnableAutoConfiguration
@Ignore
public class DubboEndpointAutoConfigurationTest {

    @Autowired
    private DubboEndpoint dubboEndpoint;

    @Autowired
    private DubboConfigsMetadata dubboConfigsMetadata;

    @Autowired
    private DubboPropertiesMetadata dubboProperties;

    @Autowired
    private DubboReferencesMetadata dubboReferencesMetadata;

    @Autowired
    private DubboServicesMetadata dubboServicesMetadata;

    @Autowired
    private DubboShutdownMetadata dubboShutdownMetadata;

    private RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ObjectMapper objectMapper;

    @Value("http://127.0.0.1:${local.management.port}${management.contextPath:}")
    private String actuatorBaseURL;

    @Test
    public void testShutdown() throws Exception {

        Map<String, Object> value = dubboShutdownMetadata.shutdown();

        Map<String, Object> shutdownCounts = (Map<String, Object>) value.get("shutdown.count");

        Assert.assertEquals(0, shutdownCounts.get("registries"));
        Assert.assertEquals(1, shutdownCounts.get("protocols"));
        Assert.assertEquals(1, shutdownCounts.get("services"));
        Assert.assertEquals(0, shutdownCounts.get("references"));

    }

    @Test
    public void testConfigs() {

        Map<String, Map<String, Map<String, Object>>> configsMap = dubboConfigsMetadata.configs();

        Map<String, Map<String, Object>> beansMetadata = configsMap.get("ApplicationConfig");
        Assert.assertEquals("dubbo-demo-application", beansMetadata.get("my-application").get("name"));

        beansMetadata = configsMap.get("ConsumerConfig");
        Assert.assertTrue(beansMetadata.isEmpty());

        beansMetadata = configsMap.get("MethodConfig");
        Assert.assertTrue(beansMetadata.isEmpty());

        beansMetadata = configsMap.get("ModuleConfig");
        Assert.assertEquals("dubbo-demo-module", beansMetadata.get("my-module").get("name"));

        beansMetadata = configsMap.get("MonitorConfig");
        Assert.assertTrue(beansMetadata.isEmpty());

        beansMetadata = configsMap.get("ProtocolConfig");
        Assert.assertEquals("dubbo", beansMetadata.get("my-protocol").get("name"));

        beansMetadata = configsMap.get("ProviderConfig");
        Assert.assertEquals("127.0.0.1", beansMetadata.get("my-provider").get("host"));

        beansMetadata = configsMap.get("ReferenceConfig");
        Assert.assertTrue(beansMetadata.isEmpty());

        beansMetadata = configsMap.get("RegistryConfig");
        Assert.assertEquals("N/A", beansMetadata.get("my-registry").get("address"));

        beansMetadata = configsMap.get("ServiceConfig");
        Assert.assertFalse(beansMetadata.isEmpty());

    }

    @Test
    public void testServices() {

        Map<String, Map<String, Object>> services = dubboServicesMetadata.services();

        Assert.assertEquals(1, services.size());

        Map<String, Object> demoServiceMeta = services.get("ServiceBean:org.apache.dubbo.spring.boot.actuate.autoconfigure.DubboEndpointAutoConfigurationTest$DemoService:1.0.0");

        Assert.assertEquals("1.0.0", demoServiceMeta.get("version"));

    }

    @Test
    public void testReferences() {

        Map<String, Map<String, Object>> references = dubboReferencesMetadata.references();

        Assert.assertTrue(references.isEmpty());

    }

    @Test
    public void testProperties() {

        SortedMap<String, Object> properties = dubboProperties.properties();

        Assert.assertEquals("my-application", properties.get("dubbo.application.id"));
        Assert.assertEquals("dubbo-demo-application", properties.get("dubbo.application.name"));
        Assert.assertEquals("my-module", properties.get("dubbo.module.id"));
        Assert.assertEquals("dubbo-demo-module", properties.get("dubbo.module.name"));
        Assert.assertEquals("my-registry", properties.get("dubbo.registry.id"));
        Assert.assertEquals("N/A", properties.get("dubbo.registry.address"));
        Assert.assertEquals("my-protocol", properties.get("dubbo.protocol.id"));
        Assert.assertEquals("dubbo", properties.get("dubbo.protocol.name"));
        Assert.assertEquals("20880", properties.get("dubbo.protocol.port"));
        Assert.assertEquals("my-provider", properties.get("dubbo.provider.id"));
        Assert.assertEquals("127.0.0.1", properties.get("dubbo.provider.host"));
        Assert.assertEquals("org.apache.dubbo.spring.boot.actuate.autoconfigure", properties.get("dubbo.scan.basePackages"));
    }

    @Test
    public void testHttpEndpoints() throws JsonProcessingException {
//        testHttpEndpoint("/dubbo", dubboEndpoint::invoke);
        testHttpEndpoint("/dubbo/configs", dubboConfigsMetadata::configs);
        testHttpEndpoint("/dubbo/services", dubboServicesMetadata::services);
        testHttpEndpoint("/dubbo/references", dubboReferencesMetadata::references);
        testHttpEndpoint("/dubbo/properties", dubboProperties::properties);
    }

    private void testHttpEndpoint(String actuatorURI, Supplier<Map> resultsSupplier) throws JsonProcessingException {
        String actuatorURL = actuatorBaseURL + actuatorURI;
        String response = restTemplate.getForObject(actuatorURL, String.class);
        Assert.assertEquals(objectMapper.writeValueAsString(resultsSupplier.get()), response);
    }


    interface DemoService {
        String sayHello(String name);
    }

    @DubboService(
            version = "${dubbo.service.version}",
            application = "${dubbo.application.id}",
            protocol = "${dubbo.protocol.id}",
            registry = "${dubbo.registry.id}"
    )
    static class DefaultDemoService implements DemoService {

        public String sayHello(String name) {
            return "Hello, " + name + " (from Spring Boot)";
        }

    }


}
