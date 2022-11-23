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

import org.apache.dubbo.common.url.component.DubboServiceAddressURL;
import org.apache.dubbo.common.url.component.ServiceAddressURL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;

import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class InterfaceAddressURLTest {
    private static final String rawURL = "dubbo://10.20.130.230:20880/context/path?version=1.0.0&group=g1&application=provider&timeout=1000&category=provider&side=provider&sayHello.weight=222";
    private static final URL overrideURL = URL.valueOf("override://10.20.130.230:20880/context/path?version=1.0.0&application=morgan&timeout=2000&category=configurators&sayHello.overrideKey=override");
    private static final URL consumerURL = URL.valueOf("consumer://10.20.130.230/context/path?version=2.0.0,1.0.0&group=g2&application=morgan&timeout=3000&side=consumer&sayHello.timeout=5000");

    @Test
    void testMergeOverriden() {
        URL url = URL.valueOf(rawURL);
        ServiceAddressURL interfaceAddressURL = new DubboServiceAddressURL(url.getUrlAddress(), url.getUrlParam(), null, null);
        assertEquals("1000", interfaceAddressURL.getParameter(TIMEOUT_KEY));

        ServiceAddressURL withConsumer = DubboServiceAddressURL.valueOf(rawURL, consumerURL);
        assertEquals("3000", withConsumer.getParameter(TIMEOUT_KEY));

        ServiceAddressURL withOverriden = DubboServiceAddressURL.valueOf(rawURL, consumerURL, (ServiceConfigURL) overrideURL);
        assertEquals("2000", withOverriden.getParameter(TIMEOUT_KEY));
    }

    @Test
    void testGetParameter() {
        URL url = URL.valueOf(rawURL);
        ServiceAddressURL interfaceAddressURL = new DubboServiceAddressURL(url.getUrlAddress(), url.getUrlParam(), consumerURL, null);

        assertEquals("3000", interfaceAddressURL.getParameter(TIMEOUT_KEY));

        assertEquals("morgan", interfaceAddressURL.getApplication());
        assertEquals("provider", interfaceAddressURL.getRemoteApplication());

        assertEquals("dubbo", interfaceAddressURL.getProtocol());
        assertEquals("context/path", interfaceAddressURL.getPath());

        assertEquals("consumer", interfaceAddressURL.getSide());
        assertEquals("1.0.0", interfaceAddressURL.getVersion());
        assertEquals("g1", interfaceAddressURL.getGroup());
    }

    @Test
    void testGetMethodParameter() {
        URL url = URL.valueOf(rawURL);
        ServiceAddressURL interfaceAddressURL = new DubboServiceAddressURL(url.getUrlAddress(), url.getUrlParam(), consumerURL, (ServiceConfigURL) overrideURL);

        assertEquals("5000", interfaceAddressURL.getMethodParameter("sayHello", TIMEOUT_KEY));
        assertEquals("2000", interfaceAddressURL.getMethodParameter("non-exist-methods", TIMEOUT_KEY));
        assertEquals("222", interfaceAddressURL.getMethodParameter("sayHello", "weight"));
        assertEquals("222", interfaceAddressURL.getMethodParameter("sayHello", "weight"));
        assertEquals("override", interfaceAddressURL.getMethodParameter("sayHello", "overrideKey"));
    }

    @Test
    void testURLEquals() {
        URL url1 = URL.valueOf(rawURL);
        URL url2 = URL.valueOf(rawURL);
        assertNotSame(url1, url2);
        assertEquals(url1, url2);

        // with consumer
        ServiceAddressURL withConsumer = new DubboServiceAddressURL(url1.getUrlAddress(), url1.getUrlParam(), consumerURL, null);
        ServiceAddressURL withConsumer2 = new DubboServiceAddressURL(url1.getUrlAddress(), url1.getUrlParam(), consumerURL, null);
        assertEquals(withConsumer, withConsumer2);

        ServiceAddressURL withOverride = new DubboServiceAddressURL(url1.getUrlAddress(), url1.getUrlParam(), consumerURL, (ServiceConfigURL) overrideURL);
        url2 = url2.addParameter("timeout", "4444");
        ServiceAddressURL withOverride2 = new DubboServiceAddressURL(url2.getUrlAddress(), url2.getUrlParam(), consumerURL, (ServiceConfigURL) overrideURL);
        assertNotEquals(url1, url2);
        assertEquals(withOverride, withOverride2);
    }

    @Test
    void testToString() {
        URL url1 = URL.valueOf(rawURL);
        System.out.println(url1.toString());
        ServiceAddressURL withConsumer = new DubboServiceAddressURL(url1.getUrlAddress(), url1.getUrlParam(), consumerURL, null);
        System.out.println(withConsumer.toString());
        ServiceAddressURL withOverride2 = new DubboServiceAddressURL(url1.getUrlAddress(), url1.getUrlParam(), consumerURL, (ServiceConfigURL) overrideURL);
        System.out.println(withOverride2.toString());
    }
}