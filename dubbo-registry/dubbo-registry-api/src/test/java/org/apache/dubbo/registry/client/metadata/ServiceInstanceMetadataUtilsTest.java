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
//package org.apache.dubbo.registry.client.metadata;
//
//import org.apache.dubbo.common.URL;
//import org.apache.dubbo.config.ApplicationConfig;
//import org.apache.dubbo.metadata.MetadataService;
//import org.apache.dubbo.metadata.ServiceNameMapping;
//import org.apache.dubbo.registry.Registry;
//import org.apache.dubbo.registry.client.DefaultServiceInstance;
//import org.apache.dubbo.registry.client.InMemoryServiceDiscovery;
//import org.apache.dubbo.registry.client.ServiceDiscovery;
//import org.apache.dubbo.registry.client.ServiceDiscoveryRegistry;
//import org.apache.dubbo.registry.support.RegistryManager;
//import org.apache.dubbo.rpc.model.ApplicationModel;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//import com.google.gson.Gson;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.lang.reflect.Constructor;
//import java.lang.reflect.Field;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
//import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
//import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_CLUSTER_PROPERTY_NAME;
//import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME;
//import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_STORAGE_TYPE_PROPERTY_NAME;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
///**
// * {@link ServiceInstanceMetadataUtils} Test
// *
// * @since 2.7.5
// */
//public class ServiceInstanceMetadataUtilsTest {
//
//    private static URL url = URL.valueOf("dubbo://192.168.0.102:20880/org.apache.dubbo.metadata.MetadataService?&anyhost=true&application=spring-cloud-alibaba-dubbo-provider&bind.ip=192.168.0.102&bind.port=20880&default.deprecated=false&default.dynamic=false&default.register=true&deprecated=false&dubbo=2.0.2&dynamic=false&generic=false&group=spring-cloud-alibaba-dubbo-provider&interface=org.apache.dubbo.metadata.MetadataService&methods=getAllServiceKeys,getServiceRestMetadata,getExportedURLs,getAllExportedURLs&pid=58350&register=true&release=2.7.1&revision=1.0.0&side=provider&timestamp=1557928573174&version=1.0.0");
//    private static URL url2 = URL.valueOf("rest://192.168.0.102:20880/org.apache.dubbo.metadata.MetadataService?&anyhost=true&application=spring-cloud-alibaba-dubbo-provider&bind.ip=192.168.0.102&bind.port=20880&default.deprecated=false&default.dynamic=false&default.register=true&deprecated=false&dubbo=2.0.2&dynamic=false&generic=false&group=spring-cloud-alibaba-dubbo-provider&interface=org.apache.dubbo.metadata.MetadataService&methods=getAllServiceKeys,getServiceRestMetadata,getExportedURLs,getAllExportedURLs&pid=58350&register=true&release=2.7.1&revision=1.0.0&side=provider&timestamp=1557928573174&version=1.0.0");
//
//    private static final String VALUE_URL = "{\"version\":\"1.0.0\",\"dubbo\":\"2.0.2\",\"release\":\"2.7.1\",\"port\":\"20880\",\"protocol\":\"dubbo\"}";
//    private static final String VALUE_URL2 = "{\"version\":\"1.0.0\",\"dubbo\":\"2.0.2\",\"release\":\"2.7.1\",\"port\":\"20880\",\"protocol\":\"rest\"}";
//
//    private DefaultServiceInstance serviceInstance;
//
//    @BeforeEach
//    public void init() {
//        serviceInstance = new DefaultServiceInstance("test", "127.0.0.1", 8080, ApplicationModel.defaultModel());
//    }
//
//    @BeforeAll
//    public static void setUp() {
//        ApplicationConfig applicationConfig = new ApplicationConfig("demo");
//        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(applicationConfig);
//    }
//
//    @AfterAll
//    public static void clearUp() {
//        ApplicationModel.reset();
//    }
//
//
//    @Test
//    public void testMetadataServiceURLParameters() {
//
//        List<URL> urls = Arrays.asList(url, url2);
//
//        urls.forEach(url -> {
//            String parameter = ServiceInstanceMetadataUtils.getMetadataServiceParameter(url);
//
//            JSONObject jsonObject = JSON.parseObject(parameter);
//
//            for (Map.Entry<String, String> param : url.getParameters().entrySet()) {
//                String value = jsonObject.getString(param.getKey());
//                if (value != null) {
//                    assertEquals(param.getValue(), value);
//                }
//            }
//
//        });
//
//        assertEquals(ServiceInstanceMetadataUtils.getMetadataServiceParameter(url), VALUE_URL);
//        assertEquals(ServiceInstanceMetadataUtils.getMetadataServiceParameter(url2), VALUE_URL2);
//    }
//
//    @Test
//    public void getMetadataServiceURLsParams() {
//        Map<String, String> urlParams = new HashMap<>();
//        urlParams.put("dubbo", "1111");
//        urlParams.put("rest", "2222");
//        serviceInstance.getMetadata().put(METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME, new Gson().toJson(urlParams));
//        Map<String, String> metadataServiceURLsParams = ServiceInstanceMetadataUtils.getMetadataServiceURLsParams(serviceInstance);
//        Assertions.assertEquals(metadataServiceURLsParams.get("dubbo"), "1111");
//        Assertions.assertEquals(metadataServiceURLsParams.get("rest"), "2222");
//    }
//
//    @Test
//    public void testMetadataStorageType() {
//        Assertions.assertEquals(ServiceInstanceMetadataUtils.getMetadataStorageType(serviceInstance), DEFAULT_METADATA_STORAGE_TYPE);
//        serviceInstance.getMetadata().put(METADATA_STORAGE_TYPE_PROPERTY_NAME, REMOTE_METADATA_STORAGE_TYPE);
//        Assertions.assertEquals(ServiceInstanceMetadataUtils.getMetadataStorageType(serviceInstance), REMOTE_METADATA_STORAGE_TYPE);
//    }
//
//    @Test
//    public void getRemoteCluster() {
//        Assertions.assertNull(ServiceInstanceMetadataUtils.getRemoteCluster(serviceInstance));
//
//        serviceInstance.getMetadata().put(METADATA_CLUSTER_PROPERTY_NAME, "REGISTRY_CLUSTER_9103");
//        Assertions.assertEquals(ServiceInstanceMetadataUtils.getRemoteCluster(serviceInstance), "REGISTRY_CLUSTER_9103");
//    }
//
//    @Test
//    public void testEndpoints() {
//        Assertions.assertFalse(ServiceInstanceMetadataUtils.hasEndpoints(serviceInstance));
//
//        Map<String, Integer> endpoints = new HashMap<>();
//        endpoints.put("dubbo", 20880);
//        endpoints.put("rest", 8080);
//        ServiceInstanceMetadataUtils.setEndpoints(serviceInstance, endpoints);
//        Assertions.assertTrue(ServiceInstanceMetadataUtils.hasEndpoints(serviceInstance));
//
//        for (Map.Entry<String, Integer> entry : endpoints.entrySet()) {
//            String protocol = entry.getKey();
//            Integer port = entry.getValue();
//            DefaultServiceInstance.Endpoint endpoint = ServiceInstanceMetadataUtils.getEndpoint(serviceInstance, protocol);
//            Assertions.assertEquals(endpoint.getPort(), port);
//        }
//    }
//
//    @Test
//    public void testRegisterMetadataAndInstance() throws Exception {
//        InMemoryServiceDiscovery inMemoryServiceDiscovery = prepare();
//        ServiceInstanceMetadataUtils.registerMetadataAndInstance(ApplicationModel.defaultModel());
//
//        Assertions.assertTrue(inMemoryServiceDiscovery.getServices().contains(serviceInstance.getServiceName()));
//    }
//
//    @Test
//    public void refreshMetadataAndInstance() throws Exception {
//        InMemoryServiceDiscovery inMemoryServiceDiscovery = prepare();
//
//        Assertions.assertNull(inMemoryServiceDiscovery.getLocalInstance());
//
//        ServiceInstanceMetadataUtils.refreshMetadataAndInstance(ApplicationModel.defaultModel());
//
//        Assertions.assertEquals(inMemoryServiceDiscovery.getLocalInstance().getServiceName(), serviceInstance.getServiceName());
//        Assertions.assertEquals(inMemoryServiceDiscovery.getLocalInstance().getHost(), serviceInstance.getHost());
//        Assertions.assertEquals(inMemoryServiceDiscovery.getLocalInstance().getPort(), serviceInstance.getPort());
//        Assertions.assertEquals(inMemoryServiceDiscovery.getLocalInstance().getApplicationModel(), serviceInstance.getApplicationModel());
//    }
//
//    private InMemoryServiceDiscovery prepare() throws NoSuchMethodException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException, NoSuchFieldException {
//        // Construct serviceDiscoveryRegistry
//        InMemoryServiceDiscovery inMemoryServiceDiscovery = new InMemoryServiceDiscovery("ServiceInstanceMetadataUtilsTest");
//        URL registryURL = URL.valueOf("registry://localhost:2181?registry=zookeeper");
//        Constructor<ServiceDiscoveryRegistry> constructor = ServiceDiscoveryRegistry.class.getDeclaredConstructor(URL.class, ServiceDiscovery.class, ServiceNameMapping.class);
//        constructor.setAccessible(true);
//        ServiceDiscoveryRegistry serviceDiscoveryRegistry = constructor.newInstance(registryURL, inMemoryServiceDiscovery, metadataService);
//
//        // Add serviceDiscoveryRegistry to RegisterManger
//        RegistryManager manager = ApplicationModel.defaultModel().getBeanFactory().getBean(RegistryManager.class);
//        Field field = manager.getClass().getDeclaredField("registries");
//        field.setAccessible(true);
//        Map<String, Registry> registries = new ConcurrentHashMap<>();
//        registries.put("127.0.0.1:2181", serviceDiscoveryRegistry);
//        field.set(manager, registries);
//        return inMemoryServiceDiscovery;
//    }
//
//}
