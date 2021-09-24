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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDED_BY;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;

/**
 * @see AbstractServiceNameMapping
 */
class AbstractServiceNameMappingTest {

    private MockServiceNameMapping mapping = new MockServiceNameMapping();
    private MockWritableMetadataService writableMetadataService = new MockWritableMetadataService();

    @BeforeEach
    public void setUp() throws Exception {
        Field metadataService = mapping.getClass().getSuperclass().getDeclaredField("metadataService");
        metadataService.setAccessible(true);
        metadataService.set(mapping, writableMetadataService);
    }

    @Test
    void testGetServices() {
        URL url = URL.valueOf("dubbo://127.0.0.1:21880/" + AbstractServiceNameMappingTest.class);
        url = url.addParameter(PROVIDED_BY, "app1,app2");
        Set<String> services = mapping.getServices(url);
        Assertions.assertTrue(services.contains("app1"));
        Assertions.assertTrue(services.contains("app2"));

        url = url.removeParameter(PROVIDED_BY);
        services = mapping.getServices(url);
        Assertions.assertTrue(services.contains("remote-app1"));
        Assertions.assertTrue(services.contains("remote-app2"));


        Map<String, Set<String>> cachedMapping = writableMetadataService.getCachedMapping();
        Assertions.assertNotNull(cachedMapping);
        Assertions.assertTrue(cachedMapping.containsKey(ServiceNameMapping.buildMappingKey(url)));
        Assertions.assertIterableEquals(cachedMapping.get(ServiceNameMapping.buildMappingKey(url)), services);

    }

    @Test
    public void testGetAndListener() {
        URL url = URL.valueOf("dubbo://127.0.0.1:21880/" + AbstractServiceNameMappingTest.class);
        URL registryURL = URL.valueOf("registry://127.0.0.1:7777/test");
        registryURL = registryURL.addParameter(SUBSCRIBED_SERVICE_NAMES_KEY, "registry-app1");

        Set<String> services = mapping.getAndListenServices(registryURL, url, null);
        Assertions.assertTrue(services.contains("registry-app1"));

        mapping.enabled = true;
        services = mapping.getAndListenServices(registryURL, url, event -> {
        });
        Assertions.assertTrue(services.contains("remote-app3"));

    }


    private class MockServiceNameMapping extends AbstractServiceNameMapping {

        public boolean enabled = false;

        @Override
        public Set<String> get(URL url) {
            return new HashSet<>(Arrays.asList("remote-app1", "remote-app2"));
        }

        @Override
        public Set<String> getAndListen(URL url, MappingListener mappingListener) {
            if (!enabled) {
                return Collections.emptySet();
            }
            return new HashSet<>(Arrays.asList("remote-app3"));
        }

        @Override
        public boolean map(URL url) {
            return false;
        }
    }

    private class MockWritableMetadataService implements WritableMetadataService {
        private final Map<String, Set<String>> serviceToAppsMapping = new HashMap<>();

        @Override
        public String serviceName() {
            return null;
        }

        @Override
        public SortedSet<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
            return null;
        }

        @Override
        public String getServiceDefinition(String serviceKey) {
            return null;
        }

        @Override
        public MetadataInfo getMetadataInfo(String revision) {
            return null;
        }

        @Override
        public Map<String, MetadataInfo> getMetadataInfos() {
            return null;
        }

        @Override
        public boolean exportURL(URL url) {
            return false;
        }

        @Override
        public boolean unexportURL(URL url) {
            return false;
        }

        @Override
        public boolean subscribeURL(URL url) {
            return false;
        }

        @Override
        public boolean unsubscribeURL(URL url) {
            return false;
        }

        @Override
        public void publishServiceDefinition(URL url) {

        }

        @Override
        public Set<String> getCachedMapping(String mappingKey) {
            return serviceToAppsMapping.get(mappingKey);
        }

        @Override
        public Set<String> getCachedMapping(URL consumerURL) {
            String serviceKey = ServiceNameMapping.buildMappingKey(consumerURL);
            return serviceToAppsMapping.get(serviceKey);
        }

        @Override
        public Set<String> removeCachedMapping(String serviceKey) {
            return serviceToAppsMapping.remove(serviceKey);
        }

        @Override
        public void putCachedMapping(String serviceKey, Set<String> apps) {
            serviceToAppsMapping.put(serviceKey, new TreeSet<>(apps));
        }

        @Override
        public Map<String, Set<String>> getCachedMapping() {
            return serviceToAppsMapping;
        }

        @Override
        public MetadataInfo getDefaultMetadataInfo() {
            return null;
        }
    }

}
