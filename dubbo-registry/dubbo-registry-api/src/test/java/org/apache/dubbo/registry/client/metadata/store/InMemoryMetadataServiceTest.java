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
//package org.apache.dubbo.registry.client.metadata.store;
//
//import org.apache.dubbo.common.URL;
//import org.apache.dubbo.config.ApplicationConfig;
//import org.apache.dubbo.metadata.MetadataInfo;
//import org.apache.dubbo.metadata.MetadataService;
//import org.apache.dubbo.metadata.definition.model.ServiceDefinition;
//import org.apache.dubbo.registry.MockLogger;
//import org.apache.dubbo.rpc.model.ApplicationModel;
//import org.apache.dubbo.rpc.model.FrameworkModel;
//
//import com.google.gson.Gson;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//
//import java.util.Map;
//import java.util.SortedSet;
//
//import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
//import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
//import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
//import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
//import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
//import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
//import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertNotSame;
//import static org.junit.jupiter.api.Assertions.assertNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
///**
// * Construction of {@link org.apache.dubbo.metadata.MetadataInfo} and {@link org.apache.dubbo.metadata.MetadataInfo.ServiceInfo} included.
// */
//public class InMemoryMetadataServiceTest {
//    private static final MockLogger logger = new MockLogger();
//
//    @BeforeAll
//    public static void setUp() {
//        FrameworkModel.destroyAll();
//        ApplicationConfig applicationConfig = new ApplicationConfig();
//        applicationConfig.setName("demo-provider2");
//        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(applicationConfig);
//    }
//
//    @AfterAll
//    public static void clearUp() {
//        ApplicationModel.reset();
//    }
//
//    /**
//     * <ul>
//     *   export url
//     *   <li>normal service</li>
//     *   <li>generic service</li>
//     * </ul>
//     */
//    @Test
//    public void testExport() {
//        MetadataServiceDelegation metadataService = new MetadataServiceDelegation();
//        metadataService.setApplicationModel(ApplicationModel.defaultModel());
//        // export normal url
//        URL url = URL.valueOf("dubbo://30.225.21.30:20880/org.apache.dubbo.registry.service.DemoService?" +
//            "REGISTRY_CLUSTER=registry1&anyhost=true&application=demo-provider2&delay=5000&deprecated=false&dubbo=2.0.2" +
//            "&dynamic=true&generic=false&group=greeting&interface=org.apache.dubbo.registry.service.DemoService" +
//            "&metadata-type=remote&methods=sayHello&pid=36621&release=&revision=1.0.0&service-name-mapping=true" +
//            "&side=provider&timeout=5000&timestamp=1629970068002&version=1.0.0&params-filter=-default");
//        metadataService.exportURL(url);
//
//        Map<String, MetadataInfo> metadataInfoMap = metadataService.getMetadataInfos();
//        MetadataInfo defaultMetadataInfo = metadataInfoMap.get("registry1");
//        assertNotNull(defaultMetadataInfo.getServiceInfo(url.getProtocolServiceKey()));
//        assertEquals("demo-provider2", defaultMetadataInfo.getApp());
//        assertEquals(1, defaultMetadataInfo.getServices().size());
//        MetadataInfo.ServiceInfo serviceInfo = defaultMetadataInfo.getServiceInfo(url.getProtocolServiceKey());
//        assertNotNull(serviceInfo);
//        assertEquals(url.getServiceKey(), serviceInfo.getServiceKey());
//        assertEquals(url.getProtocolServiceKey(), serviceInfo.getMatchKey());
//        assertNull(serviceInfo.getParams().get(PID_KEY));
//        assertNull(serviceInfo.getParams().get(TIMESTAMP_KEY));
//        assertNotNull(serviceInfo.getParams().get(APPLICATION_KEY));
//        assertNotNull(serviceInfo.getParams().get(INTERFACE_KEY));
//        assertNotNull(serviceInfo.getParams().get("delay"));
//
//        // export normal url again
//        URL url2 = URL.valueOf("dubbo://30.225.21.30:20880/org.apache.dubbo.registry.service.DemoService2?" +
//            "REGISTRY_CLUSTER=registry1&anyhost=true&application=demo-provider2&delay=5000&deprecated=false&dubbo=2.0.2" +
//            "&dynamic=true&generic=false&group=greeting&interface=org.apache.dubbo.registry.service.DemoService2" +
//            "&metadata-type=remote&methods=sayHello&pid=36621&release=&revision=1.0.0&service-name-mapping=true" +
//            "&side=provider&timeout=5000&timestamp=1629970068002&version=1.0.0&params-filter=customized,-excluded");
//        metadataService.exportURL(url2);
//        assertEquals("demo-provider2", defaultMetadataInfo.getApp());
//        assertEquals(2, defaultMetadataInfo.getServices().size());
//        MetadataInfo.ServiceInfo serviceInfo2 = defaultMetadataInfo.getServiceInfo(url2.getProtocolServiceKey());
//        assertNotNull(serviceInfo2);
//        assertEquals(4, serviceInfo2.getParams().size());
//        assertNull(serviceInfo2.getParams().get(INTERFACE_KEY));
//        assertNull(serviceInfo2.getParams().get("delay"));
//        assertNotNull(serviceInfo2.getParams().get(APPLICATION_KEY));
//        assertNotNull(serviceInfo2.getParams().get(VERSION_KEY));
//        assertNotNull(serviceInfo2.getParams().get(GROUP_KEY));
//        assertNotNull(serviceInfo2.getParams().get(TIMEOUT_KEY));
//
//        // repeat the same url
//        metadataService.exportURL(url);
//        // serviceInfo is replaced
//        assertEquals(2, defaultMetadataInfo.getServices().size());
//        assertNotSame(serviceInfo, defaultMetadataInfo.getServiceInfo(url.getProtocolServiceKey()));
//
//        try {
//            metadataService.blockUntilUpdated();
//            assertTrue(true);
//            metadataService.logger = logger;
//           Thread mainThread = Thread.currentThread();
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    mainThread.interrupt();
//                }
//            }).start();
//            metadataService.blockUntilUpdated();
//            logger.checkLogHappened("metadata refresh thread has been ");
//            metadataService.exportURL(url.addParameter(GROUP_KEY, "anotherGroup"));
//            metadataService.blockUntilUpdated();
//            assertTrue(true);
//        } finally {
//            metadataService.releaseBlock();
//        }
//    }
//
//
//
//    /**
//     * <ul>
//     *   unexport url
//     *   <li>normal service</li>
//     *   <li>generic service</li>
//     * </ul>
//     */
//    @Test
//    public void testUnExport() {
//        MetadataServiceDelegation metadataService = new MetadataServiceDelegation();
//        metadataService.setApplicationModel(ApplicationModel.defaultModel());
//        // export normal url
//        URL url = URL.valueOf("dubbo://30.225.21.30:20880/org.apache.dubbo.registry.service.DemoService?" +
//            "REGISTRY_CLUSTER=registry1&anyhost=true&application=demo-provider2&delay=5000&deprecated=false&dubbo=2.0.2" +
//            "&dynamic=true&generic=false&group=greeting&interface=org.apache.dubbo.registry.service.DemoService" +
//            "&metadata-type=remote&methods=sayHello&pid=36621&release=&revision=1.0.0&service-name-mapping=true" +
//            "&side=provider&timeout=5000&timestamp=1629970068002&version=1.0.0&params-filter=-default");
//        metadataService.exportURL(url);
//        Map<String, MetadataInfo> metadataInfoMap = metadataService.getMetadataInfos();
//        MetadataInfo defaultMetadataInfo = metadataInfoMap.get("registry1");
//        assertEquals(1, defaultMetadataInfo.getServices().size());
//        MetadataInfo.ServiceInfo serviceInfo = defaultMetadataInfo.getServiceInfo(url.getProtocolServiceKey());
//        assertNotNull(serviceInfo);
//
//        metadataService.unexportURL(url);
//        assertEquals(0, defaultMetadataInfo.getServices().size());
//        assertNull(defaultMetadataInfo.getServiceInfo(url.getProtocolServiceKey()));
//    }
//
//    @Test
//    public void testServiceDefinition() {
//        URL url = URL.valueOf("dubbo://30.225.21.30:20880/org.apache.dubbo.registry.service.DemoService");
//        MetadataServiceDelegation metadataService = new MetadataServiceDelegation();
//        metadataService.setApplicationModel(ApplicationModel.defaultModel());
//        metadataService.publishServiceDefinition(url);
//
//        String serviceDefinition = metadataService.getServiceDefinition(url.getServiceInterface(), url.getVersion(), url.getGroup());
//        Gson gson = new Gson();
//        ServiceDefinition serviceDefinitionBuilder = gson.fromJson(serviceDefinition, ServiceDefinition.class);
//        assertEquals(serviceDefinitionBuilder.getCanonicalName(), url.getServiceInterface());
//    }
//
//    @Test
//    public void testSubscribe() {
//        MetadataServiceDelegation metadataService = new MetadataServiceDelegation();
//        metadataService.setApplicationModel(ApplicationModel.defaultModel());
//
//        URL url = URL.valueOf("dubbo://30.225.21.30:20880/org.apache.dubbo.registry.service.DemoService");
//        metadataService.subscribeURL(url);
//
//        URL url2 = URL.valueOf("dubbo://30.225.21.30:20880/org.apache.dubbo.registry.service.DemoService2");
//        metadataService.subscribeURL(url2);
//
//        URL url3 = URL.valueOf("dubbo://30.225.21.30:20880/" + MetadataService.class.getName());
//        metadataService.subscribeURL(url3);
//
//        SortedSet<String> subscribedURLs = metadataService.getSubscribedURLs();
//        assertEquals(subscribedURLs.size(), 2);
//        assertEquals(subscribedURLs.first(), url.toFullString());
//        assertEquals(subscribedURLs.last(), url2.toFullString());
//    }
//}
