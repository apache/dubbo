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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UrlUtilsTest {

    String localAddress = "127.0.0.1";

    @Test
    public void testAddressNull() {
        assertNull(UrlUtils.parseURL(null, null));
    }

    @Test
    public void testParseUrl() {
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
    public void testParseURLWithSpecial() {
        String address = "127.0.0.1:2181?backup=127.0.0.1:2182,127.0.0.1:2183";
        assertEquals("dubbo://" + address,UrlUtils.parseURL(address, null).toString());
    }

    @Test
    public void testDefaultUrl() {
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
    public void testParseFromParameter() {
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
    public void testParseUrl2() {
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
    public void testParseUrls() {
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
    public void testParseUrlsAddressNull() {
        assertNull(UrlUtils.parseURLs(null, null));
    }

    @Test
    public void testConvertRegister() {
        String key = "perf/dubbo.test.api.HelloService:1.0.0";
        Map<String, Map<String, String>> register = new HashMap<String, Map<String, String>>();
        register.put(key, null);
        Map<String, Map<String, String>> newRegister = UrlUtils.convertRegister(register);
        assertEquals(register, newRegister);
    }

    @Test
    public void testConvertRegister2() {
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
    public void testSubscribe() {
        String key = "perf/dubbo.test.api.HelloService:1.0.0";
        Map<String, String> subscribe = new HashMap<String, String>();
        subscribe.put(key, null);
        Map<String, String> newSubscribe = UrlUtils.convertSubscribe(subscribe);
        assertEquals(subscribe, newSubscribe);
    }

    @Test
    public void testSubscribe2() {
        String key = "dubbo.test.api.HelloService";
        Map<String, String> subscribe = new HashMap<String, String>();
        subscribe.put(key, "version=1.0.0&group=test&dubbo.version=2.0.0");
        Map<String, String> newSubscribe = UrlUtils.convertSubscribe(subscribe);
        assertEquals("dubbo.version=2.0.0&group=test&version=1.0.0", newSubscribe.get("test/dubbo.test.api.HelloService:1.0.0"));
    }

    @Test
    public void testRevertRegister() {
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
    public void testRevertRegister2() {
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
    public void testRevertSubscribe() {
        String key = "perf/dubbo.test.api.HelloService:1.0.0";
        Map<String, String> subscribe = new HashMap<String, String>();
        subscribe.put(key, null);
        Map<String, String> newSubscribe = UrlUtils.revertSubscribe(subscribe);
        Map<String, String> expectSubscribe = new HashMap<String, String>();
        expectSubscribe.put("dubbo.test.api.HelloService", "group=perf&version=1.0.0");
        assertEquals(expectSubscribe, newSubscribe);
    }

    @Test
    public void testRevertSubscribe2() {
        String key = "dubbo.test.api.HelloService";
        Map<String, String> subscribe = new HashMap<String, String>();
        subscribe.put(key, null);
        Map<String, String> newSubscribe = UrlUtils.revertSubscribe(subscribe);
        assertEquals(subscribe, newSubscribe);
    }

    @Test
    public void testRevertNotify() {
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
    public void testRevertNotify2() {
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
    public void testRevertForbid() {
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
    public void testRevertForbid2() {
        List<String> newForbid = UrlUtils.revertForbid(null, null);
        assertNull(newForbid);
    }

    @Test
    public void testRevertForbid3() {
        String service1 = "dubbo.test.api.HelloService:1.0.0";
        String service2 = "dubbo.test.api.HelloService:2.0.0";
        List<String> forbid = new ArrayList<String>();
        forbid.add(service1);
        forbid.add(service2);
        List<String> newForbid = UrlUtils.revertForbid(forbid, null);
        assertEquals(forbid, newForbid);
    }

    @Test
    public void testIsMatch() {
        URL consumerUrl = URL.valueOf("dubbo://127.0.0.1:20880/com.xxx.XxxService?version=1.0.0&group=test");
        URL providerUrl = URL.valueOf("http://127.0.0.1:8080/com.xxx.XxxService?version=1.0.0&group=test");
        assertTrue(UrlUtils.isMatch(consumerUrl, providerUrl));
    }

    @Test
    public void testIsMatch2() {
        URL consumerUrl = URL.valueOf("dubbo://127.0.0.1:20880/com.xxx.XxxService?version=2.0.0&group=test");
        URL providerUrl = URL.valueOf("http://127.0.0.1:8080/com.xxx.XxxService?version=1.0.0&group=test");
        assertFalse(UrlUtils.isMatch(consumerUrl, providerUrl));
    }

    @Test
    public void testIsMatch3() {
        URL consumerUrl = URL.valueOf("dubbo://127.0.0.1:20880/com.xxx.XxxService?version=1.0.0&group=aa");
        URL providerUrl = URL.valueOf("http://127.0.0.1:8080/com.xxx.XxxService?version=1.0.0&group=test");
        assertFalse(UrlUtils.isMatch(consumerUrl, providerUrl));
    }

    @Test
    public void testIsMatch4() {
        URL consumerUrl = URL.valueOf("dubbo://127.0.0.1:20880/com.xxx.XxxService?version=1.0.0&group=*");
        URL providerUrl = URL.valueOf("http://127.0.0.1:8080/com.xxx.XxxService?version=1.0.0&group=test");
        assertTrue(UrlUtils.isMatch(consumerUrl, providerUrl));
    }

    @Test
    public void testIsMatch5() {
        URL consumerUrl = URL.valueOf("dubbo://127.0.0.1:20880/com.xxx.XxxService?version=*&group=test");
        URL providerUrl = URL.valueOf("http://127.0.0.1:8080/com.xxx.XxxService?version=1.0.0&group=test");
        assertTrue(UrlUtils.isMatch(consumerUrl, providerUrl));
    }

    @Test
    public void testIsItemMatch() throws Exception {
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
    public void testIsServiceKeyMatch() throws Exception {
        URL url = URL.valueOf("test://127.0.0.1");
        URL pattern = url.addParameter(Constants.GROUP_KEY, "test")
                .addParameter(Constants.INTERFACE_KEY, "test")
                .addParameter(Constants.VERSION_KEY, "test");
        URL value = pattern;
        assertTrue(UrlUtils.isServiceKeyMatch(pattern, value));

        pattern = pattern.addParameter(Constants.GROUP_KEY, "*");
        assertTrue(UrlUtils.isServiceKeyMatch(pattern, value));

        pattern = pattern.addParameter(Constants.VERSION_KEY, "*");
        assertTrue(UrlUtils.isServiceKeyMatch(pattern, value));
    }

    @Test
    public void testGetEmptyUrl() throws Exception {
        URL url = UrlUtils.getEmptyUrl("dubbo/a.b.c.Foo:1.0.0", "test");
        assertThat(url.toFullString(), equalTo("empty://0.0.0.0/a.b.c.Foo?category=test&group=dubbo&version=1.0.0"));
    }

    @Test
    public void testIsMatchGlobPattern() throws Exception {
        assertTrue(UrlUtils.isMatchGlobPattern("*", "value"));
        assertTrue(UrlUtils.isMatchGlobPattern("", null));
        assertFalse(UrlUtils.isMatchGlobPattern("", "value"));
        assertTrue(UrlUtils.isMatchGlobPattern("value", "value"));
        assertTrue(UrlUtils.isMatchGlobPattern("v*", "value"));
        assertTrue(UrlUtils.isMatchGlobPattern("*e", "value"));
        assertTrue(UrlUtils.isMatchGlobPattern("v*e", "value"));
        assertTrue(UrlUtils.isMatchGlobPattern("$key", "value", URL.valueOf("dubbo://localhost:8080/Foo?key=v*e")));
    }
}