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
//package org.apache.dubbo.registry.client.event.listener;
//
//import org.apache.dubbo.common.URL;
//import org.apache.dubbo.common.utils.StringUtils;
//import org.apache.dubbo.metadata.MetadataInfo;
//import org.apache.dubbo.metadata.MetadataService;
//import org.apache.dubbo.registry.NotifyListener;
//import org.apache.dubbo.registry.client.DefaultServiceInstance;
//import org.apache.dubbo.registry.client.InstanceAddressURL;
//import org.apache.dubbo.registry.client.ServiceDiscovery;
//import org.apache.dubbo.registry.client.ServiceInstance;
//import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
//import org.apache.dubbo.registry.client.metadata.MetadataUtils;
//
//import com.google.gson.Gson;
//import org.apache.dubbo.rpc.model.ApplicationModel;
//import org.hamcrest.Matchers;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.MethodOrderer;
//import org.junit.jupiter.api.Order;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestMethodOrder;
//import org.mockito.ArgumentCaptor;
//import org.mockito.MockedStatic;
//import org.mockito.Mockito;
//import org.mockito.invocation.InvocationOnMock;
//import org.mockito.stubbing.Answer;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ConcurrentMap;
//import java.util.concurrent.ThreadLocalRandom;
//
//import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;
//import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;
//import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.eq;
//
///**
// * {@link ServiceInstancesChangedListener} Test
// *
// * @since 2.7.5
// */
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//public class ServiceInstancesChangedListenerTest {
//    private static Gson gson = new Gson();
//
//    static List<ServiceInstance> app1Instances;
//    static List<ServiceInstance> app2Instances;
//    static List<ServiceInstance> app1FailedInstances;
//    static List<ServiceInstance> app1FailedInstances2;
//    static List<ServiceInstance> app1InstancesWithNoRevision;
//
//    static String metadata_111 = "{\"app\":\"app1\",\"revision\":\"111\",\"services\":{"
//        + "\"org.apache.dubbo.demo.DemoService:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.demo.DemoService\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app1\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}}"
//        + "}}";
//    static String metadata_222 = "{\"app\":\"app2\",\"revision\":\"333\",\"services\":{"
//        + "\"org.apache.dubbo.demo.DemoService:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.demo.DemoService\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app2\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}},"
//        + "\"org.apache.dubbo.demo.DemoService2:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService2\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.demo.DemoService2\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService2\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app2\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}}"
//        + "}}";
//    static String metadata_333 = "{\"app\":\"app2\",\"revision\":\"333\",\"services\":{"
//        + "\"org.apache.dubbo.demo.DemoService:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.demo.DemoService\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app2\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}},"
//        + "\"org.apache.dubbo.demo.DemoService2:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService2\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.demo.DemoService2\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService2\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app2\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}},"
//        + "\"org.apache.dubbo.demo.DemoService3:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService3\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.demo.DemoService3\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService3\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app2\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}}"
//        + "}}";
//    // failed
//    static String metadata_444 = "{\"app\":\"app1\",\"revision\":\"444\",\"services\":{"
//        + "\"org.apache.dubbo.demo.DemoService:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.demo.DemoService\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app2\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}}"
//        + "}}";
//
//    static String bad_metadatainfo = "{\"xxx\":\"yyy\"}";
//
//    static String service1 = "org.apache.dubbo.demo.DemoService";
//    static String service2 = "org.apache.dubbo.demo.DemoService2";
//    static String service3 = "org.apache.dubbo.demo.DemoService3";
//
//    static URL consumerURL = URL.valueOf("dubbo://127.0.0.1/org.apache.dubbo.demo.DemoService?registry_cluster=default");
//    static URL registryURL = URL.valueOf("dubbo://127.0.0.1:2181/org.apache.dubbo.demo.RegistryService");
//
//    static MetadataInfo metadataInfo_111;
//    static MetadataInfo metadataInfo_222;
//    static MetadataInfo metadataInfo_333;
//    static MetadataInfo metadataInfo_444;
//
//    static MetadataService metadataService;
//
//    static ServiceDiscovery serviceDiscovery;
//
//    ServiceInstancesChangedListener listener = null;
//
//    @BeforeAll
//    public static void setUp() {
//        List<Object> urlsSameRevision = new ArrayList<>();
//        urlsSameRevision.add("127.0.0.1:20880?revision=111");
//        urlsSameRevision.add("127.0.0.2:20880?revision=111");
//        urlsSameRevision.add("127.0.0.3:20880?revision=111");
//
//        List<Object> urlsDifferentRevision = new ArrayList<>();
//        urlsDifferentRevision.add("30.10.0.1:20880?revision=222");
//        urlsDifferentRevision.add("30.10.0.2:20880?revision=222");
//        urlsDifferentRevision.add("30.10.0.3:20880?revision=333");
//        urlsDifferentRevision.add("30.10.0.4:20880?revision=333");
//
//        List<Object> urlsFailedRevision = new ArrayList<>();
//        urlsFailedRevision.add("30.10.0.5:20880?revision=222");
//        urlsFailedRevision.add("30.10.0.6:20880?revision=222");
//        urlsFailedRevision.add("30.10.0.7:20880?revision=444");// revision will fail
//        urlsFailedRevision.add("30.10.0.8:20880?revision=444");// revision will fail
//
//        List<Object> urlsFailedRevision2 = new ArrayList<>();
//        urlsFailedRevision2.add("30.10.0.1:20880?revision=222");
//        urlsFailedRevision2.add("30.10.0.2:20880?revision=222");
//
//        List<Object> urlsWithoutRevision = new ArrayList<>();
//        urlsWithoutRevision.add("30.10.0.1:20880");
//
//        app1Instances = buildInstances(urlsSameRevision);
//        app2Instances = buildInstances(urlsDifferentRevision);
//        app1FailedInstances = buildInstances(urlsFailedRevision);
//        app1FailedInstances2 = buildInstances(urlsFailedRevision2);
//        app1InstancesWithNoRevision = buildInstances(urlsWithoutRevision);
//
//        metadataInfo_111 = gson.fromJson(metadata_111, MetadataInfo.class);
//        metadataInfo_222 = gson.fromJson(metadata_222, MetadataInfo.class);
//        metadataInfo_333 = gson.fromJson(metadata_333, MetadataInfo.class);
//        metadataInfo_444 = gson.fromJson(metadata_444, MetadataInfo.class);
//
//        metadataService = Mockito.mock(MetadataService.class);
//        Mockito.doReturn(metadataInfo_111).when(metadataService).getMetadataInfo("111");
//        Mockito.doReturn(metadataInfo_222).when(metadataService).getMetadataInfo("222");
//        Mockito.doReturn(metadataInfo_333).when(metadataService).getMetadataInfo("333");
//        Mockito.doThrow(IllegalStateException.class).when(metadataService).getMetadataInfo("444");
//
//        serviceDiscovery = Mockito.mock(ServiceDiscovery.class);
//        Mockito.doReturn(registryURL).when(serviceDiscovery).getUrl();
//    }
//
//    @AfterEach
//    public void tearDown() {
//        if (listener != null) {
//            listener.destroy();
//            listener = null;
//        }
//    }
//
//    // 正常场景。单应用app1 通知地址基本流程，只做instance-metadata关联，没有metadata内容的解析
//    @Test
//    @Order(1)
//    public void testInstanceNotification() {
//        Set<String> serviceNames = new HashSet<>();
//        serviceNames.add("app1");
//        ServiceDiscovery serviceDiscovery = Mockito.mock(ServiceDiscovery.class);
//        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
//        ServiceInstancesChangedListener spyListener = Mockito.spy(listener);
//        Mockito.doReturn(metadataInfo_111).when(spyListener).getRemoteMetadata(eq("111"), Mockito.anyMap(), Mockito.any());
//        ServiceInstancesChangedEvent event = new ServiceInstancesChangedEvent("app1", app1Instances);
//        spyListener.onEvent(event);
//
//        Map<String, List<ServiceInstance>> allInstances = spyListener.getAllInstances();
//        Assertions.assertEquals(1, allInstances.size());
//        Assertions.assertEquals(3, allInstances.get("app1").size());
//
//        Map<String, MetadataInfo> revisionToMetadata = spyListener.getRevisionToMetadata();
//        Assertions.assertEquals(1, revisionToMetadata.size());
//        Assertions.assertEquals(metadataInfo_111, revisionToMetadata.get("111"));
//
////        // test app2 notification
////        Mockito.doReturn(metadataInfo_222).when(spyListener).getRemoteMetadata(eq("222"), Mockito.anyMap(), Mockito.anyList());
////        Mockito.doReturn(metadataInfo_333).when(spyListener).getRemoteMetadata(eq("333"), Mockito.anyMap(), Mockito.anyList());
////
////        ServiceInstancesChangedEvent event_app2 = new ServiceInstancesChangedEvent("app2", app2Instances);
////        spyListener.onEvent(event_app2);
//
//    }
//
//    // 正常场景。单应用app1，进一步检查 metadata service 是否正确映射
//    @Test
//    @Order(2)
//    public void testInstanceNotificationAndMetadataParse() {
//        Set<String> serviceNames = new HashSet<>();
//        serviceNames.add("app1");
//        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
//
//        try (MockedStatic<MetadataUtils> mockedMetadataUtils = Mockito.mockStatic(MetadataUtils.class)) {
//            mockedMetadataUtils.when(() -> MetadataUtils.getMetadataServiceProxy(Mockito.any())).thenReturn(metadataService);
//            // notify instance change
//            ServiceInstancesChangedEvent event = new ServiceInstancesChangedEvent("app1", app1Instances);
//            listener.onEvent(event);
//
//            Map<String, List<ServiceInstance>> allInstances = listener.getAllInstances();
//            Assertions.assertEquals(1, allInstances.size());
//            Assertions.assertEquals(3, allInstances.get("app1").size());
//
//            Map<String, MetadataInfo> revisionToMetadata = listener.getRevisionToMetadata();
//            Assertions.assertEquals(1, revisionToMetadata.size());
//            Assertions.assertEquals(metadataInfo_111, revisionToMetadata.get("111"));
//
//            List<URL> serviceUrls = listener.getAddresses(service1 + ":dubbo", consumerURL);
//            Assertions.assertEquals(3, serviceUrls.size());
//            assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
//
//            assertThat(serviceUrls, Matchers.hasItem(Matchers.hasProperty("instance", Matchers.notNullValue())));
//            assertThat(serviceUrls, Matchers.hasItem(Matchers.hasProperty("metadataInfo", Matchers.notNullValue())));
//
//        }
//    }
//
//    // 正常场景。多应用，app1 app2 分别通知地址
//    @Test
//    @Order(3)
//    public void testMultipleAppNotification() {
//        Set<String> serviceNames = new HashSet<>();
//        serviceNames.add("app1");
//        serviceNames.add("app2");
//        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
//
//        try (MockedStatic<MetadataUtils> mockedMetadataUtils = Mockito.mockStatic(MetadataUtils.class)) {
//            mockedMetadataUtils.when(() -> MetadataUtils.getMetadataServiceProxy(Mockito.any())).thenReturn(metadataService);
//            // notify app1 instance change
//            ServiceInstancesChangedEvent app1_event = new ServiceInstancesChangedEvent("app1", app1Instances);
//            listener.onEvent(app1_event);
//
//            // notify app2 instance change
//            ServiceInstancesChangedEvent app2_event = new ServiceInstancesChangedEvent("app2", app2Instances);
//            listener.onEvent(app2_event);
//
//            // check
//            Map<String, List<ServiceInstance>> allInstances = listener.getAllInstances();
//            Assertions.assertEquals(2, allInstances.size());
//            Assertions.assertEquals(3, allInstances.get("app1").size());
//            Assertions.assertEquals(4, allInstances.get("app2").size());
//
//            Map<String, MetadataInfo> revisionToMetadata = listener.getRevisionToMetadata();
//            Assertions.assertEquals(3, revisionToMetadata.size());
//            Assertions.assertEquals(metadataInfo_111, revisionToMetadata.get("111"));
//            Assertions.assertEquals(metadataInfo_222, revisionToMetadata.get("222"));
//            Assertions.assertEquals(metadataInfo_333, revisionToMetadata.get("333"));
//
//            List<URL> serviceUrls = listener.getAddresses(service1 + ":dubbo", consumerURL);
//            Assertions.assertEquals(7, serviceUrls.size());
//            List<URL> serviceUrls2 = listener.getAddresses(service2 + ":dubbo", consumerURL);
//            Assertions.assertEquals(4, serviceUrls2.size());
//            assertTrue(serviceUrls2.get(0).getIp().contains("30.10."));
//            List<URL> serviceUrls3 = listener.getAddresses(service3 + ":dubbo", consumerURL);
//            Assertions.assertEquals(2, serviceUrls3.size());
//            assertTrue(serviceUrls3.get(0).getIp().contains("30.10."));
//        }
//    }
//
//    // 正常场景。多应用，app1 app2，空地址通知（边界条件）能否解析出正确的空地址列表
//    @Test
//    @Order(4)
//    public void testMultipleAppEmptyNotification() {
//        Set<String> serviceNames = new HashSet<>();
//        serviceNames.add("app1");
//        serviceNames.add("app2");
//        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
//
//        try (MockedStatic<MetadataUtils> mockedMetadataUtils = Mockito.mockStatic(MetadataUtils.class)) {
//            mockedMetadataUtils.when(() -> MetadataUtils.getMetadataServiceProxy(Mockito.any())).thenReturn(metadataService);
//            // notify app1 instance change
//            ServiceInstancesChangedEvent app1_event = new ServiceInstancesChangedEvent("app1", app1Instances);
//            listener.onEvent(app1_event);
//
//            // notify app2 instance change
//            ServiceInstancesChangedEvent app2_event = new ServiceInstancesChangedEvent("app2", app2Instances);
//            listener.onEvent(app2_event);
//
//            // empty notification
//            ServiceInstancesChangedEvent app1_event_again = new ServiceInstancesChangedEvent("app1", Collections.EMPTY_LIST);
//            listener.onEvent(app1_event_again);
//
//            // check app1 cleared
//            Map<String, List<ServiceInstance>> allInstances = listener.getAllInstances();
//            Assertions.assertEquals(2, allInstances.size());
//            Assertions.assertEquals(0, allInstances.get("app1").size());
//            Assertions.assertEquals(4, allInstances.get("app2").size());
//
//            Map<String, MetadataInfo> revisionToMetadata = listener.getRevisionToMetadata();
//            Assertions.assertEquals(2, revisionToMetadata.size());
//            Assertions.assertNull(revisionToMetadata.get("111"));
//            Assertions.assertEquals(metadataInfo_222, revisionToMetadata.get("222"));
//            Assertions.assertEquals(metadataInfo_333, revisionToMetadata.get("333"));
//
//            List<URL> serviceUrls = listener.getAddresses(service1 + ":dubbo", consumerURL);
//            Assertions.assertEquals(4, serviceUrls.size());
//            assertTrue(serviceUrls.get(0).getIp().contains("30.10."));
//            List<URL> serviceUrls2 = listener.getAddresses(service2 + ":dubbo", consumerURL);
//            Assertions.assertEquals(4, serviceUrls2.size());
//            assertTrue(serviceUrls2.get(0).getIp().contains("30.10."));
//            List<URL> serviceUrls3 = listener.getAddresses(service3 + ":dubbo", consumerURL);
//            Assertions.assertEquals(2, serviceUrls3.size());
//            assertTrue(serviceUrls3.get(0).getIp().contains("30.10."));
//
//            // app2 empty notification
//            ServiceInstancesChangedEvent app2_event_again = new ServiceInstancesChangedEvent("app2", Collections.EMPTY_LIST);
//            listener.onEvent(app2_event_again);
//
//            // check app2 cleared
//            Map<String, List<ServiceInstance>> allInstances_app2 = listener.getAllInstances();
//            Assertions.assertEquals(2, allInstances_app2.size());
//            Assertions.assertEquals(0, allInstances_app2.get("app1").size());
//            Assertions.assertEquals(0, allInstances_app2.get("app2").size());
//
//            Map<String, MetadataInfo> revisionToMetadata_app2 = listener.getRevisionToMetadata();
//            Assertions.assertEquals(0, revisionToMetadata_app2.size());
//
//            assertTrue(isEmpty(listener.getAddresses(service1 + ":dubbo", consumerURL)));
//            assertTrue(isEmpty(listener.getAddresses(service2 + ":dubbo", consumerURL)));
//            assertTrue(isEmpty(listener.getAddresses(service3 + ":dubbo", consumerURL)));
//        }
//    }
//
//    // 正常场景。检查instance listener -> service listener(Directory)地址推送流程
//    @Test
//    @Order(5)
//    public void testServiceListenerNotification() {
//        Set<String> serviceNames = new HashSet<>();
//        serviceNames.add("app1");
//        serviceNames.add("app2");
//        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
//        NotifyListener demoServiceListener = Mockito.mock(NotifyListener.class);
//        NotifyListener demoService2Listener = Mockito.mock(NotifyListener.class);
//        listener.addListenerAndNotify(service1 + ":dubbo", demoServiceListener);
//        listener.addListenerAndNotify(service2 + ":dubbo", demoService2Listener);
//
//        try (MockedStatic<MetadataUtils> mockedMetadataUtils = Mockito.mockStatic(MetadataUtils.class)) {
//            mockedMetadataUtils.when(() -> MetadataUtils.getMetadataServiceProxy(Mockito.any())).thenReturn(metadataService);
//            // notify app1 instance change
//            ServiceInstancesChangedEvent app1_event = new ServiceInstancesChangedEvent("app1", app1Instances);
//            listener.onEvent(app1_event);
//
//            // check
//            ArgumentCaptor<List<URL>> captor = ArgumentCaptor.forClass(List.class);
//            Mockito.verify(demoServiceListener, Mockito.times(1)).notify(captor.capture());
//            List<URL> notifiedUrls = captor.getValue();
//            Assertions.assertEquals(3, notifiedUrls.size());
//            ArgumentCaptor<List<URL>> captor2 = ArgumentCaptor.forClass(List.class);
//            Mockito.verify(demoService2Listener, Mockito.times(1)).notify(captor2.capture());
//            List<URL> notifiedUrls2 = captor2.getValue();
//            Assertions.assertEquals(0, notifiedUrls2.size());
//
//            // notify app2 instance change
//            ServiceInstancesChangedEvent app2_event = new ServiceInstancesChangedEvent("app2", app2Instances);
//            listener.onEvent(app2_event);
//
//            // check
//            ArgumentCaptor<List<URL>> app2_captor = ArgumentCaptor.forClass(List.class);
//            Mockito.verify(demoServiceListener, Mockito.times(2)).notify(app2_captor.capture());
//            List<URL> app2_notifiedUrls = app2_captor.getValue();
//            Assertions.assertEquals(7, app2_notifiedUrls.size());
//            ArgumentCaptor<List<URL>> app2_captor2 = ArgumentCaptor.forClass(List.class);
//            Mockito.verify(demoService2Listener, Mockito.times(2)).notify(app2_captor2.capture());
//            List<URL> app2_notifiedUrls2 = app2_captor2.getValue();
//            Assertions.assertEquals(4, app2_notifiedUrls2.size());
//        }
//
//        // test service listener still get notified when added after instance notification.
//        NotifyListener demoService3Listener = Mockito.mock(NotifyListener.class);
//        listener.addListenerAndNotify(service3 + ":dubbo", demoService3Listener);
//        Mockito.verify(demoService3Listener, Mockito.times(1)).notify(Mockito.anyList());
//    }
//
//    // revision 异常场景。第一次启动，完全拿不到metadata，只能通知部分地址
//    @Test
//    @Order(6)
//    public void testRevisionFailureOnStartup() {
//        Set<String> serviceNames = new HashSet<>();
//        serviceNames.add("app1");
//        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
//        try (MockedStatic<MetadataUtils> mockedMetadataUtils = Mockito.mockStatic(MetadataUtils.class)) {
//            mockedMetadataUtils.when(() -> MetadataUtils.getMetadataServiceProxy(Mockito.any())).thenReturn(metadataService);
//            // notify app1 instance change
//            ServiceInstancesChangedEvent failed_revision_event = new ServiceInstancesChangedEvent("app1", app1FailedInstances);
//            listener.onEvent(failed_revision_event);
//
//            List<URL> serviceUrls = listener.getAddresses(service1 + ":dubbo", consumerURL);
//            List<URL> serviceUrls2 = listener.getAddresses(service2 + ":dubbo", consumerURL);
//
//            assertTrue(isEmpty(serviceUrls));
//            assertTrue(isEmpty(serviceUrls2));
//
//            Map<String, MetadataInfo> revisionToMetadata = listener.getRevisionToMetadata();
//            Assertions.assertEquals(2, revisionToMetadata.size());
//            Assertions.assertEquals(metadataInfo_222, revisionToMetadata.get("222"));
//            Assertions.assertEquals(MetadataInfo.EMPTY, revisionToMetadata.get("444"));
//        }
//    }
//
//    // revision 异常场景。运行中地址通知，拿不到revision就用老版本revision
//    @Test
//    public void testRevisionFailureOnNotification() {
//        Set<String> serviceNames = new HashSet<>();
//        serviceNames.add("app1");
//        serviceNames.add("app2");
//        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
//
//        ConcurrentMap tmpProxyMap = MetadataUtils.metadataServiceProxies;
//
//        try (MockedStatic<MetadataUtils> mockedMetadataUtils = Mockito.mockStatic(MetadataUtils.class)) {
//            mockedMetadataUtils.when(() -> MetadataUtils.getMetadataServiceProxy(Mockito.any())).thenReturn(metadataService);
//
//            // notify app1 instance change
//            ServiceInstancesChangedEvent event = new ServiceInstancesChangedEvent("app1", app1Instances);
//            listener.onEvent(event);
//
//            Mockito.when(metadataService.getMetadataInfo("222")).thenAnswer(new Answer<MetadataInfo>() {
//                @Override
//                public MetadataInfo answer(InvocationOnMock invocationOnMock) throws Throwable {
//                    if (Thread.currentThread().getName().contains("Dubbo-metadata-retry")) {
//                        return metadataInfo_222;
//                    }
//                    return null;
//                }
//            });
////            Mockito.when(metadataService.getMetadataInfo("444")).thenAnswer(new Answer<MetadataInfo>() {
////                @Override
////                public MetadataInfo answer(InvocationOnMock invocationOnMock) throws Throwable {
////                    if (Thread.currentThread().getName().contains("Dubbo-metadata-retry")) {
////                        return metadataInfo_444;
////                    }
////                    return null;
////                }
////            });
//
//            ServiceInstancesChangedEvent event2 = new ServiceInstancesChangedEvent("app2", app1FailedInstances2);
//            listener.onEvent(event2);
//
//            // FIXME, manually mock proxy util, for retry task will work on another thread which makes MockStatic useless.
//            ConcurrentMap map = Mockito.mock(ConcurrentMap.class);
//            Mockito.doReturn(metadataService).when(map).get(Mockito.any());
//            Mockito.doReturn(metadataService).when(map).computeIfAbsent(Mockito.any(), Mockito.any());
//            MetadataUtils.metadataServiceProxies = map;
//
//            // event2 did not really take effect
//            Map<String, MetadataInfo> revisionToMetadata = listener.getRevisionToMetadata();
//            Assertions.assertEquals(2, revisionToMetadata.size());
//            Assertions.assertEquals(metadataInfo_111, revisionToMetadata.get("111"));
//            Assertions.assertEquals(MetadataInfo.EMPTY, revisionToMetadata.get("222"));
//
//            Assertions.assertEquals(3, listener.getAddresses(service1 + ":dubbo", consumerURL).size());
//            assertTrue(isEmpty(listener.getAddresses(service2 + ":dubbo", consumerURL)));
//
//            try {
//                Thread.sleep(15000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            // check recovered after retry.
//            Map<String, MetadataInfo> revisionToMetadata_after_retry = listener.getRevisionToMetadata();
//            Assertions.assertEquals(2, revisionToMetadata_after_retry.size());
//            Assertions.assertEquals(metadataInfo_111, revisionToMetadata_after_retry.get("111"));
//            Assertions.assertEquals(metadataInfo_222, revisionToMetadata_after_retry.get("222"));
//
//            List<URL> serviceUrls_after_retry = listener.getAddresses(service1 + ":dubbo", consumerURL);
//            Assertions.assertEquals(5, serviceUrls_after_retry.size());
//            List<URL> serviceUrls2_after_retry = listener.getAddresses(service2 + ":dubbo", consumerURL);
//            Assertions.assertEquals(2, serviceUrls2_after_retry.size());
//        } finally {
//            MetadataUtils.metadataServiceProxies = tmpProxyMap;
//        }
//    }
//
//    // Abnormal case. Instance does not has revision
//    @Test
//    public void testInstanceWithoutRevision() {
//        Set<String> serviceNames = new HashSet<>();
//        serviceNames.add("app1");
//        ServiceDiscovery serviceDiscovery = Mockito.mock(ServiceDiscovery.class);
//        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
//        ServiceInstancesChangedListener spyListener = Mockito.spy(listener);
//        Mockito.doReturn(null).when(spyListener).getRemoteMetadata(eq(null), Mockito.anyMap(), Mockito.any());
//        ServiceInstancesChangedEvent event = new ServiceInstancesChangedEvent("app1", app1InstancesWithNoRevision);
//        spyListener.onEvent(event);
//        // notification succeeded
//        assertTrue(true);
//    }
//
//    @Test
//    public void testSelectInstance() {
//        System.out.println(ThreadLocalRandom.current().nextInt(0, 100));
//        System.out.println(ThreadLocalRandom.current().nextInt(0, 100));
//        System.out.println(ThreadLocalRandom.current().nextInt(0, 100));
//        System.out.println(ThreadLocalRandom.current().nextInt(0, 100));
//        System.out.println(ThreadLocalRandom.current().nextInt(0, 100));
//    }
//
//    static List<ServiceInstance> buildInstances(List<Object> rawURls) {
//        List<ServiceInstance> instances = new ArrayList<>();
//
//        for (Object obj : rawURls) {
//            String rawURL = (String) obj;
//            DefaultServiceInstance instance = new DefaultServiceInstance();
//            final URL dubboUrl = URL.valueOf(rawURL);
//            instance.setRawAddress(rawURL);
//            instance.setHost(dubboUrl.getHost());
//            instance.setEnabled(true);
//            instance.setHealthy(true);
//            instance.setPort(dubboUrl.getPort());
//            instance.setRegistryCluster("default");
//            instance.setApplicationModel(ApplicationModel.defaultModel());
//
//            Map<String, String> metadata = new HashMap<>();
//            if (StringUtils.isNotEmpty(dubboUrl.getParameter(REVISION_KEY))) {
//                metadata.put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, dubboUrl.getParameter(REVISION_KEY));
//            }
//            instance.setMetadata(metadata);
//
//            instances.add(instance);
//        }
//
//        return instances;
//    }
//}
