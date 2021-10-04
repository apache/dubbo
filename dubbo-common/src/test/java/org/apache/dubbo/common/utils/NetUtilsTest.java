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


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_NETWORK_IGNORED_INTERFACE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
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
        assertThat(NetUtils.getAvailablePort(-1), greaterThanOrEqualTo(0));
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
        assertTrue(NetUtils.isInvalidLocalHost("127.0.0.1"));
        assertFalse(NetUtils.isInvalidLocalHost("128.0.0.1"));
    }

    @Test
    public void testIsValidLocalHost() throws Exception {
        assertTrue(NetUtils.isValidLocalHost("1.2.3.4"));
        assertTrue(NetUtils.isValidLocalHost("128.0.0.1"));
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
        assertFalse(NetUtils.isValidV4Address((InetAddress) null));
        InetAddress address = mock(InetAddress.class);
        when(address.isLoopbackAddress()).thenReturn(true);
        assertFalse(NetUtils.isValidV4Address(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("localhost");
        assertFalse(NetUtils.isValidV4Address(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("0.0.0.0");
        assertFalse(NetUtils.isValidV4Address(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("127.0.0.1");
        assertFalse(NetUtils.isValidV4Address(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("1.2.3.4");
        assertTrue(NetUtils.isValidV4Address(address));
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
        assertThat(NetUtils.getIpByHost("dubbo.local"), equalTo("dubbo.local"));
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

    @Test
    public void testIsValidV6Address() {
        String saved = System.getProperty("java.net.preferIPv6Addresses", "false");
        System.setProperty("java.net.preferIPv6Addresses", "true");

        InetAddress address = NetUtils.getLocalAddress();
        boolean isPreferIPV6Address = NetUtils.isPreferIPV6Address();

        // Restore system property to previous value before executing test
        System.setProperty("java.net.preferIPv6Addresses", saved);

        assumeTrue(address instanceof Inet6Address);
        assertThat(isPreferIPV6Address, equalTo(true));
    }

    /**
     * Mockito starts to support mocking final classes since 2.1.0
     * see https://github.com/mockito/mockito/wiki/What%27s-new-in-Mockito-2#unmockable
     * But enable it will cause other UT to fail.
     * Therefore currently disabling this UT.
     */
    @Disabled
    @Test
    public void testNormalizeV6Address() {
        Inet6Address address = mock(Inet6Address.class);
        when(address.getHostAddress()).thenReturn("fe80:0:0:0:894:aeec:f37d:23e1%en0");
        when(address.getScopeId()).thenReturn(5);
        InetAddress normalized = NetUtils.normalizeV6Address(address);
        assertThat(normalized.getHostAddress(), equalTo("fe80:0:0:0:894:aeec:f37d:23e1%5"));
    }

    @Test
    public void testMatchIpRangeMatchWhenIpv4() throws UnknownHostException {
        assertTrue(NetUtils.matchIpRange("*.*.*.*", "192.168.1.63", 90));
        assertTrue(NetUtils.matchIpRange("192.168.1.*", "192.168.1.63", 90));
        assertTrue(NetUtils.matchIpRange("192.168.1.63", "192.168.1.63", 90));
        assertTrue(NetUtils.matchIpRange("192.168.1.1-65", "192.168.1.63", 90));
        assertFalse(NetUtils.matchIpRange("192.168.1.1-61", "192.168.1.63", 90));
        assertFalse(NetUtils.matchIpRange("192.168.1.62", "192.168.1.63", 90));
    }

    @Test
    public void testMatchIpRangeMatchWhenIpv6() throws UnknownHostException {
        assertTrue(NetUtils.matchIpRange("*.*.*.*", "192.168.1.63", 90));
        assertTrue(NetUtils.matchIpRange("234e:0:4567:0:0:0:3d:*", "234e:0:4567::3d:ff", 90));
        assertTrue(NetUtils.matchIpRange("234e:0:4567:0:0:0:3d:ee", "234e:0:4567::3d:ee", 90));
        assertTrue(NetUtils.matchIpRange("234e:0:4567::3d:ee", "234e:0:4567::3d:ee", 90));
        assertTrue(NetUtils.matchIpRange("234e:0:4567:0:0:0:3d:0-ff", "234e:0:4567::3d:ee", 90));
        assertTrue(NetUtils.matchIpRange("234e:0:4567:0:0:0:3d:0-ee", "234e:0:4567::3d:ee", 90));

        assertFalse(NetUtils.matchIpRange("234e:0:4567:0:0:0:3d:ff", "234e:0:4567::3d:ee", 90));
        assertFalse(NetUtils.matchIpRange("234e:0:4567:0:0:0:3d:0-ea", "234e:0:4567::3d:ee", 90));
    }

    @Test
    public void testMatchIpRangeMatchWhenIpv6Exception() throws UnknownHostException {
        IllegalArgumentException thrown =
            assertThrows(IllegalArgumentException.class, () ->
                NetUtils.matchIpRange("234e:0:4567::3d:*", "234e:0:4567::3d:ff", 90));
        assertTrue(thrown.getMessage().contains("If you config ip expression that contains '*'"));

        thrown = assertThrows(IllegalArgumentException.class, () ->
            NetUtils.matchIpRange("234e:0:4567:3d", "234e:0:4567::3d:ff", 90));
        assertTrue(thrown.getMessage().contains("The host is ipv6, but the pattern is not ipv6 pattern"));

        thrown =
            assertThrows(IllegalArgumentException.class, () ->
                NetUtils.matchIpRange("192.168.1.1-65-3", "192.168.1.63", 90));
        assertTrue(thrown.getMessage().contains("There is wrong format of ip Address"));
    }

    @Test
    public void testMatchIpRangeMatchWhenIpWrongException() throws UnknownHostException {
        UnknownHostException thrown =
            assertThrows(UnknownHostException.class, () ->
                NetUtils.matchIpRange("192.168.1.63", "192.168.1.ff", 90));
        assertTrue(thrown.getMessage().contains("192.168.1.ff"));
    }

    @Test
    public void testMatchIpMatch() throws UnknownHostException {
        assertTrue(NetUtils.matchIpExpression("192.168.1.*", "192.168.1.63", 90));
        assertTrue(NetUtils.matchIpExpression("192.168.1.192/26", "192.168.1.199", 90));
    }

    @Test
    public void testMatchIpv6WithIpPort() throws UnknownHostException {
        assertTrue(NetUtils.matchIpRange("[234e:0:4567::3d:ee]", "234e:0:4567::3d:ee", 8090));
        assertTrue(NetUtils.matchIpRange("[234e:0:4567:0:0:0:3d:ee]", "234e:0:4567::3d:ee", 8090));
        assertTrue(NetUtils.matchIpRange("[234e:0:4567:0:0:0:3d:ee]:8090", "234e:0:4567::3d:ee", 8090));
        assertTrue(NetUtils.matchIpRange("[234e:0:4567:0:0:0:3d:0-ee]:8090", "234e:0:4567::3d:ee", 8090));
        assertTrue(NetUtils.matchIpRange("[234e:0:4567:0:0:0:3d:ee-ff]:8090", "234e:0:4567::3d:ee", 8090));
        assertTrue(NetUtils.matchIpRange("[234e:0:4567:0:0:0:3d:*]:90", "234e:0:4567::3d:ff", 90));

        assertFalse(NetUtils.matchIpRange("[234e:0:4567:0:0:0:3d:ee]:7289", "234e:0:4567::3d:ee", 8090));
        assertFalse(NetUtils.matchIpRange("[234e:0:4567:0:0:0:3d:ee-ff]:8090", "234e:0:4567::3d:ee", 9090));
    }

    @Test
    public void testMatchIpv4WithIpPort() throws UnknownHostException {
        NumberFormatException thrown =
            assertThrows(NumberFormatException.class, () -> NetUtils.matchIpExpression("192.168.1.192/26:90", "192.168.1.199", 90));
        assertTrue(thrown instanceof NumberFormatException);

        assertTrue(NetUtils.matchIpRange("*.*.*.*:90", "192.168.1.63", 90));
        assertTrue(NetUtils.matchIpRange("192.168.1.*:90", "192.168.1.63", 90));
        assertTrue(NetUtils.matchIpRange("192.168.1.63:90", "192.168.1.63", 90));
        assertTrue(NetUtils.matchIpRange("192.168.1.63-65:90", "192.168.1.63", 90));
        assertTrue(NetUtils.matchIpRange("192.168.1.1-63:90", "192.168.1.63", 90));

        assertFalse(NetUtils.matchIpRange("*.*.*.*:80", "192.168.1.63", 90));
        assertFalse(NetUtils.matchIpRange("192.168.1.*:80", "192.168.1.63", 90));
        assertFalse(NetUtils.matchIpRange("192.168.1.63:80", "192.168.1.63", 90));
        assertFalse(NetUtils.matchIpRange("192.168.1.63-65:80", "192.168.1.63", 90));
        assertFalse(NetUtils.matchIpRange("192.168.1.1-63:80", "192.168.1.63", 90));

        assertFalse(NetUtils.matchIpRange("192.168.1.1-61:90", "192.168.1.62", 90));
        assertFalse(NetUtils.matchIpRange("192.168.1.62:90", "192.168.1.63", 90));
    }

    @Test
    public void testLocalHost() {
        assertEquals(NetUtils.getLocalHost(), NetUtils.getLocalAddress().getHostAddress());
        assertTrue(NetUtils.isValidLocalHost(NetUtils.getLocalHost()));
        assertFalse(NetUtils.isInvalidLocalHost(NetUtils.getLocalHost()));
    }

    @Test
    public void testIsMulticastAddress() {
        assertTrue(NetUtils.isMulticastAddress("224.0.0.1"));
        assertFalse(NetUtils.isMulticastAddress("127.0.0.1"));
    }

    @Test
    public void testFindNetworkInterface() {
        assertNotNull(NetUtils.findNetworkInterface());
    }

    @Test
    public void testIgnoreAllInterfaces() {
        // store the origin ignored interfaces
        String originIgnoredInterfaces = this.getIgnoredInterfaces();
        try {
            // ignore all interfaces
            this.setIgnoredInterfaces(".*");
            assertNull(NetUtils.findNetworkInterface());
        } finally {
            // recover the origin ignored interfaces
            this.setIgnoredInterfaces(originIgnoredInterfaces);
        }
    }

    @Test
    public void testIgnoreGivenInterface() {
        // store the origin ignored interfaces
        String originIgnoredInterfaces = this.getIgnoredInterfaces();
        try {
            NetworkInterface networkInterface = NetUtils.findNetworkInterface();
            assertNotNull(networkInterface);
            // ignore the given network interface's display name
            this.setIgnoredInterfaces(Pattern.quote(networkInterface.getDisplayName()));
            NetworkInterface newNetworkInterface = NetUtils.findNetworkInterface();
            if (newNetworkInterface != null) {
                assertTrue(!networkInterface.getDisplayName().equals(newNetworkInterface.getDisplayName()));
            }
        } finally {
            // recover the origin ignored interfaces
            this.setIgnoredInterfaces(originIgnoredInterfaces);
        }
    }

    @Test
    public void testIgnoreGivenPrefixInterfaceName() {
        // store the origin ignored interfaces
        String originIgnoredInterfaces = this.getIgnoredInterfaces();
        try {
            NetworkInterface networkInterface = NetUtils.findNetworkInterface();
            assertNotNull(networkInterface);
            // ignore the given prefix network interface's display name
            String displayName = networkInterface.getDisplayName();
            if (StringUtils.isNotEmpty(displayName) && displayName.length() > 2) {
                String ignoredInterfaces = Pattern.quote(displayName.substring(0, 1)) + ".*";
                this.setIgnoredInterfaces(ignoredInterfaces);
                NetworkInterface newNetworkInterface = NetUtils.findNetworkInterface();
                if (newNetworkInterface != null) {
                    assertTrue(!newNetworkInterface.getDisplayName().startsWith(displayName.substring(0, 1)));
                }
            }
        } finally {
            // recover the origin ignored interfaces
            this.setIgnoredInterfaces(originIgnoredInterfaces);
        }
    }

    private String getIgnoredInterfaces() {
        return System.getProperty(DUBBO_NETWORK_IGNORED_INTERFACE);
    }

    private void setIgnoredInterfaces(String ignoredInterfaces) {
        if (ignoredInterfaces != null) {
            System.setProperty(DUBBO_NETWORK_IGNORED_INTERFACE, ignoredInterfaces);
        } else {
            System.setProperty(DUBBO_NETWORK_IGNORED_INTERFACE, "");
        }
    }
}
