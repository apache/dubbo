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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.URL;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

class UrlUtilsTest {

    String localAddress = "127.0.0.1";





    @Test
    void testAddressNull() {
        String exceptionMessage = "Address is not allowed to be empty, please re-enter.";
        try {
            UrlUtils.parseURL(null, null);
        } catch (IllegalArgumentException illegalArgumentException) {
            assertEquals(exceptionMessage, illegalArgumentException.getMessage());
        }
    }

    @Test
    void testParseUrl() {
        String address = "remote://root:alibaba@127.0.0.1:9090/dubbo.test.api";
        URL url = UrlUtils.parseURL(address, null);
        assertEquals(localAddress + ":9090", url.getAddress());
        assertEquals("root", url.getUsername());
        assertEquals("alibaba", url.getPassword());
        assertEquals("dubbo.test.api", url.getPath());
        assertEquals(9090, url.getPort());
        assertEquals("remote", url.getProtocol());
    }

    @Test
    void testParseURLWithSpecial() {
        String address = "127.0.0.1:2181?backup=127.0.0.1:2182,127.0.0.1:2183";
        assertEquals("dubbo://" + address, UrlUtils.parseURL(address, null).toString());
    }

    @Test
    void testDefaultUrl() {
        String address = "127.0.0.1";
        URL url = UrlUtils.parseURL(address, null);
        assertEquals(localAddress + ":9090", url.getAddress());
        assertEquals(9090, url.getPort());
        assertEquals("dubbo", url.getProtocol());
        assertNull(url.getUsername());
        assertNull(url.getPassword());
        assertNull(url.getPath());
    }

    @Test
    void testParseFromParameter() {
        String address = "127.0.0.1";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("username", "root");
        parameters.put("password", "alibaba");
        parameters.put("port", "10000");
        parameters.put("protocol", "dubbo");
        parameters.put("path", "dubbo.test.api");
        parameters.put("aaa", "bbb");
        parameters.put("ccc", "ddd");
        URL url = UrlUtils.parseURL(address, parameters);
        assertEquals(localAddress + ":10000", url.getAddress());
        assertEquals("root", url.getUsername());
        assertEquals("alibaba", url.getPassword());
        assertEquals(10000, url.getPort());
        assertEquals("dubbo", url.getProtocol());
        assertEquals("dubbo.test.api", url.getPath());
        assertEquals("bbb", url.getParameter("aaa"));
        assertEquals("ddd", url.getParameter("ccc"));
    }

    @Test
    void testParseUrl2() {
        String address = "192.168.0.1";
        String backupAddress1 = "192.168.0.2";
        String backupAddress2 = "192.168.0.3";

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("username", "root");
        parameters.put("password", "alibaba");
        parameters.put("port", "10000");
        parameters.put("protocol", "dubbo");
        URL url = UrlUtils.parseURL(address + "," + backupAddress1 + "," + backupAddress2, parameters);
        assertEquals("192.168.0.1:10000", url.getAddress());
        assertEquals("root", url.getUsername());
        assertEquals("alibaba", url.getPassword());
        assertEquals(10000, url.getPort());
        assertEquals("dubbo", url.getProtocol());
        assertEquals("192.168.0.2" + "," + "192.168.0.3", url.getParameter("backup"));
    }

    @Test
    void testParseUrls() {
        String addresses = "192.168.0.1|192.168.0.2|192.168.0.3";
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("username", "root");
        parameters.put("password", "alibaba");
        parameters.put("port", "10000");
        parameters.put("protocol", "dubbo");
        List<URL> urls = UrlUtils.parseURLs(addresses, parameters);
        assertEquals("192.168.0.1" + ":10000", urls.get(0).getAddress());
        assertEquals("192.168.0.2" + ":10000", urls.get(1).getAddress());
    }

    @Test
    void testParseUrlsAddressNull() {
        String exceptionMessage = "Address is not allowed to be empty, please re-enter.";
        try {
            UrlUtils.parseURLs(null, null);
        } catch (IllegalArgumentException illegalArgumentException) {
            assertEquals(exceptionMessage, illegalArgumentException.getMessage());
        }
    }

