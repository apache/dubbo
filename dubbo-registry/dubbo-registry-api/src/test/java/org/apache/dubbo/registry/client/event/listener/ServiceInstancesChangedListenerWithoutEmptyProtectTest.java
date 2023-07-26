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
package org.apache.dubbo.registry.client.event.listener;

import org.apache.dubbo.common.ProtocolServiceKey;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.LRUCache;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.InstanceAddressURL;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.metadata.store.MetaCacheManager;
import org.apache.dubbo.registry.client.support.MockServiceDiscovery;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;
import static org.apache.dubbo.common.utils.CollectionUtils.isNotEmpty;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link ServiceInstancesChangedListener} Test
 *
 * @since 2.7.5
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceInstancesChangedListenerWithoutEmptyProtectTest {

    static List<ServiceInstance> app1Instances;
    static List<ServiceInstance> app2Instances;
    static List<ServiceInstance> app1FailedInstances;
    static List<ServiceInstance> app1FailedInstances2;
    static List<ServiceInstance> app1InstancesWithNoRevision;
    static List<ServiceInstance> app1InstancesMultipleProtocols;

    static String metadata_111 = "{\"app\":\"app1\",\"revision\":\"111\",\"services\":{"
        + "\"org.apache.dubbo.demo.DemoService:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.demo.DemoService\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app1\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}}"
        + "}}";
    static String metadata_222 = "{\"app\":\"app2\",\"revision\":\"222\",\"services\":{"
        + "\"org.apache.dubbo.demo.DemoService:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.demo.DemoService\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app2\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}},"
        + "\"org.apache.dubbo.demo.DemoService2:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService2\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.demo.DemoService2\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService2\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app2\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}}"
        + "}}";
    static String metadata_333 = "{\"app\":\"app2\",\"revision\":\"333\",\"services\":{"
        + "\"org.apache.dubbo.demo.DemoService:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.demo.DemoService\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app2\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}},"
        + "\"org.apache.dubbo.demo.DemoService2:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService2\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.demo.DemoService2\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService2\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app2\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}},"
        + "\"org.apache.dubbo.demo.DemoService3:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService3\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.demo.DemoService3\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService3\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app2\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}}"
        + "}}";
    // failed
    static String metadata_444 = "{\"app\":\"app1\",\"revision\":\"444\",\"services\":{"
        + "\"org.apache.dubbo.demo.DemoService:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.demo.DemoService\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app2\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}}"
        + "}}";
    // only triple protocol enabled
    static String metadata_555_triple = "{\"app\":\"app1\",\"revision\":\"555\",\"services\":{"
        + "\"org.apache.dubbo.demo.DemoService:dubbo\":{\"name\":\"org.apache.dubbo.demo.DemoService\",\"protocol\":\"tri\",\"path\":\"org.apache.dubbo.demo.DemoService\",\"params\":{\"side\":\"provider\",\"release\":\"\",\"methods\":\"sayHello,sayHelloAsync\",\"deprecated\":\"false\",\"dubbo\":\"2.0.2\",\"pid\":\"72723\",\"interface\":\"org.apache.dubbo.demo.DemoService\",\"service-name-mapping\":\"true\",\"timeout\":\"3000\",\"generic\":\"false\",\"metadata-type\":\"remote\",\"delay\":\"5000\",\"application\":\"app2\",\"dynamic\":\"true\",\"REGISTRY_CLUSTER\":\"registry1\",\"anyhost\":\"true\",\"timestamp\":\"1625800233446\"}}"
        + "}}";

    static String service1 = "org.apache.dubbo.demo.DemoService";
    static String service2 = "org.apache.dubbo.demo.DemoService2";
    static String service3 = "org.apache.dubbo.demo.DemoService3";

    static URL consumerURL = URL.valueOf("dubbo://127.0.0.1/org.apache.dubbo.demo.DemoService?interface=org.apache.dubbo.demo.DemoService&protocol=dubbo&registry_cluster=default");
    static URL consumerURL2 = URL.valueOf("dubbo://127.0.0.1/org.apache.dubbo.demo.DemoService2?interface=org.apache.dubbo.demo.DemoService2&protocol=dubbo&registry_cluster=default");
    static URL consumerURL3 = URL.valueOf("dubbo://127.0.0.1/org.apache.dubbo.demo.DemoService3?interface=org.apache.dubbo.demo.DemoService3&protocol=dubbo&registry_cluster=default");
    static URL multipleProtocolsConsumerURL = URL.valueOf("dubbo,tri://127.0.0.1/org.apache.dubbo.demo.DemoService?interface=org.apache.dubbo.demo.DemoService&protocol=dubbo,tri&registry_cluster=default");
    static URL noProtocolConsumerURL = URL.valueOf("consumer://127.0.0.1/org.apache.dubbo.demo.DemoService?interface=org.apache.dubbo.demo.DemoService&registry_cluster=default");
    static URL singleProtocolsConsumerURL = URL.valueOf("tri://127.0.0.1/org.apache.dubbo.demo.DemoService?interface=org.apache.dubbo.demo.DemoService&protocol=tri&registry_cluster=default");
    static URL registryURL = URL.valueOf("dubbo://127.0.0.1:2181/org.apache.dubbo.demo.RegistryService");

    static MetadataInfo metadataInfo_111;
    static MetadataInfo metadataInfo_222;
    static MetadataInfo metadataInfo_333;
    static MetadataInfo metadataInfo_444;
    static MetadataInfo metadataInfo_555_tri;

    static MetadataService metadataService;

    static ServiceDiscovery serviceDiscovery;

    static ServiceInstancesChangedListener listener = null;

    @BeforeAll
    public static void setUp() {

        metadataService = Mockito.mock(MetadataService.class);

        List<Object> urlsSameRevision = new ArrayList<>();
        urlsSameRevision.add("127.0.0.1:20880?revision=111");
        urlsSameRevision.add("127.0.0.2:20880?revision=111");
        urlsSameRevision.add("127.0.0.3:20880?revision=111");

        List<Object> urlsDifferentRevision = new ArrayList<>();
        urlsDifferentRevision.add("30.10.0.1:20880?revision=222");
        urlsDifferentRevision.add("30.10.0.2:20880?revision=222");
        urlsDifferentRevision.add("30.10.0.3:20880?revision=333");
        urlsDifferentRevision.add("30.10.0.4:20880?revision=333");

        List<Object> urlsFailedRevision = new ArrayList<>();
        urlsFailedRevision.add("30.10.0.5:20880?revision=222");
        urlsFailedRevision.add("30.10.0.6:20880?revision=222");
        urlsFailedRevision.add("30.10.0.7:20880?revision=444");// revision will fail
        urlsFailedRevision.add("30.10.0.8:20880?revision=444");// revision will fail

        List<Object> urlsFailedRevision2 = new ArrayList<>();
        urlsFailedRevision2.add("30.10.0.1:20880?revision=222");
        urlsFailedRevision2.add("30.10.0.2:20880?revision=222");

        List<Object> urlsWithoutRevision = new ArrayList<>();
        urlsWithoutRevision.add("30.10.0.1:20880");

        List<Object> urlsMultipleProtocols = new ArrayList<>();
        urlsMultipleProtocols.add("30.10.0.1:20880?revision=555");//triple
        urlsMultipleProtocols.addAll(urlsSameRevision);// dubbo

        app1Instances = buildInstances(urlsSameRevision);
        app2Instances = buildInstances(urlsDifferentRevision);
        app1FailedInstances = buildInstances(urlsFailedRevision);
        app1FailedInstances2 = buildInstances(urlsFailedRevision2);
        app1InstancesWithNoRevision = buildInstances(urlsWithoutRevision);
        app1InstancesMultipleProtocols = buildInstances(urlsMultipleProtocols);

        metadataInfo_111 = JsonUtils.toJavaObject(metadata_111, MetadataInfo.class);
        metadataInfo_222 = JsonUtils.toJavaObject(metadata_222, MetadataInfo.class);
        metadataInfo_333 = JsonUtils.toJavaObject(metadata_333, MetadataInfo.class);
        metadataInfo_444 = JsonUtils.toJavaObject(metadata_444, MetadataInfo.class);
        metadataInfo_555_tri = JsonUtils.toJavaObject(metadata_555_triple, MetadataInfo.class);

        serviceDiscovery = Mockito.mock(ServiceDiscovery.class);
        when(serviceDiscovery.getUrl()).thenReturn(registryURL);

        when(serviceDiscovery.getRemoteMetadata(eq("111"), anyList())).thenReturn(metadataInfo_111);
        when(serviceDiscovery.getRemoteMetadata(eq("222"), anyList())).thenReturn(metadataInfo_222);
        when(serviceDiscovery.getRemoteMetadata(eq("333"), anyList())).thenReturn(metadataInfo_333);
        when(serviceDiscovery.getRemoteMetadata(eq("444"), anyList())).thenReturn(MetadataInfo.EMPTY);
        when(serviceDiscovery.getRemoteMetadata(eq("555"), anyList())).thenReturn(metadataInfo_555_tri);
    }


    @BeforeEach
    public void init() {
        // Because all tests use the same ServiceDiscovery, the previous metadataCache should be cleared before next unit test
        // to avoid contaminating next unit test.
        clearMetadataCache();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (listener != null) {
            listener.destroy();
            listener = null;
        }
    }

    @AfterAll
    public static void destroy() throws Exception {
        serviceDiscovery.destroy();
    }

    // 正常场景。单应用app1 通知地址基本流程，只做instance-metadata关联，没有metadata内容的解析
    @Test
    @Order(1)
    public void testInstanceNotification() {
        Set<String> serviceNames = new HashSet<>();
        serviceNames.add("app1");
        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
        ServiceInstancesChangedEvent event = new ServiceInstancesChangedEvent("app1", app1Instances);
        listener.onEvent(event);

        Map<String, List<ServiceInstance>> allInstances = listener.getAllInstances();
        Assertions.assertEquals(1, allInstances.size());
        Assertions.assertEquals(3, allInstances.get("app1").size());

        ProtocolServiceKey protocolServiceKey = new ProtocolServiceKey(service1, null, null, "dubbo");
        List<URL> serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(3, serviceUrls.size());
        assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    }

    // 正常场景。单应用app1，进一步检查 metadata service 是否正确映射
    @Test
    @Order(2)
    public void testInstanceNotificationAndMetadataParse() {
        Set<String> serviceNames = new HashSet<>();
        serviceNames.add("app1");
        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);

        // notify instance change
        ServiceInstancesChangedEvent event = new ServiceInstancesChangedEvent("app1", app1Instances);
        listener.onEvent(event);

        Map<String, List<ServiceInstance>> allInstances = listener.getAllInstances();
        Assertions.assertEquals(1, allInstances.size());
        Assertions.assertEquals(3, allInstances.get("app1").size());

        ProtocolServiceKey protocolServiceKey = new ProtocolServiceKey(service1, null, null, "dubbo");
        List<URL> serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(3, serviceUrls.size());
        assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);

        assertThat(serviceUrls, Matchers.hasItem(Matchers.hasProperty("instance", Matchers.notNullValue())));
        assertThat(serviceUrls, Matchers.hasItem(Matchers.hasProperty("metadataInfo", Matchers.notNullValue())));
    }

    // 正常场景。多应用，app1 app2 分别通知地址
    @Test
    @Order(3)
    public void testMultipleAppNotification() {
        Set<String> serviceNames = new HashSet<>();
        serviceNames.add("app1");
        serviceNames.add("app2");
        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);

        // notify app1 instance change
        ServiceInstancesChangedEvent app1_event = new ServiceInstancesChangedEvent("app1", app1Instances);
        listener.onEvent(app1_event);

        // notify app2 instance change
        ServiceInstancesChangedEvent app2_event = new ServiceInstancesChangedEvent("app2", app2Instances);
        listener.onEvent(app2_event);

        // check
        Map<String, List<ServiceInstance>> allInstances = listener.getAllInstances();
        Assertions.assertEquals(2, allInstances.size());
        Assertions.assertEquals(3, allInstances.get("app1").size());
        Assertions.assertEquals(4, allInstances.get("app2").size());

        ProtocolServiceKey protocolServiceKey1 = new ProtocolServiceKey(service1, null, null, "dubbo");
        ProtocolServiceKey protocolServiceKey2 = new ProtocolServiceKey(service2, null, null, "dubbo");
        ProtocolServiceKey protocolServiceKey3 = new ProtocolServiceKey(service3, null, null, "dubbo");

        List<URL> serviceUrls = listener.getAddresses(protocolServiceKey1, consumerURL);
        Assertions.assertEquals(7, serviceUrls.size());
        List<URL> serviceUrls2 = listener.getAddresses(protocolServiceKey2, consumerURL);
        Assertions.assertEquals(4, serviceUrls2.size());
        assertTrue(serviceUrls2.get(0).getIp().contains("30.10."));
        List<URL> serviceUrls3 = listener.getAddresses(protocolServiceKey3, consumerURL);
        Assertions.assertEquals(2, serviceUrls3.size());
        assertTrue(serviceUrls3.get(0).getIp().contains("30.10."));
    }

    // 正常场景。多应用，app1 app2，空地址通知（边界条件）能否解析出正确的空地址列表
    @Test
    @Order(4)
    public void testMultipleAppEmptyNotification() {
        Set<String> serviceNames = new HashSet<>();
        serviceNames.add("app1");
        serviceNames.add("app2");
        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);

        // notify app1 instance change
        ServiceInstancesChangedEvent app1_event = new ServiceInstancesChangedEvent("app1", app1Instances);
        listener.onEvent(app1_event);

        // notify app2 instance change
        ServiceInstancesChangedEvent app2_event = new ServiceInstancesChangedEvent("app2", app2Instances);
        listener.onEvent(app2_event);

        // empty notification
        ServiceInstancesChangedEvent app1_event_again = new ServiceInstancesChangedEvent("app1", Collections.EMPTY_LIST);
        listener.onEvent(app1_event_again);

        // check app1 cleared
        Map<String, List<ServiceInstance>> allInstances = listener.getAllInstances();
        Assertions.assertEquals(2, allInstances.size());
        Assertions.assertEquals(0, allInstances.get("app1").size());
        Assertions.assertEquals(4, allInstances.get("app2").size());

        ProtocolServiceKey protocolServiceKey1 = new ProtocolServiceKey(service1, null, null, "dubbo");
        ProtocolServiceKey protocolServiceKey2 = new ProtocolServiceKey(service2, null, null, "dubbo");
        ProtocolServiceKey protocolServiceKey3 = new ProtocolServiceKey(service3, null, null, "dubbo");

        List<URL> serviceUrls = listener.getAddresses(protocolServiceKey1, consumerURL);
        Assertions.assertEquals(4, serviceUrls.size());
        assertTrue(serviceUrls.get(0).getIp().contains("30.10."));
        List<URL> serviceUrls2 = listener.getAddresses(protocolServiceKey2, consumerURL);
        Assertions.assertEquals(4, serviceUrls2.size());
        assertTrue(serviceUrls2.get(0).getIp().contains("30.10."));
        List<URL> serviceUrls3 = listener.getAddresses(protocolServiceKey3, consumerURL);
        Assertions.assertEquals(2, serviceUrls3.size());
        assertTrue(serviceUrls3.get(0).getIp().contains("30.10."));

        // app2 empty notification
        ServiceInstancesChangedEvent app2_event_again = new ServiceInstancesChangedEvent("app2", Collections.EMPTY_LIST);
        listener.onEvent(app2_event_again);

        // check app2 cleared
        Map<String, List<ServiceInstance>> allInstances_app2 = listener.getAllInstances();
        Assertions.assertEquals(2, allInstances_app2.size());
        Assertions.assertEquals(0, allInstances_app2.get("app1").size());
        Assertions.assertEquals(0, allInstances_app2.get("app2").size());

        assertTrue(isEmpty(listener.getAddresses(protocolServiceKey1, consumerURL)));
        assertTrue(isEmpty(listener.getAddresses(protocolServiceKey2, consumerURL)));
        assertTrue(isEmpty(listener.getAddresses(protocolServiceKey3, consumerURL)));
    }

    // 正常场景。检查instance listener -> service listener(Directory)地址推送流程
    @Test
    @Order(5)
    public void testServiceListenerNotification() {
        Set<String> serviceNames = new HashSet<>();
        serviceNames.add("app1");
        serviceNames.add("app2");
        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
        NotifyListener demoServiceListener = Mockito.mock(NotifyListener.class);
        when(demoServiceListener.getConsumerUrl()).thenReturn(consumerURL);
        NotifyListener demoService2Listener = Mockito.mock(NotifyListener.class);
        when(demoService2Listener.getConsumerUrl()).thenReturn(consumerURL2);
        listener.addListenerAndNotify(consumerURL, demoServiceListener);
        listener.addListenerAndNotify(consumerURL2, demoService2Listener);
        // notify app1 instance change
        ServiceInstancesChangedEvent app1_event = new ServiceInstancesChangedEvent("app1", app1Instances);
        listener.onEvent(app1_event);

        // check
        ArgumentCaptor<List<URL>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(demoServiceListener, Mockito.times(1)).notify(captor.capture());
        List<URL> notifiedUrls = captor.getValue();
        Assertions.assertEquals(3, notifiedUrls.size());
        ArgumentCaptor<List<URL>> captor2 = ArgumentCaptor.forClass(List.class);
        Mockito.verify(demoService2Listener, Mockito.times(1)).notify(captor2.capture());
        List<URL> notifiedUrls2 = captor2.getValue();
        Assertions.assertEquals(1, notifiedUrls2.size());

        // notify app2 instance change
        ServiceInstancesChangedEvent app2_event = new ServiceInstancesChangedEvent("app2", app2Instances);
        listener.onEvent(app2_event);

        // check
        ArgumentCaptor<List<URL>> app2_captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(demoServiceListener, Mockito.times(2)).notify(app2_captor.capture());
        List<URL> app2_notifiedUrls = app2_captor.getValue();
        Assertions.assertEquals(7, app2_notifiedUrls.size());
        ArgumentCaptor<List<URL>> app2_captor2 = ArgumentCaptor.forClass(List.class);
        Mockito.verify(demoService2Listener, Mockito.times(2)).notify(app2_captor2.capture());
        List<URL> app2_notifiedUrls2 = app2_captor2.getValue();
        Assertions.assertEquals(4, app2_notifiedUrls2.size());

        // test service listener still get notified when added after instance notification.
        NotifyListener demoService3Listener = Mockito.mock(NotifyListener.class);
        when(demoService3Listener.getConsumerUrl()).thenReturn(consumerURL3);
        listener.addListenerAndNotify(consumerURL3, demoService3Listener);
        Mockito.verify(demoService3Listener, Mockito.times(1)).notify(Mockito.anyList());
    }

    @Test
    @Order(6)
    public void testMultiServiceListenerNotification() {
        Set<String> serviceNames = new HashSet<>();
        serviceNames.add("app1");
        serviceNames.add("app2");
        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
        NotifyListener demoServiceListener1 = Mockito.mock(NotifyListener.class);
        when(demoServiceListener1.getConsumerUrl()).thenReturn(consumerURL);
        NotifyListener demoServiceListener2 = Mockito.mock(NotifyListener.class);
        when(demoServiceListener2.getConsumerUrl()).thenReturn(consumerURL);
        NotifyListener demoService2Listener1 = Mockito.mock(NotifyListener.class);
        when(demoService2Listener1.getConsumerUrl()).thenReturn(consumerURL2);
        NotifyListener demoService2Listener2 = Mockito.mock(NotifyListener.class);
        when(demoService2Listener2.getConsumerUrl()).thenReturn(consumerURL2);
        listener.addListenerAndNotify(consumerURL, demoServiceListener1);
        listener.addListenerAndNotify(consumerURL, demoServiceListener2);
        listener.addListenerAndNotify(consumerURL2, demoService2Listener1);
        listener.addListenerAndNotify(consumerURL2, demoService2Listener2);
        // notify app1 instance change
        ServiceInstancesChangedEvent app1_event = new ServiceInstancesChangedEvent("app1", app1Instances);
        listener.onEvent(app1_event);

        // check
        ArgumentCaptor<List<URL>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(demoServiceListener1, Mockito.times(1)).notify(captor.capture());
        List<URL> notifiedUrls = captor.getValue();
        Assertions.assertEquals(3, notifiedUrls.size());
        ArgumentCaptor<List<URL>> captor2 = ArgumentCaptor.forClass(List.class);
        Mockito.verify(demoService2Listener1, Mockito.times(1)).notify(captor2.capture());
        List<URL> notifiedUrls2 = captor2.getValue();
        Assertions.assertEquals(1, notifiedUrls2.size());

        // notify app2 instance change
        ServiceInstancesChangedEvent app2_event = new ServiceInstancesChangedEvent("app2", app2Instances);
        listener.onEvent(app2_event);

        // check
        ArgumentCaptor<List<URL>> app2_captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(demoServiceListener1, Mockito.times(2)).notify(app2_captor.capture());
        List<URL> app2_notifiedUrls = app2_captor.getValue();
        Assertions.assertEquals(7, app2_notifiedUrls.size());
        ArgumentCaptor<List<URL>> app2_captor2 = ArgumentCaptor.forClass(List.class);
        Mockito.verify(demoService2Listener1, Mockito.times(2)).notify(app2_captor2.capture());
        List<URL> app2_notifiedUrls2 = app2_captor2.getValue();
        Assertions.assertEquals(4, app2_notifiedUrls2.size());

        // test service listener still get notified when added after instance notification.
        NotifyListener demoService3Listener = Mockito.mock(NotifyListener.class);
        when(demoService3Listener.getConsumerUrl()).thenReturn(consumerURL3);
        listener.addListenerAndNotify(consumerURL3, demoService3Listener);
        Mockito.verify(demoService3Listener, Mockito.times(1)).notify(Mockito.anyList());
    }

    /**
     * Test subscribe multiple protocols
     */
    @Test
    @Order(7)
    public void testSubscribeMultipleProtocols() {
        Set<String> serviceNames = new HashSet<>();
        serviceNames.add("app1");
        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
        // no protocol specified, consume all instances
        NotifyListener demoServiceListener1 = Mockito.mock(NotifyListener.class);
        when(demoServiceListener1.getConsumerUrl()).thenReturn(noProtocolConsumerURL);
        listener.addListenerAndNotify(noProtocolConsumerURL, demoServiceListener1);
        // multiple protocols specified
        NotifyListener demoServiceListener2 = Mockito.mock(NotifyListener.class);
        when(demoServiceListener2.getConsumerUrl()).thenReturn(multipleProtocolsConsumerURL);
        listener.addListenerAndNotify(multipleProtocolsConsumerURL, demoServiceListener2);
        // one protocol specified
        NotifyListener demoServiceListener3 = Mockito.mock(NotifyListener.class);
        when(demoServiceListener3.getConsumerUrl()).thenReturn(singleProtocolsConsumerURL);
        listener.addListenerAndNotify(singleProtocolsConsumerURL, demoServiceListener3);

        // notify app1 instance change
        ServiceInstancesChangedEvent app1_event = new ServiceInstancesChangedEvent("app1", app1InstancesMultipleProtocols);
        listener.onEvent(app1_event);

        // check instances expose framework supported default protocols(currently dubbo, triple and rest) are notified
        ArgumentCaptor<List<URL>> default_protocol_captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(demoServiceListener1, Mockito.times(1)).notify(default_protocol_captor.capture());
        List<URL> default_protocol_notifiedUrls = default_protocol_captor.getValue();
        Assertions.assertEquals(4, default_protocol_notifiedUrls.size());
        // check instances expose protocols in consuming list(dubbo and triple) are notified
        ArgumentCaptor<List<URL>> multi_protocols_captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(demoServiceListener2, Mockito.times(1)).notify(multi_protocols_captor.capture());
        List<URL> multi_protocol_notifiedUrls = multi_protocols_captor.getValue();
        Assertions.assertEquals(4, multi_protocol_notifiedUrls.size());
        // check instances expose protocols in consuming list(only triple) are notified
        ArgumentCaptor<List<URL>> single_protocols_captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(demoServiceListener3, Mockito.times(1)).notify(single_protocols_captor.capture());
        List<URL> single_protocol_notifiedUrls = single_protocols_captor.getValue();
        Assertions.assertEquals(1, single_protocol_notifiedUrls.size());
    }

    /**
     * Test subscribe multiple groups
     */
    @Test
    @Order(8)
    public void testSubscribeMultipleGroups() {
        Set<String> serviceNames = new HashSet<>();
        serviceNames.add("app1");
        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);

        // notify instance change
        ServiceInstancesChangedEvent event = new ServiceInstancesChangedEvent("app1", app1Instances);
        listener.onEvent(event);

        Map<String, List<ServiceInstance>> allInstances = listener.getAllInstances();
        Assertions.assertEquals(1, allInstances.size());
        Assertions.assertEquals(3, allInstances.get("app1").size());

        ProtocolServiceKey protocolServiceKey = new ProtocolServiceKey(service1, null, null, "dubbo");
        List<URL> serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(3, serviceUrls.size());
        assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);

        protocolServiceKey = new ProtocolServiceKey(service1, null, "", "dubbo");
        serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(3, serviceUrls.size());
        assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);

        protocolServiceKey = new ProtocolServiceKey(service1, null, ",group1", "dubbo");
        serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(3, serviceUrls.size());
        assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);

        protocolServiceKey = new ProtocolServiceKey(service1, null, "group1,", "dubbo");
        serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(3, serviceUrls.size());
        assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);

        protocolServiceKey = new ProtocolServiceKey(service1, null, "*", "dubbo");
        serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(3, serviceUrls.size());
        assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);

        protocolServiceKey = new ProtocolServiceKey(service1, null, "group1", "dubbo");
        serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(0, serviceUrls.size());

        protocolServiceKey = new ProtocolServiceKey(service1, null, "group1,group2", "dubbo");
        serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(0, serviceUrls.size());

        protocolServiceKey = new ProtocolServiceKey(service1, null, "group1,,group2", "dubbo");
        serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(3, serviceUrls.size());
        assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    }

    /**
     * Test subscribe multiple versions
     */
    @Test
    @Order(9)
    public void testSubscribeMultipleVersions() {
        Set<String> serviceNames = new HashSet<>();
        serviceNames.add("app1");
        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);

        // notify instance change
        ServiceInstancesChangedEvent event = new ServiceInstancesChangedEvent("app1", app1Instances);
        listener.onEvent(event);

        Map<String, List<ServiceInstance>> allInstances = listener.getAllInstances();
        Assertions.assertEquals(1, allInstances.size());
        Assertions.assertEquals(3, allInstances.get("app1").size());

        ProtocolServiceKey protocolServiceKey = new ProtocolServiceKey(service1, null, null, "dubbo");
        List<URL> serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(3, serviceUrls.size());
        assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);

        protocolServiceKey = new ProtocolServiceKey(service1, "", null, "dubbo");
        serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(3, serviceUrls.size());
        assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);

        protocolServiceKey = new ProtocolServiceKey(service1, "*", null, "dubbo");
        serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(3, serviceUrls.size());
        assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);

        protocolServiceKey = new ProtocolServiceKey(service1, ",1.0.0", null, "dubbo");
        serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(3, serviceUrls.size());
        assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);

        protocolServiceKey = new ProtocolServiceKey(service1, "1.0.0,", null, "dubbo");
        serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(3, serviceUrls.size());
        assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);

        protocolServiceKey = new ProtocolServiceKey(service1, "1.0.0,,1.0.1", null, "dubbo");
        serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(3, serviceUrls.size());
        assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);

        protocolServiceKey = new ProtocolServiceKey(service1, "1.0.1,1.0.0", null, "dubbo");
        serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
        Assertions.assertEquals(0, serviceUrls.size());
    }

    // revision 异常场景。第一次启动，完全拿不到metadata，只能通知部分地址
    @Test
    @Order(10)
    public void testRevisionFailureOnStartup() {
        Set<String> serviceNames = new HashSet<>();
        serviceNames.add("app1");
        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
        // notify app1 instance change
        ServiceInstancesChangedEvent failed_revision_event = new ServiceInstancesChangedEvent("app1", app1FailedInstances);
        listener.onEvent(failed_revision_event);


        ProtocolServiceKey protocolServiceKey1 = new ProtocolServiceKey(service1, null, null, "dubbo");
        ProtocolServiceKey protocolServiceKey2 = new ProtocolServiceKey(service2, null, null, "dubbo");

        List<URL> serviceUrls = listener.getAddresses(protocolServiceKey1, consumerURL);
        List<URL> serviceUrls2 = listener.getAddresses(protocolServiceKey2, consumerURL);

        assertTrue(isNotEmpty(serviceUrls));
        assertTrue(isNotEmpty(serviceUrls2));
    }

    // revision 异常场景。运行中地址通知，拿不到revision就用老版本revision
    @Test
    @Order(11)
    public void testRevisionFailureOnNotification() {
        Set<String> serviceNames = new HashSet<>();
        serviceNames.add("app1");
        serviceNames.add("app2");
        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);

        // notify app1 instance change
        ServiceInstancesChangedEvent event = new ServiceInstancesChangedEvent("app1", app1Instances);
        listener.onEvent(event);

        when(serviceDiscovery.getRemoteMetadata(eq("222"), anyList())).thenAnswer(new Answer<MetadataInfo>() {
            @Override
            public MetadataInfo answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (Thread.currentThread().getName().contains("Dubbo-framework-metadata-retry")) {
                    return metadataInfo_222;
                }
                return MetadataInfo.EMPTY;
            }
        });

        ServiceInstancesChangedEvent event2 = new ServiceInstancesChangedEvent("app2", app1FailedInstances2);
        listener.onEvent(event2);

        // event2 did not really take effect
        ProtocolServiceKey protocolServiceKey1 = new ProtocolServiceKey(service1, null, null, "dubbo");
        ProtocolServiceKey protocolServiceKey2 = new ProtocolServiceKey(service2, null, null, "dubbo");

        Assertions.assertEquals(3, listener.getAddresses(protocolServiceKey1, consumerURL).size());
        assertTrue(isEmpty(listener.getAddresses(protocolServiceKey2, consumerURL)));

        //
        init();

        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // check recovered after retry.
        List<URL> serviceUrls_after_retry = listener.getAddresses(protocolServiceKey1, consumerURL);
        Assertions.assertEquals(5, serviceUrls_after_retry.size());
        List<URL> serviceUrls2_after_retry = listener.getAddresses(protocolServiceKey2, consumerURL);
        Assertions.assertEquals(2, serviceUrls2_after_retry.size());

    }

    // Abnormal case. Instance does not have revision
    @Test
    @Order(12)
    public void testInstanceWithoutRevision() {
        Set<String> serviceNames = new HashSet<>();
        serviceNames.add("app1");
        ServiceDiscovery serviceDiscovery = Mockito.mock(ServiceDiscovery.class);
        listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
        ServiceInstancesChangedListener spyListener = Mockito.spy(listener);
        Mockito.doReturn(null).when(metadataService).getMetadataInfo(eq(null));
        ServiceInstancesChangedEvent event = new ServiceInstancesChangedEvent("app1", app1InstancesWithNoRevision);
        spyListener.onEvent(event);
        // notification succeeded
        assertTrue(true);
    }

    Set<String> getExpectedSet(List<String> list) {
        return new HashSet<>(list);
    }

    static List<ServiceInstance> buildInstances(List<Object> rawURls) {
        List<ServiceInstance> instances = new ArrayList<>();

        for (Object obj : rawURls) {
            String rawURL = (String) obj;
            DefaultServiceInstance instance = new DefaultServiceInstance();
            final URL dubboUrl = URL.valueOf(rawURL);
            instance.setRawAddress(rawURL);
            instance.setHost(dubboUrl.getHost());
            instance.setEnabled(true);
            instance.setHealthy(true);
            instance.setPort(dubboUrl.getPort());
            instance.setRegistryCluster("default");
            instance.setApplicationModel(ApplicationModel.defaultModel());

            Map<String, String> metadata = new HashMap<>();
            if (StringUtils.isNotEmpty(dubboUrl.getParameter(REVISION_KEY))) {
                metadata.put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, dubboUrl.getParameter(REVISION_KEY));
            }
            instance.setMetadata(metadata);

            instances.add(instance);
        }

        return instances;
    }

    private void clearMetadataCache() {
        try {
            MockServiceDiscovery mockServiceDiscovery = (MockServiceDiscovery) ServiceInstancesChangedListenerWithoutEmptyProtectTest.serviceDiscovery;
            MetaCacheManager metaCacheManager = mockServiceDiscovery.getMetaCacheManager();
            Field cacheField = metaCacheManager.getClass().getDeclaredField("cache");
            cacheField.setAccessible(true);
            LRUCache<String, MetadataInfo> cache = (LRUCache<String, MetadataInfo>) cacheField.get(metaCacheManager);
            cache.clear();
            cacheField.setAccessible(false);
        } catch (Exception e) {
            // ignore
        }
    }
}
