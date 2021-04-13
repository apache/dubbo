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
package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistry;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSortedSet;
import static org.apache.dubbo.common.URL.valueOf;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_TYPE;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;
import static org.apache.dubbo.rpc.Constants.ID_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ServiceDiscoveryRegistry} Test
 *
 * @since 2.7.5
 */
public class ServiceOrientedRegistryTest {

    private static final URL registryURL = valueOf("in-memory://localhost:12345")
            .addParameter(REGISTRY_TYPE_KEY, SERVICE_REGISTRY_TYPE)
            .addParameter(ID_KEY, "org.apache.dubbo.config.RegistryConfig#0")
            .addParameter(SUBSCRIBED_SERVICE_NAMES_KEY, "a, b , c,d,e ,");

    private static final String SERVICE_INTERFACE = "org.apache.dubbo.metadata.MetadataService";

    private static final String GROUP = "dubbo-provider";

    private static final String VERSION = "1.0.0";

    private static URL url = valueOf("dubbo://192.168.0.102:20880/" + SERVICE_INTERFACE +
            "?&application=" + GROUP +
            "&interface=" + SERVICE_INTERFACE +
            "&group=" + GROUP +
            "&version=" + VERSION +
            "&methods=getAllServiceKeys,getServiceRestMetadata,getExportedURLs,getAllExportedURLs" +
            "&side=" + PROVIDER_SIDE
    );

    private static URL url2 = url.setProtocol("rest");

    private WritableMetadataService metadataService;

    private ServiceDiscoveryRegistry registry;

    private NotifyListener notifyListener;

    @BeforeEach
    public void init() {
        registry = ServiceDiscoveryRegistry.create(registryURL);
        metadataService = WritableMetadataService.getDefaultExtension();
        notifyListener = new MyNotifyListener();
        ApplicationModel.getConfigManager().setApplication(new ApplicationConfig("Test"));
    }

    @Test
    public void testSupports() {
        assertTrue(ServiceDiscoveryRegistry.supports(registryURL));
    }

    @Test
    public void testCreate() {
        assertNotNull(registry);
    }

    @Test
    public void testRegister() {

        registry.register(url);

        SortedSet<String> urls = metadataService.getExportedURLs();

        assertTrue(urls.isEmpty());
        assertEquals(toSortedSet(), metadataService.getExportedURLs(SERVICE_INTERFACE));
        assertEquals(toSortedSet(), metadataService.getExportedURLs(SERVICE_INTERFACE, GROUP));

        String serviceInterface = "com.acme.UserService";

        URL newURL = url.setServiceInterface(serviceInterface).setPath(serviceInterface);

        registry.register(newURL);

        urls = metadataService.getExportedURLs();

        assertEquals(metadataService.getExportedURLs(serviceInterface, GROUP, VERSION), toSortedSet(urls.first()));
        assertEquals(metadataService.getExportedURLs(serviceInterface, GROUP, VERSION, DEFAULT_PROTOCOL), toSortedSet(urls.first()));

    }

    @Test
    public void testUnregister() {

        String serviceInterface = "com.acme.UserService";

        URL newURL = url.setServiceInterface(serviceInterface).setPath(serviceInterface);

        // register
        registry.register(newURL);

        SortedSet<String> urls = metadataService.getExportedURLs();

        assertEquals(1, urls.size());
        assertTrue(urls.iterator().next().contains(serviceInterface));
        assertEquals(metadataService.getExportedURLs(serviceInterface, GROUP, VERSION), urls);
        assertEquals(metadataService.getExportedURLs(serviceInterface, GROUP, VERSION, DEFAULT_PROTOCOL), urls);

        // unregister
        registry.unregister(newURL);

        urls = metadataService.getExportedURLs();

        assertEquals(toSortedSet(), urls);
        assertTrue(CollectionUtils.isEmpty(metadataService.getExportedURLs(serviceInterface)));
        assertTrue(CollectionUtils.isEmpty(metadataService.getExportedURLs(serviceInterface, GROUP)));
        assertTrue(CollectionUtils.isEmpty(metadataService.getExportedURLs(serviceInterface, GROUP, VERSION)));
        assertTrue(CollectionUtils.isEmpty(metadataService.getExportedURLs(serviceInterface, GROUP, VERSION, DEFAULT_PROTOCOL)));
    }

    @Test
    public void testSubscribe() {

        registry.subscribe(url, new MyNotifyListener());

        SortedSet<String> urls = metadataService.getSubscribedURLs();

        assertTrue(urls.isEmpty());

    }


    private class MyNotifyListener implements NotifyListener {

        private List<URL> cache = new LinkedList<>();

        @Override
        public void notify(List<URL> urls) {
            cache.addAll(urls);
        }

        public List<URL> getURLs() {
            return cache;
        }
    }

    private static <T extends Comparable<T>> SortedSet<T> toSortedSet(T... values) {
        return unmodifiableSortedSet(new TreeSet<>(asList(values)));
    }

}
