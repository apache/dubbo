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
package org.apache.dubbo.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLStrParser;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.ENABLE_EMPTY_PROTECTION_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheableFailbackRegistryTest {

    static String service;
    static URL serviceUrl;
    static URL registryUrl;
    static String urlStr;
    static String urlStr2;
    static String urlStr3;

    MockCacheableRegistryImpl registry;

    @BeforeAll
    static void setProperty() {
        System.setProperty("dubbo.application.url.cache.task.interval", "0");
        System.setProperty("dubbo.application.url.cache.clear.waiting", "0");
        FrameworkModel.destroyAll();
    }

    @BeforeEach
    public void setUp() throws Exception {
        service = "org.apache.dubbo.test.DemoService";
        serviceUrl = URL.valueOf("dubbo://127.0.0.1/org.apache.dubbo.test.DemoService?category=providers");
        registryUrl = URL.valueOf("http://1.2.3.4:9090/registry?check=false&file=N/A");
        urlStr = "dubbo%3A%2F%2F172.19.4.113%3A20880%2Forg.apache.dubbo.demo.DemoService%3Fside%3Dprovider%26timeout%3D3000";
        urlStr2 = "dubbo%3A%2F%2F172.19.4.114%3A20880%2Forg.apache.dubbo.demo.DemoService%3Fside%3Dprovider%26timeout%3D3000";
        urlStr3 = "dubbo%3A%2F%2F172.19.4.115%3A20880%2Forg.apache.dubbo.demo.DemoService%3Fside%3Dprovider%26timeout%3D3000";
    }

    @AfterEach
    public void tearDown() {
        registry.getStringUrls().clear();
        registry.getStringAddress().clear();
        registry.getStringParam().clear();
    }

    @Test
    void testFullURLCache() {
        final AtomicReference<Integer> resCount = new AtomicReference<>(0);
        registry = new MockCacheableRegistryImpl(registryUrl);
        URL url = URLStrParser.parseEncodedStr(urlStr);

        NotifyListener listener = urls -> resCount.set(urls.size());

        registry.addChildren(url);
        registry.subscribe(serviceUrl, listener);
        assertEquals(1, registry.getStringUrls().get(serviceUrl).size());
        assertEquals(1, resCount.get());

        registry.addChildren(url);
        registry.subscribe(serviceUrl, listener);
        assertEquals(1, registry.getStringUrls().get(serviceUrl).size());
        assertEquals(1, resCount.get());

        URL url1 = url.addParameter("k1", "v1");
        registry.addChildren(url1);
        registry.subscribe(serviceUrl, listener);
        assertEquals(2, registry.getStringUrls().get(serviceUrl).size());
        assertEquals(2, resCount.get());

        URL url2 = url1.setHost("192.168.1.1");
        registry.addChildren(url2);
        registry.subscribe(serviceUrl, listener);
        assertEquals(3, registry.getStringUrls().get(serviceUrl).size());
        assertEquals(3, resCount.get());
    }

    @Test
    void testURLAddressCache() {
        final AtomicReference<Integer> resCount = new AtomicReference<>(0);
        registry = new MockCacheableRegistryImpl(registryUrl);
        URL url = URLStrParser.parseEncodedStr(urlStr);

        NotifyListener listener = urls -> resCount.set(urls.size());

        registry.addChildren(url);
        registry.subscribe(serviceUrl, listener);
        assertEquals(1, registry.getStringAddress().size());
        assertEquals(1, resCount.get());

        URL url1 = url.addParameter("k1", "v1");
        registry.addChildren(url1);
        registry.subscribe(serviceUrl, listener);
        assertEquals(1, registry.getStringAddress().size());
        assertEquals(2, resCount.get());

        URL url2 = url1.setHost("192.168.1.1");
        registry.addChildren(url2);
        registry.subscribe(serviceUrl, listener);
        assertEquals(2, registry.getStringAddress().size());
        assertEquals(3, resCount.get());
    }

    @Test
    void testURLParamCache() {
        final AtomicReference<Integer> resCount = new AtomicReference<>(0);
        registry = new MockCacheableRegistryImpl(registryUrl);
        URL url = URLStrParser.parseEncodedStr(urlStr);

        NotifyListener listener = urls -> resCount.set(urls.size());

        registry.addChildren(url);
        registry.subscribe(serviceUrl, listener);
        assertEquals(1, registry.getStringParam().size());
        assertEquals(1, resCount.get());

        URL url1 = url.addParameter("k1", "v1");
        registry.addChildren(url1);
        registry.subscribe(serviceUrl, listener);
        assertEquals(2, registry.getStringParam().size());
        assertEquals(2, resCount.get());

        URL url2 = url1.setHost("192.168.1.1");
        registry.addChildren(url2);
        registry.subscribe(serviceUrl, listener);
        assertEquals(2, registry.getStringParam().size());
        assertEquals(3, resCount.get());
    }

    @Test
    void testRemove() {
        final AtomicReference<Integer> resCount = new AtomicReference<>(0);
        registry = new MockCacheableRegistryImpl(registryUrl);
        URL url = URLStrParser.parseEncodedStr(urlStr);

        NotifyListener listener = urls -> resCount.set(urls.size());

        registry.addChildren(url);
        registry.subscribe(serviceUrl, listener);
        assertEquals(1, registry.getStringUrls().get(serviceUrl).size());
        assertEquals(1, registry.getStringAddress().size());
        assertEquals(1, registry.getStringParam().size());
        assertEquals(1, resCount.get());

        registry.clearChildren();
        URL url1 = url.addParameter("k1", "v1");
        registry.addChildren(url1);
        registry.subscribe(serviceUrl, listener);
        assertEquals(1, registry.getStringUrls().get(serviceUrl).size());
        assertEquals(1, resCount.get());

        // After RemovalTask
        assertEquals(1, registry.getStringParam().size());
        // StringAddress will be deleted because the related stringUrls cache has been deleted.
        assertEquals(0, registry.getStringAddress().size());

        registry.clearChildren();
        URL url2 = url1.setHost("192.168.1.1");
        registry.addChildren(url2);
        registry.subscribe(serviceUrl, listener);
        assertEquals(1, registry.getStringUrls().get(serviceUrl).size());
        assertEquals(1, resCount.get());

        // After RemovalTask
        assertEquals(1, registry.getStringAddress().size());
        // StringParam will be deleted because the related stringUrls cache has been deleted.
        assertEquals(0, registry.getStringParam().size());
    }

    @Test
    void testEmptyProtection() {
        final AtomicReference<Integer> resCount = new AtomicReference<>(0);
        final AtomicReference<List<URL>> currentUrls = new AtomicReference<>();
        final List<URL> EMPTY_LIST = new ArrayList<>();

        registry = new MockCacheableRegistryImpl(registryUrl.addParameter(ENABLE_EMPTY_PROTECTION_KEY, true));
        URL url = URLStrParser.parseEncodedStr(urlStr);
        URL url2 = URLStrParser.parseEncodedStr(urlStr2);
        URL url3 = URLStrParser.parseEncodedStr(urlStr3);

        NotifyListener listener = urls -> {
            if (CollectionUtils.isEmpty(urls)) {
                // do nothing
            } else if (urls.size() == 1 && urls.get(0).getProtocol().equals(EMPTY_PROTOCOL)) {
                resCount.set(0);
                currentUrls.set(EMPTY_LIST);
            } else {
                resCount.set(urls.size());
                currentUrls.set(urls);
            }
        };

        registry.addChildren(url);
        registry.addChildren(url2);
        registry.addChildren(url3);

        registry.subscribe(serviceUrl, listener);
        assertEquals(3, resCount.get());
        registry.removeChildren(url);
        assertEquals(2, resCount.get());
        registry.clearChildren();
        assertEquals(2, resCount.get());

        URL emptyRegistryURL = registryUrl.addParameter(ENABLE_EMPTY_PROTECTION_KEY, false);
        MockCacheableRegistryImpl emptyRegistry = new MockCacheableRegistryImpl(emptyRegistryURL);

        emptyRegistry.addChildren(url);
        emptyRegistry.addChildren(url2);

        emptyRegistry.subscribe(serviceUrl, listener);
        assertEquals(2, resCount.get());
        emptyRegistry.clearChildren();
        assertEquals(0, currentUrls.get().size());
        assertEquals(EMPTY_LIST, currentUrls.get());

    }

    @Test
    void testNoEmptyProtection() {
        final AtomicReference<Integer> resCount = new AtomicReference<>(0);
        final AtomicReference<List<URL>> currentUrls = new AtomicReference<>();
        final List<URL> EMPTY_LIST = new ArrayList<>();

        registry = new MockCacheableRegistryImpl(registryUrl);
        URL url = URLStrParser.parseEncodedStr(urlStr);
        URL url2 = URLStrParser.parseEncodedStr(urlStr2);
        URL url3 = URLStrParser.parseEncodedStr(urlStr3);

        NotifyListener listener = urls -> {
            if (CollectionUtils.isEmpty(urls)) {
                // do nothing
            } else if (urls.size() == 1 && urls.get(0).getProtocol().equals(EMPTY_PROTOCOL)) {
                resCount.set(0);
                currentUrls.set(EMPTY_LIST);
            } else {
                resCount.set(urls.size());
                currentUrls.set(urls);
            }
        };

        registry.addChildren(url);
        registry.addChildren(url2);
        registry.addChildren(url3);

        registry.subscribe(serviceUrl, listener);
        assertEquals(3, resCount.get());
        registry.removeChildren(url);
        assertEquals(2, resCount.get());
        registry.clearChildren();
        assertEquals(0, resCount.get());

        URL emptyRegistryURL = registryUrl.addParameter(ENABLE_EMPTY_PROTECTION_KEY, true);
        MockCacheableRegistryImpl emptyRegistry = new MockCacheableRegistryImpl(emptyRegistryURL);

        emptyRegistry.addChildren(url);
        emptyRegistry.addChildren(url2);

        emptyRegistry.subscribe(serviceUrl, listener);
        assertEquals(2, resCount.get());
        emptyRegistry.clearChildren();
        assertEquals(2, currentUrls.get().size());

    }
}