    @Test
    void testConvertRegister() {
        String key = "perf/dubbo.test.api.HelloService:1.0.0";
        Map<String, Map<String, String>> register = new HashMap<String, Map<String, String>>();
        register.put(key, null);
        Map<String, Map<String, String>> newRegister = UrlUtils.convertRegister(register);
        assertEquals(register, newRegister);
    }

    @Test
    void testConvertRegister2() {
        String key = "dubbo.test.api.HelloService";
        Map<String, Map<String, String>> register = new HashMap<String, Map<String, String>>();
        Map<String, String> service = new HashMap<String, String>();
        service.put("dubbo://127.0.0.1:20880/com.xxx.XxxService", "version=1.0.0&group=test&dubbo.version=2.0.0");
        register.put(key, service);
        Map<String, Map<String, String>> newRegister = UrlUtils.convertRegister(register);
        Map<String, String> newService = new HashMap<String, String>();
        newService.put("dubbo://127.0.0.1:20880/com.xxx.XxxService", "dubbo.version=2.0.0&group=test&version=1.0.0");
        assertEquals(newService, newRegister.get("test/dubbo.test.api.HelloService:1.0.0"));
    }

    @Test
    void testSubscribe() {
        String key = "perf/dubbo.test.api.HelloService:1.0.0";
        Map<String, String> subscribe = new HashMap<String, String>();
        subscribe.put(key, null);
        Map<String, String> newSubscribe = UrlUtils.convertSubscribe(subscribe);
        assertEquals(subscribe, newSubscribe);
    }

    @Test
    void testSubscribe2() {
        String key = "dubbo.test.api.HelloService";
        Map<String, String> subscribe = new HashMap<String, String>();
        subscribe.put(key, "version=1.0.0&group=test&dubbo.version=2.0.0");
        Map<String, String> newSubscribe = UrlUtils.convertSubscribe(subscribe);
        assertEquals("dubbo.version=2.0.0&group=test&version=1.0.0", newSubscribe.get("test/dubbo.test.api.HelloService:1.0.0"));
    }

    @Test
    void testRevertRegister() {
        String key = "perf/dubbo.test.api.HelloService:1.0.0";
        Map<String, Map<String, String>> register = new HashMap<String, Map<String, String>>();
        Map<String, String> service = new HashMap<String, String>();
        service.put("dubbo://127.0.0.1:20880/com.xxx.XxxService", null);
        register.put(key, service);
        Map<String, Map<String, String>> newRegister = UrlUtils.revertRegister(register);
        Map<String, Map<String, String>> expectedRegister = new HashMap<String, Map<String, String>>();
        service.put("dubbo://127.0.0.1:20880/com.xxx.XxxService", "group=perf&version=1.0.0");
        expectedRegister.put("dubbo.test.api.HelloService", service);
        assertEquals(expectedRegister, newRegister);
    }

    @Test
    void testRevertRegister2() {
        String key = "dubbo.test.api.HelloService";
        Map<String, Map<String, String>> register = new HashMap<String, Map<String, String>>();
        Map<String, String> service = new HashMap<String, String>();
        service.put("dubbo://127.0.0.1:20880/com.xxx.XxxService", null);
        register.put(key, service);
        Map<String, Map<String, String>> newRegister = UrlUtils.revertRegister(register);
        Map<String, Map<String, String>> expectedRegister = new HashMap<String, Map<String, String>>();
        service.put("dubbo://127.0.0.1:20880/com.xxx.XxxService", null);
        expectedRegister.put("dubbo.test.api.HelloService", service);
        assertEquals(expectedRegister, newRegister);
    }

