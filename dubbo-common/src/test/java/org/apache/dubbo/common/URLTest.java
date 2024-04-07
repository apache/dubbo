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
package org.apache.dubbo.common;

import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.CollectionUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.OS_NAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.OS_WIN_PREFIX;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class URLTest {

    @Test
    void test_ignore_pond() {
        URL url = URL.valueOf(
                "dubbo://admin:hello1234@10.20.130.230:20880/context/path#index?version=1.0.0&id=org.apache.dubbo.config.RegistryConfig#0");
        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());
        assertEquals("org.apache.dubbo.config.RegistryConfig#0", url.getParameter("id"));
    }

    @Test
    void testDefault() {
        URL url1 = URL.valueOf("dubbo://127.0.0.1:12345?timeout=1234&default.timeout=5678");
        assertEquals(1234, url1.getParameter("timeout", 0));
        assertEquals(5678, url1.getParameter("default.timeout", 0));

        URL url2 = URL.valueOf("dubbo://127.0.0.1:12345?default.timeout=5678");
        assertEquals(5678, url2.getParameter("timeout", 0));
        assertEquals(5678, url2.getParameter("default.timeout", 0));
    }

    @Test
    void test_valueOf_noProtocolAndHost() throws Exception {
        URL url = URL.valueOf("/context/path?version=1.0.0&application=morgan");
        assertURLStrDecoder(url);
        assertNull(url.getProtocol());
        assertNull(url.getUsername());
        assertNull(url.getPassword());
        assertNull(url.getHost());
        assertNull(url.getAddress());
        assertEquals(0, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());
        assertEquals("morgan", url.getParameter("application"));

        url = URL.valueOf("context/path?version=1.0.0&application=morgan");
        //                 ^^^^^^^ Caution , parse as host
        assertURLStrDecoder(url);
        assertNull(url.getProtocol());
        assertNull(url.getUsername());
        assertNull(url.getPassword());
        assertEquals("context", url.getHost());
        assertEquals(0, url.getPort());
        assertEquals("path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());
        assertEquals("morgan", url.getParameter("application"));
    }

    private void assertURLStrDecoder(URL url) {
        String fullURLStr = url.toFullString();
        URL newUrl = URLStrParser.parseEncodedStr(URL.encode(fullURLStr));
        assertEquals(URL.valueOf(fullURLStr), newUrl);

        URL newUrl2 = URLStrParser.parseDecodedStr(fullURLStr);
        assertEquals(URL.valueOf(fullURLStr), newUrl2);
    }

    @Test
    void test_valueOf_noProtocol() throws Exception {
        URL url = URL.valueOf("10.20.130.230");
        assertURLStrDecoder(url);
        assertNull(url.getProtocol());
        assertNull(url.getUsername());
        assertNull(url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230", url.getAddress());
        assertEquals(0, url.getPort());
        assertNull(url.getPath());
        assertEquals(0, url.getParameters().size());

        url = URL.valueOf("10.20.130.230:20880");
        assertURLStrDecoder(url);
        assertNull(url.getProtocol());
        assertNull(url.getUsername());
        assertNull(url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertNull(url.getPath());
        assertEquals(0, url.getParameters().size());

        url = URL.valueOf("10.20.130.230/context/path");
        assertURLStrDecoder(url);
        assertNull(url.getProtocol());
        assertNull(url.getUsername());
        assertNull(url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230", url.getAddress());
        assertEquals(0, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(0, url.getParameters().size());

        url = URL.valueOf("10.20.130.230:20880/context/path");
        assertURLStrDecoder(url);
        assertNull(url.getProtocol());
        assertNull(url.getUsername());
        assertNull(url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(0, url.getParameters().size());

        url = URL.valueOf("admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        assertURLStrDecoder(url);
        assertNull(url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());
        assertEquals("morgan", url.getParameter("application"));
    }

    @Test
    void test_valueOf_noHost() throws Exception {
        URL url = URL.valueOf("file:///home/user1/router.js");
        assertURLStrDecoder(url);
        assertEquals("file", url.getProtocol());
        assertNull(url.getUsername());
        assertNull(url.getPassword());
        assertNull(url.getHost());
        assertNull(url.getAddress());
        assertEquals(0, url.getPort());
        assertEquals("home/user1/router.js", url.getPath());
        assertEquals(0, url.getParameters().size());

        // Caution!!
        url = URL.valueOf("file://home/user1/router.js");
        //                      ^^ only tow slash!
        assertURLStrDecoder(url);
        assertEquals("file", url.getProtocol());
        assertNull(url.getUsername());
        assertNull(url.getPassword());
        assertEquals("home", url.getHost());
        assertEquals(0, url.getPort());
        assertEquals("user1/router.js", url.getPath());
        assertEquals(0, url.getParameters().size());

        url = URL.valueOf("file:/home/user1/router.js");
        assertURLStrDecoder(url);
        assertEquals("file", url.getProtocol());
        assertNull(url.getUsername());
        assertNull(url.getPassword());
        assertNull(url.getHost());
        assertNull(url.getAddress());
        assertEquals(0, url.getPort());
        assertEquals("home/user1/router.js", url.getPath());
        assertEquals(0, url.getParameters().size());

        url = URL.valueOf("file:///d:/home/user1/router.js");
        assertURLStrDecoder(url);
        assertEquals("file", url.getProtocol());
        assertNull(url.getUsername());
        assertNull(url.getPassword());
        assertNull(url.getHost());
        assertNull(url.getAddress());
        assertEquals(0, url.getPort());
        assertEquals("d:/home/user1/router.js", url.getPath());
        assertEquals(0, url.getParameters().size());

        url = URL.valueOf("file:///home/user1/router.js?p1=v1&p2=v2");
        assertURLStrDecoder(url);
        assertEquals("file", url.getProtocol());
        assertNull(url.getUsername());
        assertNull(url.getPassword());
        assertNull(url.getHost());
        assertNull(url.getAddress());
        assertEquals(0, url.getPort());
        assertEquals("home/user1/router.js", url.getPath());
        assertEquals(2, url.getParameters().size());
        Map<String, String> params = new HashMap<String, String>();
        params.put("p1", "v1");
        params.put("p2", "v2");
        assertEquals(params, url.getParameters());

        url = URL.valueOf("file:/home/user1/router.js?p1=v1&p2=v2");
        assertURLStrDecoder(url);
        assertEquals("file", url.getProtocol());
        assertNull(url.getUsername());
        assertNull(url.getPassword());
        assertNull(url.getHost());
        assertNull(url.getAddress());
        assertEquals(0, url.getPort());
        assertEquals("home/user1/router.js", url.getPath());
        assertEquals(2, url.getParameters().size());
        params = new HashMap<String, String>();
        params.put("p1", "v1");
        params.put("p2", "v2");
        assertEquals(params, url.getParameters());
    }

    @Test
    void test_valueOf_WithProtocolHost() throws Exception {
        URL url = URL.valueOf("dubbo://10.20.130.230");
        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertNull(url.getUsername());
        assertNull(url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230", url.getAddress());
        assertEquals(0, url.getPort());
        assertNull(url.getPath());
        assertEquals(0, url.getParameters().size());

        url = URL.valueOf("dubbo://10.20.130.230:20880/context/path");
        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertNull(url.getUsername());
        assertNull(url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(0, url.getParameters().size());

        url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880");
        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertNull(url.getPath());
        assertEquals(0, url.getParameters().size());

        url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880?version=1.0.0");
        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertNull(url.getPath());
        assertEquals(1, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());

        url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());
        assertEquals("morgan", url.getParameter("application"));

        url = URL.valueOf(
                "dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan&noValue=");
        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(3, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());
        assertEquals("morgan", url.getParameter("application"));
        assertEquals("", url.getParameter("noValue"));
    }

    // TODO Do not want to use spaces? See: DUBBO-502, URL class handles special conventions for special characters.
    @Test
    void test_valueOf_spaceSafe() throws Exception {
        URL url = URL.valueOf("http://1.2.3.4:8080/path?key=value1 value2");
        assertURLStrDecoder(url);
        assertEquals("http://1.2.3.4:8080/path?key=value1 value2", url.toString());
        assertEquals("value1 value2", url.getParameter("key"));
    }

    @Test
    void test_noValueKey() throws Exception {
        URL url = URL.valueOf("http://1.2.3.4:8080/path?k0=&k1=v1");

        assertURLStrDecoder(url);
        assertFalse(url.hasParameter("k0"));

        // If a Key has no corresponding Value, then empty string used as the Value.
        assertEquals("", url.getParameter("k0"));
    }

    @Test
    void test_valueOf_Exception_noProtocol() throws Exception {
        try {
            URL.valueOf("://1.2.3.4:8080/path");
            fail();
        } catch (IllegalStateException expected) {
            assertEquals("url missing protocol: \"://1.2.3.4:8080/path\"", expected.getMessage());
        }

        try {
            String encodedURLStr = URL.encode("://1.2.3.4:8080/path");
            URLStrParser.parseEncodedStr(encodedURLStr);
            fail();
        } catch (IllegalStateException expected) {
            assertEquals("url missing protocol: \"://1.2.3.4:8080/path\"", URL.decode(expected.getMessage()));
        }

        try {
            URLStrParser.parseDecodedStr("://1.2.3.4:8080/path");
            fail();
        } catch (IllegalStateException expected) {
            assertEquals("url missing protocol: \"://1.2.3.4:8080/path\"", expected.getMessage());
        }
    }

    @Test
    void test_getAddress() throws Exception {
        URL url1 = URL.valueOf(
                "dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        assertURLStrDecoder(url1);
        assertEquals("10.20.130.230:20880", url1.getAddress());
    }

    @Test
    void test_getAbsolutePath() throws Exception {
        URL url = new ServiceConfigURL("p1", "1.2.2.2", 33);
        assertURLStrDecoder(url);
        assertNull(url.getAbsolutePath());

        url = new ServiceConfigURL("file", null, 90, "/home/user1/route.js");
        assertURLStrDecoder(url);
        assertEquals("/home/user1/route.js", url.getAbsolutePath());
    }

    @Test
    void test_equals() throws Exception {
        URL url1 = URL.valueOf(
                "dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        assertURLStrDecoder(url1);

        Map<String, String> params = new HashMap<String, String>();
        params.put("version", "1.0.0");
        params.put("application", "morgan");
        URL url2 = new ServiceConfigURL("dubbo", "admin", "hello1234", "10.20.130.230", 20880, "context/path", params);

        assertURLStrDecoder(url2);
        assertEquals(url1, url2);
    }

    @Test
    void test_toString() throws Exception {
        URL url1 = URL.valueOf(
                "dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        assertURLStrDecoder(url1);
        assertThat(
                url1.toString(),
                anyOf(
                        equalTo("dubbo://10.20.130.230:20880/context/path?version=1.0.0&application=morgan"),
                        equalTo("dubbo://10.20.130.230:20880/context/path?application=morgan&version=1.0.0")));
    }

    @Test
    void test_toFullString() throws Exception {
        URL url1 = URL.valueOf(
                "dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        assertURLStrDecoder(url1);
        assertThat(
                url1.toFullString(),
                anyOf(
                        equalTo(
                                "dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan"),
                        equalTo(
                                "dubbo://admin:hello1234@10.20.130.230:20880/context/path?application=morgan&version=1.0.0")));
    }

    @Test
    void test_set_methods() throws Exception {
        URL url = URL.valueOf(
                "dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        assertURLStrDecoder(url);

        url = url.setHost("host");

        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("host", url.getHost());
        assertEquals("host:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());
        assertEquals("morgan", url.getParameter("application"));

        url = url.setPort(1);

        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("host", url.getHost());
        assertEquals("host:1", url.getAddress());
        assertEquals(1, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());
        assertEquals("morgan", url.getParameter("application"));

        url = url.setPath("path");

        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("host", url.getHost());
        assertEquals("host:1", url.getAddress());
        assertEquals(1, url.getPort());
        assertEquals("path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());
        assertEquals("morgan", url.getParameter("application"));

        url = url.setProtocol("protocol");

        assertURLStrDecoder(url);
        assertEquals("protocol", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("host", url.getHost());
        assertEquals("host:1", url.getAddress());
        assertEquals(1, url.getPort());
        assertEquals("path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());
        assertEquals("morgan", url.getParameter("application"));

        url = url.setUsername("username");

        assertURLStrDecoder(url);
        assertEquals("protocol", url.getProtocol());
        assertEquals("username", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("host", url.getHost());
        assertEquals("host:1", url.getAddress());
        assertEquals(1, url.getPort());
        assertEquals("path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());
        assertEquals("morgan", url.getParameter("application"));

        url = url.setPassword("password");

        assertURLStrDecoder(url);
        assertEquals("protocol", url.getProtocol());
        assertEquals("username", url.getUsername());
        assertEquals("password", url.getPassword());
        assertEquals("host", url.getHost());
        assertEquals("host:1", url.getAddress());
        assertEquals(1, url.getPort());
        assertEquals("path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());
        assertEquals("morgan", url.getParameter("application"));
    }

    @Test
    void test_removeParameters() throws Exception {
        URL url = URL.valueOf(
                "dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan&k1=v1&k2=v2");
        assertURLStrDecoder(url);

        url = url.removeParameter("version");
        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(3, url.getParameters().size());
        assertEquals("morgan", url.getParameter("application"));
        assertEquals("v1", url.getParameter("k1"));
        assertEquals("v2", url.getParameter("k2"));
        assertNull(url.getVersion());

        url = URL.valueOf(
                "dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan&k1=v1&k2=v2");
        url = url.removeParameters("version", "application", "NotExistedKey");
        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("v1", url.getParameter("k1"));
        assertEquals("v2", url.getParameter("k2"));
        assertNull(url.getVersion());
        assertNull(url.getParameter("application"));

        url = URL.valueOf(
                "dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan&k1=v1&k2=v2");
        url = url.removeParameters(Arrays.asList("version", "application"));
        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("v1", url.getParameter("k1"));
        assertEquals("v2", url.getParameter("k2"));
        assertNull(url.getVersion());
        assertNull(url.getParameter("application"));
    }

    @Test
    void test_addParameter() throws Exception {
        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?application=morgan");
        url = url.addParameter("k1", "v1");

        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("morgan", url.getParameter("application"));
        assertEquals("v1", url.getParameter("k1"));
    }

    @Test
    void test_addParameter_sameKv() throws Exception {
        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?application=morgan&k1=v1");
        URL newUrl = url.addParameter("k1", "v1");

        assertURLStrDecoder(url);
        assertSame(newUrl, url);
    }

    @Test
    void test_addParameters() throws Exception {
        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?application=morgan");
        url = url.addParameters(CollectionUtils.toStringMap("k1", "v1", "k2", "v2"));

        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(3, url.getParameters().size());
        assertEquals("morgan", url.getParameter("application"));
        assertEquals("v1", url.getParameter("k1"));
        assertEquals("v2", url.getParameter("k2"));

        url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?application=morgan");
        url = url.addParameters("k1", "v1", "k2", "v2", "application", "xxx");

        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(3, url.getParameters().size());
        assertEquals("xxx", url.getParameter("application"));
        assertEquals("v1", url.getParameter("k1"));
        assertEquals("v2", url.getParameter("k2"));

        url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?application=morgan");
        url = url.addParametersIfAbsent(CollectionUtils.toStringMap("k1", "v1", "k2", "v2", "application", "xxx"));

        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(3, url.getParameters().size());
        assertEquals("morgan", url.getParameter("application"));
        assertEquals("v1", url.getParameter("k1"));
        assertEquals("v2", url.getParameter("k2"));

        url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?application=morgan");
        url = url.addParameter("k1", "v1");

        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("morgan", url.getParameter("application"));
        assertEquals("v1", url.getParameter("k1"));

        url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?application=morgan");
        url = url.addParameter("application", "xxx");

        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(1, url.getParameters().size());
        assertEquals("xxx", url.getParameter("application"));
    }

    @Test
    void test_addParameters_SameKv() throws Exception {
        {
            URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?application=morgan&k1=v1");
            URL newUrl = url.addParameters(CollectionUtils.toStringMap("k1", "v1"));

            assertURLStrDecoder(url);
            assertSame(url, newUrl);
        }
        {
            URL url = URL.valueOf(
                    "dubbo://admin:hello1234@10.20.130.230:20880/context/path?application=morgan&k1=v1&k2=v2");
            URL newUrl = url.addParameters(CollectionUtils.toStringMap("k1", "v1", "k2", "v2"));

            assertURLStrDecoder(url);
            assertSame(newUrl, url);
        }
    }

    @Test
    void test_addParameterIfAbsent() throws Exception {
        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?application=morgan");
        url = url.addParameterIfAbsent("application", "xxx");

        assertURLStrDecoder(url);
        assertEquals("dubbo", url.getProtocol());
        assertEquals("admin", url.getUsername());
        assertEquals("hello1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(1, url.getParameters().size());
        assertEquals("morgan", url.getParameter("application"));
    }

    @Test
    void test_windowAbsolutePathBeginWithSlashIsValid() throws Exception {
        final String osProperty = System.getProperties().getProperty(OS_NAME_KEY);
        if (!osProperty.toLowerCase().contains(OS_WIN_PREFIX)) return;

        System.out.println("Test Windows valid path string.");

        File f0 = new File("C:/Windows");
        File f1 = new File("/C:/Windows");

        File f2 = new File("C:\\Windows");
        File f3 = new File("/C:\\Windows");
        File f4 = new File("\\C:\\Windows");

        assertEquals(f0, f1);
        assertEquals(f0, f2);
        assertEquals(f0, f3);
        assertEquals(f0, f4);
    }

    @Test
    void test_javaNetUrl() throws Exception {
        java.net.URL url = new java.net.URL(
                "http://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan#anchor1");

        assertEquals("http", url.getProtocol());
        assertEquals("admin:hello1234", url.getUserInfo());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals(20880, url.getPort());
        assertEquals("/context/path", url.getPath());
        assertEquals("version=1.0.0&application=morgan", url.getQuery());
        assertEquals("anchor1", url.getRef());

        assertEquals("admin:hello1234@10.20.130.230:20880", url.getAuthority());
        assertEquals("/context/path?version=1.0.0&application=morgan", url.getFile());
    }

    @Test
    void test_Anyhost() throws Exception {
        URL url = URL.valueOf("dubbo://0.0.0.0:20880");
        assertURLStrDecoder(url);
        assertEquals("0.0.0.0", url.getHost());
        assertTrue(url.isAnyHost());
    }

    @Test
    void test_Localhost() throws Exception {
        URL url = URL.valueOf("dubbo://127.0.0.1:20880");
        assertURLStrDecoder(url);
        assertEquals("127.0.0.1", url.getHost());
        assertEquals("127.0.0.1:20880", url.getAddress());
        assertTrue(url.isLocalHost());

        url = URL.valueOf("dubbo://127.0.1.1:20880");
        assertURLStrDecoder(url);
        assertEquals("127.0.1.1", url.getHost());
        assertEquals("127.0.1.1:20880", url.getAddress());
        assertTrue(url.isLocalHost());

        url = URL.valueOf("dubbo://localhost:20880");
        assertURLStrDecoder(url);
        assertEquals("localhost", url.getHost());
        assertEquals("localhost:20880", url.getAddress());
        assertTrue(url.isLocalHost());
    }

    @Test
    void test_Path() throws Exception {
        URL url = new ServiceConfigURL("dubbo", "localhost", 20880, "////path");
        assertURLStrDecoder(url);
        assertEquals("path", url.getPath());
    }

    @Test
    void testAddParameters() throws Exception {
        URL url = URL.valueOf("dubbo://127.0.0.1:20880");
        assertURLStrDecoder(url);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("version", null);
        url.addParameters(parameters);
        assertURLStrDecoder(url);
    }

    @Test
    void testUserNamePasswordContainsAt() {
        // Test username or password contains "@"
        URL url = URL.valueOf("ad@min:hello@1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        assertURLStrDecoder(url);
        assertNull(url.getProtocol());
        assertEquals("ad@min", url.getUsername());
        assertEquals("hello@1234", url.getPassword());
        assertEquals("10.20.130.230", url.getHost());
        assertEquals("10.20.130.230:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());
        assertEquals("morgan", url.getParameter("application"));
    }

    @Test
    void testIpV6Address() {
        // Test username or password contains "@"
        URL url = URL.valueOf(
                "ad@min111:haha@1234@2001:0db8:85a3:08d3:1319:8a2e:0370:7344:20880/context/path?version=1.0.0&application=morgan");
        assertURLStrDecoder(url);
        assertNull(url.getProtocol());
        assertEquals("ad@min111", url.getUsername());
        assertEquals("haha@1234", url.getPassword());
        assertEquals("2001:0db8:85a3:08d3:1319:8a2e:0370:7344", url.getHost());
        assertEquals("2001:0db8:85a3:08d3:1319:8a2e:0370:7344:20880", url.getAddress());
        assertEquals(20880, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());
        assertEquals("morgan", url.getParameter("application"));
    }

    @Test
    void testIpV6AddressWithScopeId() {
        URL url =
                URL.valueOf("2001:0db8:85a3:08d3:1319:8a2e:0370:7344%5/context/path?version=1.0.0&application=morgan");
        assertURLStrDecoder(url);
        assertNull(url.getProtocol());
        assertEquals("2001:0db8:85a3:08d3:1319:8a2e:0370:7344%5", url.getHost());
        assertEquals("2001:0db8:85a3:08d3:1319:8a2e:0370:7344%5", url.getAddress());
        assertEquals(0, url.getPort());
        assertEquals("context/path", url.getPath());
        assertEquals(2, url.getParameters().size());
        assertEquals("1.0.0", url.getVersion());
        assertEquals("morgan", url.getParameter("application"));
    }

    @Test
    void testDefaultPort() {
        Assertions.assertEquals("10.20.153.10:2181", URL.appendDefaultPort("10.20.153.10:0", 2181));
        Assertions.assertEquals("10.20.153.10:2181", URL.appendDefaultPort("10.20.153.10", 2181));
    }

    @Test
    void testGetServiceKey() {
        URL url1 = URL.valueOf("10.20.130.230:20880/context/path?interface=org.apache.dubbo.test.interfaceName");
        assertURLStrDecoder(url1);
        Assertions.assertEquals("org.apache.dubbo.test.interfaceName", url1.getServiceKey());

        URL url2 = URL.valueOf(
                "10.20.130.230:20880/org.apache.dubbo.test.interfaceName?interface=org.apache.dubbo.test.interfaceName");
        assertURLStrDecoder(url2);
        Assertions.assertEquals("org.apache.dubbo.test.interfaceName", url2.getServiceKey());

        URL url3 = URL.valueOf(
                "10.20.130.230:20880/org.apache.dubbo.test.interfaceName?interface=org.apache.dubbo.test.interfaceName&group=group1&version=1.0.0");
        assertURLStrDecoder(url3);
        Assertions.assertEquals("group1/org.apache.dubbo.test.interfaceName:1.0.0", url3.getServiceKey());

        URL url4 = URL.valueOf("10.20.130.230:20880/context/path?interface=org.apache.dubbo.test.interfaceName");
        assertURLStrDecoder(url4);
        Assertions.assertEquals("context/path", url4.getPathKey());

        URL url5 = URL.valueOf(
                "10.20.130.230:20880/context/path?interface=org.apache.dubbo.test.interfaceName&group=group1&version=1.0.0");
        assertURLStrDecoder(url5);
        Assertions.assertEquals("group1/context/path:1.0.0", url5.getPathKey());
    }

    @Test
    void testGetColonSeparatedKey() {
        URL url1 = URL.valueOf(
                "10.20.130.230:20880/context/path?interface=org.apache.dubbo.test.interfaceName&group=group&version=1.0.0");
        assertURLStrDecoder(url1);
        Assertions.assertEquals("org.apache.dubbo.test.interfaceName:1.0.0:group", url1.getColonSeparatedKey());

        URL url2 = URL.valueOf(
                "10.20.130.230:20880/context/path?interface=org.apache.dubbo.test.interfaceName&version=1.0.0");
        assertURLStrDecoder(url2);
        Assertions.assertEquals("org.apache.dubbo.test.interfaceName:1.0.0:", url2.getColonSeparatedKey());

        URL url3 = URL.valueOf(
                "10.20.130.230:20880/context/path?interface=org.apache.dubbo.test.interfaceName&group=group");
        assertURLStrDecoder(url3);
        Assertions.assertEquals("org.apache.dubbo.test.interfaceName::group", url3.getColonSeparatedKey());

        URL url4 = URL.valueOf("10.20.130.230:20880/context/path?interface=org.apache.dubbo.test.interfaceName");
        assertURLStrDecoder(url4);
        Assertions.assertEquals("org.apache.dubbo.test.interfaceName::", url4.getColonSeparatedKey());

        URL url5 = URL.valueOf("10.20.130.230:20880/org.apache.dubbo.test.interfaceName");
        assertURLStrDecoder(url5);
        Assertions.assertEquals("org.apache.dubbo.test.interfaceName::", url5.getColonSeparatedKey());

        URL url6 = URL.valueOf(
                "10.20.130.230:20880/org.apache.dubbo.test.interfaceName?interface=org.apache.dubbo.test.interfaceName1");
        assertURLStrDecoder(url6);
        Assertions.assertEquals("org.apache.dubbo.test.interfaceName1::", url6.getColonSeparatedKey());
    }

    @Test
    void testValueOf() {
        URL url = URL.valueOf("10.20.130.230");
        assertURLStrDecoder(url);

        url = URL.valueOf("10.20.130.230:20880");
        assertURLStrDecoder(url);

        url = URL.valueOf("dubbo://10.20.130.230:20880");
        assertURLStrDecoder(url);

        url = URL.valueOf("dubbo://10.20.130.230:20880/path");
        assertURLStrDecoder(url);
    }

    /**
     * Test {@link URL#getParameters(Predicate)} method
     *
     * @since 2.7.8
     */
    @Test
    void testGetParameters() {
        URL url = URL.valueOf(
                "10.20.130.230:20880/context/path?interface=org.apache.dubbo.test.interfaceName&group=group&version=1.0.0");
        Map<String, String> parameters = url.getParameters(i -> "version".equals(i));
        String version = parameters.get("version");
        assertEquals(1, parameters.size());
        assertEquals("1.0.0", version);
    }

    @Test
    void testGetParameter() {
        URL url = URL.valueOf("http://127.0.0.1:8080/path?i=1&b=false");
        assertEquals(Integer.valueOf(1), url.getParameter("i", Integer.class));
        assertEquals(Boolean.FALSE, url.getParameter("b", Boolean.class));
    }

    @Test
    void testEquals() {
        URL url1 = URL.valueOf(
                "10.20.130.230:20880/context/path?interface=org.apache.dubbo.test.interfaceName&group=group&version=1.0.0");
        URL url2 = URL.valueOf(
                "10.20.130.230:20880/context/path?interface=org.apache.dubbo.test.interfaceName&group=group&version=1.0.0");
        Assertions.assertEquals(url1, url2);

        URL url3 = URL.valueOf(
                "10.20.130.230:20881/context/path?interface=org.apache.dubbo.test.interfaceName&group=group&version=1.0.0");
        Assertions.assertNotEquals(url1, url3);

        URL url4 = URL.valueOf(
                "10.20.130.230:20880/context/path?interface=org.apache.dubbo.test.interfaceName&weight=10&group=group&version=1.0.0");
        Assertions.assertNotEquals(url1, url4);

        URL url5 = URL.valueOf(
                "10.20.130.230:20880/context/path?interface=org.apache.dubbo.test.interfaceName&weight=10&group=group&version=1.0.0");
        Assertions.assertEquals(url4, url5);

        URL url6 = URL.valueOf("consumer://30.225.20.150/org.apache.dubbo.rpc.service.GenericService?application="
                + "dubbo-demo-api-consumer&category=consumers&check=false&dubbo=2.0.2&generic=true&interface="
                + "org.apache.dubbo.demo.DemoService&pid=7375&side=consumer&sticky=false&timestamp=1599556506417");
        URL url7 = URL.valueOf("consumer://30.225.20.150/org.apache.dubbo.rpc.service.GenericService?application="
                + "dubbo-demo-api-consumer&category=consumers&check=false&dubbo=2.0.2&generic=true&interface="
                + "org.apache.dubbo.demo.DemoService&pid=7375&side=consumer&sticky=false&timestamp=2299556506417");
        assertEquals(url6, url7);

        URL url8 = URL.valueOf("consumer://30.225.20.150/org.apache.dubbo.rpc.service.GenericService?application="
                + "dubbo-demo-api-consumer&category=consumers&check=false&dubbo=2.0.2&interface="
                + "org.apache.dubbo.demo.DemoService&pid=7375&side=consumer&sticky=false&timestamp=2299556506417");
        assertNotEquals(url7, url8);

        URL url9 = URL.valueOf("consumer://30.225.20.150/org.apache.dubbo.rpc.service.GenericService?application="
                + "dubbo-demo-api-consumer&category=consumers&check=true&dubbo=2.0.2&interface="
                + "org.apache.dubbo.demo.DemoService&pid=7375&side=consumer&sticky=false&timestamp=2299556506417");
        assertNotEquals(url8, url9);
    }

    @Test
    void testEqualsWithPassword() {
        URL url1 = URL.valueOf("ad@min:hello@1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        URL url2 = URL.valueOf("ad@min:hello@4321@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        URL url3 = URL.valueOf("ad@min:hello@1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");

        boolean actual1 = url1.equals(url2);
        boolean actual2 = url1.equals(url3);
        assertFalse(actual1);
        assertTrue(actual2);
    }

    @Test
    void testEqualsWithPath() {
        URL url1 = URL.valueOf("ad@min:hello@1234@10.20.130.230:20880/context/path1?version=1.0.0&application=morgan");
        URL url2 = URL.valueOf("ad@min:hello@1234@10.20.130.230:20880/context/path2?version=1.0.0&application=morgan");
        URL url3 = URL.valueOf("ad@min:hello@1234@10.20.130.230:20880/context/path1?version=1.0.0&application=morgan");

        boolean actual1 = url1.equals(url2);
        boolean actual2 = url1.equals(url3);
        assertFalse(actual1);
        assertTrue(actual2);
    }

    @Test
    void testEqualsWithPort() {
        URL url1 = URL.valueOf("ad@min:hello@1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        URL url2 = URL.valueOf("ad@min:hello@1234@10.20.130.230:20881/context/path?version=1.0.0&application=morgan");
        URL url3 = URL.valueOf("ad@min:hello@1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");

        boolean actual1 = url1.equals(url2);
        boolean actual2 = url1.equals(url3);
        assertFalse(actual1);
        assertTrue(actual2);
    }

    @Test
    void testEqualsWithProtocol() {
        URL url1 = URL.valueOf(
                "dubbo://ad@min:hello@1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        URL url2 = URL.valueOf(
                "file://ad@min:hello@1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        URL url3 = URL.valueOf(
                "dubbo://ad@min:hello@1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");

        boolean actual1 = url1.equals(url2);
        boolean actual2 = url1.equals(url3);
        assertFalse(actual1);
        assertTrue(actual2);
    }

    @Test
    void testEqualsWithUser() {
        URL url1 = URL.valueOf("ad@min1:hello@1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        URL url2 = URL.valueOf("ad@min2:hello@1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        URL url3 = URL.valueOf("ad@min1:hello@1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");

        boolean actual1 = url1.equals(url2);
        boolean actual2 = url1.equals(url3);
        assertFalse(actual1);
        assertTrue(actual2);
    }

    @Test
    void testHashcode() {
        URL url1 = URL.valueOf("consumer://30.225.20.150/org.apache.dubbo.rpc.service.GenericService?application="
                + "dubbo-demo-api-consumer&category=consumers&check=false&dubbo=2.0.2&generic=true&interface="
                + "org.apache.dubbo.demo.DemoService&pid=7375&side=consumer&sticky=false&timestamp=1599556506417");
        URL url2 = URL.valueOf("consumer://30.225.20.150/org.apache.dubbo.rpc.service.GenericService?application="
                + "dubbo-demo-api-consumer&category=consumers&check=false&dubbo=2.0.2&generic=true&interface="
                + "org.apache.dubbo.demo.DemoService&pid=7375&side=consumer&sticky=false&timestamp=2299556506417");
        assertEquals(url1.hashCode(), url2.hashCode());

        URL url3 = URL.valueOf("consumer://30.225.20.150/org.apache.dubbo.rpc.service.GenericService?application="
                + "dubbo-demo-api-consumer&category=consumers&check=false&dubbo=2.0.2&interface="
                + "org.apache.dubbo.demo.DemoService&pid=7375&side=consumer&sticky=false&timestamp=2299556506417");
        assertNotEquals(url2.hashCode(), url3.hashCode());

        URL url4 = URL.valueOf("consumer://30.225.20.150/org.apache.dubbo.rpc.service.GenericService?application="
                + "dubbo-demo-api-consumer&category=consumers&check=true&dubbo=2.0.2&interface="
                + "org.apache.dubbo.demo.DemoService&pid=7375&side=consumer&sticky=false&timestamp=2299556506417");
        assertNotEquals(url3.hashCode(), url4.hashCode());
    }

    @Test
    void testParameterContainPound() {
        URL url = URL.valueOf(
                "dubbo://ad@min:hello@1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan&pound=abcd#efg&protocol=registry");
        Assertions.assertEquals("abcd#efg", url.getParameter("pound"));
        Assertions.assertEquals("registry", url.getParameter("protocol"));
    }

    @Test
    void test_valueOfHasNameWithoutValue() throws Exception {
        URL url = URL.valueOf(
                "dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan&noValue");
        Assertions.assertEquals("", url.getParameter("noValue"));
    }

    @Test
    void testGetAuthority() {
        URL url = URL.valueOf("admin1:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=app1");
        assertEquals("admin1:hello1234@10.20.130.230:20880", url.getAuthority());

        URL urlWithoutUsername =
                URL.valueOf(":hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=app1");
        assertEquals(":hello1234@10.20.130.230:20880", urlWithoutUsername.getAuthority());

        URL urlWithoutPassword = URL.valueOf("admin1:@10.20.130.230:20880/context/path?version=1.0.0&application=app1");
        assertEquals("admin1:@10.20.130.230:20880", urlWithoutPassword.getAuthority());

        URL urlWithoutUserInformation = URL.valueOf("10.20.130.230:20880/context/path?version=1.0.0&application=app1");
        assertEquals("10.20.130.230:20880", urlWithoutUserInformation.getAuthority());

        URL urlWithoutPort = URL.valueOf("admin1:hello1234@10.20.130.230/context/path?version=1.0.0&application=app1");
        assertEquals("admin1:hello1234@10.20.130.230", urlWithoutPort.getAuthority());
    }

    @Test
    void testGetUserInformation() {
        URL url = URL.valueOf("admin1:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=app1");
        assertEquals("admin1:hello1234", url.getUserInformation());

        URL urlWithoutUsername =
                URL.valueOf(":hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=app1");
        assertEquals(":hello1234@10.20.130.230:20880", urlWithoutUsername.getAuthority());

        URL urlWithoutPassword = URL.valueOf("admin1:@10.20.130.230:20880/context/path?version=1.0.0&application=app1");
        assertEquals("admin1:@10.20.130.230:20880", urlWithoutPassword.getAuthority());

        URL urlWithoutUserInformation = URL.valueOf("10.20.130.230:20880/context/path?version=1.0.0&application=app1");
        assertEquals("10.20.130.230:20880", urlWithoutUserInformation.getAuthority());
    }

    @Test
    void testIPV6() {
        URL url = URL.valueOf("dubbo://[2408:4004:194:8896:3e8a:82ae:814a:398]:20881?name=apache");
        assertEquals("[2408:4004:194:8896:3e8a:82ae:814a:398]", url.getHost());
        assertEquals(20881, url.getPort());
        assertEquals("apache", url.getParameter("name"));
    }

    @Test
    void testToServiceString() {
        URL url = URL.valueOf(
                "zookeeper://10.20.130.230:4444/org.apache.dubbo.metadata.report.MetadataReport?version=1.0.0&application=vic&group=aaa");
        assertEquals(
                "zookeeper://10.20.130.230:4444/aaa/org.apache.dubbo.metadata.report.MetadataReport:1.0.0",
                url.toServiceString());
    }

    @Test
    void testToServiceStringWithParameters() {
        URL url = URL.valueOf(
                "zookeeper://10.20.130.230:4444/org.apache.dubbo.metadata.report.MetadataReport?version=1.0.0&application=vic&group=aaa&namespace=test");
        assertEquals(
                "zookeeper://10.20.130.230:4444/aaa/org.apache.dubbo.metadata.report.MetadataReport:1.0.0?namespace=test",
                url.toServiceString("namespace"));
    }
}
