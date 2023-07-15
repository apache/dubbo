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
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDED_BY;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;

/**
 * @see AbstractServiceNameMapping
 */
class AbstractServiceNameMappingTest {

    private MockServiceNameMapping mapping = new MockServiceNameMapping(ApplicationModel.defaultModel());
    private MockServiceNameMapping2 mapping2 = new MockServiceNameMapping2(ApplicationModel.defaultModel());

    URL url = URL.valueOf("dubbo://127.0.0.1:21880/" + AbstractServiceNameMappingTest.class.getName());

    @BeforeEach
    public void setUp() throws Exception {
    }

    @AfterEach
    public void clearup() {
        mapping.removeCachedMapping(ServiceNameMapping.buildMappingKey(url));
    }

    @Test
    void testGetServices() {
        url = url.addParameter(PROVIDED_BY, "app1,app2");
        Set<String> services = mapping.getMapping(url);
        Assertions.assertTrue(services.contains("app1"));
        Assertions.assertTrue(services.contains("app2"));

//        // remove mapping cache, check get() works.
//        mapping.removeCachedMapping(ServiceNameMapping.buildMappingKey(url));
//        services = mapping.initInterfaceAppMapping(url);
//        Assertions.assertTrue(services.contains("remote-app1"));
//        Assertions.assertTrue(services.contains("remote-app2"));


//        Assertions.assertNotNull(mapping.getCachedMapping(url));
//        Assertions.assertIterableEquals(mapping.getCachedMapping(url), services);
    }

    @Test
    void testGetAndListener() {
        URL registryURL = URL.valueOf("registry://127.0.0.1:7777/test");
        registryURL = registryURL.addParameter(SUBSCRIBED_SERVICE_NAMES_KEY, "registry-app1");

        Set<String> services = mapping2.getAndListen(registryURL, url, null);
        Assertions.assertTrue(services.contains("registry-app1"));

        // remove mapping cache, check get() works.
        mapping2.removeCachedMapping(ServiceNameMapping.buildMappingKey(url));
        mapping2.enabled = true;
        services = mapping2.getAndListen(registryURL, url, new MappingListener() {
            @Override
            public void onEvent(MappingChangedEvent event) {

            }

            @Override
            public void stop() {

            }
        });
        Assertions.assertTrue(services.contains("remote-app3"));

    }

    private class MockServiceNameMapping extends AbstractServiceNameMapping {

        public boolean enabled = false;

        public MockServiceNameMapping(ApplicationModel applicationModel) {
            super(applicationModel);
        }

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
        protected void removeListener(URL url, MappingListener mappingListener) {

        }

        @Override
        public boolean map(URL url) {
            return false;
        }

        @Override
        public boolean hasValidMetadataCenter() {
            return false;
        }
    }

    private class MockServiceNameMapping2 extends AbstractServiceNameMapping {

        public boolean enabled = false;

        public MockServiceNameMapping2(ApplicationModel applicationModel) {
            super(applicationModel);
        }

        @Override
        public Set<String> get(URL url) {
            return Collections.emptySet();
        }

        @Override
        public Set<String> getAndListen(URL url, MappingListener mappingListener) {
            if (!enabled) {
                return Collections.emptySet();
            }
            return new HashSet<>(Arrays.asList("remote-app3"));
        }

        @Override
        protected void removeListener(URL url, MappingListener mappingListener) {

        }

        @Override
        public boolean map(URL url) {
            return false;
        }

        @Override
        public boolean hasValidMetadataCenter() {
            return false;
        }
    }

}