    @Test
    void testRevertSubscribe() {
        String key = "perf/dubbo.test.api.HelloService:1.0.0";
        Map<String, String> subscribe = new HashMap<String, String>();
        subscribe.put(key, null);
        Map<String, String> newSubscribe = UrlUtils.revertSubscribe(subscribe);
        Map<String, String> expectSubscribe = new HashMap<String, String>();
        expectSubscribe.put("dubbo.test.api.HelloService", "group=perf&version=1.0.0");
        assertEquals(expectSubscribe, newSubscribe);
    }

    @Test
    void testRevertSubscribe2() {
        String key = "dubbo.test.api.HelloService";
        Map<String, String> subscribe = new HashMap<String, String>();
        subscribe.put(key, null);
        Map<String, String> newSubscribe = UrlUtils.revertSubscribe(subscribe);
        assertEquals(subscribe, newSubscribe);
    }

    @Test
    void testRevertNotify() {
        String key = "dubbo.test.api.HelloService";
        Map<String, Map<String, String>> notify = new HashMap<String, Map<String, String>>();
        Map<String, String> service = new HashMap<String, String>();
        service.put("dubbo://127.0.0.1:20880/com.xxx.XxxService", "group=perf&version=1.0.0");
        notify.put(key, service);
        Map<String, Map<String, String>> newRegister = UrlUtils.revertNotify(notify);
        Map<String, Map<String, String>> expectedRegister = new HashMap<String, Map<String, String>>();
        service.put("dubbo://127.0.0.1:20880/com.xxx.XxxService", "group=perf&version=1.0.0");
        expectedRegister.put("perf/dubbo.test.api.HelloService:1.0.0", service);
        assertEquals(expectedRegister, newRegister);
    }

    @Test
    void testRevertNotify2() {
        String key = "perf/dubbo.test.api.HelloService:1.0.0";
        Map<String, Map<String, String>> notify = new HashMap<String, Map<String, String>>();
        Map<String, String> service = new HashMap<String, String>();
        service.put("dubbo://127.0.0.1:20880/com.xxx.XxxService", "group=perf&version=1.0.0");
        notify.put(key, service);
        Map<String, Map<String, String>> newRegister = UrlUtils.revertNotify(notify);
        Map<String, Map<String, String>> expectedRegister = new HashMap<String, Map<String, String>>();
        service.put("dubbo://127.0.0.1:20880/com.xxx.XxxService", "group=perf&version=1.0.0");
        expectedRegister.put("perf/dubbo.test.api.HelloService:1.0.0", service);
        assertEquals(expectedRegister, newRegister);
    }

    // backward compatibility for version 2.0.0
    @Test
    void testRevertForbid() {
        String service = "dubbo.test.api.HelloService";
        List<String> forbid = new ArrayList<String>();
        forbid.add(service);
        Set<URL> subscribed = new HashSet<URL>();
        subscribed.add(URL.valueOf("dubbo://127.0.0.1:20880/" + service + "?group=perf&version=1.0.0"));
        List<String> newForbid = UrlUtils.revertForbid(forbid, subscribed);
        List<String> expectForbid = new ArrayList<String>();
        expectForbid.add("perf/" + service + ":1.0.0");
        assertEquals(expectForbid, newForbid);
    }

    @Test
    void testRevertForbid2() {
        List<String> newForbid = UrlUtils.revertForbid(null, null);
        assertNull(newForbid);
    }

    @Test
    void testRevertForbid3() {
        String service1 = "dubbo.test.api.HelloService:1.0.0";
        String service2 = "dubbo.test.api.HelloService:2.0.0";
        List<String> forbid = new ArrayList<String>();
        forbid.add(service1);
        forbid.add(service2);
        List<String> newForbid = UrlUtils.revertForbid(forbid, null);
        assertEquals(forbid, newForbid);
    }

    @Test
    void testIsMatch() {
        URL consumerUrl = URL.valueOf("dubbo://127.0.0.1:20880/com.xxx.XxxService?version=1.0.0&group=test");
        URL providerUrl = URL.valueOf("http://127.0.0.1:8080/com.xxx.XxxService?version=1.0.0&group=test");
        assertTrue(UrlUtils.isMatch(consumerUrl, providerUrl));
    }

