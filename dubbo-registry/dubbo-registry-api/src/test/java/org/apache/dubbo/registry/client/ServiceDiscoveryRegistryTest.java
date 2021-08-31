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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.ServiceNameMapping;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.event.listener.MockServiceInstancesChangedListener;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.apache.dubbo.common.constants.CommonConstants.CHECK_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_CHAR_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.metadata.ServiceNameMapping.toStringKeys;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class ServiceDiscoveryRegistryTest {
    public static final String APP_NAME1 = "app1";
    public static final String APP_NAME2 = "app2";
    public static final String APP_NAME3 = "app3";

    private static ServiceNameMapping mapping = mock(ServiceNameMapping.class);
    private static URL registryURL = URL.valueOf("zookeeper://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService");
    private static URL url = URL.valueOf("consumer://127.0.0.1/TestService?interface=TestService1&check=false");
    private static NotifyListener testServiceListener = mock(NotifyListener.class);

    private static List<ServiceInstance> instanceList1 = new ArrayList<>();
    private static List<ServiceInstance> instanceList2 = new ArrayList<>();

    private ServiceDiscoveryRegistry serviceDiscoveryRegistry;
    private ServiceDiscovery serviceDiscovery;
    private MockServiceInstancesChangedListener instanceListener;
    private WritableMetadataService spiedMetadataService;

    @BeforeAll
    public static void setUp() {
        instanceList1.add(new DefaultServiceInstance());
        instanceList1.add(new DefaultServiceInstance());
        instanceList1.add(new DefaultServiceInstance());

        instanceList2.add(new DefaultServiceInstance());
        instanceList2.add(new DefaultServiceInstance());

    }

    @BeforeEach
    public void init() {
        serviceDiscovery = mock(ServiceDiscovery.class);
        instanceListener = spy(new MockServiceInstancesChangedListener(Collections.emptySet(), serviceDiscovery));
        doNothing().when(instanceListener).onEvent(any());
        when(serviceDiscovery.createListener(any())).thenReturn(instanceListener);
        when(serviceDiscovery.getInstances(any())).thenReturn(Collections.emptyList());

        spiedMetadataService  = spy(WritableMetadataService.getDefaultExtension());
        serviceDiscoveryRegistry = new ServiceDiscoveryRegistry(registryURL, serviceDiscovery, spiedMetadataService);
    }

    /**
     * Test subscribe
     *  - Normal case
     *  - Exception case
     *    - check=true
     *    - check=false
     */
    @Test
    public void testDoSubscribe() {
        try (MockedStatic<ServiceNameMapping> mockStaticMapping = Mockito.mockStatic(ServiceNameMapping.class, withSettings().defaultAnswer(CALLS_REAL_METHODS))) {
            mockStaticMapping.when(ServiceNameMapping::getDefaultExtension).thenReturn(mapping);
            // Exception case, no interface-app mapping found
            when(mapping.getAndListenServices(any(), any(), any())).thenReturn(Collections.emptySet());
            // when check = false
            try {
                serviceDiscoveryRegistry.doSubscribe(url, testServiceListener);
            } finally {
                spiedMetadataService.unsubscribeURL(url);
            }
            // when check = true
            URL checkURL = url.addParameter(CHECK_KEY, true);
            Exception exceptionShouldHappen = null;
            try {
                serviceDiscoveryRegistry.doSubscribe(checkURL, testServiceListener);
            } catch (IllegalStateException e) {
                exceptionShouldHappen = e;
            } finally {
                spiedMetadataService.unsubscribeURL(checkURL);
            }
            if (exceptionShouldHappen == null) {
                fail();
            }

            // Normal case
            Set<String> singleApp = new HashSet<>();
            singleApp.add(APP_NAME1);
            when(mapping.getAndListenServices(any(), any(), any())).thenReturn(singleApp);
            try {
                serviceDiscoveryRegistry.doSubscribe(checkURL, testServiceListener);
            } finally {
                spiedMetadataService.unsubscribeURL(checkURL);
            }
        }
    }

    /**
     * Test instance listener registration
     *  - one app
     *  - multi apps
     *  - repeat same multi apps, instance listener shared
     *  - protocol included in key
     *  - instance listener gets notified
     *  - instance listener and service listener rightly mapped
     */
    @Test
    public void testSubscribeURLs() {
        try (MockedStatic<ServiceNameMapping> mockStaticMapping = Mockito.mockStatic(ServiceNameMapping.class, withSettings().defaultAnswer(CALLS_REAL_METHODS))) {
            mockStaticMapping.when(ServiceNameMapping::getDefaultExtension).thenReturn(mapping);
            // interface to single app mapping
            Set<String> singleApp = new TreeSet<>();
            singleApp.add(APP_NAME1);
            serviceDiscoveryRegistry.subscribeURLs(url, testServiceListener, singleApp);

            assertEquals(1, serviceDiscoveryRegistry.getServiceListeners().size());
            verify(testServiceListener, times(1)).addServiceListener(instanceListener);
            verify(instanceListener, never()).onEvent(any());
            verify(serviceDiscovery, times(1)).addServiceInstancesChangedListener(instanceListener);

            // interface to multiple apps mapping
            Set<String> multiApps = new TreeSet<>();
            multiApps.add(APP_NAME1);
            multiApps.add(APP_NAME2);
            MockServiceInstancesChangedListener multiAppsInstanceListener = spy(new MockServiceInstancesChangedListener(multiApps, serviceDiscovery));
            doNothing().when(multiAppsInstanceListener).onEvent(any());
            List<URL> urls = new ArrayList<>();
            urls.add(URL.valueOf("dubbo://127.0.0.1:20880/TestService"));
            doReturn(urls).when(multiAppsInstanceListener).getAddresses(any(), any());
            when(serviceDiscovery.createListener(multiApps)).thenReturn(multiAppsInstanceListener);
            when(serviceDiscovery.getInstances(APP_NAME1)).thenReturn(instanceList1);
            when(serviceDiscovery.getInstances(APP_NAME2)).thenReturn(instanceList2);
            serviceDiscoveryRegistry.subscribeURLs(url, testServiceListener, multiApps);

            assertEquals(2, serviceDiscoveryRegistry.getServiceListeners().size());
            assertEquals(instanceListener, serviceDiscoveryRegistry.getServiceListeners().get(toStringKeys(singleApp)));
            assertEquals(multiAppsInstanceListener, serviceDiscoveryRegistry.getServiceListeners().get(toStringKeys(multiApps)));
            verify(testServiceListener, times(1)).addServiceListener(multiAppsInstanceListener);
            verify(multiAppsInstanceListener, times(2)).onEvent(any());
            verify(multiAppsInstanceListener, times(1)).addListenerAndNotify(any(), eq(testServiceListener));
            verify(serviceDiscovery, times(1)).addServiceInstancesChangedListener(multiAppsInstanceListener);
            ArgumentCaptor<List<URL>> captor = ArgumentCaptor.forClass(List.class);
            verify(testServiceListener).notify(captor.capture());
            assertEquals(urls, captor.getValue());

            // different interface mapping to the same apps
            NotifyListener testServiceListener2 = mock(NotifyListener.class);
            URL url2 = URL.valueOf("consumer://127.0.0.1/TestService2?interface=TestService1&check=false&protocol=tri");
            serviceDiscoveryRegistry.subscribeURLs(url2, testServiceListener2, multiApps);
            // check instance listeners not changed, methods not called
            assertEquals(2, serviceDiscoveryRegistry.getServiceListeners().size());
            assertEquals(multiAppsInstanceListener, serviceDiscoveryRegistry.getServiceListeners().get(toStringKeys(multiApps)));
            verify(multiAppsInstanceListener, times(1)).addListenerAndNotify(any(), eq(testServiceListener));
            // still called once, not executed this time
            verify(serviceDiscovery, times(1)).addServiceInstancesChangedListener(multiAppsInstanceListener);
            // check different protocol
            Map<String, NotifyListener> serviceListeners = multiAppsInstanceListener.getServiceListeners();
            assertEquals(2, serviceListeners.size());
            String protocolServiceKey = url2.getServiceKey() + GROUP_CHAR_SEPARATOR + url2.getParameter(PROTOCOL_KEY, DUBBO);
            assertEquals(testServiceListener2, serviceListeners.get(protocolServiceKey));
        }
    }

    /**
     * repeat of {@link this#testSubscribeURLs()} with multi threads
     */
    @Test
    public void testConcurrencySubscribe() {
        // TODO
    }

    @Test
    public void testUnsubscribe() {
        // do subscribe to prepare for unsubscribe verification
        Set<String> multiApps = new TreeSet<>();
        multiApps.add(APP_NAME1);
        multiApps.add(APP_NAME2);
        NotifyListener testServiceListener2 = mock(NotifyListener.class);
        URL url2 = URL.valueOf("consumer://127.0.0.1/TestService2?interface=TestService1&check=false&protocol=tri");
        serviceDiscoveryRegistry.subscribeURLs(url, testServiceListener, multiApps);
        serviceDiscoveryRegistry.subscribeURLs(url2, testServiceListener2, multiApps);
        assertEquals(1, serviceDiscoveryRegistry.getServiceListeners().size());

        // do unsubscribe
        when(spiedMetadataService.getCachedMapping(url2)).thenReturn(multiApps);
        serviceDiscoveryRegistry.doUnsubscribe(url2, testServiceListener2);
        assertEquals(1, serviceDiscoveryRegistry.getServiceListeners().size());
        assertEquals(1, serviceDiscoveryRegistry.getServiceListeners().size());
        ServiceInstancesChangedListener instancesChangedListener = serviceDiscoveryRegistry.getServiceListeners().entrySet().iterator().next().getValue();
        assertTrue(instancesChangedListener.hasListeners());
        when(spiedMetadataService.getCachedMapping(url)).thenReturn(multiApps);
        serviceDiscoveryRegistry.doUnsubscribe(url, testServiceListener);
        assertEquals(0, serviceDiscoveryRegistry.getServiceListeners().size());
        assertFalse(instancesChangedListener.hasListeners());
    }

}
