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


import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NetUtilsTest {
    @Test
    public void testGetRandomPort() throws Exception {
        assertThat(NetUtils.getRandomPort(), greaterThanOrEqualTo(30000));
        assertThat(NetUtils.getRandomPort(), greaterThanOrEqualTo(30000));
        assertThat(NetUtils.getRandomPort(), greaterThanOrEqualTo(30000));
    }

    @Test
    public void testGetAvailablePort() throws Exception {
        assertThat(NetUtils.getAvailablePort(), greaterThan(0));
        assertThat(NetUtils.getAvailablePort(12345), greaterThanOrEqualTo(12345));
        assertThat(NetUtils.getAvailablePort(-1), greaterThanOrEqualTo(30000));
    }

    @Test
    public void testValidAddress() throws Exception {
        assertTrue(NetUtils.isValidAddress("10.20.130.230:20880"));
        assertFalse(NetUtils.isValidAddress("10.20.130.230"));
        assertFalse(NetUtils.isValidAddress("10.20.130.230:666666"));
    }

    @Test
    public void testIsInvalidPort() throws Exception {
        assertTrue(NetUtils.isInvalidPort(0));
        assertTrue(NetUtils.isInvalidPort(65536));
        assertFalse(NetUtils.isInvalidPort(1024));
    }

    @Test
    public void testIsLocalHost() throws Exception {
        assertTrue(NetUtils.isLocalHost("localhost"));
        assertTrue(NetUtils.isLocalHost("127.1.2.3"));
        assertFalse(NetUtils.isLocalHost("128.1.2.3"));
    }

    @Test
    public void testIsAnyHost() throws Exception {
        assertTrue(NetUtils.isAnyHost("0.0.0.0"));
        assertFalse(NetUtils.isAnyHost("1.1.1.1"));
    }

    @Test
    public void testIsInvalidLocalHost() throws Exception {
        assertTrue(NetUtils.isInvalidLocalHost(null));
        assertTrue(NetUtils.isInvalidLocalHost(""));
        assertTrue(NetUtils.isInvalidLocalHost("localhost"));
        assertTrue(NetUtils.isInvalidLocalHost("0.0.0.0"));
        assertTrue(NetUtils.isInvalidLocalHost("127.1.2.3"));
    }

    @Test
    public void testIsValidLocalHost() throws Exception {
        assertTrue(NetUtils.isValidLocalHost("1.2.3.4"));
    }

    @Test
    public void testGetLocalSocketAddress() throws Exception {
        InetSocketAddress address = NetUtils.getLocalSocketAddress("localhost", 12345);
        assertTrue(address.getAddress().isAnyLocalAddress());
        assertEquals(address.getPort(), 12345);
        address = NetUtils.getLocalSocketAddress("dubbo-addr", 12345);
        assertEquals(address.getHostName(), "dubbo-addr");
        assertEquals(address.getPort(), 12345);
    }

    @Test
    public void testIsValidAddress() throws Exception {
        assertFalse(NetUtils.isValidAddress((InetAddress) null));
        InetAddress address = mock(InetAddress.class);
        when(address.isLoopbackAddress()).thenReturn(true);
        assertFalse(NetUtils.isValidAddress(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("localhost");
        assertFalse(NetUtils.isValidAddress(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("0.0.0.0");
        assertFalse(NetUtils.isValidAddress(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("127.0.0.1");
        assertFalse(NetUtils.isValidAddress(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("1.2.3.4");
        assertTrue(NetUtils.isValidAddress(address));
    }

    @Test
    public void testGetLocalHost() throws Exception {
        assertNotNull(NetUtils.getLocalHost());
    }

    @Test
    public void testGetLocalAddress() throws Exception {
        InetAddress address = NetUtils.getLocalAddress();
        assertNotNull(address);
        assertTrue(NetUtils.isValidLocalHost(address.getHostAddress()));
    }

    @Test
    public void testFilterLocalHost() throws Exception {
        assertNull(NetUtils.filterLocalHost(null));
        assertEquals(NetUtils.filterLocalHost(""), "");
        String host = NetUtils.filterLocalHost("dubbo://127.0.0.1:8080/foo");
        assertThat(host, equalTo("dubbo://" + NetUtils.getLocalHost() + ":8080/foo"));
        host = NetUtils.filterLocalHost("127.0.0.1:8080");
        assertThat(host, equalTo(NetUtils.getLocalHost() + ":8080"));
        host = NetUtils.filterLocalHost("0.0.0.0");
        assertThat(host, equalTo(NetUtils.getLocalHost()));
        host = NetUtils.filterLocalHost("88.88.88.88");
        assertThat(host, equalTo(host));
    }

    @Test
    public void testGetHostName() throws Exception {
        assertNotNull(NetUtils.getHostName("127.0.0.1"));
    }

    @Test
    public void testGetIpByHost() throws Exception {
        assertThat(NetUtils.getIpByHost("localhost"), equalTo("127.0.0.1"));
        assertThat(NetUtils.getIpByHost("dubbo"), equalTo("dubbo"));
    }

    @Test
    public void testToAddressString() throws Exception {
        InetAddress address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("dubbo");
        InetSocketAddress socketAddress = new InetSocketAddress(address, 1234);
        assertThat(NetUtils.toAddressString(socketAddress), equalTo("dubbo:1234"));
    }

    @Test
    public void testToAddress() throws Exception {
        InetSocketAddress address = NetUtils.toAddress("localhost:1234");
        assertThat(address.getHostName(), equalTo("localhost"));
        assertThat(address.getPort(), equalTo(1234));
        address = NetUtils.toAddress("localhost");
        assertThat(address.getHostName(), equalTo("localhost"));
        assertThat(address.getPort(), equalTo(0));
    }

    @Test
    public void testToURL() throws Exception {
        String url = NetUtils.toURL("dubbo", "host", 1234, "foo");
        assertThat(url, equalTo("dubbo://host:1234/foo"));
    }
}