    @Test
    void testIsMatch2() {
        URL consumerUrl = URL.valueOf("dubbo://127.0.0.1:20880/com.xxx.XxxService?version=2.0.0&group=test");
        URL providerUrl = URL.valueOf("http://127.0.0.1:8080/com.xxx.XxxService?version=1.0.0&group=test");
        assertFalse(UrlUtils.isMatch(consumerUrl, providerUrl));
    }

    @Test
    void testIsMatch3() {
        URL consumerUrl = URL.valueOf("dubbo://127.0.0.1:20880/com.xxx.XxxService?version=1.0.0&group=aa");
        URL providerUrl = URL.valueOf("http://127.0.0.1:8080/com.xxx.XxxService?version=1.0.0&group=test");
        assertFalse(UrlUtils.isMatch(consumerUrl, providerUrl));
    }

    @Test
    void testIsMatch4() {
        URL consumerUrl = URL.valueOf("dubbo://127.0.0.1:20880/com.xxx.XxxService?version=1.0.0&group=*");
        URL providerUrl = URL.valueOf("http://127.0.0.1:8080/com.xxx.XxxService?version=1.0.0&group=test");
        assertTrue(UrlUtils.isMatch(consumerUrl, providerUrl));
    }

    @Test
    void testIsMatch5() {
        URL consumerUrl = URL.valueOf("dubbo://127.0.0.1:20880/com.xxx.XxxService?version=*&group=test");
        URL providerUrl = URL.valueOf("http://127.0.0.1:8080/com.xxx.XxxService?version=1.0.0&group=test");
        assertTrue(UrlUtils.isMatch(consumerUrl, providerUrl));
    }

    @Test
    void testIsItemMatch() throws Exception {
        assertTrue(UrlUtils.isItemMatch(null, null));
        assertTrue(!UrlUtils.isItemMatch("1", null));
        assertTrue(!UrlUtils.isItemMatch(null, "1"));
        assertTrue(UrlUtils.isItemMatch("1", "1"));
        assertTrue(UrlUtils.isItemMatch("*", null));
        assertTrue(UrlUtils.isItemMatch("*", "*"));
        assertTrue(UrlUtils.isItemMatch("*", "1234"));
        assertTrue(!UrlUtils.isItemMatch(null, "*"));
    }

    @Test
    void testIsServiceKeyMatch() throws Exception {
        URL url = URL.valueOf("test://127.0.0.1");
        URL pattern = url.addParameter(GROUP_KEY, "test")
                .addParameter(INTERFACE_KEY, "test")
                .addParameter(VERSION_KEY, "test");
        URL value = pattern;
        assertTrue(UrlUtils.isServiceKeyMatch(pattern, value));

        pattern = pattern.addParameter(GROUP_KEY, "*");
        assertTrue(UrlUtils.isServiceKeyMatch(pattern, value));

        pattern = pattern.addParameter(VERSION_KEY, "*");
        assertTrue(UrlUtils.isServiceKeyMatch(pattern, value));
    }

    @Test
    void testGetEmptyUrl() throws Exception {
        URL url = UrlUtils.getEmptyUrl("dubbo/a.b.c.Foo:1.0.0", "test");
        assertThat(url.toFullString(), equalTo("empty://0.0.0.0/a.b.c.Foo?category=test&group=dubbo&version=1.0.0"));
    }

    @Test
    void testIsMatchGlobPattern() throws Exception {
        assertTrue(UrlUtils.isMatchGlobPattern("*", "value"));
        assertTrue(UrlUtils.isMatchGlobPattern("", null));
        assertFalse(UrlUtils.isMatchGlobPattern("", "value"));
        assertTrue(UrlUtils.isMatchGlobPattern("value", "value"));
        assertTrue(UrlUtils.isMatchGlobPattern("v*", "value"));
        assertTrue(UrlUtils.isMatchGlobPattern("*e", "value"));
        assertTrue(UrlUtils.isMatchGlobPattern("v*e", "value"));
        assertTrue(UrlUtils.isMatchGlobPattern("$key", "value", URL.valueOf("dubbo://localhost:8080/Foo?key=v*e")));
    }

    @Test
    void testIsMatchUrlWithDefaultPrefix() {
        URL url = URL.valueOf("dubbo://127.0.0.1:20880/com.xxx.XxxService?default.version=1.0.0&default.group=test");
        assertEquals("1.0.0", url.getVersion());
        assertEquals("1.0.0", url.getParameter("default.version"));

        URL consumerUrl = URL.valueOf("consumer://127.0.0.1/com.xxx.XxxService?version=1.0.0&group=test");
        assertTrue(UrlUtils.isMatch(consumerUrl, url));

        URL consumerUrl1 = URL.valueOf("consumer://127.0.0.1/com.xxx.XxxService?default.version=1.0.0&default.group=test");
        assertTrue(UrlUtils.isMatch(consumerUrl1, url));
    }

    @Test
    public void testIsConsumer() {
        String address1 = "remote://root:alibaba@127.0.0.1:9090";
        URL url1 = UrlUtils.parseURL(address1, null);
        String address2 = "consumer://root:alibaba@127.0.0.1:9090";
        URL url2 = UrlUtils.parseURL(address2, null);
        String address3 = "consumer://root:alibaba@127.0.0.1";
        URL url3 = UrlUtils.parseURL(address3, null);

        assertFalse(UrlUtils.isConsumer(url1));
        assertTrue(UrlUtils.isConsumer(url2));
        assertTrue(UrlUtils.isConsumer(url3));

    }

    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor<UrlUtils> constructor = UrlUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, () -> {
            constructor.newInstance();
        });
    }


    @Test
    public void testClassifyUrls() {

        String address1 = "remote://root:alibaba@127.0.0.1:9090";
        URL url1 = UrlUtils.parseURL(address1, null);
        String address2 = "consumer://root:alibaba@127.0.0.1:9090";
        URL url2 = UrlUtils.parseURL(address2, null);
        String address3 = "remote://root:alibaba@127.0.0.1";
        URL url3 = UrlUtils.parseURL(address3, null);
        String address4 = "consumer://root:alibaba@127.0.0.1";
        URL url4 = UrlUtils.parseURL(address4, null);

        List<URL> urls = new ArrayList<>();
        urls.add(url1);
        urls.add(url2);
        urls.add(url3);
        urls.add(url4);

        List<URL> consumerUrls = UrlUtils.classifyUrls(urls, UrlUtils::isConsumer);
        assertEquals(2, consumerUrls.size());
        assertTrue(consumerUrls.contains(url2));
        assertTrue(consumerUrls.contains(url4));

        List<URL> nonConsumerUrls = UrlUtils.classifyUrls(urls, url -> !UrlUtils.isConsumer(url));
        assertEquals(2, nonConsumerUrls.size());
        assertTrue(nonConsumerUrls.contains(url1));
        assertTrue(nonConsumerUrls.contains(url3));
    }

    @Test
    public void testHasServiceDiscoveryRegistryProtocol() {
        String address1 = "http://root:alibaba@127.0.0.1:9090/dubbo.test.api";
        URL url1 = UrlUtils.parseURL(address1, null);
        String address2 = "service-discovery-registry://root:alibaba@127.0.0.1:9090/dubbo.test.api";
        URL url2 = UrlUtils.parseURL(address2, null);

        assertFalse(UrlUtils.hasServiceDiscoveryRegistryProtocol(url1));
        assertTrue(UrlUtils.hasServiceDiscoveryRegistryProtocol(url2));
    }


    private static final String SERVICE_REGISTRY_TYPE = "service";
    private static final String REGISTRY_TYPE_KEY = "registry-type";

    @Test
    public void testHasServiceDiscoveryRegistryTypeKey() {
        Map<String, String> parameters1 = new HashMap<>();
        parameters1.put(REGISTRY_TYPE_KEY, "value2");
        assertFalse(UrlUtils.hasServiceDiscoveryRegistryTypeKey(parameters1));

        Map<String, String> parameters2 = new HashMap<>();
        parameters2.put(REGISTRY_TYPE_KEY, SERVICE_REGISTRY_TYPE);

        assertTrue(UrlUtils.hasServiceDiscoveryRegistryTypeKey(parameters2));
    }

    @Test
    public void testIsConfigurator() {
        String address1 = "http://example.com";
        URL url1 = UrlUtils.parseURL(address1, null);
        String address2 = "override://example.com";
        URL url2 = UrlUtils.parseURL(address2, null);
        String address3 = "http://example.com?category=configurators";
        URL url3 = UrlUtils.parseURL(address3, null);

        assertFalse(UrlUtils.isConfigurator(url1));
        assertTrue(UrlUtils.isConfigurator(url2));
        assertTrue(UrlUtils.isConfigurator(url3));
    }

    @Test
    public void testIsRoute() {
        String address1 = "http://example.com";
        URL url1 = UrlUtils.parseURL(address1, null);
        String address2 = "route://example.com";
        URL url2 = UrlUtils.parseURL(address2, null);
        String address3 = "http://example.com?category=routers";
        URL url3 = UrlUtils.parseURL(address3, null);

        assertFalse(UrlUtils.isRoute(url1));
        assertTrue(UrlUtils.isRoute(url2));
        assertTrue(UrlUtils.isRoute(url3));
    }

    @Test
    public void testIsProvider() {
        String address1 = "http://example.com";
        URL url1 = UrlUtils.parseURL(address1, null);
        String address2 = "override://example.com";
        URL url2 = UrlUtils.parseURL(address2, null);
        String address3 = "route://example.com";
        URL url3 = UrlUtils.parseURL(address3, null);
        String address4 = "http://example.com?category=providers";
        URL url4 = UrlUtils.parseURL(address4, null);
        String address5 = "http://example.com?category=something-else";
        URL url5 = UrlUtils.parseURL(address5, null);


        assertTrue(UrlUtils.isProvider(url1));
        assertFalse(UrlUtils.isProvider(url2));
        assertFalse(UrlUtils.isProvider(url3));
        assertTrue(UrlUtils.isProvider(url4));
        assertFalse(UrlUtils.isProvider(url5));
    }


    @Test
    public void testIsRegistry() {
        String address1 = "http://example.com";
        URL url1 = UrlUtils.parseURL(address1, null);
        String address2 = "registry://example.com";
        URL url2 = UrlUtils.parseURL(address2, null);
        String address3 = "sr://example.com";
        URL url3 = UrlUtils.parseURL(address3, null);
        String address4 = "custom-registry-protocol://example.com";
        URL url4 = UrlUtils.parseURL(address4, null);

        assertFalse(UrlUtils.isRegistry(url1));
        assertTrue(UrlUtils.isRegistry(url2));
        assertFalse(UrlUtils.isRegistry(url3));
        assertTrue(UrlUtils.isRegistry(url4));
    }



    @Test
    public void testIsServiceDiscoveryURL() {
        String address1 = "http://example.com";
        URL url1 = UrlUtils.parseURL(address1, null);
        String address2 = "service-discovery-registry://example.com";
        URL url2 = UrlUtils.parseURL(address2, null);
        String address3 = "SERVICE-DISCOVERY-REGISTRY://example.com";
        URL url3 = UrlUtils.parseURL(address3, null);
        String address4 = "http://example.com?registry-type=service";
        URL url4 = UrlUtils.parseURL(address4, null);
        url4.addParameter(REGISTRY_TYPE_KEY, SERVICE_REGISTRY_TYPE);

        assertFalse(UrlUtils.isServiceDiscoveryURL(url1));
        assertTrue(UrlUtils.isServiceDiscoveryURL(url2));
        assertTrue(UrlUtils.isServiceDiscoveryURL(url3));
        assertTrue(UrlUtils.isServiceDiscoveryURL(url4));
    }
}
